package com.taurotechnologies.godseye.nativeapp.data

/**
 * 2D map theme and 3D globe style choices, ported from the web settings
 * (`src/config/basemap.ts`, `src/services/globe-render-settings.ts`).
 *
 * The native 2D map stays on OpenStreetMap raster tiles. The web's other providers are not
 * offered: OpenFreeMap and PMTiles are vector-only, and CARTO's raster tiles now require an
 * API key (keyless requests return an "API key required" placeholder tile).
 */
val MAP_THEME_OPTIONS: List<Pair<String, String>> = listOf(
    "auto" to "Match app theme",
    "light" to "Light",
    "dark" to "Dark",
)

const val DEFAULT_MAP_THEME: String = "auto"

const val GLOBE_PRESET_CLASSIC: String = "classic"
const val GLOBE_PRESET_ENHANCED: String = "enhanced"

/** Same values and labels as the web `GLOBE_VISUAL_PRESET_OPTIONS`. */
val GLOBE_PRESET_OPTIONS: List<Pair<String, String>> = listOf(
    GLOBE_PRESET_CLASSIC to "Earth",
    GLOBE_PRESET_ENHANCED to "Cosmos",
)

fun mapThemeLabel(theme: String): String = MAP_THEME_OPTIONS.firstOrNull { it.first == theme }?.second ?: theme

fun globePresetLabel(preset: String): String = GLOBE_PRESET_OPTIONS.firstOrNull { it.first == preset }?.second ?: preset

/** True when the light OpenStreetMap tiles should be recoloured into dark slate tones. */
fun usesDarkMapTiles(theme: String, appDark: Boolean): Boolean = when (theme) {
    "light" -> false
    "dark" -> true
    else -> appDark
}
