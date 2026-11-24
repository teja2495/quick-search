package com.tk.quicksearch.widgets.remindersWidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.tk.quicksearch.widgets.utils.refreshRemindersWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val remindersRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Redraws Reminders widgets on a scheduled refresh (when a reminder next turns overdue, at midnight,
 * and at least every half hour) and on clock and locale changes. Edits made in Quick Search redraw
 * them right away through [com.tk.quicksearch.widgets.utils.requestRemindersWidgetsRefresh].
 */
class RemindersWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RemindersWidget()

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
            -> {
                // Null when super.onReceive (Glance) already took the async result, e.g. on locale change.
                val pendingResult: PendingResult? = goAsync()
                val appContext = context.applicationContext
                remindersRefreshScope.launch {
                    try {
                        runCatching { refreshRemindersWidgets(appContext) }
                    } finally {
                        pendingResult?.finish()
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
    }

    companion object {
        private const val ACTION_REFRESH = "com.tk.quicksearch.widgets.remindersWidget.REFRESH"

        /** Catches changes nothing reports, such as a reminder restored from a backup. */
        private const val MAX_REFRESH_INTERVAL_MILLIS = 30L * 60L * 1000L

        /**
         * An alarm at [nextChangeMillis], or in half an hour if sooner; replaces any earlier one. It's
         * exact when allowed, so "Overdue" shows on time, and never wakes the device.
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
                Intent(context, RemindersWidgetReceiver::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
