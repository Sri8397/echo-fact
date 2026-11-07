package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.JsonElement
import java.util.Locale

private const val BACKLOG_C_MAX_ROWS = 25

/** Parsers for backlog Explore panels: grocery basket, Gulf quotes, 24/7 positioning, oil stocks, debt, predictions and shipping. */
internal val BACKLOG_MARKETS_C_PARSERS: Map<String, ExploreParser> = mapOf(
    "grocery-basket" to ExploreParser("groceryBasket", BootstrapTier.SLOW) { element -> backlogGroceryBasketPanel(element) },
    "gulf-economies" to ExploreParser("gulfQuotes", BootstrapTier.SLOW) { element -> backlogGulfQuotesPanel(element) },
    "positioning-247" to ExploreParser("hyperliquidFlow", BootstrapTier.SLOW) { element -> backlogHyperliquidPanel(element) },
    "energy-complex" to ExploreParser("oilStocksAnalysis", BootstrapTier.SLOW) { element -> backlogOilStocksPanel(element) },
    "national-debt" to ExploreParser("nationalDebt", BootstrapTier.SLOW) { element -> backlogNationalDebtPanel(element) },
    "polymarket" to ExploreParser("predictions", BootstrapTier.FAST) { element -> backlogPredictionsPanel(element) },
    "supply-chain" to ExploreParser("shippingRates", BootstrapTier.FAST) { element -> backlogShippingRatesPanel(element) },
)

/** Grocery basket: `{ countries: [{ name, currency, totalUsd, wowPct }], cheapestCountry, mostExpensiveCountry, wowAvgPct, wowAvailable }`. */
private fun backlogGroceryBasketPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    val wow = if (root.bool("wowAvailable") == true) formatPercent(root.num("wowAvgPct")) else null
    val summary = listOfNotNull(
        root.str("cheapestCountry")?.let { "Cheapest $it" },
        root.str("mostExpensiveCountry")?.let { "Priciest $it" },
        wow?.let { "$it week on week" },
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val rows = root.list("countries").mapNotNull { country ->
        val title = country.str("name") ?: country.str("code") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = country.str("currency"),
            value = formatPrice(country.num("totalUsd"))?.let { "\$$it" },
            change = country.num("wowPct")?.takeIf { it.isFinite() },
        )
    }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_C_MAX_ROWS))
}

/** Gulf quotes: `{ quotes: [{ name, country, type, price, change }], rateLimited }`. */
private fun backlogGulfQuotesPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    val summary = if (root.bool("rateLimited") == true) "Rate limited, showing the latest readings" else null
    val rows = root.list("quotes").mapNotNull { quote ->
        val title = quote.str("name") ?: quote.str("symbol") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = listOfNotNull(quote.str("country"), humanEnum(quote.str("type")))
                .joinToString(" · ")
                .takeIf { it.isNotEmpty() },
            value = formatPrice(quote.num("price")),
            change = quote.num("change")?.takeIf { it.isFinite() },
        )
    }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_C_MAX_ROWS))
}

/** 24/7 positioning: `{ assets: [{ display, symbol, group, funding, composite }], warmup, unavailable }`. */
private fun backlogHyperliquidPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    val summary = when {
        root.bool("unavailable") == true -> "Unavailable right now"
        root.bool("warmup") == true -> "Warming up, first readings pending"
        else -> null
    }
    val rows = root.list("assets")
        .sortedByDescending { it.num("composite") ?: 0.0 }
        .mapNotNull { asset ->
            val title = asset.str("display") ?: asset.str("symbol") ?: return@mapNotNull null
            ExploreRow(
                title = title,
                subtitle = listOfNotNull(asset.str("group"), asset.str("funding")?.let { "funding $it" })
                    .joinToString(" · ")
                    .takeIf { it.isNotEmpty() },
                value = asset.num("composite")?.takeIf { it.isFinite() }
                    ?.let { String.format(Locale.US, "score %.0f/100", it) },
            )
        }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_C_MAX_ROWS))
}

