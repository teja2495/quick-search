package com.tk.quicksearch.search.settings

import android.content.Context
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.data.SettingsShortcutRepository
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.util.SearchRankingUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

data class SettingsSearchResults(
    val pinned: List<SettingShortcut>,
    val excluded: List<SettingShortcut>,
    val results: List<SettingShortcut>
)

class SettingsSearchHandler(
    private val context: Context,
    private val repository: SettingsShortcutRepository,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope
) {
    private var availableSettings: List<SettingShortcut> = emptyList()

    suspend fun loadShortcuts() {
        availableSettings = repository.loadShortcuts()
    }

    fun getSettingsState(
        query: String,
        isSettingsSectionEnabled: Boolean,
        currentResults: List<SettingShortcut>
    ): SettingsSearchResults {
        // Cache preference reads to avoid repeated SharedPreferences lookups
        val pinnedIds = userPreferences.getPinnedSettingIds()
        val excludedIds = userPreferences.getExcludedSettingIds()
        
        val pinned = availableSettings
            .filter { pinnedIds.contains(it.id) && !excludedIds.contains(it.id) }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
        val excluded = availableSettings
            .filter { excludedIds.contains(it.id) }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }

        val results = if (query.isNotBlank() && isSettingsSectionEnabled) {
            searchSettingsInternal(query, excludedIds)
        } else {
            emptyList()
        }

        return SettingsSearchResults(pinned, excluded, results)
    }

    fun searchSettings(query: String): List<SettingShortcut> {
        return searchSettingsInternal(query, userPreferences.getExcludedSettingIds())
    }
    
    private fun searchSettingsInternal(query: String, excludedIds: Set<String>): List<SettingShortcut> {
        if (availableSettings.isEmpty()) return emptyList()
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val normalizedQuery = trimmed.lowercase(Locale.getDefault())
        val nicknameMatches = userPreferences.findSettingsWithMatchingNickname(trimmed)
            .filterNot { excludedIds.contains(it) }
            .toSet()
        
        // Pre-fetch all nicknames to avoid repeated SharedPreferences lookups during iteration
        val settingsToSearch = availableSettings.filterNot { excludedIds.contains(it.id) }
        val nicknameCache = settingsToSearch.associate { shortcut ->
            shortcut.id to userPreferences.getSettingNickname(shortcut.id)
        }

        return settingsToSearch
            .asSequence()
            .mapNotNull { shortcut ->
                val matchResult = checkShortcutMatchCached(shortcut, normalizedQuery, nicknameMatches, nicknameCache)
                if (!matchResult.hasMatch) return@mapNotNull null

                val priority = calculatePriority(shortcut, matchResult, trimmed)
                shortcut to priority
            }
            .sortedWith(compareBy({ it.second }, { it.first.title.lowercase(Locale.getDefault()) }))
            .take(6)
            .map { it.first }
            .toList()
    }

    private data class MatchResult(val hasMatch: Boolean, val hasNicknameMatch: Boolean)

    private fun checkShortcutMatchCached(
        shortcut: SettingShortcut,
        normalizedQuery: String,
        nicknameMatches: Set<String>,
        nicknameCache: Map<String, String?>
    ): MatchResult {
        val nickname = nicknameCache[shortcut.id]
        val hasNicknameMatch = nickname?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true
        val keywordText = shortcut.keywords.joinToString(" ")
        val hasFieldMatch = shortcut.title.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
            (shortcut.description?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true) ||
            keywordText.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
            nicknameMatches.contains(shortcut.id)

        return MatchResult(hasFieldMatch || hasNicknameMatch, hasNicknameMatch || nicknameMatches.contains(shortcut.id))
    }

    private fun calculatePriority(
        shortcut: SettingShortcut,
        matchResult: MatchResult,
        trimmedQuery: String
    ): Int {
        return if (matchResult.hasNicknameMatch) {
            0
        } else {
            val keywordText = shortcut.keywords.joinToString(" ")
            SearchRankingUtils.getBestMatchPriority(
                trimmedQuery,
                shortcut.title,
                shortcut.description ?: "",
                keywordText
            )
        }
    }

    fun openSetting(setting: SettingShortcut) {
        runCatching {
            val intent = repository.buildIntent(setting)
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(
                context,
                context.getString(R.string.error_open_setting, setting.title),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
