package com.just_sri.suvidha.radar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.just_sri.suvidha.radar.data.Source
import com.just_sri.suvidha.radar.ui.components.EntryCard
import com.just_sri.suvidha.radar.ui.components.HelpText
import com.just_sri.suvidha.radar.ui.components.Panel
import com.just_sri.suvidha.radar.ui.components.PanelRow
import com.just_sri.suvidha.radar.ui.components.SectionLabel
import com.just_sri.suvidha.radar.ui.components.Tag
import com.just_sri.suvidha.radar.ui.components.isWebUrl
import com.just_sri.suvidha.radar.ui.components.openWebUrl
import com.just_sri.suvidha.radar.ui.theme.LocalSuvidhaColors

/** One section of a story on its own page, with the related details that do not fit on the story page. */
@Composable
fun StorySectionScreen(incident: Incident?, section: StorySection, onBack: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    BackHandler(onBack = onBack)
    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to story", tint = colors.ink)
            }
            Text(section.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.ink)
        }
        if (incident == null) {
            Text("This story is no longer available.", color = colors.muted, modifier = Modifier.padding(20.dp))
            return
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(incident.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.muted, lineHeight = 21.sp)
            when (section) {
                StorySection.HAPPENED -> HappenedBody(incident)
                StorySection.TODAY -> TodayBody(incident)
                StorySection.TIMELINE -> TimelineBody(incident)
                StorySection.FACTS -> ListBody(incident.verifiedFacts, colors.green, isQuestion = false, empty = "No data points recorded.")
                StorySection.QUESTIONS -> ListBody(incident.keyQuestions, colors.accent, isQuestion = true, empty = "No open questions recorded.")
            }
            SourcesByDate(incident)
        }
    }
}

@Composable
private fun HappenedBody(incident: Incident) {
    val colors = LocalSuvidhaColors.current
    Panel {
        FactRow("Date", incident.date.ifBlank { incident.year }, divider = true)
        FactRow("Place", incident.locationName, divider = true)
        FactRow("Coordinates", "%.4f, %.4f".format(incident.lat, incident.lng), divider = true)
        FactRow("Category", incident.category, divider = false)
    }
    if (incident.summary.isNotBlank()) {
        EntryCard {
            SectionLabel("What happened", colors.muted)
            Text(incident.summary, fontSize = 15.sp, color = colors.ink, lineHeight = 22.sp)
        }
    }
    val first: Source? = incident.sources.firstOrNull()
    if (first != null) {
        SectionLabel("First reported")
        Panel { SourceItem(first, divider = false) }
    }
}

@Composable
private fun TodayBody(incident: Incident) {
    val colors = LocalSuvidhaColors.current
    EntryCard(isHighlighted = true) {
        SectionLabel("Where it stands today")
        Text(incident.currentReality.ifBlank { "No update recorded yet." }, fontSize = 15.sp, color = colors.ink, lineHeight = 22.sp)
    }
    val latest: Source? = incident.sources.lastOrNull()
    if (latest != null) {
        SectionLabel("Latest coverage")
        Panel { SourceItem(latest, divider = false) }
    }
    if (incident.verifiedFacts.isNotEmpty()) {
        SectionLabel("What is confirmed", colors.green)
        ListBody(incident.verifiedFacts, colors.green, isQuestion = false, empty = "")
    }
    if (incident.keyQuestions.isNotEmpty()) {
        SectionLabel("Still open", colors.accent)
        ListBody(incident.keyQuestions, colors.accent, isQuestion = true, empty = "")
    }
}

@Composable
private fun TimelineBody(incident: Incident) {
    val colors = LocalSuvidhaColors.current
    if (incident.hypeTimeline.isEmpty()) {
        HelpText("No timeline recorded.")
        return
    }
    Panel {
        incident.hypeTimeline.forEachIndexed { index, step ->
            PanelRow(divider = index < incident.hypeTimeline.lastIndex) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(step.year, colors.confirm)
                    Text(step.phase.uppercase(), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.muted)
                }
                Text(step.event, fontSize = 14.sp, color = colors.ink, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ListBody(items: List<String>, tint: Color, isQuestion: Boolean, empty: String) {
    val colors = LocalSuvidhaColors.current
    if (items.isEmpty()) {
        if (empty.isNotEmpty()) HelpText(empty)
        return
    }
    Panel {
        items.forEachIndexed { index, item ->
            PanelRow(
                divider = index < items.lastIndex,
                leading = { Icon(if (isQuestion) Icons.Filled.HelpOutline else Icons.Filled.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(16.dp)) },
            ) {
                Text(item, fontSize = 14.sp, color = colors.ink, lineHeight = 20.sp)
            }
        }
    }
}

/** All sources, grouped by their date in the order they were reported. */
@Composable
private fun SourcesByDate(incident: Incident) {
    if (incident.sources.isEmpty()) return
    val groups: Map<String, List<Source>> = incident.sources.groupBy { it.date.ifBlank { incident.date.ifBlank { "Undated" } } }
    SectionLabel("Sources by date (${incident.sources.size})")
    for ((date, items) in groups) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tag(date, LocalSuvidhaColors.current.confirm)
            HelpText(if (items.size == 1) "1 article" else "${items.size} articles")
        }
        Panel { items.forEachIndexed { index, source -> SourceItem(source, divider = index < items.lastIndex) } }
    }
}

@Composable
private fun FactRow(label: String, value: String, divider: Boolean) {
    val colors = LocalSuvidhaColors.current
    PanelRow(divider = divider, trailing = { Text(value.ifBlank { "—" }, fontSize = 13.sp, color = colors.ink, fontWeight = FontWeight.SemiBold) }) {
        Text(label, fontSize = 13.sp, color = colors.muted)
    }
}

@Composable
private fun SourceItem(source: Source, divider: Boolean) {
    val colors = LocalSuvidhaColors.current
    val context = LocalContext.current
    val canOpen: Boolean = isWebUrl(source.url)
    PanelRow(
        divider = divider,
        onClick = if (canOpen) ({ openWebUrl(context, source.url) }) else null,
        trailing = if (canOpen) ({ Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open source", tint = colors.green, modifier = Modifier.padding(top = 2.dp).size(16.dp)) }) else null,
    ) {
        Text(source.publisher, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.green)
        Text(source.title, fontSize = 13.sp, color = colors.ink)
    }
}
