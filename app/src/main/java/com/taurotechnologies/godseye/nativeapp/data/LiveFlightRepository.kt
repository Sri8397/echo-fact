package com.taurotechnologies.godseye.nativeapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Stages in the flight loading pipeline:
 * - [SESSION]: token exchange at `/godseye/session`
 * - [COOKIE]: host cookie verification
 * - [FLIGHTS]: live flight fetch at `/api/flights`
 * - [PARSE]: payload validation and mapping
 * - [NETWORK]: network/socket connectivity
 */
enum class FlightStep(val label: String) {
    SESSION("session"),
    COOKIE("cookie"),
    FLIGHTS("fetch"),
    PARSE("parse"),
    NETWORK("network"),
}

/**
 * Structured exception for flight failures preserving the failing stage, user-facing banner
 * message, and technical detail for diagnostics.
 */
sealed class FlightException(
    val step: FlightStep,
    val displayMessage: String,
    val detailMessage: String,
    cause: Throwable? = null,
) : IOException(detailMessage, cause) {

    class Timeout(
        step: FlightStep,
        cause: Throwable? = null,
    ) : FlightException(
        step = step,
        displayMessage = when (step) {
            FlightStep.SESSION -> "Flights session timed out (25s limit). Retry to reconnect."
            else -> "Live flights request timed out (25s limit). Retry to reconnect."
        },
        detailMessage = "Flights ${step.label} timed out after 25s limit",
        cause = cause,
    )

    class HttpError(
        step: FlightStep,
        val statusCode: Int,
        val retryAfterSeconds: String? = null,
        cause: Throwable? = null,
    ) : FlightException(
        step = step,
        displayMessage = when {
            statusCode == 401 || statusCode == 403 -> when (step) {
                FlightStep.SESSION -> "Flight session expired (HTTP $statusCode). Sign in again."
                else -> "Flight session unauthorized (HTTP $statusCode). Sign in again."
            }
            statusCode == 429 -> if (!retryAfterSeconds.isNullOrBlank()) {
                "Flights rate limited (HTTP 429). Retry after ${retryAfterSeconds}s."
            } else {
                "Flights rate limited (HTTP 429). Retry later."
            }
            statusCode in 500..599 -> "Flights upstream error (HTTP $statusCode). Retry to reconnect."
            else -> "Flights ${step.label} error (HTTP $statusCode). Retry to reconnect."
        },
        detailMessage = "Flights ${step.label} failed with HTTP $statusCode" +
            (retryAfterSeconds?.let { " (Retry-After: $it)" } ?: ""),
        cause = cause,
    )

    class MissingCookie(
        cause: Throwable? = null,
    ) : FlightException(
        step = FlightStep.COOKIE,
        displayMessage = "Flight session cookie missing. Retry or sign in again.",
        detailMessage = "GodsEye session endpoint did not return an explorer session cookie",
        cause = cause,
    )

    class ParseError(
        val reason: String,
        cause: Throwable? = null,
    ) : FlightException(
        step = FlightStep.PARSE,
        displayMessage = "Flight data unreadable. Retry to reconnect.",
        detailMessage = "Could not parse live flight data: $reason",
        cause = cause,
    )

    class Network(
        step: FlightStep,
        val reason: String,
        cause: Throwable? = null,
    ) : FlightException(
        step = step,
        displayMessage = "Flights network error (${reason.take(50)}). Retry to reconnect.",
        detailMessage = "Network failure during flights ${step.label}: $reason",
        cause = cause,
    )
}

/** Live civil-flight access through the GodsEye short-lived explorer session. */
object LiveFlightRepository {
    private const val TAG = "GodsEyeFlights"
    private const val BASE_URL = "https://explorer.gods-eye.app"
    private const val SESSION_URL = "$BASE_URL/godseye/session"
    private const val FLIGHTS_URL = "$BASE_URL/api/flights"
    private const val USER_AGENT = "GodsEyeNative/1.0 (https://www.worldmonitor.app)"
    private const val MAX_SIGNALS = 500
    private val json = Json { ignoreUnknownKeys = true }
    private val requestMediaType = "application/json; charset=utf-8".toMediaType()

