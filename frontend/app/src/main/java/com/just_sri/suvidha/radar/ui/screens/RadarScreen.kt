package com.just_sri.suvidha.radar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.radar.AppViewModel
import com.just_sri.suvidha.radar.CATEGORIES
import com.just_sri.suvidha.radar.RADAR_PROTOTYPE
import com.just_sri.suvidha.radar.REGIONS
import com.just_sri.suvidha.radar.RadarState
import com.just_sri.suvidha.radar.data.Incident
import com.just_sri.suvidha.radar.ui.components.EntryCard
import com.just_sri.suvidha.radar.ui.components.IncidentMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.just_sri.suvidha.radar.ui.components.SelectChip
import com.just_sri.suvidha.radar.ui.components.Tag
import com.just_sri.suvidha.radar.ui.theme.LocalSuvidhaColors

private const val WORLD_VIEW_MAX: Double = 4.0
private const val REGIONAL_VIEW_MAX: Double = 6.0

@Composable
fun RadarScreen(state: RadarState, model: AppViewModel) {
    if (RADAR_PROTOTYPE) {
        PrototypeRadarScreen(stories = state.incidents, onOpenStory = model::openIncident, onOpenSettings = model::openSettings)
        return
    }
    val colors = LocalSuvidhaColors.current
    val visible: List<Incident> = state.visible
    var isMapFullScreen: Boolean by rememberSaveable { mutableStateOf(false) }
    if (isMapFullScreen) {
        FullScreenMap(state, visible, model, onExit = { isMapFullScreen = false })
        return
    }
    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding()) {
        RadarHeader(state, model)
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp).padding(bottom = 8.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MapCard(state, visible, model, onFullScreen = { isMapFullScreen = true }, modifier = Modifier.fillMaxWidth().weight(0.42f))
            StreamList(state, visible, model, Modifier.fillMaxWidth().weight(0.58f))
        }
    }
}

/**
 * Map over the whole screen, drawn edge to edge. Its controls stay inside the safe area
 * (below the status bar, above the navigation bar) so nothing sits under system bars or the notch.
 */
