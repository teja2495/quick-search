package com.tk.quicksearch.search

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
        val pinned = availableSettings
            .filter { userPreferences.getPinnedSettingIds().contains(it.id) && !userPreferences.getExcludedSettingIds().contains(it.id) }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
        val excluded = availableSettings
            .filter { userPreferences.getExcludedSettingIds().contains(it.id) }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
        
        val results = if (query.isNotBlank() && isSettingsSectionEnabled) {
            searchSettings(query)
        } else if (query.isBlank() || !isSettingsSectionEnabled) {
            emptyList()
        } else {
            currentResults
        }

        return SettingsSearchResults(pinned, excluded, results)
    }

    fun searchSettings(query: String): List<SettingShortcut> {
        if (availableSettings.isEmpty()) return emptyList()
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val normalizedQuery = trimmed.lowercase(Locale.getDefault())
        val nicknameMatches = userPreferences.findSettingsWithMatchingNickname(trimmed)
            .filterNot { userPreferences.getExcludedSettingIds().contains(it) }
            .toSet()

        return availableSettings
            .asSequence()
            .filterNot { userPreferences.getExcludedSettingIds().contains(it.id) }
            .mapNotNull { shortcut ->
                val nickname = userPreferences.getSettingNickname(shortcut.id)
                val hasNicknameMatch = nickname?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true
                val keywordText = shortcut.keywords.joinToString(" ")
                val hasFieldMatch = shortcut.title.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                    (shortcut.description?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true) ||
                    keywordText.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                    nicknameMatches.contains(shortcut.id)

                if (!hasFieldMatch && !hasNicknameMatch) return@mapNotNull null

                val priority = when {
                    hasNicknameMatch || nicknameMatches.contains(shortcut.id) -> 0
                    else -> SearchRankingUtils.getBestMatchPriority(
                        trimmed,
                        shortcut.title,
                        shortcut.description ?: "",
                        keywordText
                    )
                }
                shortcut to priority
            }
            .sortedWith(compareBy({ it.second }, { it.first.title.lowercase(Locale.getDefault()) }))
            .take(6)
            .map { it.first }
            .toList()
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
