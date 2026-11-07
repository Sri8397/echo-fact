package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreViewEngineTest {
    private val json = Json { ignoreUnknownKeys = true }
    private fun view(text: String): ExploreView = json.decodeFromString(ExploreView.serializer(), text)
    private fun data(text: String) = Json.parseToJsonElement(text)

    private val payload = data(
        """
        {"asOf":"2026-11-06","total":3,"leader":{"name":"Oslo"},
         "items":[
           {"name":"Paris","score":12.5,"delta":-1.25,"link":"https://example.org/p","kind":"STATUS_ACTIVE"},
           {"name":"Oslo","score":40,"delta":2,"link":"javascript:alert(1)","kind":"STATUS_DONE"},
           {"score":5},
           {"name":"Lima","score":"7.5"}
         ]}
        """,
    )

    @Test
    fun rendersSummaryMetricsAndSortedListRows() {
        val content = ExploreViewEngine.render(
            view(
                """
                {"blocks":[
                  {"type":"summary","text":"{total} cities · as of {asOf}"},
                  {"type":"metrics","metrics":[{"label":"Leader","value":{"path":"leader.name"}}]},
                  {"type":"list","items":"items","title":"{name}","subtitle":"{kind}","value":{"path":"score","format":"number","suffix":" pts"},
                   "change":"delta","url":"link","sort":{"path":"score","order":"desc"},"limit":10}
                ]}
                """,
            ),
            payload,
        )
        assertEquals("3 cities · as of 2026-11-06", content.summary)
        assertEquals(listOf("Leader", "Oslo", "Paris", "Lima"), content.rows.map { it.title })
        assertEquals("Oslo", content.rows[0].value)
        val oslo = content.rows[1]
        assertEquals("40.00 pts", oslo.value)
        assertEquals(2.0, oslo.change!!, 0.0)
        assertNull("non-http links are dropped", oslo.url)
        assertEquals("https://example.org/p", content.rows[2].url)
        assertEquals("7.50 pts", content.rows[3].value)
    }

    @Test
    fun itemWithoutTitleIsSkippedAndLimitIsApplied() {
        val content = ExploreViewEngine.render(
            view("""{"blocks":[{"type":"list","items":"items","title":"{name}","limit":2}]}"""),
            payload,
        )
        assertEquals(listOf("Paris", "Oslo"), content.rows.map { it.title })
    }

    @Test
    fun formatsMatchSharedFormatters() {
        val scope = data("""{"p":1234.5,"s":2.345,"pc":45.67,"e":"LEVEL_HIGH_RISK","w":3}""")
        assertEquals(formatPrice(1234.5), ExploreViewEngine.formatValue(ExploreValue("p", "price"), scope))
        assertEquals("+2.35%", ExploreViewEngine.formatValue(ExploreValue("s", "signed-percent"), scope))
        assertEquals("45.7%", ExploreViewEngine.formatValue(ExploreValue("pc", "percent"), scope))
        assertEquals("High risk", ExploreViewEngine.formatValue(ExploreValue("e", "enum", strip = "LEVEL_"), scope))
        assertEquals("Level high risk", ExploreViewEngine.formatValue(ExploreValue("e", "enum"), scope))
        assertEquals("3", ExploreViewEngine.formatValue(ExploreValue("w"), scope))
        assertNull(ExploreViewEngine.formatValue(ExploreValue("missing", "number"), scope))
    }

    @Test
    fun rootArrayAndArrayIndexPathsWork() {
        val root = data("""[{"n":"a","v":[10,20]},{"n":"b","v":[30]}]""")
        val content = ExploreViewEngine.render(
            view("""{"blocks":[{"type":"list","title":"{n}","value":{"path":"v.1"}}]}"""),
            root,
        )
        assertEquals(listOf("a", "b"), content.rows.map { it.title })
        assertEquals("20", content.rows[0].value)
        assertNull(content.rows[1].value)
    }

    @Test
    fun unknownBlocksAndMissingDataNeverThrow() {
        val v = view(
            """{"blocks":[{"type":"chart-from-the-future","items":"x"},{"type":"summary","text":"{nope}"},
               {"type":"list","items":"nope","title":"{name}"},{"type":"metrics","metrics":[{"label":"X","value":{"path":"nope"}}]}],
               "newField":true}""",
        )
        val content = ExploreViewEngine.render(v, JsonObject(emptyMap()))
        assertNull(content.summary)
        assertTrue(content.rows.isEmpty())
    }

    @Test
    fun templateWithOnlyLiteralTextIsKept() {
        assertEquals("Hello", ExploreViewEngine.template("Hello", JsonObject(emptyMap())))
        assertNull(ExploreViewEngine.template("{a} · {b}", JsonObject(emptyMap())))
        assertEquals("x", ExploreViewEngine.template("{a} · {b}", data("""{"a":"x"}""")))
    }
}
