package com.tk.quicksearch.data.preferences

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

    fun getSuggestionHiddenPackages(): Set<String> = getStringSet(AppPreferences.KEY_HIDDEN_SUGGESTIONS)

    fun getResultHiddenPackages(): Set<String> = getStringSet(AppPreferences.KEY_HIDDEN_RESULTS)

    fun getPinnedPackages(): Set<String> = getStringSet(AppPreferences.KEY_PINNED)

    fun hidePackageInSuggestions(packageName: String): Set<String> = updateStringSet(AppPreferences.KEY_HIDDEN_SUGGESTIONS) {
        it.add(packageName)
    }

    fun hidePackageInResults(packageName: String): Set<String> = updateStringSet(AppPreferences.KEY_HIDDEN_RESULTS) {
        it.add(packageName)
    }

    fun unhidePackageInSuggestions(packageName: String): Set<String> = updateStringSet(AppPreferences.KEY_HIDDEN_SUGGESTIONS) {
        it.remove(packageName)
    }

    fun unhidePackageInResults(packageName: String): Set<String> = updateStringSet(AppPreferences.KEY_HIDDEN_RESULTS) {
        it.remove(packageName)
    }

    fun pinPackage(packageName: String): Set<String> = updateStringSet(AppPreferences.KEY_PINNED) {
        it.add(packageName)
    }

    fun unpinPackage(packageName: String): Set<String> = updateStringSet(AppPreferences.KEY_PINNED) {
        it.remove(packageName)
    }

    fun clearAllHiddenAppsInSuggestions(): Set<String> = clearStringSet(AppPreferences.KEY_HIDDEN_SUGGESTIONS)

    fun clearAllHiddenAppsInResults(): Set<String> = clearStringSet(AppPreferences.KEY_HIDDEN_RESULTS)

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

    companion object {
        private const val PREFIX_LAUNCH_COUNT = "launch_count_"

        // App preferences keys
        const val KEY_HIDDEN_LEGACY = "hidden_packages"
        const val KEY_HIDDEN_SUGGESTIONS = "hidden_packages_suggestions"
        const val KEY_HIDDEN_RESULTS = "hidden_packages_results"
        const val KEY_PINNED = "pinned_packages"
    }

    // ============================================================================
    // Private Helper Functions
    // ============================================================================

    private fun migrateHiddenPackages() {
        val legacyHidden = getStringSet(AppPreferences.KEY_HIDDEN_LEGACY)
        val currentSuggestions = prefs.getStringSet(AppPreferences.KEY_HIDDEN_SUGGESTIONS, null)
        val currentResults = prefs.getStringSet(AppPreferences.KEY_HIDDEN_RESULTS, null)

        if (legacyHidden.isEmpty()) {
            // Nothing to migrate; ensure legacy key is cleaned up if present
            if (prefs.contains(AppPreferences.KEY_HIDDEN_LEGACY)) {
                prefs.edit().remove(AppPreferences.KEY_HIDDEN_LEGACY).apply()
            }
            return
        }

        val editor = prefs.edit()
        if (currentSuggestions.isNullOrEmpty()) {
            editor.putStringSet(AppPreferences.KEY_HIDDEN_SUGGESTIONS, legacyHidden)
        }
        if (currentResults.isNullOrEmpty()) {
            editor.putStringSet(AppPreferences.KEY_HIDDEN_RESULTS, legacyHidden)
        }
        editor.remove(AppPreferences.KEY_HIDDEN_LEGACY).apply()
    }
}
