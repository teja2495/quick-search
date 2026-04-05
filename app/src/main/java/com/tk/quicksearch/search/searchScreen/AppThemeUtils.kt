package com.tk.quicksearch.search.searchScreen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DarkWallpaperSearchSurfaceAlpha
import com.tk.quicksearch.shared.ui.theme.LightResultCardFrostAlpha
import com.tk.quicksearch.shared.ui.theme.LightWallpaperKeyboardButtonAlpha
import com.tk.quicksearch.shared.ui.theme.LightWallpaperSearchResultCardAlpha
import com.tk.quicksearch.shared.ui.theme.SearchColorTheme

private const val NEUTRAL_APP_THEME_INTENSITY = 0.5f
private const val MAX_APP_THEME_TONE_SHIFT = 0.38f

internal fun AppThemeColors(
    theme: AppTheme,
    isDarkMode: Boolean,
    alpha: Float = 1f,
    intensity: Float = NEUTRAL_APP_THEME_INTENSITY,
): List<Color> {
    val baseColors =
        if (isDarkMode) {
            when (theme) {
                AppTheme.FOREST ->
                    AppColors.ForestDarkPalette
                AppTheme.AURORA ->
                    AppColors.AuroraDarkPalette
                AppTheme.SUNSET ->
                    AppColors.SunsetDarkPalette
                AppTheme.MONOCHROME ->
                    AppColors.MonochromeDarkPalette
            }
        } else {
            when (theme) {
                AppTheme.FOREST ->
                    AppColors.ForestLightPalette
                AppTheme.AURORA ->
                    AppColors.AuroraLightPalette
                AppTheme.SUNSET ->
                    AppColors.SunsetLightPalette
                AppTheme.MONOCHROME ->
                    AppColors.MonochromeLightPalette
            }
        }

    val clampedAlpha = alpha.coerceIn(0f, 1f)
    return baseColors.map { color ->
        adjustAppThemeTone(color = color, intensity = intensity).copy(alpha = clampedAlpha)
    }
}

internal fun appThemeResultCardColor(
    theme: AppTheme,
    isDarkMode: Boolean,
    intensity: Float = NEUTRAL_APP_THEME_INTENSITY,
): Color {
    val palette = AppThemeColors(theme = theme, isDarkMode = isDarkMode, intensity = intensity)
    return if (isDarkMode) {
        palette[1]
    } else {
        Color.White.copy(alpha = LightResultCardFrostAlpha)
    }
}

internal fun appThemeDividerColor(
    theme: AppTheme,
    isDarkMode: Boolean,
    intensity: Float = NEUTRAL_APP_THEME_INTENSITY,
): Color {
    val palette = AppThemeColors(theme = theme, isDarkMode = isDarkMode, intensity = intensity)
    val base = if (isDarkMode) palette[2] else palette[3]
    return if (isDarkMode) {
        lerp(start = base, stop = Color.White, fraction = 0.18f).copy(alpha = 0.38f)
    } else {
        lerp(start = base, stop = Color.Black, fraction = 0.15f).copy(alpha = 0.24f)
    }
}

internal fun appThemeActionColor(
    theme: AppTheme,
    isDarkMode: Boolean,
    intensity: Float = NEUTRAL_APP_THEME_INTENSITY,
): Color {
    val palette = AppThemeColors(theme = theme, isDarkMode = isDarkMode, intensity = intensity)
    return if (isDarkMode) {
        lerp(start = palette[2], stop = Color.Black, fraction = 0.28f)
    } else {
        lerp(start = palette[1], stop = Color.Black, fraction = 0.38f)
    }
}

/**
 * Computes the full [SearchColorTheme] for the given app theme and background source.
 *
 * This is the single function to call when you need all theme-derived colors (background,
 * card background, keyboard button background) from the current user theme selection.
 * Provide the result via [com.tk.quicksearch.shared.ui.theme.LocalSearchColorTheme] so
 * descendant composables can read the correct colors without knowing the active theme.
 */
internal fun resolveSearchColorTheme(
    theme: AppTheme,
    backgroundSource: BackgroundSource,
    isDarkMode: Boolean,
    intensity: Float = NEUTRAL_APP_THEME_INTENSITY,
): SearchColorTheme =
    when (backgroundSource) {
        BackgroundSource.THEME -> {
            val gradientColors = AppThemeColors(theme, isDarkMode, intensity = intensity)
            val cardBg = appThemeResultCardColor(theme, isDarkMode, intensity)
            SearchColorTheme(
                background = gradientColors[0],
                cardBackground = cardBg,
                keyboardButtonBackground = cardBg.copy(alpha = 0.75f),
            )
        }
        BackgroundSource.SYSTEM_WALLPAPER,
        BackgroundSource.CUSTOM_IMAGE -> {
            val cardBg =
                if (isDarkMode) {
                    Color.Black.copy(alpha = DarkWallpaperSearchSurfaceAlpha)
                } else {
                    Color.White.copy(alpha = LightWallpaperSearchResultCardAlpha)
                }
            SearchColorTheme(
                background = Color.Transparent,
                cardBackground = cardBg,
                keyboardButtonBackground =
                    if (isDarkMode) {
                        Color.Black.copy(alpha = DarkWallpaperSearchSurfaceAlpha)
                    } else {
                        Color.White.copy(alpha = LightWallpaperKeyboardButtonAlpha)
                    },
            )
        }
    }

private fun adjustAppThemeTone(
    color: Color,
    intensity: Float,
): Color {
    val normalizedIntensity =
        ((intensity.coerceIn(0f, 1f) - NEUTRAL_APP_THEME_INTENSITY) * 2f)
    return when {
        normalizedIntensity > 0f ->
            lerp(
                start = color,
                stop = Color.Black,
                fraction = normalizedIntensity * MAX_APP_THEME_TONE_SHIFT,
            )
        normalizedIntensity < 0f ->
            lerp(
                start = color,
                stop = Color.White,
                fraction = -normalizedIntensity * MAX_APP_THEME_TONE_SHIFT,
            )
        else -> color
    }
}
