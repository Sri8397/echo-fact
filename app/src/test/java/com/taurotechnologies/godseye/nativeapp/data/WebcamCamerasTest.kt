package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WebcamCamerasTest {
    @Test
    fun videoIdComesFromEveryYouTubeLinkShape() {
        val id = "zp6LNSoq000"
        assertEquals(id, WebcamCameras.youtubeVideoId("https://www.youtube.com/watch?v=$id"))
        assertEquals(id, WebcamCameras.youtubeVideoId("https://www.youtube.com/watch?feature=share&v=$id"))
        assertEquals(id, WebcamCameras.youtubeVideoId("https://www.youtube.com/embed/$id?autoplay=1"))
        assertEquals(id, WebcamCameras.youtubeVideoId("https://www.youtube.com/live/$id"))
        assertEquals(id, WebcamCameras.youtubeVideoId("https://youtu.be/$id"))
    }

    @Test
    fun nonYouTubeLinksHaveNoVideoId() {
        assertNull(WebcamCameras.youtubeVideoId(null))
        assertNull(WebcamCameras.youtubeVideoId("https://example.com/cam.html"))
        assertNull(WebcamCameras.youtubeVideoId("https://www.youtube.com/channel/UC123"))
    }

    @Test
    fun catalogCamerasParseWithCoordinatesOnlyWhereKnown() {
        val cameras = WebcamCameras.parse(bundledCatalog())
        assertTrue(cameras.size >= 15)
        // Dead streams sit in the catalog's `offline` list, so they are neither listed nor placed on the radar.
        assertTrue(cameras.none { it.id == "istanbul-1" })
        val tokyo = cameras.first { it.id == "tokyo-1" }
        assertEquals(35.6762, tokyo.lat!!, 0.0001)
        assertEquals(139.6503, tokyo.lng!!, 0.0001)
        // Space feeds have no fixed place: they stay in Explore but are not placed on the radar.
        assertNull(cameras.first { it.id == "iss-earth-1" }.lat)
    }

    @Test
    fun radarSignalsOnlyForCamerasWithAPlace() {
        val cameras = WebcamCameras.parse(bundledCatalog())
        val signals = WebcamCameras.radarSignals(cameras)
        assertEquals(cameras.count { it.lat != null && it.lng != null }, signals.size)
        assertTrue(signals.all { it.layer == RadarLayer.WEBCAMS && !it.isSimulated })
        val paris = signals.first { it.id == "webcam:paris-1" }
        assertEquals("Paris", paris.title)
        assertEquals("-xzg3wujOVM", WebcamCameras.youtubeVideoId(paris.eventSourceUrl))
    }

    @Test
    fun embedUrlAddsQualityOnlyWhenChosen() {
        assertEquals(
            "https://www.youtube.com/embed/abc?autoplay=1&playsinline=1&rel=0&modestbranding=1",
            WebcamCameras.embedUrl("abc", null),
        )
        assertTrue(WebcamCameras.embedUrl("abc", "hd720").endsWith("&vq=hd720"))
    }

    private fun bundledCatalog(): ExploreCatalog {
        val text = File("src/main/assets/explore-catalog.v1.json").readText()
        return checkNotNull(ExploreCatalog.parse(text))
    }
}
