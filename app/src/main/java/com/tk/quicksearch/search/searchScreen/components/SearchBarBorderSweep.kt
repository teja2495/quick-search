package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.tk.quicksearch.shared.ui.theme.AppMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Animated values of the search bar's welcome border sweep, read during draw. */
@Stable
internal class SearchBarBorderSweep(
    play: Boolean,
) {
    internal val progressAnim = Animatable(0f)
    internal val glowAlphaAnim = Animatable(0f)
    internal val borderAlphaAnim = Animatable(if (play) 0f else 1f)

    /** How far the gradient has scanned across the border, 0 to 1. */
    val progress: Float get() = progressAnim.value

    /** Opacity of the gradient glow. */
    val glowAlpha: Float get() = glowAlphaAnim.value

    /** Opacity of the resting border. */
    val borderAlpha: Float get() = borderAlphaAnim.value
}

/**
 * Welcome animation: when [play], the gradient scans across the border once, ending on its white
 * tail, then the glow fades out while the resting border fades in and [onCompleted] runs.
 * Otherwise the resting border shows, fading back in when the query is not empty.
 */
@Composable
internal fun rememberSearchBarBorderSweep(
    play: Boolean,
    queryIsEmpty: Boolean,
    onCompleted: (() -> Unit)?,
): SearchBarBorderSweep {
    val sweep = remember { SearchBarBorderSweep(play) }
    LaunchedEffect(play, queryIsEmpty) {
        if (play) {
            sweep.glowAlphaAnim.snapTo(1f)
            sweep.borderAlphaAnim.snapTo(0f)
            sweep.progressAnim.snapTo(0f)

            sweep.progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(AppMotion.DurationLong, easing = LinearEasing),
            )

            // The white end of the gradient holds the glow; the border is not snapped to opaque here.
            delay(AppMotion.DurationMicro.toLong())

            launch {
                sweep.glowAlphaAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(AppMotion.DurationGlowFade, easing = AppMotion.EmphasizedDecelerateEasing),
                )
            }
            launch {
                sweep.borderAlphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(AppMotion.DurationGlowFade, easing = AppMotion.EmphasizedDecelerateEasing),
                )
            }

            delay(AppMotion.DurationGlowFade.toLong())
            onCompleted?.invoke()
        } else if (queryIsEmpty) {
            sweep.borderAlphaAnim.snapTo(1f)
        } else {
            sweep.borderAlphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = AppMotion.DurationGlowFade, easing = AppMotion.EmphasizedDecelerateEasing),
            )
        }
    }
    return sweep
}
