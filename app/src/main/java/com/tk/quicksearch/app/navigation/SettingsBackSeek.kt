package com.tk.quicksearch.app.navigation

import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.shared.ui.components.PredictiveBackPreviewHandler
import com.tk.quicksearch.shared.ui.components.PredictiveBackPreviewState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.util.hapticGestureThreshold
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Drives the Settings page switch from the predictive back gesture: the page the user is going
 * back to slides in with the gesture (the same transition a tap on back plays), a commit lets it
 * finish, and a cancel slides it away again.
 */
@Stable
internal class SettingsBackSeek(
    initial: SettingsDetailType?,
) {
    val transitionState = SeekableTransitionState(initial)

    /** Where back leads from the current page; meaningful only while [canSeek]. */
    internal var backTarget: SettingsDetailType? = null

    /** False when back leaves Settings entirely (e.g. a note opened from search). */
    internal var canSeek by mutableStateOf(false)

    /** Navigation direction override for the change in progress; dropped while seeking back. */
    internal var directionOverride: SwipeAnimationDirection? = null

    internal var isSeeking by mutableStateOf(false)
        private set

    internal suspend fun seekTo(fraction: Float) {
        isSeeking = true
        transitionState.seekTo(fraction.coerceIn(0f, 1f), targetState = backTarget)
    }

    /**
     * Rewinds the seek to the current page, then settles there without a transition. Animating to
     * the current page instead would play a fresh "forward" switch, making the page jump in again.
     */
    internal suspend fun cancel() {
        val startFraction = transitionState.fraction
        if (startFraction > 0f) {
            val durationNanos = CANCEL_DURATION_MS * 1_000_000L * startFraction
            val startNanos = withFrameNanos { it }
            while (true) {
                val elapsed = (withFrameNanos { it } - startNanos) / durationNanos
                val t = elapsed.coerceAtMost(1f)
                transitionState.seekTo(startFraction * (1f - AppMotion.EmphasizedEasing.transform(t)), targetState = backTarget)
                if (t >= 1f) break
            }
        }
        transitionState.snapTo(transitionState.currentState)
        isSeeking = false
    }

    internal fun committed() {
        isSeeking = false
    }
}

internal val LocalSettingsBackSeek = staticCompositionLocalOf<SettingsBackSeek?> { null }

@Composable
internal fun rememberSettingsBackSeek(
    settingsDetailType: SettingsDetailType?,
    previousSettingsDetailType: SettingsDetailType?,
    directionOverride: SwipeAnimationDirection?,
): SettingsBackSeek {
    val seek = remember { SettingsBackSeek(settingsDetailType) }
    // Captured during composition: the override is consumed by an effect before the transition
    // picks up the new target, so reading it later would always see null.
    seek.directionOverride = remember(settingsDetailType) { directionOverride }
    val current = settingsDetailType
    seek.canSeek =
        current != null && !(current == SettingsDetailType.NOTE_EDITOR && previousSettingsDetailType == null)
    seek.backTarget =
        if (current != null && current.isNestedDetail()) current.resolveBackDestination(previousSettingsDetailType) else null
    LaunchedEffect(settingsDetailType) {
        val state = seek.transitionState
        if (state.currentState != settingsDetailType || state.targetState != settingsDetailType) {
            state.animateTo(settingsDetailType)
        }
        seek.committed()
    }
    return seek
}

/**
 * Back handler for a Settings page. Inside the Settings page switcher the gesture seeks the
 * switch to the previous page; elsewhere (or while [preview] is off, for a back that stays on the
 * page) it falls back to [PredictiveBackPreviewHandler].
 */
@Composable
internal fun SettingsPageBackHandler(
    previewState: PredictiveBackPreviewState,
    preview: Boolean = true,
    onBack: () -> Unit,
) {
    val seek = LocalSettingsBackSeek.current
    if (seek == null || !seek.canSeek || !preview) {
        PredictiveBackPreviewHandler(previewState, preview = preview, onBack = onBack)
        return
    }
    val latestOnBack by rememberUpdatedState(onBack)
    PredictiveBackHandler { events ->
        try {
            events.collect { event -> seek.seekTo(event.progress) }
            // Navigating sets the same target, so the switch finishes from where the finger left it.
            latestOnBack()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { seek.cancel() }
            throw cancelled
        }
    }
}

