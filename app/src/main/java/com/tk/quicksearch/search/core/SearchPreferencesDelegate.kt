package com.tk.quicksearch.search.core

import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.search.apps.IconPackService
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.tools.directSearch.DirectSearchHandler
import com.tk.quicksearch.tools.directSearch.GeminiModelCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal interface SearchPreferencesStateAccess {
    var enabledFileTypes: Set<FileType>
    var showFolders: Boolean
    var showSystemFiles: Boolean
    var folderWhitelistPatterns: Set<String>
    var folderBlacklistPatterns: Set<String>
    var oneHandedMode: Boolean
    var bottomSearchBarEnabled: Boolean
    var topResultIndicatorEnabled: Boolean
    var wallpaperAccentEnabled: Boolean
    var openKeyboardOnLaunch: Boolean
    var overlayModeEnabled: Boolean
    var autoCloseOverlay: Boolean
    var appSuggestionsEnabled: Boolean
    var showAppLabels: Boolean
    var phoneAppGridColumns: Int
    var appIconShape: AppIconShape
    var launcherAppIcon: LauncherAppIcon
    var themedIconsEnabled: Boolean
    var deviceThemeEnabled: Boolean
    var maskUnsupportedIconPackIcons: Boolean
    var wallpaperBackgroundAlpha: Float
    var wallpaperBlurRadius: Float
    var appTheme: AppTheme
    var overlayThemeIntensity: Float
    var fontScaleMultiplier: Float
    var backgroundSource: BackgroundSource
    var customImageUri: String?
    var clearQueryOnLaunch: Boolean
    var amazonDomain: String?

    fun computeEffectiveIsDarkMode(): Boolean

    fun applyLauncherIconSelection(selection: LauncherAppIcon? = null)

    fun saveStartupSurfaceSnapshotAsync(
        forcePreviewRefresh: Boolean = false,
        allowDuringQuery: Boolean = false,
    )
}

