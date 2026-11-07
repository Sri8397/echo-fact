package com.taurotechnologies.godseye.nativeapp.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant

@kotlinx.serialization.Serializable
data class FlightObservation(
    val id: String,
    val callsign: String?,
    val originCountry: String?,
    val lat: Double,
    val lng: Double,
    val altitudeMeters: Double?,
    val speedMetersPerSecond: Double?,
    val headingDegrees: Double?,
    val onGround: Boolean?,
    val observedAtEpochSeconds: Long?,
    val lastContactEpochSeconds: Long? = null,
    /** GPS (geometric) altitude in metres, OpenSky field 13. Null when the aircraft does not report it. */
    val geoAltitudeMeters: Double? = null,
) {
    /** Best altitude for placing the aircraft in 3D: GPS when reported, else barometric. */
    val bestAltitudeMeters: Double? get() = geoAltitudeMeters ?: altitudeMeters

    /**
     * Approximate height above the ground under the aircraft, given that ground's elevation. Zero on the
     * ground, null when the altitude or the ground is unknown. Barometric altitude drifts with the weather,
     * so this is an estimate, never better than tens of metres.
     */
    fun heightAboveGroundMeters(groundElevationMeters: Double?): Double? {
        if (onGround == true) return 0.0
        val alt = bestAltitudeMeters ?: return null
        val ground = groundElevationMeters?.takeIf(Double::isFinite) ?: return null
        return (alt - ground).coerceAtLeast(0.0)
    }
}

data class FlightSnapshot(
    val flights: List<FlightObservation>,
    val provider: String,
    val coverage: String,
    val observedAtEpochSeconds: Long?,
    val freshness: Freshness,
) {
    enum class Freshness { CURRENT, STALE, UNKNOWN }

    companion object {
        const val STALE_AFTER_SECONDS = 120L

        fun freshness(observedAtEpochSeconds: Long?, nowEpochSeconds: Long): Freshness {
            if (observedAtEpochSeconds == null || observedAtEpochSeconds > nowEpochSeconds) {
                return Freshness.UNKNOWN
            }
            return if (nowEpochSeconds - observedAtEpochSeconds <= STALE_AFTER_SECONDS) {
                Freshness.CURRENT
            } else {
                Freshness.STALE
            }
        }
    }
}

/** Parses one OpenSky state vector, retaining the provider's units. Invalid required fields reject it. */
object OpenSkyStateVectorNormalizer {
    fun normalize(vector: JsonArray): FlightObservation? {
        val id = vector.text(0)?.trim()?.lowercase()
            ?.takeIf { it.matches(Regex("[0-9a-f]{6}")) } ?: return null
        val lat = vector.number(6)?.takeIf { it.isFinite() && it in -90.0..90.0 } ?: return null
        val lng = vector.number(5)?.takeIf { it.isFinite() && it in -180.0..180.0 } ?: return null
        val observedAt = vector.long(3)?.takeIf { it >= 0L }
        val lastContact = vector.long(4)?.takeIf { it >= 0L }

        return FlightObservation(
            id = id,
            callsign = vector.text(1)?.trim()?.takeIf(String::isNotEmpty),
            originCountry = vector.text(2)?.trim()?.takeIf(String::isNotEmpty),
            lat = lat,
            lng = lng,
            altitudeMeters = vector.number(7)?.takeIf(Double::isFinite),
            onGround = vector.boolean(8),
            speedMetersPerSecond = vector.number(9)?.takeIf(Double::isFinite),
            headingDegrees = vector.number(10)?.takeIf { it.isFinite() && it in 0.0..360.0 },
            observedAtEpochSeconds = observedAt,
            lastContactEpochSeconds = lastContact,
            geoAltitudeMeters = vector.number(13)?.takeIf(Double::isFinite),
        )
    }

    private fun JsonArray.primitive(index: Int): JsonPrimitive? =
        getOrNull(index) as? JsonPrimitive

    private fun JsonArray.text(index: Int): String? = primitive(index)?.takeIf { it.isString }?.contentOrNull
    private fun JsonArray.number(index: Int): Double? = primitive(index)?.doubleOrNull
    private fun JsonArray.long(index: Int): Long? = primitive(index)?.longOrNull
    private fun JsonArray.boolean(index: Int): Boolean? = primitive(index)?.booleanOrNull
}

/** Fixed synthetic data for the first native UI increment. This is never live radar data. */
object MockFlightRepository {
    private const val DEMO_TIME = 1_793_961_000L // 2026-11-06T10:30:00Z
    private val demoVectors = listOf(
        """["a1b2c3","DEMO101 ","Demo Republic",${DEMO_TIME},${DEMO_TIME},-73.78,40.64,10668.0,false,235.0,92.0]""",
        """["d4e5f6","DEMO202 ","Example Federation",${DEMO_TIME},${DEMO_TIME},2.55,49.01,9144.0,false,218.0,270.0]""",
        """["0a1b2c","DEMO303 ","Fictional Union",${DEMO_TIME},${DEMO_TIME},139.78,35.55,null,false,null,44.0]""",
    )

    fun snapshot(nowEpochSeconds: Long = Instant.now().epochSecond): FlightSnapshot {
        val flights = demoVectors.mapNotNull {
            OpenSkyStateVectorNormalizer.normalize(
                kotlinx.serialization.json.Json.parseToJsonElement(it) as JsonArray,
            )
        }
        return FlightSnapshot(
            flights = flights,
            provider = "Synthetic demo fixtures (not live data)",
            coverage = "Three fictional aircraft for UI preview",
            observedAtEpochSeconds = DEMO_TIME,
            freshness = FlightSnapshot.freshness(DEMO_TIME, nowEpochSeconds),
        )
    }
}
