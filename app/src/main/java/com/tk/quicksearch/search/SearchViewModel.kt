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
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.model.matches
import com.tk.quicksearch.util.SearchRankingUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class SearchEngine {
    DIRECT_ANSWER,
    GOOGLE,
    CHATGPT,
    PERPLEXITY,
    GROK,
    GOOGLE_MAPS,
    GOOGLE_PLAY,
    REDDIT,
    YOUTUBE,
    AMAZON,
    AI_MODE
}

enum class SearchSection {
    APPS,
    CONTACTS,
    FILES,
    SETTINGS
}

enum class MessagingApp {
    MESSAGES,
    WHATSAPP,
    TELEGRAM
}

enum class DirectAnswerStatus {
    Idle,
    Loading,
    Success,
    Error
}

data class DirectAnswerState(
    val status: DirectAnswerStatus = DirectAnswerStatus.Idle,
    val answer: String? = null,
    val errorMessage: String? = null,
    val activeQuery: String? = null
)

data class PhoneNumberSelection(
    val contactInfo: ContactInfo,
    val isCall: Boolean // true for call, false for SMS
)

data class SearchUiState(
    val query: String = "",
    val hasUsagePermission: Boolean = false,
    val hasContactPermission: Boolean = false,
    val hasFilePermission: Boolean = false,
    val hasCallPermission: Boolean = false,
    val isLoading: Boolean = true,
    val recentApps: List<AppInfo> = emptyList(),
    val searchResults: List<AppInfo> = emptyList(),
    val pinnedApps: List<AppInfo> = emptyList(),
    val suggestionExcludedApps: List<AppInfo> = emptyList(),
    val resultExcludedApps: List<AppInfo> = emptyList(),
    val contactResults: List<ContactInfo> = emptyList(),
    val fileResults: List<DeviceFile> = emptyList(),
    val settingResults: List<SettingShortcut> = emptyList(),
    val pinnedContacts: List<ContactInfo> = emptyList(),
    val pinnedFiles: List<DeviceFile> = emptyList(),
    val pinnedSettings: List<SettingShortcut> = emptyList(),
    val excludedContacts: List<ContactInfo> = emptyList(),
    val excludedFiles: List<DeviceFile> = emptyList(),
    val excludedSettings: List<SettingShortcut> = emptyList(),
    val indexedAppCount: Int = 0,
    val cacheLastUpdatedMillis: Long = 0L,
    val errorMessage: String? = null,
    val searchEngineOrder: List<SearchEngine> = emptyList(),
    val disabledSearchEngines: Set<SearchEngine> = emptySet(),
    val phoneNumberSelection: PhoneNumberSelection? = null,
    val enabledFileTypes: Set<FileType> = FileType.values().toSet(),
    val keyboardAlignedLayout: Boolean = true,
    val shortcutsEnabled: Boolean = true,
    val shortcutCodes: Map<SearchEngine, String> = emptyMap(),
    val shortcutEnabled: Map<SearchEngine, Boolean> = emptyMap(),
    val messagingApp: MessagingApp = MessagingApp.MESSAGES,
    val isWhatsAppInstalled: Boolean = false,
    val isTelegramInstalled: Boolean = false,
    val showWallpaperBackground: Boolean = true,
    val sectionOrder: List<SearchSection> = emptyList(),
    val disabledSections: Set<SearchSection> = emptySet(),
    val searchEngineSectionEnabled: Boolean = true,
    val amazonDomain: String? = null,
    val directAnswerState: DirectAnswerState = DirectAnswerState(),
    val hasGeminiApiKey: Boolean = false,
    val geminiApiKeyLast4: String? = null
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppUsageRepository(application.applicationContext)
    private val contactRepository = ContactRepository(application.applicationContext)
    private val fileRepository = FileSearchRepository(application.applicationContext)
    private val settingsShortcutRepository = SettingsShortcutRepository(application.applicationContext)
    private val userPreferences = UserAppPreferences(application.applicationContext)
    private val permissionManager = PermissionManager(contactRepository, fileRepository, userPreferences)
    private val searchOperations = SearchOperations(contactRepository, fileRepository)
    private var cachedApps: List<AppInfo> = emptyList()
    private var noMatchPrefix: String? = null
    private var hiddenSuggestionPackages: Set<String> = userPreferences.getSuggestionHiddenPackages()
    private var hiddenResultPackages: Set<String> = userPreferences.getResultHiddenPackages()
    private var pinnedPackages: Set<String> = userPreferences.getPinnedPackages()
    private var pinnedContactIds: Set<Long> = userPreferences.getPinnedContactIds()
    private var excludedContactIds: Set<Long> = userPreferences.getExcludedContactIds()
    private var pinnedFileUris: Set<String> = userPreferences.getPinnedFileUris()
    private var excludedFileUris: Set<String> = userPreferences.getExcludedFileUris()
    private var availableSettings: List<SettingShortcut> = emptyList()
    private var pinnedSettingIds: Set<String> = userPreferences.getPinnedSettingIds()
    private var excludedSettingIds: Set<String> = userPreferences.getExcludedSettingIds()
    private var geminiApiKey: String? = userPreferences.getGeminiApiKey()
    private var geminiClient: DirectAnswerClient? = geminiApiKey?.let { DirectAnswerClient(it) }
    private var searchEngineOrder: List<SearchEngine> = loadSearchEngineOrder()
    private var disabledSearchEngines: Set<SearchEngine> = loadDisabledSearchEngines()
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
    private var sectionOrder: List<SearchSection> = loadSectionOrder()
    private var disabledSections: Set<SearchSection> = permissionManager.computeDisabledSections()
    private var searchEngineSectionEnabled: Boolean = userPreferences.isSearchEngineSectionEnabled()
    private var amazonDomain: String? = userPreferences.getAmazonDomain()
    private var searchJob: Job? = null
    private var directAnswerJob: Job? = null
    private var queryVersion: Long = 0L

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

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
                    searchEngineOrder = searchEngineOrder,
                    disabledSearchEngines = disabledSearchEngines,
                    enabledFileTypes = enabledFileTypes,
                    keyboardAlignedLayout = keyboardAlignedLayout,
                    shortcutsEnabled = shortcutsEnabled,
                    shortcutCodes = shortcutCodes,
                    shortcutEnabled = shortcutEnabled,
                    messagingApp = resolvedMessagingApp,
                    isWhatsAppInstalled = isWhatsAppInstalled,
                    isTelegramInstalled = isTelegramInstalled,
                    showWallpaperBackground = showWallpaperBackground,
                    sectionOrder = sectionOrder,
                    disabledSections = disabledSections,
                    searchEngineSectionEnabled = searchEngineSectionEnabled,
                    amazonDomain = amazonDomain,
                    hasGeminiApiKey = !geminiApiKey.isNullOrBlank(),
                    geminiApiKeyLast4 = geminiApiKey?.takeLast(4),
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
                    searchEngineOrder = searchEngineOrder,
                    disabledSearchEngines = disabledSearchEngines,
                    enabledFileTypes = enabledFileTypes,
                    keyboardAlignedLayout = keyboardAlignedLayout,
                    shortcutsEnabled = shortcutsEnabled,
                    shortcutCodes = shortcutCodes,
                    shortcutEnabled = shortcutEnabled,
                    messagingApp = resolvedMessagingApp,
                    isWhatsAppInstalled = isWhatsAppInstalled,
                    isTelegramInstalled = isTelegramInstalled,
                    showWallpaperBackground = showWallpaperBackground,
                    sectionOrder = sectionOrder,
                    disabledSections = disabledSections,
                    searchEngineSectionEnabled = searchEngineSectionEnabled,
                    amazonDomain = amazonDomain,
                    hasGeminiApiKey = !geminiApiKey.isNullOrBlank(),
                    geminiApiKeyLast4 = geminiApiKey?.takeLast(4)
                )
            }
        }
        
        // Refresh permissions (fast operations)
        refreshUsageAccess()
        refreshOptionalPermissions()
        
        // Load apps in background (refresh cache if needed)
        loadApps()
        loadSettingsShortcuts()
        
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
            .filter { pinnedSettingIds.contains(it.id) && !excludedSettingIds.contains(it.id) }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
        val excluded = availableSettings
            .filter { excludedSettingIds.contains(it.id) }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
        val currentQuery = _uiState.value.query
        val results = if (updateResults && currentQuery.isNotBlank() && SearchSection.SETTINGS !in disabledSections) {
            searchSettings(currentQuery)
        } else if (currentQuery.isBlank() || SearchSection.SETTINGS in disabledSections) {
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

    fun refreshApps() {
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
                }
                .onFailure { error ->
                    val fallbackMessage = getApplication<Application>().getString(R.string.error_loading_user_apps)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: fallbackMessage
                        )
                    }
                }
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

    fun setMessagingApp(app: MessagingApp) {
        messagingApp = app
        val whatsappInstalled = _uiState.value.isWhatsAppInstalled
        val telegramInstalled = _uiState.value.isTelegramInstalled
        updateMessagingAvailability(
            isWhatsAppInstalled = whatsappInstalled,
            isTelegramInstalled = telegramInstalled
        )
    }

    fun onQueryChange(newQuery: String) {
        val previousQuery = _uiState.value.query
        val trimmedQuery = newQuery.trim()
        val directAnswerState = _uiState.value.directAnswerState
        if (directAnswerState.status != DirectAnswerStatus.Idle && newQuery != previousQuery) {
            clearDirectAnswerState()
        } else if (directAnswerState.activeQuery != null && directAnswerState.activeQuery != trimmedQuery) {
            clearDirectAnswerState()
        }

        if (trimmedQuery.isBlank()) {
            noMatchPrefix = null
            searchJob?.cancel()
            _uiState.update { 
                it.copy(
                    query = "",
                    searchResults = emptyList(),
                    contactResults = emptyList(),
                    fileResults = emptyList(),
                    settingResults = emptyList(),
                    directAnswerState = DirectAnswerState()
                ) 
            }
            return
        }

        // Check for shortcuts if enabled
        if (shortcutsEnabled) {
            val shortcutMatch = detectShortcut(trimmedQuery)
            if (shortcutMatch != null) {
                val (queryWithoutShortcut, engine) = shortcutMatch
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

        _uiState.update { state ->
            state.copy(
                query = newQuery,
                searchResults = matches
            )
        }
        performSecondarySearches(newQuery)
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
            if (engine == SearchEngine.DIRECT_ANSWER && geminiApiKey.isNullOrBlank()) continue
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

            val pinnedContacts = if (hasContactsPermission && pinnedContactIds.isNotEmpty()) {
                contactRepository.getContactsByIds(pinnedContactIds)
                    .filterNot { excludedContactIds.contains(it.contactId) }
            } else {
                emptyList()
            }

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

            val excludedContacts = if (hasContactsPermission && excludedContactIds.isNotEmpty()) {
                contactRepository.getContactsByIds(excludedContactIds)
            } else {
                emptyList()
            }

            val excludedFiles = if (hasFilesPermission && excludedFileUris.isNotEmpty()) {
                fileRepository.getFilesByUris(excludedFileUris)
            } else {
                emptyList()
            }

            _uiState.update { state ->
                state.copy(
                    excludedContacts = excludedContacts,
                    excludedFiles = excludedFiles
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
    }

    fun openAppInfo(appInfo: AppInfo) {
        IntentHelpers.openAppInfo(getApplication(), appInfo.packageName)
    }

    fun requestUninstall(appInfo: AppInfo) {
        IntentHelpers.requestUninstall(getApplication(), appInfo)
    }

    fun openSearchUrl(query: String, searchEngine: SearchEngine) {
        val trimmedQuery = query.trim()
        if (searchEngine == SearchEngine.DIRECT_ANSWER) {
            requestDirectAnswer(trimmedQuery)
            return
        }
        val amazonDomain = if (searchEngine == SearchEngine.AMAZON) {
            userPreferences.getAmazonDomain()
        } else {
            null
        }
        IntentHelpers.openSearchUrl(getApplication(), trimmedQuery, searchEngine, amazonDomain)
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

    fun pinContact(contactInfo: ContactInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            pinnedContactIds = userPreferences.pinContact(contactInfo.contactId)
            loadPinnedContactsAndFiles()
        }
    }

    fun unpinContact(contactInfo: ContactInfo) {
        // Update UI immediately
        _uiState.update { state ->
            state.copy(
                pinnedContacts = state.pinnedContacts.filterNot { it.contactId == contactInfo.contactId }
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            pinnedContactIds = userPreferences.unpinContact(contactInfo.contactId)
            loadPinnedContactsAndFiles()
        }
    }

    fun excludeContact(contactInfo: ContactInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            excludedContactIds = userPreferences.excludeContact(contactInfo.contactId)
            // Removing from pinned if present
            if (pinnedContactIds.contains(contactInfo.contactId)) {
                pinnedContactIds = userPreferences.unpinContact(contactInfo.contactId)
            }

            // Update current state to reflect exclusion immediately
            _uiState.update { state ->
                state.copy(
                    contactResults = state.contactResults.filterNot { it.contactId == contactInfo.contactId },
                    pinnedContacts = state.pinnedContacts.filterNot { it.contactId == contactInfo.contactId }
                )
            }
            loadExcludedContactsAndFiles()
        }
    }

    fun pinFile(deviceFile: DeviceFile) {
        val uriString = deviceFile.uri.toString()
        viewModelScope.launch(Dispatchers.IO) {
            pinnedFileUris = userPreferences.pinFile(uriString)
            loadPinnedContactsAndFiles()
        }
    }

    fun unpinFile(deviceFile: DeviceFile) {
        val uriString = deviceFile.uri.toString()
        // Update UI immediately
        _uiState.update { state ->
            state.copy(
                pinnedFiles = state.pinnedFiles.filterNot { it.uri.toString() == uriString }
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            // Try to find and remove the stored URI that matches this file
            // The file's URI might differ slightly from what's stored, so we need to find the match
            val storedUriToRemove = pinnedFileUris.firstOrNull { stored ->
                // Normalize both URIs for comparison
                val storedNormalized = android.net.Uri.parse(stored)?.toString() ?: stored
                val fileNormalized = android.net.Uri.parse(uriString)?.toString() ?: uriString
                storedNormalized == fileNormalized || stored == uriString
            } ?: uriString
            
            pinnedFileUris = userPreferences.unpinFile(storedUriToRemove)
            loadPinnedContactsAndFiles()
        }
    }

    fun excludeFile(deviceFile: DeviceFile) {
        val uriString = deviceFile.uri.toString()
        viewModelScope.launch(Dispatchers.IO) {
            excludedFileUris = userPreferences.excludeFile(uriString)
            if (pinnedFileUris.contains(uriString)) {
                pinnedFileUris = userPreferences.unpinFile(uriString)
            }

            _uiState.update { state ->
                state.copy(
                    fileResults = state.fileResults.filterNot { it.uri.toString() == uriString },
                    pinnedFiles = state.pinnedFiles.filterNot { it.uri.toString() == uriString }
                )
            }
            loadExcludedContactsAndFiles()
        }
    }

    fun pinSetting(setting: SettingShortcut) {
        if (excludedSettingIds.contains(setting.id)) return
        viewModelScope.launch(Dispatchers.IO) {
            pinnedSettingIds = userPreferences.pinSetting(setting.id)
            refreshSettingsState()
        }
    }

    fun unpinSetting(setting: SettingShortcut) {
        viewModelScope.launch(Dispatchers.IO) {
            pinnedSettingIds = userPreferences.unpinSetting(setting.id)
            refreshSettingsState()
        }
    }

    fun excludeSetting(setting: SettingShortcut) {
        viewModelScope.launch(Dispatchers.IO) {
            excludedSettingIds = userPreferences.excludeSetting(setting.id)
            if (pinnedSettingIds.contains(setting.id)) {
                pinnedSettingIds = userPreferences.unpinSetting(setting.id)
            }
            _uiState.update { state ->
                state.copy(
                    settingResults = state.settingResults.filterNot { it.id == setting.id },
                    pinnedSettings = state.pinnedSettings.filterNot { it.id == setting.id }
                )
            }
            refreshSettingsState()
        }
    }

    fun setSettingNickname(setting: SettingShortcut, nickname: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setSettingNickname(setting.id, nickname)
            val currentQuery = _uiState.value.query
            if (currentQuery.isNotBlank()) {
                _uiState.update { it.copy(settingResults = searchSettings(currentQuery)) }
            } else {
                refreshSettingsState(updateResults = false)
            }
        }
    }

    fun getSettingNickname(id: String): String? {
        return userPreferences.getSettingNickname(id)
    }

    fun hideApp(appInfo: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val isSearching = _uiState.value.query.isNotBlank()
            if (isSearching) {
                hiddenResultPackages = userPreferences.hidePackageInResults(appInfo.packageName)
            } else {
                hiddenSuggestionPackages = userPreferences.hidePackageInSuggestions(appInfo.packageName)
                if (pinnedPackages.contains(appInfo.packageName)) {
                    pinnedPackages = userPreferences.unpinPackage(appInfo.packageName)
                }
            }
            refreshDerivedState()
        }
    }

    fun unhideAppFromSuggestions(appInfo: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            hiddenSuggestionPackages = userPreferences.unhidePackageInSuggestions(appInfo.packageName)
            refreshDerivedState()
        }
    }

    fun unhideAppFromResults(appInfo: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            hiddenResultPackages = userPreferences.unhidePackageInResults(appInfo.packageName)
            refreshDerivedState()
        }
    }

    fun removeExcludedContact(contactInfo: ContactInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            excludedContactIds = userPreferences.removeExcludedContact(contactInfo.contactId)
            loadExcludedContactsAndFiles()
        }
    }

    fun removeExcludedFile(deviceFile: DeviceFile) {
        viewModelScope.launch(Dispatchers.IO) {
            val uriString = deviceFile.uri.toString()
            excludedFileUris = userPreferences.removeExcludedFile(uriString)
            loadExcludedContactsAndFiles()
        }
    }

    fun removeExcludedSetting(setting: SettingShortcut) {
        viewModelScope.launch(Dispatchers.IO) {
            excludedSettingIds = userPreferences.removeExcludedSetting(setting.id)
            refreshSettingsState()
        }
    }

    fun clearAllExcludedContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            excludedContactIds = userPreferences.clearAllExcludedContacts()
            loadExcludedContactsAndFiles()
        }
    }

    fun clearAllExcludedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            excludedFileUris = userPreferences.clearAllExcludedFiles()
            loadExcludedContactsAndFiles()
        }
    }

    fun clearAllExcludedSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            excludedSettingIds = userPreferences.clearAllExcludedSettings()
            refreshSettingsState()
        }
    }

    fun clearAllHiddenApps() {
        viewModelScope.launch(Dispatchers.IO) {
            hiddenSuggestionPackages = userPreferences.clearAllHiddenAppsInSuggestions()
            hiddenResultPackages = userPreferences.clearAllHiddenAppsInResults()
            refreshDerivedState()
        }
    }

    fun clearAllExclusions() {
        viewModelScope.launch(Dispatchers.IO) {
            excludedContactIds = userPreferences.clearAllExcludedContacts()
            excludedFileUris = userPreferences.clearAllExcludedFiles()
            hiddenSuggestionPackages = userPreferences.clearAllHiddenAppsInSuggestions()
            hiddenResultPackages = userPreferences.clearAllHiddenAppsInResults()
            excludedSettingIds = userPreferences.clearAllExcludedSettings()
            loadExcludedContactsAndFiles()
            refreshSettingsState()
            refreshDerivedState()
        }
    }

    fun pinApp(appInfo: AppInfo) {
        if (hiddenSuggestionPackages.contains(appInfo.packageName)) return
        viewModelScope.launch(Dispatchers.IO) {
            pinnedPackages = userPreferences.pinPackage(appInfo.packageName)
            refreshDerivedState()
        }
    }

    fun unpinApp(appInfo: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            pinnedPackages = userPreferences.unpinPackage(appInfo.packageName)
            refreshDerivedState()
        }
    }

    fun setAppNickname(appInfo: AppInfo, nickname: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setAppNickname(appInfo.packageName, nickname)
            refreshDerivedState()
        }
    }

    fun getAppNickname(packageName: String): String? {
        return userPreferences.getAppNickname(packageName)
    }

    fun setContactNickname(contactInfo: ContactInfo, nickname: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setContactNickname(contactInfo.contactId, nickname)
            if (_uiState.value.query.isNotBlank()) {
                performSecondarySearches(_uiState.value.query)
            }
        }
    }

    fun getContactNickname(contactId: Long): String? {
        return userPreferences.getContactNickname(contactId)
    }

    fun setFileNickname(deviceFile: DeviceFile, nickname: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setFileNickname(deviceFile.uri.toString(), nickname)
            if (_uiState.value.query.isNotBlank()) {
                performSecondarySearches(_uiState.value.query)
            }
        }
    }

    fun getFileNickname(uri: String): String? {
        return userPreferences.getFileNickname(uri)
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

    fun getEnabledSearchEngines(): List<SearchEngine> {
        return searchEngineOrder.filter { it !in disabledSearchEngines }
    }

    fun setSearchEngineEnabled(engine: SearchEngine, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val disabled = disabledSearchEngines.toMutableSet()
            if (enabled) {
                disabled.remove(engine)
            } else {
                disabled.add(engine)
            }
            disabledSearchEngines = disabled
            userPreferences.setDisabledSearchEngines(disabled.map { it.name }.toSet())
            _uiState.update { 
                it.copy(disabledSearchEngines = disabledSearchEngines)
            }
        }
    }

    fun reorderSearchEngines(newOrder: List<SearchEngine>) {
        viewModelScope.launch(Dispatchers.IO) {
            searchEngineOrder = newOrder
            userPreferences.setSearchEngineOrder(newOrder.map { it.name })
            _uiState.update { 
                it.copy(searchEngineOrder = searchEngineOrder)
            }
        }
    }

    fun setSearchEngineSectionEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            searchEngineSectionEnabled = enabled
            userPreferences.setSearchEngineSectionEnabled(enabled)
            _uiState.update { 
                it.copy(searchEngineSectionEnabled = searchEngineSectionEnabled)
            }
        }
    }

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
            geminiClient = normalized?.let { DirectAnswerClient(it) }
            userPreferences.setGeminiApiKey(normalized)

            val hasGemini = !normalized.isNullOrBlank()
            if (!hasGemini) {
                clearDirectAnswerState()
            }
            updateSearchEnginesForGemini(hasGemini)
            if (hasGemini && !hadGemini) {
                clearDirectAnswerState()
            }
            _uiState.update {
                it.copy(
                    hasGeminiApiKey = hasGemini,
                    geminiApiKeyLast4 = normalized?.takeLast(4)
                )
            }
        }
    }

    fun requestDirectAnswer(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            clearDirectAnswerState()
            return
        }

        val client = geminiClient
        if (client == null || geminiApiKey.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    directAnswerState = DirectAnswerState(
                        status = DirectAnswerStatus.Error,
                        errorMessage = getApplication<Application>().getString(R.string.direct_answer_error_no_key),
                        activeQuery = trimmedQuery
                    )
                )
            }
            return
        }

        directAnswerJob?.cancel()
        directAnswerJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    directAnswerState = DirectAnswerState(
                        status = DirectAnswerStatus.Loading,
                        activeQuery = trimmedQuery
                    )
                )
            }

            val result = client.fetchAnswer(trimmedQuery)
            result.onSuccess { answer ->
                _uiState.update { state ->
                    state.copy(
                        directAnswerState = DirectAnswerState(
                            status = DirectAnswerStatus.Success,
                            answer = answer,
                            activeQuery = trimmedQuery
                        )
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                val message = error.message
                    ?: getApplication<Application>().getString(R.string.direct_answer_error_generic)
                _uiState.update { state ->
                    state.copy(
                        directAnswerState = DirectAnswerState(
                            status = DirectAnswerStatus.Error,
                            errorMessage = message,
                            activeQuery = trimmedQuery
                        )
                    )
                }
            }
        }
    }

    fun retryDirectAnswer() {
        val lastQuery = _uiState.value.directAnswerState.activeQuery ?: _uiState.value.query
        if (lastQuery.isNullOrBlank()) return
        requestDirectAnswer(lastQuery)
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
            userPreferences.setShortcutCode(engine, code)
            shortcutCodes = shortcutCodes.toMutableMap().apply { put(engine, code) }
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

    private fun loadSearchEngineOrder(): List<SearchEngine> {
        val savedOrder = userPreferences.getSearchEngineOrder()
        val hasGemini = !geminiApiKey.isNullOrBlank()
        val allEngines = if (hasGemini) {
            SearchEngine.values().toList()
        } else {
            SearchEngine.values().filterNot { it == SearchEngine.DIRECT_ANSWER }
        }
        
        if (savedOrder.isEmpty()) {
            // First time - use default order
            val defaultOrder = listOf(
                SearchEngine.GOOGLE,
                SearchEngine.CHATGPT,
                SearchEngine.PERPLEXITY,
                SearchEngine.YOUTUBE,
                SearchEngine.GOOGLE_PLAY,
                SearchEngine.AMAZON,
                SearchEngine.GROK,
                SearchEngine.AI_MODE,
                SearchEngine.GOOGLE_MAPS,
                SearchEngine.REDDIT
            )
            return applyDirectAnswerAvailability(defaultOrder, hasGemini)
        }
        
        // Merge saved order with any new engines that might have been added
        val savedEngines = savedOrder.mapNotNull { name ->
            SearchEngine.values().find { it.name == name }
        }
        val mergedSaved = applyDirectAnswerAvailability(savedEngines, hasGemini)
        val newEngines = allEngines.filter { it !in mergedSaved }
        return mergedSaved + newEngines
    }

    private fun loadDisabledSearchEngines(): Set<SearchEngine> {
        val disabledNames = userPreferences.getDisabledSearchEngines()
        val hasGemini = !geminiApiKey.isNullOrBlank()
        val disabled = disabledNames.mapNotNull { name ->
            SearchEngine.values().find { it.name == name }
        }.toSet()
        return if (hasGemini) {
            disabled
        } else {
            disabled.filterNot { it == SearchEngine.DIRECT_ANSWER }.toSet()
        }
    }

    private fun applyDirectAnswerAvailability(
        order: List<SearchEngine>,
        hasGemini: Boolean
    ): List<SearchEngine> {
        val hasDirect = order.contains(SearchEngine.DIRECT_ANSWER)
        val withoutDirect = order.filterNot { it == SearchEngine.DIRECT_ANSWER }
        return when {
            hasGemini && hasDirect -> order
            hasGemini -> listOf(SearchEngine.DIRECT_ANSWER) + withoutDirect
            else -> withoutDirect
        }
    }

    private fun updateSearchEnginesForGemini(hasGemini: Boolean) {
        val updatedOrder = applyDirectAnswerAvailability(searchEngineOrder, hasGemini)
        searchEngineOrder = updatedOrder
        if (!hasGemini) {
            disabledSearchEngines = disabledSearchEngines.filterNot { it == SearchEngine.DIRECT_ANSWER }.toSet()
        }
        userPreferences.setSearchEngineOrder(searchEngineOrder.map { it.name })
        userPreferences.setDisabledSearchEngines(disabledSearchEngines.map { it.name }.toSet())
        _uiState.update { state ->
            state.copy(
                searchEngineOrder = searchEngineOrder,
                disabledSearchEngines = disabledSearchEngines,
                hasGeminiApiKey = hasGemini,
                geminiApiKeyLast4 = if (hasGemini) geminiApiKey?.takeLast(4) else null,
                directAnswerState = if (hasGemini) state.directAnswerState else DirectAnswerState()
            )
        }
    }

    private fun clearDirectAnswerState() {
        directAnswerJob?.cancel()
        directAnswerJob = null
        _uiState.update {
            it.copy(directAnswerState = DirectAnswerState())
        }
    }

    private fun loadSectionOrder(): List<SearchSection> {
        val savedOrder = userPreferences.getSectionOrder()
        val defaultOrder = listOf(
            SearchSection.APPS,
            SearchSection.CONTACTS,
            SearchSection.FILES,
            SearchSection.SETTINGS
        )
        
        if (savedOrder.isEmpty()) {
            // First time - use default order: Apps, Contacts, Files
            return defaultOrder
        }
        
        // Merge saved order with any new sections that might have been added
        val savedSections = savedOrder.mapNotNull { name ->
            SearchSection.values().find { it.name == name }
        }
        val newSections = SearchSection.values().filter { it !in savedSections }
        return savedSections + newSections
    }

    // Removed loadDisabledSections - now using permissionManager.computeDisabledSections()

    fun reorderSections(newOrder: List<SearchSection>) {
        viewModelScope.launch(Dispatchers.IO) {
            sectionOrder = newOrder
            userPreferences.setSectionOrder(newOrder.map { it.name })
            _uiState.update { 
                it.copy(sectionOrder = sectionOrder)
            }
        }
    }

    fun setSectionEnabled(section: SearchSection, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (enabled && !permissionManager.canEnableSection(section)) {
                // Permission not granted - don't enable, permission should be requested from UI
                return@launch
            }
            
            disabledSections = if (enabled) {
                permissionManager.enableSection(section, disabledSections)
            } else {
                permissionManager.disableSection(section, disabledSections)
            }
            
            if (!enabled) {
                refreshOptionalPermissions()
            }
            
            _uiState.update { 
                it.copy(disabledSections = disabledSections)
            }
            
            // Re-run search if there's an active query to reflect the change
            val query = _uiState.value.query
            if (query.isNotBlank()) {
                performSecondarySearches(query)
            }
        }
    }
    
    /**
     * Checks if a section can be enabled (i.e., has required permissions).
     */
    fun canEnableSection(section: SearchSection): Boolean {
        return permissionManager.canEnableSection(section)
    }

    private fun deriveMatches(query: String, source: List<AppInfo>): List<AppInfo> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = query.lowercase(Locale.getDefault())
        return source
            .asSequence()
            .filter { app ->
                app.matches(query) || userPreferences.getAppNickname(app.packageName)
                    ?.lowercase(Locale.getDefault())
                    ?.contains(normalizedQuery) == true
            }
            .map { app ->
                val nickname = userPreferences.getAppNickname(app.packageName)
                val priority = when {
                    nickname?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true -> {
                        // Nickname match gets highest priority (1 = EXACT_MATCH)
                        1
                    }
                    else -> {
                        SearchRankingUtils.getBestMatchPriority(
                            query,
                            app.appName,
                            app.packageName
                        )
                    }
                }
                app to priority
            }
            .sortedWith(compareBy({ it.second }, { it.first.appName.lowercase(Locale.getDefault()) }))
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
            .filterNot { excludedSettingIds.contains(it) }
            .toSet()

        return availableSettings
            .asSequence()
            .filterNot { excludedSettingIds.contains(it.id) }
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
    }

    private fun availableApps(apps: List<AppInfo> = cachedApps): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps.filterNot { hiddenSuggestionPackages.contains(it.packageName) }
    }

    private fun searchSourceApps(apps: List<AppInfo> = cachedApps): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps.filterNot {
            hiddenResultPackages.contains(it.packageName) || pinnedPackages.contains(it.packageName)
        }
    }

    private fun computePinnedApps(apps: List<AppInfo>, exclusion: Set<String>): List<AppInfo> {
        if (apps.isEmpty() || pinnedPackages.isEmpty()) return emptyList()
        return apps
            .asSequence()
            .filter { pinnedPackages.contains(it.packageName) && !exclusion.contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
            .toList()
    }

    private fun refreshDerivedState(
        lastUpdated: Long? = null,
        isLoading: Boolean? = null
    ) {
        val apps = cachedApps
        val visibleAppList = availableApps(apps)
        val pinnedAppsForSuggestions = computePinnedApps(apps, hiddenSuggestionPackages)
        val pinnedAppsForResults = computePinnedApps(apps, hiddenResultPackages)
        val recentsSource = visibleAppList.filterNot { pinnedPackages.contains(it.packageName) }
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
            .filter { hiddenSuggestionPackages.contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
        val resultHiddenAppList = apps
            .filter { hiddenResultPackages.contains(it.packageName) }
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
            SearchSection.CONTACTS !in disabledSections
        val canSearchFiles = _uiState.value.hasFilePermission && 
            SearchSection.FILES !in disabledSections
        val currentVersion = ++queryVersion

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            val normalizedQuery = trimmedQuery.lowercase(Locale.getDefault())
            
            // Search by display name (existing behavior)
            val result = searchOperations.performSearches(
                query = trimmedQuery,
                canSearchContacts = canSearchContacts,
                canSearchFiles = canSearchFiles,
                enabledFileTypes = enabledFileTypes,
                excludedContactIds = excludedContactIds,
                excludedFileUris = excludedFileUris,
                scope = this
            )

            // Also search for contacts/files with matching nicknames
            val nicknameMatchingContactIds = if (canSearchContacts) {
                userPreferences.findContactsWithMatchingNickname(trimmedQuery)
                    .filterNot { excludedContactIds.contains(it) }
            } else {
                emptySet()
            }
            
            val nicknameMatchingFileUris = if (canSearchFiles) {
                userPreferences.findFilesWithMatchingNickname(trimmedQuery)
                    .filterNot { excludedFileUris.contains(it) }
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
                            fileType in enabledFileTypes && !excludedFileUris.contains(file.uri.toString())
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
            val filteredContacts = allContacts.filter { contact ->
                val nickname = userPreferences.getContactNickname(contact.contactId)
                contact.displayName.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                    nickname?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true
            }.sortedWith(
                compareBy<ContactInfo> { contact ->
                    val nickname = userPreferences.getContactNickname(contact.contactId)
                    when {
                        nickname?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true -> 0
                        contact.displayName.lowercase(Locale.getDefault()).startsWith(normalizedQuery) -> 1
                        else -> 2
                    }
                }.thenBy { it.displayName.lowercase(Locale.getDefault()) }
            )

            val filteredFiles = allFiles.filter { file ->
                val nickname = userPreferences.getFileNickname(file.uri.toString())
                file.displayName.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                    nickname?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true
            }.sortedWith(
                compareBy<DeviceFile> { file ->
                    val nickname = userPreferences.getFileNickname(file.uri.toString())
                    when {
                        nickname?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true -> 0
                        file.displayName.lowercase(Locale.getDefault()).startsWith(normalizedQuery) -> 1
                        else -> 2
                    }
                }.thenBy { it.displayName.lowercase(Locale.getDefault()) }
            )

            val shouldSearchSettings = SearchSection.SETTINGS !in disabledSections
            val settingsMatches = if (shouldSearchSettings) {
                searchSettings(trimmedQuery)
            } else {
                emptyList()
            }

            withContext(Dispatchers.Main) {
                if (currentVersion == queryVersion) {
                    _uiState.update { state ->
                        state.copy(
                            contactResults = filteredContacts,
                            fileResults = filteredFiles,
                            settingResults = settingsMatches
                        )
                    }
                }
            }
        }
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
            disabledSections = permissionManager.computeDisabledSections()
            
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
                    disabledSections = disabledSections,
                    showWallpaperBackground = if (shouldUpdateWallpaper) showWallpaperBackground else state.showWallpaperBackground
                )
            }
        }
        return changed
    }

    fun openContact(contactInfo: ContactInfo) {
        IntentHelpers.openContact(getApplication(), contactInfo)
    }

    fun openEmail(email: String) {
        IntentHelpers.composeEmail(getApplication(), email)
    }

    fun callContact(contactInfo: ContactInfo) {
        val context = getApplication<Application>()
        if (contactInfo.phoneNumbers.isEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.error_missing_phone_number),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Check if there's a preferred number stored
        val preferredNumber = userPreferences.getPreferredPhoneNumber(contactInfo.contactId)
        if (preferredNumber != null && contactInfo.phoneNumbers.contains(preferredNumber)) {
            // Use preferred number directly
            performCall(preferredNumber)
            return
        }
        
        // If multiple numbers, show selection dialog
        if (contactInfo.phoneNumbers.size > 1) {
            _uiState.update { it.copy(phoneNumberSelection = PhoneNumberSelection(contactInfo, isCall = true)) }
            return
        }
        
        // Single number, use it directly
        performCall(contactInfo.phoneNumbers.first())
    }

    fun smsContact(contactInfo: ContactInfo) {
        val context = getApplication<Application>()
        if (contactInfo.phoneNumbers.isEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.error_missing_phone_number),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Check if there's a preferred number stored
        val preferredNumber = userPreferences.getPreferredPhoneNumber(contactInfo.contactId)
        if (preferredNumber != null && contactInfo.phoneNumbers.contains(preferredNumber)) {
            // Use preferred number directly
            performMessaging(preferredNumber)
            return
        }
        
        // If multiple numbers, show selection dialog
        if (contactInfo.phoneNumbers.size > 1) {
            _uiState.update { it.copy(phoneNumberSelection = PhoneNumberSelection(contactInfo, isCall = false)) }
            return
        }
        
        // Single number, use it directly
        performMessaging(contactInfo.phoneNumbers.first())
    }
    
    fun onPhoneNumberSelected(phoneNumber: String, rememberChoice: Boolean) {
        val selection = _uiState.value.phoneNumberSelection ?: return
        val contactInfo = selection.contactInfo
        
        // Store preference if requested
        if (rememberChoice) {
            userPreferences.setPreferredPhoneNumber(contactInfo.contactId, phoneNumber)
        }
        
        // Perform the action
        if (selection.isCall) {
            performCall(phoneNumber)
        } else {
            performMessaging(phoneNumber)
        }
        
        // Clear the selection dialog
        _uiState.update { it.copy(phoneNumberSelection = null) }
    }
    
    fun dismissPhoneNumberSelection() {
        _uiState.update { it.copy(phoneNumberSelection = null) }
    }
    
    private fun performCall(number: String) {
        IntentHelpers.performCall(getApplication(), number)
    }
    
    private fun performSms(number: String) {
        IntentHelpers.performSms(getApplication(), number)
    }

    private fun performMessaging(number: String) {
        when (messagingApp) {
            MessagingApp.MESSAGES -> performSms(number)
            MessagingApp.WHATSAPP -> IntentHelpers.openWhatsAppChat(getApplication(), number)
            MessagingApp.TELEGRAM -> IntentHelpers.openTelegramChat(getApplication(), number)
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        directAnswerJob?.cancel()
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
    }
}

