package com.taurotechnologies.godseye.nativeapp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private val ALLOWED_TEXT_SCALES = setOf(1.0f, 1.15f, 1.3f)
private val ALLOWED_MAGNITUDES = setOf(2.5, 4.0, 5.0)
private val ALLOWED_REFRESH_MINUTES = setOf(0, 1, 5)
const val DEFAULT_REFRESH_MINUTES = 1

/** Follows the device language. Any other value is a code from [APP_LANGUAGE_OPTIONS]. */
const val LANGUAGE_SYSTEM = "system"

/** Same codes and self-names as the web `LANGUAGES` list in `src/services/i18n.ts`. Labels are endonyms, not translations. */
val APP_LANGUAGE_OPTIONS: List<Pair<String, String>> = listOf(
    LANGUAGE_SYSTEM to "System default",
    "en" to "English",
    "bg" to "Български",
    "ar" to "العربية",
    "fa" to "فارسی",
    "cs" to "Čeština",
    "zh" to "简体中文",
    "zh-TW" to "繁體中文",
    "fr" to "Français",
    "de" to "Deutsch",
    "el" to "Ελληνικά",
    "es" to "Español",
    "hr" to "Hrvatski",
    "hu" to "Magyar",
    "it" to "Italiano",
    "pl" to "Polski",
    "pt" to "Português",
    "nl" to "Nederlands",
    "sv" to "Svenska",
    "ru" to "Русский",
    "uk" to "Українська",
    "ja" to "日本語",
    "ko" to "한국어",
    "ro" to "Română",
    "th" to "ไทย",
    "tr" to "Türkçe",
    "vi" to "Tiếng Việt",
    "hi" to "हिन्दी",
    "sw" to "Kiswahili",
)

/**
 * Map imagery style for the globe and 2D map. Automatic is satellite imagery: Cesium ion when a token is
 * configured, otherwise Esri. The ids are sent to the globe page as-is (see `setImageryStyle` in globe.html).
 */
const val MAP_PROVIDER_AUTO = "auto"
const val MAP_PROVIDER_ESRI = "esri"
val MAP_PROVIDER_OPTIONS: List<Pair<String, String>> = listOf(
    MAP_PROVIDER_AUTO to "Satellite",
    MAP_PROVIDER_ESRI to "Satellite (Esri)",
    "hybrid" to "Satellite + labels",
    "streets" to "Streets",
    "topo" to "Terrain",
    "dark" to "Dark",
    "light" to "Light",
)

/** The styles offered as quick chips on the map (the Esri-only satellite stays in Settings). */
val MAP_STYLE_CHIP_OPTIONS: List<Pair<String, String>> = MAP_PROVIDER_OPTIONS.filter { it.first != MAP_PROVIDER_ESRI }

/** Coverage presets from the web `THEATER_PRESETS` ids. Applying one is left to the map screen. */
const val THEATER_PRESET_NONE = "none"
val THEATER_PRESET_OPTIONS: List<Pair<String, String>> = listOf(
    THEATER_PRESET_NONE to "Off",
    "ukraine-war" to "Ukraine War",
    "indo-pacific" to "Indo-Pacific / Taiwan",
    "sahel-west-africa" to "Sahel & West Africa",
    "caucasus-central-asia" to "Caucasus & Central Asia",
    "middle-east-flashpoints" to "Middle East Flashpoints",
)

/** Same values and labels as the web `StreamQuality` options. */
const val STREAM_QUALITY_AUTO = "auto"
val STREAM_QUALITY_OPTIONS: List<Pair<String, String>> = listOf(
    STREAM_QUALITY_AUTO to "Auto",
    "small" to "Low (360p)",
    "medium" to "Medium (480p)",
    "large" to "High (480p+)",
    "hd720" to "HD (720p)",
)

/** Idle minutes before live video stops. 0 is Never. Same options as the web `LIVE_MEDIA_IDLE_STOP_OPTIONS`. */
const val DEFAULT_IDLE_STOP_MINUTES = 60
val IDLE_STOP_MINUTES_OPTIONS: List<Pair<Int, String>> = listOf(
    15 to "15 minutes",
    30 to "30 minutes",
    60 to "1 hour",
    120 to "2 hours",
    240 to "4 hours",
    0 to "Never",
)

private const val MAX_HIDDEN_EXPLORE_KEYS = 500
private const val MAX_EXPLORE_KEY_LENGTH = 80

