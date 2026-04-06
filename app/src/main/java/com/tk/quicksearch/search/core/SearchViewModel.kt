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
import com.tk.quicksearch.search.data.NotesRepository
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
import kotlin.jvm.JvmName
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
class SearchViewModel(
        application: Application,
) : AndroidViewModel(application),
    SearchViewModelPreferencesApi,
    SearchViewModelNavigationApi,
    SearchViewModelManagementApi,
    SearchViewModelContactActionsApi,
    SearchViewModelSearchEngineApi {
    private val appContext = application.applicationContext
    private val startupPreferencesReader = UserAppPreferences(appContext)
    private val startupSurfaceStore = StartupSurfaceStore(appContext)
    private val initialState =
        SearchViewModelInitialStateFactory.create(
            appContext = appContext,
            startupPreferencesReader = startupPreferencesReader,
            startupSurfaceStore = startupSurfaceStore,
            inMemoryRetainedQuery = inMemoryRetainedQuery,
        )
    private val instantStartupSurfaceEnabled = initialState.instantStartupSurfaceEnabled
    private val initialResultsState = initialState.resultsState
    private val initialConfigState = initialState.configState
    private val repository by lazy { AppsRepository(appContext) }
    private val appShortcutRepository by lazy {
        AppShortcutRepository(appContext)
    }
    private val calendarRepository by lazy { CalendarRepository(appContext) }
    private val contactRepository by lazy { ContactRepository(appContext) }
    private val fileRepository by lazy { FileSearchRepository(appContext) }
    private val notesRepository by lazy { NotesRepository(appContext) }
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
            MutableStateFlow(initialState.featureState)
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
                                            features = initialState.featureState,
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
    private val startupState = SearchViewModelStartupState()
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
        get() = startupState.resumeNeedsStaticDataRefresh
        set(value) {
            startupState.resumeNeedsStaticDataRefresh = value
        }
    private var lastBrowserTargetRefreshMs: Long
        get() = startupState.lastBrowserTargetRefreshMs
        set(value) {
            startupState.lastBrowserTargetRefreshMs = value
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
    private val handlers: SearchHandlerContainer by lazy { SearchHandlerContainer(application = application, appContext = appContext, userPreferences = userPreferences, scope = viewModelScope, repository = repository, contactRepository = contactRepository, fileRepository = fileRepository, calendarRepository = calendarRepository, notesRepository = notesRepository, appShortcutRepository = appShortcutRepository, settingsShortcutRepository = settingsShortcutRepository, appSettingsRepository = appSettingsRepository, permissionManager = permissionManager, searchOperations = searchOperations, startupDispatcher = startupDispatcher, updateUiState = this::updateUiState, updateConfigState = this::updateConfigState, refreshSecondarySearches = this::refreshSecondarySearches, refreshAppShortcutsState = this::refreshAppShortcutsState, refreshAppSuggestions = this::refreshAppSuggestions, refreshDerivedState = this::refreshDerivedState, showToast = this::showToast, currentStateProvider = { uiState.value }, isLowRamDevice = isLowRamDevice(appContext)) }
    private val startupCoordinator by lazy { SearchStartupCoordinator(scope = viewModelScope, hasStartedStartupPhases = hasStartedStartupPhases, instantStartupSurfaceEnabled = instantStartupSurfaceEnabled, updateStartupPhase = { phase -> updateConfigState { it.copy(startupPhase = phase) } }, preloadBackgroundForInitialSearchSurface = this::preloadBackgroundForInitialSearchSurface, loadCacheAndMinimalPrefsBlock = this::loadCacheAndMinimalPrefs, loadRemainingStartupPreferencesBlock = this::loadRemainingStartupPreferences, launchDeferredInitializationBlock = this::launchDeferredInitialization) }
    private val toolCoordinator by lazy { SearchToolCoordinator(appContext = appContext, scope = viewModelScope, workerDispatcher = Dispatchers.Default, userPreferences = userPreferences, calculatorHandler = handlers.calculatorHandler, unitConverterHandler = handlers.unitConverterHandler, dateCalculatorHandler = handlers.dateCalculatorHandler, currencyConverterHandler = handlers.currencyConverterHandler, wordClockHandler = handlers.wordClockHandler, dictionaryHandler = handlers.dictionaryHandler, toolAliasStateProvider = { ToolAliasState(lockedToolMode = lockedToolMode, lockedCurrencyConverterAlias = lockedCurrencyConverterAlias, lockedWordClockAlias = lockedWordClockAlias, lockedDictionaryAlias = lockedDictionaryAlias) }, hasGeminiApiKeyProvider = { _featureState.value.hasGeminiApiKey }, currentQueryProvider = { _resultsState.value.query }, clearInformationCardsExcept = this::clearInformationCardsExcept, updateResultsState = this::updateResultsState, showToast = this::showToast) }
    private val queryCoordinator by lazy { SearchQueryCoordinator(scope = viewModelScope, workerDispatcher = Dispatchers.Default, handlers = handlers, toolCoordinator = toolCoordinator, userPreferences = userPreferences, appSearchDebounceMs = APP_SEARCH_DEBOUNCE_MS, aliasStateProvider = { SearchQueryAliasState(lockedShortcutTarget = lockedShortcutTarget, lockedAliasSearchSection = lockedAliasSearchSection, lockedToolMode = lockedToolMode, lockedCurrencyConverterAlias = lockedCurrencyConverterAlias, lockedWordClockAlias = lockedWordClockAlias, lockedDictionaryAlias = lockedDictionaryAlias) }, updateAliasState = { state -> lockedShortcutTarget = state.lockedShortcutTarget; lockedAliasSearchSection = state.lockedAliasSearchSection; lockedToolMode = state.lockedToolMode; lockedCurrencyConverterAlias = state.lockedCurrencyConverterAlias; lockedWordClockAlias = state.lockedWordClockAlias; lockedDictionaryAlias = state.lockedDictionaryAlias }, currentResultsStateProvider = { _resultsState.value }, updateUiState = this::updateUiState, updateResultsState = this::updateResultsState, clearInformationCardsExcept = this::clearInformationCardsExcept, getSearchableAppsSnapshot = this::getSearchableAppsSnapshot, getGridItemCount = this::getGridItemCount, loadAppShortcuts = this::loadAppShortcuts, refreshRecentItems = this::refreshRecentItems, refreshAliasRecentItems = this::refreshAliasRecentItems) }
    private val visibilityStateResolver by lazy { SearchVisibilityStateResolver() }
    private val appSuggestionSelector by lazy { AppSuggestionSelector(repository, userPreferences) }
    private val historyDelegate by lazy { SearchHistoryDelegate(scope = viewModelScope, userPreferences = userPreferences, contactRepository = contactRepository, fileRepository = fileRepository, settingsSearchHandler = handlers.settingsSearchHandler, appShortcutSearchHandler = handlers.appShortcutSearchHandler, appSettingsSearchHandler = handlers.appSettingsSearchHandler, calendarRepository = calendarRepository, featureStateProvider = { _featureState.value }, updateResultsState = this::updateResultsState, updateUiState = this::updateUiState) }
    private val contactActionsDelegate by lazy { SearchContactActionsDelegate(appContext = appContext, scope = viewModelScope, userPreferences = userPreferences, contactPreferences = contactPreferences, contactRepository = contactRepository, contactActionHandler = handlers.contactActionHandler, permissionStateProvider = { _permissionState.value }, directSearchActiveProvider = { isDirectSearchActive() }, updateResultsState = this::updateResultsState, updateConfigState = this::updateConfigState, showToastRes = this::showToast, setDirectDialEnabled = this::setDirectDialEnabled, handleOptionalPermissionChange = this::handleOptionalPermissionChange) }
    private val staticDataDelegate by lazy { SearchStaticDataDelegate(scope = viewModelScope, userPreferences = userPreferences, repository = repository, appShortcutRepository = appShortcutRepository, contactRepository = contactRepository, fileRepository = fileRepository, calendarRepository = calendarRepository, handlersProvider = { handlers }, resultsStateProvider = { _resultsState.value }, isAppShortcutsLoadInFlight = isAppShortcutsLoadInFlight, hasCalendarPermission = this::hasCalendarPermission, updateUiState = this::updateUiState, updateResultsState = this::updateResultsState, updatePermissionState = this::updatePermissionState, showToastRes = this::showToast, refreshRecentItems = this::refreshRecentItems) }
    private val legacyPreferenceState =
        SearchViewModelLegacyPreferenceState(clearQueryOnLaunch = initialConfigState.clearQueryOnLaunch)
    private val preferencesStateAccess =
        SearchViewModelPreferencesStateAccess(
            state = legacyPreferenceState,
            computeEffectiveIsDarkMode = this::computeEffectiveIsDarkMode,
            applyLauncherIconSelection = { selection ->
                if (selection == null) {
                    applyLauncherIconSelection()
                } else {
                    applyLauncherIconSelection(selection)
                }
            },
            saveStartupSurfaceSnapshotCallback = this::saveStartupSurfaceSnapshotAsync,
        )
    private val preferencesDelegate by lazy { SearchPreferencesDelegate(scope = viewModelScope, applicationProvider = { getApplication() }, userPreferences = userPreferences, directSearchHandler = handlers.directSearchHandler, searchEngineManager = handlers.searchEngineManager, iconPackHandler = handlers.iconPackHandler, secondarySearchOrchestrator = handlers.secondarySearchOrchestrator, resultsStateProvider = { _resultsState.value }, updateUiState = this::updateUiState, updateConfigState = this::updateConfigState, updateFeatureState = this::updateFeatureState, updateResultsState = this::updateResultsState, refreshAppSuggestions = { refreshAppSuggestions() }, refreshRecentItems = this::refreshRecentItems, stateAccess = preferencesStateAccess) }
    private val startupLifecycleStateAccess =
        SearchViewModelStartupLifecycleStateAccess(
            startupState = startupState,
            directDialEnabledProvider = { legacyPreferenceState.directDialEnabled },
            setDirectDialEnabled = { legacyPreferenceState.directDialEnabled = it },
        )
    private val startupLifecycleDelegate by lazy { SearchStartupLifecycleDelegate(scope = viewModelScope, applicationProvider = { getApplication() }, repository = repository, userPreferences = userPreferences, handlersProvider = { handlers }, resultsStateProvider = { _resultsState.value }, permissionStateProvider = { _permissionState.value }, configStateProvider = { _configState.value }, stateAccess = startupLifecycleStateAccess, getStartupConfig = { startupConfig }, setStartupConfig = { startupConfig = it }, setPrefCache = { prefCache = it }, readStartupPreferencesSnapshot = this::startupPreferencesSnapshot, readLoadedPreferencesSnapshot = this::loadedPreferencesSnapshot, updatePermissionState = this::updatePermissionState, updateFeatureState = this::updateFeatureState, updateResultsState = this::updateResultsState, updateUiState = this::updateUiState, updateConfigState = this::updateConfigState, applyVisibilityStates = this::applyVisibilityStates, hasContactPermission = this::hasContactPermission, hasFilePermission = this::hasFilePermission, hasCalendarPermission = this::hasCalendarPermission, clearQuery = this::clearQuery, refreshApps = { refreshApps() }, refreshAppSuggestions = { refreshAppSuggestions() }, refreshSettingsState = { refreshSettingsState() }, refreshAppShortcutsState = { refreshAppShortcutsState() }, refreshDerivedState = this::refreshDerivedState, saveStartupSurfaceSnapshotAsync = this::saveStartupSurfaceSnapshotAsync, applyPreferenceCacheToLegacyVars = this::applyPreferenceCacheToLegacyVars, applyLauncherIconSelection = this::applyLauncherIconSelection, refreshRecentItems = this::refreshRecentItems, getGridItemCount = this::getGridItemCount, selectSuggestedApps = this::extractSuggestedApps, shouldShowSearchBarWelcome = this::shouldShowSearchBarWelcome, loadApps = this::loadApps, loadSettingsShortcuts = this::loadSettingsShortcuts, loadAppSettings = { handlers.appSettingsSearchHandler.loadSettings() }, loadAppShortcuts = this::loadAppShortcuts, startupDispatcher = startupDispatcher, loadPinnedAndExcludedCalendarEvents = this::loadPinnedAndExcludedCalendarEvents, setDirectDialEnabled = this::setDirectDialEnabled) }
    private val derivedStateDelegate: SearchDerivedStateDelegate by lazy { SearchDerivedStateDelegate(scope = viewModelScope, appContext = appContext, applicationProvider = { getApplication() }, startupSurfaceStore = startupSurfaceStore, userPreferences = userPreferences, handlersProvider = { handlers }, appSuggestionSelector = appSuggestionSelector, instantStartupSurfaceEnabled = instantStartupSurfaceEnabled, cachedAllSearchableAppsProvider = { cachedAllSearchableApps }, setCachedAllSearchableApps = { cachedAllSearchableApps = it }, resultsStateProvider = { _resultsState.value }, permissionStateProvider = { _permissionState.value }, configStateProvider = { _configState.value }, updateResultsState = this::updateResultsState, updatePermissionState = this::updatePermissionState, updateConfigState = this::updateConfigState) }
    private val specialFlowsDelegate by lazy { SearchViewModelSpecialFlowsDelegate(scope = viewModelScope, userPreferences = userPreferences, directSearchStateFlow = handlers.directSearchHandler.directSearchState, clearDirectSearchState = { handlers.directSearchHandler.clearDirectSearchState() }, cancelInactiveTools = toolCoordinator::cancelInactive, shouldRecordPendingDirectSearchQueryInHistory = { shouldRecordPendingDirectSearchQueryInHistory }, setShouldRecordPendingDirectSearchQueryInHistory = { shouldRecordPendingDirectSearchQueryInHistory = it }, updateResultsState = this::updateResultsState, updateConfigState = this::updateConfigState, updateFeatureState = this::updateFeatureState, resultsStateProvider = { _resultsState.value }) }
    override val preferencesApiDelegate by lazy { SearchViewModelPreferencesApiDelegate(preferencesDelegate = preferencesDelegate, webSuggestionHandler = handlers.webSuggestionHandler, iconPackHandler = handlers.iconPackHandler, configStateProvider = { _configState.value }) }
    override val navigationApiDelegate by lazy { SearchViewModelNavigationApiDelegate(applicationProvider = { getApplication() }, navigationHandler = { handlers.navigationHandler }, userPreferences = userPreferences, permissionStateProvider = { _permissionState.value }, resultsStateProvider = { _resultsState.value }, onQueryChange = this::onQueryChange, updateResultsState = this::updateResultsState, onNavigationTriggered = this::onNavigationTriggered, showToastText = this::showToast) }
    override val managementApiDelegate by lazy { SearchViewModelManagementApiDelegate(scope = viewModelScope, userPreferences = userPreferences, resultsStateProvider = { _resultsState.value }, permissionStateProvider = { _permissionState.value }, historyDelegate = historyDelegate, staticDataDelegate = staticDataDelegate, appManager = { handlers.appManager }, contactManager = { handlers.contactManager }, fileManager = { handlers.fileManager }, settingsManager = { handlers.settingsManager }, calendarManager = { handlers.calendarManager }, appShortcutManager = { handlers.appShortcutManager }, notesRepository = { notesRepository }, appSearchManager = { handlers.appSearchManager }, updateUiState = this::updateUiState, updateFeatureState = this::updateFeatureState, legacyPreferenceState = legacyPreferenceState, lockedAliasSearchSectionProvider = { lockedAliasSearchSection }, refreshRecentItems = this::refreshRecentItems) }
    override val contactActionsApiDelegate by lazy { SearchViewModelContactActionsApiDelegate(userPreferences = userPreferences, resultsStateProvider = { _resultsState.value }, contactActionsDelegate = contactActionsDelegate, contactActionHandler = { handlers.contactActionHandler }, historyDelegate = historyDelegate, legacyPreferenceState = legacyPreferenceState, lockedAliasSearchSectionProvider = { lockedAliasSearchSection }, updateUiState = this::updateUiState) }
    override val searchEngineApiDelegate by lazy { SearchViewModelSearchEngineApiDelegate(scope = viewModelScope, userPreferences = userPreferences, aliasHandler = { handlers.aliasHandler }, sectionManager = { handlers.sectionManager }, messagingHandler = { handlers.messagingHandler }, searchEngineManager = { handlers.searchEngineManager }, directSearchHandler = { handlers.directSearchHandler }, releaseNotesHandler = { handlers.releaseNotesHandler }, permissionStateProvider = { _permissionState.value }, configStateProvider = { _configState.value }, updatePermissionState = this::updatePermissionState, updateConfigState = this::updateConfigState) }
    private var prefCache = SearchPreferenceCache(clearQueryOnLaunch = initialConfigState.clearQueryOnLaunch)
    private var enabledFileTypes by legacyPreferenceState::enabledFileTypes
    @set:JvmName("setShowFoldersLegacy")
    private var showFolders by legacyPreferenceState::showFolders
    @set:JvmName("setShowSystemFilesLegacy")
    private var showSystemFiles by legacyPreferenceState::showSystemFiles
    @set:JvmName("setFolderWhitelistPatternsLegacy")
    private var folderWhitelistPatterns by legacyPreferenceState::folderWhitelistPatterns
    @set:JvmName("setFolderBlacklistPatternsLegacy")
    private var folderBlacklistPatterns by legacyPreferenceState::folderBlacklistPatterns
    private var excludedFileExtensions by legacyPreferenceState::excludedFileExtensions
    @set:JvmName("setOneHandedModeLegacy")
    private var oneHandedMode by legacyPreferenceState::oneHandedMode
    @set:JvmName("setBottomSearchBarEnabledLegacy")
    private var bottomSearchBarEnabled by legacyPreferenceState::bottomSearchBarEnabled
    @set:JvmName("setTopResultIndicatorEnabledLegacy")
    private var topResultIndicatorEnabled by legacyPreferenceState::topResultIndicatorEnabled
    @set:JvmName("setWallpaperAccentEnabledLegacy")
    private var wallpaperAccentEnabled by legacyPreferenceState::wallpaperAccentEnabled
    private var openKeyboardOnLaunch by legacyPreferenceState::openKeyboardOnLaunch
    @set:JvmName("setOverlayModeEnabledLegacy")
    private var overlayModeEnabled by legacyPreferenceState::overlayModeEnabled
    private var autoCloseOverlay by legacyPreferenceState::autoCloseOverlay
    private var directDialEnabled by legacyPreferenceState::directDialEnabled
    @set:JvmName("setAssistantLaunchVoiceModeEnabledLegacy")
    private var assistantLaunchVoiceModeEnabled by legacyPreferenceState::assistantLaunchVoiceModeEnabled
    private var hasSeenDirectDialChoice by legacyPreferenceState::hasSeenDirectDialChoice
    @set:JvmName("setAppSuggestionsEnabledLegacy")
    private var appSuggestionsEnabled by legacyPreferenceState::appSuggestionsEnabled
    @set:JvmName("setShowAppLabelsLegacy")
    private var showAppLabels by legacyPreferenceState::showAppLabels
    @set:JvmName("setPhoneAppGridColumnsLegacy")
    private var phoneAppGridColumns by legacyPreferenceState::phoneAppGridColumns
    @set:JvmName("setAppIconShapeLegacy")
    private var appIconShape by legacyPreferenceState::appIconShape
    @set:JvmName("setLauncherAppIconLegacy")
    private var launcherAppIcon by legacyPreferenceState::launcherAppIcon
    @set:JvmName("setThemedIconsEnabledLegacy")
    private var themedIconsEnabled by legacyPreferenceState::themedIconsEnabled
    @set:JvmName("setDeviceThemeEnabledLegacy")
    private var deviceThemeEnabled by legacyPreferenceState::deviceThemeEnabled
    @set:JvmName("setMaskUnsupportedIconPackIconsLegacy")
    private var maskUnsupportedIconPackIcons by legacyPreferenceState::maskUnsupportedIconPackIcons
    @set:JvmName("setWallpaperBackgroundAlphaLegacy")
    private var wallpaperBackgroundAlpha by legacyPreferenceState::wallpaperBackgroundAlpha
    @set:JvmName("setWallpaperBlurRadiusLegacy")
    private var wallpaperBlurRadius by legacyPreferenceState::wallpaperBlurRadius
    @set:JvmName("setAppThemeLegacy")
    private var appTheme by legacyPreferenceState::appTheme
    @set:JvmName("setOverlayThemeIntensityLegacy")
    private var overlayThemeIntensity by legacyPreferenceState::overlayThemeIntensity
    @set:JvmName("setFontScaleMultiplierLegacy")
    private var fontScaleMultiplier by legacyPreferenceState::fontScaleMultiplier
    @set:JvmName("setBackgroundSourceLegacy")
    private var backgroundSource by legacyPreferenceState::backgroundSource
    @set:JvmName("setCustomImageUriLegacy")
    private var customImageUri by legacyPreferenceState::customImageUri
    private var lockedShortcutTarget by legacyPreferenceState::lockedShortcutTarget
    private var lockedAliasSearchSection by legacyPreferenceState::lockedAliasSearchSection
    private var lockedToolMode by legacyPreferenceState::lockedToolMode
    private var lockedCurrencyConverterAlias by legacyPreferenceState::lockedCurrencyConverterAlias
    private var lockedWordClockAlias by legacyPreferenceState::lockedWordClockAlias
    private var lockedDictionaryAlias by legacyPreferenceState::lockedDictionaryAlias
    private var clearQueryOnLaunch by legacyPreferenceState::clearQueryOnLaunch
    @set:JvmName("setAmazonDomainLegacy")
    private var amazonDomain by legacyPreferenceState::amazonDomain
    private var pendingNavigationClear: Boolean
        get() = startupState.pendingNavigationClear
        set(value) {
            startupState.pendingNavigationClear = value
        }
    private var isStartupComplete: Boolean
        get() = startupState.isStartupComplete
        set(value) {
            startupState.isStartupComplete = value
        }
    private fun applyPreferenceCacheToLegacyVars() {
        legacyPreferenceState.applyPreferenceCacheToLegacyVars(prefCache)
    }
    private fun startupPreferencesSnapshot() = SearchStartupPreferencesSnapshot(oneHandedMode = oneHandedMode, bottomSearchBarEnabled = bottomSearchBarEnabled, topResultIndicatorEnabled = topResultIndicatorEnabled, wallpaperAccentEnabled = wallpaperAccentEnabled, openKeyboardOnLaunch = openKeyboardOnLaunch, clearQueryOnLaunch = clearQueryOnLaunch, autoCloseOverlay = autoCloseOverlay, backgroundSource = backgroundSource, wallpaperBackgroundAlpha = wallpaperBackgroundAlpha, wallpaperBlurRadius = wallpaperBlurRadius, appTheme = appTheme, overlayThemeIntensity = overlayThemeIntensity, appIconShape = appIconShape, launcherAppIcon = launcherAppIcon, themedIconsEnabled = themedIconsEnabled, deviceThemeEnabled = deviceThemeEnabled, maskUnsupportedIconPackIcons = maskUnsupportedIconPackIcons, customImageUri = customImageUri)
    private fun loadedPreferencesSnapshot() = SearchLoadedPreferencesSnapshot(enabledFileTypes = enabledFileTypes, oneHandedMode = oneHandedMode, bottomSearchBarEnabled = bottomSearchBarEnabled, topResultIndicatorEnabled = topResultIndicatorEnabled, openKeyboardOnLaunch = openKeyboardOnLaunch, clearQueryOnLaunch = clearQueryOnLaunch, autoCloseOverlay = autoCloseOverlay, overlayModeEnabled = overlayModeEnabled, appSuggestionsEnabled = appSuggestionsEnabled, showAppLabels = showAppLabels, phoneAppGridColumns = phoneAppGridColumns, appIconShape = appIconShape, launcherAppIcon = launcherAppIcon, themedIconsEnabled = themedIconsEnabled, deviceThemeEnabled = deviceThemeEnabled, maskUnsupportedIconPackIcons = maskUnsupportedIconPackIcons, backgroundSource = backgroundSource, wallpaperBackgroundAlpha = wallpaperBackgroundAlpha, wallpaperBlurRadius = wallpaperBlurRadius, appTheme = appTheme, overlayThemeIntensity = overlayThemeIntensity, fontScaleMultiplier = fontScaleMultiplier, customImageUri = customImageUri, showFolders = showFolders, showSystemFiles = showSystemFiles, folderWhitelistPatterns = folderWhitelistPatterns, folderBlacklistPatterns = folderBlacklistPatterns, excludedFileExtensions = excludedFileExtensions, amazonDomain = amazonDomain, directDialEnabled = directDialEnabled, assistantLaunchVoiceModeEnabled = assistantLaunchVoiceModeEnabled)
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
    @set:JvmName("setWallpaperAvailableLegacy")
    private var wallpaperAvailable: Boolean
        get() = startupState.wallpaperAvailable
        set(value) {
            startupState.wallpaperAvailable = value
        }
    private var shouldRecordPendingDirectSearchQueryInHistory: Boolean
        get() = runtimeState.shouldRecordPendingDirectSearchQueryInHistory
        set(value) {
            runtimeState.shouldRecordPendingDirectSearchQueryInHistory = value
        }
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
            getMessagingApp = { handlers.messagingHandler.messagingApp },
            getDirectDialEnabled = { directDialEnabled },
            getHasSeenDirectDialChoice = { hasSeenDirectDialChoice },
            getCurrentState = { uiState.value },
            clearQuery = this::onNavigationTriggered,
            externalNavigation = { _externalNavigationEvent.tryEmit(Unit) },
            onRequestDirectSearch = { query, addToSearchHistory ->
                shouldRecordPendingDirectSearchQueryInHistory = addToSearchHistory
                handlers.directSearchHandler.requestDirectSearch(query)
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
        specialFlowsDelegate.clearInformationCardsExcept(activeCard)
    }
    private fun setupDirectSearchStateListener() {
        specialFlowsDelegate.setupDirectSearchStateListener()
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
    private fun shouldShowSearchBarWelcome(): Boolean =
        specialFlowsDelegate.shouldShowSearchBarWelcome()
    fun requestSearchBarWelcomeAnimationFromOnboarding() {
        specialFlowsDelegate.requestSearchBarWelcomeAnimationFromOnboarding()
    }
    fun onSearchBarWelcomeAnimationCompleted() {
        specialFlowsDelegate.onSearchBarWelcomeAnimationCompleted()
    }
    private fun refreshRecentItems() {
        historyDelegate.refreshRecentItems()
    }
    private fun refreshAliasRecentItems(section: SearchSection?) {
        historyDelegate.refreshAliasRecentItems(section)
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
    private fun refreshSettingsState(updateResults: Boolean = true) {
        staticDataDelegate.refreshSettingsState(updateResults)
    }
    private fun refreshAppShortcutsState(updateResults: Boolean = true) {
        staticDataDelegate.refreshAppShortcutsState(updateResults)
    }
    private fun loadPinnedAndExcludedCalendarEvents() {
        staticDataDelegate.loadPinnedAndExcludedCalendarEvents()
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
    private fun applyLauncherIconSelection(selection: LauncherAppIcon = launcherAppIcon) {
        launcherIconManager.applySelection(
                selection = selection,
        )
    }
    // Contact Actions
    private fun isDirectSearchActive() =
        _resultsState.value.DirectSearchState.status != DirectSearchStatus.Idle
    fun resetUsagePermissionBannerSessionDismissed() {
        specialFlowsDelegate.resetUsagePermissionBannerSessionDismissed()
    }
    fun incrementUsagePermissionBannerDismissCount() {
        specialFlowsDelegate.incrementUsagePermissionBannerDismissCount()
    }
    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) {
        specialFlowsDelegate.setUsagePermissionBannerSessionDismissed(dismissed)
    }
    private fun applyVisibilityStates(state: SearchUiState): SearchUiState =
            visibilityStateResolver.apply(state)
    private fun updateVisibilityStates() = updateUiState { currentState -> applyVisibilityStates(currentState) }
    companion object {
        @Volatile private var inMemoryRetainedQuery: String = ""
        // Debounce for the background app-search job (ms).  Kept intentionally
        // shorter than the 150 ms secondary-search debounce because app results
        // are cheap relative to contact/file queries, but still eliminates the
        // redundant mid-word searches that occur during rapid typing.
        private const val APP_SEARCH_DEBOUNCE_MS = 60L
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
    override fun onCleared() {
        queryCoordinator.cancel()
        super.onCleared()
    }
}
