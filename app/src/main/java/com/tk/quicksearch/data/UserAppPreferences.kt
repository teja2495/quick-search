package com.tk.quicksearch.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores user-driven overrides for the app grid such as hidden or pinned apps.
 */
class UserAppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHiddenPackages(): Set<String> = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()

    fun getPinnedPackages(): Set<String> = prefs.getStringSet(KEY_PINNED, emptySet()).orEmpty().toSet()

    fun shouldShowAppLabels(): Boolean = prefs.getBoolean(KEY_SHOW_APP_LABELS, true)

    fun setShowAppLabels(showLabels: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_APP_LABELS, showLabels).apply()
    }

    fun hidePackage(packageName: String): Set<String> = updateStringSet(KEY_HIDDEN) {
        it.add(packageName)
    }

    fun unhidePackage(packageName: String): Set<String> = updateStringSet(KEY_HIDDEN) {
        it.remove(packageName)
    }

    fun pinPackage(packageName: String): Set<String> = updateStringSet(KEY_PINNED) {
        it.add(packageName)
    }

    fun unpinPackage(packageName: String): Set<String> = updateStringSet(KEY_PINNED) {
        it.remove(packageName)
    }

    private inline fun updateStringSet(
        key: String,
        block: (MutableSet<String>) -> Unit
    ): Set<String> {
        val current = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        block(current)
        val snapshot = current.toSet()
        prefs.edit().putStringSet(key, snapshot).apply()
        return snapshot
    }

    private companion object {
        private const val PREFS_NAME = "user_app_preferences"
        private const val KEY_HIDDEN = "hidden_packages"
        private const val KEY_PINNED = "pinned_packages"
        private const val KEY_SHOW_APP_LABELS = "show_app_labels"
    }
}


