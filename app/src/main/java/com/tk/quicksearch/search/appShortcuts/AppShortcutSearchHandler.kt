package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.data.appShortcutRepository.AppShortcutRepository
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isShortcutDisabled
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.appShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.removeSystemShortcutsForPackage
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.utils.CachedSearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextCache
import java.util.Locale

private const val MIN_QUERY_LENGTH = 2
internal const val APP_SHORTCUT_RESULT_LIMIT = 25
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
    private val isDefaultHomeApp: () -> Boolean = { false },
) {
    // Written on IO, read on Main and Default.
    @Volatile
    private var availableShortcuts: List<StaticShortcut> = emptyList()

    @Volatile
    private var hasRefreshedFromSystem = false
    private val searchTextCache = SearchTextCache()
    private val searchMatcher = CachedSearchMatcher(searchTextCache)
    private val combinedFieldsCache = AppShortcutCombinedFieldsCache()

    fun getAvailableShortcuts(): List<StaticShortcut> = mergeIconOverrides(availableShortcuts)

    /**
     * True when a load is still worth starting: nothing is in memory and no system refresh has
     * finished yet. Cheap enough for every keystroke, unlike [getAvailableShortcuts].
     */
    fun needsInitialLoad(): Boolean = availableShortcuts.isEmpty() && !hasRefreshedFromSystem

    fun removeUnavailablePackage(packageName: String): Boolean {
        repository.markPackageUnavailable(packageName)
        val updated = removeSystemShortcutsForPackage(availableShortcuts, packageName)
        if (updated == availableShortcuts) return false
        availableShortcuts = updated
        clearSearchCaches()
        return true
    }

    fun markPackageAvailable(packageName: String) {
        repository.markPackageAvailable(packageName)
    }

    suspend fun loadCachedShortcutsOnly(): Boolean {
        val cached = repository.loadCachedShortcuts() ?: return false
        availableShortcuts = normalizeShortcuts(cached)
        clearSearchCaches()
        return true
    }

    suspend fun refreshShortcutsFromSystem(): Boolean {
        val loaded = runCatching { repository.loadStaticShortcuts() }.getOrNull() ?: return false
        availableShortcuts = normalizeShortcuts(loaded)
        clearSearchCaches()
        hasRefreshedFromSystem = true
        return true
    }

    suspend fun getShortcutsByKeys(keys: Set<String>): Map<String, StaticShortcut> {
        if (keys.isEmpty()) return emptyMap()
        // Only the cache here: a full system scan belongs to the guarded startup/refresh load.
        if (availableShortcuts.isEmpty()) {
            loadCachedShortcutsOnly()
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
            clearSearchCaches()
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
        resultLimit: Int = APP_SHORTCUT_RESULT_LIMIT,
    ): List<StaticShortcut> =
        searchShortcutsInternal(
            queryContext = queryContext,
            excludedIds = userPreferences.getExcludedAppShortcutIds(),
            disabledIds = userPreferences.getDisabledAppShortcutIds(),
            recentShortcutScores = recentShortcutScores,
            shortcutOpenCounts = shortcutOpenCounts,
            secondaryRankingSignal = secondaryRankingSignal,
            enableFuzzyMatching = enableFuzzyMatching,
            resultLimit = resultLimit,
        )

    private fun searchShortcutsInternal(
        queryContext: SearchQueryContext,
        excludedIds: Set<String>,
        disabledIds: Set<String>,
        recentShortcutScores: Map<String, Int>,
        shortcutOpenCounts: Map<String, Int>,
        secondaryRankingSignal: com.tk.quicksearch.search.models.SecondaryRankingSignal,
        enableFuzzyMatching: Boolean = false,
        resultLimit: Int = APP_SHORTCUT_RESULT_LIMIT,
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
                resultLimit = resultLimit,
                enableFuzzyMatching = enableFuzzyMatching,
                isLowRamDevice = isLowRamDevice,
                matcher = searchMatcher,
                textCache = searchTextCache,
                combinedFieldsCache = combinedFieldsCache,
            ),
        )

    private fun clearSearchCaches() {
        searchTextCache.clear()
        combinedFieldsCache.clear()
    }

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
        userPreferences.getRecentResultRecencyIndex().appShortcutScores

    private fun getShortcutOpenCounts(): Map<String, Int> =
        userPreferences.getRecentResultRecencyIndex().appShortcutOpenCounts

    private fun normalizeShortcuts(shortcuts: List<StaticShortcut>): List<StaticShortcut> {
        // Chrome/Brave shortcuts are only reliable when served by LauncherApps as the default Home app.
        val hideBrowserShortcuts = !isDefaultHomeApp()
        return shortcuts
            .filterNot {
                hideBrowserShortcuts &&
                    (it.packageName == CHROME_PACKAGE || it.packageName == BRAVE_PACKAGE) &&
                    !isUserCreatedShortcut(it)
            }.distinctBy { shortcutKey(it) }
    }
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
