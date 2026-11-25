package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Safe-drawing padding that follows the keyboard without relayout. The content is padded for the
 * keyboard's final height and a graphics layer slides it from where the keyboard is now, so each
 * IME animation frame only moves a layer instead of re-measuring and re-recording the whole
 * content. Only valid for content anchored to the bottom; top-anchored content would slide too.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Modifier.safeDrawingPaddingWithImeSlide(enabled: Boolean): Modifier {
    if (!enabled) return safeDrawingPadding()
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    val bars = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    return graphicsLayer {
        // The keyboard overlaps the navigation bar, so the padding is the larger of the two, not
        // their sum. Slide by the difference in that padding, or the content jumps by the
        // navigation bar height as the keyboard starts opening or closing.
        val barsBottom = bars.getBottom(this)
        translationY =
            (maxOf(imeTarget.getBottom(this), barsBottom) - maxOf(ime.getBottom(this), barsBottom))
                .toFloat()
    }
        .windowInsetsPadding(bars.union(imeTarget))
        // Children still use imePadding(); consume the live insets so they don't pad twice.
        .consumeWindowInsets(WindowInsets.safeDrawing)
}

/**
 * For a clipping viewport (such as a scroll container) inside [safeDrawingPaddingWithImeSlide]:
 * that layer is already sized for the keyboard's final height and slid back down, which leaves the
 * strip above it empty until the keyboard finishes. Grow the viewport upward by the same slide so
 * its top stays at the screen edge and the content there rides up with the rest instead of being
 * cut off on the first frame. Read in layout; the children keep their constraints.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Modifier.extendAboveImeSlide(enabled: Boolean): Modifier {
    if (!enabled) return this
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    val bars = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    return layout { measurable, constraints ->
        val barsBottom = bars.getBottom(this)
        val slide =
            (maxOf(imeTarget.getBottom(this), barsBottom) - maxOf(ime.getBottom(this), barsBottom)).coerceAtLeast(0)
        if (slide == 0 || !constraints.hasBoundedHeight) {
            val placeable = measurable.measure(constraints)
            return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        val height = constraints.maxHeight + slide
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(placeable.width, constraints.maxHeight) { placeable.place(0, -slide) }
    }
}

/**
 * [height] held at its value from when the current keyboard animation (or [holdWhile]) started
 * until it ends.
 * Composition that sizes itself from a height that shrinks with the keyboard (such as
 * BoxWithConstraints content) then stays skippable for the whole slide and updates once it settles,
 * instead of recomposing on every animation frame.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rememberImeSettledHeight(height: Dp, holdWhile: () -> Boolean = { false }): Dp {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    val currentHoldWhile by rememberUpdatedState(holdWhile)
    val isHolding by remember(ime, imeTarget, density) {
        derivedStateOf { ime.getBottom(density) != imeTarget.getBottom(density) || currentHoldWhile() }
    }
    val heldHeight = remember(isHolding) { height }
    return if (isHolding) heldHeight else height
}

/** Whether the keyboard is open or animating open: its animation target is above the screen edge. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rememberImeTargetVisible(): State<Boolean> {
    val density = LocalDensity.current
    val imeTarget = WindowInsets.imeAnimationTarget
    return remember(imeTarget, density) { derivedStateOf { imeTarget.getBottom(density) > 0 } }
}

/** Whether the keyboard is mid-way through opening (not yet at its target height). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rememberImeOpening(): State<Boolean> {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    return remember(ime, imeTarget, density) {
        derivedStateOf {
            val target = imeTarget.getBottom(density)
            target > 0 && ime.getBottom(density) < target
        }
    }
}

/**
 * Lets the opening keyboard cover this bottom-anchored content instead of pushing it up: its height
 * shrinks by how far the keyboard has risen past the navigation bar, so it holds still on screen
 * while the keyboard slides over it, and the content above only starts moving once it is covered.
 * Height is read in layout, so the keyboard animation relayouts without recomposing.
 */
@Composable
internal fun Modifier.collapseUnderRisingKeyboard(): Modifier {
    val ime = WindowInsets.ime
    val navigationBars = WindowInsets.navigationBars
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val navigationBottom = navigationBars.getBottom(this)
        val keyboardRise = maxOf(ime.getBottom(this), navigationBottom) - navigationBottom
        val height = (placeable.height - keyboardRise).coerceIn(0, placeable.height)
        layout(placeable.width, height) { placeable.place(0, 0) }
    }
}
