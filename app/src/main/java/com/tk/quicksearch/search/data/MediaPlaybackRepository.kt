package com.tk.quicksearch.search.data

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.media.session.PlaybackState
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsListenerService
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.data.preferences.MediaPreferences

/**
 * Reads the system's active media sessions. This piggybacks on the notification-listener grant
 * already used for [com.tk.quicksearch.search.apps.notificationDots.NotificationDotsListenerService]
 * rather than declaring a second listener service, so users who already granted notification
 * access for dots do not need to grant it again for the media card.
 */
class MediaPlaybackRepository(private val context: Context) {
    private val preferences = MediaPreferences(context)
    private val mediaSessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, NotificationDotsListenerService::class.java)

    fun isEnabled(): Boolean = preferences.isShowNowPlayingEnabled()

    fun hasAccess(): Boolean = NotificationDotsPermission.hasNotificationListenerAccess(context)

    private fun activeSessions(): List<MediaController> =
        runCatching { mediaSessionManager?.getActiveSessions(listenerComponent) }
            .getOrNull()
            .orEmpty()

    /** The system-priority session that is playing, paused, or buffering, if any. */
    private fun currentController(): MediaController? =
        activeSessions().firstOrNull { controller ->
            when (runCatching { controller.playbackState }.getOrNull()?.state) {
                PlaybackState.STATE_PLAYING,
                PlaybackState.STATE_PAUSED,
                PlaybackState.STATE_BUFFERING,
                -> true
                else -> false
            }
        }

    /**
     * The session for the At a Glance card: one that is playing or buffering first, so a paused
     * session (which the card hides) does not mask one playing behind it; otherwise the
     * system-priority paused one.
     */
    fun activeController(): MediaController? {
        if (!isEnabled() || !hasAccess()) return null
        return activeSessions().firstOrNull { controller ->
            when (runCatching { controller.playbackState }.getOrNull()?.state) {
                PlaybackState.STATE_PLAYING,
                PlaybackState.STATE_BUFFERING,
                -> true
                else -> false
            }
        } ?: currentController()
    }

    /**
     * The system-priority session, independent of the At a Glance toggle, matching the session
     * that custom widget media keys reach.
     */
    fun priorityController(): MediaController? {
        if (!hasAccess()) return null
        return currentController()
    }

    /**
     * Whether the system-priority session is actively playing, independent of the At a Glance
     * toggle. Custom widget buttons dispatch media commands to that same priority session, so
     * their Play/Pause icon must not be held in the playing state by an older session.
     */
    fun isCurrentlyPlaying(): Boolean {
        if (!hasAccess()) return false
        return runCatching { currentController()?.playbackState?.state }.getOrNull() == PlaybackState.STATE_PLAYING
    }

    /** Registers [onChanged] for whenever the set of active sessions changes; null if unavailable. */
    fun registerSessionsListener(onChanged: () -> Unit): OnActiveSessionsChangedListener? {
        val manager = mediaSessionManager ?: return null
        val listener = OnActiveSessionsChangedListener { onChanged() }
        return runCatching {
            manager.addOnActiveSessionsChangedListener(listener, listenerComponent)
            listener
        }.getOrNull()
    }

    fun unregisterSessionsListener(listener: OnActiveSessionsChangedListener?) {
        listener ?: return
        runCatching { mediaSessionManager?.removeOnActiveSessionsChangedListener(listener) }
    }
}
