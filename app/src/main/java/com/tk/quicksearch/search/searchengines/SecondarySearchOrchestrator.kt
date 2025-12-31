package com.tk.quicksearch.search.searchengines


import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SectionManager
import com.tk.quicksearch.search.core.UnifiedSearchHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SecondarySearchOrchestrator(
    private val scope: CoroutineScope,
    private val unifiedSearchHandler: UnifiedSearchHandler,
    private val webSuggestionHandler: WebSuggestionHandler,
    private val sectionManager: SectionManager,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
    private val currentStateProvider: () -> SearchUiState
) {
    private var searchJob: Job? = null
    private var queryVersion: Long = 0L
    
    companion object {
        private const val SECONDARY_SEARCH_DEBOUNCE_MS = 150L
    }

    fun performSecondarySearches(query: String) {
        searchJob?.cancel()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank() || trimmedQuery.length == 1) {
            uiStateUpdater {
                it.copy(
                    contactResults = emptyList(),
                    fileResults = emptyList(),
                    settingResults = emptyList()
                )
            }
            return
        }

        val currentState = currentStateProvider()
        val canSearchContacts = currentState.hasContactPermission &&
            SearchSection.CONTACTS !in sectionManager.disabledSections
        val canSearchFiles = currentState.hasFilePermission &&
            SearchSection.FILES !in sectionManager.disabledSections
        val canSearchSettings = SearchSection.SETTINGS !in sectionManager.disabledSections
            
        val currentVersion = ++queryVersion

        searchJob = scope.launch(Dispatchers.IO) {
            // Debounce expensive contact/file queries during rapid typing
            delay(SECONDARY_SEARCH_DEBOUNCE_MS)
            if (currentVersion != queryVersion) return@launch
            
            val unifiedResults = unifiedSearchHandler.performSearch(
                query = trimmedQuery,
                enabledFileTypes = currentState.enabledFileTypes,
                canSearchContacts = canSearchContacts,
                canSearchFiles = canSearchFiles,
                canSearchSettings = canSearchSettings
            )

            withContext(Dispatchers.Main) {
                if (currentVersion == queryVersion) {
                    val stateBeforeUpdate = currentStateProvider()
                    val hasAnyResults = unifiedResults.contactResults.isNotEmpty() || 
                        unifiedResults.fileResults.isNotEmpty() || 
                        unifiedResults.settingResults.isNotEmpty() ||
                        stateBeforeUpdate.searchResults.isNotEmpty()
                    
                    uiStateUpdater { state ->
                        state.copy(
                            contactResults = unifiedResults.contactResults,
                            fileResults = unifiedResults.fileResults,
                            settingResults = unifiedResults.settingResults,
                            webSuggestions = if (hasAnyResults) emptyList() else state.webSuggestions
                        )
                    }
                    
                    // Fetch web suggestions if there are no results, query is long enough, and suggestions are enabled
                    if (!hasAnyResults && trimmedQuery.length >= 2 && webSuggestionHandler.isEnabled) {
                         webSuggestionHandler.fetchWebSuggestions(
                             trimmedQuery, 
                             currentVersion,
                             activeQueryVersionProvider = { this@SecondarySearchOrchestrator.queryVersion },
                             activeQueryProvider = { currentStateProvider().query }
                         )
                    } else {
                        // Clear suggestions if we have results
                        webSuggestionHandler.cancelSuggestions()
                        if (hasAnyResults) {
                            uiStateUpdater { state ->
                                state.copy(webSuggestions = emptyList())
                            }
                        }
                    }
                }
            }
        }
    }
    
    fun cancel() {
        searchJob?.cancel()
    }
}
