package com.taurotechnologies.godseye.nativeapp.data

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** A place the user can jump the map to: a country, state/region or city/locality. */
data class PlaceHit(val name: String, val detail: String, val lat: Double, val lng: Double, val zoom: Double)

/**
 * Place search for the home search box. Countries match offline against [COUNTRY_CENTRES]; states, cities and
 * other places come from OpenStreetMap Nominatim (needs network, best effort).
 */
object PlaceSearch {
    fun countryMatches(term: String, limit: Int = 4): List<PlaceHit> {
        val t = term.trim()
        if (t.length < 2) return emptyList()
        return COUNTRY_CENTRES.values
            .filter { it.name.contains(t, ignoreCase = true) }
            .sortedBy { if (it.name.startsWith(t, ignoreCase = true)) 0 else 1 }
            .take(limit)
            .map { PlaceHit(it.name, "Country", it.lat, it.lng, 4.0) }
    }

    suspend fun remote(term: String, limit: Int = 6): List<PlaceHit> = withContext(Dispatchers.IO) {
        val t = term.trim()
        if (t.length < 2) return@withContext emptyList()
        runCatching {
            val url = URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=$limit&q=${URLEncoder.encode(t, "UTF-8")}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "GodsEye-Android/1.0")
                setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag())
            }
            try {
                if (conn.responseCode != 200) return@runCatching emptyList<PlaceHit>()
                val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.getJSONObject(i)
                    val lat = o.optString("lat").toDoubleOrNull() ?: return@mapNotNull null
                    val lng = o.optString("lon").toDoubleOrNull() ?: return@mapNotNull null
                    val full = o.optString("display_name")
                    val type = o.optString("addresstype").ifBlank { o.optString("type") }
                    val zoom = when (type) {
                        "country" -> 4.0
                        "state", "region", "province", "state_district" -> 5.0
                        "city", "town", "municipality" -> 8.0
                        else -> 9.0
                    }
                    PlaceHit(
                        name = o.optString("name").ifBlank { full.substringBefore(',') },
                        detail = full.substringAfter(", ", type.replaceFirstChar { it.uppercase() }),
                        lat = lat, lng = lng, zoom = zoom,
                    )
                }
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(emptyList())
    }
}
