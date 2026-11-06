package com.just_sri.suvidha.radar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarEntryTest {
    @Test
    fun allFilterInterleavesSignalsFirstAndPreservesOrderWithinInputs() {
        val stories = listOf(story("same-id"), story("story-2"), story("story-3"))
        val signals = listOf(signal("same-id"), signal("signal-2"))

        val entries = radarFeed(stories, signals, RadarFeedFilter.ALL)

        assertEquals(
            listOf(
                "signal:same-id",
                "story:same-id",
                "signal:signal-2",
                "story:story-2",
                "story:story-3",
            ),
            entries.map { it.id },
        )
        assertEquals(signals[0].lat, entries[0].lat, 0.0)
        assertEquals(stories[0].lng, entries[1].lng, 0.0)
    }

    @Test
    fun storiesFilterReturnsOnlyNamespacedStories() {
        val stories = listOf(story("a"), story("b"))

        val entries = radarFeed(stories, listOf(signal("a")), RadarFeedFilter.STORIES)

        assertEquals(listOf("story:a", "story:b"), entries.map { it.id })
        assertTrue(entries.all { it is RadarEntry.Story })
    }

    @Test
    fun layerFiltersReturnOnlyMatchingSignals() {
        val signals = listOf(
            signal("quake", RadarLayer.SEISMIC),
            signal("rain", RadarLayer.WEATHER),
            signal("ships", RadarLayer.MARITIME),
        )

        val cases = listOf(
            Triple(RadarFeedFilter.SEISMIC, RadarLayer.SEISMIC, "signal:quake"),
            Triple(RadarFeedFilter.WEATHER, RadarLayer.WEATHER, "signal:rain"),
            Triple(RadarFeedFilter.MARITIME, RadarLayer.MARITIME, "signal:ships"),
        )
        for ((filter, expectedLayer, expectedId) in cases) {
            val entries = radarFeed(listOf(story("ignored")), signals, filter)
            assertEquals(listOf(expectedId), entries.map { it.id })
            assertTrue(entries.all { it is RadarEntry.Signal && it.signal.layer == expectedLayer })
        }
    }

    @Test
    fun filtersUseTheExistingRegionChipLabels() {
        assertEquals(
            listOf("All", "Stories", "Seismic", "Weather", "Maritime"),
            RadarFeedFilter.entries.map { it.label },
        )
    }

    @Test
    fun emptyInputsProduceAnEmptyFeedForEveryFilter() {
        for (filter in RadarFeedFilter.entries) {
            assertTrue(radarFeed(emptyList(), emptyList(), filter).isEmpty())
        }
    }

    private fun story(id: String): Incident = Incident(id = id, title = id)

    private fun signal(id: String, layer: RadarLayer = RadarLayer.SEISMIC): RadarSignal = RadarSignal(
        id = id,
        layer = layer,
        title = id,
        locationName = "",
        lat = 12.0,
        lng = 34.0,
        observedAt = "2026-11-06 00:00 UTC",
        severity = "Low",
        summary = "",
    )
}
