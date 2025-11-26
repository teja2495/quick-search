package com.tk.quicksearch.search

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.quicksearch.R
import com.tk.quicksearch.data.AppUsageRepository
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.matches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class SearchUiState(
    val query: String = "",
    val hasUsagePermission: Boolean = false,
    val isLoading: Boolean = true,
    val recentApps: List<AppInfo> = emptyList(),
    val searchResults: List<AppInfo> = emptyList(),
    val indexedAppCount: Int = 0,
    val cacheLastUpdatedMillis: Long = 0L,
    val errorMessage: String? = null
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppUsageRepository(application.applicationContext)
    private var cachedApps: List<AppInfo> = emptyList()
    private var noMatchPrefix: String? = null

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        refreshUsageAccess()
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
                _uiState.update { state ->
                    state.copy(
                        recentApps = repository.extractRecentApps(cachedApps, GRID_ITEM_COUNT),
                        searchResults = deriveMatches(state.query, cachedApps),
                        isLoading = false,
                        indexedAppCount = cachedApps.size,
                        cacheLastUpdatedMillis = lastUpdated
                    )
                }
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
                    _uiState.update { state ->
                        state.copy(
                            recentApps = repository.extractRecentApps(apps, GRID_ITEM_COUNT),
                            searchResults = deriveMatches(state.query, apps),
                            isLoading = false,
                            indexedAppCount = apps.size,
                            cacheLastUpdatedMillis = lastUpdated
                        )
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
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        if (newQuery.isBlank()) {
            noMatchPrefix = null
            _uiState.update { it.copy(query = "", searchResults = emptyList()) }
            return
        }

        val normalizedQuery = newQuery.lowercase(Locale.getDefault())
        resetNoMatchPrefixIfNeeded(normalizedQuery)

        val shouldSkipSearch = shouldSkipDueToNoMatchPrefix(normalizedQuery)
        val matches = if (shouldSkipSearch) {
            emptyList()
        } else {
            deriveMatches(newQuery, cachedApps).also { results ->
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

    private fun buildSearchUrl(query: String, searchEngine: SearchEngine): String {
        val encodedQuery = Uri.encode(query)
        return when (searchEngine) {
            SearchEngine.GOOGLE -> "https://www.google.com/search?q=$encodedQuery"
            SearchEngine.CHATGPT -> "https://chatgpt.com/?prompt=$encodedQuery"
            SearchEngine.PERPLEXITY -> "https://www.perplexity.ai/search?q=$encodedQuery"
        }
    }

    enum class SearchEngine {
        GOOGLE,
        CHATGPT,
        PERPLEXITY
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
    }
}

