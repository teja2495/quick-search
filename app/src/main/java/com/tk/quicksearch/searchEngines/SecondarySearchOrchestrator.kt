package com.tk.quicksearch.searchEngines

import android.os.Looper
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SectionManager
import com.tk.quicksearch.search.core.UnifiedSearchHandler
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

    // Track query prefixes that yielded no results to avoid redundant searches
    private var lastQueryWithNoContacts: String? = null
    private var lastQueryWithNoFiles: String? = null
    private var lastQueryWithNoSettings: String? = null
    private var lastQueryWithNoCalendar: String? = null
    private var lastQueryWithNoAppSettings: String? = null
    private var lastQueryWithNoAppShortcuts: String? = null
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
            // Clear all no-results tracking when query is cleared
            lastQueryWithNoContacts = null
            lastQueryWithNoFiles = null
            lastQueryWithNoSettings = null
            lastQueryWithNoCalendar = null
            lastQueryWithNoAppSettings = null
            lastQueryWithNoAppShortcuts = null
            lastQueryLength = 0

            uiStateUpdater {
                it.copy(
                    contactResults = emptyList(),
                    fileResults = emptyList(),
                    settingResults = emptyList(),
                    calendarEvents = emptyList(),
                    appSettingResults = emptyList(),
                    appShortcutResults = emptyList(),
                )
            }
            return
        }

        // Detect backspacing: if query is shorter, reset relevant no-results prefixes
        val isBackspacing = trimmedQuery.length < lastQueryLength
        if (isBackspacing) {
            // Reset prefixes that are longer than current query
            if (lastQueryWithNoContacts != null &&
                trimmedQuery.length < lastQueryWithNoContacts!!.length
            ) {
                lastQueryWithNoContacts = null
            }
            if (lastQueryWithNoFiles != null && trimmedQuery.length < lastQueryWithNoFiles!!.length) {
                lastQueryWithNoFiles = null
            }
            if (lastQueryWithNoSettings != null &&
                trimmedQuery.length < lastQueryWithNoSettings!!.length
            ) {
                lastQueryWithNoSettings = null
            }
            if (lastQueryWithNoCalendar != null &&
                trimmedQuery.length < lastQueryWithNoCalendar!!.length
            ) {
                lastQueryWithNoCalendar = null
            }
            if (lastQueryWithNoAppSettings != null &&
                trimmedQuery.length < lastQueryWithNoAppSettings!!.length
            ) {
                lastQueryWithNoAppSettings = null
            }
            if (lastQueryWithNoAppShortcuts != null &&
                trimmedQuery.length < lastQueryWithNoAppShortcuts!!.length
            ) {
                lastQueryWithNoAppShortcuts = null
            }
        }

        val isSingleCharacterQuery = trimmedQuery.length == 1
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
        val shouldSkipContacts =
            !isBackspacing &&
                lastQueryWithNoContacts != null &&
                trimmedQuery.startsWith(lastQueryWithNoContacts!!)
        val shouldSkipFiles =
            !isBackspacing &&
                lastQueryWithNoFiles != null &&
                trimmedQuery.startsWith(lastQueryWithNoFiles!!)
        val shouldSkipSettings =
            !isBackspacing &&
                lastQueryWithNoSettings != null &&
                trimmedQuery.startsWith(lastQueryWithNoSettings!!)
        val shouldSkipCalendar =
            !isBackspacing &&
                lastQueryWithNoCalendar != null &&
                trimmedQuery.startsWith(lastQueryWithNoCalendar!!)
        val shouldSkipAppSettings =
            !isBackspacing &&
                lastQueryWithNoAppSettings != null &&
                trimmedQuery.startsWith(lastQueryWithNoAppSettings!!)
        val shouldSkipAppShortcuts =
            !isBackspacing &&
                lastQueryWithNoAppShortcuts != null &&
                trimmedQuery.startsWith(lastQueryWithNoAppShortcuts!!)
        val shouldSearchContacts = canSearchContacts && !shouldSkipContacts
        val shouldSearchFiles = canSearchFiles && !shouldSkipFiles
        val shouldSearchSettings = canSearchSettings && !shouldSkipSettings
        val shouldSearchCalendar = canSearchCalendar && !shouldSkipCalendar
        val shouldSearchAppSettings = canSearchAppSettings && !shouldSkipAppSettings
        val shouldSearchAppShortcuts = canSearchAppShortcuts && !shouldSkipAppShortcuts

        val currentVersion = queryVersion.incrementAndGet()
        lastQueryLength = trimmedQuery.length

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
                        if (shouldSearchContacts && unifiedResults.contactResults.isEmpty()) {
                            lastQueryWithNoContacts = trimmedQuery
                        } else if (shouldSearchContacts && unifiedResults.contactResults.isNotEmpty()) {
                            // Clear if we got results
                            lastQueryWithNoContacts = null
                        }

                        if (shouldSearchFiles && unifiedResults.fileResults.isEmpty()) {
                            lastQueryWithNoFiles = trimmedQuery
                        } else if (shouldSearchFiles && unifiedResults.fileResults.isNotEmpty()) {
                            lastQueryWithNoFiles = null
                        }

                        if (shouldSearchSettings && unifiedResults.settingResults.isEmpty()) {
                            lastQueryWithNoSettings = trimmedQuery
                        } else if (shouldSearchSettings && unifiedResults.settingResults.isNotEmpty()) {
                            lastQueryWithNoSettings = null
                        }
                        if (shouldSearchCalendar && unifiedResults.calendarEvents.isEmpty()) {
                            lastQueryWithNoCalendar = trimmedQuery
                        } else if (shouldSearchCalendar && unifiedResults.calendarEvents.isNotEmpty()) {
                            lastQueryWithNoCalendar = null
                        }

                        if (shouldSearchAppSettings && unifiedResults.appSettingResults.isEmpty()) {
                            lastQueryWithNoAppSettings = trimmedQuery
                        } else if (shouldSearchAppSettings &&
                            unifiedResults.appSettingResults.isNotEmpty()
                        ) {
                            lastQueryWithNoAppSettings = null
                        }

                        if (shouldSearchAppShortcuts && unifiedResults.appShortcutResults.isEmpty()) {
                            lastQueryWithNoAppShortcuts = trimmedQuery
                        } else if (shouldSearchAppShortcuts &&
                            unifiedResults.appShortcutResults.isNotEmpty()
                        ) {
                            lastQueryWithNoAppShortcuts = null
                        }

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
            uiStateUpdater {
                it.copy(
                    contactResults = emptyList(),
                    fileResults = emptyList(),
                    settingResults = emptyList(),
                    calendarEvents = emptyList(),
                    appSettingResults = emptyList(),
                    appShortcutResults = emptyList(),
                    webSuggestions = emptyList(),
                )
            }
            return
        }

        val currentState = currentStateProvider()
        val isSingleCharacterQuery = trimmedQuery.length == 1
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

        val currentVersion = queryVersion.incrementAndGet()
        searchJob =
            scope.launch(Dispatchers.IO) {
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
                    uiStateUpdater { state ->
                        state.copy(
                            contactResults = unifiedResults.contactResults,
                            fileResults = unifiedResults.fileResults,
                            settingResults = unifiedResults.settingResults,
                            calendarEvents = unifiedResults.calendarEvents,
                            appSettingResults = unifiedResults.appSettingResults,
                            appShortcutResults = unifiedResults.appShortcutResults,
                            webSuggestions = emptyList(),
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
        lastQueryWithNoContacts = null
        lastQueryWithNoFiles = null
        lastQueryWithNoSettings = null
        lastQueryWithNoCalendar = null
        lastQueryWithNoAppSettings = null
        lastQueryWithNoAppShortcuts = null
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
            }
            return
        }
        searchJob?.cancel()
    }

    private fun isOnMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()
}
