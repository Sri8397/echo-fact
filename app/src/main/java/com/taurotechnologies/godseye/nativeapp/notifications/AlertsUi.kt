package com.taurotechnologies.godseye.nativeapp.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taurotechnologies.godseye.nativeapp.nav.NativeOverlay
import com.taurotechnologies.godseye.nativeapp.ui.components.EntryCard
import com.taurotechnologies.godseye.nativeapp.ui.components.HelpText
import com.taurotechnologies.godseye.nativeapp.ui.components.Panel
import com.taurotechnologies.godseye.nativeapp.ui.components.PanelRow
import com.taurotechnologies.godseye.nativeapp.ui.components.PrimaryButton
import com.taurotechnologies.godseye.nativeapp.ui.components.SectionLabel
import com.taurotechnologies.godseye.nativeapp.ui.components.SelectChip
import com.taurotechnologies.godseye.nativeapp.ui.components.Tag
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors
import com.taurotechnologies.godseye.nativeapp.ui.theme.SuvidhaColors
import java.text.DateFormat
import java.util.Date

/** Header bell with the unread count. Opens the alert list. */
@Composable
fun AlertsBell(unread: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSuvidhaColors.current
    IconButton(onClick = onClick, modifier = modifier) {
        BadgedBox(badge = {
            if (unread > 0) Badge(containerColor = colors.danger) { Text(if (unread > 99) "99+" else unread.toString()) }
        }) {
            Icon(Icons.Filled.Notifications, contentDescription = "Alerts, $unread unread", tint = colors.ink)
        }
    }
}

/**
 * Full-screen alert pages (list, then detail). They are overlays on the screen stack: System Back closes the
 * newest page first via [AlertsViewModel.back], and a screen's own back handler runs only when none is open.
 */
@Composable
fun AlertsOverlay(
    alerts: AlertsViewModel,
    isPanelKnown: (String) -> Boolean,
    onOpenPanel: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val stack by alerts.overlays.stack.collectAsState()
    val top: NativeOverlay? = stack.lastOrNull()
    BackHandler(enabled = top != null) { alerts.back() }
    if (top == null) return
    val colors = LocalSuvidhaColors.current
    Surface(modifier = Modifier.fillMaxSize(), color = colors.canvas) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                IconButton(onClick = { alerts.closeOverlay(top) }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.ink)
                }
                Text(
                    if (top == NativeOverlay.ALERT_DETAIL) "Alert" else "Alerts",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.ink,
                )
            }
            when (top) {
                NativeOverlay.ALERT_LIST -> AlertList(alerts, onRequestPermission, onOpenNotificationSettings)
                NativeOverlay.ALERT_DETAIL -> AlertDetail(alerts, isPanelKnown, onOpenPanel)
            }
        }
    }
}

@Composable
private fun AlertList(
    alerts: AlertsViewModel,
    onRequestPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalSuvidhaColors.current
    val inbox by AlertInboxStore.state.collectAsState()
    val permission by alerts.permission.collectAsState()
    var unreadOnly by rememberSaveable { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val visible = AlertInboxRules.visible(inbox, now).filter { !unreadOnly || it.readAtMillis == null }
    val unread = inbox.unreadCount(now)
    val hasRead = AlertInboxRules.visible(inbox, now).any { it.readAtMillis != null }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { PermissionCard(permission, onRequestPermission, onOpenNotificationSettings) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SelectChip("All", isSelected = !unreadOnly, onClick = { unreadOnly = false })
                SelectChip("Unread", isSelected = unreadOnly, onClick = { unreadOnly = true })
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { AlertInboxStore.markAllRead(context, now) }, enabled = unread > 0) { Text("Mark all read") }
                TextButton(onClick = { AlertInboxStore.clearRead(context) }, enabled = hasRead) { Text("Clear read") }
            }
        }
        if (visible.isEmpty()) {
            item {
                HelpText(if (inbox.items.isEmpty()) "All clear. New alerts will appear here." else "No alerts match this filter.")
            }
        }
        items(visible, key = { it.id }) { alert ->
            EntryCard(isHighlighted = alert.readAtMillis == null, onClick = {
                AlertInboxStore.markRead(context, alert.id, now)
                alerts.openDetail(alert.copy(readAtMillis = alert.readAtMillis ?: now))
            }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag(alert.severity.wire, severityColor(alert.severity, colors))
                    Text(alert.category.label(), fontSize = 11.sp, color = colors.muted)
                }
                Text(alert.title, fontSize = 15.sp, fontWeight = if (alert.readAtMillis == null) FontWeight.Bold else FontWeight.Medium, color = colors.ink)
                if (alert.body.isNotBlank()) Text(alert.body, fontSize = 13.sp, color = colors.muted, maxLines = 3)
                Text(formatAlertTime(alert.createdAtMillis), fontSize = 11.sp, color = colors.muted)
            }
        }
        item {
            HelpText("Stored on this device for 7 days. Opening the list does not mark alerts as read.")
        }
    }
}

