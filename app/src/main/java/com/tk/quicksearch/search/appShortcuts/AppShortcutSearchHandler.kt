package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.data.appShortcutRepository.AppShortcutRepository
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isShortcutDisabled
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.appShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.removeSystemShortcutsForPackage
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.CachedSearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextCache
import java.util.Locale

private const val MIN_QUERY_LENGTH = 2
private const val RESULT_LIMIT = 25
private const val CHROME_PACKAGE = "com.android.chrome"
private const val BRAVE_PACKAGE = "com.brave.browser"

data class AppShortcutSearchResults(
    val pinned: List<StaticShortcut>,
    val excluded: List<StaticShortcut>,
    val results: List<StaticShortcut>,
)

class AppShortcutSearchHandler(
    private val repository: AppShortcutRepository,
    private val userPreferences: UserAppPreferences,
    private val isLowRamDevice: Boolean = false,
) {
    private var availableShortcuts: List<StaticShortcut> = emptyList()
    private val searchTextCache = SearchTextCache()
    private val searchMatcher = CachedSearchMatcher(searchTextCache)

    fun getAvailableShortcuts(): List<StaticShortcut> = mergeIconOverrides(availableShortcuts)

    fun removeUnavailablePackage(packageName: String): Boolean {
        repository.markPackageUnavailable(packageName)
        val updated = removeSystemShortcutsForPackage(availableShortcuts, packageName)
        if (updated == availableShortcuts) return false
        availableShortcuts = updated
        searchTextCache.clear()
        return true
    }

    fun markPackageAvailable(packageName: String) {
        repository.markPackageAvailable(packageName)
    }

    suspend fun loadCachedShortcutsOnly(): Boolean {
        val cached = repository.loadCachedShortcuts() ?: return false
        availableShortcuts = normalizeShortcuts(cached)
        searchTextCache.clear()
        return true
    }

    suspend fun refreshShortcutsFromSystem(): Boolean {
        val loaded = runCatching { repository.loadStaticShortcuts() }.getOrNull() ?: return false
        availableShortcuts = normalizeShortcuts(loaded)
        searchTextCache.clear()
        return true
    }

    suspend fun loadShortcuts() {
        loadCachedShortcutsOnly()
        refreshShortcutsFromSystem()
    }

    suspend fun getShortcutsByKeys(keys: Set<String>): Map<String, StaticShortcut> {
        if (keys.isEmpty()) return emptyMap()
        if (availableShortcuts.isEmpty()) {
            loadShortcuts()
        }
        val disabledIds = userPreferences.getDisabledAppShortcutIds()
        val raw =
            availableShortcuts
                .filter { keys.contains(shortcutKey(it)) && !isShortcutDisabled(it, disabledIds) }
                .associateBy { shortcutKey(it) }
        return mergeIconOverridesByKey(raw)
    }

    suspend fun getPinnedAndExcludedOnly(): AppShortcutSearchResults {
        val cached = repository.loadCachedShortcuts()
        if (cached != null) {
            availableShortcuts = normalizeShortcuts(cached)
            searchTextCache.clear()
        }

        val pinnedIds = userPreferences.getPinnedAppShortcutIds()
        val excludedIds = userPreferences.getExcludedAppShortcutIds()
        val disabledIds = userPreferences.getDisabledAppShortcutIds()

        val pinned =
            availableShortcuts
                .filter {
                    pinnedIds.contains(shortcutKey(it)) &&
                        !excludedIds.contains(shortcutKey(it)) &&
                        !isShortcutDisabled(it, disabledIds)
                }.sortedByPinnedOrder(
                    order = userPreferences.getPinnedAppShortcutOrder(),
                    fallbackSelector = { shortcutDisplayName(it).lowercase(Locale.getDefault()) },
                ) { shortcutKey(it) }

        val excluded =
            availableShortcuts.filter { excludedIds.contains(shortcutKey(it)) }.sortedBy {
                shortcutDisplayName(it).lowercase(Locale.getDefault())
            }

        return AppShortcutSearchResults(
            mergeIconOverrides(pinned),
            mergeIconOverrides(excluded),
            emptyList(),
        )
    }

    fun getShortcutsState(
        query: String,
        isSectionEnabled: Boolean,
    ): AppShortcutSearchResults {
        val pinnedIds = userPreferences.getPinnedAppShortcutIds()
        val excludedIds = userPreferences.getExcludedAppShortcutIds()
        val disabledIds = userPreferences.getDisabledAppShortcutIds()

        val pinned =
            availableShortcuts
                .filter {
                    pinnedIds.contains(shortcutKey(it)) &&
                        !excludedIds.contains(shortcutKey(it)) &&
                        !isShortcutDisabled(it, disabledIds)
                }.sortedByPinnedOrder(
                    order = userPreferences.getPinnedAppShortcutOrder(),
                    fallbackSelector = { shortcutDisplayName(it).lowercase(Locale.getDefault()) },
                ) { shortcutKey(it) }

        val excluded =
            availableShortcuts.filter { excludedIds.contains(shortcutKey(it)) }.sortedBy {
                shortcutDisplayName(it).lowercase(Locale.getDefault())
            }

        val results =
            if (query.isNotBlank() && isSectionEnabled) {
                searchShortcutsInternal(
                    queryContext = SearchQueryContext.fromRawQuery(query),
                    excludedIds = excludedIds,
                    disabledIds = disabledIds,
                    recentShortcutScores = getRecentShortcutScores(),
                    shortcutOpenCounts = getShortcutOpenCounts(),
                    secondaryRankingSignal = userPreferences.getSecondaryRankingSignal(),
                )
            } else {
                emptyList()
            }

        return AppShortcutSearchResults(
            mergeIconOverrides(pinned),
            mergeIconOverrides(excluded),
            results,
        )
    }

    fun searchShortcuts(
        queryContext: SearchQueryContext,
        recentShortcutScores: Map<String, Int> = getRecentShortcutScores(),
        shortcutOpenCounts: Map<String, Int> = getShortcutOpenCounts(),
        secondaryRankingSignal: com.tk.quicksearch.search.models.SecondaryRankingSignal =
            userPreferences.getSecondaryRankingSignal(),
        enableFuzzyMatching: Boolean = false,
    ): List<StaticShortcut> =
        searchShortcutsInternal(
            queryContext = queryContext,
            excludedIds = userPreferences.getExcludedAppShortcutIds(),
            disabledIds = userPreferences.getDisabledAppShortcutIds(),
            recentShortcutScores = recentShortcutScores,
            shortcutOpenCounts = shortcutOpenCounts,
            secondaryRankingSignal = secondaryRankingSignal,
            enableFuzzyMatching = enableFuzzyMatching,
        )

    private fun searchShortcutsInternal(
        queryContext: SearchQueryContext,
        excludedIds: Set<String>,
        disabledIds: Set<String>,
        recentShortcutScores: Map<String, Int>,
        shortcutOpenCounts: Map<String, Int>,
        secondaryRankingSignal: com.tk.quicksearch.search.models.SecondaryRankingSignal,
        enableFuzzyMatching: Boolean = false,
    ): List<StaticShortcut> =
        mergeIconOverrides(
            AppShortcutSearchAlgorithm.search(
                fullList = availableShortcuts,
                queryContext = queryContext,
                excludedIds = excludedIds,
                disabledIds = disabledIds,
                shortcutNicknames = userPreferences.getAllAppShortcutNicknames(),
                recentShortcutScores = recentShortcutScores,
                shortcutOpenCounts = shortcutOpenCounts,
                secondaryRankingSignal = secondaryRankingSignal,
                resultLimit = RESULT_LIMIT,
                enableFuzzyMatching = enableFuzzyMatching,
                isLowRamDevice = isLowRamDevice,
                matcher = searchMatcher,
                textCache = searchTextCache,
            ),
        )

    private fun mergeIconOverrides(shortcuts: List<StaticShortcut>): List<StaticShortcut> {
        val overrides = userPreferences.getAllAppShortcutIconOverrides()
        if (overrides.isEmpty()) return shortcuts
        return shortcuts.map { shortcut ->
            val key = shortcutKey(shortcut)
            val overrideIcon = overrides[key] ?: return@map shortcut
            if (isUserCreatedShortcut(shortcut)) shortcut else shortcut.copy(iconBase64 = overrideIcon)
        }
    }

    private fun mergeIconOverridesByKey(map: Map<String, StaticShortcut>): Map<String, StaticShortcut> {
        val overrides = userPreferences.getAllAppShortcutIconOverrides()
        if (overrides.isEmpty() || map.isEmpty()) return map
        return map.mapValues { (key, shortcut) ->
            val overrideIcon = overrides[key] ?: return@mapValues shortcut
            if (isUserCreatedShortcut(shortcut)) shortcut else shortcut.copy(iconBase64 = overrideIcon)
        }
    }

    private fun getRecentShortcutScores(): Map<String, Int> =
        RecentResultRankingUtils
            .buildRecencyIndex(userPreferences.getRecentResultOpens())
            .appShortcutScores

    private fun getShortcutOpenCounts(): Map<String, Int> =
        RecentResultRankingUtils
            .buildRecencyIndex(emptyList(), userPreferences.getRecentResultOpenCounts())
            .appShortcutOpenCounts

    private fun normalizeShortcuts(shortcuts: List<StaticShortcut>): List<StaticShortcut> =
        shortcuts
            .filterNot {
                (it.packageName == CHROME_PACKAGE || it.packageName == BRAVE_PACKAGE) &&
                    !isUserCreatedShortcut(it)
            }
            .distinctBy { shortcutKey(it) }
}

private fun <T, K> List<T>.sortedByPinnedOrder(
    order: List<K>,
    fallbackSelector: (T) -> String,
    keySelector: (T) -> K,
): List<T> {
    val orderIndex = order.withIndex().associate { it.value to it.index }
    return sortedWith(
        compareBy<T> { orderIndex[keySelector(it)] ?: Int.MAX_VALUE }
            .thenBy(fallbackSelector),
    )
}
