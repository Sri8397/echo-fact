package com.taurotechnologies.godseye.nativeapp.data.explore

import com.taurotechnologies.godseye.nativeapp.data.ExploreCatalog
import com.taurotechnologies.godseye.nativeapp.data.ExploreCatalogStore
import com.taurotechnologies.godseye.nativeapp.data.ExploreSource
import com.taurotechnologies.godseye.nativeapp.data.RpcEndpoint
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class ExploreRpcSourceTest {
    private val bundledText = File("src/main/assets/${ExploreCatalogStore.ASSET_NAME}").readText()

    /** Live response shape, captured 2026-11-07 from `gods-eye.app/api/prediction/v1/list-prediction-markets?public=1`. */
    private val liveEmpty = Json.parseToJsonElement("""{"markets":[],"fetchedAt":0,"dataAvailable":false}""")

    /** Fixture in the documented proto shape (yesPrice is 0-1, closesAt epoch ms). Not captured from live data. */
    private val fixture = Json.parseToJsonElement(
        """
        {"markets":[
          {"id":"pm-1","title":"Will the ceasefire hold?","yesPrice":0.425,"volume":1200.5,
           "url":"https://polymarket.com/event/ceasefire","closesAt":1798761600000,"category":"Geopolitics",
           "source":"MARKET_SOURCE_POLYMARKET","yesPriceChange24h":-0.02},
          {"id":"pm-2","title":"Rate cut in December?","yesPrice":0.9,"volume":10,
           "url":"javascript:alert(1)","closesAt":0,"category":"Markets","source":"MARKET_SOURCE_KALSHI"}
        ],"fetchedAt":1793961600000,"dataAvailable":true}
        """,
    )

    private fun predictionPanel() = ExploreCatalog.parse(bundledText)!!.panel("prediction-markets")!!

    @Test
    fun bundledPredictionPanelDeclaresAnRpcSourceWithPublicQuery() {
        val source = predictionPanel().source!!
        assertEquals("rpc", source.type)
        assertEquals("prediction/v1/list-prediction-markets?public=1", source.key)
        val endpoint = RpcEndpoint.parse(source.key)!!
        assertEquals("prediction/v1/list-prediction-markets", endpoint.path)
        assertEquals(mapOf("public" to "1"), endpoint.query)
        assertEquals(ExploreSource.TYPE_BOOTSTRAP, ExploreSource().type)
    }

    @Test
    fun rendersMarketRowsWithProbabilityAsPercentAndSafeLinks() {
        val content = ExploreViewEngine.render(predictionPanel().view!!, fixture)
        assertEquals(2, content.rows.size)
        val first = content.rows[0]
        assertEquals("Will the ceasefire hold?", first.title)
        assertEquals("Geopolitics", first.subtitle)
        assertEquals("42.5%", first.value)
        assertEquals(-0.02, first.change!!, 0.0)
        assertEquals("https://polymarket.com/event/ceasefire", first.url)
        val second = content.rows[1]
        assertEquals("90.0%", second.value)
        assertNull(second.url)
    }

    @Test
    fun unavailableSnapshotIsTreatedAsNoData() {
        assertNull(ExploreDataRepository.rpcPayload(liveEmpty))
        assertEquals(fixture, ExploreDataRepository.rpcPayload(fixture))
    }

    @Test
    fun responseWithoutAvailabilityFlagIsKept() {
        val plain = Json.parseToJsonElement("""{"markets":[]}""")
        assertEquals(plain, ExploreDataRepository.rpcPayload(plain))
    }

    @Test
    fun probabilityFormatIgnoresNonNumbers() {
        val value = ExploreValue(path = "p", format = "probability")
        assertEquals("50.0%", ExploreViewEngine.formatValue(value, Json.parseToJsonElement("""{"p":0.5}""")))
        assertNull(ExploreViewEngine.formatValue(value, Json.parseToJsonElement("""{"p":"n/a"}""")))
    }

    @Test
    fun rpcSourceKeysAreValidatedBeforeAnyRequest() {
        assertNull(RpcEndpoint.parse("https://evil.example/api/x?public=1"))
        assertNull(RpcEndpoint.parse("prediction/v1/list-prediction-markets?public=1&public=1"))
    }
}
