package com.just_sri.suvidha.radar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.radar.StorySection
import com.just_sri.suvidha.radar.data.Incident
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.just_sri.suvidha.radar.data.Source
import com.just_sri.suvidha.radar.ui.components.EntryCard
import com.just_sri.suvidha.radar.ui.components.Panel
import com.just_sri.suvidha.radar.ui.components.PanelRow
import com.just_sri.suvidha.radar.ui.components.SectionLabel
import com.just_sri.suvidha.radar.ui.components.Tag
import com.just_sri.suvidha.radar.ui.components.isWebUrl
import com.just_sri.suvidha.radar.ui.components.openWebUrl
import com.just_sri.suvidha.radar.ui.theme.LocalSuvidhaColors

@Composable
fun DetailScreen(incident: Incident?, onBack: () -> Unit, onOpenSection: (StorySection) -> Unit) {
    val colors = LocalSuvidhaColors.current
    BackHandler(onBack = onBack)
    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to stream", tint = colors.ink)
            }
            Text("Back to stream", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.ink)
        }
        if (incident == null) {
            Text("This story is no longer available.", color = colors.muted, modifier = Modifier.padding(20.dp))
            return
        }
        DetailBody(incident, onOpenSection)
    }
}

@Composable
private fun DetailBody(incident: Incident, onOpenSection: (StorySection) -> Unit) {
    val colors = LocalSuvidhaColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Tag(incident.category, colors.green)
            Text(incident.date, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = colors.muted)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
            Text(" ${incident.locationName}", fontSize = 12.sp, color = colors.muted)
        }
        Text(incident.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.ink, lineHeight = 26.sp)
        if (incident.summary.isNotBlank()) {
            EntryCard(onClick = { onOpenSection(StorySection.HAPPENED) }) {
                MoreLabel("When it happened", colors.muted)
                Text(incident.summary, fontSize = 14.sp, color = colors.ink, lineHeight = 20.sp)
            }
        }
        if (incident.currentReality.isNotBlank()) {
            EntryCard(isHighlighted = true, onClick = { onOpenSection(StorySection.TODAY) }) {
                MoreLabel("Where it stands today", colors.green)
                Text(incident.currentReality, fontSize = 14.sp, color = colors.ink, lineHeight = 20.sp)
            }
        }
        if (incident.hypeTimeline.isNotEmpty()) {
            MoreLabel("Event timeline", colors.confirm, onClick = { onOpenSection(StorySection.TIMELINE) })
            Panel {
                incident.hypeTimeline.forEachIndexed { index, step ->
                    PanelRow(divider = index < incident.hypeTimeline.lastIndex, onClick = { onOpenSection(StorySection.TIMELINE) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Tag(step.year, colors.confirm)
                            Text(step.phase.uppercase(), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.muted)
                        }
                        Text(step.event, fontSize = 13.sp, color = colors.ink)
                    }
                }
            }
        }
        BulletSection("Key data points", incident.verifiedFacts, colors.green, isQuestion = false, onClick = { onOpenSection(StorySection.FACTS) })
        BulletSection("Questions to explore", incident.keyQuestions, colors.accent, isQuestion = true, onClick = { onOpenSection(StorySection.QUESTIONS) })
        SectionLabel("News sources (${incident.sources.size})")
        if (incident.sources.isEmpty()) {
            Text("No direct link available.", fontSize = 12.sp, color = colors.muted)
        }
        if (incident.sources.isNotEmpty()) {
            Panel {
                incident.sources.forEachIndexed { index, source -> SourceRow(source, divider = index < incident.sources.lastIndex) }
            }
        }
        Text("Suvidha — Read the sources. Form your own view.", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = colors.muted, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun BulletSection(title: String, items: List<String>, tint: Color, isQuestion: Boolean, onClick: () -> Unit) {
    if (items.isEmpty()) return
    val colors = LocalSuvidhaColors.current
    MoreLabel(title, tint, onClick = onClick)
    Panel {
        items.forEachIndexed { index, item ->
            PanelRow(
                divider = index < items.lastIndex,
                onClick = onClick,
                leading = { Icon(if (isQuestion) Icons.Filled.HelpOutline else Icons.Filled.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(16.dp)) },
            ) {
                Text(item, fontSize = 13.sp, color = colors.ink, lineHeight = 19.sp)
            }
        }
    }
}

/** Opens only http(s) links; other schemes show as plain text. */
@Composable
private fun SourceRow(source: Source, divider: Boolean) {
    val colors = LocalSuvidhaColors.current
    val context = LocalContext.current
    val canOpen: Boolean = isWebUrl(source.url)
    PanelRow(
        divider = divider,
        onClick = if (canOpen) ({ openWebUrl(context, source.url) }) else null,
        trailing = if (canOpen) ({ Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open source", tint = colors.green, modifier = Modifier.padding(top = 2.dp).size(16.dp)) }) else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(source.publisher, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.green)
            if (source.date.isNotBlank()) Text(source.date, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.muted)
        }
        Text(source.title, fontSize = 13.sp, color = colors.ink)
    }
}

/** Section label with a "More" chevron, showing the section opens its own page. */
@Composable
private fun MoreLabel(title: String, tint: Color, onClick: (() -> Unit)? = null) {
    val colors = LocalSuvidhaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(modifier = Modifier.weight(1f)) { SectionLabel(title, tint) }
        Text("More", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.muted)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
    }
}
