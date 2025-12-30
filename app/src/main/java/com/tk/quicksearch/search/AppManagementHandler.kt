package com.tk.quicksearch.search

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles app management operations like pinning, hiding, and nicknames.
 */
class AppManagementHandler(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit
) {

    fun pinApp(appInfo: AppInfo) {
        if (userPreferences.getSuggestionHiddenPackages().contains(appInfo.packageName)) return
        scope.launch(Dispatchers.IO) {
            userPreferences.pinPackage(appInfo.packageName)
            onStateChanged()
        }
    }

    fun unpinApp(appInfo: AppInfo) {
        scope.launch(Dispatchers.IO) {
            userPreferences.unpinPackage(appInfo.packageName)
            onStateChanged()
        }
    }

    fun hideApp(appInfo: AppInfo, isSearching: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (isSearching) {
                userPreferences.hidePackageInResults(appInfo.packageName)
            } else {
                userPreferences.hidePackageInSuggestions(appInfo.packageName)
                if (userPreferences.getPinnedPackages().contains(appInfo.packageName)) {
                    userPreferences.unpinPackage(appInfo.packageName)
                }
            }
            onStateChanged()
        }
    }

    fun unhideAppFromSuggestions(appInfo: AppInfo) {
        scope.launch(Dispatchers.IO) {
            userPreferences.unhidePackageInSuggestions(appInfo.packageName)
            onStateChanged()
        }
    }

    fun unhideAppFromResults(appInfo: AppInfo) {
        scope.launch(Dispatchers.IO) {
            userPreferences.unhidePackageInResults(appInfo.packageName)
            onStateChanged()
        }
    }

    fun setAppNickname(appInfo: AppInfo, nickname: String?) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setAppNickname(appInfo.packageName, nickname)
            onStateChanged()
        }
    }

    fun getAppNickname(packageName: String): String? {
        return userPreferences.getAppNickname(packageName)
    }

    fun clearAllHiddenApps() {
        scope.launch(Dispatchers.IO) {
            userPreferences.clearAllHiddenAppsInSuggestions()
            userPreferences.clearAllHiddenAppsInResults()
            onStateChanged()
        }
    }
}
