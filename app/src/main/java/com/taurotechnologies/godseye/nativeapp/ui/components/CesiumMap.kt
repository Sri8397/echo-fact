package com.taurotechnologies.godseye.nativeapp.ui.components

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.taurotechnologies.godseye.nativeapp.data.WeatherCell
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.taurotechnologies.godseye.nativeapp.BuildConfig
import com.taurotechnologies.godseye.nativeapp.FlyTarget
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors
import kotlinx.coroutines.delay
import com.taurotechnologies.godseye.nativeapp.data.severityRank
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.math.ln
import kotlin.math.pow

private const val TAG = "CesiumMap"
private const val GLOBE_URL = "https://appassets.androidplatform.net/assets/cesium/globe.html"

/** Gives the CesiumJS engine (loaded from cesium.com) this long before the map shows an error. */
private const val LOAD_TIMEOUT_MS = 25_000L

private const val MSG_NETWORK = "The map could not load. Check your connection, then retry."
private const val MSG_ENGINE = "The map engine could not start. Check your connection, then retry."
private const val MSG_TIMEOUT = "The map is taking too long to load. Check your connection, then retry."
private const val MSG_IMAGERY = "Satellite imagery is unavailable offline. Map markers still show."

/** Camera height in metres that roughly matches a web-map zoom level. */
private const val ZOOM0_HEIGHT_M = 35_000_000.0

internal fun zoomToHeight(zoom: Double): Double = ZOOM0_HEIGHT_M / 2.0.pow(zoom)

/** Above this camera height the globe is in the far view: its centre stays on screen and tilt is hidden. Keep in sync with EARTH_LOCK_ABOVE_M in globe.html. */
internal const val EARTH_LOCK_ABOVE_M = 1_000_000.0

/** Height the map starts at on every open, and the height of the first flight (matches START_ALT_M in globe.html). */
internal const val MAP_START_HEIGHT_M = 20_000_000.0

/** Tilt controls and readout only make sense on the 3D globe below the far-view height. */
internal fun tiltAvailable(flat: Boolean, heightM: Double): Boolean = !flat && heightM <= EARTH_LOCK_ABOVE_M

