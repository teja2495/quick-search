package com.tk.quicksearch.search.apps

import android.content.Context
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.data.AppUsageRepository
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.util.SearchRankingUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AppSearchHandler(
    private val context: Context,
    private val repository: AppUsageRepository,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onAppsUpdated: () -> Unit,
    private val onLoadingStateChanged: (Boolean, String?) -> Unit
) {

    var cachedApps: List<AppInfo> = emptyList()
        private set
    
    private var noMatchPrefix: String? = null
    var sortAppsByUsageEnabled: Boolean = userPreferences.shouldSortAppsByUsage()
        private set

    companion object {
        private const val GRID_ITEM_COUNT = 10
    }

    fun initCache(initialApps: List<AppInfo>) {
        cachedApps = initialApps
    }

    fun loadApps() {
        scope.launch(Dispatchers.IO) {
            refreshApps()
        }
    }

    fun refreshApps(showToast: Boolean = false, forceUiUpdate: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            if (cachedApps.isEmpty()) {
                onLoadingStateChanged(true, null)
            }

            runCatching { repository.loadLaunchableApps() }
                .onSuccess { apps ->
                    val currentPackageSet = cachedApps.map { it.packageName }.toSet()
                    val newPackageSet = apps.map { it.packageName }.toSet()
                    val appSetChanged = currentPackageSet != newPackageSet

                    if (showToast || cachedApps.isEmpty() || appSetChanged || forceUiUpdate) {
                        cachedApps = apps
                        noMatchPrefix = null
                        onAppsUpdated()
                    }
                    
                    if (cachedApps.isNotEmpty()) {
                         onLoadingStateChanged(false, null)
                    }

                    if (showToast) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.apps_refreshed_successfully),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .onFailure { error ->
                    val fallbackMessage = context.getString(R.string.error_loading_user_apps)
                    onLoadingStateChanged(false, error.localizedMessage ?: fallbackMessage)
                    
                    if (showToast) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.failed_to_refresh_apps),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        }
    }

    fun clearCachedApps() {
        scope.launch(Dispatchers.IO) {
            repository.clearCache()
            cachedApps = emptyList()
            noMatchPrefix = null
            // We need to notify VM to clear its state
            onLoadingStateChanged(true, null)
            onAppsUpdated() // VM will see empty cachedApps
            
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_cache_cleared_toast),
                    Toast.LENGTH_SHORT
                ).show()
            }
            refreshApps()
        }
    }
    
    fun setSortAppsByUsage(enabled: Boolean) {
        sortAppsByUsageEnabled = enabled
        // VM should update preference
    }

    fun resetNoMatchPrefixIfNeeded(normalizedQuery: String) {
        val prefix = noMatchPrefix ?: return
        if (!normalizedQuery.startsWith(prefix)) {
            noMatchPrefix = null
        }
    }

    fun shouldSkipDueToNoMatchPrefix(normalizedQuery: String): Boolean {
        val prefix = noMatchPrefix ?: return false
        return normalizedQuery.length >= prefix.length && normalizedQuery.startsWith(prefix)
    }
    
    fun setNoMatchPrefix(prefix: String?) {
        noMatchPrefix = prefix
    }

    fun availableApps(): List<AppInfo> {
        if (cachedApps.isEmpty()) return emptyList()
        return cachedApps.filterNot { userPreferences.getSuggestionHiddenPackages().contains(it.packageName) }
    }

    fun searchSourceApps(): List<AppInfo> {
        if (cachedApps.isEmpty()) return emptyList()
        return cachedApps.filterNot {
            userPreferences.getResultHiddenPackages().contains(it.packageName) || userPreferences.getPinnedPackages().contains(it.packageName)
        }
    }

    fun computePinnedApps(exclusion: Set<String>): List<AppInfo> {
        if (cachedApps.isEmpty() || userPreferences.getPinnedPackages().isEmpty()) return emptyList()
        return cachedApps
            .asSequence()
            .filter { userPreferences.getPinnedPackages().contains(it.packageName) && !exclusion.contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
            .toList()
    }

    fun deriveMatches(query: String, source: List<AppInfo>): List<AppInfo> {
        if (query.isBlank()) return emptyList()

        val appMatches = source.asSequence()
            .mapNotNull { app -> calculateAppMatch(app, query) }
            .sortedWith(createAppComparator())
            .map { it.first }
            .take(GRID_ITEM_COUNT)
            .toList()

        return appMatches
    }

    private fun calculateAppMatch(app: AppInfo, query: String): Pair<AppInfo, Int>? {
        val nickname = userPreferences.getAppNickname(app.packageName)
        val priority = SearchRankingUtils.calculateMatchPriorityWithNickname(
            app.appName,
            nickname,
            query
        )
        return if (SearchRankingUtils.isOtherMatch(priority)) {
            null
        } else {
            app to priority
        }
    }

    private fun createAppComparator(): Comparator<Pair<AppInfo, Int>> {
        return compareBy(
            { it.second }, // First by match priority
            {
                if (sortAppsByUsageEnabled) {
                    -it.first.lastUsedTime // Most recently used first
                } else {
                    it.first.appName.lowercase(Locale.getDefault()) // Alphabetical
                }
            }
        )
    }
}
