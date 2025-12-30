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
    private var cachedApps: List<AppInfo> = emptyList()
    private var noMatchPrefix: String? = null
    private var availableSettings: List<SettingShortcut> = emptyList()
    private var geminiApiKey: String? = userPreferences.getGeminiApiKey()
    private var geminiClient: DirectSearchClient? = geminiApiKey?.let { DirectSearchClient(it) }
    private var personalContext: String = userPreferences.getPersonalContext().orEmpty()
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
    private var DirectSearchJob: Job? = null
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
        // Load cached apps synchronously for instant UI display
        val cachedAppsList = runCatching {
            repository.loadCachedApps()
        }.getOrNull()
        
        if (cachedAppsList != null && cachedAppsList.isNotEmpty()) {
            // Set cached apps immediately
            cachedApps = cachedAppsList
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
                    hasGeminiApiKey = !geminiApiKey.isNullOrBlank(),
                    geminiApiKeyLast4 = geminiApiKey?.takeLast(4),
                    personalContext = personalContext,
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
                    hasGeminiApiKey = !geminiApiKey.isNullOrBlank(),
                    geminiApiKeyLast4 = geminiApiKey?.takeLast(4),
                    personalContext = personalContext
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

    /**
     * Loads apps from cache first for instant display, then refreshes in background.
     * Note: Cache is already loaded synchronously in init, so this just refreshes.
     */
    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            // Cache was already loaded synchronously in init, so just refresh in background
            refreshApps()
        }
    }

    private fun loadSettingsShortcuts() {
        viewModelScope.launch(Dispatchers.IO) {
            availableSettings = settingsShortcutRepository.loadShortcuts()
            refreshSettingsState()
        }
    }

    private fun refreshSettingsState(updateResults: Boolean = true) {
        val pinned = availableSettings
            .filter { userPreferences.getPinnedSettingIds().contains(it.id) && !userPreferences.getExcludedSettingIds().contains(it.id) }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
        val excluded = availableSettings
            .filter { userPreferences.getExcludedSettingIds().contains(it.id) }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
        val currentQuery = _uiState.value.query
        val results = if (updateResults && currentQuery.isNotBlank() && SearchSection.SETTINGS !in sectionManager.disabledSections) {
            searchSettings(currentQuery)
        } else if (currentQuery.isBlank() || SearchSection.SETTINGS in sectionManager.disabledSections) {
            emptyList()
        } else {
            _uiState.value.settingResults
        }

        _uiState.update { state ->
            state.copy(
                pinnedSettings = pinned,
                excludedSettings = excluded,
                settingResults = results
            )
        }
    }

    fun refreshUsageAccess() {
        _uiState.update { it.copy(hasUsagePermission = repository.hasUsageAccess()) }
    }

    fun refreshApps(showToast: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            // Only show loading if we don't have any cached apps yet
            if (cachedApps.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            runCatching { repository.loadLaunchableApps() }
                .onSuccess { apps ->
                    cachedApps = apps
                    noMatchPrefix = null
                    val lastUpdated = System.currentTimeMillis()
                    refreshDerivedState(
                        lastUpdated = lastUpdated,
                        isLoading = false
                    )
                    // Show success toast only for user-triggered refreshes
                    if (showToast) {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                getApplication(),
                                "Apps refreshed successfully",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .onFailure { error ->
                    val fallbackMessage = getApplication<Application>().getString(R.string.error_loading_user_apps)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: fallbackMessage
                        )
                    }
                    // Show error toast only for user-triggered refreshes
                    if (showToast) {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                getApplication(),
                                "Failed to refresh apps",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        }
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
            clearDirectSearchState()
        } else if (DirectSearchState.activeQuery != null && DirectSearchState.activeQuery != trimmedQuery) {
            clearDirectSearchState()
        }

        if (trimmedQuery.isBlank()) {
            noMatchPrefix = null
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
        resetNoMatchPrefixIfNeeded(normalizedQuery)

        val shouldSkipSearch = shouldSkipDueToNoMatchPrefix(normalizedQuery)
        val matches = if (shouldSkipSearch) {
            emptyList()
        } else {
            deriveMatches(trimmedQuery, searchSourceApps()).also { results ->
                if (results.isEmpty()) {
                    noMatchPrefix = normalizedQuery
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
            if (engine == SearchEngine.DIRECT_SEARCH && geminiApiKey.isNullOrBlank()) continue
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

    private fun resetNoMatchPrefixIfNeeded(normalizedQuery: String) {
        val prefix = noMatchPrefix ?: return
        if (!normalizedQuery.startsWith(prefix)) {
            noMatchPrefix = null
        }
    }

    private fun shouldSkipDueToNoMatchPrefix(normalizedQuery: String): Boolean {
        val prefix = noMatchPrefix ?: return false
        return normalizedQuery.length >= prefix.length && normalizedQuery.startsWith(prefix)
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearCache()
            cachedApps = emptyList()
            noMatchPrefix = null
            _uiState.update { state ->
                state.copy(
                    recentApps = emptyList(),
                    searchResults = emptyList(),
                    pinnedApps = emptyList(),
                    suggestionExcludedApps = emptyList(),
                    resultExcludedApps = emptyList(),
                    indexedAppCount = 0,
                    cacheLastUpdatedMillis = 0L,
                    isLoading = true
                )
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    getApplication(),
                    getApplication<Application>().getString(R.string.settings_cache_cleared_toast),
                    Toast.LENGTH_SHORT
                ).show()
            }
            refreshApps()
        }
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
            this@SearchViewModel.sortAppsByUsageEnabled = sortAppsByUsageEnabled
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
            val hadGemini = _uiState.value.hasGeminiApiKey
            val normalized = apiKey?.trim().takeUnless { it.isNullOrBlank() }
            if (normalized == geminiApiKey) return@launch

            geminiApiKey = normalized
            geminiClient = normalized?.let { DirectSearchClient(it) }
            userPreferences.setGeminiApiKey(normalized)

            val hasGemini = !normalized.isNullOrBlank()
            if (!hasGemini) {
                clearDirectSearchState()
            }
            searchEngineManager.updateSearchEnginesForGemini(hasGemini)
            if (hasGemini && !hadGemini) {
                clearDirectSearchState()
            }
            _uiState.update {
                it.copy(
                    hasGeminiApiKey = hasGemini,
                    geminiApiKeyLast4 = normalized?.takeLast(4)
                )
            }
        }
    }

    fun setPersonalContext(context: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalized = context?.trim().orEmpty()
            if (normalized == personalContext) return@launch

            personalContext = normalized
            userPreferences.setPersonalContext(normalized.takeUnless { it.isBlank() })
            _uiState.update {
                it.copy(personalContext = personalContext)
            }
        }
    }

    fun requestDirectSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            clearDirectSearchState()
            return
        }

        val client = geminiClient
        if (client == null || geminiApiKey.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    DirectSearchState = DirectSearchState(
                        status = DirectSearchStatus.Error,
                        errorMessage = getApplication<Application>().getString(R.string.direct_search_error_no_key),
                        activeQuery = trimmedQuery
                    ),
                    calculatorState = CalculatorState()
                )
            }
            return
        }

        DirectSearchJob?.cancel()
        DirectSearchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    DirectSearchState = DirectSearchState(
                        status = DirectSearchStatus.Loading,
                        activeQuery = trimmedQuery
                    ),
                    calculatorState = CalculatorState()
                )
            }

            val result = client.fetchAnswer(
                trimmedQuery,
                personalContext.takeIf { it.isNotBlank() }
            )
            result.onSuccess { answer ->
                _uiState.update { state ->
                    state.copy(
                        DirectSearchState = DirectSearchState(
                            status = DirectSearchStatus.Success,
                            answer = answer,
                            activeQuery = trimmedQuery
                        ),
                        calculatorState = CalculatorState()
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                val message = error.message
                    ?: getApplication<Application>().getString(R.string.direct_search_error_generic)
                _uiState.update { state ->
                    state.copy(
                        DirectSearchState = DirectSearchState(
                            status = DirectSearchStatus.Error,
                            errorMessage = message,
                            activeQuery = trimmedQuery
                        ),
                        calculatorState = CalculatorState()
                    )
                }
            }
        }
    }

    fun retryDirectSearch() {
        val lastQuery = _uiState.value.DirectSearchState.activeQuery ?: _uiState.value.query
        if (lastQuery.isNullOrBlank()) return
        requestDirectSearch(lastQuery)
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


    private fun clearDirectSearchState() {
        DirectSearchJob?.cancel()
        DirectSearchJob = null
        _uiState.update {
            it.copy(
                DirectSearchState = DirectSearchState(),
                calculatorState = CalculatorState()
            )
        }
    }


    fun reorderSections(newOrder: List<SearchSection>) = sectionManager.reorderSections(newOrder)

    fun setSectionEnabled(section: SearchSection, enabled: Boolean) = sectionManager.setSectionEnabled(section, enabled)

    /**
     * Checks if a section can be enabled (i.e., has required permissions).
     */
    fun canEnableSection(section: SearchSection): Boolean = sectionManager.canEnableSection(section)

    private fun deriveMatches(query: String, source: List<AppInfo>): List<AppInfo> {
        if (query.isBlank()) return emptyList()
        return source
            .asSequence()
            .mapNotNull { app ->
                val nickname = userPreferences.getAppNickname(app.packageName)
                val priority = SearchRankingUtils.calculateMatchPriorityWithNickname(
                    app.appName,
                    nickname,
                    query
                )
                if (SearchRankingUtils.isOtherMatch(priority)) {
                    return@mapNotNull null
                }
                app to priority
            }
            .sortedWith(compareBy(
                { it.second },
                {
                    if (sortAppsByUsageEnabled) {
                        // When sorting by usage is enabled, use negative lastUsedTime for descending order (most recent first)
                        -it.first.lastUsedTime
                    } else {
                        // Default: sort by app name
                        it.first.appName.lowercase(Locale.getDefault())
                    }
                }
            ))
            .map { it.first }
            .take(GRID_ITEM_COUNT)
            .toList()
    }

    private fun searchSettings(query: String): List<SettingShortcut> {
        if (availableSettings.isEmpty()) return emptyList()
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val normalizedQuery = trimmed.lowercase(Locale.getDefault())
        val nicknameMatches = userPreferences.findSettingsWithMatchingNickname(trimmed)
            .filterNot { userPreferences.getExcludedSettingIds().contains(it) }
            .toSet()

        return availableSettings
            .asSequence()
            .filterNot { userPreferences.getExcludedSettingIds().contains(it.id) }
            .mapNotNull { shortcut ->
                val nickname = userPreferences.getSettingNickname(shortcut.id)
                val hasNicknameMatch = nickname?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true
                val keywordText = shortcut.keywords.joinToString(" ")
                val hasFieldMatch = shortcut.title.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                    (shortcut.description?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true) ||
                    keywordText.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                    nicknameMatches.contains(shortcut.id)

                if (!hasFieldMatch && !hasNicknameMatch) return@mapNotNull null

                val priority = when {
                    hasNicknameMatch || nicknameMatches.contains(shortcut.id) -> 0
                    else -> SearchRankingUtils.getBestMatchPriority(
                        trimmed,
                        shortcut.title,
                        shortcut.description ?: "",
                        keywordText
                    )
                }
                shortcut to priority
            }
            .sortedWith(compareBy({ it.second }, { it.first.title.lowercase(Locale.getDefault()) }))
            .take(6)
            .map { it.first }
            .toList()
    }

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
        private const val GRID_ITEM_COUNT = 10
        private const val SECONDARY_SEARCH_DEBOUNCE_MS = 150L
    }

    private fun availableApps(apps: List<AppInfo> = cachedApps): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps.filterNot { userPreferences.getSuggestionHiddenPackages().contains(it.packageName) }
    }

    private fun searchSourceApps(apps: List<AppInfo> = cachedApps): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps.filterNot {
            userPreferences.getResultHiddenPackages().contains(it.packageName) || userPreferences.getPinnedPackages().contains(it.packageName)
        }
    }

    private fun computePinnedApps(apps: List<AppInfo>, exclusion: Set<String>): List<AppInfo> {
        if (apps.isEmpty() || userPreferences.getPinnedPackages().isEmpty()) return emptyList()
        return apps
            .asSequence()
            .filter { userPreferences.getPinnedPackages().contains(it.packageName) && !exclusion.contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
            .toList()
    }

    private fun refreshDerivedState(
        lastUpdated: Long? = null,
        isLoading: Boolean? = null
    ) {
        val apps = cachedApps
        val visibleAppList = availableApps(apps)
        val pinnedAppsForSuggestions = computePinnedApps(apps, userPreferences.getSuggestionHiddenPackages())
        val pinnedAppsForResults = computePinnedApps(apps, userPreferences.getResultHiddenPackages())
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
            val nonPinnedApps = searchSourceApps(apps)
            val allSearchableApps = (pinnedAppsForResults + nonPinnedApps).distinctBy { it.packageName }
            deriveMatches(trimmedQuery, allSearchableApps)
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
        val currentVersion = ++queryVersion

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            // Debounce expensive contact/file queries during rapid typing
            delay(SECONDARY_SEARCH_DEBOUNCE_MS)
            if (currentVersion != queryVersion) return@launch

            val normalizedQuery = trimmedQuery.lowercase(Locale.getDefault())
            
            // Search by display name (existing behavior)
            val result = searchOperations.performSearches(
                query = trimmedQuery,
                canSearchContacts = canSearchContacts,
                canSearchFiles = canSearchFiles,
                enabledFileTypes = enabledFileTypes,
                excludedContactIds = userPreferences.getExcludedContactIds(),
                excludedFileUris = userPreferences.getExcludedFileUris(),
                excludedFileExtensions = userPreferences.getExcludedFileExtensions(),
                scope = this
            )

            // Also search for contacts/files with matching nicknames
            val nicknameMatchingContactIds = if (canSearchContacts) {
                userPreferences.findContactsWithMatchingNickname(trimmedQuery)
                    .filterNot { userPreferences.getExcludedContactIds().contains(it) }
            } else {
                emptySet()
            }

            val nicknameMatchingFileUris = if (canSearchFiles) {
                userPreferences.findFilesWithMatchingNickname(trimmedQuery)
                    .filterNot { userPreferences.getExcludedFileUris().contains(it) }
            } else {
                emptySet()
            }

            // Fetch contacts/files that match by nickname but not by display name
            val nicknameOnlyContacts = if (nicknameMatchingContactIds.isNotEmpty()) {
                val displayNameMatchedIds = result.contacts.map { it.contactId }.toSet()
                val nicknameOnlyIds = nicknameMatchingContactIds.filterNot { displayNameMatchedIds.contains(it) }
                if (nicknameOnlyIds.isNotEmpty()) {
                    contactRepository.getContactsByIds(nicknameOnlyIds.toSet())
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val nicknameOnlyFiles = if (nicknameMatchingFileUris.isNotEmpty()) {
                val displayNameMatchedUris = result.files.map { it.uri.toString() }.toSet()
                val nicknameOnlyUris = nicknameMatchingFileUris.filterNot { displayNameMatchedUris.contains(it) }
                if (nicknameOnlyUris.isNotEmpty()) {
                    fileRepository.getFilesByUris(nicknameOnlyUris.toSet())
                        .filter { file ->
                            val fileType = com.tk.quicksearch.model.FileTypeUtils.getFileType(file)
                            fileType in enabledFileTypes &&
                            !userPreferences.getExcludedFileUris().contains(file.uri.toString()) &&
                            !FileUtils.isFileExtensionExcluded(file.displayName, userPreferences.getExcludedFileExtensions())
                        }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }

            // Combine display name matches and nickname-only matches
            val allContacts = (result.contacts + nicknameOnlyContacts).distinctBy { it.contactId }
            val allFiles = (result.files + nicknameOnlyFiles).distinctBy { it.uri.toString() }

            // Filter and rank by nickname matches
            val filteredContacts = allContacts.mapNotNull { contact ->
                val nickname = userPreferences.getContactNickname(contact.contactId)
                val priority = SearchRankingUtils.calculateMatchPriorityWithNickname(
                    contact.displayName,
                    nickname,
                    normalizedQuery
                )
                if (SearchRankingUtils.isOtherMatch(priority)) {
                    null
                } else {
                    contact to priority
                }
            }.sortedWith(
                compareBy<Pair<ContactInfo, Int>> { it.second }
                    .thenBy { it.first.displayName.lowercase(Locale.getDefault()) }
            ).map { it.first }

            val filteredFiles = allFiles.mapNotNull { file ->
                val nickname = userPreferences.getFileNickname(file.uri.toString())
                val priority = SearchRankingUtils.calculateMatchPriorityWithNickname(
                    file.displayName,
                    nickname,
                    normalizedQuery
                )
                if (SearchRankingUtils.isOtherMatch(priority)) {
                    null
                } else {
                    file to priority
                }
            }.sortedWith(
                compareBy<Pair<DeviceFile, Int>> { it.second }
                    .thenBy { it.first.displayName.lowercase(Locale.getDefault()) }
            ).map { it.first }

            val shouldSearchSettings = SearchSection.SETTINGS !in sectionManager.disabledSections
            val settingsMatches = if (shouldSearchSettings) {
                searchSettings(trimmedQuery)
            } else {
                emptyList()
            }

            withContext(Dispatchers.Main) {
                if (currentVersion == queryVersion) {
                    val hasAnyResults = filteredContacts.isNotEmpty() || 
                        filteredFiles.isNotEmpty() || 
                        settingsMatches.isNotEmpty() ||
                        _uiState.value.searchResults.isNotEmpty()
                    
                    _uiState.update { state ->
                        state.copy(
                            contactResults = filteredContacts,
                            fileResults = filteredFiles,
                            settingResults = settingsMatches,
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
        DirectSearchJob?.cancel()
    }

    fun openSetting(setting: SettingShortcut) {
        val context = getApplication<Application>()
        runCatching {
            val intent = settingsShortcutRepository.buildIntent(setting)
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(
                context,
                context.getString(R.string.error_open_setting, setting.title),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun openFile(deviceFile: DeviceFile) {
        IntentHelpers.openFile(getApplication(), deviceFile)
        clearQuery()
    }
}

