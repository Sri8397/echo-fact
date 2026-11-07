package com.taurotechnologies.godseye.nativeapp.data.explore

import com.taurotechnologies.godseye.nativeapp.data.ExploreCatalog
import com.taurotechnologies.godseye.nativeapp.data.ExploreCatalogStore
import com.taurotechnologies.godseye.nativeapp.data.ExploreSource
import com.taurotechnologies.godseye.nativeapp.data.RpcEndpoint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Catalog panels that moved from disabled to live. Fixtures are trimmed copies of the live
 * `gods-eye.app` responses captured 2026-11-07; the field names and nesting match those responses.
 */
class ExploreCatalogPanelsTest {
    private val bundled = ExploreCatalog.parse(File("src/main/assets/${ExploreCatalogStore.ASSET_NAME}").readText())!!

    private fun fixture(name: String) = Json.parseToJsonElement(File("src/test/resources/explore/$name").readText())

    private fun panel(id: String) = bundled.panel(id)!!

    @Test
    fun portedPanelsAreEnabledWithAViewAndAUsableSource() {
        val ids = listOf(
            "finance", "politics", "us", "europe", "middleeast", "africa", "latam", "asia", "gov",
            "thinktanks", "intel", "tech", "ai", "layoffs", "escalation-correlation", "disaster-correlation", "displacement",
        )
        for (id in ids) {
            val p = panel(id)
            assertTrue("$id has a view", p.view != null)
            val source = p.source!!
            assertTrue("$id source usable", when (source.type) {
                "rpc" -> RpcEndpoint.parse(source.key) != null
                ExploreSource.TYPE_BOOTSTRAP -> source.key.isNotBlank() && source.tier.isNotBlank()
                else -> false
            })
        }
    }

    @Test
    fun newsPanelsRenderTheirCategoryFromTheDigest() {
        val digest = fixture("live-news-digest-2026-11-07.json")
        val panels = listOf(
            "finance", "politics", "us", "europe", "middleeast", "africa", "latam", "asia",
            "gov", "thinktanks", "intel", "tech", "ai", "layoffs",
        )
        for (panelId in panels) {
            val content = ExploreViewEngine.render(panel(panelId).view!!, digest)
            val first = ExploreViewEngine.at(digest, "categories.$panelId.items.0") as JsonObject
            assertEquals("$panelId rows", 3, content.rows.size)
            assertEquals("$panelId first title", first.getValue("title").jsonPrimitive.content, content.rows[0].title)
            assertEquals("$panelId subtitle", first.getValue("source").jsonPrimitive.content, content.rows[0].subtitle)
            assertTrue("$panelId link is https", content.rows[0].url!!.startsWith("https://"))
        }
    }

    @Test
    fun financeHeadlinesMatchTheLiveFixtureOrder() {
        val content = ExploreViewEngine.render(panel("finance").view!!, fixture("live-news-digest-2026-11-07.json"))
        assertEquals("Business Insider", content.rows[0].subtitle)
        assertEquals("CNBC", content.rows[1].subtitle)
        assertEquals("Reuters Business", content.rows[2].subtitle)
    }

    @Test
    fun correlationPanelsRankByScoreFromTheCorrelationCards() {
        val cards = fixture("live-correlation-cards-2026-11-07.json")
        val escalation = ExploreViewEngine.render(panel("escalation-correlation").view!!, cards)
        assertEquals(2, escalation.rows.size)
        // RU and IR tie at 44, so only the set of rows is fixed, not their order.
        assertEquals(
            setOf("conflict + news escalation — RU" to "RU", "conflict + news escalation — IR" to "IR"),
            escalation.rows.map { it.title to it.subtitle }.toSet(),
        )
        assertTrue(escalation.rows.all { it.value == "44" })

        val disaster = ExploreViewEngine.render(panel("disaster-correlation").view!!, cards)
        assertEquals(3, disaster.rows.size)
        // The two 43s tie, so only the pair is fixed; the 35 ranks last.
        assertEquals(
            setOf(
                "Disaster cascade: M6.1 seismic — 144 km SE of Hasaki, Japan",
                "Disaster cascade: M6.1 seismic — Fiji region",
            ),
            disaster.rows.take(2).map { it.title }.toSet(),
        )
        assertEquals("43", disaster.rows[0].value)
        assertEquals("35", disaster.rows[2].value)
    }

    @Test
    fun displacementPanelRanksCountriesByTotalDisplaced() {
        val payload = fixture("live-displacement-summary-2026-11-07.json")
        assertTrue(ExploreDataRepository.rpcPayload(payload) != null)
        val content = ExploreViewEngine.render(panel("displacement").view!!, payload)
        assertEquals(25, content.rows.size)
        assertEquals("Sudan", content.rows[0].title)
        assertEquals("SDN", content.rows[0].subtitle)
        assertEquals("12.90M", content.rows[0].value)
        assertEquals("UNHCR 2025 displaced people by country, largest first", content.summary)
    }
}
