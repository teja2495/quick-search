package com.tk.quicksearch.search.apps

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.shared.util.hapticConfirm

// Holding a pinned tile still this long, from touch down, picks it up: it can then drag, or opens
// its options menu when released without moving.
private const val HoldMillis = 300L

private enum class PressOutcome {
    RELEASED,
    MOVED,
}

/** Waits for the press to end or move past the touch slop; never returns while it's held still. */
private suspend fun AwaitPointerEventScope.awaitPressOutcome(down: PointerInputChange): PressOutcome {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: return PressOutcome.MOVED
        if (change.changedToUp()) return PressOutcome.RELEASED
        if (
            change.isConsumed ||
                (change.position - down.position).getDistance() > viewConfiguration.touchSlop
        ) {
            return PressOutcome.MOVED
        }
    }
}

/**
 * Gestures for a reorderable pinned grid tile: tap clicks, and holding it still for [HoldMillis]
 * picks it up. Moving it after that drags the tile, while releasing it without moving opens its
 * options menu; moving before then does nothing. [onPinnedDragEnd] receives whether the drag ended with the
 * finger lifting, rather than being cancelled. [onHoldChange] reports the tile being held, from the
 * hold until the finger lifts, including any drag. Returns [Modifier] when dragging is disabled.
 */
@Composable
internal fun rememberPinnedGridDragModifier(
        key: Any,
        onClick: () -> Unit,
        onShowOptions: () -> Unit,
        onLocalDraggingChange: (Boolean) -> Unit,
        onPinnedDragStart: (() -> Unit)?,
        onPinnedDrag: ((Float, Float) -> Unit)?,
        onPinnedDragEnd: ((Boolean) -> Unit)?,
        onHoldChange: ((Boolean) -> Unit)? = null,
): Modifier {
    val view = LocalView.current
    val currentClick by rememberUpdatedState(onClick)
    val currentShowOptions by rememberUpdatedState(onShowOptions)
    val currentLocalDraggingChange by rememberUpdatedState(onLocalDraggingChange)
    val currentPinnedDragStart by rememberUpdatedState(onPinnedDragStart)
    val currentPinnedDrag by rememberUpdatedState(onPinnedDrag)
    val currentPinnedDragEnd by rememberUpdatedState(onPinnedDragEnd)
    val currentHoldChange by rememberUpdatedState(onHoldChange)
    if (onPinnedDragStart == null || onPinnedDrag == null || onPinnedDragEnd == null) {
        return Modifier
    }
    // The tile is translated and scaled while it drags, so finger movement is measured in root
    // coordinates; local deltas would be shrunk by the drag scale and the tile would lag the finger.
    val coordinatesHolder = remember { arrayOfNulls<LayoutCoordinates>(1) }
    return Modifier.onGloballyPositioned { coordinatesHolder[0] = it }.pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            val pressOutcome = withTimeoutOrNull(HoldMillis) { awaitPressOutcome(down) }
            when (pressOutcome) {
                PressOutcome.RELEASED -> {
                    currentEvent.changes.forEach { it.consume() }
                    hapticConfirm(view)()
                    currentClick()
                    return@awaitEachGesture
                }
                PressOutcome.MOVED -> return@awaitEachGesture
                null -> Unit
            }
            hapticConfirm(view)()
            currentHoldChange?.invoke(true)
            try {
                val drag =
                        awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                                ?: run {
                                    val released =
                                            currentEvent.changes.any {
                                                it.id == down.id && it.changedToUp()
                                            }
                                    currentEvent.changes.forEach { it.consume() }
                                    // Lifted without moving opens the menu; a cancel doesn't.
                                    if (released) currentShowOptions()
                                    return@awaitEachGesture
                                }
                currentLocalDraggingChange(true)
                currentPinnedDragStart?.invoke()
                var completed = false
                try {
                    // Includes the touch slop, so the tile starts exactly under the finger.
                    val initialDrag = drag.position - down.position
                    if (initialDrag != Offset.Zero) {
                        currentPinnedDrag?.invoke(initialDrag.x, initialDrag.y)
                    }
                    var lastRootPosition = coordinatesHolder[0]?.takeIf { it.isAttached }?.localToRoot(drag.position)
                    completed =
                            drag(drag.id) { change ->
                                val coordinates = coordinatesHolder[0]?.takeIf { it.isAttached }
                                val rootPosition = coordinates?.localToRoot(change.position)
                                val dragAmount =
                                        if (rootPosition != null && lastRootPosition != null) {
                                            rootPosition - lastRootPosition!!
                                        } else {
                                            change.positionChange()
                                        }
                                lastRootPosition = rootPosition
                                change.consume()
                                currentPinnedDrag?.invoke(dragAmount.x, dragAmount.y)
                            }
                } finally {
                    currentLocalDraggingChange(false)
                    currentPinnedDragEnd?.invoke(completed)
                }
            } finally {
                currentHoldChange?.invoke(false)
            }
        }
    }
}
