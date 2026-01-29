package com.tk.quicksearch.ui.theme

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

/** Dark background color used by dark theme */
private const val BACKGROUND_DARK = 0xFF121212L

/** Light background color used by light theme */
private const val BACKGROUND_LIGHT = 0xFFFFFBFEL

/** White color used for on-surface elements */
private const val WHITE = 0xFFFFFFFFL

/** Dark color used for on-surface elements in light theme */
private const val ON_SURFACE_LIGHT = 0xFF1C1B1FL

/** Shared primary container colors */
private const val PRIMARY_CONTAINER_LIGHT = 0xFFEADDFFL
private const val SECONDARY_CONTAINER_LIGHT = 0xFFE8DEF8L
private const val TERTIARY_CONTAINER_LIGHT = 0xFFFFD8E4L

// ============================================================================
// Light Theme Colors
// ============================================================================

// Primary colors
val md_theme_light_primary = Color(0xFF6750A4)
val md_theme_light_onPrimary = Color(WHITE)
val md_theme_light_primaryContainer = Color(PRIMARY_CONTAINER_LIGHT)
val md_theme_light_onPrimaryContainer = Color(0xFF21005D)

// Secondary colors
val md_theme_light_secondary = Color(0xFF625B71)
val md_theme_light_onSecondary = Color(WHITE)
val md_theme_light_secondaryContainer = Color(SECONDARY_CONTAINER_LIGHT)
val md_theme_light_onSecondaryContainer = Color(0xFF1D192B)

// Tertiary colors
val md_theme_light_tertiary = Color(0xFF7D5260)
val md_theme_light_onTertiary = Color(WHITE)
val md_theme_light_tertiaryContainer = Color(TERTIARY_CONTAINER_LIGHT)
val md_theme_light_onTertiaryContainer = Color(0xFF31101D)

// Error colors
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(WHITE)

// Surface colors (light theme uses light colors)
val md_theme_light_background = Color(BACKGROUND_LIGHT)
val md_theme_light_onBackground = Color(ON_SURFACE_LIGHT)
val md_theme_light_surface = Color(BACKGROUND_LIGHT)
val md_theme_light_onSurface = Color(ON_SURFACE_LIGHT)
val md_theme_light_surfaceVariant = Color(0xFFE7E0EC)
val md_theme_light_onSurfaceVariant = Color(0xFF49454F)

// Outline color
val md_theme_light_outline = Color(0xFF7A757F)

// ============================================================================
// Dark Theme Colors
// ============================================================================

// Primary colors
val md_theme_dark_primary = Color(0xFFD0BCFF)
val md_theme_dark_onPrimary = Color(0xFF371E73)
val md_theme_dark_primaryContainer = Color(0xFF4F378B)
val md_theme_dark_onPrimaryContainer = Color(PRIMARY_CONTAINER_LIGHT)

// Secondary colors
val md_theme_dark_secondary = Color(0xFFCCC2DC)
val md_theme_dark_onSecondary = Color(0xFF332D41)
val md_theme_dark_secondaryContainer = Color(0xFF4A4458)
val md_theme_dark_onSecondaryContainer = Color(SECONDARY_CONTAINER_LIGHT)

// Tertiary colors
val md_theme_dark_tertiary = Color(0xFFEFB8C8)
val md_theme_dark_onTertiary = Color(0xFF492532)
val md_theme_dark_tertiaryContainer = Color(0xFF633B48)
val md_theme_dark_onTertiaryContainer = Color(TERTIARY_CONTAINER_LIGHT)

// Error colors
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)

// Surface colors
val md_theme_dark_background = Color(BACKGROUND_DARK)
val md_theme_dark_onBackground = Color(WHITE)
val md_theme_dark_surface = Color(BACKGROUND_DARK)
val md_theme_dark_onSurface = Color(WHITE)
val md_theme_dark_surfaceVariant = Color(0xFF49454F)
val md_theme_dark_onSurfaceVariant = Color(0xFFCAC4D0)

// Outline color
val md_theme_dark_outline = Color(0xFF948F99)
