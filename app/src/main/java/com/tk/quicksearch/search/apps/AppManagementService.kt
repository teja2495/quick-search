package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.core.GenericManagementHandler
import com.tk.quicksearch.search.core.ManagementHandler
import com.tk.quicksearch.search.core.AppManagementConfig
import com.tk.quicksearch.search.core.SearchUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handles app management operations like pinning, hiding, and nicknames.
 */
class AppManagementService(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit,
    // AppManagementService doesn't use UI state updates like the others, so we provide no-op
    onUiStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit = {}
) : ManagementHandler<AppInfo> by GenericManagementHandler(
    AppManagementConfig(),
    userPreferences,
    scope,
    onStateChanged,
    onUiStateUpdate
) {

    // App-specific methods that extend beyond the base interface
    fun hideApp(appInfo: AppInfo, isSearching: Boolean) {
        scope.launch {
            if (isSearching) {
                userPreferences.hidePackageInResults(appInfo.packageName)
            } else {
                excludeItem(appInfo) // Use the base exclude logic for suggestions
            }
            onStateChanged()
        }
    }

    fun unhideAppFromResults(appInfo: AppInfo) {
        scope.launch {
            userPreferences.unhidePackageInResults(appInfo.packageName)
            onStateChanged()
        }
    }

    fun clearAllHiddenApps() {
        scope.launch {
            userPreferences.clearAllHiddenAppsInSuggestions()
            userPreferences.clearAllHiddenAppsInResults()
            onStateChanged()
        }
    }

    // Convenience methods that delegate to the interface
    fun pinApp(appInfo: AppInfo) = pinItem(appInfo)
    fun unpinApp(appInfo: AppInfo) = unpinItem(appInfo)
    fun unhideAppFromSuggestions(appInfo: AppInfo) = removeExcludedItem(appInfo)
    fun setAppNickname(appInfo: AppInfo, nickname: String?) = setItemNickname(appInfo, nickname)
    fun getAppNickname(packageName: String): String? = getItemNickname(AppInfo("", packageName, 0L, 0L, 0, false))
}
