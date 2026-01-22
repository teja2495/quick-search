package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.data.AppShortcutRepository
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.shortcutDisplayName
import com.tk.quicksearch.search.data.shortcutKey
import com.tk.quicksearch.search.utils.SearchRankingUtils
import java.util.Locale

private const val MIN_QUERY_LENGTH = 2
private const val RESULT_LIMIT = 6
private const val CHROME_PACKAGE = "com.android.chrome"

data class AppShortcutSearchResults(
        val pinned: List<StaticShortcut>,
        val excluded: List<StaticShortcut>,
        val results: List<StaticShortcut>
)

class AppShortcutSearchHandler(
        private val repository: AppShortcutRepository,
        private val userPreferences: UserAppPreferences
) {
    private var availableShortcuts: List<StaticShortcut> = emptyList()

    suspend fun loadShortcuts() {
        val cached = repository.loadCachedShortcuts()
        if (cached != null) {
            availableShortcuts = normalizeShortcuts(cached)
        }

        val loaded = runCatching { repository.loadStaticShortcuts() }.getOrNull()
        if (loaded != null) {
            availableShortcuts = normalizeShortcuts(loaded)
        }
    }

    suspend fun getPinnedAndExcludedOnly(): AppShortcutSearchResults {
        val cached = repository.loadCachedShortcuts()
        if (cached != null) {
            availableShortcuts = normalizeShortcuts(cached)
        }

        val pinnedIds = userPreferences.getPinnedAppShortcutIds()
        val excludedIds = userPreferences.getExcludedAppShortcutIds()

        val pinned =
                availableShortcuts
                        .filter {
                            pinnedIds.contains(shortcutKey(it)) &&
                                    !excludedIds.contains(shortcutKey(it))
                        }
                        .sortedBy { shortcutDisplayName(it).lowercase(Locale.getDefault()) }

        val excluded =
                availableShortcuts.filter { excludedIds.contains(shortcutKey(it)) }.sortedBy {
                    shortcutDisplayName(it).lowercase(Locale.getDefault())
                }

        return AppShortcutSearchResults(pinned, excluded, emptyList())
    }

    fun getShortcutsState(query: String, isSectionEnabled: Boolean): AppShortcutSearchResults {
        val pinnedIds = userPreferences.getPinnedAppShortcutIds()
        val excludedIds = userPreferences.getExcludedAppShortcutIds()

        val pinned =
                availableShortcuts
                        .filter {
                            pinnedIds.contains(shortcutKey(it)) &&
                                    !excludedIds.contains(shortcutKey(it))
                        }
                        .sortedBy { shortcutDisplayName(it).lowercase(Locale.getDefault()) }

        val excluded =
                availableShortcuts.filter { excludedIds.contains(shortcutKey(it)) }.sortedBy {
                    shortcutDisplayName(it).lowercase(Locale.getDefault())
                }

        val results =
                if (query.isNotBlank() && isSectionEnabled) {
                    searchShortcutsInternal(query, excludedIds)
                } else {
                    emptyList()
                }

        return AppShortcutSearchResults(pinned, excluded, results)
    }

    fun searchShortcuts(query: String): List<StaticShortcut> {
        return searchShortcutsInternal(query, userPreferences.getExcludedAppShortcutIds())
    }

    private fun searchShortcutsInternal(
            query: String,
            excludedIds: Set<String>
    ): List<StaticShortcut> {
        if (availableShortcuts.isEmpty()) return emptyList()
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return emptyList()

        val normalizedQuery = trimmed.lowercase(Locale.getDefault())
        val queryTokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val shortcutNicknames = userPreferences.getAllAppShortcutNicknames()

        return availableShortcuts
                .asSequence()
                .filterNot { excludedIds.contains(shortcutKey(it)) }
                .mapNotNull { shortcut ->
                    val shortcutId = shortcutKey(shortcut)
                    val displayName = shortcutDisplayName(shortcut)
                    val nickname = shortcutNicknames[shortcutId]
                    val priority =
                            minOf(
                                    SearchRankingUtils.calculateMatchPriorityWithNickname(
                                            displayName,
                                            nickname,
                                            normalizedQuery,
                                            queryTokens
                                    ),
                                    SearchRankingUtils.calculateMatchPriority(
                                            shortcut.appLabel,
                                            normalizedQuery,
                                            queryTokens
                                    )
                            )

                    if (SearchRankingUtils.isOtherMatch(priority)) {
                        null
                    } else {
                        shortcut to priority
                    }
                }
                .sortedWith(
                        compareBy<Pair<StaticShortcut, Int>> { it.second }.thenBy {
                            shortcutDisplayName(it.first).lowercase(Locale.getDefault())
                        }
                )
                .take(RESULT_LIMIT)
                .map { it.first }
                .toList()
    }

    private fun normalizeShortcuts(shortcuts: List<StaticShortcut>): List<StaticShortcut> {
        return shortcuts.filterNot { it.packageName == CHROME_PACKAGE }.distinctBy {
            shortcutKey(it)
        }
    }
}
