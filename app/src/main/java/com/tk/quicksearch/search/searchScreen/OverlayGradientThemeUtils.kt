package com.tk.quicksearch.search.searchScreen

import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.search.core.OverlayGradientTheme

internal fun overlayGradientColors(
    theme: OverlayGradientTheme,
    isDarkMode: Boolean,
    alpha: Float = 1f,
): List<Color> {
    val baseColors =
        if (isDarkMode) {
            when (theme) {
                OverlayGradientTheme.DUSK ->
                    listOf(
                        Color(0xFF24334B),
                        Color(0xFF3A2F45),
                        Color(0xFF2D3D4E),
                        Color(0xFF4A4135),
                    )
                OverlayGradientTheme.FOREST ->
                    listOf(
                        Color(0xFF27382F),
                        Color(0xFF2F4640),
                        Color(0xFF435034),
                        Color(0xFF1F3340),
                    )
                OverlayGradientTheme.OCEAN ->
                    listOf(
                        Color(0xFF1F3444),
                        Color(0xFF24465A),
                        Color(0xFF2E4D63),
                        Color(0xFF31455B),
                    )
                OverlayGradientTheme.SLATE ->
                    listOf(
                        Color(0xFF2A2E39),
                        Color(0xFF313A45),
                        Color(0xFF3A414D),
                        Color(0xFF262A31),
                    )
                OverlayGradientTheme.SAND ->
                    listOf(
                        Color(0xFF3A332B),
                        Color(0xFF4A4035),
                        Color(0xFF52453B),
                        Color(0xFF353029),
                    )
            }
        } else {
            when (theme) {
                OverlayGradientTheme.DUSK ->
                    listOf(
                        Color(0xFFE6ECF5),
                        Color(0xFFEAE4F0),
                        Color(0xFFE1EAF0),
                        Color(0xFFEEE8DF),
                    )
                OverlayGradientTheme.FOREST ->
                    listOf(
                        Color(0xFFE4ECE7),
                        Color(0xFFE4ECE9),
                        Color(0xFFEBEEE2),
                        Color(0xFFE0E9EC),
                    )
                OverlayGradientTheme.OCEAN ->
                    listOf(
                        Color(0xFFE1EAF2),
                        Color(0xFFDDEAF0),
                        Color(0xFFE2EBF2),
                        Color(0xFFE6EDF2),
                    )
                OverlayGradientTheme.SLATE ->
                    listOf(
                        Color(0xFFE6E8ED),
                        Color(0xFFE2E7EC),
                        Color(0xFFE8EAEE),
                        Color(0xFFDEE1E5),
                    )
                OverlayGradientTheme.SAND ->
                    listOf(
                        Color(0xFFEFE9E1),
                        Color(0xFFF1EBE4),
                        Color(0xFFEEE7DE),
                        Color(0xFFE7E2DA),
                    )
            }
        }

    val clampedAlpha = alpha.coerceIn(0f, 1f)
    return baseColors.map { color -> color.copy(alpha = clampedAlpha) }
}
