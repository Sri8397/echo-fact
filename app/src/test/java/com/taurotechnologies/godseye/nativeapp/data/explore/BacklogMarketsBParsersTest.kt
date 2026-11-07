package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BacklogMarketsBParsersTest {
    private fun parse(panelId: String, json: String): PanelContent =
        BACKLOG_MARKETS_B_PARSERS.getValue(panelId).parse(Json.parseToJsonElement(json))

    @Test
    fun mapCoversAllTwelveBacklogPanels() {
        assertEquals(
            setOf(
                "oil-inventories", "crypto-heatmap", "trade-policy", "fx", "energy-crisis", "fsi",
                "energy-risk-overview", "macro-tiles", "fao-food-price-index", "fear-greed", "fuel-prices", "fuel-shortages",
            ),
            BACKLOG_MARKETS_B_PARSERS.keys,
        )
    }

    @Test
    fun crudeInventoriesListsNewestWeekFirst() {
        val content = parse(
            "oil-inventories",
            """{"latestPeriod":"2026-10-30","weeks":[
                {"period":"2026-10-23","stocksMb":420.5,"weeklyChangeMb":-1.0},
                {"period":"2026-10-30","stocksMb":418.4,"weeklyChangeMb":-2.0}]}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("2026-10-30", content.rows.first().title)
        assertEquals("418.4 M bbl", content.rows.first().value)
        assertEquals(-2.0, content.rows.first().change!!, 0.0)
    }

    @Test
    fun crudeInventoriesKeepsNewestFirstWhenPayloadIsAlreadyNewestFirst() {
        // Live seeded payload order (2026-11-06): newest week first.
        val content = parse(
            "oil-inventories",
            """{"latestPeriod":"2026-10-30","weeks":[
                {"period":"2026-10-30","stocksMb":409.76,"weeklyChangeMb":-0.865},
                {"period":"2026-10-23","stocksMb":410.6,"weeklyChangeMb":1.2}]}""",
        )
        assertEquals(listOf("2026-10-30", "2026-10-23"), content.rows.map { it.title })
    }

    @Test
    fun ecbFxRatesAcceptsRatesKeyedByPair() {
        // Live seeded payload shape (2026-11-06): rates is an object keyed by pair.
        val content = parse(
            "fx",
            """{"updatedAt":"2026-11-05T14:00:00Z","rates":{
                "EURUSD":{"rate":1.1224,"date":"2026-11-05","change1d":-0.0043},
                "EURJPY":{"rate":177.85,"date":"2026-11-05","change1d":-0.45}}}""",
        )
        assertEquals(listOf("EURUSD", "EURJPY"), content.rows.map { it.title })
        assertEquals("1.1224", content.rows.first().value)
        assertEquals(-0.0043, content.rows.first().change!!, 0.0)
    }

    @Test
    fun cryptoSectorsFormatsPercentChange() {
        val content = parse(
            "crypto-heatmap",
            """{"sectors":[{"id":"defi","name":"DeFi","change":2.5},{"id":"l1","name":"Layer 1","change":-1.25}]}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("DeFi", content.rows.first().title)
        assertEquals("+2.50%", content.rows.first().value)
    }

    @Test
    fun customsRevenueShowsNewestMonthInBillions() {
        val content = parse(
            "trade-policy",
            """{"fetchedAt":"2026-11-01","upstreamUnavailable":false,"months":[
                {"recordDate":"2026-09-30","monthlyAmountBillions":80.0,"fytdAmountBillions":500.0},
                {"recordDate":"2026-10-31","monthlyAmountBillions":95.5,"fytdAmountBillions":595.5}]}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("2026-10-31", content.rows.first().title)
        assertEquals("95.50B", content.rows.first().value)
    }

    @Test
    fun ecbFxRatesShowsFourDecimalRates() {
        val content = parse(
            "fx",
            """{"updatedAt":"2026-11-05","unavailable":false,"rates":[
                {"pair":"EUR/USD","rate":1.25,"date":"2026-11-05","change1d":0.5},
                {"pair":"EUR/GBP","rate":0.875,"date":"2026-11-05","change1d":-0.25}]}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("EUR/USD", content.rows.first().title)
        assertEquals("1.2500", content.rows.first().value)
        assertEquals(0.5, content.rows.first().change!!, 0.0)
    }

    @Test
    fun energyCrisisPoliciesListMeasuresWithHumanStatus() {
        val content = parse(
            "energy-crisis",
            """{"source":"IEA","unavailable":false,"policies":[
                {"country":"Germany","countryCode":"DE","category":"consumer_support","sector":"electricity","measure":"Electricity price cap","dateAnnounced":"2026-10-01","status":"active"},
                {"country":"France","countryCode":"FR","category":"conservation","sector":"transport","measure":"Speed limit cut","dateAnnounced":"2026-09-15","status":"ended"}]}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("Electricity price cap", content.rows.first().title)
        assertEquals("Active", content.rows.first().value)
        assertTrue(content.summary!!.contains("2 measures"))
    }

    @Test
    fun euFsiShowsNewestObservationFirst() {
        val content = parse(
            "fsi",
            """{"latestValue":0.412,"latestDate":"2026-10-30","label":"Elevated","unavailable":false,"history":[
                {"date":"2026-10-23","value":0.4},
                {"date":"2026-10-30","value":0.412}]}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("2026-10-30", content.rows.first().title)
        assertEquals("0.412", content.rows.first().value)
    }

    @Test
    fun euGasStorageShowsFillPercentHistory() {
        val content = parse(
            "energy-risk-overview",
            """{"fillPct":62.5,"fillPctChange1d":0.4,"trend":"injecting","unavailable":false,"history":[
                {"date":"2026-10-29","fillPct":62.1,"gasTwh":400.0},
                {"date":"2026-10-30","fillPct":62.5,"gasTwh":402.0}]}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("2026-10-30", content.rows.first().title)
        assertEquals("62.5%", content.rows.first().value)
    }

    @Test
    fun eurostatCountriesAreSortedByCode() {
        val content = parse(
            "macro-tiles",
            """{"seededAt":"2026-10-01","unavailable":false,"countries":{
                "FR":{"unemployment":{"value":7.2,"date":"2026-09-01","unit":"PC","priorValue":7.1,"hasPrior":true}},
                "DE":{"unemployment":{"value":3.4,"date":"2026-09-01","unit":"PC","priorValue":3.3,"hasPrior":true},
                      "cpi":{"value":2.1,"date":"2026-09-01","unit":"RCH_A","priorValue":2.0,"hasPrior":true}}}}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("DE", content.rows.first().title)
        assertEquals("3.4%", content.rows.first().value)
    }

    @Test
    fun foodPriceIndexListsSubIndicesThenHistory() {
        val content = parse(
            "fao-food-price-index",
            """{"currentFfpi":130.5,"momPct":1.95,"yoyPct":3.2,"points":[
                {"date":"2026-09-01","ffpi":128.0,"meat":120.0,"dairy":110.0,"cereals":100.0,"oils":150.0,"sugar":90.0},
                {"date":"2026-10-01","ffpi":130.5,"meat":121.0,"dairy":111.0,"cereals":101.0,"oils":152.0,"sugar":91.0}]}""",
        )
        assertEquals(7, content.rows.size)
        assertEquals("Meat", content.rows.first().title)
        assertEquals("121.0", content.rows.first().value)
        assertTrue(content.summary!!.contains("FFPI 130.5"))
    }

    @Test
    fun fearGreedShowsCompositeCategoriesAndHeaders() {
        val content = parse(
            "fear-greed",
            """{"timestamp":"2026-11-05","composite":{"score":42.0,"label":"Fear","previous":40.0},
                "categories":{"sentiment":{"score":35.0,"weight":0.1}},
                "headerMetrics":{"vix":{"value":18.5,"label":"Calm"},"hySpread":null}}""",
        )
        assertEquals(3, content.rows.size)
        assertEquals("Composite", content.rows.first().title)
        assertEquals("42", content.rows.first().value)
        assertEquals(2.0, content.rows.first().change!!, 0.0)
    }

    @Test
    fun fuelPricesShowsGasolineWithDieselSubtitle() {
        val content = parse(
            "fuel-prices",
            """{"cheapestGasoline":"US","mostExpensiveGasoline":"JP","countries":[
                {"code":"US","name":"United States","currency":"USD","flag":"","fxRate":1.0,
                 "gasoline":{"usdPrice":0.95,"localPrice":0.95,"grade":"regular","source":"x","available":true,"wowPct":-1.5,"observedAt":"2026-11-01"},
                 "diesel":{"usdPrice":1.1,"localPrice":1.1,"grade":"diesel","source":"x","available":true,"wowPct":0.0,"observedAt":"2026-11-01"}},
                {"code":"JP","name":"Japan","currency":"JPY","flag":"","fxRate":150.0,
                 "diesel":{"usdPrice":1.4,"localPrice":210.0,"grade":"diesel","source":"x","available":true,"wowPct":0.0,"observedAt":"2026-11-01"}}]}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("United States", content.rows.first().title)
        assertEquals("0.9500", content.rows.first().value)
        assertEquals("Diesel 1.40", content.rows[1].subtitle)
        assertEquals(null, content.rows[1].value)
    }

    @Test
    fun fuelShortagesListConfirmedFirst() {
        val content = parse(
            "fuel-shortages",
            """{"classifierVersion":"v1","shortages":{
                "a":{"id":"a","country":"NG","product":"diesel","severity":"watch","shortDescription":"Rationing reported","firstSeen":"2026-10-01"},
                "b":{"id":"b","country":"PK","product":"petrol","severity":"confirmed","shortDescription":"Stations closed","firstSeen":"2026-10-20"}}}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("PK · petrol", content.rows.first().title)
        assertEquals("Confirmed", content.rows.first().value)
    }

    @Test
    fun emptyObjectReturnsNoRowsForEveryPanel() {
        for ((panelId, parser) in BACKLOG_MARKETS_B_PARSERS) {
            val content = parser.parse(Json.parseToJsonElement("{}"))
            assertNotNull(panelId, content)
            assertTrue("$panelId should have no rows", content.rows.isEmpty())
        }
    }

    @Test
    fun nonObjectPayloadReturnsNoRowsForEveryPanel() {
        for ((panelId, parser) in BACKLOG_MARKETS_B_PARSERS) {
            val content = parser.parse(Json.parseToJsonElement("[]"))
            assertTrue("$panelId should have no rows", content.rows.isEmpty())
        }
    }
}
