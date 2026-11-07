package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRadarRepositoryTest {
    @Test fun mapsUsgsGeoJsonPropertiesAndCoordinatesToLiveSignal() {
        val signals = LiveRadarRepository.parseEarthquakes(
            """{"type":"FeatureCollection","features":[{"type":"Feature","id":"us7000abcd","properties":{"mag":4.25,"place":"12 km NE of Testville","time":1793961000000,"url":"https://earthquake.usgs.gov/earthquakes/eventpage/us7000abcd"},"geometry":{"type":"Point","coordinates":[-121.5,37.25,8.4]}}]}""",
        )

        assertEquals(1, signals.size)
        val quake = signals.single()
        assertEquals("usgs:us7000abcd", quake.id)
        assertEquals(RadarLayer.SEISMIC, quake.layer)
        assertEquals("M 4.3 earthquake", quake.title)
        assertEquals("12 km NE of Testville", quake.locationName)
        assertEquals(37.25, quake.lat, 0.0)
        assertEquals(-121.5, quake.lng, 0.0)
        assertEquals("2026-11-06 10:30 UTC", quake.observedAt)
        assertEquals("USGS", quake.provider)
        assertTrue(quake.summary.contains("Depth 8.4 km"))
        assertFalse(quake.isSimulated)
        assertEquals("https://earthquake.usgs.gov/earthquakes/eventpage/us7000abcd", quake.eventSourceUrl)
    }

    @Test fun skipsInvalidCoordinatesAndFeaturesButAllowsMissingOptionalValues() {
        val signals = LiveRadarRepository.parseEarthquakes(
            """{"features":[{"id":"bad-lat","properties":{"url":"https://earthquake.usgs.gov/x"},"geometry":{"type":"Point","coordinates":[10,91,2]}},{"id":"missing-geometry","properties":{}},{"id":"minimal","properties":{"url":null},"geometry":{"type":"Point","coordinates":[10,20]}}]}""",
        )

        assertEquals(1, signals.size)
        assertEquals("minimal", signals.single().id.removePrefix("usgs:"))
        assertEquals("Location unavailable", signals.single().locationName)
        assertEquals("Unknown", signals.single().observedAt)
        assertNull(signals.single().eventSourceUrl)
        assertTrue(signals.single().summary.contains("Magnitude unavailable"))
        assertTrue(signals.single().summary.contains("Depth unavailable"))
    }

    @Test fun rejectsNonUsgsOrNonHttpsEventLinks() {
        val signals = LiveRadarRepository.parseEarthquakes(
            """{"features":[{"id":"external","properties":{"url":"https://example.com/quake"},"geometry":{"type":"Point","coordinates":[1,2,3]}},{"id":"plain-http","properties":{"url":"http://earthquake.usgs.gov/quake"},"geometry":{"type":"Point","coordinates":[1,2,3]}},{"id":"safe","properties":{"url":"https://earthquake.usgs.gov/quake"},"geometry":{"type":"Point","coordinates":[1,2,3]}}]}""",
        )

        assertEquals(listOf("usgs:safe"), signals.map { it.id })
    }

    @Test fun invalidJsonFailsClearlyAndEmptyFeatureListIsValid() {
        try {
            LiveRadarRepository.parseEarthquakes("not-json")
            throw AssertionError("expected parser error")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message!!.contains("valid GeoJSON"))
        }
        assertTrue(LiveRadarRepository.parseEarthquakes("""{"features":[]}""").isEmpty())
        try {
            LiveRadarRepository.parseEarthquakes("""{"type":"FeatureCollection"}""")
            throw AssertionError("expected missing features error")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message!!.contains("missing a features array"))
        }
    }
}
