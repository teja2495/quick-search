package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.SearchBarStyle
import com.tk.quicksearch.search.searchScreen.insetCardBackground
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAmoledThemeActive

/** Resting outline width for [SearchBarStyle], shared by the live bar and the settings preview. */
internal fun SearchBarStyle.borderWidth(): Dp = borderWidthDp.dp

/** Bar corner radius, shared by the standalone and inset bars. */
internal fun SearchBarStyle.cornerRadius(): Dp = cornerRadiusDp.dp

/** Resting outline color before the welcome-animation fade is applied. */
internal fun SearchBarStyle.borderColor(accent: Color): Color =
    borderColorArgb?.let { Color(it) }
        ?: accent.copy(alpha = DesignTokens.SearchFieldAccentOutlineAlpha)

/** Fill color with the custom color and transparency applied over [themeBackground]. */
internal fun SearchBarStyle.backgroundColor(themeBackground: Color): Color {
    val base = backgroundColorArgb?.let { Color(it) } ?: themeBackground
    return base.copy(alpha = base.alpha * (1f - backgroundTransparency.coerceIn(0f, 1f)))
}

/** Resolves foregrounds against the same fill used by standalone and inset search chrome. */
@Composable
internal fun searchBarContrastBackground(
    style: SearchBarStyle,
    showWallpaperBackground: Boolean,
    opaqueBackground: Boolean = false,
): Color {
    val fill = if (opaqueBackground) {
        AppColors.DialogBackground
    } else {
        insetCardBackground(true, style.backgroundColor(AppColors.getSearchBarBackground(showWallpaperBackground)), anywhere = true)
    }
    val backing = if (LocalAmoledThemeActive.current) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    return fill.compositeOver(backing)
}

internal fun Color.readableOn(background: Color, minimumContrast: Float = 4.5f): Color {
    val backgroundLuminance = background.luminance()
    val foregroundLuminance = compositeOver(background).luminance()
    val contrast = (maxOf(backgroundLuminance, foregroundLuminance) + 0.05f) /
        (minOf(backgroundLuminance, foregroundLuminance) + 0.05f)
    if (contrast >= minimumContrast) return this
    val blackContrast = (backgroundLuminance + 0.05f) / 0.05f
    val whiteContrast = 1.05f / (backgroundLuminance + 0.05f)
    return if (blackContrast >= whiteContrast) Color.Black else Color.White
}
