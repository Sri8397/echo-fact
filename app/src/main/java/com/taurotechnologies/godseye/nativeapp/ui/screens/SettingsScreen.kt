package com.taurotechnologies.godseye.nativeapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.platform.LocalUriHandler
import com.taurotechnologies.godseye.nativeapp.AppViewModel
import com.taurotechnologies.godseye.nativeapp.BuildConfig
import com.taurotechnologies.godseye.nativeapp.ThemeChoice
import com.taurotechnologies.godseye.nativeapp.ui.components.AppSheet
import com.taurotechnologies.godseye.nativeapp.ui.components.ConfirmSheet
import com.taurotechnologies.godseye.nativeapp.ui.components.Panel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.taurotechnologies.godseye.nativeapp.data.BOOTSTRAP_RADAR_SOURCES
import com.taurotechnologies.godseye.nativeapp.data.NativePreferences
import com.taurotechnologies.godseye.nativeapp.data.NativePreferencesStore
import com.taurotechnologies.godseye.nativeapp.data.RadarSignal
import com.taurotechnologies.godseye.nativeapp.data.SavedRadarSignals
import com.taurotechnologies.godseye.nativeapp.ui.theme.ColorStyle
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors
import com.taurotechnologies.godseye.nativeapp.ui.theme.StylePalette
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.stringResource
import com.taurotechnologies.godseye.nativeapp.R
import com.taurotechnologies.godseye.nativeapp.data.APP_LANGUAGE_OPTIONS
import com.taurotechnologies.godseye.nativeapp.data.ExploreCatalogStore
import com.taurotechnologies.godseye.nativeapp.data.GLOBE_PRESET_OPTIONS
import com.taurotechnologies.godseye.nativeapp.data.IDLE_STOP_MINUTES_OPTIONS
import com.taurotechnologies.godseye.nativeapp.data.MAP_PROVIDER_OPTIONS
import com.taurotechnologies.godseye.nativeapp.data.MAP_THEME_OPTIONS
import com.taurotechnologies.godseye.nativeapp.data.STREAM_QUALITY_OPTIONS
import com.taurotechnologies.godseye.nativeapp.data.THEATER_PRESET_OPTIONS
import com.taurotechnologies.godseye.nativeapp.data.globePresetLabel
import com.taurotechnologies.godseye.nativeapp.data.mapThemeLabel

private val IconTileShape: RoundedCornerShape = RoundedCornerShape(12.dp)

