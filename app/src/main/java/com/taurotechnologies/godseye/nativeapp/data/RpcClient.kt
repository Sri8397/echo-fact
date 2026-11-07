package com.taurotechnologies.godseye.nativeapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Credentials a call carries. [Public] needs none; the server only accepts shapes it marks public. */
sealed interface RpcAuth {
    /** Public host, no credentials. */
    data object Public : RpcAuth

    /**
     * Signed-in explorer host. The Supabase [accessToken] is exchanged at `/godseye/session` for a
     * short-lived cookie that lives in a memory-only jar for this one call.
     */
    class Session(internal val accessToken: String) : RpcAuth {
        init {
            require(accessToken.isNotBlank()) { "Sign in to call this endpoint" }
        }

        override fun toString(): String = "RpcAuth.Session"
    }
}

/**
 * A sebuf route `<domain>/v1/<method>` plus its query parameters, for example
 * `prediction/v1/list-prediction-markets` with `public=1`. Maps to `/api/<path>`.
 */
data class RpcEndpoint(val path: String, val query: Map<String, String> = emptyMap()) {
    init {
        require(PATH.matches(path)) { "Invalid RPC path" }
        require(query.all { (key, value) -> KEY.matches(key) && VALUE.matches(value) }) { "Invalid RPC query" }
    }

