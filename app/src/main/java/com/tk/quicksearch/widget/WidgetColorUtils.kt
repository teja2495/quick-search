package com.tk.quicksearch.widget

import androidx.compose.ui.graphics.Color

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
        backgroundAlpha: Float
    ): Color {
        val baseColor = if (backgroundColorIsWhite) Color.White else Color.Black
        return baseColor.copy(alpha = backgroundAlpha)
    }

    /**
     * Calculates the border color with alpha applied.
     * Border is always white and never fully opaque.
     */
    fun getBorderColor(borderColor: Int, backgroundAlpha: Float): Color {
        // Keep some transparency even if the user picks a fully opaque background.
        val appliedAlpha = backgroundAlpha.coerceAtMost(0.4f)
        return Color.White.copy(alpha = appliedAlpha)
    }

    /**
     * Text and icons remain fully opaque (no transparency).
     */
    fun getTextIconColor(
        textIconColorIsWhite: Boolean,
        backgroundAlpha: Float
    ): Color {
        val base = if (textIconColorIsWhite) Color.White else {
            if (backgroundAlpha > ALPHA_THRESHOLD) DARK_GREY else Color.Black
        }
        return base
    }
}
