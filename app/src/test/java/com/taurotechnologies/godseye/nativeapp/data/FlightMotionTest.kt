package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos

class FlightMotionTest {
    private fun flight(
        speed: Double? = 250.0,
        heading: Double? = 90.0,
        onGround: Boolean? = false,
        fix: Long? = 1_000L,
        contact: Long? = 1_000L,
        lat: Double = 0.0,
        lng: Double = 0.0,
    ) = FlightObservation("abc123", "TEST1", null, lat, lng, 10_000.0, speed, heading, onGround, fix, contact)

    @Test
    fun coastsAlongHeadingAtReportedSpeed() {
        // 250 m/s east for 60 s on the equator = 15 km = 0.1349 degrees of longitude.
        val moved = FlightMotion.position(flight(), 1_060.0)
        assertEquals(0.0, moved.lat, 1e-9)
        assertEquals(15_000.0 / 111_195.08, moved.lng, 1e-4)
        assertEquals(60.0, moved.coastedSeconds, 0.0)
    }

    @Test
    fun northboundMovesLatitudeOnly() {
        val moved = FlightMotion.position(flight(heading = 0.0, lat = 40.0, lng = -74.0), 1_030.0)
        assertTrue(moved.lat > 40.0)
        assertEquals(-74.0, moved.lng, 1e-9)
    }

    @Test
    fun groundSlowOrIncompleteAircraftHoldTheirFix() {
        for (held in listOf(
            flight(onGround = true), flight(speed = 1.5), flight(speed = null),
            flight(heading = null), flight(speed = Double.NaN),
        )) {
            val position = FlightMotion.position(held, 1_100.0)
            assertEquals(held.lat, position.lat, 0.0)
            assertEquals(held.lng, position.lng, 0.0)
            assertEquals(0.0, position.coastedSeconds, 0.0)
        }
    }

    @Test
    fun clockBeforeTheFixDoesNotMoveBackwards() {
        assertEquals(0.0, FlightMotion.position(flight(), 900.0).coastedSeconds, 0.0)
    }

    @Test
    fun continuouslyAdvancesOverTimeWithoutFreezing() {
        val f = flight()
        val pos1 = FlightMotion.position(f, 1_010.0)
        val pos2 = FlightMotion.position(f, 1_020.0)
        val pos3 = FlightMotion.position(f, 1_030.0)
        assertTrue("Position must continuously advance", pos2.lng > pos1.lng)
        assertTrue("Position must continuously advance", pos3.lng > pos2.lng)
    }

    @Test
    fun longitudeWrapsAcrossTheAntimeridian() {
        val moved = FlightMotion.position(flight(lng = 179.99, heading = 90.0), 1_060.0)
        assertTrue(moved.lng < -179.0)
        assertTrue(abs(moved.lng) <= 180.0)
    }

    @Test
    fun destinationDistanceIsConsistentAtLatitude() {
        val (lat, lng) = FlightMotion.destination(60.0, 10.0, 90.0, 10_000.0)
        assertEquals(60.0, lat, 0.01)
        assertEquals(10_000.0 / (111_195.08 * cos(Math.toRadians(60.0))), lng - 10.0, 1e-3)
    }
}
