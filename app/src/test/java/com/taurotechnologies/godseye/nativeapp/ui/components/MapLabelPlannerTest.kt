package com.taurotechnologies.godseye.nativeapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLabelPlannerTest {
    private val zoomedIn = 1_000_000.0
    private val zoomedOut = 2.0e7
    private val middle = 5_000_000.0

    @Test
    fun labelsAreCappedAtThirtyEvenWhenNothingOverlaps() {
        val points = (0 until 100).map { i -> point("p$i", x = 20.0 + i * 120.0, y = 100.0, priority = 3.5, title = "Event $i") }
        val plan = MapLabelPlanner.plan(points, zoomedIn, width = 20_000.0, height = 800.0, selectedId = null)
        assertEquals(MapLabelPlanner.MAX_LABELS, plan.labels.size)
        assertTrue(plan.clusters.isEmpty())
    }

    @Test
    fun overlappingLabelsKeepOnlyTheHighestPriority() {
        val points = listOf(
            point("low", x = 100.0, y = 100.0, priority = 3.5, title = "Low one"),
            point("high", x = 102.0, y = 101.0, priority = 5.0, title = "High one"),
        )
        val plan = MapLabelPlanner.plan(points, zoomedIn, 800.0, 800.0, selectedId = null)
        assertEquals(listOf("high"), plan.labels.map { it.id })
    }

    @Test
    fun selectedMarkerLabelIsAlwaysShownFirst() {
        val points = listOf(
            point("loud", x = 100.0, y = 100.0, priority = 5.0, title = "Loud"),
            point("picked", x = 101.0, y = 100.0, priority = 3.5, title = "Picked"),
        )
        val plan = MapLabelPlanner.plan(points, zoomedIn, 800.0, 800.0, selectedId = "picked")
        assertEquals("picked", plan.labels.first().id)
        assertTrue(plan.labels.any { it.id == "picked" })
    }

    @Test
    fun markersWithoutTitlesGetNoLabel() {
        val plan = MapLabelPlanner.plan(listOf(point("blank", 10.0, 10.0, 3.5, title = "   ")), zoomedIn, 800.0, 800.0, null)
        assertTrue(plan.labels.isEmpty())
    }

    @Test
    fun longTitlesAreTruncatedToOneLine() {
        assertNull(MapLabelPlanner.labelText(null))
        val text = MapLabelPlanner.labelText("A".repeat(60))!!
        assertEquals(MapLabelPlanner.LABEL_MAX_CHARS, text.length)
        assertTrue(text.endsWith("…"))
        assertEquals("Short", MapLabelPlanner.labelText("  Short  "))
    }

    @Test
    fun zoomedOutMarkersInOneCellBecomeOneBadgeWithCount() {
        val points = listOf(
            point("a", x = 100.0, y = 100.0, priority = 3.5, title = "A", lat = 10.0, lng = 20.0),
            point("b", x = 110.0, y = 104.0, priority = 3.5, title = "B", lat = 12.0, lng = 22.0),
            point("c", x = 400.0, y = 400.0, priority = 3.5, title = "C", lat = 0.0, lng = 0.0),
        )
        val plan = MapLabelPlanner.plan(points, zoomedOut, 800.0, 800.0, selectedId = null)
        assertEquals(1, plan.clusters.size)
        val badge = plan.clusters.single()
        assertEquals(2, badge.count)
        assertEquals(11.0, badge.lat, 1e-9)
        assertEquals(21.0, badge.lng, 1e-9)
        assertEquals(0, badge.index)
        // The lone marker is at a height above the label limit, so it gets no label either.
        assertTrue(plan.labels.isEmpty())
    }

    @Test
    fun selectedMarkerIsNeverFoldedIntoABadge() {
        val points = listOf(
            point("a", x = 100.0, y = 100.0, priority = 3.5, title = "A"),
            point("picked", x = 102.0, y = 101.0, priority = 3.5, title = "P"),
        )
        val plan = MapLabelPlanner.plan(points, middle, 800.0, 800.0, selectedId = "picked")
        assertTrue(plan.clusters.isEmpty())
    }

    @Test
    fun middleZoomShowsLabelsAndBadgesWithoutOverlap() {
        val cluster = listOf(
            point("c1", x = 300.0, y = 300.0, priority = 3.5, title = "Clustered one"),
            point("c2", x = 305.0, y = 302.0, priority = 3.5, title = "Clustered two"),
        )
        val lone = point("lone", x = 310.0, y = 300.0, priority = 3.5, title = "Lone")
        val plan = MapLabelPlanner.plan(cluster + lone, middle, 800.0, 800.0, selectedId = null)
        assertEquals(1, plan.clusters.size)
        // The lone marker's label would sit on the badge, so it is skipped.
        assertTrue(plan.labels.none { it.id == "lone" })
    }

    @Test
    fun pointsOutsideTheViewAreDropped() {
        val points = listOf(point("off", x = -500.0, y = 100.0, priority = 5.0, title = "Off"))
        val plan = MapLabelPlanner.plan(points, zoomedIn, 800.0, 800.0, selectedId = null)
        assertTrue(plan.labels.isEmpty())
    }

    @Test
    fun clusterIndexesAreStableForTheSameInput() {
        val points = listOf(
            point("a1", 10.0, 10.0, 3.5, "A"), point("a2", 12.0, 12.0, 3.5, "A"),
            point("b1", 500.0, 500.0, 3.5, "B"), point("b2", 503.0, 503.0, 3.5, "B"),
        )
        val first = MapLabelPlanner.plan(points, zoomedOut, 800.0, 800.0, null).clusters.map { it.index to it.count }
        val second = MapLabelPlanner.plan(points, zoomedOut, 800.0, 800.0, null).clusters.map { it.index to it.count }
        assertEquals(first, second)
        assertEquals(listOf(0, 1), first.map { it.first })
    }

    private fun point(id: String, x: Double, y: Double, priority: Double, title: String?, lat: Double = 0.0, lng: Double = 0.0) =
        ScreenPoint(id = id, x = x, y = y, priority = priority, title = title, lat = lat, lng = lng)
}
