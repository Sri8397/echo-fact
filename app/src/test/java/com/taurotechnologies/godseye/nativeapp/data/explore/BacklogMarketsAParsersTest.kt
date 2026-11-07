package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BacklogMarketsAParsersTest {

    private fun parse(panelId: String, json: String): PanelContent =
        BACKLOG_MARKETS_A_PARSERS.getValue(panelId).parse(Json.parseToJsonElement(json))

    @Test
    fun aaiiSentimentParsesWeeklySpread() {
        val panel = parse(
            "aaii-sentiment",
            """
            {
              "latest": {"date": "2026-10-29", "bullish": 42.5, "bearish": 30.1, "neutral": 27.4, "spread": 12.4},
              "weeks": [
                {"date": "2026-10-29", "bullish": 42.5, "bearish": 30.1, "neutral": 27.4, "spread": 12.4},
                {"date": "2026-10-22", "bullish": 35.0, "bearish": 38.0, "neutral": 27.0, "spread": -3.0}
              ]
            }
            """.trimIndent(),
        )
        assertEquals(2, panel.rows.size)
        assertEquals("2026-10-29", panel.rows[0].title)
        assertEquals("+12.40%", panel.rows[0].value)
        assertEquals(12.4, panel.rows[0].change!!, 1e-9)
        assertEquals("-3.00%", panel.rows[1].value)
        assertEquals("Bullish · spread +12.40% · as of 2026-10-29", panel.summary)
    }

    @Test
    fun bigMacSkipsUnpricedCountriesAndSortsByPrice() {
        val panel = parse(
            "bigmac",
            """
            {
              "countries": [
                {"code": "US", "name": "United States", "flag": "US", "currency": "USD", "usdPrice": 5.69, "wowPct": 1.2},
                {"code": "CH", "name": "Switzerland", "flag": "CH", "currency": "CHF", "usdPrice": 7.2, "wowPct": -0.5},
                {"code": "XX", "name": "Missing", "usdPrice": 0}
              ],
              "cheapestCountry": "Vietnam",
              "mostExpensiveCountry": "Switzerland",
              "wowAvailable": true,
              "wowAvgPct": 0.4
            }
            """.trimIndent(),
        )
        assertEquals(2, panel.rows.size)
        assertEquals("CH Switzerland", panel.rows[0].title)
        assertEquals("\$7.20", panel.rows[0].value)
        assertEquals(-0.5, panel.rows[0].change!!, 1e-9)
        assertEquals("Cheapest Vietnam · Priciest Switzerland · avg WoW +0.40%", panel.summary)
    }

    @Test
    fun marketBreadthParsesHistoryAndCurrentReadings() {
        val panel = parse(
            "market-breadth",
            """
            {
              "currentPctAbove20d": 61.2,
              "currentPctAbove50d": 55.4,
              "currentPctAbove200d": 62.0,
              "history": [
                {"date": "2026-11-03", "pctAbove20d": 58.0, "pctAbove50d": 52.1, "pctAbove200d": 60.3},
                {"date": "2026-11-04", "pctAbove20d": 60.5, "pctAbove50d": 54.0, "pctAbove200d": 61.0},
                {"date": "2026-11-05", "pctAbove20d": 61.2, "pctAbove50d": 55.4, "pctAbove200d": 62.0}
              ]
            }
            """.trimIndent(),
        )
        assertEquals(3, panel.rows.size)
        assertEquals(listOf("2026-11-05", "2026-11-04", "2026-11-03"), panel.rows.map { it.title })
        assertEquals("58.0%", panel.rows[2].value)
        assertEquals("50d 52.1% · 200d 60.3%", panel.rows[2].subtitle)
        assertEquals("Above moving average: 20d 61.2% · 50d 55.4% · 200d 62.0%", panel.summary)
    }

    @Test
    fun consumerPricesOverviewParsesTopCategories() {
        val panel = parse(
            "consumer-prices",
            """
            {
              "marketCode": "US",
              "asOf": "2026-11-04",
              "essentialsIndex": 128.4,
              "valueBasketIndex": 112.9,
              "momPct": 0.3,
              "upstreamUnavailable": false,
              "topCategories": [
                {"slug": "food", "name": "Food", "wowPct": 0.8, "momPct": 1.1, "currentIndex": 131.2, "itemCount": 24},
                {"slug": "energy", "name": "Energy", "wowPct": -1.4, "momPct": -2.0, "currentIndex": 98.7, "itemCount": 6}
              ]
            }
            """.trimIndent(),
        )
        assertEquals(2, panel.rows.size)
        assertEquals("Food", panel.rows[0].title)
        assertEquals("24 items", panel.rows[0].subtitle)
        assertEquals("131.20", panel.rows[0].value)
        assertEquals(0.8, panel.rows[0].change!!, 1e-9)
        assertEquals("Essentials 128.40 · Value basket 112.90 · MoM +0.30%", panel.summary)
    }

    @Test
    fun emptyObjectReturnsNoRowsForEveryPanel() {
        for (panelId in BACKLOG_MARKETS_A_PARSERS.keys) {
            val panel = parse(panelId, "{}")
            assertTrue("$panelId should have no rows", panel.rows.isEmpty())
            assertNull("$panelId should have no summary", panel.summary)
        }
    }
}
