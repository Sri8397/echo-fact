package com.taurotechnologies.godseye.nativeapp.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.util.Locale

/**
 * One live camera from the Explore catalog's `live-webcams` panel. [lat] and [lng] are null for cameras
 * with no fixed place (space feeds, regional feeds), which stay in Explore but are not placed on the radar.
 */
data class WebcamCamera(
    val id: String,
    val city: String,
    val region: String,
    val detail: String,
    val url: String,
    val lat: Double?,
    val lng: Double?,
)

/** Live webcams: read from the Explore catalog, placed on the radar as a layer, played in the app. */
object WebcamCameras {
    const val PANEL_ID = "live-webcams"

    private val youtubeIdPattern = Regex(
        """(?:youtube\.com/(?:watch\?(?:.*&)?v=|embed/|live/|shorts/)|youtu\.be/)([A-Za-z0-9_-]{11})""",
    )

    /** The 11-character YouTube video id in a watch, embed, live, shorts or youtu.be link, or null. */
    fun youtubeVideoId(url: String?): String? = url?.let { youtubeIdPattern.find(it)?.groupValues?.get(1) }

    /** Every camera in the catalog's live-webcams panel, in catalog order. Empty when the panel is missing. */
    fun parse(catalog: ExploreCatalog): List<WebcamCamera> {
        val data = catalog.panels.firstOrNull { it.id == PANEL_ID }?.source?.data as? JsonObject ?: return emptyList()
        val cameras = data["cameras"] as? JsonArray ?: return emptyList()
        return cameras.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            fun text(key: String): String? = (obj[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
            val id = text("id") ?: return@mapNotNull null
            val url = text("url")?.takeIf { youtubeVideoId(it) != null } ?: return@mapNotNull null
            WebcamCamera(
                id = id,
                city = text("city") ?: id,
                region = text("region").orEmpty(),
                detail = text("detail").orEmpty(),
                url = url,
                lat = (obj["lat"] as? JsonPrimitive)?.doubleOrNull,
                lng = (obj["lng"] as? JsonPrimitive)?.doubleOrNull,
            )
        }
    }

    /** Radar signals for the cameras that have a fixed place. Each keeps its YouTube link as its source. */
    fun radarSignals(cameras: List<WebcamCamera>): List<RadarSignal> = cameras.mapNotNull { camera ->
        val lat = camera.lat ?: return@mapNotNull null
        val lng = camera.lng ?: return@mapNotNull null
        RadarSignal(
            id = "webcam:${camera.id}",
            layer = RadarLayer.WEBCAMS,
            title = camera.city,
            locationName = "${camera.detail.ifBlank { camera.region }} · ${String.format(Locale.ROOT, "%.3f, %.3f", lat, lng)}",
            lat = lat,
            lng = lng,
            observedAt = "Live now",
            severity = "Live camera",
            summary = "Live stream from ${camera.city}. Watch it in the app. Streams can go offline.",
            provider = "YouTube live stream",
            coverage = "Curated live camera list",
            isSimulated = false,
            eventSourceUrl = camera.url,
        )
    }

    /**
     * The embed page's player source. [quality] is a YouTube `vq` value, or null for Auto. A [preview] player
     * is muted and has no controls, for the small inline view in the details sheet.
     */
    fun embedUrl(videoId: String, quality: String?, preview: Boolean = false): String {
        val params = buildList {
            add("autoplay=1")
            add("playsinline=1")
            add("rel=0")
            add("modestbranding=1")
            if (preview) {
                add("mute=1")
                add("controls=0")
                add("disablekb=1")
                add("fs=0")
            }
            if (!quality.isNullOrBlank() && quality != STREAM_QUALITY_AUTO) add("vq=$quality")
        }
        return "https://www.youtube.com/embed/$videoId?${params.joinToString("&")}"
    }
}
