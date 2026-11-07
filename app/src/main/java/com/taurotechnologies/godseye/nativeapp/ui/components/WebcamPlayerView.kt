package com.taurotechnologies.godseye.nativeapp.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taurotechnologies.godseye.nativeapp.BuildConfig
import com.taurotechnologies.godseye.nativeapp.data.IDLE_STOP_MINUTES_OPTIONS
import com.taurotechnologies.godseye.nativeapp.data.NativePreferencesStore
import com.taurotechnologies.godseye.nativeapp.data.STREAM_QUALITY_AUTO
import com.taurotechnologies.godseye.nativeapp.data.WebcamCameras
import kotlinx.coroutines.delay

/** Origin the embed page claims, so YouTube accepts the player inside the app's WebView. */
private const val EMBED_BASE_URL = "https://www.worldmonitor.app/"
private const val STOPPED_TAG = "stopped"
private const val IDLE_CHECK_MILLIS = 30_000L
private val LiveRed = Color(0xFFFF5C8A)
private val Panel = Color(0xE61A2233)
private val Muted = Color(0xFF9CA3AF)

/**
 * Plays a live webcam inside the app, the same way the satellite eye does: a WebView that holds only the
 * YouTube embed. With no touch for the chosen idle-stop time the stream is unloaded until the viewer resumes.
 * The header keeps a "YouTube" fallback for streams that refuse embedding. The square zoom button opens a
 * landscape, video-only view; its back button returns here.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebcamPlayerView(title: String, subtitle: String?, sourceUrl: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val view = LocalView.current
    val preferences by NativePreferencesStore.state.collectAsStateWithLifecycle()
    val videoId = remember(sourceUrl) { WebcamCameras.youtubeVideoId(sourceUrl) }
    val idleMinutes = preferences.idleStopMinutes
    val idleLabel = IDLE_STOP_MINUTES_OPTIONS.firstOrNull { it.first == idleMinutes }?.second

    var stopped by remember(sourceUrl) { mutableStateOf(false) }
    var failed by remember(sourceUrl) { mutableStateOf(false) }
    var lastActivity by remember(sourceUrl) { mutableLongStateOf(System.currentTimeMillis()) }
    var fullscreen by remember(sourceUrl) { mutableStateOf(false) }

    // The back button leaves the landscape view first; only a second press closes the player.
    BackHandler(enabled = fullscreen) { fullscreen = false }

    // Landscape view: force landscape and hide the system bars while it is open, then restore both on exit.
    DisposableEffect(fullscreen) {
        val inFullscreen = fullscreen
        val activity = context.findActivity()
        val window = (view.parent as? DialogWindowProvider)?.window ?: activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (inFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (inFullscreen) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Idle stop: checked every IDLE_CHECK_MILLIS; any touch on the player resets the clock.
    LaunchedEffect(sourceUrl, idleMinutes, stopped) {
        if (stopped || idleMinutes <= 0) return@LaunchedEffect
        while (true) {
            delay(IDLE_CHECK_MILLIS)
            if (System.currentTimeMillis() - lastActivity >= idleMinutes * 60_000L) {
                stopped = true
                return@LaunchedEffect
            }
        }
    }

    val embedHtml = remember(videoId, preferences.streamQuality) {
        videoId?.let { embedPage(it, preferences.streamQuality, preview = false) }
    }
    val onTouch: () -> Unit = { lastActivity = System.currentTimeMillis() }
    val onResume: () -> Unit = {
        lastActivity = System.currentTimeMillis()
        stopped = false
    }

    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            CameraPicture(
                embedHtml = embedHtml,
                sourceUrl = sourceUrl,
                stopped = stopped,
                failed = failed,
                idleLabel = idleLabel,
                onFailed = { failed = it },
                onTouch = onTouch,
                onResume = onResume,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = { fullscreen = false },
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp).background(Panel, RoundedCornerShape(12.dp)),
            ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to camera", tint = Color.White) }
        }
    } else {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            // Portrait phones: the player is a true 16:9 box at the top, so YouTube's own controls sit on the video
            // instead of spreading across black letterbox. The zoom button sits in the space under it.
            val portrait = maxHeight > maxWidth
            Column(Modifier.fillMaxSize()) {
                // Header: close, the place and what is playing, the zoom button (landscape only; portrait has it below
                // the video), and the YouTube fallback. It sits above the player, not over it, so it never covers
                // YouTube's own title bar and channel avatar.
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)
                        .background(Panel, RoundedCornerShape(16.dp)).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Close live camera", tint = Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text("LIVE CAMERA", color = LiveRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        subtitle?.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted, fontSize = 11.sp, maxLines = 1) }
                    }
                    if (!portrait) ZoomSquare(onClick = { fullscreen = true })
                    if (isWebUrl(sourceUrl)) {
                        TextButton(onClick = { openWebUrl(context, sourceUrl) }) { Text("YouTube", color = Color.White, fontSize = 12.sp) }
                    }
                }
                CameraPicture(
                    embedHtml = embedHtml,
                    sourceUrl = sourceUrl,
                    stopped = stopped,
                    failed = failed,
                    idleLabel = idleLabel,
                    onFailed = { failed = it },
                    onTouch = onTouch,
                    onResume = onResume,
                    modifier = if (portrait) Modifier.fillMaxWidth().aspectRatio(16f / 9f) else Modifier.fillMaxWidth().weight(1f),
                )
                if (portrait) {
                    // Details sit beside the zoom button, under the video.
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            subtitle?.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted, fontSize = 13.sp) }
                            Text(
                                if (idleLabel != null) "Pauses after $idleLabel without a touch to save data." else "Auto-pause is off.",
                                color = Muted, fontSize = 12.sp,
                            )
                        }
                        ZoomSquare(onClick = { fullscreen = true })
                    }
                    Box(Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
}

/** The square button that opens the landscape, video-only view. */
@Composable
private fun ZoomSquare(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).background(Panel, RoundedCornerShape(12.dp)),
    ) { Icon(Icons.Outlined.Fullscreen, contentDescription = "Zoom video to full screen", tint = Color.White) }
}

