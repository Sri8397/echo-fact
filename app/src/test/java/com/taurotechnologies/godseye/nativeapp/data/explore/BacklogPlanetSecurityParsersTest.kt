package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BacklogPlanetSecurityParsersTest {
    private fun parse(id: String, json: String) =
        BACKLOG_PLANET_SECURITY_PARSERS.getValue(id).parse(Json.parseToJsonElement(json))

    private fun parseEmpty(id: String) = parse(id, "{}")

    @Test
    fun climateNewsParsesHeadlines() {
        val content = parse(
            "climate-news",
            """{"items":[
                {"id":"1","title":"Coral reefs show recovery","url":"https://example.com/a","sourceName":"Wire A","publishedAt":1,"summary":""},
                {"id":"2","title":"Glacier survey released","url":"https://example.com/b","sourceName":"Wire B","publishedAt":2,"summary":""}
            ]}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("Coral reefs show recovery", content.rows[0].title)
        assertEquals("Wire A", content.rows[0].subtitle)
        assertTrue(parseEmpty("climate-news").rows.isEmpty())
    }

    @Test
    fun diseaseOutbreaksParsesAlertLevels() {
        val content = parse(
            "disease-outbreaks",
            """{"outbreaks":[
                {"id":"1","disease":"Measles","location":"Region X","countryCode":"XX","alertLevel":"alert","summary":"","sourceUrl":"https://example.com/m","publishedAt":1,"sourceName":"WHO","lat":0,"lng":0,"cases":120},
                {"id":"2","disease":"Dengue","location":"Region Y","countryCode":"YY","alertLevel":"watch","summary":"","sourceUrl":"","publishedAt":1,"sourceName":"CDC","lat":0,"lng":0,"cases":0}
            ],"fetchedAt":1,"alertLevelMethodologyVersion":"v1"}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("Measles", content.rows[0].title)
        assertEquals("Alert", content.rows[0].value)
        assertNull(content.rows[1].url)
        assertTrue(parseEmpty("disease-outbreaks").rows.isEmpty())
    }

    @Test
    fun givingParsesPlatformsAndCategories() {
        val content = parse(
            "giving",
            """{"summary":{"activityIndex":42.0,"estimatedDailyFlowUsd":2500.0,"trend":"TREND_RISING",
                "platforms":[{"platform":"gofundme","dailyVolumeUsd":2500.0,"activeCampaignsSampled":10,"newCampaigns24h":2,"donationVelocity":0,"dataFreshness":"fresh","lastUpdated":""}],
                "categories":[{"category":"health","share":0.25,"change24h":1.0,"activeCampaigns":5,"trending":true}]},
                "fetchedAt":1,"dataAvailable":true}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("Gofundme", content.rows[0].title)
        assertEquals("\$2,500/day", content.rows[0].value)
        assertEquals("25.0%", content.rows[1].value)
        assertTrue(parseEmpty("giving").rows.isEmpty())
    }

    @Test
    fun crossSourceSignalsParsesSignals() {
        val content = parse(
            "cross-source-signals",
            """{"signals":[
                {"id":"s1","type":"CROSS_SOURCE_SIGNAL_TYPE_CONVERGENCE","theater":"Taiwan Strait","summary":"Shipping and aircraft activity converge","severity":"CROSS_SOURCE_SIGNAL_SEVERITY_HIGH","severityScore":8.0,"detectedAt":1,"contributingTypes":["a","b"],"signalCount":3},
                {"id":"s2","type":"CROSS_SOURCE_SIGNAL_TYPE_UNSPECIFIED","theater":"Global","summary":"","severity":"CROSS_SOURCE_SIGNAL_SEVERITY_LOW","severityScore":1.0,"detectedAt":1,"contributingTypes":[],"signalCount":1}
            ],"evaluatedAt":1,"compositeCount":1}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("Shipping and aircraft activity converge", content.rows[0].title)
        assertEquals("High", content.rows[0].value)
        assertTrue(parseEmpty("cross-source-signals").rows.isEmpty())
    }

    @Test
    fun militaryCorrelationParsesTaiwanReportingDaysNewestFirst() {
        val content = parse(
            "military-correlation",
            """{"schemaVersion":1,"generatedAt":"2026-11-05T00:00:00Z","status":"healthy",
                "sources":[],"coverage":{"usableMndReportingDays":2,"earliestMndReportingDay":null,"latestMndReportingDay":null,"backfillComplete":true},
                "baselines":{"sourceId":"taiwan-mnd","semantics":"prior-usable-reporting-days-excluding-current","categories":{}},
                "observations":[
                  {"id":"o1","sourceId":"taiwan-mnd","reportingDay":"2026-11-04","sourceUrl":"https://example.com/1","categories":{"plaAircraftSorties":20,"planShips":5,"officialShips":1,"medianLineCrossings":2,"adizEntries":3}},
                  {"id":"o2","sourceId":"taiwan-mnd","reportingDay":"2026-11-05","sourceUrl":"https://example.com/2","categories":{"plaAircraftSorties":34,"planShips":null,"officialShips":null,"medianLineCrossings":null,"adizEntries":null}},
                  {"id":"o3","sourceId":"japan-mod","reportingDay":"2026-11-05","sourceUrl":"https://example.com/3","categories":{"plaAircraft":1}}
                ]}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("Taiwan MND · 2026-11-05", content.rows[0].title)
        assertEquals("34 aircraft", content.rows[0].value)
        assertEquals("Healthy", content.summary?.removePrefix("Official activity: "))
        assertTrue(parseEmpty("military-correlation").rows.isEmpty())
    }

    @Test
    fun socialVelocitySortsByVelocity() {
        val content = parse(
            "social-velocity",
            """{"posts":[
                {"id":"p1","title":"Quiet thread","subreddit":"worldnews","url":"https://example.com/q","score":5,"upvoteRatio":0.5,"numComments":2,"velocityScore":10.0,"createdAt":1},
                {"id":"p2","title":"Fast thread","subreddit":"geopolitics","url":"https://example.com/f","score":900,"upvoteRatio":0.9,"numComments":300,"velocityScore":1234.0,"createdAt":1}
            ],"fetchedAt":1}""",
        )
        assertEquals(2, content.rows.size)
        assertEquals("Fast thread", content.rows[0].title)
        assertEquals("1,234", content.rows[0].value)
        assertEquals("r/geopolitics · 300 comments", content.rows[0].subtitle)
        assertTrue(parseEmpty("social-velocity").rows.isEmpty())
    }
}
