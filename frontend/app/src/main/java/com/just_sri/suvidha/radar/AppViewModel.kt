package com.just_sri.suvidha.radar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_sri.suvidha.radar.data.ApiException
import com.just_sri.suvidha.radar.data.FALLBACK_INCIDENTS
import com.just_sri.suvidha.radar.data.GENERIC_FAILURE
import com.just_sri.suvidha.radar.data.Incident
import com.just_sri.suvidha.radar.data.IncidentLoad
import com.just_sri.suvidha.radar.data.RadarApi
import com.just_sri.suvidha.radar.data.TokenStore
import com.just_sri.suvidha.radar.ui.theme.ColorStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Region(val id: String, val label: String, val lat: Double, val lng: Double, val zoom: Double)

val REGIONS: List<Region> = listOf(
    Region("world", "World", 20.0, 0.0, 2.0),
    Region("americas", "Americas", 15.0, -90.0, 3.0),
    Region("europe", "Europe", 50.0, 15.0, 4.0),
    Region("asia", "Asia-Pacific", 25.0, 105.0, 3.0),
)

// Keeps backend calls disabled for this prototype build.
const val RADAR_PROTOTYPE: Boolean = true

const val ALL_CATEGORIES: String = "all"

val CATEGORIES: List<Pair<String, String>> = listOf(
    ALL_CATEGORIES to "All News",
    "Science & Tech" to "Science & Tech",
    "Disaster & Rescue" to "Disaster & Rescue",
    "Geopolitical" to "Geopolitical",
    "Environmental" to "Environmental",
)

private const val INCIDENT_ZOOM: Double = 5.0
private const val DEFAULT_PIPELINE_PROMPT: String = "Discover and research recent major global news updates"
private const val LOGIN_FAILURE: String = "Couldn't sign in. Try again."
private const val SESSION_EXPIRED: String = "Signed out. Please sign in again."

enum class Screen { RADAR, DETAIL, SECTION, SETTINGS, THEME, ADMIN }

/** A part of a story that opens on its own page from the story detail. */
enum class StorySection(val title: String) {
    HAPPENED("When it happened"),
    TODAY("Where it stands today"),
    TIMELINE("Event timeline"),
    FACTS("Key data points"),
    QUESTIONS("Questions to explore"),
}

/** Appearance choice, saved on the device (same options as Suvidha's Theme page). */
enum class ThemeChoice(val title: String, val subtitle: String?) {
    SYSTEM("System default", "Follow device appearance"),
    LIGHT("Light", null),
    DARK("Dark", null);