/** Display preferences for the radar and live flight view. */
data class NativePreferences(
    val showMap: Boolean = true,
    val reduceMotion: Boolean = false,
    val monoFont: Boolean = false,
    val textScale: Float = 1.0f,
    val aviationUnits: Boolean = false,
    val seismicEnabled: Boolean = true,
    val flightsEnabled: Boolean = true,
    /** Ids from [BOOTSTRAP_RADAR_SOURCES] the user turned off; every other source is on. */
    val disabledRadarSources: Set<String> = emptySet(),
    val minimumMagnitude: Double = 2.5,
    /** Radar auto-refresh; live (every minute) by default. 0 is Manual. */
    val refreshMinutes: Int = DEFAULT_REFRESH_MINUTES,
    val mapTheme: String = DEFAULT_MAP_THEME,
    val globePreset: String = GLOBE_PRESET_CLASSIC,
    /** [LANGUAGE_SYSTEM] or a code from [APP_LANGUAGE_OPTIONS]. Kept across reset. */
    val appLanguage: String = LANGUAGE_SYSTEM,
    val mapProvider: String = MAP_PROVIDER_AUTO,
    val theaterPreset: String = THEATER_PRESET_NONE,
    val streamQuality: String = STREAM_QUALITY_AUTO,
    /** Minutes without activity before live video stops; 0 is Never. */
    val idleStopMinutes: Int = DEFAULT_IDLE_STOP_MINUTES,
    /** Explore panel ids the user turned off. Panels not listed are shown. */
    val hiddenExplorePanels: Set<String> = emptySet(),
    /** Explore categories the user turned off, which hides every panel in them. */
    val hiddenExploreCategories: Set<String> = emptySet(),
    /** Radar layer chips pinned under the map search bar. Null is the built-in default set; otherwise RadarFeedFilter names. */
    val pinnedRadarLayers: Set<String>? = null,
) {

    /** True when the Explore panel should be shown. A hidden category hides all of its panels. */
    fun isExplorePanelVisible(panelId: String, category: String): Boolean =
        category !in hiddenExploreCategories && panelId !in hiddenExplorePanels

    fun sanitized(): NativePreferences = copy(
        textScale = textScale.takeIf(ALLOWED_TEXT_SCALES::contains) ?: 1.0f,
        minimumMagnitude = minimumMagnitude.takeIf(ALLOWED_MAGNITUDES::contains) ?: 2.5,
        disabledRadarSources = disabledRadarSources.filterTo(HashSet()) { id -> BOOTSTRAP_RADAR_SOURCES.any { it.id == id } },
        refreshMinutes = refreshMinutes.takeIf(ALLOWED_REFRESH_MINUTES::contains) ?: DEFAULT_REFRESH_MINUTES,
        mapTheme = mapTheme.takeIf { theme -> MAP_THEME_OPTIONS.any { it.first == theme } } ?: DEFAULT_MAP_THEME,
        globePreset = globePreset.takeIf { preset -> GLOBE_PRESET_OPTIONS.any { it.first == preset } } ?: GLOBE_PRESET_CLASSIC,
        appLanguage = appLanguage.takeIf { code -> APP_LANGUAGE_OPTIONS.any { it.first == code } } ?: LANGUAGE_SYSTEM,
        mapProvider = mapProvider.takeIf { provider -> MAP_PROVIDER_OPTIONS.any { it.first == provider } } ?: MAP_PROVIDER_AUTO,
        theaterPreset = theaterPreset.takeIf { preset -> THEATER_PRESET_OPTIONS.any { it.first == preset } } ?: THEATER_PRESET_NONE,
        streamQuality = streamQuality.takeIf { quality -> STREAM_QUALITY_OPTIONS.any { it.first == quality } } ?: STREAM_QUALITY_AUTO,
        idleStopMinutes = idleStopMinutes.takeIf { minutes -> IDLE_STOP_MINUTES_OPTIONS.any { it.first == minutes } } ?: DEFAULT_IDLE_STOP_MINUTES,
        hiddenExplorePanels = cleanExploreKeys(hiddenExplorePanels),
        hiddenExploreCategories = cleanExploreKeys(hiddenExploreCategories),
        pinnedRadarLayers = pinnedRadarLayers?.filterTo(HashSet()) { name -> name.length <= MAX_EXPLORE_KEY_LENGTH },
    )
}

private fun cleanExploreKeys(keys: Set<String>): Set<String> = keys.asSequence()
    .filter { key -> key.isNotEmpty() && key.length <= MAX_EXPLORE_KEY_LENGTH && key == key.trim() }
    .take(MAX_HIDDEN_EXPLORE_KEYS)
    .toSet()

