package com.taurotechnologies.godseye.nativeapp.data

import com.taurotechnologies.godseye.nativeapp.data.explore.BootstrapTier
import com.taurotechnologies.godseye.nativeapp.data.explore.ExploreDataRepository
import com.taurotechnologies.godseye.nativeapp.data.explore.bool
import com.taurotechnologies.godseye.nativeapp.data.explore.humanEnum
import com.taurotechnologies.godseye.nativeapp.data.explore.list
import com.taurotechnologies.godseye.nativeapp.data.explore.long
import com.taurotechnologies.godseye.nativeapp.data.explore.num
import com.taurotechnologies.godseye.nativeapp.data.explore.obj
import com.taurotechnologies.godseye.nativeapp.data.explore.str
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A radar map dataset fed by one public bootstrap key. This is the shared path for every
 * bootstrap-backed map layer: add an entry to [BOOTSTRAP_RADAR_SOURCES] instead of a new loader.
 * Data comes through [ExploreDataRepository]'s fetch and cache, so map and Explore share requests.
 */
class BootstrapRadarSource(
    /** Stable id, persisted when the user turns the source off. */
    val id: String,
    val title: String,
    val description: String,
    val key: String,
    val tier: BootstrapTier,
    val layer: RadarLayer,
    val parse: (JsonElement) -> List<RadarSignal>,
)

val BOOTSTRAP_RADAR_SOURCES: List<BootstrapRadarSource> = listOf(
    BootstrapRadarSource("weather-alerts", "Weather alerts", "Official severe-weather warnings", "weatherAlerts", BootstrapTier.FAST, RadarLayer.WEATHER, ::parseWeatherAlerts),
    BootstrapRadarSource("chokepoints", "Maritime chokepoints", "Disruption status of key shipping lanes", "chokepoints", BootstrapTier.FAST, RadarLayer.MARITIME, ::parseChokepoints),
    BootstrapRadarSource("disasters", "GDACS disasters", "Cyclones, floods, droughts and volcanoes", "climateDisasters", BootstrapTier.SLOW, RadarLayer.HAZARDS, ::parseClimateDisasters),
    BootstrapRadarSource("natural-events", "NASA EONET events", "Open storms, ice and other natural events", "naturalEvents", BootstrapTier.SLOW, RadarLayer.HAZARDS, ::parseNaturalEvents),
    BootstrapRadarSource("canada-alerts", "Canada emergency alerts", "Provincial evacuation and emergency alerts", "canadaAlerts", BootstrapTier.FAST, RadarLayer.HAZARDS, ::parseCanadaAlerts),
    BootstrapRadarSource("outages", "Internet outages", "Country and regional connectivity outages", "outages", BootstrapTier.FAST, RadarLayer.OUTAGES, ::parseOutages),
    BootstrapRadarSource("unrest", "Unrest reports", "Unverified media signals of protests and unrest", "unrestEvents", BootstrapTier.SLOW, RadarLayer.UNREST, ::parseUnrestEvents),
    BootstrapRadarSource("wildfires", "NASA wildfires", "Satellite active thermal fire detections", "wildfires", BootstrapTier.SLOW, RadarLayer.FIRES, ::parseWildfires),
    BootstrapRadarSource("cyber-threats", "Cyber threat intel", "Malware C2 servers and threat infrastructure", "cyberThreats", BootstrapTier.SLOW, RadarLayer.CYBER, ::parseCyberThreats),
    BootstrapRadarSource("ucdp-conflicts", "Armed conflict events", "Georeferenced UCDP conflict incidents", "ucdpEvents", BootstrapTier.SLOW, RadarLayer.CONFLICTS, ::parseUcdpEvents),
    BootstrapRadarSource("traffic-anomalies", "Internet traffic anomalies", "Network and country traffic drops flagged by Cloudflare Radar", "trafficAnomalies", BootstrapTier.FAST, RadarLayer.OUTAGES, ::parseTrafficAnomalies),
    BootstrapRadarSource("disease-outbreaks", "Disease outbreak reports", "Reported human disease outbreaks with alert levels", "diseaseOutbreaks", BootstrapTier.SLOW, RadarLayer.DISEASE, ::parseDiseaseOutbreaks),
    BootstrapRadarSource("displacement-flows", "Refugee outflows", "UNHCR refugee and displaced-person outflows by origin", "crossBorderArrivals", BootstrapTier.ON_DEMAND, RadarLayer.DISPLACEMENT, ::parseCrossBorderArrivals),
    BootstrapRadarSource("sanctions", "Sanctions pressure", "Sanctions list entries linked to each country", "sanctionsPressure", BootstrapTier.SLOW, RadarLayer.SANCTIONS, ::parseSanctionsPressure),
    BootstrapRadarSource("fuel-shortages", "Fuel shortages", "Confirmed and watched national fuel supply shortages", "fuelShortages", BootstrapTier.SLOW, RadarLayer.FUEL, ::parseFuelShortages),
    BootstrapRadarSource("gas-pipelines", "Gas pipelines", "Gas pipeline registry with physical and commercial status", "pipelinesGas", BootstrapTier.ON_DEMAND, RadarLayer.PIPELINES, ::parsePipelineRegistry),
    BootstrapRadarSource("oil-pipelines", "Oil pipelines", "Oil pipeline registry with physical and commercial status", "pipelinesOil", BootstrapTier.ON_DEMAND, RadarLayer.PIPELINES, ::parsePipelineRegistry),
    BootstrapRadarSource("critical-minerals", "Critical minerals", "Leading producer country for each critical mineral", "minerals", BootstrapTier.SLOW, RadarLayer.MINERALS, ::parseMinerals),
)

