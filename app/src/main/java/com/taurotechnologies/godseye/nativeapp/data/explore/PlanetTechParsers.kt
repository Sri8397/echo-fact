package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.Locale

/** Parsers for the Planet, Climate and Tech Explore panels. Each one is null-safe and skips incomplete rows. */
internal val PLANET_TECH_PARSERS: Map<String, ExploreParser> = mapOf(
    "climate" to ExploreParser("climateAnomalies", BootstrapTier.SLOW, ::parsePlanetClimate),
    "satellite-fires" to ExploreParser("wildfires", BootstrapTier.SLOW, ::parsePlanetWildfires),
    "radiation-watch" to ExploreParser("radiationWatch", BootstrapTier.SLOW, ::parsePlanetRadiation),
    "thermal-escalation" to ExploreParser("thermalEscalation", BootstrapTier.SLOW, ::parsePlanetThermal),
    "positive-feed" to ExploreParser("positiveGeoEvents", BootstrapTier.FAST, ::parsePlanetPositive),
    "progress" to ExploreParser("progressData", BootstrapTier.SLOW, ::parsePlanetProgress),
    "renewable-energy" to ExploreParser("renewableEnergy", BootstrapTier.SLOW, ::parsePlanetRenewable),
    "events" to ExploreParser("techEvents", BootstrapTier.SLOW, ::parsePlanetTechEvents),
    "service-status" to ExploreParser("serviceStatuses", BootstrapTier.FAST, ::parsePlanetServiceStatus),
    "tech-readiness" to ExploreParser("techReadiness", BootstrapTier.SLOW, ::parsePlanetTechReadiness),
    "insights" to ExploreParser("insights", BootstrapTier.FAST, ::parsePlanetInsights),
)

private fun parsePlanetClimate(element: JsonElement): PanelContent {
    val rows = element.obj().list("anomalies").mapNotNull { row ->
        val title = row.str("zone") ?: return@mapNotNull null
        val delta = row.num("tempDelta")
        ExploreRow(
            title = title,
            subtitle = planetJoin(humanEnum(row.str("type"), "ANOMALY_TYPE_"), row.str("period")),
            value = delta?.let { String.format(Locale.US, "%+.1f°C", it) },
            change = delta,
        )
    }
    return PanelContent(rows = rows)
}

private fun parsePlanetWildfires(element: JsonElement): PanelContent {
    val rows = element.obj().list("fireDetections")
        .filter { it.str("region") != null }
        .sortedByDescending { it.num("frp") ?: 0.0 }
        .take(60)
        .mapNotNull { row ->
            val title = row.str("region") ?: return@mapNotNull null
            ExploreRow(
                title = title,
                subtitle = humanEnum(row.str("confidence"), "FIRE_CONFIDENCE_"),
                value = row.num("frp")?.let { "FRP ${planetOne(it)}" },
            )
        }
    return PanelContent(rows = rows)
}

private fun parsePlanetRadiation(element: JsonElement): PanelContent {
    val obj = element.obj()
    val summary = listOfNotNull(
        obj.long("anomalyCount")?.let { "$it anomalies" },
        obj.long("elevatedCount")?.let { "$it elevated" },
        obj.long("spikeCount")?.let { "$it spikes" },
    ).takeIf { it.isNotEmpty() }?.joinToString(" · ")
    val rows = obj.list("observations").mapNotNull { row ->
        val title = row.str("locationName") ?: return@mapNotNull null
        val value = row.num("value")
        ExploreRow(
            title = title,
            subtitle = row.str("country"),
            value = value?.let { listOfNotNull(planetTrim(it), row.str("unit")).joinToString(" ") },
        )
    }
    return PanelContent(summary = summary, rows = rows)
}

private fun parsePlanetThermal(element: JsonElement): PanelContent {
    val rows = element.obj().list("clusters").mapNotNull { row ->
        val title = row.str("regionLabel") ?: row.str("countryName") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = planetJoin(
                humanEnum(row.str("status"), "THERMAL_STATUS_"),
                row.long("observationCount")?.let { "$it detections" },
            ),
            value = row.num("maxFrp")?.let { "FRP ${planetOne(it)}" },
        )
    }
    return PanelContent(rows = rows)
}

private fun parsePlanetPositive(element: JsonElement): PanelContent {
    val rows = element.obj().list("events").mapNotNull { row ->
        val title = row.str("name") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = humanEnum(row.str("category")?.replace('-', '_')),
            value = row.long("count")?.let { "$it reports" },
        )
    }
    return PanelContent(rows = rows)
}

