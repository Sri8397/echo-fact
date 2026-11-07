package com.taurotechnologies.godseye.nativeapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taurotechnologies.godseye.nativeapp.data.ExploreCatalogStore
import com.taurotechnologies.godseye.nativeapp.data.WebcamCameras
import com.taurotechnologies.godseye.nativeapp.data.ExploreStore
import com.taurotechnologies.godseye.nativeapp.ui.theme.ColorStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.collect
import com.taurotechnologies.godseye.nativeapp.data.NativePreferencesStore
import com.taurotechnologies.godseye.nativeapp.data.RadarSignal
import com.taurotechnologies.godseye.nativeapp.data.LiveRadarRepository
import com.taurotechnologies.godseye.nativeapp.data.BOOTSTRAP_RADAR_SOURCES
import com.taurotechnologies.godseye.nativeapp.data.explore.BootstrapTier
import com.taurotechnologies.godseye.nativeapp.data.BootstrapRadarRepository
import com.taurotechnologies.godseye.nativeapp.data.CuratedInfrastructure
import com.taurotechnologies.godseye.nativeapp.data.RadarLayer
import com.taurotechnologies.godseye.nativeapp.data.FlightTrackHistory
import com.taurotechnologies.godseye.nativeapp.data.LiveFlightRepository
import com.taurotechnologies.godseye.nativeapp.data.FlightException

data class LiveRadarState(
    val signals: List<RadarSignal> = emptyList(),
    val loading: Boolean = false,
    val message: String = "",
    val hasError: Boolean = false,
)

enum class Screen { RADAR, SETTINGS, THEME, EXPLORE, PANEL }

/** Appearance choice, saved on the device (same options as Suvidha's Theme page). */
enum class ThemeChoice(val title: String, val subtitle: String?) {
    SYSTEM("System default", "Follow device appearance"),
    LIGHT("Light", null),
    DARK("Dark", null);

