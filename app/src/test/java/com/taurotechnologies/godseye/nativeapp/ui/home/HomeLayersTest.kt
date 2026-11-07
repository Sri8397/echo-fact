package com.taurotechnologies.godseye.nativeapp.ui.home

import com.taurotechnologies.godseye.nativeapp.data.RadarEntry
import com.taurotechnologies.godseye.nativeapp.data.RadarFeedFilter
import com.taurotechnologies.godseye.nativeapp.data.RadarLayer
import com.taurotechnologies.godseye.nativeapp.data.RadarSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayersTest {
    @Test
    fun everyLayerBelongsToExactlyOneGroup() {
        val grouped = LayerGroup.entries.flatMap { it.layers }
        assertEquals(grouped.size, grouped.toSet().size)
        assertEquals(LAYER_FILTERS.toSet(), grouped.toSet())
        assertEquals(RadarLayer.entries.size, LAYER_FILTERS.size)
        assertEquals(RadarLayer.entries.map { it.name }.toSet(), grouped.map { it.name }.toSet())
    }

    @Test
    fun layerCountsCountRowsPerLayerAndAll() {
        val entries = listOf(
            signal("a", RadarLayer.SEISMIC),
            signal("b", RadarLayer.SEISMIC),
            signal("c", RadarLayer.FLIGHTS),
        )
        val counts = layerCounts(entries)
        assertEquals(2, counts[RadarFeedFilter.SEISMIC])
        assertEquals(1, counts[RadarFeedFilter.FLIGHTS])
        assertEquals(0, counts[RadarFeedFilter.MILITARY] ?: 0)
        assertEquals(3, counts[RadarFeedFilter.ALL])
    }

    @Test
    fun chipsHideLayersWithZeroRowsButKeepAll() {
        val counts = mapOf(RadarFeedFilter.SEISMIC to 3, RadarFeedFilter.FLIGHTS to 0)
        val chips = chipFilters(counts, listOf(RadarFeedFilter.SEISMIC, RadarFeedFilter.FLIGHTS), RadarFeedFilter.ALL)
        assertEquals(listOf(RadarFeedFilter.ALL, RadarFeedFilter.SEISMIC), chips)
    }

    @Test
    fun selectedLayerChipStaysVisibleEvenWithZeroRows() {
        val chips = chipFilters(emptyMap(), listOf(RadarFeedFilter.SEISMIC), RadarFeedFilter.MINERALS)
        assertTrue(RadarFeedFilter.MINERALS in chips)
        assertFalse(RadarFeedFilter.SEISMIC in chips)
    }

    @Test
    fun pinnedLayersUseDefaultsUntilChosenAndDropUnknownNames() {
        assertEquals(DEFAULT_PINNED_LAYERS, pinnedLayers(null))
        assertEquals(listOf(RadarFeedFilter.FLIGHTS), pinnedLayers(setOf("FLIGHTS", "NOT_A_LAYER", "ALL")))
    }

    @Test
    fun togglePinnedStartsFromDefaultsAndFlipsOneLayer() {
        val unpinned = togglePinnedLayer(null, RadarFeedFilter.SEISMIC)
        assertFalse("SEISMIC" in unpinned)
        assertTrue("FLIGHTS" in unpinned)
        val pinnedAgain = togglePinnedLayer(unpinned, RadarFeedFilter.MINERALS)
        assertTrue("MINERALS" in pinnedAgain)
    }

    @Test
    fun sheetFlingStepsOneState() {
        assertEquals(StreamSheet.HIDDEN, settleStreamSheet(StreamSheet.PEEK, dragPx = 0f, flingPxPerSec = 1200f))
        assertEquals(StreamSheet.HALF, settleStreamSheet(StreamSheet.PEEK, dragPx = 0f, flingPxPerSec = -1200f))
        assertEquals(StreamSheet.FULL, settleStreamSheet(StreamSheet.HALF, dragPx = 0f, flingPxPerSec = -1200f))
    }

    @Test
    fun smallDragsDoNotMoveTheSheet() {
        assertEquals(StreamSheet.HALF, settleStreamSheet(StreamSheet.HALF, dragPx = 12f, flingPxPerSec = 0f))
        assertEquals(StreamSheet.HALF, settleStreamSheet(StreamSheet.HALF, dragPx = -30f, flingPxPerSec = 100f))
    }

    @Test
    fun slowDragStepsByDistance() {
        assertEquals(StreamSheet.HALF, settleStreamSheet(StreamSheet.FULL, dragPx = 150f, flingPxPerSec = 0f))
        assertEquals(StreamSheet.PEEK, settleStreamSheet(StreamSheet.FULL, dragPx = 300f, flingPxPerSec = 0f))
        assertEquals(StreamSheet.HALF, settleStreamSheet(StreamSheet.PEEK, dragPx = -200f, flingPxPerSec = 0f))
        assertEquals(StreamSheet.FULL, settleStreamSheet(StreamSheet.HALF, dragPx = -400f, flingPxPerSec = 0f))
    }

    @Test
    fun sheetNeverGoesPastItsEnds() {
        assertEquals(StreamSheet.FULL, StreamSheet.FULL.higher())
        assertEquals(StreamSheet.HIDDEN, StreamSheet.HIDDEN.lower())
        assertEquals(StreamSheet.HALF, StreamSheet.FULL.lower())
        assertEquals(StreamSheet.PEEK, StreamSheet.HALF.lower())
    }

    @Test
    fun searchMatchesTitleAndLocationCaseInsensitively() {
        val quake = signal("quake", RadarLayer.SEISMIC, title = "Strong Quake", location = "Aleutians")
        assertTrue(matchesSearch(quake, ""))
        assertTrue(matchesSearch(quake, "  quake "))
        assertTrue(matchesSearch(quake, "aleut"))
        assertFalse(matchesSearch(quake, "tornado"))
    }

    private fun signal(
        id: String,
        layer: RadarLayer,
        title: String = "Signal $id",
        location: String = "Somewhere",
        severity: String = "Low",
        magnitude: Double? = null,
    ): RadarEntry.Signal = RadarEntry.Signal(
        RadarSignal(
            id = id, layer = layer, title = title, locationName = location, lat = 1.0, lng = 2.0,
            observedAt = "2026-11-07 10:00 UTC", severity = severity, summary = "Summary. More.",
            magnitude = magnitude,
        ),
    )
}