object BootstrapRadarRepository {
    /** Signals for [source]. Empty when the dataset is not published; throws on network failure. */
    suspend fun load(source: BootstrapRadarSource): List<RadarSignal> =
        ExploreDataRepository.dataset(source.key, source.tier)?.let(source.parse).orEmpty()
}

/** Same display format as the USGS and flight feeds, so the combined feed sorts as text. */
internal fun radarTimestamp(epochMillis: Long?): String = try {
    if (epochMillis == null || epochMillis <= 0) "Unknown"
    else DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(epochMillis))
} catch (_: Exception) {
    "Unknown"
}

internal fun isoMillis(value: String?): Long? = value?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

/** Valid, non-null-island coordinates; providers send 0,0 for "unknown". */
private fun validPoint(lat: Double?, lng: Double?): Boolean =
    lat != null && lng != null && lat.isFinite() && lng.isFinite() &&
        lat in -90.0..90.0 && lng in -180.0..180.0 && !(lat == 0.0 && lng == 0.0)

private fun httpUrl(value: String?): String? = value?.takeIf { it.startsWith("https://") || it.startsWith("http://") }

/**
 * Map ranking when markers are capped at low zoom. Kept below stories (6) and M5+ earthquakes so
 * these layers never push the existing stories and strong quakes off the world view.
 */
internal fun severityRank(label: String?): Double = when (label?.lowercase(Locale.ROOT)) {
    "extreme", "red", "total" -> 5.0
    "severe", "orange", "major" -> 4.5
    "moderate", "yellow", "partial" -> 4.0
    else -> 3.5
}

private fun JsonObject?.numberAt(index: Int, key: String): Double? =
    ((this?.get(key) as? JsonArray)?.getOrNull(index) as? JsonPrimitive)?.doubleOrNull

/** `{ alerts: [{ id, event, severity, headline, description, areaDesc, onset, expires, centroid: [lon, lat], countryCode, source, geometryPrecision }] }`. */
internal fun parseWeatherAlerts(element: JsonElement): List<RadarSignal> = element.obj().list("alerts").mapNotNull { alert ->
    val id = alert.str("id") ?: return@mapNotNull null
    // GeoJSON order: centroid is [longitude, latitude].
    val lng = alert.numberAt(0, "centroid")
    val lat = alert.numberAt(1, "centroid")
    if (!validPoint(lat, lng)) return@mapNotNull null
    val event = alert.str("event") ?: "Weather alert"
    val severity = alert.str("severity") ?: "Unknown severity"
    val area = listOfNotNull(alert.str("areaDesc"), alert.str("countryCode")).joinToString(", ").ifBlank { "Area unavailable" }
    val description = alert.str("description")?.takeIf { it != alert.str("headline") && it != event }
    RadarSignal(
        id = "weather:$id", layer = RadarLayer.WEATHER,
        title = alert.str("headline") ?: event, locationName = area, lat = lat!!, lng = lng!!,
        observedAt = radarTimestamp(isoMillis(alert.str("onset"))), severity = severity,
        summary = listOfNotNull(description, alert.str("expires")?.let { "Expires ${radarTimestamp(isoMillis(it))}." }).joinToString(" ").ifBlank { event },
        provider = alert.str("source")?.let { "Weather alerts · $it" } ?: "Weather alerts",
        coverage = if (alert.str("geometryPrecision") == "country") "Position is the country centre, not the alert area." else null,
        isSimulated = false, rank = severityRank(severity),
    )
}

