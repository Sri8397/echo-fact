package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.Locale

private const val BACKLOG_MAX_ROWS = 25
private const val BACKLOG_LONG_MAX_ROWS = 30
private const val BACKLOG_NO_DATA = "No data published right now."

/** Parsers for backlog Explore panels: oil stocks, crypto sectors, customs, FX, energy policy, stress, food and fuel. */
internal val BACKLOG_MARKETS_B_PARSERS: Map<String, ExploreParser> = mapOf(
    "oil-inventories" to ExploreParser("crudeInventories", BootstrapTier.SLOW) { element -> backlogCrudeInventoriesPanel(element) },
    "crypto-heatmap" to ExploreParser("cryptoSectors", BootstrapTier.SLOW) { element -> backlogCryptoSectorPanel(element) },
    "trade-policy" to ExploreParser("customsRevenue", BootstrapTier.SLOW) { element -> backlogCustomsRevenuePanel(element) },
    "fx" to ExploreParser("ecbFxRates", BootstrapTier.SLOW) { element -> backlogEcbFxPanel(element) },
    "energy-crisis" to ExploreParser("energyCrisisPolicies", BootstrapTier.SLOW) { element -> backlogEnergyCrisisPanel(element) },
    "fsi" to ExploreParser("euFsi", BootstrapTier.SLOW) { element -> backlogEuFsiPanel(element) },
    "energy-risk-overview" to ExploreParser("euGasStorage", BootstrapTier.SLOW) { element -> backlogEuGasStoragePanel(element) },
    "macro-tiles" to ExploreParser("eurostatCountryData", BootstrapTier.SLOW) { element -> backlogEurostatPanel(element) },
    "fao-food-price-index" to ExploreParser("faoFoodPriceIndex", BootstrapTier.SLOW) { element -> backlogFoodPriceIndexPanel(element) },
    "fear-greed" to ExploreParser("fearGreedIndex", BootstrapTier.SLOW) { element -> backlogFearGreedPanel(element) },
    "fuel-prices" to ExploreParser("fuelPrices", BootstrapTier.SLOW) { element -> backlogFuelPricesPanel(element) },
    "fuel-shortages" to ExploreParser("fuelShortages", BootstrapTier.SLOW) { element -> backlogFuelShortagePanel(element) },
)

/** Crude oil stocks: `{ weeks: [{ period, stocksMb, weeklyChangeMb? }], latestPeriod }`. Newest week first. */
private fun backlogCrudeInventoriesPanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    val summary = root.str("latestPeriod")?.let { "Latest week $it" }
    val rows = root.list("weeks").mapNotNull { week ->
        val period = week.str("period") ?: return@mapNotNull null
        ExploreRow(
            title = period,
            value = week.num("stocksMb")?.takeIf { it.isFinite() }?.let { String.format(Locale.US, "%.1f M bbl", it) },
            change = week.num("weeklyChangeMb")?.takeIf { it.isFinite() },
        )
    }
    val chart = lineChart(
        "Crude stocks, million barrels",
        root.list("weeks").map { (it.str("period") ?: "") to it.num("stocksMb") },
    ) { String.format(Locale.US, "%.1f", it) }
    // ISO dates sort as text; newest week first regardless of payload order.
    return PanelContent(summary = summary, rows = rows.sortedByDescending { it.title }.take(BACKLOG_MAX_ROWS), chart = chart)
}

/** Crypto sector performance: `{ sectors: [{ id, name, change }] }`. */
private fun backlogCryptoSectorPanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    val rows = root.list("sectors").mapNotNull { sector ->
        val title = sector.str("name") ?: return@mapNotNull null
        val change = sector.num("change")?.takeIf { it.isFinite() }
        ExploreRow(
            title = title,
            subtitle = sector.str("id"),
            value = formatPercent(change),
            change = change,
        )
    }
    val shown = rows.take(BACKLOG_MAX_ROWS)
    return PanelContent(rows = shown, chart = shown.percentChangeChart("Sector moves, % change"))
}

/** US customs revenue: `{ months: [{ recordDate, monthlyAmountBillions, fytdAmountBillions }], fetchedAt, upstreamUnavailable }`. */
private fun backlogCustomsRevenuePanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    if (root.bool("upstreamUnavailable") == true) return PanelContent(summary = "Upstream data unavailable")
    val rows = root.list("months").mapNotNull { month ->
        val date = month.str("recordDate") ?: return@mapNotNull null
        val monthly = month.num("monthlyAmountBillions")?.takeIf { it.isFinite() }?.let { it * 1e9 }
        val fytd = month.num("fytdAmountBillions")?.takeIf { it.isFinite() }?.let { it * 1e9 }
        ExploreRow(
            title = date,
            subtitle = fytd?.let { "FYTD ${formatNumber(it)}" },
            value = formatNumber(monthly),
        )
    }
    return PanelContent(
        summary = root.str("fetchedAt")?.let { "Fetched $it" },
        rows = rows.sortedByDescending { it.title }.take(BACKLOG_MAX_ROWS),
    )
}

