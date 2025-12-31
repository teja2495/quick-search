package com.tk.quicksearch.search.core

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.quicksearch.R
import com.tk.quicksearch.data.AppUsageRepository
import com.tk.quicksearch.data.ContactRepository
import com.tk.quicksearch.data.FileSearchRepository
import com.tk.quicksearch.data.SettingsShortcutRepository
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.ContactMethod
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.model.matches
import com.tk.quicksearch.permissions.PermissionRequestHandler
import com.tk.quicksearch.search.apps.AppManagementHandler
import com.tk.quicksearch.search.apps.AppSearchHandler
import com.tk.quicksearch.search.contacts.ContactActionHandler
import com.tk.quicksearch.search.contacts.ContactManagementHandler
import com.tk.quicksearch.search.contacts.MessagingHandler
import com.tk.quicksearch.search.core.CalculatorHandler
import com.tk.quicksearch.search.core.PermissionManager
import com.tk.quicksearch.search.core.SearchOperations
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.files.FileManagementHandler
import com.tk.quicksearch.search.handlers.IconPackHandler
import com.tk.quicksearch.search.handlers.NavigationHandler
import com.tk.quicksearch.search.handlers.PinningHandler
import com.tk.quicksearch.search.handlers.ReleaseNotesHandler
import com.tk.quicksearch.search.handlers.ShortcutHandler
import com.tk.quicksearch.search.searchengines.DirectSearchHandler
import com.tk.quicksearch.search.searchengines.SearchEngineManager
import com.tk.quicksearch.search.searchengines.SecondarySearchOrchestrator
import com.tk.quicksearch.search.searchengines.WebSuggestionHandler
import com.tk.quicksearch.search.searchengines.getDisplayNameResId
import com.tk.quicksearch.search.settings.SettingsManagementHandler
import com.tk.quicksearch.search.settings.SettingsSearchHandler
import com.tk.quicksearch.util.CalculatorUtils // Keep if needed for static check, but likely moved
import com.tk.quicksearch.util.FileUtils
import com.tk.quicksearch.util.SearchRankingUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppUsageRepository(application.applicationContext)
    private val contactRepository = ContactRepository(application.applicationContext)
    private val fileRepository = FileSearchRepository(application.applicationContext)
    private val settingsShortcutRepository = SettingsShortcutRepository(application.applicationContext)
    private val userPreferences = UserAppPreferences(application.applicationContext)
    private val permissionManager = PermissionManager(contactRepository, fileRepository, userPreferences)
    private val searchOperations = SearchOperations(contactRepository, fileRepository)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private fun updateUiState(updater: (SearchUiState) -> SearchUiState) {
        _uiState.update(updater)
    }


    // Management handlers
    val appManager = AppManagementHandler(userPreferences, viewModelScope, this::refreshDerivedState)
    val contactManager = ContactManagementHandler(userPreferences, viewModelScope, this::refreshDerivedState, this::updateUiState)
    val fileManager = FileManagementHandler(userPreferences, viewModelScope, this::refreshDerivedState, this::updateUiState)
    val settingsManager = SettingsManagementHandler(userPreferences, viewModelScope, this::refreshDerivedState, this::updateUiState)
    val searchEngineManager = SearchEngineManager(userPreferences, viewModelScope, this::updateUiState)
    val sectionManager = SectionManager(userPreferences, permissionManager, viewModelScope, this::updateUiState)
    private val uiStateManager = UiStateManager(repository, userPreferences, this::updateUiState)
    val iconPackHandler = IconPackHandler(application, userPreferences, viewModelScope, this::updateUiState)

    // New Handlers
    val messagingHandler = MessagingHandler(application, userPreferences, this::updateUiState)
    val releaseNotesHandler = ReleaseNotesHandler(application, userPreferences, this::updateUiState)
    
    // Feature handlers (extracted)
    private val pinningHandler = PinningHandler(
        scope = viewModelScope,
        permissionManager = permissionManager,
        contactRepository = contactRepository,
        fileRepository = fileRepository,
        userPreferences = userPreferences,
        uiStateUpdater = this::updateUiState
    )

    val webSuggestionHandler = WebSuggestionHandler(
        scope = viewModelScope,
        userPreferences = userPreferences,
        uiStateUpdater = this::updateUiState
    )

    val calculatorHandler = CalculatorHandler(
        scope = viewModelScope,
        userPreferences = userPreferences,
        uiStateUpdater = this::updateUiState
    )
    
    val appSearchHandler = AppSearchHandler(
        context = application.applicationContext,
        repository = repository,
        userPreferences = userPreferences,
        scope = viewModelScope,
        onAppsUpdated = { this.refreshDerivedState() },
        onLoadingStateChanged = { isLoading, error -> 
            _uiState.update { it.copy(isLoading = isLoading, errorMessage = error) }
        }
    )

    val settingsSearchHandler = SettingsSearchHandler(
        context = application.applicationContext,
        repository = settingsShortcutRepository,
        userPreferences = userPreferences,
        scope = viewModelScope
    )
    
    val directSearchHandler = DirectSearchHandler(
        context = application.applicationContext,
        userPreferences = userPreferences,
        scope = viewModelScope
    )

    val shortcutHandler = ShortcutHandler(
        userPreferences = userPreferences,
        scope = viewModelScope,
        uiStateUpdater = this::updateUiState,
        directSearchHandler = directSearchHandler
    )
    
    val navigationHandler = NavigationHandler(
        application = application,
        userPreferences = userPreferences,
        settingsSearchHandler = settingsSearchHandler,
        onRequestDirectSearch = { query -> directSearchHandler.requestDirectSearch(query) },
        onClearQuery = this::clearQuery
    )
    
    private val unifiedSearchHandler = UnifiedSearchHandler(
        contactRepository = contactRepository,
        fileRepository = fileRepository,
        userPreferences = userPreferences,
        settingsSearchHandler = settingsSearchHandler,
        searchOperations = searchOperations
    )

    private val secondarySearchOrchestrator = SecondarySearchOrchestrator(
        scope = viewModelScope,
        unifiedSearchHandler = unifiedSearchHandler,
        webSuggestionHandler = webSuggestionHandler,
        sectionManager = sectionManager,
        uiStateUpdater = this::updateUiState,
        currentStateProvider = { _uiState.value }
    )

    private var enabledFileTypes: Set<FileType> = userPreferences.getEnabledFileTypes()
    private var keyboardAlignedLayout: Boolean = userPreferences.isKeyboardAlignedLayout()
    private var messagingApp: MessagingApp = userPreferences.getMessagingApp()
    private var directDialEnabled: Boolean = userPreferences.isDirectDialEnabled()
    private var hasSeenDirectDialChoice: Boolean = userPreferences.hasSeenDirectDialChoice()
    private var showWallpaperBackground: Boolean = run {
        val prefValue = userPreferences.shouldShowWallpaperBackground()
        val hasFilesPermission = permissionManager.hasFilePermission()
        if (hasFilesPermission) prefValue else false
    }
    private var clearQueryAfterSearchEngine: Boolean = userPreferences.shouldClearQueryAfterSearchEngine()
    private var showAllResults: Boolean = userPreferences.shouldShowAllResults()
    private var sortAppsByUsageEnabled: Boolean = userPreferences.shouldSortAppsByUsage()
    private var amazonDomain: String? = userPreferences.getAmazonDomain()
    private var searchJob: Job? = null
    private var queryVersion: Long = 0L

    val contactActionHandler = ContactActionHandler(
        context = application,
        userPreferences = userPreferences,
        messagingApp = messagingApp,
        directDialEnabled = directDialEnabled,
        hasSeenDirectDialChoice = hasSeenDirectDialChoice,
        clearQueryAfterSearchEngine = clearQueryAfterSearchEngine,
        getCurrentState = { _uiState.value },
        uiStateUpdater = { update -> _uiState.update(update) },
        clearQuery = this::clearQuery
    )

    init {
        // Initialize handlers logic
        
        // Listen to Direct Search State
        viewModelScope.launch {
            directSearchHandler.directSearchState.collect { dsState ->
                 _uiState.update { it.copy(DirectSearchState = dsState) }
            }
        }
        
        // Load cached apps synchronously for instant UI display
        val cachedAppsList = runCatching {
            repository.loadCachedApps()
        }.getOrNull()
        
        // Get initial shortcut state
        val shortcutsState = shortcutHandler.getInitialState()
        
        if (cachedAppsList != null && cachedAppsList.isNotEmpty()) {
            // Set cached apps immediately in handler
            appSearchHandler.initCache(cachedAppsList)
            val lastUpdated = repository.cacheLastUpdatedMillis()
            val packageNames = cachedAppsList.map { it.packageName }.toSet()
            val isWhatsAppInstalled = packageNames.contains(WHATSAPP_PACKAGE)
            val isTelegramInstalled = packageNames.contains(TELEGRAM_PACKAGE)
            val resolvedMessagingApp = messagingHandler.updateMessagingAvailability(
                whatsappInstalled = isWhatsAppInstalled,
                telegramInstalled = isTelegramInstalled,
                updateState = false
            )
            
            // Initialize UI state with cached data - UI appears instantly
            _uiState.update {
                it.copy(
                    isLoading = false,
                    searchEngineOrder = searchEngineManager.searchEngineOrder,
                    disabledSearchEngines = searchEngineManager.disabledSearchEngines,
                    enabledFileTypes = enabledFileTypes,
                    keyboardAlignedLayout = keyboardAlignedLayout,
                    shortcutsEnabled = shortcutsState.shortcutsEnabled,
                    shortcutCodes = shortcutsState.shortcutCodes,
                    shortcutEnabled = shortcutsState.shortcutEnabled,
                    messagingApp = resolvedMessagingApp,
                    directDialEnabled = directDialEnabled,
                    isWhatsAppInstalled = isWhatsAppInstalled,
                    isTelegramInstalled = isTelegramInstalled,
                    showWallpaperBackground = showWallpaperBackground,
                    selectedIconPackPackage = iconPackHandler.selectedIconPackPackage,
                    availableIconPacks = iconPackHandler.availableIconPacks,
                    clearQueryAfterSearchEngine = clearQueryAfterSearchEngine,
                    showAllResults = showAllResults,
                    sortAppsByUsageEnabled = sortAppsByUsageEnabled,
                    sectionOrder = sectionManager.sectionOrder,
                    disabledSections = sectionManager.disabledSections,
                    searchEngineSectionEnabled = searchEngineManager.searchEngineSectionEnabled,
                    amazonDomain = amazonDomain,
                    webSuggestionsEnabled = webSuggestionHandler.isEnabled,
                    calculatorEnabled = calculatorHandler.isEnabled,
                    hasGeminiApiKey = !directSearchHandler.getGeminiApiKey().isNullOrBlank(),
                    geminiApiKeyLast4 = directSearchHandler.getGeminiApiKey()?.takeLast(4),
                    personalContext = directSearchHandler.getPersonalContext(),
                    cacheLastUpdatedMillis = lastUpdated
                )
            }
            // Update derived state immediately with cached apps
            refreshDerivedState(lastUpdated = lastUpdated, isLoading = false)
        } else {
            // No cache - initialize with defaults
            // No cache - initialize with defaults
            val isWhatsAppInstalled = messagingHandler.isPackageInstalled(WHATSAPP_PACKAGE)
            val isTelegramInstalled = messagingHandler.isPackageInstalled(TELEGRAM_PACKAGE)
            val resolvedMessagingApp = messagingHandler.updateMessagingAvailability(
                whatsappInstalled = isWhatsAppInstalled,
                telegramInstalled = isTelegramInstalled,
                updateState = false
            )
            _uiState.update {
                it.copy(
                    searchEngineOrder = searchEngineManager.searchEngineOrder,
                    disabledSearchEngines = searchEngineManager.disabledSearchEngines,
                    enabledFileTypes = enabledFileTypes,
                    keyboardAlignedLayout = keyboardAlignedLayout,
                    shortcutsEnabled = shortcutsState.shortcutsEnabled,
                    shortcutCodes = shortcutsState.shortcutCodes,
                    shortcutEnabled = shortcutsState.shortcutEnabled,
                    messagingApp = resolvedMessagingApp,
                    directDialEnabled = directDialEnabled,
                    isWhatsAppInstalled = isWhatsAppInstalled,
                    isTelegramInstalled = isTelegramInstalled,
                    showWallpaperBackground = showWallpaperBackground,
                    selectedIconPackPackage = iconPackHandler.selectedIconPackPackage,
                    availableIconPacks = iconPackHandler.availableIconPacks,
                    clearQueryAfterSearchEngine = clearQueryAfterSearchEngine,
                    showAllResults = showAllResults,
                    sortAppsByUsageEnabled = sortAppsByUsageEnabled,
                    sectionOrder = sectionManager.sectionOrder,
                    disabledSections = sectionManager.disabledSections,
                    searchEngineSectionEnabled = searchEngineManager.searchEngineSectionEnabled,
                    amazonDomain = amazonDomain,
                    hasGeminiApiKey = !directSearchHandler.getGeminiApiKey().isNullOrBlank(),
                    geminiApiKeyLast4 = directSearchHandler.getGeminiApiKey()?.takeLast(4),
                    personalContext = directSearchHandler.getPersonalContext()
                )
            }
        }
        
        // Refresh permissions (fast operations)
        refreshUsageAccess()
        refreshOptionalPermissions()
        refreshOptionalPermissions()
        releaseNotesHandler.checkForReleaseNotes()
        
        // Load apps in background (refresh cache if needed)
        loadApps()
        loadSettingsShortcuts()
        iconPackHandler.refreshIconPacks()
        
        // Defer non-critical loads until after UI is visible
        viewModelScope.launch {
            kotlinx.coroutines.delay(100) // Small delay to let UI render first
            pinningHandler.loadPinnedContactsAndFiles()
            pinningHandler.loadExcludedContactsAndFiles()
        }
    }

    private fun loadApps() {
        appSearchHandler.loadApps()
    }

    private fun loadSettingsShortcuts() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsSearchHandler.loadShortcuts()
        }
    }

    private fun refreshSettingsState(updateResults: Boolean = true) {
        val currentResults = _uiState.value.settingResults
        val currentState = settingsSearchHandler.getSettingsState(
            query = _uiState.value.query,
            isSettingsSectionEnabled = SearchSection.SETTINGS !in sectionManager.disabledSections,
            currentResults = currentResults // Pass current results to avoid re-search if not needed? Actually logic handles it
        )

        _uiState.update { state ->
            state.copy(
                pinnedSettings = currentState.pinned,
                excludedSettings = currentState.excluded,
                settingResults = currentState.results
            )
        }
    }

    fun refreshUsageAccess() {
        _uiState.update { it.copy(hasUsagePermission = repository.hasUsageAccess()) }
    }

    fun refreshApps(showToast: Boolean = false) {
        appSearchHandler.refreshApps(showToast)
    }

    fun refreshContacts(showToast: Boolean = false) {
        // Contacts are queried directly from the system provider, so we don't need to refresh a cache
        // Instead, we can clear any current contact results to force a fresh query on next search
        _uiState.update { it.copy(contactResults = emptyList()) }
        // Show success toast only for user-triggered refreshes
        if (showToast) {
            android.widget.Toast.makeText(
                getApplication(),
                "Contacts refreshed successfully",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun refreshFiles(showToast: Boolean = false) {
        // Files are queried directly from MediaStore, so we don't need to refresh a cache
        // Instead, we can clear any current file results to force a fresh query on next search
        _uiState.update { it.copy(fileResults = emptyList()) }
        // Show success toast only for user-triggered refreshes
        if (showToast) {
            android.widget.Toast.makeText(
                getApplication(),
                "Files refreshed successfully",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Removed messaging logic (moved to MessagingHandler)

    // Removed isPackageInstalled (moved to MessagingHandler)

    // Removed Release Notes logic (moved to ReleaseNotesHandler)

    // Removed setMessagingApp (delegate in SearchViewModelExtensions if needed, or direct)

    fun setDirectDialEnabled(enabled: Boolean) {
        directDialEnabled = enabled
        hasSeenDirectDialChoice = true
        userPreferences.setDirectDialEnabled(enabled)
        userPreferences.setHasSeenDirectDialChoice(true)
        _uiState.update { it.copy(directDialEnabled = enabled) }
    }

    fun onQueryChange(newQuery: String) {
        val previousQuery = _uiState.value.query
        val trimmedQuery = newQuery.trim()
        val DirectSearchState = _uiState.value.DirectSearchState
        if (DirectSearchState.status != DirectSearchStatus.Idle && newQuery != previousQuery) {
            directSearchHandler.clearDirectSearchState()
        } else if (DirectSearchState.activeQuery != null && DirectSearchState.activeQuery != trimmedQuery) {
            directSearchHandler.clearDirectSearchState()
        }

        if (trimmedQuery.isBlank()) {
            appSearchHandler.setNoMatchPrefix(null)
            searchJob?.cancel()
            webSuggestionHandler.cancelSuggestions()
            _uiState.update { 
                it.copy(
                    query = "",
                    searchResults = emptyList(),
                    contactResults = emptyList(),
                    fileResults = emptyList(),
                    settingResults = emptyList(),
                    DirectSearchState = DirectSearchState(),
                    calculatorState = CalculatorState(),
                    webSuggestions = emptyList()
                ) 
            }
            return
        }

        // Check if query is a math expression (only if calculator is enabled)
        val calculatorResult = calculatorHandler.processQuery(trimmedQuery)

        // Check for shortcuts if enabled
        val shortcutMatch = shortcutHandler.detectShortcut(trimmedQuery)
        if (shortcutMatch != null) {
            val (queryWithoutShortcut, engine) = shortcutMatch
            // Show toast notification for shortcut trigger
            android.widget.Toast.makeText(
                getApplication(),
                getApplication<Application>().getString(engine.getDisplayNameResId()) + " search shortcut triggered",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            // Automatically perform search with the detected engine
            navigationHandler.openSearchUrl(queryWithoutShortcut.trim(), engine, clearQueryAfterSearchEngine)
            // Update query to remove shortcut but keep the remaining query
            if (queryWithoutShortcut.isBlank()) {
                clearQuery()
            } else {
                onQueryChange(queryWithoutShortcut)
            }
            return
        }

        val normalizedQuery = trimmedQuery.lowercase(Locale.getDefault())
        appSearchHandler.resetNoMatchPrefixIfNeeded(normalizedQuery)

        val shouldSkipSearch = appSearchHandler.shouldSkipDueToNoMatchPrefix(normalizedQuery)
        val matches = if (shouldSkipSearch) {
            emptyList()
        } else {
            appSearchHandler.deriveMatches(trimmedQuery, appSearchHandler.searchSourceApps()).also { results ->
                if (results.isEmpty()) {
                    appSearchHandler.setNoMatchPrefix(normalizedQuery)
                }
            }
        }

        // Clear web suggestions when query changes
        webSuggestionHandler.cancelSuggestions()
        _uiState.update { state ->
            state.copy(
                query = newQuery,
                searchResults = matches,
                calculatorState = calculatorResult,
                webSuggestions = emptyList()
            )
        }
        
        // Skip secondary searches if calculator result is shown
        if (calculatorResult.result == null) {
            secondarySearchOrchestrator.performSecondarySearches(newQuery)
        } else {
            // Clear search results when showing calculator
            _uiState.update { state ->
                state.copy(
                    contactResults = emptyList(),
                    fileResults = emptyList(),
                    settingResults = emptyList(),
                    webSuggestions = emptyList()
                )
            }
        }
    }

    // Removed detectShortcut (unused)





    fun clearQuery() {
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
    }

    // Navigation Delegates
    fun openUsageAccessSettings() = navigationHandler.openUsageAccessSettings()
    fun openAppSettings() = navigationHandler.openAppSettings()
    fun openAllFilesAccessSettings() = navigationHandler.openAllFilesAccessSettings()
    fun openFilesPermissionSettings() = navigationHandler.openFilesPermissionSettings()
    fun openContactPermissionSettings() = navigationHandler.openContactPermissionSettings()
    fun launchApp(appInfo: AppInfo) = navigationHandler.launchApp(appInfo)
    fun openAppInfo(appInfo: AppInfo) = navigationHandler.openAppInfo(appInfo)
    fun requestUninstall(appInfo: AppInfo) = navigationHandler.requestUninstall(appInfo)
    fun openSearchUrl(query: String, searchEngine: SearchEngine) = navigationHandler.openSearchUrl(query, searchEngine, clearQueryAfterSearchEngine)
    fun searchIconPacks() = navigationHandler.searchIconPacks(clearQueryAfterSearchEngine)
    fun openFile(deviceFile: DeviceFile) = navigationHandler.openFile(deviceFile)
    fun openContact(contactInfo: ContactInfo) = navigationHandler.openContact(contactInfo, clearQueryAfterSearchEngine)
    fun openEmail(email: String) = navigationHandler.openEmail(email)

    // App Management Delegates
    fun hideApp(appInfo: AppInfo) = appManager.hideApp(appInfo, _uiState.value.query.isNotBlank())
    fun unhideAppFromSuggestions(appInfo: AppInfo) = appManager.unhideAppFromSuggestions(appInfo)
    fun unhideAppFromResults(appInfo: AppInfo) = appManager.unhideAppFromResults(appInfo)
    fun clearAllHiddenApps() = appManager.clearAllHiddenApps()
    fun pinApp(appInfo: AppInfo) = appManager.pinApp(appInfo)
    fun unpinApp(appInfo: AppInfo) = appManager.unpinApp(appInfo)
    fun setAppNickname(appInfo: AppInfo, nickname: String?) = appManager.setAppNickname(appInfo, nickname)
    fun getAppNickname(packageName: String): String? = appManager.getAppNickname(packageName)
    fun clearCachedApps() = appSearchHandler.clearCachedApps()

    // Contact Management Delegates
    fun pinContact(contactInfo: ContactInfo) = contactManager.pinContact(contactInfo)
    fun unpinContact(contactInfo: ContactInfo) = contactManager.unpinContact(contactInfo)
    fun excludeContact(contactInfo: ContactInfo) = contactManager.excludeContact(contactInfo)
    fun removeExcludedContact(contactInfo: ContactInfo) = contactManager.removeExcludedContact(contactInfo)
    fun clearAllExcludedContacts() = contactManager.clearAllExcludedContacts()
    fun setContactNickname(contactInfo: ContactInfo, nickname: String?) = contactManager.setContactNickname(contactInfo, nickname)
    fun getContactNickname(contactId: Long): String? = contactManager.getContactNickname(contactId)

    // File Management Delegates
    fun pinFile(deviceFile: DeviceFile) = fileManager.pinFile(deviceFile)
    fun unpinFile(deviceFile: DeviceFile) = fileManager.unpinFile(deviceFile)
    fun excludeFile(deviceFile: DeviceFile) = fileManager.excludeFile(deviceFile)
    fun excludeFileExtension(deviceFile: DeviceFile) = fileManager.excludeFileExtension(deviceFile)
    fun removeExcludedFileExtension(extension: String) = fileManager.removeExcludedFileExtension(extension)
    fun removeExcludedFile(deviceFile: DeviceFile) = fileManager.removeExcludedFile(deviceFile)
    fun clearAllExcludedFiles() = fileManager.clearAllExcludedFiles()
    fun setFileNickname(deviceFile: DeviceFile, nickname: String?) = fileManager.setFileNickname(deviceFile, nickname)
    fun getFileNickname(uri: String): String? = fileManager.getFileNickname(uri)

    // Settings Management Delegates
    fun pinSetting(setting: SettingShortcut) = settingsManager.pinSetting(setting)
    fun unpinSetting(setting: SettingShortcut) = settingsManager.unpinSetting(setting)
    fun excludeSetting(setting: SettingShortcut) = settingsManager.excludeSetting(setting)
    fun setSettingNickname(setting: SettingShortcut, nickname: String?) = settingsManager.setSettingNickname(setting, nickname)
    fun getSettingNickname(id: String): String? = settingsManager.getSettingNickname(id)
    fun removeExcludedSetting(setting: SettingShortcut) = settingsManager.removeExcludedSetting(setting)
    fun clearAllExcludedSettings() = settingsManager.clearAllExcludedSettings()
    fun openSetting(setting: SettingShortcut) = settingsSearchHandler.openSetting(setting)

    // Global Actions
    fun clearAllExclusions() {
        contactManager.clearAllExcludedContacts()
        fileManager.clearAllExcludedFiles()
        appManager.clearAllHiddenApps()
        settingsManager.clearAllExcludedSettings()
    }

    fun setShowWallpaperBackground(showWallpaper: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            // If trying to enable, check if files permission is granted
            if (showWallpaper && !permissionManager.hasFilePermission()) {
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

    fun refreshIconPacks() = iconPackHandler.refreshIconPacks()
    fun setIconPackPackage(packageName: String?) = iconPackHandler.setIconPackPackage(packageName)
    fun setWebSuggestionsEnabled(enabled: Boolean) = webSuggestionHandler.setEnabled(enabled)
    fun setCalculatorEnabled(enabled: Boolean) = calculatorHandler.setEnabled(enabled)
    
    // Shortcuts
    fun setShortcutsEnabled(enabled: Boolean) = shortcutHandler.setShortcutsEnabled(enabled)
    fun setShortcutCode(engine: SearchEngine, code: String) = shortcutHandler.setShortcutCode(engine, code)
    fun setShortcutEnabled(engine: SearchEngine, enabled: Boolean) = shortcutHandler.setShortcutEnabled(engine, enabled)
    fun getShortcutCode(engine: SearchEngine): String = shortcutHandler.getShortcutCode(engine)
    fun isShortcutEnabled(engine: SearchEngine): Boolean = shortcutHandler.isShortcutEnabled(engine)
    fun areShortcutsEnabled(): Boolean = shortcutHandler.shortcutsEnabled
    
    // Sections
    fun reorderSections(newOrder: List<SearchSection>) = sectionManager.reorderSections(newOrder)
    fun setSectionEnabled(section: SearchSection, enabled: Boolean) = sectionManager.setSectionEnabled(section, enabled)
    fun canEnableSection(section: SearchSection): Boolean = sectionManager.canEnableSection(section)
    
    // Messaging & Feature delegates
    fun setMessagingApp(app: MessagingApp) = messagingHandler.setMessagingApp(app)
    fun acknowledgeReleaseNotes() = releaseNotesHandler.acknowledgeReleaseNotes(_uiState.value.releaseNotesVersionName)
    fun requestDirectSearch(query: String) = directSearchHandler.requestDirectSearch(query)
    fun retryDirectSearch() = directSearchHandler.retryDirectSearch(_uiState.value.query)
    
    // Contact Actions
    fun callContact(contactInfo: ContactInfo) = contactActionHandler.callContact(contactInfo)
    fun smsContact(contactInfo: ContactInfo) = contactActionHandler.smsContact(contactInfo)
    fun onPhoneNumberSelected(phoneNumber: String, rememberChoice: Boolean) = contactActionHandler.onPhoneNumberSelected(phoneNumber, rememberChoice)
    fun dismissPhoneNumberSelection() = contactActionHandler.dismissPhoneNumberSelection()
    fun dismissDirectDialChoice() = contactActionHandler.dismissDirectDialChoice()
    fun handleContactMethod(contactInfo: ContactInfo, method: ContactMethod) = contactActionHandler.handleContactMethod(contactInfo, method)

    fun setClearQueryAfterSearchEngine(clearQuery: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setClearQueryAfterSearchEngine(clearQuery)
            clearQueryAfterSearchEngine = clearQuery
            _uiState.update {
                it.copy(clearQueryAfterSearchEngine = clearQuery)
            }
        }
    }

    fun setShowAllResults(showAllResults: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setShowAllResults(showAllResults)
            this@SearchViewModel.showAllResults = showAllResults
            _uiState.update {
                it.copy(showAllResults = showAllResults)
            }
        }
    }

    fun setSortAppsByUsageEnabled(sortAppsByUsageEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setSortAppsByUsage(sortAppsByUsageEnabled)
            appSearchHandler.setSortAppsByUsage(sortAppsByUsageEnabled)
            _uiState.update {
                it.copy(sortAppsByUsageEnabled = sortAppsByUsageEnabled)
            }
        }
    }

    fun getEnabledSearchEngines(): List<SearchEngine> = searchEngineManager.getEnabledSearchEngines()
    fun setSearchEngineEnabled(engine: SearchEngine, enabled: Boolean) = searchEngineManager.setSearchEngineEnabled(engine, enabled)
    fun reorderSearchEngines(newOrder: List<SearchEngine>) = searchEngineManager.reorderSearchEngines(newOrder)
    fun setSearchEngineSectionEnabled(enabled: Boolean) = searchEngineManager.setSearchEngineSectionEnabled(enabled)

    fun setFileTypeEnabled(fileType: FileType, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = enabledFileTypes.toMutableSet()
            if (enabled) {
                updated.add(fileType)
            } else {
                updated.remove(fileType)
            }
            enabledFileTypes = updated
            userPreferences.setEnabledFileTypes(enabledFileTypes)
            _uiState.update { 
                it.copy(enabledFileTypes = enabledFileTypes)
            }
            // Re-run file search if there's an active query
            val query = _uiState.value.query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.performSecondarySearches(query)
            }
        }
    }

    fun setKeyboardAlignedLayout(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setKeyboardAlignedLayout(enabled)
            keyboardAlignedLayout = enabled
            _uiState.update { 
                it.copy(keyboardAlignedLayout = keyboardAlignedLayout)
            }
        }
    }

    fun setAmazonDomain(domain: String?) {
        amazonDomain = domain
        userPreferences.setAmazonDomain(domain)
        _uiState.update { state ->
            state.copy(amazonDomain = amazonDomain)
        }
    }

    fun setGeminiApiKey(apiKey: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            directSearchHandler.setGeminiApiKey(apiKey)
            
            val hasGemini = !apiKey.isNullOrBlank()
            searchEngineManager.updateSearchEnginesForGemini(hasGemini)
            
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
            _uiState.update {
                it.copy(personalContext = context?.trim().orEmpty())
            }
        }
    }

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
        private const val GRID_ITEM_COUNT = 10
        private const val SECONDARY_SEARCH_DEBOUNCE_MS = 150L
    }

    private fun refreshDerivedState(
        lastUpdated: Long? = null,
        isLoading: Boolean? = null
    ) {
        val apps = appSearchHandler.cachedApps
        val visibleAppList = appSearchHandler.availableApps()
        val pinnedAppsForSuggestions = appSearchHandler.computePinnedApps(userPreferences.getSuggestionHiddenPackages())
        val pinnedAppsForResults = appSearchHandler.computePinnedApps(userPreferences.getResultHiddenPackages())
        val recentsSource = visibleAppList.filterNot { userPreferences.getPinnedPackages().contains(it.packageName) }
        val recents = repository.extractRecentApps(recentsSource, GRID_ITEM_COUNT)
        val query = _uiState.value.query
        val trimmedQuery = query.trim()
        val packageNames = apps.map { it.packageName }.toSet()
        val isWhatsAppInstalled = packageNames.contains(WHATSAPP_PACKAGE)
        val isTelegramInstalled = packageNames.contains(TELEGRAM_PACKAGE)
        val resolvedMessagingApp = messagingHandler.updateMessagingAvailability(
            whatsappInstalled = isWhatsAppInstalled,
            telegramInstalled = isTelegramInstalled,
            updateState = false
        )
        val searchResults = if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            // Include both pinned and non-pinned apps in search, let ranking determine order
            val nonPinnedApps = appSearchHandler.searchSourceApps()
            val allSearchableApps = (pinnedAppsForResults + nonPinnedApps).distinctBy { it.packageName }
            appSearchHandler.deriveMatches(trimmedQuery, allSearchableApps)
        }
        val suggestionHiddenAppList = apps
            .filter { userPreferences.getSuggestionHiddenPackages().contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
        val resultHiddenAppList = apps
            .filter { userPreferences.getResultHiddenPackages().contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }

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
                isTelegramInstalled = isTelegramInstalled
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
        // Copy the suggestion text to the search bar
        onQueryChange(suggestion)
    }

    private fun refreshOptionalPermissions(): Boolean {
        val hasContacts = permissionManager.hasContactPermission()
        val hasFiles = permissionManager.hasFilePermission()
        val hasCall = com.tk.quicksearch.permissions.PermissionRequestHandler.checkCallPermission(getApplication())
        val previousState = _uiState.value
        val changed =
            previousState.hasContactPermission != hasContacts || 
            previousState.hasFilePermission != hasFiles ||
            previousState.hasCallPermission != hasCall

        if (changed) {
            
            // Handle wallpaper background based on files permission
            val userPrefValue = userPreferences.shouldShowWallpaperBackground()
            val shouldUpdateWallpaper = when {
                // If files permission is revoked, disable wallpaper (but keep user preference unchanged)
                !hasFiles && showWallpaperBackground -> {
                    showWallpaperBackground = false
                    true
                }
                // If files permission is granted and user preference says enabled, enable wallpaper
                hasFiles && !showWallpaperBackground && userPrefValue -> {
                    showWallpaperBackground = true
                    true
                }
                // If files permission is granted but user explicitly disabled it, keep it disabled
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
            
            _uiState.update { state ->
                state.copy(
                    hasContactPermission = hasContacts,
                    hasFilePermission = hasFiles,
                    hasCallPermission = hasCall,
                    contactResults = if (hasContacts) state.contactResults else emptyList(),
                    fileResults = if (hasFiles) state.fileResults else emptyList(),
                    disabledSections = sectionManager.disabledSections,
                    showWallpaperBackground = if (shouldUpdateWallpaper) showWallpaperBackground else state.showWallpaperBackground
                )
            }
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
            directDialEnabled = useDirectDial
        }
        hasSeenDirectDialChoice = true
    }



    fun onCallPermissionResult(isGranted: Boolean) {
        contactActionHandler.onCallPermissionResult(isGranted)
        // Refresh permission state after request result
        handleOptionalPermissionChange()
    }


    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
