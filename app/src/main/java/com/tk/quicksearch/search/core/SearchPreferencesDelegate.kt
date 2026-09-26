package com.tk.quicksearch.search.core

import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.search.apps.IconPackService
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.tools.aiSearch.AiSearchHandler
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import com.tk.quicksearch.tools.aiSearch.LlmTextModel
import com.tk.quicksearch.tools.aiSearch.resolveModelSelection
import com.tk.quicksearch.settings.settingsDetailScreen.AiBackedToolConfigId
import com.tk.quicksearch.shared.util.isLowRamDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal interface SearchPreferencesStateAccess {
    var enabledFileTypes: Set<FileType>
    var showFolders: Boolean
    var filePreviewsEnabled: Boolean
    var showSystemFiles: Boolean
    var folderWhitelistPatterns: Set<String>
    var folderBlacklistPatterns: Set<String>
    var oneHandedMode: Boolean
    var bottomSearchBarEnabled: Boolean
    var unifiedPinnedItemsEnabled: Boolean
    var settingsIconEnabled: Boolean
    var topResultIndicatorEnabled: Boolean
    var openTopResultUsingKeyboardEnabled: Boolean
    var accentColorMode: AccentColorMode
    var customAccentColorArgb: Int
    var openKeyboardOnLaunch: Boolean
    var overlayModeEnabled: Boolean
    var autoCloseOverlay: Boolean
    var appSuggestionsEnabled: Boolean
    var showAllAppsButton: Boolean
    var showAppLabels: Boolean
    var phoneAppGridColumns: Int
    var appIconSizeStep: Int
    var appIconShape: AppIconShape
    var launcherAppIcon: LauncherAppIcon
    var themedIconsEnabled: Boolean
    var deviceThemeEnabled: Boolean
    var amoledThemeEnabled: Boolean
    var maskUnsupportedIconPackIcons: Boolean
    var wallpaperBackgroundAlpha: Float
    var wallpaperBlurRadius: Float
    var appTheme: AppTheme
    var overlayThemeIntensity: Float
    var fontScaleMultiplier: Float
    var useSystemFont: Boolean
    var homeTextColorOverride: HomeTextColor?
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
    internal val scope: CoroutineScope,
    internal val applicationProvider: () -> android.app.Application,
    internal val userPreferences: UserAppPreferences,
    internal val aiSearchHandler: AiSearchHandler,
    internal val searchEngineManager: com.tk.quicksearch.searchEngines.SearchEngineManager,
    internal val iconPackHandler: IconPackService,
    internal val secondarySearchOrchestrator: com.tk.quicksearch.searchEngines.SecondarySearchOrchestrator,
    internal val resultsStateProvider: () -> SearchResultsState,
    internal val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
    internal val updateConfigState: ((SearchUiConfigState) -> SearchUiConfigState) -> Unit,
    internal val updateFeatureState: ((SearchFeatureState) -> SearchFeatureState) -> Unit,
    internal val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    internal val refreshAppSuggestions: () -> Unit,
    internal val refreshApps: () -> Unit,
    internal val refreshRecentItems: () -> Unit,
    internal val refreshCalendarEvents: () -> Unit,
    internal val stateAccess: SearchPreferencesStateAccess,
) {
    @Volatile internal var lastModelRefreshAtMillis = 0L
    @Volatile internal var lastModelRefreshProviderIds: Set<AiSearchLlmProviderId> = emptySet()

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

    fun setColorVisualizerEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setColorVisualizerEnabled(enabled)
            updateFeatureState { it.copy(colorVisualizerEnabled = enabled) }
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

    fun setWorldClockEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setWorldClockEnabled(enabled)
            updateFeatureState { it.copy(worldClockEnabled = enabled) }
            if (!enabled) {
                updateResultsState { it.copy(worldClockState = WorldClockState()) }
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

    fun setWeatherEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setWeatherEnabled(enabled)
            updateFeatureState { it.copy(weatherEnabled = enabled) }
            if (!enabled) updateResultsState { it.copy(weatherState = WeatherState()) }
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

    fun setIncludeNonLaunchableAppsInSearch(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setIncludeNonLaunchableAppsInSearch,
            stateUpdater = {
                updateConfigState { state -> state.copy(includeNonLaunchableAppsInSearch = it) }
                refreshAppSuggestions()
                refreshApps()
            },
        )
    }

    fun setIncludeArchivedAppsInSearch(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setIncludeArchivedAppsInSearch,
            stateUpdater = {
                updateConfigState { state -> state.copy(includeArchivedAppsInSearch = it) }
                refreshApps()
            },
        )
    }

    fun setShowInRecents(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setShowInRecents,
            stateUpdater = { updateConfigState { state -> state.copy(showInRecents = it) } },
        )
    }

    fun setNotificationDotsEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setNotificationDotsEnabled,
            stateUpdater = { updateConfigState { state -> state.copy(notificationDotsEnabled = it) } },
        )
    }

    fun setShowAllAppsButton(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setShowAllAppsButton,
            stateUpdater = {
                stateAccess.showAllAppsButton = it
                updateUiState { state -> state.copy(showAllAppsButton = it) }
                stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            },
        )
    }

    fun setSelectedAppSuggestionTab(tab: AppSuggestionTabType) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setSelectedAppSuggestionTab(tab)
            updateConfigState { it.copy(selectedAppSuggestionTab = tab) }
        }
    }

    fun setAppSuggestionTabEnabled(
        tab: AppSuggestionTabType,
        enabled: Boolean,
    ) {
        if (tab == AppSuggestionTabType.PINNED && !enabled) return
        scope.launch(Dispatchers.IO) {
            val currentTabs = userPreferences.getEnabledAppSuggestionTabs()
            val updatedTabs =
                currentTabs.toMutableSet().apply {
                    if (enabled) {
                        add(tab)
                    } else {
                        remove(tab)
                    }
                    if (
                        AppSuggestionTabType.RECENTS !in this &&
                            AppSuggestionTabType.MOST_USED !in this
                    ) {
                        add(AppSuggestionTabType.PINNED)
                    }
                }
            userPreferences.setEnabledAppSuggestionTabs(updatedTabs)
            updateConfigState { state ->
                val selectedTab =
                    state.selectedAppSuggestionTab.takeIf { it in updatedTabs }
                        ?: updatedTabs.firstOrNull()
                        ?: AppSuggestionTabType.RECENTS
                state.copy(
                    enabledAppSuggestionTabs = updatedTabs,
                    selectedAppSuggestionTab = selectedTab,
                )
            }
        }
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

    fun setAppIconSizeStep(step: Int) {
        scope.launch(Dispatchers.IO) {
            val normalized =
                step.coerceIn(
                    UiPreferences.MIN_APP_ICON_SIZE_STEP,
                    UiPreferences.MAX_APP_ICON_SIZE_STEP,
                )
            userPreferences.setAppIconSizeStep(normalized)
            stateAccess.appIconSizeStep = normalized
            updateConfigState { state -> state.copy(appIconSizeStep = normalized) }
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

    fun setRecentQueriesDisplayCount(count: Int) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setRecentQueriesDisplayCount(count)
            updateFeatureState { it.copy(recentQueriesDisplayCount = count) }
        }
    }

    fun setAppResultRowCount(rowCount: Int) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setAppResultRowCount(rowCount)
            updateFeatureState { it.copy(appResultRowCount = rowCount) }
        }
    }

    fun setFuzzySearchEnabled(enabled: Boolean) {
        if (isLowRamDevice(applicationProvider())) return

        // Persist before scheduling result work so leaving Settings cannot cancel the write.
        userPreferences.setFuzzySearchEnabled(enabled)
        updateFeatureState {
            it.copy(
                fuzzySearchEnabled = enabled,
                fuzzySearchAvailable = true,
            )
        }

        scope.launch(Dispatchers.IO) {
            refreshAppSuggestions()
            secondarySearchOrchestrator.resetNoResultTracking()
            rerunSecondarySearchIfNeeded()
        }
    }

    fun setSecondaryRankingSignal(signal: com.tk.quicksearch.search.models.SecondaryRankingSignal) {
        userPreferences.setSecondaryRankingSignal(signal)
        updateFeatureState { it.copy(secondaryRankingSignal = signal) }

        scope.launch(Dispatchers.IO) {
            refreshAppSuggestions()
            secondarySearchOrchestrator.resetNoResultTracking()
            rerunSecondarySearchIfNeeded()
        }
    }

    fun setTopMatchesEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setTopMatchesEnabled(enabled)
            updateFeatureState { it.copy(topMatchesEnabled = enabled) }
        }
    }

    fun setTopMatchesLimit(limit: Int) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setTopMatchesLimit(limit)
            updateFeatureState { it.copy(topMatchesLimit = userPreferences.getTopMatchesLimit()) }
        }
    }

    fun setTopMatchesSectionOrder(order: List<SearchSection>) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setTopMatchesSectionOrder(order)
            updateFeatureState {
                it.copy(topMatchesSectionOrder = userPreferences.getTopMatchesSectionOrder())
            }
        }
    }

    fun setHomePinnedSectionOrder(order: List<SearchSection>) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setHomePinnedSectionOrder(order)
            updateFeatureState {
                it.copy(homePinnedSectionOrder = userPreferences.getHomePinnedSectionOrder())
            }
        }
    }

    fun setPinnedAppShortcutsInAppGridEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setPinnedAppShortcutsInAppGridEnabled(enabled)
            updateFeatureState { it.copy(pinnedAppShortcutsInAppGrid = enabled) }
        }
    }

    fun setTopMatchesSectionEnabled(section: SearchSection, enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            val updated =
                    if (enabled) {
                        userPreferences.getDisabledTopMatchesSections() - section
                    } else {
                        userPreferences.getDisabledTopMatchesSections() + section
                    }
            userPreferences.setDisabledTopMatchesSections(updated)
            updateFeatureState {
                it.copy(disabledTopMatchesSections = userPreferences.getDisabledTopMatchesSections())
            }
        }
    }

    fun setShowTodayEvents(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setShowTodayEvents(enabled)
            updateFeatureState { it.copy(showTodayEvents = enabled) }
            refreshCalendarEvents()
        }
    }

    fun archiveTodayCalendarEvent(eventId: Long) {
        scope.launch(Dispatchers.IO) {
            userPreferences.archiveTodayCalendarEvent(eventId)
            updateResultsState { state ->
                state.copy(todayCalendarEvents = state.todayCalendarEvents.filterNot { it.eventId == eventId })
            }
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

    fun setUseSystemFont(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.useSystemFont == enabled) return@launch
            userPreferences.setUseSystemFont(enabled)
            stateAccess.useSystemFont = enabled
            updateConfigState { it.copy(useSystemFont = enabled) }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setHomeTextColorOverride(color: HomeTextColor) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.homeTextColorOverride == color) return@launch
            userPreferences.setHomeTextColorOverride(color)
            stateAccess.homeTextColorOverride = color
            updateConfigState { it.copy(homeTextColorOverride = color) }
        }
    }

    fun resetHomeTextColorForNewWallpaper() {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.homeTextColorOverride == null) return@launch
            userPreferences.clearHomeTextColorOverride()
            stateAccess.homeTextColorOverride = null
            updateConfigState { it.copy(homeTextColorOverride = null) }
        }
    }

    fun setBackgroundSource(source: BackgroundSource) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.backgroundSource == source) return@launch
            userPreferences.setBackgroundSource(source)
            userPreferences.clearHomeTextColorOverride()
            stateAccess.backgroundSource = source
            stateAccess.homeTextColorOverride = null
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
                    homeTextColorOverride = null,
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
            userPreferences.clearHomeTextColorOverride()
            stateAccess.customImageUri = normalized
            stateAccess.homeTextColorOverride = null
            updateConfigState { it.copy(customImageUri = normalized, homeTextColorOverride = null) }
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
            val shouldSwitchFromCustomToWallpaper =
                enabled && stateAccess.backgroundSource == BackgroundSource.CUSTOM_IMAGE
            if (shouldSwitchFromCustomToWallpaper) {
                userPreferences.setBackgroundSource(BackgroundSource.SYSTEM_WALLPAPER)
                stateAccess.backgroundSource = BackgroundSource.SYSTEM_WALLPAPER
            }
            updateConfigState {
                it.copy(
                    deviceThemeEnabled = enabled,
                    backgroundSource =
                        if (shouldSwitchFromCustomToWallpaper) {
                            BackgroundSource.SYSTEM_WALLPAPER
                        } else {
                            it.backgroundSource
                        },
                    showWallpaperBackground =
                        if (shouldSwitchFromCustomToWallpaper) {
                            true
                        } else {
                            it.showWallpaperBackground
                        },
                )
            }
            stateAccess.saveStartupSurfaceSnapshotAsync(
                forcePreviewRefresh = shouldSwitchFromCustomToWallpaper,
                allowDuringQuery = true,
            )
        }
    }

    fun setAmoledThemeEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.amoledThemeEnabled == enabled) return@launch
            userPreferences.setAmoledThemeEnabled(enabled)
            stateAccess.amoledThemeEnabled = enabled
            updateConfigState { it.copy(amoledThemeEnabled = enabled) }
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

}
