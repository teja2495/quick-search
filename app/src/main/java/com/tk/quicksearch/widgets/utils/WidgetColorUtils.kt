package com.tk.quicksearch.widgets.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.tk.quicksearch.shared.ui.theme.AppColors

/**
 * Utility functions for widget color calculations.
 * Shared between widget rendering and preview.
 */
object WidgetColorUtils {
    /**
     * Threshold alpha value above which we use dark grey text on white backgrounds.
     */
    private const val ALPHA_THRESHOLD = 0.6f

    fun getBackgroundColor(
        backgroundColorIsWhite: Boolean,
        backgroundAlpha: Float,
    ): Color {
        val baseColor =
            if (backgroundColorIsWhite) {
                AppColors.WidgetBackgroundLight
            } else {
                AppColors.WidgetBackgroundDark
            }
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
     * Uses white for dark theme and black for light theme.
     */
    fun getBorderColor(
        borderColor: Int,
        borderAlpha: Float,
        effectiveTheme: WidgetTheme = WidgetTheme.DARK,
    ): Color {
        // Keep some transparency even if the user picks a fully opaque border.
        val appliedAlpha = borderAlpha.coerceAtMost(0.4f)
        val base = if (effectiveTheme == WidgetTheme.LIGHT) AppColors.WidgetBorderDefault else AppColors.WidgetBorder
        return base.copy(alpha = appliedAlpha)
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
                AppColors.WidgetTextLight
            } else {
                if (backgroundAlpha > ALPHA_THRESHOLD) {
                    AppColors.WidgetTextDarkGrey
                } else {
                    AppColors.WidgetTextDark
                }
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
