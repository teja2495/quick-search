package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.appSettings.AppSettingsDestination.EXCLUDED_ITEMS
import com.tk.quicksearch.search.appSettings.AppSettingsDestination.NICKNAMES
import com.tk.quicksearch.search.appSettings.AppSettingsDestination.TRIGGERS
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.CachedSearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextCache
import java.util.Locale

private const val RESULT_LIMIT = 25

class AppSettingsSearchHandler(
    private val repository: AppSettingsRepository,
    private val userPreferences: UserAppPreferences,
    private val isLowRamDevice: Boolean = false,
) {
    private var availableSettings: List<AppSettingResult> = emptyList()
    private val searchTextCache = SearchTextCache()
    private val searchMatcher = CachedSearchMatcher(searchTextCache)

    fun loadSettings() {
        availableSettings = repository.loadSettings()
        searchTextCache.clear()
    }

    fun getSettingsByIds(ids: Set<String>): Map<String, AppSettingResult> {
        if (ids.isEmpty()) return emptyMap()
        ensureLoaded()
        return getVisibleSettings()
            .filter { ids.contains(it.id) }
            .associateBy { it.id }
    }

    fun getAvailableSettings(): List<AppSettingResult> {
        ensureLoaded()
        return getVisibleSettings().sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    fun searchSettings(
        queryContext: SearchQueryContext,
        recentSettingScores: Map<String, Int> = getRecentSettingScores(),
        settingOpenCounts: Map<String, Int> = getSettingOpenCounts(),
        secondaryRankingSignal: com.tk.quicksearch.search.models.SecondaryRankingSignal =
            userPreferences.getSecondaryRankingSignal(),
        enableFuzzyMatching: Boolean = false,
    ): List<AppSettingResult> {
        ensureLoaded()
        return AppSettingsSearchAlgorithm
            .search(
                fullList = getVisibleSettings(),
                queryContext = queryContext,
                recentSettingScores = recentSettingScores,
                settingOpenCounts = settingOpenCounts,
                secondaryRankingSignal = secondaryRankingSignal,
                resultLimit = RESULT_LIMIT,
                enableFuzzyMatching = enableFuzzyMatching,
                isLowRamDevice = isLowRamDevice,
                matcher = searchMatcher,
                textCache = searchTextCache,
            )
    }

    private fun ensureLoaded() {
        if (availableSettings.isEmpty()) {
            availableSettings = repository.loadSettings()
            searchTextCache.clear()
        }
    }

    private fun getRecentSettingScores(): Map<String, Int> =
        RecentResultRankingUtils
            .buildRecencyIndex(userPreferences.getRecentResultOpens())
            .appSettingScores

    private fun getSettingOpenCounts(): Map<String, Int> =
        RecentResultRankingUtils
            .buildRecencyIndex(emptyList(), userPreferences.getRecentResultOpenCounts())
            .appSettingOpenCounts

    private fun getVisibleSettings(): List<AppSettingResult> {
        return availableSettings.filter { setting ->
            val shouldHideExcludedItems =
                !hasExcludedItems() && setting.destination == EXCLUDED_ITEMS
            val shouldHideNicknames =
                !userPreferences.hasAnyNicknameItems() && setting.destination == NICKNAMES
            val shouldHideTriggers =
                !userPreferences.hasAnyTriggerItems() && setting.destination == TRIGGERS
            val shouldHideTopResultIndicator =
                setting.toggleKey == AppSettingsToggleKey.TOP_RESULT_INDICATOR &&
                    (userPreferences.isPhysicalKeyboardConnected() ||
                        !userPreferences.isOpenTopResultUsingKeyboardEnabled())
            val shouldHidePinnedSectionsOrder =
                setting.id == PINNED_SECTIONS_ORDER_SETTING_ID &&
                    userPreferences.isUnifiedPinnedItemsEnabled()
            val shouldHideFuzzySearch =
                setting.toggleKey == AppSettingsToggleKey.FUZZY_SEARCH &&
                    isLowRamDevice
            val shouldHideAppResultRows =
                setting.toggleKey == AppSettingsToggleKey.APP_RESULT_ROWS &&
                    SearchSection.APPS.name in userPreferences.getDisabledSections()
            // Android archives apps starting with Android 15.
            val shouldHideArchivedApps =
                setting.toggleKey == AppSettingsToggleKey.INCLUDE_ARCHIVED_APPS_IN_SEARCH &&
                    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM
            val shouldHideAmoledTheme =
                setting.toggleKey == AppSettingsToggleKey.AMOLED_THEME &&
                    (
                        userPreferences.getAppTheme() != com.tk.quicksearch.search.core.AppTheme.MONOCHROME ||
                            userPreferences.getAppThemeMode() !=
                                com.tk.quicksearch.search.core.AppThemeMode.DARK ||
                            userPreferences.isDeviceThemeEnabled() ||
                            userPreferences.getBackgroundSource() !=
                                com.tk.quicksearch.search.core.BackgroundSource.THEME
                    )
            !shouldHideExcludedItems &&
                !shouldHideNicknames &&
                !shouldHideTriggers &&
                !shouldHideTopResultIndicator &&
                !shouldHidePinnedSectionsOrder &&
                !shouldHideFuzzySearch &&
                !shouldHideAppResultRows &&
                !shouldHideArchivedApps &&
                !shouldHideAmoledTheme
        }
    }

    private fun hasExcludedItems(): Boolean =
        userPreferences.getSuggestionHiddenPackages().isNotEmpty() ||
            userPreferences.getResultHiddenPackages().isNotEmpty() ||
            userPreferences.getExcludedContactIds().isNotEmpty() ||
            userPreferences.getExcludedFileUris().isNotEmpty() ||
            userPreferences.getExcludedFileExtensions().isNotEmpty() ||
            userPreferences.getExcludedSettingIds().isNotEmpty() ||
            userPreferences.getExcludedAppShortcutIds().isNotEmpty() ||
            userPreferences.getExcludedOtherItemIds().isNotEmpty()
}
