package com.just_sri.suvidha.radar.data

import kotlinx.serialization.Serializable

@Serializable
data class TimelineStep(
    val year: String = "",
    val phase: String = "",
    val event: String = "",
)

@Serializable
data class Source(
    val date: String = "",
    val publisher: String = "",
    val title: String = "",
    val url: String = "",
)

@Serializable
data class Incident(
    val id: String,
    val title: String,
    val locationName: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val date: String = "",
    val year: String = "",
    val summary: String = "",
    val category: String = "",
    val currentReality: String = "",
    val hypeTimeline: List<TimelineStep> = emptyList(),
    val verifiedFacts: List<String> = emptyList(),
    val keyQuestions: List<String> = emptyList(),
    val sources: List<Source> = emptyList(),
)

/** Shown when the backend is offline or has no records (same records the React build shipped). */
val FALLBACK_INCIDENTS: List<Incident> = listOf(
    Incident(
        id = "case-cjpprotestliveu",
        title = "CJP Protest Live Updates: Tear gas shells, stones fly as fresh clashes erupt between protesters and police near Jantar Mantar; 5 cops injured",
        locationName = "Delhi, India",
        lat = 28.6139,
        lng = 77.2090,
        date = "23 Jul 2026",
        year = "2026",
        summary = "In a video message, activist Sonam Wangchuk shared updates on the hunger strike. Heavy security deployed at Jantar Mantar as CJP protest intensifies in Delhi.",
        category = "Geopolitical",
        currentReality = "Ongoing monitoring and deployment by Delhi police.",
        hypeTimeline = listOf(TimelineStep("Current", "Initial Coverage", "Article published on Timesofindia.indiatimes.com.")),
        verifiedFacts = listOf(
            "Reported incident location: Delhi, India (28.6139, 77.2090).",
            "Published source link verified at Timesofindia.indiatimes.com.",
        ),
        keyQuestions = listOf("What further developments are reported by local authorities?"),
        sources = listOf(
            Source("23 Jul 2026", "Times of India", "CJP Protest Live Updates: Clashes near Jantar Mantar", "https://timesofindia.indiatimes.com"),
            Source("23 Jul 2026", "NDTV News", "Heavy Security Deployed At Jantar Mantar", "https://www.ndtv.com"),
        ),
    ),
    Incident(
        id = "case-ever-given",
        title = "Ever Given Suez Canal Blockage Crisis",
        locationName = "Suez Canal, Egypt",
        lat = 30.0176,
        lng = 32.5700,
        date = "23 Mar 2021",
        year = "2021",
        summary = "A 400-meter-long container ship ran aground diagonally across the Suez Canal, holding up $9.6 billion of global trade per day for nearly a week.",
        category = "Geopolitical",
        currentReality = "Canal reopened after salvage operations freed the vessel.",
        hypeTimeline = listOf(TimelineStep("March 2021", "Initial Coverage", "Ship grounded; supply chains disrupted globally.")),
        verifiedFacts = listOf(
            "Blocked ~12% of global shipping trade.",
            "Successfully refloated after 6 days of intensive dredging.",
        ),
        keyQuestions = listOf("What long-term logistics changes resulted from the Suez blockage?"),
        sources = listOf(
            Source("23 Mar 2021", "BBC News", "Suez Canal blocked as huge container ship runs aground", "https://www.bbc.com/news"),
        ),
    ),
    Incident(
        id = "case-fukushima",
        title = "Fukushima ALPS Treated Water Ocean Release",
        locationName = "Fukushima, Japan",
        lat = 37.4214,
        lng = 141.0325,
        date = "24 Aug 2023",
        year = "2023",
        summary = "TEPCO began discharging treated radioactive water from Fukushima Daiichi plant into Pacific Ocean under IAEA oversight.",
        category = "Environmental",
        currentReality = "IAEA real-time monitoring confirms tritium levels remain far below international safety thresholds.",
        hypeTimeline = listOf(TimelineStep("August 2023", "Initial Coverage", "Discharge initiated; regional bans enacted.")),
        verifiedFacts = listOf(
            "ALPS removes 62 radionuclides except tritium.",
            "Tritium is diluted 100-fold prior to discharge.",
        ),
        keyQuestions = listOf("What are the long-term marine ecosystem audits showing?"),
        sources = listOf(
            Source("24 Aug 2023", "Kyodo News", "Japan begins releasing treated nuclear water into Pacific", "https://english.kyodonews.net"),
        ),
    ),
    Incident(
        id = "case-lk99",
        title = "LK-99 Ambient Superconductor Craze",
        locationName = "Seoul, South Korea",
        lat = 37.5665,
        lng = 126.9780,
        date = "22 Jul 2023",
        year = "2023",
        summary = "Researchers claimed room-temperature superconductivity in LK-99. Global replications confirmed it was a lead apatite insulator with Cu2S impurities.",
        category = "Science & Tech",
        currentReality = "Independent international replication resolved LK-99 is not a superconductor.",
        hypeTimeline = listOf(TimelineStep("July 2023", "Initial Coverage", "ArXiv preprints published; viral speculation.")),
        verifiedFacts = listOf(
            "Phase transition caused by Cu2S impurity at 104°C.",
            "Nature paper confirmed structural phase change.",
        ),
        keyQuestions = listOf("How did open science preprints respond to viral claims?"),
        sources = listOf(
            Source("16 Aug 2023", "Nature Journal", "LK-99 is not a superconductor", "https://www.nature.com"),
        ),
    ),
)
