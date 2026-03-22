package com.tk.quicksearch.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// ============================================================================
// Color Schemes
// ============================================================================

/**
 * Dark theme color scheme following Material Design 3 specifications.
 */
private val DarkColorScheme =
    darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        error = md_theme_dark_error,
        onError = md_theme_dark_onError,
        outline = md_theme_dark_outline,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = md_theme_light_primaryContainer,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        secondary = md_theme_light_secondary,
        onSecondary = md_theme_light_onSecondary,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = md_theme_light_onTertiary,
        tertiaryContainer = md_theme_light_tertiaryContainer,
        onTertiaryContainer = md_theme_light_onTertiaryContainer,
        background = md_theme_light_background,
        onBackground = md_theme_light_onBackground,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        error = md_theme_light_error,
        onError = md_theme_light_onError,
        outline = md_theme_light_outline,
    )

// ============================================================================
// Theme Composable
// ============================================================================

/**
 * QuickSearch application theme composable.
 *
 * Provides Material 3 color schemes and app-specific semantic color tokens.
 *
 * @param useDarkTheme Whether dark colors should be used. Defaults to `true` to preserve current behavior.
 * @param content The composable content to be themed.
 */
@Composable
fun QuickSearchTheme(
    fontScaleMultiplier: Float = 1f,
    appThemeMode: com.tk.quicksearch.search.core.AppThemeMode = com.tk.quicksearch.search.core.AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val baseDensity = LocalDensity.current
    val appDensity =
        remember(baseDensity, fontScaleMultiplier) {
            Density(
                density = baseDensity.density,
                fontScale = baseDensity.fontScale * fontScaleMultiplier,
            )
        }
    val isSystemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val useDarkTheme = when (appThemeMode) {
        com.tk.quicksearch.search.core.AppThemeMode.LIGHT -> false
        com.tk.quicksearch.search.core.AppThemeMode.DARK -> true
        com.tk.quicksearch.search.core.AppThemeMode.SYSTEM -> isSystemDarkTheme
    }
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val appPalette =
        if (useDarkTheme) {
            DarkQuickSearchAppColorPalette
        } else {
            LightQuickSearchAppColorPalette
        }

    CompositionLocalProvider(
        LocalDensity provides appDensity,
        LocalQuickSearchAppColorPalette provides appPalette,
        LocalAppIsDarkTheme provides useDarkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