/** IEA oil stocks: `{ dataMonth, belowObligation: [], ieaMembers: [{ iso2, daysOfCover, vsObligation, rank }] }`. */
private fun backlogOilStocksPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    val members = root.list("ieaMembers")
    val belowCount = members.count { it.bool("belowObligation") == true }
    val summary = listOfNotNull(
        root.str("dataMonth")?.let { "IEA data $it" },
        if (members.isNotEmpty()) "$belowCount below obligation" else null,
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val rows = members
        .sortedBy { it.num("rank") ?: Double.MAX_VALUE }
        .mapNotNull { member ->
            val title = member.str("iso2") ?: return@mapNotNull null
            val status = when {
                member.bool("belowObligation") == true -> "Below obligation"
                member.bool("obligationMet") == true -> "Obligation met"
                else -> null
            }
            val exporter = if (member.bool("netExporter") == true) "Net exporter" else null
            ExploreRow(
                title = title,
                subtitle = listOfNotNull(status, exporter).joinToString(" · ").takeIf { it.isNotEmpty() },
                value = member.num("daysOfCover")?.takeIf { it.isFinite() }
                    ?.let { String.format(Locale.US, "%.0f days", it) },
                change = member.num("vsObligation")?.takeIf { it.isFinite() },
            )
        }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_C_MAX_ROWS))
}

/** National debt: `{ entries: [{ iso3, debtUsd, debtToGdp, annualGrowth }], unavailable }`, highest debt-to-GDP first. */
private fun backlogNationalDebtPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    if (root.bool("unavailable") == true) return PanelContent(summary = "Unavailable right now")
    val rows = root.list("entries")
        .sortedByDescending { it.num("debtToGdp") ?: 0.0 }
        .mapNotNull { entry ->
            val title = entry.str("iso3") ?: return@mapNotNull null
            val ratio = entry.num("debtToGdp")?.takeIf { it.isFinite() && it > 0 }
            ExploreRow(
                title = title,
                subtitle = ratio?.let { String.format(Locale.US, "Debt %.1f%% of GDP", it) },
                value = formatNumber(entry.num("debtUsd"))?.let { "\$$it" },
                change = entry.num("annualGrowth")?.takeIf { it.isFinite() && it != 0.0 },
            )
        }
    val chart = rankedBarChart(
        "Highest debt, % of GDP",
        root.list("entries").map { (it.str("iso3") ?: "") to it.num("debtToGdp")?.takeIf { v -> v > 0 } },
    ) { String.format(Locale.US, "%.0f%%", it) }
    return PanelContent(rows = rows.take(BACKLOG_C_MAX_ROWS), chart = chart)
}

/** Prediction markets: `{ geopolitical: [{ title, yesPrice, source, endDate, url }] }` (0-100 yes price). */
private fun backlogPredictionsPanel(element: JsonElement): PanelContent {
    val rows = element.obj().list("geopolitical").mapNotNull { market ->
        val title = market.str("title") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = listOfNotNull(
                humanEnum(market.str("source") ?: "polymarket"),
                market.str("endDate")?.let { "Ends $it" },
            ).joinToString(" · "),
            value = market.num("yesPrice")?.takeIf { it.isFinite() }
                ?.let { String.format(Locale.US, "%.0f%% yes", it) },
            url = market.str("url") ?: market.str("displayUrl"),
        )
    }
    return PanelContent(rows = rows.take(BACKLOG_C_MAX_ROWS))
}

/** Shipping indices: `{ indices: [{ name, unit, currentValue, changePct, spikeAlert }], upstreamUnavailable }`. */
private fun backlogShippingRatesPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    val spikes = root.list("indices").count { it.bool("spikeAlert") == true }
    val summary = when {
        root.bool("upstreamUnavailable") == true -> "Upstream temporarily unavailable"
        spikes > 0 -> "$spikes spike alert${if (spikes == 1) "" else "s"}"
        else -> null
    }
    val rows = root.list("indices").mapNotNull { index ->
        val title = index.str("name") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = listOfNotNull(
                index.str("unit"),
                if (index.bool("spikeAlert") == true) "Spike alert" else null,
            ).joinToString(" · ").takeIf { it.isNotEmpty() },
            value = formatPrice(index.num("currentValue")),
            change = index.num("changePct")?.takeIf { it.isFinite() },
        )
    }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_C_MAX_ROWS))
}
