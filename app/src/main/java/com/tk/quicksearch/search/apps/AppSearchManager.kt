package com.tk.quicksearch.search.apps

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.AppUsageRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.frosch2010.fuzzywuzzy_kotlin.FuzzySearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class AppSearchManager(
    private val context: Context,
    private val repository: AppUsageRepository,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onAppsUpdated: () -> Unit,
    private val onLoadingStateChanged: (Boolean, String?) -> Unit,
    private val showToastCallback: (Int) -> Unit
) {

    var cachedApps: List<AppInfo> = emptyList()
        private set
    
    private var noMatchPrefix: String? = null
    var sortAppsByUsageEnabled: Boolean = false
        private set
    private var fuzzySearchEnabled: Boolean = false

    companion object {
        private const val GRID_ITEM_COUNT = 10
        private const val FUZZY_MATCH_THRESHOLD = 70
        private const val FUZZY_MIN_QUERY_LENGTH = 3
        private const val FUZZY_PRIORITY = 5
        private const val ABBREVIATION_MAX_QUERY_LENGTH = 3
        private const val SUBSEQUENCE_MAX_QUERY_LENGTH = 2
        private val WHITESPACE_REGEX = "\\s+".toRegex()
        private val NON_ALPHANUMERIC_REGEX = "[^A-Za-z0-9]+".toRegex()
        private val CAMEL_CASE_REGEX = "([a-z])([A-Z])".toRegex()
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
                        showToastCallback(R.string.apps_refreshed_successfully)
                    }
                }
                .onFailure { error ->
                    val fallbackMessage = context.getString(R.string.error_loading_user_apps)
                    onLoadingStateChanged(false, error.localizedMessage ?: fallbackMessage)
                    
                    if (showToast) {
                        showToastCallback(R.string.failed_to_refresh_apps)
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
            
            showToastCallback(R.string.settings_cache_cleared_toast)
            refreshApps()
        }
    }
    
    fun setSortAppsByUsage(enabled: Boolean) {
        sortAppsByUsageEnabled = enabled
        // VM should update preference
    }

    fun setFuzzySearchEnabled(enabled: Boolean) {
        fuzzySearchEnabled = enabled
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
        val queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }

        val appMatches = source.asSequence()
            .mapNotNull { app -> calculateAppMatch(app, normalizedQuery, queryTokens) }
            .sortedWith(createAppComparator())
            .map { it.app }
            .take(GRID_ITEM_COUNT)
            .toList()

        return appMatches
    }

    private data class AppMatch(
        val app: AppInfo,
        val priority: Int,
        val fuzzyScore: Int,
        val isFuzzy: Boolean
    )

    private fun calculateAppMatch(
        app: AppInfo, 
        normalizedQuery: String, 
        queryTokens: List<String>
    ): AppMatch? {
        // Use cached nickname to avoid SharedPreferences lookup per item
        val nickname = cachedAppNicknames[app.packageName]
        val priority = SearchRankingUtils.calculateMatchPriorityWithNickname(
            app.appName,
            nickname,
            normalizedQuery,
            queryTokens
        )
        if (!SearchRankingUtils.isOtherMatch(priority)) {
            return AppMatch(app, priority, 0, false)
        }

        if (!fuzzySearchEnabled) {
            return null
        }

        val fuzzyScore = computeFuzzyScore(normalizedQuery, app.appName, nickname)
        return if (fuzzyScore >= FUZZY_MATCH_THRESHOLD) {
            AppMatch(app, priority = FUZZY_PRIORITY, fuzzyScore = fuzzyScore, isFuzzy = true)
        } else {
            null
        }
    }

    private fun computeFuzzyScore(
        normalizedQuery: String,
        appName: String,
        nickname: String?
    ): Int {
        var bestScore = 0
        val compactQuery = normalizedQuery.replace(WHITESPACE_REGEX, "")
        if (compactQuery.length in 2..ABBREVIATION_MAX_QUERY_LENGTH) {
            bestScore = maxOf(bestScore, computeAbbreviationScore(compactQuery, appName, nickname))
        }

        if (normalizedQuery.length >= FUZZY_MIN_QUERY_LENGTH) {
            val normalizedAppName = appName.lowercase(Locale.getDefault())
            val nicknameScore = nickname?.let {
                FuzzySearch.tokenSetRatio(normalizedQuery, it.lowercase(Locale.getDefault()))
            } ?: 0
            val appNameScore = FuzzySearch.tokenSetRatio(normalizedQuery, normalizedAppName)
            bestScore = maxOf(bestScore, appNameScore, nicknameScore)
        }

        return bestScore
    }

    private fun computeAbbreviationScore(query: String, appName: String, nickname: String?): Int {
        if (query.length < 2) return 0

        val appInitialism = buildInitialism(appName)
        val nicknameInitialism = nickname?.let { buildInitialism(it) } ?: ""
        var bestScore = 0

        if (appInitialism.startsWith(query)) {
            bestScore = 100
        }
        if (nicknameInitialism.startsWith(query)) {
            bestScore = 100
        }

        if (bestScore == 0 && query.length <= SUBSEQUENCE_MAX_QUERY_LENGTH) {
            val normalizedAppName = appName.lowercase(Locale.getDefault())
            if (isSubsequence(query, normalizedAppName)) {
                bestScore = maxOf(bestScore, 80)
            }
            if (nickname != null && isSubsequence(query, nickname.lowercase(Locale.getDefault()))) {
                bestScore = maxOf(bestScore, 80)
            }
        }

        return bestScore
    }

    private fun buildInitialism(text: String): String {
        if (text.isBlank()) return ""

        val separated = text.replace(CAMEL_CASE_REGEX, "$1 $2")
        val tokens = separated.split(NON_ALPHANUMERIC_REGEX).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""

        val builder = StringBuilder()
        for (token in tokens) {
            if (token.length > 1 && token.none { it.isLowerCase() }) {
                builder.append(token.lowercase(Locale.getDefault()))
            } else {
                builder.append(token[0].lowercaseChar())
            }
        }

        return builder.toString()
    }

    private fun isSubsequence(query: String, text: String): Boolean {
        var index = 0
        for (ch in text) {
            if (ch == query[index]) {
                index++
                if (index == query.length) {
                    return true
                }
            }
        }
        return false
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

    private fun compareByUsageOrName(first: AppInfo, second: AppInfo): Int {
        return if (sortAppsByUsageEnabled) {
            second.launchCount.compareTo(first.launchCount)
        } else {
            first.appName.lowercase(Locale.getDefault())
                .compareTo(second.appName.lowercase(Locale.getDefault()))
        }
    }
}
