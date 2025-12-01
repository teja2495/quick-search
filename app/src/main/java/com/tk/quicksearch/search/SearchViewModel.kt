package com.tk.quicksearch.search

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.provider.ContactsContract
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
import com.tk.quicksearch.model.FileTypeUtils
import com.tk.quicksearch.model.matches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
    val useWhatsAppForMessages: Boolean = false,
    val showSectionTitles: Boolean = true
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppUsageRepository(application.applicationContext)
    private val contactRepository = ContactRepository(application.applicationContext)
    private val fileRepository = FileSearchRepository(application.applicationContext)
    private val userPreferences = UserAppPreferences(application.applicationContext)
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
    private var useWhatsAppForMessages: Boolean = userPreferences.useWhatsAppForMessages()
    private var showSectionTitles: Boolean = userPreferences.shouldShowSectionTitles()
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
                useWhatsAppForMessages = useWhatsAppForMessages,
                showSectionTitles = showSectionTitles
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

    fun setUseWhatsAppForMessages(useWhatsApp: Boolean) {
        useWhatsAppForMessages = useWhatsApp
        userPreferences.setUseWhatsAppForMessages(useWhatsApp)
        _uiState.update { state ->
            state.copy(useWhatsAppForMessages = useWhatsAppForMessages)
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
        if (userPreferences.areShortcutsEnabled()) {
            val shortcutMatch = detectShortcut(newQuery)
            if (shortcutMatch != null) {
                val (queryWithoutShortcut, engine) = shortcutMatch
                // Automatically perform search with the detected engine
                openSearchUrl(queryWithoutShortcut.trim(), engine)
                // Clear the query after performing search
                clearQuery()
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
            val hasContactsPermission = contactRepository.hasPermission()
            val hasFilesPermission = fileRepository.hasPermission()

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
            val hasContactsPermission = contactRepository.hasPermission()
            val hasFilesPermission = fileRepository.hasPermission()

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
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAppSettings() {
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAllFilesAccessSettings() {
        val context = getApplication<Application>()
        val manageIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(manageIntent)
        }.onFailure {
            val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun launchApp(appInfo: AppInfo) {
        val context = getApplication<Application>()
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(appInfo.packageName)

        if (launchIntent == null) {
            Toast.makeText(
                context,
                context.getString(R.string.error_launch_app, appInfo.appName),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(launchIntent)
    }

    fun openAppInfo(appInfo: AppInfo) {
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${appInfo.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun requestUninstall(appInfo: AppInfo) {
        val context = getApplication<Application>()
        val packageName = appInfo.packageName
        if (packageName == context.packageName) {
            Toast.makeText(
                context,
                context.getString(R.string.error_uninstall_self),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to uninstall ${appInfo.appName}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun openSearchUrl(query: String, searchEngine: SearchEngine) {
        val context = getApplication<Application>()
        val searchUrl = buildSearchUrl(query, searchEngine)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
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
            return allEngines
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

    private fun buildSearchUrl(query: String, searchEngine: SearchEngine): String {
        val encodedQuery = Uri.encode(query)
        return when (searchEngine) {
            SearchEngine.GOOGLE -> "https://www.google.com/search?q=$encodedQuery"
            SearchEngine.CHATGPT -> "https://chatgpt.com/?prompt=$encodedQuery"
            SearchEngine.PERPLEXITY -> "https://www.perplexity.ai/search?q=$encodedQuery"
            SearchEngine.GROK -> "https://grok.com/?q=$encodedQuery"
            SearchEngine.GOOGLE_MAPS -> "http://maps.google.com/?q=$encodedQuery"
            SearchEngine.GOOGLE_PLAY -> "https://play.google.com/store/search?q=$encodedQuery&c=apps"
            SearchEngine.REDDIT -> "https://www.reddit.com/search/?q=$encodedQuery"
            SearchEngine.YOUTUBE -> "https://www.youtube.com/results?search_query=$encodedQuery"
            SearchEngine.AMAZON -> "https://www.amazon.com/s?k=$encodedQuery"
            SearchEngine.AI_MODE -> "https://www.google.com/search?q=$encodedQuery&udm=50"
        }
    }


    private fun deriveMatches(query: String, source: List<AppInfo>): List<AppInfo> {
        if (query.isBlank()) return emptyList()
        return source
            .asSequence()
            .filter { it.matches(query) }
            .mapNotNull { app ->
                val priority = com.tk.quicksearch.util.SearchRankingUtils.getBestMatchPriority(
                    query,
                    app.appName,
                    app.packageName
                )
                if (com.tk.quicksearch.util.SearchRankingUtils.isOtherMatch(priority)) {
                    null
                } else {
                    app to priority
                }
            }
            .sortedWith(compareBy({ it.second }, { it.first.appName.lowercase(Locale.getDefault()) }))
            .map { it.first }
            .take(GRID_ITEM_COUNT)
            .toList()
    }

    companion object {
        private const val GRID_ITEM_COUNT = 10
        private const val CONTACT_RESULT_LIMIT = 5
        private const val FILE_RESULT_LIMIT = 5
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
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    contactResults = emptyList(),
                    fileResults = emptyList()
                )
            }
            return
        }

        // Avoid running contact/file searches for single-character queries
        if (query.length == 1) {
            _uiState.update {
                it.copy(
                    contactResults = emptyList(),
                    fileResults = emptyList()
                )
            }
            return
        }

        val canSearchContacts = _uiState.value.hasContactPermission
        val canSearchFiles = _uiState.value.hasFilePermission
        val currentVersion = ++queryVersion

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            val contactsDeferred = async {
                if (canSearchContacts) {
                    // Start with a small, UI-friendly limit
                    contactRepository.searchContacts(query, CONTACT_RESULT_LIMIT)
                        .filterNot { excludedContactIds.contains(it.contactId) }
                } else {
                    emptyList()
                }
            }
            val filesDeferred = async {
                if (canSearchFiles) {
                    // Start with a small limit, we may widen it later
                    val allFiles = fileRepository.searchFiles(query, FILE_RESULT_LIMIT * 2)
                    // Filter files based on enabled file types and keep only top N initially
                    allFiles.filter { file ->
                        val fileType = FileTypeUtils.getFileType(file)
                        fileType in enabledFileTypes && !excludedFileUris.contains(file.uri.toString())
                    }.take(FILE_RESULT_LIMIT)
                } else {
                    emptyList()
                }
            }

            var contactResults = contactsDeferred.await()
            var fileResults = filesDeferred.await()

            // If only one type has results, fetch all results for that type
            if (canSearchContacts && contactResults.isNotEmpty() && !canSearchFiles) {
                contactResults = contactRepository.searchContacts(query, Int.MAX_VALUE)
                    .filterNot { excludedContactIds.contains(it.contactId) }
            } else if (canSearchFiles && fileResults.isNotEmpty() && !canSearchContacts) {
                val allFiles = fileRepository.searchFiles(query, Int.MAX_VALUE)
                fileResults = allFiles.filter { file ->
                    val fileType = FileTypeUtils.getFileType(file)
                    fileType in enabledFileTypes && !excludedFileUris.contains(file.uri.toString())
                }
            } else if (canSearchContacts && canSearchFiles) {
                // Both searches are enabled; widen the one that is present if the other is empty
                if (contactResults.isEmpty() && fileResults.isNotEmpty()) {
                    val allFiles = fileRepository.searchFiles(query, Int.MAX_VALUE)
                    fileResults = allFiles.filter { file ->
                        val fileType = FileTypeUtils.getFileType(file)
                        fileType in enabledFileTypes && !excludedFileUris.contains(file.uri.toString())
                    }
                } else if (fileResults.isEmpty() && contactResults.isNotEmpty()) {
                    contactResults = contactRepository.searchContacts(query, Int.MAX_VALUE)
                        .filterNot { excludedContactIds.contains(it.contactId) }
                }
            }

            withContext(Dispatchers.Main) {
                if (currentVersion == queryVersion) {
                    _uiState.update { state ->
                        state.copy(
                            contactResults = contactResults,
                            fileResults = fileResults
                        )
                    }
                }
            }
        }
    }

    private fun refreshOptionalPermissions(): Boolean {
        val hasContacts = contactRepository.hasPermission()
        val hasFiles = fileRepository.hasPermission()
        val previousState = _uiState.value
        val changed =
            previousState.hasContactPermission != hasContacts || previousState.hasFilePermission != hasFiles

        if (changed) {
            _uiState.update { state ->
                state.copy(
                    hasContactPermission = hasContacts,
                    hasFilePermission = hasFiles,
                    contactResults = if (hasContacts) state.contactResults else emptyList(),
                    fileResults = if (hasFiles) state.fileResults else emptyList()
                )
            }
        }
        return changed
    }

    fun openContact(contactInfo: ContactInfo) {
        val context = getApplication<Application>()
        val lookupUri = ContactsContract.Contacts.getLookupUri(contactInfo.contactId, contactInfo.lookupKey)
        if (lookupUri == null) {
            Toast.makeText(
                context,
                context.getString(R.string.error_open_contact),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, lookupUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
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
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(number)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    private fun performSms(number: String) {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(number)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun performMessaging(number: String) {
        if (useWhatsAppForMessages) {
            openWhatsAppChat(number)
        } else {
            performSms(number)
        }
    }

    private fun openWhatsAppChat(phoneNumber: String) {
        if (phoneNumber.isBlank()) return

        val context = getApplication<Application>()
        val uri = Uri.parse("https://wa.me/${Uri.encode(phoneNumber)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            // Prefer the standard WhatsApp package if available
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback: try without explicit package (e.g. WhatsApp Business or browser)
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (inner: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_missing_phone_number),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun openFile(deviceFile: DeviceFile) {
        val context = getApplication<Application>()

        // Normalise APK handling so installers can recognise the file correctly
        val isApk = isApkFile(deviceFile)
        val mimeType = if (isApk) {
            // Always force the canonical APK MIME type
            "application/vnd.android.package-archive"
        } else {
            deviceFile.mimeType ?: "*/*"
        }

        // Primary intent: generic VIEW, which most installers handle
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(deviceFile.uri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(viewIntent)
        } catch (exception: ActivityNotFoundException) {
            // If no VIEW handler is found for APKs, explicitly try the package installer
            if (isApk) {
                val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    setDataAndType(deviceFile.uri, mimeType)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                try {
                    context.startActivity(installIntent)
                    return
                } catch (_: Exception) {
                    // Fall through to generic error toast below
                }
            }
            Toast.makeText(
                context,
                context.getString(R.string.error_open_file, deviceFile.displayName),
                Toast.LENGTH_SHORT
            ).show()
        } catch (exception: SecurityException) {
            // Covers cases where the system blocks installing APKs from this app
            Toast.makeText(
                context,
                context.getString(R.string.error_open_file, deviceFile.displayName),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Best-effort detection of APK files, even when MIME type is missing or incorrect.
     */
    private fun isApkFile(deviceFile: DeviceFile): Boolean {
        val mime = deviceFile.mimeType?.lowercase(Locale.getDefault())
        if (mime == "application/vnd.android.package-archive") {
            return true
        }

        val name = deviceFile.displayName.lowercase(Locale.getDefault())
        return name.endsWith(".apk")
    }
}

