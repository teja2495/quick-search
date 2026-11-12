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
 * Noted's broadcast after its open dated tasks change. Redraws the At a Glance widget, and home and
 * search while they are open. It carries no data, so a stray sender can only cause a redraw.
 */
class NotedTasksChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DUE_TASKS_CHANGED) return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (NotedIntegration.ensureChecked(appContext)) {
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
    }
}
