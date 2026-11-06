package com.just_sri.suvidha.radar.ui.components

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.just_sri.suvidha.radar.FlyTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

private const val EARTH_ASSET = "earth_night.jpg"
private const val MAX_SCALE = 16f
private const val FLY_MILLIS = 1100
private const val TAP_RADIUS_DP = 28
private const val BASE_ZOOM = 2.0
private const val MAP_BACKGROUND = 0xFF030A10

/** Flat, wrapped equirectangular Earth map with Compose-rendered incident markers. */
@Composable
fun FlatRadarMap(
    markers: List<GlobeMarker>,
    selectedId: String?,
    fly: FlyTarget,
    onSelect: (String) -> Unit,
    onZoomChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val density = LocalDensity.current
    val centerLon = remember { Animatable(0f) }
    val centerLat = remember { Animatable(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var viewportWidth by remember { mutableFloatStateOf(0f) }
    var viewportHeight by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val currentMarkers by rememberUpdatedState(markers)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnZoom by rememberUpdatedState(onZoomChanged)
    val tapRadiusPx = with(density) { TAP_RADIUS_DP.dp.toPx() }
    val bitmapState = remember(context) { androidx.compose.runtime.mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(context) {
        bitmapState.value = withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.assets.open(EARTH_ASSET).use { BitmapFactory.decodeStream(it, null, bounds) }
                var sampleSize = 1
                while (bounds.outWidth / sampleSize > 1800) sampleSize *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                context.assets.open(EARTH_ASSET).use { BitmapFactory.decodeStream(it, null, options) }
            } catch (error: Exception) {
                Log.w("FlatRadarMap", "Earth map texture unavailable", error)
                null
            }
        }
    }

    LaunchedEffect(Unit) { currentOnZoom(BASE_ZOOM) }
    LaunchedEffect(fly.sequence, viewportWidth, viewportHeight) {
        if (fly.sequence == 0L) return@LaunchedEffect
        val targetScale = 2.0.pow(fly.zoom - BASE_ZOOM).toFloat().coerceIn(1f, MAX_SCALE)
        val baseWidth = min(viewportWidth, viewportHeight * 2f)
        val mapHeight = baseWidth * targetScale * 0.5f
        val maxLat = if (mapHeight <= viewportHeight || mapHeight <= 0f) 0f else ((mapHeight - viewportHeight) * 90f / mapHeight).coerceIn(0f, 90f)
        launch { centerLon.animateTo(nearestLongitude(centerLon.value, fly.lng.toFloat()), tween(FLY_MILLIS)) }
        launch { centerLat.animateTo(fly.lat.toFloat().coerceIn(-maxLat, maxLat), tween(FLY_MILLIS)) }
        animate(scale, targetScale, animationSpec = tween(FLY_MILLIS)) { value, _ -> scale = value }
        currentOnZoom(zoomOf(scale))
    }

    Canvas(
        modifier = modifier
            .background(Color(MAP_BACKGROUND))
            .onSizeChanged { viewportWidth = it.width.toFloat(); viewportHeight = it.height.toFloat() }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val baseWidth = min(size.width.toFloat(), size.height.toFloat() * 2f).coerceAtLeast(1f)
                    val oldWidth = baseWidth * scale
                    val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                    val newWidth = baseWidth * newScale
                    val nextLon = longitudeAfterGesture(centerLon.value, centroid.x - size.width / 2f, pan.x, oldWidth, newWidth)
                    val nextLat = centerLat.value + pan.y * 180f / (baseWidth * 0.5f * newScale)
                    scale = newScale
                    val mapHeight = baseWidth * 0.5f * newScale
                    val maxLat = if (mapHeight <= size.height) 0f else ((mapHeight - size.height) * 90f / mapHeight).coerceIn(0f, 90f)
                    scope.launch {
                        centerLon.snapTo(wrapLongitude(nextLon))
                        centerLat.snapTo(nextLat.coerceIn(-maxLat, maxLat))
                    }
                    currentOnZoom(zoomOf(scale))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val baseWidth = min(size.width.toFloat(), size.height.toFloat() * 2f).coerceAtLeast(1f)
                    val mapWidth = baseWidth * scale
                    val mapHeight = mapWidth / 2f
                    val mapY = size.height / 2f + centerLat.value / 180f * mapHeight
                    val tapMapX = tap.x
                    val hit = currentMarkers.mapNotNull { marker ->
                        val markerX = size.width / 2f + (marker.lng.toFloat() - centerLon.value) / 360f * mapWidth
                        val wrappedX = markerX + mapWidth * kotlin.math.round((tapMapX - markerX) / mapWidth)
                        val markerY = mapY - marker.lat.toFloat() / 180f * mapHeight
                        val distance = hypot(wrappedX - tap.x, markerY - tap.y)
                        if (distance <= tapRadiusPx) marker to distance else null
                    }.minByOrNull { it.second }?.first
                    if (hit != null) currentOnSelect(hit.id)
                }
            },
    ) {
        val baseWidth = min(size.width, size.height * 2f).coerceAtLeast(1f)
        val mapWidth = baseWidth * scale
        val mapHeight = mapWidth / 2f
        val centerX = size.width / 2f
        val mapY = size.height / 2f + centerLat.value / 180f * mapHeight
        val left = centerX - mapWidth / 2f - centerLon.value / 360f * mapWidth
        val firstCopy = floor((-left) / mapWidth).toInt() - 1
        val lastCopy = ceil((size.width - left) / mapWidth).toInt() + 1
        val image = bitmapState.value?.asImageBitmap()
        if (image != null) {
            for (copy in firstCopy..lastCopy) {
                val x = left + copy * mapWidth
                drawImage(
                    image = image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(image.width, image.height),
                    dstOffset = IntOffset(x.toInt(), (mapY - mapHeight / 2f).toInt()),
                    dstSize = IntSize(mapWidth.toInt().coerceAtLeast(1), mapHeight.toInt().coerceAtLeast(1)),
                )
            }
        }

        for (marker in markers) {
            val markerY = mapY - marker.lat.toFloat().coerceIn(-90f, 90f) / 180f * mapHeight
            val rawX = centerX + (marker.lng.toFloat() - centerLon.value) / 360f * mapWidth
            val firstVisibleCopy = floor((-rawX) / mapWidth).toInt() - 1
            val lastVisibleCopy = ceil((size.width - rawX) / mapWidth).toInt() + 1
            for (copy in firstVisibleCopy..lastVisibleCopy) {
                val x = rawX + copy * mapWidth
                if (x < -40f || x > size.width + 40f || markerY < -40f || markerY > size.height + 40f) continue
                val spot = Offset(x, markerY)
                val isSelected = marker.id == selectedId
                val baseTint = marker.color ?: Color(0xFF51E6BA)
                val tint = if (baseTint.luminance() < 0.3f) lerp(baseTint, Color.White, 0.35f) else baseTint
                val radius = (if (isSelected) 9f else 6f).dp.toPx()
                drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.5f), Color.Transparent), spot, radius * 2.8f), radius * 2.8f, spot)
                drawCircle(Color(MAP_BACKGROUND).copy(alpha = 0.85f), radius, spot)
                drawCircle(tint, radius, spot, style = Stroke(2.dp.toPx()))
                if (isSelected) drawCircle(Color.White, 2.dp.toPx(), spot)
            }
        }
    }
}

private fun nearestLongitude(from: Float, to: Float): Float = to + 360f * kotlin.math.round((from - to) / 360f)
private fun wrapLongitude(value: Float): Float = ((value + 180f) % 360f + 360f) % 360f - 180f
private fun zoomOf(scale: Float): Double = BASE_ZOOM + ln(scale.toDouble()) / ln(2.0)

/** Keep the touched longitude under the fingers while the image follows their horizontal pan. */
internal fun longitudeAfterGesture(center: Float, focalOffset: Float, pan: Float, oldWidth: Float, newWidth: Float): Float {
    val focalLongitude = center + focalOffset * 360f / oldWidth
    return focalLongitude - (focalOffset + pan) * 360f / newWidth
}
