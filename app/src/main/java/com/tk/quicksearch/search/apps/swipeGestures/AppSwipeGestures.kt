package com.tk.quicksearch.search.apps.swipeGestures

import android.content.Context
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppSwipeDirection(val keySuffix: String) {
    UP("up"),
    DOWN("down"),
}

/**
 * Per-app and per-folder swipe up / swipe down gestures on app and folder icons.
 *
 * Each assignment is a [CustomWidgetButtonAction] chosen with the same picker used by custom
 * widget buttons and custom gestures. Kept as a lightweight façade over its own
 * [android.content.SharedPreferences] file, mirroring
 * [com.tk.quicksearch.search.apps.speedBump.SpeedBump], so app icons and the app action menu can
 * read it without extra state plumbing.
 */
object AppSwipeGestures {
    private const val PreferencesName = "app_swipe_gestures"

    /** Pending request to open the action picker; rendered by [AppSwipeGesturePickerHost]. */
    data class PickerRequest(
        /** [targetKey] of the app or folder being assigned. */
        val targetKey: String,
        val targetName: String,
        val direction: AppSwipeDirection,
    )

    private val assignmentsState = MutableStateFlow<Map<String, String>?>(null)
    private val pickerRequestState = MutableStateFlow<PickerRequest?>(null)

    val pickerRequest: StateFlow<PickerRequest?> = pickerRequestState.asStateFlow()

    private val loadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadStarted = AtomicBoolean(false)

    /**
     * Raw action JSON keyed by [key]. Null until the first access finishes loading preferences in
     * the background, so app icons never block composition on the read.
     */
    fun assignments(context: Context): StateFlow<Map<String, String>?> {
        if (loadStarted.compareAndSet(false, true)) {
            val appContext = context.applicationContext
            loadScope.launch { ensureLoaded(appContext) }
        }
        return assignmentsState.asStateFlow()
    }

    private fun ensureLoaded(context: Context): Map<String, String> {
        assignmentsState.value?.let { return it }
        assignmentsState.compareAndSet(null, loadAll(context))
        return assignmentsState.value.orEmpty()
    }

    fun setAction(
        context: Context,
        app: AppInfo,
        direction: AppSwipeDirection,
        action: CustomWidgetButtonAction?,
    ) = setAction(context, targetKey(app), direction, action)

    fun setAction(
        context: Context,
        targetKey: String,
        direction: AppSwipeDirection,
        action: CustomWidgetButtonAction?,
    ) {
        val key = key(targetKey, direction)
        val json = action?.toJson()
        prefs(context).edit().apply {
            if (json == null) remove(key) else putString(key, json)
        }.apply()
        ensureLoaded(context)
        assignmentsState.update { current ->
            val updated = current.orEmpty().toMutableMap()
            if (json == null) updated.remove(key) else updated[key] = json
            updated
        }
    }

    fun requestPicker(app: AppInfo, direction: AppSwipeDirection) =
        requestPicker(targetKey(app), app.appName, direction)

    fun requestPicker(targetKey: String, targetName: String, direction: AppSwipeDirection) {
        pickerRequestState.value = PickerRequest(targetKey, targetName, direction)
    }

    fun clearPickerRequest() {
        pickerRequestState.value = null
    }

    /** Apps are keyed by their launch count key; folders by their pinned grid key (`folder:<id>`). */
    fun targetKey(app: AppInfo): String = app.launchCountKey()

    fun key(targetKey: String, direction: AppSwipeDirection): String = "$targetKey#${direction.keySuffix}"

    private fun loadAll(context: Context): Map<String, String> =
        prefs(context).all.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
}
