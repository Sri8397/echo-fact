package com.taurotechnologies.godseye.nativeapp.ui.components

import androidx.compose.ui.graphics.Color
import com.taurotechnologies.godseye.nativeapp.data.RadarLayer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CesiumMapTest {
    @Test
    fun markersJsonCarriesEveryLayerMarkerWithColorAndAltitude() {
        val markers = RadarLayer.entries.mapIndexed { index, layer ->
            GlobeMarker(id = "id-$index", lat = 10.0 + index, lng = 20.0, color = Color(0xFF00FF00), layer = layer)
        }
        // Stub the icon renderer: it draws android.graphics.Bitmap, which plain JVM tests cannot create.
        val array = Json.parseToJsonElement(markersJson(markers, iconFor = { name, _ -> "icon:$name" })).jsonArray

        assertEquals(RadarLayer.entries.size, array.size)
        val first = array[0].jsonObject
        assertEquals("id-0", first.getValue("id").jsonPrimitive.content)
        assertEquals(10.0, first.getValue("lat").jsonPrimitive.content.toDouble(), 1e-9)
        assertEquals("#00FF00", first.getValue("color").jsonPrimitive.content)
        assertEquals(0.0, first.getValue("alt").jsonPrimitive.content.toDouble(), 1e-9)
    }

    @Test
    fun markerWithoutColorSendsNullSoTheWebPageUsesItsDefault() {
        val array = Json.parseToJsonElement(markersJson(listOf(GlobeMarker("a", 1.0, 2.0)))) as JsonArray
        assertTrue(array[0].jsonObject.getValue("color").toString() == "null")
    }

    @Test
    fun markerIdsAreEscapedAsJsonStrings() {
        val array = Json.parseToJsonElement(markersJson(listOf(GlobeMarker("a\"b\\c", 0.0, 0.0)))).jsonArray
        assertEquals("a\"b\\c", array[0].jsonObject.getValue("id").jsonPrimitive.content)
    }

    @Test
    fun zoomAndHeightAreInverses() {
        for (zoom in listOf(1.0, 2.0, 2.6, 5.0, 12.0)) {
            assertEquals(zoom, heightToZoom(zoomToHeight(zoom)), 1e-9)
        }
        assertTrue(zoomToHeight(3.0) < zoomToHeight(2.0))
    }

    @Test
    fun viewportContainsHandlesAntimeridianCrossing() {
        val crossing = ViewportBounds(south = -10.0, north = 10.0, west = 170.0, east = -170.0)
        assertTrue(crossing.contains(0.0, 175.0))
        assertTrue(crossing.contains(0.0, -175.0))
        assertFalse(crossing.contains(0.0, 0.0))
    }

    @Test
    fun movingMarkerSendsItsRateSoTheGlobeCanGlideIt() {
        val motion = MarkerMotion.between(10.0, 179.9, 10.01, -179.9, 1.0)!!
        assertEquals(0.01, motion.latDegPerSec, 1e-9)
        assertEquals(0.2, motion.lngDegPerSec, 1e-9) // short way across the antimeridian
        assertTrue(MarkerMotion.between(1.0, 2.0, 1.0, 2.0, 1.0) == null)

        val json = markersJson(
            listOf(GlobeMarker("f", 1.0, 2.0, isFlight = true, motion = motion), GlobeMarker("s", 0.0, 0.0)),
            iconFor = { name, _ -> "icon:$name" },
        )
        val array = Json.parseToJsonElement(json).jsonArray
        val rate = array[0].jsonObject.getValue("rate").jsonArray
        assertEquals(0.2, rate[1].jsonPrimitive.content.toDouble(), 1e-9)
        assertFalse(array[1].jsonObject.containsKey("rate"))
    }
}