    companion object {
        fun from(value: String?): ThemeChoice = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

private const val PREFS_NAME: String = "suvidha_radar"
private const val KEY_THEME: String = "theme"
private const val KEY_COLOR_STYLE: String = "color_style"
private const val KEY_GLOBE: String = "map_globe"
private const val KEY_GLOBE_ROTATION: String = "globe_auto_rotate"

enum class AdminTab(val label: String) { ADD_URL("Add story"), RECORDS("Stories"), PIPELINE("Find stories") }

/** [sequence] changes on every request so the map flies even when the target repeats. */
data class FlyTarget(val lat: Double, val lng: Double, val zoom: Double, val sequence: Long)

/** Blue for success, red for failure (Suvidha toast rule). */
data class Toast(val text: String, val isSuccess: Boolean)

data class RadarState(
    val incidents: List<Incident> = emptyList(),
    val isLive: Boolean = false,
    val isLoading: Boolean = true,
    val query: String = "",
    val category: String = ALL_CATEGORIES,
    val regionId: String = "world",
    val selectedId: String? = null,
    val fly: FlyTarget = FlyTarget(20.0, 0.0, 2.0, 0),
) {
    val visible: List<Incident>
        get() = incidents.filter { incident ->
            val q: String = query.trim().lowercase()
            val categoryOk: Boolean = category == ALL_CATEGORIES || incident.category == category
            val queryOk: Boolean = q.isEmpty() ||
                incident.title.lowercase().contains(q) ||
                incident.locationName.lowercase().contains(q) ||
                incident.category.lowercase().contains(q)
            categoryOk && queryOk
        }

    val selected: Incident? get() = incidents.firstOrNull { it.id == selectedId }
}

data class AdminState(
    val isSignedIn: Boolean = false,
    val isBusy: Boolean = false,
    val loginError: String? = null,
    val tab: AdminTab = AdminTab.ADD_URL,
    val pipelinePrompt: String = DEFAULT_PIPELINE_PROMPT,
    val recordFilter: String = "",
    val deleteTarget: Incident? = null,
    val jsonTarget: Incident? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val api: RadarApi = RadarApi(BuildConfig.API_BASE_URL, TokenStore(application))

    private val _screen = MutableStateFlow(Screen.RADAR)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _radar = MutableStateFlow(RadarState())
    val radar: StateFlow<RadarState> = _radar.asStateFlow()

    private val _admin = MutableStateFlow(AdminState(isSignedIn = api.isSignedIn()))
    val admin: StateFlow<AdminState> = _admin.asStateFlow()

    private val _toast = MutableStateFlow<Toast?>(null)
    val toast: StateFlow<Toast?> = _toast.asStateFlow()

    private val prefs = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    private val _theme = MutableStateFlow(ThemeChoice.from(prefs.getString(KEY_THEME, null)))
    val theme: StateFlow<ThemeChoice> = _theme.asStateFlow()

    private val _colorStyle = MutableStateFlow(ColorStyle.from(prefs.getString(KEY_COLOR_STYLE, null)))
    val colorStyle: StateFlow<ColorStyle> = _colorStyle.asStateFlow()

    /** true = 3D globe, false = flat 2D map. */
    private val _isGlobe = MutableStateFlow(prefs.getBoolean(KEY_GLOBE, RADAR_PROTOTYPE))
    val isGlobe: StateFlow<Boolean> = _isGlobe.asStateFlow()

    private val _globeAutoRotate = MutableStateFlow(prefs.getBoolean(KEY_GLOBE_ROTATION, true))
    val globeAutoRotate: StateFlow<Boolean> = _globeAutoRotate.asStateFlow()

    fun setGlobeAutoRotate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GLOBE_ROTATION, enabled).apply()
        _globeAutoRotate.value = enabled
    }

    fun setGlobe(isGlobe: Boolean) {
        prefs.edit().putBoolean(KEY_GLOBE, isGlobe).apply()
        _isGlobe.value = isGlobe
    }

    /** Applies at once and stays on the Theme page, like Suvidha. */
    fun setColorStyle(style: ColorStyle) {
        prefs.edit().putString(KEY_COLOR_STYLE, style.name).apply()
        _colorStyle.value = style
    }

    val apiBaseUrl: String = BuildConfig.API_BASE_URL

    private var flySequence: Long = 0

    init {
        refresh()
    }

    // ---- radar ----

