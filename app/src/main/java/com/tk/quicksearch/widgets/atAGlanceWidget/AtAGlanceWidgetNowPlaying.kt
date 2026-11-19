package com.tk.quicksearch.widgets.atAGlanceWidget

import android.content.Context
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.tk.quicksearch.media.MediaCommand
import com.tk.quicksearch.search.data.MediaPlaybackRepository
import com.tk.quicksearch.search.data.preferences.GlanceItem
import com.tk.quicksearch.search.data.preferences.GlanceItemSettings
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.NowPlayingTransitionStates
import com.tk.quicksearch.widgets.customButtonsWidget.CustomButtonsWidgetMediaAction
import com.tk.quicksearch.widgets.mediaControlsWidget.MediaControlsSnapshot
import com.tk.quicksearch.widgets.mediaControlsWidget.readMediaControlsSnapshot

/**
 * When the widget's Now Playing row shows, following home's card: while media plays, and after a
 * pause from the widget until that session plays again. A dismissal hides the session until it
 * plays again after a pause, or right away when it was dismissed while paused. Like home's card,
 * the row is held for [HOLD_MILLIS] after it would hide, as sessions briefly stop between tracks.
 * Process-wide, like home's; a dismissal or pause is forgotten when the process ends.
 */
internal object AtAGlanceWidgetNowPlayingState {
    private const val HOLD_MILLIS = 2_000L

    private var dismissedToken: MediaSession.Token? = null
    private var dismissedWhilePlaying = false
    private var sawNonPlayingSinceDismiss = false
    private var pausedFromWidgetToken: MediaSession.Token? = null
    private var sawPausedSincePause = false
    private var lastShown: MediaControlsSnapshot? = null
    private var hideStartedAt: Long? = null

    /**
     * What the row shows for [media], the current session or null without one: [media] while it
     * shows, then the last shown session through the hold, and a redraw is requested for when the
     * hold ends.
     */
    @Synchronized
    fun resolve(
        media: MediaControlsSnapshot?,
        nowMillis: Long,
    ): MediaControlsSnapshot? {
        if (media != null && isShown(media)) {
            lastShown = media
            hideStartedAt = null
            return media
        }
        val held = lastShown ?: return null
        if (media != null && media.sessionToken == dismissedToken) return clearHold()
        val startedAt = hideStartedAt ?: nowMillis.also { hideStartedAt = it }
        val remaining = startedAt + HOLD_MILLIS - nowMillis
        if (remaining <= 0) return clearHold()
        AtAGlanceWidgetUpdater.requestRefreshAfter(remaining)
        // The same session may already report its next track.
        return if (media != null && media.sessionToken == held.sessionToken) media else held
    }

    private fun clearHold(): MediaControlsSnapshot? {
        lastShown = null
        hideStartedAt = null
        return null
    }

    /** Whether [media] shows, after updating the dismissal and pause with its current state. */
    private fun isShown(media: MediaControlsSnapshot): Boolean {
        val token = media.sessionToken ?: return false
        val isPlaying = media.playbackState == PlaybackState.STATE_PLAYING
        if (dismissedToken != null) {
            when {
                dismissedToken != token -> dismissedToken = null
                !isPlaying -> sawNonPlayingSinceDismiss = true
                !dismissedWhilePlaying || sawNonPlayingSinceDismiss -> dismissedToken = null
            }
        }
        // Pausing sets the token while the state still reads playing, so only a resume seen after
        // the pause landed (or a switch to another session) clears it.
        if (pausedFromWidgetToken != null) {
            when {
                pausedFromWidgetToken != token || (isPlaying && sawPausedSincePause) -> pausedFromWidgetToken = null
                !isPlaying -> sawPausedSincePause = true
            }
        }
        if (dismissedToken == token) return false
        return isPlaying || media.playbackState in NowPlayingTransitionStates || pausedFromWidgetToken == token
    }

    @Synchronized
    fun dismiss(
        token: MediaSession.Token,
        isPlaying: Boolean,
    ) {
        dismissedToken = token
        dismissedWhilePlaying = isPlaying
        sawNonPlayingSinceDismiss = false
        clearHold()
    }

    @Synchronized
    fun pausedFromWidget(token: MediaSession.Token) {
        pausedFromWidgetToken = token
        sawPausedSincePause = false
    }
}

/** The system-priority session for the widget's Now Playing row, or null when the row is hidden. */
internal fun loadAtAGlanceWidgetNowPlaying(
    context: Context,
    items: GlanceItemSettings,
): MediaControlsSnapshot? {
    if (!items.isShown(GlanceItem.NOW_PLAYING)) return null
    val media = runCatching { readMediaControlsSnapshot(context) }.getOrNull()
    if (media != null && !media.hasAccess) return null
    return AtAGlanceWidgetNowPlayingState.resolve(media?.takeIf { it.hasSession }, System.currentTimeMillis())
}

/** Hides the widget's Now Playing row for the current session. */
internal fun dismissAtAGlanceWidgetNowPlaying(context: Context) {
    val controller = MediaPlaybackRepository(context).priorityController() ?: return
    val isPlaying = runCatching { controller.playbackState?.state }.getOrNull() == PlaybackState.STATE_PLAYING
    AtAGlanceWidgetNowPlayingState.dismiss(controller.sessionToken, isPlaying)
}

/**
 * The Now Playing row's media buttons: [CustomButtonsWidgetMediaAction], plus a pause from here
 * keeps the row up so playback can be resumed from it.
 */
class AtAGlanceWidgetNowPlayingAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        if (MediaCommand.fromValue(parameters[CustomButtonsWidgetMediaAction.MEDIA_COMMAND_KEY]) == MediaCommand.PLAY_PAUSE) {
            MediaPlaybackRepository(context).priorityController()?.let { controller ->
                val state = runCatching { controller.playbackState?.state }.getOrNull()
                if (state == PlaybackState.STATE_PLAYING) AtAGlanceWidgetNowPlayingState.pausedFromWidget(controller.sessionToken)
            }
        }
        CustomButtonsWidgetMediaAction().onAction(context, glanceId, parameters)
    }
}
