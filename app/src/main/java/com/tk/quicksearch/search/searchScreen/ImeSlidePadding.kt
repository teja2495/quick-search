package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
