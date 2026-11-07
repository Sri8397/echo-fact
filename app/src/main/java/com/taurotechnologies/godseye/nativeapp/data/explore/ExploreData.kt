package com.taurotechnologies.godseye.nativeapp.data.explore

import com.taurotechnologies.godseye.nativeapp.data.ExploreCatalogStore
import com.taurotechnologies.godseye.nativeapp.data.ExploreSource
import com.taurotechnologies.godseye.nativeapp.data.RpcClient
import com.taurotechnologies.godseye.nativeapp.data.RpcEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** One line in a panel: a label, optional detail, a formatted value and an optional signed change. */
data class ExploreRow(
    val title: String,
    val subtitle: String? = null,
    val value: String? = null,
    /** Signed change, for green or red colouring. Null when the row has no direction. */
    val change: Double? = null,
    val url: String? = null,
)

/** What a panel shows: an optional headline summary, an optional chart, and its rows. */
data class PanelContent(
    val summary: String? = null,
    val rows: List<ExploreRow> = emptyList(),
    /** Drawn above the rows. The rows stay the full table view of the same data. */
    val chart: ExploreChart? = null,
)

/** One plotted value. [display] is the formatted text shown beside it, with units. */
data class ExploreChartPoint(val label: String, val value: Double, val display: String)

enum class ExploreChartKind {
    /** Magnitude, ranked largest first, one hue. */
    BARS,
    /** Above or below zero, from a centre baseline, two hues. */
    DIVERGING_BARS,
    /** A single series over time, oldest to newest. */
    LINE,
}

/** A single-series chart. Its [title] names what is plotted, so it needs no legend. */
data class ExploreChart(val kind: ExploreChartKind, val title: String, val points: List<ExploreChartPoint>)

/** Public bootstrap tier a dataset lives in. On-demand keys are fetched one at a time with `keys=`. */
enum class BootstrapTier(val query: String) {
    FAST("fast"), SLOW("slow"), ON_DEMAND("on-demand");

    companion object {
        fun from(value: String?): BootstrapTier? = entries.firstOrNull { it.query == value }
    }
}

/**
 * Catalog `source.type` for a sebuf RPC. The catalog's `source.key` holds the endpoint as
 * `<domain>/v1/<method>` with an optional query, for example `prediction/v1/list-prediction-markets?public=1`.
 */
internal const val RPC_SOURCE_TYPE = "rpc"

/** Maps one Explore panel to a public bootstrap dataset and turns that dataset into rows. */
class ExploreParser(val key: String, val tier: BootstrapTier, val parse: (JsonElement) -> PanelContent)

/**
 * Loads Explore panel data from GodsEye's public bootstrap tiers, the same `public=1`
 * contract the web dashboard uses. No sign-in is needed. Each tier is cached for a few minutes.
 */
object ExploreDataRepository {
    private const val BASE_URL = "https://gods-eye.app/api/bootstrap"
    private const val CACHE_MILLIS: Long = 5 * 60 * 1000
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    /** One lock per request, so different tiers and keys fetch in parallel while repeat callers of one request still share it. */
    private val locks = java.util.concurrent.ConcurrentHashMap<Any, Mutex>()
    private fun lockFor(key: Any): Mutex = locks.getOrPut(key) { Mutex() }
    /** Cached responses keyed by request query: `tier=fast`, `tier=slow` or `keys=<name>`. */
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, JsonObject>>()

    /** Built-in parsers, keyed by the web panel id. A catalog view takes precedence over these. */
    val parsers: Map<String, ExploreParser> = listOf(
        MARKETS_PARSERS, WORLD_SECURITY_PARSERS, PLANET_TECH_PARSERS,
        BACKLOG_MARKETS_A_PARSERS, BACKLOG_MARKETS_B_PARSERS, BACKLOG_MARKETS_C_PARSERS, BACKLOG_PLANET_SECURITY_PARSERS,
    ).fold(emptyMap()) { merged, next ->
        val duplicates = merged.keys intersect next.keys
        check(duplicates.isEmpty()) { "Duplicate Explore parser ids: $duplicates" }
        merged + next
    }

