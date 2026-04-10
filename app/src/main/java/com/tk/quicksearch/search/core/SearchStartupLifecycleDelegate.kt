package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.StartupPreferencesFacade
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.apps.prefetchAppIcons
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.util.PackageConstants
import com.tk.quicksearch.shared.util.WallpaperUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal interface SearchStartupLifecycleStateAccess {
    var pendingNavigationClear: Boolean
    var isStartupComplete: Boolean
    var resumeNeedsStaticDataRefresh: Boolean
    var lastBrowserTargetRefreshMs: Long
    var wallpaperAvailable: Boolean
    var directDialEnabled: Boolean
}

internal data class SearchStartupPreferencesSnapshot(
    val oneHandedMode: Boolean,
    val bottomSearchBarEnabled: Boolean,
    val topResultIndicatorEnabled: Boolean,
    val wallpaperAccentEnabled: Boolean,
    val openKeyboardOnLaunch: Boolean,
    val clearQueryOnLaunch: Boolean,
    val autoCloseOverlay: Boolean,
    val backgroundSource: BackgroundSource,
    val wallpaperBackgroundAlpha: Float,
    val wallpaperBlurRadius: Float,
    val appTheme: AppTheme,
    val overlayThemeIntensity: Float,
    val appIconShape: AppIconShape,
    val launcherAppIcon: LauncherAppIcon,
    val themedIconsEnabled: Boolean,
    val deviceThemeEnabled: Boolean,
    val maskUnsupportedIconPackIcons: Boolean,
    val customImageUri: String?,
)

internal data class SearchLoadedPreferencesSnapshot(
    val enabledFileTypes: Set<FileType>,
    val oneHandedMode: Boolean,
    val bottomSearchBarEnabled: Boolean,
    val topResultIndicatorEnabled: Boolean,
    val openKeyboardOnLaunch: Boolean,
    val clearQueryOnLaunch: Boolean,
    val autoCloseOverlay: Boolean,
    val overlayModeEnabled: Boolean,
    val appSuggestionsEnabled: Boolean,
    val showAppLabels: Boolean,
    val phoneAppGridColumns: Int,
    val appIconShape: AppIconShape,
    val launcherAppIcon: LauncherAppIcon,
    val themedIconsEnabled: Boolean,
    val deviceThemeEnabled: Boolean,
    val maskUnsupportedIconPackIcons: Boolean,
    val backgroundSource: BackgroundSource,
    val wallpaperBackgroundAlpha: Float,
    val wallpaperBlurRadius: Float,
    val appTheme: AppTheme,
    val overlayThemeIntensity: Float,
    val fontScaleMultiplier: Float,
    val customImageUri: String?,
    val showFolders: Boolean,
    val showSystemFiles: Boolean,
    val folderWhitelistPatterns: Set<String>,
    val folderBlacklistPatterns: Set<String>,
    val excludedFileExtensions: Set<String>,
    val amazonDomain: String?,
    val directDialEnabled: Boolean,
    val assistantLaunchVoiceModeEnabled: Boolean,
)

