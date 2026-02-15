package com.tk.quicksearch.search.searchScreen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.tk.quicksearch.search.core.OverlayGradientTheme

private const val NEUTRAL_OVERLAY_THEME_INTENSITY = 0.5f
private const val MAX_OVERLAY_THEME_TONE_SHIFT = 0.38f

internal fun overlayGradientColors(
    theme: OverlayGradientTheme,
    isDarkMode: Boolean,
    alpha: Float = 1f,
    intensity: Float = NEUTRAL_OVERLAY_THEME_INTENSITY,
): List<Color> {
    val baseColors =
        if (isDarkMode) {
            when (theme) {
                OverlayGradientTheme.FOREST ->
                    listOf(
                        Color(0xFF27382F),
                        Color(0xFF2F4640),
                        Color(0xFF435034),
                        Color(0xFF1F3340),
                    )
                OverlayGradientTheme.AURORA ->
                    listOf(
                        Color(0xFF1F2E4A),
                        Color(0xFF1F4A5A),
                        Color(0xFF3A3E6B),
                        Color(0xFF2A3150),
                    )
                OverlayGradientTheme.SUNSET ->
                    listOf(
                        Color(0xFF4A2C34),
                        Color(0xFF5A3A2A),
                        Color(0xFF5C3046),
                        Color(0xFF3E2A3B),
                    )
                OverlayGradientTheme.MONOCHROME ->
                    listOf(
                        Color(0xFF121212),
                        Color(0xFF2A2A2A),
                        Color(0xFF3E3E3E),
                        Color(0xFFE8E8E8),
                    )
            }
        } else {
            when (theme) {
                OverlayGradientTheme.FOREST ->
                    listOf(
                        Color(0xFFE4ECE7),
                        Color(0xFFE4ECE9),
                        Color(0xFFEBEEE2),
                        Color(0xFFE0E9EC),
                    )
                OverlayGradientTheme.AURORA ->
                    listOf(
                        Color(0xFFDCE8F8),
                        Color(0xFFD8F1F0),
                        Color(0xFFE2E2FA),
                        Color(0xFFDCE6F4),
                    )
                OverlayGradientTheme.SUNSET ->
                    listOf(
                        Color(0xFFF8E1D8),
                        Color(0xFFF8E8D8),
                        Color(0xFFF4DCE8),
                        Color(0xFFF6E1DF),
                    )
                OverlayGradientTheme.MONOCHROME ->
                    listOf(
                        Color(0xFFF0F0F0),
                        Color(0xFFE2E2E2),
                        Color(0xFFD5D5D5),
                        Color(0xFFBFBFBF),
                    )
            }
        }

    val clampedAlpha = alpha.coerceIn(0f, 1f)
    return baseColors.map { color ->
        adjustOverlayThemeTone(color = color, intensity = intensity).copy(alpha = clampedAlpha)
    }
}

internal fun overlayResultCardColor(
    theme: OverlayGradientTheme,
    isDarkMode: Boolean,
    intensity: Float = NEUTRAL_OVERLAY_THEME_INTENSITY,
): Color {
    val palette = overlayGradientColors(theme = theme, isDarkMode = isDarkMode, intensity = intensity)
    return if (isDarkMode) palette[1] else palette[0]
}

internal fun overlayDividerColor(
    theme: OverlayGradientTheme,
    isDarkMode: Boolean,
    intensity: Float = NEUTRAL_OVERLAY_THEME_INTENSITY,
): Color {
    val palette = overlayGradientColors(theme = theme, isDarkMode = isDarkMode, intensity = intensity)
    val base = if (isDarkMode) palette[2] else palette[3]
    return if (isDarkMode) {
        lerp(start = base, stop = Color.White, fraction = 0.18f).copy(alpha = 0.38f)
    } else {
        lerp(start = base, stop = Color.Black, fraction = 0.15f).copy(alpha = 0.24f)
    }
}

internal fun overlayActionColor(
    theme: OverlayGradientTheme,
    isDarkMode: Boolean,
    intensity: Float = NEUTRAL_OVERLAY_THEME_INTENSITY,
): Color {
    val palette = overlayGradientColors(theme = theme, isDarkMode = isDarkMode, intensity = intensity)
    return if (isDarkMode) {
        lerp(start = palette[2], stop = Color.Black, fraction = 0.28f)
    } else {
        lerp(start = palette[1], stop = Color.Black, fraction = 0.38f)
    }
}

private fun adjustOverlayThemeTone(
    color: Color,
    intensity: Float,
): Color {
    val normalizedIntensity =
        ((intensity.coerceIn(0f, 1f) - NEUTRAL_OVERLAY_THEME_INTENSITY) * 2f)
    return when {
        normalizedIntensity > 0f ->
            lerp(
                start = color,
                stop = Color.Black,
                fraction = normalizedIntensity * MAX_OVERLAY_THEME_TONE_SHIFT,
            )
        normalizedIntensity < 0f ->
            lerp(
                start = color,
                stop = Color.White,
                fraction = -normalizedIntensity * MAX_OVERLAY_THEME_TONE_SHIFT,
            )
        else -> color
    }
}
