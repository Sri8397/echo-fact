package com.taurotechnologies.godseye.nativeapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Right-edge stack of round map buttons: layers, 2D/3D, map controls, fullscreen, compass and refresh.
 * Only these buttons take touches; the rest of the column is transparent to the map.
 */
@Composable
fun HomeMapButtons(
    onLayers: () -> Unit,
    flatMode: Boolean,
    onToggleMode: () -> Unit,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    loading: Boolean,
    onRefresh: () -> Unit,
    /** Opens the Explore hub. Null hides the button. Uses a grid glyph; the compass owns the Explore icon. */
    onExplore: (() -> Unit)? = null,
    navBarOpen: Boolean = false,
    onToggleNavBar: () -> Unit = {},
    /** Heading in degrees when the compass should show (globe, not facing north or tilted); null hides it. */
    compassHeading: Float? = null,
    onCompass: () -> Unit = {},
    /** False when the full-screen button is placed elsewhere (see [FullscreenMapButton]). */
    showFullscreen: Boolean = true,
    /** An extra button at the bottom of the column (landscape: the switch into zoom mode). */
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSuvidhaColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onExplore != null) {
            MapFloatButton("Explore", onExplore) { Icon(Icons.Outlined.GridView, null, tint = colors.ink, modifier = Modifier.size(20.dp)) }
        }
        MapFloatButton("Map layers", onLayers) { Icon(Icons.Outlined.Layers, null, tint = colors.ink, modifier = Modifier.size(20.dp)) }
        MapFloatButton(if (flatMode) "Switch to 3D globe" else "Switch to 2D map", onToggleMode) {
            Icon(if (flatMode) Icons.Outlined.Public else Icons.Outlined.Map, null, tint = colors.ink, modifier = Modifier.size(20.dp))
        }
        if (!fullscreen) {
            MapFloatButton(if (navBarOpen) "Hide map controls" else "Show map controls", onToggleNavBar, active = navBarOpen) {
                Icon(Icons.Outlined.Tune, null, tint = if (navBarOpen) colors.canvas else colors.ink, modifier = Modifier.size(20.dp))
            }
        }
        if (showFullscreen) FullscreenMapButton(fullscreen, onToggleFullscreen)
        if (compassHeading != null) {
            MapFloatButton("Compass, tap to face north", onCompass) {
                Icon(Icons.Outlined.Explore, null, tint = androidx.compose.ui.graphics.Color(0xFFFF5A4F), modifier = Modifier.size(24.dp).rotate(-compassHeading))
            }
        }
        MapFloatButton("Refresh radar", onRefresh, enabled = !loading) {
            if (loading) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = colors.green, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Filled.Refresh, null, tint = colors.ink, modifier = Modifier.size(20.dp))
            }
        }
        trailing?.invoke()
    }
}

/** Enter or exit the full-screen map. */
@Composable
fun FullscreenMapButton(fullscreen: Boolean, onToggle: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    MapFloatButton(if (fullscreen) "Exit full screen map" else "Full screen map", onToggle) {
        Icon(if (fullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen, null, tint = colors.ink, modifier = Modifier.size(20.dp))
    }
}

/** The map's round 44 dp button. [active] fills it green for an "on" toggle. Shared with the satellite eye. */
@Composable
internal fun MapFloatButton(label: String, onClick: () -> Unit, enabled: Boolean = true, active: Boolean = false, icon: @Composable () -> Unit) {
    val colors = LocalSuvidhaColors.current
    Surface(shape = CircleShape, color = if (active) colors.green else colors.cardSurface, shadowElevation = 4.dp) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp).semantics { contentDescription = label }) {
            icon()
        }
    }
}
