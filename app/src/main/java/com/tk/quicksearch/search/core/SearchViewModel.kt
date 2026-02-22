package com.tk.quicksearch.search.core

import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.quicksearch.R
import com.tk.quicksearch.app.ReleaseNotesHandler
import com.tk.quicksearch.navigation.NavigationHandler
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler
import com.tk.quicksearch.search.appShortcuts.AppShortcutManagementHandler
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.apps.AppManagementService
import com.tk.quicksearch.search.apps.AppSearchManager
import com.tk.quicksearch.search.apps.IconPackService
import com.tk.quicksearch.search.apps.invalidateAppIconCache
import com.tk.quicksearch.search.apps.prefetchAppIcons
import com.tk.quicksearch.search.calculator.CalculatorHandler
import com.tk.quicksearch.search.common.PinningHandler
import com.tk.quicksearch.search.contacts.actions.ContactActionHandler
import com.tk.quicksearch.search.contacts.utils.ContactCallingAppResolver
import com.tk.quicksearch.search.contacts.utils.ContactManagementHandler
import com.tk.quicksearch.search.contacts.utils.ContactMessagingAppResolver
import com.tk.quicksearch.search.contacts.utils.MessagingHandler
import com.tk.quicksearch.search.contacts.utils.TelegramContactUtils
import com.tk.quicksearch.search.data.AppShortcutRepository
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.isUserCreatedShortcut
import com.tk.quicksearch.search.data.launchStaticShortcut
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.data.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsManagementHandler
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsRepository
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.directSearch.DirectSearchHandler
import com.tk.quicksearch.search.directSearch.GeminiModelCatalog
import com.tk.quicksearch.search.files.FileManagementHandler
import com.tk.quicksearch.search.files.FileSearchHandler
import com.tk.quicksearch.search.fuzzy.FuzzySearchConfigurationManager
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.overlay.OverlayModeController
import com.tk.quicksearch.search.recentSearches.RecentSearchEntry
import com.tk.quicksearch.search.recentSearches.RecentSearchItem
import com.tk.quicksearch.search.searchEngines.SearchEngineManager
import com.tk.quicksearch.search.searchEngines.SecondarySearchOrchestrator
import com.tk.quicksearch.search.searchEngines.ShortcutHandler
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import com.tk.quicksearch.search.webSuggestions.WebSuggestionHandler
import com.tk.quicksearch.util.PackageConstants
import com.tk.quicksearch.util.getAppGridColumns
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(
        application: Application,
) : AndroidViewModel(application) {
    private val repository by lazy { AppsRepository(application.applicationContext) }
    private val appShortcutRepository by lazy {
        AppShortcutRepository(application.applicationContext)
    }
    private val contactRepository by lazy { ContactRepository(application.applicationContext) }
    private val fileRepository by lazy { FileSearchRepository(application.applicationContext) }
    private val settingsShortcutRepository by lazy {
        DeviceSettingsRepository(application.applicationContext)
    }
    private val userPreferences by lazy { UserAppPreferences(application.applicationContext) }
    private val contactPreferences by lazy {
        com.tk.quicksearch.search.data.preferences.ContactPreferences(
                application.applicationContext,
        )
    }

    private val permissionManager by lazy {
        PermissionManager(contactRepository, fileRepository, userPreferences)
    }
    private val searchOperations by lazy { SearchOperations(contactRepository) }

    private val initialOverlayModeEnabled: Boolean =
            runCatching { userPreferences.isOverlayModeEnabled() }.getOrDefault(false)
    private val initialWallpaperBackgroundAlpha: Float =
            runCatching { userPreferences.getWallpaperBackgroundAlpha() }
                    .getOrDefault(UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA)
    private val initialWallpaperBlurRadius: Float =
            runCatching { userPreferences.getWallpaperBlurRadius() }
                    .getOrDefault(UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS)
    private val initialOverlayGradientTheme: OverlayGradientTheme =
            runCatching { userPreferences.getOverlayGradientTheme() }
                    .getOrDefault(OverlayGradientTheme.MONOCHROME)
    private val initialOverlayThemeIntensity: Float =
            sanitizeOverlayThemeIntensity(
                    runCatching { userPreferences.getOverlayThemeIntensity() }
                            .getOrDefault(UiPreferences.DEFAULT_OVERLAY_THEME_INTENSITY)
            )
    private val initialBackgroundSource: BackgroundSource =
            runCatching { userPreferences.getBackgroundSource() }
                    .getOrDefault(BackgroundSource.THEME)
    private val initialOverlayCustomImageUri: String? =
            runCatching { userPreferences.getCustomImageUri() }.getOrNull()

    private val _uiState =
            MutableStateFlow(
                    SearchUiState(
                            overlayModeEnabled = initialOverlayModeEnabled,
                            showWallpaperBackground = false,
                            wallpaperBackgroundAlpha = initialWallpaperBackgroundAlpha,
                            wallpaperBlurRadius = initialWallpaperBlurRadius,
                            overlayGradientTheme = initialOverlayGradientTheme,
                            overlayThemeIntensity = initialOverlayThemeIntensity,
                            backgroundSource = initialBackgroundSource,
                            customImageUri = initialOverlayCustomImageUri,
                    )
            )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Cache searchable apps to avoid re-computing on every query change
    @Volatile private var cachedAllSearchableApps: List<AppInfo> = emptyList()

    // Consolidated startup configuration loaded in single batch operation
    @Volatile private var startupConfig: UserAppPreferences.StartupConfig? = null

    // UI feedback is now handled by UiFeedbackService

    private fun updateUiState(updater: (SearchUiState) -> SearchUiState) {
        _uiState.update { state ->
            val updated = updater(state)
            val withHint = applyContactActionHint(state, updated)

            // Compute visibility states inline to avoid double state emissions
            // Only update visibility states after startup completes to avoid lazy handler init
            // during first paint
            if (isStartupComplete) {
                applyVisibilityStates(withHint)
            } else {
                withHint
            }
        }
    }

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

    // Management handlers - lazy initialize non-critical ones
    val appManager by lazy {
        AppManagementService(userPreferences, viewModelScope, this::refreshDerivedState)
    }
    val contactManager by lazy {
        ContactManagementHandler(
                userPreferences,
                viewModelScope,
                this::refreshDerivedState,
                this::updateUiState,
        )
    }
    val fileManager by lazy {
        FileManagementHandler(
                userPreferences,
                viewModelScope,
                this::refreshDerivedState,
                this::updateUiState,
        )
    }
    val settingsManager by lazy {
        DeviceSettingsManagementHandler(
                userPreferences,
                viewModelScope,
                this::refreshDerivedState,
                this::updateUiState,
        )
    }
    val appShortcutManager by lazy {
        AppShortcutManagementHandler(
                userPreferences,
                viewModelScope,
                this::refreshAppShortcutsState,
                this::updateUiState,
        )
    }
    val searchEngineManager by lazy {
        SearchEngineManager(
                application.applicationContext,
                userPreferences,
                viewModelScope,
                this::updateUiState,
        )
    }
    val sectionManager by lazy {
        SectionManager(userPreferences, permissionManager, viewModelScope, this::updateUiState)
    }
    val iconPackHandler by lazy {
        IconPackService(application, userPreferences, viewModelScope, this::updateUiState)
    }

    // New Handlers - lazy initialize non-critical ones
    val messagingHandler by lazy {
        MessagingHandler(application, userPreferences, this::updateUiState)
    }
    val releaseNotesHandler by lazy {
        ReleaseNotesHandler(application, userPreferences, this::updateUiState)
    }

    // Feature handlers (extracted)
    private val pinningHandler by lazy {
        PinningHandler(
                scope = viewModelScope,
                permissionManager = permissionManager,
                contactRepository = contactRepository,
                fileRepository = fileRepository,
                userPreferences = userPreferences,
                uiStateUpdater = this::updateUiState,
        )
    }

    val webSuggestionHandler by lazy {
        WebSuggestionHandler(
                scope = viewModelScope,
                userPreferences = userPreferences,
                uiStateUpdater = this::updateUiState,
        )
    }

    val calculatorHandler by lazy {
        CalculatorHandler(
                scope = viewModelScope,
                userPreferences = userPreferences,
                uiStateUpdater = this::updateUiState,
        )
    }

    val fuzzySearchConfigManager by lazy {
        FuzzySearchConfigurationManager(userPreferences.uiPreferences)
    }

    val appSearchManager by lazy {
        AppSearchManager(
                context = application.applicationContext,
                repository = repository,
                userPreferences = userPreferences,
                scope = viewModelScope,
                onAppsUpdated = { this.refreshDerivedState() },
                onLoadingStateChanged = { isLoading, error ->
                    _uiState.update { it.copy(isLoading = isLoading, errorMessage = error) }
                },
                showToastCallback = this::showToast,
                initialFuzzyConfig = fuzzySearchConfigManager.getAppFuzzyConfig(),
        )
    }

    val settingsSearchHandler by lazy {
        DeviceSettingsSearchHandler(
                context = application.applicationContext,
                repository = settingsShortcutRepository,
                userPreferences = userPreferences,
                scope = viewModelScope,
                showToastCallback = this::showToast,
        )
    }

    val appShortcutSearchHandler by lazy {
        AppShortcutSearchHandler(
                repository = appShortcutRepository,
                userPreferences = userPreferences,
        )
    }

    val fileSearchHandler by lazy {
        FileSearchHandler(fileRepository = fileRepository, userPreferences = userPreferences)
    }

    val directSearchHandler by lazy {
        DirectSearchHandler(
                context = application.applicationContext,
                userPreferences = userPreferences,
                scope = viewModelScope,
                showToastCallback = this::showToast,
        )
    }

    val shortcutHandler by lazy {
        ShortcutHandler(
                userPreferences = userPreferences,
                scope = viewModelScope,
                uiStateUpdater = this::updateUiState,
                directSearchHandler = directSearchHandler,
                searchTargetsProvider = { searchEngineManager.searchTargetsOrder },
        )
    }

    // NavigationHandler is now initialized in initializeServices()
    lateinit var navigationHandler: NavigationHandler

    private val unifiedSearchHandler by lazy {
        UnifiedSearchHandler(
                contactRepository = contactRepository,
                fileRepository = fileRepository,
                userPreferences = userPreferences,
                settingsSearchHandler = settingsSearchHandler,
                appShortcutSearchHandler = appShortcutSearchHandler,
                fileSearchHandler = fileSearchHandler,
                searchOperations = searchOperations,
        )
    }

    private val secondarySearchOrchestrator by lazy {
        SecondarySearchOrchestrator(
                scope = viewModelScope,
                unifiedSearchHandler = unifiedSearchHandler,
                webSuggestionHandler = webSuggestionHandler,
                sectionManager = sectionManager,
                uiStateUpdater = this::updateUiState,
                currentStateProvider = { _uiState.value },
        )
    }

    private var enabledFileTypes: Set<FileType> = emptySet()
    private var showFolders: Boolean = false
    private var showSystemFiles: Boolean = false
    private var showHiddenFiles: Boolean = false
    private var folderWhitelistPatterns: Set<String> = emptySet()
    private var folderBlacklistPatterns: Set<String> = emptySet()
    private var excludedFileExtensions: Set<String> = emptySet()
    private var oneHandedMode: Boolean = false
    private var overlayModeEnabled: Boolean = initialOverlayModeEnabled
    private var directDialEnabled: Boolean = false
    private var hasSeenDirectDialChoice: Boolean = false
    private var appSuggestionsEnabled: Boolean = true
    private var showAppLabels: Boolean = true
    private var appIconSizeOption: AppIconSizeOption = AppIconSizeOption.MEDIUM
    private var wallpaperBackgroundAlpha: Float = initialWallpaperBackgroundAlpha
    private var wallpaperBlurRadius: Float = initialWallpaperBlurRadius
    private var overlayGradientTheme: OverlayGradientTheme = initialOverlayGradientTheme
    private var overlayThemeIntensity: Float = initialOverlayThemeIntensity
    private var backgroundSource: BackgroundSource = initialBackgroundSource
    private var customImageUri: String? = initialOverlayCustomImageUri
    private var lockedShortcutTarget: SearchTarget? = null
    private var amazonDomain: String? = null
    private var searchJob: Job? = null
    private var queryVersion: Long = 0L
    private var pendingNavigationClear: Boolean = false
    private var isStartupComplete: Boolean = false

    private fun onNavigationTriggered() {
        pendingNavigationClear = true
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(getApplication(), messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hasContactPermission(): Boolean = contactRepository.hasPermission()

    private fun hasFilePermission(): Boolean = fileRepository.hasPermission()

    private fun hasCallPermission(): Boolean =
            PermissionRequestHandler.checkCallPermission(getApplication())

    private fun hasWallpaperPermission(): Boolean =
            PermissionRequestHandler.checkWallpaperPermission(getApplication())

    private var wallpaperAvailable: Boolean = false

    fun setWallpaperAvailable(available: Boolean) {
        if (wallpaperAvailable != available) {
            wallpaperAvailable = available
            updateUiState { it.copy(wallpaperAvailable = available) }
        }
    }

    fun handleOnStop() {
        clearQuery()
        if (pendingNavigationClear) {
            pendingNavigationClear = false
        }
    }

    // ContactActionHandler is initialized in initializeServices()
    lateinit var contactActionHandler: ContactActionHandler

    private fun initializeServices() {
        val app = getApplication<Application>()

        // Initialize NavigationHandler
        navigationHandler =
                NavigationHandler(
                        application = app,
                        userPreferences = userPreferences,
                        settingsSearchHandler = settingsSearchHandler,
                        onRequestDirectSearch = { query ->
                            directSearchHandler.requestDirectSearch(query)
                        },
                        onClearQuery = this::onNavigationTriggered,
                        showToastCallback = this::showToast,
                )

        // Initialize ContactActionHandler with uiFeedbackService
        contactActionHandler =
                ContactActionHandler(
                        context = app,
                        userPreferences = userPreferences,
                        getCallingApp = { _uiState.value.callingApp },
                        getMessagingApp = { messagingHandler.messagingApp },
                        getDirectDialEnabled = { directDialEnabled },
                        getHasSeenDirectDialChoice = { hasSeenDirectDialChoice },
                        getCurrentState = { _uiState.value },
                        uiStateUpdater = { update -> _uiState.update(update) },
                        clearQuery = this::onNavigationTriggered,
                        showToastCallback = this::showToast,
                )
    }

    init {
        // Initialize services after all handlers are available
        initializeServices()

        setupDirectSearchStateListener()

        // INSTANT RENDER: Create minimal initial state synchronously
        // UI renders immediately with empty/placeholder state based on definitions in SearchUiState
        // No update needed - use default SearchUiState values

        // POST-FIRST-FRAME: Load cache and preferences after UI renders
        viewModelScope.launch(Dispatchers.Main.immediate) {
            // Immediately load cache without yield - cache read is fast and we want to show it ASAP
            withContext(Dispatchers.IO) { loadCacheAndMinimalPrefs() }

            // Now yield to let UI render with cached data
            kotlinx.coroutines.yield()

            // Then compute derived state and load remaining preferences
            withContext(Dispatchers.IO) { loadRemainingStartupPreferences() }

            // Then start all background operations (non-blocking)
            launchDeferredInitialization()
        }
    }

    private fun setupDirectSearchStateListener() {
        viewModelScope.launch {
            directSearchHandler.directSearchState.collect { dsState ->
                _uiState.update { it.copy(DirectSearchState = dsState) }
            }
        }
    }

    /** Phase 1: Load critical data (cache and essential prefs) */

    /** Phase 1: Load ONLY what's needed for the first paint (cache + minimal config) */
    private suspend fun loadCacheAndMinimalPrefs() {
        // Load consolidated startup config in single batch operation
        val startupConfig = userPreferences.loadStartupConfig()

        // Extract critical data for immediate use
        oneHandedMode = startupConfig.oneHandedMode

        // Load cached data - this is the critical path for content
        // This is just a fast JSON parse
        val cachedAppsList = runCatching { repository.loadCachedApps() }.getOrNull()
        val hasUsagePermission = repository.hasUsageAccess()
        val disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds()

        // Apply immediately - DO NOT BLOCK on icon loading
        withContext(Dispatchers.Main) {
            _uiState.update {
                it.copy(
                        oneHandedMode = oneHandedMode,
                        disabledAppShortcutIds = disabledAppShortcutIds,
                        // We don't have full prefs yet, so keep initializing flag true
                        // but show the apps we found in cache
                        isInitializing = true,
                )
            }

            if (cachedAppsList != null && cachedAppsList.isNotEmpty()) {
                initializeWithCacheMinimal(cachedAppsList, hasUsagePermission)
            }
        }

        // Store the full startup config for Phase 2
        this.startupConfig = startupConfig

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

            // Now we can compute the full state including pinned/hidden apps
            val lastUpdated =
                    startupConfig?.cachedAppsLastUpdate ?: repository.cacheLastUpdatedMillis()
            refreshDerivedState(lastUpdated = lastUpdated, isLoading = false)

            // Fully initialized now
            _uiState.update { it.copy(isInitializing = false) }
        }
    }

    private fun applyStartupPreferences(prefs: UserAppPreferences.StartupPreferences) {
        enabledFileTypes = prefs.enabledFileTypes
        showFolders = prefs.showFolders
        showSystemFiles = prefs.showSystemFiles
        showHiddenFiles = prefs.showHiddenFiles
        folderWhitelistPatterns = prefs.folderWhitelistPatterns
        folderBlacklistPatterns = prefs.folderBlacklistPatterns
        excludedFileExtensions = prefs.excludedFileExtensions
        oneHandedMode = prefs.oneHandedMode
        overlayModeEnabled = prefs.overlayModeEnabled
        directDialEnabled = prefs.directDialEnabled
        hasSeenDirectDialChoice = prefs.hasSeenDirectDialChoice
        appSuggestionsEnabled = prefs.appSuggestionsEnabled
        showAppLabels = prefs.showAppLabels
        appIconSizeOption = prefs.appIconSizeOption
        wallpaperBackgroundAlpha = prefs.wallpaperBackgroundAlpha
        wallpaperBlurRadius = prefs.wallpaperBlurRadius
        overlayGradientTheme = prefs.overlayGradientTheme
        overlayThemeIntensity = sanitizeOverlayThemeIntensity(prefs.overlayThemeIntensity)
        backgroundSource = prefs.backgroundSource
        customImageUri = prefs.customImageUri
        amazonDomain = prefs.amazonDomain

        _uiState.update {
            it.copy(
                    enabledFileTypes = enabledFileTypes,
                    showFolders = showFolders,
                    showSystemFiles = showSystemFiles,
                    showHiddenFiles = showHiddenFiles,
                    folderWhitelistPatterns = folderWhitelistPatterns,
                    folderBlacklistPatterns = folderBlacklistPatterns,
                    excludedFileExtensions = excludedFileExtensions,
                    oneHandedMode = oneHandedMode,
                    overlayModeEnabled = overlayModeEnabled,
                    directDialEnabled = directDialEnabled,
                    appSuggestionsEnabled = appSuggestionsEnabled,
                    showAppLabels = showAppLabels,
                    appIconSizeOption = appIconSizeOption,
                    disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds(),
                    showWallpaperBackground = backgroundSource != BackgroundSource.THEME,
                    wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = wallpaperBlurRadius,
                    overlayGradientTheme = overlayGradientTheme,
                    overlayThemeIntensity = overlayThemeIntensity,
                    backgroundSource = backgroundSource,
                    customImageUri = customImageUri,
                    amazonDomain = amazonDomain,
                    recentQueriesEnabled = prefs.recentSearchesEnabled,
                    webSuggestionsCount = userPreferences.getWebSuggestionsCount(),
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner(),
                    showOverlayCloseTip = !userPreferences.hasSeenOverlayCloseTip(),
                    hasSeenOverlayAssistantTip = userPreferences.hasSeenOverlayAssistantTip(),
            )
        }

        // Load recent queries on startup if enabled
        refreshRecentItems()
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
        val iconSizeOption =
                startupPrefs?.appIconSizeOption ?: userPreferences.getAppIconSizeOption()

        // Just show the raw list of apps first!
        // Don't filter, don't sort, don't check pinned apps yet
        // This is the fastest possible way to get pixels on screen
        _uiState.update { state ->
            state.copy(
                    cacheLastUpdatedMillis = lastUpdated,
                    // Temporarily show all cached apps until we load hidden/pinned prefs
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
                    // Critical: update these to prevent flashing
                    oneHandedMode = oneHandedMode,
                    appSuggestionsEnabled = suggestionsEnabled,
                    showAppLabels = labelsEnabled,
                    appIconSizeOption = iconSizeOption,
            )
        }
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

            // Update state with messaging info and correct search engine/section config
            // accessing these handlers now is safe as UI is rendered
            val shortcutsState = shortcutHandler.getInitialState()

            // Ensure SearchEngineManager is initialized on IO thread
            searchEngineManager.ensureInitialized()

            _uiState.update { state ->
                state.copy(
                        // Now safely access lazy handlers
                        searchTargetsOrder = searchEngineManager.searchTargetsOrder,
                        disabledSearchTargetIds = searchEngineManager.disabledSearchTargetIds,
                        shortcutsEnabled = shortcutsState.shortcutsEnabled,
                        shortcutCodes = shortcutsState.shortcutCodes,
                        shortcutEnabled = shortcutsState.shortcutEnabled,
                        disabledSections = sectionManager.disabledSections,
                        isSearchEngineCompactMode = searchEngineManager.isSearchEngineCompactMode,
                        showSearchEngineOnboarding =
                                searchEngineManager.isSearchEngineCompactMode &&
                                        !userPreferences.hasSeenSearchEngineOnboarding(),
                        showSearchBarWelcomeAnimation = shouldShowSearchBarWelcome(),
                        appSuggestionsEnabled = userPreferences.areAppSuggestionsEnabled(),
                        showAppLabels = userPreferences.shouldShowAppLabels(),
                        appIconSizeOption = userPreferences.getAppIconSizeOption(),
                        disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds(),
                        webSuggestionsEnabled = webSuggestionHandler.isEnabled,
                        calculatorEnabled = userPreferences.isCalculatorEnabled(),
                        hasGeminiApiKey = !directSearchHandler.getGeminiApiKey().isNullOrBlank(),
                        geminiApiKeyLast4 = directSearchHandler.getGeminiApiKey()?.takeLast(4),
                        personalContext = directSearchHandler.getPersonalContext(),
                        geminiModel = directSearchHandler.getGeminiModel(),
                        geminiGroundingEnabled = directSearchHandler.isGeminiGroundingEnabled(),
                        availableGeminiModels = directSearchHandler.getAvailableGeminiModels(),
                        showPersonalContextHint =
                                !userPreferences.hasSeenPersonalContextHint() &&
                                        directSearchHandler.getPersonalContext().isBlank(),
                        // Messaging info
                        messagingApp = messagingInfo.messagingApp,
                        callingApp = messagingInfo.callingApp,
                        isWhatsAppInstalled = messagingInfo.isWhatsAppInstalled,
                        isTelegramInstalled = messagingInfo.isTelegramInstalled,
                        isSignalInstalled = messagingInfo.isSignalInstalled,
                        isGoogleMeetInstalled = messagingInfo.isGoogleMeetInstalled,
                )
            }

            if (!directSearchHandler.getGeminiApiKey().isNullOrBlank()) {
                launch(Dispatchers.IO) {
                    val models = directSearchHandler.refreshAvailableGeminiModels()
                    _uiState.update { state -> state.copy(availableGeminiModels = models) }
                }
            }

            // 3. Start heavy background loads
            launch(Dispatchers.IO) { loadApps() }

            launch(Dispatchers.IO) { iconPackHandler.refreshIconPacks() }

            launch(Dispatchers.IO) {
                // Load pinned items first (fast) - contacts, files, app shortcuts, and device
                // settings
                pinningHandler.loadPinnedContactsAndFiles()
                pinningHandler.loadExcludedContactsAndFiles()

                // Load pinned app shortcuts from cache (fast)
                val pinnedAppShortcutsState = appShortcutSearchHandler.getPinnedAndExcludedOnly()
                updateUiState { state ->
                    state.copy(
                            allAppShortcuts = appShortcutSearchHandler.getAvailableShortcuts(),
                            disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds(),
                            pinnedAppShortcuts = pinnedAppShortcutsState.pinned,
                            excludedAppShortcuts = pinnedAppShortcutsState.excluded,
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
            launch(Dispatchers.IO) { loadSettingsShortcuts() }

            launch(Dispatchers.IO) { loadAppShortcuts() }

            // 4. Release notes
            releaseNotesHandler.checkForReleaseNotes()

            // 5. Startup complete - now compute visibility and refresh UI
            withContext(Dispatchers.Main) {
                isStartupComplete = true
                updateVisibilityStates()
                refreshDerivedState()
            }
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
        _uiState.update { it.copy(showSearchBarWelcomeAnimation = true) }
    }

    fun onSearchBarWelcomeAnimationCompleted() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setForceSearchBarWelcomeOnNextOpen(false)
        }
        _uiState.update { it.copy(showSearchBarWelcomeAnimation = false) }
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
                CallingApp.SIGNAL ->
                        if (isSignalInstalled) CallingApp.SIGNAL else CallingApp.CALL
                CallingApp.GOOGLE_MEET ->
                        if (isGoogleMeetInstalled) CallingApp.GOOGLE_MEET else CallingApp.CALL
                CallingApp.CALL -> CallingApp.CALL
            }

    fun setCalculatorEnabled(enabled: Boolean) {
        // Delegate to new CalculatorHandler once implemented
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setCalculatorEnabled(enabled)
            _uiState.update { it.copy(calculatorEnabled = enabled) }
        }
    }

    fun hasSeenOverlayCloseTip(): Boolean = userPreferences.hasSeenOverlayCloseTip()

    fun dismissOverlayCloseTip() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setHasSeenOverlayCloseTip(true)
            _uiState.update { it.copy(showOverlayCloseTip = false) }
        }
    }

    fun dismissOverlayAssistantTip() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setHasSeenOverlayAssistantTip(true)
            _uiState.update { it.copy(hasSeenOverlayAssistantTip = true) }
        }
    }

    fun setAppSuggestionsEnabled(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setAppSuggestionsEnabled,
                stateUpdater = {
                    appSuggestionsEnabled = it
                    updateUiState { state -> state.copy(appSuggestionsEnabled = it) }
                    refreshDerivedState()
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
                },
        )
    }

    fun setAppIconSizeOption(option: AppIconSizeOption) {
        viewModelScope.launch(Dispatchers.IO) {
            if (appIconSizeOption == option) return@launch
            userPreferences.setAppIconSizeOption(option)
            appIconSizeOption = option
            updateUiState { state -> state.copy(appIconSizeOption = option) }
        }
    }

    fun setWebSuggestionsEnabled(enabled: Boolean) = webSuggestionHandler.setEnabled(enabled)

    fun setWebSuggestionsCount(count: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setWebSuggestionsCount(count)
            _uiState.update { it.copy(webSuggestionsCount = count) }
        }
    }

    fun setRecentQueriesEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setRecentQueriesEnabled(enabled)
            _uiState.update { it.copy(recentQueriesEnabled = enabled) }

            // Refresh recent queries display if query is empty
            if (_uiState.value.query.isEmpty()) {
                refreshRecentItems()
            }
        }
    }

    private fun refreshRecentItems() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!_uiState.value.recentQueriesEnabled) {
                _uiState.update { it.copy(recentItems = emptyList()) }
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
                    }
                }
            }
            _uiState.update { it.copy(recentItems = items) }
        }
    }

    fun deleteRecentItem(entry: RecentSearchEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.deleteRecentItem(entry)
            refreshRecentItems()
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
        _uiState.update { it.copy(contactActionsVersion = it.contactActionsVersion + 1) }
    }

    fun getSecondaryContactCardAction(contactId: Long) =
            contactPreferences.getSecondaryContactCardAction(contactId)

    fun setSecondaryContactCardAction(
            contactId: Long,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
    ) {
        contactPreferences.setSecondaryContactCardAction(contactId, action)
        _uiState.update { it.copy(contactActionsVersion = it.contactActionsVersion + 1) }
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
            _uiState.update {
                it.copy(
                        contactActionPickerRequest =
                                ContactActionPickerRequest(contact, isPrimary, resolvedAction),
                )
            }
        }
    }

    fun clearContactActionPickerRequest() {
        _uiState.update { it.copy(contactActionPickerRequest = null) }
    }

    private fun getDefaultContactCardAction(
            contact: ContactInfo,
            isPrimary: Boolean,
    ): com.tk.quicksearch.search.contacts.models.ContactCardAction? {
        val phoneNumber = contact.phoneNumbers.firstOrNull() ?: return null
        return if (isPrimary) {
            when (
                ContactCallingAppResolver.resolveCallingAppForContact(
                    contactInfo = contact,
                    defaultApp = _uiState.value.callingApp,
                )
            ) {
                CallingApp.CALL -> com.tk.quicksearch.search.contacts.models.ContactCardAction.Phone(phoneNumber)
                CallingApp.WHATSAPP -> com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppCall(phoneNumber)
                CallingApp.TELEGRAM -> com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramCall(phoneNumber)
                CallingApp.SIGNAL -> com.tk.quicksearch.search.contacts.models.ContactCardAction.SignalCall(phoneNumber)
                CallingApp.GOOGLE_MEET -> com.tk.quicksearch.search.contacts.models.ContactCardAction.GoogleMeet(phoneNumber)
            }
        } else {
            when (
                ContactMessagingAppResolver.resolveMessagingAppForContact(
                    contactInfo = contact,
                    defaultApp = _uiState.value.messagingApp,
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
                                method is ContactMethod.SignalMessage &&
                                        matchesSignalNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.SignalCall -> {
                                method is ContactMethod.SignalCall &&
                                        matchesSignalNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.SignalVideoCall -> {
                                method is ContactMethod.SignalVideoCall &&
                                        matchesSignalNumber(method)
                            }
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.GoogleMeet -> {
                                method is ContactMethod.GoogleMeet && matchesPhoneNumber(method)
                            }
                        }
                    }

            withContext(Dispatchers.Main) {
                if (matchedMethod != null) {
                    contactActionHandler.handleContactMethod(contactInfo, matchedMethod)
                } else {
                    // Fallback to generic action if specific method not found (e.g. dataId changed)
                    // For Phone/SMS this is easy
                    when (action) {
                        is com.tk.quicksearch.search.contacts.models.ContactCardAction.Phone -> {
                            contactActionHandler.handleContactMethod(
                                    contactInfo,
                                    ContactMethod.Phone("Call", action.phoneNumber),
                            )
                        }
                        is com.tk.quicksearch.search.contacts.models.ContactCardAction.Sms -> {
                            contactActionHandler.handleContactMethod(
                                    contactInfo,
                                    ContactMethod.Sms("Message", action.phoneNumber),
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
        viewModelScope.launch(Dispatchers.IO) {
            appShortcutSearchHandler.loadShortcuts()
            withContext(Dispatchers.Main) { refreshAppShortcutsState() }
        }
    }

    private fun refreshSettingsState(updateResults: Boolean = true) {
        val currentResults = _uiState.value.settingResults
        val currentState =
                settingsSearchHandler.getSettingsState(
                        query = _uiState.value.query,
                        isSettingsSectionEnabled =
                                SearchSection.SETTINGS !in sectionManager.disabledSections,
                        currentResults =
                                currentResults, // Pass current results to avoid re-search if not
                        // needed? Actually logic handles it
                        )

        _uiState.update { state ->
            state.copy(
                    pinnedSettings = currentState.pinned,
                    excludedSettings = currentState.excluded,
                    settingResults = currentState.results,
                    allDeviceSettings = settingsSearchHandler.getAvailableSettings(),
            )
        }
    }

    private fun refreshAppShortcutsState(updateResults: Boolean = true) {
        val query = if (updateResults) _uiState.value.query else ""
        val disabledShortcutIds = userPreferences.getDisabledAppShortcutIds()
        val currentState =
                appShortcutSearchHandler.getShortcutsState(
                        query = query,
                        isSectionEnabled =
                                SearchSection.APP_SHORTCUTS !in sectionManager.disabledSections,
                )

        updateUiState { state ->
            state.copy(
                    allAppShortcuts = appShortcutSearchHandler.getAvailableShortcuts(),
                    disabledAppShortcutIds = disabledShortcutIds,
                    pinnedAppShortcuts = currentState.pinned,
                    excludedAppShortcuts = currentState.excluded,
                    appShortcutResults =
                            if (updateResults) currentState.results else state.appShortcutResults,
            )
        }
    }

    fun refreshUsageAccess() {
        _uiState.update { it.copy(hasUsagePermission = repository.hasUsageAccess()) }
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

            val query = _uiState.value.query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.resetNoResultTracking()
                secondarySearchOrchestrator.performSecondarySearches(query)
            } else {
                _uiState.update { it.copy(contactResults = emptyList()) }
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

            val query = _uiState.value.query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.resetNoResultTracking()
                secondarySearchOrchestrator.performSecondarySearches(query)
            } else {
                _uiState.update { it.copy(fileResults = emptyList()) }
            }

            if (showToast) {
                withContext(Dispatchers.Main) {
                    showToast(R.string.files_refreshed_successfully)
                }
            }
        }
    }

    // Removed messaging logic (moved to MessagingHandler)

    // Removed isPackageInstalled (moved to MessagingHandler)

    // Removed Release Notes logic (moved to ReleaseNotesHandler)

    // Removed setMessagingApp (delegate in SearchViewModelExtensions if needed, or direct)

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
        _uiState.update { it.copy(directDialEnabled = enabled) }
    }

    fun onQueryChange(newQuery: String) {
        val previousQuery = _uiState.value.query
        // Prevent redundant updates
        if (newQuery == previousQuery) return

        val trimmedQuery = newQuery.trim()
        val DirectSearchState = _uiState.value.DirectSearchState
        if (DirectSearchState.status != DirectSearchStatus.Idle &&
                        (DirectSearchState.activeQuery == null ||
                                DirectSearchState.activeQuery != trimmedQuery)
        ) {
            directSearchHandler.clearDirectSearchState()
        }

        if (trimmedQuery.isBlank()) {
            appSearchManager.setNoMatchPrefix(null)
            searchJob?.cancel()
            webSuggestionHandler.cancelSuggestions()
            updateUiState {
                it.copy(
                        query = "",
                        searchResults = emptyList(),
                        appShortcutResults = emptyList(),
                        contactResults = emptyList(),
                        fileResults = emptyList(),
                        settingResults = emptyList(),
                        DirectSearchState = DirectSearchState(),
                        calculatorState = CalculatorState(),
                        webSuggestions = emptyList(),
                        detectedShortcutTarget = lockedShortcutTarget,
                        webSuggestionWasSelected = false,
                )
            }
            // Load recent queries when query is empty
            refreshRecentItems()
            // Also reset locked shortcut when query is cleared completely (empty)
            // But we don't null it out here if we are just transitioning?
            // Actually, if query is empty, it means we are cleared.
            // Wait, if we stripped the shortcut, the query might not be empty immediately if there
            // was trailing text.
            // If the query becomes empty manually (user backspaced everything), we should probably
            // clear the lock?
            // User requirement: "clear the detected state only when user clears the query by
            // tapping on x icon or when the app automatically clears the query using clearQuery
            // function."
            // So if user backspaces to empty, we might want to keep it? The requirement says "x
            // icon" or "clearQuery".
            // However, if the query is blank, showing a shortcut icon without query might be weird.
            // Let's stick to the explicit clearQuery for now.
            // But wait, onQueryChange handles the empty case at the top.
            // If I backspace "hello world" to "", detectedTarget will be lockedShortcutTarget
            // (Google).
            // So detection stays.
            return
        }

        // Check for shortcuts at the start of query (show UI button) BEFORE calculator processing
        var detectedTarget: SearchTarget? = lockedShortcutTarget

        // If we don't have a locked engine, try to detect one
        if (detectedTarget == null) {
            val shortcutMatchAtStart = shortcutHandler.detectShortcutAtStart(trimmedQuery)
            if (shortcutMatchAtStart != null) {
                detectedTarget = shortcutMatchAtStart.second
                lockedShortcutTarget = detectedTarget

                // Strip the shortcut from the query and update recursively
                val queryWithoutShortcut = shortcutMatchAtStart.first
                onQueryChange(queryWithoutShortcut)
                return
            }
        }

        // Check for shortcuts at the end of query (auto-execute)
        val shortcutMatchAtEnd = shortcutHandler.detectShortcut(trimmedQuery)
        if (shortcutMatchAtEnd != null) {
            val (queryWithoutShortcut, target) = shortcutMatchAtEnd
            // Automatically perform search with the detected target
            navigationHandler.openSearchTarget(queryWithoutShortcut.trim(), target)
            // Update query to remove shortcut but keep the remaining query
            if (queryWithoutShortcut.isBlank()) {
                clearQuery()
            } else {
                onQueryChange(queryWithoutShortcut)
            }
            return
        }

        // Check if query is a math expression (only if calculator is enabled)
        // Only process calculator if no shortcut was detected at start
        val calculatorResult =
                if (detectedTarget == null) {
                    calculatorHandler.processQuery(trimmedQuery)
                } else {
                    CalculatorState()
                }

        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(trimmedQuery)
        appSearchManager.resetNoMatchPrefixIfNeeded(normalizedQuery)

        val shouldSkipSearch = appSearchManager.shouldSkipDueToNoMatchPrefix(normalizedQuery)
        val matches =
                if (shouldSkipSearch || detectedTarget != null) {
                    emptyList()
                } else {
                    appSearchManager.deriveMatches(
                                    trimmedQuery,
                                    cachedAllSearchableApps,
                                    getGridItemCount(),
                            )
                            .also { results ->
                                if (results.isEmpty()) {
                                    appSearchManager.setNoMatchPrefix(normalizedQuery)
                                }
                            }
                }

        // Clear web suggestions when query changes
        webSuggestionHandler.cancelSuggestions()
        updateUiState { state ->
            state.copy(
                    query = newQuery,
                    searchResults = matches,
                    calculatorState = calculatorResult,
                    webSuggestions = emptyList(),
                    detectedShortcutTarget = detectedTarget,
            )
        }

        // Skip secondary searches if calculator result is shown
        if (calculatorResult.result == null) {
            if (detectedTarget != null) {
                secondarySearchOrchestrator.performWebSuggestionsOnly(newQuery)
            } else {
                secondarySearchOrchestrator.performSecondarySearches(newQuery)
            }
        } else {
            // Clear search results when showing calculator
            _uiState.update { state ->
                state.copy(
                        contactResults = emptyList(),
                        fileResults = emptyList(),
                        settingResults = emptyList(),
                        appShortcutResults = emptyList(),
                        webSuggestions = emptyList(),
                )
            }
        }
    }

    // Removed detectShortcut (unused)

    fun clearDetectedShortcut() {
        lockedShortcutTarget = null
        updateUiState { it.copy(detectedShortcutTarget = null) }
    }

    fun clearQuery() {
        lockedShortcutTarget = null
        onQueryChange("")
    }

    // Moved loadPinnedContactsAndFiles and loadExcludedContactsAndFiles to PinningHandler

    fun handleOnResume() {
        val previous = _uiState.value.hasUsagePermission
        val latest = repository.hasUsageAccess()
        if (previous != latest) {
            _uiState.update { it.copy(hasUsagePermission = latest) }
        }
        if (latest) {
            refreshApps()
        } else {
            refreshDerivedState()
        }
        handleOptionalPermissionChange()
        pinningHandler.loadPinnedContactsAndFiles()
        pinningHandler.loadExcludedContactsAndFiles()
        refreshSettingsState()
        refreshAppShortcutsState()
        viewModelScope.launch(Dispatchers.IO) {
            searchEngineManager.ensureInitialized()
            searchEngineManager.refreshBrowserTargets()
        }
    }

    // Navigation Delegates
    fun openUsageAccessSettings() = navigationHandler.openUsageAccessSettings()

    fun openAppSettings() = navigationHandler.openAppSettings()

    fun openAllFilesAccessSettings() = navigationHandler.openAllFilesAccessSettings()

    fun openFilesPermissionSettings() = navigationHandler.openFilesPermissionSettings()

    fun openContactPermissionSettings() = navigationHandler.openContactPermissionSettings()

    fun launchApp(appInfo: AppInfo) =
            navigationHandler.launchApp(
                    appInfo,
                    shouldTrackRecentFallback = !_uiState.value.hasUsagePermission,
            )

    fun openAppInfo(appInfo: AppInfo) = navigationHandler.openAppInfo(appInfo)

    fun openAppInfo(packageName: String) = navigationHandler.openAppInfo(packageName)

    fun requestUninstall(appInfo: AppInfo) = navigationHandler.requestUninstall(appInfo)

    fun openSearchUrl(
            query: String,
            searchEngine: SearchEngine,
            addToRecentSearches: Boolean = true,
    ) = navigationHandler.openSearchUrl(query, searchEngine, addToRecentSearches)

    fun openSearchTarget(
            query: String,
            target: SearchTarget,
            addToRecentSearches: Boolean = true,
    ) = navigationHandler.openSearchTarget(query, target, addToRecentSearches)

    fun searchIconPacks() = navigationHandler.searchIconPacks()

    fun openFile(deviceFile: DeviceFile) = navigationHandler.openFile(deviceFile)

    fun openContainingFolder(deviceFile: DeviceFile) = navigationHandler.openContainingFolder(deviceFile)

    fun openContact(contactInfo: ContactInfo) = navigationHandler.openContact(contactInfo)

    fun openEmail(email: String) = navigationHandler.openEmail(email)

    fun launchAppShortcut(shortcut: StaticShortcut) {
        val error = launchStaticShortcut(getApplication(), shortcut)
        if (error != null) {
            showToast(error)
        } else {
            userPreferences.addRecentItem(RecentSearchEntry.AppShortcut(shortcutKey(shortcut)))
            onNavigationTriggered()
        }
    }

    // App Management Delegates
    fun hideApp(appInfo: AppInfo) = appManager.hideApp(appInfo, _uiState.value.query.isNotBlank())

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

    fun getAppShortcutNickname(shortcutId: String): String? =
            appShortcutManager.getShortcutNickname(shortcutId)

    fun removeExcludedAppShortcut(shortcut: StaticShortcut) =
            appShortcutManager.removeExcludedShortcut(shortcut)

    fun addCustomAppShortcutFromPickerResult(
        resultData: Intent?,
        showDefaultToast: Boolean = true,
        onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
        onAddFailed: (() -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val addedShortcut = appShortcutRepository.addCustomShortcutFromPickerResult(resultData)
            if (addedShortcut != null) {
                appShortcutSearchHandler.loadShortcuts()
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
            appShortcutSearchHandler.loadShortcuts()
            withContext(Dispatchers.Main) {
                refreshAppShortcutsState()
                refreshRecentItems()
                showToast(R.string.settings_app_shortcuts_delete_success)
            }
        }
    }

    // Global Actions
    fun clearAllExclusions() {
        contactManager.clearAllExcludedContacts()
        fileManager.clearAllExcludedFiles()
        appManager.clearAllHiddenApps()
        settingsManager.clearAllExcludedSettings()
        appShortcutManager.clearAllExcludedShortcuts()
        pinningHandler.loadExcludedContactsAndFiles()
        refreshSettingsState(updateResults = false)
        refreshAppShortcutsState(updateResults = false)
    }

    fun setWallpaperBackgroundAlpha(alpha: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedAlpha = alpha.coerceIn(0f, 1f)
            userPreferences.setWallpaperBackgroundAlpha(sanitizedAlpha)
            wallpaperBackgroundAlpha = sanitizedAlpha
            _uiState.update { it.copy(wallpaperBackgroundAlpha = sanitizedAlpha) }
        }
    }

    fun setWallpaperBlurRadius(radius: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedRadius = radius.coerceIn(0f, UiPreferences.MAX_WALLPAPER_BLUR_RADIUS)
            userPreferences.setWallpaperBlurRadius(sanitizedRadius)
            wallpaperBlurRadius = sanitizedRadius
            _uiState.update { it.copy(wallpaperBlurRadius = sanitizedRadius) }
        }
    }

    fun setOverlayGradientTheme(theme: OverlayGradientTheme) {
        viewModelScope.launch(Dispatchers.IO) {
            if (overlayGradientTheme == theme) return@launch
            userPreferences.setOverlayGradientTheme(theme)
            overlayGradientTheme = theme
            _uiState.update { it.copy(overlayGradientTheme = theme) }
        }
    }

    fun setOverlayThemeIntensity(intensity: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedIntensity = sanitizeOverlayThemeIntensity(intensity)
            if (overlayThemeIntensity == sanitizedIntensity) return@launch
            userPreferences.setOverlayThemeIntensity(sanitizedIntensity)
            overlayThemeIntensity = sanitizedIntensity
            _uiState.update { it.copy(overlayThemeIntensity = sanitizedIntensity) }
        }
    }

    fun setBackgroundSource(source: BackgroundSource) {
        viewModelScope.launch(Dispatchers.IO) {
            if (backgroundSource == source) return@launch
            userPreferences.setBackgroundSource(source)
            backgroundSource = source
            _uiState.update {
                it.copy(
                        backgroundSource = source,
                        showWallpaperBackground = source != BackgroundSource.THEME,
                )
            }
        }
    }

    fun setCustomImageUri(uri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalized = uri?.trim()?.takeIf { it.isNotEmpty() }
            if (customImageUri == normalized) return@launch
            userPreferences.setCustomImageUri(normalized)
            customImageUri = normalized
            _uiState.update { it.copy(customImageUri = normalized) }
        }
    }

    private fun sanitizeOverlayThemeIntensity(intensity: Float): Float =
            intensity.coerceIn(
                    UiPreferences.MIN_OVERLAY_THEME_INTENSITY,
                    UiPreferences.MAX_OVERLAY_THEME_INTENSITY,
            )

    fun refreshIconPacks() = iconPackHandler.refreshIconPacks()

    fun setIconPackPackage(packageName: String?) {
        val state = _uiState.value
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

    // Shortcuts
    fun setShortcutsEnabled(enabled: Boolean) = shortcutHandler.setShortcutsEnabled(enabled)

    fun setShortcutCode(
            target: SearchTarget,
            code: String,
    ) = shortcutHandler.setShortcutCode(target, code)

    fun setShortcutEnabled(
            target: SearchTarget,
            enabled: Boolean,
    ) = shortcutHandler.setShortcutEnabled(target, enabled)

    fun getShortcutCode(target: SearchTarget): String = shortcutHandler.getShortcutCode(target)

    fun isShortcutEnabled(target: SearchTarget): Boolean = shortcutHandler.isShortcutEnabled(target)

    fun areShortcutsEnabled(): Boolean = true

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
        val state = _uiState.value
        val resolvedCallingApp =
                resolveCallingApp(
                        app = app,
                        isWhatsAppInstalled = state.isWhatsAppInstalled,
                        isTelegramInstalled = state.isTelegramInstalled,
                        isSignalInstalled = state.isSignalInstalled,
                        isGoogleMeetInstalled = state.isGoogleMeetInstalled,
                )
        if (resolvedCallingApp != app) {
            userPreferences.setCallingApp(resolvedCallingApp)
        }
        _uiState.update { it.copy(callingApp = resolvedCallingApp) }
    }

    fun acknowledgeReleaseNotes() =
            releaseNotesHandler.acknowledgeReleaseNotes(_uiState.value.releaseNotesVersionName)

    fun requestDirectSearch(query: String) = directSearchHandler.requestDirectSearch(query)

    fun setShowStartSearchingOnOnboarding(show: Boolean) {
        _uiState.update { it.copy(showStartSearchingOnOnboarding = show) }
    }

    fun onSearchEngineOnboardingDismissed() {
        _uiState.update {
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

    fun onPersonalContextHintDismissed() {
        userPreferences.setHasSeenPersonalContextHint(true)
        updateUiState { it.copy(showPersonalContextHint = false) }
    }

    // Contact Actions
    fun callContact(contactInfo: ContactInfo) = contactActionHandler.callContact(contactInfo)

    fun smsContact(contactInfo: ContactInfo) = contactActionHandler.smsContact(contactInfo)

    fun onPhoneNumberSelected(
            phoneNumber: String,
            rememberChoice: Boolean,
    ) = contactActionHandler.onPhoneNumberSelected(phoneNumber, rememberChoice)

    fun dismissPhoneNumberSelection() = contactActionHandler.dismissPhoneNumberSelection()

    fun dismissDirectDialChoice() = contactActionHandler.dismissDirectDialChoice()

    fun handleContactMethod(
            contactInfo: ContactInfo,
            method: ContactMethod,
    ) = contactActionHandler.handleContactMethod(contactInfo, method)

    fun trackRecentContactTap(contactInfo: ContactInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.addRecentItem(RecentSearchEntry.Contact(contactInfo.contactId))
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
            normalizedTemplate: String,
            faviconBase64: String,
    ) = searchEngineManager.addCustomSearchEngine(normalizedTemplate, faviconBase64)

    fun updateCustomSearchEngine(
            customId: String,
            name: String,
            urlTemplateInput: String,
            faviconBase64: String?,
    ) = searchEngineManager.updateCustomSearchEngine(customId, name, urlTemplateInput, faviconBase64)

    fun deleteCustomSearchEngine(customId: String) =
            searchEngineManager.deleteCustomSearchEngine(customId)

    fun setSearchEngineCompactMode(enabled: Boolean) =
            searchEngineManager.setSearchEngineCompactMode(enabled)

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
            val query = _uiState.value.query
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
            val query = _uiState.value.query
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
            val query = _uiState.value.query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.performSecondarySearches(query)
            }
        }
    }

    fun setShowHiddenFiles(show: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setShowHiddenFiles(show)
            showHiddenFiles = show
            updateUiState { it.copy(showHiddenFiles = show) }

            // Re-run file search if there's an active query
            val query = _uiState.value.query
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

            val query = _uiState.value.query
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

            val query = _uiState.value.query
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

    fun setAmazonDomain(domain: String?) {
        amazonDomain = domain
        userPreferences.setAmazonDomain(domain)
        _uiState.update { state -> state.copy(amazonDomain = amazonDomain) }
    }

    fun setGeminiApiKey(apiKey: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            directSearchHandler.setGeminiApiKey(apiKey)

            val hasGemini = !apiKey.isNullOrBlank()
            searchEngineManager.updateSearchTargetsForGemini(hasGemini)

            val availableModels =
                if (hasGemini) {
                    directSearchHandler.refreshAvailableGeminiModels(forceRefresh = true)
                } else {
                    directSearchHandler.getAvailableGeminiModels()
                }

            _uiState.update {
                it.copy(
                        hasGeminiApiKey = hasGemini,
                        geminiApiKeyLast4 = apiKey?.trim()?.takeLast(4),
                        geminiModel = directSearchHandler.getGeminiModel(),
                        availableGeminiModels = availableModels,
                )
            }
        }
    }

    fun setPersonalContext(context: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            directSearchHandler.setPersonalContext(context)
            _uiState.update { it.copy(personalContext = context?.trim().orEmpty()) }
        }
    }

    fun setGeminiModel(modelId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            directSearchHandler.setGeminiModel(modelId)
            val normalized = modelId?.trim().takeUnless { it.isNullOrBlank() }
            _uiState.update {
                it.copy(
                        geminiModel = normalized ?: GeminiModelCatalog.DEFAULT_MODEL_ID,
                )
            }
        }
    }

    fun setGeminiGroundingEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            directSearchHandler.setGeminiGroundingEnabled(enabled)
            _uiState.update { it.copy(geminiGroundingEnabled = enabled) }
        }
    }

    fun refreshAvailableGeminiModels() {
        viewModelScope.launch(Dispatchers.IO) {
            val models = directSearchHandler.refreshAvailableGeminiModels(forceRefresh = true)
            _uiState.update { it.copy(availableGeminiModels = models) }
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
        val sectionEnabled = SearchSection.APPS !in state.disabledSections

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
                val hasPinned = state.pinnedApps.isNotEmpty()
                if (hasResults || hasPinned) {
                    AppsSectionVisibility.ShowingResults(hasPinned = hasPinned)
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
        val sectionEnabled = SearchSection.APP_SHORTCUTS !in state.disabledSections

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
        val sectionEnabled = SearchSection.CONTACTS !in state.disabledSections

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
        val sectionEnabled = SearchSection.FILES !in state.disabledSections

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
        val sectionEnabled = SearchSection.SETTINGS !in state.disabledSections

        return when {
            !sectionEnabled -> {
                SettingsSectionVisibility.Hidden
            }
            else -> {
                val hasResults = state.settingResults.isNotEmpty()
                val hasPinned = state.pinnedSettings.isNotEmpty()
                if (hasResults || hasPinned) {
                    SettingsSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    SettingsSectionVisibility.NoResults
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
                    searchEnginesState = computeSearchEnginesVisibility(state),
            )

    /**
     * Updates all visibility states in the UI state. Call this whenever the underlying data changes
     * that could affect visibility.
     */
    private fun updateVisibilityStates() {
        _uiState.update { currentState -> applyVisibilityStates(currentState) }
    }

    companion object {
        private const val SECONDARY_SEARCH_DEBOUNCE_MS = 150L
        private const val MAX_RECENT_ITEMS = 10
    }

    private fun getGridItemCount(): Int =
            SearchScreenConstants.ROW_COUNT * getAppGridColumns(getApplication())

    private fun refreshDerivedState(
            lastUpdated: Long? = null,
            isLoading: Boolean? = null,
    ) {
        // Refresh nicknames cache to ensure we have the latest data
        appSearchManager.refreshNicknames()

        val apps = appSearchManager.cachedApps
        val visibleAppList = appSearchManager.availableApps()
        val hasUsagePermission = _uiState.value.hasUsagePermission
        val suggestionsEnabled = _uiState.value.appSuggestionsEnabled

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
                            visibleAppList.filterNot { pinnedPackages.contains(it.launchCountKey()) }
                    extractSuggestedApps(
                            apps = recentsSource,
                            limit = getGridItemCount(),
                            hasUsagePermission = hasUsagePermission,
                    )
                } else {
                    emptyList()
                }

        val query = _uiState.value.query
        val trimmedQuery = query.trim()
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

        // Always update the searchable apps cache regardless of query state
        // Include both pinned and non-pinned apps in search, let ranking determine order
        val nonPinnedApps = appSearchManager.searchSourceApps()
        val allSearchableApps = (pinnedAppsForResults + nonPinnedApps).distinctBy { it.launchCountKey() }
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

        _uiState.update { state ->
            state.copy(
                    allApps = apps,
                    recentApps = recents,
                    searchResults = searchResults,
                    pinnedApps = pinnedAppsForSuggestions,
                    suggestionExcludedApps = suggestionHiddenAppList,
                    resultExcludedApps = resultHiddenAppList,
                    indexedAppCount = visibleAppList.size,
                    cacheLastUpdatedMillis = lastUpdated ?: state.cacheLastUpdatedMillis,
                    isLoading = isLoading ?: state.isLoading,
                    messagingApp = resolvedMessagingApp,
                    callingApp = resolvedCallingApp,
                    isWhatsAppInstalled = isWhatsAppInstalled,
                    isTelegramInstalled = isTelegramInstalled,
                    isSignalInstalled = isSignalInstalled,
                    isGoogleMeetInstalled = isGoogleMeetInstalled,
                    nicknameUpdateVersion = state.nicknameUpdateVersion + 1,
            )
        }

        iconPackHandler.prefetchVisibleAppIcons(
                pinnedApps = pinnedAppsForSuggestions,
                recents = recents,
                searchResults = searchResults,
        )

        // Re-trigger secondary searches when nicknames change and there's an active query
        // This ensures contacts/files/settings clear immediately when nicknames are removed
        if (trimmedQuery.isNotBlank()) {
            secondarySearchOrchestrator.performSecondarySearches(query)
        }
    }

    fun handleOptionalPermissionChange() {
        val optionalChanged = refreshOptionalPermissions()
        if (optionalChanged && _uiState.value.query.isNotBlank()) {
            secondarySearchOrchestrator.performSecondarySearches(_uiState.value.query)
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
                            recentInstalls.filterNot { it.launchCountKey() == topRecent?.launchCountKey() }
                    val excludedPackages =
                            recentInstallsExcludingTop
                                    .asSequence()
                                    .map { it.launchCountKey() }
                                    .toSet()
                                    .let { packages ->
                                        topRecent?.launchCountKey()?.let { packages + it } ?: packages
                                    }
                    val remainingRecents =
                            recentlyOpened.filterNot { excludedPackages.contains(it.launchCountKey()) }

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
                            recentInstalls.filterNot { it.launchCountKey() == topRecent?.launchCountKey() }
                    val excludedPackages =
                            recentInstallsExcludingTop
                                    .asSequence()
                                    .map { it.launchCountKey() }
                                    .toSet()
                                    .let { packages ->
                                        topRecent?.launchCountKey()?.let { packages + it } ?: packages
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

    // performSecondarySearches moved to SecondarySearchOrchestrator

    fun onWebSuggestionTap(suggestion: String) {
        // Check if there's a detected shortcut engine and perform immediate search
        val currentState = _uiState.value
        val detectedTarget = currentState.detectedShortcutTarget

        if (detectedTarget != null) {
            // Perform search immediately in the detected search target
            navigationHandler.openSearchTarget(suggestion.trim(), detectedTarget)
        } else {
            // No shortcut detected, copy the suggestion text to the search bar
            onQueryChange(suggestion)
        }

        // Mark that a web suggestion was selected so we can hide suggestions
        _uiState.update { it.copy(webSuggestionWasSelected = true) }
    }

    private fun refreshOptionalPermissions(): Boolean {
        val hasContacts = hasContactPermission()
        val hasFiles = hasFilePermission()
        val hasCall = hasCallPermission()
        val hasWallpaper = hasWallpaperPermission()
        val previousState = _uiState.value
        val changed =
                previousState.hasContactPermission != hasContacts ||
                        previousState.hasFilePermission != hasFiles ||
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

            _uiState.update { state ->
                state.copy(
                        hasContactPermission = hasContacts,
                        hasFilePermission = hasFiles,
                        hasCallPermission = hasCall,
                        hasWallpaperPermission = hasWallpaper,
                        wallpaperAvailable = wallpaperAvailable,
                        directDialEnabled = this@SearchViewModel.directDialEnabled,
                        contactResults = if (hasContacts) state.contactResults else emptyList(),
                        fileResults = if (hasFiles) state.fileResults else emptyList(),
                )
            }

            // Refresh disabled sections based on new permission state
            sectionManager.refreshDisabledSections()
        }
        return changed
    }

    fun showContactMethodsBottomSheet(contactInfo: ContactInfo) {
        _uiState.update { it.copy(contactMethodsBottomSheet = contactInfo) }
    }

    fun dismissContactMethodsBottomSheet() {
        _uiState.update { it.copy(contactMethodsBottomSheet = null) }
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

    fun onCallPermissionResult(isGranted: Boolean) {
        contactActionHandler.onCallPermissionResult(isGranted)
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
        _uiState.update {
            it.copy(
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner(),
            )
        }
    }

    fun incrementUsagePermissionBannerDismissCount() {
        userPreferences.incrementUsagePermissionBannerDismissCount()
        _uiState.update {
            it.copy(
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner(),
            )
        }
    }

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) {
        userPreferences.setUsagePermissionBannerSessionDismissed(dismissed)
        _uiState.update {
            it.copy(
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner(),
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