/** The player picture with its stopped and failed notices. The whole video stays visible, uncropped. */
@Composable
private fun CameraPicture(
    embedHtml: String?,
    sourceUrl: String,
    stopped: Boolean,
    failed: Boolean,
    idleLabel: String?,
    onFailed: (Boolean) -> Unit,
    onTouch: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier) {
        if (embedHtml != null) {
            YoutubeEmbed(
                html = embedHtml,
                stopped = stopped,
                onFailed = onFailed,
                modifier = Modifier.fillMaxSize().pointerInput(sourceUrl) {
                    // Observes touches without consuming them, so the player still receives every tap.
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            onTouch()
                        }
                    }
                },
            )
        } else {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("This camera has no playable live stream in the app.", color = Color.White, fontSize = 14.sp)
            }
        }

        if (stopped) {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp).background(Panel, RoundedCornerShape(16.dp)).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Paused to save data", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("No activity for ${idleLabel ?: "a while"}.", color = Muted, fontSize = 12.sp)
                TextButton(onClick = onResume) { Text("Resume", color = LiveRed, fontWeight = FontWeight.Bold) }
            }
        }
        if (failed && !stopped) {
            Text(
                "This stream would not load here. Open it on YouTube.",
                color = Color.White, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)
                    .background(Panel, RoundedCornerShape(999.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** The minimal page that holds only the YouTube player iframe, pinned to the WebView's viewport. */
private fun embedPage(videoId: String, quality: String, preview: Boolean): String {
    val src = WebcamCameras.embedUrl(videoId, quality.takeIf { it != STREAM_QUALITY_AUTO }, preview)
    return """<!doctype html><html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}iframe{position:fixed;inset:0;border:0;width:100vw;height:100vh}</style>
</head><body><iframe src="$src" allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen referrerpolicy="strict-origin-when-cross-origin"></iframe></body></html>"""
}

/** A WebView that holds one embed page. [stopped] unloads the stream; [onFailed] reports whether the page itself failed to load. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YoutubeEmbed(
    html: String,
    stopped: Boolean,
    onFailed: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                // A wrap-content WebView sizes its page to the content, so the 100%-tall player
                // collapsed to 0 px. Fill the AndroidView instead.
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(AndroidColor.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
                webViewClient = object : WebViewClient() {
                    // The player stays put: nothing the page links to opens in place of it.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        if (request.isForMainFrame && request.url.toString().startsWith(EMBED_BASE_URL)) onFailed(true)
                    }
                }
            }
        },
        update = { view ->
            // view.tag records what is loaded, so a recomposition only reloads when the state changes.
            val wanted = if (stopped) STOPPED_TAG else html
            if (view.tag != wanted) {
                view.tag = wanted
                if (stopped) {
                    view.loadUrl("about:blank")
                } else {
                    onFailed(false)
                    view.loadDataWithBaseURL(EMBED_BASE_URL, html, "text/html", "utf-8", null)
                }
            }
        },
        onRelease = { view -> view.destroy() },
    )
}

/**
 * Inline live camera for the details sheet, like the satellite eye preview: a small muted player with no
 * controls (sheet scrolling owns the gestures). A tap opens the full player. It unloads after the idle-stop
 * time, since nothing inside it can be touched to keep it alive.
 */
@Composable
fun WebcamPreview(sourceUrl: String, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val preferences by NativePreferencesStore.state.collectAsStateWithLifecycle()
    val videoId = remember(sourceUrl) { WebcamCameras.youtubeVideoId(sourceUrl) } ?: return
    val idleMinutes = preferences.idleStopMinutes
    var stopped by remember(sourceUrl) { mutableStateOf(false) }
    var failed by remember(sourceUrl) { mutableStateOf(false) }
    LaunchedEffect(sourceUrl, idleMinutes, stopped) {
        if (stopped || idleMinutes <= 0) return@LaunchedEffect
        delay(idleMinutes * 60_000L)
        stopped = true
    }
    val html = remember(videoId, preferences.streamQuality) { embedPage(videoId, preferences.streamQuality, preview = true) }
    Box(modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
        YoutubeEmbed(html = html, stopped = stopped, onFailed = { failed = it }, modifier = Modifier.fillMaxSize())
        if (stopped || failed) {
            Text(
                if (failed) "Preview unavailable" else "Preview paused to save data",
                color = Muted, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center),
            )
        }
        // Catches every touch so the WebView never fights the sheet's scroll.
        Box(Modifier.fillMaxSize().clickable(onClickLabel = "Watch this camera full screen", onClick = onExpand))
        Text(
            "LIVE",
            color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                .background(LiveRed, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Text(
            "Tap to watch with sound",
            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                .background(Panel, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** The live camera player in a full-screen dialog window, so it sits above a bottom sheet or the Explore screen. */
@Composable
fun WatchLiveDialog(title: String, subtitle: String?, sourceUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        WebcamPlayerView(title = title, subtitle = subtitle, sourceUrl = sourceUrl, onClose = onDismiss)
    }
}
