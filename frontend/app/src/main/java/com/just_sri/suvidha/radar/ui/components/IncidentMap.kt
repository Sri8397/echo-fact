package com.just_sri.suvidha.radar.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.just_sri.suvidha.radar.FlyTarget
import com.just_sri.suvidha.radar.data.Incident

/** The one map every screen uses: the 3D globe or the flat 2D map, from Settings → 3D globe. */
@Composable
fun IncidentMap(
    isGlobe: Boolean,
    incidents: List<Incident>,
    selectedId: String?,
    fly: FlyTarget,
    onSelect: (Incident) -> Unit,
    onZoomChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isGlobe) {
        GlobeMap(incidents, selectedId, fly, onSelect, onZoomChanged, modifier)
    } else {
        RadarMap(incidents, selectedId, fly, onSelect, onZoomChanged, modifier)
    }
}
