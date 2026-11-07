package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerBudgetTest {
    private fun quake(id: String, magnitude: Double) = RadarEntry.Signal(
        RadarSignal(id, RadarLayer.SEISMIC, id, "", 0.0, 0.0, "", "", "", magnitude = magnitude),
    )

    @Test
    fun budgetDoublesWithEachZoomStepAndIsCapped() {
        assertEquals(30, markerBudget(2.0))
        assertEquals(60, markerBudget(3.0))
        assertEquals(120, markerBudget(4.0))
        assertEquals(2000, markerBudget(20.0))
        assertEquals(30, markerBudget(0.5))
    }

    @Test
    fun keepsTheHottestEntriesAtDefaultZoom() {
        val entries = listOf(quake("low", 2.6), quake("high", 6.8), quake("mid", 4.5)) +
            (1..40).map { quake("q$it", 2.5) }
        val shown = entries.hottestForZoom(2.0, selectedId = null)
        assertEquals(30, shown.size)
        assertTrue(shown.any { it.id == "signal:high" })
        assertTrue(shown.any { it.id == "signal:mid" })
    }

    @Test
    fun zoomingInRevealsMoreAndSelectionStaysVisible() {
        val entries = (1..200).map { quake("q$it", 2.5 + it / 100.0) }
        assertEquals(30, entries.hottestForZoom(2.0, selectedId = null).size)
        assertEquals(120, entries.hottestForZoom(4.0, selectedId = null).size)
        val withSelection = entries.hottestForZoom(2.0, selectedId = "signal:q1")
        assertTrue(withSelection.any { it.id == "signal:q1" })
    }

    @Test
    fun smallListsAreNotTrimmed() {
        val entries = listOf(quake("a", 3.0), quake("b", 4.0))
        assertEquals(entries, entries.hottestForZoom(2.0, selectedId = null))
    }
}
