package com.just_sri.suvidha.radar.ui.components

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.just_sri.suvidha.radar.FlyTarget
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private const val GLOBE_FILL: Float = 0.44f
private const val MAX_SCALE: Float = 16f
private const val MAX_TILT: Float = 80f
private const val FLY_MILLIS: Int = 1100
private const val TAP_RADIUS_DP: Int = 28
private const val BASE_ZOOM: Double = 2.0

data class GlobeMarker(val id: String, val lat: Double, val lng: Double, val color: Color? = null)

/** Native GPU Earth with an offline NASA texture and Compose geographic beacons. */
@Composable
fun GlobeMap(
    markers: List<GlobeMarker>,
    selectedId: String?,
    fly: FlyTarget,
    onSelect: (String) -> Unit,
    onZoomChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context: Context = LocalContext.current
    val density = LocalDensity.current
    val centerLon = remember { Animatable(0f) }
    val centerLat = remember { Animatable(20f) }
    var scale: Float by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    val currentMarkers by rememberUpdatedState(markers)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnZoom by rememberUpdatedState(onZoomChanged)
    val tapRadiusPx: Float = with(density) { TAP_RADIUS_DP.dp.toPx() }

    LaunchedEffect(Unit) { currentOnZoom(BASE_ZOOM) }
    LaunchedEffect(fly.sequence) {
        if (fly.sequence == 0L) return@LaunchedEffect
        val targetScale: Float = 2.0.pow(fly.zoom - BASE_ZOOM).toFloat().coerceIn(1f, MAX_SCALE)
        launch { centerLon.animateTo(nearestLongitude(centerLon.value, fly.lng.toFloat()), tween(FLY_MILLIS)) }
        launch { centerLat.animateTo(fly.lat.toFloat().coerceIn(-MAX_TILT, MAX_TILT), tween(FLY_MILLIS)) }
        animate(scale, targetScale, animationSpec = tween(FLY_MILLIS)) { value, _ -> scale = value }
        currentOnZoom(zoomOf(scale))
    }

    val renderer = remember(context) { EarthRenderer(context.applicationContext) }
    val surface = remember(context) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(surface, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { surface.onResume(); surface.requestRender() }
                Lifecycle.Event.ON_PAUSE -> surface.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) surface.onResume()
        onDispose { lifecycle.removeObserver(observer); surface.onPause() }
    }
    val renderedLongitude = centerLon.value
    val renderedLatitude = centerLat.value
    val renderedScale = scale
    SideEffect {
        renderer.updateCamera(renderedLongitude, renderedLatitude, renderedScale)
        surface.requestRender()
    }
    Box(modifier.background(Color(0xFF030A10))) {
        AndroidView(factory = { surface }, modifier = Modifier.matchParentSize())
        Canvas(
            modifier = Modifier.matchParentSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val radius: Float = min(size.width, size.height) * GLOBE_FILL * scale
                        val degreesPerPx: Float = (180f / PI.toFloat()) / radius
                        scale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                        scope.launch {
                            centerLon.snapTo(centerLon.value - pan.x * degreesPerPx)
                            centerLat.snapTo((centerLat.value + pan.y * degreesPerPx).coerceIn(-MAX_TILT, MAX_TILT))
                        }
                        currentOnZoom(zoomOf(scale))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val projector = GlobeProjector(centerLon.value, centerLat.value, size.width / 2f, size.height / 2f, min(size.width, size.height) * GLOBE_FILL * scale)
                        val point = FloatArray(2)
                        val hit: GlobeMarker? = currentMarkers
                            .filter { projector.project(it.lng.toRadians(), it.lat.toRadians(), point) && hypot(point[0] - tap.x, point[1] - tap.y) <= tapRadiusPx }
                            .minByOrNull { projector.project(it.lng.toRadians(), it.lat.toRadians(), point); hypot(point[0] - tap.x, point[1] - tap.y) }
                        if (hit != null) currentOnSelect(hit.id)
                    }
                },
        ) {
            val radius: Float = min(size.width, size.height) * GLOBE_FILL * scale
            val center = Offset(size.width / 2f, size.height / 2f)
            val projector = GlobeProjector(centerLon.value, centerLat.value, center.x, center.y, radius)
            val point = FloatArray(2)
            for (marker in markers) {
                if (!projector.project(marker.lng.toRadians(), marker.lat.toRadians(), point)) continue
                val isSelected: Boolean = marker.id == selectedId
                val spot = Offset(point[0], point[1])
                val baseTint = marker.color ?: Color(0xFF51E6BA)
                val tint = if (baseTint.luminance() < 0.3f) lerp(baseTint, Color.White, 0.35f) else baseTint
                val beaconRadius = (if (isSelected) 9 else 6).dp.toPx()
                drawCircle(
                    Brush.radialGradient(listOf(tint.copy(alpha = 0.48f), Color.Transparent), spot, beaconRadius * 2.6f),
                    radius = beaconRadius * 2.6f, center = spot,
                )
                drawCircle(Color(0xFF030A10).copy(alpha = 0.85f), radius = beaconRadius, center = spot)
                drawCircle(tint, radius = beaconRadius, center = spot, style = Stroke(2.dp.toPx()))
                if (isSelected) drawCircle(Color.White, radius = 2.dp.toPx(), center = spot)

            }
        }
    }
}

/** Orthographic projection centred on (lon0, lat0) in degrees. */
internal class GlobeProjector(lon0: Float, lat0: Float, private val cx: Float, private val cy: Float, private val radius: Float) {
    private val lambda0: Float = lon0.toRadians()
    private val sinPhi0: Float = sin(lat0.toRadians())
    private val cosPhi0: Float = cos(lat0.toRadians())

    /** Projects geographic radians to pixels; false hides points behind Earth. */
    fun project(lon: Float, lat: Float, out: FloatArray): Boolean {
        val cosPhi: Float = cos(lat)
        val sinPhi: Float = sin(lat)
        val deltaLambda: Float = lon - lambda0
        val cosDelta: Float = cos(deltaLambda)
        val cosC: Float = sinPhi0 * sinPhi + cosPhi0 * cosPhi * cosDelta
        val x: Float = cosPhi * sin(deltaLambda)
        val y: Float = cosPhi0 * sinPhi - sinPhi0 * cosPhi * cosDelta
        out[0] = cx + radius * x
        out[1] = cy - radius * y
        return cosC >= 0f
    }
}

/** Target longitude adjusted so the globe turns the short way round. */
private fun nearestLongitude(from: Float, to: Float): Float = to + 360f * ((from - to) / 360f).roundToInt()

private fun zoomOf(scale: Float): Double = BASE_ZOOM + ln(scale.toDouble()) / ln(2.0)

private fun Float.toRadians(): Float = this * (PI.toFloat() / 180f)

private fun Double.toRadians(): Float = (this * PI / 180.0).toFloat()
