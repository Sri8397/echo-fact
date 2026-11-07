package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightTrajectoryTest {
    private fun flight(lat: Double, lng: Double, t: Long, speed: Double? = 200.0, onGround: Boolean? = false) =
        FlightObservation("abc123", "TST1", null, lat, lng, 10_000.0, speed, 90.0, onGround, t)

    @Test fun buildsPastTrailFromSuccessiveSnapshots() {
        FlightTrackHistory.clear()
        FlightTrackHistory.record(listOf(flight(10.0, 10.0, 1000)), 1000)
        FlightTrackHistory.record(listOf(flight(10.0, 10.1, 1030)), 1030)
        val track = FlightTrackHistory.trajectory(flight(10.0, 10.1, 1030))
        assertEquals(2, track.past.size)
    }

    @Test fun singleFixHasNoPastButProjectsAhead() {
        FlightTrackHistory.clear()
        val f = flight(10.0, 10.0, 1000)
        FlightTrackHistory.record(listOf(f), 1000)
        val track = FlightTrackHistory.trajectory(f)
        assertTrue(track.past.isEmpty())
        assertEquals(11, track.ahead.size)
        assertTrue(track.ahead.last().lng > 10.0)
    }

    @Test fun groundedOrStationaryFlightsHaveNoProjection() {
        assertTrue(FlightTrackHistory.projectAhead(flight(0.0, 0.0, 1, onGround = true)).isEmpty())
        assertTrue(FlightTrackHistory.projectAhead(flight(0.0, 0.0, 1, speed = null)).isEmpty())
    }

    @Test fun forgetsAircraftThatLeftTheSnapshot() {
        FlightTrackHistory.clear()
        FlightTrackHistory.record(listOf(flight(1.0, 1.0, 1000)), 1000)
        FlightTrackHistory.record(emptyList(), 1030)
        assertTrue(FlightTrackHistory.trajectory(flight(1.0, 1.0, 1030)).past.isEmpty())
    }
}
