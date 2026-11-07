package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStyleTest {
    @Test
    fun defaultsKeepTheThemeFollowingMapAndEarthGlobe() {
        val defaults = NativePreferences()
        assertEquals("auto", defaults.mapTheme)
        assertEquals(GLOBE_PRESET_CLASSIC, defaults.globePreset)
    }

    @Test
    fun autoMapThemeFollowsTheAppAndExplicitThemesDoNot() {
        assertTrue(usesDarkMapTiles("auto", appDark = true))
        assertFalse(usesDarkMapTiles("auto", appDark = false))
        assertTrue(usesDarkMapTiles("dark", appDark = false))
        assertFalse(usesDarkMapTiles("light", appDark = true))
    }

    @Test
    fun invalidStoredChoicesFallBackToDefaults() {
        val value = NativePreferences(mapTheme = "dark-matter", globePreset = "neon").sanitized()
        assertEquals("auto", value.mapTheme)
        assertEquals(GLOBE_PRESET_CLASSIC, value.globePreset)
    }

    @Test
    fun globeLabelsMatchTheWebSettings() {
        assertEquals("Earth", globePresetLabel(GLOBE_PRESET_CLASSIC))
        assertEquals("Cosmos", globePresetLabel(GLOBE_PRESET_ENHANCED))
        assertEquals("Match app theme", mapThemeLabel("auto"))
    }
}
