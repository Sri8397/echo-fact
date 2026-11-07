package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SatelliteMotionTest {
    private fun iss() = SatelliteObservation(
        noradId = "25544",
        name = "ISS (ZARYA)",
        type = "Space Station",
        country = "International",
        altitudeKm = 418.0,
        velocityKmPerSec = 7.66,
        inclinationDeg = 51.64,
        periodMinutes = 92.9,
        initialPhaseDeg = 0.0,
        initialEpochSeconds = 1_000L,
    )

    @Test
    fun latitudeNeverExceedsOrbitalInclination() {
        val sat = iss()
        for (sec in 1_000L..10_000L step 60) {
            val pos = SatelliteMotion.position(sat, sec.toDouble())
            assertTrue("Latitude ${pos.lat} exceeds inclination ${sat.inclinationDeg}", abs(pos.lat) <= sat.inclinationDeg + 1e-4)
            assertTrue("Longitude ${pos.lng} out of range", pos.lng in -180.0..180.0)
        }
    }

    @Test
    fun positionAdvancesAcrossOrbit() {
        val sat = iss()
        val pos0 = SatelliteMotion.position(sat, 1_000.0)
        val pos1 = SatelliteMotion.position(sat, 1_060.0) // 1 minute later
        assertTrue("Satellite must move across 60 seconds", pos0.lat != pos1.lat || pos0.lng != pos1.lng)
    }

    @Test
    fun equatorialOrbitStaysOnEquator() {
        val geo = SatelliteObservation(
            noradId = "99999",
            name = "EQUATORIAL-TEST",
            type = "Communications",
            country = "Test",
            altitudeKm = 35786.0,
            velocityKmPerSec = 3.07,
            inclinationDeg = 0.0,
            periodMinutes = 1436.0,
            initialPhaseDeg = 45.0,
            initialEpochSeconds = 1_000L,
        )
        val pos = SatelliteMotion.position(geo, 1_500.0)
        assertEquals(0.0, pos.lat, 1e-6)
    }
}