/**
 * Swipe right to go back from a Settings page. Inside the Settings page switcher the previous page
 * tracks the finger (the same seek the predictive back gesture drives); a flick or a drag past the
 * threshold commits, anything else slides it away again. Without a seekable switcher (or while
 * [seekable] is off, for a back that stays on the page) [onBack] runs on a committed release.
 * Children that consume horizontal drags (sliders, horizontal lists) keep them.
 */
@Composable
internal fun Modifier.settingsSwipeBack(
    enabled: Boolean = true,
    seekable: Boolean = true,
    onBack: () -> Unit,
): Modifier {
    if (!enabled) return this
    val seek = LocalSettingsBackSeek.current?.takeIf { seekable && it.canSeek }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val latestOnBack by rememberUpdatedState(onBack)
    return pointerInput(seek) {
        val velocityTracker = VelocityTracker()
        val flingVelocityPx = SWIPE_BACK_FLING_VELOCITY.toPx()
        val thresholdPx = SWIPE_BACK_THRESHOLD.toPx()
        val fraction = MutableStateFlow(0f)
        var seekJob: Job? = null
        var total = 0f
        var decided = false
        var tracking = false
        var pastThreshold = false

        fun cancelSeek() {
            val job = seekJob ?: return
            seekJob = null
            scope.launch {
                job.cancelAndJoin()
                seek?.cancel()
            }
        }

        try {
            detectHorizontalDominantDrag(
                onDragStart = {
                    velocityTracker.resetTracking()
                    total = 0f
                    decided = false
                    tracking = false
                    pastThreshold = false
                },
                onHorizontalDrag = { change, amount ->
                    velocityTracker.addPointerInputChange(change)
                    total += amount
                    if (!decided && total != 0f) {
                        decided = true
                        tracking = total > 0f
                        if (tracking && seek != null) {
                            fraction.value = 0f
                            seekJob = scope.launch { fraction.collect { seek.seekTo(it) } }
                        }
                    }
                    if (tracking) {
                        val distance = total.coerceAtLeast(0f)
                        if (size.width > 0) fraction.value = distance / size.width
                        if ((distance >= thresholdPx) != pastThreshold) {
                            pastThreshold = !pastThreshold
                            hapticGestureThreshold(view, pastThreshold)
                        }
                    }
                },
                onDragEnd = {
                    if (tracking) {
                        tracking = false
                        val velocity = velocityTracker.calculateVelocity().x
                        val commit =
                            when {
                                velocity >= flingVelocityPx -> total >= thresholdPx * SWIPE_BACK_MIN_FLING_FRACTION
                                velocity <= -flingVelocityPx -> false
                                else -> total >= thresholdPx
                            }
                        if (commit) {
                            // Navigating sets the same target, so the switch finishes from the finger's position.
                            seekJob?.cancel()
                            seekJob = null
                            latestOnBack()
                        } else {
                            cancelSeek()
                        }
                    }
                },
                onDragCancel = {
                    if (tracking) cancelSeek()
                    tracking = false
                },
            )
        } finally {
            // Restarted or disposed mid-drag: never leave the previous page half-revealed.
            if (tracking) cancelSeek()
        }
    }
}

private val SWIPE_BACK_THRESHOLD = 56.dp
private val SWIPE_BACK_FLING_VELOCITY = 400.dp

/** A fling still needs this share of the threshold, so a stray flick does not navigate. */
private const val SWIPE_BACK_MIN_FLING_FRACTION = 0.25f

/** Time to rewind a fully previewed back; shorter previews rewind proportionally faster. */
private const val CANCEL_DURATION_MS = AppMotion.DurationShort.toFloat()
