package com.taurotechnologies.godseye.nativeapp.ui.screens

import com.taurotechnologies.godseye.nativeapp.AppViewModel
import com.taurotechnologies.godseye.nativeapp.data.WeatherEffects
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.taurotechnologies.godseye.nativeapp.FlyTarget
import com.taurotechnologies.godseye.nativeapp.data.FlightMotion
import com.taurotechnologies.godseye.nativeapp.data.FlightTrackHistory
import com.taurotechnologies.godseye.nativeapp.data.NativePreferencesStore
import com.taurotechnologies.godseye.nativeapp.data.RadarEntry
import com.taurotechnologies.godseye.nativeapp.data.RadarFeedFilter
import com.taurotechnologies.godseye.nativeapp.ui.components.MapStepper
import com.taurotechnologies.godseye.nativeapp.data.RadarLayer
import com.taurotechnologies.godseye.nativeapp.data.RadarSignal
import com.taurotechnologies.godseye.nativeapp.data.SatelliteMotion
import com.taurotechnologies.godseye.nativeapp.data.RadarViewLevels
import com.taurotechnologies.godseye.nativeapp.ui.components.heightToZoom
import com.taurotechnologies.godseye.nativeapp.data.SavedRadarSignals
import com.taurotechnologies.godseye.nativeapp.data.formatFlightAltitude
import com.taurotechnologies.godseye.nativeapp.data.formatFlightLastContact
import com.taurotechnologies.godseye.nativeapp.data.formatFlightSpeed
import com.taurotechnologies.godseye.nativeapp.data.radarFeed
import com.taurotechnologies.godseye.nativeapp.data.sourceReferences
import com.taurotechnologies.godseye.nativeapp.data.usesDarkMapTiles
import com.taurotechnologies.godseye.nativeapp.notifications.AlertInboxStore
import com.taurotechnologies.godseye.nativeapp.ui.components.CesiumMap
import com.taurotechnologies.godseye.nativeapp.ui.components.EntryCard
import com.taurotechnologies.godseye.nativeapp.ui.components.GlobeMarker
import com.taurotechnologies.godseye.nativeapp.ui.components.liveLocationName
import com.taurotechnologies.godseye.nativeapp.ui.components.MapCallout
import com.taurotechnologies.godseye.nativeapp.ui.components.MarkerMotion
import com.taurotechnologies.godseye.nativeapp.ui.components.SelectChip
import com.taurotechnologies.godseye.nativeapp.ui.components.Tag
import com.taurotechnologies.godseye.nativeapp.ui.components.ViewportBounds
import com.taurotechnologies.godseye.nativeapp.ui.home.HomeLayersSheet
import com.taurotechnologies.godseye.nativeapp.ui.home.HomeLayersState
import com.taurotechnologies.godseye.nativeapp.ui.home.HomeMapButtons
import com.taurotechnologies.godseye.nativeapp.ui.home.HomeSearchOverlay
import com.taurotechnologies.godseye.nativeapp.ui.home.HomeStreamSheet
import com.taurotechnologies.godseye.nativeapp.ui.home.HomeTopBar
import com.taurotechnologies.godseye.nativeapp.ui.home.LAYER_FILTERS
import com.taurotechnologies.godseye.nativeapp.ui.home.LocalOpenAlerts
import com.taurotechnologies.godseye.nativeapp.ui.home.StreamSheet
import com.taurotechnologies.godseye.nativeapp.ui.home.chipFilters
import com.taurotechnologies.godseye.nativeapp.ui.home.color
import com.taurotechnologies.godseye.nativeapp.ui.home.layerCounts
import com.taurotechnologies.godseye.nativeapp.ui.home.lower
import com.taurotechnologies.godseye.nativeapp.ui.home.matchesSearch
import com.taurotechnologies.godseye.nativeapp.ui.home.pinnedLayers
import com.taurotechnologies.godseye.nativeapp.ui.home.signalKeyFact
import com.taurotechnologies.godseye.nativeapp.ui.home.signalMetaLine
import com.taurotechnologies.godseye.nativeapp.ui.home.togglePinnedLayer
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Map-first home. The map fills the screen; a floating search pill, a chip row and a right-edge button stack
 * float over it. Radar rows are a draggable sheet opened from the "N in view" pill. Markers get labels, clusters
 * and an anchored callout, and the top-signals strip flies the map to a card.
 */
// Opening camera: the whole world.
/** In landscape the search bar and the selected-item card keep a phone-sized width on the left, not the whole screen. */
private val LANDSCAPE_PANEL_MAX_WIDTH = 400.dp
private const val WORLD_VIEW_LAT = 20.0
private const val WORLD_VIEW_LNG = 0.0
private const val WORLD_VIEW_ZOOM = 2.0

