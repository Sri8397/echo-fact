package com.taurotechnologies.godseye.nativeapp.data

import java.util.Locale

/**
 * Where the globe draws rain, clouds and thunder: one cell per official weather alert from the app's
 * `weatherAlerts` feed whose event brings rain. Alerts without rain (heat, wind, fog, cold, snow, fire
 * weather) and alerts placed only at a country centre make no cell, so the globe never shows weather
 * that the data does not report.
 */
data class WeatherCell(
    val lat: Double,
    val lng: Double,
    /** Thunder and lightning as well as rain. */
    val thunder: Boolean,
    /** 0..1 from the alert severity: how heavy the rain is at the alert centre. */
    val intensity: Double,
)

object WeatherEffects {
    private val THUNDER = listOf("thunder", "tornado", "lightning", "severe storm", "hail")
    private val RAIN = listOf(
        "rain", "flood", "shower", "storm", "hurricane", "typhoon", "cyclone", "monsoon", "squall", "downpour",
    )
    private val NOT_RAIN = listOf("snow", "blizzard", "ice storm", "winter storm", "freez", "dust storm", "sandstorm")

    fun cells(signals: List<RadarSignal>): List<WeatherCell> = signals.mapNotNull(::cell)

    fun cell(signal: RadarSignal): WeatherCell? {
        if (signal.layer != RadarLayer.WEATHER || signal.coverage != null) return null
        val text = "${signal.title} ${signal.summary}".lowercase(Locale.ROOT)
        val thunder = THUNDER.any { it in text }
        val rain = thunder || (RAIN.any { it in text } && NOT_RAIN.none { it in text })
        if (!rain) return null
        return WeatherCell(signal.lat, signal.lng, thunder, intensity(signal.severity))
    }

    /** Alert severity to rain strength. */
    fun intensity(severity: String?): Double = when (severity?.lowercase(Locale.ROOT)) {
        "extreme", "red" -> 1.0
        "severe", "orange" -> 0.8
        "moderate", "yellow" -> 0.6
        "minor" -> 0.4
        else -> 0.5
    }
}
