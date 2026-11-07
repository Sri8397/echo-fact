package com.taurotechnologies.godseye.nativeapp.data

/**
 * Curated strategic reference infrastructure ported from the web dashboard's
 * military-bases, nuclear-facilities, and ai-datacenters datasets.
 */
object CuratedInfrastructure {

    fun militaryBases(): List<RadarSignal> = listOf(
        base("norfolk", "Norfolk Naval Station", "Atlantic Fleet HQ, largest naval base", 36.95, -76.31, "United States", "us-nato"),
        base("fort_liberty", "Fort Liberty (Fort Bragg)", "Special Operations, XVIII Airborne Corps", 35.14, -79.00, "United States", "us-nato"),
        base("pendleton", "Camp Pendleton", "1st Marine Division, Pacific Amphibious HQ", 33.38, -117.40, "United States", "us-nato"),
        base("san_diego", "Naval Base San Diego", "Pacific Fleet carrier & surface strike homeport", 32.68, -117.13, "United States", "us-nato"),
        base("nellis", "Nellis Air Force Base", "Air combat command, Red Flag exercises", 36.24, -115.03, "United States", "us-nato"),
        base("langley", "Langley AFB", "Air Combat Command HQ, F-22 Raptor wing", 37.08, -76.36, "United States", "us-nato"),
        base("cheyenne", "Cheyenne Mountain Complex", "NORAD & USNORTHCOM nuclear bunker", 38.74, -104.85, "United States", "us-nato"),
        base("peterson", "Peterson Space Force Base", "US Space Command HQ & Space Operations", 38.82, -104.71, "United States", "us-nato"),
        base("kings_bay", "Naval Submarine Base Kings Bay", "Atlantic Ohio-class ballistic nuclear deterrent", 30.80, -81.52, "United States", "us-nato"),
        base("kitsap", "Naval Base Kitsap", "Pacific Trident ballistic missile submarine base", 47.56, -122.66, "United States", "us-nato"),
        base("ramstein", "Ramstein Air Base", "USAFE & NATO Allied Air Command HQ", 49.44, 7.60, "Germany", "us-nato"),
        base("yokosuka", "Yokosuka Naval Base", "US 7th Fleet forward carrier strike homeport", 35.28, 139.67, "Japan", "us-nato"),
        base("kadena", "Kadena Air Base", "Keystone of Pacific air power, fighter wings", 26.35, 127.77, "Japan", "us-nato"),
        base("rota", "Naval Station Rota", "Aegis missile defense destroyers, Atlantic gateway", 36.62, -6.35, "Spain", "us-nato"),
        base("incirlik", "Incirlik Air Base", "NATO strategic deterrent & nuclear storage", 37.00, 35.43, "Turkey", "us-nato"),
        base("al_udeid", "Al Udeid Air Base", "USCENTCOM forward combined air operations", 25.12, 51.31, "Qatar", "us-nato"),
        base("diego_garcia", "Diego Garcia Naval Support", "Indian Ocean strategic bomber & naval facility", -7.31, 72.42, "British Indian Ocean", "us-nato"),
        base("andersen", "Andersen Air Force Base", "Pacific continuous bomber presence hub", 13.58, 144.92, "Guam", "us-nato"),
        base("pine_gap", "Pine Gap Joint Defence Facility", "Geosynchronous satellite signals intelligence", -23.80, 133.74, "Australia", "us-nato"),
        base("lakenheath", "RAF Lakenheath", "USAF F-35A 48th Fighter Wing forward base", 52.41, 0.56, "United Kingdom", "us-nato"),
        base("sigonella", "Naval Air Station Sigonella", "Mediterranean maritime surveillance & P-8 hub", 37.40, 14.92, "Italy", "us-nato"),
        base("kaliningrad", "Kaliningrad Naval Base", "Baltic Fleet HQ, Iskander-M deployment", 54.71, 20.51, "Russia", "russia"),
        base("sevastopol", "Sevastopol Naval Base", "Black Sea Fleet operational port", 44.60, 33.50, "Crimea", "russia"),
        base("vladivostok", "Vladivostok Naval Base", "Pacific Fleet HQ, nuclear submarine fleet", 43.12, 131.90, "Russia", "russia"),
        base("murmansk", "Severomorsk / Murmansk Base", "Northern Fleet HQ, strategic ballistic submarines", 68.97, 33.09, "Russia", "russia"),
        base("tianjin", "Yulin Naval Base", "South China Sea nuclear submarine pens & carriers", 18.22, 109.57, "China", "china"),
    )

