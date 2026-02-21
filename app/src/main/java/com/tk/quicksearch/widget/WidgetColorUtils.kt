package com.tk.quicksearch.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Utility functions for widget color calculations.
 * Shared between widget rendering and preview.
 */
object WidgetColorUtils {
    /**
     * Threshold alpha value above which we use dark grey text on white backgrounds.
     */
    private const val ALPHA_THRESHOLD = 0.6f

    /**
     * Dark grey color used for text/icons on white backgrounds with high alpha.
     */
    private val DARK_GREY = Color(0xFF424242)

    fun getBackgroundColor(
        backgroundColorIsWhite: Boolean,
        backgroundAlpha: Float,
    ): Color {
        val baseColor = if (backgroundColorIsWhite) Color.White else Color.Black
        return baseColor.copy(alpha = backgroundAlpha)
    }

    fun getBackgroundColor(
        theme: WidgetTheme,
        backgroundAlpha: Float,
    ): Color {
        val isWhite =
            when (theme) {
                WidgetTheme.LIGHT -> true

                // white background
                WidgetTheme.DARK -> false

                // dark grey/black background
                WidgetTheme.SYSTEM -> false // default to dark for now (could be made dynamic later)
            }
        return getBackgroundColor(isWhite, backgroundAlpha)
    }

    /**
     * Calculates the border color with alpha applied.
     * Border is always white and never fully opaque.
     */
    fun getBorderColor(
        borderColor: Int,
        borderAlpha: Float,
    ): Color {
        // Keep some transparency even if the user picks a fully opaque border.
        val appliedAlpha = borderAlpha.coerceAtMost(0.4f)
        return Color.White.copy(alpha = appliedAlpha)
    }

    /**
     * Text and icons remain fully opaque (no transparency).
     */
    fun getTextIconColor(
        textIconColorIsWhite: Boolean,
        backgroundAlpha: Float,
    ): Color {
        val base =
            if (textIconColorIsWhite) {
                Color.White
            } else {
                if (backgroundAlpha > ALPHA_THRESHOLD) DARK_GREY else Color.Black
            }
        return base
    }

    fun getTextIconColor(
        theme: WidgetTheme,
        backgroundAlpha: Float,
        textIconColorOverride: TextIconColorOverride,
        customBackgroundColor: Color? = null,
        isSystemInDarkTheme: Boolean = false,
    ): Color {
        val isWhite =
            when (textIconColorOverride) {
                TextIconColorOverride.WHITE -> {
                    true
                }

                // white text/icons
                TextIconColorOverride.BLACK -> {
                    false
                }

                // black text/icons
                TextIconColorOverride.THEME -> { // follow theme
                    if (customBackgroundColor != null) {
                        customBackgroundColor.luminance() < 0.5f
                    } else {
                        val effectiveTheme =
                            when (theme) {
                                WidgetTheme.SYSTEM -> if (isSystemInDarkTheme) WidgetTheme.DARK else WidgetTheme.LIGHT
                                else -> theme
                            }
                        effectiveTheme == WidgetTheme.DARK // dark theme uses white text, light theme uses black
                    }
                }
            }
        return getTextIconColor(isWhite, backgroundAlpha)
    }
}
