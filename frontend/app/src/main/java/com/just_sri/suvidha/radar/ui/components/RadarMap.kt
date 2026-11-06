package com.just_sri.suvidha.radar.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
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

/**
 * OpenStreetMap view (osmdroid, no API key). One marker per incident; the selected one is larger.
 * Re-flies whenever [fly] changes sequence, and reports taps through [onSelect].
 */
@Composable
fun RadarMap(
    incidents: List<Incident>,
    selectedId: String?,
    fly: FlyTarget,
    onSelect: (Incident) -> Unit,
    onZoomChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context: Context = LocalContext.current
    val colors = LocalSuvidhaColors.current
    val mapView: MapView = remember { createMap(context, onZoomChanged) }
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
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
            view.overlays.removeAll { it is Marker }
            for (incident in incidents) {
                val isSelected: Boolean = incident.id == selectedId
                val marker = Marker(view).apply {
                    position = GeoPoint(incident.lat, incident.lng)
                    title = incident.title
                    icon = beacon(context, colors.green, colors.onGreen, colors.accent, isSelected)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ ->
                        onSelect(incident)
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

/** Filled brand-green dot with a light ring; the selected marker is larger with an accent ring. */
private fun beacon(context: Context, fill: Color, ring: Color, accent: Color, isSelected: Boolean): Drawable {
    val density: Float = context.resources.displayMetrics.density
    val sizePx: Int = ((if (isSelected) SELECTED_MARKER_DP else MARKER_DP) * density).toInt()
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val radius: Float = sizePx / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = (if (isSelected) accent else ring).toArgb()
    canvas.drawCircle(radius, radius, radius, paint)
    paint.color = fill.toArgb()
    canvas.drawCircle(radius, radius, radius * 0.78f, paint)
    paint.color = ring.toArgb()
    canvas.drawCircle(radius, radius, radius * 0.25f, paint)
    return BitmapDrawable(context.resources, bitmap)
}
