package com.tk.quicksearch.widgets.searchWidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class SearchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SearchWidget()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)

        // Handle configuration changes (including theme changes)
        if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
            // Update all widget instances to reflect theme changes
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SearchWidgetReceiver::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            // Send update broadcast to all widget instances
            val updateIntent =
                Intent(context, SearchWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
            context.sendBroadcast(updateIntent)
        }
    }
}
