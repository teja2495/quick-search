package com.tk.quicksearch.search.core

import android.app.Application
import android.content.Intent
import android.os.Trace
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.quicksearch.R
import com.tk.quicksearch.app.ReleaseNotesHandler
import com.tk.quicksearch.app.navigation.NavigationHandler
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.search.appShortcuts.AppShortcutManagementHandler
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingsRepository
import com.tk.quicksearch.search.appSettings.AppSettingsSearchHandler
import com.tk.quicksearch.search.apps.AppManagementService
import com.tk.quicksearch.search.apps.AppSearchManager
import com.tk.quicksearch.search.apps.IconPackService
import com.tk.quicksearch.search.apps.invalidateAppIconCache
import com.tk.quicksearch.search.apps.prefetchAppIcons
import com.tk.quicksearch.search.calendar.CalendarManagementHandler
import com.tk.quicksearch.search.common.PinningHandler
import com.tk.quicksearch.search.contacts.actions.ContactActionHandler
import com.tk.quicksearch.search.contacts.utils.ContactCallingAppResolver
import com.tk.quicksearch.search.contacts.utils.ContactManagementHandler
import com.tk.quicksearch.search.contacts.utils.ContactMessagingAppResolver
import com.tk.quicksearch.search.contacts.utils.MessagingHandler
import com.tk.quicksearch.search.contacts.utils.TelegramContactUtils
import com.tk.quicksearch.search.data.AppShortcutRepository.AppShortcutRepository
import com.tk.quicksearch.search.data.AppShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.launchStaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.StartupPreferencesFacade
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsManagementHandler
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsRepository
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.files.FileManagementHandler
import com.tk.quicksearch.search.files.FileSearchHandler
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.startup.StartupSurfaceSnapshot
import com.tk.quicksearch.search.startup.StartupSurfaceStore
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.search.webSuggestions.WebSuggestionHandler
import com.tk.quicksearch.searchEngines.SearchEngineManager
import com.tk.quicksearch.searchEngines.SecondarySearchOrchestrator
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.util.PackageConstants
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.getAppGridColumns
import com.tk.quicksearch.shared.util.isLowRamDevice
import com.tk.quicksearch.tools.aiTools.CurrencyConverterHandler
import com.tk.quicksearch.tools.aiTools.DictionaryHandler
import com.tk.quicksearch.tools.aiTools.WordClockHandler
import com.tk.quicksearch.tools.calculator.CalculatorHandler
import com.tk.quicksearch.tools.dateCalculator.DateCalculatorHandler
import com.tk.quicksearch.tools.directSearch.DirectSearchHandler
import com.tk.quicksearch.tools.directSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.unitConverter.UnitConverterHandler
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//TODO: Refactor this HUGE file

