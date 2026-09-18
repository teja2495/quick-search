package com.tk.quicksearch.search.apps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

private val PinnedGridPlacementSpring =
        spring(
                stiffness = Spring.StiffnessMedium,
                visibilityThreshold = IntOffset(1, 1),
        )

/**
 * Slides a reorderable pinned grid tile to its new slot when the grid reorders, instead of jumping,
 * and glides a dropped tile from where it was released into its slot. The tile being dragged follows
 * the finger through its own drag offset, so this leaves it alone until it is dropped. Does nothing
 * when not [enabled].
 */
@Composable
internal fun Modifier.animatePinnedGridPlacement(
        enabled: Boolean,
        isDragging: Boolean,
        dragOffset: IntOffset?,
): Modifier {
    if (!enabled) return this
    val scope = rememberCoroutineScope()
    // Offset from the tile's slot to where it is drawn, animating to zero.
    val animatedOffset = remember { Animatable(IntOffset.Zero, IntOffset.VectorConverter) }
    // Start offset of an animation whose coroutine hasn't run yet. Placement reads it so the frame in
    // between doesn't draw the tile in the wrong place.
    val pendingOffsetHolder = remember { arrayOfNulls<IntOffset>(1) }
    val slotHolder = remember { arrayOfNulls<IntOffset>(1) }
    val releaseOffsetHolder = remember { arrayOfNulls<IntOffset>(1) }
    var isSettlingDrop by remember { mutableStateOf(false) }
    val currentIsDragging by rememberUpdatedState(isDragging)

    fun animateFrom(startOffset: IntOffset, isDrop: Boolean) {
        pendingOffsetHolder[0] = startOffset
        if (isDrop) isSettlingDrop = true
        scope.launch {
            try {
                animatedOffset.snapTo(startOffset)
                if (pendingOffsetHolder[0] == startOffset) pendingOffsetHolder[0] = null
                animatedOffset.animateTo(IntOffset.Zero, PinnedGridPlacementSpring)
            } finally {
                if (isDrop) isSettlingDrop = false
            }
        }
    }

    SideEffect {
        if (isDragging) {
            releaseOffsetHolder[0] = dragOffset ?: IntOffset.Zero
        } else {
            releaseOffsetHolder[0]?.let { releaseOffset ->
                releaseOffsetHolder[0] = null
                animateFrom(releaseOffset, isDrop = true)
            }
        }
    }

    return this
            .zIndex(if (isSettlingDrop) 1f else 0f)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    val slot = coordinates?.positionInParent()?.round()
                    var drawnOffset = pendingOffsetHolder[0] ?: animatedOffset.value
                    if (slot != null && !isLookingAhead) {
                        val previousSlot = slotHolder[0]
                        slotHolder[0] = slot
                        // Starts from where the tile was drawn, in this same placement pass.
                        if (previousSlot != null && previousSlot != slot && !currentIsDragging) {
                            drawnOffset += previousSlot - slot
                            animateFrom(drawnOffset, isDrop = isSettlingDrop)
                        }
                    }
                    placeable.place(if (currentIsDragging) IntOffset.Zero else drawnOffset)
                }
            }
}
