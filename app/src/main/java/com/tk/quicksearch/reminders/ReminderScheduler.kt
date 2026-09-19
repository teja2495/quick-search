package com.tk.quicksearch.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.models.ReminderInfo
import java.text.DateFormat
import java.util.Date

/** Schedules reminder alarms and posts reminder notifications. */
object ReminderScheduler {
    const val EXTRA_REMINDER_ID = "com.tk.quicksearch.extra.REMINDER_ID"
    const val ACTION_FIRE = "com.tk.quicksearch.action.REMINDER_FIRE"
    const val ACTION_DONE = "com.tk.quicksearch.action.REMINDER_DONE"
    const val ACTION_SNOOZE = "com.tk.quicksearch.action.REMINDER_SNOOZE"

    private const val CHANNEL_ID = "reminders"
    private const val NOTIFICATION_ID_BASE = 8_000_000
    private const val REQUEST_CODE_BASE = 900_000
    private const val REQUEST_CODES_PER_REMINDER = 4
    private const val REQUEST_OFFSET_ALARM = 0
    private const val REQUEST_OFFSET_DONE = 1
    private const val REQUEST_OFFSET_SNOOZE = 2
    private const val REQUEST_OFFSET_CONTENT = 3

    /** Schedules the reminder's alarm, replacing any existing one. Done or past reminders are skipped. */
    fun schedule(context: Context, reminder: ReminderInfo, nowMillis: Long = System.currentTimeMillis()) {
        cancel(context, reminder.reminderId)
        if (reminder.isDone) return
        val triggerAtMillis = reminder.dueMillis
        if (triggerAtMillis <= nowMillis) return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = alarmPendingIntent(context, reminder.reminderId)
        if (ReminderPermissions.canScheduleExactAlarms(context)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // Without exact-alarm access the system may deliver this a few minutes late.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(alarmPendingIntent(context, reminderId))
    }

    fun rescheduleAll(context: Context) {
        val now = System.currentTimeMillis()
        ReminderRepository(context).getAllReminders().forEach { schedule(context, it, now) }
        ReminderRepository.notifyChanged()
    }

    fun cancelNotification(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(reminderId))
    }

    fun showNotification(context: Context, reminder: ReminderInfo) {
        if (!ReminderPermissions.hasPostNotifications(context)) return
        createChannel(context)
        val reminderId = reminder.reminderId
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminder)
                .setContentTitle(reminder.title)
                .setContentText(dueText(context, reminder))
                .setWhen(reminder.dueMillis)
                .setShowWhen(reminder.hasTime)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent(context, reminderId))
                .addAction(
                    0,
                    context.getString(R.string.reminder_notification_action_done),
                    actionPendingIntent(context, reminderId, ACTION_DONE, REQUEST_OFFSET_DONE),
                )
                .addAction(
                    0,
                    context.getString(R.string.reminder_notification_action_snooze),
                    actionPendingIntent(context, reminderId, ACTION_SNOOZE, REQUEST_OFFSET_SNOOZE),
                )
                .build()
        runCatching { NotificationManagerCompat.from(context).notify(notificationId(reminderId), notification) }
    }

    private fun dueText(context: Context, reminder: ReminderInfo): String {
        val date = Date(reminder.dueMillis)
        return if (reminder.hasTime) {
            context.getString(
                R.string.reminder_notification_due_at,
                DateFormat.getTimeInstance(DateFormat.SHORT).format(date),
            )
        } else {
            context.getString(
                R.string.reminder_notification_due_on,
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(date),
            )
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminders_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.reminders_notification_channel_description) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun alarmPendingIntent(context: Context, reminderId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(reminderId, REQUEST_OFFSET_ALARM),
            Intent(context, ReminderAlarmReceiver::class.java)
                .setAction(ACTION_FIRE)
                .putExtra(EXTRA_REMINDER_ID, reminderId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun actionPendingIntent(
        context: Context,
        reminderId: Long,
        action: String,
        offset: Int,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(reminderId, offset),
            Intent(context, ReminderActionReceiver::class.java)
                .setAction(action)
                .putExtra(EXTRA_REMINDER_ID, reminderId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun contentPendingIntent(context: Context, reminderId: Long): PendingIntent? {
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(
            context,
            requestCode(reminderId, REQUEST_OFFSET_CONTENT),
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(reminderId: Long, offset: Int): Int =
        (REQUEST_CODE_BASE + reminderId * REQUEST_CODES_PER_REMINDER + offset).toInt()

    private fun notificationId(reminderId: Long): Int = (NOTIFICATION_ID_BASE + reminderId).toInt()
}
