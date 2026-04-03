package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.search.core.SearchUiState

internal fun SearchUiState.toSettingsScreenState(): SettingsScreenState {
    val searchResults = toSearchResultsSettingsState()
    val searchEngines = toSearchEngineSettingsState()
    val fileSearch = toFileSearchSettingsState()
    val appearance = toAppearanceSettingsState()
    val tools = toToolsSettingsState()
    val appShortcuts = toAppShortcutsSettingsState()

    return SettingsScreenState(
        suggestionExcludedApps = searchResults.suggestionExcludedApps,
        resultExcludedApps = searchResults.resultExcludedApps,
        excludedContacts = searchResults.excludedContacts,
        excludedFiles = searchResults.excludedFiles,
        excludedSettings = searchResults.excludedSettings,
        excludedAppShortcuts = searchResults.excludedAppShortcuts,
        disabledSections = searchResults.disabledSections,
        appSuggestionsEnabled = searchResults.appSuggestionsEnabled,
        webSuggestionsEnabled = searchResults.webSuggestionsEnabled,
        webSuggestionsCount = searchResults.webSuggestionsCount,
        topResultIndicatorEnabled = searchResults.topResultIndicatorEnabled,
        recentQueriesEnabled = searchResults.recentQueriesEnabled,
        searchEngineOrder = searchEngines.searchEngineOrder,
        disabledSearchEngines = searchEngines.disabledSearchEngines,
        shortcutCodes = searchEngines.shortcutCodes,
        shortcutEnabled = searchEngines.shortcutEnabled,
        isSearchEngineCompactMode = searchEngines.isSearchEngineCompactMode,
        searchEngineCompactRowCount = searchEngines.searchEngineCompactRowCount,
        isSearchEngineAliasSuffixEnabled = searchEngines.isSearchEngineAliasSuffixEnabled,
        isAliasTriggerAfterSpaceEnabled = searchEngines.isAliasTriggerAfterSpaceEnabled,
        amazonDomain = searchEngines.amazonDomain,
        hasGeminiApiKey = searchEngines.hasGeminiApiKey,
        geminiApiKeyLast4 = searchEngines.geminiApiKeyLast4,
        isSavingGeminiApiKey = searchEngines.isSavingGeminiApiKey,
        personalContext = searchEngines.personalContext,
        geminiModel = searchEngines.geminiModel,
        geminiGroundingEnabled = searchEngines.geminiGroundingEnabled,
        availableGeminiModels = searchEngines.availableGeminiModels,
        enabledFileTypes = fileSearch.enabledFileTypes,
        showFolders = fileSearch.showFolders,
        showSystemFiles = fileSearch.showSystemFiles,
        folderWhitelistPatterns = fileSearch.folderWhitelistPatterns,
        folderBlacklistPatterns = fileSearch.folderBlacklistPatterns,
        excludedFileExtensions = fileSearch.excludedFileExtensions,
        oneHandedMode = appearance.oneHandedMode,
        bottomSearchBarEnabled = appearance.bottomSearchBarEnabled,
        overlayModeEnabled = appearance.overlayModeEnabled,
        overlayBlurEffectEnabled = appearance.overlayBlurEffectEnabled,
        hasSeenOverlayAssistantTip = appearance.hasSeenOverlayAssistantTip,
        hasWallpaperPermission = appearance.hasWallpaperPermission,
        wallpaperAvailable = appearance.wallpaperAvailable,
        wallpaperBackgroundAlpha = appearance.wallpaperBackgroundAlpha,
        wallpaperBlurRadius = appearance.wallpaperBlurRadius,
        appTheme = appearance.appTheme,
        overlayThemeIntensity = appearance.overlayThemeIntensity,
        appThemeMode = appearance.appThemeMode,
        fontScaleMultiplier = appearance.fontScaleMultiplier,
        backgroundSource = appearance.backgroundSource,
        customImageUri = appearance.customImageUri,
        selectedIconPackPackage = appearance.selectedIconPackPackage,
        availableIconPacks = appearance.availableIconPacks,
        showAppLabels = appearance.showAppLabels,
        phoneAppGridColumns = appearance.phoneAppGridColumns,
        appIconShape = appearance.appIconShape,
        launcherAppIcon = appearance.launcherAppIcon,
        themedIconsEnabled = appearance.themedIconsEnabled,
        wallpaperAccentEnabled = appearance.wallpaperAccentEnabled,
        openKeyboardOnLaunch = appearance.openKeyboardOnLaunch,
        clearQueryOnLaunch = appearance.clearQueryOnLaunch,
        autoCloseOverlay = appearance.autoCloseOverlay,
        calculatorEnabled = tools.calculatorEnabled,
        unitConverterEnabled = tools.unitConverterEnabled,
        dateCalculatorEnabled = tools.dateCalculatorEnabled,
        currencyConverterEnabled = tools.currencyConverterEnabled,
        wordClockEnabled = tools.wordClockEnabled,
        dictionaryEnabled = tools.dictionaryEnabled,
        allAppShortcuts = appShortcuts.allAppShortcuts,
        allDeviceSettings = appShortcuts.allDeviceSettings,
        allApps = appShortcuts.allApps,
        disabledAppShortcutIds = appShortcuts.disabledAppShortcutIds,
        messagingApp = appShortcuts.messagingApp,
        callingApp = appShortcuts.callingApp,
        isWhatsAppInstalled = appShortcuts.isWhatsAppInstalled,
        isTelegramInstalled = appShortcuts.isTelegramInstalled,
        isSignalInstalled = appShortcuts.isSignalInstalled,
        isGoogleMeetInstalled = appShortcuts.isGoogleMeetInstalled,
        directDialEnabled = appShortcuts.directDialEnabled,
    )
}

