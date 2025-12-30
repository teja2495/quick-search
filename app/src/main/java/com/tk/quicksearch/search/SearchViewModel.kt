package com.tk.quicksearch.search

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
import com.tk.quicksearch.search.*
import com.tk.quicksearch.util.CalculatorUtils
import com.tk.quicksearch.util.FileUtils
import com.tk.quicksearch.util.SearchRankingUtils
import com.tk.quicksearch.util.WebSuggestionsUtils
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

    // Management handlers
    private val appManager = AppManagementHandler(userPreferences, viewModelScope, this::refreshDerivedState)
    private val contactManager = ContactManagementHandler(userPreferences, viewModelScope, this::refreshDerivedState, _uiState::update)
    private val fileManager = FileManagementHandler(userPreferences, viewModelScope, this::refreshDerivedState, _uiState::update)
    private val settingsManager = SettingsManagementHandler(userPreferences, viewModelScope, this::refreshDerivedState, _uiState::update)
    private val searchEngineManager = SearchEngineManager(userPreferences, viewModelScope, _uiState::update)
    private val sectionManager = SectionManager(userPreferences, permissionManager, viewModelScope, _uiState::update)
    private val uiStateManager = UiStateManager(repository, userPreferences, _uiState::update)
    private val iconPackHandler = IconPackHandler(application, userPreferences, viewModelScope, _uiState::update)
    
    private val appSearchHandler = AppSearchHandler(
        context = application.applicationContext,
        repository = repository,
        userPreferences = userPreferences,
        scope = viewModelScope,
        onAppsUpdated = { this.refreshDerivedState() },
        onLoadingStateChanged = { isLoading, error -> 
            _uiState.update { it.copy(isLoading = isLoading, errorMessage = error) }
        }
    )

    private val settingsSearchHandler = SettingsSearchHandler(
        context = application.applicationContext,
        repository = settingsShortcutRepository,
        userPreferences = userPreferences,
        scope = viewModelScope
    )
    
    private val directSearchHandler = DirectSearchHandler(
        context = application.applicationContext,
        userPreferences = userPreferences,
        scope = viewModelScope
    )
    
    private val unifiedSearchHandler = UnifiedSearchHandler(
        contactRepository = contactRepository,
        fileRepository = fileRepository,
        userPreferences = userPreferences,
        settingsSearchHandler = settingsSearchHandler,
        searchOperations = searchOperations
    )

    private var enabledFileTypes: Set<FileType> = userPreferences.getEnabledFileTypes()
    private var keyboardAlignedLayout: Boolean = userPreferences.isKeyboardAlignedLayout()
    private var shortcutsEnabled: Boolean = run {
        val enabled = userPreferences.areShortcutsEnabled()
        if (!enabled) {
            // Remove the ability to disable all shortcuts; always keep them on
            userPreferences.setShortcutsEnabled(true)
        }
        true
    }
    private var shortcutCodes: Map<SearchEngine, String> = userPreferences.getAllShortcutCodes()
    private var shortcutEnabled: Map<SearchEngine, Boolean> = SearchEngine.values().associateWith {
        userPreferences.isShortcutEnabled(it)
    }
    private var messagingApp: MessagingApp = userPreferences.getMessagingApp()
    private var directDialEnabled: Boolean = userPreferences.isDirectDialEnabled()
    private var hasSeenDirectDialChoice: Boolean = userPreferences.hasSeenDirectDialChoice()
    private var showWallpaperBackground: Boolean = run {
        val prefValue = userPreferences.shouldShowWallpaperBackground()
        val hasFilesPermission = permissionManager.hasFilePermission()
        // If files permission is available, use user preference (defaults to true)
        // If files permission is not available, disable wallpaper (but keep user preference)
        if (hasFilesPermission) {
            // Enable by default if user hasn't explicitly disabled it
            prefValue
        } else {
            // Disable if permission is not available
            false
        }
    }
    private var clearQueryAfterSearchEngine: Boolean = userPreferences.shouldClearQueryAfterSearchEngine()
    private var showAllResults: Boolean = userPreferences.shouldShowAllResults()
    private var sortAppsByUsageEnabled: Boolean = userPreferences.shouldSortAppsByUsage()
    private var amazonDomain: String? = userPreferences.getAmazonDomain()
    private var webSuggestionsEnabled: Boolean = userPreferences.areWebSuggestionsEnabled()
    private var calculatorEnabled: Boolean = userPreferences.isCalculatorEnabled()
    private var searchJob: Job? = null

    // DirectSearchJob moved to DirectSearchHandler
    private var webSuggestionsJob: Job? = null
    private var queryVersion: Long = 0L

    private val contactActionHandler = ContactActionHandler(
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
        
        if (cachedAppsList != null && cachedAppsList.isNotEmpty()) {
            // Set cached apps immediately in handler
            appSearchHandler.initCache(cachedAppsList)
            val lastUpdated = repository.cacheLastUpdatedMillis()
            val packageNames = cachedAppsList.map { it.packageName }.toSet()
            val isWhatsAppInstalled = packageNames.contains(WHATSAPP_PACKAGE)
            val isTelegramInstalled = packageNames.contains(TELEGRAM_PACKAGE)
            val resolvedMessagingApp = updateMessagingAvailability(
                isWhatsAppInstalled = isWhatsAppInstalled,
                isTelegramInstalled = isTelegramInstalled,
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
                    shortcutsEnabled = shortcutsEnabled,
                    shortcutCodes = shortcutCodes,
                    shortcutEnabled = shortcutEnabled,
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
                    webSuggestionsEnabled = webSuggestionsEnabled,
                    calculatorEnabled = calculatorEnabled,
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
            val isWhatsAppInstalled = isPackageInstalled(WHATSAPP_PACKAGE)
            val isTelegramInstalled = isPackageInstalled(TELEGRAM_PACKAGE)
            val resolvedMessagingApp = updateMessagingAvailability(
                isWhatsAppInstalled = isWhatsAppInstalled,
                isTelegramInstalled = isTelegramInstalled,
                updateState = false
            )
            _uiState.update {
                it.copy(
                    searchEngineOrder = searchEngineManager.searchEngineOrder,
                    disabledSearchEngines = searchEngineManager.disabledSearchEngines,
                    enabledFileTypes = enabledFileTypes,
                    keyboardAlignedLayout = keyboardAlignedLayout,
                    shortcutsEnabled = shortcutsEnabled,
                    shortcutCodes = shortcutCodes,
                    shortcutEnabled = shortcutEnabled,
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
        checkForReleaseNotes()
        
        // Load apps in background (refresh cache if needed)
        loadApps()
        loadSettingsShortcuts()
        iconPackHandler.refreshIconPacks()
        
        // Defer non-critical loads until after UI is visible
        viewModelScope.launch {
            kotlinx.coroutines.delay(100) // Small delay to let UI render first
            loadPinnedContactsAndFiles()
            loadExcludedContactsAndFiles()
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

    private fun resolveMessagingApp(
        isWhatsAppInstalled: Boolean,
        isTelegramInstalled: Boolean
    ): MessagingApp {
        return when (messagingApp) {
            MessagingApp.WHATSAPP -> if (isWhatsAppInstalled) MessagingApp.WHATSAPP else MessagingApp.MESSAGES
            MessagingApp.TELEGRAM -> if (isTelegramInstalled) MessagingApp.TELEGRAM else MessagingApp.MESSAGES
            MessagingApp.MESSAGES -> MessagingApp.MESSAGES
        }
    }

    private fun updateMessagingAvailability(
        isWhatsAppInstalled: Boolean,
        isTelegramInstalled: Boolean,
        updateState: Boolean = true
    ): MessagingApp {
        val resolvedMessagingApp = resolveMessagingApp(isWhatsAppInstalled, isTelegramInstalled)
        if (resolvedMessagingApp != messagingApp) {
            messagingApp = resolvedMessagingApp
            userPreferences.setMessagingApp(resolvedMessagingApp)
        }

        if (updateState) {
            _uiState.update { state ->
                state.copy(
                    messagingApp = messagingApp,
                    isWhatsAppInstalled = isWhatsAppInstalled,
                    isTelegramInstalled = isTelegramInstalled
                )
            }
        }

        return resolvedMessagingApp
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        val packageManager = getApplication<Application>().packageManager
        return packageManager.getLaunchIntentForPackage(packageName) != null
    }

    private fun getCurrentVersionName(): String? {
        val app = getApplication<Application>()
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                app.packageManager.getPackageInfo(
                    app.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getPackageInfo(app.packageName, 0).versionName
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun checkForReleaseNotes() {
        val currentVersion = getCurrentVersionName() ?: return
        // Skip showing release notes on a fresh install; just record baseline
        if (userPreferences.isFirstLaunch()) {
            userPreferences.setLastSeenVersionName(currentVersion)
            return
        }

        val lastSeenVersion = userPreferences.getLastSeenVersionName()
        if (lastSeenVersion == null) {
            userPreferences.setLastSeenVersionName(currentVersion)
            return
        }

        if (lastSeenVersion != currentVersion) {
            _uiState.update {
                it.copy(
                    showReleaseNotesDialog = true,
                    releaseNotesVersionName = currentVersion
                )
            }
        }
    }

    fun acknowledgeReleaseNotes() {
        val versionToStore = _uiState.value.releaseNotesVersionName ?: getCurrentVersionName()
        if (versionToStore != null) {
            userPreferences.setLastSeenVersionName(versionToStore)
        }
        _uiState.update {
            it.copy(
                showReleaseNotesDialog = false,
                releaseNotesVersionName = versionToStore
            )
        }
    }

    fun setMessagingApp(app: MessagingApp) {
        messagingApp = app
        // Persist the user's explicit choice before resolving availability
        userPreferences.setMessagingApp(app)
        val whatsappInstalled = _uiState.value.isWhatsAppInstalled
        val telegramInstalled = _uiState.value.isTelegramInstalled
        updateMessagingAvailability(
            isWhatsAppInstalled = whatsappInstalled,
            isTelegramInstalled = telegramInstalled
        )
    }

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
            // Actually explicit clear method is better or just recreate handler state if needed.
            // But handler has reset logic.
            // Let's call appSearchHandler.setNoMatchPrefix(null) but I defined it as accepting String.
            // I should have made it nullable. Let's send empty string as specific logic? 
            // Checking AppSearchHandler code: `noMatchPrefix = prefix`. 
            // In AppSearchHandler: `resetNoMatchPrefixIfNeeded` sets it to null.
            // I'll assume setNoMatchPrefix("") won't match anything or I should use resetNoMatchPrefixIfNeeded("")
            // Actually, I should just set query to "" and reset handler state.
            // AppSearchHandler.refreshApps sets it to null.
            // But here we are just clearing query.
            // I'll call appSearchHandler.setNoMatchPrefix(null) if I allowed it. 
            // I defined `fun setNoMatchPrefix(prefix: String)`.
            // I'll assume passing a non-matching prefix or just ignoring it for blank query is fine as reset happens on next char.
            // Or better, let's fix AppSearchHandler to allow resetting or use internal logic.
            // Actually `resetNoMatchPrefixIfNeeded` does the job if query is blank!
            
            searchJob?.cancel()
            webSuggestionsJob?.cancel()
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
        val calculatorResult = if (calculatorEnabled && CalculatorUtils.isMathExpression(trimmedQuery)) {
            CalculatorUtils.evaluateExpression(trimmedQuery)?.let { result ->
                CalculatorState(result = result, expression = trimmedQuery)
            } ?: CalculatorState()
        } else {
            CalculatorState()
        }

        // Check for shortcuts if enabled
        if (shortcutsEnabled) {
            val shortcutMatch = detectShortcut(trimmedQuery)
            if (shortcutMatch != null) {
                val (queryWithoutShortcut, engine) = shortcutMatch
                // Show toast notification for shortcut trigger
                android.widget.Toast.makeText(
                    getApplication(),
                    getApplication<Application>().getString(engine.getDisplayNameResId()) + " search shortcut triggered",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                // Automatically perform search with the detected engine
                openSearchUrl(queryWithoutShortcut.trim(), engine)
                // Update query to remove shortcut but keep the remaining query
                if (queryWithoutShortcut.isBlank()) {
                    clearQuery()
                } else {
                    onQueryChange(queryWithoutShortcut)
                }
                return
            }
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
        webSuggestionsJob?.cancel()
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
            performSecondarySearches(newQuery)
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

    private fun detectShortcut(query: String): Pair<String, SearchEngine>? {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return null

        // Look for shortcut at the end of the query (e.g., "search query ggl")
        // Shortcut should be separated by space
        val words = trimmedQuery.split("\\s+".toRegex())
        if (words.size < 2) return null

        val lastWord = words.last().lowercase(Locale.getDefault())
        
        // Check each enabled search engine for matching shortcut
        for (engine in SearchEngine.values()) {
            if (engine == SearchEngine.DIRECT_SEARCH && directSearchHandler.getGeminiApiKey().isNullOrBlank()) continue
            if (!userPreferences.isShortcutEnabled(engine)) continue
            val shortcutCode = userPreferences.getShortcutCode(engine).lowercase(Locale.getDefault())
            if (lastWord == shortcutCode) {
                // Extract query without the shortcut
                val queryWithoutShortcut = words.dropLast(1).joinToString(" ")
                return Pair(queryWithoutShortcut, engine)
            }
        }
        
        return null
    }





    fun clearQuery() {
        onQueryChange("")
    }

    private fun loadPinnedContactsAndFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasContactsPermission = permissionManager.hasContactPermission()
            val hasFilesPermission = permissionManager.hasFilePermission()

            val pinnedContactIds = userPreferences.getPinnedContactIds()
            val excludedContactIds = userPreferences.getExcludedContactIds()
            val pinnedContacts = if (hasContactsPermission && pinnedContactIds.isNotEmpty()) {
                contactRepository.getContactsByIds(pinnedContactIds)
                    .filterNot { excludedContactIds.contains(it.contactId) }
            } else {
                emptyList()
            }

            val pinnedFileUris = userPreferences.getPinnedFileUris()
            val excludedFileUris = userPreferences.getExcludedFileUris()
            val pinnedFiles = if (hasFilesPermission && pinnedFileUris.isNotEmpty()) {
                fileRepository.getFilesByUris(pinnedFileUris)
                    .filterNot { excludedFileUris.contains(it.uri.toString()) }
            } else {
                emptyList()
            }

            _uiState.update { state ->
                state.copy(
                    pinnedContacts = pinnedContacts,
                    pinnedFiles = pinnedFiles
                )
            }
        }
    }

    private fun loadExcludedContactsAndFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasContactsPermission = permissionManager.hasContactPermission()
            val hasFilesPermission = permissionManager.hasFilePermission()

            val excludedContactIds = userPreferences.getExcludedContactIds()
            val excludedContacts = if (hasContactsPermission && excludedContactIds.isNotEmpty()) {
                contactRepository.getContactsByIds(excludedContactIds)
            } else {
                emptyList()
            }

            val excludedFileUris = userPreferences.getExcludedFileUris()
            val excludedFiles = if (hasFilesPermission && excludedFileUris.isNotEmpty()) {
                fileRepository.getFilesByUris(excludedFileUris)
            } else {
                emptyList()
            }

            _uiState.update { state ->
                state.copy(
                    excludedContacts = excludedContacts,
                    excludedFiles = excludedFiles,
                    excludedFileExtensions = userPreferences.getExcludedFileExtensions()
                )
            }
        }
    }

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
        loadPinnedContactsAndFiles()
        loadExcludedContactsAndFiles()
        refreshSettingsState()
    }

    fun openUsageAccessSettings() {
        IntentHelpers.openUsageAccessSettings(getApplication())
    }

    fun openAppSettings() {
        IntentHelpers.openAppSettings(getApplication())
    }

    fun openAllFilesAccessSettings() {
        IntentHelpers.openAllFilesAccessSettings(getApplication())
    }

    fun openFilesPermissionSettings() {
        val context: Application = getApplication()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            IntentHelpers.openAllFilesAccessSettings(context)
        } else {
            IntentHelpers.openAppSettings(context)
        }
    }

    fun openContactPermissionSettings() {
        IntentHelpers.openAppSettings(getApplication())
    }

    fun launchApp(appInfo: AppInfo) {
        IntentHelpers.launchApp(getApplication(), appInfo)
        clearQuery()
    }

    fun openAppInfo(appInfo: AppInfo) {
        IntentHelpers.openAppInfo(getApplication(), appInfo.packageName)
    }

    fun requestUninstall(appInfo: AppInfo) {
        IntentHelpers.requestUninstall(getApplication(), appInfo)
    }

    fun openSearchUrl(query: String, searchEngine: SearchEngine) {
        val trimmedQuery = query.trim()
        if (searchEngine == SearchEngine.DIRECT_SEARCH) {
            requestDirectSearch(trimmedQuery)
            return
        }
        val amazonDomain = if (searchEngine == SearchEngine.AMAZON) {
            userPreferences.getAmazonDomain()
        } else {
            null
        }
        IntentHelpers.openSearchUrl(getApplication(), trimmedQuery, searchEngine, amazonDomain)

        // Clear query after triggering search engine if enabled
        if (clearQueryAfterSearchEngine) {
            clearQuery()
        }
    }

    fun searchIconPacks() {
        val query = getApplication<Application>().getString(R.string.settings_icon_pack_search_query)
        openSearchUrl(query, SearchEngine.GOOGLE_PLAY)
    }

    fun clearCachedApps() {
        appSearchHandler.clearCachedApps()
    }

    fun pinContact(contactInfo: ContactInfo) = contactManager.pinContact(contactInfo)

    fun unpinContact(contactInfo: ContactInfo) = contactManager.unpinContact(contactInfo)

    fun excludeContact(contactInfo: ContactInfo) = contactManager.excludeContact(contactInfo)

    fun pinFile(deviceFile: DeviceFile) = fileManager.pinFile(deviceFile)

    fun unpinFile(deviceFile: DeviceFile) = fileManager.unpinFile(deviceFile)

    fun excludeFile(deviceFile: DeviceFile) = fileManager.excludeFile(deviceFile)

    fun excludeFileExtension(deviceFile: DeviceFile) = fileManager.excludeFileExtension(deviceFile)

    fun removeExcludedFileExtension(extension: String) = fileManager.removeExcludedFileExtension(extension)

    fun pinSetting(setting: SettingShortcut) = settingsManager.pinSetting(setting)

    fun unpinSetting(setting: SettingShortcut) = settingsManager.unpinSetting(setting)

    fun excludeSetting(setting: SettingShortcut) = settingsManager.excludeSetting(setting)

    fun setSettingNickname(setting: SettingShortcut, nickname: String?) = settingsManager.setSettingNickname(setting, nickname)

    fun getSettingNickname(id: String): String? = settingsManager.getSettingNickname(id)

    fun hideApp(appInfo: AppInfo) = appManager.hideApp(appInfo, _uiState.value.query.isNotBlank())

    fun unhideAppFromSuggestions(appInfo: AppInfo) = appManager.unhideAppFromSuggestions(appInfo)

    fun unhideAppFromResults(appInfo: AppInfo) = appManager.unhideAppFromResults(appInfo)

    fun removeExcludedContact(contactInfo: ContactInfo) = contactManager.removeExcludedContact(contactInfo)

    fun removeExcludedFile(deviceFile: DeviceFile) = fileManager.removeExcludedFile(deviceFile)

    fun removeExcludedSetting(setting: SettingShortcut) = settingsManager.removeExcludedSetting(setting)

    fun clearAllExcludedContacts() = contactManager.clearAllExcludedContacts()

    fun clearAllExcludedFiles() = fileManager.clearAllExcludedFiles()

    fun clearAllExcludedSettings() = settingsManager.clearAllExcludedSettings()

    fun clearAllHiddenApps() = appManager.clearAllHiddenApps()

    fun clearAllExclusions() {
        contactManager.clearAllExcludedContacts()
        fileManager.clearAllExcludedFiles()
        appManager.clearAllHiddenApps()
        settingsManager.clearAllExcludedSettings()
    }

    fun pinApp(appInfo: AppInfo) = appManager.pinApp(appInfo)

    fun unpinApp(appInfo: AppInfo) = appManager.unpinApp(appInfo)

    fun setAppNickname(appInfo: AppInfo, nickname: String?) = appManager.setAppNickname(appInfo, nickname)

    fun getAppNickname(packageName: String): String? = appManager.getAppNickname(packageName)

    fun setContactNickname(contactInfo: ContactInfo, nickname: String?) = contactManager.setContactNickname(contactInfo, nickname)

    fun getContactNickname(contactId: Long): String? = contactManager.getContactNickname(contactId)

    fun setFileNickname(deviceFile: DeviceFile, nickname: String?) = fileManager.setFileNickname(deviceFile, nickname)

    fun getFileNickname(uri: String): String? = fileManager.getFileNickname(uri)

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

    fun setWebSuggestionsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setWebSuggestionsEnabled(enabled)
            webSuggestionsEnabled = enabled

            // Update UI state
            _uiState.update { it.copy(webSuggestionsEnabled = enabled) }

            // If disabling web suggestions, clear any existing suggestions
            if (!enabled) {
                _uiState.update { it.copy(webSuggestions = emptyList()) }
            }
        }
    }

    fun setCalculatorEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setCalculatorEnabled(enabled)
            calculatorEnabled = enabled

            // Update UI state
            _uiState.update { it.copy(calculatorEnabled = enabled) }

            // If disabling calculator, clear any existing calculator result
            if (!enabled) {
                _uiState.update { it.copy(calculatorState = CalculatorState()) }
            }
        }
    }

    fun refreshIconPacks() = iconPackHandler.refreshIconPacks()

    fun setIconPackPackage(packageName: String?) = iconPackHandler.setIconPackPackage(packageName)

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
                performSecondarySearches(query)
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

    fun requestDirectSearch(query: String) {
        directSearchHandler.requestDirectSearch(query)
    }

    fun retryDirectSearch() {
        directSearchHandler.retryDirectSearch(_uiState.value.query)
    }

    fun setShortcutsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            // Keep shortcuts permanently enabled
            userPreferences.setShortcutsEnabled(true)
            shortcutsEnabled = true
            _uiState.update { 
                it.copy(shortcutsEnabled = shortcutsEnabled)
            }
        }
    }

    fun setShortcutCode(engine: SearchEngine, code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalizedCode = normalizeShortcutCodeInput(code)
            if (!isValidShortcutCode(normalizedCode)) {
                return@launch
            }
            userPreferences.setShortcutCode(engine, normalizedCode)
            shortcutCodes = shortcutCodes.toMutableMap().apply { put(engine, normalizedCode) }
            _uiState.update { 
                it.copy(shortcutCodes = shortcutCodes)
            }
        }
    }

    fun setShortcutEnabled(engine: SearchEngine, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setShortcutEnabled(engine, enabled)
            shortcutEnabled = shortcutEnabled.toMutableMap().apply { put(engine, enabled) }
            _uiState.update { 
                it.copy(shortcutEnabled = shortcutEnabled)
            }
        }
    }

    fun getShortcutCode(engine: SearchEngine): String {
        return shortcutCodes[engine] ?: userPreferences.getShortcutCode(engine)
    }

    fun isShortcutEnabled(engine: SearchEngine): Boolean {
        return shortcutEnabled[engine] ?: userPreferences.isShortcutEnabled(engine)
    }

    fun areShortcutsEnabled(): Boolean {
        return shortcutsEnabled
    }





    fun reorderSections(newOrder: List<SearchSection>) = sectionManager.reorderSections(newOrder)

    fun setSectionEnabled(section: SearchSection, enabled: Boolean) = sectionManager.setSectionEnabled(section, enabled)

    /**
     * Checks if a section can be enabled (i.e., has required permissions).
     */
    fun canEnableSection(section: SearchSection): Boolean = sectionManager.canEnableSection(section)





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
        val resolvedMessagingApp = updateMessagingAvailability(
            isWhatsAppInstalled = isWhatsAppInstalled,
            isTelegramInstalled = isTelegramInstalled,
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

        prefetchVisibleAppIcons(
            pinnedApps = pinnedAppsForSuggestions,
            recents = recents,
            searchResults = searchResults
        )
    }

    private fun prefetchVisibleAppIcons(
        pinnedApps: List<AppInfo>,
        recents: List<AppInfo>,
        searchResults: List<AppInfo>
    ) {
        val iconPack = iconPackHandler.getCurrentSelectedIconPackPackage() ?: return
        val packageNames = buildList {
            addAll(pinnedApps.map { it.packageName })
            addAll(recents.map { it.packageName })
            addAll(searchResults.take(GRID_ITEM_COUNT).map { it.packageName })
        }
        if (packageNames.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            prefetchAppIcons(
                context = getApplication(),
                packageNames = packageNames,
                iconPackPackage = iconPack,
                maxCount = 30
            )
        }
    }

    fun handleOptionalPermissionChange() {
        val optionalChanged = refreshOptionalPermissions()
        if (optionalChanged && _uiState.value.query.isNotBlank()) {
            performSecondarySearches(_uiState.value.query)
        }
    }

    private fun performSecondarySearches(query: String) {
        searchJob?.cancel()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank() || trimmedQuery.length == 1) {
            _uiState.update {
                it.copy(
                    contactResults = emptyList(),
                    fileResults = emptyList(),
                    settingResults = emptyList()
                )
            }
            return
        }

        val canSearchContacts = _uiState.value.hasContactPermission &&
            SearchSection.CONTACTS !in sectionManager.disabledSections
        val canSearchFiles = _uiState.value.hasFilePermission &&
            SearchSection.FILES !in sectionManager.disabledSections
        val canSearchSettings = SearchSection.SETTINGS !in sectionManager.disabledSections
            
        val currentVersion = ++queryVersion

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            // Debounce expensive contact/file queries during rapid typing
            delay(SECONDARY_SEARCH_DEBOUNCE_MS)
            if (currentVersion != queryVersion) return@launch
            
            val unifiedResults = unifiedSearchHandler.performSearch(
                query = trimmedQuery,
                enabledFileTypes = enabledFileTypes,
                canSearchContacts = canSearchContacts,
                canSearchFiles = canSearchFiles,
                canSearchSettings = canSearchSettings
            )

            withContext(Dispatchers.Main) {
                if (currentVersion == queryVersion) {
                    val hasAnyResults = unifiedResults.contactResults.isNotEmpty() || 
                        unifiedResults.fileResults.isNotEmpty() || 
                        unifiedResults.settingResults.isNotEmpty() ||
                        _uiState.value.searchResults.isNotEmpty()
                    
                    _uiState.update { state ->
                        state.copy(
                            contactResults = unifiedResults.contactResults,
                            fileResults = unifiedResults.fileResults,
                            settingResults = unifiedResults.settingResults,
                            webSuggestions = if (hasAnyResults) emptyList() else state.webSuggestions
                        )
                    }
                    
                    // Fetch web suggestions if there are no results, query is long enough, and suggestions are enabled
                    if (!hasAnyResults && trimmedQuery.length >= 2 && webSuggestionsEnabled) {
                        fetchWebSuggestions(trimmedQuery, currentVersion)
                    } else {
                        // Clear suggestions if we have results
                        webSuggestionsJob?.cancel()
                        if (hasAnyResults) {
                            _uiState.update { state ->
                                state.copy(webSuggestions = emptyList())
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun fetchWebSuggestions(query: String, queryVersion: Long) {
        webSuggestionsJob?.cancel()
        webSuggestionsJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val suggestions = WebSuggestionsUtils.getSuggestions(query)
                withContext(Dispatchers.Main) {
                    // Only update if query hasn't changed
                    if (this@SearchViewModel.queryVersion == queryVersion && 
                        _uiState.value.query.trim() == query.trim()) {
                        _uiState.update { state ->
                            state.copy(webSuggestions = suggestions)
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail - don't show suggestions on error
            }
        }
    }
    
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

    fun openContact(contactInfo: ContactInfo) {
        ContactIntentHelpers.openContact(getApplication(), contactInfo)
        if (clearQueryAfterSearchEngine) {
            clearQuery()
        }
    }

    fun openEmail(email: String) {
        ContactIntentHelpers.composeEmail(getApplication(), email)
    }
    
    fun handleContactMethod(contactInfo: ContactInfo, method: ContactMethod) {
        contactActionHandler.handleContactMethod(contactInfo, method)
    }

    fun showContactMethodsBottomSheet(contactInfo: ContactInfo) {
        _uiState.update { it.copy(contactMethodsBottomSheet = contactInfo) }
    }

    fun dismissContactMethodsBottomSheet() {
        _uiState.update { it.copy(contactMethodsBottomSheet = null) }
    }

    fun callContact(contactInfo: ContactInfo) {
        contactActionHandler.callContact(contactInfo)
    }

    fun smsContact(contactInfo: ContactInfo) {
        contactActionHandler.smsContact(contactInfo)
    }
    
    fun onPhoneNumberSelected(phoneNumber: String, rememberChoice: Boolean) {
        contactActionHandler.onPhoneNumberSelected(phoneNumber, rememberChoice)
    }
    
    fun dismissPhoneNumberSelection() {
        contactActionHandler.dismissPhoneNumberSelection()
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

    fun dismissDirectDialChoice() {
        contactActionHandler.dismissDirectDialChoice()
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

    fun openSetting(setting: SettingShortcut) {
        settingsSearchHandler.openSetting(setting)
    }

    fun openFile(deviceFile: DeviceFile) {
        IntentHelpers.openFile(getApplication(), deviceFile)
        clearQuery()
    }
}

