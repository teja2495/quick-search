package com.tk.quicksearch.search.deviceSettings

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.recentSearches.RecentSearchEntry
import com.tk.quicksearch.search.utils.SearchRankingUtils
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

data class DeviceSettingsSearchResults(
    val pinned: List<DeviceSetting>,
    val excluded: List<DeviceSetting>,
    val results: List<DeviceSetting>,
)

class DeviceSettingsSearchHandler(
    private val context: Context,
    private val repository: DeviceSettingsRepository,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val showToastCallback: (Int) -> Unit,
) {
    private var availableSettings: List<DeviceSetting> = emptyList()

    suspend fun loadShortcuts() {
        availableSettings = repository.loadShortcuts()
    }

    suspend fun getSettingsByIds(ids: Set<String>): Map<String, DeviceSetting> {
        if (ids.isEmpty()) return emptyMap()
        if (availableSettings.isEmpty()) {
            availableSettings = repository.loadShortcuts()
        }
        return availableSettings
            .filter { ids.contains(it.id) }
            .associateBy { it.id }
    }

    fun getAvailableSettings(): List<DeviceSetting> =
        availableSettings.sortedBy { it.title.lowercase(Locale.getDefault()) }

    suspend fun getPinnedAndExcludedOnly(): DeviceSettingsSearchResults {
        if (availableSettings.isEmpty()) {
            availableSettings = repository.loadShortcuts()
        }

        val pinnedIds = userPreferences.getPinnedSettingIds()
        val excludedIds = userPreferences.getExcludedSettingIds()

        val pinned =
            availableSettings
                .filter { pinnedIds.contains(it.id) && !excludedIds.contains(it.id) }
                .sortedBy { it.title.lowercase(Locale.getDefault()) }
        val excluded =
            availableSettings.filter { excludedIds.contains(it.id) }.sortedBy {
                it.title.lowercase(Locale.getDefault())
            }

        return DeviceSettingsSearchResults(pinned, excluded, emptyList())
    }

    fun getSettingsState(
        query: String,
        isSettingsSectionEnabled: Boolean,
        currentResults: List<DeviceSetting>,
    ): DeviceSettingsSearchResults {
        // Cache preference reads to avoid repeated SharedPreferences lookups
        val pinnedIds = userPreferences.getPinnedSettingIds()
        val excludedIds = userPreferences.getExcludedSettingIds()

        val pinned =
            availableSettings
                .filter { pinnedIds.contains(it.id) && !excludedIds.contains(it.id) }
                .sortedBy { it.title.lowercase(Locale.getDefault()) }
        val excluded =
            availableSettings.filter { excludedIds.contains(it.id) }.sortedBy {
                it.title.lowercase(Locale.getDefault())
            }

        val results =
            if (query.isNotBlank() && isSettingsSectionEnabled) {
                searchSettingsInternal(query, excludedIds)
            } else {
                emptyList()
            }

        return DeviceSettingsSearchResults(pinned, excluded, results)
    }

    fun searchSettings(query: String): List<DeviceSetting> = searchSettingsInternal(query, userPreferences.getExcludedSettingIds())

    private fun searchSettingsInternal(
        query: String,
        excludedIds: Set<String>,
    ): List<DeviceSetting> {
        if (availableSettings.isEmpty()) return emptyList()
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val normalizedQuery = trimmed.lowercase(Locale.getDefault())
        val nicknameMatches =
            userPreferences
                .findSettingsWithMatchingNickname(trimmed)
                .filterNot { excludedIds.contains(it) }
                .toSet()

        // Pre-fetch all nicknames to avoid repeated SharedPreferences lookups during iteration
        val settingsToSearch = availableSettings.filterNot { excludedIds.contains(it.id) }
        val nicknameCache =
            settingsToSearch.associate { shortcut ->
                shortcut.id to userPreferences.getSettingNickname(shortcut.id)
            }

        return settingsToSearch
            .asSequence()
            .mapNotNull { shortcut ->
                val matchResult =
                    checkShortcutMatchCached(
                        shortcut,
                        normalizedQuery,
                        nicknameMatches,
                        nicknameCache,
                    )
                if (!matchResult.hasMatch) return@mapNotNull null

                val priority = calculatePriority(shortcut, matchResult, trimmed)
                shortcut to priority
            }.sortedWith(
                compareBy({ it.second }, { it.first.title.lowercase(Locale.getDefault()) }),
            ).take(6)
            .map { it.first }
            .toList()
    }

    private data class MatchResult(
        val hasMatch: Boolean,
        val hasNicknameMatch: Boolean,
    )

    private fun checkShortcutMatchCached(
        shortcut: DeviceSetting,
        normalizedQuery: String,
        nicknameMatches: Set<String>,
        nicknameCache: Map<String, String?>,
    ): MatchResult {
        val nickname = nicknameCache[shortcut.id]
        val hasNicknameMatch =
            nickname?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true
        val keywordText = shortcut.keywords.joinToString(" ")
        val hasFieldMatch =
            shortcut.title.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                (
                    shortcut.description
                        ?.lowercase(Locale.getDefault())
                        ?.contains(normalizedQuery) == true
                ) ||
                keywordText.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                nicknameMatches.contains(shortcut.id)

        return MatchResult(
            hasFieldMatch || hasNicknameMatch,
            hasNicknameMatch || nicknameMatches.contains(shortcut.id),
        )
    }

    private fun calculatePriority(
        shortcut: DeviceSetting,
        matchResult: MatchResult,
        trimmedQuery: String,
    ): Int {
        if (matchResult.hasNicknameMatch) return 0

        val normalizedQuery = trimmedQuery.lowercase(Locale.getDefault())
        val normalizedTitle = shortcut.title.lowercase(Locale.getDefault())

        // 1. Exact match
        if (normalizedTitle == normalizedQuery) return 1

        // 2. Starts with
        if (normalizedTitle.startsWith(normalizedQuery)) return 2

        // 3. Remaining matches (keywords, description, or contained in title)
        // We shift the standard Utils priority to ensure they come after title matches
        val keywordText = shortcut.keywords.joinToString(" ")
        val utilsPriority =
            SearchRankingUtils.getBestMatchPriority(
                trimmedQuery,
                shortcut.title,
                shortcut.description ?: "",
                keywordText,
            )
        // Utils returns 1-4. shifting by 2 makes them 3-6.
        return utilsPriority + 2
    }

    fun openSetting(setting: DeviceSetting) {
        userPreferences.addRecentItem(RecentSearchEntry.Setting(setting.id))
        runCatching {
            val intent = repository.buildIntent(setting)
            context.startActivity(intent)
        }.onFailure { showToastCallback(R.string.error_open_setting) }
    }
}
