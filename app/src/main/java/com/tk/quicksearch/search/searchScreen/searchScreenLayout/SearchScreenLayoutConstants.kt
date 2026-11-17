package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Custom shape that rounds only the top corners */
val TopRoundedShape =
    AppShapes.stacked(top = DesignTokens.CornerLarge, bottom = DesignTokens.CornerNone)

/**
 * Viewport of an expanded section (More tapped) in card layout: inset by [edgeBleed] to the card
 * edges and rounded like the cards, so rows scroll out under a rounded edge instead of a straight
 * cut. [roundTop] is false when the top edge already follows a top search bar.
 */
internal data class ExpandedResultsViewportShape(
    val edgeBleed: Dp,
    val roundTop: Boolean,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val inset = with(density) { edgeBleed.toPx() }
        val radius = with(density) { CornerRadius(DesignTokens.SegmentedListOuterCorner.toPx()) }
        val top = if (roundTop) radius else CornerRadius.Zero
        return Outline.Rounded(
            RoundRect(
                left = inset,
                top = 0f,
                right = size.width - inset,
                bottom = size.height,
                topLeftCornerRadius = top,
                topRightCornerRadius = top,
                bottomRightCornerRadius = radius,
                bottomLeftCornerRadius = radius,
            ),
        )
    }
}
