package com.taurotechnologies.godseye.nativeapp.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.Place
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.taurotechnologies.godseye.nativeapp.data.PlaceHit
import com.taurotechnologies.godseye.nativeapp.data.PlaceSearch
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors

/**
 * Opens the alerts list. MainActivity provides it, so the bell can live in the home top bar.
 * Null means no alerts host, and the bell is not drawn.
 */
val LocalOpenAlerts = staticCompositionLocalOf<(() -> Unit)?> { null }

/** Provides [LocalOpenAlerts] for a subtree. */
@Composable
fun ProvideOpenAlerts(open: () -> Unit, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOpenAlerts provides open, content = content)
}

/**
 * Floating search pill: logo and menu on the left (menu holds Explore and Settings), a search affordance in the
 * middle that opens [HomeSearchOverlay], and the alerts bell on the right. Only the pill takes touches.
 */
@Composable
fun HomeTopBar(
    onOpenSearch: () -> Unit,
    onOpenMenu: () -> Unit,
    alertsBell: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSuvidhaColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.cardSurface,
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            HomeMenuButton(onClick = onOpenMenu)
            Row(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable(onClick = onOpenSearch)
                    .semantics { contentDescription = "Search radar" }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Search radar", color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            alertsBell?.invoke()
        }
    }
}

@Composable
private fun HomeMenuButton(onClick: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = colors.ink, modifier = Modifier.size(20.dp))
    }
}

/**
 * Full-screen search. Currently the existing feed search (query filters the list). The universal search
 * agent swaps this body out: keep the signature and call it from RadarScreen where `showSearch` is read.
 */
@Composable
fun HomeSearchOverlay(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit, onPickPlace: (PlaceHit) -> Unit = {},
    results: List<com.taurotechnologies.godseye.nativeapp.data.RadarEntry> = emptyList(), onPickEntry: (String) -> Unit = {}) {
    val colors = LocalSuvidhaColors.current
    var remote by remember { mutableStateOf<List<PlaceHit>>(emptyList()) }
    LaunchedEffect(query) {
        remote = emptyList()
        if (query.trim().length >= 2) {
            delay(400)
            remote = PlaceSearch.remote(query)
        }
    }
    val places = (PlaceSearch.countryMatches(query) + remote).distinctBy { "${it.name}|${it.lat.toInt()}|${it.lng.toInt()}" }
    Surface(modifier = Modifier.fillMaxSize(), color = colors.canvas) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            HeaderSearchField(query, onQueryChange, onClose, Modifier.fillMaxWidth(), placeholder = "Search radar, places, countries")
            LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                items(results.take(30)) { entry ->
                    val (title, detail) = when (entry) {
                        is com.taurotechnologies.godseye.nativeapp.data.RadarEntry.Signal ->
                            (entry.signal.flight?.callsign?.trim()?.ifBlank { null } ?: entry.signal.title) to entry.signal.locationName
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { onPickEntry(entry.id) }.padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Place, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(title, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(detail, color = colors.muted, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
                items(places) { hit ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPickPlace(hit) }.padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Place, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(hit.name, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(hit.detail, color = colors.muted, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** Right end of the search pill: the profile. Opens the profile page. */
@Composable
fun HomeProfileButton(unread: Int, onClick: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (unread > 0) Badge(containerColor = colors.danger) { Text(if (unread > 99) "99+" else unread.toString()) } }) {
            Icon(Icons.Filled.AccountCircle, contentDescription = "Profile, $unread unread alerts", tint = colors.ink)
        }
    }
}

@Composable
private fun HeaderSearchField(query: String, onChange: (String) -> Unit, onClose: () -> Unit, modifier: Modifier, placeholder: String = "Search stories") {
    val colors = LocalSuvidhaColors.current
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    // Compact single-row field: the Material outlined field is 56dp tall with wide inner padding.
    BasicTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = colors.ink, fontSize = 14.sp),
        cursorBrush = SolidColor(colors.green),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardSurface)
                    .border(1.dp, if (focused) colors.green else colors.borderLight, RoundedCornerShape(12.dp))
                    .padding(start = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(placeholder, fontSize = 14.sp, color = colors.muted)
                    inner()
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close search", tint = colors.muted, modifier = Modifier.size(18.dp))
                }
            }
        },
        modifier = modifier.height(44.dp).focusRequester(focus).onFocusChanged { focused = it.isFocused },
    )
}
