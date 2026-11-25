package com.tk.quicksearch.search.noted

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.widgets.utils.refreshAtAGlanceWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Noted's broadcasts after its open dated tasks or its hidden calendar event names change. Redraws
 * the At a Glance widget, and home and search while they are open. They carry no data, so a stray
 * sender can only cause a redraw.
 */
class NotedTasksChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != ACTION_DUE_TASKS_CHANGED && action != ACTION_HIDDEN_CALENDAR_EVENTS_CHANGED) return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (NotedIntegration.ensureChecked(appContext)) {
                    if (action == ACTION_HIDDEN_CALENDAR_EVENTS_CHANGED) NotedHiddenCalendarEvents.invalidate()
                    ReminderRepository.notifyChanged()
                    refreshAtAGlanceWidgets(appContext)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val ACTION_DUE_TASKS_CHANGED = "com.tk.noted.action.DUE_TASKS_CHANGED"
        const val ACTION_HIDDEN_CALENDAR_EVENTS_CHANGED = "com.tk.noted.action.HIDDEN_CALENDAR_EVENTS_CHANGED"
    }
}
