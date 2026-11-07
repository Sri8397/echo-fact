package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BacklogMarketsCParsersTest {
    private fun parse(panelId: String, json: String) =
        BACKLOG_MARKETS_C_PARSERS.getValue(panelId).parse(Json.parseToJsonElement(json))

    private fun parseEmpty(panelId: String) =
        BACKLOG_MARKETS_C_PARSERS.getValue(panelId).parse(Json.parseToJsonElement("{}"))

    @Test
    fun groceryBasketRowsAndSummary() {
        val content = parse("grocery-basket", """
            {"countries":[
              {"code":"US","name":"United States","currency":"USD","totalUsd":5.5,"wowPct":1.25},
              {"code":"JP","name":"Japan","currency":"JPY","totalUsd":4.1,"wowPct":-0.5}],
             "cheapestCountry":"Japan","wowAvgPct":0.4,"wowAvailable":true}
        """)
        assertEquals(2, content.rows.size)
        assertEquals("United States", content.rows[0].title)
        assertEquals("\$5.50", content.rows[0].value)
        assertEquals(1.25, content.rows[0].change!!, 0.0001)
        assertTrue(content.summary!!.contains("Cheapest Japan"))
    }

    @Test
    fun gulfQuotesRowsAndValue() {
        val content = parse("gulf-economies", """
            {"quotes":[
              {"symbol":"TASI","name":"Tadawul All Share","country":"Saudi Arabia","type":"index","price":11234.0,"change":-0.8},
              {"symbol":"DFM","name":"Dubai Financial Market","country":"UAE","type":"index","price":5120.5,"change":0.3}],
             "rateLimited":false}
        """)
        assertEquals(2, content.rows.size)
        assertEquals("Tadawul All Share", content.rows[0].title)
        assertEquals("11,234", content.rows[0].value)
        assertEquals(-0.8, content.rows[0].change!!, 0.0001)
    }

    @Test
    fun hyperliquidFlowSortsByCompositeScore() {
        val content = parse("positioning-247", """
            {"assets":[
              {"symbol":"BTC","display":"BTC-PERP","group":"crypto","funding":"0.01%","composite":40.2},
              {"symbol":"GOLD","display":"GOLD","group":"commodity","composite":71.9}],
             "warmup":false,"unavailable":false}
        """)
        assertEquals(2, content.rows.size)
        assertEquals("GOLD", content.rows[0].title)
        assertEquals("score 72/100", content.rows[0].value)
        assertNull(content.summary)
    }

    @Test
    fun oilStocksRowsOrderedByRank() {
        val content = parse("energy-complex", """
            {"dataMonth":"2026-08","unavailable":false,"ieaMembers":[
              {"iso2":"JP","daysOfCover":210.0,"belowObligation":false,"obligationMet":true,"netExporter":false,"rank":2,"vsObligation":12.5},
              {"iso2":"KR","daysOfCover":150.0,"belowObligation":true,"obligationMet":false,"netExporter":false,"rank":1,"vsObligation":-8.0}]}
        """)
        assertEquals(2, content.rows.size)
        assertEquals("KR", content.rows[0].title)
        assertEquals("150 days", content.rows[0].value)
        assertEquals(-8.0, content.rows[0].change!!, 0.0001)
    }

    @Test
    fun nationalDebtSortedByDebtToGdp() {
        val content = parse("national-debt", """
            {"entries":[
              {"iso3":"USA","debtUsd":3.4e13,"debtToGdp":122.0,"annualGrowth":-0.5},
              {"iso3":"JPN","debtUsd":1.2e13,"debtToGdp":229.0,"annualGrowth":2.1}],
             "unavailable":false}
        """)
        assertEquals(2, content.rows.size)
        assertEquals("JPN", content.rows[0].title)
        assertEquals("\$12.00T", content.rows[0].value)
        assertEquals(2.1, content.rows[0].change!!, 0.0001)
    }

    @Test
    fun predictionsRowsUseYesPriceAndUrl() {
        val content = parse("polymarket", """
            {"geopolitical":[
              {"title":"Will the ceasefire hold?","yesPrice":62.4,"source":"polymarket","endDate":"2026-12-31","url":"https://polymarket.com/event/x"},
              {"title":"Will the vote pass?","yesPrice":18.0,"source":"kalshi"}],
             "fetchedAt":1762300000000}
        """)
        assertEquals(2, content.rows.size)
        assertEquals("Will the ceasefire hold?", content.rows[0].title)
        assertEquals("62% yes", content.rows[0].value)
        assertEquals("https://polymarket.com/event/x", content.rows[0].url)
        assertEquals("Kalshi", content.rows[1].subtitle)
    }

    @Test
    fun shippingRatesRowsAndSpikeSummary() {
        val content = parse("supply-chain", """
            {"indices":[
              {"indexId":"fbx","name":"Freightos FBX","unit":"USD/FEU","currentValue":2150.0,"changePct":-3.2,"spikeAlert":true},
              {"indexId":"scfi","name":"SCFI","unit":"points","currentValue":1200.0,"changePct":1.1,"spikeAlert":false}],
             "upstreamUnavailable":false}
        """)
        assertEquals(2, content.rows.size)
        assertEquals("Freightos FBX", content.rows[0].title)
        assertEquals("2,150", content.rows[0].value)
        assertEquals(-3.2, content.rows[0].change!!, 0.0001)
        assertEquals("1 spike alert", content.summary)
    }

    @Test
    fun emptyObjectReturnsNoRowsForEveryPanel() {
        for (panelId in BACKLOG_MARKETS_C_PARSERS.keys) {
            val content = parseEmpty(panelId)
            assertTrue("$panelId should have no rows", content.rows.isEmpty())
        }
    }

    @Test
    fun mapCoversEveryBacklogPanel() {
        assertEquals(
            setOf("grocery-basket", "gulf-economies", "positioning-247", "energy-complex", "national-debt", "polymarket", "supply-chain"),
            BACKLOG_MARKETS_C_PARSERS.keys,
        )
    }
}