@Composable
private fun FullScreenMap(state: RadarState, visible: List<Incident>, model: AppViewModel, onExit: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    val isGlobe: Boolean by model.isGlobe.collectAsStateWithLifecycle()
    var zoom: Double by remember { mutableDoubleStateOf(state.fly.zoom) }
    BackHandler(onBack = onExit)
    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        IncidentMap(
            isGlobe = isGlobe,
            incidents = visible,
            selectedId = state.selectedId,
            fly = state.fly,
            onSelect = model::openIncident,
            onZoomChanged = { zoom = it },
            modifier = Modifier.fillMaxSize(),
        )
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
            MapRoundButton(Icons.Outlined.FullscreenExit, "Exit full screen", onExit, Modifier.align(Alignment.TopStart))
            ZoomBadge(zoom, Modifier.align(Alignment.TopEnd))
            Text(
                text = "${visible.size} stories",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(colors.cardSurface.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MapRoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalSuvidhaColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardSurface.copy(alpha = 0.94f))
            .clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = label, tint = colors.green, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ZoomBadge(zoom: Double, modifier: Modifier) {
    val colors = LocalSuvidhaColors.current
    val band: String = when {
        zoom < WORLD_VIEW_MAX -> "WORLD VIEW"
        zoom <= REGIONAL_VIEW_MAX -> "REGIONAL VIEW"
        else -> "LOCAL VIEW"
    }
    Text(
        text = "ZOOM ${zoom.toInt()} · $band",
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = colors.green,
        modifier = modifier
            .background(colors.cardSurface.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun RadarHeader(state: RadarState, model: AppViewModel) {
    val colors = LocalSuvidhaColors.current
    var isSearchOpen: Boolean by rememberSaveable { mutableStateOf(state.query.isNotEmpty()) }
    BackHandler(enabled = isSearchOpen) {
        model.setQuery("")
        isSearchOpen = false
    }
    Column(modifier = Modifier.padding(horizontal = 12.dp).padding(top = 4.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // One header row: the title, or (while searching) the search field in its place.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 56.dp)) {
            if (isSearchOpen) {
                HeaderSearchField(
                    query = state.query,
                    onChange = model::setQuery,
                    onClose = {
                        model.setQuery("")
                        isSearchOpen = false
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(colors.green),
                ) {
                    Icon(Icons.Filled.Radar, contentDescription = null, tint = colors.onGreen, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    Text("Suvidha", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.ink)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("WORLD NEWS RADAR", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.green)
                        Tag(if (state.isLive) "Live" else "Sample", if (state.isLive) colors.green else colors.accent)
                    }
                }
                IconButton(onClick = { isSearchOpen = true }) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = colors.muted)
                }
            }
            IconButton(onClick = model::openSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = colors.muted)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(CATEGORIES) { (id, label) ->
                SelectChip(label, isSelected = state.category == id, onClick = { model.setCategory(id) })
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(REGIONS) { region ->
                SelectChip(region.label, isSelected = state.regionId == region.id, onClick = { model.flyToRegion(region) })
            }
        }
    }
}

/** Search field that sits in the header row in place of the title. Focuses itself when opened. */
@Composable
internal fun HeaderSearchField(query: String, onChange: (String) -> Unit, onClose: () -> Unit, modifier: Modifier, placeholder: String = "Search stories") {
    val colors = LocalSuvidhaColors.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = colors.muted) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close search", tint = colors.muted)
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.green,
            unfocusedBorderColor = colors.borderLight,
            focusedContainerColor = colors.cardSurface,
            unfocusedContainerColor = colors.cardSurface,
        ),
        modifier = modifier.focusRequester(focus),
    )
}

@Composable
private fun MapCard(state: RadarState, visible: List<Incident>, model: AppViewModel, onFullScreen: () -> Unit, modifier: Modifier) {
    val colors = LocalSuvidhaColors.current
    val isGlobe: Boolean by model.isGlobe.collectAsStateWithLifecycle()
    var zoom: Double by remember { mutableDoubleStateOf(state.fly.zoom) }
    Surface(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, colors.borderLight), modifier = modifier) {
        Box {
            IncidentMap(
                isGlobe = isGlobe,
                incidents = visible,
                selectedId = state.selectedId,
                fly = state.fly,
                onSelect = model::openIncident,
                onZoomChanged = { zoom = it },
                modifier = Modifier.fillMaxSize(),
            )
            ZoomBadge(zoom, Modifier.align(Alignment.TopEnd).padding(10.dp))
            MapRoundButton(Icons.Outlined.Fullscreen, "Full screen map", onFullScreen, Modifier.align(Alignment.BottomEnd).padding(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamList(state: RadarState, visible: List<Incident>, model: AppViewModel, modifier: Modifier) {
    val colors = LocalSuvidhaColors.current
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            Text("News Stream", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.ink)
            Spacer(Modifier.size(8.dp))
            Tag("${visible.size} stories", colors.green)
        }
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = model::refresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(pullState, state.isLoading, Modifier.align(Alignment.TopCenter), containerColor = colors.cardSurface, color = colors.green)
            },
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp), modifier = Modifier.fillMaxSize()) {
                if (visible.isEmpty()) {
                    item {
                        Text(if (state.isLoading) "Loading stories…" else "No stories match.", color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 32.dp).fillMaxWidth())
                    }
                }
                items(visible, key = { it.id }) { incident ->
                    StoryCard(incident, isSelected = incident.id == state.selectedId, onClick = { model.openIncident(incident) })
                }
            }
        }
    }
}

@Composable
internal fun StoryCard(incident: Incident, isSelected: Boolean, onClick: () -> Unit, isRadarEntry: Boolean = false) {
    val colors = LocalSuvidhaColors.current
    EntryCard(onClick = onClick, isHighlighted = isSelected) {
        if (isRadarEntry) Tag("Sample story", colors.ink)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(incident.category.uppercase(), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.green, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = colors.muted, modifier = Modifier.size(11.dp))
            Text(" ${incident.date}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.muted)
        }
        Text(incident.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(incident.sources.firstOrNull()?.publisher ?: incident.locationName, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = colors.green, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("View sources →", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.muted)
        }
    }
}
