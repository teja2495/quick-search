package com.tk.quicksearch.widgets.utils

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.runComposition
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.flow.first

/**
 * Draws [appWidgetId] at its placed size and hands it to the host right away. [GlanceAppWidget.update]
 * alone waits for Glance's session worker, which can be held back until the app next runs, so a
 * saved setting would otherwise show up late. The session still redraws the same state after.
 * Returns whether it drew; if not, the session's update still draws the widget, only later.
 */
@OptIn(ExperimentalGlanceApi::class)
internal suspend fun GlanceAppWidget.drawNow(
    context: Context,
    appWidgetId: Int,
    glanceId: GlanceId,
    defaultSize: DpSize,
): Boolean {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
    return runCatching {
        val state = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val remoteViews = runComposition(context, glanceId, options, listOf(placedWidgetSize(options, defaultSize)), state).first()
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }.isSuccess
}

/** The placed size the host reported, or [defaultSize] before it reports one. */
internal fun placedWidgetSize(
    options: Bundle,
    defaultSize: DpSize,
): DpSize = reportedWidgetSize(options) ?: defaultSize

/** The placed size the host reported for [appWidgetId], or null before it reports one. */
internal fun reportedWidgetSize(
    context: Context,
    appWidgetId: Int,
): DpSize? =
    runCatching { reportedWidgetSize(AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)) }.getOrNull()

private fun reportedWidgetSize(options: Bundle): DpSize? {
    val reported =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)?.firstOrNull()
        } else {
            null
        }
    if (reported != null && reported.width > 0f && reported.height > 0f) {
        return DpSize(reported.width.dp, reported.height.dp)
    }
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else null
}
