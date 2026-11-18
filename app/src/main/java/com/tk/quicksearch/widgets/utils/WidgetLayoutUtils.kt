package com.tk.quicksearch.widgets.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens

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

    internal val customButtonsInset = DesignTokens.SpacingMedium
    internal val searchActionsInset = DesignTokens.SpacingLarge
    internal val searchActionsGap = DesignTokens.ConnectedGroupGap

    internal fun customButtonsGap(count: Int, width: Dp): Dp =
        if (count >= 5 || width <= DEFAULT_WIDTH_DP.dp) DesignTokens.ConnectedGroupGap
        else DesignTokens.SpacingSmall

    internal fun buttonCapacity(width: Dp, gap: Dp): Int =
        ((width + gap) / (DesignTokens.MinTouchTarget + gap)).toInt().coerceAtLeast(0)

    internal fun searchShortcutCapacity(
        widgetWidth: Dp,
        innerWidth: Dp,
        showLabel: Boolean,
        showSearchIcon: Boolean,
        iconSize: Dp,
        showMic: Boolean,
    ): Int {
        if (widgetWidth <= NARROW_WIDTH_DP.dp) return 0
        val searchReserve = when {
            showLabel -> 96.dp
            showSearchIcon -> maxOf(DesignTokens.MinTouchTarget, iconSize + DesignTokens.SpacingLarge)
            else -> DesignTokens.MinTouchTarget
        }
        val micReserve = if (showMic) DesignTokens.MinTouchTarget + searchActionsGap else 0.dp
        return buttonCapacity(innerWidth - searchActionsInset * 2 - searchReserve - micReserve, searchActionsGap)
    }

    internal fun searchActionsWidth(count: Int): Dp =
        if (count == 0) 0.dp
        else DesignTokens.MinTouchTarget * count + searchActionsGap * (count - 1)


    fun resolveOr(
        value: Dp,
        default: Dp,
    ): Dp =
        if (value == Dp.Unspecified || value.value <= 0f) {
            default
        } else {
            value
        }
}
