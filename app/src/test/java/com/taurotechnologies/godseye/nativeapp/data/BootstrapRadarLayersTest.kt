package com.taurotechnologies.godseye.nativeapp.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixtures mirror the live public bootstrap payloads captured on 2026-11-06. */
class BootstrapRadarLayersTest {
    private fun json(text: String) = Json.parseToJsonElement(text)

    @Test
    fun sourceIdsAreUniqueAndLayersAreNotSeismic() {
        val ids = BOOTSTRAP_RADAR_SOURCES.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(BOOTSTRAP_RADAR_SOURCES.none { it.layer == RadarLayer.SEISMIC || it.layer == RadarLayer.FLIGHTS })
    }

    @Test
    fun weatherCentroidIsLongitudeThenLatitude() {
        val signals = parseWeatherAlerts(
            json(
                """{"alerts":[
                  {"id":"kz-1","event":"Forestfire","severity":"Extreme","headline":"Forestfire","description":"Forestfire",
                   "areaDesc":"Shalkar district (Aktobe Region)","onset":"2026-11-06T12:57:01.000Z","expires":"2026-11-07T15:00:00.000Z",
                   "centroid":[66.923684,48.019573],"countryCode":"KZ","source":"swic","geometryPrecision":"country"},
                  {"id":"no-centroid","event":"Rain","severity":"Moderate","centroid":[]}]}""",
            ),
        )
        assertEquals(1, signals.size)
        val alert = signals.single()
        assertEquals(48.019573, alert.lat, 1e-9)
        assertEquals(66.923684, alert.lng, 1e-9)
        assertEquals(RadarLayer.WEATHER, alert.layer)
        assertEquals("2026-11-06 12:57 UTC", alert.observedAt)
        assertEquals("Expires 2026-11-07 15:00 UTC.", alert.summary)
        assertTrue(alert.coverage!!.contains("country centre"))
        assertNull("not an earthquake magnitude", alert.magnitude)
    }

    @Test
    fun chokepointsMapStatusAndFlagPartialUpstream() {
        val signals = parseChokepoints(
            json(
                """{"fetchedAt":"2026-11-06T14:49:19.440Z","upstreamUnavailable":true,"chokepoints":[
                  {"id":"suez","name":"Suez Canal","lat":30.45,"lon":32.35,"disruptionScore":30,"status":"yellow",
                   "description":"JWC Listed Area","affectedRoutes":["China-Europe (Suez)","Gulf-Europe"]}]}""",
            ),
        )
        val suez = signals.single()
        assertEquals(RadarLayer.MARITIME, suez.layer)
        assertEquals("Elevated risk · score 30/100", suez.severity)
        assertEquals("2026-11-06 14:49 UTC", suez.observedAt)
        assertTrue(suez.summary.contains("China-Europe (Suez)"))
        assertTrue(suez.coverage!!.contains("unavailable"))
    }

    @Test
    fun disastersAndNaturalEventsUseHazardsLayer() {
        val disasters = parseClimateDisasters(
            json(
                """{"disasters":[{"id":"gdacs-TC-1","type":"cyclone","name":"Tropical Cyclone MURASU-26","country":"India",
                   "lat":15.5,"lng":80.3,"severity":"red","startedAt":1793491200000,"status":"alert","source":"GDACS",
                   "sourceUrl":"https://www.gdacs.org/report.aspx?eventid=1"}]}""",
            ),
        )
        assertEquals("Red alert", disasters.single().severity)
        assertEquals("https://www.gdacs.org/report.aspx?eventid=1", disasters.single().eventSourceUrl)
        val natural = parseNaturalEvents(
            json(
                """{"events":[
                  {"id":"EONET_1","title":"Tropical Cyclone Murasu","categoryTitle":"Severe Storms","lat":15.8,"lon":80.2,
                   "date":1793685600000,"magnitude":65,"magnitudeUnit":"kts","sourceName":"JTWC","closed":false},
                  {"id":"EONET_2","title":"Closed","lat":1,"lon":1,"closed":true}]}""",
            ),
        )
        val storm = natural.single()
        assertEquals(RadarLayer.HAZARDS, storm.layer)
        assertEquals("65 kts", storm.severity)
        assertNull("storm strength must not pass the earthquake magnitude filter", storm.magnitude)
    }

