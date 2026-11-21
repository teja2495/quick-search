package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.searchScreen.ExpandedSection
import com.tk.quicksearch.shared.ui.theme.AppMotion

/** Distance the results drift while they fade in after a More / Collapse switch. */
private val ExpandRevealOffset = 16.dp

/**
 * Subtle fade-through for the results area when a section or search history expands into its
 * full view, or collapses back. Content composes once (no crossfade copy); only its alpha and a
 * small vertical drift animate. Expanding drifts up into place, collapsing drifts down.
 */
@Composable
internal fun rememberExpandRevealModifier(
    expandedSection: ExpandedSection,
    isSearchHistoryExpanded: Boolean,
): Modifier {
    val key = expandedSection to isSearchHistoryExpanded
    val isExpanded = expandedSection != ExpandedSection.NONE || isSearchHistoryExpanded
    var revealedKey by remember { mutableStateOf(key) }
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(key) {
        if (revealedKey != key) {
            reveal.snapTo(0f)
            revealedKey = key
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EmphasizedDecelerateEasing),
            )
        }
    }
    // Hide the first frame of the new content before the effect resets the animation.
    val revealPending = revealedKey != key
    val offsetPx = with(LocalDensity.current) { ExpandRevealOffset.toPx() }
    val direction = if (isExpanded) 1f else -1f
    return Modifier.graphicsLayer {
        val progress = if (revealPending) 0f else reveal.value
        alpha = progress
        translationY = (1f - progress) * offsetPx * direction
    }
}

/** Fades and scales the floating Collapse pill in and out with the expanded view. */
@Composable
internal fun AnimatedCollapseButton(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            fadeIn(tween(AppMotion.DurationStandard, delayMillis = AppMotion.DurationQuick)) +
                scaleIn(
                    animationSpec = tween(AppMotion.DurationStandard, delayMillis = AppMotion.DurationQuick, easing = AppMotion.EmphasizedDecelerateEasing),
                    initialScale = 0.85f,
                ),
        exit =
            fadeOut(tween(AppMotion.DurationExit, easing = AppMotion.EmphasizedAccelerateEasing)) +
                scaleOut(
                    animationSpec = tween(AppMotion.DurationExit, easing = AppMotion.EmphasizedAccelerateEasing),
                    targetScale = 0.85f,
                ),
    ) {
        content()
    }
}

/** Distance the Home content drifts while it fades in after the query is cleared. */
private val ClearRevealOffset = 8.dp

/**
 * Subtle fade-in for the results area after the clear (X) button empties the query. Each tap bumps
 * [clearKey]; the stale results hide at once and the Home content fades in with a small downward
 * settle, so the switch doesn't read as a hard cut.
 */
@Composable
internal fun rememberClearRevealModifier(clearKey: Int): Modifier {
    var revealedKey by remember { mutableStateOf(clearKey) }
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(clearKey) {
        if (revealedKey != clearKey) {
            reveal.snapTo(0f)
            revealedKey = clearKey
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(AppMotion.DurationStandard, easing = AppMotion.EmphasizedDecelerateEasing),
            )
        }
    }
    val revealPending = revealedKey != clearKey
    val offsetPx = with(LocalDensity.current) { ClearRevealOffset.toPx() }
    return Modifier.graphicsLayer {
        val progress = if (revealPending) 0f else reveal.value
        alpha = progress
        translationY = -(1f - progress) * offsetPx
    }
}
