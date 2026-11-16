package com.tk.quicksearch.search.searchScreen.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Material 3 Expressive segmented lists: with the card layout on, a section's rows are separate
 * tiles [DesignTokens.SegmentedListGap] apart instead of one card split by dividers. The group's
 * outer corners are large and the corners between tiles small, so the rows still read as one list.
 *
 * Set by [com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard] for sections that
 * opt in; null (rows on one card, or flat rows in Compact Layout) everywhere else.
 */
@Immutable
internal data class SegmentedRowsStyle(val containerColor: Color)

internal val LocalSegmentedRowsStyle = compositionLocalOf<SegmentedRowsStyle?> { null }

/** True while rows are drawn as segmented tiles. */
@Composable
internal fun segmentedRowsActive(): Boolean = LocalSegmentedRowsStyle.current != null

/**
 * Padding of a rows container (Column or LazyColumn) around its rows. Segmented tiles span the full
 * width and carry the inset themselves, so the container drops it.
 */
@Composable
internal fun resultRowsContainerPadding(value: Dp): Dp = if (segmentedRowsActive()) 0.dp else value

/** Shape of the tile at [index] of [count]: large outer corners, small corners between tiles. */
internal fun segmentShape(index: Int, count: Int): Shape {
    val outer = DesignTokens.SegmentedListOuterCorner
    val inner = DesignTokens.SegmentedListInnerCorner
    val top = if (index <= 0) outer else inner
    val bottom = if (index >= count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * One row of a result list. Segmented, it becomes a tile at [index] of [count] (count includes a
 * trailing More button tile); otherwise it renders [content] unchanged.
 */
@Composable
internal fun ResultSegment(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val style = LocalSegmentedRowsStyle.current
    if (style == null) {
        content()
        return
    }
    val shape = segmentShape(index, count)
    Box(
        modifier =
            modifier
                .padding(top = if (index > 0) DesignTokens.SegmentedListGap else 0.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(style.containerColor, shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(
                    horizontal = DesignTokens.SegmentedListTileHorizontalPadding,
                    vertical = DesignTokens.SegmentedListTileVerticalPadding,
                ),
    ) {
        content()
    }
}

/** Divider between two result rows. Segmented tiles are separated by their gap instead. */
@Composable
internal fun ResultRowDivider(
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier.compactEdgeToEdge().fillMaxWidth(),
    color: Color? = null,
) {
    if (segmentedRowsActive()) return
    HorizontalDivider(
        modifier = modifier,
        color =
            color ?: SearchResultCardDefaults.dividerColor(showWallpaperBackground),
    )
}
