package com.tk.quicksearch.shared.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import com.tk.quicksearch.shared.ui.theme.AppMotion
import kotlinx.coroutines.launch

/**
 * Animates this node's height when [expanded] flips, such as a card showing more or fewer rows.
 *
 * Unlike `animateContentSize`, only the first height change after a toggle animates, so results
 * changing while the user types still snap into place. It clips only the bottom edge, and only
 * while animating, so content that bleeds past the sides (flat-card dividers) is never cut.
 */
fun Modifier.animateExpandCollapse(expanded: Boolean): Modifier = this then ExpandCollapseElement(expanded)

private data class ExpandCollapseElement(val expanded: Boolean) : ModifierNodeElement<ExpandCollapseNode>() {
    override fun create() = ExpandCollapseNode(expanded)

    override fun update(node: ExpandCollapseNode) = node.setExpanded(expanded)

    override fun InspectorInfo.inspectableProperties() {
        name = "animateExpandCollapse"
        properties["expanded"] = expanded
    }
}

private class ExpandCollapseNode(private var expanded: Boolean) : Modifier.Node(), LayoutModifierNode, DrawModifierNode {
    private var pendingToggle = false
    private var lastHeight = -1
    private var heightAnimation: Animatable<Int, AnimationVector1D>? = null

    fun setExpanded(value: Boolean) {
        if (value == expanded) return
        expanded = value
        pendingToggle = true
        invalidateMeasurement()
    }

    override fun onDetach() {
        heightAnimation = null
        pendingToggle = false
        lastHeight = -1
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val contentHeight = placeable.height
        if (isLookingAhead) {
            return layout(placeable.width, contentHeight) { placeable.place(0, 0) }
        }
        val running = heightAnimation
        val height =
            when {
                running != null -> {
                    if (running.targetValue != contentHeight) animateHeight(running.value, contentHeight)
                    running.value
                }
                pendingToggle && lastHeight >= 0 && lastHeight != contentHeight -> {
                    animateHeight(lastHeight, contentHeight)
                    lastHeight
                }
                else -> contentHeight
            }
        pendingToggle = false
        val clamped = height.coerceIn(constraints.minHeight, constraints.maxHeight)
        lastHeight = clamped
        return layout(placeable.width, clamped) { placeable.place(0, 0) }
    }

    private fun animateHeight(from: Int, to: Int) {
        val animation = heightAnimation ?: Animatable(from, Int.VectorConverter).also { heightAnimation = it }
        invalidateDraw()
        coroutineScope.launch {
            animation.animateTo(to, AppMotion.expandCollapse())
            // A retarget cancels this coroutine before here, so only the final run clears it.
            heightAnimation = null
            invalidateDraw()
            invalidateMeasurement()
        }
    }

    override fun ContentDrawScope.draw() {
        if (heightAnimation == null) {
            drawContent()
            return
        }
        clipRect(
            left = -ClipBleed,
            top = -ClipBleed,
            right = size.width + ClipBleed,
            bottom = size.height,
        ) {
            this@draw.drawContent()
        }
    }
}

/** How far past the sides and top drawing may go while the bottom edge is clipped. */
private const val ClipBleed = 100_000f