    fun nuclearSites(): List<RadarSignal> = listOf(
        nuclear("zaporizhzhia", "Zaporizhzhia Nuclear Power Plant", "Europe's largest NPP (6 VVER-1000 units), front-line contested", 47.51, 34.58, "Ukraine", "Operational (Cold Shutdown)"),
        nuclear("chernobyl", "Chernobyl Nuclear Site", "Disaster exclusion zone, NSC shelter, radioactive storage", 51.39, 30.10, "Ukraine", "Decommissioning / Storage"),
        nuclear("dimona", "Negev Nuclear Research Center (Dimona)", "Undeclared nuclear weapons program & heavy water reactor", 31.00, 35.15, "Israel", "Operational / Strategic"),
        nuclear("natanz", "Natanz Fuel Enrichment Plant", "Primary underground gas centrifuge uranium enrichment facility", 33.72, 51.73, "Iran", "Centrifuge Enrichment"),
        nuclear("fordow", "Fordow Fuel Enrichment Plant", "Deep-underground mountain-fortified uranium enrichment site", 34.88, 50.99, "Iran", "High Enrichment"),
        nuclear("bushehr", "Bushehr Nuclear Power Plant", "Russian-built VVER-1000 commercial power reactor on Gulf coast", 28.83, 50.89, "Iran", "Operational Commercial"),
        nuclear("sellafield", "Sellafield Reprocessing Site", "Nuclear fuel reprocessing, MOX plant & legacy waste storage", 54.42, -3.50, "United Kingdom", "Reprocessing / Decommissioning"),
        nuclear("la_hague", "La Hague Site", "World's largest commercial light-water spent fuel reprocessing plant", 49.68, -1.88, "France", "Spent Fuel Reprocessing"),
        nuclear("fukushima", "Fukushima Daiichi", "TEPCO decommissioning, treated water release, ALPS plant", 37.42, 141.03, "Japan", "Decommissioning"),
        nuclear("bruce", "Bruce Nuclear Generating Station", "Largest fully operational nuclear power plant in North America (8 CANDU units)", 44.33, -81.60, "Canada", "Operational Commercial"),
        nuclear("gravelines", "Gravelines Nuclear Power Station", "6-reactor power station cooling on North Sea coast", 51.01, 2.14, "France", "Operational Commercial"),
        nuclear("hanul", "Hanul Nuclear Power Plant", "Major South Korean PWR complex (6 active reactors)", 37.09, 129.38, "South Korea", "Operational Commercial"),
        nuclear("dimapur_bhabha", "Bhabha Atomic Research Centre (Trombay)", "India's premier nuclear research facility & research reactors", 19.01, 72.92, "India", "Nuclear Research"),
        nuclear("kudankulam", "Kudankulam Nuclear Power Plant", "VVER-1000 reactors operated with Russian Rosatom support", 8.17, 77.71, "India", "Operational Commercial"),
    )

