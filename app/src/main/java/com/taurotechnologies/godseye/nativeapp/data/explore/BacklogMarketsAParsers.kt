package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.JsonElement
import java.util.Locale

private const val BACKLOG_MARKETS_A_MAX_ROWS = 25

/** Parsers for backlog Explore panels: AAII sentiment, Big Mac, market breadth, China macro and consumer prices. */
internal val BACKLOG_MARKETS_A_PARSERS: Map<String, ExploreParser> = mapOf(
    "aaii-sentiment" to ExploreParser("aaiiSentiment", BootstrapTier.SLOW) { element -> aaiiSentimentPanel(element) },
    "bigmac" to ExploreParser("bigmac", BootstrapTier.SLOW) { element -> bigMacPanel(element) },
    "market-breadth" to ExploreParser("breadthHistory", BootstrapTier.SLOW) { element -> marketBreadthPanel(element) },
    "consumer-prices" to ExploreParser("consumerPricesOverview", BootstrapTier.SLOW) { element -> consumerPricesOverviewPanel(element) },
)

/** AAII weekly survey: `{ latest: { date, bullish, bearish, neutral, spread }, weeks: [...] }`. */
private fun aaiiSentimentPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    val latest = root?.get("latest").obj()
    val spread = latest.num("spread")?.takeIf { it.isFinite() }
    val summary = listOfNotNull(
        spread?.let { aaiiSentimentLabel(it) },
        formatPercent(spread)?.let { "spread $it" },
        latest.str("date")?.let { "as of $it" },
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val rows = root.list("weeks").mapNotNull { week ->
        val date = week.str("date") ?: return@mapNotNull null
        val weekSpread = week.num("spread")?.takeIf { it.isFinite() }
        ExploreRow(
            title = date,
            subtitle = listOfNotNull(
                plainPercent(week.num("bullish"))?.let { "Bull $it" },
                plainPercent(week.num("bearish"))?.let { "Bear $it" },
            ).joinToString(" · ").takeIf { it.isNotEmpty() },
            value = formatPercent(weekSpread),
            change = weekSpread,
        )
    }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_MARKETS_A_MAX_ROWS))
}

/** Bull-bear spread bands, matching the web panel's labels. */
private fun aaiiSentimentLabel(spread: Double): String = when {
    spread <= -20 -> "Extreme bearish"
    spread <= -10 -> "Bearish"
    spread < 0 -> "Mildly bearish"
    spread < 10 -> "Neutral"
    spread < 20 -> "Bullish"
    else -> "Extreme bullish"
}

/** Big Mac prices: `{ countries: [{ code, name, flag, currency, usdPrice, wowPct }], cheapestCountry, mostExpensiveCountry, wowAvgPct, wowAvailable }`. */
private fun bigMacPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    val summary = listOfNotNull(
        root.str("cheapestCountry")?.let { "Cheapest $it" },
        root.str("mostExpensiveCountry")?.let { "Priciest $it" },
        if (root.bool("wowAvailable") == true) formatPercent(root.num("wowAvgPct"))?.let { "avg WoW $it" } else null,
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val rows = root.list("countries")
        .mapNotNull { country ->
            val usd = country.num("usdPrice")?.takeIf { it.isFinite() && it > 0 } ?: return@mapNotNull null
            country to usd
        }
        .sortedByDescending { it.second }
        .mapNotNull { (country, usd) ->
            val name = country.str("name") ?: return@mapNotNull null
            ExploreRow(
                title = listOfNotNull(country.str("flag"), name).joinToString(" "),
                subtitle = country.str("currency"),
                value = formatPrice(usd)?.let { "\$$it" },
                change = country.num("wowPct")?.takeIf { it.isFinite() },
            )
        }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_MARKETS_A_MAX_ROWS))
}

/** Share of S&P 500 stocks above their moving averages: `{ current..., history: [{ date, pctAbove20d, ... }] }`. */
private fun marketBreadthPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    if (root.bool("unavailable") == true) return PanelContent(summary = "Breadth data unavailable.")
    val current = root?.get("current").obj()
    val pct20 = root.num("currentPctAbove20d") ?: current.num("pctAbove20d")
    val pct50 = root.num("currentPctAbove50d") ?: current.num("pctAbove50d")
    val pct200 = root.num("currentPctAbove200d") ?: current.num("pctAbove200d")
    val summary = listOfNotNull(
        plainPercent(pct20)?.let { "20d $it" },
        plainPercent(pct50)?.let { "50d $it" },
        plainPercent(pct200)?.let { "200d $it" },
    ).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { "Above moving average: $it" }
    // ISO dates sort as text; show the most recent sessions first regardless of payload order.
    val rows = root.list("history").mapNotNull { entry ->
        val date = entry.str("date") ?: return@mapNotNull null
        ExploreRow(
            title = date,
            subtitle = listOfNotNull(
                plainPercent(entry.num("pctAbove50d"))?.let { "50d $it" },
                plainPercent(entry.num("pctAbove200d"))?.let { "200d $it" },
            ).joinToString(" · ").takeIf { it.isNotEmpty() },
            value = plainPercent(entry.num("pctAbove20d")),
        )
    }
    val chart = lineChart(
        "Share above 20-day average",
        root.list("history").map { (it.str("date") ?: "") to it.num("pctAbove20d") },
    ) { String.format(Locale.US, "%.1f%%", it) }
    return PanelContent(summary = summary, rows = rows.sortedByDescending { it.title }.take(BACKLOG_MARKETS_A_MAX_ROWS), chart = chart)
}

/** Consumer price overview: `{ essentialsIndex, valueBasketIndex, momPct, topCategories: [...], upstreamUnavailable }`. */
private fun consumerPricesOverviewPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    if (root.bool("upstreamUnavailable") == true) return PanelContent(summary = "Consumer price data unavailable.")
    val summary = listOfNotNull(
        formatPrice(root.num("essentialsIndex"))?.let { "Essentials $it" },
        formatPrice(root.num("valueBasketIndex"))?.let { "Value basket $it" },
        formatPercent(root.num("momPct"))?.let { "MoM $it" },
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val rows = root.list("topCategories").mapNotNull { category ->
        val title = category.str("name") ?: return@mapNotNull null
        val items = category.num("itemCount")?.takeIf { it.isFinite() }?.toLong()
        ExploreRow(
            title = title,
            subtitle = items?.let { "$it items" },
            value = formatPrice(category.num("currentIndex")),
            change = category.num("wowPct")?.takeIf { it.isFinite() },
        )
    }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_MARKETS_A_MAX_ROWS))
}

/** One decimal place with a percent sign and no sign prefix, for shares and levels. */
private fun plainPercent(value: Double?): String? =
    if (value == null || !value.isFinite()) null else String.format(Locale.US, "%.1f%%", value)
