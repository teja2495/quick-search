package com.tk.quicksearch.tools.tickTick

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Adds a task to TickTick through its share target. TickTick saves the shared text as a new task
 * and reads any date or time in it ("tomorrow at 5pm") as the due date, then opens the task.
 */
internal object TickTickTaskHandler {
    const val PACKAGE_NAME = "com.ticktick.task"
    private const val SHARE_ACTIVITY = "com.ticktick.task.activity.ShareReceiveActivity"

    /** Queries [PackageManager]; call off the main thread. */
    fun isInstalled(context: Context): Boolean =
        try {
            context.packageManager.getApplicationInfo(PACKAGE_NAME, 0).enabled
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    fun launchAddTask(context: Context, text: String): Boolean {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                component = ComponentName(PACKAGE_NAME, SHARE_ACTIVITY)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
