package com.taurotechnologies.godseye.nativeapp.data

/** Filters available in the global radar feed. */
enum class RadarFeedFilter(val label: String) {
    ALL("All"),
    SEISMIC("Seismic"),
    WEATHER("Weather"),
    MARITIME("Maritime"),
    FLIGHTS("Flights"),
    HAZARDS("Hazards"),
    OUTAGES("Outages"),
    UNREST("Unrest"),
    MILITARY("Military"),
    NUCLEAR("Nuclear"),
    CONFLICTS("Conflicts"),
    CYBER("Cyber"),
    FIRES("Fires"),
    DATACENTERS("Datacenters"),
    SATELLITES("Satellites"),
    WEBCAMS("Webcams"),
    DISEASE("Disease"),
    DISPLACEMENT("Displacement"),
    SANCTIONS("Sanctions"),
    FUEL("Fuel"),
    PIPELINES("Pipelines"),
    MINERALS("Minerals"),
}

/** A map/list item in the live-signal radar feed. */
sealed interface RadarEntry {
    val id: String
    val lat: Double
    val lng: Double

    data class Signal(val signal: RadarSignal) : RadarEntry {
        override val id: String = "signal:${signal.id}"
        override val lat: Double get() = signal.lat
        override val lng: Double get() = signal.lng
    }
}

/**
 * How "hot" an entry is for the map: stronger earthquakes rank first; live aircraft tie and keep
 * the feed's newest-first order.
 */
fun RadarEntry.heat(): Double = when (this) {
    // Airborne flights have heat 4.0 so they share the global map budget alongside quakes.
    is RadarEntry.Signal -> signal.magnitude ?: signal.rank ?: when (signal.layer) {
        RadarLayer.FLIGHTS -> if (signal.flight?.onGround == true) 2.0 else 4.0
        RadarLayer.SATELLITES -> 4.5
        else -> 3.0
    }
}

/** Markers shown at a map zoom: about 30 at the default zoom, doubling with each zoom step. */
fun markerBudget(zoom: Double): Int {
    val steps = (zoom - BASE_MAP_ZOOM).coerceAtLeast(0.0)
    return (MIN_MARKERS * Math.pow(2.0, steps)).toInt().coerceIn(MIN_MARKERS, MAX_MARKERS)
}

/** Keeps the hottest entries that fit the zoom budget; [selectedId] is always kept visible. */
fun <T : RadarEntry> List<T>.hottestForZoom(zoom: Double, selectedId: String?): List<T> {
    val budget = markerBudget(zoom)
    if (size <= budget) return this
    val keep = sortedByDescending { it.heat() }.take(budget).map { it.id }.toMutableSet()
    if (selectedId != null) keep += selectedId
    return filter { it.id in keep }
}

private const val BASE_MAP_ZOOM: Double = 2.0
private const val MIN_MARKERS: Int = 30
private const val MAX_MARKERS: Int = 2000

/** Build the feed for [filter], preserving the signals' source order. */
fun radarFeed(
    signals: List<RadarSignal>,
    filter: RadarFeedFilter,
): List<RadarEntry> = when (filter) {
    RadarFeedFilter.ALL -> signals.map(RadarEntry::Signal)
    RadarFeedFilter.SEISMIC,
    RadarFeedFilter.WEATHER,
    RadarFeedFilter.MARITIME,
    RadarFeedFilter.FLIGHTS,
    RadarFeedFilter.HAZARDS,
    RadarFeedFilter.OUTAGES,
    RadarFeedFilter.UNREST,
    RadarFeedFilter.MILITARY,
    RadarFeedFilter.NUCLEAR,
    RadarFeedFilter.CONFLICTS,
    RadarFeedFilter.CYBER,
    RadarFeedFilter.FIRES,
    RadarFeedFilter.DATACENTERS,
    RadarFeedFilter.SATELLITES,
    RadarFeedFilter.WEBCAMS,
    RadarFeedFilter.DISEASE,
    RadarFeedFilter.DISPLACEMENT,
    RadarFeedFilter.SANCTIONS,
    RadarFeedFilter.FUEL,
    RadarFeedFilter.PIPELINES,
    RadarFeedFilter.MINERALS,
    -> signals
        .filter { it.layer.name == filter.name }
        .map(RadarEntry::Signal)
}
