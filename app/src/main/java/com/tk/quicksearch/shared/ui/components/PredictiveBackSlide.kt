package com.tk.quicksearch.shared.ui.components

import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** How far a page has been slid off by the back gesture, from 0 (in place) to 1 (gone). */
@Stable
class PredictiveBackSlideState {
    var offset by mutableFloatStateOf(0f)
        internal set
}

@Composable
fun rememberPredictiveBackSlideState(): PredictiveBackSlideState = remember { PredictiveBackSlideState() }

/**
 * Back handler for a page in its own see-through window over the page it returns to: the page
 * slides off to the right with the gesture (from either edge, like the in-app Settings pages),
 * revealing the one beneath. A commit slides it the rest of the way
 * and then calls [onBack], which should close it without a further animation; a cancel slides it
 * back.
 */
@Composable
fun PredictiveBackSlideHandler(
    state: PredictiveBackSlideState,
    enabled: Boolean = true,
    onBack: () -> Unit,
) {
    val latestOnBack by rememberUpdatedState(onBack)
    PredictiveBackHandler(enabled = enabled) { events ->
        try {
            events.collect { event ->
                state.offset = event.progress
            }
            state.animateOffsetTo(1f)
            latestOnBack()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { state.animateOffsetTo(0f) }
            throw cancelled
        }
    }
}

private suspend fun PredictiveBackSlideState.animateOffsetTo(target: Float) {
    val distance = kotlin.math.abs(target - offset)
    if (distance <= 0f) return
    val duration = (SLIDE_DURATION_MS * distance).toInt().coerceAtLeast(1)
    animate(offset, target, animationSpec = tween(duration, easing = AppMotion.EmphasizedEasing)) { value, _ ->
        offset = value
    }
}

/** Slides the page off to the right by [state]'s offset. Reads it only in the draw phase. */
fun Modifier.predictiveBackSlide(state: PredictiveBackSlideState): Modifier =
    graphicsLayer {
        val offset = state.offset
        if (offset <= 0f) return@graphicsLayer
        translationX = size.width * offset
    }

/** Time to slide a page fully on or off; partial slides take proportionally less. */
private const val SLIDE_DURATION_MS = 250f
