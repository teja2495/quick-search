package com.tk.quicksearch.shared.ui.components

import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Gesture progress of an in-flight predictive back on a full-screen page. */
@Stable
class PredictiveBackPreviewState {
    var progress by mutableFloatStateOf(0f)
        internal set
    var fromLeftEdge by mutableStateOf(true)
        internal set
}

@Composable
fun rememberPredictiveBackPreviewState(): PredictiveBackPreviewState = remember { PredictiveBackPreviewState() }

/**
 * Back handler that previews the back gesture on [state] (read it with [predictiveBackPreview]).
 * Pass `preview = false` while back only changes something inside the page (exiting a selection
 * mode, closing an inline panel), so the page does not shrink for an action that keeps it open.
 * On commit the page stays shrunk while it leaves; if it is still shown afterwards it springs back.
 */
@Composable
fun PredictiveBackPreviewHandler(
    state: PredictiveBackPreviewState,
    enabled: Boolean = true,
    preview: Boolean = true,
    onBack: () -> Unit,
) {
    val latestOnBack by rememberUpdatedState(onBack)
    val latestPreview by rememberUpdatedState(preview)
    val scope = rememberCoroutineScope()
    PredictiveBackHandler(enabled = enabled) { events ->
        try {
            events.collect { event ->
                if (latestPreview) {
                    state.fromLeftEdge = event.swipeEdge != BackEventCompat.EDGE_RIGHT
                    state.progress = event.progress
                }
            }
            latestOnBack()
            if (state.progress > 0f) {
                // Disposed with the page when it leaves; otherwise undo the shrink once it has had time to.
                scope.launch {
                    delay(SETTLE_DELAY_MS)
                    state.animateProgressTo(0f)
                }
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { state.animateProgressTo(0f) }
            throw cancelled
        }
    }
}

private suspend fun PredictiveBackPreviewState.animateProgressTo(target: Float) {
    animate(progress, target, animationSpec = tween(RESET_DURATION_MS, easing = AppMotion.EmphasizedEasing)) { value, _ ->
        progress = value
    }
}

/**
 * Material's full-screen back preview: the page shrinks, drifts away from the swiped edge and
 * rounds its corners as the gesture progresses. Reads [state] only in the draw phase.
 */
fun Modifier.predictiveBackPreview(state: PredictiveBackPreviewState): Modifier =
    graphicsLayer {
        val p = AppMotion.EmphasizedEasing.transform(state.progress.coerceIn(0f, 1f))
        if (p <= 0f) return@graphicsLayer
        val scale = 1f - MAX_SCALE_REDUCTION * p
        scaleX = scale
        scaleY = scale
        val edgeShift = (size.width / EDGE_SHIFT_DIVISOR - EDGE_MARGIN.toPx()).coerceAtLeast(0f)
        translationX = (if (state.fromLeftEdge) edgeShift else -edgeShift) * p
        shape = RoundedCornerShape(MAX_CORNER_RADIUS * p)
        clip = true
    }

private const val MAX_SCALE_REDUCTION = 0.1f
private const val EDGE_SHIFT_DIVISOR = 20f
private val EDGE_MARGIN = 8.dp
private val MAX_CORNER_RADIUS = 28.dp
private const val RESET_DURATION_MS = AppMotion.DurationShort
private const val SETTLE_DELAY_MS = 400L
