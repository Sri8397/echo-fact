package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreChartsTest {
    private val pct: (Double) -> String? = ::formatPercent

    @Test
    fun divergingKeepsStrongestMovesEitherWayMostPositiveFirst() {
        val values = (1..14).map { "S$it" to (if (it % 2 == 0) it.toDouble() else -it.toDouble()) }
        val chart = divergingBarChart("Moves", values, pct)!!
        assertEquals(ExploreChartKind.DIVERGING_BARS, chart.kind)
        assertEquals(MAX_BARS, chart.points.size)
        assertEquals(listOf(14.0, 12.0, 10.0, 8.0, 6.0, -5.0, -7.0, -9.0, -11.0, -13.0), chart.points.map { it.value })
        assertEquals("+14.00%", chart.points.first().display)
    }

    @Test
    fun chartsNeedThreeFiniteDistinctPoints() {
        assertNull(divergingBarChart("x", listOf("a" to 1.0, "b" to -1.0), pct))
        assertNull(rankedBarChart("x", listOf("a" to 1.0, "b" to Double.NaN, "c" to null, "" to 4.0), pct))
        assertNull("all-zero moves say nothing", divergingBarChart("x", listOf("a" to 0.0, "b" to 0.0, "c" to 0.0), pct))
        assertEquals(1, rankedBarChart("x", listOf("a" to 1.0, "a" to 2.0, "b" to 3.0, "c" to 4.0), pct)!!.points.count { it.label == "a" })
    }

    @Test
    fun rankedBarsLargestFirst() {
        val chart = rankedBarChart("Debt", listOf("JPN" to 199.4, "USA" to 126.8, "SDN" to 334.4, "CHN" to 104.4), pct)!!
        assertEquals(listOf("SDN", "JPN", "USA", "CHN"), chart.points.map { it.label })
    }

    @Test
    fun lineSortsChronologicallyAndKeepsTheMostRecentPoints() {
        val days = (1..150).map { "2026-%02d-%02d".format(1 + (it - 1) / 28, 1 + (it - 1) % 28) to it.toDouble() }.shuffled()
        val chart = lineChart("Index", days) { "%.0f".format(it) }!!
        assertEquals(MAX_LINE_POINTS, chart.points.size)
        assertEquals(chart.points.map { it.label }.sorted(), chart.points.map { it.label })
        assertEquals("150", chart.points.last().display)
    }

    @Test
    fun percentChangeChartUsesRowChanges() {
        val rows = listOf(
            ExploreRow("S&P 500", value = "7,428", change = 1.01),
            ExploreRow("Dow Jones", value = "51,420", change = -0.2),
            ExploreRow("NASDAQ", value = "24,772", change = 1.88),
            ExploreRow("No change field"),
        )
        val chart = rows.percentChangeChart("Biggest moves, % change")!!
        assertEquals(listOf("NASDAQ", "S&P 500", "Dow Jones"), chart.points.map { it.label })
        assertEquals("-0.20%", chart.points.last().display)
    }

    @Test
    fun engineChartBlockUsesSharedBuilders() {
        val view = Json { ignoreUnknownKeys = true }.decodeFromString(
            ExploreView.serializer(),
            """{"blocks":[
                 {"type":"chart","kind":"line","text":"Fill {unit}","items":"history","title":"{date}",
                  "value":{"path":"fillPct","format":"number","suffix":"%"}},
                 {"type":"chart","kind":"bars","text":"ignored second chart","items":"history","title":"{date}","value":{"path":"fillPct"}},
                 {"type":"chart","kind":"radar-from-the-future","text":"x","items":"history","title":"{date}","value":{"path":"fillPct"}}]}""",
        )
        val data = Json.parseToJsonElement(
            """{"unit":"(%)","history":[{"date":"2026-11-04","fillPct":73.75},{"date":"2026-11-02","fillPct":74.4},{"date":"2026-11-03","fillPct":74.07}]}""",
        )
        val chart = ExploreViewEngine.render(view, data).chart
        assertNotNull(chart)
        assertEquals(ExploreChartKind.LINE, chart!!.kind)
        assertEquals("Fill (%)", chart.title)
        assertEquals(listOf("2026-11-02", "2026-11-03", "2026-11-04"), chart.points.map { it.label })
        assertEquals("73.75%", chart.points.last().display)
        assertTrue(ExploreViewEngine.render(
            Json.decodeFromString(ExploreView.serializer(), """{"blocks":[{"type":"chart","kind":"unknown","text":"x","title":"{a}","value":{"path":"a"}}]}"""),
            Json.parseToJsonElement("[]"),
        ).chart == null)
    }
}