    private fun logFlightError(error: FlightException) {
        try {
            android.util.Log.w(TAG, "[${error.step.label}] ${error.detailMessage}", error.cause ?: error)
        } catch (_: RuntimeException) {
            // Safe for JVM unit tests where android.util.Log may not be mocked
        }
    }

    private fun isTimeout(e: Throwable): Boolean =
        e is java.net.SocketTimeoutException || e is java.io.InterruptedIOException

    /**
     * Exchanges the caller's in-memory Supabase JWT for the short-lived host session cookie,
     * then fetches flights. Each invocation has an isolated, memory-only cookie jar.
     */
    suspend fun loadFlights(accessToken: String): List<RadarSignal> = withContext(Dispatchers.IO) {
        require(accessToken.isNotBlank()) { "Sign in to load live flights" }
        val cookieJar = SessionCookieJar()
        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        try {
            val sessionBody = JsonObject(mapOf("accessToken" to JsonPrimitive(accessToken))).toString()
            val sessionRequest = Request.Builder()
                .url(SESSION_URL)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .post(sessionBody.toRequestBody(requestMediaType))
                .build()

            val sessionResponse = try {
                client.newCall(sessionRequest).execute()
            } catch (error: IOException) {
                val exception = if (isTimeout(error)) {
                    FlightException.Timeout(FlightStep.SESSION, error)
                } else {
                    FlightException.Network(FlightStep.SESSION, error.message ?: "connection failure", error)
                }
                logFlightError(exception)
                throw exception
            }

            sessionResponse.use { response ->
                if (!response.isSuccessful) {
                    val exception = FlightException.HttpError(
                        step = FlightStep.SESSION,
                        statusCode = response.code,
                        retryAfterSeconds = response.header("Retry-After"),
                    )
                    logFlightError(exception)
                    throw exception
                }
                if (cookieJar.sessionCookieCount == 0) {
                    val exception = FlightException.MissingCookie()
                    logFlightError(exception)
                    throw exception
                }
            }

            val flightsRequest = Request.Builder()
                .url(FLIGHTS_URL)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()

            val flightsResponse = try {
                client.newCall(flightsRequest).execute()
            } catch (error: IOException) {
                val exception = if (isTimeout(error)) {
                    FlightException.Timeout(FlightStep.FLIGHTS, error)
                } else {
                    FlightException.Network(FlightStep.FLIGHTS, error.message ?: "connection failure", error)
                }
                logFlightError(exception)
                throw exception
            }

            val (body, provider, coverage) = flightsResponse.use { response ->
                if (!response.isSuccessful) {
                    val exception = FlightException.HttpError(
                        step = FlightStep.FLIGHTS,
                        statusCode = response.code,
                        retryAfterSeconds = response.header("Retry-After"),
                    )
                    logFlightError(exception)
                    throw exception
                }
                val rawBody = response.body.string()
                if (rawBody.isNullOrBlank()) {
                    val exception = FlightException.ParseError("Live flights returned an empty response")
                    logFlightError(exception)
                    throw exception
                }
                val provider = response.header("X-Flight-Source")
                    ?: response.header("X-Feed-Source")
                    ?: "Flight data provider"
                val coverage = response.header("X-Flight-Coverage") ?: "Coverage not reported"
                Triple(rawBody, provider, coverage)
            }

            try {
                parseFlights(body, provider, coverage)
            } catch (error: IllegalArgumentException) {
                val exception = FlightException.ParseError(error.message ?: "malformed flight data", error)
                logFlightError(exception)
                throw exception
            }
        } finally {
            cookieJar.clear()
        }
    }

