package com.just_sri.suvidha.radar.data

/** Official provider material that can guide a future live-data integration for a radar layer. */
data class RadarSourceReference(
    val publisher: String,
    val title: String,
    val description: String,
    val url: String,
)

/**
 * Official provider references for this layer. They describe real data products and documentation;
 * they do not corroborate the fictional signals returned by [MockRadarApi].
 */
fun RadarLayer.sourceReferences(): List<RadarSourceReference> = when (this) {
    RadarLayer.SEISMIC -> listOf(
        RadarSourceReference(
            publisher = "U.S. Geological Survey",
            title = "Real-time earthquake feeds",
            description = "USGS real-time earthquake feeds in GeoJSON, Atom, KML, and other formats.",
            url = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/",
        ),
        RadarSourceReference(
            publisher = "U.S. Geological Survey",
            title = "Earthquake Catalog API",
            description = "Documentation for querying the USGS earthquake event catalog and its event metadata.",
            url = "https://earthquake.usgs.gov/fdsnws/event/1/",
        ),
    )

    RadarLayer.WEATHER -> listOf(
        RadarSourceReference(
            publisher = "U.S. National Weather Service",
            title = "Weather alerts service",
            description = "NWS watches, warnings, and advisories, with API guidance for active and regional alerts.",
            url = "https://www.weather.gov/documentation/services-web-alerts",
        ),
        RadarSourceReference(
            publisher = "NOAA National Hurricane Center",
            title = "Tropical cyclone products",
            description = "Official tropical cyclone advisories, forecasts, and outlook product descriptions for supported basins.",
            url = "https://www.nhc.noaa.gov/aboutnhcprod.shtml",
        ),
    )

    RadarLayer.MARITIME -> listOf(
        RadarSourceReference(
            publisher = "MarineTraffic",
            title = "AIS Data API documentation",
            description = "MarineTraffic API reference for AIS vessel position data; service access requires API credentials.",
            url = "https://servicedocs.marinetraffic.com/",
        ),
        RadarSourceReference(
            publisher = "Fintraffic Digitraffic",
            title = "Marine traffic open data",
            description = "Finnish open marine data, including AIS vessel locations, port calls, and sea-state observations.",
            url = "https://www.digitraffic.fi/en/marine-traffic/",
        ),
    )
}
