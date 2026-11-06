package com.just_sri.suvidha.radar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.radar.FlyTarget
import com.just_sri.suvidha.radar.REGIONS
import com.just_sri.suvidha.radar.data.Incident
import com.just_sri.suvidha.radar.data.MockRadarApi
import com.just_sri.suvidha.radar.data.RadarEntry
import com.just_sri.suvidha.radar.data.RadarFeedFilter
import com.just_sri.suvidha.radar.data.RadarLayer
import com.just_sri.suvidha.radar.data.RadarSignal
import com.just_sri.suvidha.radar.data.SavedRadarSignals
import com.just_sri.suvidha.radar.data.radarFeed
import com.just_sri.suvidha.radar.data.sourceReferences
import com.just_sri.suvidha.radar.ui.components.EntryCard
import com.just_sri.suvidha.radar.ui.components.GlobeMap
import com.just_sri.suvidha.radar.ui.components.RadarMarkerMap
import com.just_sri.suvidha.radar.ui.components.GlobeMarker
import com.just_sri.suvidha.radar.ui.components.Tag
import com.just_sri.suvidha.radar.ui.theme.LocalSuvidhaColors
import com.just_sri.suvidha.radar.ui.theme.SuvidhaColors

@Composable
internal fun PrototypeRadarScreen(
    stories: List<Incident>,
    onOpenStory: (Incident) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    val context = LocalContext.current
    SavedRadarSignals.initialize(context)
    val signals = remember { MockRadarApi.signals().sortedByDescending { it.observedAt } }
    var filter by rememberSaveable { mutableStateOf(RadarFeedFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var regionId by rememberSaveable { mutableStateOf(REGIONS.first().id) }
    var activeId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var flatMode by rememberSaveable { mutableStateOf(false) }
    val currentFlatMode = rememberUpdatedState(flatMode)
    var expanded by rememberSaveable { mutableStateOf(false) }
    var mapVisible by rememberSaveable { mutableStateOf(true) }
    var cameraRequest by rememberSaveable { mutableStateOf(0L) }
    val visible = remember(stories, signals, filter, query) {
        val term = query.trim()
        radarFeed(stories, signals, filter).filter { entry ->
            when (entry) {
                is RadarEntry.Signal -> entry.signal.title.contains(term, ignoreCase = true) ||
                    entry.signal.locationName.contains(term, ignoreCase = true)
                is RadarEntry.Story -> entry.incident.title.contains(term, ignoreCase = true) ||
                    entry.incident.locationName.contains(term, ignoreCase = true) ||
                    entry.incident.category.contains(term, ignoreCase = true)
            }
        }
    }
    val region = REGIONS.first { it.id == regionId }
    val active = visible.firstOrNull { it.id == activeId }
    val detail = visible.filterIsInstance<RadarEntry.Signal>().firstOrNull { it.id == detailId }
    val markers = remember(visible, colors, flatMode) {
        visible.map {
            val color = when (it) {
                is RadarEntry.Signal -> it.signal.layer.color(colors)
                is RadarEntry.Story -> if (flatMode) colors.ink else Color(0xFFE8F2F5)
            }
            GlobeMarker(it.id, it.lat, it.lng, color)
        }
    }
    val fly = FlyTarget(active?.lat ?: region.lat, active?.lng ?: region.lng, if (active == null) region.zoom else 2.6, cameraRequest)
    val showEntry: (String) -> Unit = { id ->
        val entry = visible.firstOrNull { it.id == id }
        if (entry != null) {
            activeId = entry.id
            cameraRequest++
            when (entry) {
                is RadarEntry.Story -> onOpenStory(entry.incident)
                is RadarEntry.Signal -> detailId = entry.id
            }
        }
    }
    val resetCamera: () -> Unit = {
        regionId = REGIONS.first().id
        activeId = null
        cameraRequest++
    }
    val countLabel = when (filter) {
        RadarFeedFilter.ALL -> "${visible.size} entries"
        RadarFeedFilter.STORIES -> "${visible.size} stories"
        else -> "${visible.size} demo signals"
    }

    // Move the same globe between layouts so manual pan and zoom survive fullscreen changes.
    val globeMap = remember {
        movableContentOf<List<GlobeMarker>, String?, FlyTarget, (String) -> Unit> { points, selection, target, onSelect ->
            if (currentFlatMode.value) {
                RadarMarkerMap(points, selection, target, onSelect, onZoomChanged = {}, modifier = Modifier.fillMaxSize())
            } else {
                GlobeMap(points, selection, target, onSelect, onZoomChanged = {}, modifier = Modifier.fillMaxSize())
            }
        }
    }
    val hideMap: () -> Unit = {
        expanded = false
        mapVisible = false
    }
    BackHandler(enabled = expanded) { expanded = false }
    if (expanded) {
        Box(Modifier.fillMaxSize().background(colors.canvas)) {
            globeMap(markers, activeId, fly, showEntry)
            Box(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
                MapModeSwitch(flatMode, { flatMode = it }, Modifier.align(Alignment.TopStart))
                MapCredit(flatMode, Modifier.align(Alignment.BottomEnd))
                GlobeControls(true, { expanded = false }, resetCamera, hideMap, Modifier.align(Alignment.TopEnd))
                Surface(shape = RoundedCornerShape(10.dp), color = colors.cardSurface, modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(countLabel, color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding()
                .navigationBarsPadding().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PrototypeRadarHeader(query, onQueryChange = { query = it }, onOpenSettings)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(RadarFeedFilter.entries) { entry ->
                    RadarFilterChip(
                        filter = entry,
                        selected = filter == entry,
                        onClick = {
                            if (filter != entry) {
                                filter = entry
                                activeId = null
                            }
                        },
                    )
                }
            }
            if (mapVisible) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(REGIONS, key = { it.id }) { preset ->
                        FilterChip(
                            selected = regionId == preset.id,
                            onClick = {
                                regionId = preset.id
                                activeId = null
                                cameraRequest++
                            },
                            label = { Text(preset.label, fontSize = 12.sp) },
                        )
                    }
                }
            } else {
                TextButton(onClick = { mapVisible = true }) {
                    Icon(Icons.Outlined.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Show map", modifier = Modifier.padding(start = 8.dp))
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
                            globeMap(markers, activeId, fly, showEntry)
                            MapModeSwitch(flatMode, { flatMode = it }, Modifier.align(Alignment.TopStart).padding(8.dp))
                            GlobeControls(false, { expanded = true }, resetCamera, hideMap, Modifier.align(Alignment.TopEnd).padding(4.dp))
                            MapCredit(flatMode, Modifier.align(Alignment.BottomEnd).padding(6.dp))
                        }
                    }
                }
                if (!mapVisible) {
                    RadarFeedList(visible, activeId, countLabel, showEntry, Modifier.fillMaxSize())
                } else if (sideBySide) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        globe(Modifier.weight(1f).fillMaxSize())
                        RadarFeedList(visible, activeId, countLabel, showEntry, Modifier.weight(1f).fillMaxSize())
                    }
                } else {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        globe(Modifier.weight(1.2f).fillMaxWidth())
                        RadarFeedList(visible, activeId, countLabel, showEntry, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }
    }
    if (detail != null) SignalDetails(detail.signal, onClose = { detailId = null })
}

@Composable
private fun RadarFilterChip(filter: RadarFeedFilter, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.mint else colors.cardSurface,
        border = BorderStroke(1.dp, if (selected) colors.green else colors.borderLight),
    ) {
        Row(
            Modifier.heightIn(min = 34.dp).padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            filter.color(colors)?.let { Box(Modifier.size(6.dp).background(it, CircleShape)) }
            Text(filter.label, color = colors.ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MapModeSwitch(flatMode: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSuvidhaColors.current
    Row(modifier.background(colors.cardSurface.copy(alpha = 0.94f), RoundedCornerShape(10.dp))) {
        listOf(true to "2D", false to "3D").forEach { (flat, label) ->
            Surface(
                selected = flatMode == flat,
                onClick = { onChange(flat) },
                shape = RoundedCornerShape(8.dp),
                color = if (flatMode == flat) colors.mint else Color.Transparent,
                modifier = Modifier.heightIn(min = 40.dp),
            ) {
                Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = if (flatMode == flat) colors.green else colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MapCredit(flatMode: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalSuvidhaColors.current
    val uri = LocalUriHandler.current
    Surface(modifier = modifier, shape = RoundedCornerShape(6.dp), color = colors.cardSurface.copy(alpha = 0.94f)) {
        Text(
            if (flatMode) "© OpenStreetMap contributors" else "NASA Black Marble · 2016",
            color = colors.muted, fontSize = 9.sp,
            modifier = Modifier.clickable {
                uri.openUri(if (flatMode) "https://www.openstreetmap.org/copyright" else "https://science.nasa.gov/earth/earth-observatory/earth-at-night/maps/")
            }.padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun GlobeControls(expanded: Boolean, onExpand: () -> Unit, onReset: () -> Unit, onHide: () -> Unit, modifier: Modifier) {
    val colors = LocalSuvidhaColors.current
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = colors.cardSurface.copy(alpha = 0.9f)) {
        Row {
            IconButton(onClick = onHide) {
                Icon(Icons.Outlined.VisibilityOff, contentDescription = "Hide map", tint = colors.ink)
            }
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
private fun PrototypeRadarHeader(query: String, onQueryChange: (String) -> Unit, onOpenSettings: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    var searching by rememberSaveable { mutableStateOf(query.isNotEmpty()) }
    val closeSearch = {
        onQueryChange("")
        searching = false
    }
    BackHandler(enabled = searching, onBack = closeSearch)
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (searching) {
            HeaderSearchField(query, onQueryChange, closeSearch, Modifier.weight(1f), placeholder = "Search radar")
        } else {
            Box(
                Modifier.size(36.dp).background(colors.green, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Radar, contentDescription = null, tint = colors.onGreen)
            }
            Column(Modifier.weight(1f)) {
                Text("Suvidha", color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("WORLD RADAR", color = colors.green, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Tag("Prototype", colors.accent)
                }
            }
            IconButton(onClick = { searching = true }) {
                Icon(Icons.Outlined.Search, contentDescription = "Search radar", tint = colors.muted)
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = colors.muted)
        }
    }
}

@Composable
private fun RadarFeedList(
    entries: List<RadarEntry>,
    activeId: String?,
    countLabel: String,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    val colors = LocalSuvidhaColors.current
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text("Radar stream", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Tag(countLabel, colors.accent)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            if (entries.isEmpty()) {
                item { Text("No matches in this layer.", color = colors.muted, fontSize = 13.sp) }
            }
            items(entries, key = { it.id }) { entry ->
                val selected = entry.id == activeId
                val onClick = { onSelect(entry.id) }
                when (entry) {
                    is RadarEntry.Story -> StoryCard(entry.incident, selected, onClick, isRadarEntry = true)
                    is RadarEntry.Signal -> SignalCard(entry.signal, selected, onClick)
                }
            }
        }
    }
}

@Composable
private fun SignalCard(signal: RadarSignal, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    EntryCard(isHighlighted = selected, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(signal.layer.label, signal.layer.color(colors))
            Text("Demo · ${signal.severity}", color = colors.muted, fontSize = 12.sp)
        }
        Text(signal.title, color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(signal.locationName, color = colors.muted, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SignalDetails(signal: RadarSignal, onClose: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val savedIds by SavedRadarSignals.ids.collectAsStateWithLifecycle()
    val isSaved = signal.id in savedIds
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.cardSurface,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag("Simulated", colors.accent)
                    Tag(signal.layer.label, signal.layer.color(colors))
                }
                IconButton(onClick = { SavedRadarSignals.toggle(context, signal.id) }, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from saved radar signals" else "Save radar signal",
                        tint = if (isSaved) colors.green else colors.muted,
                    )
                }
            }
            Text(signal.title, color = colors.ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(signal.locationName, color = colors.ink, fontSize = 15.sp)
            Text("Demo time  ${signal.observedAt}  ·  ${signal.severity}", color = colors.muted, fontSize = 13.sp)
            Text(signal.summary, color = colors.ink, fontSize = 15.sp)
            Text("Sources", color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("Official references for this layer. This demo event is fictional.", color = colors.muted, fontSize = 13.sp)
            for (source in signal.layer.sourceReferences()) {
                EntryCard(onClick = { uriHandler.openUri(source.url) }) {
                    Text(source.publisher, color = colors.muted, fontSize = 12.sp)
                    Text(source.title, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(source.description, color = colors.muted, fontSize = 13.sp)
                    Text("Open reference ↗", color = colors.green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun RadarLayer.color(colors: SuvidhaColors): Color = when (this) {
    RadarLayer.SEISMIC -> colors.danger
    RadarLayer.WEATHER -> colors.confirm
    RadarLayer.MARITIME -> colors.green
}

private fun RadarFeedFilter.color(colors: SuvidhaColors): Color? = when (this) {
    RadarFeedFilter.ALL -> null
    RadarFeedFilter.STORIES -> colors.ink
    RadarFeedFilter.SEISMIC -> RadarLayer.SEISMIC.color(colors)
    RadarFeedFilter.WEATHER -> RadarLayer.WEATHER.color(colors)
    RadarFeedFilter.MARITIME -> RadarLayer.MARITIME.color(colors)
}
