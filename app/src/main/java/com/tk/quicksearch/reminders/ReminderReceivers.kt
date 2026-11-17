package com.tk.quicksearch.reminders

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tk.quicksearch.customInfo.CustomInfoScheduler
import com.tk.quicksearch.search.data.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fires when a reminder is due and posts its notification. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_FIRE) return
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        val reminder = ReminderRepository(context).getReminderById(reminderId) ?: return
        if (reminder.isDone) return
        ReminderScheduler.showNotification(context, reminder)
        ReminderRepository.notifyChanged()
    }
}

/** Handles the Done and Snooze notification actions. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return
        val repository = ReminderRepository(context)
        when (intent.action) {
            ReminderScheduler.ACTION_DONE -> repository.setDone(reminderId, isDone = true)
            ReminderScheduler.ACTION_SNOOZE -> repository.snooze(reminderId)
        }
        ReminderScheduler.cancelNotification(context, reminderId)
    }
}

/** Re-registers reminder alarms after events that clear or shift them. */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> {
                // Every alarm is re-registered, so keep that off the main thread.
                val appContext = context.applicationContext
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        ReminderScheduler.rescheduleAll(appContext)
                        CustomInfoScheduler.rescheduleAll(appContext)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