    companion object {
        private val PATH = Regex("[a-z][a-z0-9-]*/v1/[a-z][a-z0-9-]*")
        private val KEY = Regex("[a-z][a-z0-9_]{0,40}")
        private val VALUE = Regex("[A-Za-z0-9_.,:-]{0,200}")

        /**
         * Parses `domain/v1/method` with an optional `?key=value&...` tail. Null when the text is not a
         * valid endpoint, so a catalog entry with a bad path is ignored instead of fetching anything else.
         */
        fun parse(text: String): RpcEndpoint? {
            val path = text.substringBefore('?')
            val queryText = text.substringAfter('?', missingDelimiterValue = "")
            val query = LinkedHashMap<String, String>()
            if (queryText.isNotEmpty()) {
                for (pair in queryText.split('&')) {
                    val key = pair.substringBefore('=', missingDelimiterValue = "")
                    if (!pair.contains('=') || key in query) return null
                    query[key] = pair.substringAfter('=')
                }
            }
            return try {
                RpcEndpoint(path, query)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}

/** Failure from an RPC call. Mirrors `FlightException`: a user-facing message plus a technical detail. */
sealed class RpcException(
    val displayMessage: String,
    val detailMessage: String,
    cause: Throwable? = null,
) : IOException(detailMessage, cause) {

    class Timeout(stage: RpcStage, path: String, timeoutSeconds: Long, cause: Throwable? = null) : RpcException(
        displayMessage = "GodsEye request timed out (${timeoutSeconds}s limit). Retry.",
        detailMessage = "RPC ${stage.label} for $path timed out after ${timeoutSeconds}s",
        cause = cause,
    )

    class HttpError(
        stage: RpcStage,
        path: String,
        val statusCode: Int,
        val retryAfterSeconds: String? = null,
    ) : RpcException(
        displayMessage = when (statusCode) {
            401, 403 -> "Sign in required for this data (HTTP $statusCode)."
            429 -> if (!retryAfterSeconds.isNullOrBlank()) {
                "Rate limited (HTTP 429). Retry after ${retryAfterSeconds}s."
            } else {
                "Rate limited (HTTP 429). Retry later."
            }
            in 500..599 -> "GodsEye upstream error (HTTP $statusCode). Retry."
            else -> "GodsEye request failed (HTTP $statusCode)."
        },
        detailMessage = "RPC ${stage.label} for $path failed with HTTP $statusCode" +
            (retryAfterSeconds?.let { " (Retry-After: $it)" } ?: ""),
    )

    class MissingSessionCookie(path: String) : RpcException(
        displayMessage = "Session cookie missing. Sign in again.",
        detailMessage = "GodsEye session endpoint did not return an explorer cookie for $path",
    )

    class ParseError(path: String, reason: String, cause: Throwable? = null) : RpcException(
        displayMessage = "Data unreadable. Retry.",
        detailMessage = "Could not parse RPC response for $path: $reason",
        cause = cause,
    )

    class Network(stage: RpcStage, path: String, reason: String, cause: Throwable? = null) : RpcException(
        displayMessage = "Network error (${reason.take(50)}). Retry.",
        detailMessage = "Network failure during RPC ${stage.label} for $path: $reason",
        cause = cause,
    )
}

/** Stage of a call that failed: the session exchange or the data fetch. */
enum class RpcStage(val label: String) { SESSION("session"), FETCH("fetch") }

/**
 * Shared client for GodsEye's `/api/<domain>/v1/<method>` sebuf endpoints. Calls are cancellable (the
 * OkHttp call is cancelled with the coroutine) and time out after [timeoutSeconds] per call. Responses
 * are returned as [JsonElement] or decoded with a [DeserializationStrategy]. Caching is up to the caller.
 */
class RpcClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build(),
    private val publicBaseUrl: String = "https://gods-eye.app",
    private val sessionBaseUrl: String = "https://explorer.gods-eye.app",
    private val timeoutSeconds: Long = 25,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** Calls [endpoint] and returns the JSON body. Throws [RpcException] on failure, or [kotlinx.coroutines.CancellationException]. */
    suspend fun getJson(endpoint: RpcEndpoint, auth: RpcAuth = RpcAuth.Public): JsonElement = withContext(Dispatchers.IO) {
        val callClient = http.newBuilder().callTimeout(timeoutSeconds, TimeUnit.SECONDS).build()
        when (auth) {
            RpcAuth.Public -> {
                val url = dataUrl(publicBaseUrl, endpoint)
                fetch(callClient, url, endpoint)
            }
            is RpcAuth.Session -> {
                val jar = SessionCookieJar()
                val sessionClient = callClient.newBuilder().cookieJar(jar).build()
                try {
                    exchangeSession(sessionClient, jar, auth, endpoint)
                    fetch(sessionClient, dataUrl(sessionBaseUrl, endpoint), endpoint)
                } finally {
                    jar.clear()
                }
            }
        }
    }

    /** Calls [endpoint] and decodes the body. A body that does not match [deserializer] is a [RpcException.ParseError]. */
    suspend fun <T> get(
        endpoint: RpcEndpoint,
        deserializer: DeserializationStrategy<T>,
        auth: RpcAuth = RpcAuth.Public,
    ): T {
        val element = getJson(endpoint, auth)
        return try {
            json.decodeFromJsonElement(deserializer, element)
        } catch (error: SerializationException) {
            throw RpcException.ParseError(endpoint.path, error.message ?: "unexpected shape", error)
        } catch (error: IllegalArgumentException) {
            throw RpcException.ParseError(endpoint.path, error.message ?: "unexpected shape", error)
        }
    }

    private fun dataUrl(base: String, endpoint: RpcEndpoint): HttpUrl {
        val builder = base.toHttpUrl().newBuilder().addPathSegments("api/${endpoint.path}")
        for ((key, value) in endpoint.query) builder.addQueryParameter(key, value)
        return builder.build()
    }

    private suspend fun exchangeSession(client: OkHttpClient, jar: SessionCookieJar, auth: RpcAuth.Session, endpoint: RpcEndpoint) {
        val body = JsonObject(mapOf("accessToken" to JsonPrimitive(auth.accessToken))).toString()
            .toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$sessionBaseUrl/godseye/session")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .post(body)
            .build()
        send(client, request, RpcStage.SESSION, endpoint.path).use { response ->
            if (!response.isSuccessful) {
                throw RpcException.HttpError(RpcStage.SESSION, endpoint.path, response.code, response.header("Retry-After"))
            }
        }
        if (jar.sessionCookieCount == 0) throw RpcException.MissingSessionCookie(endpoint.path)
    }

    private suspend fun fetch(client: OkHttpClient, url: HttpUrl, endpoint: RpcEndpoint): JsonElement {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()
        return send(client, request, RpcStage.FETCH, endpoint.path).use { response ->
            if (!response.isSuccessful) {
                throw RpcException.HttpError(RpcStage.FETCH, endpoint.path, response.code, response.header("Retry-After"))
            }
            val text = try {
                response.body.string()
            } catch (error: IOException) {
                throw ioFailure(RpcStage.FETCH, endpoint.path, error)
            }
            if (text.isBlank()) throw RpcException.ParseError(endpoint.path, "empty response")
            val root = try {
                json.parseToJsonElement(text)
            } catch (error: SerializationException) {
                throw RpcException.ParseError(endpoint.path, "response is not valid JSON", error)
            } catch (error: IllegalArgumentException) {
                throw RpcException.ParseError(endpoint.path, "response is not valid JSON", error)
            }
            // Sebuf responses are always a JSON object (or array); a bare scalar means the body was not an API response.
            if (root !is JsonObject && root !is JsonArray) throw RpcException.ParseError(endpoint.path, "response is not a JSON object")
            root
        }
    }

    private suspend fun send(client: OkHttpClient, request: Request, stage: RpcStage, path: String): Response =
        try {
            client.newCall(request).await()
        } catch (error: IOException) {
            throw ioFailure(stage, path, error)
        }

    private fun ioFailure(stage: RpcStage, path: String, error: IOException): RpcException =
        if (error is SocketTimeoutException || error is InterruptedIOException) {
            RpcException.Timeout(stage, path, timeoutSeconds, error)
        } else {
            RpcException.Network(stage, path, error.message ?: "connection failure", error)
        }

    /** Runs the call on OkHttp's dispatcher. Cancelling the coroutine cancels the call. */
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { response.close() }
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }

    /** Holds only the explorer session cookie, for one call. Same rules as the flight session jar. */
    private class SessionCookieJar : CookieJar {
        private var cookies: List<Cookie> = emptyList()
        val sessionCookieCount: Int get() = cookies.count { it.name == SESSION_COOKIE }
        fun clear() {
            cookies = emptyList()
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies = cookies.filter {
                it.name == SESSION_COOKIE && it.secure && it.domain == "explorer.gods-eye.app" && it.path == "/"
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.filter { it.matches(url) }
    }

    companion object {
        private const val SESSION_COOKIE = "__Host-godseye-explorer"
        private const val USER_AGENT = "GodsEyeNative/1.0 (https://www.worldmonitor.app)"

        /** Shared instance for app code. Tests build their own with a fake [OkHttpClient]. */
        val shared: RpcClient by lazy { RpcClient() }
    }
}
