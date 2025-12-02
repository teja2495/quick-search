package com.tk.quicksearch.model

import java.util.Locale

/**
 * Snapshot of a launchable application that can be rendered inside the quick search grid.
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val lastUsedTime: Long,
    val isSystemApp: Boolean
)

/**
 * Returns true when the current app matches the provided search query.
 */
fun AppInfo.matches(query: String): Boolean {
    if (query.isBlank()) return true
    
    val locale = Locale.getDefault()
    val normalizedQuery = query.lowercase(locale)
    val normalizedAppName = appName.lowercase(locale)
    val normalizedPackageName = packageName.lowercase(locale)
    
    return normalizedAppName.contains(normalizedQuery) ||
        normalizedPackageName.contains(normalizedQuery)
}
