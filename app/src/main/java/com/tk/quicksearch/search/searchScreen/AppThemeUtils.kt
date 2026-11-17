package com.tk.quicksearch.search.searchScreen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DarkWallpaperSearchSurfaceAlpha
import com.tk.quicksearch.shared.ui.theme.LightResultCardFrostAlpha
import com.tk.quicksearch.shared.ui.theme.LightWallpaperKeyboardButtonAlpha
import com.tk.quicksearch.shared.ui.theme.LightWallpaperSearchResultCardAlpha
import com.tk.quicksearch.shared.ui.theme.SearchColorTheme
import com.tk.quicksearch.shared.ui.theme.ThemeColorRegistry

private const val NEUTRAL_APP_THEME_INTENSITY = 0.5f
private const val MAX_APP_THEME_TONE_SHIFT = 0.38f

internal fun isAmoledSurfaceTheme(
    amoledThemeEnabled: Boolean,
    theme: AppTheme,
    isDarkMode: Boolean,
    deviceThemeEnabled: Boolean = false,
    backgroundSource: BackgroundSource = BackgroundSource.THEME,
): Boolean =
    amoledThemeEnabled &&
        isDarkMode &&
        !deviceThemeEnabled &&
        backgroundSource == BackgroundSource.THEME

internal fun AppThemeColors(
    theme: AppTheme,
    isDarkMode: Boolean,
    alpha: Float = 1f,
    intensity: Float = NEUTRAL_APP_THEME_INTENSITY,
): List<Color> {
    val baseColors = ThemeColorRegistry.gradient(theme, isDarkMode)

    val clampedAlpha = alpha.coerceIn(0f, 1f)
    return baseColors.map { color ->
        adjustAppThemeTone(color = color, intensity = intensity).copy(alpha = clampedAlpha)
    }
}

internal fun appThemeResultCardColor(
    theme: AppTheme,
    isDarkMode: Boolean,
    intensity: Float = NEUTRAL_APP_THEME_INTENSITY,
    amoledThemeEnabled: Boolean = false,
): Color {
    if (isAmoledSurfaceTheme(amoledThemeEnabled = amoledThemeEnabled, theme = theme, isDarkMode = isDarkMode)) {
        return AppColors.AmoledCardBackground
    }
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
        lerp(start = base, stop = Color.White, fraction = 0.18f).copy(alpha = AppColors.DisabledContentAlpha)
    } else {
        lerp(start = base, stop = Color.Black, fraction = 0.15f).copy(alpha = AppColors.TintStrongAlpha)
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
    amoledThemeEnabled: Boolean = false,
): SearchColorTheme =
    when (backgroundSource) {
        BackgroundSource.THEME -> {
            val gradientColors = AppThemeColors(theme, isDarkMode, intensity = intensity)
            val cardBg =
                appThemeResultCardColor(
                    theme = theme,
                    isDarkMode = isDarkMode,
                    intensity = intensity,
                    amoledThemeEnabled = amoledThemeEnabled,
                )
            SearchColorTheme(
                background = gradientColors[0],
                cardBackground = cardBg,
                keyboardButtonBackground = cardBg.copy(alpha = AppColors.ContentHighAlpha),
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

/** Result-card, divider and action colors tinted by the app theme; all null when tints don't apply. */
internal data class OverlayThemeTints(
    val cardColor: Color?,
    val dividerTint: Color?,
    val actionTint: Color?,
)

/**
 * Theme tints for the search surface when the app theme (not the device theme) colors the
 * background; otherwise every tint is null and callers fall back to the default palette.
 */
internal fun overlayThemeTints(
    state: SearchUiState,
    isDarkMode: Boolean,
): OverlayThemeTints {
    if (state.deviceThemeEnabled || state.backgroundSource != BackgroundSource.THEME) {
        return OverlayThemeTints(cardColor = null, dividerTint = null, actionTint = null)
    }
    return OverlayThemeTints(
        cardColor =
            appThemeResultCardColor(
                theme = state.appTheme,
                isDarkMode = isDarkMode,
                intensity = state.overlayThemeIntensity,
                amoledThemeEnabled = state.amoledThemeEnabled,
            ),
        dividerTint = appThemeDividerColor(state.appTheme, isDarkMode, state.overlayThemeIntensity),
        actionTint = appThemeActionColor(state.appTheme, isDarkMode, state.overlayThemeIntensity),
    )
}
