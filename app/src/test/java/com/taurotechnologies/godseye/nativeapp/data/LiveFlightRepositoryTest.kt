package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveFlightRepositoryTest {
    @Test fun mapsOpenSkyVectorsAndUsesPositionTimeAndProviderMetadata() {
        val signals = LiveFlightRepository.parseFlights(
            """{"time":1793961600,"states":[["abc123"," DEMO42 ","Example Country",1793961540,1793961600,-73.78,40.64,10668.0,false,235.0,92.0]]}""",
            provider = "Example provider",
            coverage = "regional sample",
            nowEpochSeconds = 1_793_961_650L,
        )

        assertEquals(1, signals.size)
        val flight = signals.single()
        assertEquals("flight:abc123", flight.id)
        assertEquals(RadarLayer.FLIGHTS, flight.layer)
        assertEquals("Flight DEMO42", flight.title)
        assertEquals(40.64, flight.lat, 0.0)
        assertEquals(-73.78, flight.lng, 0.0)
        assertEquals("2026-11-06 10:39 UTC", flight.observedAt)
        assertEquals("Example provider", flight.provider)
        assertEquals("regional sample", flight.coverage)
        assertEquals(10668.0, flight.flight!!.altitudeMeters!!, 0.0)
        assertEquals(235.0, flight.flight.speedMetersPerSecond!!, 0.0)
        assertEquals(92.0, flight.flight.headingDegrees!!, 0.0)
        assertFalse(flight.isSimulated)
        assertEquals(null, flight.eventSourceUrl)
        assertTrue(flight.summary.contains("Position 40.64°, -73.78°"))
        assertTrue(flight.summary.contains("Altitude 10668 m"))
    }

    @Test fun staleSnapshotAndStalePositionAreLabeledSeparately() {
        val stale = LiveFlightRepository.parseFlights(
            """{"time":1000,"states":[["abc123","DEMO","Country",990,1000,12.0,34.0,null,false,null,null]]}""",
            "provider",
            "worldwide",
            nowEpochSeconds = 1121,
        ).single()
        assertTrue(stale.summary.contains("Snapshot is stale"))
        assertTrue(stale.coverage!!.contains("stale snapshot"))

        val oldPosition = LiveFlightRepository.parseFlights(
            """{"time":1100,"states":[["abc123","DEMO","Country",900,1100,12.0,34.0,null,false,null,null]]}""",
            "provider",
            "worldwide",
            nowEpochSeconds = 1100,
        ).single()
        assertFalse(oldPosition.summary.contains("Snapshot is stale"))
        assertTrue(oldPosition.summary.contains("Position is stale"))

        val unknown = LiveFlightRepository.parseFlights(
            """{"time":1100,"states":[["abc123","DEMO","Country",null,1100,12.0,34.0,null,false,null,null]]}""",
            "provider",
            "worldwide",
            nowEpochSeconds = 1121,
        ).single()
        assertEquals("Unknown", unknown.observedAt)
        assertTrue(unknown.summary.contains("Position time unavailable; snapshot time shown"))
        assertFalse(unknown.summary.contains("Snapshot is stale"))
    }

    @Test fun nullStatesIsValidEmptyFeedButMissingStatesIsMalformed() {
        assertTrue(LiveFlightRepository.parseFlights("""{"time":1000,"states":null}""", "p", "c", 1000).isEmpty())
        try {
            LiveFlightRepository.parseFlights("""{"time":1000}""", "p", "c", 1000)
            throw AssertionError("expected missing states error")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message!!.contains("missing a states array"))
        }
    }

    @Test fun malformedOrUnpositionedVectorsAreSkippedAndOutputIsCapped() {
        val vectors = buildList {
            add("[\"badid\",\"BAD\",\"Country\",1000,1000,1.0,2.0]")
            add("[\"abc124\",\"NOCOORDS\",\"Country\",1000,1000,null,null]")
            repeat(502) { index ->
                val id = index.toString(16).padStart(6, '0')
                add("[\"$id\",\"D$index\",\"Country\",1000,1000,1.0,2.0]")
            }
        }.joinToString(",")
        val signals = LiveFlightRepository.parseFlights(
            """{"time":1000,"states":[$vectors]}""",
            "provider",
            "worldwide",
            nowEpochSeconds = 1000,
        )

        assertEquals(500, signals.size)
        assertTrue(signals.first().coverage!!.contains("limited to first 500 records"))
        assertTrue(signals.last().coverage!!.contains("limited to first 500 records"))
        assertTrue(signals.all { !it.isSimulated })
    }

    @Test fun invalidJsonAndNonObjectResponseFailClearly() {
        for (input in listOf("not-json", "[]")) {
            try {
                LiveFlightRepository.parseFlights(input, "provider", "coverage", nowEpochSeconds = 0)
                throw AssertionError("expected parser error for $input")
            } catch (error: IllegalArgumentException) {
                assertTrue(error.message!!.contains("JSON object") || error.message!!.contains("valid JSON"))
            }
        }
    }

    @Test fun flightExceptionsDistinguishStagesAndMessages() {
        val sessionTimeout = FlightException.Timeout(FlightStep.SESSION)
        assertEquals(FlightStep.SESSION, sessionTimeout.step)
        assertTrue(sessionTimeout.displayMessage.contains("Flights session timed out"))
        assertTrue(sessionTimeout.displayMessage.contains("25s limit"))
        assertTrue(sessionTimeout.detailMessage.contains("session timed out"))

        val fetchTimeout = FlightException.Timeout(FlightStep.FLIGHTS)
        assertEquals(FlightStep.FLIGHTS, fetchTimeout.step)
        assertTrue(fetchTimeout.displayMessage.contains("Live flights request timed out"))
        assertTrue(fetchTimeout.detailMessage.contains("fetch timed out"))

        val sessionAuth = FlightException.HttpError(FlightStep.SESSION, 401)
        assertTrue(sessionAuth.displayMessage.contains("Flight session expired (HTTP 401)"))
        assertTrue(sessionAuth.detailMessage.contains("HTTP 401"))

        val fetchAuth = FlightException.HttpError(FlightStep.FLIGHTS, 401)
        assertTrue(fetchAuth.displayMessage.contains("Flight session unauthorized (HTTP 401)"))

        val rateLimited = FlightException.HttpError(FlightStep.FLIGHTS, 429, retryAfterSeconds = "60")
        assertTrue(rateLimited.displayMessage.contains("Flights rate limited (HTTP 429)"))
        assertTrue(rateLimited.displayMessage.contains("60s"))
        assertTrue(rateLimited.detailMessage.contains("Retry-After: 60"))

        val upstreamError = FlightException.HttpError(FlightStep.FLIGHTS, 502)
        assertTrue(upstreamError.displayMessage.contains("Flights upstream error (HTTP 502)"))

        val missingCookie = FlightException.MissingCookie()
        assertEquals(FlightStep.COOKIE, missingCookie.step)
        assertTrue(missingCookie.displayMessage.contains("cookie missing"))

        val parseError = FlightException.ParseError("empty payload")
        assertEquals(FlightStep.PARSE, parseError.step)
        assertTrue(parseError.displayMessage.contains("unreadable"))

        val networkError = FlightException.Network(FlightStep.FLIGHTS, "connection reset")
        assertEquals(FlightStep.FLIGHTS, networkError.step)
        assertTrue(networkError.displayMessage.contains("network error"))
    }
}
