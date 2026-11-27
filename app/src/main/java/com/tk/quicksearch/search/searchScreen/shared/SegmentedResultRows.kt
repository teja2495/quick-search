package com.tk.quicksearch.search.searchScreen.shared

import com.tk.quicksearch.search.searchScreen.frostedCardBackground
import androidx.compose.foundation.layout.Box
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
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.foundation.layout.PaddingValues
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.theme.animatedSegmentShape

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

/** Flat lists get their outer vertical spacing from the gap around the card's dividers. */
@Composable
internal fun resultRowsContainerVerticalPadding(value: Dp): Dp =
    if (LocalSearchCardLayoutEnabled.current) resultRowsContainerPadding(value) else 0.dp

/**
 * One row of a result list. Segmented, it becomes a tile at [index] of [count] (including More).
 * Flat rows add space next to internal dividers. Set [padCompactOuterEdges] to match
 * that inset at the card's outer dividers as well. Rows that come and go in place pass
 * [cornerIndex] and [cornerCount] to place their corners separately from their spacing; their
 * outer corners then morph when the tile becomes (or stops being) the first or last.
 */
@Composable
internal fun ResultSegment(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padCompactOuterEdges: Boolean = false,
    cornerIndex: Int? = null,
    cornerCount: Int? = null,
    content: @Composable () -> Unit,
) {
    val style = LocalSegmentedRowsStyle.current
    if (style == null) {
        if (LocalSearchCardLayoutEnabled.current || (count == 1 && !padCompactOuterEdges)) {
            content()
        } else {
            val dividerInset = LocalCardlessStackSpacing.current / 2
            Box(
                modifier = Modifier.fillMaxWidth().padding(
                    top = if (padCompactOuterEdges || index > 0) dividerInset else 0.dp,
                    bottom = if (padCompactOuterEdges || index < count - 1) dividerInset else 0.dp,
                ),
            ) {
                content()
            }
        }
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
        shape =
            if (cornerIndex != null && cornerCount != null) {
                animatedSegmentShape(cornerIndex, cornerCount, resultCardInnerCorner(), resultCardOuterCorner())
            } else if (resultCardOuterCorner() != DesignTokens.SegmentedListOuterCorner) {
                AppShapes.segment(index, count, resultCardInnerCorner(), resultCardOuterCorner())
            } else {
                null
            },
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
