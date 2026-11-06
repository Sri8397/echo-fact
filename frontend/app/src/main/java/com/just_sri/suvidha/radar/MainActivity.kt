package com.just_sri.suvidha.radar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.just_sri.suvidha.radar.ui.components.AppToast
import com.just_sri.suvidha.radar.ui.screens.AdminScreen
import com.just_sri.suvidha.radar.ui.screens.DetailScreen
import com.just_sri.suvidha.radar.ui.screens.RadarScreen
import com.just_sri.suvidha.radar.ui.screens.SettingsScreen
import com.just_sri.suvidha.radar.ui.screens.StorySectionScreen
import com.just_sri.suvidha.radar.ui.screens.ThemeScreen
import androidx.compose.foundation.isSystemInDarkTheme
import com.just_sri.suvidha.radar.ui.theme.ColorStyle
import com.just_sri.suvidha.radar.ui.theme.SuvidhaRadarTheme

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme: ThemeChoice by model.theme.collectAsStateWithLifecycle()
            val style: ColorStyle by model.colorStyle.collectAsStateWithLifecycle()
            val isDark: Boolean = when (theme) {
                ThemeChoice.SYSTEM -> isSystemInDarkTheme()
                ThemeChoice.LIGHT -> false
                ThemeChoice.DARK -> true
            }
            SuvidhaRadarTheme(darkTheme = isDark, style = style) {
                SuvidhaRadarApp(model)
            }
        }
    }
}

/** Routes between the radar, a story's detail page and the admin console. */
@Composable
fun SuvidhaRadarApp(model: AppViewModel) {
    val screen: Screen by model.screen.collectAsStateWithLifecycle()
    val radar: RadarState by model.radar.collectAsStateWithLifecycle()
    val admin: AdminState by model.admin.collectAsStateWithLifecycle()
    val toast: Toast? by model.toast.collectAsStateWithLifecycle()
    val theme: ThemeChoice by model.theme.collectAsStateWithLifecycle()
    val radarStateHolder = rememberSaveableStateHolder()
    Box(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            Screen.RADAR -> radarStateHolder.SaveableStateProvider(Screen.RADAR.name) { RadarScreen(radar, model) }
            Screen.DETAIL -> DetailScreen(radar.selected, onBack = model::closeDetail, onOpenSection = model::openSection)
            Screen.SECTION -> StorySectionScreen(radar.selected, model.section.collectAsStateWithLifecycle().value, onBack = model::closeSection)
            Screen.SETTINGS -> SettingsScreen(admin, theme, radar.incidents.size, radar.isLive, model)
            Screen.THEME -> ThemeScreen(theme, model.colorStyle.collectAsStateWithLifecycle().value, model)
            // Admin manages database rows only, never the bundled offline sample.
            Screen.ADMIN -> AdminScreen(admin, if (radar.isLive) radar.incidents else emptyList(), model)
        }
        AppToast(toast, onDismiss = model::dismissToast, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}
