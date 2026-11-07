package com.taurotechnologies.godseye.nativeapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taurotechnologies.godseye.nativeapp.data.explorePanel
import com.taurotechnologies.godseye.nativeapp.ui.components.ExplorePanelData
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors

/** One explore panel opened on its own (deep link or alert): a back bar over the shared [ExplorePanelData]. */
@Composable
fun ExplorePanelScreen(
    panelId: String,
    saved: Boolean,
    onBack: () -> Unit,
    onToggleSave: (String) -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    BackHandler(onBack = onBack)
    val panel = explorePanel(panelId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.ink)
            }
            Text(
                text = panel?.title ?: "Explore",
                color = colors.ink,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }
        ExplorePanelData(panelId = panelId, saved = saved, onToggleSave = onToggleSave)
    }
}