    /** Pure parser for OpenSky-like `{time, states}` snapshots. */
    fun parseFlights(
        jsonText: String,
        provider: String,
        coverage: String,
        nowEpochSeconds: Long = Instant.now().epochSecond,
    ): List<RadarSignal> {
        val root = try {
            json.parseToJsonElement(jsonText) as? JsonObject
                ?: throw IllegalArgumentException("response root must be a JSON object")
        } catch (error: SerializationException) {
            throw IllegalArgumentException("response is not valid JSON", error)
        } catch (error: IllegalStateException) {
            throw IllegalArgumentException("response root must be a JSON object", error)
        }
        val snapshotTime = root["time"].asLong()?.takeIf { it >= 0L }
        val states = when (val value = root["states"]) {
            null -> throw IllegalArgumentException("response is missing a states array")
            JsonNull -> return emptyList()
            is JsonArray -> value
            else -> throw IllegalArgumentException("response states field must be an array or null")
        }
        val snapshotStale = FlightSnapshot.freshness(snapshotTime, nowEpochSeconds) ==
            FlightSnapshot.Freshness.STALE
        val cleanProvider = provider.trim().take(120).ifEmpty { "Flight data provider" }
        val cleanCoverage = coverage.trim().take(240).ifEmpty { "Coverage not reported" }
        val validFlights = states.mapNotNull { state ->
            (state as? JsonArray)?.let(OpenSkyStateVectorNormalizer::normalize)
        }
        val sampleLimited = validFlights.size > MAX_SIGNALS
        val output = ArrayList<RadarSignal>(minOf(validFlights.size, MAX_SIGNALS))

        for (flight in validFlights.take(MAX_SIGNALS)) {
            val observedTime = flight.observedAtEpochSeconds
            val callsign = flight.callsign ?: flight.id.uppercase(Locale.ROOT)
            val positionFreshness = FlightSnapshot.freshness(flight.observedAtEpochSeconds, nowEpochSeconds)
            val summary = buildList {
                add("Position ${flight.lat.formatCoordinate()}, ${flight.lng.formatCoordinate()}.")
                flight.altitudeMeters?.let { add("Altitude ${it.formatNumber()} m.") }
                flight.speedMetersPerSecond?.let { add("Speed ${it.formatNumber()} m/s.") }
                flight.headingDegrees?.let { add("Heading ${it.formatNumber()}°.") }
                flight.onGround?.let { add(if (it) "On ground." else "Airborne.") }
                if (snapshotStale) add("Snapshot is stale.")
                when (positionFreshness) {
                    FlightSnapshot.Freshness.STALE -> add("Position is stale.")
                    FlightSnapshot.Freshness.UNKNOWN -> add(
                        if (snapshotTime == null) "Position time unavailable."
                        else "Position time unavailable; snapshot time shown: ${formatTimestamp(snapshotTime)}.",
                    )
                    FlightSnapshot.Freshness.CURRENT -> Unit
                }
            }.joinToString(" ")
            val timestamp = observedTime?.let(::formatTimestamp) ?: "Unknown"
            output += RadarSignal(
                id = "flight:${flight.id}",
                layer = RadarLayer.FLIGHTS,
                title = "Flight $callsign",
                locationName = "${flight.lat.formatCoordinate()}, ${flight.lng.formatCoordinate()}",
                lat = flight.lat,
                lng = flight.lng,
                observedAt = timestamp,
                severity = if (flight.onGround == true) "On ground" else "Flight",
                summary = summary,
                flight = flight,
                provider = cleanProvider,
                coverage = buildString {
                    append(cleanCoverage)
                    if (snapshotStale) append(" · stale snapshot")
                    if (sampleLimited) append(" · limited to first $MAX_SIGNALS records")
                },
                isSimulated = false,
                eventSourceUrl = null,
            )
        }
        return output
    }

    private fun formatTimestamp(epochSeconds: Long): String = try {
        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm 'UTC'", Locale.ROOT)
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochSecond(epochSeconds))
    } catch (_: Exception) {
        "Unknown"
    }

    private fun Double.formatNumber(): String = String.format(Locale.ROOT, "%.0f", this)
    private fun Double.formatCoordinate(): String = String.format(Locale.ROOT, "%.2f°", this)

    private fun kotlinx.serialization.json.JsonElement?.asLong(): Long? =
        (this as? JsonPrimitive)?.longOrNull

    /** Cookie is held only by this load call's client and only the host session is retained. */
    private class SessionCookieJar : CookieJar {
        private var cookies: List<Cookie> = emptyList()
        val sessionCookieCount: Int get() = cookies.count { it.name == "__Host-godseye-explorer" }
        fun clear() { cookies = emptyList() }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies = cookies.filter {
                it.name == "__Host-godseye-explorer" && it.secure &&
                    it.domain == "explorer.gods-eye.app" && it.path == "/"
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            cookies.filter { it.matches(url) }
    }
}