    fun refresh() {
        if (RADAR_PROTOTYPE) {
            _radar.update { it.copy(incidents = FALLBACK_INCIDENTS, isLive = false, isLoading = false) }
            return
        }
        _radar.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val load: IncidentLoad = api.fetchIncidents()
            _radar.update { state ->
                val keepSelection: String? = state.selectedId?.takeIf { id -> load.incidents.any { it.id == id } }
                state.copy(incidents = load.incidents, isLive = load.isLive, isLoading = false, selectedId = keepSelection)
            }
        }
    }

    fun setQuery(query: String) {
        _radar.update { it.copy(query = query) }
    }

    fun setCategory(category: String) {
        _radar.update { it.copy(category = category) }
    }

    fun flyToRegion(region: Region) {
        _radar.update { it.copy(regionId = region.id, fly = nextFly(region.lat, region.lng, region.zoom)) }
    }

    /** Selects on the map and in the stream; on phones it also opens the detail page. */
    fun openIncident(incident: Incident) {
        _radar.update {
            it.copy(selectedId = incident.id, fly = nextFly(incident.lat, incident.lng, INCIDENT_ZOOM))
        }
        _screen.value = Screen.DETAIL
    }

    private val _section = MutableStateFlow(StorySection.HAPPENED)
    val section: StateFlow<StorySection> = _section.asStateFlow()

    fun openSection(section: StorySection) {
        _section.value = section
        _screen.value = Screen.SECTION
    }

    fun closeSection() {
        _screen.value = Screen.DETAIL
    }

    fun closeDetail() {
        _radar.update { it.copy(selectedId = null) }
        _screen.value = Screen.RADAR
    }

    private fun nextFly(lat: Double, lng: Double, zoom: Double): FlyTarget {
        flySequence += 1
        return FlyTarget(lat, lng, zoom, flySequence)
    }

    // ---- navigation ----

    fun openSettings() {
        _admin.update { it.copy(isSignedIn = api.isSignedIn()) }
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

    /** Opens the admin console (sign-in when signed out), optionally on a given tab. Back returns to Settings. */
    fun openAdmin(tab: AdminTab? = null) {
        if (RADAR_PROTOTYPE) return
        _admin.update { it.copy(isSignedIn = api.isSignedIn(), tab = tab ?: it.tab) }
        _screen.value = Screen.ADMIN
    }

    fun closeAdmin() {
        _screen.value = Screen.SETTINGS
    }

    fun dismissToast() {
        _toast.value = null
    }

    // ---- admin ----

    fun signIn(password: String) {
        if (password.isBlank() || _admin.value.isBusy) return
        _admin.update { it.copy(isBusy = true, loginError = null) }
        viewModelScope.launch {
            val error: String? = runCatchingSafe(LOGIN_FAILURE) { api.login(password.trim()) }
            _admin.update { it.copy(isBusy = false, isSignedIn = error == null, loginError = error) }
            if (error == null) {
                refresh()
                _toast.value = Toast("Signed in as super admin", isSuccess = true)
                _screen.value = Screen.SETTINGS
            }
        }
    }

    fun signOut() {
        api.signOut()
        _admin.value = AdminState()
        _toast.value = Toast("Signed out", isSuccess = true)
        _screen.value = Screen.SETTINGS
    }

    fun setAdminTab(tab: AdminTab) {
        _admin.update { it.copy(tab = tab) }
    }

    fun setPipelinePrompt(prompt: String) {
        _admin.update { it.copy(pipelinePrompt = prompt) }
    }

    fun setRecordFilter(filter: String) {
        _admin.update { it.copy(recordFilter = filter) }
    }

    fun showJson(incident: Incident?) {
        _admin.update { it.copy(jsonTarget = incident) }
    }

    fun askDelete(incident: Incident?) {
        _admin.update { it.copy(deleteTarget = incident) }
    }

    /** Returns true when the URL was ingested, so the screen can clear its field. */
    fun importUrl(url: String, onDone: (Boolean) -> Unit) {
        val clean: String = url.trim()
        if (clean.isEmpty() || _admin.value.isBusy) return
        runAdminAction(onDone) {
            val added: Incident = api.addIncidentFromUrl(clean)
            refresh()
            "Imported \"${added.title}\""
        }
    }

    fun runPipeline() {
        val prompt: String = _admin.value.pipelinePrompt.trim()
        if (prompt.isEmpty() || _admin.value.isBusy) return
        runAdminAction { api.runPipeline(prompt) }
    }

    fun confirmDelete() {
        val target: Incident = _admin.value.deleteTarget ?: return
        runAdminAction {
            api.deleteIncident(target.id)
            _radar.update { state -> state.copy(incidents = state.incidents.filterNot { it.id == target.id }) }
            refresh()
            "Deleted \"${target.title}\""
        }
    }

    private fun runAdminAction(onDone: (Boolean) -> Unit = {}, action: suspend () -> String) {
        _admin.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            var message: String = GENERIC_FAILURE
            val error: String? = runCatchingSafe(GENERIC_FAILURE) { message = action() }
            _admin.update { it.copy(isBusy = false, deleteTarget = null) }
            if (!api.isSignedIn()) {
                _admin.update { it.copy(isSignedIn = false, loginError = SESSION_EXPIRED) }
            }
            _toast.value = if (error == null) Toast(message, isSuccess = true) else Toast(error, isSuccess = false)
            onDone(error == null)
        }
    }

    /** Runs [block]; returns null on success, or a message that is safe to show (never raw exception text). */
    private suspend fun runCatchingSafe(fallback: String, block: suspend () -> Unit): String? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        e.message ?: fallback
    } catch (e: Exception) {
        fallback
    }
}