/** The home radar: the live map, layer chips and the radar stream. */
@Composable
fun RadarScreen(model: AppViewModel) {
    val isGlobe by model.isGlobe.collectAsStateWithLifecycle()
    val liveRadar by model.liveRadar.collectAsStateWithLifecycle()
    RadarScreenContent(isGlobe = isGlobe, liveRadar = liveRadar, onRefresh = model::refreshLiveRadar, onOpenSettings = model::openSettings, onOpenExplore = model::openExplore, onToggleGlobe = { model.setGlobe(!isGlobe) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadarScreenContent(
    isGlobe: Boolean,
    liveRadar: com.taurotechnologies.godseye.nativeapp.LiveRadarState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExplore: () -> Unit,
    onToggleGlobe: () -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val preferences by NativePreferencesStore.state.collectAsStateWithLifecycle()
    SavedRadarSignals.initialize(context)
    val savedIds by SavedRadarSignals.ids.collectAsStateWithLifecycle()
    val inbox by AlertInboxStore.state.collectAsStateWithLifecycle()
    val openAlerts = LocalOpenAlerts.current
    val signals = liveRadar.signals

    var filter by rememberSaveable { mutableStateOf(RadarFeedFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var activeId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    // Terrain elevation under the selected aircraft, as reported by the globe: (marker id, metres).
    val groundElevation = remember { mutableStateOf<Pair<String, Double>?>(null) }
    var currentScreenOnly by rememberSaveable { mutableStateOf(false) }
    var streamSheet by rememberSaveable { mutableStateOf(StreamSheet.HIDDEN) }
    var layersOpen by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var pickedPlace by remember { mutableStateOf<com.taurotechnologies.godseye.nativeapp.data.PlaceHit?>(null) }
    val flatMode = !isGlobe
    val currentFlatMode = rememberUpdatedState(flatMode)
    val currentDarkMap = rememberUpdatedState(usesDarkMapTiles(preferences.mapTheme, colors.isDark))
    val currentReduceMotion = rememberUpdatedState(preferences.reduceMotion)
    val currentImageryStyle = rememberUpdatedState(preferences.mapProvider)
    // Weather effects follow every loaded weather alert, whichever filter chip is selected.
    val weatherCells = remember(signals) { WeatherEffects.cells(signals) }
    val currentWeatherCells = rememberUpdatedState(weatherCells)
    var mapVisible by rememberSaveable { mutableStateOf(preferences.showMap) }
    var mapFull by rememberSaveable { mutableStateOf(false) }
    var navBarOpen by rememberSaveable { mutableStateOf(false) }
    // The right edge shows one group of buttons at a time (landscape is too short for both, and portrait stays
    // uncluttered). Normal mode has layers/mode/compass/refresh and a way in; zoom mode has tilt, full screen and
    // zoom and a way out.
    var zoomMode by rememberSaveable { mutableStateOf(false) }
    var heading by remember { mutableStateOf(0f) }
    var pitch by remember { mutableStateOf(-90f) }
    var navCommand by remember { mutableStateOf<com.taurotechnologies.godseye.nativeapp.ui.components.NavCommand?>(null) }
    val sendNav: (String) -> Unit = { cmd ->
        navCommand = com.taurotechnologies.godseye.nativeapp.ui.components.NavCommand(cmd, (navCommand?.sequence ?: 0L) + 1)
    }
    var bootSkipped by rememberSaveable { mutableStateOf(false) }
    var cameraRequest by rememberSaveable { mutableStateOf(0L) }
    // Every open of the map flies to 20,000 km first, ignoring the saved selection's zoom. Not saved: a fresh
    // composition (each time the map screen opens) starts true again.
    var openPending by remember { mutableStateOf(true) }
    var viewportBounds by remember { mutableStateOf<ViewportBounds?>(null) }
    var topChromePx by remember { mutableStateOf(0) }
    val topChrome: Dp = with(density) { topChromePx.toDp() }

    // Back order, newest first: search overlay, list sheet (full, then peek, then hidden), selected callout,
    // full-screen map. Layers and detail sheets handle their own Back.
    val backOpen = showMenu || showProfile || showSearch || streamSheet != StreamSheet.HIDDEN || activeId != null || mapFull
    BackHandler(enabled = backOpen) {
        when {
            showMenu -> showMenu = false
            showProfile -> showProfile = false
            showSearch -> { query = ""; showSearch = false }
            // Back from the full-screen list goes straight to the map, like its back arrow.
            streamSheet == StreamSheet.FULL -> streamSheet = if (mapVisible) StreamSheet.HIDDEN else StreamSheet.PEEK
            streamSheet != StreamSheet.HIDDEN -> streamSheet = streamSheet.lower()
            activeId != null -> activeId = null
            mapFull -> mapFull = false
        }
    }
    LaunchedEffect(preferences.showMap) { mapVisible = preferences.showMap }

    val visible = remember(signals, filter, query) {
        radarFeed(signals, filter).filter { matchesSearch(it, query) }
    }
    val counts = remember(signals) { layerCounts(radarFeed(signals, RadarFeedFilter.ALL)) }
    val pinned = pinnedLayers(preferences.pinnedRadarLayers)
    val chips = chipFilters(counts, pinned, filter)
    val active = visible.firstOrNull { it.id == activeId }
    val mapZoomRef = remember { doubleArrayOf(0.0) }
    val cameraZoom = remember { mutableDoubleStateOf(0.0) }
    // Tilt is hidden in the far view, where the globe's centre is held on screen (see EARTH_LOCK_ABOVE_M).
    val tiltShown = com.taurotechnologies.godseye.nativeapp.ui.components.tiltAvailable(flatMode, com.taurotechnologies.godseye.nativeapp.ui.components.zoomToHeight(cameraZoom.doubleValue))
    val cameraLat = remember { mutableDoubleStateOf(0.0) }
    val cameraLng = remember { mutableDoubleStateOf(0.0) }
    val detail = visible.filterIsInstance<RadarEntry.Signal>().firstOrNull { it.id == detailId }

    val displayEntries = remember(visible, currentScreenOnly, viewportBounds) {
        val bounds = viewportBounds
        if (currentScreenOnly && bounds != null) visible.filter { bounds.contains(it.lat, it.lng) } else visible
    }

    // Every entry gets a marker. Live flights and satellites move along their motion models while on the map,
    // gliding smoothly between ticks. Their movement is live data, not decoration, so it stays smooth under
    // Reduce motion too; that setting only turns off the rain streaks and lightning flashes on the globe.
    val animateEntities = mapVisible &&
        visible.any { it is RadarEntry.Signal && (it.signal.flight != null || it.signal.satellite != null) }
    var motionClock by remember { mutableDoubleStateOf(System.currentTimeMillis() / 1000.0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(animateEntities, lifecycleOwner) {
        if (!animateEntities) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                motionClock = System.currentTimeMillis() / 1000.0
                delay(FLIGHT_MOTION_TICK_MS)
            }
        }
    }
    val markers = remember(visible, colors, flatMode, if (animateEntities) motionClock else null) {
        visible.map {
            val color = when (it) {
                is RadarEntry.Signal -> it.signal.layer.color(colors)
            }
            val signal = (it as? RadarEntry.Signal)?.signal
            val flight = signal?.flight
            val satellite = signal?.satellite
            if (flight != null) {
                val moved = if (animateEntities) FlightMotion.position(flight, motionClock) else null
                val next = if (moved != null && moved.coastedSeconds > 0.0) {
                    FlightMotion.position(flight, motionClock + MOTION_RATE_STEP_S)
                } else {
                    null
                }
                GlobeMarker(
                    id = it.id,
                    lat = moved?.lat ?: it.lat,
                    lng = moved?.lng ?: it.lng,
                    color = color,
                    isFlight = true,
                    heading = flight.headingDegrees?.toFloat() ?: 0f,
                    layer = RadarLayer.FLIGHTS,
                    altitudeMeters = flight.bestAltitudeMeters,
                    onGround = flight.onGround == true,
                    title = flight.callsign ?: signal.title,
                    severity = signal.severity,
                    motion = if (moved != null && next != null) {
                        MarkerMotion.between(moved.lat, moved.lng, next.lat, next.lng, MOTION_RATE_STEP_S)
                    } else {
                        null
                    },
                )
            } else if (satellite != null) {
                val pos = if (animateEntities) SatelliteMotion.position(satellite, motionClock) else null
                val next = if (pos != null) SatelliteMotion.position(satellite, motionClock + MOTION_RATE_STEP_S) else null
                GlobeMarker(
                    id = it.id,
                    lat = pos?.lat ?: it.lat,
                    lng = pos?.lng ?: it.lng,
                    color = color,
                    isSatellite = true,
                    layer = RadarLayer.SATELLITES,
                    altitudeMeters = satellite.altitudeKm * 1000.0,
                    title = satellite.name,
                    severity = signal.severity,
                    motion = if (pos != null && next != null) {
                        MarkerMotion.between(pos.lat, pos.lng, next.lat, next.lng, MOTION_RATE_STEP_S)
                    } else {
                        null
                    },
                )
            } else if (it is RadarEntry.Signal) {
                GlobeMarker(
                    id = it.id,
                    lat = it.lat,
                    lng = it.lng,
                    color = color,
                    layer = it.signal.layer,
                    severity = it.signal.severity,
                    title = it.signal.title,
                    altitudeMeters = if (it.signal.layer == RadarLayer.SEISMIC) -10000.0 else 0.0,
                )
            } else {
                GlobeMarker(it.id, it.lat, it.lng, color)
            }
        }
    }

    val callout: MapCallout? = active?.let { entry ->
        when (entry) {
            is RadarEntry.Signal -> MapCallout(
                id = entry.id,
                title = entry.signal.title,
                layerLabel = entry.signal.layer.label,
                colorHex = String.format(Locale.ROOT, "#%06X", entry.signal.layer.color(colors).toArgb() and 0xFFFFFF),
                time = signalMetaLine(entry.signal),
                source = if (entry.signal.isSimulated) "Demo" else entry.signal.provider ?: "Provider",
                fact = signalKeyFact(
                    entry.signal,
                    preferences.aviationUnits,
                    groundElevation.value?.takeIf { it.first == entry.id }?.second,
                ),
                saved = entry.signal.id in savedIds,
            )
        }
    }

    val place = pickedPlace
    val openZoom: Double? = if (openPending) heightToZoom(com.taurotechnologies.godseye.nativeapp.ui.components.MAP_START_HEIGHT_M) else null
    val fly = when {
        // Flights and satellites move: aim at the marker's live position, not the feed's snapshot.
        // The camera settles at the layer's own level (flights 50 km, satellites 700 km, the rest 1 km).
        active != null -> {
            val live = markers.firstOrNull { it.id == active.id }
            val level = RadarViewLevels.heightMeters((active as? RadarEntry.Signal)?.signal?.layer)
            FlyTarget(live?.lat ?: active.lat, live?.lng ?: active.lng, openZoom ?: heightToZoom(level), cameraRequest)
        }
        place != null -> FlyTarget(place.lat, place.lng, openZoom ?: place.zoom, cameraRequest)
        else -> FlyTarget(WORLD_VIEW_LAT, WORLD_VIEW_LNG, openZoom ?: WORLD_VIEW_ZOOM, cameraRequest)
    }

    // Marker tap, strip swipe and list row all select the entry: the map flies there and shows its callout.
    // Alerts bell in the top bar. MainActivity provides the opener; without one, no bell is drawn.
    val bellContent: @Composable () -> Unit = {
        com.taurotechnologies.godseye.nativeapp.ui.home.HomeProfileButton(
            unread = inbox.unreadCount(System.currentTimeMillis()),
            onClick = { showProfile = true },
        )
    }
    val revealMap: () -> Unit = {
        mapVisible = true
        NativePreferencesStore.update(context) { it.copy(showMap = true) }
    }
    val select: (String) -> Unit = { id ->
        if (visible.any { it.id == id }) {
            activeId = id
            cameraRequest++
        }
    }
    val openDetails: (String) -> Unit = { id ->
        when (val entry = visible.firstOrNull { it.id == id }) {
            is RadarEntry.Signal -> detailId = entry.id
            null -> Unit
        }
    }
    val toggleSave: (String) -> Unit = { id ->
        (visible.firstOrNull { it.id == id } as? RadarEntry.Signal)?.let { SavedRadarSignals.toggle(context, it.signal) }
    }
    val clearSelection: () -> Unit = { activeId = null }
    // Previous / next beside the map card: the neighbours of the selected item in feed order, within its own
    // layer (flights step through flights). Null when nothing is selected.
    val stepGroup = remember(visible, active) {
        active?.let { current ->
            val key = (current as? RadarEntry.Signal)?.signal?.layer
            visible.filter { (it as? RadarEntry.Signal)?.signal?.layer == key }
        }.orEmpty()
    }
    val stepAt = stepGroup.indexOfFirst { it.id == activeId }
    val stepTo: (Int) -> Unit = { index -> stepGroup.getOrNull(index)?.let { select(it.id) } }
    val openStepList: () -> Unit = {
        // The full list, filtered to this layer when it has a chip of its own.
        (active as? RadarEntry.Signal)?.signal?.layer?.let { layer ->
            RadarFeedFilter.entries.firstOrNull { it.name == layer.name }?.let { filter = it }
        }
        activeId = null
        streamSheet = StreamSheet.FULL
    }
    val rowTap: (String) -> Unit = { id ->
        if (mapVisible) {
            // Back to the map with just the selected item's card; the Radar stream bar would only cover it.
            select(id)
            streamSheet = StreamSheet.HIDDEN
        } else {
            openDetails(id)
        }
    }
    val countLabel = when {
        currentScreenOnly -> "${displayEntries.size} in view"
        filter == RadarFeedFilter.ALL -> "${displayEntries.size} entries"
        else -> "${displayEntries.size} signals"
    }

    // Move the same globe between layouts so manual pan and zoom survive fullscreen changes.
    val currentOpenDetails = rememberUpdatedState(openDetails)
    val currentToggleSave = rememberUpdatedState(toggleSave)
    val currentClear = rememberUpdatedState(clearSelection)
    val flightTrajectory = remember(active, liveRadar) {
        active.let { it as? RadarEntry.Signal }?.signal?.flight?.let(FlightTrackHistory::trajectory)
    }
    val currentTrajectory = rememberUpdatedState(flightTrajectory)
    val globeMap = remember {
        movableContentOf<List<GlobeMarker>, String?, FlyTarget, (String) -> Unit> { points, selection, target, onSelect ->
            CesiumMap(
                points, selection, target, onSelect,
                onZoomChanged = { mapZoomRef[0] = it; cameraZoom.doubleValue = it },
                onFlown = { openPending = false },
                modifier = Modifier.fillMaxSize(),
                flat = currentFlatMode.value,
                onViewportBounds = { viewportBounds = it },
                darkTheme = currentDarkMap.value,
                navControls = false,
                callout = null,
                onPose = { h, p -> heading = h; pitch = p },
                onCameraCenter = { lat, lng -> cameraLat.doubleValue = lat; cameraLng.doubleValue = lng },
                navCommand = navCommand,
                reduceMotion = currentReduceMotion.value,
                imageryStyle = currentImageryStyle.value,
                weatherCells = currentWeatherCells.value,
                trajectory = currentTrajectory.value,
                onCalloutDetails = { currentOpenDetails.value(it) },
                onCalloutSave = { currentToggleSave.value(it) },
                onMapTap = { currentClear.value() },
                onGroundElevation = { id, meters -> groundElevation.value = id to meters },
            )
        }
    }
    val mapPane: @Composable (Modifier) -> Unit = { mapModifier ->
        // No rounded clip here: a clip around the osmdroid map stopped its tiles from drawing.
        Box(mapModifier.background(colors.surfaceMuted)) {
            if (mapVisible) {
                // A new CesiumJS instance per fullscreen toggle. 2D and 3D switch in place, so the engine is not reloaded.
                key(mapFull) { globeMap(markers, activeId, fly, select) }
            }
        }
    }

    if (liveRadar.loading && signals.isEmpty() && !bootSkipped) {
        // Preload: the globe is composed under the opaque boot screen, so Cesium loads during boot. It is the same
        // movable instance the home layout uses, so it carries over with no second load.
        Box(Modifier.fillMaxSize()) {
            if (mapVisible) mapPane(Modifier.fillMaxSize())
            BootScreen(sourcesLoading = liveRadar.loading, onCancel = { bootSkipped = true }, modifier = Modifier.fillMaxSize())
        }
    } else {
        BoxWithConstraints(Modifier.fillMaxSize().background(colors.canvas)) {
            // Landscape is only ~360 dp tall: the top-right buttons and the bottom-right tilt/zoom column
            // would run into each other and the search bar. There they sit in two side-by-side columns
            // (116 dp wide in all) and the top bar and card stop short of them.
            val landscape = maxWidth > maxHeight
            val showMainButtons = !zoomMode || mapFull
            val showZoomButtons = zoomMode
            mapPane(Modifier.fillMaxSize())

            // Zoom mode is a map-only view: the search pill, chips and selected card step aside, the zoom controls stay.
            if (!mapFull && !zoomMode) {
                // Floating search pill and chip row. Only these take touches; the rest of the top edge is the map.
                Column(
                    Modifier
                        .align(if (landscape) Alignment.TopStart else Alignment.TopCenter)
                        .then(if (landscape) Modifier.widthIn(max = LANDSCAPE_PANEL_MAX_WIDTH) else Modifier)
                        .fillMaxWidth()
                        .onSizeChanged { topChromePx = it.height }
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HomeTopBar(
                        onOpenSearch = { showSearch = true },
                        onOpenMenu = { showMenu = true },
                        alertsBell = if (openAlerts != null) bellContent else null,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(chips, key = { it.name }) { entry ->
                            SelectChip(
                                label = entry.label,
                                isSelected = filter == entry,
                                onClick = {
                                    if (filter != entry) {
                                        filter = entry
                                        activeId = null
                                        // Any radar layer: jump to the first row in that layer, same as tapping it in the list.
                                        if (entry in LAYER_FILTERS) {
                                            radarFeed(signals, entry).firstOrNull { matchesSearch(it, query) }?.let {
                                                pickedPlace = null
                                                activeId = it.id
                                                cameraRequest++
                                            }
                                        }
                                    }
                                },
                                dotColor = entry.color(colors),
                            )
                        }
                        item {
                            SelectChip(label = "More", isSelected = false, onClick = { showMenu = true })
                        }
                    }
                }
            }

            // Right-edge buttons. In full-screen map mode they sit under the status bar; otherwise under the chips.
            if ((streamSheet != StreamSheet.FULL || mapFull) && showMainButtons) {
                HomeMapButtons(
                    onLayers = { layersOpen = true },
                    flatMode = flatMode,
                    onToggleMode = onToggleGlobe,
                    fullscreen = mapFull,
                    onToggleFullscreen = { mapFull = !mapFull },
                    loading = liveRadar.loading,
                    onRefresh = onRefresh,
                    onExplore = onOpenExplore,
                    navBarOpen = navBarOpen || mapFull,
                    onToggleNavBar = { navBarOpen = !navBarOpen },
                    compassHeading = if (!flatMode && (kotlin.math.abs(((heading + 540f) % 360f) - 180f) > 1f || pitch > -89f)) heading else null,
                    onCompass = { sendNav("north") },
                    showFullscreen = false,
                    trailing = if (!mapFull) ({
                        com.taurotechnologies.godseye.nativeapp.ui.home.MapFloatButton("Zoom and tilt controls", { zoomMode = true }) {
                            Icon(Icons.Outlined.Fullscreen, null, tint = colors.ink, modifier = Modifier.size(20.dp))
                        }
                    }) else null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .then(
                            if (mapFull) Modifier.statusBarsPadding().padding(top = 12.dp, end = 12.dp)
                            else if (landscape) Modifier.statusBarsPadding().padding(top = 8.dp, end = 12.dp)
                            else Modifier.padding(top = topChrome + 8.dp, end = 12.dp),
                        ),
                )
            }

            // One bottom-right column, centred on the same line as the top-right buttons (all 44 dp):
            // tilt up / tilt readout / tilt down (3D only), full screen, zoom in / camera height / zoom out.
            if (!mapFull && streamSheet == StreamSheet.HIDDEN && mapVisible) {
                if (showZoomButtons) Column(
                    // The same spot as the main buttons, which this group replaces while zoom mode is on.
                    (if (landscape) Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 12.dp)
                    else Modifier.align(Alignment.TopEnd).padding(top = topChrome + 8.dp, end = 12.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (landscape) 6.dp else 8.dp),
                ) {
                    if (tiltShown) {
                        com.taurotechnologies.godseye.nativeapp.ui.components.MapTiltPill(
                            flat = flatMode, onCommand = sendNav,
                        )
                    }
                    com.taurotechnologies.godseye.nativeapp.ui.components.MapZoomPill(onCommand = sendNav)
                    // The square that opened zoom mode, always last in the column: square-out to open, square-in to close.
                    com.taurotechnologies.godseye.nativeapp.ui.home.MapFloatButton("Close zoom and tilt controls", { zoomMode = false }) {
                        Icon(Icons.Outlined.FullscreenExit, null, tint = colors.ink, modifier = Modifier.size(20.dp))
                    }
                }
                // Camera height and tilt in the strip along the bottom edge, beside the map credits.
                com.taurotechnologies.godseye.nativeapp.ui.components.MapCameraReadout(
                    heightMeters = { com.taurotechnologies.godseye.nativeapp.ui.components.zoomToHeight(cameraZoom.doubleValue) },
                    tiltPitch = if (tiltShown) ({ pitch }) else null,
                    lat = { cameraLat.doubleValue }, lng = { cameraLng.doubleValue }, heading = { heading },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 14.dp, bottom = 6.dp),
                )
            }
            if (mapFull) {
                Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 12.dp, bottom = 76.dp)) {
                    com.taurotechnologies.godseye.nativeapp.ui.home.FullscreenMapButton(true) { mapFull = false }
                }
            }

            if (mapFull) {
                com.taurotechnologies.godseye.nativeapp.ui.components.MapCameraReadout(
                    heightMeters = { com.taurotechnologies.godseye.nativeapp.ui.components.zoomToHeight(cameraZoom.doubleValue) },
                    tiltPitch = if (tiltShown) ({ pitch }) else null,
                    lat = { cameraLat.doubleValue }, lng = { cameraLng.doubleValue }, heading = { heading },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 14.dp, bottom = 6.dp),
                )
            }

            if (!mapFull) {
                if (streamSheet == StreamSheet.HIDDEN && mapVisible) {
                    Column(
                        Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp)
                            .then(if (landscape) Modifier.fillMaxWidth() else Modifier),
                        horizontalAlignment = if (landscape) Alignment.Start else Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (navBarOpen) {
                            com.taurotechnologies.godseye.nativeapp.ui.components.MapNavBar(flat = flatMode, tiltAvailable = tiltShown, onCommand = sendNav, modifier = Modifier.padding(horizontal = 12.dp))
                        }
                        if (callout != null && !zoomMode) {
                            val selectedEntry = active
                            if (selectedEntry != null) {
                                // Same row as the Radar stream list, so a marker tap and a list row look alike.
                                // Card on the left, clear of the bottom-right control column (12 + 44 + 10 dp).
                                val stepper: @Composable () -> Unit = {
                                    if (stepAt >= 0 && stepGroup.size > 1) {
                                        MapStepper(
                                            position = stepAt + 1,
                                            total = stepGroup.size,
                                            onPrevious = if (stepAt > 0) ({ stepTo(stepAt - 1) }) else null,
                                            onNext = if (stepAt < stepGroup.size - 1) ({ stepTo(stepAt + 1) }) else null,
                                            onOpenList = openStepList,
                                        )
                                    }
                                }
                                Row(
                                    // The width limit has to come before fillMaxWidth, or the full width wins.
                                    // Landscape: exactly the search bar's width and edges, with the stepper inside it.
                                    (if (landscape) Modifier.widthIn(max = LANDSCAPE_PANEL_MAX_WIDTH) else Modifier)
                                        .fillMaxWidth()
                                        // Card and stepper share one height: the row is as tall as its taller child.
                                        .height(IntrinsicSize.Min)
                                        .padding(start = if (landscape) 12.dp else 16.dp, end = 16.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = colors.cardSurface,
                                    tonalElevation = 2.dp,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                ) {
                                    Box {
                                        // Card tap points the camera back at the item; the info button opens its details.
                                        // A tap on empty map clears the selection.
                                        val onClick = { select(selectedEntry.id) }
                                        // The card is as tall as the stepper beside it; centre its text in that height instead of
                                        // leaving the location line stranded at the bottom.
                                        Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                                            when (selectedEntry) {
                                                is RadarEntry.Signal -> SignalRow(selectedEntry.signal, false, onClick)
                                            }
                                        }
                                        // Details, and X to stop tracking (clears the selection).
                                        Row(Modifier.align(Alignment.TopEnd)) {
                                            IconButton(onClick = { openDetails(selectedEntry.id) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Outlined.Info, contentDescription = "Details", tint = colors.muted, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = clearSelection, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Outlined.Close, contentDescription = "Stop tracking", tint = colors.muted, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                                stepper()
                                }
                            }
                        }
                        // No selection: the map is clear. Pick a layer from the Layers menu to open its list.
                    }
                } else {
                    // With the map hidden, the list is the screen, so it never closes below peek.
                    val shown = if (streamSheet == StreamSheet.HIDDEN) StreamSheet.PEEK else streamSheet
                    if (shown == StreamSheet.FULL) {
                        // Fully open: the list takes the whole screen, with a back arrow to return to the map.
                        RadarStreamFullScreen(
                            onBack = { streamSheet = if (mapVisible) StreamSheet.HIDDEN else StreamSheet.PEEK },
                            header = {
                                RadarStreamHeader(
                                    countLabel = countLabel,
                                    liveRadar = liveRadar,
                                    showMapAction = if (mapVisible) null else ({ revealMap(); streamSheet = StreamSheet.HIDDEN }),
                                )
                            },
                            body = { RadarFeedBody(entries = displayEntries, activeId = activeId, onSelect = rowTap) },
                        )
                    } else HomeStreamSheet(
                        state = shown,
                        onStateChange = { streamSheet = it },
                        maxHeight = maxHeight,
                        topChrome = topChrome,
                        header = {
                            RadarStreamHeader(
                                countLabel = countLabel,
                                liveRadar = liveRadar,
                                showMapAction = if (mapVisible) null else revealMap,
                            )
                        },
                        body = {
                            RadarFeedBody(entries = displayEntries, activeId = activeId, onSelect = rowTap)
                        },
                    )
                }
            }

            if (showMenu) {
                com.taurotechnologies.godseye.nativeapp.ui.home.HomeMenuScreen(
                    layers = HomeLayersState(
                filter = filter,
                counts = counts,
                pinned = pinned,
                currentScreenOnly = currentScreenOnly,
                mapTheme = preferences.mapTheme,
                mapStyle = preferences.mapProvider,
                globePreset = preferences.globePreset,
            ),
                    onSelectFilter = { picked ->
                        if (filter != picked) {
                            filter = picked
                            activeId = null
                        }
                        showMenu = false
                        // Picking a layer opens its list full-screen; drag down or press Back to return to the map.
                        streamSheet = StreamSheet.FULL
                    },
                    onTogglePin = { picked ->
                        NativePreferencesStore.update(context) { it.copy(pinnedRadarLayers = togglePinnedLayer(it.pinnedRadarLayers, picked)) }
                    },
                    onBack = { showMenu = false },
                )
            }
            if (showProfile) {
                com.taurotechnologies.godseye.nativeapp.ui.home.HomeProfileScreen(
                    unread = inbox.unreadCount(System.currentTimeMillis()),
                    onBack = { showProfile = false },
                    onOpenAlerts = { showProfile = false; openAlerts?.invoke() },
                    onOpenSettings = { showProfile = false; onOpenSettings() },
                )
            }
            if (showSearch) {
                HomeSearchOverlay(
                    query = query,
                    onQueryChange = { query = it },
                    onClose = { query = ""; showSearch = false },
                    results = if (query.isBlank()) emptyList() else radarFeed(signals, RadarFeedFilter.ALL).filter { matchesSearch(it, query) },
                    onPickEntry = { id ->
                        query = ""
                        showSearch = false
                        mapVisible = true
                        filter = RadarFeedFilter.ALL
                        pickedPlace = null
                        activeId = id
                        cameraRequest++
                    },
                    onPickPlace = { hit ->
                        pickedPlace = hit
                        activeId = null
                        query = ""
                        showSearch = false
                        mapVisible = true
                        cameraRequest++
                    },
                )
            }
        }
    }

    if (layersOpen) {
        HomeLayersSheet(
            state = HomeLayersState(
                filter = filter,
                counts = counts,
                pinned = pinned,
                currentScreenOnly = currentScreenOnly,
                mapTheme = preferences.mapTheme,
                mapStyle = preferences.mapProvider,
                globePreset = preferences.globePreset,
            ),
            onCurrentScreenOnly = { currentScreenOnly = it },
            onMapTheme = { value -> NativePreferencesStore.update(context) { it.copy(mapTheme = value) } },
            onMapStyle = { value -> NativePreferencesStore.update(context) { it.copy(mapProvider = value) } },
            onGlobePreset = { value -> NativePreferencesStore.update(context) { it.copy(globePreset = value) } },
            onDismiss = { layersOpen = false },
        )
    }
    if (detail != null) {
        SignalDetails(
            detail.signal,
            onClose = { detailId = null },
            groundElevationMeters = groundElevation.value?.takeIf { it.first == detail.id }?.second,
        )
    }
}

@Composable
private fun RadarStreamHeader(countLabel: String, liveRadar: com.taurotechnologies.godseye.nativeapp.LiveRadarState, showMapAction: (() -> Unit)?) {
    val colors = LocalSuvidhaColors.current
    Column(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Radar stream", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Tag(countLabel, colors.accent)
            Spacer(Modifier.weight(1f))
            if (showMapAction != null) {
                TextButton(onClick = showMapAction) { Text("Show map", color = colors.green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (liveRadar.message.isNotBlank()) {
            Text(
                liveRadar.message,
                color = if (liveRadar.hasError) colors.danger else colors.muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The Radar stream list as a full-screen page: back arrow and header on top, list below. */
@Composable
private fun RadarStreamFullScreen(onBack: () -> Unit, header: @Composable () -> Unit, body: @Composable () -> Unit) {
    val colors = LocalSuvidhaColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.cardSurface)
            // Swallow taps on empty space so they do not reach the map underneath.
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = {})
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to map", tint = colors.ink)
            }
            Box(Modifier.weight(1f)) { header() }
        }
        HorizontalDivider(color = colors.borderLight.copy(alpha = 0.6f))
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) { body() }
    }
}

@Composable
private fun RadarFeedBody(entries: List<RadarEntry>, activeId: String?, onSelect: (String) -> Unit) {
    val colors = LocalSuvidhaColors.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
        if (entries.isEmpty()) {
            item { Text("No matches in this layer.", color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(16.dp)) }
        }
        items(entries, key = { it.id }) { entry ->
            val selected = entry.id == activeId
            val onClick = { onSelect(entry.id) }
            when (entry) {
                is RadarEntry.Signal -> SignalRow(entry.signal, selected, onClick)
            }
            HorizontalDivider(color = colors.borderLight.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun SignalRow(signal: RadarSignal, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    Surface(onClick = onClick, color = if (selected) colors.mint else Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(5.dp).background(signal.layer.color(colors), CircleShape))
                Text(signal.layer.label.uppercase(Locale.ROOT), color = signal.layer.color(colors), fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(if (signal.isSimulated) "Demo" else signal.provider ?: "Provider", color = colors.muted, fontSize = 11.sp, lineHeight = 14.sp)
            }
            Text(
                signal.title, color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(liveLocationName(signal), color = colors.muted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SignalDetails(
    signal: RadarSignal,
    onClose: () -> Unit,
    groundElevationMeters: Double? = null,
) {
    val colors = LocalSuvidhaColors.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val preferences by NativePreferencesStore.state.collectAsStateWithLifecycle()
    val savedIds by SavedRadarSignals.ids.collectAsStateWithLifecycle()
    val isSaved = signal.id in savedIds
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.cardSurface,
        // The sheet's default is 640 dp, which is nearly the whole screen in landscape. A phone-sized column
        // reads better and leaves the map visible beside it; portrait phones are narrower than this anyway.
        sheetMaxWidth = 440.dp,
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
                    Tag(if (signal.isSimulated) "Simulated" else "Provider data", colors.accent)
                    Tag(signal.layer.label, signal.layer.color(colors))
                }
                androidx.compose.material3.IconButton(onClick = { SavedRadarSignals.toggle(context, signal) }, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from saved radar signals" else "Save radar signal",
                        tint = if (isSaved) colors.green else colors.muted,
                    )
                }
            }
            Text(signal.title, color = colors.ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(liveLocationName(signal), color = colors.ink, fontSize = 15.sp)
            Text("${if (signal.isSimulated) "Demo time" else "Observed"}  ${signal.observedAt}  ·  ${signal.severity}", color = colors.muted, fontSize = 13.sp)
            Text(signal.summary, color = colors.ink, fontSize = 15.sp)
            signal.flight?.let { flight ->
                Text("Aircraft details", color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                val telemetry = listOfNotNull(
                    "Callsign" to (flight.callsign ?: "Unknown"),
                    "${if (signal.isSimulated) "Demo " else ""}ICAO address" to flight.id.uppercase(Locale.ROOT),
                    flight.originCountry?.let { "Origin country" to it },
                    "Position" to String.format(Locale.ROOT, "%.3f°, %.3f°", flight.lat, flight.lng),
                    "Barometric altitude" to formatFlightAltitude(flight.altitudeMeters, preferences.aviationUnits),
                    "Height above ground" to when {
                        flight.onGround == true -> "On ground"
                        else -> flight.heightAboveGroundMeters(groundElevationMeters)
                            ?.let { "~" + formatFlightAltitude(it, preferences.aviationUnits) }
                            ?: "Unknown"
                    },
                    "Ground speed" to formatFlightSpeed(flight.speedMetersPerSecond, preferences.aviationUnits),
                    "Heading" to (flight.headingDegrees?.let { String.format(Locale.ROOT, "%.0f°", it) } ?: "Unknown"),
                    formatFlightLastContact(flight.lastContactEpochSeconds, System.currentTimeMillis() / 1000L)
                        ?.let { "Last contact" to it },
                )
                telemetry.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, Modifier.weight(1f), color = colors.muted, fontSize = 13.sp)
                        Text(value, color = colors.ink, fontSize = 13.sp)
                    }
                }
                Text(signal.provider.orEmpty(), color = colors.muted, fontSize = 13.sp)
                Text(signal.coverage.orEmpty(), color = colors.muted, fontSize = 13.sp)
            }
            signal.satellite?.let { sat ->
                var eyeOpen by remember(sat.noradId) { mutableStateOf(false) }
                Text("Satellite eye", color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("The Earth below ${sat.name} right now, following its orbit.", color = colors.muted, fontSize = 13.sp)
                com.taurotechnologies.godseye.nativeapp.ui.components.SatelliteEyePreview(sat, onExpand = { eyeOpen = true })
                if (eyeOpen) {
                    // A dialog window, so the full-screen view sits above this bottom sheet's own window.
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { eyeOpen = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
                    ) {
                        com.taurotechnologies.godseye.nativeapp.ui.components.SatelliteEyeView(sat, onClose = { eyeOpen = false })
                    }
                }
                Text("Orbital elements", color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                val elements = listOf(
                    "Satellite" to sat.name,
                    "NORAD ID" to sat.noradId,
                    "Operator" to sat.country,
                    "Orbit type" to sat.type,
                    "Altitude" to "${sat.altitudeKm.toInt()} km LEO",
                    "Velocity" to "${sat.velocityKmPerSec} km/s (${(sat.velocityKmPerSec * 3600).toInt()} km/h)",
                    "Inclination" to "${sat.inclinationDeg}°",
                    "Period" to "${sat.periodMinutes} min",
                )
                elements.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, Modifier.weight(1f), color = colors.muted, fontSize = 13.sp)
                        Text(value, color = colors.ink, fontSize = 13.sp)
                    }
                }
            }
            if (signal.layer == RadarLayer.WEBCAMS) {
                signal.eventSourceUrl?.let { camUrl ->
                    var watchOpen by remember(signal.id) { mutableStateOf(false) }
                    Text("Live camera", color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Streaming now from ${signal.title}. It pauses after a period without touch, set in Settings.", color = colors.muted, fontSize = 13.sp)
                    // One stream at a time: the preview unloads while the full player is open.
                    if (!watchOpen) {
                        com.taurotechnologies.godseye.nativeapp.ui.components.WebcamPreview(camUrl, onExpand = { watchOpen = true })
                    } else {
                        Spacer(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                    }
                    if (watchOpen) {
                        com.taurotechnologies.godseye.nativeapp.ui.components.WatchLiveDialog(
                            title = signal.title, subtitle = signal.locationName, sourceUrl = camUrl, onDismiss = { watchOpen = false },
                        )
                    }
                }
            }
            Text("Sources", color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            signal.eventSourceUrl?.let { eventUrl ->
                EntryCard(onClick = { uriHandler.openUri(eventUrl) }) {
                    Text("${signal.provider ?: "Provider"} event record ↗", color = colors.green, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Open the original event and its latest revisions.", color = colors.muted, fontSize = 13.sp)
                }
            }
            Text(if (signal.isSimulated) "Official references for this layer. This demo event is fictional." else "Provider documentation. Observations may be revised; timestamps identify the received data.", color = colors.muted, fontSize = 13.sp)
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

/** One marker update per second: smooth enough at map zoom levels without redrawing every frame. */
// Every tick rebuilds every marker and re-sends them to the globe, so it is kept slow; the globe glides
// moving craft between ticks along their motion rate (globe.html, MAX_COAST_S must exceed this).
private const val FLIGHT_MOTION_TICK_MS = 5_000L

/** Look-ahead used to turn the motion models into a per-second rate the globe glides along between ticks. */
private const val MOTION_RATE_STEP_S = 1.0
