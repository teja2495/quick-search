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
    
    // Track query prefixes that yielded no results to avoid redundant searches
    private var lastQueryWithNoContacts: String? = null
    private var lastQueryWithNoFiles: String? = null
    private var lastQueryWithNoSettings: String? = null
    private var lastQueryLength: Int = 0
    
    companion object {
        private const val SECONDARY_SEARCH_DEBOUNCE_MS = 150L
    }

    fun performSecondarySearches(query: String) {
        searchJob?.cancel()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank() || trimmedQuery.length == 1) {
            // Clear all no-results tracking when query is cleared
            lastQueryWithNoContacts = null
            lastQueryWithNoFiles = null
            lastQueryWithNoSettings = null
            lastQueryLength = 0
            
            uiStateUpdater {
                it.copy(
                    contactResults = emptyList(),
                    fileResults = emptyList(),
                    settingResults = emptyList()
                )
            }
            return
        }

        // Detect backspacing: if query is shorter, reset relevant no-results prefixes
        val isBackspacing = trimmedQuery.length < lastQueryLength
        if (isBackspacing) {
            // Reset prefixes that are longer than current query
            if (lastQueryWithNoContacts != null && trimmedQuery.length < lastQueryWithNoContacts!!.length) {
                lastQueryWithNoContacts = null
            }
            if (lastQueryWithNoFiles != null && trimmedQuery.length < lastQueryWithNoFiles!!.length) {
                lastQueryWithNoFiles = null
            }
            if (lastQueryWithNoSettings != null && trimmedQuery.length < lastQueryWithNoSettings!!.length) {
                lastQueryWithNoSettings = null
            }
        }
        
        val currentState = currentStateProvider()
        val canSearchContacts = currentState.hasContactPermission &&
            SearchSection.CONTACTS !in sectionManager.disabledSections
        val canSearchFiles = currentState.hasFilePermission &&
            SearchSection.FILES !in sectionManager.disabledSections
        val canSearchSettings = SearchSection.SETTINGS !in sectionManager.disabledSections
        
        // Skip searches if current query extends a previous no-results query
        val shouldSkipContacts = !isBackspacing && 
            lastQueryWithNoContacts != null && 
            trimmedQuery.startsWith(lastQueryWithNoContacts!!)
        val shouldSkipFiles = !isBackspacing && 
            lastQueryWithNoFiles != null && 
            trimmedQuery.startsWith(lastQueryWithNoFiles!!)
        val shouldSkipSettings = !isBackspacing && 
            lastQueryWithNoSettings != null && 
            trimmedQuery.startsWith(lastQueryWithNoSettings!!)
            
        val currentVersion = ++queryVersion
        lastQueryLength = trimmedQuery.length

        searchJob = scope.launch(Dispatchers.IO) {
            // Debounce expensive contact/file queries during rapid typing
            delay(SECONDARY_SEARCH_DEBOUNCE_MS)
            if (currentVersion != queryVersion) return@launch
            
            val unifiedResults = unifiedSearchHandler.performSearch(
                query = trimmedQuery,
                enabledFileTypes = currentState.enabledFileTypes,
                canSearchContacts = canSearchContacts && !shouldSkipContacts,
                canSearchFiles = canSearchFiles && !shouldSkipFiles,
                canSearchSettings = canSearchSettings && !shouldSkipSettings
            )

            withContext(Dispatchers.Main) {
                if (currentVersion == queryVersion) {
                    // Update no-results tracking based on search results
                    if (unifiedResults.contactResults.isEmpty() && !shouldSkipContacts) {
                        lastQueryWithNoContacts = trimmedQuery
                    } else if (unifiedResults.contactResults.isNotEmpty()) {
                        // Clear if we got results
                        lastQueryWithNoContacts = null
                    }
                    
                    if (unifiedResults.fileResults.isEmpty() && !shouldSkipFiles) {
                        lastQueryWithNoFiles = trimmedQuery
                    } else if (unifiedResults.fileResults.isNotEmpty()) {
                        lastQueryWithNoFiles = null
                    }
                    
                    if (unifiedResults.settingResults.isEmpty() && !shouldSkipSettings) {
                        lastQueryWithNoSettings = trimmedQuery
                    } else if (unifiedResults.settingResults.isNotEmpty()) {
                        lastQueryWithNoSettings = null
                    }
                    
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
    
    fun performWebSuggestionsOnly(query: String) {
        searchJob?.cancel()
        val trimmedQuery = query.trim()
        val currentVersion = ++queryVersion
        lastQueryLength = trimmedQuery.length

        searchJob = scope.launch(Dispatchers.IO) {
            // Debounce to match regular search behavior
            delay(SECONDARY_SEARCH_DEBOUNCE_MS)
            if (currentVersion != queryVersion) return@launch

            withContext(Dispatchers.Main) {
                // Clear all other results
                uiStateUpdater { state ->
                    state.copy(
                        contactResults = emptyList(),
                        fileResults = emptyList(),
                        settingResults = emptyList()
                    )
                }

                // Fetch web suggestions if enabled and query is long enough
                if (webSuggestionHandler.isEnabled && trimmedQuery.length >= 2) {
                    webSuggestionHandler.fetchWebSuggestions(
                        trimmedQuery,
                        currentVersion,
                        activeQueryVersionProvider = { this@SecondarySearchOrchestrator.queryVersion },
                        activeQueryProvider = { currentStateProvider().query }
                    )
                } else {
                    webSuggestionHandler.cancelSuggestions()
                    uiStateUpdater { state ->
                        state.copy(webSuggestions = emptyList())
                    }
                }
            }
        }
    }
    
    fun cancel() {
        searchJob?.cancel()
    }
}
