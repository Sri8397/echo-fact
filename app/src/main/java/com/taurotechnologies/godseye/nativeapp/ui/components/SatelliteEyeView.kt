package com.taurotechnologies.godseye.nativeapp.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.taurotechnologies.godseye.nativeapp.BuildConfig
import com.taurotechnologies.godseye.nativeapp.data.SatelliteMotion
import com.taurotechnologies.godseye.nativeapp.data.SatelliteObservation
import com.taurotechnologies.godseye.nativeapp.ui.home.MapFloatButton
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

private const val TAG = "SatelliteEye"
private const val EYE_URL = "https://appassets.androidplatform.net/assets/cesium/eye.html"

/**
 * Full-screen "satellite eye": the Earth seen from [satellite], riding its live orbit. The globe shows the
 * latest full day of NASA VIIRS imagery (not a video feed) over a sharp basemap, with the real day/night line.
 * Pinch zooms, drag swings the view; "Free" releases the camera to roam, the target button locks back on.
 */
/** Live handle to one eye globe: its WebView and what the page last reported. */
class EyeState(initialHeightM: Double) {
    var webView by mutableStateOf<WebView?>(null)
    var ready by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var follow by mutableStateOf(true)
    var viewHeightM by mutableDoubleStateOf(initialHeightM)
    fun js(code: String) { if (ready) webView?.evaluateJavascript(code, null) }
}

// Yesterday (UTC) is the newest day with full global coverage; today's swaths are still filling in.
internal fun eyeImageryDate(): String = LocalDate.now(ZoneOffset.UTC).minusDays(1).toString()

private val eyeJson = Json { encodeDefaults = true }

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EyeGlobe(satellite: SatelliteObservation, state: EyeState, imageryDate: String, modifier: Modifier = Modifier) {
    val satJson = remember(satellite) { eyeJson.encodeToString(SatelliteObservation.serializer(), satellite) }
    Box(modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val main = Handler(Looper.getMainLooper())
                val loader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                    .build()
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
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
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                            loader.shouldInterceptRequest(request.url)
                    }
                    addJavascriptInterface(object {
                        @JavascriptInterface fun satellite(): String = satJson
                        @JavascriptInterface fun imageryDate(): String = imageryDate
                        @JavascriptInterface fun onReady() { main.post { state.ready = true; state.error = null } }
                        @JavascriptInterface fun onError(message: String) { main.post { state.error = message } }
                        @JavascriptInterface fun onView(height: Double) { main.post { state.viewHeightM = height } }
                        @JavascriptInterface fun onFollow(on: Boolean) { main.post { state.follow = on } }
                    }, "Android")
                    loadUrl(EYE_URL)
                    state.webView = this
                }
            },
            onRelease = { view ->
                state.ready = false
                state.webView = null
                view.destroy()
            },
        )
        if (!state.ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val err = state.error
                if (err != null) Text("Couldn't open the satellite view: $err", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                else CircularProgressIndicator(color = EyeGreen)
            }
        }
    }
}

/**
 * Inline satellite eye for the details sheet: the same live, following globe, small and non-interactive
 * (sheet scrolling owns the gestures). A tap opens [SatelliteEyeView] to zoom and move.
 */
@Composable
fun SatelliteEyePreview(satellite: SatelliteObservation, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val state = remember(satellite.noradId) { EyeState(satellite.altitudeKm * 1000.0) }
    val imageryDate = remember { eyeImageryDate() }
    Box(modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))) {
        EyeGlobe(satellite, state, imageryDate, Modifier.fillMaxSize())
        // Catches every touch so the WebView never fights the sheet's scroll.
        Box(Modifier.fillMaxSize().clickable(onClickLabel = "Explore from this satellite", onClick = onExpand))
        Text(
            "Tap to zoom and explore",
            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                .background(Panel, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        )
        Text(
            "NASA VIIRS · $imageryDate UTC",
            color = Muted, fontSize = 10.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                .background(Panel, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SatelliteEyeView(satellite: SatelliteObservation, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val state = remember(satellite.noradId) { EyeState(satellite.altitudeKm * 1000.0) }
    var today by remember { mutableStateOf(true) }
    var now by remember { mutableDoubleStateOf(System.currentTimeMillis() / 1000.0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis() / 1000.0
        }
    }
    val imageryDate = remember { eyeImageryDate() }
    val js: (String) -> Unit = state::js

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        // Landscape is only ~360 dp tall: the header and telemetry keep a phone-sized width on the left and the
        // buttons sit in the top-right corner, so nothing overlaps.
        val landscape = maxWidth > maxHeight
        val panelWidth = if (landscape) Modifier.widthIn(max = 400.dp) else Modifier
        EyeGlobe(satellite, state, imageryDate, Modifier.fillMaxSize())

        // Header: back, name, honest label for what the imagery is.
        Row(
            Modifier.align(Alignment.TopStart).then(panelWidth).fillMaxWidth().statusBarsPadding().padding(12.dp)
                .background(Panel, RoundedCornerShape(16.dp)).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Close satellite eye", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("SATELLITE EYE", color = EyeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(satellite.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    if (today) "NASA VIIRS imagery · $imageryDate UTC · live orbit & daylight" else "Basemap imagery · live orbit & daylight",
                    color = Muted, fontSize = 11.sp, maxLines = 1,
                )
            }
        }

        // Right-side controls: the same round buttons and hold-to-repeat zoom as the home map.
        Column(
            (if (landscape) Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 12.dp)
            else Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MapZoomPill(onCommand = { command ->
                when (command) {
                    "zoomIn" -> js("zoom(0.5)")
                    "zoomOut" -> js("zoom(2)")
                }
            })
            MapFloatButton(if (state.follow) "Following" else "Follow satellite", {
                state.follow = !state.follow
                js("setFollow(${state.follow})")
            }, active = state.follow) {
                Icon(Icons.Outlined.GpsFixed, null, tint = if (state.follow) LocalSuvidhaColors.current.onGreen else LocalSuvidhaColors.current.ink, modifier = Modifier.size(20.dp))
            }
            MapFloatButton("Daily NASA imagery", {
                today = !today
                js("setToday($today)")
            }, active = today) {
                Icon(Icons.Outlined.Layers, null, tint = if (today) LocalSuvidhaColors.current.onGreen else LocalSuvidhaColors.current.ink, modifier = Modifier.size(20.dp))
            }
        }

        // Live telemetry.
        val pos = SatelliteMotion.position(satellite, now)
        Row(
            (if (landscape) Modifier.align(Alignment.BottomStart) else Modifier.align(Alignment.BottomCenter))
                .then(panelWidth).fillMaxWidth().navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = if (landscape) 16.dp else 28.dp)
                .background(Panel, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Stat("Below", formatLatLng(pos.lat, pos.lng))
            Stat("Altitude", "${satellite.altitudeKm.toInt()} km")
            Stat("Speed", String.format(Locale.ROOT, "%.1f km/s", satellite.velocityKmPerSec))
            Stat("View", if (state.viewHeightM >= 10_000) "${(state.viewHeightM / 1000).toInt()} km" else "${state.viewHeightM.toInt()} m")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label.uppercase(Locale.ROOT), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private val EyeGreen = Color(0xFF6EE7B7)
private val Panel = Color(0xE61A2233)
private val Muted = Color(0xFF9CA3AF)
