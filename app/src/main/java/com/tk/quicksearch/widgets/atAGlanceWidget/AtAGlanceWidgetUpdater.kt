package com.tk.quicksearch.widgets.atAGlanceWidget

import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.tk.quicksearch.customInfo.CustomInfoRepository
import com.tk.quicksearch.search.data.AppNotificationsSettings
import com.tk.quicksearch.search.data.GlanceNotificationsStore
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.ACTION_WIFI_AP_STATE_CHANGED
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.CaptivePortalWifiCallback
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.registerWifiNetworkCallback
import com.tk.quicksearch.widgets.utils.refreshWidget
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Bursts of changes (a notification update, a calendar sync) redraw the widget once. */
private const val REFRESH_DEBOUNCE_MILLIS = 500L

/**
 * Keeps placed At a Glance widgets current while the app's process runs: it watches the notification
 * store, reminders, Custom Info and the device states the rows show, and redraws on a change. When
 * the process is gone, the receiver's scheduled refresh and system broadcasts bring the widget back.
 */
internal object AtAGlanceWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val litCameraIdsState = MutableStateFlow(emptySet<String>())
    private var job: Job? = null
    private var unregisterListeners: (() -> Unit)? = null
    private val isRefreshPending = AtomicBoolean(false)

    /** One redraw at a time, as Glance fails a second composition of the same widget running alongside. */
    private val refreshMutex = Mutex()

    /** Cameras whose torch is on, which the system only reports through a callback. */
    val litCameraIds: StateFlow<Set<String>> = litCameraIdsState.asStateFlow()

    fun requestRefresh() {
        requests.tryEmit(Unit)
    }

    /** [requestRefresh] once [delayMillis] has passed, such as when a held row is due to go. */
    fun requestRefreshAfter(delayMillis: Long) {
        scope.launch {
            delay(delayMillis)
            requestRefresh()
        }
    }

    /** Starts watching and redraws, for callers that change what the rows show while no widget is drawing. */
    fun startIfPlaced(context: Context) {
        val appContext = context.applicationContext
        val isPlaced =
            runCatching {
                AppWidgetManager
                    .getInstance(appContext)
                    .getAppWidgetIds(ComponentName(appContext, AtAGlanceWidgetReceiver::class.java))
                    .isNotEmpty()
            }.getOrDefault(false)
        if (!isPlaced) return
        start(appContext)
        requestRefresh()
    }

    @OptIn(FlowPreview::class)
    @Synchronized
    fun start(context: Context) {
        if (job != null) return
        val appContext = context.applicationContext
        // Each source's current value is already drawn, so only later changes trigger a redraw.
        val sources: List<Flow<Any?>> =
            listOf(
                GlanceNotificationsStore.timers,
                GlanceNotificationsStore.progress,
                GlanceNotificationsStore.finishedProgress,
                GlanceNotificationsStore.missedCalls,
                GlanceNotificationsStore.ongoingCalls,
                GlanceNotificationsStore.workouts,
                GlanceNotificationsStore.otps,
                GlanceNotificationsStore.weather,
                GlanceNotificationsStore.appNotifications,
                GlanceNotificationsStore.appNotificationCandidates,
                ReminderRepository.changes,
                CustomInfoRepository.changes,
                AppNotificationsSettings.config,
            ).map { it.drop(1) }
        unregisterListeners = registerDeviceListeners(appContext)
        job =
            scope.launch {
                merge(requests, *sources.map { flow -> flow.map { } }.toTypedArray())
                    .debounce(REFRESH_DEBOUNCE_MILLIS)
                    .collect { refreshWhenVisible(appContext) }
            }
    }

    /**
     * Redraws the widgets whose content changed. With the screen off no widget can be seen, so
     * changes then (a night of notifications) only mark a redraw for when the screen turns on.
     */
    private suspend fun refreshWhenVisible(context: Context) {
        val powerManager = runCatching { context.getSystemService(PowerManager::class.java) }.getOrNull()
        if (powerManager != null && !powerManager.isInteractive) {
            isRefreshPending.set(true)
            // The screen may have turned on before the flag was set, too late for its broadcast to see it.
            if (!powerManager.isInteractive || !isRefreshPending.getAndSet(false)) return
        } else {
            isRefreshPending.set(false)
        }
        refreshMutex.withLock { refreshChanged(context) }
    }

    /**
     * Draws each placed widget unless it already shows what it would draw now. On Android 12L+ a
     * direct draw is the whole update, as lists travel with the views; earlier versions serve
     * lists from Glance's session, so that is updated too.
     */
    private suspend fun refreshChanged(context: Context) {
        val appWidgetIds =
            runCatching {
                AppWidgetManager
                    .getInstance(context)
                    .getAppWidgetIds(ComponentName(context, AtAGlanceWidgetReceiver::class.java))
            }.getOrNull()
        if (appWidgetIds == null || appWidgetIds.isEmpty()) return
        val glanceManager = GlanceAppWidgetManager(context)
        val widget = AtAGlanceWidget()
        appWidgetIds.forEach { appWidgetId ->
            runCatching {
                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
                val isCurrent = AtAGlanceWidgetContent.isShowingCurrent(context, glanceId, prefs)
                val drawn =
                    isCurrent ||
                        (
                            Build.VERSION.SDK_INT > Build.VERSION_CODES.S &&
                                widget.drawNow(context, appWidgetId, glanceId, allSizes = true)
                        )
                if (!drawn) {
                    refreshWidget(context, widget, appWidgetId, glanceId) { id, gid -> widget.drawNow(context, id, gid) }
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
        unregisterListeners?.invoke()
        unregisterListeners = null
        litCameraIdsState.value = emptySet()
        isRefreshPending.set(false)
    }

    /** Registers the device-state listeners and returns how to unregister them. */
    private fun registerDeviceListeners(context: Context): () -> Unit {
        val mainHandler = Handler(Looper.getMainLooper())
        // Battery broadcasts also fire for temperature and voltage, so only what the rows show counts.
        var lastBatteryState: Triple<Int, Int, Int>? = null
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    if (intent.action == Intent.ACTION_SCREEN_ON) {
                        // Drawn right away rather than after the debounce, as home may show any moment.
                        if (isRefreshPending.getAndSet(false)) {
                            scope.launch { refreshMutex.withLock { refreshChanged(context.applicationContext) } }
                        }
                        return
                    }
                    if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                        val state =
                            Triple(
                                intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                                intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
                                intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1),
                            )
                        if (state == lastBatteryState) return
                        val isFirst = lastBatteryState == null
                        lastBatteryState = state
                        // Registering delivers the sticky battery state, which is already drawn.
                        if (isFirst) return
                    }
                    requestRefresh()
                }
            }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
                addAction(ACTION_WIFI_AP_STATE_CHANGED)
            }
        val receiverRegistered =
            runCatching {
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            }.isSuccess

        val connectivityManager = runCatching { context.getSystemService(ConnectivityManager::class.java) }.getOrNull()
        var needsSignIn: Boolean? = null
        val networkCallback =
            connectivityManager?.let {
                CaptivePortalWifiCallback(it) { value ->
                    mainHandler.post {
                        if (needsSignIn != null && needsSignIn != value) requestRefresh()
                        needsSignIn = value
                    }
                }
            }
        val networkRegistered =
            networkCallback != null && registerWifiNetworkCallback(connectivityManager, networkCallback)

        val cameraManager = runCatching { context.getSystemService(CameraManager::class.java) }.getOrNull()
        val torchCallback =
            object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(
                    cameraId: String,
                    enabled: Boolean,
                ) = setTorch(cameraId, enabled)

                override fun onTorchModeUnavailable(cameraId: String) = setTorch(cameraId, false)

                private fun setTorch(
                    cameraId: String,
                    enabled: Boolean,
                ) {
                    val before = litCameraIdsState.value
                    val after = if (enabled) before + cameraId else before - cameraId
                    litCameraIdsState.value = after
                    if (before.isEmpty() != after.isEmpty()) requestRefresh()
                }
            }
        val torchRegistered =
            cameraManager != null &&
                runCatching { cameraManager.registerTorchCallback(torchCallback, mainHandler) }.isSuccess

        val calendarObserver =
            object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean) = requestRefresh()
            }
        // Fails without calendar access; the scheduled refresh then catches the change.
        val calendarRegistered =
            runCatching {
                context.contentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, calendarObserver)
            }.isSuccess

        return {
            if (receiverRegistered) runCatching { context.unregisterReceiver(receiver) }
            if (networkRegistered) {
                runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
            }
            if (torchRegistered) runCatching { cameraManager.unregisterTorchCallback(torchCallback) }
            if (calendarRegistered) runCatching { context.contentResolver.unregisterContentObserver(calendarObserver) }
        }
    }
}
