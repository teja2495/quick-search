package com.tk.quicksearch.shared.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Material 3 expressive palette tuned for the quick search UI.
 *
 * This file defines color tokens for both light and dark themes following Material Design 3
 * color system conventions. Colors are organized by theme variant and semantic role.
 */

// ============================================================================
// Shared Color Constants
// ============================================================================

private const val BACKGROUND_DARK = 0xFF000000L
private const val WHITE = 0xFFFFFFFFL

// ============================================================================
// App Brand / Accent Colors
// ============================================================================
//
// Default purple accent (used as fallback and for MONOCHROME theme).

/** Primary brand accent — used as Material `primary` in the light color scheme. */
val AppAccentLight = Color(0xFF6750A4)

/** Primary brand accent variant — used as Material `primary` in the dark color scheme. */
val AppAccentDark = Color(0xFFD0BCFF)

// ============================================================================
// Per-Theme Accent Palettes
// ============================================================================
//
// Each AppTheme has its own accent colors so that toggles, selection indicators,
// and icons automatically match the chosen visual theme.

/** Accent color set for a single theme, covering both light and dark modes. */
data class ThemeAccentColors(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val lightSecondaryContainer: Color,
    val lightOnSecondaryContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val darkSecondaryContainer: Color,
    val darkOnSecondaryContainer: Color,
)

val ForestThemeAccent = ThemeAccentColors(
    lightPrimary = Color(0xFF2E7D32),
    lightOnPrimary = Color(0xFFFFFFFF),
    lightPrimaryContainer = Color(0xFFC8E6C9),
    lightOnPrimaryContainer = Color(0xFF1B5E20),
    lightSecondaryContainer = Color(0xFFC8E6C9),
    lightOnSecondaryContainer = Color(0xFF1B5E20),
    darkPrimary = Color(0xFF81C784),
    darkOnPrimary = Color(0xFF1B3A1D),
    darkPrimaryContainer = Color(0xFF2E7D32),
    darkOnPrimaryContainer = Color(0xFFC8E6C9),
    darkSecondaryContainer = Color(0xFF1B3A1D),
    darkOnSecondaryContainer = Color(0xFFC8E6C9),
)

val AuroraThemeAccent = ThemeAccentColors(
    lightPrimary = Color(0xFF1565C0),
    lightOnPrimary = Color(0xFFFFFFFF),
    lightPrimaryContainer = Color(0xFFBBDEFB),
    lightOnPrimaryContainer = Color(0xFF0D47A1),
    lightSecondaryContainer = Color(0xFFBBDEFB),
    lightOnSecondaryContainer = Color(0xFF0D47A1),
    darkPrimary = Color(0xFF90CAF9),
    darkOnPrimary = Color(0xFF0D2A5A),
    darkPrimaryContainer = Color(0xFF1565C0),
    darkOnPrimaryContainer = Color(0xFFBBDEFB),
    darkSecondaryContainer = Color(0xFF0D2A5A),
    darkOnSecondaryContainer = Color(0xFFBBDEFB),
)

val SunsetThemeAccent = ThemeAccentColors(
    lightPrimary = Color(0xFFBF360C),
    lightOnPrimary = Color(0xFFFFFFFF),
    lightPrimaryContainer = Color(0xFFFFCCBC),
    lightOnPrimaryContainer = Color(0xFF7B1E00),
    lightSecondaryContainer = Color(0xFFFFCCBC),
    lightOnSecondaryContainer = Color(0xFF7B1E00),
    darkPrimary = Color(0xFFFFAB91),
    darkOnPrimary = Color(0xFF5C1A06),
    darkPrimaryContainer = Color(0xFFBF360C),
    darkOnPrimaryContainer = Color(0xFFFFCCBC),
    darkSecondaryContainer = Color(0xFF5C1A06),
    darkOnSecondaryContainer = Color(0xFFFFCCBC),
)

val MonochromeThemeAccent = ThemeAccentColors(
    lightPrimary = AppAccentLight,
    lightOnPrimary = Color(0xFFFFFFFF),
    lightPrimaryContainer = Color(0xFFEADDFF),
    lightOnPrimaryContainer = Color(0xFF21005D),
    lightSecondaryContainer = Color(0xFFE8DEF8),
    lightOnSecondaryContainer = Color(0xFF1D192B),
    darkPrimary = AppAccentDark,
    darkOnPrimary = Color(0xFF371E73),
    darkPrimaryContainer = Color(0xFF4F378B),
    darkOnPrimaryContainer = Color(0xFFEADDFF),
    darkSecondaryContainer = Color(0xFF4A4458),
    darkOnSecondaryContainer = Color(0xFFE8DEF8),
)

// ============================================================================
// Light Theme Colors
// ============================================================================

val md_theme_light_primary = AppAccentLight
val md_theme_light_onPrimary = Color(WHITE)
val md_theme_light_primaryContainer = Color(0xFFEADDFF)
val md_theme_light_onPrimaryContainer = Color(0xFF21005D)

val md_theme_light_secondary = Color(0xFF625B71)
val md_theme_light_onSecondary = Color(WHITE)
val md_theme_light_secondaryContainer = Color(0xFFE8DEF8)
val md_theme_light_onSecondaryContainer = Color(0xFF1D192B)

val md_theme_light_tertiary = Color(0xFF7D5260)
val md_theme_light_onTertiary = Color(WHITE)
val md_theme_light_tertiaryContainer = Color(0xFFFFD8E4)
val md_theme_light_onTertiaryContainer = Color(0xFF31111D)

val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(WHITE)

val md_theme_light_background = Color(WHITE)
val md_theme_light_onBackground = Color(0xFF1C1B1F)
val md_theme_light_surface = Color(WHITE)
val md_theme_light_onSurface = Color(0xFF1C1B1F)
val md_theme_light_surfaceVariant = Color(0xFFE7E0EC)
val md_theme_light_onSurfaceVariant = Color(0xFF49454F)
val md_theme_light_outline = Color(0xFF79747E)

// ============================================================================
// Dark Theme Colors
// ============================================================================

val md_theme_dark_primary = AppAccentDark
val md_theme_dark_onPrimary = Color(0xFF371E73)
val md_theme_dark_primaryContainer = Color(0xFF4F378B)
val md_theme_dark_onPrimaryContainer = Color(0xFFEADDFF)

val md_theme_dark_secondary = Color(0xFFCCC2DC)
val md_theme_dark_onSecondary = Color(0xFF332D41)
val md_theme_dark_secondaryContainer = Color(0xFF4A4458)
val md_theme_dark_onSecondaryContainer = Color(0xFFE8DEF8)

val md_theme_dark_tertiary = Color(0xFFEFB8C8)
val md_theme_dark_onTertiary = Color(0xFF492532)
val md_theme_dark_tertiaryContainer = Color(0xFF633B48)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFD8E4)

val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)

val md_theme_dark_background = Color(BACKGROUND_DARK)
val md_theme_dark_onBackground = Color(WHITE)
val md_theme_dark_surface = Color(BACKGROUND_DARK)
val md_theme_dark_onSurface = Color(WHITE)
val md_theme_dark_surfaceVariant = Color(0xFF49454F)
val md_theme_dark_onSurfaceVariant = Color(0xFFCAC4D0)
val md_theme_dark_outline = Color(0xFF948F99)
