package com.tk.quicksearch.search.apps

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.fuzzy.FuzzySearchConfig
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

private val WHITESPACE_REGEX = "\\s+".toRegex()

class AppSearchManager(
    private val context: Context,
    private val repository: AppsRepository,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onAppsUpdated: () -> Unit,
    private val onLoadingStateChanged: (Boolean, String?) -> Unit,
    private val showToastCallback: (Int) -> Unit,
    initialFuzzyConfig: FuzzySearchConfig = FuzzySearchConfig.DEFAULT_APP_CONFIG,
) {
    var cachedApps: List<AppInfo> = emptyList()
        private set

    private var noMatchPrefix: String? = null
    var sortAppsByUsageEnabled: Boolean = false
        private set

    private var fuzzySearchStrategy = FuzzyAppSearchStrategy(initialFuzzyConfig)

    fun initCache(initialApps: List<AppInfo>) {
        cachedApps = initialApps
    }

    fun loadApps() {
        scope.launch(Dispatchers.IO) {
            refreshApps()
        }
    }

    fun refreshApps(
        showToast: Boolean = false,
        forceUiUpdate: Boolean = false,
    ) {
        scope.launch(Dispatchers.IO) {
            if (cachedApps.isEmpty()) {
                onLoadingStateChanged(true, null)
            }

            val launchCounts = userPreferences.getAllAppLaunchCounts()
            runCatching { repository.loadLaunchableApps(launchCounts) }
                .onSuccess { apps ->
                    val currentPackageSet = cachedApps.map { it.launchCountKey() }.toSet()
                    val newPackageSet = apps.map { it.launchCountKey() }.toSet()
                    val appSetChanged = currentPackageSet != newPackageSet
                    val currentUsageMap = cachedApps.associate { it.launchCountKey() to it.launchCount }
                    val newUsageMap = apps.associate { it.launchCountKey() to it.launchCount }
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
                        scope.launch(Dispatchers.Main) {
                            showToastCallback(R.string.apps_refreshed_successfully)
                        }
                    }
                }.onFailure { error ->
                    val fallbackMessage = context.getString(R.string.error_loading_user_apps)
                    onLoadingStateChanged(false, error.localizedMessage ?: fallbackMessage)

                    if (showToast) {
                        scope.launch(Dispatchers.Main) {
                            showToastCallback(R.string.failed_to_refresh_apps)
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

            scope.launch(Dispatchers.Main) {
                showToastCallback(R.string.settings_cache_cleared_toast)
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
        if (normalizedQuery.length >= fuzzySearchStrategy.config.minQueryLength) return false
        return normalizedQuery.length >= prefix.length && normalizedQuery.startsWith(prefix)
    }

    fun setNoMatchPrefix(prefix: String?) {
        noMatchPrefix = prefix
    }

    fun availableApps(): List<AppInfo> {
        if (cachedApps.isEmpty()) return emptyList()
        val hidden = userPreferences.getSuggestionHiddenPackages()
        return cachedApps.filterNot { app ->
            hidden.contains(app.launchCountKey()) || hidden.contains(app.packageName)
        }
    }

    fun searchSourceApps(): List<AppInfo> {
        if (cachedApps.isEmpty()) return emptyList()
        val resultHidden = userPreferences.getResultHiddenPackages()
        val pinned = userPreferences.getPinnedPackages()
        return cachedApps.filterNot { app ->
            resultHidden.contains(app.launchCountKey()) ||
                resultHidden.contains(app.packageName) ||
                pinned.contains(app.launchCountKey())
        }
    }

    fun computePinnedApps(exclusion: Set<String>): List<AppInfo> {
        val pinnedPackages = userPreferences.getPinnedPackages()
        if (cachedApps.isEmpty() || pinnedPackages.isEmpty()) return emptyList()

        return cachedApps
            .asSequence()
            .filter { pinnedPackages.contains(it.launchCountKey()) && !exclusion.contains(it.launchCountKey()) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
            .toList()
    }

    private var cachedAppNicknames: Map<String, String> = emptyMap()

    init {
        // Initial load of nicknames
        refreshNicknames()
    }

    fun refreshNicknames() {
        cachedAppNicknames = userPreferences.getAllAppNicknames()
    }

    fun deriveMatches(
        query: String,
        source: List<AppInfo>,
        limit: Int,
    ): List<AppInfo> {
        if (query.isBlank()) return emptyList()

        // Pre-compute normalized query and tokens once
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
        val queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }

        val appMatches =
            source
                .asSequence()
                .mapNotNull { app -> calculateAppMatch(app, normalizedQuery, queryTokens) }
                .sortedWith(createAppComparator())
                .map { it.app }
                .take(limit)
                .toList()

        return appMatches
    }

    private data class AppMatch(
        val app: AppInfo,
        val priority: Int,
        val fuzzyScore: Int,
        val isFuzzy: Boolean,
    )

    private fun calculateAppMatch(
        app: AppInfo,
        normalizedQuery: String,
        queryTokens: List<String>,
    ): AppMatch? {
        val nickname = cachedAppNicknames[app.packageName]
        val priority =
            SearchRankingUtils.calculateMatchPriorityWithNickname(
                app.appName,
                nickname,
                normalizedQuery,
                queryTokens,
            )
        if (!SearchRankingUtils.isOtherMatch(priority)) {
            if (!queryTokensCoveredByApp(queryTokens, app.appName, nickname)) return null
            return AppMatch(app, priority, 0, false)
        }

        val fuzzyMatches =
            fuzzySearchStrategy.findMatchesWithNicknames(
                normalizedQuery,
                listOf(app),
            ) { cachedAppNicknames[it.packageName] }

        return fuzzyMatches.firstOrNull()?.let { match ->
            if (!queryTokensCoveredByApp(queryTokens, app.appName, nickname)) return null
            AppMatch(app, match.priority, match.score, true)
        }
    }

    private fun queryTokensCoveredByApp(
        queryTokens: List<String>,
        appName: String,
        nickname: String?,
    ): Boolean {
        if (queryTokens.size <= 1) return true
        return queryTokens.all { token ->
            fuzzySearchStrategy.isTokenCoveredByApp(token, appName, nickname)
        }
    }

    private fun createAppComparator(): Comparator<AppMatch> {
        return Comparator { first, second ->
            if (first.isFuzzy != second.isFuzzy) {
                return@Comparator if (first.isFuzzy) 1 else -1
            }

            if (!first.isFuzzy) {
                val priorityCompare = first.priority.compareTo(second.priority)
                if (priorityCompare != 0) {
                    return@Comparator priorityCompare
                }
                return@Comparator compareByUsageOrName(first.app, second.app)
            }

            val fuzzyCompare = second.fuzzyScore.compareTo(first.fuzzyScore)
            if (fuzzyCompare != 0) {
                return@Comparator fuzzyCompare
            }
            compareByUsageOrName(first.app, second.app)
        }
    }

    private fun compareByUsageOrName(
        first: AppInfo,
        second: AppInfo,
    ): Int =
        if (sortAppsByUsageEnabled) {
            second.launchCount.compareTo(first.launchCount)
        } else {
            first.appName
                .lowercase(Locale.getDefault())
                .compareTo(second.appName.lowercase(Locale.getDefault()))
        }
}
