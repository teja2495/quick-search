package com.tk.quicksearch.overlay

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Height of the bubble's arrow; content below it has to be padded by this much. */
internal val OverlayBubbleArrowHeight = 12.dp
private val OverlayBubbleArrowWidth = 30.dp

/**
 * A rounded card with a pointed arrow on its top edge, like a chat bubble, aimed at
 * [arrowCenterX] (in px from the shape's left edge). The arrow flares smoothly into the card
 * edge and ends in a sharp tip. Used by the notch's Pinned & At a Glance popup to point at the
 * notch the user tapped.
 */
internal class OverlayBubbleShape(
    private val arrowCenterX: Float?,
    private val cornerRadius: Dp = 28.dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val arrowHeight = with(density) { OverlayBubbleArrowHeight.toPx() }
        val halfArrowWidth = with(density) { OverlayBubbleArrowWidth.toPx() } / 2f
        val radius = with(density) { cornerRadius.toPx() }
        // Keep the arrow on the straight part of the top edge, clear of the rounded corners.
        val minCenter = radius + halfArrowWidth
        val maxCenter = size.width - radius - halfArrowWidth
        val center =
            if (minCenter > maxCenter) {
                size.width / 2f
            } else {
                (arrowCenterX ?: (size.width / 2f)).coerceIn(minCenter, maxCenter)
            }
        val body =
            Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = arrowHeight,
                        right = size.width,
                        bottom = size.height,
                        cornerRadius = CornerRadius(radius),
                    ),
                )
            }
        // Starts and ends slightly inside the body so the union has no seam along the top edge.
        val baseY = arrowHeight + 1f
        val arrow =
            Path().apply {
                moveTo(center - halfArrowWidth, baseY)
                cubicTo(
                    center - halfArrowWidth * 0.45f, baseY,
                    center - halfArrowWidth * 0.12f, arrowHeight * 0.2f,
                    center, 0f,
                )
                cubicTo(
                    center + halfArrowWidth * 0.12f, arrowHeight * 0.2f,
                    center + halfArrowWidth * 0.45f, baseY,
                    center + halfArrowWidth, baseY,
                )
                close()
            }
        return Outline.Generic(Path.combine(PathOperation.Union, body, arrow))
    }
}
