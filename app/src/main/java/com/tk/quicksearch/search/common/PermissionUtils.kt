package com.tk.quicksearch.search.utils

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import androidx.core.content.ContextCompat
import com.tk.quicksearch.shared.permissions.PermissionHelper

/**
 * Utility functions for checking various Android permissions.
 */
object PermissionUtils {
    /**
     * Checks if the app has usage stats permission.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps =
            context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false

        val mode =
            runCatching {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            }.getOrElse { return false }

        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Checks if the app has read contacts permission.
     */
    fun hasContactsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED

    /** Checks if the app has read calendar permission. */
    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Checks if the app has file access permission (READ_EXTERNAL_STORAGE pre-R, MANAGE_EXTERNAL_STORAGE R+).
     */
    fun hasFileAccessPermission(context: Context): Boolean = PermissionHelper.checkFilesPermission(context)
}
