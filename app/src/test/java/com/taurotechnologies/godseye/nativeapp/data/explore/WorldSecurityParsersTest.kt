package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldSecurityParsersTest {
    private fun parse(id: String, json: String) =
        WORLD_SECURITY_PARSERS.getValue(id).parse(Json.parseToJsonElement(json))

    @Test
    fun airlineIntelParsesDisruptionsAndRanksBySeverityAndDelay() {
        val json = """
        {
          "alerts": [
            {
              "iata": "ORD",
              "name": "Chicago O'Hare",
              "city": "Chicago",
              "country": "USA",
              "severity": "FLIGHT_DELAY_SEVERITY_MINOR",
              "delayType": "FLIGHT_DELAY_TYPE_GENERAL",
              "avgDelayMinutes": 23,
              "reason": "VOL:Multi-taxi"
            },
            {
              "iata": "LAX",
              "name": "Los Angeles International",
              "city": "Los Angeles",
              "country": "USA",
              "severity": "FLIGHT_DELAY_SEVERITY_SEVERE",
              "delayType": "FLIGHT_DELAY_TYPE_GROUND_STOP",
              "avgDelayMinutes": 120,
              "reason": "Airport closure"
            },
            {
              "iata": "MIA",
              "name": "Miami International",
              "city": "Miami",
              "country": "USA",
              "severity": "FLIGHT_DELAY_SEVERITY_MODERATE",
              "delayType": "FLIGHT_DELAY_TYPE_GENERAL",
              "avgDelayMinutes": 45,
              "reason": "Thunderstorms"
            },
            {
              "iata": "JFK",
              "name": "John F. Kennedy",
              "city": "New York",
              "country": "USA",
              "severity": "FLIGHT_DELAY_SEVERITY_NORMAL",
              "delayType": "FLIGHT_DELAY_TYPE_GENERAL",
              "avgDelayMinutes": 0,
              "reason": "none"
            }
          ]
        }
        """.trimIndent()

        val content = parse("airline-intel", json)
        assertEquals(4, content.rows.size)

        // Severe ground stop is ranked first
        val top = content.rows[0]
        assertEquals("LAX · Los Angeles International", top.title)
        assertTrue(top.subtitle!!.contains("Los Angeles, USA"))
        assertTrue(top.subtitle!!.contains("Airport closure"))
        assertEquals("120m delay", top.value)

        // Moderate delay ranked second
        val second = content.rows[1]
        assertEquals("MIA · Miami International", second.title)
        assertEquals("45m delay", second.value)

        // Minor delay ranked third
        val third = content.rows[2]
        assertEquals("ORD · Chicago O'Hare", third.title)
        assertEquals("23m delay", third.value)

        // Normal on schedule ranked last
        val fourth = content.rows[3]
        assertEquals("JFK · John F. Kennedy", fourth.title)
        assertEquals("On schedule", fourth.value)

        // Summary reflects monitored airports and ground stops
        assertTrue(content.summary!!.contains("4 airports monitored"))
        assertTrue(content.summary!!.contains("3 disrupted"))
        assertTrue(content.summary!!.contains("1 ground stops"))

        // Chart includes delay minutes
        val chart = content.chart
        assertNotNull(chart)
        assertEquals(ExploreChartKind.BARS, chart!!.kind)
        assertEquals("Average delay (mins)", chart.title)
        assertEquals(3, chart.points.size)
        assertEquals(120.0, chart.points[0].value, 0.0)
    }

    @Test
    fun airlineIntelHandlesEmptyAlerts() {
        val empty = parse("airline-intel", """{"alerts": []}""")
        assertTrue(empty.rows.isEmpty())
        assertTrue(empty.summary!!.contains("No airport disruption"))
    }
}
