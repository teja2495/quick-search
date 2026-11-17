package com.tk.quicksearch.search.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Preferences for app-related settings such as hidden and pinned apps.
 */
class AppPreferences(
    context: Context,
) : BasePreferences(context) {
    private val launchCountsPrefs: SharedPreferences =
        appContext.getSharedPreferences(LAUNCH_COUNTS_PREFS_NAME, Context.MODE_PRIVATE)


    // ============================================================================
    // App Preferences
    // ============================================================================

    fun getSuggestionHiddenPackages(): Set<String> = getStringSet(BasePreferences.KEY_HIDDEN_SUGGESTIONS)

    fun getResultHiddenPackages(): Set<String> = getStringSet(BasePreferences.KEY_HIDDEN_RESULTS)

    fun getPinnedPackages(): Set<String> = getPinnedStringItems(BasePreferences.KEY_PINNED)

    fun getPinnedPackageOrder(): List<String> {
        val pinnedPackages = getPinnedPackages()
        return getStringListPref(BasePreferences.KEY_PINNED_APP_ORDER).filter(pinnedPackages::contains)
    }

    fun setPinnedPackageOrder(packageNames: List<String>): List<String> {
        val pinnedPackages = getPinnedPackages()
        val normalized =
            buildList {
                packageNames.forEach { packageName ->
                    if (pinnedPackages.contains(packageName) && !contains(packageName)) {
                        add(packageName)
                    }
                }
                pinnedPackages.forEach { packageName ->
                    if (!contains(packageName)) {
                        add(packageName)
                    }
                }
            }
        setStringListPref(BasePreferences.KEY_PINNED_APP_ORDER, normalized)
        return normalized
    }

    fun hidePackageInSuggestions(packageName: String): Set<String> =
        updateStringSet(BasePreferences.KEY_HIDDEN_SUGGESTIONS) {
            it.add(packageName)
        }

    fun hidePackageInResults(packageName: String): Set<String> =
        updateStringSet(BasePreferences.KEY_HIDDEN_RESULTS) {
            it.add(packageName)
        }

    fun unhidePackageInSuggestions(packageName: String): Set<String> =
        updateStringSet(BasePreferences.KEY_HIDDEN_SUGGESTIONS) {
            it.remove(packageName)
        }

    fun unhidePackageInResults(packageName: String): Set<String> =
        updateStringSet(BasePreferences.KEY_HIDDEN_RESULTS) {
            it.remove(packageName)
        }

    fun pinPackage(packageName: String): Set<String> {
        val existingOrder = getPinnedPackageOrder()
        val updated = pinStringItem(BasePreferences.KEY_PINNED, packageName)
        if (existingOrder.isNotEmpty() && !existingOrder.contains(packageName)) {
            setStringListPref(BasePreferences.KEY_PINNED_APP_ORDER, existingOrder + packageName)
        }
        return updated
    }

    fun unpinPackage(packageName: String): Set<String> {
        val updated = unpinStringItem(BasePreferences.KEY_PINNED, packageName)
        setStringListPref(BasePreferences.KEY_PINNED_APP_ORDER, getPinnedPackageOrder().filterNot { it == packageName })
        return updated
    }

    fun getAppIconOverride(packageName: String): AppIconOverride? =
        prefs.getString("${BasePreferences.KEY_APP_ICON_OVERRIDE_PREFIX}$packageName", null)
            ?.split('|', limit = 2)
            ?.takeIf { it.size == 2 && it.all(String::isNotBlank) }
            ?.let { (iconPackPackage, drawableName) ->
                if (iconPackPackage == SYSTEM_DEFAULT_ICON_OVERRIDE_VALUE &&
                    drawableName == SYSTEM_DEFAULT_ICON_OVERRIDE_VALUE
                ) {
                    AppIconOverride(useSystemDefault = true)
                } else {
                    AppIconOverride(iconPackPackage = iconPackPackage, drawableName = drawableName)
                }
            }

    fun setAppIconOverride(
        packageName: String,
        iconPackPackage: String,
        drawableName: String,
    ) {
        prefs.edit()
            .putString(
                "${BasePreferences.KEY_APP_ICON_OVERRIDE_PREFIX}$packageName",
                "$iconPackPackage|$drawableName",
            )
            .apply()
    }

    fun setAppIconToSystemDefault(packageName: String) {
        prefs.edit()
            .putString(
                "${BasePreferences.KEY_APP_ICON_OVERRIDE_PREFIX}$packageName",
                "$SYSTEM_DEFAULT_ICON_OVERRIDE_VALUE|$SYSTEM_DEFAULT_ICON_OVERRIDE_VALUE",
            )
            .apply()
    }

    fun getAppIconOverridePackageNames(): Set<String> =
        prefs.all.keys
            .asSequence()
            .filter { it.startsWith(BasePreferences.KEY_APP_ICON_OVERRIDE_PREFIX) }
            .map { it.removePrefix(BasePreferences.KEY_APP_ICON_OVERRIDE_PREFIX) }
            .filter { it.isNotBlank() }
            .toSet()

    fun clearAppIconOverrides(packageNames: Collection<String>) {
        if (packageNames.isEmpty()) return

        prefs.edit()
            .apply {
                packageNames.forEach { packageName ->
                    remove("${BasePreferences.KEY_APP_ICON_OVERRIDE_PREFIX}$packageName")
                }
            }
            .apply()
    }

    fun clearAllHiddenAppsInSuggestions(): Set<String> = clearStringSet(BasePreferences.KEY_HIDDEN_SUGGESTIONS)

    fun clearAllHiddenAppsInResults(): Set<String> = clearStringSet(BasePreferences.KEY_HIDDEN_RESULTS)

    fun getAppLaunchCount(packageName: String): Int = getAppLaunchCount(packageName, null)

    fun getAppLaunchCount(packageName: String, userHandleId: Int?): Int =
        launchCountsPrefs.getInt(launchCountKey(packageName, userHandleId), 0)

    fun incrementAppLaunchCount(packageName: String) = incrementAppLaunchCount(packageName, null)

    fun incrementAppLaunchCount(packageName: String, userHandleId: Int?) {
        val key = launchCountKey(packageName, userHandleId)
        val current = launchCountsPrefs.getInt(key, 0)
        launchCountsPrefs.edit().putInt(key, current + 1).apply()
    }

    private fun launchCountKey(packageName: String, userHandleId: Int?): String =
        if (userHandleId == null) packageName else "$packageName:$userHandleId"

    fun getAllAppLaunchCounts(): Map<String, Int> =
        launchCountsPrefs.all
            .mapValues { it.value as? Int ?: 0 }

    fun getRecentAppLaunches(): List<String> =
        com.tk.quicksearch.search.data.preferences.PreferenceUtils
            .getStringListPref(sessionPrefs, BasePreferences.KEY_RECENT_APP_LAUNCHES)

    fun setRecentAppLaunches(packageNames: List<String>): List<String> {
        val trimmed = packageNames.take(MAX_RECENT_APP_LAUNCHES)
        com.tk.quicksearch.search.data.preferences.PreferenceUtils.setStringListPref(
            sessionPrefs,
            BasePreferences.KEY_RECENT_APP_LAUNCHES,
            trimmed,
        )
        return trimmed
    }

    fun addRecentAppLaunch(
        packageName: String,
        maxSize: Int = MAX_RECENT_APP_LAUNCHES,
    ): List<String> {
        val current =
            com.tk.quicksearch.search.data.preferences.PreferenceUtils
                .getStringListPref(
                    sessionPrefs,
                    BasePreferences.KEY_RECENT_APP_LAUNCHES,
                ).toMutableList()
        current.remove(packageName)
        current.add(0, packageName)
        if (current.size > maxSize) {
            current.subList(maxSize, current.size).clear()
        }
        com.tk.quicksearch.search.data.preferences.PreferenceUtils.setStringListPref(
            sessionPrefs,
            BasePreferences.KEY_RECENT_APP_LAUNCHES,
            current,
        )
        return current
    }

    companion object {
        private const val LAUNCH_COUNTS_PREFS_NAME = "app_launch_counts"
        private const val MAX_RECENT_APP_LAUNCHES = 10
        private const val SYSTEM_DEFAULT_ICON_OVERRIDE_VALUE = "__system_default__"
    }

    // ============================================================================
    // Private Helper Functions
    // ============================================================================

}

data class AppIconOverride(
    val iconPackPackage: String? = null,
    val drawableName: String? = null,
    val useSystemDefault: Boolean = false,
) {
    init {
        require(useSystemDefault || (!iconPackPackage.isNullOrBlank() && !drawableName.isNullOrBlank()))
    }
}
