package com.tk.quicksearch.search.data.preferences

import android.content.Context

/**
 * Preferences for app-related settings such as hidden and pinned apps.
 */
class AppPreferences(context: Context) : BasePreferences(context) {

    init {
        migrateHiddenPackages()
    }

    // ============================================================================
    // App Preferences
    // ============================================================================

    fun getSuggestionHiddenPackages(): Set<String> = getStringSet(BasePreferences.KEY_HIDDEN_SUGGESTIONS)

    fun getResultHiddenPackages(): Set<String> = getStringSet(BasePreferences.KEY_HIDDEN_RESULTS)

    fun getPinnedPackages(): Set<String> = getPinnedStringItems(BasePreferences.KEY_PINNED)

    fun hidePackageInSuggestions(packageName: String): Set<String> = updateStringSet(BasePreferences.KEY_HIDDEN_SUGGESTIONS) {
        it.add(packageName)
    }

    fun hidePackageInResults(packageName: String): Set<String> = updateStringSet(BasePreferences.KEY_HIDDEN_RESULTS) {
        it.add(packageName)
    }

    fun unhidePackageInSuggestions(packageName: String): Set<String> = updateStringSet(BasePreferences.KEY_HIDDEN_SUGGESTIONS) {
        it.remove(packageName)
    }

    fun unhidePackageInResults(packageName: String): Set<String> = updateStringSet(BasePreferences.KEY_HIDDEN_RESULTS) {
        it.remove(packageName)
    }

    fun pinPackage(packageName: String): Set<String> = pinStringItem(BasePreferences.KEY_PINNED, packageName)

    fun unpinPackage(packageName: String): Set<String> = unpinStringItem(BasePreferences.KEY_PINNED, packageName)

    fun clearAllHiddenAppsInSuggestions(): Set<String> = clearStringSet(BasePreferences.KEY_HIDDEN_SUGGESTIONS)

    fun clearAllHiddenAppsInResults(): Set<String> = clearStringSet(BasePreferences.KEY_HIDDEN_RESULTS)

    fun getAppLaunchCount(packageName: String): Int {
        return prefs.getInt(PREFIX_LAUNCH_COUNT + packageName, 0)
    }

    fun incrementAppLaunchCount(packageName: String) {
        val current = getAppLaunchCount(packageName)
        prefs.edit().putInt(PREFIX_LAUNCH_COUNT + packageName, current + 1).apply()
    }

    fun getAllAppLaunchCounts(): Map<String, Int> {
        return prefs.all
            .filterKeys { it.startsWith(PREFIX_LAUNCH_COUNT) }
            .mapKeys { it.key.removePrefix(PREFIX_LAUNCH_COUNT) }
            .mapValues { it.value as? Int ?: 0 }
    }

    fun getRecentAppLaunches(): List<String> =
        getStringListPref(BasePreferences.KEY_RECENT_APP_LAUNCHES)

    fun setRecentAppLaunches(packageNames: List<String>): List<String> {
        val trimmed = packageNames.take(MAX_RECENT_APP_LAUNCHES)
        setStringListPref(BasePreferences.KEY_RECENT_APP_LAUNCHES, trimmed)
        return trimmed
    }

    fun addRecentAppLaunch(packageName: String, maxSize: Int = MAX_RECENT_APP_LAUNCHES): List<String> {
        val current = getStringListPref(BasePreferences.KEY_RECENT_APP_LAUNCHES).toMutableList()
        current.remove(packageName)
        current.add(0, packageName)
        if (current.size > maxSize) {
            current.subList(maxSize, current.size).clear()
        }
        setStringListPref(BasePreferences.KEY_RECENT_APP_LAUNCHES, current)
        return current
    }

    companion object {
        private const val PREFIX_LAUNCH_COUNT = "launch_count_"
        private const val MAX_RECENT_APP_LAUNCHES = 10
    }

    // ============================================================================
    // Private Helper Functions
    // ============================================================================

    private fun migrateHiddenPackages() {
        val legacyHidden = getStringSet(BasePreferences.KEY_HIDDEN_LEGACY)
        val currentSuggestions = prefs.getStringSet(BasePreferences.KEY_HIDDEN_SUGGESTIONS, null)
        val currentResults = prefs.getStringSet(BasePreferences.KEY_HIDDEN_RESULTS, null)

        if (legacyHidden.isEmpty()) {
            // Nothing to migrate; ensure legacy key is cleaned up if present
            if (prefs.contains(BasePreferences.KEY_HIDDEN_LEGACY)) {
                prefs.edit().remove(BasePreferences.KEY_HIDDEN_LEGACY).apply()
            }
            return
        }

        val editor = prefs.edit()
        if (currentSuggestions.isNullOrEmpty()) {
            editor.putStringSet(BasePreferences.KEY_HIDDEN_SUGGESTIONS, legacyHidden)
        }
        if (currentResults.isNullOrEmpty()) {
            editor.putStringSet(BasePreferences.KEY_HIDDEN_RESULTS, legacyHidden)
        }
        editor.remove(BasePreferences.KEY_HIDDEN_LEGACY).apply()
    }
}
