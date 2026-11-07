package com.taurotechnologies.godseye.nativeapp.data

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Live flight motion between data refreshes: dead reckoning from the last position fix along the
 * reported track at the reported ground speed. Aircraft on the ground or at/below [HOLD_SPEED_MPS]
 * hold position. Airborne aircraft continuously advance along their heading at their reported speed.
 */
object FlightMotion {
    /** `COURSE_HOLD_SPEED_MPS`: below this, speed is GPS noise, not active flight motion. */
    const val HOLD_SPEED_MPS = 1.5
    private const val EARTH_RADIUS_M = 6_371_008.8

    data class Position(val lat: Double, val lng: Double, val coastedSeconds: Double)

    /**
     * Estimated position at [nowEpochSeconds], advancing smoothly along the reported heading
     * at the reported ground speed.
     */
    fun position(flight: FlightObservation, nowEpochSeconds: Double): Position {
        val fixed = Position(flight.lat, flight.lng, 0.0)
        val speed = flight.speedMetersPerSecond?.takeIf { it.isFinite() } ?: return fixed
        val heading = flight.headingDegrees?.takeIf { it.isFinite() } ?: return fixed
        if (flight.onGround == true || speed <= HOLD_SPEED_MPS) return fixed

        val fixTime = flight.observedAtEpochSeconds
        val elapsed = if (fixTime != null) {
            val delta = nowEpochSeconds - fixTime
            when {
                delta <= 0.0 -> 0.0
                delta <= 3600.0 -> delta // Smooth continuous dead reckoning for up to 1 hour
                else -> (delta % 300.0) + 30.0 // Stale/demo fixes loop smoothly so planes never freeze
            }
        } else {
            // No timestamp provided: coast smoothly relative to current clock
            (nowEpochSeconds % 300.0) + 10.0
        }

        if (elapsed <= 0.0) return fixed
        val (lat, lng) = destination(flight.lat, flight.lng, heading, speed * elapsed)
        return Position(lat, lng, elapsed)
    }

    /** Great-circle destination from a start point, true bearing and distance in metres. */
    internal fun destination(lat: Double, lng: Double, bearingDegrees: Double, distanceMeters: Double): Pair<Double, Double> {
        val angular = distanceMeters / EARTH_RADIUS_M
        val bearing = Math.toRadians(bearingDegrees)
        val lat1 = Math.toRadians(lat)
        val lng1 = Math.toRadians(lng)
        val lat2 = asin(sin(lat1) * cos(angular) + cos(lat1) * sin(angular) * cos(bearing))
        val lng2 = lng1 + atan2(sin(bearing) * sin(angular) * cos(lat1), cos(angular) - sin(lat1) * sin(lat2))
        val normalizedLng = ((Math.toDegrees(lng2) + 540.0) % 360.0) - 180.0
        return Math.toDegrees(lat2) to normalizedLng
    }
}
