package com.just_sri.suvidha.radar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.radar.AdminState
import com.just_sri.suvidha.radar.AdminTab
import com.just_sri.suvidha.radar.AppViewModel
import com.just_sri.suvidha.radar.data.Incident
import com.just_sri.suvidha.radar.ui.components.AppSheet
import com.just_sri.suvidha.radar.ui.components.ConfirmSheet
import com.just_sri.suvidha.radar.ui.components.EntryCard
import com.just_sri.suvidha.radar.ui.components.HelpText
import com.just_sri.suvidha.radar.ui.components.PrimaryButton
import com.just_sri.suvidha.radar.ui.components.SectionLabel
import com.just_sri.suvidha.radar.ui.components.SelectChip
import com.just_sri.suvidha.radar.ui.components.Tag
import com.just_sri.suvidha.radar.ui.theme.LocalSuvidhaColors
import kotlinx.serialization.json.Json

private val prettyJson: Json = Json { prettyPrint = true }

@Composable
fun AdminScreen(state: AdminState, incidents: List<Incident>, model: AppViewModel) {
    val colors = LocalSuvidhaColors.current
    BackHandler(onBack = model::closeAdmin)
    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = model::closeAdmin) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to radar", tint = colors.ink)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Suvidha Admin Console", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.ink)
                Text(if (state.isSignedIn) "Signed in" else "Sign in to manage stories", fontSize = 11.sp, color = colors.muted)
            }
        }
        if (state.isSignedIn) Dashboard(state, incidents, model) else LoginForm(state, model)
    }
    state.deleteTarget?.let { target ->
        ConfirmSheet(
            title = "Delete this story?",
            body = "\"${target.title}\" will be removed for good.",
            confirmText = "Delete",
            isBusy = state.isBusy,
            onConfirm = model::confirmDelete,
            onDismiss = { model.askDelete(null) },
        )
    }
    state.jsonTarget?.let { target -> JsonSheet(target, onDismiss = { model.showJson(null) }) }
}

@Composable
private fun LoginForm(state: AdminState, model: AppViewModel) {
    val colors = LocalSuvidhaColors.current
    var password: String by rememberSaveable { mutableStateOf("") }
    var isVisible: Boolean by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.green, modifier = Modifier.size(16.dp))
            HelpText("Your password is encrypted before it is sent.")
        }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { model.signIn(password) }),
            trailingIcon = {
                IconButton(onClick = { isVisible = !isVisible }) {
                    Icon(if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = if (isVisible) "Hide password" else "Show password")
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        state.loginError?.let { Text(it, color = colors.danger, fontSize = 13.sp) }
        PrimaryButton(
            text = if (state.isBusy) "Signing in…" else "Sign in",
            onClick = { model.signIn(password) },
            isBusy = state.isBusy,
            isEnabled = password.isNotBlank(),
        )
    }
}

@Composable
private fun Dashboard(state: AdminState, incidents: List<Incident>, model: AppViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp)) {
            for (tab in AdminTab.entries) {
                val label: String = if (tab == AdminTab.RECORDS) "${tab.label} (${incidents.size})" else tab.label
                SelectChip(label, isSelected = state.tab == tab, onClick = { model.setAdminTab(tab) })
            }
        }
        when (state.tab) {
            AdminTab.ADD_URL -> ImportUrlTab(state, model)
            AdminTab.RECORDS -> RecordsTab(state, incidents, model)
            AdminTab.PIPELINE -> PipelineTab(state, model)
        }
    }
}

@Composable
private fun ImportUrlTab(state: AdminState, model: AppViewModel) {
    val colors = LocalSuvidhaColors.current
    var url: String by rememberSaveable { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Add a story")
        HelpText("Paste a news article link.")
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Article link") },
            placeholder = { Text("https://…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { model.importUrl(url) { ok -> if (ok) url = "" } }),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = if (state.isBusy) "Adding…" else "Add story",
            onClick = { model.importUrl(url) { ok -> if (ok) url = "" } },
            isBusy = state.isBusy,
            isEnabled = url.isNotBlank(),
        )
    }
}

@Composable
private fun RecordsTab(state: AdminState, incidents: List<Incident>, model: AppViewModel) {
    val colors = LocalSuvidhaColors.current
    val query: String = state.recordFilter.trim().lowercase()
    val rows: List<Incident> = if (query.isEmpty()) incidents else incidents.filter {
        it.title.lowercase().contains(query) || it.locationName.lowercase().contains(query) ||
            it.category.lowercase().contains(query) || it.id.lowercase().contains(query)
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = state.recordFilter,
            onValueChange = model::setRecordFilter,
            singleLine = true,
            placeholder = { Text("Search records") },
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (rows.isEmpty()) {
            Text(if (incidents.isEmpty()) "No stories saved yet." else "No matches.", color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 24.dp))
            return
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.navigationBarsPadding()) {
            items(rows, key = { it.id }) { incident -> RecordRow(incident, model) }
        }
    }
}

@Composable
private fun RecordRow(incident: Incident, model: AppViewModel) {
    val colors = LocalSuvidhaColors.current
    EntryCard {
        Text(incident.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.ink, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Tag(incident.category, colors.green)
            Text("${incident.sources.size} link(s)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = colors.muted)
        }
        Text("${incident.locationName} (${incident.lat}, ${incident.lng})", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = colors.muted)
        Text(incident.id, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { model.showJson(incident) }) {
                Icon(Icons.Filled.Code, contentDescription = null, tint = colors.confirm, modifier = Modifier.size(16.dp))
                Text(" JSON", color = colors.confirm)
            }
            TextButton(onClick = { model.askDelete(incident) }) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = colors.danger, modifier = Modifier.size(16.dp))
                Text(" Delete", color = colors.danger)
            }
        }
    }
}

@Composable
private fun PipelineTab(state: AdminState, model: AppViewModel) {
    val colors = LocalSuvidhaColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Find new stories", colors.confirm)
        HelpText("AI agents search recent news and check the facts.")
        OutlinedTextField(
            value = state.pipelinePrompt,
            onValueChange = model::setPipelinePrompt,
            label = { Text("What to look for") },
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = if (state.isBusy) "Starting…" else "Find stories",
            onClick = model::runPipeline,
            isBusy = state.isBusy,
            isEnabled = state.pipelinePrompt.isNotBlank(),
        )
    }
}

@Composable
private fun JsonSheet(incident: Incident, onDismiss: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    AppSheet(onDismiss = onDismiss) {
        Text("JSON record", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.ink)
        Text(incident.id, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = colors.muted)
        Text(
            text = prettyJson.encodeToString(Incident.serializer(), incident),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = colors.green,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .background(colors.surfaceMuted, RoundedCornerShape(12.dp))
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LocalSuvidhaColors.current.green,
    unfocusedBorderColor = LocalSuvidhaColors.current.borderLight,
    focusedLabelColor = LocalSuvidhaColors.current.green,
    cursorColor = LocalSuvidhaColors.current.green,
)
