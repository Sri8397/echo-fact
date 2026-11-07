package com.taurotechnologies.godseye.nativeapp.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightObservationTest {
    private fun vector(json: String) = Json.parseToJsonElement(json) as JsonArray

    @Test fun rejectsMissingIdentityAndInvalidCoordinates() {
        assertNull(OpenSkyStateVectorNormalizer.normalize(vector("""[null,"X","Country",100,100,2.0,3.0]""")))
        assertNull(OpenSkyStateVectorNormalizer.normalize(vector("""["   ","X","Country",100,100,2.0,3.0]""")))
        assertNull(OpenSkyStateVectorNormalizer.normalize(vector("""["nothex","X","Country",100,100,2.0,3.0]""")))
        assertNull(OpenSkyStateVectorNormalizer.normalize(vector("""[123456,"X","Country",100,100,2.0,3.0]""")))
        assertNull(OpenSkyStateVectorNormalizer.normalize(vector("""[true,"X","Country",100,100,2.0,3.0]""")))
        assertNull(OpenSkyStateVectorNormalizer.normalize(vector("""["abc123","X","Country",100,100,null,3.0]""")))
        assertNull(OpenSkyStateVectorNormalizer.normalize(vector("""["abc123","X","Country",100,100,181.0,3.0]""")))
    }

    @Test fun preservesUnitsAndDoesNotCoerceNullFieldsToZero() {
        val flight = OpenSkyStateVectorNormalizer.normalize(
            vector("""[" ABC123 "," DEMO7 ","Example",1000,1001,12.5,-8.25,null,false,null,270.0]"""),
        )!!
        assertEquals("abc123", flight.id)
        assertEquals("DEMO7", flight.callsign)
        assertEquals(-8.25, flight.lat, 0.0)
        assertEquals(12.5, flight.lng, 0.0)
        assertNull(flight.altitudeMeters)
        assertNull(flight.speedMetersPerSecond)
        assertEquals(270.0, flight.headingDegrees!!, 0.0)
        assertEquals(false, flight.onGround)
        assertEquals(1000L, flight.observedAtEpochSeconds)
        assertEquals(1001L, flight.lastContactEpochSeconds)
    }

    @Test fun readsGpsAltitudeAndPrefersItForPlacement() {
        val withGps = OpenSkyStateVectorNormalizer.normalize(
            vector("""["abc123","DEMO","Example",1000,1000,12.5,-8.25,3000.0,false,120.0,90.0,null,null,3050.0]"""),
        )!!
        assertEquals(3050.0, withGps.geoAltitudeMeters!!, 0.0)
        assertEquals(3050.0, withGps.bestAltitudeMeters!!, 0.0)
        val baroOnly = OpenSkyStateVectorNormalizer.normalize(
            vector("""["abc123","DEMO","Example",1000,1000,12.5,-8.25,3000.0,false]"""),
        )!!
        assertNull(baroOnly.geoAltitudeMeters)
        assertEquals(3000.0, baroOnly.bestAltitudeMeters!!, 0.0)
    }

    @Test fun heightAboveGroundSubtractsTerrainAndNeverGoesNegative() {
        val flight = FlightObservation("abc123", null, null, 0.0, 0.0, 3000.0, null, null, false, null, geoAltitudeMeters = 3050.0)
        assertEquals(2400.0, flight.heightAboveGroundMeters(650.0)!!, 0.0) // GPS altitude minus terrain
        assertEquals(0.0, flight.heightAboveGroundMeters(4000.0)!!, 0.0)   // altitude noise below terrain
        assertNull(flight.heightAboveGroundMeters(null))                    // terrain unknown
        assertEquals(0.0, flight.copy(onGround = true).heightAboveGroundMeters(null)!!, 0.0)
        assertNull(flight.copy(altitudeMeters = null, geoAltitudeMeters = null).heightAboveGroundMeters(650.0))
    }

    @Test fun positionTimeIsNullableAndNeverInferredFromContactTime() {
        val flight = OpenSkyStateVectorNormalizer.normalize(
            vector("""["abc123","DEMO","Example",null,1001,12.5,-8.25]"""),
        )!!
        assertNull(flight.observedAtEpochSeconds)
        assertEquals(1001L, flight.lastContactEpochSeconds)
    }

    @Test fun freshnessHasBoundaryAndUnknownStates() {
        assertEquals(FlightSnapshot.Freshness.CURRENT, FlightSnapshot.freshness(880L, 1000L))
        assertEquals(FlightSnapshot.Freshness.STALE, FlightSnapshot.freshness(879L, 1000L))
        assertEquals(FlightSnapshot.Freshness.UNKNOWN, FlightSnapshot.freshness(null, 1000L))
        assertEquals(FlightSnapshot.Freshness.UNKNOWN, FlightSnapshot.freshness(1001L, 1000L))
    }

    @Test fun mockSnapshotIsDeterministicAndExplicitlySynthetic() {
        val first = MockFlightRepository.snapshot(nowEpochSeconds = 1_793_961_100L)
        val second = MockFlightRepository.snapshot(nowEpochSeconds = 1_793_961_100L)
        assertEquals(first, second)
        assertEquals(listOf("DEMO101", "DEMO202", "DEMO303"), first.flights.map { it.callsign })
        assertTrue(first.provider.contains("not live data"))
        assertEquals(FlightSnapshot.Freshness.CURRENT, first.freshness)
        assertFalse(first.flights.any { it.id.isBlank() })
    }
}
