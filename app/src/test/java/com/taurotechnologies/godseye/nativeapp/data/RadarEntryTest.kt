package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarEntryTest {
    @Test
    fun allFilterKeepsEverySignalInOrder() {
        val signals = listOf(signal("first", RadarLayer.SEISMIC), signal("second", RadarLayer.FLIGHTS))

        val entries = radarFeed(signals, RadarFeedFilter.ALL)

        assertEquals(listOf("signal:first", "signal:second"), entries.map { it.id })
        assertEquals(signals[0].lat, entries[0].lat, 0.0)
    }

    @Test
    fun layerFiltersReturnOnlyMatchingSignals() {
        val signals = listOf(
            signal("quake", RadarLayer.SEISMIC),
            signal("rain", RadarLayer.WEATHER),
            signal("ships", RadarLayer.MARITIME),
            signal("flight", RadarLayer.FLIGHTS),
        )

        val cases = listOf(
            Triple(RadarFeedFilter.SEISMIC, RadarLayer.SEISMIC, "signal:quake"),
            Triple(RadarFeedFilter.WEATHER, RadarLayer.WEATHER, "signal:rain"),
            Triple(RadarFeedFilter.MARITIME, RadarLayer.MARITIME, "signal:ships"),
            Triple(RadarFeedFilter.FLIGHTS, RadarLayer.FLIGHTS, "signal:flight"),
        )
        for ((filter, expectedLayer, expectedId) in cases) {
            val entries = radarFeed(signals, filter)
            assertEquals(listOf(expectedId), entries.map { it.id })
            assertTrue(entries.all { it is RadarEntry.Signal && it.signal.layer == expectedLayer })
        }
    }

    @Test
    fun filtersUseTheExistingRegionChipLabels() {
        assertEquals(
            listOf("All", "Seismic", "Weather", "Maritime", "Flights", "Hazards", "Outages", "Unrest", "Military", "Nuclear", "Conflicts", "Cyber", "Fires", "Datacenters", "Satellites", "Webcams", "Disease", "Displacement", "Sanctions", "Fuel", "Pipelines", "Minerals"),
            RadarFeedFilter.entries.map { it.label },
        )
    }

    @Test
    fun emptyInputsProduceAnEmptyFeedForEveryFilter() {
        for (filter in RadarFeedFilter.entries) {
            assertTrue(radarFeed(emptyList(), filter).isEmpty())
        }
    }

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
