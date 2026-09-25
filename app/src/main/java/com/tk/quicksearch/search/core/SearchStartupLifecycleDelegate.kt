package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.userAppPreferences.StartupPreferencesFacade
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
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
import com.tk.quicksearch.app.PinnedShortcutRequests
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
    internal val scope: CoroutineScope,
    internal val applicationProvider: () -> android.app.Application,
    internal val repository: AppsRepository,
    internal val userPreferences: UserAppPreferences,
    internal val handlersProvider: () -> SearchHandlerContainer,
    internal val resultsStateProvider: () -> SearchResultsState,
    internal val permissionStateProvider: () -> SearchPermissionState,
    internal val configStateProvider: () -> SearchUiConfigState,
    internal val stateAccess: SearchStartupLifecycleStateAccess,
    internal val getStartupConfig: () -> StartupPreferencesFacade.StartupConfig?,
    internal val setStartupConfig: (StartupPreferencesFacade.StartupConfig?) -> Unit,
    internal val setPrefCache: (SearchPreferenceCache) -> Unit,
    internal val readStartupPreferencesSnapshot: () -> SearchStartupPreferencesSnapshot,
    internal val readLoadedPreferencesSnapshot: () -> SearchLoadedPreferencesSnapshot,
    internal val updatePermissionState: ((SearchPermissionState) -> SearchPermissionState) -> Unit,
    internal val updateFeatureState: ((SearchFeatureState) -> SearchFeatureState) -> Unit,
    internal val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    internal val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
    internal val updateConfigState: ((SearchUiConfigState) -> SearchUiConfigState) -> Unit,
    internal val applyVisibilityStates: (SearchUiState) -> SearchUiState,
    internal val hasContactPermission: () -> Boolean,
    internal val hasFilePermission: () -> Boolean,
    internal val hasCalendarPermission: () -> Boolean,
    internal val clearQuery: () -> Unit,
    internal val refreshApps: () -> Unit,
    internal val refreshAppSuggestions: () -> Unit,
    internal val warmSearchableAppsSnapshot: (List<AppInfo>) -> Unit,
    internal val refreshSettingsState: () -> Unit,
    internal val refreshAppShortcutsState: () -> Unit,
    internal val refreshDerivedState: (Long?, Boolean?) -> Unit,
    internal val refreshPostStartupState: () -> Unit,
    internal val saveStartupSurfaceSnapshotAsync: (Boolean, Boolean) -> Unit,
    internal val applyPreferenceCacheToLegacyVars: () -> Unit,
    internal val applyLauncherIconSelection: () -> Unit,
    internal val refreshRecentItems: () -> Unit,
    internal val awaitRecentItemsReady: suspend () -> Unit,
    internal val getGridItemCount: () -> Int,
    internal val selectSuggestedApps: (List<AppInfo>, Int, Boolean) -> List<AppInfo>,
    internal val shouldShowSearchBarWelcome: () -> Boolean,
    internal val loadApps: suspend () -> Unit,
    internal val loadSettingsShortcuts: () -> Unit,
    internal val loadAppSettings: () -> Unit,
    internal val loadAppShortcuts: suspend () -> Unit,
    internal val startupDispatcher: CoroutineDispatcher,
    internal val loadPinnedAndExcludedCalendarEvents: () -> Unit,
    internal val setDirectDialEnabled: (Boolean, Boolean) -> Unit,
    internal val isQueryActive: () -> Boolean,
) {
    internal var optionalStartupJob: Job? = null
    internal var packageRefreshJob: Job? = null
    internal var appUsageRefreshJob: Job? = null
    internal var resumeCalendarRefreshJob: Job? = null
    internal val pinningHandler get() = handlersProvider().pinningHandler
    internal val searchEngineManager get() = handlersProvider().searchEngineManager
    internal val secondarySearchOrchestrator get() = handlersProvider().secondarySearchOrchestrator
    internal val sectionManager get() = handlersProvider().sectionManager

    internal val aliasHandler get() = handlersProvider().aliasHandler
    internal val appSearchManager get() = handlersProvider().appSearchManager
    internal val appShortcutSearchHandler get() = handlersProvider().appShortcutSearchHandler
    internal val aiSearchHandler get() = handlersProvider().aiSearchHandler
    internal val iconPackHandler get() = handlersProvider().iconPackHandler
    internal val messagingHandler get() = handlersProvider().messagingHandler
    internal val releaseNotesHandler get() = handlersProvider().releaseNotesHandler
    internal val settingsSearchHandler get() = handlersProvider().settingsSearchHandler
    internal val webSuggestionHandler get() = handlersProvider().webSuggestionHandler

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

        if (startupComplete && PinnedShortcutRequests.consumeRefreshNeeded()) {
            scope.launch(Dispatchers.IO) { loadAppShortcuts() }
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

    internal fun markPermissionSnapshotRefreshed() {
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
        disableNotificationDotsIfPermissionMissing()
        return changed
    }

    private fun disableNotificationDotsIfPermissionMissing() {
        if (!configStateProvider().notificationDotsEnabled) return
        if (NotificationDotsPermission.canEnableNotificationDots(applicationProvider())) return
        userPreferences.setNotificationDotsEnabled(false)
        updateConfigState { it.copy(notificationDotsEnabled = false) }
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
                    availableLlmModelsByProvider = emptyMap(),
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
                        availableLlmModelsByProvider = emptyMap(),
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

}
