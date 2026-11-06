package com.just_sri.suvidha.radar.data

/** Filters available in the global radar feed. */
enum class RadarFeedFilter(val label: String) {
    ALL("All"),
    STORIES("Stories"),
    SEISMIC("Seismic"),
    WEATHER("Weather"),
    MARITIME("Maritime"),
}

/** A map/list item in the combined stories and live-signal radar feed. */
sealed interface RadarEntry {
    val id: String
    val lat: Double
    val lng: Double

    data class Story(val incident: Incident) : RadarEntry {
        override val id: String = "story:${incident.id}"
        override val lat: Double get() = incident.lat
        override val lng: Double get() = incident.lng
    }

    data class Signal(val signal: RadarSignal) : RadarEntry {
        override val id: String = "signal:${signal.id}"
        override val lat: Double get() = signal.lat
        override val lng: Double get() = signal.lng
    }
}

/** Build the combined feed while preserving source order within each input list. */
fun radarFeed(
    stories: List<Incident>,
    signals: List<RadarSignal>,
    filter: RadarFeedFilter,
): List<RadarEntry> = when (filter) {
    RadarFeedFilter.ALL -> buildList(stories.size + signals.size) {
        val count = maxOf(stories.size, signals.size)
        for (index in 0 until count) {
            if (index < signals.size) add(RadarEntry.Signal(signals[index]))
            if (index < stories.size) add(RadarEntry.Story(stories[index]))
        }
    }
    RadarFeedFilter.STORIES -> stories.map(RadarEntry::Story)
    RadarFeedFilter.SEISMIC,
    RadarFeedFilter.WEATHER,
    RadarFeedFilter.MARITIME
    -> signals
        .filter { it.layer.name == filter.name }
        .map(RadarEntry::Signal)
}