private data class SearchResultsMapperState(
    val suggestionExcludedApps: List<com.tk.quicksearch.search.models.AppInfo>,
    val resultExcludedApps: List<com.tk.quicksearch.search.models.AppInfo>,
    val excludedContacts: List<com.tk.quicksearch.search.models.ContactInfo>,
    val excludedFiles: List<com.tk.quicksearch.search.models.DeviceFile>,
    val excludedSettings: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting>,
    val excludedAppShortcuts: List<com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut>,
    val disabledSections: Set<com.tk.quicksearch.search.core.SearchSection>,
    val appSuggestionsEnabled: Boolean,
    val webSuggestionsEnabled: Boolean,
    val webSuggestionsCount: Int,
    val topResultIndicatorEnabled: Boolean,
    val recentQueriesEnabled: Boolean,
)

private fun SearchUiState.toSearchResultsSettingsState() =
    SearchResultsMapperState(
        suggestionExcludedApps = suggestionExcludedApps,
        resultExcludedApps = resultExcludedApps,
        excludedContacts = excludedContacts,
        excludedFiles = excludedFiles,
        excludedSettings = excludedSettings,
        excludedAppShortcuts = excludedAppShortcuts,
        disabledSections = disabledSections,
        appSuggestionsEnabled = appSuggestionsEnabled,
        webSuggestionsEnabled = webSuggestionsEnabled,
        webSuggestionsCount = webSuggestionsCount,
        topResultIndicatorEnabled = topResultIndicatorEnabled,
        recentQueriesEnabled = recentQueriesEnabled,
    )

private data class SearchEngineMapperState(
    val searchEngineOrder: List<com.tk.quicksearch.search.core.SearchTarget>,
    val disabledSearchEngines: Set<String>,
    val shortcutCodes: Map<String, String>,
    val shortcutEnabled: Map<String, Boolean>,
    val isSearchEngineCompactMode: Boolean,
    val searchEngineCompactRowCount: Int,
    val isSearchEngineAliasSuffixEnabled: Boolean,
    val isAliasTriggerAfterSpaceEnabled: Boolean,
    val amazonDomain: String?,
    val hasGeminiApiKey: Boolean,
    val geminiApiKeyLast4: String?,
    val isSavingGeminiApiKey: Boolean,
    val personalContext: String,
    val geminiModel: String,
    val geminiGroundingEnabled: Boolean,
    val availableGeminiModels: List<com.tk.quicksearch.tools.directSearch.GeminiTextModel>,
)

private fun SearchUiState.toSearchEngineSettingsState() =
    SearchEngineMapperState(
        searchEngineOrder = searchTargetsOrder,
        disabledSearchEngines = disabledSearchTargetIds,
        shortcutCodes = shortcutCodes,
        shortcutEnabled = shortcutEnabled,
        isSearchEngineCompactMode = isSearchEngineCompactMode,
        searchEngineCompactRowCount = searchEngineCompactRowCount,
        isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled,
        isAliasTriggerAfterSpaceEnabled = isAliasTriggerAfterSpaceEnabled,
        amazonDomain = amazonDomain,
        hasGeminiApiKey = hasGeminiApiKey,
        geminiApiKeyLast4 = geminiApiKeyLast4,
        isSavingGeminiApiKey = isSavingGeminiApiKey,
        personalContext = personalContext,
        geminiModel = geminiModel,
        geminiGroundingEnabled = geminiGroundingEnabled,
        availableGeminiModels = availableGeminiModels,
    )

private data class FileSearchMapperState(
    val enabledFileTypes: Set<com.tk.quicksearch.search.models.FileType>,
    val showFolders: Boolean,
    val showSystemFiles: Boolean,
    val folderWhitelistPatterns: Set<String>,
    val folderBlacklistPatterns: Set<String>,
    val excludedFileExtensions: Set<String>,
)

private fun SearchUiState.toFileSearchSettingsState() =
    FileSearchMapperState(
        enabledFileTypes = enabledFileTypes,
        showFolders = showFolders,
        showSystemFiles = showSystemFiles,
        folderWhitelistPatterns = folderWhitelistPatterns,
        folderBlacklistPatterns = folderBlacklistPatterns,
        excludedFileExtensions = excludedFileExtensions,
    )