private fun parsePlanetProgress(element: JsonElement): PanelContent {
    val rows = planetRows(element).mapNotNull { indicator ->
        val id = indicator.str("id") ?: return@mapNotNull null
        val series = indicator.list("data").mapNotNull { point ->
            val year = point.long("year") ?: return@mapNotNull null
            val value = point.num("value") ?: return@mapNotNull null
            year to value
        }.sortedBy { it.first }
        if (series.isEmpty()) return@mapNotNull null
        val first = series.first()
        val last = series.last()
        ExploreRow(
            title = planetCamel(id),
            subtitle = "since ${first.first}",
            value = planetOne(last.second),
            change = last.second - first.second,
        )
    }
    return PanelContent(rows = rows)
}

private fun parsePlanetRenewable(element: JsonElement): PanelContent {
    val obj = element.obj()
    val percentage = obj.num("globalPercentage")
    val globalYear = obj.long("globalYear")
    val summary = percentage?.let {
        "${planetOne(it)}% of global energy" + (globalYear?.let { year -> " ($year)" }.orEmpty())
    }
    val rows = obj.list("regions").mapNotNull { row ->
        val title = row.str("name") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = row.long("year")?.let { "Share in $it" },
            value = row.num("percentage")?.let { "${planetOne(it)}%" },
        )
    }
    return PanelContent(summary = summary, rows = rows)
}

private fun parsePlanetTechEvents(element: JsonElement): PanelContent {
    val rows = element.obj().list("events").mapNotNull { row ->
        val title = row.str("title") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = planetJoin(row.str("location"), row.str("startDate")),
            url = row.str("url"),
        )
    }.take(60)
    return PanelContent(rows = rows)
}

private fun parsePlanetServiceStatus(element: JsonElement): PanelContent {
    val rows = planetRows(element).mapNotNull { row ->
        val title = row.str("name") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = row.str("description"),
            value = humanEnum(row.str("status"), "SERVICE_OPERATIONAL_STATUS_"),
        )
    }
    return PanelContent(rows = rows)
}

private fun parsePlanetTechReadiness(element: JsonElement): PanelContent {
    val rows = planetRows(element)
        .sortedBy { it.num("rank") ?: Double.MAX_VALUE }
        .mapNotNull { row ->
            val title = row.str("countryName") ?: return@mapNotNull null
            ExploreRow(
                title = title,
                subtitle = row.num("rank")?.let { "rank ${planetTrim(it)}" },
                value = row.num("score")?.let { "score ${planetTrim(it)}" },
            )
        }
        .take(60)
    return PanelContent(rows = rows)
}

private fun parsePlanetInsights(element: JsonElement): PanelContent {
    val obj = element.obj()
    val summary = obj.str("worldBrief")?.let { planetText(it) }
    val storyLines = obj.list("briefStoryLines").mapNotNull { line ->
        val text = line.str("text")?.let { planetText(it) }?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        ExploreRow(title = text)
    }
    val topStories = obj.list("topStories").mapNotNull { story ->
        val title = story.str("primaryTitle")?.let { planetText(it) }?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = story.str("primarySource"),
            url = story.str("primaryLink"),
        )
    }
    return PanelContent(summary = summary, rows = storyLines + topStories)
}

/** Rows of a dataset that is published as a top-level array. */
private fun planetRows(element: JsonElement): List<JsonObject> =
    element.arr()?.mapNotNull { it as? JsonObject }.orEmpty()

/** Joins the non-blank parts with a middle dot, or returns null when nothing is left. */
private fun planetJoin(vararg parts: String?): String? =
    parts.filterNotNull().filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotEmpty() }

/** One decimal place, US locale. */
private fun planetOne(value: Double): String = String.format(Locale.US, "%.1f", value)

/** Whole numbers print without decimals; everything else keeps two. */
private fun planetTrim(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)

/** "lifeExpectancy" becomes "Life expectancy". */
private fun planetCamel(id: String): String = id
    .replace(Regex("([a-z0-9])([A-Z])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
    .lowercase(Locale.ROOT)
    .replaceFirstChar { it.titlecase(Locale.ROOT) }

/** Normalises non-breaking characters, drops citation markers such as "[1]" and collapses whitespace. */
private fun planetText(value: String): String = value
    .replace(' ', ' ')
    .replace('‑', '-')
    .replace(Regex("\\s*\\[\\d+]"), "")
    .replace(Regex("\\s+"), " ")
    .trim()