    @Test
    fun canadaAlertsUseExplicitLatLon() {
        val alert = parseCanadaAlerts(
            json(
                """{"alerts":[{"id":"bc-evacuation-5","province":"BC","severity":"Extreme","event":"Fire",
                   "headline":"Bradley Creek Wildfire","description":"Order.","areaDesc":"Okanagan Indian Band IR 1",
                   "onset":"2026-07-31T12:00:00.000Z","publishedAt":1785499200000,"lat":50.3314,"lon":-119.3547,
                   "centroid":[-119.3547,50.3314]}]}""",
            ),
        ).single()
        assertEquals(50.3314, alert.lat, 1e-9)
        assertEquals(-119.3547, alert.lng, 1e-9)
        assertEquals("Okanagan Indian Band IR 1, BC", alert.locationName)
    }

    @Test
    fun outagesAndUnrestReadNestedLocationAndSkipNullIsland() {
        val outages = parseOutages(
            json(
                """{"outages":[
                  {"id":"cf-1742","title":"Internet disruption in Peru","link":"https://x.com/CloudflareRadar/status/211",
                   "description":" A failure on Peru's national power grid","detectedAt":1793562300000,"endedAt":1793572200000,
                   "country":"Peru","region":"","location":{"latitude":-9.19,"longitude":-75.02},
                   "severity":"OUTAGE_SEVERITY_MAJOR","cause":"POWER_OUTAGE"},
                  {"id":"cf-0","title":"Unknown place","location":{"latitude":0,"longitude":0}}]}""",
            ),
        )
        val peru = outages.single()
        assertEquals(RadarLayer.OUTAGES, peru.layer)
        assertEquals("Major · ended", peru.severity)
        assertEquals("Peru", peru.locationName)
        assertTrue(peru.summary.startsWith("A failure"))
        val unrest = parseUnrestEvents(
            json(
                """{"events":[{"id":"gdelt-1","title":"Greece (23 reports)","summary":"Unverified media signal.",
                   "city":"Greece","country":"Greece","location":{"latitude":39,"longitude":22},"occurredAt":1793976300000,
                   "severity":"SEVERITY_LEVEL_UNSPECIFIED","sources":["GDELT"]}]}""",
            ),
        ).single()
        assertEquals("Greece", unrest.locationName)
        assertEquals("Severity not assessed", unrest.severity)
        assertEquals("GDELT", unrest.provider)
    }

    @Test
    fun malformedPayloadsReturnNoSignals() {
        val empty = json("{}")
        val wrong = json("""{"alerts":"x","events":[1,2],"outages":null}""")
        for (source in BOOTSTRAP_RADAR_SOURCES) {
            assertTrue(source.parse(empty).isEmpty())
            assertTrue(source.parse(wrong).isEmpty())
        }
    }

    @Test
    fun layerRanksStayBelowStrongEarthquakes() {
        val strongest = listOf("extreme", "red", "total").maxOf(::severityRank)
        assertTrue(strongest <= 5.0)
    }

    @Test
    fun timestampsMatchExistingFeedFormat() {
        assertEquals("2026-11-06 12:57 UTC", radarTimestamp(isoMillis("2026-11-06T12:57:01.000Z")))
        assertEquals("Unknown", radarTimestamp(null))
        assertNull(isoMillis(""))
    }

    @Test
    fun diseaseOutbreaksSkipNullIslandAndKeepAlertLevel() {
        val outbreaks = parseDiseaseOutbreaks(
            json(
                """{"outbreaks":[
                  {"id":"d-1","disease":"Diphtheria","location":"Senegal","countryCode":"SN","alertLevel":"watch",
                   "summary":"Senegal has confirmed cases. ","sourceUrl":"https://example.org/report","publishedAt":1792454400000,
                   "sourceName":"ThinkGlobalHealth","lat":14.36,"lng":-14.46,"cases":368},
                  {"id":"d-2","disease":"Unknown","location":"Nowhere","alertLevel":"alert","lat":0,"lng":0}]}""",
            ),
        )
        val senegal = outbreaks.single()
        assertEquals(RadarLayer.DISEASE, senegal.layer)
        assertEquals("Watch", senegal.severity)
        assertEquals("Diphtheria: Senegal", senegal.title)
        assertEquals("Senegal has confirmed cases. Cases reported: 368.".trim(), senegal.summary)
        assertEquals("2026-10-20 00:00 UTC", senegal.observedAt)
    }

