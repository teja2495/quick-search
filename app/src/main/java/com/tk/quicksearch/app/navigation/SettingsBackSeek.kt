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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
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

/** Time to rewind a fully previewed back; shorter previews rewind proportionally faster. */
private const val CANCEL_DURATION_MS = AppMotion.DurationShort.toFloat()
