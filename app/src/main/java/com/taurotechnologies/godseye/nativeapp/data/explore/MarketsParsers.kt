package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.JsonElement
import java.util.Locale
import kotlin.math.roundToInt

private const val MARKETS_MAX_ROWS = 60

/** Parsers for the Explore markets panels: quotes, sectors, stress, flows, stablecoins and tokens. */
internal val MARKETS_PARSERS: Map<String, ExploreParser> = mapOf(
    "markets" to ExploreParser("marketQuotes", BootstrapTier.FAST) { element -> marketsQuotePanel(element) },
    "commodities" to ExploreParser("commodityQuotes", BootstrapTier.FAST) { element -> marketsQuotePanel(element) },
    "crypto" to ExploreParser("cryptoQuotes", BootstrapTier.SLOW) { element -> marketsQuotePanel(element) },
    "heatmap" to ExploreParser("sectors", BootstrapTier.SLOW) { element -> marketsSectorPanel(element) },
    "economic" to ExploreParser("economicStress", BootstrapTier.SLOW) { element -> marketsEconomicPanel(element) },
    "macro-signals" to ExploreParser("macroSignals", BootstrapTier.FAST) { element -> marketsMacroPanel(element) },
    "etf-flows" to ExploreParser("etfFlows", BootstrapTier.SLOW) { element -> marketsEtfPanel(element) },
    "stablecoins" to ExploreParser("stablecoinMarkets", BootstrapTier.SLOW) { element -> marketsStablecoinPanel(element) },
    "defi-tokens" to ExploreParser("defiTokens", BootstrapTier.SLOW) { element -> marketsTokenPanel(element) },
    "ai-tokens" to ExploreParser("aiTokens", BootstrapTier.SLOW) { element -> marketsTokenPanel(element) },
    "other-tokens" to ExploreParser("otherTokens", BootstrapTier.SLOW) { element -> marketsTokenPanel(element) },
)

/** Quote feeds (markets, commodities, crypto): `{ quotes: [...], asOf? }`. */
private fun marketsQuotePanel(element: JsonElement): PanelContent {
    val root = element.obj()
    // Data time, distinct from the screen's "Updated" fetch time; same format as the radar feed.
    val summary = root.str("asOf")?.let { asOf -> com.taurotechnologies.godseye.nativeapp.data.isoMillis(asOf)?.let { "Prices as of ${com.taurotechnologies.godseye.nativeapp.data.radarTimestamp(it)}" } ?: "Prices as of $asOf" }
    val rows = root.list("quotes").mapNotNull { quote ->
        val title = quote.str("name") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = quote.str("display") ?: quote.str("symbol"),
            value = formatPrice(quote.num("price")),
            change = quote.num("change"),
        )
    }
    val shown = rows.take(MARKETS_MAX_ROWS)
    return PanelContent(summary = summary, rows = shown, chart = shown.percentChangeChart("Biggest moves, % change"))
}

/** Sector performance: `{ sectors: [{ symbol, name, change }] }`. */
private fun marketsSectorPanel(element: JsonElement): PanelContent {
    val rows = element.obj().list("sectors").mapNotNull { sector ->
        val title = sector.str("name") ?: return@mapNotNull null
        val change = sector.num("change")
        ExploreRow(
            title = title,
            subtitle = sector.str("symbol"),
            value = formatPercent(change),
            change = change,
        )
    }
    val shown = rows.take(MARKETS_MAX_ROWS)
    return PanelContent(rows = shown, chart = shown.percentChangeChart("Sector moves, % change"))
}

/** Composite financial stress: `{ label, compositeScore, components: [{ id, label, score }] }`. */
private fun marketsEconomicPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    val label = root.str("label")
    val composite = root.num("compositeScore")?.takeIf { it.isFinite() }?.roundToInt()
    val summary = listOfNotNull(label, composite?.let { "score $it" })
        .joinToString(" · ")
        .takeIf { it.isNotEmpty() }
    val rows = root.list("components").mapNotNull { component ->
        val title = component.str("label") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = component.str("id"),
            value = component.num("score")?.takeIf { it.isFinite() }?.let { String.format(Locale.US, "score %.1f", it) },
        )
    }
    return PanelContent(summary = summary, rows = rows.take(MARKETS_MAX_ROWS))
}

