package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherEffectsTest {
    private fun alert(title: String, severity: String = "Severe", coverage: String? = null, layer: RadarLayer = RadarLayer.WEATHER) =
        RadarSignal(
            id = "w", layer = layer, title = title, locationName = "Area", lat = 40.0, lng = -76.0,
            observedAt = "now", severity = severity, summary = title, coverage = coverage, isSimulated = false,
        )

    @Test
    fun thunderstormAlertBringsRainAndThunder() {
        val cell = WeatherEffects.cell(alert("Severe Thunderstorm Warning"))!!
        assertTrue(cell.thunder)
        assertEquals(0.8, cell.intensity, 1e-9)
    }

    @Test
    fun floodAlertBringsRainWithoutThunder() {
        val cell = WeatherEffects.cell(alert("Flash Flood Warning", severity = "Extreme"))!!
        assertFalse(cell.thunder)
        assertEquals(1.0, cell.intensity, 1e-9)
    }

    @Test
    fun alertsWithoutRainMakeNoWeather() {
        assertNull(WeatherEffects.cell(alert("Heat Advisory")))
        assertNull(WeatherEffects.cell(alert("Winter Storm Warning")))
        assertNull(WeatherEffects.cell(alert("Dense Fog Advisory")))
    }

    @Test
    fun countryCentreAlertsAndOtherLayersAreSkipped() {
        assertNull(WeatherEffects.cell(alert("Heavy Rain Warning", coverage = "Position is the country centre, not the alert area.")))
        assertNull(WeatherEffects.cell(alert("Thunderstorm", layer = RadarLayer.SEISMIC)))
    }
}