/**
 * ECB reference FX rates. The seeded payload keys rates by pair, `{ rates: { EURUSD: { rate, date, change1d } } }`;
 * the RPC shape is a list, `{ rates: [{ pair, rate, date, change1d }] }`. Both are accepted.
 */
private fun backlogEcbFxPanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    if (root.bool("unavailable") == true) return PanelContent(summary = BACKLOG_NO_DATA)
    val summary = root.str("updatedAt")?.let { "Updated $it" }
    val keyed = root["rates"].obj()?.mapNotNull { (pair, value) -> value.obj()?.let { pair to it } }
    val pairs = keyed ?: root.list("rates").mapNotNull { rate -> rate.str("pair")?.let { it to rate } }
    val rows = pairs.mapNotNull { (pair, rate) ->
        ExploreRow(
            title = pair,
            subtitle = rate.str("date"),
            value = rate.num("rate")?.takeIf { it.isFinite() }?.let { String.format(Locale.US, "%.4f", it) },
            change = rate.num("change1d")?.takeIf { it.isFinite() },
        )
    }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_MAX_ROWS))
}

/** Energy crisis policy measures: `{ source, policies: [{ country, category, sector, measure, dateAnnounced, status }] }`. */
private fun backlogEnergyCrisisPanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    if (root.bool("unavailable") == true) return PanelContent(summary = BACKLOG_NO_DATA)
    val policies = root.list("policies")
    val active = policies.count { it.str("status") == "active" }
    val summary = listOfNotNull(
        if (policies.isEmpty()) null else "${policies.size} measures · $active active",
        root.str("source"),
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val rows = policies.mapNotNull { policy ->
        val title = policy.str("measure") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = listOfNotNull(
                policy.str("country"),
                humanEnum(policy.str("category")),
                humanEnum(policy.str("sector")),
                policy.str("dateAnnounced"),
            ).joinToString(" · ").takeIf { it.isNotEmpty() },
            value = humanEnum(policy.str("status")),
        )
    }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_MAX_ROWS))
}

/** EU financial stress (CISS): `{ latestValue, latestDate, label, history: [{ date, value }], unavailable }`. */
private fun backlogEuFsiPanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    if (root.bool("unavailable") == true) return PanelContent(summary = BACKLOG_NO_DATA)
    val latest = root.num("latestValue")?.takeIf { it.isFinite() }
    val summary = listOfNotNull(
        root.str("label"),
        latest?.let { String.format(Locale.US, "%.3f", it) },
        root.str("latestDate")?.let { "as of $it" },
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val observations = root.list("history").mapNotNull { obs ->
        val date = obs.str("date") ?: return@mapNotNull null
        val value = obs.num("value")?.takeIf { it.isFinite() } ?: return@mapNotNull null
        date to value
    }.sortedBy { it.first }
    val newestFirst = observations.asReversed()
    val rows = newestFirst.mapIndexed { index, (date, value) ->
        ExploreRow(
            title = date,
            value = String.format(Locale.US, "%.3f", value),
            change = newestFirst.getOrNull(index + 1)?.second?.let { value - it },
        )
    }
    val chart = lineChart("EU stress index (CISS)", observations) { String.format(Locale.US, "%.3f", it) }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_MAX_ROWS), chart = chart)
}

/** EU gas storage: `{ fillPct, fillPctChange1d, trend, history: [{ date, fillPct, gasTwh }], unavailable }`. */
private fun backlogEuGasStoragePanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    if (root.bool("unavailable") == true) return PanelContent(summary = BACKLOG_NO_DATA)
    val fill = root.num("fillPct")?.takeIf { it.isFinite() }
    val summary = listOfNotNull(
        fill?.let { String.format(Locale.US, "EU gas storage %.1f%% full", it) },
        formatPercent(root.num("fillPctChange1d"))?.let { "1d $it" },
        humanEnum(root.str("trend")),
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val history = root.list("history").mapNotNull { entry ->
        val date = entry.str("date") ?: return@mapNotNull null
        val pct = entry.num("fillPct")?.takeIf { it.isFinite() } ?: return@mapNotNull null
        Triple(date, pct, entry.num("gasTwh")?.takeIf { it.isFinite() })
    }.sortedBy { it.first }
    val newestFirst = history.asReversed()
    val rows = newestFirst.mapIndexed { index, (date, pct, twh) ->
        ExploreRow(
            title = date,
            subtitle = twh?.let { String.format(Locale.US, "%.1f TWh", it) },
            value = String.format(Locale.US, "%.1f%%", pct),
            change = newestFirst.getOrNull(index + 1)?.second?.let { pct - it },
        )
    }
    val chart = lineChart("EU gas storage, % full", history.map { it.first to it.second }) { String.format(Locale.US, "%.1f%%", it) }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_MAX_ROWS), chart = chart)
}

