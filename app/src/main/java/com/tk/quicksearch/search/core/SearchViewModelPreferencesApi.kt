package com.tk.quicksearch.search.core

internal interface SearchViewModelPreferencesApi {
    val preferencesApiDelegate: SearchViewModelPreferencesApiDelegate

    fun setCalculatorEnabled(enabled: Boolean) = preferencesApiDelegate.setCalculatorEnabled(enabled)

    fun setUnitConverterEnabled(enabled: Boolean) =
        preferencesApiDelegate.setUnitConverterEnabled(enabled)

    fun setDateCalculatorEnabled(enabled: Boolean) =
        preferencesApiDelegate.setDateCalculatorEnabled(enabled)

    fun setCurrencyConverterEnabled(enabled: Boolean) =
        preferencesApiDelegate.setCurrencyConverterEnabled(enabled)

    fun setWordClockEnabled(enabled: Boolean) = preferencesApiDelegate.setWordClockEnabled(enabled)

    fun setDictionaryEnabled(enabled: Boolean) = preferencesApiDelegate.setDictionaryEnabled(enabled)

    fun addCustomTool(
        name: String,
        prompt: String,
        modelId: String,
        groundingEnabled: Boolean = false,
        aliasCode: String = "",
        thinkingEnabled: Boolean = false,
    ) = preferencesApiDelegate.addCustomTool(name, prompt, modelId, groundingEnabled, aliasCode, thinkingEnabled)

    fun updateCustomTool(
        id: String,
        name: String,
        prompt: String,
        modelId: String,
        groundingEnabled: Boolean = false,
        thinkingEnabled: Boolean = false,
    ) = preferencesApiDelegate.updateCustomTool(id, name, prompt, modelId, groundingEnabled, thinkingEnabled)

    fun deleteCustomTool(id: String) = preferencesApiDelegate.deleteCustomTool(id)

    fun setCustomToolEnabled(
        id: String,
        enabled: Boolean,
    ) = preferencesApiDelegate.setCustomToolEnabled(id, enabled)

    fun dismissOverlayAssistantTip() = preferencesApiDelegate.dismissOverlayAssistantTip()

    fun setAppSuggestionsEnabled(enabled: Boolean) =
        preferencesApiDelegate.setAppSuggestionsEnabled(enabled)

    fun setShowAppLabels(show: Boolean) = preferencesApiDelegate.setShowAppLabels(show)

    fun setPhoneAppGridColumns(columns: Int) = preferencesApiDelegate.setPhoneAppGridColumns(columns)

    fun setWebSuggestionsEnabled(enabled: Boolean) =
        preferencesApiDelegate.setWebSuggestionsEnabled(enabled)

    fun setWebSuggestionsCount(count: Int) = preferencesApiDelegate.setWebSuggestionsCount(count)

    fun setRecentQueriesEnabled(enabled: Boolean) =
        preferencesApiDelegate.setRecentQueriesEnabled(enabled)

    fun setTopMatchesEnabled(enabled: Boolean) =
        preferencesApiDelegate.setTopMatchesEnabled(enabled)

    fun setTopMatchesLimit(limit: Int) = preferencesApiDelegate.setTopMatchesLimit(limit)

    fun setTopMatchesSectionOrder(order: List<SearchSection>) =
        preferencesApiDelegate.setTopMatchesSectionOrder(order)

    fun setTopMatchesSectionEnabled(section: SearchSection, enabled: Boolean) =
        preferencesApiDelegate.setTopMatchesSectionEnabled(section, enabled)

    fun setShowTodayEvents(enabled: Boolean) = preferencesApiDelegate.setShowTodayEvents(enabled)

    fun dismissSearchHistoryTip() = preferencesApiDelegate.dismissSearchHistoryTip()

    fun setWallpaperBackgroundAlpha(alpha: Float) =
        preferencesApiDelegate.setWallpaperBackgroundAlpha(alpha)

    fun setWallpaperBlurRadius(radius: Float) = preferencesApiDelegate.setWallpaperBlurRadius(radius)

    fun setAppTheme(theme: AppTheme) = preferencesApiDelegate.setAppTheme(theme)

    fun setAppThemeMode(theme: AppThemeMode) = preferencesApiDelegate.setAppThemeMode(theme)

    fun setOverlayThemeIntensity(intensity: Float) =
        preferencesApiDelegate.setOverlayThemeIntensity(intensity)

    fun setFontScaleMultiplier(multiplier: Float) =
        preferencesApiDelegate.setFontScaleMultiplier(multiplier)

