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
    YOUTUBE
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
    val indexedAppCount: Int = 0,
    val cacheLastUpdatedMillis: Long = 0L,
    val errorMessage: String? = null,
    val showAppLabels: Boolean = true,
    val searchEngineOrder: List<SearchEngine> = emptyList(),
    val disabledSearchEngines: Set<SearchEngine> = emptySet(),
    val phoneNumberSelection: PhoneNumberSelection? = null
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
    private var showAppLabels: Boolean = userPreferences.shouldShowAppLabels()
    private var searchEngineOrder: List<SearchEngine> = loadSearchEngineOrder()
    private var disabledSearchEngines: Set<SearchEngine> = loadDisabledSearchEngines()
    private var searchJob: Job? = null
    private var queryVersion: Long = 0L

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { 
            it.copy(
                showAppLabels = showAppLabels,
                searchEngineOrder = searchEngineOrder,
                disabledSearchEngines = disabledSearchEngines
            )
        }
        refreshUsageAccess()
        refreshOptionalPermissions()
        loadApps()
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
        }
    }


    private fun deriveMatches(query: String, source: List<AppInfo>): List<AppInfo> {
        if (query.isBlank()) return emptyList()
        return source
            .asSequence()
            .filter { it.matches(query) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
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
            deriveMatches(query, searchSourceApps(apps))
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

        val canSearchContacts = _uiState.value.hasContactPermission
        val canSearchFiles = _uiState.value.hasFilePermission
        val currentVersion = ++queryVersion

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            val contactsDeferred = async {
                if (canSearchContacts) {
                    contactRepository.searchContacts(query, CONTACT_RESULT_LIMIT)
                } else {
                    emptyList()
                }
            }
            val filesDeferred = async {
                if (canSearchFiles) {
                    fileRepository.searchFiles(query, FILE_RESULT_LIMIT)
                } else {
                    emptyList()
                }
            }
            val contactResults = contactsDeferred.await()
            val fileResults = filesDeferred.await()

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
            performSms(preferredNumber)
            return
        }
        
        // If multiple numbers, show selection dialog
        if (contactInfo.phoneNumbers.size > 1) {
            _uiState.update { it.copy(phoneNumberSelection = PhoneNumberSelection(contactInfo, isCall = false)) }
            return
        }
        
        // Single number, use it directly
        performSms(contactInfo.phoneNumbers.first())
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
            performSms(phoneNumber)
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

    fun openFile(deviceFile: DeviceFile) {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(deviceFile.uri, deviceFile.mimeType ?: "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.error_open_file, deviceFile.displayName),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

