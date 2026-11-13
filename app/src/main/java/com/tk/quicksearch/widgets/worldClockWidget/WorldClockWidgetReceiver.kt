package com.tk.quicksearch.widgets.worldClockWidget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.tk.quicksearch.widgets.utils.refreshWorldClockWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val worldClockRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Redraws World Clock widgets when the device's clock, time zone, 12/24-hour setting or locale
 * changes, so the offsets and formats stay right. The clocks tick on their own, and the provider's
 * 30-minute update period picks up daylight saving changes in the shown zones.
 */
class WorldClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorldClockWidget()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_CONFIGURATION_CHANGED,
            -> {
                // Null when super.onReceive (Glance) already took the async result, e.g. on locale change.
                val pendingResult: PendingResult? = goAsync()
                val appContext = context.applicationContext
                worldClockRefreshScope.launch {
                    try {
                        runCatching { refreshWorldClockWidgets(appContext) }
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
        }
    }
}
