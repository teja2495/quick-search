package com.tk.quicksearch.reminders

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Permission checks for reminder notifications and exact alarms. */
object ReminderPermissions {
    fun hasPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Exact alarms need user approval on Android 12+; earlier versions always allow them. */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun hasAllPermissions(context: Context): Boolean =
        hasPostNotifications(context) && canScheduleExactAlarms(context)

    /** Opens the "Alarms & reminders" settings page for this app, or null below Android 12. */
    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun openNotificationSettings(context: Context): Boolean {
        val intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun openExactAlarmSettings(context: Context): Boolean {
        val intent = exactAlarmSettingsIntent(context) ?: return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
