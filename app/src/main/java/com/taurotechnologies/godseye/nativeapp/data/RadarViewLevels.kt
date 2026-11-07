package com.taurotechnologies.godseye.nativeapp.data

/**
 * How high the camera sits above the ground when a radar item is selected, per layer. Selecting an item flies
 * the camera to this height over it. Flights are watched from 50 km, satellites from 700 km, and every other
 * layer from 1 km. Change a layer's level here and every selection path (map tap, card, list) follows.
 */
object RadarViewLevels {
    const val DEFAULT_M = 1_000.0

    private val byLayer: Map<RadarLayer, Double> = mapOf(
        RadarLayer.FLIGHTS to 50_000.0,
        RadarLayer.SATELLITES to 700_000.0,
    )

    fun heightMeters(layer: RadarLayer?): Double = layer?.let { byLayer[it] } ?: DEFAULT_M
}
