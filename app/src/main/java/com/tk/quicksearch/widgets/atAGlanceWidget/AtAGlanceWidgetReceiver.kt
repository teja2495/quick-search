package com.tk.quicksearch.widgets.atAGlanceWidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.tk.quicksearch.widgets.utils.refreshAtAGlanceWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val atAGlanceRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Redraws At a Glance widgets on a scheduled refresh (when a row next appears or leaves on its own,
 * and at least every half hour) and on clock, locale and next-alarm changes, which also restart the
 * live updates after the app's process was stopped.
 */
class AtAGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AtAGlanceWidget()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_CONFIGURATION_CHANGED,
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            -> {
                val pendingResult = goAsync()
                val appContext = context.applicationContext
                atAGlanceRefreshScope.launch {
                    try {
                        runCatching { refreshAtAGlanceWidgets(appContext) }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleRefresh(context, nextChangeMillis = null)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.getSystemService(AlarmManager::class.java)?.cancel(refreshIntent(context))
        AtAGlanceWidgetUpdater.stop()
    }

    companion object {
        private const val ACTION_REFRESH = "com.tk.quicksearch.widgets.atAGlanceWidget.REFRESH"

        /** Catches changes nothing reports, such as a contact's birthday being edited. */
        private const val MAX_REFRESH_INTERVAL_MILLIS = 30L * 60L * 1000L

        /**
         * An alarm at [nextChangeMillis], or in half an hour if sooner; replaces any earlier one. It's
         * exact when allowed, so a call's or timer's minutes don't lag, and never wakes the device.
         */
        fun scheduleRefresh(
            context: Context,
            nextChangeMillis: Long?,
        ) {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val latest = System.currentTimeMillis() + MAX_REFRESH_INTERVAL_MILLIS
            val triggerAt = nextChangeMillis?.coerceAtMost(latest) ?: latest
            val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            runCatching {
                if (canBeExact) {
                    alarmManager.setExact(AlarmManager.RTC, triggerAt, refreshIntent(context))
                } else {
                    alarmManager.set(AlarmManager.RTC, triggerAt, refreshIntent(context))
                }
            }
        }

        private fun refreshIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, AtAGlanceWidgetReceiver::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