    fun setUseSystemFont(enabled: Boolean) =
        preferencesApiDelegate.setUseSystemFont(enabled)

    fun setBackgroundSource(source: BackgroundSource) =
        preferencesApiDelegate.setBackgroundSource(source)

    fun setCustomImageUri(uri: String?) = preferencesApiDelegate.setCustomImageUri(uri)

    fun refreshIconPacks() = preferencesApiDelegate.refreshIconPacks()

    fun setIconPackPackage(packageName: String?) =
        preferencesApiDelegate.setIconPackPackage(packageName)

    fun setIconPackUnsupportedIconMaskEnabled(enabled: Boolean) =
        preferencesApiDelegate.setIconPackUnsupportedIconMaskEnabled(enabled)

    fun setAppIconShape(shape: AppIconShape) = preferencesApiDelegate.setAppIconShape(shape)

    fun setLauncherAppIcon(selection: LauncherAppIcon) =
        preferencesApiDelegate.setLauncherAppIcon(selection)

    fun onSystemDarkModeChanged(isDarkMode: Boolean) =
        preferencesApiDelegate.onSystemDarkModeChanged(isDarkMode)

    fun setThemedIconsEnabled(enabled: Boolean) = preferencesApiDelegate.setThemedIconsEnabled(enabled)

    fun setDeviceThemeEnabled(enabled: Boolean) = preferencesApiDelegate.setDeviceThemeEnabled(enabled)

    fun setSearchEngineAliasSuffixEnabled(enabled: Boolean) =
        preferencesApiDelegate.setSearchEngineAliasSuffixEnabled(enabled)

    fun setAliasTriggerAfterSpaceEnabled(enabled: Boolean) =
        preferencesApiDelegate.setAliasTriggerAfterSpaceEnabled(enabled)

    fun setFileTypeEnabled(fileType: com.tk.quicksearch.search.models.FileType, enabled: Boolean) =
        preferencesApiDelegate.setFileTypeEnabled(fileType, enabled)

    fun setShowFolders(show: Boolean) = preferencesApiDelegate.setShowFolders(show)

    fun setShowSystemFiles(show: Boolean) = preferencesApiDelegate.setShowSystemFiles(show)

    fun setFolderWhitelistPatterns(patterns: Set<String>) =
        preferencesApiDelegate.setFolderWhitelistPatterns(patterns)

    fun setFolderBlacklistPatterns(patterns: Set<String>) =
        preferencesApiDelegate.setFolderBlacklistPatterns(patterns)

    fun setOneHandedMode(enabled: Boolean) = preferencesApiDelegate.setOneHandedMode(enabled)

    fun setBottomSearchBarEnabled(enabled: Boolean) =
        preferencesApiDelegate.setBottomSearchBarEnabled(enabled)

    fun setOpenKeyboardOnLaunchEnabled(enabled: Boolean) =
        preferencesApiDelegate.setOpenKeyboardOnLaunchEnabled(enabled)

    fun setTopResultIndicatorEnabled(enabled: Boolean) =
        preferencesApiDelegate.setTopResultIndicatorEnabled(enabled)

    fun setWallpaperAccentEnabled(enabled: Boolean) =
        preferencesApiDelegate.setWallpaperAccentEnabled(enabled)

    fun setClearQueryOnLaunchEnabled(enabled: Boolean) =
        preferencesApiDelegate.setClearQueryOnLaunchEnabled(enabled)

    fun setAutoCloseOverlayEnabled(enabled: Boolean) =
        preferencesApiDelegate.setAutoCloseOverlayEnabled(enabled)

    fun setOverlayModeEnabled(enabled: Boolean) = preferencesApiDelegate.setOverlayModeEnabled(enabled)

    fun setAmazonDomain(domain: String?) = preferencesApiDelegate.setAmazonDomain(domain)

    fun setGeminiApiKey(apiKey: String?) = preferencesApiDelegate.setGeminiApiKey(apiKey)

    fun setPersonalContext(context: String?) = preferencesApiDelegate.setPersonalContext(context)

    fun setGeminiModel(modelId: String?) = preferencesApiDelegate.setGeminiModel(modelId)

    fun setGeminiGroundingEnabled(enabled: Boolean) =
        preferencesApiDelegate.setGeminiGroundingEnabled(enabled)

    fun setGeminiThinkingEnabled(enabled: Boolean) =
        preferencesApiDelegate.setGeminiThinkingEnabled(enabled)

    fun refreshAvailableGeminiModels() = preferencesApiDelegate.refreshAvailableGeminiModels()

