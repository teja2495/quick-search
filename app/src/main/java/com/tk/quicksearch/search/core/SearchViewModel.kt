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
import com.tk.quicksearch.search.appShortcuts.AppShortcutManagementHandler
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingsRepository
import com.tk.quicksearch.search.appSettings.AppSettingsSearchHandler
import com.tk.quicksearch.search.apps.AppManagementService
import com.tk.quicksearch.search.apps.AppSearchManager
import com.tk.quicksearch.search.apps.IconPackService
import com.tk.quicksearch.search.calendar.CalendarManagementHandler
import com.tk.quicksearch.search.common.PinningHandler
import com.tk.quicksearch.search.contacts.actions.ContactActionHandler
import com.tk.quicksearch.search.contacts.utils.ContactManagementHandler
import com.tk.quicksearch.search.contacts.utils.MessagingHandler
import com.tk.quicksearch.search.data.AppShortcutRepository.AppShortcutRepository
import com.tk.quicksearch.search.data.AppShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
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
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.startup.StartupSurfaceSnapshot
import com.tk.quicksearch.search.startup.StartupSurfaceStore
import com.tk.quicksearch.search.webSuggestions.WebSuggestionHandler
import com.tk.quicksearch.searchEngines.SearchEngineManager
import com.tk.quicksearch.searchEngines.SecondarySearchOrchestrator
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.isLowRamDevice
import com.tk.quicksearch.tools.aiTools.CurrencyConverterHandler
import com.tk.quicksearch.tools.aiTools.DictionaryHandler
import com.tk.quicksearch.tools.aiTools.WordClockHandler
import com.tk.quicksearch.tools.calculator.CalculatorHandler
import com.tk.quicksearch.tools.dateCalculator.DateCalculatorHandler
import com.tk.quicksearch.tools.directSearch.DirectSearchHandler
import com.tk.quicksearch.tools.unitConverter.UnitConverterHandler
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.Dispatchers
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
                            (startupSnapshot?.overlayThemeIntensity
                                            ?: startupPreferencesReader.getOverlayThemeIntensity())
                                    .coerceIn(
                                            UiPreferences.MIN_OVERLAY_THEME_INTENSITY,
                                            UiPreferences.MAX_OVERLAY_THEME_INTENSITY,
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
                            (startupSnapshot?.fontScaleMultiplier
                                            ?: startupPreferencesReader.getFontScaleMultiplier())
                                    .coerceIn(
                                            UiPreferences.MIN_FONT_SCALE_MULTIPLIER,
                                            UiPreferences.MAX_FONT_SCALE_MULTIPLIER,
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

    internal enum class ActiveInformationCard {
        DIRECT_SEARCH,
        CALCULATOR,
        CURRENCY_CONVERTER,
        WORD_CLOCK,
        DICTIONARY,
    }

    private val startupDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val runtimeState =
            SearchRuntimeState(
                    prefCache =
                            SearchPreferenceCache(
                                    clearQueryOnLaunch = initialConfigState.clearQueryOnLaunch,
                            ),
                    clearQueryOnLaunch = initialConfigState.clearQueryOnLaunch,
                    phoneAppGridColumns = UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
                    wallpaperBackgroundAlpha = UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA,
                    wallpaperBlurRadius = UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS,
                    overlayThemeIntensity = UiPreferences.DEFAULT_OVERLAY_THEME_INTENSITY,
                    fontScaleMultiplier = UiPreferences.DEFAULT_FONT_SCALE_MULTIPLIER,
            )

    private var cachedAllSearchableApps: List<AppInfo>
        get() = runtimeState.cachedAllSearchableApps
        set(value) {
            runtimeState.cachedAllSearchableApps = value
        }

    private var startupConfig: StartupPreferencesFacade.StartupConfig?
        get() = runtimeState.startupConfig
        set(value) {
            runtimeState.startupConfig = value
        }

    private val isAppShortcutsLoadInFlight: AtomicBoolean
        get() = runtimeState.isAppShortcutsLoadInFlight

    private val hasStartedStartupPhases: AtomicBoolean
        get() = runtimeState.hasStartedStartupPhases

    private var resumeNeedsStaticDataRefresh: Boolean
        get() = runtimeState.resumeNeedsStaticDataRefresh
        set(value) {
            runtimeState.resumeNeedsStaticDataRefresh = value
        }

    private var lastBrowserTargetRefreshMs: Long
        get() = runtimeState.lastBrowserTargetRefreshMs
        set(value) {
            runtimeState.lastBrowserTargetRefreshMs = value
        }
    private val uiStateMutationLock = ReentrantLock()

    private fun updateResultsState(updater: (SearchResultsState) -> SearchResultsState) {
        uiStateMutationLock.withLock { _resultsState.update(updater) }
    }

    private fun updatePermissionState(updater: (SearchPermissionState) -> SearchPermissionState) {
        uiStateMutationLock.withLock { _permissionState.update(updater) }
    }

    private fun updateFeatureState(updater: (SearchFeatureState) -> SearchFeatureState) {
        uiStateMutationLock.withLock { _featureState.update(updater) }
    }

    private fun updateConfigState(updater: (SearchUiConfigState) -> SearchUiConfigState) {
        uiStateMutationLock.withLock { _configState.update(updater) }
    }
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

            val withHint =
                    SearchStateExtractor.applyContactActionHint(
                            updated = updater(before),
                            hasSeenContactActionHint = userPreferences.hasSeenContactActionHint(),
                    )
            val after = if (isStartupComplete) applyVisibilityStates(withHint) else withHint

            // Write back each sub-state only if it actually changed
            val newResults = SearchStateExtractor.extractResultsState(after)
            if (newResults != currentResults) _resultsState.value = newResults

            val newPermissions = SearchStateExtractor.extractPermissionState(after)
            if (newPermissions != currentPermissions) _permissionState.value = newPermissions

            val newFeatures = SearchStateExtractor.extractFeatureState(after)
            if (newFeatures != currentFeatures) _featureState.value = newFeatures

            val newConfig = SearchStateExtractor.extractConfigState(after)
            if (newConfig != currentConfig) _configState.value = newConfig
        }
    }

    private val handlers: SearchHandlerContainer by lazy {
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

    private val environment by lazy {
        SearchViewModelEnvironment(
            application = application,
            appContext = appContext,
            scope = viewModelScope,
            startupSurfaceStore = startupSurfaceStore,
            userPreferences = userPreferences,
            repository = repository,
            appShortcutRepository = appShortcutRepository,
            calendarRepository = calendarRepository,
            contactRepository = contactRepository,
            fileRepository = fileRepository,
            settingsShortcutRepository = settingsShortcutRepository,
            appSettingsRepository = appSettingsRepository,
            launcherIconManager = launcherIconManager,
            contactPreferences = contactPreferences,
            permissionManager = permissionManager,
            searchOperations = searchOperations,
            startupDispatcher = startupDispatcher,
            getHandlers = { handlers },
            getUiState = { uiState.value },
            getResultsState = { _resultsState.value },
            getPermissionState = { _permissionState.value },
            getFeatureState = { _featureState.value },
            getConfigState = { _configState.value },
            updateUiState = this::updateUiState,
            updateResultsState = this::updateResultsState,
            updatePermissionState = this::updatePermissionState,
            updateFeatureState = this::updateFeatureState,
            updateConfigState = this::updateConfigState,
            showToastRes = this::showToast,
            showToastText = this::showToast,
            onNavigationTriggered = this::onNavigationTriggered,
        )
    }

    private val visibilityStateResolver by lazy { SearchVisibilityStateResolver() }

    private val appSuggestionSelector by lazy { AppSuggestionSelector(repository, userPreferences) }

    private val historyDelegate by lazy {
        SearchHistoryDelegate(
            scope = viewModelScope,
            userPreferences = userPreferences,
            contactRepository = contactRepository,
            fileRepository = fileRepository,
            settingsSearchHandler = settingsSearchHandler,
            appShortcutSearchHandler = appShortcutSearchHandler,
            appSettingsSearchHandler = appSettingsSearchHandler,
            calendarRepository = calendarRepository,
            featureStateProvider = { _featureState.value },
            updateResultsState = this::updateResultsState,
            updateUiState = this::updateUiState,
        )
    }

    private val contactActionsDelegate by lazy {
        SearchContactActionsDelegate(
            appContext = appContext,
            scope = viewModelScope,
            userPreferences = userPreferences,
            contactPreferences = contactPreferences,
            contactRepository = contactRepository,
            contactActionHandler = contactActionHandler,
            permissionStateProvider = { _permissionState.value },
            directSearchActiveProvider = { isDirectSearchActive() },
            updateResultsState = this::updateResultsState,
            updateConfigState = this::updateConfigState,
            showToastRes = this::showToast,
            setDirectDialEnabled = this::setDirectDialEnabled,
            handleOptionalPermissionChange = this::handleOptionalPermissionChange,
        )
    }

    private val staticDataDelegate by lazy {
        SearchStaticDataDelegate(
            scope = viewModelScope,
            userPreferences = userPreferences,
            repository = repository,
            appShortcutRepository = appShortcutRepository,
            contactRepository = contactRepository,
            fileRepository = fileRepository,
            calendarRepository = calendarRepository,
            handlersProvider = { handlers },
            resultsStateProvider = { _resultsState.value },
            isAppShortcutsLoadInFlight = isAppShortcutsLoadInFlight,
            hasCalendarPermission = this::hasCalendarPermission,
            updateUiState = this::updateUiState,
            updateResultsState = this::updateResultsState,
            updatePermissionState = this::updatePermissionState,
            showToastRes = this::showToast,
            refreshRecentItems = this::refreshRecentItems,
        )
    }

    private val preferencesStateAccess = object : SearchPreferencesStateAccess {
        override var enabledFileTypes: Set<FileType>
            get() = this@SearchViewModel.enabledFileTypes
            set(value) {
                this@SearchViewModel.enabledFileTypes = value
            }

        override var showFolders: Boolean
            get() = this@SearchViewModel.showFolders
            set(value) {
                this@SearchViewModel.showFolders = value
            }

        override var showSystemFiles: Boolean
            get() = this@SearchViewModel.showSystemFiles
            set(value) {
                this@SearchViewModel.showSystemFiles = value
            }

        override var folderWhitelistPatterns: Set<String>
            get() = this@SearchViewModel.folderWhitelistPatterns
            set(value) {
                this@SearchViewModel.folderWhitelistPatterns = value
            }

        override var folderBlacklistPatterns: Set<String>
            get() = this@SearchViewModel.folderBlacklistPatterns
            set(value) {
                this@SearchViewModel.folderBlacklistPatterns = value
            }

        override var oneHandedMode: Boolean
            get() = this@SearchViewModel.oneHandedMode
            set(value) {
                this@SearchViewModel.oneHandedMode = value
            }

        override var bottomSearchBarEnabled: Boolean
            get() = this@SearchViewModel.bottomSearchBarEnabled
            set(value) {
                this@SearchViewModel.bottomSearchBarEnabled = value
            }

        override var topResultIndicatorEnabled: Boolean
            get() = this@SearchViewModel.topResultIndicatorEnabled
            set(value) {
                this@SearchViewModel.topResultIndicatorEnabled = value
            }

        override var wallpaperAccentEnabled: Boolean
            get() = this@SearchViewModel.wallpaperAccentEnabled
            set(value) {
                this@SearchViewModel.wallpaperAccentEnabled = value
            }

        override var openKeyboardOnLaunch: Boolean
            get() = this@SearchViewModel.openKeyboardOnLaunch
            set(value) {
                this@SearchViewModel.openKeyboardOnLaunch = value
            }

        override var overlayModeEnabled: Boolean
            get() = this@SearchViewModel.overlayModeEnabled
            set(value) {
                this@SearchViewModel.overlayModeEnabled = value
            }

        override var autoCloseOverlay: Boolean
            get() = this@SearchViewModel.autoCloseOverlay
            set(value) {
                this@SearchViewModel.autoCloseOverlay = value
            }

        override var appSuggestionsEnabled: Boolean
            get() = this@SearchViewModel.appSuggestionsEnabled
            set(value) {
                this@SearchViewModel.appSuggestionsEnabled = value
            }

        override var showAppLabels: Boolean
            get() = this@SearchViewModel.showAppLabels
            set(value) {
                this@SearchViewModel.showAppLabels = value
            }

        override var phoneAppGridColumns: Int
            get() = this@SearchViewModel.phoneAppGridColumns
            set(value) {
                this@SearchViewModel.phoneAppGridColumns = value
            }

        override var appIconShape: AppIconShape
            get() = this@SearchViewModel.appIconShape
            set(value) {
                this@SearchViewModel.appIconShape = value
            }

        override var launcherAppIcon: LauncherAppIcon
            get() = this@SearchViewModel.launcherAppIcon
            set(value) {
                this@SearchViewModel.launcherAppIcon = value
            }

        override var themedIconsEnabled: Boolean
            get() = this@SearchViewModel.themedIconsEnabled
            set(value) {
                this@SearchViewModel.themedIconsEnabled = value
            }

        override var wallpaperBackgroundAlpha: Float
            get() = this@SearchViewModel.wallpaperBackgroundAlpha
            set(value) {
                this@SearchViewModel.wallpaperBackgroundAlpha = value
            }

        override var wallpaperBlurRadius: Float
            get() = this@SearchViewModel.wallpaperBlurRadius
            set(value) {
                this@SearchViewModel.wallpaperBlurRadius = value
            }

        override var appTheme: AppTheme
            get() = this@SearchViewModel.appTheme
            set(value) {
                this@SearchViewModel.appTheme = value
            }

        override var overlayThemeIntensity: Float
            get() = this@SearchViewModel.overlayThemeIntensity
            set(value) {
                this@SearchViewModel.overlayThemeIntensity = value
            }

        override var fontScaleMultiplier: Float
            get() = this@SearchViewModel.fontScaleMultiplier
            set(value) {
                this@SearchViewModel.fontScaleMultiplier = value
            }

        override var backgroundSource: BackgroundSource
            get() = this@SearchViewModel.backgroundSource
            set(value) {
                this@SearchViewModel.backgroundSource = value
            }

        override var customImageUri: String?
            get() = this@SearchViewModel.customImageUri
            set(value) {
                this@SearchViewModel.customImageUri = value
            }

        override var clearQueryOnLaunch: Boolean
            get() = this@SearchViewModel.clearQueryOnLaunch
            set(value) {
                this@SearchViewModel.clearQueryOnLaunch = value
            }

        override var amazonDomain: String?
            get() = this@SearchViewModel.amazonDomain
            set(value) {
                this@SearchViewModel.amazonDomain = value
            }

        override fun computeEffectiveIsDarkMode(): Boolean =
                this@SearchViewModel.computeEffectiveIsDarkMode()

        override fun applyLauncherIconSelection(selection: LauncherAppIcon?) {
            if (selection == null) {
                this@SearchViewModel.applyLauncherIconSelection()
            } else {
                this@SearchViewModel.applyLauncherIconSelection(selection)
            }
        }

        override fun saveStartupSurfaceSnapshotAsync(
                forcePreviewRefresh: Boolean,
                allowDuringQuery: Boolean,
        ) {
            this@SearchViewModel.saveStartupSurfaceSnapshotAsync(
                    forcePreviewRefresh = forcePreviewRefresh,
                    allowDuringQuery = allowDuringQuery,
            )
        }
    }

    private val preferencesDelegate by lazy {
        SearchPreferencesDelegate(
            scope = viewModelScope,
            applicationProvider = { getApplication() },
            userPreferences = userPreferences,
            directSearchHandler = directSearchHandler,
            searchEngineManager = searchEngineManager,
            iconPackHandler = iconPackHandler,
            secondarySearchOrchestrator = secondarySearchOrchestrator,
            resultsStateProvider = { _resultsState.value },
            updateUiState = this::updateUiState,
            updateConfigState = this::updateConfigState,
            updateFeatureState = this::updateFeatureState,
            updateResultsState = this::updateResultsState,
            refreshAppSuggestions = { refreshAppSuggestions() },
            refreshRecentItems = this::refreshRecentItems,
            stateAccess = preferencesStateAccess,
        )
    }

    private val startupLifecycleStateAccess = object : SearchStartupLifecycleStateAccess {
        override var pendingNavigationClear: Boolean
            get() = this@SearchViewModel.pendingNavigationClear
            set(value) {
                this@SearchViewModel.pendingNavigationClear = value
            }

        override var isStartupComplete: Boolean
            get() = this@SearchViewModel.isStartupComplete
            set(value) {
                this@SearchViewModel.isStartupComplete = value
            }

        override var resumeNeedsStaticDataRefresh: Boolean
            get() = this@SearchViewModel.resumeNeedsStaticDataRefresh
            set(value) {
                this@SearchViewModel.resumeNeedsStaticDataRefresh = value
            }

        override var lastBrowserTargetRefreshMs: Long
            get() = this@SearchViewModel.lastBrowserTargetRefreshMs
            set(value) {
                this@SearchViewModel.lastBrowserTargetRefreshMs = value
            }

        override var wallpaperAvailable: Boolean
            get() = this@SearchViewModel.wallpaperAvailable
            set(value) {
                this@SearchViewModel.wallpaperAvailable = value
            }

        override var directDialEnabled: Boolean
            get() = this@SearchViewModel.directDialEnabled
            set(value) {
                this@SearchViewModel.directDialEnabled = value
            }
    }

    private val startupLifecycleDelegate by lazy {
        SearchStartupLifecycleDelegate(
            scope = viewModelScope,
            applicationProvider = { getApplication() },
            repository = repository,
            userPreferences = userPreferences,
            handlersProvider = { handlers },
            resultsStateProvider = { _resultsState.value },
            permissionStateProvider = { _permissionState.value },
            configStateProvider = { _configState.value },
            stateAccess = startupLifecycleStateAccess,
            getStartupConfig = { startupConfig },
            setStartupConfig = { startupConfig = it },
            setPrefCache = { prefCache = it },
            readStartupPreferencesSnapshot = {
                SearchStartupPreferencesSnapshot(
                    oneHandedMode = oneHandedMode,
                    bottomSearchBarEnabled = bottomSearchBarEnabled,
                    topResultIndicatorEnabled = topResultIndicatorEnabled,
                    wallpaperAccentEnabled = wallpaperAccentEnabled,
                    openKeyboardOnLaunch = openKeyboardOnLaunch,
                    clearQueryOnLaunch = clearQueryOnLaunch,
                    autoCloseOverlay = autoCloseOverlay,
                    backgroundSource = backgroundSource,
                    wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = wallpaperBlurRadius,
                    appTheme = appTheme,
                    overlayThemeIntensity = overlayThemeIntensity,
                    appIconShape = appIconShape,
                    launcherAppIcon = launcherAppIcon,
                    themedIconsEnabled = themedIconsEnabled,
                    customImageUri = customImageUri,
                )
            },
            readLoadedPreferencesSnapshot = {
                SearchLoadedPreferencesSnapshot(
                    enabledFileTypes = enabledFileTypes,
                    oneHandedMode = oneHandedMode,
                    bottomSearchBarEnabled = bottomSearchBarEnabled,
                    topResultIndicatorEnabled = topResultIndicatorEnabled,
                    openKeyboardOnLaunch = openKeyboardOnLaunch,
                    clearQueryOnLaunch = clearQueryOnLaunch,
                    autoCloseOverlay = autoCloseOverlay,
                    overlayModeEnabled = overlayModeEnabled,
                    appSuggestionsEnabled = appSuggestionsEnabled,
                    showAppLabels = showAppLabels,
                    phoneAppGridColumns = phoneAppGridColumns,
                    appIconShape = appIconShape,
                    launcherAppIcon = launcherAppIcon,
                    themedIconsEnabled = themedIconsEnabled,
                    backgroundSource = backgroundSource,
                    wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = wallpaperBlurRadius,
                    appTheme = appTheme,
                    overlayThemeIntensity = overlayThemeIntensity,
                    fontScaleMultiplier = fontScaleMultiplier,
                    customImageUri = customImageUri,
                    showFolders = showFolders,
                    showSystemFiles = showSystemFiles,
                    folderWhitelistPatterns = folderWhitelistPatterns,
                    folderBlacklistPatterns = folderBlacklistPatterns,
                    excludedFileExtensions = excludedFileExtensions,
                    amazonDomain = amazonDomain,
                    directDialEnabled = directDialEnabled,
                    assistantLaunchVoiceModeEnabled = assistantLaunchVoiceModeEnabled,
                )
            },
            updatePermissionState = this::updatePermissionState,
            updateFeatureState = this::updateFeatureState,
            updateResultsState = this::updateResultsState,
            updateUiState = this::updateUiState,
            updateConfigState = this::updateConfigState,
            applyVisibilityStates = this::applyVisibilityStates,
            hasContactPermission = this::hasContactPermission,
            hasFilePermission = this::hasFilePermission,
            hasCalendarPermission = this::hasCalendarPermission,
            clearQuery = this::clearQuery,
            refreshApps = { refreshApps() },
            refreshAppSuggestions = { refreshAppSuggestions() },
            refreshSettingsState = { refreshSettingsState() },
            refreshAppShortcutsState = { refreshAppShortcutsState() },
            refreshDerivedState = this::refreshDerivedState,
            saveStartupSurfaceSnapshotAsync = this::saveStartupSurfaceSnapshotAsync,
            applyPreferenceCacheToLegacyVars = this::applyPreferenceCacheToLegacyVars,
            applyLauncherIconSelection = this::applyLauncherIconSelection,
            refreshRecentItems = this::refreshRecentItems,
            getGridItemCount = this::getGridItemCount,
            selectSuggestedApps = this::extractSuggestedApps,
            shouldShowSearchBarWelcome = this::shouldShowSearchBarWelcome,
            loadApps = this::loadApps,
            loadSettingsShortcuts = this::loadSettingsShortcuts,
            loadAppSettings = { appSettingsSearchHandler.loadSettings() },
            loadAppShortcuts = this::loadAppShortcuts,
            startupDispatcher = startupDispatcher,
            loadPinnedAndExcludedCalendarEvents = this::loadPinnedAndExcludedCalendarEvents,
            setDirectDialEnabled = this::setDirectDialEnabled,
        )
    }

    private val derivedStateDelegate: SearchDerivedStateDelegate by lazy {
        SearchDerivedStateDelegate(
            scope = viewModelScope,
            appContext = appContext,
            applicationProvider = { getApplication() },
            startupSurfaceStore = startupSurfaceStore,
            userPreferences = userPreferences,
            handlersProvider = { handlers },
            appSuggestionSelector = appSuggestionSelector,
            instantStartupSurfaceEnabled = instantStartupSurfaceEnabled,
            cachedAllSearchableAppsProvider = { cachedAllSearchableApps },
            setCachedAllSearchableApps = { cachedAllSearchableApps = it },
            resultsStateProvider = { _resultsState.value },
            permissionStateProvider = { _permissionState.value },
            configStateProvider = { _configState.value },
            updateResultsState = this::updateResultsState,
            updatePermissionState = this::updatePermissionState,
            updateConfigState = this::updateConfigState,
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
    private var autoCloseOverlay: Boolean = true
    private var directDialEnabled: Boolean = false
    private var assistantLaunchVoiceModeEnabled: Boolean = false
    private var hasSeenDirectDialChoice: Boolean = false
    private var appSuggestionsEnabled: Boolean = true
    private var showAppLabels: Boolean = true
    private var phoneAppGridColumns: Int = UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS
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

    private var wallpaperAvailable: Boolean = false
    private var shouldRecordPendingDirectSearchQueryInHistory: Boolean = true

    fun setWallpaperAvailable(available: Boolean) {
        startupLifecycleDelegate.setWallpaperAvailable(available)
    }

    fun markStartupCoreSurfaceReady() {
        startupLifecycleDelegate.markStartupCoreSurfaceReady()
    }

    fun handleOnStop() {
        startupLifecycleDelegate.handleOnStop()
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

    private suspend fun loadCacheAndMinimalPrefs() {
        startupLifecycleDelegate.loadCacheAndMinimalPrefs()
    }

    private suspend fun loadRemainingStartupPreferences() {
        startupLifecycleDelegate.loadRemainingStartupPreferences(this::applyStartupPreferences)
    }

    private fun applyStartupPreferences(prefs: StartupPreferencesFacade.StartupPreferences) {
        startupLifecycleDelegate.applyStartupPreferences(prefs)
    }

    private fun launchDeferredInitialization() {
        startupLifecycleDelegate.launchDeferredInitialization()
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

    fun setCalculatorEnabled(enabled: Boolean) = preferencesDelegate.setCalculatorEnabled(enabled)

    fun setUnitConverterEnabled(enabled: Boolean) = preferencesDelegate.setUnitConverterEnabled(enabled)

    fun setDateCalculatorEnabled(enabled: Boolean) = preferencesDelegate.setDateCalculatorEnabled(enabled)

    fun setCurrencyConverterEnabled(enabled: Boolean) =
            preferencesDelegate.setCurrencyConverterEnabled(enabled)

    fun setWordClockEnabled(enabled: Boolean) = preferencesDelegate.setWordClockEnabled(enabled)

    fun setDictionaryEnabled(enabled: Boolean) = preferencesDelegate.setDictionaryEnabled(enabled)

    fun dismissOverlayAssistantTip() = preferencesDelegate.dismissOverlayAssistantTip()

    fun setAppSuggestionsEnabled(enabled: Boolean) =
            preferencesDelegate.setAppSuggestionsEnabled(enabled)

    fun setShowAppLabels(show: Boolean) = preferencesDelegate.setShowAppLabels(show)

    fun setPhoneAppGridColumns(columns: Int) = preferencesDelegate.setPhoneAppGridColumns(columns)

    fun setWebSuggestionsEnabled(enabled: Boolean) = webSuggestionHandler.setEnabled(enabled)

    fun setWebSuggestionsCount(count: Int) = preferencesDelegate.setWebSuggestionsCount(count)

    fun setRecentQueriesEnabled(enabled: Boolean) = preferencesDelegate.setRecentQueriesEnabled(enabled)

    fun dismissSearchHistoryTip() = preferencesDelegate.dismissSearchHistoryTip()

    private fun refreshRecentItems() {
        historyDelegate.refreshRecentItems()
    }

    private fun refreshAliasRecentItems(section: SearchSection?) {
        historyDelegate.refreshAliasRecentItems(section)
    }

    fun deleteRecentItem(entry: RecentSearchEntry) {
        historyDelegate.deleteRecentItem(entry, lockedAliasSearchSection)
    }

    // Contact Card Actions (Synchronous access for UI composition, persistence is sync in prefs)
    fun getPrimaryContactCardAction(contactId: Long) =
            contactActionsDelegate.getPrimaryContactCardAction(contactId)

    fun setPrimaryContactCardAction(
            contactId: Long,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
    ) {
        contactActionsDelegate.setPrimaryContactCardAction(contactId, action)
    }

    fun getSecondaryContactCardAction(contactId: Long) =
            contactActionsDelegate.getSecondaryContactCardAction(contactId)

    fun setSecondaryContactCardAction(
            contactId: Long,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
    ) {
        contactActionsDelegate.setSecondaryContactCardAction(contactId, action)
    }

    fun requestContactActionPicker(
            contactId: Long,
            isPrimary: Boolean,
            serializedAction: String?,
    ) {
        contactActionsDelegate.requestContactActionPicker(contactId, isPrimary, serializedAction)
    }

    fun clearContactActionPickerRequest() {
        contactActionsDelegate.clearContactActionPickerRequest()
    }

    fun onCustomAction(
            contactInfo: ContactInfo,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
    ) {
        contactActionsDelegate.onCustomAction(contactInfo, action)
    }

    private fun loadApps() {
        staticDataDelegate.loadApps()
    }

    private fun loadSettingsShortcuts() {
        staticDataDelegate.loadSettingsShortcuts()
    }

    private fun loadAppShortcuts() {
        staticDataDelegate.loadAppShortcuts()
    }

    fun refreshAppShortcutsCacheFirst() {
        staticDataDelegate.refreshAppShortcutsCacheFirst()
    }

    private fun refreshSettingsState(updateResults: Boolean = true) {
        staticDataDelegate.refreshSettingsState(updateResults)
    }

    private fun refreshAppShortcutsState(updateResults: Boolean = true) {
        staticDataDelegate.refreshAppShortcutsState(updateResults)
    }

    fun refreshUsageAccess() {
        staticDataDelegate.refreshUsageAccess()
    }

    fun refreshApps(
            showToast: Boolean = false,
            forceUiUpdate: Boolean = false,
    ) {
        staticDataDelegate.refreshApps(showToast, forceUiUpdate)
    }

    fun refreshContacts(showToast: Boolean = false) {
        staticDataDelegate.refreshContacts(showToast)
    }

    fun refreshFiles(showToast: Boolean = false) {
        staticDataDelegate.refreshFiles(showToast)
    }

    private fun loadPinnedAndExcludedCalendarEvents() {
        staticDataDelegate.loadPinnedAndExcludedCalendarEvents()
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

    fun executeCurrencyConversion() = toolCoordinator.executeCurrencyConversion()

    fun executeWordClockLookup() = toolCoordinator.executeWordClockLookup()

    fun executeDictionaryLookup() = toolCoordinator.executeDictionaryLookup()

    fun activateSearchSectionFilter(section: SearchSection) =
            queryCoordinator.activateSearchSectionFilter(section)

    fun clearDetectedShortcut() = queryCoordinator.clearDetectedShortcut()

    fun clearQuery() = queryCoordinator.clearQuery()

    fun consumeRetainedQuerySelectionRequest() {
        if (_configState.value.selectRetainedQuery) {
            updateConfigState { it.copy(selectRetainedQuery = false) }
        }
    }

    fun onSettingsImported() {
        startupLifecycleDelegate.onSettingsImported(
                applyStartupPreferences = this::applyStartupPreferences,
                handleOnResume = this::handleOnResume,
        )
    }

    fun handleOnResume() {
        startupLifecycleDelegate.handleOnResume()
    }

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
    ) = staticDataDelegate.setAppShortcutEnabled(shortcut, enabled)

    fun setAppShortcutIconOverride(
            shortcut: StaticShortcut,
            iconBase64: String?,
    ) = staticDataDelegate.setAppShortcutIconOverride(shortcut, iconBase64)

    fun getAppShortcutIconOverride(shortcutId: String): String? =
            staticDataDelegate.getAppShortcutIconOverride(shortcutId)

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
        staticDataDelegate.addCustomAppShortcutFromPickerResult(
                resultData = resultData,
                sourcePackageName = sourcePackageName,
                showDefaultToast = showDefaultToast,
                onShortcutAdded = onShortcutAdded,
                onAddFailed = onAddFailed,
        )
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
        staticDataDelegate.addSearchTargetQueryShortcut(
                target = target,
                shortcutName = shortcutName,
                shortcutQuery = shortcutQuery,
                mode = mode,
                showDefaultToast = showDefaultToast,
                onShortcutAdded = onShortcutAdded,
                onAddFailed = onAddFailed,
        )
    }

    fun addCustomAppActivityShortcut(
            packageName: String,
            activityClassName: String,
            activityLabel: String,
            showDefaultToast: Boolean = true,
            onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
            onAddFailed: (() -> Unit)? = null,
    ) {
        staticDataDelegate.addCustomAppActivityShortcut(
                packageName = packageName,
                activityClassName = activityClassName,
                activityLabel = activityLabel,
                showDefaultToast = showDefaultToast,
                onShortcutAdded = onShortcutAdded,
                onAddFailed = onAddFailed,
        )
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
        staticDataDelegate.addCustomAppDeepLinkShortcut(
                packageName = packageName,
                shortcutName = shortcutName,
                deepLink = deepLink,
                iconBase64 = iconBase64,
                showDefaultToast = showDefaultToast,
                onShortcutAdded = onShortcutAdded,
                onAddFailed = onAddFailed,
        )
    }

    fun deleteCustomAppShortcut(shortcut: StaticShortcut) =
            staticDataDelegate.deleteCustomAppShortcut(shortcut)

    fun updateCustomAppShortcut(
            shortcut: StaticShortcut,
            shortcutName: String,
            shortcutValue: String?,
            iconBase64: String?,
    ) {
        staticDataDelegate.updateCustomAppShortcut(shortcut, shortcutName, shortcutValue, iconBase64)
    }

    fun clearAllExclusions() = staticDataDelegate.clearAllExclusions()

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

    fun setWallpaperBackgroundAlpha(alpha: Float) = preferencesDelegate.setWallpaperBackgroundAlpha(alpha)

    fun setWallpaperBlurRadius(radius: Float) = preferencesDelegate.setWallpaperBlurRadius(radius)

    fun setAppTheme(theme: AppTheme) = preferencesDelegate.setAppTheme(theme)

    fun setAppThemeMode(theme: AppThemeMode) = preferencesDelegate.setAppThemeMode(theme)

    fun setOverlayThemeIntensity(intensity: Float) =
            preferencesDelegate.setOverlayThemeIntensity(intensity)

    fun setFontScaleMultiplier(multiplier: Float) =
            preferencesDelegate.setFontScaleMultiplier(multiplier)

    fun setBackgroundSource(source: BackgroundSource) = preferencesDelegate.setBackgroundSource(source)

    fun setCustomImageUri(uri: String?) = preferencesDelegate.setCustomImageUri(uri)

    fun refreshIconPacks() = iconPackHandler.refreshIconPacks()

    fun setIconPackPackage(packageName: String?) = preferencesDelegate.setIconPackPackage(packageName)

    fun setAppIconShape(shape: AppIconShape) = preferencesDelegate.setAppIconShape(shape)

    fun setLauncherAppIcon(selection: LauncherAppIcon) =
            preferencesDelegate.setLauncherAppIcon(selection)

    fun onSystemDarkModeChanged(isDarkMode: Boolean) {
        preferencesDelegate.onSystemDarkModeChanged(isDarkMode, _configState.value.appThemeMode)
    }

    fun setThemedIconsEnabled(enabled: Boolean) = preferencesDelegate.setThemedIconsEnabled(enabled)

    private fun applyLauncherIconSelection(selection: LauncherAppIcon = launcherAppIcon) {
        launcherIconManager.applySelection(
                selection = selection,
                appTheme = appTheme,
                isDarkMode = computeEffectiveIsDarkMode(),
        )
    }

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

    fun setSectionEnabled(
            section: SearchSection,
            enabled: Boolean,
    ) = sectionManager.setSectionEnabled(section, enabled)

    fun canEnableSection(section: SearchSection): Boolean = sectionManager.canEnableSection(section)

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

    fun setShowStartSearchingOnOnboarding(show: Boolean) =
            updateConfigState { it.copy(showStartSearchingOnOnboarding = show) }

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
        contactActionsDelegate.callContact(contactInfo)

    fun smsContact(contactInfo: ContactInfo) =
        contactActionsDelegate.smsContact(contactInfo)

    fun onPhoneNumberSelected(
            phoneNumber: String,
            rememberChoice: Boolean,
    ) = contactActionHandler.onPhoneNumberSelected(phoneNumber, rememberChoice)

    fun dismissPhoneNumberSelection() = contactActionHandler.dismissPhoneNumberSelection()

    fun dismissDirectDialChoice() = contactActionHandler.dismissDirectDialChoice()

    fun handleContactMethod(
            contactInfo: ContactInfo,
            method: ContactMethod,
    ) = contactActionsDelegate.handleContactMethod(contactInfo, method)

    fun trackRecentContactTap(contactInfo: ContactInfo) = historyDelegate.trackRecentContactTap(contactInfo)

    fun trackRecentSettingTap(settingId: String) = historyDelegate.trackRecentSettingTap(settingId)

    fun trackRecentAppSettingTap(settingId: String) =
            historyDelegate.trackRecentAppSettingTap(settingId, lockedAliasSearchSection)

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

    fun setSearchEngineAliasSuffixEnabled(enabled: Boolean) =
            preferencesDelegate.setSearchEngineAliasSuffixEnabled(enabled)

    fun setAliasTriggerAfterSpaceEnabled(enabled: Boolean) =
            preferencesDelegate.setAliasTriggerAfterSpaceEnabled(enabled)

    fun setFileTypeEnabled(
            fileType: FileType,
            enabled: Boolean,
    ) = preferencesDelegate.setFileTypeEnabled(fileType, enabled)

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

    fun refreshAvailableGeminiModels() = preferencesDelegate.refreshAvailableGeminiModels()

    private fun applyVisibilityStates(state: SearchUiState): SearchUiState =
            visibilityStateResolver.apply(state)

    private fun updateVisibilityStates() = updateUiState { currentState -> applyVisibilityStates(currentState) }

    companion object {
        @Volatile private var inMemoryRetainedQuery: String = ""
        private const val MAX_RECENT_ITEMS = 10
        // Debounce for the background app-search job (ms).  Kept intentionally
        // shorter than the 150 ms secondary-search debounce because app results
        // are cheap relative to contact/file queries, but still eliminates the
        // redundant mid-word searches that occur during rapid typing.
        private const val APP_SEARCH_DEBOUNCE_MS = 60L
        private const val CURRENCY_CONVERTER_DEBOUNCE_MS = 450L
    }

    private fun getGridItemCount(): Int =
            derivedStateDelegate.getGridItemCount()

    private fun getSearchableAppsSnapshot(): List<AppInfo> = derivedStateDelegate.getSearchableAppsSnapshot()

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
        derivedStateDelegate.refreshAppSuggestions(lastUpdated = lastUpdated, isLoading = isLoading)
    }

    /**
     * Recomputes messaging and calling app availability based on the currently installed app list.
     * Updates permission state only — does NOT touch app suggestions or secondary searches.
     *
     * Call this when the installed-apps list changes and we need to verify that the previously
     * selected messaging / calling app is still present.
     */
    private fun refreshMessagingState() = derivedStateDelegate.refreshMessagingState()

    /**
     * Re-triggers secondary searches (contacts, files, settings) for the current query. Used by
     * management handlers (contact/file/settings pin/exclude operations) so they don't have to
     * touch app-suggestion state at all.
     */
    private fun refreshSecondarySearches() = derivedStateDelegate.refreshSecondarySearches()

    /**
     * Full derived-state refresh: recomputes app suggestions, messaging state, and re-triggers
     * secondary searches. Use only when the installed app list changes (e.g. app
     * installed/uninstalled) or during startup, where all three concerns need updating together.
     */
    private fun refreshDerivedState(
            lastUpdated: Long? = null,
            isLoading: Boolean? = null,
    ) {
        derivedStateDelegate.refreshDerivedState(lastUpdated = lastUpdated, isLoading = isLoading)
    }

    private fun saveStartupSurfaceSnapshotAsync(
            forcePreviewRefresh: Boolean = false,
            allowDuringQuery: Boolean = false,
    ) {
        derivedStateDelegate.saveStartupSurfaceSnapshotAsync(
            forcePreviewRefresh = forcePreviewRefresh,
            allowDuringQuery = allowDuringQuery,
        )
    }

    fun handleOptionalPermissionChange() = startupLifecycleDelegate.handleOptionalPermissionChange()

    fun refreshPermissionSnapshotAtLaunch() = startupLifecycleDelegate.refreshPermissionSnapshotAtLaunch()

    private fun extractSuggestedApps(
            apps: List<AppInfo>,
            limit: Int,
            hasUsagePermission: Boolean,
    ): List<AppInfo> =
            appSuggestionSelector.selectSuggestedApps(
                    apps = apps,
                    limit = limit,
                    hasUsagePermission = hasUsagePermission,
            )

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

    fun showContactMethodsBottomSheet(contactInfo: ContactInfo) =
            contactActionsDelegate.showContactMethodsBottomSheet(contactInfo)

    fun dismissContactMethodsBottomSheet() = contactActionsDelegate.dismissContactMethodsBottomSheet()

    fun onDirectDialChoiceSelected(
            option: DirectDialOption,
            rememberChoice: Boolean,
    ) {
        contactActionsDelegate.onDirectDialChoiceSelected(option, rememberChoice)
        if (!rememberChoice) {
            hasSeenDirectDialChoice = true
        }
    }

    fun onCallPermissionResult(
            isGranted: Boolean,
            shouldShowPermissionError: Boolean = true,
    ) = contactActionsDelegate.onCallPermissionResult(isGranted, shouldShowPermissionError)

    fun getLastShownPhoneNumber(contactId: Long): String? =
            contactActionsDelegate.getLastShownPhoneNumber(contactId)

    fun setLastShownPhoneNumber(
            contactId: Long,
            phoneNumber: String,
    ) = contactActionsDelegate.setLastShownPhoneNumber(contactId, phoneNumber)

    fun resetUsagePermissionBannerSessionDismissed() {
        userPreferences.resetUsagePermissionBannerSessionDismissed()
        refreshUsagePermissionBannerState()
    }

    fun incrementUsagePermissionBannerDismissCount() {
        userPreferences.incrementUsagePermissionBannerDismissCount()
        refreshUsagePermissionBannerState()
    }

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) {
        userPreferences.setUsagePermissionBannerSessionDismissed(dismissed)
        refreshUsagePermissionBannerState()
    }

    private fun refreshUsagePermissionBannerState() {
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
