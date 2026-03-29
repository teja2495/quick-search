package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.data.AppShortcutRepository.AppShortcutRepository
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.AppShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
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
) {
    private var availableShortcuts: List<StaticShortcut> = emptyList()

    fun getAvailableShortcuts(): List<StaticShortcut> = mergeIconOverrides(availableShortcuts)

    suspend fun loadCachedShortcutsOnly(): Boolean {
        val cached = repository.loadCachedShortcuts() ?: return false
        availableShortcuts = normalizeShortcuts(cached)
        return true
    }

    suspend fun refreshShortcutsFromSystem(): Boolean {
        val loaded = runCatching { repository.loadStaticShortcuts() }.getOrNull() ?: return false
        availableShortcuts = normalizeShortcuts(loaded)
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
                .filter { keys.contains(shortcutKey(it)) && shortcutKey(it) !in disabledIds }
                .associateBy { shortcutKey(it) }
        return mergeIconOverridesByKey(raw)
    }

    suspend fun getPinnedAndExcludedOnly(): AppShortcutSearchResults {
        val cached = repository.loadCachedShortcuts()
        if (cached != null) {
            availableShortcuts = normalizeShortcuts(cached)
        }

        val pinnedIds = userPreferences.getPinnedAppShortcutIds()
        val excludedIds = userPreferences.getExcludedAppShortcutIds()
        val disabledIds = userPreferences.getDisabledAppShortcutIds()

        val pinned =
            availableShortcuts
                .filter {
                    pinnedIds.contains(shortcutKey(it)) &&
                        !excludedIds.contains(shortcutKey(it)) &&
                        !disabledIds.contains(shortcutKey(it))
                }.sortedBy { shortcutDisplayName(it).lowercase(Locale.getDefault()) }

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
                        !disabledIds.contains(shortcutKey(it))
                }.sortedBy { shortcutDisplayName(it).lowercase(Locale.getDefault()) }

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
    ): List<StaticShortcut> =
        searchShortcutsInternal(
            queryContext = queryContext,
            excludedIds = userPreferences.getExcludedAppShortcutIds(),
            disabledIds = userPreferences.getDisabledAppShortcutIds(),
            recentShortcutScores = recentShortcutScores,
        )

    private fun searchShortcutsInternal(
        queryContext: SearchQueryContext,
        excludedIds: Set<String>,
        disabledIds: Set<String>,
        recentShortcutScores: Map<String, Int>,
    ): List<StaticShortcut> =
        mergeIconOverrides(
            AppShortcutSearchAlgorithm.search(
                fullList = availableShortcuts,
                queryContext = queryContext,
                excludedIds = excludedIds,
                disabledIds = disabledIds,
                shortcutNicknames = userPreferences.getAllAppShortcutNicknames(),
                recentShortcutScores = recentShortcutScores,
                resultLimit = RESULT_LIMIT,
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

    private fun normalizeShortcuts(shortcuts: List<StaticShortcut>): List<StaticShortcut> =
        shortcuts
            .filterNot {
                (it.packageName == CHROME_PACKAGE || it.packageName == BRAVE_PACKAGE) &&
                    !isUserCreatedShortcut(it)
            }
            .distinctBy { shortcutKey(it) }
}
