package com.tk.quicksearch.widgets.utils

import android.annotation.TargetApi
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.unit.DpSize
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.tk.quicksearch.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val MediaPlaybackRevisionKey = longPreferencesKey("media_playback_revision")

private val noteWidgetRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun requestAddQuickSearchWidget(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        requestPinForAndroidOPlus(context)
    } else {
        showUnsupportedVersionToast(context)
    }
}

@TargetApi(Build.VERSION_CODES.O)
private fun requestPinForAndroidOPlus(context: Context) {
    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
    if (appWidgetManager?.isRequestPinAppWidgetSupported != true) {
        showUnsupportedVersionToast(context)
        return
    }

    val provider = ComponentName(context, BuiltInWidget.SEARCH.receiver)
    try {
        val requested = appWidgetManager.requestPinAppWidget(provider, null, null)
        if (!requested) {
            showErrorToast(context)
        }
    } catch (e: Exception) {
        showErrorToast(context)
    }
}

private fun showUnsupportedVersionToast(context: Context) {
    showToast(context, R.string.home_screen_widget_not_supported, Toast.LENGTH_LONG)
}

private fun showErrorToast(context: Context) {
    showToast(context, R.string.home_screen_widget_error)
}

private fun showToast(
    context: Context,
    messageResId: Int,
    duration: Int = Toast.LENGTH_SHORT,
) {
    Toast.makeText(context, context.getString(messageResId), duration).show()
}

/**
 * Redraws every placed search, custom-buttons and media controls widget instance, e.g. so a
 * Play/Pause button's icon reflects a playback-state change that just happened outside of this app.
 */
suspend fun refreshAllWidgets(context: Context) {
    for (definition in BuiltInWidget.entries) {
        if (definition.updatesWithPlayback) refreshBuiltInWidget(context, definition)
    }
}

suspend fun refreshMediaControlsWidgets(context: Context) = refreshBuiltInWidget(context, BuiltInWidget.MEDIA_CONTROLS)

suspend fun refreshNoteWidgets(context: Context) = refreshBuiltInWidget(context, BuiltInWidget.NOTE)

suspend fun refreshCountdownWidgets(context: Context) = refreshBuiltInWidget(context, BuiltInWidget.COUNTDOWN)

suspend fun refreshWorldClockWidgets(context: Context) = refreshBuiltInWidget(context, BuiltInWidget.WORLD_CLOCK)

/**
 * How much the launcher shrinks or grows a placed widget's content. Samsung One UI Home draws
 * widgets scaled to its grid and reports the factor in the widget's options; other launchers draw
 * them as is.
 */
fun launcherWidgetContentScale(
    context: Context,
    appWidgetId: Int,
): Float =
    runCatching {
        AppWidgetManager
            .getInstance(context)
            .getAppWidgetOptions(appWidgetId)
            .getFloat(ONE_UI_RESIZE_RATIO_OPTION, 1f)
    }.getOrDefault(1f).takeIf { it.isFinite() && it in 0.5f..1.5f } ?: 1f

private const val ONE_UI_RESIZE_RATIO_OPTION = "hsResizeRatio"

/** Redraws placed At a Glance widgets, e.g. when a notification, device state or reminder changes. */
suspend fun refreshAtAGlanceWidgets(context: Context) = refreshBuiltInWidget(context, BuiltInWidget.AT_A_GLANCE)

/** Fire-and-forget [refreshNoteWidgets] for callers outside a coroutine. */
fun requestNoteWidgetsRefresh(context: Context) {
    val appContext = context.applicationContext
    // Devices without widget support have no AppWidgetManager.
    val hasNoteWidgets =
        runCatching {
            AppWidgetManager
                .getInstance(appContext)
                .getAppWidgetIds(ComponentName(appContext, BuiltInWidget.NOTE.receiver))
                .isNotEmpty()
        }.getOrDefault(false)
    if (!hasNoteWidgets) return
    noteWidgetRefreshScope.launch { runCatching { refreshNoteWidgets(appContext) } }
}

/** The Glance widget that renders [variant]'s placed instances. */
fun glanceWidgetFor(variant: WidgetVariant): GlanceAppWidget =
    BuiltInWidget.forVariant(variant).createWidget()

/** The size the widget is drawn at until the host reports the placed size. */
fun defaultWidgetSize(variant: WidgetVariant): DpSize = BuiltInWidget.forVariant(variant).defaultSize()

internal suspend fun refreshBuiltInWidget(context: Context, definition: BuiltInWidget) {
    val widget = definition.createWidget()
    refreshWidgets(context, definition.receiver, widget) { appWidgetId, glanceId ->
        definition.drawPlaced(context, widget, appWidgetId, glanceId)
    }
}

private suspend fun refreshWidgets(
    context: Context,
    receiverClass: Class<*>,
    widget: GlanceAppWidget,
    beforeUpdate: suspend (appWidgetId: Int, glanceId: GlanceId) -> Unit = { _, _ -> },
) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, receiverClass)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
    val glanceManager = GlanceAppWidgetManager(context)
    appWidgetIds.forEach { appWidgetId ->
        runCatching { glanceManager.getGlanceIdBy(appWidgetId) }
            .getOrNull()
            ?.let { glanceId -> refreshWidget(context, widget, appWidgetId, glanceId, beforeUpdate) }
    }
}

/** Redraws one placed widget, as [refreshAtAGlanceWidgets] and the others do for each of theirs. */
internal suspend fun refreshWidget(
    context: Context,
    widget: GlanceAppWidget,
    appWidgetId: Int,
    glanceId: GlanceId,
    beforeUpdate: suspend (appWidgetId: Int, glanceId: GlanceId) -> Unit = { _, _ -> },
) {
    // The widgets observe their Glance preferences. Advancing this otherwise-private
    // revision makes an already running Glance composition re-read live data (playback,
    // notes) instead of coalescing the request as an unchanged update.
    updateAppWidgetState(context, glanceId) { preferences ->
        preferences[MediaPlaybackRevisionKey] =
            (preferences[MediaPlaybackRevisionKey] ?: 0L) + 1L
    }
    beforeUpdate(appWidgetId, glanceId)
    widget.update(context, glanceId)
}
