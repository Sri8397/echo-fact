package com.taurotechnologies.godseye.nativeapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.taurotechnologies.godseye.nativeapp.data.RadarFeedFilter
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taurotechnologies.godseye.nativeapp.ui.components.AppSheet
import com.taurotechnologies.godseye.nativeapp.ui.components.HelpText
import com.taurotechnologies.godseye.nativeapp.ui.components.Panel
import com.taurotechnologies.godseye.nativeapp.ui.components.PanelRow
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors

/**
 * Full-screen Layers page opened by the top bar's three-line button and the More chip. The system back
 * gesture is handled by the home screen's BackHandler; the arrow here does the same.
 */
@Composable
fun HomeMenuScreen(
    layers: HomeLayersState,
    onSelectFilter: (RadarFeedFilter) -> Unit,
    onTogglePin: (RadarFeedFilter) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.canvas)
            // Swallow taps on empty space so they do not reach the map underneath.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            .statusBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to map", tint = colors.ink)
            }
            SheetTitle("Layers")
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            HomeLayerList(layers, onSelectFilter, onTogglePin)
        }
    }
}

/** Bottom popup opened by the profile button. */
@Composable
fun HomeProfileScreen(unread: Int, onBack: () -> Unit, onOpenAlerts: () -> Unit, onOpenSettings: () -> Unit) {
    AppSheet(onDismiss = onBack, containerColor = LocalSuvidhaColors.current.cardSurface) {
        SheetTitle("Profile")
        Panel {
            NavRow(Icons.Outlined.Notifications, if (unread > 0) "Alerts ($unread new)" else "Alerts", "Your alert inbox", true, onOpenAlerts)
            NavRow(Icons.Outlined.Settings, "Account and settings", "Sign-in, preferences and sign out", false, onOpenSettings)
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(text, color = LocalSuvidhaColors.current.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, divider: Boolean, onClick: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    PanelRow(
        divider = divider,
        onClick = onClick,
        leading = { Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(24.dp)) },
    ) {
        Text(title, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        HelpText(subtitle)
    }
}
