package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import java.util.Locale

private const val RESULT_LIMIT = 25

class AppSettingsSearchHandler(
    private val repository: AppSettingsRepository,
    private val userPreferences: UserAppPreferences,
) {
    private var availableSettings: List<AppSettingResult> = emptyList()

    fun loadSettings() {
        availableSettings = repository.loadSettings()
    }

    fun getSettingsByIds(ids: Set<String>): Map<String, AppSettingResult> {
        if (ids.isEmpty()) return emptyMap()
        ensureLoaded()
        return availableSettings
            .filter { ids.contains(it.id) }
            .associateBy { it.id }
    }

    fun getAvailableSettings(): List<AppSettingResult> {
        ensureLoaded()
        return availableSettings.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    fun searchSettings(
        queryContext: SearchQueryContext,
        recentSettingScores: Map<String, Int> = getRecentSettingScores(),
        enableFuzzyMatching: Boolean = false,
    ): List<AppSettingResult> {
        ensureLoaded()
        return AppSettingsSearchAlgorithm.search(
            fullList = availableSettings,
            queryContext = queryContext,
            recentSettingScores = recentSettingScores,
            resultLimit = RESULT_LIMIT,
            enableFuzzyMatching = enableFuzzyMatching,
        )
    }

    private fun ensureLoaded() {
        if (availableSettings.isEmpty()) {
            availableSettings = repository.loadSettings()
        }
    }

    private fun getRecentSettingScores(): Map<String, Int> =
        RecentResultRankingUtils
            .buildRecencyIndex(userPreferences.getRecentResultOpens())
            .settingScores
}
