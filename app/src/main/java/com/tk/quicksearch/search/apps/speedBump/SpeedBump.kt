package com.tk.quicksearch.search.apps.speedBump

import android.content.Context
import android.content.SharedPreferences

/**
 * Per-app "SpeedBump" state.
 *
 * When an app is bumped, launching it from Quick Search first plays a short calming
 * interstitial that gives the user a chance to back out before the app opens.
 *
 * Kept as a lightweight façade over its own [android.content.SharedPreferences] file,
 * mirroring [com.tk.quicksearch.pinnedNotifications.PinnedNotifications], so the app
 * action menu can read and toggle it without extra state plumbing.
 */
object SpeedBump {
    /** How long the interstitial runs before the app is launched. */
    const val DELAY_MILLIS = 6_000L

    private const val PreferencesName = "speed_bump_state"
    private const val BumpedPackagesKey = "bumped_packages"
    private const val ExplainerSeenKey = "explainer_seen"
    private const val OutsideQuickSearchKey = "outside_quick_search_enabled"

    fun isEnabled(context: Context, packageName: String): Boolean =
        bumpedPackages(context).contains(packageName)

    /** Flips the bump for [packageName] and returns the resulting state. */
    fun toggle(context: Context, packageName: String): Boolean {
        val enabled = !isEnabled(context, packageName)
        setEnabled(context, packageName, enabled)
        return enabled
    }

    fun setEnabled(context: Context, packageName: String, enabled: Boolean) {
        val updated =
            bumpedPackages(context).toMutableSet().apply {
                if (enabled) add(packageName) else remove(packageName)
            }
        prefs(context).edit().putStringSet(BumpedPackagesKey, updated).apply()
    }

    fun bumpedPackages(context: Context): Set<String> =
        prefs(context).getStringSet(BumpedPackagesKey, emptySet()).orEmpty()

    /** True once the user has been shown the explainer for this feature. */
    fun hasSeenExplainer(context: Context): Boolean =
        prefs(context).getBoolean(ExplainerSeenKey, false)

    fun markExplainerSeen(context: Context) {
        prefs(context).edit().putBoolean(ExplainerSeenKey, true).apply()
    }

    /**
     * Whether bumped apps also get the pause when opened outside Quick Search (other launchers,
     * recents, notifications). Detection runs in the accessibility service, so it only takes
     * effect while that service is on.
     */
    fun isOutsideQuickSearchEnabled(context: Context): Boolean =
        prefs(context).getBoolean(OutsideQuickSearchKey, false)

    fun setOutsideQuickSearchEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(OutsideQuickSearchKey, enabled).apply()
    }

    /** Notifies [listener] whenever the bumped apps or the outside-Quick-Search switch change. */
    fun registerChangeListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) = prefs(context).registerOnSharedPreferenceChangeListener(listener)

    fun unregisterChangeListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) = prefs(context).unregisterOnSharedPreferenceChangeListener(listener)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
}
