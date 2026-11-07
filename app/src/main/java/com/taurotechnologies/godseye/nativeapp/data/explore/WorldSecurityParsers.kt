package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.util.Locale

private const val MAX_ROWS = 60
private const val DOT = " · "

/** Explore parsers for the world and security panels. Every parser is null-safe and skips incomplete rows. */
internal val WORLD_SECURITY_PARSERS: Map<String, ExploreParser> = mapOf(
    "gdelt-intel" to ExploreParser("gdeltIntel", BootstrapTier.FAST, ::parseGdeltIntel),
    "strategic-posture" to ExploreParser("theaterPosture", BootstrapTier.FAST, ::parseTheaterPosture),
    "cii" to ExploreParser("riskScores", BootstrapTier.FAST, ::parseRiskScores),
    "security-advisories" to ExploreParser("securityAdvisories", BootstrapTier.SLOW, ::parseSecurityAdvisories),
    "sanctions-pressure" to ExploreParser("sanctionsPressure", BootstrapTier.SLOW, ::parseSanctionsPressure),
    "airline-intel" to ExploreParser("flightDelays", BootstrapTier.ON_DEMAND, ::parseAirlineIntel),
)

/** "iran-theater" -> "Iran Theater"; null stays null. */
private fun humanSlug(value: String?): String? = value
    ?.replace('-', ' ')
    ?.split(' ')
    ?.filter { it.isNotBlank() }
    ?.joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.ROOT) } }
    ?.ifEmpty { null }

private fun parseGdeltIntel(element: JsonElement): PanelContent {
    val rows = element.obj().list("topics").flatMap { topic ->
        val topicLabel = humanSlug(topic.str("id"))
        topic.list("articles").mapNotNull { article ->
            val title = article.str("title") ?: return@mapNotNull null
            ExploreRow(title = title, subtitle = topicLabel, url = article.str("url"))
        }
    }.take(MAX_ROWS)
    return PanelContent(rows = rows)
}

private fun parseTheaterPosture(element: JsonElement): PanelContent {
    val rows = element.obj().list("theaters").mapNotNull { theater ->
        val name = humanSlug(theater.str("theater")) ?: return@mapNotNull null
        val flights = theater.long("activeFlights") ?: 0L
        val vessels = theater.long("trackedVessels") ?: 0L
        ExploreRow(
            title = name,
            subtitle = "$flights flights$DOT$vessels vessels",
            value = humanEnum(theater.str("postureLevel")),
        )
    }
    return PanelContent(rows = rows)
}

/**
 * Best-effort parser for country risk scores. The feed is currently null, so this accepts either
 * a list of objects (name/country plus score) or a map of key to score or to an object with a score.
 */
private fun parseRiskScores(element: JsonElement): PanelContent {
    val scored: List<Pair<String, Double?>> = element.arr()?.mapNotNull { item ->
        val entry = item.obj() ?: return@mapNotNull null
        val name = entry.str("countryName") ?: entry.str("country") ?: entry.str("countryCode")
            ?: entry.str("name") ?: return@mapNotNull null
        val score = entry.num("score") ?: entry.num("riskScore") ?: entry.num("cii") ?: entry.num("value")
        name to score
    } ?: element.obj()?.entries?.mapNotNull { (key, value) ->
        val score = (value as? JsonPrimitive)?.doubleOrNull ?: value.obj()?.num("score")
            ?: value.obj()?.num("riskScore") ?: value.obj()?.num("cii")
        if (key.isBlank()) null else key to score
    } ?: emptyList()

    val rows = scored
        .sortedWith(compareByDescending<Pair<String, Double?>> { it.second ?: Double.NEGATIVE_INFINITY })
        .take(MAX_ROWS)
        .map { (name, score) ->
            ExploreRow(
                title = name,
                value = score?.takeIf { it.isFinite() }?.let { String.format(Locale.US, "%.1f", it) },
            )
        }
    return PanelContent(rows = rows)
}

