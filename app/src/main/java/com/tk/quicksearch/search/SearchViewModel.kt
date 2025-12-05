package com.tk.quicksearch.search

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.quicksearch.R
import com.tk.quicksearch.data.AppUsageRepository
import com.tk.quicksearch.data.ContactRepository
import com.tk.quicksearch.data.FileSearchRepository
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.model.matches
import com.tk.quicksearch.util.SearchRankingUtils
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
    FILES
}

enum class MessagingApp {
    MESSAGES,
    WHATSAPP,
    TELEGRAM
}

data class PhoneNumberSelection(
    val contactInfo: ContactInfo,
    val isCall: Boolean // true for call, false for SMS
)

data class SearchUiState(
    val query: String = "",
    val hasUsagePermission: Boolean = false,
    val hasContactPermission: Boolean = false,
    val hasFilePermission: Boolean = false,
    val isLoading: Boolean = true,
    val recentApps: List<AppInfo> = emptyList(),
    val searchResults: List<AppInfo> = emptyList(),
    val pinnedApps: List<AppInfo> = emptyList(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val contactResults: List<ContactInfo> = emptyList(),
    val fileResults: List<DeviceFile> = emptyList(),
    val pinnedContacts: List<ContactInfo> = emptyList(),
    val pinnedFiles: List<DeviceFile> = emptyList(),
    val excludedContacts: List<ContactInfo> = emptyList(),
    val excludedFiles: List<DeviceFile> = emptyList(),
    val indexedAppCount: Int = 0,
    val cacheLastUpdatedMillis: Long = 0L,
    val errorMessage: String? = null,
    val showAppLabels: Boolean = true,
    val searchEngineOrder: List<SearchEngine> = emptyList(),
    val disabledSearchEngines: Set<SearchEngine> = emptySet(),
    val phoneNumberSelection: PhoneNumberSelection? = null,
    val enabledFileTypes: Set<FileType> = FileType.values().toSet(),
    val keyboardAlignedLayout: Boolean = true,
    val shortcutsEnabled: Boolean = true,
    val shortcutCodes: Map<SearchEngine, String> = emptyMap(),
    val shortcutEnabled: Map<SearchEngine, Boolean> = emptyMap(),
    val messagingApp: MessagingApp = MessagingApp.MESSAGES,
    val showSectionTitles: Boolean = true,
    val showWallpaperBackground: Boolean = true,
    val sectionOrder: List<SearchSection> = emptyList(),
    val disabledSections: Set<SearchSection> = emptySet(),
    val searchEngineSectionEnabled: Boolean = true,
    val amazonDomain: String? = null
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppUsageRepository(application.applicationContext)
    private val contactRepository = ContactRepository(application.applicationContext)
    private val fileRepository = FileSearchRepository(application.applicationContext)
    private val userPreferences = UserAppPreferences(application.applicationContext)
    private val permissionManager = PermissionManager(contactRepository, fileRepository, userPreferences)
    private val searchOperations = SearchOperations(contactRepository, fileRepository)
    private var cachedApps: List<AppInfo> = emptyList()
    private var noMatchPrefix: String? = null
    private var hiddenPackages: Set<String> = userPreferences.getHiddenPackages()
    private var pinnedPackages: Set<String> = userPreferences.getPinnedPackages()
    private var pinnedContactIds: Set<Long> = userPreferences.getPinnedContactIds()
    private var excludedContactIds: Set<Long> = userPreferences.getExcludedContactIds()
    private var pinnedFileUris: Set<String> = userPreferences.getPinnedFileUris()
    private var excludedFileUris: Set<String> = userPreferences.getExcludedFileUris()
    private var showAppLabels: Boolean = userPreferences.shouldShowAppLabels()
    private var searchEngineOrder: List<SearchEngine> = loadSearchEngineOrder()
    private var disabledSearchEngines: Set<SearchEngine> = loadDisabledSearchEngines()
    private var enabledFileTypes: Set<FileType> = userPreferences.getEnabledFileTypes()
    private var keyboardAlignedLayout: Boolean = userPreferences.isKeyboardAlignedLayout()
    private var shortcutsEnabled: Boolean = userPreferences.areShortcutsEnabled()
    private var shortcutCodes: Map<SearchEngine, String> = userPreferences.getAllShortcutCodes()
    private var shortcutEnabled: Map<SearchEngine, Boolean> = SearchEngine.values().associateWith { 
        userPreferences.isShortcutEnabled(it) 
    }
    private var messagingApp: MessagingApp = userPreferences.getMessagingApp()
    private var showSectionTitles: Boolean = userPreferences.shouldShowSectionTitles()
    private var showWallpaperBackground: Boolean = userPreferences.shouldShowWallpaperBackground()
    private var sectionOrder: List<SearchSection> = loadSectionOrder()
    private var disabledSections: Set<SearchSection> = permissionManager.computeDisabledSections()
    private var searchEngineSectionEnabled: Boolean = userPreferences.isSearchEngineSectionEnabled()
    private var amazonDomain: String? = userPreferences.getAmazonDomain()
    private var searchJob: Job? = null
    private var queryVersion: Long = 0L

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { 
            it.copy(
                showAppLabels = showAppLabels,
                searchEngineOrder = searchEngineOrder,
                disabledSearchEngines = disabledSearchEngines,
                enabledFileTypes = enabledFileTypes,
                keyboardAlignedLayout = keyboardAlignedLayout,
                shortcutsEnabled = shortcutsEnabled,
                shortcutCodes = shortcutCodes,
                shortcutEnabled = shortcutEnabled,
                messagingApp = messagingApp,
                showSectionTitles = showSectionTitles,
                showWallpaperBackground = showWallpaperBackground,
                sectionOrder = sectionOrder,
                disabledSections = disabledSections,
                searchEngineSectionEnabled = searchEngineSectionEnabled,
                amazonDomain = amazonDomain
            )
        }
        refreshUsageAccess()
        refreshOptionalPermissions()
        loadApps()
        loadPinnedContactsAndFiles()
        loadExcludedContactsAndFiles()
    }

    /**
     * Loads apps from cache first for instant display, then refreshes in background.
     */
    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            // Try to load from cache first
            val cachedApps = repository.loadCachedApps()
            if (cachedApps != null && cachedApps.isNotEmpty()) {
                // Show cached apps immediately
                this@SearchViewModel.cachedApps = cachedApps
                noMatchPrefix = null
                val lastUpdated = repository.cacheLastUpdatedMillis()
                refreshDerivedState(
                    lastUpdated = lastUpdated,
                    isLoading = false
                )
            }
            
            // Then refresh in background
            refreshApps()
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

    fun setMessagingApp(app: MessagingApp) {
        messagingApp = app
        userPreferences.setMessagingApp(app)
        _uiState.update { state ->
            state.copy(messagingApp = messagingApp)
        }
    }

    fun onQueryChange(newQuery: String) {
        if (newQuery.isBlank()) {
            noMatchPrefix = null
            searchJob?.cancel()
            _uiState.update { 
                it.copy(
                    query = "",
                    searchResults = emptyList(),
                    contactResults = emptyList(),
                    fileResults = emptyList()
                ) 
            }
            return
        }

        // Check for shortcuts if enabled
        if (shortcutsEnabled) {
            val shortcutMatch = detectShortcut(newQuery)
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

        val normalizedQuery = newQuery.lowercase(Locale.getDefault())
        resetNoMatchPrefixIfNeeded(normalizedQuery)

        val shouldSkipSearch = shouldSkipDueToNoMatchPrefix(normalizedQuery)
        val matches = if (shouldSkipSearch) {
            emptyList()
        } else {
            deriveMatches(newQuery, searchSourceApps()).also { results ->
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
        performSecondarySearches(newQuery.trim())
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
        val amazonDomain = if (searchEngine == SearchEngine.AMAZON) {
            userPreferences.getAmazonDomain()
        } else {
            null
        }
        IntentHelpers.openSearchUrl(getApplication(), query, searchEngine, amazonDomain)
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
                    hiddenApps = emptyList(),
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

    fun hideApp(appInfo: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            hiddenPackages = userPreferences.hidePackage(appInfo.packageName)
            if (pinnedPackages.contains(appInfo.packageName)) {
                pinnedPackages = userPreferences.unpinPackage(appInfo.packageName)
            }
            refreshDerivedState()
        }
    }

    fun unhideApp(appInfo: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            hiddenPackages = userPreferences.unhidePackage(appInfo.packageName)
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

    fun clearAllHiddenApps() {
        viewModelScope.launch(Dispatchers.IO) {
            hiddenPackages = userPreferences.clearAllHiddenApps()
            refreshDerivedState()
        }
    }

    fun clearAllExclusions() {
        viewModelScope.launch(Dispatchers.IO) {
            excludedContactIds = userPreferences.clearAllExcludedContacts()
            excludedFileUris = userPreferences.clearAllExcludedFiles()
            hiddenPackages = userPreferences.clearAllHiddenApps()
            loadExcludedContactsAndFiles()
            refreshDerivedState()
        }
    }

    fun pinApp(appInfo: AppInfo) {
        if (hiddenPackages.contains(appInfo.packageName)) return
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

    fun setShowAppLabels(showLabels: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setShowAppLabels(showLabels)
            showAppLabels = showLabels
            _uiState.update { it.copy(showAppLabels = showLabels) }
        }
    }

    fun setShowSectionTitles(showTitles: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setShowSectionTitles(showTitles)
            showSectionTitles = showTitles
            _uiState.update { it.copy(showSectionTitles = showTitles) }
        }
    }

    fun setShowWallpaperBackground(showWallpaper: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
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

    fun setShortcutsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.setShortcutsEnabled(enabled)
            shortcutsEnabled = enabled
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
        val allEngines = SearchEngine.values().toList()
        
        if (savedOrder.isEmpty()) {
            // First time - use default order
            return listOf(
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
        }
        
        // Merge saved order with any new engines that might have been added
        val savedEngines = savedOrder.mapNotNull { name ->
            SearchEngine.values().find { it.name == name }
        }
        val newEngines = allEngines.filter { it !in savedEngines }
        return savedEngines + newEngines
    }

    private fun loadDisabledSearchEngines(): Set<SearchEngine> {
        val disabledNames = userPreferences.getDisabledSearchEngines()
        return disabledNames.mapNotNull { name ->
            SearchEngine.values().find { it.name == name }
        }.toSet()
    }

    private fun loadSectionOrder(): List<SearchSection> {
        val savedOrder = userPreferences.getSectionOrder()
        val defaultOrder = listOf(SearchSection.APPS, SearchSection.CONTACTS, SearchSection.FILES)
        
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

    companion object {
        private const val GRID_ITEM_COUNT = 10
    }

    private fun availableApps(apps: List<AppInfo> = cachedApps): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps.filterNot { hiddenPackages.contains(it.packageName) }
    }

    private fun searchSourceApps(apps: List<AppInfo> = cachedApps): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps.filterNot {
            hiddenPackages.contains(it.packageName) || pinnedPackages.contains(it.packageName)
        }
    }

    private fun computePinnedApps(apps: List<AppInfo>): List<AppInfo> {
        if (apps.isEmpty() || pinnedPackages.isEmpty()) return emptyList()
        return apps
            .asSequence()
            .filter { pinnedPackages.contains(it.packageName) && !hiddenPackages.contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
            .toList()
    }

    private fun refreshDerivedState(
        lastUpdated: Long? = null,
        isLoading: Boolean? = null
    ) {
        val apps = cachedApps
        val visibleAppList = availableApps(apps)
        val pinnedAppList = computePinnedApps(apps)
        val recentsSource = visibleAppList.filterNot { pinnedPackages.contains(it.packageName) }
        val recents = repository.extractRecentApps(recentsSource, GRID_ITEM_COUNT)
        val query = _uiState.value.query
        val searchResults = if (query.isBlank()) {
            emptyList()
        } else {
            // Include both pinned and non-pinned apps in search, let ranking determine order
            val nonPinnedApps = searchSourceApps(apps)
            val allSearchableApps = (pinnedAppList + nonPinnedApps).distinctBy { it.packageName }
            deriveMatches(query, allSearchableApps)
        }
        val hiddenAppList = apps
            .filter { hiddenPackages.contains(it.packageName) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }

        _uiState.update { state ->
            state.copy(
                recentApps = recents,
                searchResults = searchResults,
                pinnedApps = pinnedAppList,
                hiddenApps = hiddenAppList,
                indexedAppCount = visibleAppList.size,
                cacheLastUpdatedMillis = lastUpdated ?: state.cacheLastUpdatedMillis,
                isLoading = isLoading ?: state.isLoading
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
        if (query.isBlank() || query.length == 1) {
            _uiState.update {
                it.copy(
                    contactResults = emptyList(),
                    fileResults = emptyList()
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
            val normalizedQuery = query.lowercase(Locale.getDefault())
            
            // Search by display name (existing behavior)
            val result = searchOperations.performSearches(
                query = query,
                canSearchContacts = canSearchContacts,
                canSearchFiles = canSearchFiles,
                enabledFileTypes = enabledFileTypes,
                excludedContactIds = excludedContactIds,
                excludedFileUris = excludedFileUris,
                scope = this
            )

            // Also search for contacts/files with matching nicknames
            val nicknameMatchingContactIds = if (canSearchContacts) {
                userPreferences.findContactsWithMatchingNickname(query)
                    .filterNot { excludedContactIds.contains(it) }
            } else {
                emptySet()
            }
            
            val nicknameMatchingFileUris = if (canSearchFiles) {
                userPreferences.findFilesWithMatchingNickname(query)
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

            withContext(Dispatchers.Main) {
                if (currentVersion == queryVersion) {
                    _uiState.update { state ->
                        state.copy(
                            contactResults = filteredContacts,
                            fileResults = filteredFiles
                        )
                    }
                }
            }
        }
    }

    private fun refreshOptionalPermissions(): Boolean {
        val hasContacts = permissionManager.hasContactPermission()
        val hasFiles = permissionManager.hasFilePermission()
        val previousState = _uiState.value
        val changed =
            previousState.hasContactPermission != hasContacts || previousState.hasFilePermission != hasFiles

        if (changed) {
            disabledSections = permissionManager.computeDisabledSections()
            
            _uiState.update { state ->
                state.copy(
                    hasContactPermission = hasContacts,
                    hasFilePermission = hasFiles,
                    contactResults = if (hasContacts) state.contactResults else emptyList(),
                    fileResults = if (hasFiles) state.fileResults else emptyList(),
                    disabledSections = disabledSections
                )
            }
        }
        return changed
    }

    fun openContact(contactInfo: ContactInfo) {
        IntentHelpers.openContact(getApplication(), contactInfo)
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

    fun openFile(deviceFile: DeviceFile) {
        IntentHelpers.openFile(getApplication(), deviceFile)
    }
}

