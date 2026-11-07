package com.taurotechnologies.godseye.nativeapp.data

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
    RadarLayer.FLIGHTS -> listOf(
        RadarSourceReference(
            publisher = "OpenSky Network",
            title = "Aircraft state vectors and tracks",
            description = "Official field definitions for aircraft position, altitude in metres, ground speed in metres per second, and heading. Provider documentation, not an individual aircraft record.",
            url = "https://openskynetwork.github.io/opensky-api/rest.html",
        ),
        RadarSourceReference(
            publisher = "OpenSky Network",
            title = "Data products and coverage",
            description = "Overview of the provider's aviation data products. The received snapshot identifies its actual provider and coverage.",
            url = "https://opensky-network.org/data/",
        ),
    )
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

    RadarLayer.HAZARDS -> listOf(
        RadarSourceReference(
            publisher = "GDACS",
            title = "Global Disaster Alert and Coordination System",
            description = "UN and European Commission alerts for cyclones, floods, earthquakes, droughts and volcanoes.",
            url = "https://www.gdacs.org/",
        ),
        RadarSourceReference(
            publisher = "NASA",
            title = "Earth Observatory Natural Event Tracker (EONET)",
            description = "Curated natural events such as storms, wildfires, sea and lake ice, and volcanoes.",
            url = "https://eonet.gsfc.nasa.gov/",
        ),
    )

    RadarLayer.OUTAGES -> listOf(
        RadarSourceReference(
            publisher = "Cloudflare Radar",
            title = "Outage Center",
            description = "Observed internet disruptions by country and region, with causes when known.",
            url = "https://radar.cloudflare.com/outage-center",
        ),
        RadarSourceReference(
            publisher = "Georgia Tech IODA",
            title = "Internet Outage Detection and Analysis",
            description = "Research platform measuring internet connectivity disruptions by country, region and network.",
            url = "https://ioda.inetintel.cc.gatech.edu/",
        ),
    )

    RadarLayer.UNREST -> listOf(
        RadarSourceReference(
            publisher = "The GDELT Project",
            title = "Global news event database",
            description = "Machine-coded events from worldwide news coverage. Reports are unverified media signals.",
            url = "https://www.gdeltproject.org/",
        ),
        RadarSourceReference(
            publisher = "ACLED",
            title = "Armed Conflict Location & Event Data",
            description = "Researcher-coded records of political violence and protest events worldwide.",
            url = "https://acleddata.com/",
        ),
    )

    RadarLayer.MILITARY -> listOf(
        RadarSourceReference(
            publisher = "DoD & NATO",
            title = "Global military installations",
            description = "Major strategic bases, naval stations, and air command headquarters.",
            url = "https://www.defense.gov/",
        ),
        RadarSourceReference(
            publisher = "SIPRI",
            title = "Military Expenditure & Bases Database",
            description = "Stockholm International Peace Research Institute global arms and military data.",
            url = "https://www.sipri.org/",
        ),
    )

    RadarLayer.NUCLEAR -> listOf(
        RadarSourceReference(
            publisher = "IAEA",
            title = "Power Reactor Information System (PRIS)",
            description = "Official database on nuclear power reactors and fuel cycle facilities globally.",
            url = "https://pris.iaea.org/",
        ),
        RadarSourceReference(
            publisher = "World Nuclear Association",
            title = "Reactor & Fuel Cycle Information",
            description = "Global nuclear power reactor status, uranium enrichment, and fuel cycle operations.",
            url = "https://world-nuclear.org/",
        ),
    )

    RadarLayer.CONFLICTS -> listOf(
        RadarSourceReference(
            publisher = "Uppsala Conflict Data Program (UCDP)",
            title = "Georeferenced Event Dataset",
            description = "Uppsala University organized violence data with dates and geo-coordinates.",
            url = "https://ucdp.uu.se/",
        ),
        RadarSourceReference(
            publisher = "Council on Foreign Relations",
            title = "Global Conflict Tracker",
            description = "Active conflict monitoring and geopolitical flashpoint assessments.",
            url = "https://www.cfr.org/global-conflict-tracker",
        ),
    )

    RadarLayer.CYBER -> listOf(
        RadarSourceReference(
            publisher = "abuse.ch",
            title = "Threat Intelligence Feeds",
            description = "Community-driven malware host, botnet C2, and phishing infrastructure tracker.",
            url = "https://abuse.ch/",
        ),
        RadarSourceReference(
            publisher = "CISA",
            title = "Cybersecurity Alerts & Advisories",
            description = "US Cybersecurity & Infrastructure Security Agency threat alerts and IOCs.",
            url = "https://www.cisa.gov/news-events/cybersecurity-advisories",
        ),
    )

    RadarLayer.FIRES -> listOf(
        RadarSourceReference(
            publisher = "NASA FIRMS",
            title = "Fire Information for Resource Management System",
            description = "Near real-time active fire locations from MODIS and VIIRS satellite instruments.",
            url = "https://firms.modaps.eosdis.nasa.gov/",
        ),
        RadarSourceReference(
            publisher = "Natural Resources Canada",
            title = "Canadian Wildland Fire Information System",
            description = "Fire weather index, active fire perimeters, and smoke dispersion data.",
            url = "https://cwfis.cfs.nrcan.gc.ca/",
        ),
    )

    RadarLayer.DATACENTERS -> listOf(
        RadarSourceReference(
            publisher = "Data Center Map",
            title = "Global Colocation & Cloud Data Centers",
            description = "Index of hyperscale data centers, subsea fiber landings, and internet exchanges.",
            url = "https://www.datacentermap.com/",
        ),
        RadarSourceReference(
            publisher = "Uptime Institute",
            title = "Data Center Research & Outage Analysis",
            description = "Global digital infrastructure research and critical facility tracking.",
            url = "https://uptimeinstitute.com/",
        ),
    )

    RadarLayer.WEBCAMS -> listOf(
        RadarSourceReference(
            publisher = "YouTube",
            title = "Embedded player parameters",
            description = "Official options for the embedded YouTube player used to show a live camera inside the app.",
            url = "https://developers.google.com/youtube/player_parameters",
        ),
        RadarSourceReference(
            publisher = "YouTube",
            title = "IFrame Player API",
            description = "Official reference for embedding and controlling YouTube players in web pages.",
            url = "https://developers.google.com/youtube/iframe_api_reference",
        ),
    )
    RadarLayer.SATELLITES -> listOf(
        RadarSourceReference(
            publisher = "CelesTrak",
            title = "NORAD Two-Line Element Sets",
            description = "Authoritative orbital element sets, ephemeris data, and space situational awareness tracking.",
            url = "https://celestrak.org/",
        ),
        RadarSourceReference(
            publisher = "Space-Track",
            title = "U.S. Space Force Satellite Catalog",
            description = "Official orbital data and satellite catalog maintained by the 18th Space Defense Squadron.",
            url = "https://www.space-track.org/",
        ),
    )

    RadarLayer.DISEASE -> listOf(
        RadarSourceReference(
            publisher = "World Health Organization",
            title = "Disease Outbreak News",
            description = "WHO assessments of notable disease outbreaks, with case counts and risk statements as published.",
            url = "https://www.who.int/emergencies/disease-outbreak-news",
        ),
        RadarSourceReference(
            publisher = "European Centre for Disease Prevention and Control",
            title = "Infectious disease topics",
            description = "ECDC surveillance and threat assessments for communicable diseases, including outbreaks outside Europe.",
            url = "https://www.ecdc.europa.eu/en/infectious-disease-topics",
        ),
    )

    RadarLayer.DISPLACEMENT -> listOf(
        RadarSourceReference(
            publisher = "UNHCR",
            title = "Operational Data Portal",
            description = "UNHCR refugee and displacement figures by situation and country of origin, with the as-of date for each figure.",
            url = "https://data.unhcr.org/",
        ),
        RadarSourceReference(
            publisher = "UNHCR",
            title = "Situations",
            description = "Index of UNHCR operational situations, the crises that the displacement figures are grouped under.",
            url = "https://data.unhcr.org/en/situations",
        ),
    )

    RadarLayer.SANCTIONS -> listOf(
        RadarSourceReference(
            publisher = "U.S. Department of the Treasury, OFAC",
            title = "Sanctions List Service",
            description = "Official SDN and consolidated sanctions list downloads and programme information from the Office of Foreign Assets Control.",
            url = "https://ofac.treasury.gov/sanctions-list-service",
        ),
        RadarSourceReference(
            publisher = "European Union",
            title = "Consolidated list of EU financial sanctions",
            description = "Consolidated EU list of persons, groups and entities subject to financial sanctions.",
            url = "https://data.europa.eu/data/datasets/consolidated-list-of-persons-groups-and-entities-subject-to-eu-financial-sanctions",
        ),
    )

    RadarLayer.FUEL -> listOf(
        RadarSourceReference(
            publisher = "International Energy Agency",
            title = "Data and statistics",
            description = "IEA oil market and supply statistics, including the monthly oil market data used to assess supply tightness.",
            url = "https://www.iea.org/data-and-statistics",
        ),
        RadarSourceReference(
            publisher = "International Energy Agency",
            title = "Energy security",
            description = "IEA analysis of energy supply disruptions and emergency response to oil and fuel shortages.",
            url = "https://www.iea.org/topics/energy-security",
        ),
    )

    RadarLayer.PIPELINES -> listOf(
        RadarSourceReference(
            publisher = "Global Energy Monitor",
            title = "Global Oil Infrastructure Tracker",
            description = "Open-licence (CC BY 4.0) inventory of oil pipelines and related infrastructure, with operator and status information.",
            url = "https://globalenergymonitor.org/projects/global-oil-infrastructure-tracker/",
        ),
        RadarSourceReference(
            publisher = "Global Energy Monitor",
            title = "Global Gas Infrastructure Tracker",
            description = "Open-licence (CC BY 4.0) inventory of gas pipelines and storage, with operator and status information.",
            url = "https://globalenergymonitor.org/projects/global-gas-infrastructure-tracker/",
        ),
    )

    RadarLayer.MINERALS -> listOf(
        RadarSourceReference(
            publisher = "U.S. Geological Survey",
            title = "Mineral Commodity Summaries 2026",
            description = "Annual USGS review of world mine production and reserves for critical minerals, by country.",
            url = "https://pubs.usgs.gov/periodicals/mcs2026/mcs2026.pdf",
        ),
        RadarSourceReference(
            publisher = "U.S. Geological Survey",
            title = "Publications Warehouse",
            description = "USGS catalogue of mineral commodity reports and supply data used for critical mineral assessments.",
            url = "https://pubs.usgs.gov/",
        ),
    )
}