private fun parseSecurityAdvisories(element: JsonElement): PanelContent {
    val rows = element.obj().list("advisories").mapNotNull { advisory ->
        val title = advisory.str("title") ?: return@mapNotNull null
        val subtitle = listOfNotNull(advisory.str("source"), advisory.str("level"))
            .joinToString(DOT)
            .ifEmpty { null }
        ExploreRow(title = title, subtitle = subtitle, url = advisory.str("link"))
    }.take(MAX_ROWS)
    return PanelContent(rows = rows)
}

private fun parseSanctionsPressure(element: JsonElement): PanelContent {
    val root = element.obj()
    val total = root.long("totalCount")
    val newCount = root.long("newEntryCount") ?: 0L
    val summary = total?.let { "$it listed$DOT$newCount new" }
    val rows = root.list("countries").mapNotNull { country ->
        val name = country.str("countryName") ?: return@mapNotNull null
        val vessels = country.long("vesselCount") ?: 0L
        val aircraft = country.long("aircraftCount") ?: 0L
        val entries = country.long("entryCount") ?: 0L
        ExploreRow(
            title = name,
            subtitle = "$vessels vessels$DOT$aircraft aircraft",
            value = "$entries entries",
        )
    }
    return PanelContent(summary = summary, rows = rows)
}

private data class DisruptedAirport(
    val iata: String,
    val name: String,
    val subtitle: String?,
    val value: String,
    val delayMins: Double?,
    val severityWeight: Int,
)

internal fun parseAirlineIntel(element: JsonElement): PanelContent {
    val root = element.obj()
    val alerts = root.list("alerts")
    if (alerts.isEmpty()) return PanelContent(summary = "No airport disruption alerts reported.")

    fun severityRank(severity: String?): Int = when {
        severity == null -> 0
        severity.contains("SEVERE") -> 4
        severity.contains("MODERATE") -> 3
        severity.contains("MINOR") -> 2
        severity.contains("NORMAL") -> 1
        else -> 0
    }

    val items = alerts.mapNotNull { item ->
        val iata = item.str("iata") ?: item.str("icao") ?: return@mapNotNull null
        val name = item.str("name") ?: iata
        val city = item.str("city")
        val country = item.str("country")
        val rawSev = item.str("severity")
        val severity = humanEnum(rawSev, "FLIGHT_DELAY_SEVERITY_") ?: "Unknown"
        val delayType = humanEnum(item.str("delayType"), "FLIGHT_DELAY_TYPE_")
        val delayMins = item.num("avgDelayMinutes")?.takeIf { it > 0 }
        val reason = item.str("reason")?.takeIf { it.isNotBlank() && !it.equals("none", ignoreCase = true) }

        val loc = listOfNotNull(city, country).joinToString(", ")
        val detail = reason ?: delayType
        val subtitle = listOfNotNull(loc.ifBlank { null }, detail).joinToString(DOT).ifBlank { null }
        val value = when {
            delayMins != null -> "${delayMins.toInt()}m delay"
            severity != "Normal" -> severity
            else -> "On schedule"
        }
        DisruptedAirport(
            iata = iata,
            name = name,
            subtitle = subtitle,
            value = value,
            delayMins = delayMins,
            severityWeight = severityRank(rawSev),
        )
    }

    val sorted = items.sortedWith(
        compareByDescending<DisruptedAirport> { it.severityWeight }
            .thenByDescending { it.delayMins ?: 0.0 }
            .thenBy { it.iata },
    )

    val disruptedCount = sorted.count { it.severityWeight >= 2 }
    val severeCount = sorted.count { it.severityWeight == 4 }
    val summary = buildList {
        add("${alerts.size} airports monitored")
        if (disruptedCount > 0) add("$disruptedCount disrupted")
        if (severeCount > 0) add("$severeCount ground stops")
    }.joinToString(DOT)

    val chartPoints = sorted
        .filter { it.delayMins != null && it.delayMins > 0 }
        .take(MAX_BARS)
        .map { "${it.iata} ${it.name.take(12)}" to it.delayMins }

    val chart = rankedBarChart("Average delay (mins)", chartPoints) { "${it.toInt()}m" }

    val rows = sorted.take(MAX_ROWS).map {
        ExploreRow(title = "${it.iata} · ${it.name}", subtitle = it.subtitle, value = it.value)
    }

    return PanelContent(summary = summary, rows = rows, chart = chart)
}