    @Test
    fun refugeeOutflowsUseOriginPointOrCountryCentreAndSkipReturns() {
        val signals = parseCrossBorderArrivals(
            json(
                """{"source":"UNHCR Operational Data Portal","situations":[{"id":"sudan","name":"Sudan crisis","origin":"SD",
                   "sourceUrl":"https://data.unhcr.org/en/situations/sudansituation","flows":[
                     {"kind":"outflow","label":"Refugees from Sudan","origin":"SD","originLocation":{"latitude":15.97,"longitude":29.94},
                      "measure":"stock","total":3695741,"asOf":"2026-10-05","countries":[{"country":"Egypt"},{"country":"Chad"}]},
                     {"kind":"outflow","label":"Refugees from Sudan (no point)","origin":"SD","measure":"stock","total":50000,"asOf":"2026-10-05"},
                     {"kind":"return","label":"Returns","origin":"SD","originLocation":{"latitude":15,"longitude":30},"total":1,"asOf":"2026-10-05"}]}]}""",
            ),
        )
        assertEquals(2, signals.size)
        val withPoint = signals.first()
        assertEquals(15.97, withPoint.lat, 1e-9)
        assertEquals(29.94, withPoint.lng, 1e-9)
        assertEquals("Severe", withPoint.severity)
        assertEquals("2026-10-05 00:00 UTC", withPoint.observedAt)
        assertTrue(withPoint.summary.contains("Main destinations: Egypt, Chad."))
        assertNull(withPoint.coverage)
        val fallback = signals.last()
        assertEquals("Sudan", fallback.locationName)
        assertEquals(RadarLayer.DISPLACEMENT, fallback.layer)
        assertEquals("Low", fallback.severity)
        assertTrue(fallback.coverage!!.contains("country centre"))
    }

    @Test
    fun sanctionsPlotCountriesAndDropUnattributedEntries() {
        val signals = parseSanctionsPressure(
            json(
                """{"fetchedAt":"2026-11-06T18:02:00.000Z","countries":[
                  {"countryCode":"XX","countryName":"Unknown","entryCount":8497},
                  {"countryCode":"RU","countryName":"Russia","entryCount":5951,"newEntryCount":0,"vesselCount":0,"aircraftCount":0},
                  {"countryCode":"ZZ","countryName":"Nowhere","entryCount":0}]}""",
            ),
        )
        val russia = signals.single()
        assertEquals("Sanctions pressure: Russia", russia.title)
        assertEquals("5,951 linked entries", russia.severity)
        assertEquals(RadarLayer.SANCTIONS, russia.layer)
        assertEquals(4.0, russia.rank!!, 1e-9)
        assertTrue(russia.lat in 40.0..80.0)
    }

    @Test
    fun fuelShortagesReadKeyedObjectAndSkipResolved() {
        val signals = parseFuelShortages(
            json(
                """{"shortages":{
                  "pk-petrol":{"id":"pk-petrol","country":"PK","product":"petrol","severity":"confirmed",
                    "firstSeen":"2026-01-12T00:00:00Z","lastConfirmed":"2026-04-15T00:00:00Z","resolvedAt":null,
                    "impactTypes":["stations_closed","rationing"],"shortDescription":"Retail petrol shortages.",
                    "evidence":{"evidenceSources":[{"authority":"OGRA","url":"https://www.ogra.org.pk/"}]}},
                  "old":{"id":"old","country":"LK","product":"diesel","severity":"watch","resolvedAt":"2026-02-01T00:00:00Z"}}}""",
            ),
        )
        val pakistan = signals.single()
        assertEquals("Petrol shortage in Pakistan", pakistan.title)
        assertEquals("Confirmed", pakistan.severity)
        assertEquals("OGRA", pakistan.provider)
        assertEquals("https://www.ogra.org.pk/", pakistan.eventSourceUrl)
        assertTrue(pakistan.summary.contains("Impacts: Stations closed, Rationing."))
        assertEquals("2026-04-15 00:00 UTC", pakistan.observedAt)
    }

