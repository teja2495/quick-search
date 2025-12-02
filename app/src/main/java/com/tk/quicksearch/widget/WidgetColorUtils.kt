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

    /**
     * Calculates the background color based on preferences.
     */
    fun getBackgroundColor(
        backgroundColorIsWhite: Boolean,
        backgroundAlpha: Float
    ): Color {
        val baseColor = if (backgroundColorIsWhite) Color.White else Color.Black
        return baseColor.copy(alpha = backgroundAlpha)
    }

    /**
     * Calculates the border color with alpha applied.
     */
    fun getBorderColor(borderColor: Int, backgroundAlpha: Float): Color {
        return Color(borderColor).copy(alpha = backgroundAlpha)
    }

    /**
     * Calculates the text and icon color based on background and transparency.
     * Text and icons should remain fully opaque (no transparency).
     */
    fun getTextIconColor(
        borderColor: Int,
        backgroundColorIsWhite: Boolean,
        backgroundAlpha: Float
    ): Color {
        val baseBorderColor = Color(borderColor)
        return if (backgroundAlpha > ALPHA_THRESHOLD && backgroundColorIsWhite) {
            DARK_GREY
        } else {
            baseBorderColor
        }
    }
}
