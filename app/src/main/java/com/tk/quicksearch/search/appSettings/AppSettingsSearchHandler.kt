package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.utils.CachedSearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextCache
import java.util.Locale

internal const val APP_SETTINGS_RESULT_LIMIT = 25

class AppSettingsSearchHandler(
    private val repository: AppSettingsRepository,
    private val userPreferences: UserAppPreferences,
    private val isLowRamDevice: Boolean = false,
) {
    @Volatile private var availableSettings: List<AppSettingResult> = emptyList()
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
        resultLimit: Int = APP_SETTINGS_RESULT_LIMIT,
    ): List<AppSettingResult> {
        ensureLoaded()
        return AppSettingsSearchAlgorithm
            .search(
                fullList = getVisibleSettings(),
                queryContext = queryContext,
                recentSettingScores = recentSettingScores,
                settingOpenCounts = settingOpenCounts,
                secondaryRankingSignal = secondaryRankingSignal,
                resultLimit = resultLimit,
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
        userPreferences.getRecentResultRecencyIndex().appSettingScores

    private fun getSettingOpenCounts(): Map<String, Int> =
        userPreferences.getRecentResultRecencyIndex().appSettingOpenCounts

    /**
     * Settings that apply to the current configuration. Each visibility flag is read at most once
     * per call, and only when a row it governs is present, instead of once per catalog row.
     */
    private fun getVisibleSettings(): List<AppSettingResult> {
        val hideExcludedItems by lazy(LazyThreadSafetyMode.NONE) { !hasExcludedItems() }
        val hideNicknames by lazy(LazyThreadSafetyMode.NONE) { !userPreferences.hasAnyNicknameItems() }
        val hideTriggers by lazy(LazyThreadSafetyMode.NONE) { !userPreferences.hasAnyTriggerItems() }
        val hidePinnedNotificationItems by lazy(LazyThreadSafetyMode.NONE) {
            !repository.hasPinnedNotificationItems()
        }
        val hideTopResultIndicator by lazy(LazyThreadSafetyMode.NONE) {
            userPreferences.isPhysicalKeyboardConnected() ||
                !userPreferences.isOpenTopResultUsingKeyboardEnabled()
        }
        val hidePinnedSectionsOrder by lazy(LazyThreadSafetyMode.NONE) {
            userPreferences.isUnifiedPinnedItemsEnabled()
        }
        val hideAliasTriggerAfterSpace by lazy(LazyThreadSafetyMode.NONE) {
            !userPreferences.isSearchEngineAliasSuffixEnabled()
        }
        val hideTopMatchesOptions by lazy(LazyThreadSafetyMode.NONE) { !userPreferences.isTopMatchesEnabled() }
        val hideAppSuggestionTabs by lazy(LazyThreadSafetyMode.NONE) {
            !userPreferences.areAppSuggestionsEnabled()
        }
        // AI provider settings (model, personal context) only exist once a key is set up.
        val hideAiProvider by lazy(LazyThreadSafetyMode.NONE) { !userPreferences.hasConfiguredAiProviderHint() }
        val hideAppResultRows by lazy(LazyThreadSafetyMode.NONE) {
            SearchSection.APPS.name in userPreferences.getDisabledSections()
        }
        // Android archives apps starting with Android 15.
        val hideArchivedApps =
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM
        val hideAmoledTheme by lazy(LazyThreadSafetyMode.NONE) {
            userPreferences.getAppThemeMode() == com.tk.quicksearch.search.core.AppThemeMode.LIGHT ||
                userPreferences.isDeviceThemeEnabled()
        }
        return availableSettings.filter { setting ->
            val hidden =
                when {
                    setting.destination == AppSettingsTarget.Page(SettingsDetailType.EXCLUDED_ITEMS) && hideExcludedItems -> true
                    setting.destination == AppSettingsTarget.Page(SettingsDetailType.NICKNAMES) && hideNicknames -> true
                    setting.destination == AppSettingsTarget.Page(SettingsDetailType.TRIGGERS) && hideTriggers -> true
                    setting.destination == AppSettingsRouteAction.PINNED_NOTIFICATION_ITEMS &&
                        hidePinnedNotificationItems -> true
                    setting.toggleKey == AppSettingsToggleKey.TOP_RESULT_INDICATOR && hideTopResultIndicator -> true
                    setting.destination == AppSettingsRouteAction.PINNED_SECTIONS_ORDER && hidePinnedSectionsOrder -> true
                    setting.toggleKey == AppSettingsToggleKey.ALIAS_TRIGGER_AFTER_SPACE && hideAliasTriggerAfterSpace -> true
                    (setting.destination == AppSettingsRouteAction.TOP_MATCHES_PRIORITY ||
                        setting.id == TOP_MATCHES_COUNT_SETTING_ID) && hideTopMatchesOptions -> true
                    setting.destination == AppSettingsRouteAction.APP_SUGGESTION_TABS && hideAppSuggestionTabs -> true
                    (setting.destination == AppSettingsTarget.Page(SettingsDetailType.GEMINI_API_CONFIG) ||
                        setting.destination == AppSettingsRouteAction.AI_MODEL) && hideAiProvider -> true
                    setting.toggleKey == AppSettingsToggleKey.FUZZY_SEARCH && isLowRamDevice -> true
                    setting.toggleKey == AppSettingsToggleKey.APP_RESULT_ROWS && hideAppResultRows -> true
                    setting.toggleKey == AppSettingsToggleKey.INCLUDE_ARCHIVED_APPS_IN_SEARCH && hideArchivedApps -> true
                    setting.toggleKey == AppSettingsToggleKey.AMOLED_THEME && hideAmoledTheme -> true
                    else -> false
                }
            !hidden
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