    @Test
    fun pipelineStartPointsUseLonKeyAndStatusLabels() {
        val signals = parsePipelineRegistry(
            json(
                """{"updatedAt":"2026-11-06T07:39:27.543Z","pipelines":{
                  "nord-stream-1":{"id":"nord-stream-1","name":"Nord Stream 1","operator":"Nord Stream AG","commodityType":"gas",
                    "fromCountry":"RU","toCountry":"DE","lengthKm":1224,"inService":2011,
                    "startPoint":{"lat":60.08,"lon":29.05},"endPoint":{"lat":54.14,"lon":13.66},
                    "evidence":{"physicalState":"offline","commercialState":"suspended",
                      "operatorStatement":{"text":"Damaged in 2022.","url":"https://www.nordstream.com/press-info/"}}},
                  "no-start":{"id":"no-start","name":"Unplaced","startPoint":{}}}}""",
            ),
        )
        val pipeline = signals.single()
        assertEquals(RadarLayer.PIPELINES, pipeline.layer)
        assertEquals(60.08, pipeline.lat, 1e-9)
        assertEquals(29.05, pipeline.lng, 1e-9)
        assertEquals("RU → DE", pipeline.locationName)
        assertEquals("Offline", pipeline.severity)
        assertTrue(pipeline.summary.startsWith("Gas pipeline operated by Nord Stream AG. Length 1224 km. In service since 2011."))
        assertEquals("2026-11-06 07:39 UTC", pipeline.observedAt)
    }

    @Test
    fun mineralsPlotLeadingProducerCentre() {
        val signals = parseMinerals(
            json(
                """{"fetchedAt":"2026-11-06T09:50:00.000Z","minerals":[{"mineral":"Gallium","riskRating":"critical","hhi":9280.1,
                   "globalProduction":623,"unit":"tonnes","topProducers":[
                     {"country":"Japan","countryCode":"JP","sharePct":1.6},
                     {"country":"China","countryCode":"CN","sharePct":96.3}]}]}""",
            ),
        )
        val gallium = signals.single()
        assertEquals("Gallium: 96% from China", gallium.title)
        assertEquals("Critical", gallium.severity)
        assertEquals(4.5, gallium.rank!!, 1e-9)
        assertTrue(gallium.summary.contains("Global production 623 tonnes."))
        assertTrue(gallium.lat in 20.0..50.0)
    }

    @Test
    fun trafficAnomaliesFallBackToCountryCentreForNullIsland() {
        val signals = parseTrafficAnomalies(
            json(
                """{"anomalies":[
                  {"uuid":"a-1","type":"LOCATION","status":"VERIFIED","startDate":1793983500000,"endDate":0,
                   "locationCode":"BF","locationName":"Burkina Faso","latitude":0,"longitude":0},
                  {"uuid":"a-2","type":"ORIGIN","status":"TP","startDate":1793983500000,"latitude":0,"longitude":0},
                  {"uuid":"a-3","type":"AS","status":"VERIFIED","startDate":1793983500000,"endDate":1793990000000,
                   "asn":"6400","asnName":"Claro Dominicana","latitude":18.74,"longitude":-70.16}]}""",
            ),
        )
        assertEquals(2, signals.size)
        val burkina = signals.first()
        assertEquals(RadarLayer.OUTAGES, burkina.layer)
        assertEquals("Burkina Faso", burkina.locationName)
        assertTrue(burkina.coverage!!.contains("country centre"))
        assertEquals("Traffic anomaly: Burkina Faso", burkina.title)
        val network = signals.last()
        assertEquals(18.74, network.lat, 1e-9)
        assertNull(network.coverage)
        assertTrue(network.summary.contains("Ended"))
    }
}