    companion object {
        fun from(value: String?): ThemeChoice = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

private const val PREFS_NAME: String = "godseye_native"
private const val KEY_THEME: String = "theme"
private const val KEY_COLOR_STYLE: String = "color_style"
private const val KEY_GLOBE: String = "map_globe_v2"

/** [sequence] changes on every request so the map flies even when the target repeats. */
data class FlyTarget(val lat: Double, val lng: Double, val zoom: Double, val sequence: Long)

private const val BOOT_WAIT_MILLIS = 6_000L

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val nativePreferences = NativePreferencesStore.also { it.initialize(application) }.state
    private val _liveRadar = MutableStateFlow(LiveRadarState())
    val liveRadar: StateFlow<LiveRadarState> = _liveRadar.asStateFlow()
    private var apiSession: String? = null
    private var liveLoadJob: Job? = null

    fun setApiSession(accessToken: String?) {
        if (apiSession == accessToken) return
        apiSession = accessToken
        // Never retain another session's authenticated feed after sign-out/account change.
        _liveRadar.value = LiveRadarState()
        refreshLiveRadar()
    }

    fun refreshLiveRadar(isBackground: Boolean = false) {
        liveLoadJob?.cancel()
        val token = apiSession
        val options = nativePreferences.value
        if (!isBackground) {
            _liveRadar.update { it.copy(loading = true, message = "Loading live feeds…", hasError = false) }
        }
        liveLoadJob = viewModelScope.launch {
            suspend fun load(block: suspend () -> List<RadarSignal>): Result<List<RadarSignal>> = try {
                Result.success(block())
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                // The underlying cause goes to logcat with full diagnostic context; sensitive tokens are never logged.
                try {
                    android.util.Log.w("GodsEyeRadar", "Live feed failed: ${error.message}", error)
                } catch (_: RuntimeException) {}
                Result.failure(error)
            }
            val earthquakes = async { if (!options.seismicEnabled) Result.success(emptyList()) else load { LiveRadarRepository.loadEarthquakes() } }
            val flights = async { if (token == null || !options.flightsEnabled) Result.success(emptyList()) else load { LiveFlightRepository.loadFlights(token) } }
            val enabledSources = BOOTSTRAP_RADAR_SOURCES.filter { it.id !in options.disabledRadarSources }
            val bootstrap = enabledSources.map { source -> source to async { load { BootstrapRadarRepository.load(source) } } }
            val webcams = WebcamCameras.radarSignals(WebcamCameras.parse(ExploreCatalogStore.current))
            val infrastructure = CuratedInfrastructure.militaryBases() + CuratedInfrastructure.nuclearSites() + CuratedInfrastructure.aiDatacenters() + CuratedInfrastructure.satellites() + webcams
            if (token == null || !options.flightsEnabled) FlightTrackHistory.clear()
            var flightsRecorded = false

            // Publishes whatever has finished. A partial publish ends the boot screen; the final one adds the rest.
            fun publish(final: Boolean) {
                val quakeResult = if (earthquakes.isCompleted) earthquakes.getCompleted() else null
                val flightResult = if (flights.isCompleted) flights.getCompleted() else null
                if (!flightsRecorded && flightResult != null) {
                    flightsRecorded = true
                    flightResult.getOrNull()?.let { loaded -> FlightTrackHistory.record(loaded.mapNotNull { it.flight }) }
                }
                val finished = bootstrap.filter { it.second.isCompleted }.map { it.first to it.second.getCompleted() }
                val failedSources = finished.filter { it.second.isFailure }.map { it.first.title }
                val errors = listOfNotNull(
                    quakeResult?.exceptionOrNull()?.let { "USGS unavailable. Retry to reconnect." },
                    flightResult?.exceptionOrNull()?.let { formatFlightError(it) },
                    failedSources.takeIf { it.isNotEmpty() }?.let { "${it.joinToString(", ")} unavailable. Retry to reconnect." },
                )
                val entries = ((quakeResult?.getOrDefault(emptyList()) ?: emptyList()) + (flightResult?.getOrDefault(emptyList()) ?: emptyList()) +
                    finished.flatMap { it.second.getOrDefault(emptyList()) } + infrastructure)
                    .filter { it.layer != RadarLayer.SEISMIC || it.magnitude == null || it.magnitude >= options.minimumMagnitude }
                    .distinctBy { it.id }.sortedByDescending { it.observedAt }
                _liveRadar.value = LiveRadarState(
                    signals = entries,
                    loading = false,
                    message = when {
                        errors.isNotEmpty() -> errors.joinToString(" ")
                        !final -> "Loading more layers…"
                        else -> buildList {
                            if (options.seismicEnabled) add("USGS live")
                            if (options.flightsEnabled) add(if (token == null) "Sign in for flights" else "GodsEye flights")
                            if (enabledSources.isNotEmpty()) add("${enabledSources.size} GodsEye map layers")
                        }.joinToString(" · ").ifBlank { "Sources are disabled in Settings" }
                    },
                    hasError = errors.isNotEmpty(),
                )
            }

            // Phase 1: the quick feeds (earthquakes, flights, fast tier), capped so the boot screen never hangs on a slow source.
            val quick = listOf<kotlinx.coroutines.Deferred<*>>(earthquakes, flights) +
                bootstrap.filter { it.first.tier != BootstrapTier.SLOW && it.first.tier != BootstrapTier.ON_DEMAND }.map { it.second }
            kotlinx.coroutines.withTimeoutOrNull(BOOT_WAIT_MILLIS) { quick.forEach { it.await() } }
            publish(final = false)
            // Phase 2: the heavy tiers fill in as they finish.
            earthquakes.await(); flights.await(); bootstrap.forEach { it.second.await() }
            publish(final = true)
        }
    }

    private fun formatFlightError(error: Throwable): String = when (error) {
        is FlightException -> error.displayMessage
        is IllegalArgumentException -> error.message ?: "Sign in again to load flights."
        else -> "Flights unavailable (${error.message?.take(50) ?: "error"}). Sign in again or retry."
    }

    private val _screen = MutableStateFlow(Screen.RADAR)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _explorePanelId = MutableStateFlow<String?>(null)
    val explorePanelId: StateFlow<String?> = _explorePanelId.asStateFlow()

    private val prefs = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    private val _theme = MutableStateFlow(ThemeChoice.from(prefs.getString(KEY_THEME, null)))
    val theme: StateFlow<ThemeChoice> = _theme.asStateFlow()

    private val _colorStyle = MutableStateFlow(ColorStyle.from(prefs.getString(KEY_COLOR_STYLE, null)))
    val colorStyle: StateFlow<ColorStyle> = _colorStyle.asStateFlow()

    /** true = 3D globe, false = vector 3D/2D map. Default to OpenFreeMap 3D vector map. */
    private val _isGlobe = MutableStateFlow(prefs.getBoolean(KEY_GLOBE, false))
    val isGlobe: StateFlow<Boolean> = _isGlobe.asStateFlow()

    fun setGlobe(isGlobe: Boolean) {
        prefs.edit().putBoolean(KEY_GLOBE, isGlobe).apply()
        _isGlobe.value = isGlobe
    }

    /** Applies at once and stays on the Theme page, like Suvidha. */
    fun setColorStyle(style: ColorStyle) {
        prefs.edit().putString(KEY_COLOR_STYLE, style.name).apply()
        _colorStyle.value = style
    }

    init {
        ExploreStore.initialize(application)
        ExploreCatalogStore.initialize(application)
        viewModelScope.launch { ExploreCatalogStore.refresh(application, BuildConfig.EXPLORE_CATALOG_URL) }
        refreshLiveRadar()
        viewModelScope.launch {
            nativePreferences.map { listOf(it.seismicEnabled, it.flightsEnabled, it.minimumMagnitude, it.disabledRadarSources) }
                .distinctUntilChanged().drop(1).collect { refreshLiveRadar() }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                refreshLiveRadar(isBackground = true)
            }
        }
    }

    // ---- navigation ----

    fun openSettings() {
        _screen.value = Screen.SETTINGS
    }

    fun closeSettings() {
        _screen.value = Screen.RADAR
    }

    fun openTheme() {
        _screen.value = Screen.THEME
    }

    fun setTheme(choice: ThemeChoice) {
        prefs.edit().putString(KEY_THEME, choice.name).apply()
        _theme.value = choice
        _screen.value = Screen.SETTINGS
    }

    fun openExplore() {
        _screen.value = Screen.EXPLORE
    }

    fun openExplorePanel(id: String) {
        _explorePanelId.value = id
        _screen.value = Screen.PANEL
    }

    fun closeExplorePanel() {
        _screen.value = Screen.EXPLORE
    }

    fun closeExplore() {
        _screen.value = Screen.RADAR
    }
}
