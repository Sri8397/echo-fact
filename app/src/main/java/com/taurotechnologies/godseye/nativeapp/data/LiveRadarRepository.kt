package com.taurotechnologies.godseye.nativeapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Live USGS M2.5+ past-day earthquake feed. Network failures are surfaced; no demo fallback. */
object LiveRadarRepository {
    private const val FEED_URL =
        "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_day.geojson"
    private const val USER_AGENT = "GodsEyeNative/1.0 (https://www.worldmonitor.app)"
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun loadEarthquakes(): List<RadarSignal> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(FEED_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/geo+json, application/json")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("USGS earthquake feed returned HTTP ${response.code}")
                }
                val body = response.body?.string()
                    ?: throw IOException("USGS earthquake feed returned an empty response")
                try {
                    parseEarthquakes(body)
                } catch (error: IllegalArgumentException) {
                    throw IOException("Could not parse USGS earthquake feed: ${error.message}", error)
                }
            }
        } catch (error: IOException) {
            throw IOException("Unable to load live USGS earthquakes: ${error.message}", error)
        }
    }

    /** Pure parser for the USGS GeoJSON summary response. Invalid features are skipped. */
    fun parseEarthquakes(jsonText: String): List<RadarSignal> {
        val root = try {
            json.parseToJsonElement(jsonText) as? JsonObject
                ?: throw IllegalArgumentException("response is not valid GeoJSON: root must be an object")
        } catch (error: SerializationException) {
            throw IllegalArgumentException("response is not valid GeoJSON", error)
        } catch (error: IllegalStateException) {
            throw IllegalArgumentException("response root must be a GeoJSON object", error)
        }
        val features = when (val value = root["features"]) {
            null -> throw IllegalArgumentException("response is missing a features array")
            JsonNull -> throw IllegalArgumentException("response features field must be an array")
            is JsonArray -> value
            else -> throw IllegalArgumentException("response features field must be an array")
        }
        return features.mapNotNull { it as? JsonObject }
            .mapNotNull(::parseFeature)
    }

    private fun parseFeature(feature: JsonObject): RadarSignal? {
        val properties = feature["properties"] as? JsonObject ?: return null
        val geometry = feature["geometry"] as? JsonObject ?: return null
        if (geometry.string("type") != "Point") return null
        val coordinates = geometry["coordinates"] as? JsonArray ?: return null
        val lng = coordinates.getOrNull(0).number()?.takeIf { it.isFinite() && it in -180.0..180.0 }
            ?: return null
        val lat = coordinates.getOrNull(1).number()?.takeIf { it.isFinite() && it in -90.0..90.0 }
            ?: return null
        val depthKm = coordinates.getOrNull(2).number()?.takeIf(Double::isFinite)
        val eventId = feature.string("id")?.trim()?.takeIf(String::isNotEmpty)
            ?: properties.string("code")?.trim()?.takeIf(String::isNotEmpty)
            ?: return null
        val eventUrl = when (val rawUrl = properties["url"]) {
            null -> null
            JsonNull -> null
            else -> {
                val urlText = (rawUrl as? JsonPrimitive)?.takeIf { it.isString }?.content
                    ?: return null
                validatedEventUrl(urlText) ?: return null
            }
        }

        val magnitude = properties["mag"].number()?.takeIf(Double::isFinite)
        val place = properties.string("place")?.trim()?.takeIf(String::isNotEmpty)
            ?: "Location unavailable"
        val timeMillis = properties["time"].long()
        val observedAt = timeMillis?.let { formatTimestamp(it) } ?: "Unknown"
        val magnitudeLabel = magnitude?.let { "M ${formatNumber(it)}" } ?: "Magnitude unavailable"
        val depthSummary = depthKm?.let { "Depth ${formatNumber(it)} km." } ?: "Depth unavailable."

        return RadarSignal(
            id = "usgs:$eventId",
            layer = RadarLayer.SEISMIC,
            title = "$magnitudeLabel earthquake",
            locationName = place,
            lat = lat,
            lng = lng,
            observedAt = observedAt,
            severity = magnitudeLabel,
            summary = "$magnitudeLabel; $depthSummary",
            provider = "USGS",
            coverage = "M2.5+ earthquakes in the past day",
            isSimulated = false,
            eventSourceUrl = eventUrl,
            magnitude = magnitude,
        )
    }

    private fun validatedEventUrl(raw: String): String? = try {
        val uri = java.net.URI(raw)
        raw.takeIf {
            uri.scheme?.equals("https", ignoreCase = true) == true &&
                uri.host?.equals("earthquake.usgs.gov", ignoreCase = true) == true &&
                uri.userInfo == null && uri.port in listOf(-1, 443)
        }
    } catch (_: Exception) {
        null
    }

    private fun formatTimestamp(epochMillis: Long): String = try {
        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm 'UTC'", Locale.ROOT)
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochMilli(epochMillis))
    } catch (_: Exception) {
        "Unknown"
    }

    private fun formatNumber(value: Double): String =
        String.format(Locale.ROOT, "%.1f", value)

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun kotlinx.serialization.json.JsonElement?.number(): Double? =
        (this as? JsonPrimitive)?.doubleOrNull

    private fun kotlinx.serialization.json.JsonElement?.long(): Long? =
        (this as? JsonPrimitive)?.longOrNull
}