/** Persisted settings live in their own preference file; reset never clears auth or saved signals. */
object NativePreferencesStore {
    private const val PREFERENCES = "godseye_native_settings"
    private const val KEY_SHOW_MAP = "show_map"
    private const val KEY_REDUCE_MOTION = "reduce_motion"
    private const val KEY_MONO_FONT = "mono_font"
    private const val KEY_TEXT_SCALE = "text_scale"
    private const val KEY_AVIATION_UNITS = "aviation_units"
    private const val KEY_SEISMIC_ENABLED = "seismic_enabled"
    private const val KEY_FLIGHTS_ENABLED = "flights_enabled"
    private const val KEY_DISABLED_RADAR_SOURCES = "disabled_radar_sources"
    private const val KEY_MIN_MAGNITUDE = "minimum_magnitude"
    private const val KEY_REFRESH_MINUTES = "refresh_minutes"
    private const val KEY_MAP_THEME = "map_theme"
    private const val KEY_GLOBE_PRESET = "globe_visual_preset"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_MAP_PROVIDER = "map_provider"
    private const val KEY_THEATER_PRESET = "theater_preset"
    private const val KEY_STREAM_QUALITY = "stream_quality"
    private const val KEY_IDLE_STOP_MINUTES = "live_media_idle_stop_minutes"
    private const val KEY_HIDDEN_EXPLORE_PANELS = "hidden_explore_panels"
    private const val KEY_HIDDEN_EXPLORE_CATEGORIES = "hidden_explore_categories"
    private const val KEY_PINNED_RADAR_LAYERS = "pinned_radar_layers"

    private val mutableState = MutableStateFlow(NativePreferences())
    val state: StateFlow<NativePreferences> = mutableState.asStateFlow()

    @Synchronized
    fun initialize(context: Context) {
        mutableState.value = read(context.applicationContext)
    }

    @Synchronized
    fun update(context: Context, transform: (NativePreferences) -> NativePreferences): NativePreferences {
        val appContext = context.applicationContext
        val updated = transform(read(appContext)).sanitized()
        persist(appContext, updated)
        mutableState.value = updated
        return updated
    }

    /** Reset keeps the language choice: it is an app-wide setting, not a radar preference. */
    @Synchronized
    fun reset(context: Context) {
        val appContext = context.applicationContext
        val language = read(appContext).appLanguage
        appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().clear()
            .putString(KEY_APP_LANGUAGE, language)
            .apply()
        mutableState.value = NativePreferences(appLanguage = language)
    }

    /** Reads the stored values without publishing them. For code that runs before the UI, such as a base Context. */
    @Synchronized
    fun peek(context: Context): NativePreferences = read(context.applicationContext)

    private fun read(context: Context): NativePreferences {
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        return NativePreferences(
            showMap = safely(true) { prefs.getBoolean(KEY_SHOW_MAP, true) },
            reduceMotion = safely(false) { prefs.getBoolean(KEY_REDUCE_MOTION, false) },
            monoFont = safely(false) { prefs.getBoolean(KEY_MONO_FONT, false) },
            textScale = safely(1.0f) { prefs.getFloat(KEY_TEXT_SCALE, 1.0f) },
            aviationUnits = safely(false) { prefs.getBoolean(KEY_AVIATION_UNITS, false) },
            seismicEnabled = safely(true) { prefs.getBoolean(KEY_SEISMIC_ENABLED, true) },
            flightsEnabled = safely(true) { prefs.getBoolean(KEY_FLIGHTS_ENABLED, true) },
            disabledRadarSources = safely(emptySet()) { prefs.getStringSet(KEY_DISABLED_RADAR_SOURCES, emptySet())?.toSet() ?: emptySet() },
            minimumMagnitude = safely(2.5) { prefs.getFloat(KEY_MIN_MAGNITUDE, 2.5f).toDouble() },
            refreshMinutes = safely(DEFAULT_REFRESH_MINUTES) { prefs.getInt(KEY_REFRESH_MINUTES, DEFAULT_REFRESH_MINUTES) },
            mapTheme = safely(DEFAULT_MAP_THEME) { prefs.getString(KEY_MAP_THEME, null) ?: DEFAULT_MAP_THEME },
            globePreset = safely(GLOBE_PRESET_CLASSIC) { prefs.getString(KEY_GLOBE_PRESET, null) ?: GLOBE_PRESET_CLASSIC },
            appLanguage = safely(LANGUAGE_SYSTEM) { prefs.getString(KEY_APP_LANGUAGE, null) ?: LANGUAGE_SYSTEM },
            mapProvider = safely(MAP_PROVIDER_AUTO) { prefs.getString(KEY_MAP_PROVIDER, null) ?: MAP_PROVIDER_AUTO },
            theaterPreset = safely(THEATER_PRESET_NONE) { prefs.getString(KEY_THEATER_PRESET, null) ?: THEATER_PRESET_NONE },
            streamQuality = safely(STREAM_QUALITY_AUTO) { prefs.getString(KEY_STREAM_QUALITY, null) ?: STREAM_QUALITY_AUTO },
            idleStopMinutes = safely(DEFAULT_IDLE_STOP_MINUTES) { prefs.getInt(KEY_IDLE_STOP_MINUTES, DEFAULT_IDLE_STOP_MINUTES) },
            hiddenExplorePanels = safely(emptySet()) { prefs.getStringSet(KEY_HIDDEN_EXPLORE_PANELS, emptySet())?.toSet() ?: emptySet() },
            hiddenExploreCategories = safely(emptySet()) { prefs.getStringSet(KEY_HIDDEN_EXPLORE_CATEGORIES, emptySet())?.toSet() ?: emptySet() },
            pinnedRadarLayers = safely(null) { if (prefs.contains(KEY_PINNED_RADAR_LAYERS)) prefs.getStringSet(KEY_PINNED_RADAR_LAYERS, emptySet())?.toSet() ?: emptySet() else null },
        ).sanitized()
    }

