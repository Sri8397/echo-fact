package com.taurotechnologies.godseye.nativeapp.ui.components

import androidx.compose.ui.graphics.Color
import com.taurotechnologies.godseye.nativeapp.data.RadarLayer
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A geographic marker shared by the map and the ground-to-sky view. */
data class GlobeMarker(
    val id: String,
    val lat: Double,
    val lng: Double,
    val color: Color? = null,
    val isFlight: Boolean = false,
    val isSatellite: Boolean = false,
    val heading: Float? = null,
    val layer: RadarLayer? = null,
    val altitudeMeters: Double? = null,
    val severity: String? = null,
    val title: String? = null,
    /** An aircraft reported on the ground: the globe sets it on the terrain instead of at its altitude. */
    val onGround: Boolean = false,
    /**
     * How fast the marker is moving right now, in degrees of latitude and longitude per second, from the
     * same motion model that placed it. The globe uses it to glide the marker between position ticks.
     * Null for markers that hold still (static events, reduced motion, aircraft on the ground).
     */
    val motion: MarkerMotion? = null,
)

/** Degrees per second of latitude and longitude. */
data class MarkerMotion(val latDegPerSec: Double, val lngDegPerSec: Double) {
    companion object {
        /** Rate between two positions [seconds] apart, taking the short way across the antimeridian. Null when still. */
        fun between(lat1: Double, lng1: Double, lat2: Double, lng2: Double, seconds: Double): MarkerMotion? {
            if (seconds <= 0.0) return null
            var dLng = lng2 - lng1
            if (dLng > 180.0) dLng -= 360.0
            if (dLng < -180.0) dLng += 360.0
            val dLat = lat2 - lat1
            if (dLat == 0.0 && dLng == 0.0) return null
            return MarkerMotion(dLat / seconds, dLng / seconds)
        }
    }
}

/** The visible map area, used to filter the feed to what is on screen. */
data class ViewportBounds(
    val south: Double,
    val north: Double,
    val west: Double,
    val east: Double,
) {
    fun contains(lat: Double, lng: Double): Boolean {
        val latIn = lat in south..north
        val lngIn = if (west <= east) lng in west..east else lng >= west || lng <= east
        return latIn && lngIn
    }
}

/** A marker projected to WebView pixels. The WebView reports these; [MapLabelPlanner] decides what is drawn. */
data class ScreenPoint(
    val id: String,
    val x: Double,
    val y: Double,
    /** Higher wins a label slot. Callers add a boost for the selected marker. */
    val priority: Double,
    val title: String?,
    val lat: Double,
    val lng: Double,
)

data class LabelPlacement(val id: String, val x: Double, val y: Double, val text: String)

data class ClusterPlacement(val index: Int, val count: Int, val lat: Double, val lng: Double, val x: Double, val y: Double)

data class MapPlan(val labels: List<LabelPlacement>, val clusters: List<ClusterPlacement>)

/**
 * Decides which marker labels and cluster badges the map shows. Pure, so the rules can be tested:
 * - Zoomed out (camera at or above [CLUSTER_MIN_HEIGHT_M]), markers that share a [CLUSTER_CELL_PX] screen cell
 *   become one badge with a count. The selected marker is never folded into a badge.
 * - Zoomed in (at or below [LABEL_MAX_HEIGHT_M]), single markers get a one-line label. Labels are placed by
 *   priority, skip any that would overlap an accepted label or badge, and stop at [MAX_LABELS].
 */
object MapLabelPlanner {
    const val MAX_LABELS = 30
    const val CLUSTER_MIN_HEIGHT_M = 4_000_000.0
    const val LABEL_MAX_HEIGHT_M = 6_000_000.0
    const val CLUSTER_CELL_PX = 56.0
    const val LABEL_MAX_CHARS = 26
    private const val CHAR_PX = 6.6
    private const val LABEL_HEIGHT_PX = 16.0
    private const val LABEL_OFFSET_PX = 10.0
    private const val BADGE_HALF_W_PX = 16.0
    private const val BADGE_HALF_H_PX = 10.0
    private const val EDGE_PX = 24.0
    private const val SELECTED_BOOST = 1_000.0

    /** One-line label text, or null when the marker has no title. */
    fun labelText(title: String?): String? {
        val trimmed = title?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return if (trimmed.length > LABEL_MAX_CHARS) trimmed.take(LABEL_MAX_CHARS - 1) + "…" else trimmed
    }

