package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.core.AppManagementConfig
import com.tk.quicksearch.search.core.GenericManagementHandler
import com.tk.quicksearch.search.core.ManagementHandler
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Handles app management operations like pinning, hiding, and nicknames.
 */
class AppManagementService(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit,
    // AppManagementService doesn't use UI state updates like the others, so we provide no-op
    onUiStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit = {},
) : ManagementHandler<AppInfo> by GenericManagementHandler(
        AppManagementConfig(),
        userPreferences,
        scope,
        onStateChanged,
        onUiStateUpdate,
    ) {
    // App-specific methods that extend beyond the base interface
    fun hideApp(
        appInfo: AppInfo,
        isSearching: Boolean,
    ) {
        scope.launch {
            if (isSearching) {
                userPreferences.hidePackageInResults(appInfo.launchCountKey())
                onStateChanged()
            } else {
                // excludeItem already refreshes the derived state.
                excludeItem(appInfo)
            }
        }
    }

    fun unhideAppFromResults(appInfo: AppInfo) {
        scope.launch {
            userPreferences.unhidePackageInResults(appInfo.launchCountKey())
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

    fun pinApp(appInfo: AppInfo) = pinItem(appInfo)

    fun unpinApp(appInfo: AppInfo) = unpinItem(appInfo)

    fun reorderPinnedApps(apps: List<AppInfo>) {
        scope.launch {
            userPreferences.setPinnedPackageOrder(apps.map { it.launchCountKey() })
            onStateChanged()
        }
    }

    fun unhideAppFromSuggestions(appInfo: AppInfo) = removeExcludedItem(appInfo)

    fun setAppNickname(
        appInfo: AppInfo,
        nickname: String?,
    ) = setItemNickname(appInfo, nickname)

    // [appKey] is [AppInfo.launchCountKey], so cloned and work-profile apps keep their own nickname.
    fun getAppNickname(appKey: String): String? = userPreferences.getAppNickname(appKey)

    companion object {
        /**
         * Wraps [refresh] so callers return at once and the refresh runs on a background thread.
         * Requests are conflated and run one at a time, and each run reads preferences when it
         * starts, so the last run always reflects the last preference write.
         */
        fun backgroundRefresher(
            scope: CoroutineScope,
            refresh: () -> Unit,
        ): () -> Unit {
            val requests = Channel<Unit>(Channel.CONFLATED)
            scope.launch(Dispatchers.Default) {
                for (request in requests) refresh()
            }
            return { requests.trySend(Unit) }
        }
    }
}
