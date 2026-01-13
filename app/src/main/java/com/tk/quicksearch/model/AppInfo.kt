package com.tk.quicksearch.model

import java.util.Locale

/**
 * Snapshot of a launchable application that can be rendered inside the quick search grid.
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val lastUsedTime: Long,
    val totalTimeInForeground: Long,
    val launchCount: Int = 0,
    val isSystemApp: Boolean
)

