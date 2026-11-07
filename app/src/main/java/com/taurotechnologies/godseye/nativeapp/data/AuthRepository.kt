package com.taurotechnologies.godseye.nativeapp.data

import android.content.Context
import com.taurotechnologies.godseye.nativeapp.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Supabase Auth REST client for password, signup, Google ID token, refresh, user validation, and logout. */
class AuthRepository(
    context: Context,
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(25, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    private val storage = SecureSessionStorage(context)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = mutableState.asStateFlow()

    suspend fun signIn(email: String, password: String) = authenticate {
        post("/auth/v1/token?grant_type=password", JSONObject().put("email", email.trim()).put("password", password))
    }

    suspend fun signUp(email: String, password: String) = authenticate {
        post("/auth/v1/signup", JSONObject().put("email", email.trim()).put("password", password))
    }

    suspend fun signInWithGoogle(idToken: String, nonce: String?) = authenticate {
        require(idToken.isNotBlank())
        val body = JSONObject().put("provider", "google").put("id_token", idToken)
        if (!nonce.isNullOrBlank()) body.put("nonce", nonce)
        post("/auth/v1/token?grant_type=id_token", body)
    }

    /** Restores only after the server confirms the stored access token and its user identity. */
    suspend fun restore() = mutex.withLock {
        val previouslyVerified = mutableState.value.session
        mutableState.value = AuthState(session = previouslyVerified, isLoading = true)
        try {
            withContext(Dispatchers.IO) {
                requireConfiguration()
                val initialSession = storage.read()
                if (initialSession == null) {
                    mutableState.value = AuthState()
                    return@withContext
                }
                var saved: AuthSession = initialSession
                if (saved.expiresAtEpochSeconds <= nowSeconds() + EXPIRY_SKEW_SECONDS) {
                    saved = refreshSession(saved.refreshToken, saved)
                    coroutineContext.ensureActive()
                }
                try {
                    val verifiedUser = fetchUser(saved.accessToken)
                    coroutineContext.ensureActive()
                    if (verifiedUser.id != saved.userId || verifiedUser.email.isNullOrBlank()) {
                        storage.clear()
                        mutableState.value = AuthState(error = GENERIC_AUTH_ERROR)
                    } else {
                        val verified = saved.withEmail(verifiedUser.email)
                        storage.write(verified)
                        mutableState.value = AuthState(session = verified)
                    }
                } catch (exception: AuthHttpException) {
                    if (exception.statusCode == 401 && saved.expiresAtEpochSeconds > nowSeconds() + EXPIRY_SKEW_SECONDS) {
                        saved = refreshSession(saved.refreshToken, saved)
                        coroutineContext.ensureActive()
                        val verifiedUser = fetchUser(saved.accessToken)
                        coroutineContext.ensureActive()
                        if (verifiedUser.id == saved.userId && !verifiedUser.email.isNullOrBlank()) {
                            val verified = saved.withEmail(verifiedUser.email)
                            storage.write(verified)
                            mutableState.value = AuthState(session = verified)
                        } else {
                            storage.clear()
                            mutableState.value = AuthState(error = GENERIC_AUTH_ERROR)
                        }
                    } else {
                        throw exception
                    }
                }
            }
        } catch (exception: CancellationException) {
            mutableState.value = AuthState(session = previouslyVerified)
            throw exception
        } catch (exception: AuthHttpException) {
            val invalid = exception.statusCode == 401 || exception.statusCode == 403
            if (invalid) storage.clear()
            mutableState.value = AuthState(session = previouslyVerified.takeUnless { invalid }, error = exception.safeMessage)
        } catch (_: IOException) {
            mutableState.value = AuthState(session = previouslyVerified, error = CONNECTION_ERROR)
        } catch (_: ConfigurationException) {
            mutableState.value = AuthState(session = previouslyVerified, error = CONFIGURATION_ERROR)
        } catch (_: Exception) {
            mutableState.value = AuthState(session = previouslyVerified, error = GENERIC_AUTH_ERROR)
        }
    }

    /**
     * Refreshes the access token shortly before it expires so long-lived screens keep a valid
     * bearer token. No-op when signed out or the token is still comfortably valid. A transient
     * network failure keeps the current session; a rejected refresh token signs the user out.
     */
    suspend fun refreshIfExpiring() = mutex.withLock {
        val current = mutableState.value.session ?: return@withLock
        if (current.expiresAtEpochSeconds > nowSeconds() + REFRESH_AHEAD_SECONDS) return@withLock
        try {
            val refreshed = withContext(Dispatchers.IO) {
                requireConfiguration()
                refreshSession(current.refreshToken, current).also { storage.write(it) }
            }
            mutableState.value = AuthState(session = refreshed)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: AuthHttpException) {
            if (exception.statusCode == 400 || exception.statusCode == 401 || exception.statusCode == 403) {
                withContext(NonCancellable + Dispatchers.IO) { storage.clear() }
                mutableState.value = AuthState(error = exception.safeMessage)
            }
        } catch (_: Exception) {
            // Keep the existing session; the next tick retries.
        }
    }

    suspend fun signOut() = mutex.withLock {
        val verifiedSession = mutableState.value.session
        mutableState.value = AuthState(session = verifiedSession, isLoading = true)
        var cancellation: CancellationException? = null
        try {
            val current = verifiedSession ?: withContext(Dispatchers.IO) { storage.read() }
            if (current != null && configurationReady()) {
                withContext(Dispatchers.IO) { request("POST", "/auth/v1/logout?scope=local", null, current.accessToken) }
            }
        } catch (exception: CancellationException) {
            cancellation = exception
        } catch (_: Exception) {
            // Local sign-out takes effect even when the server cannot be reached.
        } finally {
            try {
                withContext(NonCancellable + Dispatchers.IO) { storage.clear() }
            } finally {
                mutableState.value = AuthState()
            }
        }
        cancellation?.let { throw it }
    }

    private suspend fun authenticate(request: suspend AuthRepository.() -> JSONObject) = mutex.withLock {
        mutableState.value = AuthState(isLoading = true)
        try {
            withContext(Dispatchers.IO) {
                requireConfiguration()
                val response = request()
                coroutineContext.ensureActive()
                val session = AuthSession.fromAuthResponse(response)
                if (session == null) {
                    storage.clear()
                    mutableState.value = AuthState(info = EMAIL_CONFIRMATION_INFO)
                } else {
                    val userJson = response.optJSONObject("user")
                    val user = if (userJson != null) {
                        AuthUser(userJson.optString("id"), userJson.optString("email").takeIf { it.isNotBlank() })
                    } else {
                        fetchUser(session.accessToken)
                    }
                    coroutineContext.ensureActive()
                    val email = user.email?.takeIf { it.isNotBlank() }
                    val userId = user.id.takeIf { it.isNotBlank() }
                    if (email == null || userId == null) {
                        storage.clear()
                        mutableState.value = AuthState(error = GENERIC_AUTH_ERROR)
                    } else {
                        val authenticated = session.withIdentity(email, userId)
                        storage.write(authenticated)
                        mutableState.value = AuthState(session = authenticated)
                    }
                }
            }
        } catch (exception: CancellationException) {
            mutableState.value = AuthState()
            throw exception
        } catch (exception: AuthHttpException) {
            mutableState.value = AuthState(error = exception.safeMessage)
        } catch (_: IOException) {
            mutableState.value = AuthState(error = CONNECTION_ERROR)
        } catch (_: ConfigurationException) {
            mutableState.value = AuthState(error = CONFIGURATION_ERROR)
        } catch (_: Exception) {
            mutableState.value = AuthState(error = GENERIC_AUTH_ERROR)
        }
    }

    private fun refreshSession(refreshToken: String, previous: AuthSession): AuthSession {
        val response = post("/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refreshToken))
        return (AuthSession.fromAuthResponse(response) ?: throw AuthHttpException(401, GENERIC_AUTH_ERROR))
            .withIdentity(previous.email, previous.userId)
    }

    private fun fetchUser(accessToken: String): AuthUser {
        val json = request("GET", "/auth/v1/user", null, accessToken)
        return AuthUser(json.optString("id"), json.optString("email").takeIf { it.isNotBlank() })
    }

    private fun post(path: String, body: JSONObject): JSONObject = request("POST", path, body)

    private fun request(method: String, path: String, body: JSONObject?, accessToken: String? = null): JSONObject {
        requireConfiguration()
        val url = supabaseUrl.trimEnd('/') + path
        val builder = Request.Builder().url(url)
            .header("apikey", anonKey)
            .header("Accept", "application/json")
        if (accessToken != null) builder.header("Authorization", "Bearer $accessToken")
        val requestBody = body?.toString()?.toRequestBody(JSON_MEDIA_TYPE)
        when (method) {
            "POST" -> builder.post(requestBody ?: EMPTY_BODY)
            "GET" -> builder.get()
            else -> throw IllegalArgumentException("Unsupported auth method")
        }
        client.newCall(builder.build()).execute().use { response ->
            val content = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw AuthHttpException(response.code, safeErrorMessage(response.code, content))
            if (content.isBlank()) return JSONObject()
            return try {
                JSONObject(content)
            } catch (_: JSONException) {
                throw AuthHttpException(response.code, GENERIC_AUTH_ERROR)
            }
        }
    }

    private fun requireConfiguration() {
        val parsed = supabaseUrl.toHttpUrlOrNull()
        if (parsed == null || parsed.scheme != "https" || parsed.host.isBlank() || anonKey.isBlank()) throw ConfigurationException()
    }

    private fun configurationReady(): Boolean = supabaseUrl.toHttpUrlOrNull()?.let { it.scheme == "https" && it.host.isNotBlank() } == true && anonKey.isNotBlank()

    private fun safeErrorMessage(statusCode: Int, content: String): String {
        val payload = runCatching { JSONObject(content) }.getOrNull()
        val code = payload?.optString("code").orEmpty().lowercase()
        val message = payload?.optString("msg")?.lowercase().orEmpty() + " " + payload?.optString("message")?.lowercase().orEmpty()
        return when {
            code == "email_not_confirmed" || message.contains("email not confirmed") -> EMAIL_CONFIRMATION_REQUIRED
            code == "user_already_exists" || message.contains("already registered") -> ACCOUNT_EXISTS_ERROR
            statusCode == 400 || statusCode == 401 -> INVALID_CREDENTIALS_ERROR
            statusCode == 429 -> RATE_LIMIT_ERROR
            statusCode in 500..599 -> SERVICE_ERROR
            else -> GENERIC_AUTH_ERROR
        }
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1_000L

    private data class AuthUser(val id: String, val email: String?)

    private class AuthHttpException(val statusCode: Int, val safeMessage: String) : Exception()
    private class ConfigurationException : Exception()

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val EMPTY_BODY = "".toRequestBody(JSON_MEDIA_TYPE)
        const val EXPIRY_SKEW_SECONDS = 60L
        const val REFRESH_AHEAD_SECONDS = 300L
        const val CONFIGURATION_ERROR = "Sign-in is not configured on this build."
        const val GENERIC_AUTH_ERROR = "We couldn't complete sign-in. Please try again."
        const val INVALID_CREDENTIALS_ERROR = "Email or password was not accepted."
        const val EMAIL_CONFIRMATION_REQUIRED = "Confirm your email address, then try signing in."
        const val EMAIL_CONFIRMATION_INFO = "Check your email for a confirmation link before signing in."
        const val ACCOUNT_EXISTS_ERROR = "An account with this email already exists. Try signing in."
        const val CONNECTION_ERROR = "Couldn't reach the sign-in service. Check your connection and try again."
        const val RATE_LIMIT_ERROR = "Too many attempts. Wait a moment and try again."
        const val SERVICE_ERROR = "The sign-in service is temporarily unavailable. Try again later."
    }
}

data class AuthState(
    val session: AuthSession? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

class AuthSession internal constructor(
    internal val accessToken: String,
    internal val refreshToken: String,
    internal val expiresAtEpochSeconds: Long,
    val email: String,
    internal val userId: String,
) {
    internal fun withEmail(value: String): AuthSession = AuthSession(accessToken, refreshToken, expiresAtEpochSeconds, value, userId)
    internal fun withIdentity(email: String, userId: String): AuthSession = AuthSession(accessToken, refreshToken, expiresAtEpochSeconds, email, userId)

    internal fun toJson(): String = JSONObject()
        .put("access_token", accessToken)
        .put("refresh_token", refreshToken)
        .put("expires_at", expiresAtEpochSeconds)
        .put("email", email)
        .put("user_id", userId)
        .toString()

    companion object {
        internal fun fromAuthResponse(response: JSONObject): AuthSession? {
            val user = response.optJSONObject("user")
            return parseAuthResponseFields(
                accessToken = response.optString("access_token").takeIf { it.isNotBlank() },
                refreshToken = response.optString("refresh_token").takeIf { it.isNotBlank() },
                expiresAtEpochSeconds = response.optLong("expires_at").takeIf { it > 0 },
                expiresInSeconds = response.optLong("expires_in", 3600L),
                email = user?.optString("email").orEmpty(),
                userId = user?.optString("id").orEmpty(),
            )
        }

        internal fun fromJson(json: JSONObject): AuthSession = AuthSession(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            expiresAtEpochSeconds = json.getLong("expires_at"),
            email = json.optString("email"),
            userId = json.getString("user_id"),
        )
    }
}

internal fun parseAuthResponseFields(
    accessToken: String?,
    refreshToken: String?,
    expiresAtEpochSeconds: Long?,
    expiresInSeconds: Long,
    email: String,
    userId: String,
    nowEpochSeconds: Long = System.currentTimeMillis() / 1_000L,
): AuthSession? {
    if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) return null
    val expiresAt = expiresAtEpochSeconds?.takeIf { it > 0 } ?: (nowEpochSeconds + expiresInSeconds)
    return AuthSession(accessToken, refreshToken, expiresAt, email, userId)
}
