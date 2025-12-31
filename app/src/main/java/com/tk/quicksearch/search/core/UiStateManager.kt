package com.tk.quicksearch.search.core

import com.tk.quicksearch.data.AppUsageRepository
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.SettingShortcut
import java.util.Locale

/**
 * Handles UI state calculations and derived state management.
 */
class UiStateManager(
    private val repository: AppUsageRepository,
    private val userPreferences: UserAppPreferences,
    private val onStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) {

    fun refreshDerivedState(
        apps: List<AppInfo>,
        availableSettings: List<SettingShortcut>,
        lastUpdated: Long? = null,
        isLoading: Boolean? = null,
        currentState: SearchUiState
    ) {
        val visibleAppList = availableApps(apps)
        val pinnedAppsForSuggestions = computePinnedApps(apps, userPreferences.getSuggestionHiddenPackages())
        val pinnedAppsForResults = computePinnedApps(apps, userPreferences.getResultHiddenPackages())
        val recentsSource = visibleAppList.filterNot { userPreferences.getPinnedPackages().contains(it.packageName) }
        val recents = repository.extractRecentApps(recentsSource, GRID_ITEM_COUNT)
        val query = currentState.query
        val trimmedQuery = query.trim()

        val searchResults = if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            // Include both pinned and non-pinned apps in search, let ranking determine order
            val nonPinnedApps = searchSourceApps(apps)
            val allSearchableApps = (pinnedAppsForResults + nonPinnedApps).distinctBy { it.packageName }
            deriveMatches(trimmedQuery, allSearchableApps)
        }

        val suggestionHiddenAppList = apps
            .filter { userPreferences.getSuggestionHiddenPackages().contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
        val resultHiddenAppList = apps
            .filter { userPreferences.getResultHiddenPackages().contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }

        onStateUpdate { state ->
            state.copy(
                recentApps = recents,
                searchResults = searchResults,
                pinnedApps = pinnedAppsForSuggestions,
                suggestionExcludedApps = suggestionHiddenAppList,
                resultExcludedApps = resultHiddenAppList,
                indexedAppCount = visibleAppList.size,
                cacheLastUpdatedMillis = lastUpdated ?: state.cacheLastUpdatedMillis,
                isLoading = isLoading ?: state.isLoading
            )
        }
    }

    private fun availableApps(apps: List<AppInfo> = emptyList()): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps.filterNot { userPreferences.getSuggestionHiddenPackages().contains(it.packageName) }
    }

    private fun searchSourceApps(apps: List<AppInfo> = emptyList()): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps.filterNot {
            userPreferences.getResultHiddenPackages().contains(it.packageName) ||
            userPreferences.getPinnedPackages().contains(it.packageName)
        }
    }

    private fun computePinnedApps(apps: List<AppInfo>, exclusion: Set<String>): List<AppInfo> {
        if (apps.isEmpty() || userPreferences.getPinnedPackages().isEmpty()) return emptyList()
        return apps
            .asSequence()
            .filter { userPreferences.getPinnedPackages().contains(it.packageName) && !exclusion.contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
            .toList()
    }

    private fun deriveMatches(query: String, source: List<AppInfo>): List<AppInfo> {
        if (query.isBlank()) return emptyList()
        return source
            .asSequence()
            .mapNotNull { app ->
                val nickname = userPreferences.getAppNickname(app.packageName)
                val priority = com.tk.quicksearch.util.SearchRankingUtils.calculateMatchPriorityWithNickname(
                    app.appName,
                    nickname,
                    query
                )
                if (com.tk.quicksearch.util.SearchRankingUtils.isOtherMatch(priority)) {
                    return@mapNotNull null
                }
                app to priority
            }
            .sortedWith(compareBy(
                { it.second },
                {
                    if (userPreferences.shouldSortAppsByUsage()) {
                        // When sorting by usage is enabled, use negative lastUsedTime for descending order (most recent first)
                        -it.first.lastUsedTime
                    } else {
                        // Default: sort by app name
                        it.first.appName.lowercase(Locale.getDefault())
                    }
                }
            ))
            .map { it.first }
            .take(GRID_ITEM_COUNT)
            .toList()
    }

    companion object {
        private const val GRID_ITEM_COUNT = 10
    }
}
