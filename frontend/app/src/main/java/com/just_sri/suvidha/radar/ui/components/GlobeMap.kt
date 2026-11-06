package com.just_sri.suvidha.radar.ui.components

import android.content.Context
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.just_sri.suvidha.radar.FlyTarget
import com.just_sri.suvidha.radar.ui.theme.LocalSuvidhaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private const val LAND_ASSET: String = "land_110m.txt"
private const val GLOBE_FILL: Float = 0.44f
private const val MAX_SCALE: Float = 16f
private const val MAX_TILT: Float = 80f
private const val FLY_MILLIS: Int = 1100
private const val TAP_RADIUS_DP: Int = 28
private const val GRID_STEP: Int = 30
private const val BASE_ZOOM: Double = 2.0

data class GlobeMarker(val id: String, val lat: Double, val lng: Double, val color: Color? = null)

/** One land outline, as longitude/latitude in radians. */
private class LandRing(val lon: FloatArray, val lat: FloatArray)

/**
 * Native 3D globe (orthographic projection on a Compose Canvas, Natural Earth 1:110m land).
 * Drag to rotate, pinch to zoom, and tap a beacon to select its marker.
 * No WebView and no network, so it draws the same on every device.
 */
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
    val colors = LocalSuvidhaColors.current
    val density = LocalDensity.current
    val land: List<LandRing> by produceState(initialValue = emptyList()) { value = withContext(Dispatchers.IO) { loadLand(context) } }
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

    val ocean: Color = lerp(colors.canvas, colors.confirm, 0.24f)
    val landFill: Color = lerp(colors.canvas, colors.green, 0.42f)
    val coast: Color = colors.green.copy(alpha = 0.55f)
    val grid: Color = colors.muted.copy(alpha = 0.22f)
    val shade: Color = (if (colors.isDark) colors.canvas else colors.ink).copy(alpha = 0.28f)

    Canvas(
        modifier = modifier
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
                    val projector = Projector(centerLon.value, centerLat.value, size.width / 2f, size.height / 2f, min(size.width, size.height) * GLOBE_FILL * scale)
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
        val projector = Projector(centerLon.value, centerLat.value, center.x, center.y, radius)
        val sphere = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, radius)) }
        drawCircle(colors.green.copy(alpha = 0.18f), radius = radius + 6.dp.toPx(), center = center)
        drawCircle(ocean, radius = radius, center = center)
        clipPath(sphere) {
            drawGraticule(projector, grid)
            for (ring in land) drawLand(ring, projector, landFill, coast)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, shade),
                    center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
                    radius = radius * 1.6f,
                ),
                radius = radius,
                center = center,
            )
        }
        drawCircle(colors.green.copy(alpha = 0.35f), radius = radius, center = center, style = Stroke(width = 1.5.dp.toPx()))
        val point = FloatArray(2)
        for (marker in markers) {
            if (!projector.project(marker.lng.toRadians(), marker.lat.toRadians(), point)) continue
            val isSelected: Boolean = marker.id == selectedId
            val spot = Offset(point[0], point[1])
            drawCircle(if (isSelected) colors.accent else colors.onGreen, radius = (if (isSelected) 12 else 9).dp.toPx(), center = spot)
            drawCircle(marker.color ?: colors.green, radius = (if (isSelected) 9 else 7).dp.toPx(), center = spot)
            drawCircle(colors.onGreen, radius = 2.5.dp.toPx(), center = spot)
        }
    }
}

/** Orthographic projection centred on (lon0, lat0) in degrees. */
private class Projector(lon0: Float, lat0: Float, private val cx: Float, private val cy: Float, private val radius: Float) {
    private val lambda0: Float = lon0.toRadians()
    private val sinPhi0: Float = sin(lat0.toRadians())
    private val cosPhi0: Float = cos(lat0.toRadians())

    /**
     * Writes the screen point into [out] and returns true when it is on the visible side.
     * Points on the far side are pushed to the edge of the globe so land shapes stay closed.
     */
    fun project(lon: Float, lat: Float, out: FloatArray): Boolean {
        val cosPhi: Float = cos(lat)
        val sinPhi: Float = sin(lat)
        val deltaLambda: Float = lon - lambda0
        val cosDelta: Float = cos(deltaLambda)
        val cosC: Float = sinPhi0 * sinPhi + cosPhi0 * cosPhi * cosDelta
        var x: Float = cosPhi * sin(deltaLambda)
        var y: Float = cosPhi0 * sinPhi - sinPhi0 * cosPhi * cosDelta
        if (cosC < 0f) {
            val length: Float = hypot(x, y)
            if (length > 0f) {
                x /= length
                y /= length
            }
        }
        out[0] = cx + radius * x
        out[1] = cy - radius * y
        return cosC >= 0f
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLand(ring: LandRing, projector: Projector, fill: Color, coast: Color) {
    val point = FloatArray(2)
    val path = Path()
    var visibleCount = 0
    for (index in ring.lon.indices) {
        if (projector.project(ring.lon[index], ring.lat[index], point)) visibleCount++
        if (index == 0) path.moveTo(point[0], point[1]) else path.lineTo(point[0], point[1])
    }
    if (visibleCount == 0) return
    path.close()
    drawPath(path, fill)
    drawPath(path, coast, style = Stroke(width = 0.8.dp.toPx()))
}

/** Latitude and longitude lines every 30°, only on the visible side. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraticule(projector: Projector, color: Color) {
    val point = FloatArray(2)
    val stroke = Stroke(width = 0.7.dp.toPx())
    fun line(points: List<Pair<Float, Float>>) {
        val path = Path()
        var isDrawing = false
        for ((lon, lat) in points) {
            val isVisible: Boolean = projector.project(lon.toRadians(), lat.toRadians(), point)
            if (!isVisible) {
                isDrawing = false
                continue
            }
            if (isDrawing) path.lineTo(point[0], point[1]) else path.moveTo(point[0], point[1])
            isDrawing = true
        }
        drawPath(path, color, style = stroke)
    }
    for (lat in -60..60 step GRID_STEP) line((-180..180 step 3).map { it.toFloat() to lat.toFloat() })
    for (lon in -180 until 180 step GRID_STEP) line((-90..90 step 3).map { lon.toFloat() to it.toFloat() })
}

/** Reads the bundled land outlines: one ring per line, "lon lat lon lat ..." in degrees. */
private fun loadLand(context: Context): List<LandRing> = try {
    context.assets.open(LAND_ASSET).bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
            val numbers: List<Float> = line.trim().split(' ').map { it.toFloat() }
            val count: Int = numbers.size / 2
            LandRing(
                lon = FloatArray(count) { numbers[it * 2].toRadians() },
                lat = FloatArray(count) { numbers[it * 2 + 1].toRadians() },
            )
        }.toList()
    }
} catch (e: Exception) {
    emptyList()
}

/** Target longitude adjusted so the globe turns the short way round. */
private fun nearestLongitude(from: Float, to: Float): Float = to + 360f * ((from - to) / 360f).roundToInt()

private fun zoomOf(scale: Float): Double = BASE_ZOOM + ln(scale.toDouble()) / ln(2.0)

private fun Float.toRadians(): Float = this * (PI.toFloat() / 180f)

private fun Double.toRadians(): Float = (this * PI / 180.0).toFloat()