    fun aiDatacenters(): List<RadarSignal> = listOf(
        datacenter("ashburn", "Ashburn Data Center Alley", "World's dense internet hub, ~70% global traffic, hyperscale AI clusters", 39.04, -77.49, "United States", "AWS / Azure / Equinix"),
        datacenter("santa_clara", "Santa Clara / Silicon Valley Cluster", "AI research labs, NVIDIA, Meta, Google TPU & GPU development farms", 37.35, -121.95, "United States", "NVIDIA / CoreWeave"),
        datacenter("hillsboro", "Hillsboro Subsea & AI Cluster", "Transpacific fiber landing, Intel & hyperscale compute campus", 45.52, -122.99, "United States", "Hyperscale Compute"),
        datacenter("council_bluffs", "Council Bluffs Google AI Campus", "One of the world's largest Google AI & Gemini TPU training clusters", 41.26, -95.86, "United States", "Google Gemini TPU"),
        datacenter("phoenix", "Phoenix-Mesa Megacampus", "Major expansion hub for Microsoft OpenAI & Meta AI compute", 33.42, -111.83, "United States", "Microsoft / OpenAI"),
        datacenter("dublin", "Dublin Tech Hub (Grange Castle)", "European hyperscale crossroads for Microsoft, Google, AWS AI workloads", 53.32, -6.44, "Ireland", "AWS / Microsoft / Google"),
        datacenter("frankfurt", "Frankfurt DE-CIX Campus", "Continental Europe's largest internet exchange & AI cloud cluster", 50.11, 8.68, "Germany", "Equinix / Interxion"),
        datacenter("tokyo", "Tokyo Inzai Data Center Park", "Asia-Pacific primary hyperscale campus, LLM training & inference", 35.82, 140.14, "Japan", "NTT / Equinix / AWS"),
        datacenter("singapore", "Singapore Jurong Island AI Hub", "Southeast Asia gateway compute & sovereign AI model infrastructure", 1.28, 103.70, "Singapore", "Singtel / Equinix / AWS"),
        datacenter("the_dalles", "The Dalles Columbia Hydro AI Campus", "Massive hydro-powered Google AI data center campus", 45.60, -121.18, "United States", "Google AI Hydro"),
    )

    private fun base(id: String, name: String, desc: String, lat: Double, lng: Double, country: String, type: String): RadarSignal =
        RadarSignal(
            id = "mil_base:$id",
            layer = RadarLayer.MILITARY,
            title = name,
            locationName = "$country · ${formatCoord(lat)}, ${formatCoord(lng)}",
            lat = lat,
            lng = lng,
            observedAt = "Permanent",
            severity = "Strategic Base",
            summary = desc,
            provider = "GodsEye Military Reference",
            coverage = "Curated global installation",
            isSimulated = false,
            rank = 4.2,
        )

    private fun nuclear(id: String, name: String, desc: String, lat: Double, lng: Double, country: String, status: String): RadarSignal =
        RadarSignal(
            id = "nuclear:$id",
            layer = RadarLayer.NUCLEAR,
            title = name,
            locationName = "$country · ${formatCoord(lat)}, ${formatCoord(lng)}",
            lat = lat,
            lng = lng,
            observedAt = "Active Facility",
            severity = status,
            summary = desc,
            provider = "IAEA / Nuclear Watch",
            coverage = "Global nuclear asset monitor",
            isSimulated = false,
            rank = 4.3,
        )

