package com.taurotechnologies.godseye.nativeapp.data

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Metadata for a tracked Earth satellite.
 */
@kotlinx.serialization.Serializable
data class SatelliteObservation(
    val noradId: String,
    val name: String,
    val type: String,
    val country: String,
    val altitudeKm: Double,
    val velocityKmPerSec: Double,
    val inclinationDeg: Double,
    val periodMinutes: Double,
    val initialPhaseDeg: Double = 0.0,
    val initialEpochSeconds: Long = 1_793_980_000L,
)

/**
 * Computes instantaneous ground position (latitude, longitude) of a satellite along its orbit
 * at [nowEpochSeconds]. Ported from the orbital propagation models in GodsEye's satellite engine.
 */
object SatelliteMotion {
    data class Position(val lat: Double, val lng: Double, val altKm: Double)

    fun position(sat: SatelliteObservation, nowEpochSeconds: Double): Position {
        val periodSec = (sat.periodMinutes * 60.0).coerceAtLeast(60.0)
        val n = (2.0 * Math.PI) / periodSec // mean motion in rad/s
        val elapsed = nowEpochSeconds - sat.initialEpochSeconds
        val meanAnomalyRad = Math.toRadians(sat.initialPhaseDeg) + (n * elapsed)
        val incRad = Math.toRadians(sat.inclinationDeg)

        // Sub-satellite latitude: sin(lat) = sin(inc) * sin(M)
        val sinLat = (sin(incRad) * sin(meanAnomalyRad)).coerceIn(-1.0, 1.0)
        val latDeg = Math.toDegrees(asin(sinLat))

        // In-plane right ascension: tan(alpha) = cos(inc) * sin(M) / cos(M)
        val inPlaneRad = atan2(cos(incRad) * sin(meanAnomalyRad), cos(meanAnomalyRad))
        val inPlaneDeg = Math.toDegrees(inPlaneRad)

        // Earth rotation westward: 360° per sidereal/solar day (86164.1 s / 86400 s)
        val earthRotDeg = ((elapsed * 360.0) / 86164.1) % 360.0

        val rawLng = sat.initialPhaseDeg + inPlaneDeg - earthRotDeg
        val normLng = ((rawLng % 360.0) + 540.0) % 360.0 - 180.0

        return Position(lat = latDeg, lng = normLng, altKm = sat.altitudeKm)
    }
}
