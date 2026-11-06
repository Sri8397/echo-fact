package com.just_sri.suvidha.radar.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FlatRadarGestureTest {
    @Test fun draggingRightMovesAnExistingMarkerRight() {
        val center = longitudeAfterGesture(0f, 0f, 120f, 720f, 720f)
        val markerX = 360f + (0f - center) / 360f * 720f
        assertEquals(480f, markerX, 0.001f)
    }

    @Test fun pinchKeepsTouchedLongitudeAtTheGestureFocus() {
        val oldCenter = 25f
        val touchedLongitude = oldCenter + 100f / 720f * 360f
        val center = longitudeAfterGesture(oldCenter, 100f, 0f, 720f, 1440f)
        val markerOffset = (touchedLongitude - center) / 360f * 1440f
        assertEquals(100f, markerOffset, 0.001f)
    }
}