/** Eurostat country macro: `{ countries: { CODE: { unemployment?, cpi?, gdpGrowth? } } }` with `{ value }` metrics. */
private fun backlogEurostatPanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    val countries = root["countries"].obj() ?: return PanelContent()
    val rows = countries.entries.sortedBy { it.key }.mapNotNull { (code, value) ->
        val entry = value.obj() ?: return@mapNotNull null
        val unemployment = entry["unemployment"].obj().num("value")?.takeIf { it.isFinite() }
        val cpi = entry["cpi"].obj().num("value")?.takeIf { it.isFinite() }
        val gdp = entry["gdpGrowth"].obj().num("value")?.takeIf { it.isFinite() }
        if (unemployment == null && cpi == null && gdp == null) return@mapNotNull null
        ExploreRow(
            title = code,
            subtitle = listOfNotNull(
                unemployment?.let { String.format(Locale.US, "Unemployment %.1f%%", it) },
                cpi?.let { String.format(Locale.US, "CPI %.1f", it) },
                gdp?.let { String.format(Locale.US, "GDP growth %.1f%%", it) },
            ).joinToString(" · ").takeIf { it.isNotEmpty() },
            value = unemployment?.let { String.format(Locale.US, "%.1f%%", it) },
        )
    }
    return PanelContent(rows = rows.take(BACKLOG_LONG_MAX_ROWS))
}

/** FAO Food Price Index: `{ points: [{ date, ffpi, meat, dairy, cereals, oils, sugar }], currentFfpi, momPct, yoyPct }`. */
private fun backlogFoodPriceIndexPanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    val points = root.list("points").sortedBy { it.str("date").orEmpty() }
    val summary = listOfNotNull(
        root.num("currentFfpi")?.takeIf { it.isFinite() }?.let { String.format(Locale.US, "FFPI %.1f", it) },
        formatPercent(root.num("momPct"))?.let { "MoM $it" },
        formatPercent(root.num("yoyPct"))?.let { "YoY $it" },
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val latest = points.lastOrNull()
    val previous = points.getOrNull(points.size - 2)
    val subIndices = listOf(
        "meat" to "Meat",
        "dairy" to "Dairy",
        "cereals" to "Cereals",
        "oils" to "Vegetable oils",
        "sugar" to "Sugar",
    )
    val subRows = subIndices.mapNotNull { (key, name) ->
        val value = latest?.num(key)?.takeIf { it.isFinite() } ?: return@mapNotNull null
        ExploreRow(
            title = name,
            subtitle = latest.str("date")?.let { "as of $it" },
            value = String.format(Locale.US, "%.1f", value),
            change = previous?.num(key)?.takeIf { it.isFinite() }?.let { value - it },
        )
    }
    val history = points.mapNotNull { point ->
        val date = point.str("date") ?: return@mapNotNull null
        val value = point.num("ffpi")?.takeIf { it.isFinite() } ?: return@mapNotNull null
        date to value
    }
    val newestFirst = history.asReversed()
    val historyRows = newestFirst.mapIndexed { index, (date, value) ->
        ExploreRow(
            title = date,
            value = String.format(Locale.US, "%.1f", value),
            change = newestFirst.getOrNull(index + 1)?.second?.let { value - it },
        )
    }
    return PanelContent(summary = summary, rows = (subRows + historyRows.take(12)).take(BACKLOG_MAX_ROWS))
}

private val BACKLOG_FEAR_GREED_CATEGORIES = listOf(
    "sentiment" to "Sentiment",
    "volatility" to "Volatility",
    "positioning" to "Positioning",
    "trend" to "Trend",
    "breadth" to "Breadth",
    "momentum" to "Momentum",
    "liquidity" to "Liquidity",
    "credit" to "Credit",
    "macro" to "Macro",
    "crossAsset" to "Cross-asset",
)

