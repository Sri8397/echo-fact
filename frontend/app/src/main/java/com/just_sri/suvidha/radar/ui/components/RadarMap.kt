package com.just_sri.suvidha.radar.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.just_sri.suvidha.radar.BuildConfig
import com.just_sri.suvidha.radar.FlyTarget
import com.just_sri.suvidha.radar.data.Incident
import com.just_sri.suvidha.radar.ui.theme.LocalSuvidhaColors
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.ln
import kotlin.math.max

private const val MIN_ZOOM: Double = 2.0
private const val MAX_ZOOM: Double = 12.0
private const val FLY_MILLIS: Long = 1200
private const val MARKER_DP: Int = 22
private const val SELECTED_MARKER_DP: Int = 30
private const val TILE_PX: Double = 256.0

/** Backwards-compatible incident API; the shared map itself works with stable marker ids. */
@Composable
fun RadarMap(
    incidents: List<Incident>,
    selectedId: String?,
    fly: FlyTarget,
    onSelect: (Incident) -> Unit,
    onZoomChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val markers = remember(incidents) { incidents.map { GlobeMarker(it.id, it.lat, it.lng) } }
    val currentIncidents = rememberUpdatedState(incidents)
    val currentOnSelect = rememberUpdatedState(onSelect)
    RadarMarkerMapContent(
        markers = markers,
        selectedId = selectedId,
        fly = fly,
        onSelect = { id -> currentIncidents.value.firstOrNull { it.id == id }?.let(currentOnSelect.value) },
        onZoomChanged = onZoomChanged,
        modifier = modifier,
        markerTitles = remember(incidents) { incidents.associate { it.id to it.title } },
    )
}

/** Shared osmdroid map for globe-style marker data and other id-based callers. */
@Composable
internal fun RadarMarkerMap(
    markers: List<GlobeMarker>,
    selectedId: String?,
    fly: FlyTarget,
    onSelect: (String) -> Unit,
    onZoomChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) = RadarMarkerMapContent(markers, selectedId, fly, onSelect, onZoomChanged, modifier)

@Composable
private fun RadarMarkerMapContent(
    markers: List<GlobeMarker>,
    selectedId: String?,
    fly: FlyTarget,
    onSelect: (String) -> Unit,
    onZoomChanged: (Double) -> Unit,
    modifier: Modifier,
    markerTitles: Map<String, String> = emptyMap(),
) {
    val context: Context = LocalContext.current
    val colors = LocalSuvidhaColors.current
    val density = LocalDensity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnZoom = rememberUpdatedState(onZoomChanged)
    val currentOnSelect = rememberUpdatedState(onSelect)
    val darkTileFilter = remember {
        ColorMatrixColorFilter(
            floatArrayOf(
                // Invert luminance into slate tones: pale land becomes navy, dark labels become bright.
                -0.136064f, -0.457728f, -0.046208f, 0f, 170f,
                -0.140316f, -0.472032f, -0.047652f, 0f, 184f,
                -0.142442f, -0.479184f, -0.048374f, 0f, 205f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }
    val mapView: MapView = remember(context) { createMap(context) { currentOnZoom.value(it) } }

    DisposableEffect(mapView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(fly.sequence) {
        mapView.controller.animateTo(GeoPoint(fly.lat, fly.lng), fly.zoom, FLY_MILLIS)
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            // TilesOverlay filters only the base map; marker overlays retain their palette colors.
            view.overlayManager.tilesOverlay.setColorFilter(if (colors.isDark) darkTileFilter else null)
            view.overlays.removeAll { it is Marker }
            for (mapMarker in markers) {
                val isSelected = mapMarker.id == selectedId
                val fill = mapMarker.color ?: colors.green
                val marker = Marker(view).apply {
                    position = GeoPoint(mapMarker.lat, mapMarker.lng)
                    title = markerTitles[mapMarker.id] ?: mapMarker.id
                    icon = beacon(context, fill, colors.onGreen, colors.accent, isSelected, density.density)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ ->
                        currentOnSelect.value(mapMarker.id)
                        true
                    }
                }
                view.overlays.add(marker)
            }
            view.invalidate()
        },
    )
}

private fun createMap(context: Context, onZoomChanged: (Double) -> Unit): MapView {
    Configuration.getInstance().userAgentValue = "SuvidhaRadar/${BuildConfig.VERSION_NAME}"
    Configuration.getInstance().osmdroidBasePath = context.cacheDir.resolve("osmdroid")
    Configuration.getInstance().osmdroidTileCache = context.cacheDir.resolve("osmdroid/tiles")
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        minZoomLevel = MIN_ZOOM
        maxZoomLevel = MAX_ZOOM
        isVerticalMapRepetitionEnabled = false
        controller.setZoom(MIN_ZOOM)
        controller.setCenter(GeoPoint(20.0, 0.0))
        setScrollableAreaLimitLatitude(MapView.getTileSystem().maxLatitude, MapView.getTileSystem().minLatitude, 0)
        // Never zoom out past the point where the world is shorter than the view (no empty bands).
        addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val map: MapView = view as MapView
            if (map.height <= 0) return@addOnLayoutChangeListener
            val fitZoom: Double = ln(map.height.toDouble() / TILE_PX) / ln(2.0)
            val floor: Double = max(MIN_ZOOM, fitZoom)
            if (map.minZoomLevel != floor) {
                map.minZoomLevel = floor
                if (map.zoomLevelDouble < floor) map.controller.setZoom(floor)
            }
        }
        addMapListener(object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean = false
            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                onZoomChanged(zoomLevelDouble)
                return false
            }
        })
    }
}

/** Filled marker dot with a light ring; selected and per-marker colors remain distinct. */
private fun beacon(context: Context, fill: Color, ring: Color, accent: Color, isSelected: Boolean, density: Float): Drawable {
    val sizePx = ((if (isSelected) SELECTED_MARKER_DP else MARKER_DP) * density).toInt().coerceAtLeast(1)
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val radius = sizePx / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = (if (isSelected) accent else ring).toArgb()
    canvas.drawCircle(radius, radius, radius, paint)
    paint.color = fill.toArgb()
    canvas.drawCircle(radius, radius, radius * 0.78f, paint)
    paint.color = ring.toArgb()
    canvas.drawCircle(radius, radius, radius * 0.25f, paint)
    return BitmapDrawable(context.resources, bitmap)
}
