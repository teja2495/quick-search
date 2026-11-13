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
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidget
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetReceiver
import com.tk.quicksearch.widgets.customButtonsWidget.CustomButtonsWidgetReceiver
import com.tk.quicksearch.widgets.mediaControlsWidget.MediaControlsWidget
import com.tk.quicksearch.widgets.mediaControlsWidget.MediaControlsWidgetReceiver
import com.tk.quicksearch.widgets.countdownWidget.CountdownWidget
import com.tk.quicksearch.widgets.countdownWidget.CountdownWidgetReceiver
import com.tk.quicksearch.widgets.noteWidget.NoteWidget
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetReceiver
import com.tk.quicksearch.widgets.searchWidget.SearchWidget
import com.tk.quicksearch.widgets.searchWidget.SearchWidgetReceiver
import com.tk.quicksearch.widgets.worldClockWidget.WorldClockWidget
import com.tk.quicksearch.widgets.worldClockWidget.WorldClockWidgetReceiver
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

    val provider = ComponentName(context, SearchWidgetReceiver::class.java)
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
    refreshAndDrawNow(context, SearchWidgetReceiver::class.java, SearchWidget(WidgetVariant.STANDARD), SearchWidget.DEFAULT_SIZE)
    refreshAndDrawNow(
        context,
        CustomButtonsWidgetReceiver::class.java,
        SearchWidget(WidgetVariant.CUSTOM_BUTTONS_ONLY),
        SearchWidget.DEFAULT_SIZE,
    )
    refreshMediaControlsWidgets(context)
}

/** Redraws placed media controls widgets, e.g. when the playing track's title or artwork changes. */
suspend fun refreshMediaControlsWidgets(context: Context) {
    refreshAndDrawNow(context, MediaControlsWidgetReceiver::class.java, MediaControlsWidget(), MediaControlsWidget.DEFAULT_SIZE)
}

/** Redraws placed Note widgets, e.g. after a note is edited, deleted or restored from a backup. */
suspend fun refreshNoteWidgets(context: Context) {
    refreshAndDrawNow(context, NoteWidgetReceiver::class.java, NoteWidget(), NoteWidget.DEFAULT_SIZE)
}

/** Redraws placed Countdown widgets, e.g. when the day changes. */
suspend fun refreshCountdownWidgets(context: Context) {
    refreshAndDrawNow(context, CountdownWidgetReceiver::class.java, CountdownWidget(), CountdownWidget.DEFAULT_SIZE)
}

/** Redraws placed World Clock widgets, e.g. when the device's time zone or clock format changes. */
suspend fun refreshWorldClockWidgets(context: Context) {
    refreshAndDrawNow(context, WorldClockWidgetReceiver::class.java, WorldClockWidget(), WorldClockWidget.DEFAULT_SIZE)
}

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
suspend fun refreshAtAGlanceWidgets(context: Context) {
    val widget = AtAGlanceWidget()
    refreshWidgets(context, AtAGlanceWidgetReceiver::class.java, widget) { appWidgetId, glanceId ->
        widget.drawNow(context, appWidgetId, glanceId)
    }
}

/** Fire-and-forget [refreshNoteWidgets] for callers outside a coroutine. */
fun requestNoteWidgetsRefresh(context: Context) {
    val appContext = context.applicationContext
    // Devices without widget support have no AppWidgetManager.
    val hasNoteWidgets =
        runCatching {
            AppWidgetManager
                .getInstance(appContext)
                .getAppWidgetIds(ComponentName(appContext, NoteWidgetReceiver::class.java))
                .isNotEmpty()
        }.getOrDefault(false)
    if (!hasNoteWidgets) return
    noteWidgetRefreshScope.launch { runCatching { refreshNoteWidgets(appContext) } }
}

/** The Glance widget that renders [variant]'s placed instances. */
fun glanceWidgetFor(variant: WidgetVariant): GlanceAppWidget =
    when (variant) {
        WidgetVariant.STANDARD,
        WidgetVariant.CUSTOM_BUTTONS_ONLY,
        -> SearchWidget(variant)
        WidgetVariant.MEDIA_CONTROLS -> MediaControlsWidget()
        WidgetVariant.NOTE -> NoteWidget()
        WidgetVariant.AT_A_GLANCE -> AtAGlanceWidget()
    }

/** The size [variant]'s widget is drawn at until the host reports the placed size. */
fun defaultWidgetSize(variant: WidgetVariant): DpSize =
    when (variant) {
        WidgetVariant.STANDARD,
        WidgetVariant.CUSTOM_BUTTONS_ONLY,
        -> SearchWidget.DEFAULT_SIZE
        WidgetVariant.MEDIA_CONTROLS -> MediaControlsWidget.DEFAULT_SIZE
        WidgetVariant.NOTE -> NoteWidget.DEFAULT_SIZE
        WidgetVariant.AT_A_GLANCE -> AtAGlanceWidget.DEFAULT_SIZE
    }

/** [refreshWidgets], drawing each widget right away instead of waiting for Glance's session worker. */
private suspend fun refreshAndDrawNow(
    context: Context,
    receiverClass: Class<*>,
    widget: GlanceAppWidget,
    defaultSize: DpSize,
) {
    refreshWidgets(context, receiverClass, widget) { appWidgetId, glanceId ->
        widget.drawNow(context, appWidgetId, glanceId, defaultSize)
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
