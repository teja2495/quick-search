package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.StartupPreferencesFacade
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.apps.AppSearchPerformanceLogger
import com.tk.quicksearch.search.apps.prefetchAppIcons
import com.tk.quicksearch.searchEngines.getAppPackageCandidates
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.util.PackageConstants
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.app.startup.StartupTrace
import android.os.SystemClock

internal interface SearchStartupLifecycleStateAccess {
    var pendingNavigationClear: Boolean
    var isStartupComplete: Boolean
    var resumeNeedsStaticDataRefresh: Boolean
    var lastBrowserTargetRefreshMs: Long
    var lastPermissionSnapshotElapsedMs: Long
    var wallpaperAvailable: Boolean
    var directDialEnabled: Boolean
}

internal data class SearchStartupPreferencesSnapshot(
    val oneHandedMode: Boolean,
    val bottomSearchBarEnabled: Boolean,
    val unifiedPinnedItemsEnabled: Boolean,
    val topResultIndicatorEnabled: Boolean,
    val openTopResultUsingKeyboardEnabled: Boolean,
    val accentColorMode: AccentColorMode,
    val customAccentColorArgb: Int,
    val openKeyboardOnLaunch: Boolean,
    val clearQueryOnLaunch: Boolean,
    val autoCloseOverlay: Boolean,
    val backgroundSource: BackgroundSource,
    val wallpaperBackgroundAlpha: Float,
    val wallpaperBlurRadius: Float,
    val appTheme: AppTheme,
    val overlayThemeIntensity: Float,
    val useSystemFont: Boolean,
    val appIconSizeStep: Int,
    val appIconShape: AppIconShape,
    val launcherAppIcon: LauncherAppIcon,
    val themedIconsEnabled: Boolean,
    val deviceThemeEnabled: Boolean,
    val amoledThemeEnabled: Boolean,
    val maskUnsupportedIconPackIcons: Boolean,
    val customImageUri: String?,
)

