package com.tk.quicksearch.search.searchScreen.shared

import com.tk.quicksearch.search.searchScreen.frostedCardBackground
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.foundation.layout.PaddingValues
import com.tk.quicksearch.shared.ui.components.SegmentedTile

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
    SegmentedTile(
        index = index,
        count = count,
        modifier = modifier.padding(top = if (index > 0) DesignTokens.SegmentedListGap else 0.dp),
        onClick = onClick,
        containerColor = style.containerColor,
        backdrop = { m, shape -> m.frostedCardBackground(shape) },
        contentPadding =
            PaddingValues(
                horizontal = DesignTokens.SegmentedListTileHorizontalPadding,
                vertical = DesignTokens.SegmentedListTileVerticalPadding,
            ),
        pressMorph = false,
        content = content,
    )
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
