package com.tk.quicksearch.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receiver for the Quick Search widget that handles widget lifecycle events
 * and provides the widget instance to the Glance framework.
 */
class QuickSearchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickSearchWidget()
}