internal data class SearchLoadedPreferencesSnapshot(
    val enabledFileTypes: Set<FileType>,
    val oneHandedMode: Boolean,
    val bottomSearchBarEnabled: Boolean,
    val unifiedPinnedItemsEnabled: Boolean,
    val searchHintsEnabled: Boolean,
    val settingsIconEnabled: Boolean,
    val topResultIndicatorEnabled: Boolean,
    val openTopResultUsingKeyboardEnabled: Boolean,
    val openKeyboardOnLaunch: Boolean,
    val clearQueryOnLaunch: Boolean,
    val autoCloseOverlay: Boolean,
    val overlayModeEnabled: Boolean,
    val appSuggestionsEnabled: Boolean,
    val showAllAppsButton: Boolean,
    val includeNonLaunchableAppsInSearch: Boolean,
    val selectedAppSuggestionTab: AppSuggestionTabType,
    val enabledAppSuggestionTabs: Set<AppSuggestionTabType>,
    val showAppLabels: Boolean,
    val phoneAppGridColumns: Int,
    val appIconSizeStep: Int,
    val appIconShape: AppIconShape,
    val launcherAppIcon: LauncherAppIcon,
    val themedIconsEnabled: Boolean,
    val deviceThemeEnabled: Boolean,
    val amoledThemeEnabled: Boolean,
    val maskUnsupportedIconPackIcons: Boolean,
    val backgroundSource: BackgroundSource,
    val wallpaperBackgroundAlpha: Float,
    val wallpaperBlurRadius: Float,
    val appTheme: AppTheme,
    val overlayThemeIntensity: Float,
    val fontScaleMultiplier: Float,
    val useSystemFont: Boolean,
    val customImageUri: String?,
    val showFolders: Boolean,
    val filePreviewsEnabled: Boolean,
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
    private val warmSearchableAppsSnapshot: (List<AppInfo>) -> Unit,
    private val refreshSettingsState: () -> Unit,
    private val refreshAppShortcutsState: () -> Unit,
    private val refreshDerivedState: (Long?, Boolean?) -> Unit,
    private val refreshPostStartupState: () -> Unit,
    private val saveStartupSurfaceSnapshotAsync: (Boolean, Boolean) -> Unit,
    private val applyPreferenceCacheToLegacyVars: () -> Unit,
    private val applyLauncherIconSelection: () -> Unit,
    private val refreshRecentItems: () -> Unit,
    private val awaitRecentItemsReady: suspend () -> Unit,
    private val getGridItemCount: () -> Int,
    private val selectSuggestedApps: (List<AppInfo>, Int, Boolean) -> List<AppInfo>,
    private val shouldShowSearchBarWelcome: () -> Boolean,
    private val loadApps: suspend () -> Unit,
    private val loadSettingsShortcuts: () -> Unit,
    private val loadAppSettings: () -> Unit,
    private val loadAppShortcuts: suspend () -> Unit,
    private val startupDispatcher: CoroutineDispatcher,
    private val loadPinnedAndExcludedCalendarEvents: () -> Unit,
    private val setDirectDialEnabled: (Boolean, Boolean) -> Unit,
    private val isQueryActive: () -> Boolean,
) {
    private var optionalStartupJob: Job? = null
    private var packageRefreshJob: Job? = null
    private var appUsageRefreshJob: Job? = null
    private var resumeCalendarRefreshJob: Job? = null
    private val pinningHandler get() = handlersProvider().pinningHandler
    private val searchEngineManager get() = handlersProvider().searchEngineManager
    private val secondarySearchOrchestrator get() = handlersProvider().secondarySearchOrchestrator
    private val sectionManager get() = handlersProvider().sectionManager

    private val aliasHandler get() = handlersProvider().aliasHandler
    private val appSearchManager get() = handlersProvider().appSearchManager
    private val appShortcutSearchHandler get() = handlersProvider().appShortcutSearchHandler
    private val aiSearchHandler get() = handlersProvider().aiSearchHandler
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
        disableOverlayModeForDefaultLauncher()
        val startupComplete = stateAccess.isStartupComplete

        val shouldRefreshLaunchPermissions = shouldRefreshPermissionSnapshot()
        var optionalPermissionsChanged = false
        if (shouldRefreshLaunchPermissions) {
            val previousUsage = permissionStateProvider().hasUsagePermission
            val latestUsage = repository.hasUsageAccess()
            val usageChanged = previousUsage != latestUsage
            if (usageChanged) {
                updatePermissionState { it.copy(hasUsagePermission = latestUsage) }
                if (startupComplete) {
                    if (latestUsage) refreshApps() else refreshAppSuggestions()
                }
            }

            optionalPermissionsChanged = run {
                val before = permissionStateProvider()
                handleOptionalPermissionChangeInternal(allowAppRefresh = startupComplete)
                permissionStateProvider() != before
            }
            markPermissionSnapshotRefreshed()
        }

        if (startupComplete && optionalPermissionsChanged) {
            pinningHandler.loadPinnedContactsAndFiles()
            pinningHandler.loadExcludedContactsAndFiles()
        }

        if (startupComplete && stateAccess.resumeNeedsStaticDataRefresh) {
            stateAccess.resumeNeedsStaticDataRefresh = false
            refreshSettingsState()
            refreshAppShortcutsState()
        }

        if (startupComplete) {
            resumeCalendarRefreshJob?.cancel()
            resumeCalendarRefreshJob =
                scope.launch(Dispatchers.IO) {
                    loadPinnedAndExcludedCalendarEvents()
                }
            refreshAppUsageMetadata()
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
            applyDefaultLauncherPreferenceTransition()
            if (!shouldRefreshPermissionSnapshot()) return@launch
            val latestUsagePermission = repository.hasUsageAccess()
            if (permissionStateProvider().hasUsagePermission != latestUsagePermission) {
                updatePermissionState { it.copy(hasUsagePermission = latestUsagePermission) }
            }
            refreshOptionalPermissions()
            markPermissionSnapshotRefreshed()
        }
    }

    private fun shouldRefreshPermissionSnapshot(): Boolean {
        val elapsed = SystemClock.elapsedRealtime() - stateAccess.lastPermissionSnapshotElapsedMs
        return elapsed !in 0 until PERMISSION_SNAPSHOT_DEDUP_WINDOW_MS
    }

    private fun markPermissionSnapshotRefreshed() {
        stateAccess.lastPermissionSnapshotElapsedMs = SystemClock.elapsedRealtime()
    }

    private fun applyDefaultLauncherPreferenceTransition() {
        val appliedDefaults =
            userPreferences.applyDefaultLauncherPreferencesIfNeeded(
                applicationProvider().isDefaultHomeApp(),
            )
        if (!appliedDefaults) return

        updateConfigState { state ->
            state.copy(
                oneHandedMode = userPreferences.isOneHandedMode(),
                bottomSearchBarEnabled = userPreferences.isBottomSearchBarEnabled(),
                openKeyboardOnLaunch = userPreferences.isOpenKeyboardOnLaunchEnabled(),
                showAllAppsButton = userPreferences.shouldShowAllAppsButton(),
                selectedAppSuggestionTab = userPreferences.getSelectedAppSuggestionTab(),
                enabledAppSuggestionTabs = userPreferences.getEnabledAppSuggestionTabs(),
            )
        }
        saveStartupSurfaceSnapshotAsync(true, false)
    }

    private fun disableOverlayModeForDefaultLauncher() {
        if (!applicationProvider().isDefaultHomeApp() || !userPreferences.isOverlayModeEnabled()) {
            return
        }

        userPreferences.setOverlayModeEnabled(false)
        updateUiState { it.copy(overlayModeEnabled = false) }
        OverlayModeController.stopOverlay(applicationProvider())
    }

    fun refreshOptionalPermissions(
        refreshCalendarData: Boolean = stateAccess.isStartupComplete,
    ): Boolean {
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

            if (hasCalendar && refreshCalendarData) {
                loadPinnedAndExcludedCalendarEvents()
            }

            sectionManager.refreshDisabledSections()
        }
        return changed
    }

    fun launchDeferredInitialization() {
        scope.launch(startupDispatcher) {
            val startupStartedAtElapsedMs = SystemClock.elapsedRealtime()
            AppSearchPerformanceLogger.log { "startupDeferredInitializationStarted" }
            startPackageChangeMonitoring()

            withContext(Dispatchers.Main.immediate) {
                releaseNotesHandler.checkForReleaseNotes()
            }

            val pinnedContactsStartupJob =
                scope.launch(Dispatchers.IO) {
                    pinningHandler.loadPinnedContactsForStartup()
                }

            // These provider-backed values define whether enabled Home sections have content or
            // a valid empty state. Load them on IO during phased startup instead of holding them
            // behind the long-idle maintenance window.
            val homeProviderStateJob = scope.launch(Dispatchers.IO) {
                pinnedContactsStartupJob.join()
                pinningHandler.loadPinnedContactsAndFilesNow()
                pinningHandler.loadExcludedContactsAndFilesNow()
                loadPinnedAndExcludedCalendarEvents()

                val pinnedSettingsState = settingsSearchHandler.getPinnedAndExcludedOnly()
                updateResultsState { state ->
                    state.copy(
                        pinnedSettings = pinnedSettingsState.pinned,
                        excludedSettings = pinnedSettingsState.excluded,
                    )
                }
                StartupTrace.mark("QS.Home.PinnedSettingsAvailable")
                StartupTrace.mark("QS.Home.EnabledSectionsAvailable")
            }

            // Pinned shortcuts are part of the empty-query home surface. Restore the bounded
            // persisted cache as soon as deferred initialization begins so they do not wait for
            // the long-idle system refresh below.
            if (appShortcutSearchHandler.loadCachedShortcutsOnly()) {
                withContext(Dispatchers.Main) { refreshAppShortcutsState() }
                StartupTrace.mark("QS.Home.ShortcutsCacheAvailable")
            }

            refreshAppsUsageAndPermissions()
            if (appSearchManager.cachedApps.isNotEmpty() && permissionStateProvider().hasUsagePermission) {
                appSearchManager.refreshUsageMetadataNow()
            }
            // A persisted catalog is already safe to render. Publish its suggestions before any
            // required reconciliation so slow PackageManager metadata reads cannot hold the app
            // grid in its loading state. A missing catalog still has to be loaded first.
            val hasCachedApps = appSearchManager.cachedApps.isNotEmpty()
            val reconcileAppsInBackground =
                shouldReconcileAppsAtStartup() && hasCachedApps
            val catalogReconciliationStartedAtElapsedMs = SystemClock.elapsedRealtime()
            AppSearchPerformanceLogger.log {
                "startupCatalogReconciliation decision=" +
                    when {
                        reconcileAppsInBackground -> "refreshCachedCatalog"
                        appSearchManager.cachedApps.isEmpty() -> "loadEmptyCatalog"
                        else -> "reuseFreshCatalog"
                    } + " cachedApps=${appSearchManager.cachedApps.size}"
            }
            if (hasCachedApps) {
                publishCurrentStartupAppSuggestions()
            }
            if (reconcileAppsInBackground) {
                packageRefreshJob?.cancel()
                packageRefreshJob = launch(startupDispatcher) { loadApps() }
                packageRefreshJob?.join()
            } else if (!hasCachedApps) {
                loadApps()
            }
            AppSearchPerformanceLogger.logTiming(
                event = "startupCatalogReady",
                elapsedMs = SystemClock.elapsedRealtime() - catalogReconciliationStartedAtElapsedMs,
                slowThresholdMs = 500L,
            ) {
                "apps=${appSearchManager.cachedApps.size} reconciled=$reconcileAppsInBackground"
            }
            if (!hasCachedApps) {
                publishCurrentStartupAppSuggestions()
            }

            val packageNames = appSearchManager.cachedApps.map { it.packageName }.toSet()
            val messagingInfo = getMessagingAppInfo(packageNames)

            searchEngineManager.ensureInitialized()
            val shortcutsState = aliasHandler.getInitialState()
            val customTools = normalizeCustomToolModels(userPreferences.getCustomTools())
            val hasApiKey = userPreferences.hasAnyLlmApiKey()
            val activeProviderId = aiSearchHandler.getAiSearchProviderId()
            val availableAiModels = aiSearchHandler.getAvailableGeminiModels()

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
                    colorVisualizerEnabled = userPreferences.isColorVisualizerEnabled(),
                    currencyConverterEnabled = userPreferences.isCurrencyConverterEnabled(),
                    worldClockEnabled = userPreferences.isWorldClockEnabled(),
                    dictionaryEnabled = userPreferences.isDictionaryEnabled(),
                    weatherEnabled = userPreferences.isWeatherEnabled(),
                    weatherLocationConfigured = userPreferences.getWeatherLocation().isNotBlank(),
                    weatherLocation = userPreferences.getWeatherLocation(),
                    customTools = customTools,
                    disabledCustomToolIds = userPreferences.getDisabledCustomTools(),
                    taskerIntentTools = userPreferences.getTaskerIntentTools(),
                    hasApiKey = hasApiKey,
                    geminiApiKeyLast4 = aiSearchHandler.getGeminiApiKey()?.takeLast(4),
                    llmApiKeyLast4ByProvider = userPreferences.getLlmApiKeyLast4ByProvider(),
                    customLlmBaseUrlByProvider = userPreferences.getCustomLlmBaseUrlByProvider(),
                    customLlmAdvancedPayloadByProvider =
                        userPreferences.getCustomLlmAdvancedPayloadByProvider(),
                    aiSearchLlmProviderId = activeProviderId,
                    personalContext = aiSearchHandler.getPersonalContext(),
                    geminiModel = aiSearchHandler.getGeminiModel(),
                    geminiGroundingEnabled = aiSearchHandler.isGeminiGroundingEnabled(),
                    geminiThinkingEnabled = aiSearchHandler.isGeminiThinkingEnabled(),
                    availableGeminiModels = availableAiModels,
                    availableLlmModelsByProvider =
                        userPreferences.getConfiguredLlmProviderIds().associateWith { providerId ->
                            if (providerId == activeProviderId) {
                                availableAiModels
                            } else {
                                AiSearchLlmProviderRegistry
                                    .get(providerId, applicationProvider())
                                    .fallbackTextModels
                            }
                        },
                )
            }
            updateResultsState { state ->
                val searchEnginesState =
                    when {
                        state.detectedShortcutTarget != null ->
                            SearchEnginesVisibility.ShortcutDetected(state.detectedShortcutTarget)
                        state.detectedAliasSearchSection != null -> SearchEnginesVisibility.Hidden
                        searchEngineManager.isSearchEngineCompactMode &&
                            searchEngineManager.getEnabledSearchTargets().isNotEmpty() ->
                            SearchEnginesVisibility.Compact
                        else -> SearchEnginesVisibility.Hidden
                    }
                state.copy(searchEnginesState = searchEnginesState)
            }
            StartupTrace.mark("QS.Home.SearchEnginesPublished")

            val visibleSearchTargetIconPackages =
                searchEngineManager
                    .getEnabledSearchTargets()
                    .take(MAX_STARTUP_SEARCH_TARGETS_TO_PREFETCH)
                    .flatMap { target ->
                        when (target) {
                            is SearchTarget.Engine -> target.engine.getAppPackageCandidates()
                            is SearchTarget.Browser -> listOf(target.app.packageName)
                            is SearchTarget.Custom -> emptyList()
                        }
                    }
            prefetchAppIcons(
                context = applicationProvider(),
                packageNames = visibleSearchTargetIconPackages,
                iconPackPackage = userPreferences.getSelectedIconPackPackage(),
                maxCount = MAX_STARTUP_SEARCH_TARGET_ICON_PACKAGES,
                forceCircularMask = configStateProvider().appIconShape == AppIconShape.CIRCLE,
            )
            updateConfigState { state ->
                state.copy(
                    showSearchEngineOnboarding =
                        searchEngineManager.isSearchEngineCompactMode &&
                            !userPreferences.hasSeenSearchEngineOnboarding(),
                    showSearchBarWelcomeAnimation = shouldShowSearchBarWelcome(),
                    appSuggestionsEnabled = userPreferences.areAppSuggestionsEnabled(),
                    showAllAppsButton = userPreferences.shouldShowAllAppsButton(),
                    selectedAppSuggestionTab = userPreferences.getSelectedAppSuggestionTab(),
                enabledAppSuggestionTabs = userPreferences.getEnabledAppSuggestionTabs(),
                showAppLabels = userPreferences.shouldShowAppLabels(),
                phoneAppGridColumns = userPreferences.getPhoneAppGridColumns(),
                appIconSizeStep = userPreferences.getAppIconSizeStep(),
                bottomSearchBarEnabled = userPreferences.isBottomSearchBarEnabled(),
                unifiedPinnedItemsEnabled = userPreferences.isUnifiedPinnedItemsEnabled(),
                searchHintsEnabled = userPreferences.isSearchHintsEnabled(),
                settingsIconEnabled = userPreferences.isSettingsIconEnabled(),
                    topResultIndicatorEnabled = userPreferences.isTopResultIndicatorEnabled(),
                    openTopResultUsingKeyboardEnabled = userPreferences.isOpenTopResultUsingKeyboardEnabled(),
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
                    isWhatsAppBusinessInstalled = messagingInfo.isWhatsAppBusinessInstalled,
                    isTelegramInstalled = messagingInfo.isTelegramInstalled,
                    isSignalInstalled = messagingInfo.isSignalInstalled,
                    isGoogleMeetInstalled = messagingInfo.isGoogleMeetInstalled,
                )
            }
            updateFeatureState { state ->
                state.copy(disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds())
            }

            if (!aiSearchHandler.getGeminiApiKey().isNullOrBlank()) {
                launch(Dispatchers.IO) {
                    delay(DEFERRED_AI_SEARCH_MODELS_DELAY_MS)
                    while (isQueryActive()) delay(OPTIONAL_STARTUP_QUERY_RECHECK_MS)
                    val models = aiSearchHandler.refreshAvailableGeminiModels()
                    updateFeatureState { state ->
                        state.copy(
                            availableGeminiModels = models,
                            availableLlmModelsByProvider =
                                state.availableLlmModelsByProvider +
                                    (aiSearchHandler.getAiSearchProviderId() to models),
                        )
                    }
                }
            }

            optionalStartupJob?.cancel()
            optionalStartupJob = launch(startupDispatcher) {
                delay(OPTIONAL_STARTUP_DELAY_MS)
                while (isQueryActive()) {
                    delay(OPTIONAL_STARTUP_QUERY_RECHECK_MS)
                }
                val hasApiKey = userPreferences.refreshConfiguredAiProviderHint()
                val activeProviderId = aiSearchHandler.getAiSearchProviderId()
                val availableGeminiModels = aiSearchHandler.getAvailableGeminiModels()
                updateFeatureState { state ->
                    state.copy(
                        hasApiKey = hasApiKey,
                        geminiApiKeyLast4 = aiSearchHandler.getGeminiApiKey()?.takeLast(4),
                        llmApiKeyLast4ByProvider = userPreferences.getLlmApiKeyLast4ByProvider(),
                        customLlmBaseUrlByProvider = userPreferences.getCustomLlmBaseUrlByProvider(),
                        customLlmAdvancedPayloadByProvider = userPreferences.getCustomLlmAdvancedPayloadByProvider(),
                        aiSearchLlmProviderId = activeProviderId,
                        personalContext = aiSearchHandler.getPersonalContext(),
                        geminiModel = aiSearchHandler.getGeminiModel(),
                        geminiGroundingEnabled = aiSearchHandler.isGeminiGroundingEnabled(),
                        geminiThinkingEnabled = aiSearchHandler.isGeminiThinkingEnabled(),
                        availableGeminiModels = availableGeminiModels,
                        availableLlmModelsByProvider =
                            userPreferences.getConfiguredLlmProviderIds().associateWith { providerId ->
                                if (providerId == activeProviderId) {
                                    availableGeminiModels
                                } else {
                                    AiSearchLlmProviderRegistry
                                        .get(providerId, applicationProvider())
                                        .fallbackTextModels
                                }
                            },
                    )
                }
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

                // App settings build themselves lazily on the first matching query. Device
                // settings do the same on their IO search path. Refresh app shortcuts only in
                // the long-idle tier; their bounded cache was already loaded above.
                loadAppShortcuts()

                // The persisted package is applied to initial state before first render. Keep the
                // PackageManager scan out of the critical path and only validate/discover packs
                // after the optional-startup idle window.
                iconPackHandler.refreshIconPacks()
                StartupTrace.mark("QS.Startup.NonessentialRefreshComplete")
            }

            homeProviderStateJob.join()
            awaitRecentItemsReady()

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
            // Suggestions were finalized and published above. Only refresh the remaining
            // derived state here so the visible app grid does not change after first display.
            withContext(Dispatchers.Default) { refreshPostStartupState() }
            StartupTrace.mark("QS.Startup.PhasedInitializationComplete")
            AppSearchPerformanceLogger.logTiming(
                event = "startupDeferredInitializationComplete",
                elapsedMs = SystemClock.elapsedRealtime() - startupStartedAtElapsedMs,
                slowThresholdMs = 1_500L,
            ) {
                "apps=${appSearchManager.cachedApps.size}"
            }
            saveStartupSurfaceSnapshotAsync(true, false)
        }
    }

    private fun startPackageChangeMonitoring() {
        repository.startPackageChangeMonitoring { change ->
            change.packageName?.let { packageName ->
                if (change.isRemoval) {
                    if (appShortcutSearchHandler.removeUnavailablePackage(packageName)) {
                        refreshAppShortcutsState()
                    }
                } else {
                    appShortcutSearchHandler.markPackageAvailable(packageName)
                }
            }

            if (change.isRemoval) {
                appSearchManager.removeUnavailableApp(change)
            }

            packageRefreshJob?.cancel()
            packageRefreshJob =
                scope.launch(startupDispatcher) {
                    loadApps()
                    loadAppShortcuts()
                }
        }
    }

    private fun refreshAppUsageMetadata() {
        if (!permissionStateProvider().hasUsagePermission) {
            refreshAppSuggestions()
            return
        }

        appUsageRefreshJob?.cancel()
        appUsageRefreshJob =
            scope.launch(startupDispatcher) {
                appSearchManager.refreshUsageMetadataNow()
            }
    }

    private fun shouldReconcileAppsAtStartup(): Boolean {
        if (appSearchManager.cachedApps.isEmpty()) return true
        if (repository.isAppCatalogInvalidated()) return true
        val ageMs = System.currentTimeMillis() - repository.cacheLastUpdatedMillis()
        return ageMs !in 0 until APP_RECONCILIATION_FRESHNESS_MS
    }

    suspend fun loadCacheAndMinimalPrefs() {
        val startedAtElapsedMs = SystemClock.elapsedRealtime()
        val startupConfig = userPreferences.loadStartupConfig()
        setPrefCache(
            SearchPreferenceCache.from(
                config = startupConfig,
                assistantLaunchVoiceModeEnabled = userPreferences.isAssistantLaunchVoiceModeEnabled(),
            ),
        )
        applyPreferenceCacheToLegacyVars()
        val startupSnapshot = readStartupPreferencesSnapshot()

        val cachedAppsList =
            runCatching {
                repository.loadCachedApps(
                    includeNonLaunchableApps = userPreferences.shouldIncludeNonLaunchableAppsInSearch(),
                )
            }.getOrNull()
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
                    unifiedPinnedItemsEnabled = startupSnapshot.unifiedPinnedItemsEnabled,
                    topResultIndicatorEnabled = startupSnapshot.topResultIndicatorEnabled,
                    openTopResultUsingKeyboardEnabled = startupSnapshot.openTopResultUsingKeyboardEnabled,
                    accentColorMode = startupSnapshot.accentColorMode,
                    customAccentColorArgb = startupSnapshot.customAccentColorArgb,
                    openKeyboardOnLaunch = startupSnapshot.openKeyboardOnLaunch,
                    clearQueryOnLaunch = startupSnapshot.clearQueryOnLaunch,
                    autoCloseOverlay = startupSnapshot.autoCloseOverlay,
                    showWallpaperBackground =
                        startupSnapshot.backgroundSource != BackgroundSource.THEME,
                    wallpaperBackgroundAlpha = startupSnapshot.wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = startupSnapshot.wallpaperBlurRadius,
                    appTheme = startupSnapshot.appTheme,
                    overlayThemeIntensity = startupSnapshot.overlayThemeIntensity,
                    useSystemFont = startupSnapshot.useSystemFont,
                    backgroundSource = startupSnapshot.backgroundSource,
                    customImageUri = startupSnapshot.customImageUri,
                    appIconShape = startupSnapshot.appIconShape,
                    launcherAppIcon = startupSnapshot.launcherAppIcon,
                    themedIconsEnabled = startupSnapshot.themedIconsEnabled,
                    deviceThemeEnabled = startupSnapshot.deviceThemeEnabled,
                    amoledThemeEnabled = startupSnapshot.amoledThemeEnabled,
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
                initializeWithCacheMinimal(cachedAppsList)
                StartupTrace.mark("QS.Home.CachedAppsAvailable")
            }
        }
        markPermissionSnapshotRefreshed()

        setStartupConfig(startupConfig)
        applyLauncherIconSelection()

        if (!cachedAppsList.isNullOrEmpty()) {
            val searchableAppsWarmupJob =
                scope.launch(Dispatchers.Default) {
                    warmSearchableAppsSnapshot(cachedAppsList)
                }
            if (userPreferences.areAppSuggestionsEnabled()) {
                val visibleApps =
                    selectSuggestedApps(cachedAppsList, getGridItemCount(), hasUsagePermission)
                val iconPack = userPreferences.getSelectedIconPackPackage()
                prefetchAppIcons(
                    context = applicationProvider(),
                    packageNames = visibleApps.map { it.packageName },
                    iconPackPackage = iconPack,
                    forceCircularMask = startupSnapshot.appIconShape == AppIconShape.CIRCLE,
                )
            }
            searchableAppsWarmupJob.join()
        }
        AppSearchPerformanceLogger.logTiming(
            event = "startupCacheAndMinimalPrefsReady",
            elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
            slowThresholdMs = 250L,
        ) {
            "cachedApps=${cachedAppsList?.size ?: 0} usagePermission=$hasUsagePermission"
        }
    }

    suspend fun loadRemainingStartupPreferences(applyStartupPreferences: (StartupPreferencesFacade.StartupPreferences) -> Unit) {
        val startedAtElapsedMs = SystemClock.elapsedRealtime()
        val startupPrefs =
            getStartupConfig()?.startupPreferences
                ?: userPreferences.getStartupPreferences()

        withContext(Dispatchers.Main) {
            applyStartupPreferences(startupPrefs)
        }

        val lastUpdated =
            getStartupConfig()?.cachedAppsLastUpdate
                ?: repository.cacheLastUpdatedMillis()
        withContext(Dispatchers.Default) { refreshDerivedState(lastUpdated, false) }
        withContext(Dispatchers.Main) { updateConfigState { it.copy(isInitializing = false) } }
        AppSearchPerformanceLogger.logTiming(
            event = "startupRemainingPreferencesReady",
            elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
            slowThresholdMs = 250L,
        )
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
                unifiedPinnedItemsEnabled = snapshot.unifiedPinnedItemsEnabled,
                searchHintsEnabled = snapshot.searchHintsEnabled,
                settingsIconEnabled = snapshot.settingsIconEnabled,
                topResultIndicatorEnabled = snapshot.topResultIndicatorEnabled,
                openTopResultUsingKeyboardEnabled = snapshot.openTopResultUsingKeyboardEnabled,
                openKeyboardOnLaunch = snapshot.openKeyboardOnLaunch,
                clearQueryOnLaunch = snapshot.clearQueryOnLaunch,
                autoCloseOverlay = snapshot.autoCloseOverlay,
                overlayModeEnabled = snapshot.overlayModeEnabled,
                appSuggestionsEnabled = snapshot.appSuggestionsEnabled,
                showAllAppsButton = snapshot.showAllAppsButton,
                includeNonLaunchableAppsInSearch = snapshot.includeNonLaunchableAppsInSearch,
                selectedAppSuggestionTab = snapshot.selectedAppSuggestionTab,
                enabledAppSuggestionTabs = snapshot.enabledAppSuggestionTabs,
                showAppLabels = snapshot.showAppLabels,
                phoneAppGridColumns = snapshot.phoneAppGridColumns,
                appIconSizeStep = snapshot.appIconSizeStep,
                appIconShape = snapshot.appIconShape,
                launcherAppIcon = snapshot.launcherAppIcon,
                themedIconsEnabled = snapshot.themedIconsEnabled,
                deviceThemeEnabled = snapshot.deviceThemeEnabled,
                amoledThemeEnabled = snapshot.amoledThemeEnabled,
                maskUnsupportedIconPackIcons = snapshot.maskUnsupportedIconPackIcons,
                showWallpaperBackground = snapshot.backgroundSource != BackgroundSource.THEME,
                wallpaperBackgroundAlpha = snapshot.wallpaperBackgroundAlpha,
                wallpaperBlurRadius = snapshot.wallpaperBlurRadius,
                appTheme = snapshot.appTheme,
                overlayThemeIntensity = snapshot.overlayThemeIntensity,
                fontScaleMultiplier = snapshot.fontScaleMultiplier,
                useSystemFont = snapshot.useSystemFont,
                backgroundSource = snapshot.backgroundSource,
                customImageUri = snapshot.customImageUri,
                showFolders = snapshot.showFolders,
                filePreviewsEnabled = snapshot.filePreviewsEnabled,
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
                numberSearchEnabled = userPreferences.isNumberSearchEnabled(),
                assistantLaunchVoiceModeEnabled = snapshot.assistantLaunchVoiceModeEnabled,
                disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds(),
                recentQueriesEnabled = prefs.searchHistoryEnabled,
                recentQueriesDisplayCount = userPreferences.getRecentQueriesDisplayCount(),
                appResultRowCount = userPreferences.getAppResultRowCount(),
                fuzzySearchEnabled =
                    !com.tk.quicksearch.shared.util.isLowRamDevice(applicationProvider()) &&
                        userPreferences.isFuzzySearchEnabled(),
                fuzzySearchAvailable =
                    !com.tk.quicksearch.shared.util.isLowRamDevice(applicationProvider()),
                secondaryRankingSignal = userPreferences.getSecondaryRankingSignal(),
                webSuggestionsCount = userPreferences.getWebSuggestionsCount(),
                topMatchesEnabled = userPreferences.isTopMatchesEnabled(),
                topMatchesLimit = userPreferences.getTopMatchesLimit(),
                topMatchesSectionOrder = userPreferences.getTopMatchesSectionOrder(),
                disabledTopMatchesSections = userPreferences.getDisabledTopMatchesSections(),
                shouldShowUsagePermissionBanner = userPreferences.shouldShowUsagePermissionBanner(),
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
            aiSearchHandler.reloadFromPreferences()
            val webSuggestionsEnabled = webSuggestionHandler.reloadFromPreferences()

            val geminiApiKey = aiSearchHandler.getGeminiApiKey()
            val personalContext = aiSearchHandler.getPersonalContext()
            val geminiModel = aiSearchHandler.getGeminiModel()
            val geminiGroundingEnabled = aiSearchHandler.isGeminiGroundingEnabled()
            val geminiThinkingEnabled = aiSearchHandler.isGeminiThinkingEnabled()
            val availableGeminiModels = aiSearchHandler.getAvailableGeminiModels()
            val hasApiKey = userPreferences.hasAnyLlmApiKey()
            val customTools = normalizeCustomToolModels(userPreferences.getCustomTools())

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
                        colorVisualizerEnabled = userPreferences.isColorVisualizerEnabled(),
                        currencyConverterEnabled = userPreferences.isCurrencyConverterEnabled(),
                        worldClockEnabled = userPreferences.isWorldClockEnabled(),
                        dictionaryEnabled = userPreferences.isDictionaryEnabled(),
                        weatherEnabled = userPreferences.isWeatherEnabled(),
                        weatherLocationConfigured = userPreferences.getWeatherLocation().isNotBlank(),
                        weatherLocation = userPreferences.getWeatherLocation(),
                        customTools = customTools,
                        disabledCustomToolIds = userPreferences.getDisabledCustomTools(),
                        taskerIntentTools = userPreferences.getTaskerIntentTools(),
                        hasApiKey = hasApiKey,
                        geminiApiKeyLast4 = geminiApiKey?.takeLast(4),
                        llmApiKeyLast4ByProvider = userPreferences.getLlmApiKeyLast4ByProvider(),
                        customLlmBaseUrlByProvider = userPreferences.getCustomLlmBaseUrlByProvider(),
                        customLlmAdvancedPayloadByProvider = userPreferences.getCustomLlmAdvancedPayloadByProvider(),
                        aiSearchLlmProviderId = aiSearchHandler.getAiSearchProviderId(),
                        personalContext = personalContext,
                        geminiModel = geminiModel,
                        geminiGroundingEnabled = geminiGroundingEnabled,
                        geminiThinkingEnabled = geminiThinkingEnabled,
                        availableGeminiModels = availableGeminiModels,
                        availableLlmModelsByProvider =
                            userPreferences.getConfiguredLlmProviderIds().associateWith { providerId ->
                                if (providerId == aiSearchHandler.getAiSearchProviderId()) {
                                    availableGeminiModels
                                } else {
                                    AiSearchLlmProviderRegistry
                                        .get(providerId, applicationProvider())
                                        .fallbackTextModels
                                }
                            },
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
        return state.AiSearchState.status != AiSearchStatus.Idle ||
            state.currencyConverterState.status != CurrencyConverterStatus.Idle ||
            state.worldClockState.status != WorldClockStatus.Idle ||
            state.dictionaryState.status != DictionaryStatus.Idle
    }

    private fun refreshAppsUsageAndPermissions() {
        updatePermissionState { it.copy(hasUsagePermission = repository.hasUsageAccess()) }
        refreshOptionalPermissions()
    }

    private fun initializeWithCacheMinimal(
        cachedAppsList: List<AppInfo>,
    ) {
        val startupSnapshot = readStartupPreferencesSnapshot()
        appSearchManager.initCache(cachedAppsList)
        val lastUpdated = repository.cacheLastUpdatedMillis()
        val suggestionsEnabled = userPreferences.areAppSuggestionsEnabled()
        val startupPrefs = getStartupConfig()?.startupPreferences
        val labelsEnabled = startupPrefs?.showAppLabels ?: userPreferences.shouldShowAppLabels()
        val columnsForPhone =
            startupPrefs?.phoneAppGridColumns ?: userPreferences.getPhoneAppGridColumns()
        val appIconSizeStep =
            startupPrefs?.appIconSizeStep ?: userPreferences.getAppIconSizeStep()

        updateResultsState {
            it.copy(
                cacheLastUpdatedMillis = lastUpdated,
                // The cached order is kept off-screen until usage metadata and any required app
                // catalog reconciliation are complete.
                recentApps = emptyList(),
                indexedAppCount = cachedAppsList.size,
            )
        }
        updateConfigState {
            it.copy(
                oneHandedMode = startupSnapshot.oneHandedMode,
                bottomSearchBarEnabled = startupSnapshot.bottomSearchBarEnabled,
                openKeyboardOnLaunch = startupSnapshot.openKeyboardOnLaunch,
                appSuggestionsEnabled = suggestionsEnabled,
                showAllAppsButton = userPreferences.shouldShowAllAppsButton(),
                includeNonLaunchableAppsInSearch =
                    userPreferences.shouldIncludeNonLaunchableAppsInSearch(),
                selectedAppSuggestionTab = userPreferences.getSelectedAppSuggestionTab(),
                enabledAppSuggestionTabs = userPreferences.getEnabledAppSuggestionTabs(),
                showAppLabels = labelsEnabled,
                phoneAppGridColumns = columnsForPhone,
                appIconSizeStep = appIconSizeStep,
                isStartupCoreSurfaceReady = true,
            )
        }
        saveStartupSurfaceSnapshotAsync(false, false)
    }

    private suspend fun publishCurrentStartupAppSuggestions() {
        val startedAtElapsedMs = SystemClock.elapsedRealtime()
        refreshAppSuggestions()
        publishStartupAppSuggestions()
        AppSearchPerformanceLogger.logTiming(
            event = "startupSuggestionsPublished",
            elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
            slowThresholdMs = 100L,
        ) {
            "recents=${resultsStateProvider().recentApps.size} pinned=${resultsStateProvider().pinnedApps.size}"
        }
    }

    private suspend fun publishStartupAppSuggestions() {
        withContext(Dispatchers.Main.immediate) {
            updateResultsState { state ->
                if (state.query.isNotBlank() || state.recentApps.isEmpty()) {
                    state
                } else {
                    state.copy(
                        screenState = ScreenVisibilityState.Content,
                        appsSectionState =
                            AppsSectionVisibility.ShowingResults(
                                hasPinned = state.pinnedApps.isNotEmpty(),
                            ),
                    )
                }
            }
            StartupTrace.mark("QS.Home.AppSuggestionsPublished")
        }
    }

    private fun getMessagingAppInfo(packageNames: Set<String>): MessagingAppInfo {
        val isWhatsAppInstalled =
            if (packageNames.isNotEmpty()) {
                packageNames.contains(PackageConstants.WHATSAPP_PACKAGE)
            } else {
                messagingHandler.isPackageInstalled(PackageConstants.WHATSAPP_PACKAGE)
            }
        val isWhatsAppBusinessInstalled =
            if (packageNames.isNotEmpty()) {
                packageNames.contains(PackageConstants.WHATSAPP_BUSINESS_PACKAGE)
            } else {
                messagingHandler.isPackageInstalled(PackageConstants.WHATSAPP_BUSINESS_PACKAGE)
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
                whatsappBusinessInstalled = isWhatsAppBusinessInstalled,
                telegramInstalled = isTelegramInstalled,
                signalInstalled = isSignalInstalled,
                updateState = false,
            )
        val selectedCallingApp = userPreferences.getCallingApp()
        val resolvedCallingApp =
            resolveCallingApp(
                app = selectedCallingApp,
                isWhatsAppInstalled = isWhatsAppInstalled,
                isWhatsAppBusinessInstalled = isWhatsAppBusinessInstalled,
                isTelegramInstalled = isTelegramInstalled,
                isSignalInstalled = isSignalInstalled,
                isGoogleMeetInstalled = isGoogleMeetInstalled,
            )
        if (resolvedCallingApp != selectedCallingApp) {
            userPreferences.setCallingApp(resolvedCallingApp)
        }

        return MessagingAppInfo(
            isWhatsAppInstalled,
            isWhatsAppBusinessInstalled,
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
        isWhatsAppBusinessInstalled: Boolean,
        isTelegramInstalled: Boolean,
        isSignalInstalled: Boolean,
        isGoogleMeetInstalled: Boolean,
    ): CallingApp =
        when (app) {
            CallingApp.WHATSAPP -> if (isWhatsAppInstalled) CallingApp.WHATSAPP else CallingApp.CALL
            CallingApp.WHATSAPP_BUSINESS -> if (isWhatsAppBusinessInstalled) CallingApp.WHATSAPP_BUSINESS else CallingApp.CALL
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

    private fun normalizeCustomToolModels(tools: List<CustomTool>): List<CustomTool> {
        val normalizedTools =
            tools.map { tool ->
                if (tool.modelId.isNotBlank()) {
                    tool
                } else {
                    tool.copy(
                        modelId =
                            AiSearchLlmProviderRegistry
                                .get(tool.providerId, applicationProvider())
                                .defaultModelId,
                    )
                }
            }

        if (normalizedTools != tools) {
            userPreferences.setCustomTools(normalizedTools)
        }
        return normalizedTools
    }

    private data class MessagingAppInfo(
        val isWhatsAppInstalled: Boolean,
        val isWhatsAppBusinessInstalled: Boolean,
        val isTelegramInstalled: Boolean,
        val isSignalInstalled: Boolean,
        val messagingApp: MessagingApp,
        val isGoogleMeetInstalled: Boolean,
        val callingApp: CallingApp,
    )

    companion object {
        private const val BROWSER_REFRESH_INTERVAL_MS = 5 * 60 * 1_000L
        private const val DEFERRED_AI_SEARCH_MODELS_DELAY_MS = 15_000L
        private const val OPTIONAL_STARTUP_DELAY_MS = 10_000L
        private const val OPTIONAL_STARTUP_QUERY_RECHECK_MS = 1_000L
        private const val APP_RECONCILIATION_FRESHNESS_MS = 24L * 60L * 60L * 1_000L
        private const val PERMISSION_SNAPSHOT_DEDUP_WINDOW_MS = 1_500L
        private const val MAX_STARTUP_SEARCH_TARGETS_TO_PREFETCH = 14
        private const val MAX_STARTUP_SEARCH_TARGET_ICON_PACKAGES = 30
    }
}