/** Rich native preferences grouped by appearance, map, sources, data, and account. */
@Composable
fun SettingsScreen(theme: ThemeChoice, model: AppViewModel, accountEmail: String?, onSignOut: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    NativePreferencesStore.initialize(context)
    SavedRadarSignals.initialize(context)
    val preferences by NativePreferencesStore.state.collectAsStateWithLifecycle()
    val savedSignals by SavedRadarSignals.signals.collectAsStateWithLifecycle()
    val liveRadar by model.liveRadar.collectAsStateWithLifecycle()
    // Root shows a search field and one row per group; each group opens as its own nested page.
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var showSavedSignals by remember { mutableStateOf(false) }
    var selectedSavedSignal by remember { mutableStateOf<RadarSignal?>(null) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var choiceRequest by remember { mutableStateOf<ChoiceRequest?>(null) }
    val isGlobe: Boolean by model.isGlobe.collectAsStateWithLifecycle()
    val colorStyle: ColorStyle by model.colorStyle.collectAsStateWithLifecycle()
    val catalog by ExploreCatalogStore.catalog.collectAsStateWithLifecycle()
    val show: (String) -> Boolean = { page == it }
    val languageTitle = stringResource(R.string.settings_language)
    val mapProviderTitle = stringResource(R.string.settings_map_provider)
    val mapThemeTitle = stringResource(R.string.settings_map_theme)
    val globePresetTitle = stringResource(R.string.settings_globe_preset)
    val theaterTitle = stringResource(R.string.settings_theater_presets)
    val streamQualityTitle = stringResource(R.string.settings_stream_quality)
    val idleStopTitle = stringResource(R.string.settings_stream_idle_stop)
    // Every row title, mapped to the nested page that holds it, so search can jump straight there.
    val searchIndex: List<Pair<String, String>> = buildList {
        listOf(languageTitle, "Theme and color", "Monospace font", "Text size").forEach { add(it to "Appearance") }
        listOf(mapProviderTitle, mapThemeTitle, globePresetTitle, "Map visibility", "Reduce motion", "2D map", "3D globe").forEach { add(it to "Map") }
        catalog.visibleCategories.forEach { add(it to "Panels") }
        catalog.visiblePanels.forEach { add(it.title to "Panels") }
        listOf("Live feed status", "USGS earthquakes", "Minimum magnitude", "OpenSky flights", "Aviation units", theaterTitle, "Refresh interval", streamQualityTitle, idleStopTitle).forEach { add(it to "Sources") }
        BOOTSTRAP_RADAR_SOURCES.forEach { add(it.title to "Sources") }
        listOf("Saved radar signals", "Refresh live data", "Version", "Reset radar preferences").forEach { add(it to "Data") }
        listOf("Sign out", "Guest mode").forEach { add(it to "Account") }
    }
    fun updatePreferences(transform: (NativePreferences) -> NativePreferences) {
        NativePreferencesStore.update(context, transform)
    }
    fun setExploreCategoryVisible(category: String, visible: Boolean) = updatePreferences {
        it.copy(hiddenExploreCategories = if (visible) it.hiddenExploreCategories - category else it.hiddenExploreCategories + category)
    }
    fun setExplorePanelVisible(panelId: String, visible: Boolean) = updatePreferences {
        it.copy(hiddenExplorePanels = if (visible) it.hiddenExplorePanels - panelId else it.hiddenExplorePanels + panelId)
    }

    BackHandler(onBack = {
        when {
            choiceRequest != null -> choiceRequest = null
            showResetConfirmation -> showResetConfirmation = false
            selectedSavedSignal != null -> selectedSavedSignal = null
            showSavedSignals -> showSavedSignals = false
            page != null -> page = null
            else -> model.closeSettings()
        }
    })
    if (showSavedSignals) {
        SettingsPage("Saved radar signals", onBack = { showSavedSignals = false }) {
            item { Text("Signals saved on this phone", fontSize = 13.sp, lineHeight = 18.sp, color = colors.muted) }
            if (savedSignals.isEmpty()) {
                item {
                    Panel {
                        Text("No saved radar signals yet", color = colors.ink, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(14.dp))
                        Text("Bookmark a signal to keep its snapshot here.", color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp))
                    }
                }
            } else {
                item {
                    Panel {
                        savedSignals.forEachIndexed { index, signal ->
                            SettingsRow(
                                title = signal.title,
                                subtitle = "${signal.layer.label} · ${signal.locationName}",
                                icon = Icons.Outlined.BookmarkBorder,
                                onClick = { selectedSavedSignal = signal },
                                divider = index < savedSignals.lastIndex,
                            )
                        }
                    }
                }
            }
        }
        selectedSavedSignal?.let { signal -> SignalDetails(signal, onClose = { selectedSavedSignal = null }) }
    } else if (page != null) SettingsPage(page.orEmpty(), onBack = { page = null }) {
        if (show("Appearance")) {
            item {
                Panel {
                    SettingsRow(languageTitle, optionLabel(APP_LANGUAGE_OPTIONS, preferences.appLanguage), Icons.Outlined.Language, onClick = {
                        choiceRequest = ChoiceRequest(languageTitle, preferences.appLanguage, APP_LANGUAGE_OPTIONS, "appLanguage")
                    }, divider = true)
                    SettingsRow("Theme and color", "${theme.title} · ${colorStyle.title}", Icons.Outlined.Palette, onClick = model::openTheme, divider = true)
                    SettingsRow("Monospace font", "Use fixed-width text in radar details", Icons.Outlined.Tune, onClick = { updatePreferences { it.copy(monoFont = !preferences.monoFont) } }, trailing = { PreferenceSwitch(preferences.monoFont) { checked -> updatePreferences { it.copy(monoFont = checked) } } }, showChevron = false, divider = true)
                    SettingsRow("Text size", "${textScaleLabel(preferences.textScale)} · applies to radar text", Icons.Outlined.AccessibilityNew, onClick = {
                        choiceRequest = ChoiceRequest("Text size", preferences.textScale.toString(), TEXT_SCALE_CHOICES, "textScale")
                    })
                }
            }
        }

        if (show("Map")) {
            item {
                Panel {
                    SettingsRow(mapProviderTitle, optionLabel(MAP_PROVIDER_OPTIONS, preferences.mapProvider), Icons.Outlined.Map, onClick = {
                        choiceRequest = ChoiceRequest(mapProviderTitle, preferences.mapProvider, MAP_PROVIDER_OPTIONS, "mapProvider")
                    }, divider = true)
                    SettingsRow(mapThemeTitle, mapThemeLabel(preferences.mapTheme), Icons.Outlined.Map, onClick = {
                        choiceRequest = ChoiceRequest(mapThemeTitle, preferences.mapTheme, MAP_THEME_OPTIONS, "mapTheme")
                    }, divider = true)
                    SettingsRow(globePresetTitle, globePresetLabel(preferences.globePreset), Icons.Outlined.Public, onClick = {
                        choiceRequest = ChoiceRequest(globePresetTitle, preferences.globePreset, GLOBE_PRESET_OPTIONS, "globePreset")
                    }, divider = true)
                    SettingsRow("Map visibility", if (preferences.showMap) "Show the map when radar opens" else "Open the signal list first", Icons.Outlined.Visibility, onClick = { updatePreferences { it.copy(showMap = !preferences.showMap) } }, trailing = { PreferenceSwitch(preferences.showMap) { checked -> updatePreferences { it.copy(showMap = checked) } } }, showChevron = false, divider = true)
                    SettingsRow("Reduce motion", "Reduce animated map movement", Icons.Outlined.AccessibilityNew, onClick = { updatePreferences { it.copy(reduceMotion = !preferences.reduceMotion) } }, trailing = { PreferenceSwitch(preferences.reduceMotion) { checked -> updatePreferences { it.copy(reduceMotion = checked) } } }, showChevron = false, divider = true)
                    SettingsRow("2D map", "Flat map view", Icons.Outlined.Map, onClick = { model.setGlobe(false) }, trailing = { StyleRadio(!isGlobe) { model.setGlobe(false) } }, showChevron = false, divider = true)
                    SettingsRow("3D globe", "Globe view", Icons.Outlined.Public, onClick = { model.setGlobe(true) }, trailing = { StyleRadio(isGlobe) { model.setGlobe(true) } }, showChevron = false, divider = false)
                }
            }
        }

        if (show("Panels")) {
            item {
                Text("Turn Explore panels on or off. A category switch hides every panel in it.", fontSize = 13.sp, lineHeight = 18.sp, color = colors.muted)
            }
            if (catalog.visibleCategories.isEmpty()) {
                item {
                    Panel {
                        Text("No Explore panels are loaded yet.", color = colors.muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(14.dp))
                    }
                }
            }
            catalog.visibleCategories.forEach { category ->
                val panelsInCategory = catalog.visiblePanels.filter { it.category == category }
                val categoryVisible = category !in preferences.hiddenExploreCategories
                item {
                    Panel {
                        SettingsRow(category, "${panelsInCategory.size} panels", Icons.Outlined.Explore, onClick = { setExploreCategoryVisible(category, !categoryVisible) }, trailing = {
                            PreferenceSwitch(categoryVisible) { checked -> setExploreCategoryVisible(category, checked) }
                        }, showChevron = false, divider = true)
                        panelsInCategory.forEachIndexed { index, panel ->
                            val panelVisible = panel.id !in preferences.hiddenExplorePanels
                            SettingsRow(panel.title, null, null, onClick = { setExplorePanelVisible(panel.id, !panelVisible) }, trailing = {
                                PreferenceSwitch(panelVisible) { checked -> setExplorePanelVisible(panel.id, checked) }
                            }, showChevron = false, divider = index < panelsInCategory.lastIndex)
                        }
                    }
                }
            }
        }

        if (show("Sources")) {
            item {
                Panel {
                    SettingsRow("Live feed status", liveRadar.message.ifBlank { if (liveRadar.loading) "Checking feeds…" else "Ready to refresh" }, Icons.Outlined.CloudQueue, showChevron = false, divider = true)
                    SettingsRow("USGS earthquakes", "Public earthquake feed", Icons.Outlined.Sensors, onClick = { updatePreferences { it.copy(seismicEnabled = !preferences.seismicEnabled) } }, trailing = { PreferenceSwitch(preferences.seismicEnabled) { checked -> updatePreferences { it.copy(seismicEnabled = checked) } } }, showChevron = false, divider = true)
                    SettingsRow("Minimum magnitude", magnitudeLabel(preferences.minimumMagnitude), Icons.Outlined.Tune, onClick = {
                        choiceRequest = ChoiceRequest("Minimum earthquake magnitude", preferences.minimumMagnitude.toString(), MIN_MAGNITUDE_CHOICES, "minimumMagnitude")
                    }, divider = true)
                    SettingsRow("OpenSky flights", if (accountEmail == null) "Sign in required for authenticated flight data" else "Authenticated flight feed", Icons.Outlined.Flight, onClick = { updatePreferences { it.copy(flightsEnabled = !preferences.flightsEnabled) } }, trailing = { PreferenceSwitch(preferences.flightsEnabled) { checked -> updatePreferences { it.copy(flightsEnabled = checked) } } }, showChevron = false, divider = true)
                    BOOTSTRAP_RADAR_SOURCES.forEach { source ->
                        val enabled = source.id !in preferences.disabledRadarSources
                        val toggle: (Boolean) -> Unit = { on ->
                            updatePreferences { it.copy(disabledRadarSources = if (on) it.disabledRadarSources - source.id else it.disabledRadarSources + source.id) }
                        }
                        SettingsRow(source.title, "${source.layer.label} layer · ${source.description}", Icons.Outlined.Sensors, onClick = { toggle(!enabled) }, trailing = { PreferenceSwitch(enabled) { checked -> toggle(checked) } }, showChevron = false, divider = true)
                    }
                    SettingsRow("Aviation units", if (preferences.aviationUnits) "Feet and knots" else "Meters and meters per second", Icons.Outlined.Straighten, onClick = { updatePreferences { it.copy(aviationUnits = !preferences.aviationUnits) } }, trailing = { PreferenceSwitch(preferences.aviationUnits) { checked -> updatePreferences { it.copy(aviationUnits = checked) } } }, showChevron = false, divider = true)
                    SettingsRow(theaterTitle, optionLabel(THEATER_PRESET_OPTIONS, preferences.theaterPreset), Icons.Outlined.Language, onClick = {
                        choiceRequest = ChoiceRequest(theaterTitle, preferences.theaterPreset, THEATER_PRESET_OPTIONS, "theaterPreset")
                    }, divider = true)
                    SettingsRow("Refresh interval", refreshLabel(preferences.refreshMinutes), Icons.Outlined.Refresh, onClick = {
                        choiceRequest = ChoiceRequest("Automatic refresh", preferences.refreshMinutes.toString(), REFRESH_CHOICES, "refreshMinutes")
                    })
                }
            }
            item {
                Panel {
                    SettingsRow("USGS event data", "Source documentation", Icons.Outlined.OpenInNew, onClick = { uriHandler.openUri("https://earthquake.usgs.gov/data/comcat/") }, trailingText = "Open", showChevron = false, divider = true)
                    SettingsRow("OpenSky Network", "Flight data documentation", Icons.Outlined.OpenInNew, onClick = { uriHandler.openUri("https://openskynetwork.github.io/opensky-api/") }, trailingText = "Open", showChevron = false)
                }
            }
            item { SectionHeading("Live media") }
            item {
                Panel {
                    SettingsRow(streamQualityTitle, optionLabel(STREAM_QUALITY_OPTIONS, preferences.streamQuality), Icons.Outlined.Tune, onClick = {
                        choiceRequest = ChoiceRequest(streamQualityTitle, preferences.streamQuality, STREAM_QUALITY_OPTIONS, "streamQuality")
                    }, divider = true)
                    SettingsRow(idleStopTitle, "Stops live video after this long without activity", Icons.Outlined.Visibility, onClick = {
                        choiceRequest = ChoiceRequest(idleStopTitle, preferences.idleStopMinutes.toString(), IDLE_STOP_MINUTES_OPTIONS.map { it.first.toString() to it.second }, "idleStopMinutes")
                    }, trailingText = optionLabel(IDLE_STOP_MINUTES_OPTIONS, preferences.idleStopMinutes))
                }
            }
        }

        if (show("Data")) {
            item {
                Panel {
                    SettingsRow("Saved radar signals", if (savedSignals.isEmpty()) "No saved signals" else "${savedSignals.size} saved on this phone", Icons.Outlined.BookmarkBorder, onClick = { showSavedSignals = true }, divider = true)
                    SettingsRow("Refresh live data", liveRadar.message.ifBlank { "Check the latest available feeds" }, Icons.Outlined.Refresh, onClick = model::refreshLiveRadar, trailingText = if (liveRadar.loading) "Loading" else null, showChevron = false)
                }
            }
            item { SectionHeading("About") }
            item {
                Panel {
                    SettingsRow("GodsEye World Radar", "by Suvidha", Icons.Outlined.Public, showChevron = false, divider = true)
                    SettingsRow("Version", BuildConfig.VERSION_NAME, Icons.Outlined.Info, showChevron = false, divider = true)
                    SettingsRow("Reset radar preferences", "Restore radar settings to their defaults", Icons.Outlined.RestartAlt, onClick = { showResetConfirmation = true }, tint = colors.danger, showChevron = false)
                }
            }
        }

        if (show("Account")) {
            item {
                Panel {
                    SettingsRow(if (accountEmail == null) "Guest" else "Signed in", accountEmail ?: "Public radar only", Icons.Outlined.Info, showChevron = false, divider = true)
                    SettingsRow(if (accountEmail == null) "Exit guest mode" else "Sign out", null, Icons.AutoMirrored.Outlined.Logout, onClick = onSignOut, tint = colors.danger, showChevron = false)
                }
            }
        }
    } else SettingsPage("Settings", onBack = model::closeSettings, header = {
        SettingsSearchField(query, onQueryChange = { query = it })
    }) {
        val q = query.trim()
        if (q.isEmpty()) {
            item {
                Panel {
                    SETTINGS_GROUPS.forEachIndexed { index, group ->
                        SettingsRow(group.title, group.subtitle, group.icon, onClick = { page = group.title }, divider = index < SETTINGS_GROUPS.lastIndex)
                    }
                }
            }
        } else {
            val results = searchIndex.filter { (title, owner) -> title.contains(q, ignoreCase = true) || owner.contains(q, ignoreCase = true) }
            if (results.isEmpty()) {
                item { Text("No settings match \"$q\"", fontSize = 13.sp, lineHeight = 18.sp, color = colors.muted, modifier = Modifier.padding(4.dp)) }
            } else {
                item {
                    Panel {
                        results.forEachIndexed { index, (title, owner) ->
                            val group = SETTINGS_GROUPS.firstOrNull { it.title == owner }
                            SettingsRow(title, owner, group?.icon, onClick = { page = owner }, divider = index < results.lastIndex)
                        }
                    }
                }
            }
        }
    }

    choiceRequest?.let { request ->
        ChoiceDialog(request, onDismiss = { choiceRequest = null }) { value ->
            when (request.key) {
                "textScale" -> updatePreferences { it.copy(textScale = value.toFloat()) }
                "minimumMagnitude" -> updatePreferences { it.copy(minimumMagnitude = value.toDouble()) }
                "refreshMinutes" -> updatePreferences { it.copy(refreshMinutes = value.toInt()) }
                "mapTheme" -> updatePreferences { it.copy(mapTheme = value) }
                "globePreset" -> updatePreferences { it.copy(globePreset = value) }
                "mapProvider" -> updatePreferences { it.copy(mapProvider = value) }
                "theaterPreset" -> updatePreferences { it.copy(theaterPreset = value) }
                "streamQuality" -> updatePreferences { it.copy(streamQuality = value) }
                "idleStopMinutes" -> updatePreferences { it.copy(idleStopMinutes = value.toInt()) }
                "appLanguage" -> if (value != preferences.appLanguage) {
                    updatePreferences { it.copy(appLanguage = value) }
                    // The base context applies the stored language on start (withAppLanguage), so recreate to reload strings.
                    context.findActivity()?.recreate()
                }
            }
            choiceRequest = null
        }
    }
    if (showResetConfirmation) {
        ConfirmSheet(
            title = "Reset radar preferences?",
            body = "This resets radar display, map, panel, source, live media and refresh preferences. Theme, language, current 2D/3D mode, and globe rotation stay as set.",
            confirmText = "Reset",
            isBusy = false,
            onConfirm = {
                NativePreferencesStore.reset(context)
                showResetConfirmation = false
            },
            onDismiss = { showResetConfirmation = false },
        )
    }
    selectedSavedSignal?.let { signal -> SignalDetails(signal, onClose = { selectedSavedSignal = null }) }
}