/** Macro regime signals: `{ verdict, bullishCount, totalCount, signals: { name: { status, value? } } }`. */
private fun marketsMacroPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    val verdict = root.str("verdict")
    val bullish = root.long("bullishCount")
    val total = root.long("totalCount")
    val tally = if (bullish != null && total != null) "$bullish/$total bullish" else null
    val summary = listOfNotNull(verdict, tally).joinToString(" · ").takeIf { it.isNotEmpty() }
    val rows = root?.get("signals").obj()?.entries.orEmpty().mapNotNull { (key, value) ->
        val signal = value.obj() ?: return@mapNotNull null
        ExploreRow(
            title = marketsSignalLabel(key),
            subtitle = signal.str("status"),
            value = signal.num("value")?.takeIf { it.isFinite() }?.let { marketsPlainNumber(it) },
        )
    }
    return PanelContent(summary = summary, rows = rows.take(MARKETS_MAX_ROWS))
}

/** Spot bitcoin ETF flows: `{ summary: { netDirection, totalEstFlow }, etfs: [...] }`. */
private fun marketsEtfPanel(element: JsonElement): PanelContent {
    val root = element.obj()
    val overview = root?.get("summary").obj()
    val direction = humanEnum(overview.str("netDirection"))
    val netFlow = formatNumber(overview.num("totalEstFlow"))
    val summary = if (direction != null && netFlow != null) {
        "$direction · $netFlow USD est. net flow"
    } else {
        null
    }
    val rows = root.list("etfs").mapNotNull { etf ->
        val title = etf.str("ticker") ?: return@mapNotNull null
        val subtitle = listOfNotNull(etf.str("issuer"), humanEnum(etf.str("direction")))
            .joinToString(" · ")
            .takeIf { it.isNotEmpty() }
        ExploreRow(
            title = title,
            subtitle = subtitle,
            value = formatNumber(etf.num("estFlow")),
            change = etf.num("priceChange"),
        )
    }
    return PanelContent(summary = summary, rows = rows.take(MARKETS_MAX_ROWS))
}

/** Stablecoin peg monitor: `{ stablecoins: [{ name, pegStatus, price, change24h }] }`. */
private fun marketsStablecoinPanel(element: JsonElement): PanelContent {
    val rows = element.obj().list("stablecoins").mapNotNull { coin ->
        val title = coin.str("name") ?: coin.str("symbol") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = coin.str("pegStatus"),
            value = formatPrice(coin.num("price")),
            change = coin.num("change24h"),
        )
    }
    return PanelContent(rows = rows.take(MARKETS_MAX_ROWS))
}

/** Token lists (DeFi, AI, other): `{ tokens: [{ name, symbol, price, change24h }] }`. */
private fun marketsTokenPanel(element: JsonElement): PanelContent {
    val rows = element.obj().list("tokens").mapNotNull { token ->
        val title = token.str("name") ?: token.str("symbol") ?: return@mapNotNull null
        ExploreRow(
            title = title,
            subtitle = token.str("symbol"),
            value = formatPrice(token.num("price")),
            change = token.num("change24h"),
        )
    }
    val shown = rows.take(MARKETS_MAX_ROWS)
    return PanelContent(rows = shown, chart = shown.percentChangeChart("24h moves, % change"))
}

/** "flowStructure" becomes "Flow Structure". */
private fun marketsSignalLabel(key: String): String =
    key.replace(Regex("([a-z0-9])([A-Z])"), "\$1 \$2").replaceFirstChar { it.titlecase(Locale.ROOT) }

/** Whole numbers print without decimals; other values use the shared number format. */
private fun marketsPlainNumber(value: Double): String =
    if (value == kotlin.math.floor(value) && kotlin.math.abs(value) < 1e9) {
        value.toLong().toString()
    } else {
        formatNumber(value).orEmpty()
    }
