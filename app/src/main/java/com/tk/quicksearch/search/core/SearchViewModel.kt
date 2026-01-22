package com.tk.quicksearch.search.core

import android.app.Application
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
import com.tk.quicksearch.search.apps.prefetchAppIcons
import com.tk.quicksearch.search.calculator.CalculatorHandler
import com.tk.quicksearch.search.common.PinningHandler
import com.tk.quicksearch.search.contacts.actions.ContactActionHandler
import com.tk.quicksearch.search.contacts.utils.ContactManagementHandler
import com.tk.quicksearch.search.contacts.utils.MessagingHandler
import com.tk.quicksearch.search.contacts.utils.TelegramContactUtils
import com.tk.quicksearch.search.data.AppShortcutRepository
import com.tk.quicksearch.search.data.AppUsageRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.launchStaticShortcut
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsManagementHandler
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsRepository
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.directSearch.DirectSearchHandler
import com.tk.quicksearch.search.files.FileManagementHandler
import com.tk.quicksearch.search.files.FileSearchHandler
import com.tk.quicksearch.search.fuzzy.FuzzySearchConfigurationManager
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.searchEngines.SearchEngineManager
import com.tk.quicksearch.search.searchEngines.SecondarySearchOrchestrator
import com.tk.quicksearch.search.searchEngines.ShortcutHandler
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.search.webSuggestions.WebSuggestionHandler
import com.tk.quicksearch.util.PackageConstants
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository by lazy { AppUsageRepository(application.applicationContext) }
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
                application.applicationContext
        )
    }

    private val permissionManager by lazy {
        PermissionManager(contactRepository, fileRepository, userPreferences)
    }
    private val searchOperations by lazy { SearchOperations(contactRepository) }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Cache searchable apps to avoid re-computing on every query change
    @Volatile private var cachedAllSearchableApps: List<AppInfo> = emptyList()

    // Consolidated startup configuration loaded in single batch operation
    @Volatile private var startupConfig: UserAppPreferences.StartupConfig? = null

    // UI feedback is now handled by UiFeedbackService

    private fun updateUiState(updater: (SearchUiState) -> SearchUiState) {
        _uiState.update { state ->
            val updated = updater(state)
            applyContactActionHint(state, updated)
        }
        // Only update visibility states after startup completes to avoid lazy handler init during
        // first paint
        if (isStartupComplete) {
            updateVisibilityStates()
        }
    }

    private fun applyContactActionHint(
            previous: SearchUiState,
            updated: SearchUiState
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
                this::updateUiState
        )
    }
    val fileManager by lazy {
        FileManagementHandler(
                userPreferences,
                viewModelScope,
                this::refreshDerivedState,
                this::updateUiState
        )
    }
    val settingsManager by lazy {
        DeviceSettingsManagementHandler(
                userPreferences,
                viewModelScope,
                this::refreshDerivedState,
                this::updateUiState
        )
    }
    val appShortcutManager by lazy {
        AppShortcutManagementHandler(
                userPreferences,
                viewModelScope,
                this::refreshAppShortcutsState,
                this::updateUiState
        )
    }
    val searchEngineManager by lazy {
        SearchEngineManager(
                application.applicationContext,
                userPreferences,
                viewModelScope,
                this::updateUiState
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
                uiStateUpdater = this::updateUiState
        )
    }

    val webSuggestionHandler by lazy {
        WebSuggestionHandler(
                scope = viewModelScope,
                userPreferences = userPreferences,
                uiStateUpdater = this::updateUiState
        )
    }

    val calculatorHandler by lazy {
        CalculatorHandler(
                scope = viewModelScope,
                userPreferences = userPreferences,
                uiStateUpdater = this::updateUiState
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
                initialFuzzyConfig = fuzzySearchConfigManager.getAppFuzzyConfig()
        )
    }

    val settingsSearchHandler by lazy {
        DeviceSettingsSearchHandler(
                context = application.applicationContext,
                repository = settingsShortcutRepository,
                userPreferences = userPreferences,
                scope = viewModelScope,
                showToastCallback = this::showToast
        )
    }

    val appShortcutSearchHandler by lazy {
        AppShortcutSearchHandler(
                repository = appShortcutRepository,
                userPreferences = userPreferences
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
                showToastCallback = this::showToast
        )
    }

    val shortcutHandler by lazy {
        ShortcutHandler(
                userPreferences = userPreferences,
                scope = viewModelScope,
                uiStateUpdater = this::updateUiState,
                directSearchHandler = directSearchHandler,
                searchTargetsProvider = { searchEngineManager.searchTargetsOrder }
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
                searchOperations = searchOperations
        )
    }

    private val secondarySearchOrchestrator by lazy {
        SecondarySearchOrchestrator(
                scope = viewModelScope,
                unifiedSearchHandler = unifiedSearchHandler,
                webSuggestionHandler = webSuggestionHandler,
                sectionManager = sectionManager,
                uiStateUpdater = this::updateUiState,
                currentStateProvider = { _uiState.value }
        )
    }

    private var enabledFileTypes: Set<FileType> = emptySet()
    private var showFolders: Boolean = false
    private var showSystemFiles: Boolean = false
    private var showHiddenFiles: Boolean = false
    private var excludedFileExtensions: Set<String> = emptySet()
    private var oneHandedMode: Boolean = false
    private var directDialEnabled: Boolean = false
    private var hasSeenDirectDialChoice: Boolean = false
    private var showWallpaperBackground: Boolean = true
    private var wallpaperBackgroundAlpha: Float = UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA
    private var wallpaperBlurRadius: Float = UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS
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

    private fun hasContactPermission(): Boolean {
        return contactRepository.hasPermission()
    }

    private fun hasFilePermission(): Boolean {
        return fileRepository.hasPermission()
    }

    private fun hasCallPermission(): Boolean {
        return PermissionRequestHandler.checkCallPermission(getApplication())
    }

    fun handleOnStop() {
        if (pendingNavigationClear) {
            clearQuery()
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
                        showToastCallback = this::showToast
                )

        // Initialize ContactActionHandler with uiFeedbackService
        contactActionHandler =
                ContactActionHandler(
                        context = app,
                        userPreferences = userPreferences,
                        getMessagingApp = { messagingHandler.messagingApp },
                        getDirectDialEnabled = { directDialEnabled },
                        getHasSeenDirectDialChoice = { hasSeenDirectDialChoice },
                        getCurrentState = { _uiState.value },
                        uiStateUpdater = { update -> _uiState.update(update) },
                        clearQuery = this::onNavigationTriggered,
                        showToastCallback = this::showToast
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

        // Apply immediately - DO NOT BLOCK on icon loading
        withContext(Dispatchers.Main) {
            _uiState.update {
                it.copy(
                        oneHandedMode = oneHandedMode,
                        // We don't have full prefs yet, so keep initializing flag true
                        // but show the apps we found in cache
                        isInitializing = true
                )
            }

            if (cachedAppsList != null && cachedAppsList.isNotEmpty()) {
                initializeWithCacheMinimal(cachedAppsList)
            }
        }

        // Store the full startup config for Phase 2
        this.startupConfig = startupConfig

        // Prefetch icons in background (non-blocking) after UI is shown
        // Icons will lazy-load via rememberAppIcon() with placeholders until ready
        if (cachedAppsList != null && cachedAppsList.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val visibleApps =
                        repository.extractRecentlyOpenedApps(cachedAppsList, GRID_ITEM_COUNT)
                val iconPack = userPreferences.getSelectedIconPackPackage()
                prefetchAppIcons(
                        context = getApplication(),
                        packageNames = visibleApps.map { it.packageName },
                        iconPackPackage = iconPack
                )
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
            appSearchManager.setFuzzySearchEnabled(true)

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
        excludedFileExtensions = prefs.excludedFileExtensions
        oneHandedMode = prefs.oneHandedMode
        directDialEnabled = prefs.directDialEnabled
        hasSeenDirectDialChoice = prefs.hasSeenDirectDialChoice
        showWallpaperBackground = prefs.showWallpaperBackground
        wallpaperBackgroundAlpha = prefs.wallpaperBackgroundAlpha
        wallpaperBlurRadius = prefs.wallpaperBlurRadius
        amazonDomain = prefs.amazonDomain

        _uiState.update {
            it.copy(
                    enabledFileTypes = enabledFileTypes,
                    showFolders = showFolders,
                    showSystemFiles = showSystemFiles,
                    showHiddenFiles = showHiddenFiles,
                    excludedFileExtensions = excludedFileExtensions,
                    oneHandedMode = oneHandedMode,
                    directDialEnabled = directDialEnabled,
                    showWallpaperBackground = showWallpaperBackground,
                    wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = wallpaperBlurRadius,
                    amazonDomain = amazonDomain,
                    recentQueriesEnabled = prefs.recentSearchesEnabled,
                    recentQueriesCount = userPreferences.getRecentQueriesCount(),
                    webSuggestionsCount = userPreferences.getWebSuggestionsCount(),
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner()
            )
        }

        // Load recent queries on startup if enabled
        refreshRecentQueries()
    }

    private fun initializeWithCacheMinimal(cachedAppsList: List<AppInfo>) {
        appSearchManager.initCache(cachedAppsList)
        val lastUpdated = repository.cacheLastUpdatedMillis()

        // Just show the raw list of apps first!
        // Don't filter, don't sort, don't check pinned apps yet
        // This is the fastest possible way to get pixels on screen
        _uiState.update { state ->
            state.copy(
                    cacheLastUpdatedMillis = lastUpdated,
                    // Temporarily show all cached apps until we load hidden/pinned prefs
                    recentApps =
                            repository.extractRecentlyOpenedApps(cachedAppsList, GRID_ITEM_COUNT),
                    indexedAppCount = cachedAppsList.size,

                    // Critical: update these to prevent flashing
                    oneHandedMode = oneHandedMode
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
                        sectionOrder = sectionManager.sectionOrder,
                        disabledSections = sectionManager.disabledSections,
                        isSearchEngineCompactMode = searchEngineManager.isSearchEngineCompactMode,
                        showSearchEngineOnboarding = false,
                        showSearchBarWelcomeAnimation = shouldShowSearchBarWelcome(),
                        webSuggestionsEnabled = webSuggestionHandler.isEnabled,
                        calculatorEnabled = userPreferences.isCalculatorEnabled(),
                        hasGeminiApiKey = !directSearchHandler.getGeminiApiKey().isNullOrBlank(),
                        geminiApiKeyLast4 = directSearchHandler.getGeminiApiKey()?.takeLast(4),
                        personalContext = directSearchHandler.getPersonalContext(),
                        showPersonalContextHint = !userPreferences.hasSeenPersonalContextHint(),
                        // Messaging info
                        messagingApp = messagingInfo.messagingApp,
                        isWhatsAppInstalled = messagingInfo.isWhatsAppInstalled,
                        isTelegramInstalled = messagingInfo.isTelegramInstalled
                )
            }

            // 3. Start heavy background loads
            launch(Dispatchers.IO) { loadApps() }

            launch(Dispatchers.IO) { loadSettingsShortcuts() }

            launch(Dispatchers.IO) { loadAppShortcuts() }

            launch(Dispatchers.IO) { iconPackHandler.refreshIconPacks() }

            launch(Dispatchers.IO) {
                // Pinning handler loads contacts and files
                pinningHandler.loadPinnedContactsAndFiles()
                pinningHandler.loadExcludedContactsAndFiles()
            }

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
        val hasSeen = userPreferences.hasSeenSearchBarWelcome()

        if (!hasSeen) {
            userPreferences.setHasSeenSearchBarWelcome(true)
            return true
        }
        return false
    }

    fun onSearchBarWelcomeAnimationCompleted() {
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
        val resolvedMessagingApp =
                messagingHandler.updateMessagingAvailability(
                        whatsappInstalled = isWhatsAppInstalled,
                        telegramInstalled = isTelegramInstalled,
                        updateState = false
                )

        return MessagingAppInfo(isWhatsAppInstalled, isTelegramInstalled, resolvedMessagingApp)
    }

    private data class MessagingAppInfo(
            val isWhatsAppInstalled: Boolean,
            val isTelegramInstalled: Boolean,
            val messagingApp: MessagingApp
    )

    fun setCalculatorEnabled(enabled: Boolean) {
        // Delegate to new CalculatorHandler once implemented
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setCalculatorEnabled(enabled)
            _uiState.update { it.copy(calculatorEnabled = enabled) }
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
                refreshRecentQueries()
            }
        }
    }

    fun setRecentQueriesCount(count: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setRecentQueriesCount(count)
            _uiState.update { it.copy(recentQueriesCount = count) }

            // Refresh recent queries display if query is empty
            if (_uiState.value.query.isEmpty()) {
                refreshRecentQueries()
            }
        }
    }

    private fun refreshRecentQueries() {
        viewModelScope.launch(Dispatchers.IO) {
            val queries =
                    if (_uiState.value.recentQueriesEnabled) {
                        userPreferences
                                .getRecentQueries()
                                .take(userPreferences.getRecentQueriesCount())
                    } else {
                        emptyList()
                    }
            _uiState.update { it.copy(recentQueries = queries) }
        }
    }

    fun onRecentQueryClick(query: String) {
        onQueryChange(query)
    }

    fun deleteRecentQuery(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.deleteRecentQuery(query)
            refreshRecentQueries()
        }
    }

    // Contact Card Actions (Synchronous access for UI composition, persistence is sync in prefs)
    fun getPrimaryContactCardAction(contactId: Long) =
            contactPreferences.getPrimaryContactCardAction(contactId)
    fun setPrimaryContactCardAction(
            contactId: Long,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction
    ) {
        contactPreferences.setPrimaryContactCardAction(contactId, action)
        _uiState.update { it.copy(contactActionsVersion = it.contactActionsVersion + 1) }
    }

    fun getSecondaryContactCardAction(contactId: Long) =
            contactPreferences.getSecondaryContactCardAction(contactId)

    fun setSecondaryContactCardAction(
            contactId: Long,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction
    ) {
        contactPreferences.setSecondaryContactCardAction(contactId, action)
        _uiState.update { it.copy(contactActionsVersion = it.contactActionsVersion + 1) }
    }

    fun onCustomAction(
            contactInfo: ContactInfo,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction
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

            fun matchesTelegramNumber(method: ContactMethod): Boolean {
                return TelegramContactUtils.isTelegramMethodForPhoneNumber(
                        context = appContext,
                        phoneNumber = action.phoneNumber,
                        telegramMethod = method
                ) || matchesPhoneNumber(method)
            }

            // Match the action to a ContactMethod
            val matchedMethod: ContactMethod? =
                    methods.find { method ->
                        when (action) {
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.Phone ->
                                    method is ContactMethod.Phone && matchesPhoneNumber(method)
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.Sms ->
                                    method is ContactMethod.Sms && matchesPhoneNumber(method)
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppCall ->
                                    method is ContactMethod.WhatsAppCall &&
                                            matchesPhoneNumber(method)
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppMessage ->
                                    method is ContactMethod.WhatsAppMessage &&
                                            matchesPhoneNumber(method)
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppVideoCall ->
                                    method is ContactMethod.WhatsAppVideoCall &&
                                            matchesPhoneNumber(method)
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramMessage ->
                                    method is ContactMethod.TelegramMessage &&
                                            matchesTelegramNumber(method)
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramCall ->
                                    method is ContactMethod.TelegramCall &&
                                            matchesTelegramNumber(method)
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramVideoCall ->
                                    method is ContactMethod.TelegramVideoCall &&
                                            matchesTelegramNumber(method)
                            is com.tk.quicksearch.search.contacts.models.ContactCardAction.GoogleMeet ->
                                    method is ContactMethod.GoogleMeet && matchesPhoneNumber(method)
                        }
                    }

            withContext(Dispatchers.Main) {
                if (matchedMethod != null) {
                    contactActionHandler.handleContactMethod(contactInfo, matchedMethod)
                } else {
                    // Fallback to generic action if specific method not found (e.g. dataId changed)
                    // For Phone/SMS this is easy
                    when (action) {
                        is com.tk.quicksearch.search.contacts.models.ContactCardAction.Phone ->
                                contactActionHandler.handleContactMethod(
                                        contactInfo,
                                        ContactMethod.Phone("Call", action.phoneNumber)
                                )
                        is com.tk.quicksearch.search.contacts.models.ContactCardAction.Sms ->
                                contactActionHandler.handleContactMethod(
                                        contactInfo,
                                        ContactMethod.Sms("Message", action.phoneNumber)
                                )
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
            stateUpdater: (Boolean) -> Unit
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
                                currentResults // Pass current results to avoid re-search if not
                        // needed? Actually logic handles it
                        )

        _uiState.update { state ->
            state.copy(
                    pinnedSettings = currentState.pinned,
                    excludedSettings = currentState.excluded,
                    settingResults = currentState.results
            )
        }
    }

    private fun refreshAppShortcutsState(updateResults: Boolean = true) {
        val query = if (updateResults) _uiState.value.query else ""
        val currentState =
                appShortcutSearchHandler.getShortcutsState(
                        query = query,
                        isSectionEnabled =
                                SearchSection.APP_SHORTCUTS !in sectionManager.disabledSections
                )

        updateUiState { state ->
            state.copy(
                    pinnedAppShortcuts = currentState.pinned,
                    excludedAppShortcuts = currentState.excluded,
                    appShortcutResults =
                            if (updateResults) currentState.results else state.appShortcutResults
            )
        }
    }

    fun refreshUsageAccess() {
        _uiState.update { it.copy(hasUsagePermission = repository.hasUsageAccess()) }
    }

    fun refreshApps(showToast: Boolean = false, forceUiUpdate: Boolean = false) {
        appSearchManager.refreshApps(showToast, forceUiUpdate)
    }

    fun refreshContacts(showToast: Boolean = false) {
        // Contacts are queried directly from the system provider, so we don't need to refresh a
        // cache
        // Instead, we can clear any current contact results to force a fresh query on next search
        _uiState.update { it.copy(contactResults = emptyList()) }
        // Show success toast only for user-triggered refreshes
        if (showToast) {
            showToast(R.string.contacts_refreshed_successfully)
        }
    }

    fun refreshFiles(showToast: Boolean = false) {
        // Files are queried directly from MediaStore, so we don't need to refresh a cache
        // Instead, we can clear any current file results to force a fresh query on next search
        _uiState.update { it.copy(fileResults = emptyList()) }
        // Show success toast only for user-triggered refreshes
        if (showToast) {
            showToast(R.string.files_refreshed_successfully)
        }
    }

    // Removed messaging logic (moved to MessagingHandler)

    // Removed isPackageInstalled (moved to MessagingHandler)

    // Removed Release Notes logic (moved to ReleaseNotesHandler)

    // Removed setMessagingApp (delegate in SearchViewModelExtensions if needed, or direct)

    fun setDirectDialEnabled(enabled: Boolean, manual: Boolean = true) {
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
                        webSuggestionWasSelected = false
                )
            }
            // Load recent queries when query is empty
            refreshRecentQueries()
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

        val normalizedQuery = trimmedQuery.lowercase(Locale.getDefault())
        appSearchManager.resetNoMatchPrefixIfNeeded(normalizedQuery)

        val shouldSkipSearch = appSearchManager.shouldSkipDueToNoMatchPrefix(normalizedQuery)
        val matches =
                if (shouldSkipSearch || detectedTarget != null) {
                    emptyList()
                } else {
                    appSearchManager.deriveMatches(trimmedQuery, cachedAllSearchableApps).also {
                            results ->
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
                    detectedShortcutTarget = detectedTarget
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
                        webSuggestions = emptyList()
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
    fun launchApp(appInfo: AppInfo) = navigationHandler.launchApp(appInfo)
    fun openAppInfo(appInfo: AppInfo) = navigationHandler.openAppInfo(appInfo)
    fun openAppInfo(packageName: String) = navigationHandler.openAppInfo(packageName)
    fun requestUninstall(appInfo: AppInfo) = navigationHandler.requestUninstall(appInfo)
    fun openSearchUrl(
            query: String,
            searchEngine: SearchEngine,
            addToRecentSearches: Boolean = true
    ) = navigationHandler.openSearchUrl(query, searchEngine, addToRecentSearches)
    fun openSearchTarget(query: String, target: SearchTarget, addToRecentSearches: Boolean = true) =
            navigationHandler.openSearchTarget(query, target, addToRecentSearches)
    fun searchIconPacks() = navigationHandler.searchIconPacks()
    fun openFile(deviceFile: DeviceFile) = navigationHandler.openFile(deviceFile)
    fun openContact(contactInfo: ContactInfo) = navigationHandler.openContact(contactInfo)
    fun openEmail(email: String) = navigationHandler.openEmail(email)
    fun launchAppShortcut(shortcut: StaticShortcut) {
        val error = launchStaticShortcut(getApplication(), shortcut)
        if (error != null) {
            showToast(error)
        } else {
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
    fun setAppNickname(appInfo: AppInfo, nickname: String?) =
            appManager.setAppNickname(appInfo, nickname)
    fun getAppNickname(packageName: String): String? = appManager.getAppNickname(packageName)
    fun clearCachedApps() = appSearchManager.clearCachedApps()

    // Contact Management Delegates
    fun pinContact(contactInfo: ContactInfo) = contactManager.pinContact(contactInfo)
    fun unpinContact(contactInfo: ContactInfo) = contactManager.unpinContact(contactInfo)
    fun excludeContact(contactInfo: ContactInfo) = contactManager.excludeContact(contactInfo)
    fun removeExcludedContact(contactInfo: ContactInfo) =
            contactManager.removeExcludedContact(contactInfo)
    fun clearAllExcludedContacts() = contactManager.clearAllExcludedContacts()
    fun setContactNickname(contactInfo: ContactInfo, nickname: String?) =
            contactManager.setContactNickname(contactInfo, nickname)
    fun getContactNickname(contactId: Long): String? = contactManager.getContactNickname(contactId)

    // File Management Delegates
    fun pinFile(deviceFile: DeviceFile) = fileManager.pinFile(deviceFile)
    fun unpinFile(deviceFile: DeviceFile) = fileManager.unpinFile(deviceFile)
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
    fun setFileNickname(deviceFile: DeviceFile, nickname: String?) =
            fileManager.setFileNickname(deviceFile, nickname)
    fun getFileNickname(uri: String): String? = fileManager.getFileNickname(uri)

    // Settings Management Delegates
    fun pinSetting(setting: DeviceSetting) = settingsManager.pinSetting(setting)
    fun unpinSetting(setting: DeviceSetting) = settingsManager.unpinSetting(setting)
    fun excludeSetting(setting: DeviceSetting) = settingsManager.excludeSetting(setting)
    fun setSettingNickname(setting: DeviceSetting, nickname: String?) =
            settingsManager.setSettingNickname(setting, nickname)
    fun getSettingNickname(id: String): String? = settingsManager.getSettingNickname(id)
    fun removeExcludedSetting(setting: DeviceSetting) =
            settingsManager.removeExcludedSetting(setting)
    fun clearAllExcludedSettings() = settingsManager.clearAllExcludedSettings()
    fun openSetting(setting: DeviceSetting) = settingsSearchHandler.openSetting(setting)

    // App Shortcut Management Delegates
    fun pinAppShortcut(shortcut: StaticShortcut) = appShortcutManager.pinShortcut(shortcut)
    fun unpinAppShortcut(shortcut: StaticShortcut) = appShortcutManager.unpinShortcut(shortcut)
    fun excludeAppShortcut(shortcut: StaticShortcut) = appShortcutManager.excludeShortcut(shortcut)
    fun setAppShortcutNickname(shortcut: StaticShortcut, nickname: String?) =
            appShortcutManager.setShortcutNickname(shortcut, nickname)
    fun getAppShortcutNickname(shortcutId: String): String? =
            appShortcutManager.getShortcutNickname(shortcutId)
    fun removeExcludedAppShortcut(shortcut: StaticShortcut) =
            appShortcutManager.removeExcludedShortcut(shortcut)

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

    fun setShowWallpaperBackground(showWallpaper: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            // If trying to enable, check if files permission is granted
            if (showWallpaper && !hasFilePermission()) {
                // Don't enable if files permission is not granted
                // The caller should request permission first
                return@launch
            }

            // Update user preference (this tracks explicit user choice)
            userPreferences.setShowWallpaperBackground(showWallpaper)
            showWallpaperBackground = showWallpaper
            _uiState.update { it.copy(showWallpaperBackground = showWallpaper) }
        }
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

    fun refreshIconPacks() = iconPackHandler.refreshIconPacks()
    fun setIconPackPackage(packageName: String?) = iconPackHandler.setIconPackPackage(packageName)

    // Shortcuts
    fun setShortcutsEnabled(enabled: Boolean) = shortcutHandler.setShortcutsEnabled(enabled)
    fun setShortcutCode(target: SearchTarget, code: String) =
            shortcutHandler.setShortcutCode(target, code)
    fun setShortcutEnabled(target: SearchTarget, enabled: Boolean) =
            shortcutHandler.setShortcutEnabled(target, enabled)
    fun getShortcutCode(target: SearchTarget): String = shortcutHandler.getShortcutCode(target)
    fun isShortcutEnabled(target: SearchTarget): Boolean = shortcutHandler.isShortcutEnabled(target)
    fun areShortcutsEnabled(): Boolean = true

    // Sections
    fun setSectionEnabled(section: SearchSection, enabled: Boolean) =
            sectionManager.setSectionEnabled(section, enabled)
    fun canEnableSection(section: SearchSection): Boolean = sectionManager.canEnableSection(section)

    // Messaging & Feature delegates
    fun setMessagingApp(app: MessagingApp) = messagingHandler.setMessagingApp(app)
    fun acknowledgeReleaseNotes() =
            releaseNotesHandler.acknowledgeReleaseNotes(_uiState.value.releaseNotesVersionName)
    fun requestDirectSearch(query: String) = directSearchHandler.requestDirectSearch(query)

    fun onSearchEngineOnboardingDismissed() {
        _uiState.update { it.copy(showSearchEngineOnboarding = false) }
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
    fun onPhoneNumberSelected(phoneNumber: String, rememberChoice: Boolean) =
            contactActionHandler.onPhoneNumberSelected(phoneNumber, rememberChoice)
    fun dismissPhoneNumberSelection() = contactActionHandler.dismissPhoneNumberSelection()
    fun dismissDirectDialChoice() = contactActionHandler.dismissDirectDialChoice()
    fun handleContactMethod(contactInfo: ContactInfo, method: ContactMethod) =
            contactActionHandler.handleContactMethod(contactInfo, method)

    fun getEnabledSearchTargets(): List<SearchTarget> =
            searchEngineManager.getEnabledSearchTargets()
    fun setSearchTargetEnabled(target: SearchTarget, enabled: Boolean) =
            searchEngineManager.setSearchTargetEnabled(target, enabled)
    fun reorderSearchTargets(newOrder: List<SearchTarget>) =
            searchEngineManager.reorderSearchTargets(newOrder)
    fun setSearchEngineCompactMode(enabled: Boolean) =
            searchEngineManager.setSearchEngineCompactMode(enabled)

    fun setFileTypeEnabled(fileType: FileType, enabled: Boolean) {
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

    fun setOneHandedMode(enabled: Boolean) {
        updateBooleanPreference(
                value = enabled,
                preferenceSetter = userPreferences::setOneHandedMode,
                stateUpdater = {
                    oneHandedMode = it
                    updateUiState { state -> state.copy(oneHandedMode = it) }
                }
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

            _uiState.update {
                it.copy(
                        hasGeminiApiKey = hasGemini,
                        geminiApiKeyLast4 = apiKey?.trim()?.takeLast(4)
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

    /** Computes the overall screen visibility state based on current data and permissions. */
    private fun computeScreenVisibilityState(state: SearchUiState): ScreenVisibilityState {
        return when {
            state.isInitializing -> ScreenVisibilityState.Initializing
            state.isLoading -> ScreenVisibilityState.Loading
            state.errorMessage != null ->
                    ScreenVisibilityState.Error(state.errorMessage, canRetry = true)
            !state.hasUsagePermission -> ScreenVisibilityState.NoPermissions
            state.query.isBlank() && state.recentApps.isEmpty() && state.pinnedApps.isEmpty() ->
                    ScreenVisibilityState.Empty
            else -> ScreenVisibilityState.Content
        }
    }

    /** Computes the apps section visibility state. */
    private fun computeAppsSectionVisibility(state: SearchUiState): AppsSectionVisibility {
        val sectionEnabled = SearchSection.APPS !in state.disabledSections

        return when {
            !sectionEnabled -> AppsSectionVisibility.Hidden
            state.isInitializing || state.isLoading -> AppsSectionVisibility.Loading
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
            !sectionEnabled -> AppShortcutsSectionVisibility.Hidden
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
            !sectionEnabled -> ContactsSectionVisibility.Hidden
            !state.hasContactPermission -> ContactsSectionVisibility.NoPermission
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
            !sectionEnabled -> FilesSectionVisibility.Hidden
            !state.hasFilePermission -> FilesSectionVisibility.NoPermission
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
            !sectionEnabled -> SettingsSectionVisibility.Hidden
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
    private fun computeSearchEnginesVisibility(state: SearchUiState): SearchEnginesVisibility {
        return when {
            state.detectedShortcutTarget != null ->
                    SearchEnginesVisibility.ShortcutDetected(state.detectedShortcutTarget)
            state.isSearchEngineCompactMode -> SearchEnginesVisibility.Compact
            else -> SearchEnginesVisibility.Hidden
        }
    }

    /**
     * Updates all visibility states in the UI state. Call this whenever the underlying data changes
     * that could affect visibility.
     */
    private fun updateVisibilityStates() {
        _uiState.update { currentState ->
            currentState.copy(
                    screenState = computeScreenVisibilityState(currentState),
                    appsSectionState = computeAppsSectionVisibility(currentState),
                    appShortcutsSectionState = computeAppShortcutsSectionVisibility(currentState),
                    contactsSectionState = computeContactsSectionVisibility(currentState),
                    filesSectionState = computeFilesSectionVisibility(currentState),
                    settingsSectionState = computeSettingsSectionVisibility(currentState),
                    searchEnginesState = computeSearchEnginesVisibility(currentState)
            )
        }
    }

    companion object {
        private const val GRID_ITEM_COUNT = 10
        private const val SECONDARY_SEARCH_DEBOUNCE_MS = 150L
    }

    private fun refreshDerivedState(lastUpdated: Long? = null, isLoading: Boolean? = null) {
        // Refresh nicknames cache to ensure we have the latest data
        appSearchManager.refreshNicknames()

        val apps = appSearchManager.cachedApps
        val visibleAppList = appSearchManager.availableApps()

        // Cache these to avoid multiple SharedPreferences reads
        val suggestionHiddenPackages = userPreferences.getSuggestionHiddenPackages()
        val resultHiddenPackages = userPreferences.getResultHiddenPackages()
        val pinnedPackages = userPreferences.getPinnedPackages()

        val pinnedAppsForSuggestions = appSearchManager.computePinnedApps(suggestionHiddenPackages)
        val pinnedAppsForResults = appSearchManager.computePinnedApps(resultHiddenPackages)
        val recentsSource = visibleAppList.filterNot { pinnedPackages.contains(it.packageName) }
        val recents = repository.extractRecentlyOpenedApps(recentsSource, GRID_ITEM_COUNT)
        val query = _uiState.value.query
        val trimmedQuery = query.trim()
        val packageNames = apps.map { it.packageName }.toSet()
        val isWhatsAppInstalled = packageNames.contains(PackageConstants.WHATSAPP_PACKAGE)
        val isTelegramInstalled = packageNames.contains(PackageConstants.TELEGRAM_PACKAGE)
        val resolvedMessagingApp =
                messagingHandler.updateMessagingAvailability(
                        whatsappInstalled = isWhatsAppInstalled,
                        telegramInstalled = isTelegramInstalled,
                        updateState = false
                )

        // Always update the searchable apps cache regardless of query state
        // Include both pinned and non-pinned apps in search, let ranking determine order
        val nonPinnedApps = appSearchManager.searchSourceApps()
        val allSearchableApps = (pinnedAppsForResults + nonPinnedApps).distinctBy { it.packageName }
        cachedAllSearchableApps = allSearchableApps

        val searchResults =
                if (trimmedQuery.isBlank()) {
                    emptyList()
                } else {
                    appSearchManager.deriveMatches(trimmedQuery, allSearchableApps)
                }
        val suggestionHiddenAppList =
                apps.filter { suggestionHiddenPackages.contains(it.packageName) }.sortedBy {
                    it.appName.lowercase(Locale.getDefault())
                }
        val resultHiddenAppList =
                apps.filter { resultHiddenPackages.contains(it.packageName) }.sortedBy {
                    it.appName.lowercase(Locale.getDefault())
                }

        _uiState.update { state ->
            state.copy(
                    recentApps = recents,
                    searchResults = searchResults,
                    pinnedApps = pinnedAppsForSuggestions,
                    suggestionExcludedApps = suggestionHiddenAppList,
                    resultExcludedApps = resultHiddenAppList,
                    indexedAppCount = visibleAppList.size,
                    cacheLastUpdatedMillis = lastUpdated ?: state.cacheLastUpdatedMillis,
                    isLoading = isLoading ?: state.isLoading,
                    messagingApp = resolvedMessagingApp,
                    isWhatsAppInstalled = isWhatsAppInstalled,
                    isTelegramInstalled = isTelegramInstalled,
                    nicknameUpdateVersion = state.nicknameUpdateVersion + 1
            )
        }

        iconPackHandler.prefetchVisibleAppIcons(
                pinnedApps = pinnedAppsForSuggestions,
                recents = recents,
                searchResults = searchResults
        )
    }

    fun handleOptionalPermissionChange() {
        val optionalChanged = refreshOptionalPermissions()
        if (optionalChanged && _uiState.value.query.isNotBlank()) {
            secondarySearchOrchestrator.performSecondarySearches(_uiState.value.query)
        }
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
        val previousState = _uiState.value
        val changed =
                previousState.hasContactPermission != hasContacts ||
                        previousState.hasFilePermission != hasFiles ||
                        previousState.hasCallPermission != hasCall

        if (changed) {

            // Handle wallpaper background based on files permission
            val userPrefValue = userPreferences.shouldShowWallpaperBackground()
            val shouldUpdateWallpaper =
                    when {
                        // If files permission is revoked, disable wallpaper (but keep user
                        // preference unchanged)
                        !hasFiles && showWallpaperBackground -> {
                            showWallpaperBackground = false
                            true
                        }
                        // If files permission is granted and user preference says enabled, enable
                        // wallpaper
                        hasFiles && !showWallpaperBackground && userPrefValue -> {
                            showWallpaperBackground = true
                            true
                        }
                        // If files permission is granted but user explicitly disabled it, keep it
                        // disabled
                        hasFiles && !showWallpaperBackground && !userPrefValue -> {
                            // User explicitly disabled it, keep it disabled
                            false
                        }
                        // If files permission is granted and wallpaper is enabled, keep it enabled
                        hasFiles && showWallpaperBackground -> {
                            false // No change needed
                        }
                        else -> false
                    }

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
                        directDialEnabled = this@SearchViewModel.directDialEnabled,
                        contactResults = if (hasContacts) state.contactResults else emptyList(),
                        fileResults = if (hasFiles) state.fileResults else emptyList(),
                        showWallpaperBackground =
                                if (shouldUpdateWallpaper) showWallpaperBackground
                                else state.showWallpaperBackground
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

    fun onDirectDialChoiceSelected(option: DirectDialOption, rememberChoice: Boolean) {
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
    fun getLastShownPhoneNumber(contactId: Long): String? {
        return userPreferences.getLastShownPhoneNumber(contactId)
    }

    fun setLastShownPhoneNumber(contactId: Long, phoneNumber: String) {
        userPreferences.setLastShownPhoneNumber(contactId, phoneNumber)
    }

    // Usage permission banner management
    fun resetUsagePermissionBannerSessionDismissed() {
        userPreferences.resetUsagePermissionBannerSessionDismissed()
        _uiState.update {
            it.copy(
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner()
            )
        }
    }

    fun incrementUsagePermissionBannerDismissCount() {
        userPreferences.incrementUsagePermissionBannerDismissCount()
        _uiState.update {
            it.copy(
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner()
            )
        }
    }

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) {
        userPreferences.setUsagePermissionBannerSessionDismissed(dismissed)
        _uiState.update {
            it.copy(
                    shouldShowUsagePermissionBanner =
                            userPreferences.shouldShowUsagePermissionBanner()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
