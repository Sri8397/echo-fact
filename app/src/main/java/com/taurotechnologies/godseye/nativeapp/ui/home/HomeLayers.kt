package com.taurotechnologies.godseye.nativeapp.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.taurotechnologies.godseye.nativeapp.data.RadarEntry
import com.taurotechnologies.godseye.nativeapp.data.RadarFeedFilter
import com.taurotechnologies.godseye.nativeapp.data.RadarLayer
import com.taurotechnologies.godseye.nativeapp.data.RadarSignal
import com.taurotechnologies.godseye.nativeapp.ui.theme.SuvidhaColors
import kotlin.math.abs
import kotlin.math.roundToInt

/** Layer groups in the layers sheet. Every layer belongs to exactly one group (enforced by a test). */
enum class LayerGroup(val title: String, val layers: List<RadarFeedFilter>) {
    NATURAL(
        "Natural",
        listOf(RadarFeedFilter.SEISMIC, RadarFeedFilter.WEATHER, RadarFeedFilter.HAZARDS, RadarFeedFilter.FIRES, RadarFeedFilter.DISEASE),
    ),
    SECURITY(
        "Security",
        listOf(RadarFeedFilter.MILITARY, RadarFeedFilter.NUCLEAR, RadarFeedFilter.CONFLICTS, RadarFeedFilter.UNREST, RadarFeedFilter.CYBER),
    ),
    INFRASTRUCTURE(
        "Infrastructure",
        listOf(
            RadarFeedFilter.MARITIME, RadarFeedFilter.FLIGHTS, RadarFeedFilter.SATELLITES, RadarFeedFilter.WEBCAMS, RadarFeedFilter.DATACENTERS,
            RadarFeedFilter.OUTAGES, RadarFeedFilter.PIPELINES, RadarFeedFilter.FUEL, RadarFeedFilter.MINERALS,
        ),
    ),
    HUMANITARIAN_ECONOMY(
        "Humanitarian & economy",
        listOf(RadarFeedFilter.DISPLACEMENT, RadarFeedFilter.SANCTIONS),
    ),
}

/** Every layer filter, in enum order. ALL is not a layer. */
val LAYER_FILTERS: List<RadarFeedFilter> = RadarFeedFilter.entries.filter { it != RadarFeedFilter.ALL }

/** Chips pinned when the user has not chosen their own set. */
val DEFAULT_PINNED_LAYERS: List<RadarFeedFilter> = listOf(
    RadarFeedFilter.SEISMIC, RadarFeedFilter.FLIGHTS, RadarFeedFilter.MILITARY, RadarFeedFilter.CONFLICTS, RadarFeedFilter.WEATHER,
)

/** Snap points of the list sheet, from hidden to fully open. */
enum class StreamSheet { HIDDEN, PEEK, HALF, FULL }

/** Sheet drags faster than this (px/s) snap one step in the fling direction. */
const val SHEET_FLING_PX_PER_SEC: Float = 900f
/** Slower drags move one step per this many px, so a long drag can cross more than one snap point. */
const val SHEET_STEP_PX: Float = 140f
private const val SHEET_MIN_DRAG_PX: Float = 40f
private const val SHEET_MAX_STEPS = 3

fun RadarLayer.feedFilter(): RadarFeedFilter = RadarFeedFilter.valueOf(name)

/** Live rows per filter, from one feed (unfiltered by layer or search). ALL counts every row. */
fun layerCounts(entries: List<RadarEntry>): Map<RadarFeedFilter, Int> {
    val counts = HashMap<RadarFeedFilter, Int>()
    for (entry in entries) {
        when (entry) {
            is RadarEntry.Signal -> counts.merge(entry.signal.layer.feedFilter(), 1, Int::plus)
        }
    }
    counts[RadarFeedFilter.ALL] = entries.size
    return counts
}

/** Pinned layers from preferences. A null set means the defaults; unknown names are dropped. */
fun pinnedLayers(saved: Set<String>?): List<RadarFeedFilter> =
    if (saved == null) DEFAULT_PINNED_LAYERS else LAYER_FILTERS.filter { it.name in saved }

/** Flips one layer's pin and returns the new stored set, starting from the effective pinned list. */
fun togglePinnedLayer(saved: Set<String>?, filter: RadarFeedFilter): Set<String> {
    val current = pinnedLayers(saved).toMutableList()
    if (filter in current) current.remove(filter) else current.add(filter)
    return current.map { it.name }.toSet()
}

/**
 * Chips under the search bar: All always shows. A layer chip shows when it is pinned or selected,
 * and it has rows. A layer with zero rows is hidden, but a selected layer stays so the user can clear it.
 */
fun chipFilters(counts: Map<RadarFeedFilter, Int>, pinned: List<RadarFeedFilter>, selected: RadarFeedFilter): List<RadarFeedFilter> {
    val candidates = if (selected in LAYER_FILTERS && selected !in pinned) pinned + selected else pinned
    val layers = candidates.distinct().filter { (counts[it] ?: 0) > 0 || it == selected }
    return listOf(RadarFeedFilter.ALL) + layers
}

