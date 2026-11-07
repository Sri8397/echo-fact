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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taurotechnologies.godseye.nativeapp.data.ExploreCatalogStore
import com.taurotechnologies.godseye.nativeapp.data.ExplorePanel
import com.taurotechnologies.godseye.nativeapp.ui.components.ChipRow
import com.taurotechnologies.godseye.nativeapp.ui.components.ExplorePanelData
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors

/**
 * Explore hub, laid out like the radar: a chip list of categories, a chip list of that category's panels,
 * and the selected panel's data (summary, chart, rows) right below. No separate list-then-open step.
 */
@Composable
fun ExploreScreen(
    saved: Set<String>,
    isVisible: (panelId: String, category: String) -> Boolean = { _, _ -> true },
    onBack: () -> Unit,
    onToggleSave: (String) -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    BackHandler(onBack = onBack)
    val catalog by ExploreCatalogStore.catalog.collectAsState()
    val shownPanels = catalog.visiblePanels.filter { isVisible(it.id, it.category) }
    val categories = catalog.visibleCategories.filter { category -> shownPanels.any { it.category == category } }
    var chosenCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var chosenPanelId by rememberSaveable { mutableStateOf<String?>(null) }
    // A catalog refresh can remove the chosen category or panel; fall back to the first one.
    val selectedCategory = chosenCategory?.takeIf { it in categories } ?: categories.firstOrNull()
    val panels: List<ExplorePanel> = shownPanels.filter { it.category == selectedCategory }
    val selectedPanel: ExplorePanel? = panels.firstOrNull { it.id == chosenPanelId } ?: panels.firstOrNull()

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
            Text(text = "Explore", color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        ChipRow(
            items = categories,
            selected = selectedCategory,
            label = { it },
            onSelect = { chosenCategory = it; chosenPanelId = null },
        )
        ChipRow(
            items = panels,
            selected = selectedPanel,
            label = { it.title },
            onSelect = { chosenPanelId = it.id },
        )

        if (selectedPanel != null) {
            ExplorePanelData(
                panelId = selectedPanel.id,
                saved = selectedPanel.id in saved,
                onToggleSave = onToggleSave,
            )
        } else {
            Text(text = "No Explore panels are loaded yet.", color = colors.muted, modifier = Modifier.padding(16.dp))
        }
    }
}