private data class AppearanceMapperState(
    val oneHandedMode: Boolean,
    val bottomSearchBarEnabled: Boolean,
    val overlayModeEnabled: Boolean,
    val overlayBlurEffectEnabled: Boolean,
    val hasSeenOverlayAssistantTip: Boolean,
    val hasWallpaperPermission: Boolean,
    val wallpaperAvailable: Boolean,
    val wallpaperBackgroundAlpha: Float,
    val wallpaperBlurRadius: Float,
    val appTheme: com.tk.quicksearch.search.core.AppTheme,
    val overlayThemeIntensity: Float,
    val appThemeMode: com.tk.quicksearch.search.core.AppThemeMode,
    val fontScaleMultiplier: Float,
    val backgroundSource: com.tk.quicksearch.search.core.BackgroundSource,
    val customImageUri: String?,
    val selectedIconPackPackage: String?,
    val availableIconPacks: List<com.tk.quicksearch.search.core.IconPackInfo>,
    val showAppLabels: Boolean,
    val phoneAppGridColumns: Int,
    val appIconShape: com.tk.quicksearch.search.core.AppIconShape,
    val launcherAppIcon: com.tk.quicksearch.search.core.LauncherAppIcon,
    val themedIconsEnabled: Boolean,
    val wallpaperAccentEnabled: Boolean,
    val openKeyboardOnLaunch: Boolean,
    val clearQueryOnLaunch: Boolean,
    val autoCloseOverlay: Boolean,
)

private fun SearchUiState.toAppearanceSettingsState() =
    AppearanceMapperState(
        oneHandedMode = oneHandedMode,
        bottomSearchBarEnabled = bottomSearchBarEnabled,
        overlayModeEnabled = overlayModeEnabled,
        overlayBlurEffectEnabled = overlayBlurEffectEnabled,
        hasSeenOverlayAssistantTip = hasSeenOverlayAssistantTip,
        hasWallpaperPermission = hasWallpaperPermission,
        wallpaperAvailable = wallpaperAvailable,
        wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
        wallpaperBlurRadius = wallpaperBlurRadius,
        appTheme = appTheme,
        overlayThemeIntensity = overlayThemeIntensity,
        appThemeMode = appThemeMode,
        fontScaleMultiplier = fontScaleMultiplier,
        backgroundSource = backgroundSource,
        customImageUri = customImageUri,
        selectedIconPackPackage = selectedIconPackPackage,
        availableIconPacks = availableIconPacks,
        showAppLabels = showAppLabels,
        phoneAppGridColumns = phoneAppGridColumns,
        appIconShape = appIconShape,
        launcherAppIcon = launcherAppIcon,
        themedIconsEnabled = themedIconsEnabled,
        wallpaperAccentEnabled = wallpaperAccentEnabled,
        openKeyboardOnLaunch = openKeyboardOnLaunch,
        clearQueryOnLaunch = clearQueryOnLaunch,
        autoCloseOverlay = autoCloseOverlay,
    )

private data class ToolsMapperState(
    val calculatorEnabled: Boolean,
    val unitConverterEnabled: Boolean,
    val dateCalculatorEnabled: Boolean,
    val currencyConverterEnabled: Boolean,
    val wordClockEnabled: Boolean,
    val dictionaryEnabled: Boolean,
)

private fun SearchUiState.toToolsSettingsState() =
    ToolsMapperState(
        calculatorEnabled = calculatorEnabled,
        unitConverterEnabled = unitConverterEnabled,
        dateCalculatorEnabled = dateCalculatorEnabled,
        currencyConverterEnabled = currencyConverterEnabled,
        wordClockEnabled = wordClockEnabled,
        dictionaryEnabled = dictionaryEnabled,
    )

private data class AppShortcutsMapperState(
    val allAppShortcuts: List<com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut>,
    val allDeviceSettings: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting>,
    val allApps: List<com.tk.quicksearch.search.models.AppInfo>,
    val disabledAppShortcutIds: Set<String>,
    val messagingApp: com.tk.quicksearch.search.core.MessagingApp,
    val callingApp: com.tk.quicksearch.search.core.CallingApp,
    val isWhatsAppInstalled: Boolean,
    val isTelegramInstalled: Boolean,
    val isSignalInstalled: Boolean,
    val isGoogleMeetInstalled: Boolean,
    val directDialEnabled: Boolean,
)

private fun SearchUiState.toAppShortcutsSettingsState() =
    AppShortcutsMapperState(
        allAppShortcuts = allAppShortcuts,
        allDeviceSettings = allDeviceSettings,
        allApps = allApps,
        disabledAppShortcutIds = disabledAppShortcutIds,
        messagingApp = messagingApp,
        callingApp = callingApp,
        isWhatsAppInstalled = isWhatsAppInstalled,
        isTelegramInstalled = isTelegramInstalled,
        isSignalInstalled = isSignalInstalled,
        isGoogleMeetInstalled = isGoogleMeetInstalled,
        directDialEnabled = directDialEnabled,
    )
