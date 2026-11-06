package com.just_sri.suvidha.radar.ui.components

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class GlobeProjectorTest {
    private fun rad(degrees: Double) = Math.toRadians(degrees).toFloat()

    @Test fun cameraTargetProjectsToCenterAndAntipodeIsHidden() {
        val projector = GlobeProjector(77f, 28f, 360f, 400f, 300f)
        val point = FloatArray(2)
        assertTrue(projector.project(rad(77.0), rad(28.0), point))
        assertEquals(360f, point[0], 0.001f)
        assertEquals(400f, point[1], 0.001f)
        assertFalse(projector.project(rad(-103.0), rad(-28.0), point))
    }

    @Test fun datelineRemainsContinuous() {
        val point = FloatArray(2)
        assertTrue(GlobeProjector(179f, 0f, 0f, 0f, 100f).project(rad(-179.0), 0f, point))
        assertEquals(3.49f, point[0], 0.02f)
    }

    @Test fun markerProjectionMatchesTextureInverseAcrossCameraPoses() {
        // Inverse is the ray/sphere geographic sampling used by EarthRenderer's shader.
        for ((lon0, lat0) in listOf(0.0 to 20.0, 140.0 to -30.0, -170.0 to 70.0)) {
            val point = FloatArray(2)
            for (dLon in listOf(-25.0, 0.0, 25.0)) {
                val lon = lon0 + dLon
                val lat = lat0 - 8.0
                assertTrue(GlobeProjector(lon0.toFloat(), lat0.toFloat(), 0f, 0f, 1f).project(rad(lon), rad(lat), point))
                val x = point[0].toDouble()
                val y = -point[1].toDouble()
                val z = sqrt(max(0.0, 1.0 - x*x - y*y))
                val phi = Math.toRadians(lat0)
                val sampledLat = asin(y*cos(phi) + z*sin(phi))
                val sampledLon = Math.toRadians(lon0) + atan2(x, z*cos(phi)-y*sin(phi))
                assertEquals(lat, Math.toDegrees(sampledLat), 0.001)
                assertEquals(lon, Math.toDegrees(sampledLon), 0.001)
            }
        }
    }
}