/** Feed rows whose title, location or category contain [term]. A blank term matches everything. */
fun matchesSearch(entry: RadarEntry, term: String): Boolean {
    val t = term.trim()
    if (t.isEmpty()) return true
    return when (entry) {
        is RadarEntry.Signal -> entry.signal.title.contains(t, ignoreCase = true) ||
            entry.signal.locationName.contains(t, ignoreCase = true) ||
            entry.signal.flight?.let { f ->
                f.callsign?.replace(" ", "")?.contains(t.replace(" ", ""), ignoreCase = true) == true ||
                    f.id.contains(t, ignoreCase = true)
            } == true ||
            entry.signal.id.contains(t, ignoreCase = true)
    }
}

/**
 * Moves the list sheet after a drag. [dragPx] is positive when the finger moved down (the sheet shrinks).
 * A fling snaps one step. A slow drag moves one step per [SHEET_STEP_PX], at most [SHEET_MAX_STEPS].
 */
fun settleStreamSheet(current: StreamSheet, dragPx: Float, flingPxPerSec: Float): StreamSheet {
    val flinging = abs(flingPxPerSec) >= SHEET_FLING_PX_PER_SEC
    val downward = when {
        flinging -> flingPxPerSec > 0
        abs(dragPx) >= SHEET_MIN_DRAG_PX -> dragPx > 0
        else -> return current
    }
    val steps = if (flinging) 1 else (abs(dragPx) / SHEET_STEP_PX).roundToInt().coerceIn(1, SHEET_MAX_STEPS)
    var sheet = current
    repeat(steps) { sheet = if (downward) sheet.lower() else sheet.higher() }
    return sheet
}

fun StreamSheet.lower(): StreamSheet = when (this) {
    StreamSheet.FULL -> StreamSheet.HALF
    StreamSheet.HALF -> StreamSheet.PEEK
    StreamSheet.PEEK, StreamSheet.HIDDEN -> StreamSheet.HIDDEN
}

fun StreamSheet.higher(): StreamSheet = when (this) {
    StreamSheet.HIDDEN -> StreamSheet.PEEK
    StreamSheet.PEEK -> StreamSheet.HALF
    StreamSheet.HALF, StreamSheet.FULL -> StreamSheet.FULL
}

/** Colour for a layer. Theme-derived hues keep seismic red, flight amber and the the default ink apart. */
fun RadarLayer.color(colors: SuvidhaColors): Color = when (this) {
    RadarLayer.SEISMIC -> colors.danger
    RadarLayer.WEATHER -> colors.confirm
    RadarLayer.MARITIME -> colors.green
    RadarLayer.FLIGHTS -> colors.accent
    RadarLayer.HAZARDS -> lerp(colors.danger, colors.accent, 0.5f)
    RadarLayer.OUTAGES -> colors.muted
    RadarLayer.UNREST -> lerp(colors.danger, colors.confirm, 0.5f)
    RadarLayer.MILITARY -> Color(0xFFFF5252)
    RadarLayer.NUCLEAR -> Color(0xFFFFD166)
    RadarLayer.CONFLICTS -> Color(0xFFFF6B6B)
    RadarLayer.CYBER -> Color(0xFF845EC2)
    RadarLayer.FIRES -> Color(0xFFFF9671)
    RadarLayer.DATACENTERS -> Color(0xFF2C73D2)
    RadarLayer.SATELLITES -> Color(0xFF00D2FF)
    RadarLayer.WEBCAMS -> Color(0xFFFF5C8A)
    RadarLayer.DISEASE -> Color(0xFFB5E48C)
    RadarLayer.DISPLACEMENT -> Color(0xFFF4A261)
    RadarLayer.SANCTIONS -> Color(0xFFADB5BD)
    RadarLayer.FUEL -> Color(0xFFE9C46A)
    RadarLayer.PIPELINES -> Color(0xFF90BE6D)
    RadarLayer.MINERALS -> Color(0xFFC77DFF)
}

fun RadarFeedFilter.color(colors: SuvidhaColors): Color? = when (this) {
    RadarFeedFilter.ALL -> null
    else -> RadarLayer.valueOf(name).color(colors)
}

/** Short time and place line for a callout or card. */
fun signalMetaLine(signal: RadarSignal): String = listOfNotNull(signal.observedAt.takeIf { it.isNotBlank() }, signal.locationName.takeIf { it.isNotBlank() })
    .joinToString("  ·  ")

/**
 * One key fact: aircraft and satellite numbers when present, else the first sentence of the summary.
 * [groundElevationMeters] is the terrain under the aircraft, when the globe knows it, for its height above ground.
 */
fun signalKeyFact(signal: RadarSignal, aviationUnits: Boolean, groundElevationMeters: Double? = null): String {
    signal.flight?.let { flight ->
        fun fmt(m: Double) = if (aviationUnits) "${(m * 3.280839895).roundToInt()} ft" else "${m.roundToInt()} m"
        val alt = flight.altitudeMeters?.let(::fmt)
        val agl = when {
            flight.onGround == true -> "On ground"
            else -> flight.heightAboveGroundMeters(groundElevationMeters)?.let { "Above ground ~${fmt(it)}" }
        }
        return listOfNotNull(alt?.let { "Altitude $it" }, agl, flight.callsign?.let { "Callsign $it" }).joinToString("  ·  ")
            .ifEmpty { signal.summary.substringBefore('.').trim() }
    }
    signal.satellite?.let { return "${it.altitudeKm.toInt()} km LEO · ${it.type}" }
    return signal.summary.substringBefore('.').trim()
}
