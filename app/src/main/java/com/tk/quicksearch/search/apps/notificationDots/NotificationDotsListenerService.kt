package com.tk.quicksearch.search.apps.notificationDots

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.tk.quicksearch.search.notificationHistory.NotificationHistoryAccess
import com.tk.quicksearch.search.notificationHistory.NotificationHistoryStore
import com.tk.quicksearch.widgets.utils.refreshAllWidgets
import com.tk.quicksearch.widgets.utils.refreshMediaControlsWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationDotsListenerService : NotificationListenerService() {
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeSessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private val sessionCallbacks = mutableMapOf<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()
    private val widgetRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onListenerConnected() {
        super.onListenerConnected()
        val active = runCatching { activeNotifications }.getOrNull()
        NotificationDotsStore.updateFromNotifications(active)
        NotificationHistoryAccess.set(true)
        NotificationHistoryStore.seed(this, active)
        startMediaPlaybackMonitoring()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationDotsStore.clear()
        NotificationHistoryAccess.set(false)
        stopMediaPlaybackMonitoring()
    }

    override fun onDestroy() {
        widgetRefreshScope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        NotificationDotsStore.updateFromNotifications(runCatching { activeNotifications }.getOrNull())
        NotificationHistoryStore.record(this, sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        NotificationDotsStore.updateFromNotifications(runCatching { activeNotifications }.getOrNull())
    }

    /**
     * Mirrors active sessions' playback to placed widgets, so a Play/Pause button's icon (and the
     * media controls widget's track) updates as soon as playback changes rather than on the
     * widget's next unrelated redraw.
     */
    private fun startMediaPlaybackMonitoring() {
        val manager = getSystemService(MediaSessionManager::class.java) ?: return
        mediaSessionManager = manager
        val component = ComponentName(this, NotificationDotsListenerService::class.java)
        val listener =
            MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                onActiveSessionsChanged(controllers.orEmpty())
            }
        runCatching {
            manager.addOnActiveSessionsChangedListener(listener, component)
            activeSessionsListener = listener
            onActiveSessionsChanged(manager.getActiveSessions(component))
        }
    }

    private fun stopMediaPlaybackMonitoring() {
        activeSessionsListener?.let { listener ->
            runCatching { mediaSessionManager?.removeOnActiveSessionsChangedListener(listener) }
        }
        activeSessionsListener = null
        mediaSessionManager = null
        sessionCallbacks.values.forEach { (controller, callback) ->
            runCatching { controller.unregisterCallback(callback) }
        }
        sessionCallbacks.clear()
    }

    private fun onActiveSessionsChanged(controllers: List<MediaController>) {
        val activeTokens = controllers.map { it.sessionToken }.toSet()
        sessionCallbacks.keys.filter { it !in activeTokens }.forEach { token ->
            sessionCallbacks.remove(token)?.let { (controller, callback) ->
                runCatching { controller.unregisterCallback(callback) }
            }
        }
        controllers.forEach { controller ->
            if (sessionCallbacks.containsKey(controller.sessionToken)) return@forEach
            val callback =
                object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        // The callback is delivered as the session commits its new state. Do not
                        // immediately re-read controller.playbackState and suppress the update:
                        // that read can still expose the previous state (especially on pause),
                        // leaving the widget icon stuck on Pause.
                        refreshWidgets()
                    }

                    // Only the media controls widget shows the track, so a new title or artwork
                    // does not need to redraw the search widgets.
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        widgetRefreshScope.launch {
                            refreshMediaControlsWidgets(this@NotificationDotsListenerService)
                        }
                    }

                    override fun onSessionDestroyed() {
                        sessionCallbacks.remove(controller.sessionToken)
                    }
                }
            runCatching { controller.registerCallback(callback) }
            sessionCallbacks[controller.sessionToken] = controller to callback
        }
        refreshWidgets()
    }

    private fun refreshWidgets() {
        widgetRefreshScope.launch {
            refreshAllWidgets(this@NotificationDotsListenerService)
        }
    }
}
