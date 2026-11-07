package com.taurotechnologies.godseye.nativeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.taurotechnologies.godseye.nativeapp.data.WebcamCameras
import com.taurotechnologies.godseye.nativeapp.data.explore.ExploreDataRepository
import com.taurotechnologies.godseye.nativeapp.data.explore.ExploreRow
import com.taurotechnologies.godseye.nativeapp.data.explore.PanelContent
import com.taurotechnologies.godseye.nativeapp.data.explorePanel
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.Locale

private const val ERROR_MESSAGE = "Couldn't load this panel. Check your connection and retry."

/** Live panels refetch at most this often while open; also the freshness cap for that refetch. */
private const val LIVE_REFRESH_MILLIS: Long = 60_000
private val UPDATED_FORMAT: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault())

private sealed interface LoadState {
    object Loading : LoadState
    object NoFeed : LoadState
    data class Content(val content: PanelContent) : LoadState
    data class Error(val message: String) : LoadState
}

/**
 * Shared body for one Explore panel: a status line (category, last update, refresh, save star), then the
 * panel's summary, chart and rows loaded from the data layer. Used by the Explore hub under its chip lists
 * and by the full panel screen, so both show the same data the same way.
 */
@Composable
fun ExplorePanelData(
    panelId: String,
    saved: Boolean,
    onToggleSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSuvidhaColors.current
    val panel = explorePanel(panelId)

    var reloadKey by remember { mutableIntStateOf(0) }
    var state by remember(panelId) { mutableStateOf<LoadState>(LoadState.Loading) }
    var updatedAtMillis by remember(panelId) { mutableStateOf<Long?>(null) }
    val live = remember(panelId) { ExploreDataRepository.isLive(panelId) }
    // A YouTube camera row plays in the app; any other row opens its link in the browser.
    var watching by remember(panelId) { mutableStateOf<ExploreRow?>(null) }
    val uriHandler = LocalUriHandler.current
    val openRow: (ExploreRow) -> Unit = { row ->
        row.url?.let { url ->
            if (WebcamCameras.youtubeVideoId(url) != null) watching = row
            else runCatching { uriHandler.openUri(url) }
        }
    }

    // reloadKey 0 is the first open (cached data is fine); later values are a retry or a manual refresh.
    LaunchedEffect(panelId, reloadKey) {
        if (!ExploreDataRepository.hasData(panelId)) {
            state = LoadState.NoFeed
            return@LaunchedEffect
        }
        // Keep showing current content while refreshing; only a first load shows the spinner.
        if (state !is LoadState.Content) state = LoadState.Loading
        try {
            val content = ExploreDataRepository.load(panelId, maxAgeMillis = if (reloadKey == 0) null else 0L)
            state = if (content == null) LoadState.NoFeed else LoadState.Content(content)
            updatedAtMillis = System.currentTimeMillis()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (state !is LoadState.Content) state = LoadState.Error(ERROR_MESSAGE)
        }
    }

    // Live refresh while the panel is on screen; paused in the background. A failed refresh keeps
    // the last good content rather than replacing it with an error.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(panelId, live, lifecycleOwner) {
        if (!live) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(LIVE_REFRESH_MILLIS)
                try {
                    ExploreDataRepository.load(panelId, maxAgeMillis = LIVE_REFRESH_MILLIS)?.let { fresh ->
                        state = LoadState.Content(fresh)
                        updatedAtMillis = System.currentTimeMillis()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Next tick retries.
                }
            }
        }
    }

    if (panel == null) {
        Text(text = "Panel not found.", color = colors.muted, modifier = modifier.padding(16.dp))
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val updated = updatedAtMillis?.takeIf { live && state is LoadState.Content }?.let {
            " · Updated ${UPDATED_FORMAT.format(java.time.Instant.ofEpochMilli(it))} · live"
        }.orEmpty()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = panel.title + " · " + panel.category + updated,
                color = colors.muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (live) {
                IconButton(onClick = { reloadKey++ }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh ${panel.title}", tint = colors.muted)
                }
            }
            IconButton(onClick = { onToggleSave(panel.id) }) {
                Icon(
                    imageVector = if (saved) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (saved) "Unsave ${panel.title}" else "Save ${panel.title}",
                    tint = if (saved) colors.green else colors.muted,
                )
            }
        }
        when (val current = state) {
            LoadState.Loading -> LoadingRow()
            LoadState.NoFeed -> NoFeedCard()
            is LoadState.Error -> ErrorBlock(message = current.message, onRetry = { reloadKey++ })
            is LoadState.Content -> ExploreContent(content = current.content, onOpenRow = openRow)
        }
    }
    watching?.let { row ->
        WatchLiveDialog(title = row.title, subtitle = row.subtitle, sourceUrl = row.url.orEmpty(), onDismiss = { watching = null })
    }
}

@Composable
private fun LoadingRow() {
    val colors = LocalSuvidhaColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.muted, strokeWidth = 2.dp)
        Text(text = "Loading…", color = colors.muted, fontSize = 14.sp)
    }
}

@Composable
private fun NoFeedCard() {
    val colors = LocalSuvidhaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderLight),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Live data for this panel is not connected in the native app yet.", color = colors.ink, fontSize = 15.sp)
            Text(text = "Open GodsEye on the web to see it.", color = colors.muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = message, color = colors.danger, fontSize = 14.sp)
        TextButton(onClick = onRetry) { Text(text = "Retry", color = colors.ink) }
    }
}

@Composable
private fun ColumnScope.ExploreContent(content: PanelContent, onOpenRow: (ExploreRow) -> Unit) {
    val colors = LocalSuvidhaColors.current
    val summary = content.summary

    if (summary == null && content.chart == null && content.rows.isEmpty()) {
        Text(text = "No data published right now.", color = colors.muted, fontSize = 14.sp)
        return
    }
    // Summary, chart and rows scroll together so a chart never squeezes the list on small screens.
    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
        if (summary != null) {
            item(key = "summary") {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderLight),
                ) {
                    Text(text = summary, color = colors.ink, fontSize = 15.sp, modifier = Modifier.padding(16.dp))
                }
            }
        }
        content.chart?.let { chart ->
            item(key = "chart") { ExploreChartView(chart, Modifier.padding(bottom = 12.dp)) }
        }
        itemsIndexed(content.rows) { index, row ->
            Column {
                if (index > 0) HorizontalDivider(color = colors.borderLight)
                ExploreRowItem(row = row, onOpen = onOpenRow)
            }
        }
    }
}

@Composable
private fun ExploreRowItem(row: ExploreRow, onOpen: (ExploreRow) -> Unit) {
    val colors = LocalSuvidhaColors.current
    val url = row.url

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (url != null) Modifier.clickable { onOpen(row) } else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                color = colors.ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            row.subtitle?.let { subtitle ->
                Text(text = subtitle, color = colors.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            row.value?.let { value ->
                Text(text = value, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            row.change?.let { change ->
                Text(
                    text = String.format(Locale.US, "%+.2f", change),
                    color = if (change >= 0) colors.green else colors.danger,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
