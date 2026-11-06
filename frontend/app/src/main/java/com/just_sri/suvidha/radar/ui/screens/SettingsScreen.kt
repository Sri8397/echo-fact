package com.just_sri.suvidha.radar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.radar.AdminState
import com.just_sri.suvidha.radar.AdminTab
import com.just_sri.suvidha.radar.AppViewModel
import com.just_sri.suvidha.radar.BuildConfig
import com.just_sri.suvidha.radar.RADAR_PROTOTYPE
import com.just_sri.suvidha.radar.ThemeChoice
import com.just_sri.suvidha.radar.ui.components.Panel
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.just_sri.suvidha.radar.ui.theme.ColorStyle
import com.just_sri.suvidha.radar.ui.theme.LocalSuvidhaColors
import com.just_sri.suvidha.radar.ui.theme.StylePalette
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults

private val IconTileShape: RoundedCornerShape = RoundedCornerShape(12.dp)

/**
 * Settings, laid out like Suvidha's Profile page: sections of panels with rows.
 * The Admin section only appears for a signed-in super admin; otherwise one sign-in row is shown.
 */
@Composable
fun SettingsScreen(admin: AdminState, theme: ThemeChoice, storyCount: Int, isLive: Boolean, model: AppViewModel) {
    val colors = LocalSuvidhaColors.current
    val isGlobe: Boolean by model.isGlobe.collectAsStateWithLifecycle()
    val colorStyle: ColorStyle by model.colorStyle.collectAsStateWithLifecycle()
    BackHandler(onBack = model::closeSettings)
    SettingsPage("Settings", onBack = model::closeSettings) {
        item { SectionHeading("Appearance") }
        item {
            Panel {
                SettingsRow("Theme", "${theme.title} · ${colorStyle.title}", Icons.Outlined.Palette, onClick = model::openTheme, divider = !RADAR_PROTOTYPE)
                if (!RADAR_PROTOTYPE) {
                    SettingsRow(
                        title = "3D globe",
                        subtitle = if (isGlobe) "Map shows a 3D globe" else "Map shows a flat 2D view",
                        icon = Icons.Outlined.Public,
                        onClick = { model.setGlobe(!isGlobe) },
                        trailing = {
                            Switch(
                                checked = isGlobe,
                                onCheckedChange = { model.setGlobe(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.onGreen,
                                    checkedTrackColor = colors.green,
                                    uncheckedThumbColor = colors.cardSurface,
                                    uncheckedTrackColor = colors.borderLight,
                                    uncheckedBorderColor = Color.Transparent,
                                    checkedBorderColor = Color.Transparent,
                                ),
                            )
                        },
                        showChevron = false,
                    )
                }
            }
        }
        if (RADAR_PROTOTYPE) {
            item { SectionHeading("Data") }
            item {
                Panel {
                    SettingsRow("Prototype mode", "Bundled signals and sample stories", Icons.Outlined.CloudQueue, showChevron = false)
                }
            }
        } else if (admin.isSignedIn) {
            item { SectionHeading("Admin", detail = "Super admin") }
            item {
                Panel {
                    SettingsRow("Add a story", "Paste a news article link", Icons.Outlined.AddLink, onClick = { model.openAdmin(AdminTab.ADD_URL) }, divider = true)
                    SettingsRow("Stories", if (isLive) "$storyCount saved" else "None saved yet", Icons.Outlined.ListAlt, onClick = { model.openAdmin(AdminTab.RECORDS) }, divider = true)
                    SettingsRow("Find new stories", "Run the AI agents", Icons.Outlined.AutoAwesome, onClick = { model.openAdmin(AdminTab.PIPELINE) })
                }
            }
        } else {
            item { SectionHeading("Account") }
            item {
                Panel {
                    SettingsRow("Admin sign-in", "For super admins only", Icons.Outlined.AdminPanelSettings, onClick = { model.openAdmin() })
                }
            }
        }
        item { SectionHeading("About") }
        item {
            Panel {
                SettingsRow("Version", null, Icons.Outlined.Info, trailingText = BuildConfig.VERSION_NAME, showChevron = false, divider = !RADAR_PROTOTYPE)
                if (!RADAR_PROTOTYPE) SettingsRow("Server", if (isLive) "Connected" else "Offline, showing sample stories", Icons.Outlined.CloudQueue, trailingText = serverLabel(model.apiBaseUrl), showChevron = false)
            }
        }
        if (!RADAR_PROTOTYPE && admin.isSignedIn) {
            item {
                Panel {
                    SettingsRow("Sign out", null, Icons.AutoMirrored.Outlined.Logout, onClick = model::signOut, tint = colors.danger, showChevron = false)
                }
            }
        }
    }
}

/**
 * Full-page theme choice, like Suvidha's Theme page: appearance (returns to Settings when picked)
 * and colour style (applies at once and stays here so the change can be seen).
 */
@Composable
fun ThemeScreen(theme: ThemeChoice, style: ColorStyle, model: AppViewModel) {
    val colors = LocalSuvidhaColors.current
    BackHandler(onBack = model::openSettings)
    SettingsPage("Theme", onBack = model::openSettings) {
        item { Text("Choose your preferred appearance.", fontSize = 13.sp, lineHeight = 20.sp, color = colors.muted) }
        item {
            Panel {
                ThemeChoice.entries.forEachIndexed { index, choice ->
                    SettingsRow(
                        title = choice.title,
                        subtitle = choice.subtitle,
                        icon = null,
                        onClick = { model.setTheme(choice) },
                        trailing = { StyleRadio(choice == theme) { model.setTheme(choice) } },
                        showChevron = false,
                        divider = index < ThemeChoice.entries.lastIndex,
                    )
                }
            }
        }
        item { SectionHeading("Color style") }
        item {
            Panel {
                ColorStyle.entries.forEachIndexed { index, option ->
                    SettingsRow(
                        title = option.title,
                        subtitle = option.description,
                        icon = null,
                        leading = { StyleSwatches(option.palette) },
                        onClick = { model.setColorStyle(option) },
                        trailing = { StyleRadio(option == style) { model.setColorStyle(option) } },
                        showChevron = false,
                        divider = index < ColorStyle.entries.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleRadio(isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = colors.green, unselectedColor = colors.muted))
}

private val SWATCH_SIZE = 24.dp
private val SWATCH_STEP = 14.dp
private val SWATCH_STRIP_WIDTH = 52.dp

/** Three overlapping dots: the style's primary, secondary and accent colours (as in Suvidha). */
@Composable
private fun StyleSwatches(palette: StylePalette) {
    Box(Modifier.size(width = SWATCH_STRIP_WIDTH, height = SWATCH_SIZE)) {
        listOf(palette.primary.base, palette.secondary.base, palette.accent.base).forEachIndexed { index, color ->
            Box(
                Modifier
                    .offset(x = SWATCH_STEP * index)
                    .size(SWATCH_SIZE)
                    .border(2.dp, palette.neutrals.background, CircleShape)
                    .padding(2.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

private fun serverLabel(url: String): String = url.removePrefix("https://").removePrefix("http://").trimEnd('/')

@Composable
private fun SettingsPage(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    val colors = LocalSuvidhaColors.current
    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.green, modifier = Modifier.size(24.dp))
            }
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = colors.ink, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = colors.borderLight)
        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionHeading(title: String, detail: String? = null) {
    val colors = LocalSuvidhaColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.ink, modifier = Modifier.weight(1f))
        if (detail != null) {
            Text(
                detail,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.green,
                modifier = Modifier.background(colors.mint, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}


@Composable
private fun SettingsRow(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    tint: Color = LocalSuvidhaColors.current.green,
    isSelected: Boolean = false,
    showChevron: Boolean = true,
    divider: Boolean = false,
) {
    val colors = LocalSuvidhaColors.current
    val isDanger: Boolean = tint == colors.danger
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leading?.invoke()
            if (icon != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).background(if (isDanger) colors.danger.copy(alpha = 0.12f) else colors.mint, IconTileShape),
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (isDanger) colors.danger else colors.ink)
                if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trailingText != null) Text(trailingText, fontSize = 13.sp, color = colors.muted, maxLines = 1)
            if (isSelected) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = colors.green, modifier = Modifier.size(20.dp))
            trailing?.invoke()
            if (showChevron && onClick != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.muted)
        }
        if (divider) HorizontalDivider(color = colors.borderLight, modifier = Modifier.padding(start = if (icon != null) 68.dp else 16.dp))
    }
}