/** Parses the WebView's `[{"id","x","y"}]` into planner input, using the marker's own severity and title. */
internal fun parseProjected(json: String, markers: List<GlobeMarker>, selectedId: String?): List<ScreenPoint> {
    val byId = markers.associateBy { it.id }
    val array = runCatching { Json.parseToJsonElement(json).jsonArray }.getOrNull() ?: return emptyList()
    return array.mapNotNull { element ->
        val obj = element.jsonObject
        val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val marker = byId[id] ?: return@mapNotNull null
        val x = obj["x"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        val y = obj["y"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        ScreenPoint(
            id = id,
            x = x,
            y = y,
            priority = severityRank(marker.severity),
            title = marker.title,
            lat = marker.lat,
            lng = marker.lng,
        )
    }
}

/** `{"labels":[{id,x,y,text}],"clusters":[{index,count,lat,lng}]}` for the page's `setPlan`. */
internal fun planJson(plan: MapPlan): String = buildJsonObject {
    put("labels", buildJsonArray {
        for (l in plan.labels) addJsonObject {
            put("id", l.id)
            put("text", l.text)
        }
    })
    put("clusters", buildJsonArray {
        for (c in plan.clusters) addJsonObject {
            put("index", c.index)
            put("count", c.count)
            put("lat", c.lat)
            put("lng", c.lng)
        }
    })
}.toString()

/** The anchored card's fields, or `null` to hide it. Text is sent as JSON strings and set with textContent on the page. */
internal fun calloutJson(callout: MapCallout?): String {
    if (callout == null) return "null"
    return buildJsonObject {
        put("id", callout.id)
        put("title", callout.title)
        put("layer", callout.layerLabel)
        put("color", callout.colorHex?.let { JsonPrimitive(it) } ?: JsonNull)
        put("time", callout.time)
        put("source", callout.source)
        put("fact", callout.fact)
        put("saved", callout.saved)
    }.toString()
}

internal fun weatherJson(cells: List<WeatherCell>): String = buildJsonArray {
    for (c in cells) addJsonObject {
        put("lat", c.lat)
        put("lng", c.lng)
        put("thunder", c.thunder)
        put("intensity", c.intensity)
    }
}.toString()

internal fun heightToZoom(height: Double): Double = ln(ZOOM0_HEIGHT_M / height.coerceAtLeast(1.0)) / ln(2.0)

/** [iconFor] draws the marker icon (Android bitmaps); JVM unit tests pass a stub since Bitmap is unavailable there. */
internal fun markersJson(
    markers: List<GlobeMarker>,
    selectedId: String? = null,
    iconFor: (name: String, argb: Int) -> String = ::iconDataUrl,
    /**
     * Icon keys the page already holds. Their image data is left out and the page reuses its copy, so the
     * once-a-second position update stays small (each icon is an embedded PNG). New keys are added here.
     */
    sentIcons: MutableSet<String>? = null,
    /** Keys whose image went into this JSON; the caller adds them to [sentIcons] once the page has received it. */
    newIconKeys: MutableSet<String>? = null,
): String = buildJsonArray {
    for (m in markers) addJsonObject {
        val name = causeIconId(m, m.id == selectedId)
        val argb = m.color?.toArgb() ?: 0xFF51E6BA.toInt()
        val key = "$name/$argb"
        if (sentIcons != null && key in sentIcons) {
            put("iconKey", key)
        } else {
            val icon = iconFor(name, argb)
            if (icon.isNotEmpty()) {
                put("icon", icon)
                put("iconKey", key)
                newIconKeys?.add(key)
            }
        }
        put("id", m.id)
        put("lat", m.lat)
        put("lng", m.lng)
        put("alt", m.altitudeMeters ?: 0.0)
        put("isFlight", m.isFlight)
        put("onGround", m.onGround)
        put("heading", (m.heading ?: 0f).toDouble())
        put("isSatellite", m.isSatellite)
        m.motion?.let { put("rate", buildJsonArray { add(JsonPrimitive(it.latDegPerSec)); add(JsonPrimitive(it.lngDegPerSec)) }) }
        put("color", m.color?.let { JsonPrimitive(String.format("#%06X", it.toArgb() and 0xFFFFFF)) } ?: JsonNull)
    }
}.toString()

/**
 * CesiumJS globe in a WebView, with a 2D overhead mode, a light or dark imagery theme, tappable
 * markers, and a visible error with Retry when the engine or imagery cannot load.
 * Attribution is drawn by Cesium's own credit bar at the bottom of the map (Esri, Maxar and
 * Earthstar Geographics credits, or Cesium ion credits when a token is configured).
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun CesiumMap(
    markers: List<GlobeMarker>,
    selectedId: String?,
    fly: FlyTarget,
    onSelect: (String) -> Unit,
    onZoomChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    onViewportBounds: (ViewportBounds) -> Unit = {},
    darkTheme: Boolean = false,
    /** Shows the rotate, tilt and zoom bar. The home map turns it off because its own controls take that corner. */
    navControls: Boolean = true,
    /** The anchored card for the selected marker, or null for none. */
    callout: MapCallout? = null,
    onCalloutDetails: (String) -> Unit = {},
    onCalloutSave: (String) -> Unit = {},
    /** A tap on empty map (not on a marker, label or badge). */
    onMapTap: () -> Unit = {},
    /** Called once the page has flown to [fly]; the caller uses it to end the open-time 20,000 km flight. */
    onFlown: () -> Unit = {},
    /** Heading and pitch in degrees, reported whenever the camera settles (drives the compass). */
    onPose: (heading: Float, pitch: Float) -> Unit = { _, _ -> },
    onCameraCenter: (lat: Double, lng: Double) -> Unit = { _, _ -> },
    /** A camera command from outside controls (see [NavCommand]); the sequence makes repeats run. */
    navCommand: NavCommand? = null,
    /** Reduce motion: no gliding between position ticks, no rain streaks or lightning flashes. */
    reduceMotion: Boolean = false,
    /** Imagery style id from [com.taurotechnologies.godseye.nativeapp.data.MAP_PROVIDER_OPTIONS]. */
    imageryStyle: String = "auto",
    /** Rain, cloud and thunder cells from the app's weather alerts (see [WeatherEffects]). */
    weatherCells: List<WeatherCell> = emptyList(),
    /** The selected flight's observed trail and projected path, or null for none. */
    trajectory: com.taurotechnologies.godseye.nativeapp.data.FlightTrajectory? = null,
    /** Terrain elevation in metres under the selected aircraft, reported when the globe has real terrain. */
    onGroundElevation: (id: String, meters: Double) -> Unit = { _, _ -> },
) {
    val currentOnGround by rememberUpdatedState(onGroundElevation)
    val currentOnPose by rememberUpdatedState(onPose)
    val currentOnCenter by rememberUpdatedState(onCameraCenter)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnZoom by rememberUpdatedState(onZoomChanged)
    val currentOnBounds by rememberUpdatedState(onViewportBounds)
    val currentOnDetails by rememberUpdatedState(onCalloutDetails)
    val currentOnSave by rememberUpdatedState(onCalloutSave)
    val currentOnMapTap by rememberUpdatedState(onMapTap)
    val currentOnFlown by rememberUpdatedState(onFlown)
    val currentMarkers by rememberUpdatedState(markers)
    val currentSelectedId by rememberUpdatedState(selectedId)
    var ready by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var failure by remember { mutableStateOf<String?>(null) }
    var imageryFailed by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var headingDeg by remember { mutableStateOf(0f) }
    var pitchDeg by remember { mutableStateOf(-90f) }
    var cameraHeightM by remember { mutableStateOf(0.0) }

    val retry: () -> Unit = {
        failure = null
        imageryFailed = false
        ready = false
        attempt++
        webView?.loadUrl(GLOBE_URL)
    }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val main = Handler(Looper.getMainLooper())
                val loader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                    .build()
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.rgb(2, 5, 11))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(m: android.webkit.ConsoleMessage): Boolean {
                            Log.d(TAG, "js ${m.messageLevel()}: ${m.message()} (${m.sourceId()}:${m.lineNumber()})")
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        // The map page never navigates; block any top-level navigation.
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true

                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                            loader.shouldInterceptRequest(request.url)

                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (!request.isForMainFrame) return
                            Log.w(TAG, "Main frame error ${error.errorCode}: ${error.description}")
                            main.post { failure = MSG_NETWORK }
                        }

                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                            if (!request.isForMainFrame) return
                            Log.w(TAG, "Main frame HTTP ${errorResponse.statusCode}")
                            main.post { failure = MSG_NETWORK }
                        }
                    }
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun ionToken(): String = BuildConfig.CESIUM_ION_TOKEN

                        @JavascriptInterface
                        fun onReady() {
                            main.post {
                                failure = null
                                ready = true
                            }
                        }

                        @JavascriptInterface
                        fun onMarkerClick(id: String) {
                            main.post { currentOnSelect(id) }
                        }

                        @JavascriptInterface
                        fun onCameraCenter(lat: Double, lng: Double) {
                            main.post { currentOnCenter(lat, lng) }
                        }

                        @JavascriptInterface
                        fun onCamera(height: Double, south: Double, north: Double, west: Double, east: Double) {
                            main.post {
                                cameraHeightM = height
                                currentOnZoom(heightToZoom(height))
                                currentOnBounds(ViewportBounds(south, north, west, east))
                            }
                        }

                        /** Screen positions of the markers after a camera move or a marker change. Labels are planned in Kotlin. */
                        @JavascriptInterface
                        fun onProjected(json: String, width: Double, height: Double, cameraHeight: Double) {
                            main.post {
                                val points = parseProjected(json, currentMarkers, currentSelectedId)
                                val plan = MapLabelPlanner.plan(points, cameraHeight, width, height, currentSelectedId)
                                webView?.evaluateJavascript("setPlan(${planJson(plan)})", null)
                            }
                        }

                        @JavascriptInterface
                        fun onCalloutDetails(id: String) {
                            main.post { currentOnDetails(id) }
                        }

                        @JavascriptInterface
                        fun onCalloutSave(id: String) {
                            main.post { currentOnSave(id) }
                        }

                        @JavascriptInterface
                        fun onGroundElevation(id: String, meters: Double) {
                            main.post { currentOnGround(id, meters) }
                        }

                        @JavascriptInterface
                        fun onMapTap() {
                            main.post { currentOnMapTap() }
                        }

                        @JavascriptInterface
                        fun onHeading(degrees: Double) {
                            main.post { headingDeg = degrees.toFloat(); currentOnPose(headingDeg, pitchDeg) }
                        }

                        @JavascriptInterface
                        fun onPitch(degrees: Double) {
                            main.post { pitchDeg = degrees.toFloat(); currentOnPose(headingDeg, pitchDeg) }
                        }

                        @JavascriptInterface
                        fun onImageryError() {
                            main.post { imageryFailed = true }
                        }

                        @JavascriptInterface
                        fun onError(message: String) {
                            Log.e(TAG, message)
                            main.post { failure = MSG_ENGINE }
                        }
                    }, "Android")
                    loadUrl(GLOBE_URL)
                    webView = this
                }
            },
            onRelease = { view ->
                ready = false
                webView = null
                view.destroy()
            },
        )

        // Fail visibly instead of leaving a blank WebView when Cesium never reports ready.
        LaunchedEffect(attempt, ready) {
            if (ready) return@LaunchedEffect
            delay(LOAD_TIMEOUT_MS)
            failure = failure ?: MSG_TIMEOUT
        }

        LaunchedEffect(ready, flat) {
            if (ready) webView?.evaluateJavascript("setFlat($flat)", null)
        }
        LaunchedEffect(ready, darkTheme) {
            if (ready) webView?.evaluateJavascript("setTheme($darkTheme)", null)
        }
        LaunchedEffect(ready, imageryStyle) {
            if (ready) webView?.evaluateJavascript("setImageryStyle(${JsonPrimitive(imageryStyle)})", null)
        }
        LaunchedEffect(ready, weatherCells) {
            if (ready) webView?.evaluateJavascript("setWeather(${weatherJson(weatherCells)})", null)
        }
        LaunchedEffect(ready, trajectory) {
            if (ready) webView?.evaluateJavascript("setTrajectory(${trajectoryJson(trajectory)})", null)
        }
        LaunchedEffect(ready, reduceMotion) {
            if (ready) webView?.evaluateJavascript("setReduceMotion($reduceMotion)", null)
        }
        // Icons the current page has cached; a reloaded page starts empty, so this resets with it.
        val sentIcons = remember(attempt, ready) { java.util.Collections.synchronizedSet(mutableSetOf<String>()) }
        LaunchedEffect(ready, markers, selectedId) {
            if (!ready) return@LaunchedEffect
            val selected = selectedId?.let { JsonPrimitive(it).toString() } ?: "null"
            // Hundreds of KB for a full feed: build it off the main thread so the app UI never stalls on it.
            val newKeys = mutableSetOf<String>()
            val json = withContext(Dispatchers.Default) { markersJson(markers, selectedId, sentIcons = sentIcons, newIconKeys = newKeys) }
            val view = webView ?: return@LaunchedEffect
            view.evaluateJavascript("setMarkers($json, $selected)", null)
            // Only now does the page hold these images; a cancelled update must not mark them sent.
            sentIcons.addAll(newKeys)
        }
        LaunchedEffect(ready, fly.sequence) {
            if (ready) {
                webView?.evaluateJavascript("flyTo(${fly.lng}, ${fly.lat}, ${zoomToHeight(fly.zoom)})", null)
                currentOnFlown()
            }
        }

        LaunchedEffect(ready, callout) {
            if (ready) webView?.evaluateJavascript("setCallout(${calloutJson(callout)})", null)
        }

        LaunchedEffect(navCommand?.sequence) {
            val c = navCommand
            if (ready && c != null) webView?.evaluateJavascript("nav('${c.command}')", null)
        }

        if (ready && failure == null && navControls) {
            MapNavControls(
                headingDeg = headingDeg,
                flat = flat,
                tiltAvailable = tiltAvailable(flat, cameraHeightM),
                onCommand = { webView?.evaluateJavascript("nav('$it')", null) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 8.dp, end = 8.dp, bottom = 28.dp),
            )
        }

        val error = failure
        if (error != null) {
            MapErrorPanel(error, onRetry = retry, modifier = Modifier.align(Alignment.Center).padding(24.dp))
        } else if (imageryFailed) {
            MapNotice(MSG_IMAGERY, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        }
    }
}

@Composable
private fun MapErrorPanel(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSuvidhaColors.current
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = colors.cardSurface) {
        Column(Modifier.padding(16.dp)) {
            Text(message, color = colors.ink, fontSize = 14.sp)
            Spacer(Modifier.padding(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRetry) { Text("Retry", color = colors.green) }
            }
        }
    }
}

@Composable
private fun MapNotice(message: String, modifier: Modifier = Modifier) {
    val colors = LocalSuvidhaColors.current
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = colors.cardSurface.copy(alpha = 0.85f)) {
        Text(message, color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

/** One camera command for the map ("zoomIn", "left", "rotateCw", "tiltUp", "north", ...). */
data class NavCommand(val command: String, val sequence: Long)
