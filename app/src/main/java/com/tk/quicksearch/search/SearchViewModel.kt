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
import java.util.Locale

data class SearchUiState(
    val query: String = "",
    val hasUsagePermission: Boolean = false,
    val isLoading: Boolean = true,
    val recentApps: List<AppInfo> = emptyList(),
    val searchResults: List<AppInfo> = emptyList(),
    val errorMessage: String? = null
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppUsageRepository(application.applicationContext)
    private var cachedApps: List<AppInfo> = emptyList()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        refreshUsageAccess()
        refreshApps()
    }

    fun refreshUsageAccess() {
        _uiState.update { it.copy(hasUsagePermission = repository.hasUsageAccess()) }
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadLaunchableApps() }
                .onSuccess { apps ->
                    cachedApps = apps
                    _uiState.update { state ->
                        state.copy(
                            recentApps = repository.extractRecentApps(apps, GRID_ITEM_COUNT),
                            searchResults = deriveMatches(state.query, apps),
                            isLoading = false
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
        _uiState.update { state ->
            state.copy(
                query = newQuery,
                searchResults = deriveMatches(newQuery, cachedApps)
            )
        }
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