    fun satellites(): List<RadarSignal> = listOf(
        satellite("25544", "ISS (ZARYA)", "Space Station", "International", 418.0, 7.66, 51.64, 92.9, 0.0),
        satellite("48274", "TIANGONG (CSS)", "Space Station", "China", 390.0, 7.68, 41.47, 92.2, 45.0),
        satellite("51445", "USA 326 (NROL-87)", "Optical ISR", "United States", 512.0, 7.61, 97.4, 94.8, 90.0),
        satellite("48247", "USA 314 (NROL-82)", "Optical ISR", "United States", 580.0, 7.56, 97.9, 96.2, 135.0),
        satellite("53323", "COSMOS 2558", "Military ISR", "Russia", 445.0, 7.65, 97.4, 93.5, 180.0),
        satellite("44797", "COSMOS 2542", "Inspector Satellite", "Russia", 420.0, 7.66, 97.9, 93.0, 225.0),
        satellite("49495", "YAOGAN-35 01A", "SAR Radar ISR", "China", 498.0, 7.62, 35.0, 94.6, 270.0),
        satellite("57655", "GAOFEN-12 04", "High-Res Radar", "China", 600.0, 7.55, 98.0, 96.7, 315.0),
        satellite("39634", "SENTINEL-1A", "SAR Radar Earth Obs", "European Union", 693.0, 7.51, 98.18, 98.6, 30.0),
        satellite("40697", "SENTINEL-2A", "Optical Multispectral", "European Union", 786.0, 7.46, 98.62, 100.6, 75.0),
        satellite("31698", "TERRASAR-X", "Commercial Radar ISR", "Germany", 514.0, 7.61, 97.44, 94.9, 120.0),
        satellite("40115", "WORLDVIEW-3", "0.3m Commercial Optical", "United States", 617.0, 7.54, 97.98, 97.0, 165.0),
        satellite("44804", "CARTOSAT-3", "High-Res Optical ISR", "India", 505.0, 7.61, 97.5, 94.7, 210.0),
        satellite("43145", "TOPAZ-5", "Radar Reconnaissance", "United States", 1100.0, 7.30, 106.0, 107.0, 255.0),
        satellite("39227", "KOMPSAT-5", "SAR Reconnaissance", "South Korea", 550.0, 7.58, 97.6, 95.6, 300.0),
        satellite("41848", "SKYSAT-C1", "Constellation Optical", "United States", 450.0, 7.65, 97.0, 93.6, 345.0),
    )

    private fun satellite(
        noradId: String,
        name: String,
        type: String,
        country: String,
        altitudeKm: Double,
        velocityKmPerSec: Double,
        inclinationDeg: Double,
        periodMinutes: Double,
        phaseDeg: Double,
    ): RadarSignal {
        val sat = SatelliteObservation(
            noradId = noradId,
            name = name,
            type = type,
            country = country,
            altitudeKm = altitudeKm,
            velocityKmPerSec = velocityKmPerSec,
            inclinationDeg = inclinationDeg,
            periodMinutes = periodMinutes,
            initialPhaseDeg = phaseDeg,
            initialEpochSeconds = 1_793_980_000L,
        )
        val initialPos = SatelliteMotion.position(sat, 1_793_980_000.0)
        return RadarSignal(
            id = "satellite:$noradId",
            layer = RadarLayer.SATELLITES,
            title = "Satellite $name",
            locationName = "$country · ${altitudeKm.toInt()} km LEO",
            lat = initialPos.lat,
            lng = initialPos.lng,
            observedAt = "Orbital Epoch",
            severity = type,
            summary = "$name ($country) in ${altitudeKm.toInt()} km orbit at $velocityKmPerSec km/s. Inclination: $inclinationDeg°, Period: $periodMinutes min.",
            satellite = sat,
            provider = "CelesTrak / Space-Track",
            coverage = "Live orbital trajectory",
            isSimulated = false,
            rank = 4.5,
        )
    }

    private fun datacenter(id: String, name: String, desc: String, lat: Double, lng: Double, country: String, operator: String): RadarSignal =
        RadarSignal(
            id = "datacenter:$id",
            layer = RadarLayer.DATACENTERS,
            title = name,
            locationName = "$country · ${formatCoord(lat)}, ${formatCoord(lng)}",
            lat = lat,
            lng = lng,
            observedAt = "Online Cluster",
            severity = operator,
            summary = desc,
            provider = "Cloud / AI Infrastructure Monitor",
            coverage = "Global AI compute clusters",
            isSimulated = false,
            rank = 3.8,
        )

    private fun formatCoord(value: Double): String = String.format(java.util.Locale.ROOT, "%.2f°", value)
}
