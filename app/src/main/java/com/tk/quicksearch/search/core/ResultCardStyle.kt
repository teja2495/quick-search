package com.tk.quicksearch.search.core

/**
 * User customization of search result and home cards (pinned items, At a Glance, widgets and other
 * result cards). Defaults reproduce the stock look.
 *
 * @property cornerRadiusDp corner radius of a card's outer corners.
 * @property transparency 0 keeps the card's own alpha, 1 makes its background fully clear.
 * @property blurEnabled draw the frosted (blurred) wallpaper behind cards where it is supported.
 */
data class ResultCardStyle(
    val cornerRadiusDp: Float = DEFAULT_CORNER_RADIUS_DP,
    val transparency: Float = DEFAULT_TRANSPARENCY,
    val blurEnabled: Boolean = true,
) {
    val isDefault: Boolean
        get() = this == ResultCardStyle()

    companion object {
        /** Matches DesignTokens.CornerExtraLarge, the stock result card radius. */
        const val DEFAULT_CORNER_RADIUS_DP = 28f
        const val MIN_CORNER_RADIUS_DP = 0f
        const val MAX_CORNER_RADIUS_DP = 32f
        const val DEFAULT_TRANSPARENCY = 0f
    }
}
