package com.tk.quicksearch.search.apps.appLock

import android.content.Context
import android.content.SharedPreferences

/** Persistent per-package biometric lock state. */
object AppLock {
    private const val PreferencesName = "app_lock_state"
    private const val LockedPackagesKey = "locked_packages"
    private const val OutsideQuickSearchKey = "outside_quick_search_enabled"

    fun isLocked(context: Context, packageName: String): Boolean =
        packageName in lockedPackages(context)

    fun setLocked(context: Context, packageName: String, locked: Boolean) {
        val updatedPackages = lockedPackages(context).toMutableSet().apply {
            if (locked) add(packageName) else remove(packageName)
        }
        preferences(context).edit().putStringSet(LockedPackagesKey, updatedPackages).apply()
    }

    fun lockedPackages(context: Context): Set<String> =
        preferences(context).getStringSet(LockedPackagesKey, emptySet()).orEmpty()

    /**
     * Whether locked apps also ask for unlock when opened outside Quick Search (other launchers,
     * recents, notifications). Detection runs in the accessibility service, so it only takes
     * effect while that service is on.
     */
    fun isOutsideQuickSearchEnabled(context: Context): Boolean =
        preferences(context).getBoolean(OutsideQuickSearchKey, false)

    fun setOutsideQuickSearchEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(OutsideQuickSearchKey, enabled).apply()
    }

    /** Notifies [listener] whenever the locked apps or the outside-Quick-Search switch change. */
    fun registerChangeListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) = preferences(context).registerOnSharedPreferenceChangeListener(listener)

    fun unregisterChangeListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) = preferences(context).unregisterOnSharedPreferenceChangeListener(listener)

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
}
