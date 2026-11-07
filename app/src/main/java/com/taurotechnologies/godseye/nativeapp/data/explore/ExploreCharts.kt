package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlin.math.abs

/**
 * Shared chart builders for Explore panels. Parsers and the view engine use these instead of
 * assembling charts by hand, so every chart follows the same form rules:
 * at least [MIN_POINTS] points (fewer reads better as the summary or rows), finite values only,
 * and a bounded number of marks so labels stay readable on a phone.
 */
internal const val MIN_POINTS = 3
internal const val MAX_BARS = 10
internal const val MAX_LINE_POINTS = 120

/** Ranked magnitude: largest first. */
internal fun rankedBarChart(title: String, values: List<Pair<String, Double?>>, format: (Double) -> String?): ExploreChart? =
    chartPoints(values, format).sortedByDescending { it.value }.take(MAX_BARS)
        .takeIf { it.size >= MIN_POINTS }?.let { ExploreChart(ExploreChartKind.BARS, title, it) }

/** Polarity: the strongest moves either way, most positive at the top. */
internal fun divergingBarChart(title: String, values: List<Pair<String, Double?>>, format: (Double) -> String?): ExploreChart? =
    chartPoints(values, format).sortedByDescending { abs(it.value) }.take(MAX_BARS).sortedByDescending { it.value }
        .takeIf { it.size >= MIN_POINTS && it.any { point -> point.value != 0.0 } }
        ?.let { ExploreChart(ExploreChartKind.DIVERGING_BARS, title, it) }

/**
 * A series over time. [values] can be in any order; labels must sort chronologically as text
 * (ISO dates). Long series keep the most recent [MAX_LINE_POINTS].
 */
internal fun lineChart(title: String, values: List<Pair<String, Double?>>, format: (Double) -> String?): ExploreChart? =
    chartPoints(values, format).sortedBy { it.label }.takeLast(MAX_LINE_POINTS)
        .takeIf { it.size >= MIN_POINTS }?.let { ExploreChart(ExploreChartKind.LINE, title, it) }

/** Diverging bars of each row's percent [ExploreRow.change]. Only for panels whose change is in percent. */
internal fun List<ExploreRow>.percentChangeChart(title: String): ExploreChart? =
    divergingBarChart(title, map { it.title to it.change }, ::formatPercent)

private fun chartPoints(values: List<Pair<String, Double?>>, format: (Double) -> String?): List<ExploreChartPoint> =
    values.mapNotNull { (label, value) ->
        val v = value?.takeIf { it.isFinite() } ?: return@mapNotNull null
        val display = format(v) ?: return@mapNotNull null
        label.takeIf { it.isNotBlank() }?.let { ExploreChartPoint(it, v, display) }
    }.distinctBy { it.label }
