package com.tk.quicksearch.search.apps

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.data.AppUsageRepository
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.interfaces.UiFeedbackService
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
    private val onLoadingStateChanged: (Boolean, String?) -> Unit,
    private val uiFeedbackService: UiFeedbackService
) {

    var cachedApps: List<AppInfo> = emptyList()
        private set
    
    private var noMatchPrefix: String? = null
    var sortAppsByUsageEnabled: Boolean = false
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

            val launchCounts = userPreferences.getAllAppLaunchCounts()
            runCatching { repository.loadLaunchableApps(launchCounts) }
                .onSuccess { apps ->
                    val currentPackageSet = cachedApps.map { it.packageName }.toSet()
                    val newPackageSet = apps.map { it.packageName }.toSet()
                    val appSetChanged = currentPackageSet != newPackageSet
                    val currentUsageMap = cachedApps.associate { it.packageName to it.launchCount }
                    val newUsageMap = apps.associate { it.packageName to it.launchCount }
                    val usageStatsChanged = currentUsageMap != newUsageMap

                    if (showToast || cachedApps.isEmpty() || appSetChanged || usageStatsChanged || forceUiUpdate) {
                        cachedApps = apps
                        noMatchPrefix = null
                        onAppsUpdated()
                    }
                    
                    if (cachedApps.isNotEmpty()) {
                         onLoadingStateChanged(false, null)
                    }

                    if (showToast) {
                        uiFeedbackService.showToast(R.string.apps_refreshed_successfully)
                    }
                }
                .onFailure { error ->
                    val fallbackMessage = context.getString(R.string.error_loading_user_apps)
                    onLoadingStateChanged(false, error.localizedMessage ?: fallbackMessage)
                    
                    if (showToast) {
                        uiFeedbackService.showToast(R.string.failed_to_refresh_apps)
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
            
            uiFeedbackService.showToast(R.string.settings_cache_cleared_toast)
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
        val pinnedPackages = userPreferences.getPinnedPackages()
        if (cachedApps.isEmpty() || pinnedPackages.isEmpty()) return emptyList()
        
        return cachedApps
            .asSequence()
            .filter { pinnedPackages.contains(it.packageName) && !exclusion.contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
            .toList()
    }

    fun getRecentlyOpenedApps(limit: Int): List<AppInfo> {
        return repository.extractRecentlyOpenedApps(availableApps(), limit)
    }

    private var cachedAppNicknames: Map<String, String> = emptyMap()

    init {
        // Initial load of nicknames
        refreshNicknames()
    }

    fun refreshNicknames() {
        cachedAppNicknames = userPreferences.getAllAppNicknames()
    }

    fun deriveMatches(query: String, source: List<AppInfo>): List<AppInfo> {
        if (query.isBlank()) return emptyList()

        // Pre-compute normalized query and tokens once
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        val queryTokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }

        val appMatches = source.asSequence()
            .mapNotNull { app -> calculateAppMatch(app, normalizedQuery, queryTokens) }
            .sortedWith(createAppComparator())
            .map { it.first }
            .take(GRID_ITEM_COUNT)
            .toList()

        return appMatches
    }

    private fun calculateAppMatch(
        app: AppInfo, 
        normalizedQuery: String, 
        queryTokens: List<String>
    ): Pair<AppInfo, Int>? {
        // Use cached nickname to avoid SharedPreferences lookup per item
        val nickname = cachedAppNicknames[app.packageName]
        val priority = SearchRankingUtils.calculateMatchPriorityWithNickname(
            app.appName,
            nickname,
            normalizedQuery,
            queryTokens
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
                    -it.first.launchCount // Most launched first
                } else {
                    it.first.appName.lowercase(Locale.getDefault()) // Alphabetical
                }
            }
        )
    }
}
