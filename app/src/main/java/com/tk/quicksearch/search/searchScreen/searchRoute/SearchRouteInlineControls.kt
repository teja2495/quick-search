package com.tk.quicksearch.search.searchScreen.searchRoute

import com.tk.quicksearch.search.appSettings.AiModelInlineState
import com.tk.quicksearch.search.appSettings.AppSettingInlineControls
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.settings.appearanceSettings.hasWallpaperColorSource

/** Binds the app-setting rows that edit their value inline to the current state and ViewModel setters. */
internal fun appSettingInlineControls(
    uiState: SearchUiState,
    viewModel: SearchViewModel,
): AppSettingInlineControls =
    AppSettingInlineControls(
        fontScaleMultiplier = uiState.fontScaleMultiplier,
        onFontScaleMultiplierChange = viewModel::setFontScaleMultiplier,
        appIconSizeStep = uiState.appIconSizeStep,
        onAppIconSizeStepChange = viewModel::setAppIconSizeStep,
        homeTextColorOverride = uiState.homeTextColorOverride,
        onHomeTextColorChange = viewModel::setHomeTextColorOverride,
        backgroundSource = uiState.backgroundSource,
        customImageUri = uiState.customImageUri,
        appThemeMode = uiState.appThemeMode,
        onAppThemeModeChange = viewModel::setAppThemeMode,
        topMatchesLimit = uiState.topMatchesLimit,
        onTopMatchesLimitChange = viewModel::setTopMatchesLimit,
        hasEnabledSearchEngines = uiState.searchTargetsOrder.any { target ->
            target.getId() !in uiState.disabledSearchTargetIds
        },
        searchEngineCompactMode = uiState.isSearchEngineCompactMode,
        onSearchEngineCompactModeChange = viewModel::setSearchEngineCompactMode,
        searchEngineCompactRowCount = uiState.searchEngineCompactRowCount,
        onSearchEngineCompactRowCountChange = viewModel::setSearchEngineCompactRowCount,
        recentQueriesDisplayCount = uiState.recentQueriesDisplayCount,
        onRecentQueriesDisplayCountChange = viewModel::setRecentQueriesDisplayCount,
        appTheme = uiState.appTheme,
        deviceThemeEnabled = uiState.deviceThemeEnabled,
        wallpaperAccentAvailable = hasWallpaperColorSource(
            backgroundSource = uiState.backgroundSource,
            hasWallpaperPermission = uiState.hasWallpaperPermission,
            wallpaperAvailable = uiState.wallpaperAvailable,
        ),
        accentColorMode = uiState.accentColorMode,
        customAccentColorArgb = uiState.customAccentColorArgb,
        onAccentColorModeChange = viewModel::setAccentColorMode,
        onCustomAccentColorChange = viewModel::setCustomAccentColorArgb,
        aiModel =
            AiModelInlineState(
                providerId = uiState.aiSearchLlmProviderId,
                selectedModelId = uiState.activeLlmModel,
                availableModels = uiState.activeLlmAvailableModels,
                modelsByProvider = uiState.availableLlmModelsByProvider,
                configuredProviderIds = uiState.llmApiKeyLast4ByProvider.keys,
                thinkingEnabled = uiState.activeLlmThinkingEnabled,
                groundingEnabled = uiState.activeLlmGroundingEnabled,
                onThinkingChange = viewModel::setActiveLlmThinkingEnabled,
                onGroundingChange = viewModel::setActiveLlmGroundingEnabled,
                onRefreshModels = viewModel::refreshAvailableLlmModels,
            ),
    )
