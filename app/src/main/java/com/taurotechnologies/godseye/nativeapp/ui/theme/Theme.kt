package com.taurotechnologies.godseye.nativeapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/*
 * Suvidha colour styles, with the same values and light/dark mixing rules as the Suvidha Android app
 * (ui/theme/ColorStyle.kt). Screens read LocalSuvidhaColors; no colours outside the chosen style are used.
 */

@Immutable
data class Tone(val base: Color, val light: Color, val dark: Color)

@Immutable
data class Neutrals(val background: Color, val text: Color, val textSecondary: Color)

@Immutable
data class StylePalette(val primary: Tone, val secondary: Tone, val accent: Tone, val confirm: Tone, val neutrals: Neutrals)

/** Failures and destructive actions. No palette has a red, so one fixed red is shared by every style. */
private val FAILURE_RED_LIGHT: Color = Color(0xFFB42318)
private val FAILURE_RED_DARK: Color = Color(0xFFF04438)

enum class ColorStyle(val title: String, val description: String, val palette: StylePalette) {
    EMERALD_LEDGER(
        "Emerald Ledger",
        "Deep emerald greens with warm neutrals.",
        StylePalette(
            primary = Tone(Color(0xFF1B7A5A), Color(0xFF2EAD80), Color(0xFF0F4F3A)),
            secondary = Tone(Color(0xFF2D6A8F), Color(0xFF4A9BC2), Color(0xFF1A3F57)),
            accent = Tone(Color(0xFFF4A623), Color(0xFFF7C46A), Color(0xFFC07D0A)),
            confirm = Tone(Color(0xFF2D6A8F), Color(0xFF4A9BC2), Color(0xFF1A3F57)),
            neutrals = Neutrals(Color(0xFFF5F7F5), Color(0xFF1A2420), Color(0xFF5A6E68)),
        ),
    ),
    SLATE_VIOLET(
        "Slate & Violet",
        "Deep slate with a soft violet.",
        StylePalette(
            primary = Tone(Color(0xFF4C5F8A), Color(0xFF7389BE), Color(0xFF2A3550)),
            secondary = Tone(Color(0xFF6B4E9B), Color(0xFF9A7DC8), Color(0xFF2A3550)),
            accent = Tone(Color(0xFF3DBFA8), Color(0xFF72D9C9), Color(0xFF227A6D)),
            confirm = Tone(Color(0xFF6B4E9B), Color(0xFF9A7DC8), Color(0xFF2A3550)),
            neutrals = Neutrals(Color(0xFFF4F5F9), Color(0xFF1C1E2E), Color(0xFF636B8A)),
        ),
    ),
    WARM_COPPER(
        "Warm Copper",
        "Terracotta and copper on soft off-white.",
        StylePalette(
            primary = Tone(Color(0xFFC0633A), Color(0xFFE08A65), Color(0xFF7A3218)),
            secondary = Tone(Color(0xFF8A7355), Color(0xFFB5A07D), Color(0xFF7A3218)),
            accent = Tone(Color(0xFF4A90A4), Color(0xFF79BDD1), Color(0xFF2E6070)),
            confirm = Tone(Color(0xFF4A90A4), Color(0xFF79BDD1), Color(0xFF2E6070)),
            neutrals = Neutrals(Color(0xFFFBF7F2), Color(0xFF231812), Color(0xFF7A6558)),
        ),
    );

    companion object {
        val DEFAULT: ColorStyle = EMERALD_LEDGER

        fun from(value: String?): ColorStyle = entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

@Immutable
data class SuvidhaColors(
    val canvas: Color,
    val cardSurface: Color,
    val surfaceMuted: Color,
    val ink: Color,
    val muted: Color,
    val green: Color,
    val onGreen: Color,
    val mint: Color,
    val borderLight: Color,
    val accent: Color,
    val confirm: Color,
    val danger: Color,
    val isDark: Boolean,
)

/** [fraction] of [over] mixed into [base]: tints stay inside the palette instead of adding new colours. */
private fun mix(base: Color, over: Color, fraction: Float): Color = lerp(base, over, fraction)

fun StylePalette.lightColors(): SuvidhaColors = SuvidhaColors(
    canvas = neutrals.background,
    cardSurface = neutrals.background,
    surfaceMuted = mix(neutrals.background, primary.base, 0.06f),
    ink = neutrals.text,
    muted = neutrals.textSecondary,
    green = primary.base,
    onGreen = neutrals.background,
    mint = mix(neutrals.background, primary.light, 0.18f),
    borderLight = mix(neutrals.background, neutrals.textSecondary, 0.18f),
    accent = accent.base,
    confirm = confirm.base,
    danger = FAILURE_RED_LIGHT,
    isDark = false,
)

/** Dark mode swaps each style's text and background, so it adds no colours. */
fun StylePalette.darkColors(): SuvidhaColors = SuvidhaColors(
    canvas = neutrals.text,
    cardSurface = mix(neutrals.text, neutrals.textSecondary, 0.15f),
    surfaceMuted = mix(neutrals.text, neutrals.textSecondary, 0.08f),
    ink = neutrals.background,
    muted = mix(neutrals.textSecondary, neutrals.background, 0.45f),
    green = primary.light,
    onGreen = neutrals.text,
    mint = mix(neutrals.text, primary.dark, 0.55f),
    borderLight = mix(neutrals.text, neutrals.textSecondary, 0.35f),
    accent = accent.light,
    confirm = confirm.light,
    danger = FAILURE_RED_DARK,
    isDark = true,
)

val LocalSuvidhaColors = staticCompositionLocalOf { ColorStyle.DEFAULT.palette.lightColors() }

/** Every Material role comes from the style, so Material's purple defaults never show. */
private fun SuvidhaColors.toColorScheme(): ColorScheme {
    val base: ColorScheme = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = green,
        onPrimary = onGreen,
        primaryContainer = mint,
        onPrimaryContainer = green,
        secondary = confirm,
        onSecondary = canvas,
        tertiary = accent,
        onTertiary = ink,
        background = canvas,
        onBackground = ink,
        surface = cardSurface,
        onSurface = ink,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = muted,
        surfaceContainer = cardSurface,
        surfaceContainerLow = cardSurface,
        surfaceContainerHigh = cardSurface,
        surfaceContainerHighest = surfaceMuted,
        surfaceContainerLowest = canvas,
        outline = borderLight,
        outlineVariant = borderLight,
        error = danger,
        onError = canvas,
        inverseSurface = ink,
        inverseOnSurface = canvas,
        secondaryContainer = mint,
        onSecondaryContainer = green,
    )
}

@Composable
fun SuvidhaRadarTheme(darkTheme: Boolean, style: ColorStyle = ColorStyle.DEFAULT, monoFont: Boolean = false, content: @Composable () -> Unit) {
    val colors: SuvidhaColors = if (darkTheme) style.palette.darkColors() else style.palette.lightColors()
    CompositionLocalProvider(LocalSuvidhaColors provides colors) {
        MaterialTheme(colorScheme = colors.toColorScheme(), typography = radarTypography(monoFont), content = content)
    }
}

private fun radarTypography(monoFont: Boolean): Typography {
    val base = Typography()
    if (!monoFont) return base
    val family = FontFamily.Monospace
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}