@Composable
private fun AlertDetail(
    alerts: AlertsViewModel,
    isPanelKnown: (String) -> Boolean,
    onOpenPanel: (String) -> Unit,
) {
    val context = LocalContext.current
    val selected by alerts.selected.collectAsState()
    val inbox by AlertInboxStore.state.collectAsState()
    val alert = selected ?: return
    val colors = LocalSuvidhaColors.current
    // Prefer the stored copy so read state stays in step with the list.
    val current = inbox.items.firstOrNull { it.id == alert.id } ?: alert
    val now = System.currentTimeMillis()
    val panelId = current.panelId?.takeIf { isPanelKnown(it) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Tag(current.severity.wire, severityColor(current.severity, colors))
            Text(current.category.label(), fontSize = 11.sp, color = colors.muted)
        }
        Text(current.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.ink)
        Text(formatAlertTime(current.createdAtMillis), fontSize = 12.sp, color = colors.muted)
        if (current.body.isNotBlank()) Text(current.body, fontSize = 15.sp, color = colors.ink)
        if (panelId != null) {
            PrimaryButton("Open panel", onClick = { onOpenPanel(panelId) })
        } else if (current.panelId != null) {
            HelpText("This alert's panel is not in your Explore catalog yet.")
        }
        if (current.readAtMillis == null) {
            OutlinedButton(onClick = {
                AlertInboxStore.markRead(context, current.id, now)
            }, modifier = Modifier.fillMaxWidth()) { Text("Mark read") }
        }
        OutlinedButton(onClick = {
            AlertInboxStore.dismiss(context, current.id)
            alerts.closeOverlay(NativeOverlay.ALERT_DETAIL)
        }, modifier = Modifier.fillMaxWidth()) { Text("Dismiss") }
    }
}

@Composable
private fun PermissionCard(
    permission: PushPermissionState,
    onRequestPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    Panel {
        PanelRow(divider = false) {
            SectionLabel("Notifications")
            when (permission) {
                PushPermissionState.NOT_ASKED -> {
                    Text("Allow notifications so important alerts can reach this device.", fontSize = 13.sp, color = colors.ink)
                    PrimaryButton("Allow notifications", onClick = onRequestPermission)
                }
                PushPermissionState.DENIED_RETRYABLE -> {
                    Text("Notifications are blocked. Allow them to receive alerts.", fontSize = 13.sp, color = colors.ink)
                    PrimaryButton("Allow notifications", onClick = onRequestPermission)
                }
                PushPermissionState.DENIED_PERMANENT -> {
                    Text("Notifications are blocked for God's Eye. Allow them in Android settings, then come back here.", fontSize = 13.sp, color = colors.ink)
                    PrimaryButton("Open notification settings", onClick = onOpenNotificationSettings)
                }
                PushPermissionState.GRANTED, PushPermissionState.NOT_REQUIRED -> {
                    HelpText("Notifications are allowed. Background push is not connected in this build, so alerts appear here while the app is open.")
                }
            }
        }
    }
}

private fun severityColor(severity: AlertSeverity, colors: SuvidhaColors) = when (severity) {
    AlertSeverity.CRITICAL -> colors.danger
    AlertSeverity.HIGH -> colors.accent
    AlertSeverity.MEDIUM -> colors.muted
}

private fun AlertCategory.label(): String = when (this) {
    AlertCategory.NEWS -> "Breaking news"
    AlertCategory.SECURITY -> "Security"
    AlertCategory.MARKETS -> "Markets"
    AlertCategory.DISASTERS -> "Disasters"
    AlertCategory.SYSTEM -> "System"
}

private fun formatAlertTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