    private fun persist(context: Context, value: NativePreferences) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SHOW_MAP, value.showMap)
            .putBoolean(KEY_REDUCE_MOTION, value.reduceMotion)
            .putBoolean(KEY_MONO_FONT, value.monoFont)
            .putFloat(KEY_TEXT_SCALE, value.textScale)
            .putBoolean(KEY_AVIATION_UNITS, value.aviationUnits)
            .putBoolean(KEY_SEISMIC_ENABLED, value.seismicEnabled)
            .putBoolean(KEY_FLIGHTS_ENABLED, value.flightsEnabled)
            .putStringSet(KEY_DISABLED_RADAR_SOURCES, value.disabledRadarSources)
            .putFloat(KEY_MIN_MAGNITUDE, value.minimumMagnitude.toFloat())
            .putInt(KEY_REFRESH_MINUTES, value.refreshMinutes)
            .putString(KEY_MAP_THEME, value.mapTheme)
            .putString(KEY_GLOBE_PRESET, value.globePreset)
            .putString(KEY_APP_LANGUAGE, value.appLanguage)
            .putString(KEY_MAP_PROVIDER, value.mapProvider)
            .putString(KEY_THEATER_PRESET, value.theaterPreset)
            .putString(KEY_STREAM_QUALITY, value.streamQuality)
            .putInt(KEY_IDLE_STOP_MINUTES, value.idleStopMinutes)
            .putStringSet(KEY_HIDDEN_EXPLORE_PANELS, value.hiddenExplorePanels)
            .putStringSet(KEY_HIDDEN_EXPLORE_CATEGORIES, value.hiddenExploreCategories)
            .also { editor ->
                val pins = value.pinnedRadarLayers
                if (pins == null) editor.remove(KEY_PINNED_RADAR_LAYERS) else editor.putStringSet(KEY_PINNED_RADAR_LAYERS, pins)
            }
            .apply()
    }

    private inline fun <T> safely(fallback: T, read: () -> T): T = runCatching(read).getOrDefault(fallback)
}

fun formatFlightAltitude(meters: Double?, aviation: Boolean): String {
    if (meters == null || !meters.isFinite()) return "Unknown"
    return if (aviation) {
        String.format(Locale.ROOT, "%,.0f ft", meters * METERS_TO_FEET)
    } else {
        String.format(Locale.ROOT, "%,.0f m", meters)
    }
}

fun formatFlightSpeed(metersPerSecond: Double?, aviation: Boolean): String {
    if (metersPerSecond == null || !metersPerSecond.isFinite()) return "Unknown"
    return if (aviation) {
        String.format(Locale.ROOT, "%.0f kt", metersPerSecond * METERS_PER_SECOND_TO_KNOTS)
    } else {
        String.format(Locale.ROOT, "%.0f m/s", metersPerSecond)
    }
}

/**
 * How long ago OpenSky last heard from the aircraft, e.g. "12s ago" or "3 min ago". Null when the contact time
 * is absent or zero, so the caller can leave the row out. A contact time ahead of the clock counts as 0s ago.
 */
fun formatFlightLastContact(lastContactEpochSeconds: Long?, nowEpochSeconds: Long): String? {
    val contact = lastContactEpochSeconds?.takeIf { it > 0L } ?: return null
    val ago = (nowEpochSeconds - contact).coerceAtLeast(0L)
    return when {
        ago < 60L -> "${ago}s ago"
        ago < 3_600L -> "${ago / 60L} min ago"
        ago < 86_400L -> "${ago / 3_600L} h ago"
        else -> "${ago / 86_400L} d ago"
    }
}

private const val METERS_TO_FEET = 3.280839895
private const val METERS_PER_SECOND_TO_KNOTS = 1.943844492
