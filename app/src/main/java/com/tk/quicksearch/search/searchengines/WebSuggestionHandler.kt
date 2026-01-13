package com.tk.quicksearch.search.searchEngines

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.util.WebSuggestionsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WebSuggestionHandler(
    private val scope: CoroutineScope,
    private val userPreferences: UserAppPreferences,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit
) {
    private val webSuggestionsCount: Int
        get() = userPreferences.getWebSuggestionsCount()
    private var webSuggestionsJob: Job? = null
    var isEnabled: Boolean = userPreferences.areWebSuggestionsEnabled()
        private set

    fun setEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setWebSuggestionsEnabled(enabled)
            isEnabled = enabled

            // Update UI state
            uiStateUpdater { it.copy(webSuggestionsEnabled = enabled) }

            // If disabling web suggestions, clear any existing suggestions
            if (!enabled) {
                uiStateUpdater { it.copy(webSuggestions = emptyList()) }
            }
        }
    }

    fun fetchWebSuggestions(query: String, currentQueryVersion: Long, activeQueryVersionProvider: () -> Long, activeQueryProvider: () -> String) {
        webSuggestionsJob?.cancel()
        webSuggestionsJob = scope.launch(Dispatchers.IO) {
            try {
                // Add delay to prevent immediate API calls while user is typing
                delay(300L)
                // Check if query version still matches after delay
                if (activeQueryVersionProvider() != currentQueryVersion ||
                    activeQueryProvider().trim() != query.trim()) {
                    return@launch
                }

                val suggestions = WebSuggestionsUtils.getSuggestions(query)
                withContext(Dispatchers.Main) {
                    // Only update if query hasn't changed
                    if (activeQueryVersionProvider() == currentQueryVersion && 
                        activeQueryProvider().trim() == query.trim()) {
                        uiStateUpdater { state ->
                            state.copy(webSuggestions = suggestions.take(webSuggestionsCount))
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail - don't show suggestions on error
            }
        }
    }

    fun cancelSuggestions() {
        webSuggestionsJob?.cancel()
    }
}
