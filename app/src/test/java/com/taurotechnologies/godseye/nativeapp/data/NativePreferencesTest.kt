package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativePreferencesTest {
    @Test
    fun newSettingsDefaultToTheWebDefaults() {
        val value = NativePreferences()
        assertEquals(LANGUAGE_SYSTEM, value.appLanguage)
        assertEquals(MAP_PROVIDER_AUTO, value.mapProvider)
        assertEquals(DEFAULT_MAP_THEME, value.mapTheme)
        assertEquals(GLOBE_PRESET_CLASSIC, value.globePreset)
        assertEquals(THEATER_PRESET_NONE, value.theaterPreset)
        assertEquals(STREAM_QUALITY_AUTO, value.streamQuality)
        assertEquals(60, value.idleStopMinutes)
        assertEquals(emptySet<String>(), value.hiddenExplorePanels)
        assertEquals(emptySet<String>(), value.hiddenExploreCategories)
    }

    @Test
    fun unknownNewChoicesFallBackToDefaults() {
        val value = NativePreferences(
            appLanguage = "klingon",
            mapProvider = "bing",
            theaterPreset = "mars",
            streamQuality = "4k",
            idleStopMinutes = 45,
            mapTheme = "neon",
            globePreset = "retro",
        ).sanitized()
        assertEquals(LANGUAGE_SYSTEM, value.appLanguage)
        assertEquals(MAP_PROVIDER_AUTO, value.mapProvider)
        assertEquals(THEATER_PRESET_NONE, value.theaterPreset)
        assertEquals(STREAM_QUALITY_AUTO, value.streamQuality)
        assertEquals(DEFAULT_IDLE_STOP_MINUTES, value.idleStopMinutes)
        assertEquals(DEFAULT_MAP_THEME, value.mapTheme)
        assertEquals(GLOBE_PRESET_CLASSIC, value.globePreset)
    }

    @Test
    fun validNewChoicesSurviveSanitizing() {
        val value = NativePreferences(
            appLanguage = "zh-TW",
            mapProvider = MAP_PROVIDER_ESRI,
            theaterPreset = "ukraine-war",
            streamQuality = "hd720",
            idleStopMinutes = 0,
        )
        assertEquals(value, value.sanitized())
    }

    @Test
    fun everyOfferedChoiceIsAcceptedByItsSanitizer() {
        APP_LANGUAGE_OPTIONS.forEach { (code, _) -> assertEquals(code, NativePreferences(appLanguage = code).sanitized().appLanguage) }
        MAP_PROVIDER_OPTIONS.forEach { (code, _) -> assertEquals(code, NativePreferences(mapProvider = code).sanitized().mapProvider) }
        THEATER_PRESET_OPTIONS.forEach { (code, _) -> assertEquals(code, NativePreferences(theaterPreset = code).sanitized().theaterPreset) }
        STREAM_QUALITY_OPTIONS.forEach { (code, _) -> assertEquals(code, NativePreferences(streamQuality = code).sanitized().streamQuality) }
        IDLE_STOP_MINUTES_OPTIONS.forEach { (minutes, _) -> assertEquals(minutes, NativePreferences(idleStopMinutes = minutes).sanitized().idleStopMinutes) }
    }

    @Test
    fun languageOptionsMatchTheWebLocalesOnce() {
        val codes = APP_LANGUAGE_OPTIONS.map { it.first }
        assertEquals(codes.size, codes.toSet().size)
        assertEquals(LANGUAGE_SYSTEM, codes.first())
        // The 28 catalogue codes in src/services/i18n.ts SUPPORTED_LANGUAGES, plus the system choice.
        val web = listOf(
            "en", "bg", "cs", "fr", "de", "el", "es", "hr", "hu", "it", "pl", "pt", "nl", "sv", "ru", "uk",
            "ar", "fa", "zh", "zh-TW", "ja", "ko", "ro", "tr", "th", "vi", "hi", "sw",
        )
        assertEquals(web.toSet(), codes.drop(1).toSet())
        assertEquals(29, codes.size)
    }

    @Test
    fun exploreKeysAreCleanedAndCapped() {
        val value = NativePreferences(
            hiddenExplorePanels = setOf("ok-panel", "", "  padded ", "x".repeat(81)),
            hiddenExploreCategories = setOf("Markets", "\t"),
        ).sanitized()
        assertEquals(setOf("ok-panel"), value.hiddenExplorePanels)
        assertEquals(setOf("Markets"), value.hiddenExploreCategories)
        val many = NativePreferences(hiddenExplorePanels = (1..600).map { "panel-$it" }.toSet()).sanitized()
        assertEquals(500, many.hiddenExplorePanels.size)
    }

    @Test
    fun exploreVisibilityFollowsPanelAndCategoryToggles() {
        assertTrue(NativePreferences().isExplorePanelVisible("markets-quotes", "Markets"))
        val hiddenPanel = NativePreferences(hiddenExplorePanels = setOf("markets-quotes"))
        assertFalse(hiddenPanel.isExplorePanelVisible("markets-quotes", "Markets"))
        assertTrue(hiddenPanel.isExplorePanelVisible("markets-tokens", "Markets"))
        val hiddenCategory = NativePreferences(hiddenExploreCategories = setOf("Markets"))
        assertFalse(hiddenCategory.isExplorePanelVisible("markets-tokens", "Markets"))
        assertTrue(hiddenCategory.isExplorePanelVisible("weather", "Weather"))
    }

    @Test
    fun defaultsMatchTheNativeProductSettings() {
        assertEquals(NativePreferences(), NativePreferences().sanitized())
    }

    @Test
    fun invalidPersistedChoicesFallBackToSafeDefaults() {
        val value = NativePreferences(
            textScale = 1.2f,
            minimumMagnitude = Double.NaN,
            refreshMinutes = -4,
        ).sanitized()
        assertEquals(1.0f, value.textScale)
        assertEquals(2.5, value.minimumMagnitude, 0.0)
        assertEquals(DEFAULT_REFRESH_MINUTES, value.refreshMinutes)
        assertEquals("live refresh is on by default", 1, NativePreferences().refreshMinutes)
    }

    @Test
    fun formatsAltitudeAndSpeedInMetricAndAviationUnits() {
        assertEquals("1,000 m", formatFlightAltitude(1_000.0, aviation = false))
        assertEquals("3,281 ft", formatFlightAltitude(1_000.0, aviation = true))
        assertEquals("100 m/s", formatFlightSpeed(100.0, aviation = false))
        assertEquals("194 kt", formatFlightSpeed(100.0, aviation = true))
    }

    @Test
    fun missingOrNonFiniteMeasurementsAreUnknown() {
        assertEquals("Unknown", formatFlightAltitude(null, aviation = false))
        assertEquals("Unknown", formatFlightAltitude(Double.NaN, aviation = true))
        assertEquals("Unknown", formatFlightSpeed(null, aviation = false))
        assertEquals("Unknown", formatFlightSpeed(Double.POSITIVE_INFINITY, aviation = true))
    }

    @Test
    fun lastContactShowsRelativeAgeAndHidesAbsentTimes() {
        val now = 1_000_000L
        assertEquals("12s ago", formatFlightLastContact(now - 12, now))
        assertEquals("3 min ago", formatFlightLastContact(now - 180, now))
        assertEquals("2 h ago", formatFlightLastContact(now - 7_200, now))
        assertEquals("3 d ago", formatFlightLastContact(now - 259_200, now))
        assertEquals("0s ago", formatFlightLastContact(now + 5, now))
        assertNull(formatFlightLastContact(null, now))
        assertNull(formatFlightLastContact(0L, now))
    }
}
