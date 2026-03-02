package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.data.preferences.AppPreferences

/**
 * Facade for app preference operations
 */
class AppPreferencesFacade(
    private val appPreferences: AppPreferences
) {
    fun getSuggestionHiddenPackages(): Set<String> = appPreferences.getSuggestionHiddenPackages()

    fun getResultHiddenPackages(): Set<String> = appPreferences.getResultHiddenPackages()

    fun getPinnedPackages(): Set<String> = appPreferences.getPinnedPackages()

    fun hidePackageInSuggestions(packageName: String): Set<String> =
            appPreferences.hidePackageInSuggestions(packageName)

    fun hidePackageInResults(packageName: String): Set<String> =
            appPreferences.hidePackageInResults(packageName)

    fun unhidePackageInSuggestions(packageName: String): Set<String> =
            appPreferences.unhidePackageInSuggestions(packageName)

    fun unhidePackageInResults(packageName: String): Set<String> =
            appPreferences.unhidePackageInResults(packageName)

    fun pinPackage(packageName: String): Set<String> = appPreferences.pinPackage(packageName)

    fun unpinPackage(packageName: String): Set<String> = appPreferences.unpinPackage(packageName)

    fun clearAllHiddenAppsInSuggestions(): Set<String> =
            appPreferences.clearAllHiddenAppsInSuggestions()

    fun clearAllHiddenAppsInResults(): Set<String> = appPreferences.clearAllHiddenAppsInResults()

    fun getAppLaunchCount(packageName: String): Int = appPreferences.getAppLaunchCount(packageName)

    fun getAppLaunchCount(packageName: String, userHandleId: Int?): Int =
        appPreferences.getAppLaunchCount(packageName, userHandleId)

    fun incrementAppLaunchCount(packageName: String) =
        appPreferences.incrementAppLaunchCount(packageName)

    fun incrementAppLaunchCount(packageName: String, userHandleId: Int?) =
        appPreferences.incrementAppLaunchCount(packageName, userHandleId)

    fun getAllAppLaunchCounts(): Map<String, Int> = appPreferences.getAllAppLaunchCounts()

    fun getRecentAppLaunches(): List<String> = appPreferences.getRecentAppLaunches()

    fun setRecentAppLaunches(packageNames: List<String>): List<String> =
            appPreferences.setRecentAppLaunches(packageNames)

    fun addRecentAppLaunch(packageName: String): List<String> =
            appPreferences.addRecentAppLaunch(packageName)
}