    fun archiveTodayCalendarEvent(eventId: Long) = preferencesApiDelegate.archiveTodayCalendarEvent(eventId)
}

class SearchViewModelPreferencesApiDelegate internal constructor(
    private val preferencesDelegate: SearchPreferencesDelegate,
    private val webSuggestionHandler: com.tk.quicksearch.search.webSuggestions.WebSuggestionHandler,
    private val iconPackHandler: com.tk.quicksearch.search.apps.IconPackService,
    private val configStateProvider: () -> SearchUiConfigState,
) {
    fun setCalculatorEnabled(enabled: Boolean) = preferencesDelegate.setCalculatorEnabled(enabled)

    fun setUnitConverterEnabled(enabled: Boolean) = preferencesDelegate.setUnitConverterEnabled(enabled)

    fun setDateCalculatorEnabled(enabled: Boolean) = preferencesDelegate.setDateCalculatorEnabled(enabled)

    fun setCurrencyConverterEnabled(enabled: Boolean) =
        preferencesDelegate.setCurrencyConverterEnabled(enabled)

    fun setWordClockEnabled(enabled: Boolean) = preferencesDelegate.setWordClockEnabled(enabled)

    fun setDictionaryEnabled(enabled: Boolean) = preferencesDelegate.setDictionaryEnabled(enabled)

    fun addCustomTool(
        name: String,
        prompt: String,
        modelId: String,
        groundingEnabled: Boolean = false,
        aliasCode: String = "",
        thinkingEnabled: Boolean = false,
    ) = preferencesDelegate.addCustomTool(name, prompt, modelId, groundingEnabled, aliasCode, thinkingEnabled)

    fun updateCustomTool(
        id: String,
        name: String,
        prompt: String,
        modelId: String,
        groundingEnabled: Boolean = false,
        thinkingEnabled: Boolean = false,
    ) = preferencesDelegate.updateCustomTool(id, name, prompt, modelId, groundingEnabled, thinkingEnabled)

    fun deleteCustomTool(id: String) = preferencesDelegate.deleteCustomTool(id)

    fun setCustomToolEnabled(
        id: String,
        enabled: Boolean,
    ) = preferencesDelegate.setCustomToolEnabled(id, enabled)

    fun dismissOverlayAssistantTip() = preferencesDelegate.dismissOverlayAssistantTip()

    fun setAppSuggestionsEnabled(enabled: Boolean) =
        preferencesDelegate.setAppSuggestionsEnabled(enabled)

    fun setShowAppLabels(show: Boolean) = preferencesDelegate.setShowAppLabels(show)

    fun setPhoneAppGridColumns(columns: Int) = preferencesDelegate.setPhoneAppGridColumns(columns)

    fun setWebSuggestionsEnabled(enabled: Boolean) = webSuggestionHandler.setEnabled(enabled)

    fun setWebSuggestionsCount(count: Int) = preferencesDelegate.setWebSuggestionsCount(count)

    fun setRecentQueriesEnabled(enabled: Boolean) = preferencesDelegate.setRecentQueriesEnabled(enabled)

    fun setShowTodayEvents(enabled: Boolean) = preferencesDelegate.setShowTodayEvents(enabled)

    fun archiveTodayCalendarEvent(eventId: Long) = preferencesDelegate.archiveTodayCalendarEvent(eventId)

    fun dismissSearchHistoryTip() = preferencesDelegate.dismissSearchHistoryTip()

    fun setWallpaperBackgroundAlpha(alpha: Float) =
        preferencesDelegate.setWallpaperBackgroundAlpha(alpha)

    fun setWallpaperBlurRadius(radius: Float) = preferencesDelegate.setWallpaperBlurRadius(radius)

    fun setAppTheme(theme: AppTheme) = preferencesDelegate.setAppTheme(theme)

    fun setAppThemeMode(theme: AppThemeMode) = preferencesDelegate.setAppThemeMode(theme)

    fun setOverlayThemeIntensity(intensity: Float) =
        preferencesDelegate.setOverlayThemeIntensity(intensity)

    fun setFontScaleMultiplier(multiplier: Float) =
        preferencesDelegate.setFontScaleMultiplier(multiplier)

    fun setUseSystemFont(enabled: Boolean) =
        preferencesDelegate.setUseSystemFont(enabled)

    fun setBackgroundSource(source: BackgroundSource) = preferencesDelegate.setBackgroundSource(source)

    fun setCustomImageUri(uri: String?) = preferencesDelegate.setCustomImageUri(uri)

    fun refreshIconPacks() = iconPackHandler.refreshIconPacks()

    fun setIconPackPackage(packageName: String?) = preferencesDelegate.setIconPackPackage(packageName)

    fun setIconPackUnsupportedIconMaskEnabled(enabled: Boolean) =
        preferencesDelegate.setIconPackUnsupportedIconMaskEnabled(enabled)

    fun setAppIconShape(shape: AppIconShape) = preferencesDelegate.setAppIconShape(shape)

    fun setLauncherAppIcon(selection: LauncherAppIcon) =
        preferencesDelegate.setLauncherAppIcon(selection)

    fun onSystemDarkModeChanged(isDarkMode: Boolean) {
        preferencesDelegate.onSystemDarkModeChanged(isDarkMode, configStateProvider().appThemeMode)
    }

    fun setThemedIconsEnabled(enabled: Boolean) = preferencesDelegate.setThemedIconsEnabled(enabled)

    fun setDeviceThemeEnabled(enabled: Boolean) = preferencesDelegate.setDeviceThemeEnabled(enabled)

    fun setSearchEngineAliasSuffixEnabled(enabled: Boolean) =
        preferencesDelegate.setSearchEngineAliasSuffixEnabled(enabled)

    fun setAliasTriggerAfterSpaceEnabled(enabled: Boolean) =
        preferencesDelegate.setAliasTriggerAfterSpaceEnabled(enabled)

    fun setFileTypeEnabled(fileType: com.tk.quicksearch.search.models.FileType, enabled: Boolean) =
        preferencesDelegate.setFileTypeEnabled(fileType, enabled)

    fun setShowFolders(show: Boolean) = preferencesDelegate.setShowFolders(show)

    fun setShowSystemFiles(show: Boolean) = preferencesDelegate.setShowSystemFiles(show)

    fun setFolderWhitelistPatterns(patterns: Set<String>) =
        preferencesDelegate.setFolderWhitelistPatterns(patterns)

    fun setFolderBlacklistPatterns(patterns: Set<String>) =
        preferencesDelegate.setFolderBlacklistPatterns(patterns)

    fun setOneHandedMode(enabled: Boolean) = preferencesDelegate.setOneHandedMode(enabled)

    fun setBottomSearchBarEnabled(enabled: Boolean) =
        preferencesDelegate.setBottomSearchBarEnabled(enabled)

    fun setOpenKeyboardOnLaunchEnabled(enabled: Boolean) =
        preferencesDelegate.setOpenKeyboardOnLaunchEnabled(enabled)

    fun setTopResultIndicatorEnabled(enabled: Boolean) =
        preferencesDelegate.setTopResultIndicatorEnabled(enabled)

    fun setTopMatchesEnabled(enabled: Boolean) = preferencesDelegate.setTopMatchesEnabled(enabled)

    fun setTopMatchesLimit(limit: Int) = preferencesDelegate.setTopMatchesLimit(limit)

    fun setTopMatchesSectionOrder(order: List<SearchSection>) =
        preferencesDelegate.setTopMatchesSectionOrder(order)

    fun setTopMatchesSectionEnabled(section: SearchSection, enabled: Boolean) =
        preferencesDelegate.setTopMatchesSectionEnabled(section, enabled)

    fun setWallpaperAccentEnabled(enabled: Boolean) =
        preferencesDelegate.setWallpaperAccentEnabled(enabled)

    fun setClearQueryOnLaunchEnabled(enabled: Boolean) =
        preferencesDelegate.setClearQueryOnLaunchEnabled(enabled)

    fun setAutoCloseOverlayEnabled(enabled: Boolean) =
        preferencesDelegate.setAutoCloseOverlayEnabled(enabled)

    fun setOverlayModeEnabled(enabled: Boolean) = preferencesDelegate.setOverlayModeEnabled(enabled)

    fun setAmazonDomain(domain: String?) = preferencesDelegate.setAmazonDomain(domain)

    fun setGeminiApiKey(apiKey: String?) = preferencesDelegate.setGeminiApiKey(apiKey)

    fun setPersonalContext(context: String?) = preferencesDelegate.setPersonalContext(context)

    fun setGeminiModel(modelId: String?) = preferencesDelegate.setGeminiModel(modelId)

    fun setGeminiGroundingEnabled(enabled: Boolean) =
        preferencesDelegate.setGeminiGroundingEnabled(enabled)

    fun setGeminiThinkingEnabled(enabled: Boolean) =
        preferencesDelegate.setGeminiThinkingEnabled(enabled)

    fun refreshAvailableGeminiModels() = preferencesDelegate.refreshAvailableGeminiModels()
}