/** `{ chokepoints: [{ id, name, lat, lon, disruptionScore, status, description, affectedRoutes }], fetchedAt, upstreamUnavailable }`. */
internal fun parseChokepoints(element: JsonElement): List<RadarSignal> {
    val root = element.obj()
    val observedAt = radarTimestamp(isoMillis(root.str("fetchedAt")))
    val partial = root.bool("upstreamUnavailable") == true
    return root.list("chokepoints").mapNotNull { point ->
        val id = point.str("id") ?: return@mapNotNull null
        val lat = point.num("lat")
        val lng = point.num("lon")
        if (!validPoint(lat, lng)) return@mapNotNull null
        val status = when (point.str("status")) { "red" -> "Disrupted"; "yellow" -> "Elevated risk"; "green" -> "Normal"; else -> "Status unknown" }
        val routes = (point["affectedRoutes"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }?.take(3).orEmpty()
        RadarSignal(
            id = "chokepoint:$id", layer = RadarLayer.MARITIME,
            title = point.str("name") ?: id, locationName = point.str("name") ?: id, lat = lat!!, lng = lng!!,
            observedAt = observedAt,
            severity = point.num("disruptionScore")?.let { "$status · score ${it.toInt()}/100" } ?: status,
            summary = listOfNotNull(point.str("description"), routes.takeIf { it.isNotEmpty() }?.let { "Routes: ${it.joinToString(", ")}." }).joinToString(" ").ifBlank { status },
            provider = "GodsEye chokepoint monitor",
            coverage = if (partial) "Some upstream maritime sources were unavailable; status may be incomplete." else null,
            isSimulated = false, rank = severityRank(point.str("status")),
        )
    }
}

/** `{ disasters: [{ id, type, name, country, lat, lng, severity, startedAt, status, source, sourceUrl }] }`. */
internal fun parseClimateDisasters(element: JsonElement): List<RadarSignal> = element.obj().list("disasters").mapNotNull { disaster ->
    val id = disaster.str("id") ?: return@mapNotNull null
    val lat = disaster.num("lat")
    val lng = disaster.num("lng")
    if (!validPoint(lat, lng)) return@mapNotNull null
    val level = disaster.str("severity")
    RadarSignal(
        id = "disaster:$id", layer = RadarLayer.HAZARDS,
        title = disaster.str("name") ?: humanEnum(disaster.str("type")) ?: "Disaster",
        locationName = disaster.str("country") ?: "Location unavailable", lat = lat!!, lng = lng!!,
        observedAt = radarTimestamp(disaster.long("startedAt")),
        severity = level?.let { "${it.replaceFirstChar { c -> c.titlecase(Locale.ROOT) }} alert" } ?: "Alert level unknown",
        summary = listOfNotNull(humanEnum(disaster.str("type")), humanEnum(disaster.str("status"))?.let { "status $it" }).joinToString(" · "),
        provider = disaster.str("source") ?: "GDACS",
        isSimulated = false, eventSourceUrl = httpUrl(disaster.str("sourceUrl")), rank = severityRank(level),
    )
}

/** `{ events: [{ id, title, description, categoryTitle, lat, lon, date, magnitude, magnitudeUnit, sourceUrl, sourceName, closed }] }`. */
internal fun parseNaturalEvents(element: JsonElement): List<RadarSignal> = element.obj().list("events").mapNotNull { event ->
    val id = event.str("id") ?: return@mapNotNull null
    if (event.bool("closed") == true) return@mapNotNull null
    val lat = event.num("lat")
    val lng = event.num("lon")
    if (!validPoint(lat, lng)) return@mapNotNull null
    // Kept out of RadarSignal.magnitude: that field is the earthquake magnitude used by the minimum-magnitude filter.
    val strength = event.num("magnitude")?.takeIf { it.isFinite() && it > 0 }?.let { "${it.toInt()} ${event.str("magnitudeUnit").orEmpty()}".trim() }
    val category = event.str("categoryTitle") ?: "Natural event"
    RadarSignal(
        id = "eonet:$id", layer = RadarLayer.HAZARDS,
        title = event.str("title") ?: category, locationName = category, lat = lat!!, lng = lng!!,
        observedAt = radarTimestamp(event.long("date")),
        severity = strength ?: category,
        summary = event.str("description") ?: listOfNotNull(category, strength).joinToString(" · "),
        provider = event.str("sourceName")?.let { "NASA EONET · $it" } ?: "NASA EONET",
        isSimulated = false, eventSourceUrl = httpUrl(event.str("sourceUrl")), rank = 4.0,
    )
}

/** `{ alerts: [{ id, province, severity, event, headline, description, areaDesc, onset, publishedAt, lat, lon }] }`. */
internal fun parseCanadaAlerts(element: JsonElement): List<RadarSignal> = element.obj().list("alerts").mapNotNull { alert ->
    val id = alert.str("id") ?: return@mapNotNull null
    val lat = alert.num("lat")
    val lng = alert.num("lon")
    if (!validPoint(lat, lng)) return@mapNotNull null
    val severity = alert.str("severity") ?: "Unknown severity"
    RadarSignal(
        id = "canada:$id", layer = RadarLayer.HAZARDS,
        title = alert.str("headline") ?: alert.str("event") ?: "Emergency alert",
        locationName = listOfNotNull(alert.str("areaDesc"), alert.str("province")).joinToString(", ").ifBlank { "Canada" },
        lat = lat!!, lng = lng!!,
        observedAt = radarTimestamp(isoMillis(alert.str("onset")) ?: alert.long("publishedAt")),
        severity = severity,
        summary = alert.str("description") ?: alert.str("event") ?: severity,
        provider = alert.str("province")?.let { "Canada emergency alerts · $it" } ?: "Canada emergency alerts",
        isSimulated = false, rank = severityRank(severity),
    )
}

/** `{ outages: [{ id, title, link, description, detectedAt, endedAt, country, location: { latitude, longitude }, severity, cause }] }`. */
internal fun parseOutages(element: JsonElement): List<RadarSignal> = element.obj().list("outages").mapNotNull { outage ->
    val id = outage.str("id") ?: return@mapNotNull null
    val location = outage["location"].obj()
    val lat = location.num("latitude")
    val lng = location.num("longitude")
    if (!validPoint(lat, lng)) return@mapNotNull null
    val level = humanEnum(outage.str("severity"), "OUTAGE_SEVERITY_")
    val ended = outage.long("endedAt")?.takeIf { it > 0 }
    RadarSignal(
        id = "outage:$id", layer = RadarLayer.OUTAGES,
        title = outage.str("title") ?: "Internet outage",
        locationName = listOfNotNull(outage.str("region"), outage.str("country")).joinToString(", ").ifBlank { "Location unavailable" },
        lat = lat!!, lng = lng!!,
        observedAt = radarTimestamp(outage.long("detectedAt")),
        severity = listOfNotNull(level, if (ended != null) "ended" else "ongoing").joinToString(" · "),
        summary = listOfNotNull(
            outage.str("description")?.trim(),
            humanEnum(outage.str("cause"))?.let { "Cause: $it." },
            ended?.let { "Ended ${radarTimestamp(it)}." },
        ).joinToString(" ").ifBlank { "Connectivity outage" },
        provider = "Internet outage monitor",
        isSimulated = false, eventSourceUrl = httpUrl(outage.str("link")),
        rank = severityRank(outage.str("severity")?.removePrefix("OUTAGE_SEVERITY_")),
    )
}

/** `{ events: [{ id, title, summary, city, country, location: { latitude, longitude }, occurredAt, severity, sources }] }`. */
internal fun parseUnrestEvents(element: JsonElement): List<RadarSignal> = element.obj().list("events").mapNotNull { event ->
    val id = event.str("id") ?: return@mapNotNull null
    val location = event["location"].obj()
    val lat = location.num("latitude")
    val lng = location.num("longitude")
    if (!validPoint(lat, lng)) return@mapNotNull null
    val severity = humanEnum(event.str("severity"), "SEVERITY_LEVEL_")?.takeUnless { it == "Unspecified" }
    RadarSignal(
        id = "unrest:$id", layer = RadarLayer.UNREST,
        title = event.str("title") ?: "Unrest report",
        locationName = listOfNotNull(event.str("city"), event.str("country")).distinct().joinToString(", ").ifBlank { "Location unavailable" },
        lat = lat!!, lng = lng!!,
        observedAt = radarTimestamp(event.long("occurredAt")),
        severity = severity ?: "Severity not assessed",
        summary = event.str("summary") ?: "Unverified media signal.",
        provider = (event["sources"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Media reports",
        coverage = "Unverified media signal; not an official event record.",
        isSimulated = false, rank = 3.0,
    )
}

private fun formatCoord(value: Double): String = String.format(Locale.ROOT, "%.2f°", value)

/** NASA FIRMS active fire hotspots. */
internal fun parseWildfires(element: JsonElement): List<RadarSignal> {
    val root = element.obj()
    val list = root.list("fireDetections").ifEmpty { root.list("fires") }
    return list.take(150).mapNotNull { item ->
        val lat = item.num("latitude") ?: item.num("lat") ?: return@mapNotNull null
        val lng = item.num("longitude") ?: item.num("lon") ?: return@mapNotNull null
        if (!validPoint(lat, lng)) return@mapNotNull null
        val id = item.str("id") ?: "fire_${lat}_${lng}"
        val brightness = item.num("brightness") ?: item.num("frp") ?: 300.0
        val confidence = item.str("confidence") ?: "nominal"
        val date = item.str("acq_date") ?: ""
        val time = item.str("acq_time") ?: ""
        val source = item.str("source") ?: "NASA FIRMS"
        RadarSignal(
            id = "fire:$id",
            layer = RadarLayer.FIRES,
            title = "Active fire detection (${brightness.toInt()} K)",
            locationName = "${formatCoord(lat)}, ${formatCoord(lng)}",
            lat = lat,
            lng = lng,
            observedAt = listOf(date, time).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Recent" },
            severity = if (brightness > 350.0) "Extreme" else if (brightness > 325.0) "Severe" else "Moderate",
            summary = "Thermal anomaly detected by satellite. Brightness ${brightness.toInt()} K, confidence $confidence.",
            provider = source,
            coverage = "Worldwide satellite detection",
            isSimulated = false,
            rank = if (brightness > 350.0) 4.5 else 3.5,
        )
    }
}

/** Global cyber threat indicators and C2 servers. */
internal fun parseCyberThreats(element: JsonElement): List<RadarSignal> {
    val root = element.obj()
    val list = root.list("threats")
    return list.take(150).mapNotNull { item ->
        val lat = item.num("lat") ?: item.num("latitude") ?: return@mapNotNull null
        val lng = item.num("lon") ?: item.num("longitude") ?: return@mapNotNull null
        if (!validPoint(lat, lng)) return@mapNotNull null
        val id = item.str("id") ?: item.str("indicator") ?: return@mapNotNull null
        val rawType = item.str("type") ?: "cyber_threat"
        val type = humanEnum(rawType) ?: "Cyber threat"
        val malware = item.str("malwareFamily")?.takeIf { it.isNotBlank() } ?: item.str("indicator") ?: "Malware"
        val severity = item.str("severity") ?: "high"
        val country = item.str("country") ?: ""
        RadarSignal(
            id = "cyber:$id",
            layer = RadarLayer.CYBER,
            title = "$type: $malware",
            locationName = listOfNotNull(country.takeIf { it.isNotBlank() }, "${formatCoord(lat)}, ${formatCoord(lng)}").joinToString(" · "),
            lat = lat,
            lng = lng,
            observedAt = "Active",
            severity = severity.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
            summary = "Active cyber infrastructure indicator ($type). Family: $malware. Source: ${item.str("source") ?: "threat intel"}.",
            provider = item.str("source") ?: "Threat Intel",
            coverage = "Global IP telemetry",
            isSimulated = false,
            rank = if (severity.equals("critical", ignoreCase = true)) 4.5 else 4.0,
        )
    }
}

/** Georeferenced armed conflict incidents (UCDP). */
internal fun parseUcdpEvents(element: JsonElement): List<RadarSignal> {
    val root = element.obj()
    val list = root.list("events").ifEmpty { root.list("ucdpEvents") }
    return list.take(150).mapNotNull { item ->
        val lat = item.num("latitude") ?: item.num("lat") ?: return@mapNotNull null
        val lng = item.num("longitude") ?: item.num("lon") ?: return@mapNotNull null
        if (!validPoint(lat, lng)) return@mapNotNull null
        val id = item.str("id") ?: "${lat}_${lng}"
        val dyad = item.str("dyad_name") ?: item.str("conflict_name") ?: "Armed conflict event"
        val deaths = item.num("best_fatalities")?.toInt() ?: item.num("deaths")?.toInt() ?: 0
        val country = item.str("country") ?: ""
        val date = item.str("date_start") ?: item.str("date") ?: "Recent"
        RadarSignal(
            id = "ucdp:$id",
            layer = RadarLayer.CONFLICTS,
            title = dyad,
            locationName = listOfNotNull(country.takeIf { it.isNotBlank() }, "${formatCoord(lat)}, ${formatCoord(lng)}").joinToString(" · "),
            lat = lat,
            lng = lng,
            observedAt = date,
            severity = if (deaths > 10) "Extreme" else if (deaths > 0) "Severe" else "Moderate",
            summary = "UCDP recorded armed conflict event. Fatalities: $deaths.",
            provider = "UCDP Uppsala",
            coverage = "Georeferenced conflict dataset",
            isSimulated = false,
            rank = if (deaths > 10) 5.0 else 4.0,
        )
    }
}

/** Epoch millis from a numeric or ISO-8601 timestamp field, or null. */
private fun timeMillis(obj: JsonObject?, key: String): Long? = obj.long(key) ?: isoMillis(obj.str(key))

/** `{ outbreaks: [{ id, disease, location, countryCode, alertLevel, summary, sourceUrl, publishedAt, sourceName, lat, lng, cases }] }`. */
internal fun parseDiseaseOutbreaks(element: JsonElement): List<RadarSignal> = element.obj().list("outbreaks").mapNotNull { outbreak ->
    val id = outbreak.str("id") ?: return@mapNotNull null
    val lat = outbreak.num("lat")
    val lng = outbreak.num("lng")
    if (!validPoint(lat, lng)) return@mapNotNull null
    val disease = outbreak.str("disease") ?: "Disease outbreak"
    val location = outbreak.str("location") ?: countryCentre(outbreak.str("countryCode"))?.name ?: "Location unavailable"
    val alert = outbreak.str("alertLevel")?.lowercase(Locale.ROOT)
    val cases = outbreak.num("cases")?.takeIf { it > 0 }?.toLong()
    RadarSignal(
        id = "disease:$id", layer = RadarLayer.DISEASE,
        title = "$disease: $location", locationName = location, lat = lat!!, lng = lng!!,
        observedAt = radarTimestamp(outbreak.long("publishedAt")),
        severity = when (alert) { "alert" -> "Alert"; "watch" -> "Watch"; else -> "Level unknown" },
        summary = listOfNotNull(outbreak.str("summary")?.trim(), cases?.let { "Cases reported: $it." }).joinToString(" ").ifBlank { "Disease outbreak report." },
        provider = outbreak.str("sourceName")?.let { "Disease outbreak reports · $it" } ?: "Disease outbreak reports",
        isSimulated = false, eventSourceUrl = httpUrl(outbreak.str("sourceUrl")),
        rank = if (alert == "alert") 4.5 else 3.5,
    )
}

/**
 * `{ source, situations: [{ id, name, origin, sourceUrl, flows: [{ kind, label, origin, originLocation: { latitude, longitude }, measure, total, asOf, countries: [{ country }] }] }] }`.
 * Outflows only; an origin without a point falls back to the origin country's centre.
 */
internal fun parseCrossBorderArrivals(element: JsonElement): List<RadarSignal> {
    val root = element.obj()
    val source = root.str("source")
    return root.list("situations").flatMap { situation ->
        val situationId = situation.str("id") ?: return@flatMap emptyList<RadarSignal>()
        val situationName = situation.str("name") ?: "Displacement situation"
        situation.list("flows").mapNotNull { flow ->
            if (flow.str("kind") != "outflow") return@mapNotNull null
            val origin = flow.str("origin") ?: situation.str("origin")
            val centre = countryCentre(origin)
            val point = flow["originLocation"].obj()
            val lat = point.num("latitude") ?: centre?.lat
            val lng = point.num("longitude") ?: centre?.lng
            if (!validPoint(lat, lng)) return@mapNotNull null
            val total = flow.num("total")?.takeIf { it > 0 }?.toLong()
            val severity = when {
                total == null -> "Figure unavailable"
                total >= 1_000_000 -> "Severe"
                total >= 100_000 -> "Moderate"
                else -> "Low"
            }
            val destinations = flow.list("countries").mapNotNull { it.str("country") }.take(3)
            RadarSignal(
                id = "displacement:$situationId:${origin ?: "unknown"}", layer = RadarLayer.DISPLACEMENT,
                title = flow.str("label") ?: "Refugees from ${centre?.name ?: origin ?: "origin"}",
                locationName = centre?.name ?: origin ?: "Origin unavailable",
                lat = lat!!, lng = lng!!,
                observedAt = radarTimestamp(flow.str("asOf")?.let { isoMillis("${it}T00:00:00Z") }),
                severity = severity,
                summary = listOfNotNull(
                    "Situation: $situationName.",
                    total?.let { "Total ${String.format(Locale.ROOT, "%,d", it)} people (${flow.str("measure") ?: "reported"})." },
                    destinations.takeIf { it.isNotEmpty() }?.let { "Main destinations: ${it.joinToString(", ")}." },
                ).joinToString(" "),
                provider = source ?: "UNHCR",
                coverage = if (point.num("latitude") == null) "Origin is the country centre, not a precise location." else null,
                isSimulated = false, eventSourceUrl = httpUrl(situation.str("sourceUrl")),
                rank = severityRank(severity),
            )
        }
    }
}

/** `{ fetchedAt, countries: [{ countryCode, countryName, entryCount, newEntryCount, vesselCount, aircraftCount }] }`. Country totals at the country centre. */
internal fun parseSanctionsPressure(element: JsonElement): List<RadarSignal> {
    val root = element.obj()
    val observedAt = radarTimestamp(timeMillis(root, "fetchedAt"))
    return root.list("countries").mapNotNull { country ->
        val code = country.str("countryCode") ?: return@mapNotNull null
        // "XX" (unattributed entries) has no centre and is dropped.
        val centre = countryCentre(code) ?: return@mapNotNull null
        val entries = country.long("entryCount")?.takeIf { it > 0 } ?: return@mapNotNull null
        val vessels = country.long("vesselCount") ?: 0L
        val aircraft = country.long("aircraftCount") ?: 0L
        val newEntries = country.long("newEntryCount") ?: 0L
        RadarSignal(
            id = "sanctions:$code", layer = RadarLayer.SANCTIONS,
            title = "Sanctions pressure: ${centre.name}", locationName = centre.name,
            lat = centre.lat, lng = centre.lng, observedAt = observedAt,
            severity = "${String.format(Locale.ROOT, "%,d", entries)} linked entries",
            summary = listOfNotNull(
                "Listed entries linked to ${centre.name}: ${String.format(Locale.ROOT, "%,d", entries)}.",
                vessels.takeIf { it > 0 }?.let { "Vessels: $it." },
                aircraft.takeIf { it > 0 }?.let { "Aircraft: $it." },
                newEntries.takeIf { it > 0 }?.let { "New in latest list: $it." },
            ).joinToString(" "),
            provider = "OFAC sanctions lists",
            coverage = "Country-level count plotted at the country centre, not an event location.",
            isSimulated = false, rank = if (entries >= 1000) 4.0 else 3.5,
        )
    }
}

/** `{ shortages: { [id]: { country, product, severity, firstSeen, lastConfirmed, resolvedAt, impactTypes, shortDescription, evidence: { evidenceSources: [{ authority, url }] } } } }`. Unresolved shortages only. */
internal fun parseFuelShortages(element: JsonElement): List<RadarSignal> {
    val root = element.obj()
    val shortages = (root?.get("shortages") as? JsonObject)?.values?.mapNotNull { it as? JsonObject } ?: root.list("shortages")
    return shortages.mapNotNull { shortage ->
        val id = shortage.str("id") ?: return@mapNotNull null
        if (shortage.str("resolvedAt") != null) return@mapNotNull null
        val centre = countryCentre(shortage.str("country")) ?: return@mapNotNull null
        val product = shortage.str("product")?.replaceFirstChar { it.titlecase(Locale.ROOT) } ?: "Fuel"
        val rawSeverity = shortage.str("severity")
        val severity = when (rawSeverity) { "confirmed" -> "Confirmed"; "watch" -> "Watch"; else -> humanEnum(rawSeverity) ?: "Level unknown" }
        val sources = shortage["evidence"].obj().list("evidenceSources")
        val impacts = (shortage["impactTypes"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
        RadarSignal(
            id = "fuel:$id", layer = RadarLayer.FUEL,
            title = "$product shortage in ${centre.name}", locationName = centre.name,
            lat = centre.lat, lng = centre.lng,
            observedAt = radarTimestamp(isoMillis(shortage.str("lastConfirmed") ?: shortage.str("firstSeen"))),
            severity = severity,
            summary = listOfNotNull(
                shortage.str("shortDescription"),
                impacts.takeIf { it.isNotEmpty() }?.let { "Impacts: ${it.joinToString(", ") { impact -> humanEnum(impact) ?: impact }}." },
            ).joinToString(" ").ifBlank { "Reported fuel supply shortage." },
            provider = sources.firstNotNullOfOrNull { it.str("authority") } ?: "Fuel shortage tracker",
            coverage = "Country-level shortage plotted at the country centre.",
            isSimulated = false, eventSourceUrl = sources.firstNotNullOfOrNull { httpUrl(it.str("url")) },
            rank = if (severity == "Confirmed") 4.0 else 3.5,
        )
    }
}

/**
 * `{ updatedAt, pipelines: { [id]: { id, name, operator, commodityType, fromCountry, toCountry, lengthKm, inService, startPoint: { lat, lon }, evidence: { physicalState, commercialState, operatorStatement: { text, url } } } } }`.
 * Gas and oil registries share this shape. One marker per pipeline, at its start point.
 */
internal fun parsePipelineRegistry(element: JsonElement): List<RadarSignal> {
    val root = element.obj()
    val observedAt = radarTimestamp(timeMillis(root, "updatedAt"))
    val pipelines = (root?.get("pipelines") as? JsonObject)?.values?.mapNotNull { it as? JsonObject }.orEmpty()
    return pipelines.mapNotNull { pipeline ->
        val id = pipeline.str("id") ?: return@mapNotNull null
        // Pipeline start points use lat/lon, not lat/lng.
        val start = pipeline["startPoint"].obj()
        val lat = start.num("lat")
        val lng = start.num("lon")
        if (!validPoint(lat, lng)) return@mapNotNull null
        val route = listOfNotNull(pipeline.str("fromCountry"), pipeline.str("toCountry")).joinToString(" → ").ifBlank { "Route unavailable" }
        val evidence = pipeline["evidence"].obj()
        val statement = evidence?.get("operatorStatement").obj()
        val commodity = humanEnum(pipeline.str("commodityType")) ?: "Pipeline"
        RadarSignal(
            id = "pipeline:$id", layer = RadarLayer.PIPELINES,
            title = pipeline.str("name") ?: id, locationName = route,
            lat = lat!!, lng = lng!!,
            observedAt = observedAt,
            severity = humanEnum(evidence.str("physicalState")) ?: "Status unknown",
            summary = listOfNotNull(
                "$commodity pipeline operated by ${pipeline.str("operator") ?: "an unnamed operator"}.",
                pipeline.num("lengthKm")?.let { "Length ${it.toInt()} km." },
                pipeline.long("inService")?.let { "In service since $it." },
                humanEnum(evidence.str("commercialState"))?.let { "Commercial state: $it." },
                statement.str("text"),
            ).joinToString(" "),
            provider = "Pipeline registry (curated)",
            coverage = "Marker is the pipeline's start point; the route is not drawn.",
            isSimulated = false, eventSourceUrl = httpUrl(statement.str("url")),
        )
    }
}

/** `{ minerals: [{ mineral, topProducers: [{ country, countryCode, sharePct }], hhi, riskRating, globalProduction, unit }], fetchedAt }`. */
internal fun parseMinerals(element: JsonElement): List<RadarSignal> {
    val root = element.obj()
    val observedAt = radarTimestamp(timeMillis(root, "fetchedAt"))
    return root.list("minerals").mapNotNull { mineral ->
        val name = mineral.str("mineral") ?: return@mapNotNull null
        val leader = mineral.list("topProducers").maxByOrNull { it.num("sharePct") ?: 0.0 } ?: return@mapNotNull null
        val leaderName = leader.str("country")
        val centre = countryCentre(leader.str("countryCode")) ?: return@mapNotNull null
        val share = leader.num("sharePct")?.takeIf { it.isFinite() }
        val risk = mineral.str("riskRating")
        val unit = mineral.str("unit").orEmpty()
        RadarSignal(
            id = "mineral:${name.lowercase(Locale.ROOT).replace(' ', '-')}", layer = RadarLayer.MINERALS,
            title = if (share != null) "$name: ${share.toInt()}% from ${leaderName ?: centre.name}" else "$name: ${leaderName ?: centre.name}",
            locationName = leaderName ?: centre.name, lat = centre.lat, lng = centre.lng,
            observedAt = observedAt,
            severity = humanEnum(risk) ?: "Risk not rated",
            summary = listOfNotNull(
                share?.let { "${leaderName ?: centre.name} produces ${it.toInt()}% of supply." },
                mineral.num("hhi")?.let { "Concentration index (HHI) ${it.toInt()}." },
                mineral.num("globalProduction")?.let { "Global production ${it.toLong()} $unit".trimEnd() + "." },
                "Plotted at the leading producer's country centre.",
            ).joinToString(" "),
            provider = "Critical minerals supply data",
            coverage = "Country centre of the leading producer, not a mine location.",
            isSimulated = false,
            rank = when (risk?.lowercase(Locale.ROOT)) { "critical" -> 4.5; "high" -> 4.0; else -> 3.5 },
        )
    }
}

/**
 * `{ anomalies: [{ uuid, type, status, startDate, endDate, asn, asnName, locationCode, locationName, latitude, longitude }] }`.
 * Providers send 0,0 for unknown positions; those fall back to the country centre when a code is present.
 */
internal fun parseTrafficAnomalies(element: JsonElement): List<RadarSignal> = element.obj().list("anomalies").mapNotNull { anomaly ->
    val id = anomaly.str("uuid") ?: return@mapNotNull null
    val centre = countryCentre(anomaly.str("locationCode"))
    val reportedLat = anomaly.num("latitude")
    val reportedLng = anomaly.num("longitude")
    val fromCountry = !validPoint(reportedLat, reportedLng)
    val lat = if (fromCountry) centre?.lat else reportedLat
    val lng = if (fromCountry) centre?.lng else reportedLng
    if (!validPoint(lat, lng)) return@mapNotNull null
    val network = anomaly.str("asnName")?.let { name -> anomaly.str("asn")?.let { "$name (AS$it)" } ?: name }
    val scope = when (anomaly.str("type")) { "AS" -> "Network"; "LOCATION" -> "Country"; else -> "Traffic" }
    val ended = anomaly.long("endDate")?.takeIf { it > 0 }
    RadarSignal(
        id = "traffic:$id", layer = RadarLayer.OUTAGES,
        title = "Traffic anomaly: ${network ?: centre?.name ?: anomaly.str("locationName") ?: "unknown network"}",
        locationName = anomaly.str("locationName") ?: centre?.name ?: "Network-wide",
        lat = lat!!, lng = lng!!,
        observedAt = radarTimestamp(anomaly.long("startDate")),
        severity = if (anomaly.str("status") == "VERIFIED") "Verified" else "Reported",
        summary = listOfNotNull(
            "Cloudflare Radar $scope-level traffic anomaly.",
            if (ended != null) "Ended ${radarTimestamp(ended)}." else "Ongoing at last update.",
        ).joinToString(" "),
        provider = "Cloudflare Radar",
        coverage = if (fromCountry) "Position is the country centre, not the affected network." else null,
        isSimulated = false, rank = 3.5,
    )
}
