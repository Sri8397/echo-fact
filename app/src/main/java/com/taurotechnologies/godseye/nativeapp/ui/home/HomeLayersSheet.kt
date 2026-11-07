package com.taurotechnologies.godseye.nativeapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taurotechnologies.godseye.nativeapp.data.GLOBE_PRESET_OPTIONS
import com.taurotechnologies.godseye.nativeapp.data.MAP_STYLE_CHIP_OPTIONS
import com.taurotechnologies.godseye.nativeapp.data.MAP_THEME_OPTIONS
import com.taurotechnologies.godseye.nativeapp.data.RadarFeedFilter
import com.taurotechnologies.godseye.nativeapp.ui.components.SelectChip
import com.taurotechnologies.godseye.nativeapp.ui.components.SectionLabel
import com.taurotechnologies.godseye.nativeapp.ui.components.Tag
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors

/** Inputs for the layers sheet. Everything here is state the home screen already owns or persists. */
data class HomeLayersState(
    val filter: RadarFeedFilter,
    val counts: Map<RadarFeedFilter, Int>,
    val pinned: List<RadarFeedFilter>,
    val currentScreenOnly: Boolean,
    val mapTheme: String,
    /** Imagery style id, see MAP_PROVIDER_OPTIONS. */
    val mapStyle: String = "auto",
    val globePreset: String,
)

/**
 * "Map type / Map details" sheet, opened from the layers button or the More chip. Layers are grouped; a row
 * selects that layer on the map, and its star pins it to the chip row. The current-screen filter lives here too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLayersSheet(
    state: HomeLayersState,
    onCurrentScreenOnly: (Boolean) -> Unit,
    onMapTheme: (String) -> Unit,
    onGlobePreset: (String) -> Unit,
    onMapStyle: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.cardSurface,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Map type", color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            SectionLabel("Imagery", colors.muted)
            ChipRow {
                for ((value, label) in MAP_STYLE_CHIP_OPTIONS) {
                    SelectChip(label, state.mapStyle == value, onClick = { onMapStyle(value) })
                }
            }
            SectionLabel("Map style", colors.muted)
            ChipRow {
                for ((value, label) in MAP_THEME_OPTIONS) {
                    SelectChip(label, state.mapTheme == value, onClick = { onMapTheme(value) })
                }
            }
            SectionLabel("Globe", colors.muted)
            ChipRow {
                for ((value, label) in GLOBE_PRESET_OPTIONS) {
                    SelectChip(label, state.globePreset == value, onClick = { onGlobePreset(value) })
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Current screen only", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Limit the stream to what the map shows.", color = colors.muted, fontSize = 12.sp)
                }
                Switch(checked = state.currentScreenOnly, onCheckedChange = onCurrentScreenOnly)
            }
        }
    }
}

/** The layer list (Pinned, All, then each group). Shown on the Layers screen. */
@Composable
fun HomeLayerList(state: HomeLayersState, onSelectFilter: (RadarFeedFilter) -> Unit, onTogglePin: (RadarFeedFilter) -> Unit) {
    val colors = LocalSuvidhaColors.current
    // Pinned layers are listed first, in pin order, and left out of their usual group so each shows once.
    val pinned = state.pinned.filter { it != RadarFeedFilter.ALL }
    val layerRow: @Composable (RadarFeedFilter) -> Unit = { layer ->
        LayerRow(
            label = layer.name.lowercase().replaceFirstChar { it.uppercase() },
            count = state.counts[layer] ?: 0,
            color = layer.color(colors) ?: colors.ink,
            selected = state.filter == layer,
            pinned = layer in state.pinned,
            onSelect = { onSelectFilter(layer) },
            onTogglePin = { onTogglePin(layer) },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (pinned.isNotEmpty()) {
            SectionLabel("Pinned", colors.muted)
            for (layer in pinned) layerRow(layer)
            SectionLabel("All", colors.muted)
        }
        LayerRow(
            label = "All layers",
            count = state.counts[RadarFeedFilter.ALL] ?: 0,
            color = colors.ink,
            selected = state.filter == RadarFeedFilter.ALL,
            pinned = null,
            onSelect = { onSelectFilter(RadarFeedFilter.ALL) },
            onTogglePin = null,
        )
        for (group in LayerGroup.entries) {
            val rest = group.layers.filter { it !in pinned }
            if (rest.isEmpty()) continue
            SectionLabel(group.title, colors.muted)
            for (layer in rest) layerRow(layer)
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        content()
    }
}

@Composable
private fun LayerRow(
    label: String,
    count: Int,
    color: Color,
    selected: Boolean,
    pinned: Boolean?,
    onSelect: () -> Unit,
    onTogglePin: (() -> Unit)?,
) {
    val colors = LocalSuvidhaColors.current
    Row(
        Modifier.fillMaxWidth().height(44.dp).clickable(onClick = onSelect).padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color = if (selected) colors.green else colors.ink,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Tag(count.toString(), if (count > 0) colors.green else colors.muted)
        if (onTogglePin != null && pinned != null) {
            IconButton(onClick = onTogglePin, modifier = Modifier.size(40.dp).semantics {
                contentDescription = if (pinned) "Unpin $label from the map bar" else "Pin $label to the map bar"
            }) {
                Icon(
                    if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = null,
                    tint = if (pinned) colors.green else colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
