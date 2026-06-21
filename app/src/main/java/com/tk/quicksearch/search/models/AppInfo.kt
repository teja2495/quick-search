package com.tk.quicksearch.search.models

/**
 * Snapshot of an installed application that can be rendered inside Quick Search.
 * [hasLaunchIntent] differentiates normal launchable apps from packages that only expose app info.
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
    val hasLaunchIntent: Boolean = true,
    val userHandleId: Int? = null,
    val componentName: String? = null,
    val lastUpdateTime: Long = firstInstallTime,
) {
    fun launchCountKey(): String =
        if (userHandleId == null) packageName else "$packageName:$userHandleId"
}