private val BACKLOG_FEAR_GREED_HEADERS = listOf(
    "vix" to "VIX",
    "hySpread" to "High-yield spread",
    "yield10y" to "10-year yield",
    "putCall" to "Put/call ratio",
    "pctAbove200d" to "Above 200-day average",
    "cnnFearGreed" to "CNN Fear & Greed",
    "aaiBull" to "AAII bullish",
    "aaiBear" to "AAII bearish",
    "fsi" to "Financial stress",
)

/**
 * Composite fear and greed: `{ composite: { score, label, previous }, categories: { id: { score } },
 * headerMetrics: { id: { value, label } | null } }`.
 */
private fun backlogFearGreedPanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    if (root.bool("unavailable") == true) return PanelContent(summary = BACKLOG_NO_DATA)
    val composite = root["composite"].obj()
    val score = (composite.num("score") ?: root.num("compositeScore"))?.takeIf { it.isFinite() }
    val previous = composite.num("previous")?.takeIf { it.isFinite() }
    val label = composite.str("label") ?: root.str("compositeLabel")
    val summary = listOfNotNull(
        label,
        score?.let { String.format(Locale.US, "score %.0f", it) },
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val compositeRow = score?.let {
        ExploreRow(
            title = "Composite",
            subtitle = label,
            value = String.format(Locale.US, "%.0f", it),
            change = previous?.let { prior -> it - prior },
        )
    }
    val categories = root["categories"].obj()
    val categoryRows = BACKLOG_FEAR_GREED_CATEGORIES.mapNotNull { (id, name) ->
        val value = categories?.get(id).obj()?.num("score")?.takeIf { it.isFinite() } ?: return@mapNotNull null
        ExploreRow(title = name, value = String.format(Locale.US, "%.0f", value))
    }
    val headers = root["headerMetrics"].obj()
    val headerRows = BACKLOG_FEAR_GREED_HEADERS.mapNotNull { (id, name) ->
        val metric = headers?.get(id).obj() ?: return@mapNotNull null
        val value = metric.num("value")?.takeIf { it.isFinite() }
        val subtitle = metric.str("label")
        if (value == null && subtitle == null) return@mapNotNull null
        ExploreRow(title = name, subtitle = subtitle, value = formatNumber(value))
    }
    return PanelContent(
        summary = summary,
        rows = (listOfNotNull(compositeRow) + categoryRows + headerRows).take(BACKLOG_MAX_ROWS),
    )
}

/** Retail fuel prices: `{ countries: [{ name, gasoline?, diesel? }], cheapestGasoline, mostExpensiveGasoline }`. */
private fun backlogFuelPricesPanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    val summary = listOfNotNull(
        root.str("cheapestGasoline")?.let { "Cheapest gasoline: $it" },
        root.str("mostExpensiveGasoline")?.let { "Most expensive: $it" },
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val rows = root.list("countries").mapNotNull { country ->
        val title = country.str("name") ?: country.str("code") ?: return@mapNotNull null
        val gasoline = country["gasoline"].obj()
        val diesel = country["diesel"].obj()
        val gasolinePrice = gasoline.num("usdPrice")?.takeIf { it.isFinite() }
        val dieselPrice = diesel.num("usdPrice")?.takeIf { it.isFinite() }
        if (gasolinePrice == null && dieselPrice == null) return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = dieselPrice?.let { "Diesel ${formatPrice(it)}" },
            value = formatPrice(gasolinePrice),
            change = gasoline.num("wowPct")?.takeIf { it.isFinite() },
        )
    }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_LONG_MAX_ROWS))
}

/**
 * Fuel shortage registry: `{ shortages: { id: { country, product, severity, shortDescription, firstSeen } } }`.
 * Confirmed shortages are listed first.
 */
private fun backlogFuelShortagePanel(element: JsonElement): PanelContent {
    val root = element.obj() ?: return PanelContent()
    val entries = root["shortages"].obj()?.entries.orEmpty()
        .mapNotNull { (_, value) -> value.obj() }
        .filter { it.str("country") != null }
    val confirmed = entries.count { it.str("severity") == "confirmed" }
    val summary = if (entries.isEmpty()) null else "${entries.size} shortages · $confirmed confirmed"
    val rows = entries
        .sortedBy { if (it.str("severity") == "confirmed") 0 else 1 }
        .map { shortage ->
            ExploreRow(
                title = listOfNotNull(shortage.str("country"), shortage.str("product")).joinToString(" · "),
                subtitle = shortage.str("shortDescription") ?: shortage.str("firstSeen"),
                value = humanEnum(shortage.str("severity")),
            )
        }
    return PanelContent(summary = summary, rows = rows.take(BACKLOG_MAX_ROWS))
}
