package com.tk.quicksearch.search.models

import java.util.Locale

/**
 * Snapshot of a launchable application that can be rendered inside the quick search grid.
 * [userHandleId] is set for work profile apps so they can be launched in the correct profile.
 * [componentName] is used with LauncherApps to launch work profile apps.
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val lastUsedTime: Long,
    val totalTimeInForeground: Long,
    val launchCount: Int = 0,
    val firstInstallTime: Long,
    val isSystemApp: Boolean,
    val userHandleId: Int? = null,
    val componentName: String? = null,
) {
    fun launchCountKey(): String =
        if (userHandleId == null) packageName else "$packageName:$userHandleId"
}