class SearchViewModel(
        application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val startupPreferencesReader = UserAppPreferences(appContext)
    private val instantStartupSurfaceEnabled = startupPreferencesReader.isInstantStartupSurfaceEnabled()
    private val startupSurfaceStore = StartupSurfaceStore(appContext)
    private val startupSnapshot: StartupSurfaceSnapshot? =
            if (instantStartupSurfaceEnabled) startupSurfaceStore.loadSnapshot() else null
    private val initialBackgroundSource = startupPreferencesReader.getBackgroundSource()
    private val initialCustomImageUri = startupPreferencesReader.getCustomImageUri()
    private val initialAppThemeMode = startupPreferencesReader.getAppThemeMode()
    private val initialIsDarkMode: Boolean = when (initialAppThemeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> {
            val nightModeFlags =
                    appContext.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK
            nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }
    private val initialPreviewPath =
            startupSnapshot?.startupBackgroundPreviewPath?.takeIf { snapshotPath ->
                startupSnapshot.backgroundSource == initialBackgroundSource &&
                        (initialBackgroundSource != BackgroundSource.CUSTOM_IMAGE ||
                                startupSnapshot.customImageUri == initialCustomImageUri) &&
                        !snapshotPath.isNullOrBlank()
            }

    private val initialResultsState =
            SearchResultsState(
                    query =
                            if (startupPreferencesReader.isClearQueryOnLaunchEnabled()) {
                                ""
                            } else {
                                inMemoryRetainedQuery
                            },
                    recentApps = startupSnapshot?.suggestedApps.orEmpty(),
                    indexedAppCount = startupSnapshot?.suggestedApps?.size ?: 0,
            )

    private val initialConfigState =
            SearchUiConfigState(
                    startupPhase = StartupPhase.PHASE_1_CACHE_PREFS,
                    isInitializing = true,
                    isLoading = true,
                    isStartupCoreSurfaceReady = startupSnapshot != null,
                    showWallpaperBackground =
                            startupSnapshot?.showWallpaperBackground
                                    ?: initialBackgroundSource != BackgroundSource.THEME,
                    wallpaperBackgroundAlpha =
                            startupSnapshot?.wallpaperBackgroundAlpha
                                    ?: startupPreferencesReader.getWallpaperBackgroundAlpha(
                                            initialIsDarkMode,
                                    ),
                    wallpaperBlurRadius =
                            startupSnapshot?.wallpaperBlurRadius
                                    ?: startupPreferencesReader.getWallpaperBlurRadius(
                                            initialIsDarkMode,
                                    ),
                    appTheme =
                            startupSnapshot?.appTheme
                                    ?: startupPreferencesReader.getAppTheme(),
                    overlayThemeIntensity =
                            sanitizeOverlayThemeIntensity(
                                    startupSnapshot?.overlayThemeIntensity
                                            ?: startupPreferencesReader.getOverlayThemeIntensity(),
                            ),
                    appThemeMode =
                            initialAppThemeMode,
                    backgroundSource =
                            initialBackgroundSource,
                    customImageUri = initialCustomImageUri,
                    startupBackgroundPreviewPath = initialPreviewPath,
                    oneHandedMode =
                            startupSnapshot?.oneHandedMode
                                    ?: startupPreferencesReader.isOneHandedMode(),
                    bottomSearchBarEnabled =
                            startupSnapshot?.bottomSearchBarEnabled
                                    ?: startupPreferencesReader.isBottomSearchBarEnabled(),
                    topResultIndicatorEnabled =
                            startupSnapshot?.topResultIndicatorEnabled
                                    ?: startupPreferencesReader.isTopResultIndicatorEnabled(),
                    openKeyboardOnLaunch =
                            startupSnapshot?.openKeyboardOnLaunch
                                    ?: startupPreferencesReader.isOpenKeyboardOnLaunchEnabled(),
                    clearQueryOnLaunch =
                            startupPreferencesReader.isClearQueryOnLaunchEnabled(),
                    autoCloseOverlay =
                            startupPreferencesReader.isAutoCloseOverlayEnabled(),
                    fontScaleMultiplier =
                            sanitizeFontScaleMultiplier(
                                    startupSnapshot?.fontScaleMultiplier
                                            ?: startupPreferencesReader.getFontScaleMultiplier(),
                            ),
                    launcherAppIcon = startupPreferencesReader.getLauncherAppIcon(),
                    showAppLabels =
                            startupSnapshot?.showAppLabels
                                    ?: startupPreferencesReader.shouldShowAppLabels(),
                    appSuggestionsEnabled =
                            startupSnapshot?.appSuggestionsEnabled
                                    ?: startupPreferencesReader.areAppSuggestionsEnabled(),
                    selectRetainedQuery =
                            !startupPreferencesReader.isClearQueryOnLaunchEnabled() &&
                                    inMemoryRetainedQuery.isNotEmpty(),
            )

    private val repository by lazy { AppsRepository(appContext) }
    private val appShortcutRepository by lazy {
        AppShortcutRepository(appContext)
    }
    private val calendarRepository by lazy { CalendarRepository(appContext) }
    private val contactRepository by lazy { ContactRepository(appContext) }
    private val fileRepository by lazy { FileSearchRepository(appContext) }
    private val settingsShortcutRepository by lazy {
        DeviceSettingsRepository(appContext)
    }
    private val appSettingsRepository by lazy {
        AppSettingsRepository(appContext)
    }
    private val userPreferences by lazy { UserAppPreferences(appContext) }
    private val launcherIconManager by lazy { LauncherIconManager(appContext) }
    private val contactPreferences by lazy {
        com.tk.quicksearch.search.data.preferences.ContactPreferences(
                appContext,
        )
    }

    private val permissionManager by lazy {
        PermissionManager(contactRepository, calendarRepository, fileRepository, userPreferences)
    }
    private val searchOperations by lazy { SearchOperations(contactRepository) }

    // =========================================================================
    // Four focused sub-state flows (the core GC-pressure fix).
    //
    // WHY: The old single _uiState had 70+ fields. Every keystroke triggered
    //   5+ full copies of that object → severe GC pressure on lower-end devices.
    //   Now the per-keystroke hot path only copies SearchResultsState (~30 fields).
    //   SearchPermissionState, SearchFeatureState, and SearchUiConfigState are
    //   only copied when the user changes settings — not during typing.
    //
    // HOW the public API is preserved:
    //   uiState: StateFlow<SearchUiState> is assembled via combine() below so
    //   every consumer file continues to work with zero changes.
    // =========================================================================

    // Hot path — updated on every keystroke
    private val _resultsState = MutableStateFlow(initialResultsState)
    val resultsState: StateFlow<SearchResultsState> = _resultsState.asStateFlow()

    // Updated only when OS grants/revokes a permission
    private val _permissionState = MutableStateFlow(SearchPermissionState())
    val permissionState: StateFlow<SearchPermissionState> = _permissionState.asStateFlow()

    // Updated only when the user changes settings
    private val _featureState =
            MutableStateFlow(
                    SearchFeatureState(
                            isSearchEngineAliasSuffixEnabled =
                                    startupPreferencesReader.isSearchEngineAliasSuffixEnabled(),
                            isAliasTriggerAfterSpaceEnabled =
                                    startupPreferencesReader.isAliasTriggerAfterSpaceEnabled(),
                    ),
            )
    val featureState: StateFlow<SearchFeatureState> = _featureState.asStateFlow()

    // Updated only when appearance/display prefs change
    private val _configState = MutableStateFlow(initialConfigState)
    val configState: StateFlow<SearchUiConfigState> = _configState.asStateFlow()

    // Emits once after each external navigation action (app launch, contact open, etc.)
    // UI collects this to trigger auto-close when the setting is enabled.
    private val _externalNavigationEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val externalNavigationEvent: SharedFlow<Unit> = _externalNavigationEvent.asSharedFlow()

    /**
     * Backward-compatible aggregate StateFlow. All existing consumer files (SearchRoute,
     * SettingsRoute, etc.) continue to collect this unchanged. It is rebuilt via combine() whenever
     * any sub-state changes.
     */
    val uiState: StateFlow<SearchUiState> =
            combine(
                            _resultsState,
                            _permissionState,
                            _featureState,
                            _configState,
                    ) { results, permissions, features, config ->
                        SearchUiState(
                                results = results,
                                permissions = permissions,
                                features = features,
                                config = config,
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.Eagerly,
                            initialValue =
                                            SearchUiState(
                                            results = initialResultsState,
                                            permissions = SearchPermissionState(),
                                            features =
                                                    SearchFeatureState(
                                                            isSearchEngineAliasSuffixEnabled =
                                                                    startupPreferencesReader
                                                                            .isSearchEngineAliasSuffixEnabled(),
                                                            isAliasTriggerAfterSpaceEnabled =
                                                                    startupPreferencesReader
                                                                            .isAliasTriggerAfterSpaceEnabled(),
                                                    ),
                                            config = initialConfigState,
                                    ),
                    )

    // Cache searchable apps to avoid re-computing on every query change
    @Volatile private var cachedAllSearchableApps: List<AppInfo> = emptyList()

    internal enum class ActiveInformationCard {
        DIRECT_SEARCH,
        CALCULATOR,
        CURRENCY_CONVERTER,
        WORD_CLOCK,
        DICTIONARY,
    }

    // Consolidated startup configuration loaded in single batch operation
    @Volatile private var startupConfig: StartupPreferencesFacade.StartupConfig? = null
    private val isAppShortcutsLoadInFlight = AtomicBoolean(false)
    private val hasStartedStartupPhases = AtomicBoolean(false)

    // Single-threaded dispatcher for Phase 3 background loads. Limits startup work
    // to one concurrent task so it never saturates Dispatchers.IO and doesn't
    // flood the main thread with simultaneous state updates while the user types.
    private val startupDispatcher = Dispatchers.IO.limitedParallelism(1)

    // -------------------------------------------------------------------------
    // Resume dirty-flags — set when something changes while the screen is in
    // the background, cleared after handleOnResume() services the flag.
    // -------------------------------------------------------------------------

    /**
     * Set to true whenever device settings or app-shortcut metadata is mutated
     * (pin/exclude/disable).  handleOnResume() will call refreshSettingsState()
     * and refreshAppShortcutsState() and then clear this flag.
     */
    @Volatile private var resumeNeedsStaticDataRefresh: Boolean = false

    /**
     * Epoch-ms of the last time we told searchEngineManager to refresh browser
     * targets.  We throttle this to at most once per BROWSER_REFRESH_INTERVAL_MS
     * because it hits the PackageManager on every call.
     */
    @Volatile private var lastBrowserTargetRefreshMs: Long = 0L
    private val uiStateMutationLock = ReentrantLock()

    // =========================================================================
    // Targeted sub-state updaters — call these instead of _uiState.update{}
    // to minimise allocation: only the relevant sub-state is copied.
    // =========================================================================

    /** Updates ONLY SearchResultsState. Per-keystroke hot path. */
    private fun updateResultsState(updater: (SearchResultsState) -> SearchResultsState) {
        uiStateMutationLock.withLock { _resultsState.update(updater) }
    }

    /** Updates ONLY SearchPermissionState. */
    private fun updatePermissionState(updater: (SearchPermissionState) -> SearchPermissionState) {
        uiStateMutationLock.withLock { _permissionState.update(updater) }
    }

    /** Updates ONLY SearchFeatureState. */
    private fun updateFeatureState(updater: (SearchFeatureState) -> SearchFeatureState) {
        uiStateMutationLock.withLock { _featureState.update(updater) }
    }

    /** Updates ONLY SearchUiConfigState. */
    private fun updateConfigState(updater: (SearchUiConfigState) -> SearchUiConfigState) {
        uiStateMutationLock.withLock { _configState.update(updater) }
    }

    /**
     * Legacy bridge: accepts the old-style (SearchUiState -> SearchUiState) lambda and routes the
     * changed fields to the correct sub-state.
     *
     * Used by all handler classes (PinningHandler, SecondarySearchOrchestrator, ManagementHandler,
     * etc.) that were written against the flat API. This bridge ensures they continue to compile
     * and work correctly while internally we only copy the relevant sub-state.
     *
     * Performance: builds a temporary SearchUiState from current sub-states, applies the transform,
     * then writes back only the fields that changed to the appropriate sub-state. No full 70-field
     * copy is retained after this function returns.
     */
    private fun updateUiState(updater: (SearchUiState) -> SearchUiState) {
        uiStateMutationLock.withLock {
            // Snapshot current composite state
            val currentResults = _resultsState.value
            val currentPermissions = _permissionState.value
            val currentFeatures = _featureState.value
            val currentConfig = _configState.value

            // Build a temporary flat state to pass to legacy updater
            val before =
                    SearchUiState(
                            results = currentResults,
                            permissions = currentPermissions,
                            features = currentFeatures,
                            config = currentConfig,
                    )

            val withHint = applyContactActionHint(before, updater(before))
            val after = if (isStartupComplete) applyVisibilityStates(withHint) else withHint

            // Write back each sub-state only if it actually changed
            val newResults = extractResultsState(after)
            if (newResults != currentResults) _resultsState.value = newResults

            val newPermissions = extractPermissionState(after)
            if (newPermissions != currentPermissions) _permissionState.value = newPermissions

            val newFeatures = extractFeatureState(after)
            if (newFeatures != currentFeatures) _featureState.value = newFeatures

            val newConfig = extractConfigState(after)
            if (newConfig != currentConfig) _configState.value = newConfig
        }
    }

    // -------------------------------------------------------------------------
    // Extraction helpers: pull fields out of a flat SearchUiState into sub-states
    // -------------------------------------------------------------------------

    private fun extractResultsState(s: SearchUiState) =
            SearchResultsState(
                    query = s.query,
                    recentApps = s.recentApps,
                    searchResults = s.searchResults,
                    pinnedApps = s.pinnedApps,
                    allApps = s.allApps,
                    suggestionExcludedApps = s.suggestionExcludedApps,
                    resultExcludedApps = s.resultExcludedApps,
                    indexedAppCount = s.indexedAppCount,
                    cacheLastUpdatedMillis = s.cacheLastUpdatedMillis,
                    appShortcutResults = s.appShortcutResults,
                    allAppShortcuts = s.allAppShortcuts,
                    pinnedAppShortcuts = s.pinnedAppShortcuts,
                    excludedAppShortcuts = s.excludedAppShortcuts,
                    contactResults = s.contactResults,
                    pinnedContacts = s.pinnedContacts,
                    excludedContacts = s.excludedContacts,
                    fileResults = s.fileResults,
                    pinnedFiles = s.pinnedFiles,
                    excludedFiles = s.excludedFiles,
                    settingResults = s.settingResults,
                    appSettingResults = s.appSettingResults,
                    allDeviceSettings = s.allDeviceSettings,
                    pinnedSettings = s.pinnedSettings,
                    excludedSettings = s.excludedSettings,
                    calendarEvents = s.calendarEvents,
                    pinnedCalendarEvents = s.pinnedCalendarEvents,
                    excludedCalendarEvents = s.excludedCalendarEvents,
                    screenState = s.screenState,
                    appsSectionState = s.appsSectionState,
                    appShortcutsSectionState = s.appShortcutsSectionState,
                    contactsSectionState = s.contactsSectionState,
                    filesSectionState = s.filesSectionState,
                    settingsSectionState = s.settingsSectionState,
                    calendarSectionState = s.calendarSectionState,
                    searchEnginesState = s.searchEnginesState,
                    calculatorState = s.calculatorState,
                    currencyConverterState = s.currencyConverterState,
                    wordClockState = s.wordClockState,
                    dictionaryState = s.dictionaryState,
                    DirectSearchState = s.DirectSearchState,
                    webSuggestions = s.webSuggestions,
                    webSuggestionWasSelected = s.webSuggestionWasSelected,
                    isSecondarySearchInProgress = s.isSecondarySearchInProgress,
                    detectedShortcutTarget = s.detectedShortcutTarget,
                    detectedAliasSearchSection = s.detectedAliasSearchSection,
                    isCurrencyConverterAliasMode = s.isCurrencyConverterAliasMode,
                    isWordClockAliasMode = s.isWordClockAliasMode,
                    isDictionaryAliasMode = s.isDictionaryAliasMode,
                    recentItems = s.recentItems,
                    aliasRecentItems = s.aliasRecentItems,
                    nicknameUpdateVersion = s.nicknameUpdateVersion,
                    contactActionsVersion = s.contactActionsVersion,
            )

    private fun extractPermissionState(s: SearchUiState) =
            SearchPermissionState(
                    hasUsagePermission = s.hasUsagePermission,
                    hasContactPermission = s.hasContactPermission,
                    hasFilePermission = s.hasFilePermission,
                    hasCalendarPermission = s.hasCalendarPermission,
                    hasCallPermission = s.hasCallPermission,
                    hasWallpaperPermission = s.hasWallpaperPermission,
                    wallpaperAvailable = s.wallpaperAvailable,
                    messagingApp = s.messagingApp,
                    callingApp = s.callingApp,
                    isWhatsAppInstalled = s.isWhatsAppInstalled,
                    isTelegramInstalled = s.isTelegramInstalled,
                    isSignalInstalled = s.isSignalInstalled,
                    isGoogleMeetInstalled = s.isGoogleMeetInstalled,
            )

    private fun extractFeatureState(s: SearchUiState) =
            SearchFeatureState(
                    searchTargetsOrder = s.searchTargetsOrder,
                    disabledSearchTargetIds = s.disabledSearchTargetIds,
                    isSearchEngineCompactMode = s.isSearchEngineCompactMode,
                    searchEngineCompactRowCount = s.searchEngineCompactRowCount,
                    isSearchEngineAliasSuffixEnabled = s.isSearchEngineAliasSuffixEnabled,
                    isAliasTriggerAfterSpaceEnabled = s.isAliasTriggerAfterSpaceEnabled,
                    amazonDomain = s.amazonDomain,
                    shortcutsEnabled = s.shortcutsEnabled,
                    shortcutCodes = s.shortcutCodes,
                    shortcutEnabled = s.shortcutEnabled,
                    disabledAppShortcutIds = s.disabledAppShortcutIds,
                    disabledSections = s.disabledSections,
                    hasGeminiApiKey = s.hasGeminiApiKey,
                    geminiApiKeyLast4 = s.geminiApiKeyLast4,
                    personalContext = s.personalContext,
                    geminiModel = s.geminiModel,
                    geminiGroundingEnabled = s.geminiGroundingEnabled,
                    availableGeminiModels = s.availableGeminiModels,
                    webSuggestionsEnabled = s.webSuggestionsEnabled,
                    webSuggestionsCount = s.webSuggestionsCount,
                    calculatorEnabled = s.calculatorEnabled,
                    unitConverterEnabled = s.unitConverterEnabled,
                    dateCalculatorEnabled = s.dateCalculatorEnabled,
                    currencyConverterEnabled = s.currencyConverterEnabled,
                    wordClockEnabled = s.wordClockEnabled,
                    dictionaryEnabled = s.dictionaryEnabled,
                    recentQueriesEnabled = s.recentQueriesEnabled,
                    hasDismissedSearchHistoryTip = s.hasDismissedSearchHistoryTip,
                    directDialEnabled = s.directDialEnabled,
                    shouldShowUsagePermissionBanner = s.shouldShowUsagePermissionBanner,
            )

    private fun extractConfigState(s: SearchUiState) =
            SearchUiConfigState(
                    startupPhase = s.startupPhase,
                    isInitializing = s.isInitializing,
                    isLoading = s.isLoading,
                    errorMessage = s.errorMessage,
                    isStartupCoreSurfaceReady = s.isStartupCoreSurfaceReady,
                    showWallpaperBackground = s.showWallpaperBackground,
                    wallpaperBackgroundAlpha = s.wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = s.wallpaperBlurRadius,
                    appTheme = s.appTheme,
                    overlayThemeIntensity = s.overlayThemeIntensity,
                    appThemeMode = s.appThemeMode,
                    backgroundSource = s.backgroundSource,
                    customImageUri = s.customImageUri,
                    startupBackgroundPreviewPath = s.startupBackgroundPreviewPath,
                    overlayModeEnabled = s.overlayModeEnabled,
                    overlayBlurEffectEnabled = s.overlayBlurEffectEnabled,
                    oneHandedMode = s.oneHandedMode,
                    bottomSearchBarEnabled = s.bottomSearchBarEnabled,
                    topResultIndicatorEnabled = s.topResultIndicatorEnabled,
                    wallpaperAccentEnabled = s.wallpaperAccentEnabled,
                    openKeyboardOnLaunch = s.openKeyboardOnLaunch,
                    clearQueryOnLaunch = s.clearQueryOnLaunch,
                    autoCloseOverlay = s.autoCloseOverlay,
                    fontScaleMultiplier = s.fontScaleMultiplier,
                    showAppLabels = s.showAppLabels,
                    phoneAppGridColumns = s.phoneAppGridColumns,
                    appIconShape = s.appIconShape,
                    launcherAppIcon = s.launcherAppIcon,
                    themedIconsEnabled = s.themedIconsEnabled,
                    appSuggestionsEnabled = s.appSuggestionsEnabled,
                    selectedIconPackPackage = s.selectedIconPackPackage,
                    availableIconPacks = s.availableIconPacks,
                    enabledFileTypes = s.enabledFileTypes,
                    showFolders = s.showFolders,
                    showSystemFiles = s.showSystemFiles,
                    folderWhitelistPatterns = s.folderWhitelistPatterns,
                    folderBlacklistPatterns = s.folderBlacklistPatterns,
                    excludedFileExtensions = s.excludedFileExtensions,
                    showSearchEngineOnboarding = s.showSearchEngineOnboarding,
                    showStartSearchingOnOnboarding = s.showStartSearchingOnOnboarding,
                    showSearchBarWelcomeAnimation = s.showSearchBarWelcomeAnimation,
                    showContactActionHint = s.showContactActionHint,
                    hasSeenOverlayAssistantTip = s.hasSeenOverlayAssistantTip,
                    showReleaseNotesDialog = s.showReleaseNotesDialog,
                    releaseNotesVersionName = s.releaseNotesVersionName,
                    phoneNumberSelection = s.phoneNumberSelection,
                    directDialChoice = s.directDialChoice,
                    contactMethodsBottomSheet = s.contactMethodsBottomSheet,
                    contactActionPickerRequest = s.contactActionPickerRequest,
                    pendingDirectCallNumber = s.pendingDirectCallNumber,
                    pendingThirdPartyCall = s.pendingThirdPartyCall,
            )

    private fun applyContactActionHint(
            previous: SearchUiState,
            updated: SearchUiState,
    ): SearchUiState {
        val hasContacts = updated.contactResults.isNotEmpty() || updated.pinnedContacts.isNotEmpty()
        val shouldShowHint =
                hasContacts &&
                        updated.hasContactPermission &&
                        !updated.showContactActionHint &&
                        !userPreferences.hasSeenContactActionHint()

        return if (shouldShowHint) {
            updated.copy(showContactActionHint = true)
        } else {
            updated
        }
    }

    private val handlers by lazy {
        SearchHandlerContainer(
            application = application,
            appContext = appContext,
            userPreferences = userPreferences,
            scope = viewModelScope,
            repository = repository,
            contactRepository = contactRepository,
            fileRepository = fileRepository,
            calendarRepository = calendarRepository,
            appShortcutRepository = appShortcutRepository,
            settingsShortcutRepository = settingsShortcutRepository,
            appSettingsRepository = appSettingsRepository,
            permissionManager = permissionManager,
            searchOperations = searchOperations,
            startupDispatcher = startupDispatcher,
            updateUiState = this::updateUiState,
            updateConfigState = this::updateConfigState,
            refreshSecondarySearches = this::refreshSecondarySearches,
            refreshAppShortcutsState = this::refreshAppShortcutsState,
            refreshAppSuggestions = this::refreshAppSuggestions,
            refreshDerivedState = this::refreshDerivedState,
            showToast = this::showToast,
            currentStateProvider = { uiState.value },
            isLowRamDevice = isLowRamDevice(appContext),
        )
    }

    private val startupCoordinator by lazy {
        SearchStartupCoordinator(
            scope = viewModelScope,
            hasStartedStartupPhases = hasStartedStartupPhases,
            instantStartupSurfaceEnabled = instantStartupSurfaceEnabled,
            updateStartupPhase = { phase -> updateConfigState { it.copy(startupPhase = phase) } },
            preloadBackgroundForInitialSearchSurface = this::preloadBackgroundForInitialSearchSurface,
            loadCacheAndMinimalPrefsBlock = this::loadCacheAndMinimalPrefs,
            loadRemainingStartupPreferencesBlock = this::loadRemainingStartupPreferences,
            launchDeferredInitializationBlock = this::launchDeferredInitialization,
        )
    }

    private val toolCoordinator by lazy {
        SearchToolCoordinator(
            appContext = appContext,
            scope = viewModelScope,
            workerDispatcher = Dispatchers.Default,
            userPreferences = userPreferences,
            calculatorHandler = handlers.calculatorHandler,
            unitConverterHandler = handlers.unitConverterHandler,
            dateCalculatorHandler = handlers.dateCalculatorHandler,
            currencyConverterHandler = handlers.currencyConverterHandler,
            wordClockHandler = handlers.wordClockHandler,
            dictionaryHandler = handlers.dictionaryHandler,
            toolAliasStateProvider = {
                ToolAliasState(
                    lockedToolMode = lockedToolMode,
                    lockedCurrencyConverterAlias = lockedCurrencyConverterAlias,
                    lockedWordClockAlias = lockedWordClockAlias,
                    lockedDictionaryAlias = lockedDictionaryAlias,
                )
            },
            hasGeminiApiKeyProvider = { _featureState.value.hasGeminiApiKey },
            currentQueryProvider = { _resultsState.value.query },
            clearInformationCardsExcept = this::clearInformationCardsExcept,
            updateResultsState = this::updateResultsState,
            showToast = this::showToast,
        )
    }

    private val queryCoordinator by lazy {
        SearchQueryCoordinator(
            scope = viewModelScope,
            workerDispatcher = Dispatchers.Default,
            handlers = handlers,
            toolCoordinator = toolCoordinator,
            userPreferences = userPreferences,
            appSearchDebounceMs = APP_SEARCH_DEBOUNCE_MS,
            aliasStateProvider = {
                SearchQueryAliasState(
                    lockedShortcutTarget = lockedShortcutTarget,
                    lockedAliasSearchSection = lockedAliasSearchSection,
                    lockedToolMode = lockedToolMode,
                    lockedCurrencyConverterAlias = lockedCurrencyConverterAlias,
                    lockedWordClockAlias = lockedWordClockAlias,
                    lockedDictionaryAlias = lockedDictionaryAlias,
                )
            },
            updateAliasState = { state ->
                lockedShortcutTarget = state.lockedShortcutTarget
                lockedAliasSearchSection = state.lockedAliasSearchSection
                lockedToolMode = state.lockedToolMode
                lockedCurrencyConverterAlias = state.lockedCurrencyConverterAlias
                lockedWordClockAlias = state.lockedWordClockAlias
                lockedDictionaryAlias = state.lockedDictionaryAlias
            },
            currentResultsStateProvider = { _resultsState.value },
            updateUiState = this::updateUiState,
            updateResultsState = this::updateResultsState,
            clearInformationCardsExcept = this::clearInformationCardsExcept,
            getSearchableAppsSnapshot = this::getSearchableAppsSnapshot,
            getGridItemCount = this::getGridItemCount,
            loadAppShortcuts = this::loadAppShortcuts,
            refreshRecentItems = this::refreshRecentItems,
            refreshAliasRecentItems = this::refreshAliasRecentItems,
        )
    }

    val appManager get() = handlers.appManager
    val contactManager get() = handlers.contactManager
    val fileManager get() = handlers.fileManager
    val settingsManager get() = handlers.settingsManager
    val calendarManager get() = handlers.calendarManager
    val appShortcutManager get() = handlers.appShortcutManager
    val searchEngineManager get() = handlers.searchEngineManager
    val sectionManager get() = handlers.sectionManager
    val iconPackHandler get() = handlers.iconPackHandler
    val messagingHandler get() = handlers.messagingHandler
    val releaseNotesHandler get() = handlers.releaseNotesHandler
    private val pinningHandler get() = handlers.pinningHandler
    val webSuggestionHandler get() = handlers.webSuggestionHandler
    val calculatorHandler get() = handlers.calculatorHandler
    val unitConverterHandler get() = handlers.unitConverterHandler
    val dateCalculatorHandler get() = handlers.dateCalculatorHandler
    private val currencyConverterHandler get() = handlers.currencyConverterHandler
    private val wordClockHandler get() = handlers.wordClockHandler
    private val dictionaryHandler get() = handlers.dictionaryHandler
    val appSearchManager get() = handlers.appSearchManager
    val settingsSearchHandler get() = handlers.settingsSearchHandler
    val appShortcutSearchHandler get() = handlers.appShortcutSearchHandler
    val appSettingsSearchHandler get() = handlers.appSettingsSearchHandler
    val fileSearchHandler get() = handlers.fileSearchHandler
    val directSearchHandler get() = handlers.directSearchHandler
    val aliasHandler get() = handlers.aliasHandler
    private val unifiedSearchHandler get() = handlers.unifiedSearchHandler
    private val secondarySearchOrchestrator get() = handlers.secondarySearchOrchestrator
    private val navigationHandler get() = handlers.navigationHandler
    private val contactActionHandler get() = handlers.contactActionHandler

    private var prefCache = SearchPreferenceCache(clearQueryOnLaunch = initialConfigState.clearQueryOnLaunch)
    private var enabledFileTypes: Set<FileType> = emptySet()
    private var showFolders: Boolean = false
    private var showSystemFiles: Boolean = false
    private var folderWhitelistPatterns: Set<String> = emptySet()
    private var folderBlacklistPatterns: Set<String> = emptySet()
    private var excludedFileExtensions: Set<String> = emptySet()
    private var oneHandedMode: Boolean = false
    private var bottomSearchBarEnabled: Boolean = false
    private var topResultIndicatorEnabled: Boolean = true
    private var wallpaperAccentEnabled: Boolean = true
    private var openKeyboardOnLaunch: Boolean = true
    private var overlayModeEnabled: Boolean = false
    private var overlayBlurEffectEnabled: Boolean = false
    private var autoCloseOverlay: Boolean = true
    private var directDialEnabled: Boolean = false
    private var assistantLaunchVoiceModeEnabled: Boolean = false
    private var hasSeenDirectDialChoice: Boolean = false
    private var appSuggestionsEnabled: Boolean = true
    private var showAppLabels: Boolean = true
    private var phoneAppGridColumns: Int = com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS
    private var appIconShape: AppIconShape = AppIconShape.DEFAULT
    private var launcherAppIcon: LauncherAppIcon = LauncherAppIcon.AUTO
    private var themedIconsEnabled: Boolean = false
    private var wallpaperBackgroundAlpha: Float = UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA
    private var wallpaperBlurRadius: Float = UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS
    private var appTheme: AppTheme = AppTheme.MONOCHROME
    private var overlayThemeIntensity: Float = UiPreferences.DEFAULT_OVERLAY_THEME_INTENSITY
    private var fontScaleMultiplier: Float = UiPreferences.DEFAULT_FONT_SCALE_MULTIPLIER
    private var backgroundSource: BackgroundSource = BackgroundSource.THEME
    private var customImageUri: String? = null
    private var lockedShortcutTarget: SearchTarget? = null
    private var lockedAliasSearchSection: SearchSection? = null
    private var lockedToolMode: SearchToolType? = null
    private var lockedCurrencyConverterAlias: Boolean = false
    private var lockedWordClockAlias: Boolean = false
    private var lockedDictionaryAlias: Boolean = false
    private var clearQueryOnLaunch: Boolean = initialConfigState.clearQueryOnLaunch
    private var amazonDomain: String? = null
    private var pendingNavigationClear: Boolean = false
    private var isStartupComplete: Boolean = false

    private fun applyPreferenceCacheToLegacyVars() {
        enabledFileTypes = prefCache.enabledFileTypes
        showFolders = prefCache.showFolders
        showSystemFiles = prefCache.showSystemFiles
        folderWhitelistPatterns = prefCache.folderWhitelistPatterns
        folderBlacklistPatterns = prefCache.folderBlacklistPatterns
        excludedFileExtensions = prefCache.excludedFileExtensions
        oneHandedMode = prefCache.oneHandedMode
        bottomSearchBarEnabled = prefCache.bottomSearchBarEnabled
        topResultIndicatorEnabled = prefCache.topResultIndicatorEnabled
        wallpaperAccentEnabled = prefCache.wallpaperAccentEnabled
        openKeyboardOnLaunch = prefCache.openKeyboardOnLaunch
        overlayModeEnabled = prefCache.overlayModeEnabled
        overlayBlurEffectEnabled = prefCache.overlayBlurEffectEnabled
        autoCloseOverlay = prefCache.autoCloseOverlay
        directDialEnabled = prefCache.directDialEnabled
        assistantLaunchVoiceModeEnabled = prefCache.assistantLaunchVoiceModeEnabled
        hasSeenDirectDialChoice = prefCache.hasSeenDirectDialChoice
        appSuggestionsEnabled = prefCache.appSuggestionsEnabled
        showAppLabels = prefCache.showAppLabels
        phoneAppGridColumns = prefCache.phoneAppGridColumns
        appIconShape = prefCache.appIconShape
        launcherAppIcon = prefCache.launcherAppIcon
        themedIconsEnabled = prefCache.themedIconsEnabled
        wallpaperBackgroundAlpha = prefCache.wallpaperBackgroundAlpha
        wallpaperBlurRadius = prefCache.wallpaperBlurRadius
        appTheme = prefCache.appTheme
        overlayThemeIntensity = prefCache.overlayThemeIntensity
        fontScaleMultiplier = prefCache.fontScaleMultiplier
        backgroundSource = prefCache.backgroundSource
        customImageUri = prefCache.customImageUri
        lockedShortcutTarget = prefCache.lockedShortcutTarget
        lockedAliasSearchSection = prefCache.lockedAliasSearchSection
        lockedToolMode = prefCache.lockedToolMode
        lockedCurrencyConverterAlias = prefCache.lockedCurrencyConverterAlias
        lockedWordClockAlias = prefCache.lockedWordClockAlias
        lockedDictionaryAlias = prefCache.lockedDictionaryAlias
        clearQueryOnLaunch = prefCache.clearQueryOnLaunch
        amazonDomain = prefCache.amazonDomain
    }

    private fun onNavigationTriggered() {
        pendingNavigationClear = true
        _externalNavigationEvent.tryEmit(Unit)
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(getApplication(), messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hasContactPermission(): Boolean = contactRepository.hasPermission()

    private fun hasFilePermission(): Boolean = fileRepository.hasPermission()

    private fun hasCalendarPermission(): Boolean = calendarRepository.hasPermission()

    private fun hasCallPermission(): Boolean =
            PermissionHelper.checkCallPermission(getApplication())

    private fun hasWallpaperPermission(): Boolean =
            PermissionHelper.checkWallpaperPermission(getApplication())

    private var wallpaperAvailable: Boolean = false
    @Volatile private var shouldRecordPendingDirectSearchQueryInHistory: Boolean = true

    fun setWallpaperAvailable(available: Boolean) {
        if (wallpaperAvailable != available) {
            wallpaperAvailable = available
            updateUiState { it.copy(wallpaperAvailable = available) }
        }
    }

    fun markStartupCoreSurfaceReady() {
        if (!_configState.value.isStartupCoreSurfaceReady) {
            updateConfigState { it.copy(isStartupCoreSurfaceReady = true) }
        }
    }

    fun handleOnStop() {
        val shouldRetainDirectOrGeminiQueryOnStop = shouldRetainDirectOrGeminiQueryOnStop()
        val shouldClearQueryOnStop = _configState.value.clearQueryOnLaunch
        if (shouldRetainDirectOrGeminiQueryOnStop) {
            updateConfigState { it.copy(selectRetainedQuery = true) }
        } else if (shouldClearQueryOnStop) {
            clearQuery()
        } else if (pendingNavigationClear && _resultsState.value.query.isNotEmpty()) {
            updateConfigState { it.copy(selectRetainedQuery = true) }
        }
        if (pendingNavigationClear) {
            pendingNavigationClear = false
        }
    }

    private fun shouldRetainDirectOrGeminiQueryOnStop(): Boolean {
        val state = _resultsState.value
        if (state.query.isBlank()) return false
        return state.DirectSearchState.status != DirectSearchStatus.Idle ||
            state.currencyConverterState.status != CurrencyConverterStatus.Idle ||
            state.wordClockState.status != WordClockStatus.Idle ||
            state.dictionaryState.status != DictionaryStatus.Idle
    }

    private fun initializeServices() {
        handlers.initializeServices(
            getCallingApp = { _permissionState.value.callingApp },
            getMessagingApp = { messagingHandler.messagingApp },
            getDirectDialEnabled = { directDialEnabled },
            getHasSeenDirectDialChoice = { hasSeenDirectDialChoice },
            getCurrentState = { uiState.value },
            clearQuery = this::onNavigationTriggered,
            externalNavigation = { _externalNavigationEvent.tryEmit(Unit) },
            onRequestDirectSearch = { query, addToSearchHistory ->
                shouldRecordPendingDirectSearchQueryInHistory = addToSearchHistory
                directSearchHandler.requestDirectSearch(query)
            },
            showToastText = { resId -> showToast(resId) },
        )
    }

    init {
        FeatureFlags.initialize(appContext)
        userPreferences.ensureCalendarSectionDefaultDisabledMigration()
        // Initialize services after all handlers are available
        initializeServices()

        setupDirectSearchStateListener()
    }

    fun startStartupPhasesAfterFirstFrame() {
        startupCoordinator.startStartupPhases()
    }

    /**
     * Keep phase-0 shell visible until the selected background source is available in memory so
     * the first SearchScreen frame can render search bar + background together.
     */
    private suspend fun preloadBackgroundForInitialSearchSurface() {
        val context = getApplication<Application>().applicationContext
        when (userPreferences.getBackgroundSource()) {
            BackgroundSource.SYSTEM_WALLPAPER -> {
                WallpaperUtils.getWallpaperBitmapResult(context)
            }
            BackgroundSource.CUSTOM_IMAGE -> {
                WallpaperUtils.getOverlayCustomImageBitmap(
                    context = context,
                    uriString = userPreferences.getCustomImageUri(),
                )
            }
            BackgroundSource.THEME -> Unit
        }
    }

    private fun clearInformationCardsExcept(activeCard: ActiveInformationCard) {
        toolCoordinator.cancelInactive(activeCard)
        if (
            activeCard != ActiveInformationCard.DIRECT_SEARCH &&
                _resultsState.value.DirectSearchState.status != DirectSearchStatus.Idle
        ) {
            directSearchHandler.clearDirectSearchState()
        }

        updateResultsState { state ->
            state.copy(
                DirectSearchState =
                    if (activeCard == ActiveInformationCard.DIRECT_SEARCH) {
                        state.DirectSearchState
                    } else {
                        DirectSearchState()
                    },
                calculatorState =
                    if (activeCard == ActiveInformationCard.CALCULATOR) {
                        state.calculatorState
                    } else {
                        CalculatorState()
                    },
                currencyConverterState =
                    if (activeCard == ActiveInformationCard.CURRENCY_CONVERTER) {
                        state.currencyConverterState
                    } else {
                        CurrencyConverterState()
                    },
                wordClockState =
                    if (activeCard == ActiveInformationCard.WORD_CLOCK) {
                        state.wordClockState
                    } else {
                        WordClockState()
                    },
                dictionaryState =
                    if (activeCard == ActiveInformationCard.DICTIONARY) {
                        state.dictionaryState
                    } else {
                        DictionaryState()
                    },
            )
        }
    }

    private fun setupDirectSearchStateListener() {
        viewModelScope.launch {
            directSearchHandler.directSearchState.collect { dsState ->
                if (dsState.status == DirectSearchStatus.Loading) {
                    val activeQuery = dsState.activeQuery?.trim().orEmpty()
                    if (shouldRecordPendingDirectSearchQueryInHistory && activeQuery.isNotEmpty()) {
                        userPreferences.addRecentItem(RecentSearchEntry.Query(activeQuery))
                    }
                    shouldRecordPendingDirectSearchQueryInHistory = true
                }
                if (dsState.status != DirectSearchStatus.Idle) {
                    clearInformationCardsExcept(ActiveInformationCard.DIRECT_SEARCH)
                }
                updateResultsState { it.copy(DirectSearchState = dsState) }
            }
        }
    }

    /** Phase 1: Load critical data (cache and essential prefs) */

    /** Phase 1: Load ONLY what's needed for the first paint (cache + minimal config) */
    private suspend fun loadCacheAndMinimalPrefs() {
        // Load consolidated startup config in single batch operation
        val startupConfig = userPreferences.loadStartupConfig()
        val startupPrefs = startupConfig.startupPreferences

        prefCache =
            SearchPreferenceCache.from(
                config = startupConfig,
                wallpaperAccentEnabled = userPreferences.isWallpaperAccentEnabled(),
                assistantLaunchVoiceModeEnabled = userPreferences.isAssistantLaunchVoiceModeEnabled(),
            )
        applyPreferenceCacheToLegacyVars()

        // Load cached data - this is the critical path for content
        // This is just a fast JSON parse
        val cachedAppsList = runCatching { repository.loadCachedApps() }.getOrNull()
        val hasUsagePermission = repository.hasUsageAccess()
        val hasContactPermission = hasContactPermission()
        val hasFilePermission = hasFilePermission()
        val hasCalendarPermission = hasCalendarPermission()
        val hasCallPermission = hasCallPermission()
        val hasWallpaperPermission = hasWallpaperPermission()
        val disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds()

        withContext(Dispatchers.Main) {
            updateConfigState {
                it.copy(
                        oneHandedMode = oneHandedMode,
                        bottomSearchBarEnabled = bottomSearchBarEnabled,
                        topResultIndicatorEnabled = topResultIndicatorEnabled,
                        wallpaperAccentEnabled = wallpaperAccentEnabled,
                        openKeyboardOnLaunch = openKeyboardOnLaunch,
                        clearQueryOnLaunch = clearQueryOnLaunch,
                        autoCloseOverlay = autoCloseOverlay,
                        overlayBlurEffectEnabled = overlayBlurEffectEnabled,
                        showWallpaperBackground = backgroundSource != BackgroundSource.THEME,
                        wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                        wallpaperBlurRadius = wallpaperBlurRadius,
                        appTheme = appTheme,
                        overlayThemeIntensity = overlayThemeIntensity,
                        backgroundSource = backgroundSource,
                        customImageUri = customImageUri,
                        appIconShape = appIconShape,
                        launcherAppIcon = launcherAppIcon,
                        themedIconsEnabled = themedIconsEnabled,
                        // We don't have full prefs yet, so keep initializing flag true
                        // but show the apps we found in cache
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

            if (cachedAppsList != null && cachedAppsList.isNotEmpty()) {
                initializeWithCacheMinimal(cachedAppsList, hasUsagePermission)
            }
        }

        // Store the full startup config for Phase 2
        this.startupConfig = startupConfig
        applyLauncherIconSelection()

        // Prefetch icons in background (non-blocking) after UI is shown
        // Icons will lazy-load via rememberAppIcon() with placeholders until ready
        if (cachedAppsList != null && cachedAppsList.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                if (userPreferences.areAppSuggestionsEnabled()) {
                    val visibleApps =
                            extractSuggestedApps(
                                    apps = cachedAppsList,
                                    limit = getGridItemCount(),
                                    hasUsagePermission = hasUsagePermission,
                            )
                    val iconPack = userPreferences.getSelectedIconPackPackage()
                    prefetchAppIcons(
                            context = getApplication(),
                            packageNames = visibleApps.map { it.packageName },
                            iconPackPackage = iconPack,
                    )
                }
            }
        }
    }

    /** Phase 2: Load the rest of the startup preferences and compute derived state */
    private suspend fun loadRemainingStartupPreferences() {
        // Use pre-loaded startup config from Phase 1 (already batched)
        val startupPrefs =
                startupConfig?.startupPreferences ?: userPreferences.getStartupPreferences()

        // Apply preferences
        withContext(Dispatchers.Main) {
            applyStartupPreferences(startupPrefs)

            // Sync handlers with loaded prefs
            appSearchManager.setSortAppsByUsage(true)
        }

        // Compute heavier derived startup state off the main thread.
        val lastUpdated = startupConfig?.cachedAppsLastUpdate ?: repository.cacheLastUpdatedMillis()
        withContext(Dispatchers.Default) { refreshDerivedState(lastUpdated = lastUpdated, isLoading = false) }

        // Fully initialized now.
        withContext(Dispatchers.Main) { updateConfigState { it.copy(isInitializing = false) } }
    }

    private fun applyStartupPreferences(prefs: StartupPreferencesFacade.StartupPreferences) {
        prefCache =
            SearchPreferenceCache.from(
                prefs = prefs,
                wallpaperAccentEnabled = userPreferences.isWallpaperAccentEnabled(),
                assistantLaunchVoiceModeEnabled = userPreferences.isAssistantLaunchVoiceModeEnabled(),
            )
        applyPreferenceCacheToLegacyVars()

        updateConfigState {
            it.copy(
                    enabledFileTypes = enabledFileTypes,
                    oneHandedMode = oneHandedMode,
                    bottomSearchBarEnabled = bottomSearchBarEnabled,
                    topResultIndicatorEnabled = topResultIndicatorEnabled,
                    openKeyboardOnLaunch = openKeyboardOnLaunch,
                    clearQueryOnLaunch = clearQueryOnLaunch,
                    autoCloseOverlay = autoCloseOverlay,
                    overlayModeEnabled = overlayModeEnabled,
                    overlayBlurEffectEnabled = overlayBlurEffectEnabled,
                    appSuggestionsEnabled = appSuggestionsEnabled,
                    showAppLabels = showAppLabels,
                    phoneAppGridColumns = phoneAppGridColumns,
                    appIconShape = appIconShape,
                    launcherAppIcon = launcherAppIcon,
                    themedIconsEnabled = themedIconsEnabled,
                    showWallpaperBackground = backgroundSource != BackgroundSource.THEME,
                    wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = wallpaperBlurRadius,
                    appTheme = appTheme,
                    overlayThemeIntensity = overlayThemeIntensity,
                    fontScaleMultiplier = fontScaleMultiplier,
                    backgroundSource = backgroundSource,
                    customImageUri = customImageUri,
                    showFolders = showFolders,
                    showSystemFiles = showSystemFiles,
                    folderWhitelistPatterns = folderWhitelistPatterns,
                    folderBlacklistPatterns = folderBlacklistPatterns,
                    excludedFileExtensions = excludedFileExtensions,
                    hasSeenOverlayAssistantTip = userPreferences.hasSeenOverlayAssistantTip(),
            )
        }
        updateFeatureState {
            it.copy(
                    amazonDomain = amazonDomain,
                    directDialEnabled = directDialEnabled,
                    assistantLaunchVoiceModeEnabled = assistantLaunchVoiceModeEnabled,
                    disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds(),
                    recentQueriesEnabled = prefs.searchHistoryEnabled,
                    webSuggestionsCount = userPreferences.getWebSuggestionsCount(),
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner(),
                    hasDismissedSearchHistoryTip = userPreferences.hasDismissedSearchHistoryTip(),
            )
        }

        if (!prefs.searchHistoryEnabled) {
            userPreferences.clearRecentQueries()
        }

        applyLauncherIconSelection()

        // Load recent queries on startup if enabled
        refreshRecentItems()
        saveStartupSurfaceSnapshotAsync()
    }

    private fun initializeWithCacheMinimal(
            cachedAppsList: List<AppInfo>,
            hasUsagePermission: Boolean,
    ) {
        appSearchManager.initCache(cachedAppsList)
        val lastUpdated = repository.cacheLastUpdatedMillis()
        val suggestionsEnabled = userPreferences.areAppSuggestionsEnabled()
        val startupPrefs = startupConfig?.startupPreferences
        val labelsEnabled = startupPrefs?.showAppLabels ?: userPreferences.shouldShowAppLabels()
        val columnsForPhone = startupPrefs?.phoneAppGridColumns ?: userPreferences.getPhoneAppGridColumns()

        // Just show the raw list of apps first!
        // Don't filter, don't sort, don't check pinned apps yet
        // This is the fastest possible way to get pixels on screen
        updateResultsState { state ->
            state.copy(
                    cacheLastUpdatedMillis = lastUpdated,
                    recentApps =
                            if (suggestionsEnabled) {
                                extractSuggestedApps(
                                        apps = cachedAppsList,
                                        limit = getGridItemCount(),
                                        hasUsagePermission = hasUsagePermission,
                                )
                            } else {
                                emptyList()
                            },
                    indexedAppCount = cachedAppsList.size,
            )
        }
        updateConfigState { state ->
            state.copy(
                    oneHandedMode = oneHandedMode,
                    bottomSearchBarEnabled = bottomSearchBarEnabled,
                    openKeyboardOnLaunch = openKeyboardOnLaunch,
                    appSuggestionsEnabled = suggestionsEnabled,
                    showAppLabels = labelsEnabled,
                    phoneAppGridColumns = columnsForPhone,
                    isStartupCoreSurfaceReady = true,
            )
        }
        saveStartupSurfaceSnapshotAsync()
    }

    /** Phase 2 & 3: Deferred initialization of background handlers */
    private fun launchDeferredInitialization() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Refresh usage access and permissions (affects features)
            refreshUsageAccess()
            refreshOptionalPermissions()

            // 2. Initialize messaging handler (needed for contacts)
            val messagingInfo =
                    getMessagingAppInfo(appSearchManager.cachedApps.map { it.packageName }.toSet())

            // Ensure SearchEngineManager is initialized on IO thread
            searchEngineManager.ensureInitialized()
            // Update alias map after search targets are initialized.
            val shortcutsState = aliasHandler.getInitialState()

            updateFeatureState { state ->
                state.copy(
                        // Now safely access lazy handlers
                        searchTargetsOrder = searchEngineManager.searchTargetsOrder,
                        disabledSearchTargetIds = searchEngineManager.disabledSearchTargetIds,
                        shortcutsEnabled = shortcutsState.shortcutsEnabled,
                        shortcutCodes = shortcutsState.shortcutCodes,
                        shortcutEnabled = shortcutsState.shortcutEnabled,
                        disabledSections = sectionManager.disabledSections,
                        isSearchEngineCompactMode = searchEngineManager.isSearchEngineCompactMode,
                        searchEngineCompactRowCount =
                                searchEngineManager.searchEngineCompactRowCount,
                        isSearchEngineAliasSuffixEnabled =
                                userPreferences.isSearchEngineAliasSuffixEnabled(),
                        isAliasTriggerAfterSpaceEnabled =
                                userPreferences.isAliasTriggerAfterSpaceEnabled(),
                        webSuggestionsEnabled = webSuggestionHandler.isEnabled,
                        calculatorEnabled = userPreferences.isCalculatorEnabled(),
                        unitConverterEnabled = userPreferences.isUnitConverterEnabled(),
                        dateCalculatorEnabled = userPreferences.isDateCalculatorEnabled(),
                        currencyConverterEnabled = userPreferences.isCurrencyConverterEnabled(),
                        wordClockEnabled = userPreferences.isWordClockEnabled(),
                        dictionaryEnabled = userPreferences.isDictionaryEnabled(),
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

            // 3. Start heavy background loads
            // Apps load on Dispatchers.IO immediately (parallel) for fastest app availability.
            // All other loads use startupDispatcher (single-threaded) so they run serially and
            // don't saturate the thread pool while the user is typing.
            launch(Dispatchers.IO) {
                delay(DEFERRED_APP_REFRESH_DELAY_MS)
                loadApps()
            }

            launch(Dispatchers.IO) { iconPackHandler.refreshIconPacks() }

            launch(startupDispatcher) {
                // Load pinned items first (fast) - contacts, files, app shortcuts, and device
                // settings
                pinningHandler.loadPinnedContactsAndFiles()
                pinningHandler.loadExcludedContactsAndFiles()
                loadPinnedAndExcludedCalendarEvents()

                // Load pinned app shortcuts from cache (fast)
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
                                    applyAppShortcutIconOverrides(
                                            pinnedAppShortcutsState.pinned,
                                            iconOverrides,
                                    ),
                            excludedAppShortcuts =
                                    applyAppShortcutIconOverrides(
                                            pinnedAppShortcutsState.excluded,
                                            iconOverrides,
                                    ),
                    )
                }

                // Load pinned device settings (fast)
                val pinnedSettingsState = settingsSearchHandler.getPinnedAndExcludedOnly()
                updateUiState { state ->
                    state.copy(
                            pinnedSettings = pinnedSettingsState.pinned,
                            excludedSettings = pinnedSettingsState.excluded,
                    )
                }
            }

            // Load full shortcuts/settings in background (slower, for search)
            launch(startupDispatcher) { loadSettingsShortcuts() }
            launch(startupDispatcher) { appSettingsSearchHandler.loadSettings() }

            launch(startupDispatcher) { loadAppShortcuts() }

            // 4. Release notes
            launch(Dispatchers.IO) {
                delay(DEFERRED_RELEASE_NOTES_DELAY_MS)
                releaseNotesHandler.checkForReleaseNotes()
            }

            // 5. Startup complete - update coarse UI state on main, then run heavy
            // refresh work off the main thread.
            withContext(Dispatchers.Main) {
                isStartupComplete = true
                updateUiState {
                    applyVisibilityStates(
                            it.copy(
                                    startupPhase = StartupPhase.COMPLETE,
                            ),
                    )
                }
            }
            withContext(Dispatchers.Default) { refreshDerivedState() }
            saveStartupSurfaceSnapshotAsync(forcePreviewRefresh = true)
        }
    }

    private fun shouldShowSearchBarWelcome(): Boolean {
        // Show if not seen before, regardless of app open count.
        // This ensures existing users also see the new feature once.
        if (userPreferences.consumeForceSearchBarWelcomeOnNextOpen()) {
            userPreferences.setHasSeenSearchBarWelcome(true)
            return true
        }

        val hasSeen = userPreferences.hasSeenSearchBarWelcome()

        if (!hasSeen) {
            userPreferences.setHasSeenSearchBarWelcome(true)
            return true
        }
        return false
    }

    fun requestSearchBarWelcomeAnimationFromOnboarding() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setForceSearchBarWelcomeOnNextOpen(true)
            userPreferences.setHasSeenSearchBarWelcome(true)
        }
        updateConfigState { it.copy(showSearchBarWelcomeAnimation = true) }
    }

    fun onSearchBarWelcomeAnimationCompleted() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setForceSearchBarWelcomeOnNextOpen(false)
        }
        updateConfigState { it.copy(showSearchBarWelcomeAnimation = false) }
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

    private data class MessagingAppInfo(
            val isWhatsAppInstalled: Boolean,
            val isTelegramInstalled: Boolean,
            val isSignalInstalled: Boolean,
            val messagingApp: MessagingApp,
            val isGoogleMeetInstalled: Boolean,
            val callingApp: CallingApp,
    )

    private fun resolveCallingApp(
            app: CallingApp,
            isWhatsAppInstalled: Boolean,
            isTelegramInstalled: Boolean,
            isSignalInstalled: Boolean,
            isGoogleMeetInstalled: Boolean,
    ): CallingApp =
            when (app) {
                CallingApp.WHATSAPP ->
                        if (isWhatsAppInstalled) CallingApp.WHATSAPP else CallingApp.CALL
                CallingApp.TELEGRAM ->
                        if (isTelegramInstalled) CallingApp.TELEGRAM else CallingApp.CALL
                CallingApp.SIGNAL -> if (isSignalInstalled) CallingApp.SIGNAL else CallingApp.CALL
                CallingApp.GOOGLE_MEET ->
                        if (isGoogleMeetInstalled) CallingApp.GOOGLE_MEET else CallingApp.CALL
                CallingApp.CALL -> CallingApp.CALL
            }

    fun setCalculatorEnabled(enabled: Boolean) {
        // Delegate to new CalculatorHandler once implemented
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setCalculatorEnabled(enabled)
            updateFeatureState { it.copy(calculatorEnabled = enabled) }
        }
    }

    fun setUnitConverterEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setUnitConverterEnabled(enabled)
            updateFeatureState { it.copy(unitConverterEnabled = enabled) }
        }
    }

    fun setDateCalculatorEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setDateCalculatorEnabled(enabled)
            updateFeatureState { it.copy(dateCalculatorEnabled = enabled) }
        }
    }

    fun setCurrencyConverterEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setCurrencyConverterEnabled(enabled)
            updateFeatureState { it.copy(currencyConverterEnabled = enabled) }
            if (!enabled) {
                updateResultsState { it.copy(currencyConverterState = CurrencyConverterState()) }
            }
        }
    }

    fun setWordClockEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setWordClockEnabled(enabled)
            updateFeatureState { it.copy(wordClockEnabled = enabled) }
            if (!enabled) {
                updateResultsState { it.copy(wordClockState = WordClockState()) }
            }
        }
    }

    fun setDictionaryEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setDictionaryEnabled(enabled)
            updateFeatureState { it.copy(dictionaryEnabled = enabled) }
            if (!enabled) {
                updateResultsState { it.copy(dictionaryState = DictionaryState()) }
            }
        }
    }

    fun dismissOverlayAssistantTip() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setHasSeenOverlayAssistantTip(true)
            updateConfigState { it.copy(hasSeenOverlayAssistantTip = true) }
        }
    }

    fun setAppSuggestionsEnabled(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setAppSuggestionsEnabled,
                stateUpdater = {
                    appSuggestionsEnabled = it
                    updateUiState { state -> state.copy(appSuggestionsEnabled = it) }
                    refreshAppSuggestions()
                    saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
                },
        )
    }

    fun setShowAppLabels(show: Boolean) {
        updateBooleanPreference(
                value = show,
                preferenceSetter = userPreferences::setShowAppLabels,
                stateUpdater = {
                    showAppLabels = it
                    updateUiState { state -> state.copy(showAppLabels = it) }
                    saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
                },
        )
    }

    fun setPhoneAppGridColumns(columns: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setPhoneAppGridColumns(columns)
            phoneAppGridColumns = columns
            updateConfigState { state -> state.copy(phoneAppGridColumns = columns) }
            refreshAppSuggestions()
            saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setWebSuggestionsEnabled(enabled: Boolean) = webSuggestionHandler.setEnabled(enabled)

    fun setWebSuggestionsCount(count: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setWebSuggestionsCount(count)
            updateFeatureState { it.copy(webSuggestionsCount = count) }
        }
    }

    fun setRecentQueriesEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setRecentQueriesEnabled(enabled)
            if (!enabled) {
                userPreferences.clearRecentQueries()
            }
            updateFeatureState { it.copy(recentQueriesEnabled = enabled) }
            updateResultsState {
                it.copy(recentItems = if (enabled) it.recentItems else emptyList())
            }

            // Refresh recent queries display if query is empty
            if (enabled && _resultsState.value.query.isEmpty()) {
                refreshRecentItems()
            }
        }
    }

    fun dismissSearchHistoryTip() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setSearchHistoryTipDismissed(true)
            updateFeatureState { it.copy(hasDismissedSearchHistoryTip = true) }
        }
    }

    private fun refreshRecentItems() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!_featureState.value.recentQueriesEnabled) {
                updateResultsState { it.copy(recentItems = emptyList()) }
                return@launch
            }

            val entries = userPreferences.getRecentItems().take(MAX_RECENT_ITEMS)

            val contactIds =
                    entries.filterIsInstance<RecentSearchEntry.Contact>()
                            .map { it.contactId }
                            .toSet()
            val fileUris = entries.filterIsInstance<RecentSearchEntry.File>().map { it.uri }.toSet()
            val settingIds =
                    entries.filterIsInstance<RecentSearchEntry.Setting>().map { it.id }.toSet()
            val shortcutKeys =
                    entries.filterIsInstance<RecentSearchEntry.AppShortcut>()
                            .map { it.shortcutKey }
                            .toSet()

            val contactsById =
                    contactRepository.getContactsByIds(contactIds).associateBy { it.contactId }
            val filesByUri =
                    fileRepository.getFilesByUris(fileUris).associateBy { it.uri.toString() }
            val settingsById = settingsSearchHandler.getSettingsByIds(settingIds)
            val shortcutsByKey = appShortcutSearchHandler.getShortcutsByKeys(shortcutKeys)

            // Get all pinned items to filter them out from recent items
            val pinnedPackages = userPreferences.getPinnedPackages()
            val pinnedContactIds = userPreferences.getPinnedContactIds()
            val pinnedFileUris = userPreferences.getPinnedFileUris()
            val pinnedSettingIds = userPreferences.getPinnedSettingIds()
            val pinnedAppShortcutIds = userPreferences.getPinnedAppShortcutIds()

            val items = buildList {
                entries.forEach { entry ->
                    when (entry) {
                        is RecentSearchEntry.Query -> {
                            add(RecentSearchItem.Query(entry.trimmedQuery))
                        }
                        is RecentSearchEntry.Contact -> {
                            contactsById[entry.contactId]?.let {
                                // Skip if contact is pinned
                                if (entry.contactId !in pinnedContactIds) {
                                    add(RecentSearchItem.Contact(entry, it))
                                }
                            }
                        }
                        is RecentSearchEntry.File -> {
                            filesByUri[entry.uri]?.let {
                                // Skip if file is pinned
                                if (entry.uri !in pinnedFileUris) {
                                    add(RecentSearchItem.File(entry, it))
                                }
                            }
                        }
                        is RecentSearchEntry.Setting -> {
                            settingsById[entry.id]?.let {
                                // Skip if setting is pinned
                                if (entry.id !in pinnedSettingIds) {
                                    add(RecentSearchItem.Setting(entry, it))
                                }
                            }
                        }
                        is RecentSearchEntry.AppShortcut -> {
                            shortcutsByKey[entry.shortcutKey]?.let {
                                // Skip if app shortcut is pinned
                                if (entry.shortcutKey !in pinnedAppShortcutIds) {
                                    add(RecentSearchItem.AppShortcut(entry, it))
                                }
                            }
                        }
                        is RecentSearchEntry.AppSetting -> Unit
                    }
                }
            }
            updateResultsState { it.copy(recentItems = items) }
        }
    }

    private fun refreshAliasRecentItems(section: SearchSection?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (section == SearchSection.CALENDAR) {
                val excludedEventIds = userPreferences.getExcludedCalendarEventIds()
                val upcoming = calendarRepository.getUpcomingEventsSortedAscending(limit = MAX_RECENT_ITEMS)
                    .filterNot { excludedEventIds.contains(it.eventId) }
                updateUiState { it.copy(calendarEvents = upcoming, aliasRecentItems = emptyList()) }
                return@launch
            }

            val allOpens = userPreferences.getRecentResultOpens()
            val filteredEntries: List<RecentSearchEntry> = when (section) {
                SearchSection.CONTACTS -> allOpens.filterIsInstance<RecentSearchEntry.Contact>()
                SearchSection.FILES -> allOpens.filterIsInstance<RecentSearchEntry.File>()
                SearchSection.SETTINGS -> allOpens.filterIsInstance<RecentSearchEntry.Setting>()
                SearchSection.APP_SHORTCUTS -> allOpens.filterIsInstance<RecentSearchEntry.AppShortcut>()
                SearchSection.APP_SETTINGS -> allOpens.filterIsInstance<RecentSearchEntry.AppSetting>()
                else -> {
                    updateResultsState { it.copy(aliasRecentItems = emptyList()) }
                    return@launch
                }
            }

            val limited = filteredEntries.take(MAX_RECENT_ITEMS)

            val contactIds = limited.filterIsInstance<RecentSearchEntry.Contact>().map { it.contactId }.toSet()
            val fileUris = limited.filterIsInstance<RecentSearchEntry.File>().map { it.uri }.toSet()
            val settingIds = limited.filterIsInstance<RecentSearchEntry.Setting>().map { it.id }.toSet()
            val shortcutKeys = limited.filterIsInstance<RecentSearchEntry.AppShortcut>().map { it.shortcutKey }.toSet()
            val appSettingIds = limited.filterIsInstance<RecentSearchEntry.AppSetting>().map { it.id }.toSet()

            val contactsById = contactRepository.getContactsByIds(contactIds).associateBy { it.contactId }
            val filesByUri = fileRepository.getFilesByUris(fileUris).associateBy { it.uri.toString() }
            val settingsById = settingsSearchHandler.getSettingsByIds(settingIds)
            val shortcutsByKey = appShortcutSearchHandler.getShortcutsByKeys(shortcutKeys)
            val appSettingsById = appSettingsSearchHandler.getSettingsByIds(appSettingIds)

            val pinnedContactIds = userPreferences.getPinnedContactIds()
            val pinnedFileUris = userPreferences.getPinnedFileUris()
            val pinnedSettingIds = userPreferences.getPinnedSettingIds()
            val pinnedAppShortcutIds = userPreferences.getPinnedAppShortcutIds()

            val items = buildList {
                limited.forEach { entry ->
                    when (entry) {
                        is RecentSearchEntry.Contact -> contactsById[entry.contactId]?.let {
                            if (entry.contactId !in pinnedContactIds) add(RecentSearchItem.Contact(entry, it))
                        }
                        is RecentSearchEntry.File -> filesByUri[entry.uri]?.let {
                            if (entry.uri !in pinnedFileUris) add(RecentSearchItem.File(entry, it))
                        }
                        is RecentSearchEntry.Setting -> settingsById[entry.id]?.let {
                            if (entry.id !in pinnedSettingIds) add(RecentSearchItem.Setting(entry, it))
                        }
                        is RecentSearchEntry.AppShortcut -> shortcutsByKey[entry.shortcutKey]?.let {
                            if (entry.shortcutKey !in pinnedAppShortcutIds) add(RecentSearchItem.AppShortcut(entry, it))
                        }
                        is RecentSearchEntry.AppSetting -> appSettingsById[entry.id]?.let {
                            add(RecentSearchItem.AppSetting(entry, it))
                        }
                        is RecentSearchEntry.Query -> Unit
                    }
                }
            }
            updateResultsState { it.copy(aliasRecentItems = items) }
        }
    }

    fun deleteRecentItem(entry: RecentSearchEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.deleteRecentItem(entry)
            refreshRecentItems()
            refreshAliasRecentItems(lockedAliasSearchSection)
        }
    }

    // Contact Card Actions (Synchronous access for UI composition, persistence is sync in prefs)
    fun getPrimaryContactCardAction(contactId: Long) =
            contactPreferences.getPrimaryContactCardAction(contactId)

    fun setPrimaryContactCardAction(
            contactId: Long,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
    ) {
        contactPreferences.setPrimaryContactCardAction(contactId, action)
        updateResultsState { it.copy(contactActionsVersion = it.contactActionsVersion + 1) }
    }

    fun getSecondaryContactCardAction(contactId: Long) =
            contactPreferences.getSecondaryContactCardAction(contactId)

    fun setSecondaryContactCardAction(
            contactId: Long,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
    ) {
        contactPreferences.setSecondaryContactCardAction(contactId, action)
        updateResultsState { it.copy(contactActionsVersion = it.contactActionsVersion + 1) }
    }

    fun requestContactActionPicker(
            contactId: Long,
            isPrimary: Boolean,
            serializedAction: String?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val contact =
                    contactRepository.getContactsByIds(setOf(contactId)).firstOrNull()
                            ?: return@launch
            val requestedAction =
                    serializedAction?.let {
                        com.tk.quicksearch.search.contacts.models.ContactCardAction
                                .fromSerializedString(it)
                    }
            val resolvedAction =
                    requestedAction
                            ?: if (isPrimary) {
                                getPrimaryContactCardAction(contactId)
                            } else {
                                getSecondaryContactCardAction(contactId)
                                        ?: getDefaultContactCardAction(contact, isPrimary)
                            }
            updateConfigState {
                it.copy(
                        contactActionPickerRequest =
                                ContactActionPickerRequest(contact, isPrimary, resolvedAction),
                )
            }
        }
    }

    fun clearContactActionPickerRequest() {
        updateConfigState { it.copy(contactActionPickerRequest = null) }
    }

    private fun getDefaultContactCardAction(
            contact: ContactInfo,
            isPrimary: Boolean,
    ): com.tk.quicksearch.search.contacts.models.ContactCardAction? {
        val phoneNumber = contact.phoneNumbers.firstOrNull() ?: return null
        return if (isPrimary) {
            when (ContactCallingAppResolver.resolveCallingAppForContact(
                            contactInfo = contact,
                            defaultApp = _permissionState.value.callingApp,
                    )
            ) {
                CallingApp.CALL ->
                        com.tk.quicksearch.search.contacts.models.ContactCardAction.Phone(
                                phoneNumber
                        )
                CallingApp.WHATSAPP ->
                        com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppCall(
                                phoneNumber
                        )
                CallingApp.TELEGRAM ->
                        com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramCall(
                                phoneNumber
                        )
                CallingApp.SIGNAL ->
                        com.tk.quicksearch.search.contacts.models.ContactCardAction.SignalCall(
                                phoneNumber
                        )
                CallingApp.GOOGLE_MEET ->
                        com.tk.quicksearch.search.contacts.models.ContactCardAction.GoogleMeet(
                                phoneNumber
                        )
            }
        } else {
            when (ContactMessagingAppResolver.resolveMessagingAppForContact(
                            contactInfo = contact,
                            defaultApp = _permissionState.value.messagingApp,
                    )
            ) {
                MessagingApp.MESSAGES -> {
                    com.tk.quicksearch.search.contacts.models.ContactCardAction.Sms(phoneNumber)
                }
                MessagingApp.WHATSAPP -> {
                    com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppMessage(
                            phoneNumber,
                    )
                }
                MessagingApp.TELEGRAM -> {
                    com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramMessage(
                            phoneNumber,
                    )
                }
                MessagingApp.SIGNAL -> {
                    com.tk.quicksearch.search.contacts.models.ContactCardAction.SignalMessage(
                            phoneNumber,
                    )
                }
            }
        }
    }

    fun onCustomAction(
            contactInfo: ContactInfo,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
    ) {
        val trackHistory = !isDirectSearchActive()

        // Route baseline phone/sms card actions through the standard handlers so they preserve
        // multi-number selection and avoid the contact-method popup-specific navigation behavior.
        when (action) {
            is com.tk.quicksearch.search.contacts.models.ContactCardAction.Phone -> {
                contactActionHandler.callContact(contactInfo, trackHistory = trackHistory)
                return
            }
            is com.tk.quicksearch.search.contacts.models.ContactCardAction.Sms -> {
                contactActionHandler.smsContact(contactInfo, trackHistory = trackHistory)
                return
            }
            else -> Unit
        }

        viewModelScope.launch(Dispatchers.IO) {
            val appContext = getApplication<Application>()
            val contact =
                    contactRepository.getContactsByIds(setOf(contactInfo.contactId)).firstOrNull()
            val methods = contact?.contactMethods ?: emptyList()

            fun matchesPhoneNumber(method: ContactMethod): Boolean {
                if (method.data.isBlank()) return false
                return PhoneNumberUtils.isSameNumber(method.data, action.phoneNumber)
            }

            fun matchesTelegramNumber(method: ContactMethod): Boolean =
                    TelegramContactUtils.isTelegramMethodForPhoneNumber(
                            context = appContext,
                            phoneNumber = action.phoneNumber,
                            telegramMethod = method,
                    ) || matchesPhoneNumber(method)

            fun matchesSignalNumber(method: ContactMethod): Boolean =
                    method.data.isBlank() || matchesPhoneNumber(method)

            // Match the action to a ContactMethod
            val matchedMethod: ContactMethod? =
                    methods.find { method ->
                        when (action) {
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.Phone -> {
                                method is ContactMethod.Phone && matchesPhoneNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.Sms -> {
                                method is ContactMethod.Sms && matchesPhoneNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppCall -> {
                                method is ContactMethod.WhatsAppCall && matchesPhoneNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppMessage -> {
                                method is ContactMethod.WhatsAppMessage &&
                                        matchesPhoneNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppVideoCall -> {
                                method is ContactMethod.WhatsAppVideoCall &&
                                        matchesPhoneNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramMessage -> {
                                method is ContactMethod.TelegramMessage &&
                                        matchesTelegramNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramCall -> {
                                method is ContactMethod.TelegramCall &&
                                        matchesTelegramNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramVideoCall -> {
                                method is ContactMethod.TelegramVideoCall &&
                                        matchesTelegramNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.SignalMessage -> {
                                method is ContactMethod.SignalMessage && matchesSignalNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.SignalCall -> {
                                method is ContactMethod.SignalCall && matchesSignalNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.SignalVideoCall -> {
                                method is ContactMethod.SignalVideoCall &&
                                        matchesSignalNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.GoogleMeet -> {
                                method is ContactMethod.GoogleMeet && matchesPhoneNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.Email -> {
                                method is ContactMethod.Email &&
                                    (method.data == action.phoneNumber || matchesPhoneNumber(method))
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.VideoCall -> {
                                method is ContactMethod.VideoCall &&
                                    method.packageName == action.packageName &&
                                    (method.data == action.phoneNumber || matchesPhoneNumber(method))
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.CustomApp -> {
                                method is ContactMethod.CustomApp &&
                                    (
                                        (action.dataId != null && method.dataId == action.dataId) ||
                                            (
                                                method.mimeType == action.mimeType &&
                                                    method.packageName == action.packageName &&
                                                    (
                                                        method.data == action.phoneNumber ||
                                                            matchesPhoneNumber(method)
                                                    )
                                            )
                                    )
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.ViewInContactsApp -> {
                                method is ContactMethod.ViewInContactsApp
                            }
                        }
                    }

            withContext(Dispatchers.Main) {
                if (matchedMethod != null) {
                    contactActionHandler.handleContactMethod(contactInfo, matchedMethod, trackHistory = trackHistory)
                } else {
                    // Fallback to generic action if specific method not found (e.g. dataId changed)
                    // For Phone/SMS this is easy
                    when (action) {
                        is com.tk.quicksearch.search.contacts.models.ContactCardAction.Phone -> {
                            contactActionHandler.handleContactMethod(
                                    contactInfo,
                                    ContactMethod.Phone(
                                            appContext.getString(
                                                    R.string.contact_method_call_label
                                            ),
                                            action.phoneNumber
                                    ),
                                    trackHistory = trackHistory,
                            )
                        }
                        is com.tk.quicksearch.search.contacts.models.ContactCardAction.Sms -> {
                            contactActionHandler.handleContactMethod(
                                    contactInfo,
                                    ContactMethod.Sms(
                                            appContext.getString(
                                                    R.string.contact_method_message_label
                                            ),
                                            action.phoneNumber
                                    ),
                                    trackHistory = trackHistory,
                            )
                        }
                        is com.tk.quicksearch.search.contacts.models.ContactCardAction.Email -> {
                            contactActionHandler.handleContactMethod(
                                    contactInfo,
                                    ContactMethod.Email(
                                            displayLabel =
                                                    appContext.getString(
                                                            R.string.contacts_action_button_email
                                                    ),
                                            data = action.phoneNumber,
                                    ),
                                    trackHistory = trackHistory,
                            )
                        }
                        is com.tk.quicksearch.search.contacts.models.ContactCardAction.ViewInContactsApp -> {
                            contactActionHandler.handleContactMethod(
                                    contactInfo,
                                    ContactMethod.ViewInContactsApp(
                                            displayLabel =
                                                    appContext.getString(
                                                            R.string.contacts_action_button_contacts
                                                    ),
                                    ),
                                    trackHistory = trackHistory,
                            )
                        }
                        is com.tk.quicksearch.search.contacts.models.ContactCardAction.VideoCall -> {
                            contactActionHandler.handleContactMethod(
                                    contactInfo,
                                    ContactMethod.VideoCall(
                                            displayLabel =
                                                    appContext.getString(
                                                            R.string.contacts_action_button_video_call
                                                    ),
                                            data = action.phoneNumber,
                                            packageName = action.packageName,
                                    ),
                                    trackHistory = trackHistory,
                            )
                        }
                        is com.tk.quicksearch.search.contacts.models.ContactCardAction.CustomApp -> {
                            contactActionHandler.handleContactMethod(
                                    contactInfo,
                                    ContactMethod.CustomApp(
                                            displayLabel = action.displayLabel,
                                            data = action.phoneNumber,
                                            mimeType = action.mimeType,
                                            packageName = action.packageName,
                                            dataId = action.dataId,
                                    ),
                                    trackHistory = trackHistory,
                            )
                        }
                        else -> {
                            showToast(R.string.error_action_not_available)
                        }
                    }
                }
            }
        }
    }

    private fun updateBooleanPreference(
            value: Boolean,
            preferenceSetter: (Boolean) -> Unit,
            stateUpdater: (Boolean) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            preferenceSetter(value)
            stateUpdater(value)
        }
    }

    private fun loadApps() {
        appSearchManager.loadApps()
    }

    private fun loadSettingsShortcuts() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsSearchHandler.loadShortcuts()
            withContext(Dispatchers.Main) { refreshSettingsState(updateResults = false) }
        }
    }

    private fun loadAppShortcuts() {
        if (!isAppShortcutsLoadInFlight.compareAndSet(false, true)) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loadedCached = appShortcutSearchHandler.loadCachedShortcutsOnly()
                if (loadedCached) {
                    withContext(Dispatchers.Main) { refreshAppShortcutsState() }
                }

                val loadedFresh = appShortcutSearchHandler.refreshShortcutsFromSystem()
                if (loadedFresh || !loadedCached) {
                    withContext(Dispatchers.Main) { refreshAppShortcutsState() }
                }
            } finally {
                isAppShortcutsLoadInFlight.set(false)
            }
        }
    }

    fun refreshAppShortcutsCacheFirst() {
        loadAppShortcuts()
    }

    private fun refreshSettingsState(updateResults: Boolean = true) {
        val currentResults = _resultsState.value.settingResults
        val currentState =
                settingsSearchHandler.getSettingsState(
                        query = _resultsState.value.query,
                        isSettingsSectionEnabled =
                                SearchSection.SETTINGS !in sectionManager.disabledSections,
                )

        updateResultsState { state ->
            state.copy(
                    pinnedSettings = currentState.pinned,
                    excludedSettings = currentState.excluded,
                    settingResults = if (updateResults) currentState.results else currentResults,
                    allDeviceSettings = settingsSearchHandler.getAvailableSettings(),
            )
        }
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

    private fun refreshAppShortcutsState(updateResults: Boolean = true) {
        val query = if (updateResults) _resultsState.value.query else ""
        val disabledShortcutIds = userPreferences.getDisabledAppShortcutIds()
        val iconOverrides = userPreferences.getAllAppShortcutIconOverrides()
        val currentState =
                appShortcutSearchHandler.getShortcutsState(
                        query = query,
                        isSectionEnabled =
                                SearchSection.APP_SHORTCUTS !in sectionManager.disabledSections,
                )

        val pinned = applyAppShortcutIconOverrides(currentState.pinned, iconOverrides)
        val excluded = applyAppShortcutIconOverrides(currentState.excluded, iconOverrides)
        val allShortcuts =
                applyAppShortcutIconOverrides(
                        appShortcutSearchHandler.getAvailableShortcuts(),
                        iconOverrides,
                )

        updateUiState { state ->
            state.copy(
                    allAppShortcuts = allShortcuts,
                    disabledAppShortcutIds = disabledShortcutIds,
                    pinnedAppShortcuts = pinned,
                    excludedAppShortcuts = excluded,
                    appShortcutResults =
                            if (updateResults) {
                                applyAppShortcutIconOverrides(currentState.results, iconOverrides)
                            } else {
                                state.appShortcutResults
                            },
            )
        }
    }

    fun refreshUsageAccess() {
        updatePermissionState { it.copy(hasUsagePermission = repository.hasUsageAccess()) }
    }

    fun refreshApps(
            showToast: Boolean = false,
            forceUiUpdate: Boolean = false,
    ) {
        val shouldInvalidateIcons = showToast || forceUiUpdate
        if (shouldInvalidateIcons) {
            invalidateAppIconCache()
        }
        appSearchManager.refreshApps(showToast, forceUiUpdate = forceUiUpdate || showToast)
    }

    fun refreshContacts(showToast: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.refreshContactsProviderSnapshot()
            pinningHandler.loadPinnedAndExcludedContacts()

            val query = _resultsState.value.query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.resetNoResultTracking()
                secondarySearchOrchestrator.performSecondarySearches(query)
            } else {
                updateResultsState { it.copy(contactResults = emptyList()) }
            }

            if (showToast) {
                withContext(Dispatchers.Main) {
                    showToast(R.string.contacts_refreshed_successfully)
                }
            }
        }
    }

    fun refreshFiles(showToast: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            fileRepository.refreshFilesProviderSnapshot()
            pinningHandler.loadPinnedAndExcludedFiles()

            val query = _resultsState.value.query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.resetNoResultTracking()
                secondarySearchOrchestrator.performSecondarySearches(query)
            } else {
                updateResultsState { it.copy(fileResults = emptyList()) }
            }

            if (showToast) {
                withContext(Dispatchers.Main) { showToast(R.string.files_refreshed_successfully) }
            }
        }
    }

    private fun loadPinnedAndExcludedCalendarEvents() {
        if (!hasCalendarPermission()) {
            updateResultsState {
                it.copy(
                    pinnedCalendarEvents = emptyList(),
                    excludedCalendarEvents = emptyList(),
                )
            }
            return
        }

        val excludedIds = userPreferences.getExcludedCalendarEventIds()
        val pinnedIds = userPreferences.getPinnedCalendarEventIds().filterNot { excludedIds.contains(it) }.toSet()
        val pinned = calendarRepository.getEventsByIds(pinnedIds)
        val excluded = calendarRepository.getEventsByIds(excludedIds)

        updateResultsState {
            it.copy(
                pinnedCalendarEvents = pinned,
                excludedCalendarEvents = excluded,
            )
        }
    }

    fun setDirectDialEnabled(
            enabled: Boolean,
            manual: Boolean = true,
    ) {
        directDialEnabled = enabled
        hasSeenDirectDialChoice = true
        userPreferences.setDirectDialEnabled(enabled)
        userPreferences.setHasSeenDirectDialChoice(true)
        if (manual) {
            userPreferences.setDirectDialManuallyDisabled(!enabled)
        }
        updateFeatureState { it.copy(directDialEnabled = enabled) }
    }

    fun setAssistantLaunchVoiceModeEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            assistantLaunchVoiceModeEnabled = enabled
            userPreferences.setAssistantLaunchVoiceModeEnabled(enabled)
            updateFeatureState { it.copy(assistantLaunchVoiceModeEnabled = enabled) }
        }
    }

    fun onQueryChange(newQuery: String) {
        inMemoryRetainedQuery = newQuery
        queryCoordinator.onQueryChange(newQuery)
    }

    fun executeCurrencyConversion() {
        toolCoordinator.executeCurrencyConversion()
    }

    fun executeWordClockLookup() {
        toolCoordinator.executeWordClockLookup()
    }

    fun executeDictionaryLookup() {
        toolCoordinator.executeDictionaryLookup()
    }

    fun activateSearchSectionFilter(section: SearchSection) {
        queryCoordinator.activateSearchSectionFilter(section)
    }

    fun clearDetectedShortcut() {
        queryCoordinator.clearDetectedShortcut()
    }

    fun clearQuery() {
        queryCoordinator.clearQuery()
    }

    fun consumeRetainedQuerySelectionRequest() {
        if (_configState.value.selectRetainedQuery) {
            updateConfigState { it.copy(selectRetainedQuery = false) }
        }
    }

    fun onSettingsImported() {
        viewModelScope.launch(Dispatchers.IO) {
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
                            isSearchEngineCompactMode =
                                    searchEngineManager.isSearchEngineCompactMode,
                            searchEngineCompactRowCount =
                                    searchEngineManager.searchEngineCompactRowCount,
                            isSearchEngineAliasSuffixEnabled =
                                    userPreferences.isSearchEngineAliasSuffixEnabled(),
                            isAliasTriggerAfterSpaceEnabled =
                                    userPreferences.isAliasTriggerAfterSpaceEnabled(),
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
            }
        }
    }

    fun handleOnResume() {
        val startupComplete = isStartupComplete

        // --- 1. Usage-access permission ----------------------------------------
        // Only refresh the app list when usage-access permission has actually
        // changed state since last resume.  Rotation and notification-shade
        // dismissals no longer trigger a full app reload.
        val previousUsage = _permissionState.value.hasUsagePermission
        val latestUsage = repository.hasUsageAccess()
        val usageChanged = previousUsage != latestUsage
        if (usageChanged) {
            updatePermissionState { it.copy(hasUsagePermission = latestUsage) }
            if (startupComplete) {
                if (latestUsage) {
                    refreshApps()
                } else {
                    refreshAppSuggestions()
                }
            }
        }

        // --- 2. Optional permissions (contacts, files, call, wallpaper) ---------
        // handleOptionalPermissionChange() already does internal dirty-checking
        // and returns true only when something actually changed.
        val optionalPermissionsChanged = run {
            val before = _permissionState.value
            handleOptionalPermissionChangeInternal(allowAppRefresh = startupComplete)
            _permissionState.value != before
        }

        // --- 3. Pinned / excluded contacts & files ------------------------------
        // ContentProvider queries are expensive — only reload when a permission
        // that gates these lists has just changed.  If anything else triggered
        // this resume (rotation, notification shade) we skip these queries.
        if (startupComplete && optionalPermissionsChanged) {
            pinningHandler.loadPinnedContactsAndFiles()
            pinningHandler.loadExcludedContactsAndFiles()
            loadPinnedAndExcludedCalendarEvents()
        }

        // --- 4. Device settings & app shortcuts ---------------------------------
        // Only re-read SharedPreferences when something actually mutated them
        // (flag is set by pin/exclude/disable operations and cleared here).
        if (startupComplete && resumeNeedsStaticDataRefresh) {
            resumeNeedsStaticDataRefresh = false
            refreshSettingsState()
            refreshAppShortcutsState()
        }

        // --- 5. Browser targets -------------------------------------------------
        // PackageManager query — throttle to at most once per 5 minutes.
        val now = System.currentTimeMillis()
        if (startupComplete && now - lastBrowserTargetRefreshMs >= BROWSER_REFRESH_INTERVAL_MS) {
            lastBrowserTargetRefreshMs = now
            viewModelScope.launch(Dispatchers.IO) {
                searchEngineManager.ensureInitialized()
                searchEngineManager.refreshBrowserTargets()
            }
        }
    }

    // Navigation Delegates
    fun openUsageAccessSettings() = navigationHandler.openUsageAccessSettings()

    fun openAppSettings() = navigationHandler.openAppSettings()

    fun openAllFilesAccessSettings() = navigationHandler.openAllFilesAccessSettings()

    fun openFilesPermissionSettings() = navigationHandler.openFilesPermissionSettings()

    fun openContactPermissionSettings() = navigationHandler.openContactPermissionSettings()

    fun openCalendarPermissionSettings() = navigationHandler.openCalendarPermissionSettings()

    fun launchApp(appInfo: AppInfo) =
            navigationHandler.launchApp(
                    appInfo,
                    shouldTrackRecentFallback = !_permissionState.value.hasUsagePermission,
            )

    fun openAppInfo(appInfo: AppInfo) = navigationHandler.openAppInfo(appInfo)

    fun openAppInfo(packageName: String) = navigationHandler.openAppInfo(packageName)

    fun requestUninstall(appInfo: AppInfo) = navigationHandler.requestUninstall(appInfo)

    fun openSearchUrl(
            query: String,
            searchEngine: SearchEngine,
            addToSearchHistory: Boolean = true,
    ) = navigationHandler.openSearchUrl(query, searchEngine, addToSearchHistory)

    fun openSearchTarget(
            query: String,
            target: SearchTarget,
            addToSearchHistory: Boolean = true,
    ) = navigationHandler.openSearchTarget(query, target, addToSearchHistory)

    fun searchIconPacks() = navigationHandler.searchIconPacks()

    fun openFile(deviceFile: DeviceFile) = navigationHandler.openFile(deviceFile)

    fun openContainingFolder(deviceFile: DeviceFile) =
            navigationHandler.openContainingFolder(deviceFile)

    fun openContact(contactInfo: ContactInfo) = navigationHandler.openContact(contactInfo)

    fun openEmail(email: String) = navigationHandler.openEmail(email)

    fun launchAppShortcut(shortcut: StaticShortcut) {
        val error =
            launchStaticShortcut(
                context = getApplication(),
                shortcut = shortcut,
                skipSearchTargetQueryHistory = true,
            )
        if (error != null) {
            showToast(error)
        } else {
            userPreferences.addRecentItem(RecentSearchEntry.AppShortcut(shortcutKey(shortcut)))
            onNavigationTriggered()
        }
    }

    // App Management Delegates
    fun hideApp(appInfo: AppInfo) =
            appManager.hideApp(appInfo, _resultsState.value.query.isNotBlank())

    fun unhideAppFromSuggestions(appInfo: AppInfo) = appManager.unhideAppFromSuggestions(appInfo)

    fun unhideAppFromResults(appInfo: AppInfo) = appManager.unhideAppFromResults(appInfo)

    fun clearAllHiddenApps() = appManager.clearAllHiddenApps()

    fun pinApp(appInfo: AppInfo) = appManager.pinApp(appInfo)

    fun unpinApp(appInfo: AppInfo) = appManager.unpinApp(appInfo)

    fun setAppNickname(
            appInfo: AppInfo,
            nickname: String?,
    ) = appManager.setAppNickname(appInfo, nickname)

    fun getAppNickname(packageName: String): String? = appManager.getAppNickname(packageName)

    fun clearCachedApps() = appSearchManager.clearCachedApps()

    // Contact Management Delegates
    fun pinContact(contactInfo: ContactInfo) = contactManager.pinContact(contactInfo)

    fun unpinContact(contactInfo: ContactInfo) {
        contactManager.unpinContact(contactInfo)
        refreshRecentItems()
    }

    fun excludeContact(contactInfo: ContactInfo) = contactManager.excludeContact(contactInfo)

    fun removeExcludedContact(contactInfo: ContactInfo) =
            contactManager.removeExcludedContact(contactInfo)

    fun clearAllExcludedContacts() = contactManager.clearAllExcludedContacts()

    fun setContactNickname(
            contactInfo: ContactInfo,
            nickname: String?,
    ) = contactManager.setContactNickname(contactInfo, nickname)

    fun getContactNickname(contactId: Long): String? = contactManager.getContactNickname(contactId)

    // File Management Delegates
    fun pinFile(deviceFile: DeviceFile) = fileManager.pinFile(deviceFile)

    fun unpinFile(deviceFile: DeviceFile) {
        fileManager.unpinFile(deviceFile)
        refreshRecentItems()
    }

    fun excludeFile(deviceFile: DeviceFile) = fileManager.excludeFile(deviceFile)

    fun excludeFileExtension(deviceFile: DeviceFile) {
        excludedFileExtensions = fileManager.excludeFileExtension(deviceFile)
        updateUiState { it.copy(excludedFileExtensions = excludedFileExtensions) }
    }

    fun removeExcludedFileExtension(extension: String) {
        excludedFileExtensions = fileManager.removeExcludedFileExtension(extension)
        updateUiState { it.copy(excludedFileExtensions = excludedFileExtensions) }
    }

    fun removeExcludedFile(deviceFile: DeviceFile) = fileManager.removeExcludedFile(deviceFile)

    fun clearAllExcludedFiles() = fileManager.clearAllExcludedFiles()

    fun setFileNickname(
            deviceFile: DeviceFile,
            nickname: String?,
    ) = fileManager.setFileNickname(deviceFile, nickname)

    fun getFileNickname(uri: String): String? = fileManager.getFileNickname(uri)

    // Settings Management Delegates
    fun pinSetting(setting: DeviceSetting) = settingsManager.pinSetting(setting)

    fun unpinSetting(setting: DeviceSetting) {
        settingsManager.unpinSetting(setting)
        refreshRecentItems()
    }

    fun excludeSetting(setting: DeviceSetting) = settingsManager.excludeSetting(setting)

    fun setSettingNickname(
            setting: DeviceSetting,
            nickname: String?,
    ) = settingsManager.setSettingNickname(setting, nickname)

    fun getSettingNickname(id: String): String? = settingsManager.getSettingNickname(id)

    fun removeExcludedSetting(setting: DeviceSetting) =
            settingsManager.removeExcludedSetting(setting)

    fun clearAllExcludedSettings() = settingsManager.clearAllExcludedSettings()

    fun openSetting(setting: DeviceSetting) = navigationHandler.openSetting(setting)

    // Calendar Management Delegates
    fun pinCalendarEvent(event: CalendarEventInfo) = calendarManager.pinItem(event)

    fun unpinCalendarEvent(event: CalendarEventInfo) = calendarManager.unpinItem(event)

    fun excludeCalendarEvent(event: CalendarEventInfo) = calendarManager.excludeItem(event)

    fun removeExcludedCalendarEvent(event: CalendarEventInfo) =
            calendarManager.removeExcludedItem(event)

    fun clearAllExcludedCalendarEvents() = calendarManager.clearAllExcludedItems()

    fun setCalendarEventNickname(
            event: CalendarEventInfo,
            nickname: String?,
    ) = calendarManager.setItemNickname(event, nickname)

    fun getCalendarEventNickname(eventId: Long): String? =
            userPreferences.getCalendarEventNickname(eventId)

    fun openCalendarEvent(event: CalendarEventInfo) =
            navigationHandler.openCalendarEvent(event)

    // App Shortcut Management Delegates
    fun pinAppShortcut(shortcut: StaticShortcut) = appShortcutManager.pinShortcut(shortcut)

    fun unpinAppShortcut(shortcut: StaticShortcut) {
        appShortcutManager.unpinShortcut(shortcut)
        refreshRecentItems()
    }

    fun excludeAppShortcut(shortcut: StaticShortcut) = appShortcutManager.excludeShortcut(shortcut)

    fun setAppShortcutNickname(
            shortcut: StaticShortcut,
            nickname: String?,
    ) = appShortcutManager.setShortcutNickname(shortcut, nickname)

    fun setAppShortcutEnabled(
            shortcut: StaticShortcut,
            enabled: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setAppShortcutEnabled(shortcutKey(shortcut), enabled)
            withContext(Dispatchers.Main) {
                refreshAppShortcutsState()
                refreshRecentItems()
            }
        }
    }

    fun setAppShortcutIconOverride(
            shortcut: StaticShortcut,
            iconBase64: String?,
    ) {
        if (isUserCreatedShortcut(shortcut)) return
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setAppShortcutIconOverride(shortcutKey(shortcut), iconBase64)
            withContext(Dispatchers.Main) {
                refreshAppShortcutsState()
                refreshRecentItems()
            }
        }
    }

    fun getAppShortcutIconOverride(shortcutId: String): String? =
            userPreferences.getAppShortcutIconOverride(shortcutId)

    fun getAppShortcutNickname(shortcutId: String): String? =
            appShortcutManager.getShortcutNickname(shortcutId)

    fun removeExcludedAppShortcut(shortcut: StaticShortcut) =
            appShortcutManager.removeExcludedShortcut(shortcut)

    fun addCustomAppShortcutFromPickerResult(
            resultData: Intent?,
            sourcePackageName: String? = null,
            showDefaultToast: Boolean = true,
            onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
            onAddFailed: (() -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val addedShortcut =
                    appShortcutRepository.addCustomShortcutFromPickerResult(
                            resultData = resultData,
                            sourcePackageName = sourcePackageName,
                    )
            if (addedShortcut != null) {
                appShortcutSearchHandler.loadCachedShortcutsOnly()
                withContext(Dispatchers.Main) {
                    refreshAppShortcutsState()
                    onShortcutAdded?.invoke(addedShortcut)
                    if (showDefaultToast) {
                        showToast(R.string.settings_app_shortcuts_add_success)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (showDefaultToast) {
                        showToast(R.string.settings_app_shortcuts_add_failed)
                    }
                    onAddFailed?.invoke()
                }
            }
        }
    }

    fun addSearchTargetQueryShortcut(
            target: SearchTarget,
            shortcutName: String,
            shortcutQuery: String,
            mode: SearchTargetShortcutMode = SearchTargetShortcutMode.AUTO,
            showDefaultToast: Boolean = true,
            onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
            onAddFailed: (() -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val addedShortcut =
                    appShortcutRepository.addSearchTargetQueryShortcut(
                            target = target,
                            shortcutName = shortcutName,
                            shortcutQuery = shortcutQuery,
                            mode = mode,
                    )
            if (addedShortcut != null) {
                appShortcutSearchHandler.loadCachedShortcutsOnly()
                withContext(Dispatchers.Main) {
                    refreshAppShortcutsState()
                    onShortcutAdded?.invoke(addedShortcut)
                    if (showDefaultToast) {
                        showToast(R.string.settings_app_shortcuts_add_success)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (showDefaultToast) {
                        showToast(R.string.settings_app_shortcuts_add_failed)
                    }
                    onAddFailed?.invoke()
                }
            }
        }
    }

    fun addCustomAppActivityShortcut(
            packageName: String,
            activityClassName: String,
            activityLabel: String,
            showDefaultToast: Boolean = true,
            onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
            onAddFailed: (() -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val addedShortcut =
                    appShortcutRepository.addCustomShortcutForAppActivity(
                            packageName = packageName,
                            activityClassName = activityClassName,
                            activityLabel = activityLabel,
                    )
            if (addedShortcut != null) {
                appShortcutSearchHandler.loadCachedShortcutsOnly()
                withContext(Dispatchers.Main) {
                    refreshAppShortcutsState()
                    onShortcutAdded?.invoke(addedShortcut)
                    if (showDefaultToast) {
                        showToast(R.string.settings_app_shortcuts_add_success)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (showDefaultToast) {
                        showToast(R.string.settings_app_shortcuts_add_failed)
                    }
                    onAddFailed?.invoke()
                }
            }
        }
    }

    fun addCustomAppDeepLinkShortcut(
            packageName: String,
            shortcutName: String,
            deepLink: String,
            iconBase64: String?,
            showDefaultToast: Boolean = true,
            onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
            onAddFailed: (() -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val addedShortcut =
                    appShortcutRepository.addCustomShortcutForAppDeepLink(
                            packageName = packageName,
                            shortcutName = shortcutName,
                            deepLink = deepLink,
                            iconBase64 = iconBase64,
                    )
            if (addedShortcut != null) {
                appShortcutSearchHandler.loadCachedShortcutsOnly()
                withContext(Dispatchers.Main) {
                    refreshAppShortcutsState()
                    onShortcutAdded?.invoke(addedShortcut)
                    if (showDefaultToast) {
                        showToast(R.string.settings_app_shortcuts_add_success)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (showDefaultToast) {
                        showToast(R.string.settings_app_shortcuts_add_failed)
                    }
                    onAddFailed?.invoke()
                }
            }
        }
    }

    fun deleteCustomAppShortcut(shortcut: StaticShortcut) {
        if (!isUserCreatedShortcut(shortcut)) return
        viewModelScope.launch(Dispatchers.IO) {
            val removed = appShortcutRepository.removeCustomShortcut(shortcut)
            if (!removed) return@launch
            val id = shortcutKey(shortcut)
            userPreferences.unpinAppShortcut(id)
            userPreferences.removeExcludedAppShortcut(id)
            userPreferences.setAppShortcutEnabled(id, true)
            userPreferences.setAppShortcutNickname(id, null)
            userPreferences.setAppShortcutIconOverride(id, null)
            appShortcutSearchHandler.loadCachedShortcutsOnly()
            withContext(Dispatchers.Main) {
                refreshAppShortcutsState()
                refreshRecentItems()
                showToast(R.string.settings_app_shortcuts_delete_success)
            }
        }
    }

    fun updateCustomAppShortcut(
            shortcut: StaticShortcut,
            shortcutName: String,
            shortcutValue: String?,
            iconBase64: String?,
    ) {
        if (!isUserCreatedShortcut(shortcut)) return
        viewModelScope.launch(Dispatchers.IO) {
            val updated =
                    appShortcutRepository.updateCustomShortcut(
                            shortcut = shortcut,
                            shortcutName = shortcutName,
                            shortcutValue = shortcutValue,
                            iconBase64 = iconBase64,
                    )
            if (updated) {
                appShortcutSearchHandler.loadCachedShortcutsOnly()
            }
            withContext(Dispatchers.Main) {
                if (!updated) {
                    showToast(R.string.settings_app_shortcuts_update_failed)
                    return@withContext
                }
                refreshAppShortcutsState()
                refreshRecentItems()
                showToast(R.string.settings_app_shortcuts_update_success)
            }
        }
    }

    // Global Actions
    fun clearAllExclusions() {
        contactManager.clearAllExcludedContacts()
        fileManager.clearAllExcludedFiles()
        appManager.clearAllHiddenApps()
        settingsManager.clearAllExcludedSettings()
        calendarManager.clearAllExcludedItems()
        appShortcutManager.clearAllExcludedShortcuts()
        pinningHandler.loadExcludedContactsAndFiles()
        loadPinnedAndExcludedCalendarEvents()
        refreshSettingsState(updateResults = false)
        refreshAppShortcutsState(updateResults = false)
    }

    private fun computeEffectiveIsDarkMode(): Boolean {
        return when (_configState.value.appThemeMode) {
            AppThemeMode.DARK -> true
            AppThemeMode.LIGHT -> false
            AppThemeMode.SYSTEM -> {
                val nightModeFlags =
                        appContext.resources.configuration.uiMode and
                                android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun setWallpaperBackgroundAlpha(alpha: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedAlpha = alpha.coerceIn(0f, 1f)
            userPreferences.setWallpaperBackgroundAlpha(sanitizedAlpha, computeEffectiveIsDarkMode())
            wallpaperBackgroundAlpha = sanitizedAlpha
            updateConfigState { it.copy(wallpaperBackgroundAlpha = sanitizedAlpha) }
            saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setWallpaperBlurRadius(radius: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedRadius = radius.coerceIn(0f, UiPreferences.MAX_WALLPAPER_BLUR_RADIUS)
            userPreferences.setWallpaperBlurRadius(sanitizedRadius, computeEffectiveIsDarkMode())
            wallpaperBlurRadius = sanitizedRadius
            updateConfigState { it.copy(wallpaperBlurRadius = sanitizedRadius) }
            saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch(Dispatchers.IO) {
            if (appTheme == theme) return@launch
            userPreferences.setAppTheme(theme)
            appTheme = theme
            updateConfigState { it.copy(appTheme = theme) }
            applyLauncherIconSelection()
            saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setAppThemeMode(theme: AppThemeMode) {
        val previousIsDark = computeEffectiveIsDarkMode()
        userPreferences.setAppThemeMode(theme)
        updateConfigState { it.copy(appThemeMode = theme) }
        val newIsDark = when (theme) {
            AppThemeMode.DARK -> true
            AppThemeMode.LIGHT -> false
            AppThemeMode.SYSTEM -> {
                val nightModeFlags =
                        appContext.resources.configuration.uiMode and
                                android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
        if (newIsDark != previousIsDark) {
            viewModelScope.launch(Dispatchers.IO) {
                val newAlpha = userPreferences.getWallpaperBackgroundAlpha(newIsDark)
                val newBlur = userPreferences.getWallpaperBlurRadius(newIsDark)
                wallpaperBackgroundAlpha = newAlpha
                wallpaperBlurRadius = newBlur
                updateConfigState {
                    it.copy(wallpaperBackgroundAlpha = newAlpha, wallpaperBlurRadius = newBlur)
                }
                applyLauncherIconSelection()
                saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) { applyLauncherIconSelection() }
        }
    }

    fun setOverlayThemeIntensity(intensity: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedIntensity = sanitizeOverlayThemeIntensity(intensity)
            if (overlayThemeIntensity == sanitizedIntensity) return@launch
            userPreferences.setOverlayThemeIntensity(sanitizedIntensity)
            overlayThemeIntensity = sanitizedIntensity
            updateConfigState { it.copy(overlayThemeIntensity = sanitizedIntensity) }
            saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setFontScaleMultiplier(multiplier: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedMultiplier = sanitizeFontScaleMultiplier(multiplier)
            if (fontScaleMultiplier == sanitizedMultiplier) return@launch
            userPreferences.setFontScaleMultiplier(sanitizedMultiplier)
            fontScaleMultiplier = sanitizedMultiplier
            updateConfigState { it.copy(fontScaleMultiplier = sanitizedMultiplier) }
            saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

    fun setBackgroundSource(source: BackgroundSource) {
        viewModelScope.launch(Dispatchers.IO) {
            if (backgroundSource == source) return@launch
            userPreferences.setBackgroundSource(source)
            backgroundSource = source
            val autoTheme = if (source != BackgroundSource.THEME && appTheme != AppTheme.MONOCHROME) {
                userPreferences.setAppTheme(AppTheme.MONOCHROME)
                appTheme = AppTheme.MONOCHROME
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
            saveStartupSurfaceSnapshotAsync(forcePreviewRefresh = true, allowDuringQuery = true)
        }
    }

    fun setCustomImageUri(uri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalized = uri?.trim()?.takeIf { it.isNotEmpty() }
            if (customImageUri == normalized) return@launch
            userPreferences.setCustomImageUri(normalized)
            customImageUri = normalized
            updateConfigState { it.copy(customImageUri = normalized) }
            saveStartupSurfaceSnapshotAsync(forcePreviewRefresh = true, allowDuringQuery = true)
        }
    }

    private fun sanitizeOverlayThemeIntensity(intensity: Float): Float =
            intensity.coerceIn(
                    UiPreferences.MIN_OVERLAY_THEME_INTENSITY,
                    UiPreferences.MAX_OVERLAY_THEME_INTENSITY,
            )

    private fun sanitizeFontScaleMultiplier(multiplier: Float): Float =
            multiplier.coerceIn(
                    UiPreferences.MIN_FONT_SCALE_MULTIPLIER,
                    UiPreferences.MAX_FONT_SCALE_MULTIPLIER,
            )

    fun refreshIconPacks() = iconPackHandler.refreshIconPacks()

    fun setIconPackPackage(packageName: String?) {
        val state = _resultsState.value
        val visiblePackageNames = buildList {
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
        viewModelScope.launch(Dispatchers.IO) {
            if (appIconShape == shape) return@launch
            userPreferences.setAppIconShape(shape)
            appIconShape = shape
            updateConfigState { it.copy(appIconShape = shape) }
        }
    }

    fun setLauncherAppIcon(selection: LauncherAppIcon) {
        viewModelScope.launch(Dispatchers.IO) {
            if (launcherAppIcon == selection) return@launch
            userPreferences.setLauncherAppIcon(selection)
            launcherAppIcon = selection
            updateConfigState { it.copy(launcherAppIcon = selection) }
            applyLauncherIconSelection(selection)
        }
    }

    fun onSystemDarkModeChanged(isDarkMode: Boolean) {
        if (_configState.value.appThemeMode != AppThemeMode.SYSTEM) return
        if (launcherAppIcon != LauncherAppIcon.AUTO) return
        viewModelScope.launch(Dispatchers.IO) {
            launcherIconManager.applySelection(
                    selection = launcherAppIcon,
                    appTheme = appTheme,
                    isDarkMode = isDarkMode,
            )
        }
    }

    fun setThemedIconsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (themedIconsEnabled == enabled) return@launch
            userPreferences.setThemedIconsEnabled(enabled)
            themedIconsEnabled = enabled
            updateConfigState { it.copy(themedIconsEnabled = enabled) }
        }
    }

    private fun applyLauncherIconSelection(selection: LauncherAppIcon = launcherAppIcon) {
        launcherIconManager.applySelection(
                selection = selection,
                appTheme = appTheme,
                isDarkMode = computeEffectiveIsDarkMode(),
        )
    }

    // Aliases
    fun setAliasesEnabled(enabled: Boolean) = aliasHandler.setAliasesEnabled(enabled)

    fun setAlias(
            target: SearchTarget,
            code: String,
    ) = aliasHandler.setAlias(target, code)

    fun setAlias(
            targetId: String,
            code: String,
    ) = aliasHandler.setAlias(targetId, code)

    fun setAliasEnabled(
            target: SearchTarget,
            enabled: Boolean,
    ) = aliasHandler.setAliasEnabled(target, enabled)

    fun getAlias(target: SearchTarget): String = aliasHandler.getAlias(target)

    fun getAlias(
            targetId: String,
            defaultCode: String = "",
    ): String = aliasHandler.getAlias(targetId, defaultCode)

    fun isAliasEnabled(target: SearchTarget): Boolean = aliasHandler.isAliasEnabled(target)

    // Sections
    fun setSectionEnabled(
            section: SearchSection,
            enabled: Boolean,
    ) = sectionManager.setSectionEnabled(section, enabled)

    fun canEnableSection(section: SearchSection): Boolean = sectionManager.canEnableSection(section)

    // Messaging & Feature delegates
    fun setMessagingApp(app: MessagingApp) = messagingHandler.setMessagingApp(app)

    fun setCallingApp(app: CallingApp) {
        userPreferences.setCallingApp(app)
        val permState = _permissionState.value
        val resolvedCallingApp =
                resolveCallingApp(
                        app = app,
                        isWhatsAppInstalled = permState.isWhatsAppInstalled,
                        isTelegramInstalled = permState.isTelegramInstalled,
                        isSignalInstalled = permState.isSignalInstalled,
                        isGoogleMeetInstalled = permState.isGoogleMeetInstalled,
                )
        if (resolvedCallingApp != app) {
            userPreferences.setCallingApp(resolvedCallingApp)
        }
        updatePermissionState { it.copy(callingApp = resolvedCallingApp) }
    }

    fun acknowledgeReleaseNotes() =
            releaseNotesHandler.acknowledgeReleaseNotes(_configState.value.releaseNotesVersionName)

    fun requestDirectSearch(query: String) = directSearchHandler.requestDirectSearch(query)

    fun setShowStartSearchingOnOnboarding(show: Boolean) {
        updateConfigState { it.copy(showStartSearchingOnOnboarding = show) }
    }

    fun onSearchEngineOnboardingDismissed() {
        updateConfigState {
            it.copy(
                    showSearchEngineOnboarding = false,
                    showStartSearchingOnOnboarding = false,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setHasSeenSearchEngineOnboarding(true)
        }
    }

    fun onContactActionHintDismissed() {
        userPreferences.setHasSeenContactActionHint(true)
        updateUiState { it.copy(showContactActionHint = false) }
    }

    // Contact Actions
    private fun isDirectSearchActive() =
        _resultsState.value.DirectSearchState.status != DirectSearchStatus.Idle

    fun callContact(contactInfo: ContactInfo) =
        contactActionHandler.callContact(contactInfo, trackHistory = !isDirectSearchActive())

    fun smsContact(contactInfo: ContactInfo) =
        contactActionHandler.smsContact(contactInfo, trackHistory = !isDirectSearchActive())

    fun onPhoneNumberSelected(
            phoneNumber: String,
            rememberChoice: Boolean,
    ) = contactActionHandler.onPhoneNumberSelected(phoneNumber, rememberChoice)

    fun dismissPhoneNumberSelection() = contactActionHandler.dismissPhoneNumberSelection()

    fun dismissDirectDialChoice() = contactActionHandler.dismissDirectDialChoice()

    fun handleContactMethod(
            contactInfo: ContactInfo,
            method: ContactMethod,
    ) = contactActionHandler.handleContactMethod(contactInfo, method, trackHistory = !isDirectSearchActive())

    fun trackRecentContactTap(contactInfo: ContactInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.addRecentItem(RecentSearchEntry.Contact(contactInfo.contactId))
        }
    }

    fun trackRecentSettingTap(settingId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.addRecentItem(RecentSearchEntry.Setting(settingId))
        }
    }

    fun trackRecentAppSettingTap(settingId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.addRecentItem(RecentSearchEntry.AppSetting(settingId))
            refreshAliasRecentItems(lockedAliasSearchSection)
        }
    }

    fun getEnabledSearchTargets(): List<SearchTarget> =
            searchEngineManager.getEnabledSearchTargets()

    fun setSearchTargetEnabled(
            target: SearchTarget,
            enabled: Boolean,
    ) = searchEngineManager.setSearchTargetEnabled(target, enabled)

    fun reorderSearchTargets(newOrder: List<SearchTarget>) =
            searchEngineManager.reorderSearchTargets(newOrder)

    fun addCustomSearchEngine(
            name: String,
            normalizedTemplate: String,
            faviconBase64: String,
            browserPackage: String? = null,
    ) = searchEngineManager.addCustomSearchEngine(name, normalizedTemplate, faviconBase64, browserPackage)

    fun updateCustomSearchEngine(
            customId: String,
            name: String,
            urlTemplateInput: String,
            faviconBase64: String?,
            browserPackage: String? = null,
    ) =
            searchEngineManager.updateCustomSearchEngine(
                    customId,
                    name,
                    urlTemplateInput,
                    faviconBase64,
                    browserPackage,
            )

    fun deleteCustomSearchEngine(customId: String) =
            searchEngineManager.deleteCustomSearchEngine(customId)

    fun setSearchEngineCompactMode(enabled: Boolean) =
            searchEngineManager.setSearchEngineCompactMode(enabled)

    fun setSearchEngineCompactRowCount(rowCount: Int) =
            searchEngineManager.setSearchEngineCompactRowCount(rowCount)

    fun setSearchEngineAliasSuffixEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setSearchEngineAliasSuffixEnabled(enabled)
            updateFeatureState { it.copy(isSearchEngineAliasSuffixEnabled = enabled) }
        }
    }

    fun setAliasTriggerAfterSpaceEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setAliasTriggerAfterSpaceEnabled(enabled)
            updateFeatureState { it.copy(isAliasTriggerAfterSpaceEnabled = enabled) }
        }
    }

    fun setFileTypeEnabled(
            fileType: FileType,
            enabled: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated =
                    enabledFileTypes.toMutableSet().apply {
                        if (enabled) add(fileType) else remove(fileType)
                    }
            enabledFileTypes = updated
            userPreferences.setEnabledFileTypes(enabledFileTypes)
            updateUiState { it.copy(enabledFileTypes = enabledFileTypes) }

            // Re-run file search if there's an active query
            val query = _resultsState.value.query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.performSecondarySearches(query)
            }
        }
    }

    fun setShowFolders(show: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setShowFoldersInResults(show)
            showFolders = show
            updateUiState { it.copy(showFolders = show) }

            // Re-run file search if there's an active query
            val query = _resultsState.value.query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.performSecondarySearches(query)
            }
        }
    }

    fun setShowSystemFiles(show: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setShowSystemFiles(show)
            showSystemFiles = show
            updateUiState { it.copy(showSystemFiles = show) }

            // Re-run file search if there's an active query
            val query = _resultsState.value.query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.performSecondarySearches(query)
            }
        }
    }

    fun setFolderWhitelistPatterns(patterns: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setFolderWhitelistPatterns(patterns)
            folderWhitelistPatterns = userPreferences.getFolderWhitelistPatterns()
            updateUiState { it.copy(folderWhitelistPatterns = folderWhitelistPatterns) }

            val query = _resultsState.value.query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.performSecondarySearches(query)
            }
        }
    }

    fun setFolderBlacklistPatterns(patterns: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setFolderBlacklistPatterns(patterns)
            folderBlacklistPatterns = userPreferences.getFolderBlacklistPatterns()
            updateUiState { it.copy(folderBlacklistPatterns = folderBlacklistPatterns) }

            val query = _resultsState.value.query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.performSecondarySearches(query)
            }
        }
    }

    fun setOneHandedMode(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setOneHandedMode,
                stateUpdater = {
                    oneHandedMode = it
                    updateUiState { state -> state.copy(oneHandedMode = it) }
                    saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
                },
        )
    }

    fun setBottomSearchBarEnabled(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setBottomSearchBarEnabled,
                stateUpdater = {
                    bottomSearchBarEnabled = it
                    updateUiState { state -> state.copy(bottomSearchBarEnabled = it) }
                    saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
                },
        )
    }

    fun setOpenKeyboardOnLaunchEnabled(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setOpenKeyboardOnLaunchEnabled,
                stateUpdater = {
                    openKeyboardOnLaunch = it
                    updateUiState { state -> state.copy(openKeyboardOnLaunch = it) }
                    saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
                },
        )
    }

    fun setTopResultIndicatorEnabled(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setTopResultIndicatorEnabled,
                stateUpdater = {
                    topResultIndicatorEnabled = it
                    updateUiState { state -> state.copy(topResultIndicatorEnabled = it) }
                    saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
                },
        )
    }

    fun setWallpaperAccentEnabled(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setWallpaperAccentEnabled,
                stateUpdater = {
                    wallpaperAccentEnabled = it
                    updateUiState { state -> state.copy(wallpaperAccentEnabled = it) }
                },
        )
    }

    fun setClearQueryOnLaunchEnabled(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setClearQueryOnLaunchEnabled,
                stateUpdater = {
                    clearQueryOnLaunch = it
                    updateUiState { state -> state.copy(clearQueryOnLaunch = it) }
                },
        )
    }

    fun setAutoCloseOverlayEnabled(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setAutoCloseOverlayEnabled,
                stateUpdater = {
                    autoCloseOverlay = it
                    updateUiState { state -> state.copy(autoCloseOverlay = it) }
                },
        )
    }

    fun setOverlayModeEnabled(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setOverlayModeEnabled,
                stateUpdater = {
                    overlayModeEnabled = it
                    updateUiState { state ->
                        state.copy(
                                overlayModeEnabled = it,
                                wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                                wallpaperBlurRadius = wallpaperBlurRadius,
                        )
                    }
                    if (!it) {
                        OverlayModeController.stopOverlay(getApplication())
                    }
                },
        )
    }

    fun setOverlayBlurEffectEnabled(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setOverlayBlurEffectEnabled,
                stateUpdater = {
                    overlayBlurEffectEnabled = it
                    updateUiState { state -> state.copy(overlayBlurEffectEnabled = it) }
                },
        )
    }

    fun setAmazonDomain(domain: String?) {
        amazonDomain = domain
        userPreferences.setAmazonDomain(domain)
        updateFeatureState { state -> state.copy(amazonDomain = amazonDomain) }
    }

    fun setGeminiApiKey(apiKey: String?) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            directSearchHandler.setPersonalContext(context)
            updateFeatureState { it.copy(personalContext = context?.trim().orEmpty()) }
        }
    }

    fun setGeminiModel(modelId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            directSearchHandler.setGeminiGroundingEnabled(enabled)
            updateFeatureState { it.copy(geminiGroundingEnabled = enabled) }
        }
    }

    fun refreshAvailableGeminiModels() {
        viewModelScope.launch(Dispatchers.IO) {
            val models = directSearchHandler.refreshAvailableGeminiModels(forceRefresh = true)
            updateFeatureState { it.copy(availableGeminiModels = models) }
        }
    }

    /** Computes the overall screen visibility state based on current data and permissions. */
    private fun computeScreenVisibilityState(state: SearchUiState): ScreenVisibilityState =
            when {
                state.isInitializing -> {
                    ScreenVisibilityState.Initializing
                }
                state.isLoading -> {
                    ScreenVisibilityState.Loading
                }
                state.errorMessage != null -> {
                    ScreenVisibilityState.Error(state.errorMessage, canRetry = true)
                }
                !state.hasUsagePermission -> {
                    ScreenVisibilityState.NoPermissions
                }
                state.query.isBlank() &&
                        state.recentApps.isEmpty() &&
                        state.pinnedApps.isEmpty() -> {
                    ScreenVisibilityState.Empty
                }
                else -> {
                    ScreenVisibilityState.Content
                }
            }

    /** Computes the apps section visibility state. */
    private fun computeAppsSectionVisibility(state: SearchUiState): AppsSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.APPS)

        return when {
            !sectionEnabled -> {
                AppsSectionVisibility.Hidden
            }
            state.isInitializing || state.isLoading -> {
                AppsSectionVisibility.Loading
            }
            state.query.isBlank() -> {
                val hasContent = state.recentApps.isNotEmpty() || state.pinnedApps.isNotEmpty()
                if (hasContent) {
                    AppsSectionVisibility.ShowingResults(hasPinned = state.pinnedApps.isNotEmpty())
                } else {
                    AppsSectionVisibility.NoResults
                }
            }
            else -> {
                val hasResults = state.searchResults.isNotEmpty()
                if (hasResults) {
                    AppsSectionVisibility.ShowingResults(hasPinned = false)
                } else {
                    AppsSectionVisibility.NoResults
                }
            }
        }
    }

    /** Computes the app shortcuts section visibility state. */
    private fun computeAppShortcutsSectionVisibility(
            state: SearchUiState
    ): AppShortcutsSectionVisibility {
        val sectionEnabled =
                isSectionEnabledForCurrentQuery(state, SearchSection.APP_SHORTCUTS)

        return when {
            !sectionEnabled -> {
                AppShortcutsSectionVisibility.Hidden
            }
            else -> {
                val hasResults = state.appShortcutResults.isNotEmpty()
                val hasPinned = state.pinnedAppShortcuts.isNotEmpty()
                if (hasResults || hasPinned) {
                    AppShortcutsSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    AppShortcutsSectionVisibility.NoResults
                }
            }
        }
    }

    /** Computes the contacts section visibility state. */
    private fun computeContactsSectionVisibility(state: SearchUiState): ContactsSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.CONTACTS)

        return when {
            !sectionEnabled -> {
                ContactsSectionVisibility.Hidden
            }
            !state.hasContactPermission -> {
                ContactsSectionVisibility.NoPermission
            }
            else -> {
                val hasResults = state.contactResults.isNotEmpty()
                val hasPinned = state.pinnedContacts.isNotEmpty()
                if (hasResults || hasPinned) {
                    ContactsSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    ContactsSectionVisibility.NoResults
                }
            }
        }
    }

    /** Computes the files section visibility state. */
    private fun computeFilesSectionVisibility(state: SearchUiState): FilesSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.FILES)

        return when {
            !sectionEnabled -> {
                FilesSectionVisibility.Hidden
            }
            !state.hasFilePermission -> {
                FilesSectionVisibility.NoPermission
            }
            else -> {
                val hasResults = state.fileResults.isNotEmpty()
                val hasPinned = state.pinnedFiles.isNotEmpty()
                if (hasResults || hasPinned) {
                    FilesSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    FilesSectionVisibility.NoResults
                }
            }
        }
    }

    /** Computes the settings section visibility state. */
    private fun computeSettingsSectionVisibility(state: SearchUiState): SettingsSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.SETTINGS)
        val hasAppSettingResults = state.appSettingResults.isNotEmpty()
        val hasPinned = state.pinnedSettings.isNotEmpty()
        val hasDeviceSettingResults = state.settingResults.isNotEmpty()

        return when {
            hasAppSettingResults -> {
                SettingsSectionVisibility.ShowingResults(hasPinned = hasPinned)
            }
            !sectionEnabled -> {
                SettingsSectionVisibility.Hidden
            }
            else -> {
                if (hasDeviceSettingResults || hasPinned) {
                    SettingsSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    SettingsSectionVisibility.NoResults
                }
            }
        }
    }

    private fun computeCalendarSectionVisibility(state: SearchUiState): CalendarSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.CALENDAR)

        return when {
            !sectionEnabled -> {
                CalendarSectionVisibility.Hidden
            }
            !state.hasCalendarPermission -> {
                CalendarSectionVisibility.NoPermission
            }
            else -> {
                val hasResults = state.calendarEvents.isNotEmpty()
                val hasPinned = state.pinnedCalendarEvents.isNotEmpty()
                if (hasResults || hasPinned) {
                    CalendarSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    CalendarSectionVisibility.NoResults
                }
            }
        }
    }

    /** Computes the search engines visibility state. */
    private fun computeSearchEnginesVisibility(state: SearchUiState): SearchEnginesVisibility =
            when {
                state.detectedShortcutTarget != null -> {
                    SearchEnginesVisibility.ShortcutDetected(state.detectedShortcutTarget)
                }
                state.detectedAliasSearchSection != null -> {
                    SearchEnginesVisibility.Hidden
                }
                state.isCurrencyConverterAliasMode -> {
                    SearchEnginesVisibility.Hidden
                }
                state.isWordClockAliasMode -> {
                    SearchEnginesVisibility.Hidden
                }
                isLikelyWebUrl(state.query) -> {
                    SearchEnginesVisibility.Hidden
                }
                state.isSearchEngineCompactMode -> {
                    SearchEnginesVisibility.Compact
                }
                else -> {
                    SearchEnginesVisibility.Hidden
                }
            }

    private fun isSectionEnabledForCurrentQuery(
        state: SearchUiState,
        section: SearchSection,
    ): Boolean =
            section !in state.disabledSections || state.detectedAliasSearchSection == section

    /**
     * Computes and applies all visibility states to the given state. This is a pure function that
     * returns a new state with updated visibility fields.
     */
    private fun applyVisibilityStates(state: SearchUiState): SearchUiState =
            state.copy(
                    screenState = computeScreenVisibilityState(state),
                    appsSectionState = computeAppsSectionVisibility(state),
                    appShortcutsSectionState = computeAppShortcutsSectionVisibility(state),
                    contactsSectionState = computeContactsSectionVisibility(state),
                    filesSectionState = computeFilesSectionVisibility(state),
                    settingsSectionState = computeSettingsSectionVisibility(state),
                    calendarSectionState = computeCalendarSectionVisibility(state),
                    searchEnginesState = computeSearchEnginesVisibility(state),
            )

    /**
     * Updates all visibility states in the UI state. Call this whenever the underlying data changes
     * that could affect visibility.
     */
    private fun updateVisibilityStates() {
        updateUiState { currentState -> applyVisibilityStates(currentState) }
    }

    companion object {
        @Volatile private var inMemoryRetainedQuery: String = ""
        private const val MAX_RECENT_ITEMS = 10
        // Debounce for the background app-search job (ms).  Kept intentionally
        // shorter than the 150 ms secondary-search debounce because app results
        // are cheap relative to contact/file queries, but still eliminates the
        // redundant mid-word searches that occur during rapid typing.
        private const val APP_SEARCH_DEBOUNCE_MS = 60L
        private const val CURRENCY_CONVERTER_DEBOUNCE_MS = 450L
        /** Minimum interval between browser-target refreshes triggered by onResume. */
        private const val BROWSER_REFRESH_INTERVAL_MS = 5 * 60 * 1_000L // 5 minutes
        private const val DEFERRED_APP_REFRESH_DELAY_MS = 2_000L
        private const val DEFERRED_DIRECT_SEARCH_MODELS_DELAY_MS = 3_000L
        private const val DEFERRED_RELEASE_NOTES_DELAY_MS = 3_000L
    }

    private fun getGridItemCount(): Int =
            SearchScreenConstants.ROW_COUNT * getAppGridColumns(getApplication(), phoneAppGridColumns)

    private fun getSearchableAppsSnapshot(): List<AppInfo> {
        if (cachedAllSearchableApps.isNotEmpty()) return cachedAllSearchableApps

        val pinnedAppsForResults =
                appSearchManager.computePinnedApps(userPreferences.getResultHiddenPackages())
        val nonPinnedApps = appSearchManager.searchSourceApps()
        val fallback = (pinnedAppsForResults + nonPinnedApps).distinctBy { it.launchCountKey() }
        if (fallback.isNotEmpty()) {
            cachedAllSearchableApps = fallback
        }
        return fallback
    }

    /**
     * Recomputes only the app-suggestions / app-search part of derived state: nickname cache,
     * pinned apps, recents, search results, hidden-app lists, and icon prefetch. Does NOT touch
     * messaging/calling state and does NOT re-trigger secondary searches.
     *
     * Call this when the apps list or app preferences change but contacts/files/settings are
     * unaffected (e.g. pin/hide an app, toggle suggestions, resume without usage permission).
     */
    private fun refreshAppSuggestions(
            lastUpdated: Long? = null,
            isLoading: Boolean? = null,
    ) {
        // Refresh nicknames cache to ensure we have the latest data
        appSearchManager.refreshNicknames()

        val apps = appSearchManager.cachedApps
        val visibleAppList = appSearchManager.availableApps()
        val hasUsagePermission = _permissionState.value.hasUsagePermission
        val suggestionsEnabled = _configState.value.appSuggestionsEnabled

        // Cache these to avoid multiple SharedPreferences reads
        val suggestionHiddenPackages = userPreferences.getSuggestionHiddenPackages()
        val resultHiddenPackages = userPreferences.getResultHiddenPackages()
        val pinnedPackages = userPreferences.getPinnedPackages()

        if (suggestionsEnabled &&
                        !hasUsagePermission &&
                        userPreferences.getRecentAppLaunches().isEmpty()
        ) {
            val initialRecents =
                    visibleAppList
                            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
                            .take(getGridItemCount())
                            .map { it.launchCountKey() }
            userPreferences.setRecentAppLaunches(initialRecents)
        }

        val pinnedAppsForSuggestions = appSearchManager.computePinnedApps(emptySet())
        val pinnedAppsForResults = appSearchManager.computePinnedApps(resultHiddenPackages)
        val recents =
                if (suggestionsEnabled) {
                    val recentsSource =
                            visibleAppList.filterNot {
                                pinnedPackages.contains(it.launchCountKey())
                            }
                    extractSuggestedApps(
                            apps = recentsSource,
                            limit = getGridItemCount(),
                            hasUsagePermission = hasUsagePermission,
                    )
                } else {
                    emptyList()
                }

        val query = _resultsState.value.query
        val trimmedQuery = query.trim()

        // Always update the searchable apps cache regardless of query state
        // Include both pinned and non-pinned apps in search, let ranking determine order
        val nonPinnedApps = appSearchManager.searchSourceApps()
        val allSearchableApps =
                (pinnedAppsForResults + nonPinnedApps).distinctBy { it.launchCountKey() }
        cachedAllSearchableApps = allSearchableApps

        val searchResults =
                if (trimmedQuery.isBlank()) {
                    emptyList()
                } else {
                    appSearchManager.deriveMatches(
                            trimmedQuery,
                            allSearchableApps,
                            getGridItemCount(),
                    )
                }
        val suggestionHiddenAppList =
                apps.filter { suggestionHiddenPackages.contains(it.launchCountKey()) }.sortedBy {
                    it.appName.lowercase(Locale.getDefault())
                }
        val resultHiddenAppList =
                apps.filter { resultHiddenPackages.contains(it.launchCountKey()) }.sortedBy {
                    it.appName.lowercase(Locale.getDefault())
                }

        updateResultsState { state ->
            state.copy(
                    allApps = apps,
                    recentApps = recents,
                    searchResults = searchResults,
                    pinnedApps = pinnedAppsForSuggestions,
                    suggestionExcludedApps = suggestionHiddenAppList,
                    resultExcludedApps = resultHiddenAppList,
                    indexedAppCount = visibleAppList.size,
                    cacheLastUpdatedMillis = lastUpdated ?: state.cacheLastUpdatedMillis,
                    nicknameUpdateVersion = state.nicknameUpdateVersion + 1,
            )
        }
        if (isLoading != null) {
            updateConfigState { state -> state.copy(isLoading = isLoading) }
        }

        iconPackHandler.prefetchVisibleAppIcons(
                pinnedApps = pinnedAppsForSuggestions,
                recents = recents,
                searchResults = searchResults,
        )

        val hasStartupSuggestions = pinnedAppsForSuggestions.isNotEmpty() || recents.isNotEmpty()
        if (hasStartupSuggestions && !_configState.value.isStartupCoreSurfaceReady) {
            updateConfigState { it.copy(isStartupCoreSurfaceReady = true) }
        }
        saveStartupSurfaceSnapshotAsync()
    }

    /**
     * Recomputes messaging and calling app availability based on the currently installed app list.
     * Updates permission state only — does NOT touch app suggestions or secondary searches.
     *
     * Call this when the installed-apps list changes and we need to verify that the previously
     * selected messaging / calling app is still present.
     */
    private fun refreshMessagingState() {
        val apps = appSearchManager.cachedApps
        val packageNames = apps.map { it.packageName }.toSet()
        val isWhatsAppInstalled = packageNames.contains(PackageConstants.WHATSAPP_PACKAGE)
        val isTelegramInstalled = packageNames.contains(PackageConstants.TELEGRAM_PACKAGE)
        val isSignalInstalled = packageNames.contains(PackageConstants.SIGNAL_PACKAGE)
        val isGoogleMeetInstalled = packageNames.contains(PackageConstants.GOOGLE_MEET_PACKAGE)
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
        updatePermissionState { state ->
            state.copy(
                    messagingApp = resolvedMessagingApp,
                    callingApp = resolvedCallingApp,
                    isWhatsAppInstalled = isWhatsAppInstalled,
                    isTelegramInstalled = isTelegramInstalled,
                    isSignalInstalled = isSignalInstalled,
                    isGoogleMeetInstalled = isGoogleMeetInstalled,
            )
        }
    }

    /**
     * Re-triggers secondary searches (contacts, files, settings) for the current query. Used by
     * management handlers (contact/file/settings pin/exclude operations) so they don't have to
     * touch app-suggestion state at all.
     */
    private fun refreshSecondarySearches() {
        val query = _resultsState.value.query
        if (query.isNotBlank()) {
            secondarySearchOrchestrator.performSecondarySearches(query)
        }
    }

    /**
     * Full derived-state refresh: recomputes app suggestions, messaging state, and re-triggers
     * secondary searches. Use only when the installed app list changes (e.g. app
     * installed/uninstalled) or during startup, where all three concerns need updating together.
     */
    private fun refreshDerivedState(
            lastUpdated: Long? = null,
            isLoading: Boolean? = null,
    ) {
        refreshAppSuggestions(lastUpdated = lastUpdated, isLoading = isLoading)
        refreshMessagingState()

        // Re-trigger secondary searches when the app list changes and there's an active query
        val query = _resultsState.value.query
        if (query.isNotBlank()) {
            secondarySearchOrchestrator.performSecondarySearches(query)
        }
    }

    private fun saveStartupSurfaceSnapshotAsync(
            forcePreviewRefresh: Boolean = false,
            allowDuringQuery: Boolean = false,
    ) {
        if (!instantStartupSurfaceEnabled) return

        viewModelScope.launch(Dispatchers.IO) {
            val config = _configState.value
            val results = _resultsState.value
            val suggestionLimit = getGridItemCount().coerceAtLeast(1)
            val startupSuggestions =
                buildList {
                    addAll(results.pinnedApps)
                    addAll(results.recentApps)
                }
                    .distinctBy { it.launchCountKey() }
                    .take(suggestionLimit)

            if (!allowDuringQuery && results.query.isNotBlank()) {
                return@launch
            }

            val previewPath =
                if (config.backgroundSource == BackgroundSource.THEME) {
                    null
                } else if (forcePreviewRefresh || config.startupBackgroundPreviewPath.isNullOrBlank()) {
                    WallpaperUtils.saveStartupBackgroundPreview(
                        context = appContext,
                        backgroundSource = config.backgroundSource,
                        customImageUri = config.customImageUri,
                    )
                } else {
                    config.startupBackgroundPreviewPath
                }

            val snapshot =
                StartupSurfaceSnapshot(
                    createdAtMillis = System.currentTimeMillis(),
                    backgroundSource = config.backgroundSource,
                    showWallpaperBackground = config.showWallpaperBackground,
                    wallpaperBackgroundAlpha = config.wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = config.wallpaperBlurRadius,
                    appTheme = config.appTheme,
                    overlayThemeIntensity = config.overlayThemeIntensity,
                    customImageUri = config.customImageUri,
                    startupBackgroundPreviewPath = previewPath,
                    oneHandedMode = config.oneHandedMode,
                    bottomSearchBarEnabled = config.bottomSearchBarEnabled,
                    topResultIndicatorEnabled = config.topResultIndicatorEnabled,
                    openKeyboardOnLaunch = config.openKeyboardOnLaunch,
                    fontScaleMultiplier = config.fontScaleMultiplier,
                    showAppLabels = config.showAppLabels,
                    appSuggestionsEnabled = config.appSuggestionsEnabled,
                    phoneAppGridColumns = config.phoneAppGridColumns,
                    suggestedApps = startupSuggestions,
                )
            startupSurfaceStore.saveSnapshot(snapshot)

            if (previewPath != config.startupBackgroundPreviewPath) {
                withContext(Dispatchers.Main) {
                    updateConfigState { it.copy(startupBackgroundPreviewPath = previewPath) }
                }
            }
        }
    }

    fun handleOptionalPermissionChange() {
        handleOptionalPermissionChangeInternal(allowAppRefresh = true)
    }

    private fun handleOptionalPermissionChangeInternal(allowAppRefresh: Boolean) {
        val previousUsagePermission = _permissionState.value.hasUsagePermission
        val latestUsagePermission = repository.hasUsageAccess()
        val usagePermissionChanged = previousUsagePermission != latestUsagePermission

        if (usagePermissionChanged) {
            updatePermissionState { it.copy(hasUsagePermission = latestUsagePermission) }
            if (allowAppRefresh && latestUsagePermission) {
                refreshApps()
            } else if (allowAppRefresh) {
                // Permission revoked — recompute app suggestions (recents/pinned change without
                // usage data)
                refreshAppSuggestions()
            }
        }

        val optionalChanged = refreshOptionalPermissions()
        if ((optionalChanged || usagePermissionChanged) && _resultsState.value.query.isNotBlank()) {
            secondarySearchOrchestrator.performSecondarySearches(_resultsState.value.query)
        }
    }

    /**
     * Startup-safe permission refresh that avoids forcing expensive app refreshes during first
     * frame launch work.
     */
    fun refreshPermissionSnapshotAtLaunch() {
        viewModelScope.launch(Dispatchers.Default) {
            val latestUsagePermission = repository.hasUsageAccess()
            if (_permissionState.value.hasUsagePermission != latestUsagePermission) {
                updatePermissionState { it.copy(hasUsagePermission = latestUsagePermission) }
            }
            refreshOptionalPermissions()
        }
    }

    private fun extractSuggestedApps(
            apps: List<AppInfo>,
            limit: Int,
            hasUsagePermission: Boolean,
    ): List<AppInfo> {
        if (apps.isEmpty() || limit <= 0) return emptyList()

        val (recentInstallStart, recentInstallEnd) = getRecentInstallWindow()
        val recentInstalls =
                repository.extractRecentlyInstalledApps(apps, recentInstallStart, recentInstallEnd)

        val suggestions =
                if (hasUsagePermission) {
                    val recentlyOpened = repository.getRecentlyOpenedApps(apps)
                    val topRecent = recentlyOpened.firstOrNull()
                    val recentInstallsExcludingTop =
                            recentInstalls.filterNot {
                                it.launchCountKey() == topRecent?.launchCountKey()
                            }
                    val excludedPackages =
                            recentInstallsExcludingTop
                                    .asSequence()
                                    .map { it.launchCountKey() }
                                    .toSet()
                                    .let { packages ->
                                        topRecent?.launchCountKey()?.let { packages + it }
                                                ?: packages
                                    }
                    val remainingRecents =
                            recentlyOpened.filterNot {
                                excludedPackages.contains(it.launchCountKey())
                            }

                    buildList {
                        topRecent?.let { add(it) }
                        addAll(recentInstallsExcludingTop)
                        addAll(remainingRecents)
                    }
                } else {
                    val appByKey = apps.associateBy { it.launchCountKey() }
                    val recentlyOpened =
                            userPreferences
                                    .getRecentAppLaunches()
                                    .asSequence()
                                    .mapNotNull { appByKey[it] }
                                    .toList()
                    val topRecent = recentlyOpened.firstOrNull()
                    val recentInstallsExcludingTop =
                            recentInstalls.filterNot {
                                it.launchCountKey() == topRecent?.launchCountKey()
                            }
                    val excludedPackages =
                            recentInstallsExcludingTop
                                    .asSequence()
                                    .map { it.launchCountKey() }
                                    .toSet()
                                    .let { packages ->
                                        topRecent?.launchCountKey()?.let { packages + it }
                                                ?: packages
                                    }
                    val remainingRecents =
                            recentlyOpened.drop(1).filterNot {
                                excludedPackages.contains(it.launchCountKey())
                            }

                    buildList {
                        topRecent?.let { add(it) }
                        addAll(recentInstallsExcludingTop)
                        addAll(remainingRecents)
                    }
                }

        // If we have enough suggestions, return them
        if (suggestions.size >= limit) {
            return suggestions.take(limit)
        }

        // Otherwise, keep existing suggestions and fill remaining spots with additional apps
        val remainingSpots = limit - suggestions.size
        val suggestionPackageNames = suggestions.map { it.launchCountKey() }.toSet()

        // Fill remaining spots with apps sorted by launch count (most used first), excluding
        // already suggested ones
        val additionalApps =
                apps
                        .filterNot { suggestionPackageNames.contains(it.launchCountKey()) }
                        .sortedWith(
                                compareByDescending<AppInfo> { it.launchCount }.thenBy {
                                    it.appName.lowercase(Locale.getDefault())
                                },
                        )
                        .take(remainingSpots)

        return suggestions + additionalApps
    }

    private fun getRecentInstallWindow(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val startOfTomorrow = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        val startOfYesterday = calendar.timeInMillis
        return startOfYesterday to startOfTomorrow
    }

    fun onWebSuggestionTap(suggestion: String) {
        // Check if there's a detected shortcut engine and perform immediate search
        val currentState = _resultsState.value
        val detectedTarget = currentState.detectedShortcutTarget

        if (detectedTarget != null) {
            // Perform search immediately in the detected search target
            navigationHandler.openSearchTarget(suggestion.trim(), detectedTarget)
        } else {
            // No shortcut detected, copy the suggestion text to the search bar
            onQueryChange(suggestion)
        }

        // Mark that a web suggestion was selected so we can hide suggestions
        updateResultsState { it.copy(webSuggestionWasSelected = true) }
    }

    private fun refreshOptionalPermissions(): Boolean {
        val hasContacts = hasContactPermission()
        val hasFiles = hasFilePermission()
        val hasCalendar = hasCalendarPermission()
        val hasCall = hasCallPermission()
        val hasWallpaper = hasWallpaperPermission()
        val previousState = _permissionState.value
        val changed =
                previousState.hasContactPermission != hasContacts ||
                        previousState.hasFilePermission != hasFiles ||
                        previousState.hasCalendarPermission != hasCalendar ||
                        previousState.hasCallPermission != hasCall ||
                        previousState.hasWallpaperPermission != hasWallpaper ||
                        previousState.wallpaperAvailable != wallpaperAvailable

        if (changed) {
            // Auto-enable direct dial when call permission is granted, unless user manually
            // disabled it
            if (hasCall && !directDialEnabled && !userPreferences.isDirectDialManuallyDisabled()) {
                setDirectDialEnabled(true, manual = false)
            } else if (!hasCall && directDialEnabled) {
                // If permission is revoked, auto-disable it to avoid errors, but don't mark as
                // manually disabled
                setDirectDialEnabled(false, manual = false)
            }

            updatePermissionState { state ->
                state.copy(
                        hasContactPermission = hasContacts,
                        hasFilePermission = hasFiles,
                        hasCalendarPermission = hasCalendar,
                        hasCallPermission = hasCall,
                        hasWallpaperPermission = hasWallpaper,
                        wallpaperAvailable = wallpaperAvailable,
                )
            }
            updateFeatureState { state ->
                state.copy(directDialEnabled = this@SearchViewModel.directDialEnabled)
            }
            updateResultsState { state ->
                state.copy(
                        contactResults = if (hasContacts) state.contactResults else emptyList(),
                        fileResults = if (hasFiles) state.fileResults else emptyList(),
                        calendarEvents =
                                if (hasCalendar) state.calendarEvents else emptyList(),
                        pinnedCalendarEvents =
                                if (hasCalendar) state.pinnedCalendarEvents else emptyList(),
                        excludedCalendarEvents =
                                if (hasCalendar) state.excludedCalendarEvents else emptyList(),
                )
            }

            if (hasCalendar) {
                loadPinnedAndExcludedCalendarEvents()
            }

            // Refresh disabled sections based on new permission state
            sectionManager.refreshDisabledSections()
        }
        return changed
    }

    fun showContactMethodsBottomSheet(contactInfo: ContactInfo) {
        updateConfigState { it.copy(contactMethodsBottomSheet = contactInfo) }
    }

    fun dismissContactMethodsBottomSheet() {
        updateConfigState { it.copy(contactMethodsBottomSheet = null) }
    }

    fun onDirectDialChoiceSelected(
            option: DirectDialOption,
            rememberChoice: Boolean,
    ) {
        contactActionHandler.onDirectDialChoiceSelected(option, rememberChoice)
        // Update local state variables
        val useDirectDial = option == DirectDialOption.DIRECT_CALL
        if (rememberChoice) {
            setDirectDialEnabled(useDirectDial, manual = true)
        } else {
            hasSeenDirectDialChoice = true
            userPreferences.setHasSeenDirectDialChoice(true)
        }
    }

    fun onCallPermissionResult(
            isGranted: Boolean,
            shouldShowPermissionError: Boolean = true,
    ) {
        contactActionHandler.onCallPermissionResult(
                isGranted = isGranted,
                shouldShowPermissionError = shouldShowPermissionError,
        )
        // Refresh permission state after request result
        handleOptionalPermissionChange()
    }

    // Contact methods dialog helpers
    fun getLastShownPhoneNumber(contactId: Long): String? =
            userPreferences.getLastShownPhoneNumber(contactId)

    fun setLastShownPhoneNumber(
            contactId: Long,
            phoneNumber: String,
    ) {
        userPreferences.setLastShownPhoneNumber(contactId, phoneNumber)
    }

    // Usage permission banner management
    fun resetUsagePermissionBannerSessionDismissed() {
        userPreferences.resetUsagePermissionBannerSessionDismissed()
        updateFeatureState {
            it.copy(
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner(),
            )
        }
    }

    fun incrementUsagePermissionBannerDismissCount() {
        userPreferences.incrementUsagePermissionBannerDismissCount()
        updateFeatureState {
            it.copy(
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner(),
            )
        }
    }

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) {
        userPreferences.setUsagePermissionBannerSessionDismissed(dismissed)
        updateFeatureState {
            it.copy(
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner(),
            )
        }
    }

    override fun onCleared() {
        queryCoordinator.cancel()
        super.onCleared()
    }
}
