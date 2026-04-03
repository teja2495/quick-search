package com.tk.quicksearch.searchEngines

import android.os.Looper
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SectionManager
import com.tk.quicksearch.search.core.UnifiedSearchHandler
import com.tk.quicksearch.search.core.UnifiedSearchResults
import com.tk.quicksearch.search.webSuggestions.WebSuggestionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

class SecondarySearchOrchestrator(
    private val scope: CoroutineScope,
    private val unifiedSearchHandler: UnifiedSearchHandler,
    private val webSuggestionHandler: WebSuggestionHandler,
    private val sectionManager: SectionManager,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
    private val currentStateProvider: () -> SearchUiState,
    private val isLowRamDevice: Boolean = false,
) {
    private var searchJob: Job? = null
    private val queryVersion = AtomicLong(0L)

    // Track query prefixes that yielded no results to avoid redundant searches.
    private val lastQueryWithNoResultsBySection = mutableMapOf<SearchSection, String>()
    private var lastQueryLength: Int = 0

    companion object {
        private const val SECONDARY_SEARCH_DEBOUNCE_MS = 150L
    }

    fun performSecondarySearches(query: String) {
        if (!isOnMainThread()) {
            scope.launch(Dispatchers.Main.immediate) {
                performSecondarySearchesInternal(query)
            }
            return
        }
        performSecondarySearchesInternal(query)
    }

    private fun performSecondarySearchesInternal(query: String) {
        searchJob?.cancel()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            // Clear all no-results tracking when query is cleared.
            clearNoResultTracking()
            lastQueryLength = 0

            uiStateUpdater {
                it.copy(
                    contactResults = emptyList(),
                    fileResults = emptyList(),
                    settingResults = emptyList(),
                    calendarEvents = emptyList(),
                    appSettingResults = emptyList(),
                    appShortcutResults = emptyList(),
                    isSecondarySearchInProgress = false,
                )
            }
            return
        }

        // Detect backspacing: if query is shorter, reset relevant no-results prefixes
        val isBackspacing = trimmedQuery.length < lastQueryLength
        if (isBackspacing) {
            resetNoResultPrefixesForBackspace(trimmedQuery)
        }

        val isSingleCharacterQuery = trimmedQuery.length == 1
        if (isSingleCharacterQuery) {
            // Keep first-letter app shortcut searches responsive even if a previous
            // single-letter query was cached as "no results" before data finished loading.
            clearNoResultTracking(SearchSection.APP_SHORTCUTS)
        }
        val currentState = currentStateProvider()
        val canSearchContacts =
            !isSingleCharacterQuery &&
                currentState.hasContactPermission &&
                SearchSection.CONTACTS !in sectionManager.disabledSections
        val canSearchFiles =
            !isSingleCharacterQuery &&
                currentState.hasFilePermission &&
                SearchSection.FILES !in sectionManager.disabledSections
        val canSearchSettings =
            !isSingleCharacterQuery && SearchSection.SETTINGS !in sectionManager.disabledSections
        val canSearchCalendar =
            !isSingleCharacterQuery &&
                currentState.hasCalendarPermission &&
                SearchSection.CALENDAR !in sectionManager.disabledSections
        val canSearchAppSettings =
            !isSingleCharacterQuery &&
                SearchSection.APP_SETTINGS !in sectionManager.disabledSections
        val canSearchAppShortcuts = SearchSection.APP_SHORTCUTS !in sectionManager.disabledSections

        // Skip searches if current query extends a previous no-results query
        val shouldSkipContacts = shouldSkipSearchForSection(trimmedQuery, SearchSection.CONTACTS, isBackspacing)
        val shouldSkipFiles = shouldSkipSearchForSection(trimmedQuery, SearchSection.FILES, isBackspacing)
        val shouldSkipSettings = shouldSkipSearchForSection(trimmedQuery, SearchSection.SETTINGS, isBackspacing)
        val shouldSkipCalendar = shouldSkipSearchForSection(trimmedQuery, SearchSection.CALENDAR, isBackspacing)
        val shouldSkipAppSettings =
            shouldSkipSearchForSection(trimmedQuery, SearchSection.APP_SETTINGS, isBackspacing)
        val shouldSkipAppShortcuts =
            !isSingleCharacterQuery &&
                shouldSkipSearchForSection(trimmedQuery, SearchSection.APP_SHORTCUTS, isBackspacing)
        val shouldSearchContacts = canSearchContacts && !shouldSkipContacts
        val shouldSearchFiles = canSearchFiles && !shouldSkipFiles
        val shouldSearchSettings = canSearchSettings && !shouldSkipSettings
        val shouldSearchCalendar = canSearchCalendar && !shouldSkipCalendar
        val shouldSearchAppSettings = canSearchAppSettings && !shouldSkipAppSettings
        val shouldSearchAppShortcuts = canSearchAppShortcuts && !shouldSkipAppShortcuts

        val currentVersion = queryVersion.incrementAndGet()
        lastQueryLength = trimmedQuery.length
        uiStateUpdater { it.copy(isSecondarySearchInProgress = true) }

        searchJob =
            scope.launch(Dispatchers.IO) {
                // Debounce expensive contact/file queries during rapid typing
                delay(SECONDARY_SEARCH_DEBOUNCE_MS)
                if (currentVersion != queryVersion.get()) return@launch

                val unifiedResults =
                    unifiedSearchHandler.performSearch(
                        query = trimmedQuery,
                        enabledFileTypes = currentState.enabledFileTypes,
                        canSearchContacts = shouldSearchContacts,
                        canSearchFiles = shouldSearchFiles,
                        canSearchSettings = shouldSearchSettings,
                        canSearchCalendar = shouldSearchCalendar,
                        canSearchAppSettings = shouldSearchAppSettings,
                        canSearchAppShortcuts = shouldSearchAppShortcuts,
                        enableFuzzyContactSearch = false,
                        enableFuzzyFileSearch = false,
                        enableFuzzySettingsSearch = !isLowRamDevice,
                        enableFuzzyAppSettingsSearch = !isLowRamDevice,
                        showFolders = currentState.showFolders,
                        showSystemFiles = currentState.showSystemFiles,
                        aliasSection = null,
                    )

                withContext(Dispatchers.Main) {
                    if (currentVersion == queryVersion.get()) {
                        // Update no-results tracking based on search results
                        updateNoResultTracking(
                            section = SearchSection.CONTACTS,
                            shouldSearch = shouldSearchContacts,
                            query = trimmedQuery,
                            hadResults = unifiedResults.contactResults.isNotEmpty(),
                        )
                        updateNoResultTracking(
                            section = SearchSection.FILES,
                            shouldSearch = shouldSearchFiles,
                            query = trimmedQuery,
                            hadResults = unifiedResults.fileResults.isNotEmpty(),
                        )
                        updateNoResultTracking(
                            section = SearchSection.SETTINGS,
                            shouldSearch = shouldSearchSettings,
                            query = trimmedQuery,
                            hadResults = unifiedResults.settingResults.isNotEmpty(),
                        )
                        updateNoResultTracking(
                            section = SearchSection.CALENDAR,
                            shouldSearch = shouldSearchCalendar,
                            query = trimmedQuery,
                            hadResults = unifiedResults.calendarEvents.isNotEmpty(),
                        )
                        updateNoResultTracking(
                            section = SearchSection.APP_SETTINGS,
                            shouldSearch = shouldSearchAppSettings,
                            query = trimmedQuery,
                            hadResults = unifiedResults.appSettingResults.isNotEmpty(),
                        )
                        updateNoResultTracking(
                            section = SearchSection.APP_SHORTCUTS,
                            shouldSearch = shouldSearchAppShortcuts && !isSingleCharacterQuery,
                            query = trimmedQuery,
                            hadResults = unifiedResults.appShortcutResults.isNotEmpty(),
                        )

                        uiStateUpdater { state ->
                            state.copy(
                                contactResults = unifiedResults.contactResults,
                                fileResults = unifiedResults.fileResults,
                                settingResults = unifiedResults.settingResults,
                                calendarEvents = unifiedResults.calendarEvents,
                                appSettingResults = unifiedResults.appSettingResults,
                                // Preserve existing results when the search was skipped via the
                                // no-results cache; overwriting with empty would clear valid
                                // results that were found for the current or a prior query.
                                appShortcutResults = if (shouldSearchAppShortcuts) unifiedResults.appShortcutResults else state.appShortcutResults,
                                isSecondarySearchInProgress = false,
                            )
                        }

                        // Fetch web suggestions if query is long enough and suggestions are enabled
                        val queryLengthCheck = trimmedQuery.length >= 2
                        val suggestionsEnabled = webSuggestionHandler.isEnabled
                        if (queryLengthCheck && suggestionsEnabled) {
                            webSuggestionHandler.fetchWebSuggestions(
                                trimmedQuery,
                                currentVersion,
                                activeQueryVersionProvider = {
                                    this@SecondarySearchOrchestrator.queryVersion.get()
                                },
                                activeQueryProvider = { currentStateProvider().query },
                            )
                        } else {
                            // Clear suggestions if disabled or query too short
                            webSuggestionHandler.cancelSuggestions()
                            uiStateUpdater { state ->
                                state.copy(webSuggestions = emptyList())
                            }
                        }
                    }
                }
            }
    }

    fun performTargetedSecondarySearch(
        query: String,
        section: SearchSection,
        useFuzzyMatching: Boolean,
        ignoreSectionToggle: Boolean = false,
    ) {
        if (!isOnMainThread()) {
            scope.launch(Dispatchers.Main.immediate) {
                performTargetedSecondarySearchInternal(
                    query = query,
                    section = section,
                    useFuzzyMatching = useFuzzyMatching,
                    ignoreSectionToggle = ignoreSectionToggle,
                )
            }
            return
        }
        performTargetedSecondarySearchInternal(
            query = query,
            section = section,
            useFuzzyMatching = useFuzzyMatching,
            ignoreSectionToggle = ignoreSectionToggle,
        )
    }

    private fun performTargetedSecondarySearchInternal(
        query: String,
        section: SearchSection,
        useFuzzyMatching: Boolean,
        ignoreSectionToggle: Boolean,
    ) {
        searchJob?.cancel()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            clearNoResultTracking()
            uiStateUpdater {
                it.copy(
                    contactResults = emptyList(),
                    fileResults = emptyList(),
                    settingResults = emptyList(),
                    calendarEvents = emptyList(),
                    appSettingResults = emptyList(),
                    appShortcutResults = emptyList(),
                    webSuggestions = emptyList(),
                    isSecondarySearchInProgress = false,
                )
            }
            return
        }

        val currentState = currentStateProvider()
        val isSingleCharacterQuery = trimmedQuery.length == 1
        val isBackspacing = trimmedQuery.length < lastQueryLength
        if (isBackspacing) {
            resetNoResultPrefixesForBackspace(trimmedQuery)
        }
        if (isSingleCharacterQuery && section == SearchSection.APP_SHORTCUTS) {
            clearNoResultTracking(SearchSection.APP_SHORTCUTS)
        }
        val isSectionEnabled =
            { targetSection: SearchSection ->
                ignoreSectionToggle || targetSection !in sectionManager.disabledSections
            }
        val isContactsEnabled =
            !isSingleCharacterQuery &&
                currentState.hasContactPermission &&
                isSectionEnabled(SearchSection.CONTACTS)
        val isFilesEnabled =
            !isSingleCharacterQuery &&
                currentState.hasFilePermission &&
                isSectionEnabled(SearchSection.FILES)
        val isSettingsEnabled =
            !isSingleCharacterQuery && isSectionEnabled(SearchSection.SETTINGS)
        val isCalendarEnabled =
            !isSingleCharacterQuery &&
                currentState.hasCalendarPermission &&
                isSectionEnabled(SearchSection.CALENDAR)
        val isAppSettingsEnabled =
            !isSingleCharacterQuery &&
                isSectionEnabled(SearchSection.APP_SETTINGS)
        val isAppShortcutsEnabled =
            isSectionEnabled(SearchSection.APP_SHORTCUTS)

        val shouldSearchContacts = section == SearchSection.CONTACTS && isContactsEnabled
        val shouldSearchFiles = section == SearchSection.FILES && isFilesEnabled
        val shouldSearchSettings = section == SearchSection.SETTINGS && isSettingsEnabled
        val shouldSearchCalendar = section == SearchSection.CALENDAR && isCalendarEnabled
        val shouldSearchAppSettings = section == SearchSection.APP_SETTINGS && isAppSettingsEnabled
        val shouldSearchAppShortcuts =
            section == SearchSection.APP_SHORTCUTS && isAppShortcutsEnabled
        val shouldSkipSection =
            if (section == SearchSection.APP_SHORTCUTS && isSingleCharacterQuery) {
                false
            } else {
                shouldSkipSearchForSection(trimmedQuery, section, isBackspacing)
            }
        val shouldRunTargetedSearch =
            when (section) {
                SearchSection.CONTACTS -> shouldSearchContacts
                SearchSection.FILES -> shouldSearchFiles
                SearchSection.SETTINGS -> shouldSearchSettings
                SearchSection.CALENDAR -> shouldSearchCalendar
                SearchSection.APP_SETTINGS -> shouldSearchAppSettings
                SearchSection.APP_SHORTCUTS -> shouldSearchAppShortcuts
                SearchSection.APPS -> false
            } && !shouldSkipSection

        val currentVersion = queryVersion.incrementAndGet()
        lastQueryLength = trimmedQuery.length
        uiStateUpdater { it.copy(isSecondarySearchInProgress = true) }
        searchJob =
            scope.launch(Dispatchers.IO) {
                delay(SECONDARY_SEARCH_DEBOUNCE_MS)
                if (currentVersion != queryVersion.get()) return@launch
                if (!shouldRunTargetedSearch) {
                    withContext(Dispatchers.Main) {
                        if (currentVersion != queryVersion.get()) return@withContext
                        uiStateUpdater { state ->
                            state.copy(
                                webSuggestions = emptyList(),
                                isSecondarySearchInProgress = false,
                            )
                        }
                    }
                    return@launch
                }

                val unifiedResults =
                    unifiedSearchHandler.performSearch(
                        query = trimmedQuery,
                        enabledFileTypes = currentState.enabledFileTypes,
                        canSearchContacts = shouldSearchContacts,
                        canSearchFiles = shouldSearchFiles,
                        canSearchSettings = shouldSearchSettings,
                        canSearchCalendar = shouldSearchCalendar,
                        canSearchAppSettings = shouldSearchAppSettings,
                        canSearchAppShortcuts = shouldSearchAppShortcuts,
                        enableFuzzyContactSearch = shouldSearchContacts && useFuzzyMatching,
                        enableFuzzyFileSearch = shouldSearchFiles && useFuzzyMatching,
                        enableFuzzySettingsSearch = shouldSearchSettings && useFuzzyMatching,
                        enableFuzzyAppSettingsSearch = shouldSearchAppSettings && useFuzzyMatching,
                        showFolders = currentState.showFolders,
                        showSystemFiles = currentState.showSystemFiles,
                        aliasSection = section,
                    )

                withContext(Dispatchers.Main) {
                    if (currentVersion != queryVersion.get()) return@withContext
                    updateNoResultTracking(
                        section = section,
                        shouldSearch = shouldRunTargetedSearch,
                        query = trimmedQuery,
                        hadResults = hasResultsForSection(unifiedResults, section),
                    )
                    uiStateUpdater { state ->
                        state.copy(
                            contactResults = unifiedResults.contactResults,
                            fileResults = unifiedResults.fileResults,
                            settingResults = unifiedResults.settingResults,
                            calendarEvents = unifiedResults.calendarEvents,
                            appSettingResults = unifiedResults.appSettingResults,
                            appShortcutResults = unifiedResults.appShortcutResults,
                            webSuggestions = emptyList(),
                            isSecondarySearchInProgress = false,
                        )
                    }
                }
            }
    }

    fun resetNoResultTracking() {
        if (!isOnMainThread()) {
            scope.launch(Dispatchers.Main.immediate) {
                resetNoResultTrackingInternal()
            }
            return
        }
        resetNoResultTrackingInternal()
    }

    private fun resetNoResultTrackingInternal() {
        clearNoResultTracking()
        lastQueryLength = 0
    }

    fun performWebSuggestionsOnly(query: String) {
        if (!isOnMainThread()) {
            scope.launch(Dispatchers.Main.immediate) {
                performWebSuggestionsOnlyInternal(query)
            }
            return
        }
        performWebSuggestionsOnlyInternal(query)
    }

    private fun performWebSuggestionsOnlyInternal(query: String) {
        searchJob?.cancel()
        val trimmedQuery = query.trim()
        val currentVersion = queryVersion.incrementAndGet()
        lastQueryLength = trimmedQuery.length
        uiStateUpdater { it.copy(isSecondarySearchInProgress = false) }

        searchJob =
            scope.launch(Dispatchers.IO) {
                // Debounce to match regular search behavior
                delay(SECONDARY_SEARCH_DEBOUNCE_MS)
                if (currentVersion != queryVersion.get()) return@launch

                withContext(Dispatchers.Main) {
                    // Clear all other results
                    uiStateUpdater { state ->
                        state.copy(
                            contactResults = emptyList(),
                            fileResults = emptyList(),
                            settingResults = emptyList(),
                            calendarEvents = emptyList(),
                            appSettingResults = emptyList(),
                            appShortcutResults = emptyList(),
                            isSecondarySearchInProgress = false,
                        )
                    }

                    // Fetch web suggestions if enabled and query is long enough
                    val suggestionsEnabled = webSuggestionHandler.isEnabled
                    val queryLengthCheck = trimmedQuery.length >= 2
                    if (suggestionsEnabled && queryLengthCheck) {
                        webSuggestionHandler.fetchWebSuggestions(
                            trimmedQuery,
                            currentVersion,
                            activeQueryVersionProvider = {
                                this@SecondarySearchOrchestrator.queryVersion.get()
                            },
                            activeQueryProvider = { currentStateProvider().query },
                        )
                    } else {
                        webSuggestionHandler.cancelSuggestions()
                        uiStateUpdater { state -> state.copy(webSuggestions = emptyList()) }
                    }
                }
            }
    }

    fun cancel() {
        if (!isOnMainThread()) {
            scope.launch(Dispatchers.Main.immediate) {
                searchJob?.cancel()
                uiStateUpdater { it.copy(isSecondarySearchInProgress = false) }
            }
            return
        }
        searchJob?.cancel()
        uiStateUpdater { it.copy(isSecondarySearchInProgress = false) }
    }

    private fun isOnMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    private fun resetNoResultPrefixesForBackspace(trimmedQuery: String) {
        val currentLength = trimmedQuery.length
        val keysToClear =
            lastQueryWithNoResultsBySection
                .filterValues { cachedQuery -> currentLength < cachedQuery.length }
                .keys
        keysToClear.forEach { section -> clearNoResultTracking(section) }
    }

    private fun clearNoResultTracking(section: SearchSection? = null) {
        if (section == null) {
            lastQueryWithNoResultsBySection.clear()
        } else {
            lastQueryWithNoResultsBySection.remove(section)
        }
    }

    private fun shouldSkipSearchForSection(
        query: String,
        section: SearchSection,
        isBackspacing: Boolean,
    ): Boolean {
        if (isBackspacing) return false
        val lastNoResultQuery = lastQueryWithNoResultsBySection[section] ?: return false
        return query.startsWith(lastNoResultQuery)
    }

    private fun updateNoResultTracking(
        section: SearchSection,
        shouldSearch: Boolean,
        query: String,
        hadResults: Boolean,
    ) {
        if (!shouldSearch) return
        if (hadResults) {
            clearNoResultTracking(section)
        } else {
            lastQueryWithNoResultsBySection[section] = query
        }
    }

    private fun hasResultsForSection(
        results: UnifiedSearchResults,
        section: SearchSection,
    ): Boolean =
        when (section) {
            SearchSection.CONTACTS -> results.contactResults.isNotEmpty()
            SearchSection.FILES -> results.fileResults.isNotEmpty()
            SearchSection.SETTINGS -> results.settingResults.isNotEmpty()
            SearchSection.CALENDAR -> results.calendarEvents.isNotEmpty()
            SearchSection.APP_SETTINGS -> results.appSettingResults.isNotEmpty()
            SearchSection.APP_SHORTCUTS -> results.appShortcutResults.isNotEmpty()
            SearchSection.APPS -> false
        }
}