internal class SearchPreferencesDelegate(
    private val scope: CoroutineScope,
    private val applicationProvider: () -> android.app.Application,
    private val userPreferences: UserAppPreferences,
    private val directSearchHandler: DirectSearchHandler,
    private val searchEngineManager: com.tk.quicksearch.searchEngines.SearchEngineManager,
    private val iconPackHandler: IconPackService,
    private val secondarySearchOrchestrator: com.tk.quicksearch.searchEngines.SecondarySearchOrchestrator,
    private val resultsStateProvider: () -> SearchResultsState,
    private val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
    private val updateConfigState: ((SearchUiConfigState) -> SearchUiConfigState) -> Unit,
    private val updateFeatureState: ((SearchFeatureState) -> SearchFeatureState) -> Unit,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val refreshAppSuggestions: () -> Unit,
    private val refreshRecentItems: () -> Unit,
    private val stateAccess: SearchPreferencesStateAccess,
) {
    fun setCalculatorEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setCalculatorEnabled(enabled)
            updateFeatureState { it.copy(calculatorEnabled = enabled) }
        }
    }

    fun setUnitConverterEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setUnitConverterEnabled(enabled)
            updateFeatureState { it.copy(unitConverterEnabled = enabled) }
        }
    }

    fun setDateCalculatorEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setDateCalculatorEnabled(enabled)
            updateFeatureState { it.copy(dateCalculatorEnabled = enabled) }
        }
    }

    fun setCurrencyConverterEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setCurrencyConverterEnabled(enabled)
            updateFeatureState { it.copy(currencyConverterEnabled = enabled) }
            if (!enabled) {
                updateResultsState { it.copy(currencyConverterState = CurrencyConverterState()) }
            }
        }
    }

    fun setWordClockEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setWordClockEnabled(enabled)
            updateFeatureState { it.copy(wordClockEnabled = enabled) }
            if (!enabled) {
                updateResultsState { it.copy(wordClockState = WordClockState()) }
            }
        }
    }

    fun setDictionaryEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setDictionaryEnabled(enabled)
            updateFeatureState { it.copy(dictionaryEnabled = enabled) }
            if (!enabled) {
                updateResultsState { it.copy(dictionaryState = DictionaryState()) }
            }
        }
    }

    fun dismissOverlayAssistantTip() {
        scope.launch(Dispatchers.IO) {
            userPreferences.setHasSeenOverlayAssistantTip(true)
            updateConfigState { it.copy(hasSeenOverlayAssistantTip = true) }
        }
    }

    fun setAppSuggestionsEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setAppSuggestionsEnabled,
            stateUpdater = {
                stateAccess.appSuggestionsEnabled = it
                updateUiState { state -> state.copy(appSuggestionsEnabled = it) }
                refreshAppSuggestions()
                stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            },
        )
    }

    fun setShowAppLabels(show: Boolean) {
        updateBooleanPreference(
            value = show,
            preferenceSetter = userPreferences::setShowAppLabels,
            stateUpdater = {
                stateAccess.showAppLabels = it
                updateUiState { state -> state.copy(showAppLabels = it) }
                stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            },
        )
    }

    fun setPhoneAppGridColumns(columns: Int) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setPhoneAppGridColumns(columns)
            stateAccess.phoneAppGridColumns = columns
            updateConfigState { state -> state.copy(phoneAppGridColumns = columns) }
            refreshAppSuggestions()
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setWebSuggestionsCount(count: Int) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setWebSuggestionsCount(count)
            updateFeatureState { it.copy(webSuggestionsCount = count) }
        }
    }

    fun setRecentQueriesEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setRecentQueriesEnabled(enabled)
            if (!enabled) {
                userPreferences.clearRecentQueries()
            }
            updateFeatureState { it.copy(recentQueriesEnabled = enabled) }
            updateResultsState {
                it.copy(recentItems = if (enabled) it.recentItems else emptyList())
            }
            if (enabled && resultsStateProvider().query.isEmpty()) {
                refreshRecentItems()
            }
        }
    }

    fun dismissSearchHistoryTip() {
        scope.launch(Dispatchers.IO) {
            userPreferences.setSearchHistoryTipDismissed(true)
            updateFeatureState { it.copy(hasDismissedSearchHistoryTip = true) }
        }
    }

    fun setWallpaperBackgroundAlpha(alpha: Float) {
        scope.launch(Dispatchers.IO) {
            val sanitizedAlpha = alpha.coerceIn(0f, 1f)
            userPreferences.setWallpaperBackgroundAlpha(
                sanitizedAlpha,
                stateAccess.computeEffectiveIsDarkMode(),
            )
            stateAccess.wallpaperBackgroundAlpha = sanitizedAlpha
            updateConfigState { it.copy(wallpaperBackgroundAlpha = sanitizedAlpha) }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setWallpaperBlurRadius(radius: Float) {
        scope.launch(Dispatchers.IO) {
            val sanitizedRadius = radius.coerceIn(0f, UiPreferences.MAX_WALLPAPER_BLUR_RADIUS)
            userPreferences.setWallpaperBlurRadius(
                sanitizedRadius,
                stateAccess.computeEffectiveIsDarkMode(),
            )
            stateAccess.wallpaperBlurRadius = sanitizedRadius
            updateConfigState { it.copy(wallpaperBlurRadius = sanitizedRadius) }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setAppTheme(theme: AppTheme) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.appTheme == theme) return@launch
            userPreferences.setAppTheme(theme)
            stateAccess.appTheme = theme
            updateConfigState { it.copy(appTheme = theme) }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setAppThemeMode(theme: AppThemeMode) {
        val previousIsDark = stateAccess.computeEffectiveIsDarkMode()
        userPreferences.setAppThemeMode(theme)
        updateConfigState { it.copy(appThemeMode = theme) }
        val newIsDark =
            when (theme) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> {
                    val nightModeFlags =
                        applicationProvider().applicationContext.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }
        if (newIsDark != previousIsDark) {
            scope.launch(Dispatchers.IO) {
                val newAlpha = userPreferences.getWallpaperBackgroundAlpha(newIsDark)
                val newBlur = userPreferences.getWallpaperBlurRadius(newIsDark)
                stateAccess.wallpaperBackgroundAlpha = newAlpha
                stateAccess.wallpaperBlurRadius = newBlur
                updateConfigState {
                    it.copy(
                        wallpaperBackgroundAlpha = newAlpha,
                        wallpaperBlurRadius = newBlur,
                    )
                }
                stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            }
        }
    }

    fun setOverlayThemeIntensity(intensity: Float) {
        scope.launch(Dispatchers.IO) {
            val sanitizedIntensity =
                intensity.coerceIn(
                    UiPreferences.MIN_OVERLAY_THEME_INTENSITY,
                    UiPreferences.MAX_OVERLAY_THEME_INTENSITY,
                )
            if (stateAccess.overlayThemeIntensity == sanitizedIntensity) return@launch
            userPreferences.setOverlayThemeIntensity(sanitizedIntensity)
            stateAccess.overlayThemeIntensity = sanitizedIntensity
            updateConfigState { it.copy(overlayThemeIntensity = sanitizedIntensity) }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setFontScaleMultiplier(multiplier: Float) {
        scope.launch(Dispatchers.IO) {
            val sanitizedMultiplier =
                multiplier.coerceIn(
                    UiPreferences.MIN_FONT_SCALE_MULTIPLIER,
                    UiPreferences.MAX_FONT_SCALE_MULTIPLIER,
                )
            if (stateAccess.fontScaleMultiplier == sanitizedMultiplier) return@launch
            userPreferences.setFontScaleMultiplier(sanitizedMultiplier)
            stateAccess.fontScaleMultiplier = sanitizedMultiplier
            updateConfigState { it.copy(fontScaleMultiplier = sanitizedMultiplier) }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setBackgroundSource(source: BackgroundSource) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.backgroundSource == source) return@launch
            userPreferences.setBackgroundSource(source)
            stateAccess.backgroundSource = source
            val autoTheme =
                if (source != BackgroundSource.THEME && stateAccess.appTheme != AppTheme.MONOCHROME) {
                    userPreferences.setAppTheme(AppTheme.MONOCHROME)
                    stateAccess.appTheme = AppTheme.MONOCHROME
                    AppTheme.MONOCHROME
                } else {
                    null
                }
            updateConfigState {
                it.copy(
                    backgroundSource = source,
                    showWallpaperBackground = source != BackgroundSource.THEME,
                    appTheme = autoTheme ?: it.appTheme,
                )
            }
            stateAccess.saveStartupSurfaceSnapshotAsync(
                forcePreviewRefresh = true,
                allowDuringQuery = true,
            )
        }
    }

    fun setCustomImageUri(uri: String?) {
        scope.launch(Dispatchers.IO) {
            val normalized = uri?.trim()?.takeIf { it.isNotEmpty() }
            if (stateAccess.customImageUri == normalized) return@launch
            userPreferences.setCustomImageUri(normalized)
            stateAccess.customImageUri = normalized
            updateConfigState { it.copy(customImageUri = normalized) }
            stateAccess.saveStartupSurfaceSnapshotAsync(
                forcePreviewRefresh = true,
                allowDuringQuery = true,
            )
        }
    }

    fun refreshIconPacks() = iconPackHandler.refreshIconPacks()

    fun setIconPackPackage(packageName: String?) {
        val state = resultsStateProvider()
        val visiblePackageNames =
            buildList {
                addAll(state.pinnedApps.map { it.packageName })
                addAll(state.recentApps.map { it.packageName })
                addAll(state.searchResults.map { it.packageName })
            }

        iconPackHandler.setIconPackPackage(
            packageName = packageName,
            visiblePackageNames = visiblePackageNames,
        )
    }

    fun setAppIconShape(shape: AppIconShape) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.appIconShape == shape) return@launch
            userPreferences.setAppIconShape(shape)
            stateAccess.appIconShape = shape
            updateConfigState { it.copy(appIconShape = shape) }
        }
    }

    fun setLauncherAppIcon(selection: LauncherAppIcon) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.launcherAppIcon == selection) return@launch
            userPreferences.setLauncherAppIcon(selection)
            stateAccess.launcherAppIcon = selection
            updateConfigState { it.copy(launcherAppIcon = selection) }
            stateAccess.applyLauncherIconSelection(selection)
        }
    }

    fun onSystemDarkModeChanged(isDarkMode: Boolean, currentAppThemeMode: AppThemeMode) {
    }

    fun setThemedIconsEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.themedIconsEnabled == enabled) return@launch
            userPreferences.setThemedIconsEnabled(enabled)
            stateAccess.themedIconsEnabled = enabled
            updateConfigState { it.copy(themedIconsEnabled = enabled) }
        }
    }

    fun setDeviceThemeEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.deviceThemeEnabled == enabled) return@launch
            userPreferences.setDeviceThemeEnabled(enabled)
            stateAccess.deviceThemeEnabled = enabled
            updateConfigState { it.copy(deviceThemeEnabled = enabled) }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setIconPackUnsupportedIconMaskEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.maskUnsupportedIconPackIcons == enabled) return@launch
            userPreferences.setIconPackUnsupportedIconMaskEnabled(enabled)
            stateAccess.maskUnsupportedIconPackIcons = enabled
            updateConfigState { it.copy(maskUnsupportedIconPackIcons = enabled) }
            com.tk.quicksearch.search.apps.invalidateAppIconCache()
        }
    }

    fun setSearchEngineAliasSuffixEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setSearchEngineAliasSuffixEnabled(enabled)
            updateFeatureState { it.copy(isSearchEngineAliasSuffixEnabled = enabled) }
        }
    }

    fun setAliasTriggerAfterSpaceEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setAliasTriggerAfterSpaceEnabled(enabled)
            updateFeatureState { it.copy(isAliasTriggerAfterSpaceEnabled = enabled) }
        }
    }

    fun setFileTypeEnabled(fileType: FileType, enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            val updated =
                stateAccess.enabledFileTypes.toMutableSet().apply {
                    if (enabled) add(fileType) else remove(fileType)
                }
            stateAccess.enabledFileTypes = updated
            userPreferences.setEnabledFileTypes(stateAccess.enabledFileTypes)
            updateUiState { it.copy(enabledFileTypes = stateAccess.enabledFileTypes) }
            rerunSecondarySearchIfNeeded()
        }
    }

    fun setShowFolders(show: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setShowFoldersInResults(show)
            stateAccess.showFolders = show
            updateUiState { it.copy(showFolders = show) }
            rerunSecondarySearchIfNeeded()
        }
    }

    fun setShowSystemFiles(show: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setShowSystemFiles(show)
            stateAccess.showSystemFiles = show
            updateUiState { it.copy(showSystemFiles = show) }
            rerunSecondarySearchIfNeeded()
        }
    }

    fun setFolderWhitelistPatterns(patterns: Set<String>) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setFolderWhitelistPatterns(patterns)
            stateAccess.folderWhitelistPatterns = userPreferences.getFolderWhitelistPatterns()
            updateUiState { it.copy(folderWhitelistPatterns = stateAccess.folderWhitelistPatterns) }
            rerunSecondarySearchIfNeeded()
        }
    }

    fun setFolderBlacklistPatterns(patterns: Set<String>) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setFolderBlacklistPatterns(patterns)
            stateAccess.folderBlacklistPatterns = userPreferences.getFolderBlacklistPatterns()
            updateUiState { it.copy(folderBlacklistPatterns = stateAccess.folderBlacklistPatterns) }
            rerunSecondarySearchIfNeeded()
        }
    }

    fun setOneHandedMode(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setOneHandedMode,
            stateUpdater = {
                stateAccess.oneHandedMode = it
                updateUiState { state -> state.copy(oneHandedMode = it) }
                stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            },
        )
    }

    fun setBottomSearchBarEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setBottomSearchBarEnabled,
            stateUpdater = {
                stateAccess.bottomSearchBarEnabled = it
                updateUiState { state -> state.copy(bottomSearchBarEnabled = it) }
                stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            },
        )
    }

    fun setOpenKeyboardOnLaunchEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setOpenKeyboardOnLaunchEnabled,
            stateUpdater = {
                stateAccess.openKeyboardOnLaunch = it
                updateUiState { state -> state.copy(openKeyboardOnLaunch = it) }
                stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            },
        )
    }

    fun setTopResultIndicatorEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setTopResultIndicatorEnabled,
            stateUpdater = {
                stateAccess.topResultIndicatorEnabled = it
                updateUiState { state -> state.copy(topResultIndicatorEnabled = it) }
                stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            },
        )
    }

    fun setWallpaperAccentEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.wallpaperAccentEnabled == enabled) return@launch
            userPreferences.setWallpaperAccentEnabled(enabled)
            stateAccess.wallpaperAccentEnabled = enabled
            updateConfigState { it.copy(wallpaperAccentEnabled = enabled) }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setClearQueryOnLaunchEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setClearQueryOnLaunchEnabled,
            stateUpdater = {
                stateAccess.clearQueryOnLaunch = it
                updateUiState { state -> state.copy(clearQueryOnLaunch = it) }
            },
        )
    }

    fun setAutoCloseOverlayEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setAutoCloseOverlayEnabled,
            stateUpdater = {
                stateAccess.autoCloseOverlay = it
                updateUiState { state -> state.copy(autoCloseOverlay = it) }
            },
        )
    }

    fun setOverlayModeEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setOverlayModeEnabled,
            stateUpdater = {
                stateAccess.overlayModeEnabled = it
                updateUiState { state ->
                    state.copy(
                        overlayModeEnabled = it,
                        wallpaperBackgroundAlpha = stateAccess.wallpaperBackgroundAlpha,
                        wallpaperBlurRadius = stateAccess.wallpaperBlurRadius,
                    )
                }
                if (!it) {
                    OverlayModeController.stopOverlay(applicationProvider())
                }
            },
        )
    }

    fun setAmazonDomain(domain: String?) {
        stateAccess.amazonDomain = domain
        userPreferences.setAmazonDomain(domain)
        updateFeatureState { state -> state.copy(amazonDomain = stateAccess.amazonDomain) }
    }

    fun setGeminiApiKey(apiKey: String?) {
        scope.launch(Dispatchers.IO) {
            updateFeatureState { it.copy(isSavingGeminiApiKey = true) }
            try {
                directSearchHandler.setGeminiApiKey(apiKey)

                val hasGemini = !apiKey.isNullOrBlank()
                searchEngineManager.updateSearchTargetsForGemini(hasGemini)

                val availableModels =
                    if (hasGemini) {
                        directSearchHandler.refreshAvailableGeminiModels(forceRefresh = true)
                    } else {
                        directSearchHandler.getAvailableGeminiModels()
                    }

                updateFeatureState {
                    it.copy(
                        hasGeminiApiKey = hasGemini,
                        geminiApiKeyLast4 = apiKey?.trim()?.takeLast(4),
                        geminiModel = directSearchHandler.getGeminiModel(),
                        availableGeminiModels = availableModels,
                    )
                }
            } finally {
                updateFeatureState { it.copy(isSavingGeminiApiKey = false) }
            }
        }
    }

    fun setPersonalContext(context: String?) {
        scope.launch(Dispatchers.IO) {
            directSearchHandler.setPersonalContext(context)
            updateFeatureState { it.copy(personalContext = context?.trim().orEmpty()) }
        }
    }

    fun setGeminiModel(modelId: String?) {
        scope.launch(Dispatchers.IO) {
            directSearchHandler.setGeminiModel(modelId)
            val normalized = modelId?.trim().takeUnless { it.isNullOrBlank() }
            updateFeatureState {
                it.copy(
                    geminiModel = normalized ?: GeminiModelCatalog.DEFAULT_MODEL_ID,
                )
            }
        }
    }

    fun setGeminiGroundingEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            directSearchHandler.setGeminiGroundingEnabled(enabled)
            updateFeatureState { it.copy(geminiGroundingEnabled = enabled) }
        }
    }

    fun refreshAvailableGeminiModels() {
        scope.launch(Dispatchers.IO) {
            val models = directSearchHandler.refreshAvailableGeminiModels(forceRefresh = true)
            updateFeatureState { it.copy(availableGeminiModels = models) }
        }
    }

    private fun updateBooleanPreference(
        value: Boolean,
        preferenceSetter: (Boolean) -> Unit,
        stateUpdater: (Boolean) -> Unit,
    ) {
        scope.launch(Dispatchers.IO) {
            preferenceSetter(value)
            stateUpdater(value)
        }
    }

    fun loadCustomToolsInitialState(): Pair<List<CustomTool>, Set<String>> {
        val tools = userPreferences.getCustomTools()
        val disabled = userPreferences.getDisabledCustomTools()
        return Pair(tools, disabled)
    }

    fun addCustomTool(
        name: String,
        prompt: String,
        modelId: String,
        groundingEnabled: Boolean = false,
        aliasCode: String = "",
    ) {
        scope.launch(Dispatchers.IO) {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch
            val id = "custom_tool:${java.util.UUID.randomUUID()}"
            val newTool = CustomTool(
                id = id,
                name = trimmedName,
                prompt = prompt.trim(),
                modelId = modelId,
                groundingEnabled = groundingEnabled,
            )
            val existing = userPreferences.getCustomTools()
            val updated = existing + newTool
            userPreferences.setCustomTools(updated)
            val normalizedAlias = aliasCode.trim()
            if (normalizedAlias.isNotBlank()) {
                userPreferences.setAliasCode(id, normalizedAlias)
            }
            updateFeatureState { state ->
                state.copy(
                    customTools = updated,
                    shortcutCodes = if (normalizedAlias.isNotBlank()) {
                        state.shortcutCodes + (id to normalizedAlias)
                    } else {
                        state.shortcutCodes
                    },
                )
            }
        }
    }

    fun updateCustomTool(
        id: String,
        name: String,
        prompt: String,
        modelId: String,
        groundingEnabled: Boolean = false,
    ) {
        scope.launch(Dispatchers.IO) {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch
            val existing = userPreferences.getCustomTools()
            val updated =
                existing.map { tool ->
                    if (tool.id == id) {
                        tool.copy(
                            name = trimmedName,
                            prompt = prompt.trim(),
                            modelId = modelId,
                            groundingEnabled = groundingEnabled,
                        )
                    } else {
                        tool
                    }
                }
            userPreferences.setCustomTools(updated)
            updateFeatureState { it.copy(customTools = updated) }
        }
    }

    fun deleteCustomTool(id: String) {
        scope.launch(Dispatchers.IO) {
            val existing = userPreferences.getCustomTools()
            val updated = existing.filterNot { it.id == id }
            userPreferences.setCustomTools(updated)
            val disabled = userPreferences.getDisabledCustomTools() - id
            userPreferences.setDisabledCustomTools(disabled)
            updateFeatureState { it.copy(customTools = updated, disabledCustomToolIds = disabled) }
        }
    }

    fun setCustomToolEnabled(
        id: String,
        enabled: Boolean,
    ) {
        scope.launch(Dispatchers.IO) {
            val disabled = userPreferences.getDisabledCustomTools().toMutableSet()
            if (enabled) {
                disabled.remove(id)
            } else {
                disabled.add(id)
            }
            userPreferences.setDisabledCustomTools(disabled)
            updateFeatureState { it.copy(disabledCustomToolIds = disabled) }
        }
    }

    private fun rerunSecondarySearchIfNeeded() {
        val query = resultsStateProvider().query
        if (query.isNotBlank()) {
            secondarySearchOrchestrator.performSecondarySearches(query)
        }
    }
}
