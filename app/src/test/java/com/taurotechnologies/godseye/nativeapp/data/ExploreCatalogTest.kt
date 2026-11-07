package com.taurotechnologies.godseye.nativeapp.data

import com.taurotechnologies.godseye.nativeapp.data.explore.BootstrapTier
import com.taurotechnologies.godseye.nativeapp.data.explore.ExploreDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExploreCatalogTest {
    /** Unit tests run from the module directory. */
    private val bundledText = File("src/main/assets/${ExploreCatalogStore.ASSET_NAME}").readText()

    @Test
    fun bundledCatalogParsesAndEveryEnabledPanelHasData() {
        val catalog = assertNotNullAndGet(ExploreCatalog.parse(bundledText))
        val parserIds = ExploreDataRepository.parsers.keys
        val withoutData = catalog.visiblePanels.map { it.id }.filter { id ->
            val panel = catalog.panel(id)!!
            id !in parserIds && (panel.view == null || panel.source == null)
        }
        assertTrue("Enabled panels with no data: $withoutData", withoutData.isEmpty())
    }

    @Test
    fun everyBuiltInParserIsVisibleInTheBundledCatalog() {
        val catalog = ExploreCatalog.parse(bundledText)!!
        val visible = catalog.visiblePanels.map { it.id }.toSet()
        val hidden = ExploreDataRepository.parsers.keys - visible
        assertTrue("Parsers without an enabled catalog panel: $hidden", hidden.isEmpty())
    }

    @Test
    fun bundledCatalogIdsAreUniqueAndCategoriesKnown() {
        val catalog = ExploreCatalog.parse(bundledText)!!
        val ids = catalog.panels.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(catalog.panels.all { it.category in catalog.categories })
    }

    @Test
    fun builtInParsersUseFetchableTiers() {
        assertTrue(ExploreDataRepository.parsers.values.all { it.tier in BootstrapTier.entries })
    }

    @Test
    fun parseHidesDisabledDuplicateAndUnknownCategoryPanels() {
        val catalog = ExploreCatalog.parse(
            """
            {"version":1,"categories":["Markets","Empty"],"extra":"ignored","panels":[
              {"id":"a","title":"A","category":"Markets"},
              {"id":"a","title":"A again","category":"Markets"},
              {"id":"b","title":"B","category":"Markets","enabled":false},
              {"id":"c","title":"C","category":"Nowhere"},
              {"id":"d","title":"D","category":"Markets","source":{"key":"k","tier":"on-demand"},
               "view":{"blocks":[{"type":"list","title":"{n}"}]}}
            ]}
            """,
        )!!
        assertEquals(listOf("a", "d"), catalog.visiblePanels.map { it.id })
        assertEquals("A", catalog.visiblePanels.first().title)
        assertEquals(listOf("Markets"), catalog.visibleCategories)
        assertEquals("on-demand", catalog.panel("d")!!.source!!.tier)
        assertNull(catalog.panel("b"))
    }

    @Test
    fun bundledLiveWebcamsRenderFromStaticSourceWithYoutubeLinks() {
        val panel = ExploreCatalog.parse(bundledText)!!.panel("live-webcams")!!
        val source = panel.source!!
        assertEquals(ExploreSource.TYPE_STATIC, source.type)
        val content = com.taurotechnologies.godseye.nativeapp.data.explore.ExploreViewEngine.render(panel.view!!, source.data!!)
        assertTrue(content.summary!!.contains("YouTube"))
        assertTrue(content.rows.size >= 15)
        // Streams the check found dead are hidden from the list.
        assertTrue(content.rows.none { it.title == "Istanbul" })
        assertTrue(content.rows.all { it.url!!.matches(Regex("https://www\\.youtube\\.com/watch\\?v=[A-Za-z0-9_-]{11}")) })
        assertEquals(content.rows.size, content.rows.map { it.url }.toSet().size)
        assertEquals("Jerusalem", content.rows.first().title)
    }

    @Test
    fun staticAndUnknownSourceTypesParse() {
        val catalog = ExploreCatalog.parse(
            """
            {"version":1,"categories":["World"],"panels":[
              {"id":"s","title":"S","category":"World","source":{"type":"static","data":{"items":[{"n":"x"}]}},
               "view":{"blocks":[{"type":"list","items":"items","title":"{n}"}]}},
              {"id":"f","title":"F","category":"World","source":{"type":"future-feed","key":"k"}}
            ]}
            """,
        )!!
        assertEquals("static", catalog.panel("s")!!.source!!.type)
        assertEquals("future-feed", catalog.panel("f")!!.source!!.type)
    }

    @Test
    fun parseRejectsBadNewerOrEmptyCatalogs() {
        assertNull(ExploreCatalog.parse("not json"))
        assertNull(ExploreCatalog.parse("""{"version":2,"categories":["M"],"panels":[{"id":"a","title":"A","category":"M"}]}"""))
        assertNull(ExploreCatalog.parse("""{"version":1,"categories":["M"],"panels":[]}"""))
        assertNull(ExploreCatalog.parse("""{"version":1,"categories":["M"],"panels":[{"id":"a","title":"A","category":"M","enabled":false}]}"""))
    }

    private fun <T> assertNotNullAndGet(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