internal class SearchStartupLifecycleDelegate(
    private val scope: CoroutineScope,
    private val applicationProvider: () -> android.app.Application,
    private val repository: AppsRepository,
    private val userPreferences: UserAppPreferences,
    private val handlersProvider: () -> SearchHandlerContainer,
    private val resultsStateProvider: () -> SearchResultsState,
    private val permissionStateProvider: () -> SearchPermissionState,
    private val configStateProvider: () -> SearchUiConfigState,
    private val stateAccess: SearchStartupLifecycleStateAccess,
    private val getStartupConfig: () -> StartupPreferencesFacade.StartupConfig?,
    private val setStartupConfig: (StartupPreferencesFacade.StartupConfig?) -> Unit,
    private val setPrefCache: (SearchPreferenceCache) -> Unit,
    private val readStartupPreferencesSnapshot: () -> SearchStartupPreferencesSnapshot,
    private val readLoadedPreferencesSnapshot: () -> SearchLoadedPreferencesSnapshot,
    private val updatePermissionState: ((SearchPermissionState) -> SearchPermissionState) -> Unit,
    private val updateFeatureState: ((SearchFeatureState) -> SearchFeatureState) -> Unit,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
    private val updateConfigState: ((SearchUiConfigState) -> SearchUiConfigState) -> Unit,
    private val applyVisibilityStates: (SearchUiState) -> SearchUiState,
    private val hasContactPermission: () -> Boolean,
    private val hasFilePermission: () -> Boolean,
    private val hasCalendarPermission: () -> Boolean,
    private val clearQuery: () -> Unit,
    private val refreshApps: () -> Unit,
    private val refreshAppSuggestions: () -> Unit,
    private val refreshSettingsState: () -> Unit,
    private val refreshAppShortcutsState: () -> Unit,
    private val refreshDerivedState: (Long?, Boolean?) -> Unit,
    private val saveStartupSurfaceSnapshotAsync: (Boolean, Boolean) -> Unit,
    private val applyPreferenceCacheToLegacyVars: () -> Unit,
    private val applyLauncherIconSelection: () -> Unit,
    private val refreshRecentItems: () -> Unit,
    private val getGridItemCount: () -> Int,
    private val selectSuggestedApps: (List<AppInfo>, Int, Boolean) -> List<AppInfo>,
    private val shouldShowSearchBarWelcome: () -> Boolean,
    private val loadApps: () -> Unit,
    private val loadSettingsShortcuts: () -> Unit,
    private val loadAppSettings: () -> Unit,
    private val loadAppShortcuts: () -> Unit,
    private val startupDispatcher: CoroutineDispatcher,
    private val loadPinnedAndExcludedCalendarEvents: () -> Unit,
    private val setDirectDialEnabled: (Boolean, Boolean) -> Unit,
) {
    private val pinningHandler get() = handlersProvider().pinningHandler
    private val searchEngineManager get() = handlersProvider().searchEngineManager
    private val secondarySearchOrchestrator get() = handlersProvider().secondarySearchOrchestrator
    private val sectionManager get() = handlersProvider().sectionManager

    private val aliasHandler get() = handlersProvider().aliasHandler
    private val appSearchManager get() = handlersProvider().appSearchManager
    private val appShortcutSearchHandler get() = handlersProvider().appShortcutSearchHandler
    private val directSearchHandler get() = handlersProvider().directSearchHandler
    private val iconPackHandler get() = handlersProvider().iconPackHandler
    private val messagingHandler get() = handlersProvider().messagingHandler
    private val releaseNotesHandler get() = handlersProvider().releaseNotesHandler
    private val settingsSearchHandler get() = handlersProvider().settingsSearchHandler
    private val webSuggestionHandler get() = handlersProvider().webSuggestionHandler

    fun setWallpaperAvailable(available: Boolean) {
        if (stateAccess.wallpaperAvailable != available) {
            stateAccess.wallpaperAvailable = available
            updatePermissionState { it.copy(wallpaperAvailable = available) }
        }
    }

    fun markStartupCoreSurfaceReady() {
        if (!configStateProvider().isStartupCoreSurfaceReady) {
            updateConfigState { it.copy(isStartupCoreSurfaceReady = true) }
        }
    }

    fun handleOnStop() {
        val shouldRetainDirectOrGeminiQueryOnStop = shouldRetainDirectOrGeminiQueryOnStop()
        val shouldClearQueryOnStop = configStateProvider().clearQueryOnLaunch
        if (shouldRetainDirectOrGeminiQueryOnStop) {
            updateConfigState { it.copy(selectRetainedQuery = true) }
        } else if (shouldClearQueryOnStop) {
            clearQuery()
        } else if (stateAccess.pendingNavigationClear && resultsStateProvider().query.isNotEmpty()) {
            updateConfigState { it.copy(selectRetainedQuery = true) }
        }
        if (stateAccess.pendingNavigationClear) {
            stateAccess.pendingNavigationClear = false
        }
    }

    fun handleOnResume() {
        val startupComplete = stateAccess.isStartupComplete

        val previousUsage = permissionStateProvider().hasUsagePermission
        val latestUsage = repository.hasUsageAccess()
        val usageChanged = previousUsage != latestUsage
        if (usageChanged) {
            updatePermissionState { it.copy(hasUsagePermission = latestUsage) }
            if (startupComplete) {
                if (latestUsage) refreshApps() else refreshAppSuggestions()
            }
        }

        val optionalPermissionsChanged = run {
            val before = permissionStateProvider()
            handleOptionalPermissionChangeInternal(allowAppRefresh = startupComplete)
            permissionStateProvider() != before
        }

        if (startupComplete && optionalPermissionsChanged) {
            pinningHandler.loadPinnedContactsAndFiles()
            pinningHandler.loadExcludedContactsAndFiles()
            loadPinnedAndExcludedCalendarEvents()
        }

        if (startupComplete && stateAccess.resumeNeedsStaticDataRefresh) {
            stateAccess.resumeNeedsStaticDataRefresh = false
            refreshSettingsState()
            refreshAppShortcutsState()
        }

        if (startupComplete) {
            loadPinnedAndExcludedCalendarEvents()
        }

        val now = System.currentTimeMillis()
        if (startupComplete && now - stateAccess.lastBrowserTargetRefreshMs >= BROWSER_REFRESH_INTERVAL_MS) {
            stateAccess.lastBrowserTargetRefreshMs = now
            scope.launch(Dispatchers.IO) {
                searchEngineManager.ensureInitialized()
                searchEngineManager.refreshBrowserTargets()
            }
        }
    }

    fun handleOptionalPermissionChange() {
        handleOptionalPermissionChangeInternal(allowAppRefresh = true)
    }

    fun refreshPermissionSnapshotAtLaunch() {
        scope.launch(Dispatchers.Default) {
            val latestUsagePermission = repository.hasUsageAccess()
            if (permissionStateProvider().hasUsagePermission != latestUsagePermission) {
                updatePermissionState { it.copy(hasUsagePermission = latestUsagePermission) }
            }
            refreshOptionalPermissions()
        }
    }

    fun refreshOptionalPermissions(): Boolean {
        val hasContacts = hasContactPermission()
        val hasFiles = hasFilePermission()
        val hasCalendar = hasCalendarPermission()
        val hasCall = PermissionHelper.checkCallPermission(applicationProvider())
        val hasWallpaper = WallpaperUtils.hasWallpaperAccessPermission(applicationProvider())
        val previousState = permissionStateProvider()
        val changed =
            previousState.hasContactPermission != hasContacts ||
                previousState.hasFilePermission != hasFiles ||
                previousState.hasCalendarPermission != hasCalendar ||
                previousState.hasCallPermission != hasCall ||
                previousState.hasWallpaperPermission != hasWallpaper ||
                previousState.wallpaperAvailable != stateAccess.wallpaperAvailable

        if (changed) {
            if (hasCall && !stateAccess.directDialEnabled && !userPreferences.isDirectDialManuallyDisabled()) {
                setDirectDialEnabled(true, false)
            } else if (!hasCall && stateAccess.directDialEnabled) {
                setDirectDialEnabled(false, false)
            }

            updatePermissionState { state ->
                state.copy(
                    hasContactPermission = hasContacts,
                    hasFilePermission = hasFiles,
                    hasCalendarPermission = hasCalendar,
                    hasCallPermission = hasCall,
                    hasWallpaperPermission = hasWallpaper,
                    wallpaperAvailable = stateAccess.wallpaperAvailable,
                )
            }
            updateFeatureState { it.copy(directDialEnabled = stateAccess.directDialEnabled) }
            updateResultsState { state ->
                state.copy(
                    contactResults = if (hasContacts) state.contactResults else emptyList(),
                    fileResults = if (hasFiles) state.fileResults else emptyList(),
                    calendarEvents = if (hasCalendar) state.calendarEvents else emptyList(),
                    pinnedCalendarEvents = if (hasCalendar) state.pinnedCalendarEvents else emptyList(),
                    excludedCalendarEvents = if (hasCalendar) state.excludedCalendarEvents else emptyList(),
                    todayCalendarEvents = if (hasCalendar) state.todayCalendarEvents else emptyList(),
                )
            }

            if (hasCalendar) {
                loadPinnedAndExcludedCalendarEvents()
            }

            sectionManager.refreshDisabledSections()
        }
        return changed
    }

    fun launchDeferredInitialization() {
        scope.launch(Dispatchers.IO) {
            refreshAppsUsageAndPermissions()

            val packageNames = appSearchManager.cachedApps.map { it.packageName }.toSet()
            val messagingInfo = getMessagingAppInfo(packageNames)

            searchEngineManager.ensureInitialized()
            val shortcutsState = aliasHandler.getInitialState()

            updateFeatureState { state ->
                state.copy(
                    searchTargetsOrder = searchEngineManager.searchTargetsOrder,
                    disabledSearchTargetIds = searchEngineManager.disabledSearchTargetIds,
                    shortcutsEnabled = shortcutsState.shortcutsEnabled,
                    shortcutCodes = shortcutsState.shortcutCodes,
                    shortcutEnabled = shortcutsState.shortcutEnabled,
                    disabledSections = sectionManager.disabledSections,
                    isSearchEngineCompactMode = searchEngineManager.isSearchEngineCompactMode,
                    searchEngineCompactRowCount = searchEngineManager.searchEngineCompactRowCount,
                    isSearchEngineAliasSuffixEnabled = userPreferences.isSearchEngineAliasSuffixEnabled(),
                    isAliasTriggerAfterSpaceEnabled = userPreferences.isAliasTriggerAfterSpaceEnabled(),
                    webSuggestionsEnabled = webSuggestionHandler.isEnabled,
                    calculatorEnabled = userPreferences.isCalculatorEnabled(),
                    unitConverterEnabled = userPreferences.isUnitConverterEnabled(),
                    dateCalculatorEnabled = userPreferences.isDateCalculatorEnabled(),
                    currencyConverterEnabled = userPreferences.isCurrencyConverterEnabled(),
                    wordClockEnabled = userPreferences.isWordClockEnabled(),
                    dictionaryEnabled = userPreferences.isDictionaryEnabled(),
                    customTools = userPreferences.getCustomTools(),
                    disabledCustomToolIds = userPreferences.getDisabledCustomTools(),
                    hasGeminiApiKey = !directSearchHandler.getGeminiApiKey().isNullOrBlank(),
                    geminiApiKeyLast4 = directSearchHandler.getGeminiApiKey()?.takeLast(4),
                    personalContext = directSearchHandler.getPersonalContext(),
                    geminiModel = directSearchHandler.getGeminiModel(),
                    geminiGroundingEnabled = directSearchHandler.isGeminiGroundingEnabled(),
                    availableGeminiModels = directSearchHandler.getAvailableGeminiModels(),
                )
            }
            updateConfigState { state ->
                state.copy(
                    showSearchEngineOnboarding =
                        searchEngineManager.isSearchEngineCompactMode &&
                            !userPreferences.hasSeenSearchEngineOnboarding(),
                    showSearchBarWelcomeAnimation = shouldShowSearchBarWelcome(),
                    appSuggestionsEnabled = userPreferences.areAppSuggestionsEnabled(),
                    showAppLabels = userPreferences.shouldShowAppLabels(),
                    phoneAppGridColumns = userPreferences.getPhoneAppGridColumns(),
                    bottomSearchBarEnabled = userPreferences.isBottomSearchBarEnabled(),
                    topResultIndicatorEnabled = userPreferences.isTopResultIndicatorEnabled(),
                    openKeyboardOnLaunch = userPreferences.isOpenKeyboardOnLaunchEnabled(),
                    clearQueryOnLaunch = userPreferences.isClearQueryOnLaunchEnabled(),
                    autoCloseOverlay = userPreferences.isAutoCloseOverlayEnabled(),
                )
            }
            updatePermissionState { state ->
                state.copy(
                    messagingApp = messagingInfo.messagingApp,
                    callingApp = messagingInfo.callingApp,
                    isWhatsAppInstalled = messagingInfo.isWhatsAppInstalled,
                    isTelegramInstalled = messagingInfo.isTelegramInstalled,
                    isSignalInstalled = messagingInfo.isSignalInstalled,
                    isGoogleMeetInstalled = messagingInfo.isGoogleMeetInstalled,
                )
            }
            updateFeatureState { state ->
                state.copy(disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds())
            }

            if (!directSearchHandler.getGeminiApiKey().isNullOrBlank()) {
                launch(Dispatchers.IO) {
                    delay(DEFERRED_DIRECT_SEARCH_MODELS_DELAY_MS)
                    val models = directSearchHandler.refreshAvailableGeminiModels()
                    updateFeatureState { state -> state.copy(availableGeminiModels = models) }
                }
            }

            launch(Dispatchers.IO) {
                delay(DEFERRED_APP_REFRESH_DELAY_MS)
                loadApps()
            }

            launch(Dispatchers.IO) { iconPackHandler.refreshIconPacks() }

            launch(startupDispatcher) {
                pinningHandler.loadPinnedContactsAndFiles()
                pinningHandler.loadExcludedContactsAndFiles()
                loadPinnedAndExcludedCalendarEvents()

                val pinnedAppShortcutsState = appShortcutSearchHandler.getPinnedAndExcludedOnly()
                val iconOverrides = userPreferences.getAllAppShortcutIconOverrides()
                updateUiState { state ->
                    state.copy(
                        allAppShortcuts =
                            applyAppShortcutIconOverrides(
                                appShortcutSearchHandler.getAvailableShortcuts(),
                                iconOverrides,
                            ),
                        disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds(),
                        pinnedAppShortcuts =
                            applyAppShortcutIconOverrides(pinnedAppShortcutsState.pinned, iconOverrides),
                        excludedAppShortcuts =
                            applyAppShortcutIconOverrides(pinnedAppShortcutsState.excluded, iconOverrides),
                    )
                }

                val pinnedSettingsState = settingsSearchHandler.getPinnedAndExcludedOnly()
                updateResultsState { state ->
                    state.copy(
                        pinnedSettings = pinnedSettingsState.pinned,
                        excludedSettings = pinnedSettingsState.excluded,
                    )
                }
            }

            launch(startupDispatcher) { loadSettingsShortcuts() }
            launch(startupDispatcher) { loadAppSettings() }
            launch(startupDispatcher) { loadAppShortcuts() }

            launch(Dispatchers.IO) {
                delay(DEFERRED_RELEASE_NOTES_DELAY_MS)
                releaseNotesHandler.checkForReleaseNotes()
            }

            withContext(Dispatchers.Main) {
                stateAccess.isStartupComplete = true
                updateUiState {
                    applyVisibilityStates(
                        it.copy(
                            startupPhase = StartupPhase.COMPLETE,
                        ),
                    )
                }
            }
            withContext(Dispatchers.Default) { refreshDerivedState(null, null) }
            saveStartupSurfaceSnapshotAsync(true, false)
        }
    }

    suspend fun loadCacheAndMinimalPrefs() {
        val startupConfig = userPreferences.loadStartupConfig()
        setPrefCache(
            SearchPreferenceCache.from(
                config = startupConfig,
                assistantLaunchVoiceModeEnabled = userPreferences.isAssistantLaunchVoiceModeEnabled(),
            ),
        )
        applyPreferenceCacheToLegacyVars()
        val startupSnapshot = readStartupPreferencesSnapshot()

        val cachedAppsList = runCatching { repository.loadCachedApps() }.getOrNull()
        val hasUsagePermission = repository.hasUsageAccess()
        val hasContactPermission = hasContactPermission()
        val hasFilePermission = hasFilePermission()
        val hasCalendarPermission = hasCalendarPermission()
        val hasCallPermission = PermissionHelper.checkCallPermission(applicationProvider())
        val hasWallpaperPermission = WallpaperUtils.hasWallpaperAccessPermission(applicationProvider())
        val disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds()

        withContext(Dispatchers.Main) {
            updateConfigState {
                it.copy(
                    oneHandedMode = startupSnapshot.oneHandedMode,
                    bottomSearchBarEnabled = startupSnapshot.bottomSearchBarEnabled,
                    topResultIndicatorEnabled = startupSnapshot.topResultIndicatorEnabled,
                    wallpaperAccentEnabled = startupSnapshot.wallpaperAccentEnabled,
                    openKeyboardOnLaunch = startupSnapshot.openKeyboardOnLaunch,
                    clearQueryOnLaunch = startupSnapshot.clearQueryOnLaunch,
                    autoCloseOverlay = startupSnapshot.autoCloseOverlay,
                    showWallpaperBackground =
                        startupSnapshot.backgroundSource != BackgroundSource.THEME,
                    wallpaperBackgroundAlpha = startupSnapshot.wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = startupSnapshot.wallpaperBlurRadius,
                    appTheme = startupSnapshot.appTheme,
                    overlayThemeIntensity = startupSnapshot.overlayThemeIntensity,
                    backgroundSource = startupSnapshot.backgroundSource,
                    customImageUri = startupSnapshot.customImageUri,
                    appIconShape = startupSnapshot.appIconShape,
                    launcherAppIcon = startupSnapshot.launcherAppIcon,
                    themedIconsEnabled = startupSnapshot.themedIconsEnabled,
                    deviceThemeEnabled = startupSnapshot.deviceThemeEnabled,
                    maskUnsupportedIconPackIcons = startupSnapshot.maskUnsupportedIconPackIcons,
                    isInitializing = true,
                )
            }
            updatePermissionState {
                it.copy(
                    hasUsagePermission = hasUsagePermission,
                    hasContactPermission = hasContactPermission,
                    hasFilePermission = hasFilePermission,
                    hasCalendarPermission = hasCalendarPermission,
                    hasCallPermission = hasCallPermission,
                    hasWallpaperPermission = hasWallpaperPermission,
                )
            }
            updateFeatureState { it.copy(disabledAppShortcutIds = disabledAppShortcutIds) }

            if (!cachedAppsList.isNullOrEmpty()) {
                initializeWithCacheMinimal(cachedAppsList, hasUsagePermission)
            }
        }

        setStartupConfig(startupConfig)
        applyLauncherIconSelection()

        if (!cachedAppsList.isNullOrEmpty()) {
            scope.launch(Dispatchers.IO) {
                if (userPreferences.areAppSuggestionsEnabled()) {
                    val visibleApps =
                        selectSuggestedApps(cachedAppsList, getGridItemCount(), hasUsagePermission)
                    val iconPack = userPreferences.getSelectedIconPackPackage()
                    prefetchAppIcons(
                        context = applicationProvider(),
                        packageNames = visibleApps.map { it.packageName },
                        iconPackPackage = iconPack,
                    )
                }
            }
        }
    }

    suspend fun loadRemainingStartupPreferences(applyStartupPreferences: (StartupPreferencesFacade.StartupPreferences) -> Unit) {
        val startupPrefs =
            getStartupConfig()?.startupPreferences
                ?: userPreferences.getStartupPreferences()

        withContext(Dispatchers.Main) {
            applyStartupPreferences(startupPrefs)
            appSearchManager.setSortAppsByUsage(true)
        }

        val lastUpdated =
            getStartupConfig()?.cachedAppsLastUpdate
                ?: repository.cacheLastUpdatedMillis()
        withContext(Dispatchers.Default) { refreshDerivedState(lastUpdated, false) }
        withContext(Dispatchers.Main) { updateConfigState { it.copy(isInitializing = false) } }
    }

    fun applyStartupPreferences(prefs: StartupPreferencesFacade.StartupPreferences) {
        setPrefCache(
            SearchPreferenceCache.from(
                prefs = prefs,
                assistantLaunchVoiceModeEnabled = userPreferences.isAssistantLaunchVoiceModeEnabled(),
            ),
        )
        applyPreferenceCacheToLegacyVars()
        val snapshot = readLoadedPreferencesSnapshot()

        updateConfigState {
            it.copy(
                enabledFileTypes = snapshot.enabledFileTypes,
                oneHandedMode = snapshot.oneHandedMode,
                bottomSearchBarEnabled = snapshot.bottomSearchBarEnabled,
                topResultIndicatorEnabled = snapshot.topResultIndicatorEnabled,
                openKeyboardOnLaunch = snapshot.openKeyboardOnLaunch,
                clearQueryOnLaunch = snapshot.clearQueryOnLaunch,
                autoCloseOverlay = snapshot.autoCloseOverlay,
                overlayModeEnabled = snapshot.overlayModeEnabled,
                appSuggestionsEnabled = snapshot.appSuggestionsEnabled,
                showAppLabels = snapshot.showAppLabels,
                phoneAppGridColumns = snapshot.phoneAppGridColumns,
                appIconShape = snapshot.appIconShape,
                launcherAppIcon = snapshot.launcherAppIcon,
                themedIconsEnabled = snapshot.themedIconsEnabled,
                deviceThemeEnabled = snapshot.deviceThemeEnabled,
                maskUnsupportedIconPackIcons = snapshot.maskUnsupportedIconPackIcons,
                showWallpaperBackground = snapshot.backgroundSource != BackgroundSource.THEME,
                wallpaperBackgroundAlpha = snapshot.wallpaperBackgroundAlpha,
                wallpaperBlurRadius = snapshot.wallpaperBlurRadius,
                appTheme = snapshot.appTheme,
                overlayThemeIntensity = snapshot.overlayThemeIntensity,
                fontScaleMultiplier = snapshot.fontScaleMultiplier,
                backgroundSource = snapshot.backgroundSource,
                customImageUri = snapshot.customImageUri,
                showFolders = snapshot.showFolders,
                showSystemFiles = snapshot.showSystemFiles,
                folderWhitelistPatterns = snapshot.folderWhitelistPatterns,
                folderBlacklistPatterns = snapshot.folderBlacklistPatterns,
                excludedFileExtensions = snapshot.excludedFileExtensions,
                hasSeenOverlayAssistantTip = userPreferences.hasSeenOverlayAssistantTip(),
            )
        }
        updateFeatureState {
            it.copy(
                amazonDomain = snapshot.amazonDomain,
                directDialEnabled = snapshot.directDialEnabled,
                assistantLaunchVoiceModeEnabled = snapshot.assistantLaunchVoiceModeEnabled,
                disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds(),
                recentQueriesEnabled = prefs.searchHistoryEnabled,
                webSuggestionsCount = userPreferences.getWebSuggestionsCount(),
                shouldShowUsagePermissionBanner = userPreferences.shouldShowUsagePermissionBanner(),
                hasDismissedSearchHistoryTip = userPreferences.hasDismissedSearchHistoryTip(),
            )
        }

        if (!prefs.searchHistoryEnabled) {
            userPreferences.clearRecentQueries()
        }

        applyLauncherIconSelection()
        refreshRecentItems()
        saveStartupSurfaceSnapshotAsync(false, false)
    }

    fun onSettingsImported(
        applyStartupPreferences: (StartupPreferencesFacade.StartupPreferences) -> Unit,
        handleOnResume: () -> Unit,
        onAfterSettingsImportMain: () -> Unit = {},
    ) {
        scope.launch(Dispatchers.IO) {
            userPreferences.reloadNicknameCaches()
            val startupPrefs = userPreferences.getStartupPreferences()

            searchEngineManager.reloadFromPreferences()
            val shortcutsState = aliasHandler.reloadFromPreferences()
            directSearchHandler.reloadFromPreferences()
            val webSuggestionsEnabled = webSuggestionHandler.reloadFromPreferences()

            val geminiApiKey = directSearchHandler.getGeminiApiKey()
            val personalContext = directSearchHandler.getPersonalContext()
            val geminiModel = directSearchHandler.getGeminiModel()
            val geminiGroundingEnabled = directSearchHandler.isGeminiGroundingEnabled()
            val availableGeminiModels = directSearchHandler.getAvailableGeminiModels()
            val hasGeminiApiKey = !geminiApiKey.isNullOrBlank()

            withContext(Dispatchers.Main) {
                applyStartupPreferences(startupPrefs)
                updateFeatureState { state ->
                    state.copy(
                        searchTargetsOrder = searchEngineManager.searchTargetsOrder,
                        disabledSearchTargetIds = searchEngineManager.disabledSearchTargetIds,
                        isSearchEngineCompactMode = searchEngineManager.isSearchEngineCompactMode,
                        searchEngineCompactRowCount = searchEngineManager.searchEngineCompactRowCount,
                        isSearchEngineAliasSuffixEnabled = userPreferences.isSearchEngineAliasSuffixEnabled(),
                        isAliasTriggerAfterSpaceEnabled = userPreferences.isAliasTriggerAfterSpaceEnabled(),
                        shortcutsEnabled = shortcutsState.shortcutsEnabled,
                        shortcutCodes = shortcutsState.shortcutCodes,
                        shortcutEnabled = shortcutsState.shortcutEnabled,
                        webSuggestionsEnabled = webSuggestionsEnabled,
                        calculatorEnabled = userPreferences.isCalculatorEnabled(),
                        unitConverterEnabled = userPreferences.isUnitConverterEnabled(),
                        dateCalculatorEnabled = userPreferences.isDateCalculatorEnabled(),
                        currencyConverterEnabled = userPreferences.isCurrencyConverterEnabled(),
                        wordClockEnabled = userPreferences.isWordClockEnabled(),
                        dictionaryEnabled = userPreferences.isDictionaryEnabled(),
                        customTools = userPreferences.getCustomTools(),
                        disabledCustomToolIds = userPreferences.getDisabledCustomTools(),
                        hasGeminiApiKey = hasGeminiApiKey,
                        geminiApiKeyLast4 = geminiApiKey?.takeLast(4),
                        personalContext = personalContext,
                        geminiModel = geminiModel,
                        geminiGroundingEnabled = geminiGroundingEnabled,
                        availableGeminiModels = availableGeminiModels,
                    )
                }
                updateConfigState { state ->
                    state.copy(
                        showSearchEngineOnboarding =
                            searchEngineManager.isSearchEngineCompactMode &&
                                !userPreferences.hasSeenSearchEngineOnboarding(),
                    )
                }
                handleOnResume()
                loadAppSettings()
                updateUiState { applyVisibilityStates(it) }
                onAfterSettingsImportMain()
            }
        }
    }

    private fun handleOptionalPermissionChangeInternal(allowAppRefresh: Boolean) {
        val previousUsagePermission = permissionStateProvider().hasUsagePermission
        val latestUsagePermission = repository.hasUsageAccess()
        val usagePermissionChanged = previousUsagePermission != latestUsagePermission

        if (usagePermissionChanged) {
            updatePermissionState { it.copy(hasUsagePermission = latestUsagePermission) }
            if (allowAppRefresh && latestUsagePermission) {
                refreshApps()
            } else if (allowAppRefresh) {
                refreshAppSuggestions()
            }
        }

        val optionalChanged = refreshOptionalPermissions()
        val query = resultsStateProvider().query
        if ((optionalChanged || usagePermissionChanged) && query.isNotBlank()) {
            secondarySearchOrchestrator.performSecondarySearches(query)
        }
    }

    private fun shouldRetainDirectOrGeminiQueryOnStop(): Boolean {
        val state = resultsStateProvider()
        if (state.query.isBlank()) return false
        return state.DirectSearchState.status != DirectSearchStatus.Idle ||
            state.currencyConverterState.status != CurrencyConverterStatus.Idle ||
            state.wordClockState.status != WordClockStatus.Idle ||
            state.dictionaryState.status != DictionaryStatus.Idle
    }

    private fun refreshAppsUsageAndPermissions() {
        updatePermissionState { it.copy(hasUsagePermission = repository.hasUsageAccess()) }
        refreshOptionalPermissions()
    }

    private fun initializeWithCacheMinimal(
        cachedAppsList: List<AppInfo>,
        hasUsagePermission: Boolean,
    ) {
        val startupSnapshot = readStartupPreferencesSnapshot()
        appSearchManager.initCache(cachedAppsList)
        val lastUpdated = repository.cacheLastUpdatedMillis()
        val suggestionsEnabled = userPreferences.areAppSuggestionsEnabled()
        val startupPrefs = getStartupConfig()?.startupPreferences
        val labelsEnabled = startupPrefs?.showAppLabels ?: userPreferences.shouldShowAppLabels()
        val columnsForPhone =
            startupPrefs?.phoneAppGridColumns ?: userPreferences.getPhoneAppGridColumns()

        updateResultsState {
            it.copy(
                cacheLastUpdatedMillis = lastUpdated,
                recentApps =
                    if (suggestionsEnabled) {
                        selectSuggestedApps(cachedAppsList, getGridItemCount(), hasUsagePermission)
                    } else {
                        emptyList()
                    },
                indexedAppCount = cachedAppsList.size,
            )
        }
        updateConfigState {
            it.copy(
                oneHandedMode = startupSnapshot.oneHandedMode,
                bottomSearchBarEnabled = startupSnapshot.bottomSearchBarEnabled,
                openKeyboardOnLaunch = startupSnapshot.openKeyboardOnLaunch,
                appSuggestionsEnabled = suggestionsEnabled,
                showAppLabels = labelsEnabled,
                phoneAppGridColumns = columnsForPhone,
                isStartupCoreSurfaceReady = true,
            )
        }
        saveStartupSurfaceSnapshotAsync(false, false)
    }

    private fun getMessagingAppInfo(packageNames: Set<String>): MessagingAppInfo {
        val isWhatsAppInstalled =
            if (packageNames.isNotEmpty()) {
                packageNames.contains(PackageConstants.WHATSAPP_PACKAGE)
            } else {
                messagingHandler.isPackageInstalled(PackageConstants.WHATSAPP_PACKAGE)
            }
        val isTelegramInstalled =
            if (packageNames.isNotEmpty()) {
                packageNames.contains(PackageConstants.TELEGRAM_PACKAGE)
            } else {
                messagingHandler.isPackageInstalled(PackageConstants.TELEGRAM_PACKAGE)
            }
        val isSignalInstalled =
            if (packageNames.isNotEmpty()) {
                packageNames.contains(PackageConstants.SIGNAL_PACKAGE)
            } else {
                messagingHandler.isPackageInstalled(PackageConstants.SIGNAL_PACKAGE)
            }
        val isGoogleMeetInstalled =
            if (packageNames.isNotEmpty()) {
                packageNames.contains(PackageConstants.GOOGLE_MEET_PACKAGE)
            } else {
                messagingHandler.isPackageInstalled(PackageConstants.GOOGLE_MEET_PACKAGE)
            }
        val resolvedMessagingApp =
            messagingHandler.updateMessagingAvailability(
                whatsappInstalled = isWhatsAppInstalled,
                telegramInstalled = isTelegramInstalled,
                signalInstalled = isSignalInstalled,
                updateState = false,
            )
        val selectedCallingApp = userPreferences.getCallingApp()
        val resolvedCallingApp =
            resolveCallingApp(
                app = selectedCallingApp,
                isWhatsAppInstalled = isWhatsAppInstalled,
                isTelegramInstalled = isTelegramInstalled,
                isSignalInstalled = isSignalInstalled,
                isGoogleMeetInstalled = isGoogleMeetInstalled,
            )
        if (resolvedCallingApp != selectedCallingApp) {
            userPreferences.setCallingApp(resolvedCallingApp)
        }

        return MessagingAppInfo(
            isWhatsAppInstalled,
            isTelegramInstalled,
            isSignalInstalled,
            resolvedMessagingApp,
            isGoogleMeetInstalled,
            resolvedCallingApp,
        )
    }

    private fun resolveCallingApp(
        app: CallingApp,
        isWhatsAppInstalled: Boolean,
        isTelegramInstalled: Boolean,
        isSignalInstalled: Boolean,
        isGoogleMeetInstalled: Boolean,
    ): CallingApp =
        when (app) {
            CallingApp.WHATSAPP -> if (isWhatsAppInstalled) CallingApp.WHATSAPP else CallingApp.CALL
            CallingApp.TELEGRAM -> if (isTelegramInstalled) CallingApp.TELEGRAM else CallingApp.CALL
            CallingApp.SIGNAL -> if (isSignalInstalled) CallingApp.SIGNAL else CallingApp.CALL
            CallingApp.GOOGLE_MEET ->
                if (isGoogleMeetInstalled) CallingApp.GOOGLE_MEET else CallingApp.CALL
            CallingApp.CALL -> CallingApp.CALL
        }

    private fun applyAppShortcutIconOverrides(
        shortcuts: List<StaticShortcut>,
        overrides: Map<String, String>,
    ): List<StaticShortcut> {
        if (overrides.isEmpty()) return shortcuts
        return shortcuts.map { shortcut ->
            val key = shortcutKey(shortcut)
            val overrideIcon = overrides[key] ?: return@map shortcut
            if (isUserCreatedShortcut(shortcut)) shortcut else shortcut.copy(iconBase64 = overrideIcon)
        }
    }

    private data class MessagingAppInfo(
        val isWhatsAppInstalled: Boolean,
        val isTelegramInstalled: Boolean,
        val isSignalInstalled: Boolean,
        val messagingApp: MessagingApp,
        val isGoogleMeetInstalled: Boolean,
        val callingApp: CallingApp,
    )

    companion object {
        private const val BROWSER_REFRESH_INTERVAL_MS = 5 * 60 * 1_000L
        private const val DEFERRED_APP_REFRESH_DELAY_MS = 2_000L
        private const val DEFERRED_DIRECT_SEARCH_MODELS_DELAY_MS = 3_000L
        private const val DEFERRED_RELEASE_NOTES_DELAY_MS = 3_000L
    }
}
