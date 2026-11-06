package com.just_sri.suvidha.radar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.radar.FlyTarget
import com.just_sri.suvidha.radar.data.MockRadarApi
import com.just_sri.suvidha.radar.data.RadarLayer
import com.just_sri.suvidha.radar.data.RadarSignal
import com.just_sri.suvidha.radar.ui.components.EntryCard
import com.just_sri.suvidha.radar.ui.components.GlobeMap
import com.just_sri.suvidha.radar.ui.components.GlobeMarker
import com.just_sri.suvidha.radar.ui.components.Tag
import com.just_sri.suvidha.radar.ui.theme.LocalSuvidhaColors
import com.just_sri.suvidha.radar.ui.theme.SuvidhaColors

@Composable
fun SignalRadarScreen(expanded: Boolean, onExpandedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSuvidhaColors.current
    val signals = remember { MockRadarApi.signals().sortedByDescending { it.observedAt } }
    var layer by rememberSaveable { mutableStateOf<RadarLayer?>(null) }
    var activeId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraRequest by rememberSaveable { mutableStateOf(0L) }
    val visible = remember(signals, layer) { signals.filter { layer == null || it.layer == layer } }
    val active = visible.firstOrNull { it.id == activeId }
    val detail = visible.firstOrNull { it.id == detailId }
    val markers = remember(visible, colors) {
        visible.map { GlobeMarker(it.id, it.lat, it.lng, it.layer.color(colors)) }
    }
    val fly = FlyTarget(active?.lat ?: 20.0, active?.lng ?: 0.0, if (active == null) 2.0 else 2.6, cameraRequest)
    val showSignal: (String) -> Unit = { id ->
        activeId = id
        detailId = id
        cameraRequest++
    }
    val resetCamera: () -> Unit = {
        activeId = null
        cameraRequest++
    }

    // Move the same globe between layouts so manual pan and zoom survive fullscreen changes.
    val globeMap = remember {
        movableContentOf<List<GlobeMarker>, String?, FlyTarget, (String) -> Unit> { points, selection, target, onSelect ->
            GlobeMap(points, selection, target, onSelect, onZoomChanged = {}, modifier = Modifier.fillMaxSize())
        }
    }
    val selectLayer: (RadarLayer?) -> Unit = { selected ->
        if (layer != selected) {
            layer = selected
            resetCamera()
        }
    }
    BackHandler(enabled = expanded) { onExpandedChange(false) }
    if (expanded) {
        Box(modifier.fillMaxSize()) {
            globeMap(markers, activeId, fly, showSignal)
            Box(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
                GlobeControls(true, { onExpandedChange(false) }, resetCamera, Modifier.align(Alignment.TopEnd))
                Surface(shape = RoundedCornerShape(10.dp), color = colors.cardSurface, modifier = Modifier.align(Alignment.BottomStart)) {
                    Text("${visible.size} demo signals", color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }
    } else {
        Column(
            modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { FilterChip(selected = layer == null, onClick = { selectLayer(null) }, label = { Text("All", fontSize = 12.sp) }) }
                items(RadarLayer.entries) { entry ->
                    FilterChip(
                        selected = layer == entry,
                        onClick = { selectLayer(entry) },
                        label = { Text(entry.label, fontSize = 12.sp) },
                        leadingIcon = {
                            Box(Modifier.size(8.dp).background(entry.color(colors), CircleShape))
                        },
                    )
                }
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val sideBySide = maxWidth >= 600.dp || maxHeight < 300.dp && maxWidth > maxHeight
                val globe: @Composable (Modifier) -> Unit = { mapModifier ->
                    Surface(
                        modifier = mapModifier,
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceMuted,
                        border = BorderStroke(1.dp, colors.borderLight),
                    ) {
                        Box {
                            globeMap(markers, activeId, fly, showSignal)
                            Text("3D GLOBE", color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
                            GlobeControls(false, { onExpandedChange(true) }, resetCamera, Modifier.align(Alignment.TopEnd).padding(4.dp))
                            Text("Drag to rotate  ·  Pinch to zoom", color = colors.muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp))
                        }
                    }
                }
                if (sideBySide) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        globe(Modifier.weight(1f).fillMaxSize())
                        SignalList(visible, activeId, showSignal, Modifier.weight(1f).fillMaxSize())
                    }
                } else {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        globe(Modifier.weight(1.2f).fillMaxWidth())
                        SignalList(visible, activeId, showSignal, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }
    }
    if (detail != null) SignalDetails(detail, onClose = { detailId = null })
}

@Composable
private fun GlobeControls(expanded: Boolean, onExpand: () -> Unit, onReset: () -> Unit, modifier: Modifier) {
    val colors = LocalSuvidhaColors.current
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = colors.cardSurface.copy(alpha = 0.9f)) {
        Row {
            IconButton(onClick = onReset) {
                Icon(Icons.Outlined.CenterFocusStrong, contentDescription = "Reset globe", tint = colors.ink)
            }
            IconButton(onClick = onExpand) {
                Icon(
                    if (expanded) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                    contentDescription = if (expanded) "Exit full screen globe" else "Full screen globe",
                    tint = colors.ink,
                )
            }
        }
    }
}

@Composable
private fun SignalList(signals: List<RadarSignal>, activeId: String?, onSelect: (String) -> Unit, modifier: Modifier) {
    val colors = LocalSuvidhaColors.current
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text("Signal stream", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Tag("${signals.size} demo", colors.accent)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(signals, key = { it.id }) { signal ->
                EntryCard(isHighlighted = signal.id == activeId, onClick = { onSelect(signal.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag(signal.layer.label, signal.layer.color(colors))
                        Text(signal.severity, color = colors.muted, fontSize = 12.sp)
                    }
                    Text(signal.title, color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(signal.locationName, color = colors.muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignalDetails(signal: RadarSignal, onClose: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onClose, containerColor = colors.cardSurface) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag("Simulated", colors.accent)
                Tag(signal.layer.label, signal.layer.color(colors))
            }
            Text(signal.title, color = colors.ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(signal.locationName, color = colors.ink, fontSize = 15.sp)
            Text("Demo time  ${signal.observedAt}  ·  ${signal.severity}", color = colors.muted, fontSize = 13.sp)
            Text(signal.summary, color = colors.ink, fontSize = 15.sp)
            Text("Provider reference", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { uriHandler.openUri(signal.sourceUrl) }) {
                Text(signal.sourceName)
            }
        }
    }
}

private fun RadarLayer.color(colors: SuvidhaColors): Color = when (this) {
    RadarLayer.SEISMIC -> colors.danger
    RadarLayer.WEATHER -> colors.confirm
    RadarLayer.MARITIME -> colors.green
}