    /** The catalog's server-defined view for [panelId], when its source is usable by this app version. */
    private fun serverView(panelId: String): Pair<ExploreSource, ExploreView>? {
        val panel = ExploreCatalogStore.current.panel(panelId) ?: return null
        val view = panel.view ?: return null
        val source = panel.source ?: return null
        val usable = when (source.type) {
            ExploreSource.TYPE_STATIC -> source.data != null
            ExploreSource.TYPE_BOOTSTRAP -> source.key.isNotBlank() && BootstrapTier.from(source.tier) != null
            RPC_SOURCE_TYPE -> RpcEndpoint.parse(source.key) != null
            else -> false // A source type from a newer catalog: fall back to the built-in parser, if any.
        }
        return if (usable) source to view else null
    }

    fun hasData(panelId: String): Boolean = serverView(panelId) != null || panelId in parsers

    /** True when the panel's data comes from a feed that can change; static catalog data does not. */
    fun isLive(panelId: String): Boolean =
        serverView(panelId)?.first?.type?.let { it != ExploreSource.TYPE_STATIC } ?: (panelId in parsers)

    /**
     * Content for [panelId], or null when the panel has no data feed. Throws on network failure.
     * [maxAgeMillis] caps how old cached data may be (0 forces a fetch); null uses the normal cache.
     */
    suspend fun load(panelId: String, maxAgeMillis: Long? = null): PanelContent? {
        serverView(panelId)?.let { (source, view) ->
            val element = when (source.type) {
                ExploreSource.TYPE_STATIC -> source.data
                RPC_SOURCE_TYPE -> rpcPayload(rpcCached(RpcEndpoint.parse(source.key)!!, maxAgeMillis))
                else -> dataset(source.key, BootstrapTier.from(source.tier)!!, maxAgeMillis)
            }
            return if (element == null || element is JsonNull) PanelContent(summary = NO_DATA)
            else ExploreViewEngine.render(view, element)
        }
        val parser = parsers[panelId] ?: return null
        val element = dataset(parser.key, parser.tier, maxAgeMillis) ?: return PanelContent(summary = NO_DATA)
        return parser.parse(element)
    }

    private const val NO_DATA = "No data published right now."

    /**
     * The data part of an RPC response. Sebuf responses can say `dataAvailable: false` when the
     * snapshot is missing, so that case is treated as no data instead of an empty list. Null means no data.
     */
    internal fun rpcPayload(root: JsonElement): JsonElement? {
        val available = (root as? JsonObject)?.get("dataAvailable") as? JsonPrimitive
        return if (available != null && available.booleanOrNull == false) null else root
    }

    /** One public bootstrap dataset, or null when it is not published. Shared by Explore and radar layers. */
    internal suspend fun dataset(key: String, tier: BootstrapTier, maxAgeMillis: Long? = null): JsonElement? {
        val query = if (tier == BootstrapTier.ON_DEMAND) "keys=${java.net.URLEncoder.encode(key, "UTF-8")}" else "tier=${tier.query}"
        val element = cached(query, maxAgeMillis ?: CACHE_MILLIS)[key]
        return if (element == null || element is JsonNull) null else element
    }

    /** Cached RPC responses keyed by endpoint (path and query). Per-endpoint lock and 5-minute default, like bootstrap. */
    private val rpcCache = java.util.concurrent.ConcurrentHashMap<RpcEndpoint, Pair<Long, JsonElement>>()

    private suspend fun rpcCached(endpoint: RpcEndpoint, maxAgeMillis: Long?): JsonElement = lockFor(endpoint).withLock {
        val cached = rpcCache[endpoint]
        if (cached != null && System.currentTimeMillis() - cached.first < (maxAgeMillis ?: CACHE_MILLIS)) return cached.second
        val fresh = RpcClient.shared.getJson(endpoint)
        rpcCache[endpoint] = System.currentTimeMillis() to fresh
        fresh
    }

    private suspend fun cached(query: String, maxAgeMillis: Long): JsonObject = lockFor(query).withLock {
        val cached = cache[query]
        if (cached != null && System.currentTimeMillis() - cached.first < maxAgeMillis) return cached.second
        val fresh = withContext(Dispatchers.IO) { fetch(query) }
        cache[query] = System.currentTimeMillis() to fresh
        fresh
    }

    private fun fetch(query: String): JsonObject {
        val request = Request.Builder()
            .url("$BASE_URL?$query&public=1")
            .header("User-Agent", "GodsEyeNative/1.0")
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GodsEye data unavailable (${response.code})")
            val body = response.body.string()
            val root = json.parseToJsonElement(body).jsonObject
            return root["data"]?.jsonObject ?: JsonObject(emptyMap())
        }
    }
}
