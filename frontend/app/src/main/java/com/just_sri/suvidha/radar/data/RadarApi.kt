package com.just_sri.suvidha.radar.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/** A failed request. [message] is always safe to show: backend `detail` text or a fixed fallback. */
class ApiException(val status: Int, message: String) : Exception(message)

const val GENERIC_FAILURE: String = "Something went wrong. Please try again."
const val OFFLINE_FAILURE: String = "Can't reach the server. Try again."
private const val UNAUTHORIZED: Int = 401
private const val TIMEOUT_SECONDS: Long = 30
private const val PIPELINE_STARTED: String = "Multi-Agent Pipeline launched in background!"

/** Keeps the admin token in app-private preferences. */
class TokenStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("suvidha_radar", Context.MODE_PRIVATE)

    fun token(): String? = prefs.getString(KEY_TOKEN, null)

    fun save(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val KEY_TOKEN: String = "admin_token"
    }
}

/** Backend client: same endpoints, headers and JSON keys as the web client. */
class RadarApi(private val baseUrl: String, private val tokens: TokenStore) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val jsonType = "application/json".toMediaType()

    private class HttpResult(val status: Int, val body: String) {
        val isOk: Boolean get() = status in 200..299
    }

    private suspend fun send(path: String, method: String = "GET", body: RequestBody? = null, authed: Boolean = false): HttpResult =
        withContext(Dispatchers.IO) {
            val builder: Request.Builder = Request.Builder().url(baseUrl.trimEnd('/') + path).header("Accept", "application/json")
            if (authed) builder.header("X-Admin-Token", tokens.token() ?: "")
            val request: Request = builder.method(method, body ?: if (method == "GET" || method == "DELETE") null else ByteArray(0).toRequestBody()).build()
            try {
                client.newCall(request).execute().use { response ->
                    HttpResult(response.code, response.body.string())
                }
            } catch (e: IOException) {
                throw ApiException(0, OFFLINE_FAILURE)
            }
        }

    private fun detailOf(body: String): String? = try {
        json.parseToJsonElement(body).jsonObject["detail"]?.jsonPrimitive?.contentOrNull
    } catch (e: Exception) {
        null
    }

    private fun field(body: String, key: String): String? = try {
        json.parseToJsonElement(body).jsonObject[key]?.jsonPrimitive?.contentOrNull
    } catch (e: Exception) {
        null
    }

    private suspend fun authedCall(path: String, method: String, body: RequestBody? = null): HttpResult {
        val result: HttpResult = send(path, method, body, authed = true)
        if (result.status == UNAUTHORIZED) tokens.clear()
        if (!result.isOk) throw ApiException(result.status, detailOf(result.body) ?: GENERIC_FAILURE)
        return result
    }

    /** Public incidents. Falls back to the bundled records when the backend is unreachable or returns none. */
    suspend fun fetchIncidents(): IncidentLoad {
        return try {
            val result: HttpResult = send("/api/incidents")
            if (!result.isOk) return IncidentLoad(FALLBACK_INCIDENTS, isLive = false)
            val list: List<Incident> = json.decodeFromString(result.body)
            if (list.isEmpty()) IncidentLoad(FALLBACK_INCIDENTS, isLive = false) else IncidentLoad(list, isLive = true)
        } catch (e: Exception) {
            IncidentLoad(FALLBACK_INCIDENTS, isLive = false)
        }
    }

    fun isSignedIn(): Boolean = tokens.token() != null

    fun signOut() {
        tokens.clear()
    }

    /** RSA-OAEP (SHA-256, MGF1-SHA-256) like Web Crypto, so the backend decrypts it the same way as the web login. */
    suspend fun login(password: String) {
        val keyResult: HttpResult = send("/api/admin/public-key")
        if (!keyResult.isOk) throw ApiException(keyResult.status, "Failed to fetch RSA security credentials from server.")
        val pem: String = field(keyResult.body, "public_key") ?: throw ApiException(keyResult.status, GENERIC_FAILURE)
        val encrypted: String = withContext(Dispatchers.Default) { encryptPassword(password, pem) }
        val payload: String = JsonObject(mapOf("encrypted_password" to kotlinx.serialization.json.JsonPrimitive(encrypted))).toString()
        val loginResult: HttpResult = send("/api/admin/login", "POST", payload.toRequestBody(jsonType))
        if (!loginResult.isOk) throw ApiException(loginResult.status, detailOf(loginResult.body) ?: "Authentication failed.")
        val token: String = field(loginResult.body, "token") ?: throw ApiException(loginResult.status, GENERIC_FAILURE)
        tokens.save(token)
    }

    suspend fun addIncidentFromUrl(url: String): Incident {
        val payload: String = JsonObject(mapOf("url" to kotlinx.serialization.json.JsonPrimitive(url))).toString()
        val result: HttpResult = authedCall("/api/incidents/from-url", "POST", payload.toRequestBody(jsonType))
        return try {
            json.decodeFromString(result.body)
        } catch (e: Exception) {
            throw ApiException(result.status, GENERIC_FAILURE)
        }
    }

    suspend fun deleteIncident(id: String) {
        val path: String = "/api/incidents/" + java.net.URLEncoder.encode(id, "UTF-8").replace("+", "%20")
        authedCall(path, "DELETE")
    }

    suspend fun runPipeline(prompt: String): String {
        val url: String = (baseUrl.trimEnd('/') + "/api/pipeline/run").toHttpUrl().newBuilder()
            .addQueryParameter("prompt", prompt).build().toString()
        val path: String = url.removePrefix(baseUrl.trimEnd('/'))
        val result: HttpResult = authedCall(path, "POST")
        return field(result.body, "message") ?: PIPELINE_STARTED
    }

    private fun encryptPassword(password: String, pem: String): String {
        val base64Der: String = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .filterNot { it.isWhitespace() }
        val der: ByteArray = Base64.decode(base64Der, Base64.DEFAULT)
        val key: PublicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(der))
        val oaep = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)
        val cipher: Cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, oaep)
        return Base64.encodeToString(cipher.doFinal(password.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
}

/** Incidents plus whether they came from the backend (false = bundled fallback). */
data class IncidentLoad(val incidents: List<Incident>, val isLive: Boolean)