/**
 * Full-page theme choice, like Suvidha's Theme page: appearance (returns to Settings when picked)
 * and colour style (applies at once and stays here so the change can be seen).
 */
@Composable
fun ThemeScreen(theme: ThemeChoice, style: ColorStyle, model: AppViewModel) {
    val colors = LocalSuvidhaColors.current
    BackHandler(onBack = model::openSettings)
    SettingsPage("Theme", onBack = model::openSettings) {
        item { Text("Choose your preferred appearance.", fontSize = 13.sp, lineHeight = 20.sp, color = colors.muted) }
        item {
            Panel {
                ThemeChoice.entries.forEachIndexed { index, choice ->
                    SettingsRow(
                        title = choice.title,
                        subtitle = choice.subtitle,
                        icon = null,
                        onClick = { model.setTheme(choice) },
                        trailing = { StyleRadio(choice == theme) { model.setTheme(choice) } },
                        showChevron = false,
                        divider = index < ThemeChoice.entries.lastIndex,
                    )
                }
            }
        }
        item { SectionHeading("Color style") }
        item {
            Panel {
                ColorStyle.entries.forEachIndexed { index, option ->
                    SettingsRow(
                        title = option.title,
                        subtitle = option.description,
                        icon = null,
                        leading = { StyleSwatches(option.palette) },
                        onClick = { model.setColorStyle(option) },
                        trailing = { StyleRadio(option == style) { model.setColorStyle(option) } },
                        showChevron = false,
                        divider = index < ColorStyle.entries.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleRadio(isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = colors.green, unselectedColor = colors.muted))
}

private val SWATCH_SIZE = 24.dp
private val SWATCH_STEP = 14.dp
private val SWATCH_STRIP_WIDTH = 52.dp

/** Three overlapping dots: the style's primary, secondary and accent colours (as in Suvidha). */
@Composable
private fun StyleSwatches(palette: StylePalette) {
    Box(Modifier.size(width = SWATCH_STRIP_WIDTH, height = SWATCH_SIZE)) {
        listOf(palette.primary.base, palette.secondary.base, palette.accent.base).forEachIndexed { index, color ->
            Box(
                Modifier
                    .offset(x = SWATCH_STEP * index)
                    .size(SWATCH_SIZE)
                    .border(2.dp, palette.neutrals.background, CircleShape)
                    .padding(2.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

private data class ChoiceRequest(
    val title: String,
    val selected: String,
    val options: List<Pair<String, String>>,
    val key: String,
)

private val TEXT_SCALE_CHOICES = listOf("1.0" to "Standard", "1.15" to "Large", "1.3" to "Largest")
private val MIN_MAGNITUDE_CHOICES = listOf("2.5" to "2.5+", "4.0" to "4.0+", "5.0" to "5.0+")
private val REFRESH_CHOICES = listOf("0" to "Manual", "1" to "Every minute", "5" to "Every five minutes")

private fun textScaleLabel(value: Float): String = when (value) {
    1.15f -> "Large"
    1.3f -> "Largest"
    else -> "Standard"
}

private fun magnitudeLabel(value: Double): String = "${value.toString().removeSuffix(".0")}+ magnitude"

private fun refreshLabel(minutes: Int): String = when (minutes) {
    1 -> "Every minute"
    5 -> "Every five minutes"
    else -> "Manual"
}


/** Label for a stored option value. Falls back to the first option, which is the default, for unknown values. */
private fun <T> optionLabel(options: List<Pair<T, String>>, value: T): String =
    options.firstOrNull { it.first == value }?.second ?: options.first().second

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun PreferenceSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalSuvidhaColors.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onGreen,
            checkedTrackColor = colors.green,
            uncheckedThumbColor = colors.cardSurface,
            uncheckedTrackColor = colors.borderLight,
            uncheckedBorderColor = Color.Transparent,
            checkedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun ChoiceDialog(request: ChoiceRequest, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val colors = LocalSuvidhaColors.current
    // Slides up from the bottom like the other native sheets; tap a row to apply it.
    AppSheet(onDismiss = onDismiss) {
        Text(request.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.ink)
        // Language has 29 options, so the list scrolls inside the sheet.
        Column(modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
            request.options.forEach { (value, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onSelect(value) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioButton(
                        selected = value == request.selected,
                        onClick = { onSelect(value) },
                        colors = RadioButtonDefaults.colors(selectedColor = colors.green, unselectedColor = colors.muted),
                    )
                    Text(label, color = colors.ink, fontSize = 14.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    header: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.green, modifier = Modifier.size(22.dp))
            }
            Text(title, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, color = colors.ink, modifier = Modifier.weight(1f))
        }
        header?.invoke()
        HorizontalDivider(color = colors.borderLight)
        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

private class SettingsGroup(val title: String, val subtitle: String, val icon: ImageVector)

/** The nested Settings pages, in the order they are listed on the root page. */
private val SETTINGS_GROUPS = listOf(
    SettingsGroup("Appearance", "Language, theme, font and text size", Icons.Outlined.Palette),
    SettingsGroup("Map", "Provider, theme and 2D/3D view", Icons.Outlined.Map),
    SettingsGroup("Panels", "Show or hide Explore panels", Icons.Outlined.Explore),
    SettingsGroup("Sources", "Live feeds, units, refresh and live media", Icons.Outlined.Sensors),
    SettingsGroup("Data", "Saved signals, version and reset", Icons.Outlined.BookmarkBorder),
    SettingsGroup("Account", "Sign-in status and sign out", Icons.Outlined.Info),
)

/** Same search field style as the app-wide search overlay. */
@Composable
private fun SettingsSearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = LocalSuvidhaColors.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        singleLine = true,
        placeholder = { Text("Search settings", color = colors.muted) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.muted) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = colors.muted)
                }
            }
        },
    )
}

@Composable
private fun SectionHeading(title: String) {
    val colors = LocalSuvidhaColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(title, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = colors.ink, modifier = Modifier.weight(1f))
    }
}


@Composable
private fun SettingsRow(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    tint: Color = LocalSuvidhaColors.current.green,
    showChevron: Boolean = true,
    divider: Boolean = false,
) {
    val colors = LocalSuvidhaColors.current
    val isDanger: Boolean = tint == colors.danger
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            leading?.invoke()
            if (icon != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp).background(if (isDanger) colors.danger.copy(alpha = 0.12f) else colors.mint, IconTileShape),
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, color = if (isDanger) colors.danger else colors.ink)
                if (subtitle != null) Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = colors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (trailingText != null) Text(trailingText, fontSize = 13.sp, color = colors.muted, maxLines = 1)
            trailing?.invoke()
            if (showChevron && onClick != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.muted)
        }
        if (divider) HorizontalDivider(color = colors.borderLight, modifier = Modifier.padding(start = if (icon != null) 54.dp else 12.dp))
    }
}
