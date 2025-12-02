package com.tk.quicksearch.widget

import androidx.compose.ui.unit.Dp

/**
 * Utility functions for widget layout calculations.
 */
object WidgetLayoutUtils {
    /**
     * Default widget height in dp.
     */
    const val DEFAULT_HEIGHT_DP = 64f

    /**
     * Resolves a Dp value, returning a default if it's unspecified or invalid.
     */
    fun resolveOr(value: Dp, default: Dp): Dp {
        return if (value == Dp.Unspecified || value.value <= 0f) {
            default
        } else {
            value
        }
    }
}
