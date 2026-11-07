package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.JsonElement
import java.util.Locale

private const val MAX_ROWS = 25
private const val DOT = " · "

/** Explore parsers for the backlog Planet, Security and Technology panels. Every parser is null-safe. */
internal val BACKLOG_PLANET_SECURITY_PARSERS: Map<String, ExploreParser> = mapOf(
    "climate-news" to ExploreParser("climateNews", BootstrapTier.SLOW, ::parseClimateNews),
    "disease-outbreaks" to ExploreParser("diseaseOutbreaks", BootstrapTier.SLOW, ::parseDiseaseOutbreaks),
    "giving" to ExploreParser("giving", BootstrapTier.SLOW, ::parseGiving),
    "cross-source-signals" to ExploreParser("crossSourceSignals", BootstrapTier.SLOW, ::parseCrossSourceSignals),
    "military-correlation" to ExploreParser("crossStraitActivity", BootstrapTier.SLOW, ::parseCrossStraitActivity),
    "social-velocity" to ExploreParser("socialVelocity", BootstrapTier.FAST, ::parseSocialVelocity),
)

private fun parseClimateNews(element: JsonElement): PanelContent {
    val rows = element.obj().list("items").mapNotNull { item ->
        val title = item.str("title") ?: return@mapNotNull null
        ExploreRow(title = title, subtitle = item.str("sourceName"), url = item.str("url"))
    }.take(MAX_ROWS)
    return PanelContent(summary = rows.takeIf { it.isNotEmpty() }?.let { "${it.size} headlines" }, rows = rows)
}

private fun parseDiseaseOutbreaks(element: JsonElement): PanelContent {
    val outbreaks = element.obj().list("outbreaks")
    val rows = outbreaks.mapNotNull { item ->
        val disease = item.str("disease") ?: return@mapNotNull null
        val cases = item.num("cases")?.takeIf { it > 0 }?.toLong()?.toString()
        val subtitle = listOfNotNull(item.str("location"), item.str("sourceName"), cases?.let { "$it cases" })
            .joinToString(DOT)
            .ifEmpty { null }
        ExploreRow(
            title = disease,
            subtitle = subtitle,
            value = humanEnum(item.str("alertLevel")),
            url = item.str("sourceUrl"),
        )
    }.take(MAX_ROWS)
    val alerts = outbreaks.count { it.str("alertLevel") == "alert" }
    val summary = rows.takeIf { it.isNotEmpty() }?.let { "${outbreaks.size} outbreaks$DOT$alerts alerts" }
    return PanelContent(summary = summary, rows = rows)
}

private fun parseGiving(element: JsonElement): PanelContent {
    val summary = element.obj()?.get("summary").obj()
    val headline = listOfNotNull(
        summary.num("activityIndex")?.takeIf { it.isFinite() }?.let { "Activity ${it.toInt()}" },
        formatNumber(summary.num("estimatedDailyFlowUsd"))?.let { "\$$it per day" },
        humanEnum(summary.str("trend"))?.let { "Trend $it" },
    ).joinToString(DOT).ifEmpty { null }
    val platforms = summary.list("platforms").mapNotNull { platform ->
        val name = humanEnum(platform.str("platform")) ?: return@mapNotNull null
        ExploreRow(
            title = name,
            subtitle = listOfNotNull(
                platform.long("activeCampaignsSampled")?.let { "$it campaigns" },
                platform.long("newCampaigns24h")?.let { "$it new in 24h" },
            ).joinToString(DOT).ifEmpty { null },
            value = formatNumber(platform.num("dailyVolumeUsd"))?.let { "\$$it/day" },
        )
    }
    val categories = summary.list("categories").mapNotNull { category ->
        val name = humanEnum(category.str("category")) ?: return@mapNotNull null
        val trending = if (category.bool("trending") == true) " · Trending" else ""
        ExploreRow(
            title = name,
            subtitle = category.long("activeCampaigns")?.let { "$it active campaigns$trending" }
                ?: trending.removePrefix(DOT).ifEmpty { null },
            value = category.num("share")?.takeIf { it.isFinite() }
                ?.let { String.format(Locale.US, "%.1f%%", it * 100) },
        )
    }
    return PanelContent(summary = headline, rows = (platforms + categories).take(MAX_ROWS))
}

private fun parseCrossSourceSignals(element: JsonElement): PanelContent {
    val signals = element.obj().list("signals")
    val rows = signals.mapNotNull { signal ->
        val title = signal.str("summary")
            ?: humanEnum(signal.str("type"), "CROSS_SOURCE_SIGNAL_TYPE_")
            ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = listOfNotNull(
                signal.str("theater"),
                humanEnum(signal.str("type"), "CROSS_SOURCE_SIGNAL_TYPE_"),
                signal.long("signalCount")?.let { "$it signals" },
            ).joinToString(DOT).ifEmpty { null },
            value = humanEnum(signal.str("severity"), "CROSS_SOURCE_SIGNAL_SEVERITY_"),
        )
    }.take(MAX_ROWS)
    val composite = element.obj().long("compositeCount")
    val summary = rows.takeIf { it.isNotEmpty() }?.let {
        listOfNotNull("${signals.size} signals", composite?.let { "$it composite" }).joinToString(DOT)
    }
    return PanelContent(summary = summary, rows = rows)
}

/** Taiwan MND reporting days from the official cross-strait activity snapshot, newest first. */
private fun parseCrossStraitActivity(element: JsonElement): PanelContent {
    val root = element.obj()
    val summary = humanEnum(root.str("status"))?.let { "Official activity: $it" }
    val rows = root.list("observations")
        .filter { it.str("sourceId") == "taiwan-mnd" }
        .sortedByDescending { it.str("reportingDay") ?: "" }
        .mapNotNull { observation ->
            val day = observation.str("reportingDay") ?: return@mapNotNull null
            val categories = observation["categories"].obj()
            ExploreRow(
                title = "Taiwan MND$DOT$day",
                subtitle = listOfNotNull(
                    categories.long("planShips")?.let { "$it PLA navy ships" },
                    categories.long("medianLineCrossings")?.let { "$it median-line crossings" },
                    categories.long("adizEntries")?.let { "$it ADIZ entries" },
                ).joinToString(DOT).ifEmpty { null },
                value = categories.long("plaAircraftSorties")?.let { "$it aircraft" },
                url = observation.str("sourceUrl"),
            )
        }.take(MAX_ROWS)
    return PanelContent(summary = summary, rows = rows)
}

private fun parseSocialVelocity(element: JsonElement): PanelContent {
    val rows = element.obj().list("posts")
        .sortedByDescending { it.num("velocityScore") ?: Double.NEGATIVE_INFINITY }
        .mapNotNull { post ->
            val title = post.str("title") ?: return@mapNotNull null
            ExploreRow(
                title = title,
                subtitle = listOfNotNull(
                    post.str("subreddit")?.let { "r/$it" },
                    post.long("numComments")?.let { "$it comments" },
                ).joinToString(DOT).ifEmpty { null },
                value = formatNumber(post.num("velocityScore")),
                url = post.str("url"),
            )
        }.take(MAX_ROWS)
    return PanelContent(rows = rows)
}
