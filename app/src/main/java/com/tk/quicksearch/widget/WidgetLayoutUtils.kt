package com.tk.quicksearch.widget

import androidx.compose.ui.unit.Dp

object WidgetLayoutUtils {
    const val DEFAULT_HEIGHT_DP = 58f

    /**
     * Default widget width in dp. Keep this aligned with the provider's
     * declared min width so the fallback bitmap size matches the host.
     */
    const val DEFAULT_WIDTH_DP = 280f

    /**
     * Approximate width for a 2-column widget. When the widget is this narrow
     * or less, we collapse to icon-only to keep layout intact.
     */
    const val TWO_COLUMN_WIDTH_DP = 220f

    /**
     * Very narrow width threshold for hiding custom buttons. When the widget
     * is this narrow or less, custom buttons are hidden to preserve space.
     */
    const val NARROW_WIDTH_DP = 200f

    fun resolveOr(value: Dp, default: Dp): Dp {
        return if (value == Dp.Unspecified || value.value <= 0f) {
            default
        } else {
            value
        }
    }
}