    fun plan(points: List<ScreenPoint>, cameraHeightM: Double, width: Double, height: Double, selectedId: String?): MapPlan {
        val onScreen = points.filter { p ->
            p.x.isFinite() && p.y.isFinite() &&
                p.x >= -EDGE_PX && p.x <= width + EDGE_PX && p.y >= -EDGE_PX && p.y <= height + EDGE_PX
        }
        val clusters = mutableListOf<ClusterPlacement>()
        val singles = mutableListOf<ScreenPoint>()
        if (cameraHeightM >= CLUSTER_MIN_HEIGHT_M) {
            val cells = LinkedHashMap<Pair<Int, Int>, MutableList<ScreenPoint>>()
            for (p in onScreen) {
                if (p.id == selectedId) {
                    singles += p
                    continue
                }
                val key = Math.floor(p.x / CLUSTER_CELL_PX).toInt() to Math.floor(p.y / CLUSTER_CELL_PX).toInt()
                cells.getOrPut(key) { mutableListOf() } += p
            }
            // Sorted cell order keeps badge indexes stable for the same input.
            for ((_, members) in cells.entries.sortedWith(compareBy({ it.key.first }, { it.key.second }))) {
                if (members.size >= 2) {
                    clusters += ClusterPlacement(
                        index = clusters.size,
                        count = members.size,
                        lat = members.map { it.lat }.average(),
                        lng = members.map { it.lng }.average(),
                        x = members.map { it.x }.average(),
                        y = members.map { it.y }.average(),
                    )
                } else {
                    singles += members
                }
            }
        } else {
            singles += onScreen
        }
        if (cameraHeightM > LABEL_MAX_HEIGHT_M) return MapPlan(emptyList(), clusters)

        val taken = mutableListOf<Rect>()
        for (c in clusters) taken += Rect(c.x - BADGE_HALF_W_PX, c.y - BADGE_HALF_H_PX, c.x + BADGE_HALF_W_PX, c.y + BADGE_HALF_H_PX)
        val labels = mutableListOf<LabelPlacement>()
        // Stable sort: equal priorities keep the caller's order (the feed order, newest first).
        val ordered = singles.sortedByDescending { score(it, selectedId) }
        for (p in ordered) {
            if (labels.size >= MAX_LABELS) break
            val text = labelText(p.title) ?: continue
            val rect = Rect(
                left = p.x + LABEL_OFFSET_PX,
                top = p.y - LABEL_HEIGHT_PX / 2,
                right = p.x + LABEL_OFFSET_PX + text.length * CHAR_PX + 6,
                bottom = p.y + LABEL_HEIGHT_PX / 2,
            )
            val isSelected = p.id == selectedId
            if (!isSelected && taken.any { it.overlaps(rect) }) continue
            taken += rect
            labels += LabelPlacement(p.id, p.x, p.y, text)
        }
        return MapPlan(labels, clusters)
    }

    private fun score(p: ScreenPoint, selectedId: String?): Double =
        p.priority + if (p.id == selectedId) SELECTED_BOOST else 0.0

    private data class Rect(val left: Double, val top: Double, val right: Double, val bottom: Double) {
        fun overlaps(other: Rect): Boolean =
            left < other.right && other.left < right && top < other.bottom && other.top < bottom
    }
}

/** What the map callout shows for the selected marker. Built from the feed entry, so the WebView only renders it. */
data class MapCallout(
    val id: String,
    val title: String,
    val layerLabel: String,
    val colorHex: String?,
    val time: String,
    val source: String,
    val fact: String,
    val saved: Boolean,
)

/** `{past:[lng,lat,alt,...], ahead:[...]}` flat degree/metre triples for Cesium, or `null` for no path. */
internal fun trajectoryJson(trajectory: com.taurotechnologies.godseye.nativeapp.data.FlightTrajectory?): String {
    if (trajectory == null || trajectory.isEmpty) return "null"
    fun flat(points: List<com.taurotechnologies.godseye.nativeapp.data.TrackPoint>) = buildJsonArray {
        if (points.size < 2) return@buildJsonArray
        for (p in points) { add(JsonPrimitive(p.lng)); add(JsonPrimitive(p.lat)); add(JsonPrimitive(p.altitudeMeters ?: 0.0)) }
    }
    return buildJsonObject {
        put("past", flat(trajectory.past))
        put("ahead", flat(trajectory.ahead))
    }.toString()
}
