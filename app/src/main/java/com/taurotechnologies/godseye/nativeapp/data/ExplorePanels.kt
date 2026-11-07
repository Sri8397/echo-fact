package com.taurotechnologies.godseye.nativeapp.data

import android.content.Context
import androidx.core.content.edit
import com.taurotechnologies.godseye.nativeapp.data.explore.ExploreView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** One web panel in the Explore catalog. [id] matches the web panel id. */
data class ExplorePanel(val id: String, val title: String, val category: String)

/**
 * Where a catalog panel's data comes from. `bootstrap`: a public bootstrap [key] and its [tier]
 * (fast, slow or on-demand). `static`: [data] carried in the catalog itself, for curated lists
 * such as live camera streams that change without a data feed.
 */
@Serializable
data class ExploreSource(
    val type: String = TYPE_BOOTSTRAP,
    val key: String = "",
    val tier: String = "",
    val data: JsonElement? = null,
) {
    companion object {
        const val TYPE_BOOTSTRAP = "bootstrap"
        const val TYPE_STATIC = "static"
    }
}

/**
 * One catalog entry. With [view] and [source] the server fully defines the panel and
 * [com.taurotechnologies.godseye.nativeapp.data.explore.ExploreViewEngine] renders it. Without a view,
 * the app's built-in parser for [id] is used, when one exists.
 */
@Serializable
data class ExploreCatalogPanel(
    val id: String,
    val title: String,
    val category: String,
    val enabled: Boolean = true,
    val source: ExploreSource? = null,
    val view: ExploreView? = null,
)

/** The Explore catalog: categories in display order and the panels in each. Version 1 format. */
@Serializable
data class ExploreCatalog(val version: Int, val categories: List<String>, val panels: List<ExploreCatalogPanel>) {
    /** Enabled panels with a known category, first occurrence of each id, in catalog order. */
    val visiblePanels: List<ExplorePanel> by lazy {
        panels.asSequence()
            .filter { it.enabled && it.category in categories && it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
            .map { ExplorePanel(it.id, it.title, it.category) }
            .toList()
    }

    /** Categories that have at least one visible panel, in catalog order. */
    val visibleCategories: List<String> by lazy {
        val used = visiblePanels.mapTo(HashSet()) { it.category }
        categories.distinct().filter { it in used }
    }

    fun panel(id: String): ExploreCatalogPanel? = panels.firstOrNull { it.id == id && it.enabled }

    companion object {
        const val VERSION = 1
        val EMPTY = ExploreCatalog(VERSION, emptyList(), emptyList())
        private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

        /** Parses catalog JSON. Returns null for malformed JSON, a newer major version or an empty catalog. */
        fun parse(text: String): ExploreCatalog? = try {
            json.decodeFromString(serializer(), text)
                .takeIf { it.version == VERSION && it.visiblePanels.isNotEmpty() }
        } catch (e: kotlinx.serialization.SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

/**
 * Holds the Explore catalog. It starts from the catalog bundled in the app, then from the last server
 * copy saved on this phone, then refreshes from [remoteUrl] when one is configured. A bad server copy
 * never replaces a good one.
 */
object ExploreCatalogStore {
    const val ASSET_NAME = "explore-catalog.v1.json"
    private const val PREFERENCES = "godseye_explore_catalog"
    private const val KEY_REMOTE = "remote_catalog_v1"

    private val mutableCatalog = MutableStateFlow(ExploreCatalog.EMPTY)
    val catalog: StateFlow<ExploreCatalog> = mutableCatalog.asStateFlow()
    val current: ExploreCatalog get() = mutableCatalog.value

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    @Synchronized
    fun initialize(context: Context) {
        if (mutableCatalog.value !== ExploreCatalog.EMPTY) return
        val app = context.applicationContext
        val bundled = runCatching { app.assets.open(ASSET_NAME).bufferedReader().use { it.readText() } }
            .getOrNull()?.let(ExploreCatalog::parse)
        val saved = app.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY_REMOTE, null)
            ?.let(ExploreCatalog::parse)
        mutableCatalog.value = saved ?: bundled ?: ExploreCatalog.EMPTY
    }

    /** Fetches the server catalog. Returns true when a valid catalog replaced the current one. */
    suspend fun refresh(context: Context, remoteUrl: String): Boolean {
        if (!remoteUrl.startsWith("https://")) return false
        val text = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(remoteUrl)
                    .header("User-Agent", "GodsEyeNative/1.0").header("Accept", "application/json").get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body.string() else null
                }
            }.getOrNull()
        } ?: return false
        val fresh = ExploreCatalog.parse(text) ?: return false
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit { putString(KEY_REMOTE, text) }
        mutableCatalog.value = fresh
        return true
    }
}

fun explorePanel(id: String): ExplorePanel? = ExploreCatalogStore.current.visiblePanels.firstOrNull { it.id == id }

/** Saved (starred) Explore panels, kept on this phone. */
object ExploreStore {
    private const val PREFERENCES = "godseye_explore"
    private const val KEY_SAVED = "saved_panels"

    private val mutableSaved = MutableStateFlow<Set<String>>(emptySet())
    val saved: StateFlow<Set<String>> = mutableSaved.asStateFlow()

    @Synchronized
    fun initialize(context: Context) {
        mutableSaved.value = context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getStringSet(KEY_SAVED, emptySet())
            ?.toSet() ?: emptySet()
    }

    @Synchronized
    fun toggle(context: Context, id: String) {
        val updated = if (id in mutableSaved.value) mutableSaved.value - id else mutableSaved.value + id
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit { putStringSet(KEY_SAVED, updated) }
        mutableSaved.value = updated
    }
}
