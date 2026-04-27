package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.searchEngines.AliasTarget
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class SearchQueryAliasState(
    val lockedShortcutTarget: SearchTarget?,
    val lockedAliasSearchSection: SearchSection?,
    val lockedToolMode: SearchToolType?,
    val lockedCurrencyConverterAlias: Boolean,
    val lockedWordClockAlias: Boolean,
    val lockedDictionaryAlias: Boolean,
    val lockedCustomToolId: String? = null,
)

internal class SearchQueryCoordinator(
    private val scope: CoroutineScope,
    private val workerDispatcher: CoroutineDispatcher,
    private val handlers: SearchHandlerContainer,
    private val toolCoordinator: SearchToolCoordinator,
    private val userPreferences: com.tk.quicksearch.search.data.UserAppPreferences,
    private val appSearchDebounceMs: Long,
    private val aliasStateProvider: () -> SearchQueryAliasState,
    private val updateAliasState: (SearchQueryAliasState) -> Unit,
    private val currentResultsStateProvider: () -> SearchResultsState,
    private val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val clearInformationCardsExcept: (SearchViewModel.ActiveInformationCard) -> Unit,
    private val getSearchableAppsSnapshot: () -> List<com.tk.quicksearch.search.models.AppInfo>,
    private val getGridItemCount: () -> Int,
    private val loadAppShortcuts: () -> Unit,
    private val refreshRecentItems: () -> Unit,
    private val refreshAliasRecentItems: (SearchSection?) -> Unit,
) {
    private var appSearchJob: Job? = null
    private val appSearchQueryVersion = AtomicLong(0L)

    private sealed interface AliasQueryResolution {
        data object None : AliasQueryResolution

        data class ReprocessQuery(
            val queryWithoutAlias: String,
        ) : AliasQueryResolution

        data class ExecuteSearchTarget(
            val queryWithoutAlias: String,
            val target: SearchTarget,
        ) : AliasQueryResolution
    }

    private val aliasHandler get() = handlers.aliasHandler
    private val navigationHandler get() = handlers.navigationHandler
    private val appSearchManager get() = handlers.appSearchManager
    private val appShortcutSearchHandler get() = handlers.appShortcutSearchHandler
    private val secondarySearchOrchestrator get() = handlers.secondarySearchOrchestrator
    private val webSuggestionHandler get() = handlers.webSuggestionHandler

    fun onQueryChange(newQuery: String) {
        onQueryChangeInternal(newQuery, clearShortcutWhenBlank = false)
    }

    fun activateSearchSectionFilter(section: SearchSection) {
        setDetectedAliasMode(shortcutTarget = null, section = section, toolMode = null)
        val currentQuery = currentResultsStateProvider().query
        updateUiState { state ->
            state.copy(
                detectedShortcutTarget = null,
                detectedAliasSearchSection = section,
                isCurrencyConverterAliasMode = false,
                isWordClockAliasMode = false,
                isDictionaryAliasMode = false,
                calculatorState = CalculatorState(),
                currencyConverterState = CurrencyConverterState(),
                wordClockState = WordClockState(),
                dictionaryState = DictionaryState(),
                searchResults = emptyList(),
                isAppSearchInProgress = false,
                contactResults = emptyList(),
                fileResults = emptyList(),
                settingResults = emptyList(),
                appSettingResults = emptyList(),
                appShortcutResults = emptyList(),
                calendarEvents = emptyList(),
                noteResults = emptyList(),
            )
        }
        if (currentQuery.isNotEmpty() && section != SearchSection.APPS) {
            secondarySearchOrchestrator.performTargetedSecondarySearch(
                query = currentQuery,
                section = section,
                ignoreSectionToggle = true,
            )
        }
    }

    fun clearDetectedShortcut() {
        clearDetectedAliasMode()
        updateUiState {
            it.copy(
                detectedShortcutTarget = null,
                detectedAliasSearchSection = null,
                isCurrencyConverterAliasMode = false,
                isWordClockAliasMode = false,
                isDictionaryAliasMode = false,
                detectedCustomToolId = null,
                calculatorState = CalculatorState(),
                currencyConverterState = CurrencyConverterState(),
                wordClockState = WordClockState(),
                dictionaryState = DictionaryState(),
            )
        }
    }

    fun clearQuery() {
        clearDetectedAliasMode()
        onQueryChangeInternal("", clearShortcutWhenBlank = true)
    }

    fun cancel() {
        appSearchJob?.cancel()
    }

    private fun setDetectedAliasMode(
        shortcutTarget: SearchTarget?,
        section: SearchSection?,
        toolMode: SearchToolType?,
    ) {
        val current = aliasStateProvider()
        val isExclusive = shortcutTarget != null || section != null || toolMode != null
        updateAliasState(
            current.copy(
                lockedShortcutTarget = shortcutTarget,
                lockedAliasSearchSection = section,
                lockedToolMode = toolMode,
                lockedCurrencyConverterAlias = if (isExclusive) false else current.lockedCurrencyConverterAlias,
                lockedWordClockAlias = if (isExclusive) false else current.lockedWordClockAlias,
                lockedDictionaryAlias = if (isExclusive) false else current.lockedDictionaryAlias,
                lockedCustomToolId = if (isExclusive) null else current.lockedCustomToolId,
            ),
        )
    }

    private fun clearDetectedAliasMode() {
        updateAliasState(
            SearchQueryAliasState(
                lockedShortcutTarget = null,
                lockedAliasSearchSection = null,
                lockedToolMode = null,
                lockedCurrencyConverterAlias = false,
                lockedWordClockAlias = false,
                lockedDictionaryAlias = false,
                lockedCustomToolId = null,
            ),
        )
    }

    private fun resolveAliasQueryResolution(
        newQuery: String,
    ): AliasQueryResolution {
        val leadingAliasMatch = aliasHandler.detectAliasAtStart(newQuery)
        if (leadingAliasMatch != null) {
            val (queryWithoutAlias, aliasTarget) = leadingAliasMatch
            when (aliasTarget) {
                is AliasTarget.Search -> {
                    setDetectedAliasMode(
                        shortcutTarget = aliasTarget.target,
                        section = null,
                        toolMode = null,
                    )
                }

                is AliasTarget.Section -> {
                    setDetectedAliasMode(
                        shortcutTarget = null,
                        section = aliasTarget.section,
                        toolMode = null,
                    )
                }

                is AliasTarget.Feature -> {
                    applyFeatureAliasMode(aliasTarget.featureId)
                }
            }
            return AliasQueryResolution.ReprocessQuery(queryWithoutAlias)
        }

        val aliasState = aliasStateProvider()
        if (aliasState.lockedToolMode != null ||
            aliasState.lockedAliasSearchSection != null ||
            aliasState.lockedShortcutTarget != null ||
            aliasState.lockedCurrencyConverterAlias ||
            aliasState.lockedWordClockAlias ||
            aliasState.lockedDictionaryAlias ||
            aliasState.lockedCustomToolId != null
        ) {
            return AliasQueryResolution.None
        }

        val trailingSearchEngineAlias =
            aliasHandler.detectSearchEngineAliasAtEnd(newQuery) ?: return AliasQueryResolution.None
        val (queryWithoutAlias, target) = trailingSearchEngineAlias
        return AliasQueryResolution.ExecuteSearchTarget(
            queryWithoutAlias = queryWithoutAlias,
            target = target,
        )
    }

    private fun applyFeatureAliasMode(featureId: String) {
        // Handle custom tool aliases
        if (featureId.startsWith("custom_tool:")) {
            if (!userPreferences.hasAnyLlmApiKey()) {
                clearDetectedAliasMode()
                return
            }
            val current = aliasStateProvider()
            updateAliasState(
                current.copy(
                    lockedShortcutTarget = null,
                    lockedAliasSearchSection = null,
                    lockedToolMode = null,
                    lockedCurrencyConverterAlias = false,
                    lockedWordClockAlias = false,
                    lockedDictionaryAlias = false,
                    lockedCustomToolId = featureId,
                ),
            )
            return
        }

        val definition = aliasHandler.getFeatureAliasDefinition(featureId)
        if (definition == null) {
            clearDetectedAliasMode()
            return
        }

        if (definition.requiresGeminiApiKey && !userPreferences.hasAnyLlmApiKey()) {
            clearDetectedAliasMode()
            return
        }

        val standaloneMode = definition.standaloneMode
        if (standaloneMode != null) {
            val current = aliasStateProvider()
            updateAliasState(
                current.copy(
                    lockedShortcutTarget = null,
                    lockedAliasSearchSection = null,
                    lockedToolMode = null,
                    lockedCurrencyConverterAlias =
                        standaloneMode ==
                            AliasHandler.StandaloneFeatureAliasMode.CURRENCY_CONVERTER,
                    lockedWordClockAlias =
                        standaloneMode == AliasHandler.StandaloneFeatureAliasMode.WORD_CLOCK,
                    lockedDictionaryAlias =
                        standaloneMode == AliasHandler.StandaloneFeatureAliasMode.DICTIONARY,
                    lockedCustomToolId = null,
                ),
            )
            return
        }

        val toolMode = definition.toolType
        if (toolMode == null) {
            clearDetectedAliasMode()
            return
        }

        val current = aliasStateProvider()
        updateAliasState(
            current.copy(
                lockedCurrencyConverterAlias = false,
                lockedWordClockAlias = false,
                lockedDictionaryAlias = false,
                lockedCustomToolId = null,
            ),
        )
        setDetectedAliasMode(
            shortcutTarget = null,
            section = null,
            toolMode = toolMode,
        )
    }

    private fun createToolModeState(toolMode: SearchToolType): CalculatorState =
        when (toolMode) {
            SearchToolType.CALCULATOR ->
                CalculatorState(
                    isCalculatorMode = true,
                    toolType = SearchToolType.CALCULATOR,
                )
            SearchToolType.UNIT_CONVERTER ->
                CalculatorState(
                    isUnitConverterMode = true,
                    toolType = SearchToolType.UNIT_CONVERTER,
                )
            SearchToolType.DATE_CALCULATOR ->
                CalculatorState(
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
        }

    private fun resolveToolState(
        trimmedQuery: String,
        detectedTarget: SearchTarget?,
        detectedAliasSearchSection: SearchSection?,
        skipLocalTools: Boolean = false,
    ): CalculatorState =
        toolCoordinator.resolveToolState(
            trimmedQuery = trimmedQuery,
            detectedTarget = detectedTarget,
            detectedAliasSearchSection = detectedAliasSearchSection,
            skipLocalTools = skipLocalTools,
        )

    private fun onQueryChangeInternal(
        newQuery: String,
        clearShortcutWhenBlank: Boolean,
    ) {
        val previousQuery = currentResultsStateProvider().query
        if (newQuery == previousQuery) return

        val trimmedQuery = newQuery.trim()
        if (trimmedQuery.isNotBlank() && trimmedQuery == previousQuery.trim()) {
            updateUiState { it.copy(query = newQuery) }
            return
        }
        val aiSearchState = currentResultsStateProvider().AiSearchState
        if (aiSearchState.status != AiSearchStatus.Idle &&
            (aiSearchState.activeQuery == null ||
                aiSearchState.activeQuery != trimmedQuery)
        ) {
            handlers.aiSearchHandler.clearAiSearchState()
        }

        val currencyState = currentResultsStateProvider().currencyConverterState
        if (currencyState.status != CurrencyConverterStatus.Idle &&
            (currencyState.activeQuery == null || currencyState.activeQuery != trimmedQuery)
        ) {
            updateResultsState { it.copy(currencyConverterState = CurrencyConverterState()) }
        }
        val wordClockState = currentResultsStateProvider().wordClockState
        if (wordClockState.status != WordClockStatus.Idle &&
            (wordClockState.activeQuery == null || wordClockState.activeQuery != trimmedQuery)
        ) {
            updateResultsState { it.copy(wordClockState = WordClockState()) }
        }
        val dictionaryState = currentResultsStateProvider().dictionaryState
        if (dictionaryState.status != DictionaryStatus.Idle &&
            (dictionaryState.activeQuery == null || dictionaryState.activeQuery != trimmedQuery)
        ) {
            updateResultsState { it.copy(dictionaryState = DictionaryState()) }
        }

        if (trimmedQuery.isBlank()) {
            val aliasState = aliasStateProvider()
            val hasLockedAliasMode =
                aliasState.lockedShortcutTarget != null ||
                    aliasState.lockedAliasSearchSection != null ||
                    aliasState.lockedToolMode != null ||
                    aliasState.lockedCurrencyConverterAlias ||
                    aliasState.lockedWordClockAlias ||
                    aliasState.lockedDictionaryAlias ||
                    aliasState.lockedCustomToolId != null
            if (clearShortcutWhenBlank && hasLockedAliasMode && newQuery.isNotEmpty()) {
                appSearchJob?.cancel()
                appSearchManager.setNoMatchPrefix(null)
                secondarySearchOrchestrator.resetNoResultTracking()
                webSuggestionHandler.cancelSuggestions()
                updateUiState {
                    it.copy(
                        query = newQuery,
                        searchResults = emptyList(),
                        appShortcutResults = emptyList(),
                        contactResults = emptyList(),
                        fileResults = emptyList(),
                        settingResults = emptyList(),
                        appSettingResults = emptyList(),
                        calendarEvents = emptyList(),
                        noteResults = emptyList(),
                        AiSearchState = AiSearchState(),
                        currencyConverterState = CurrencyConverterState(),
                        wordClockState = WordClockState(),
                        dictionaryState = DictionaryState(),
                        calculatorState =
                            aliasState.lockedToolMode?.let(::createToolModeState)
                                ?: CalculatorState(),
                        webSuggestions = emptyList(),
                        webSuggestionsLoading = false,
                        isAppSearchInProgress = false,
                        detectedShortcutTarget = aliasState.lockedShortcutTarget,
                        detectedAliasSearchSection = aliasState.lockedAliasSearchSection,
                        isCurrencyConverterAliasMode = aliasState.lockedCurrencyConverterAlias,
                        isWordClockAliasMode = aliasState.lockedWordClockAlias,
                        isDictionaryAliasMode = aliasState.lockedDictionaryAlias,
                        detectedCustomToolId = aliasState.lockedCustomToolId,
                        webSuggestionWasSelected = false,
                    )
                }
                return
            }
            if (clearShortcutWhenBlank) {
                clearDetectedAliasMode()
            }
            val updatedAliasState = aliasStateProvider()
            appSearchJob?.cancel()
            appSearchManager.setNoMatchPrefix(null)
            secondarySearchOrchestrator.resetNoResultTracking()
            webSuggestionHandler.cancelSuggestions()
            val lockedMode = updatedAliasState.lockedToolMode
            updateUiState {
                it.copy(
                    query = "",
                    searchResults = emptyList(),
                    appShortcutResults = emptyList(),
                    contactResults = emptyList(),
                    fileResults = emptyList(),
                    settingResults = emptyList(),
                    appSettingResults = emptyList(),
                    calendarEvents = emptyList(),
                    noteResults = emptyList(),
                    AiSearchState = AiSearchState(),
                    currencyConverterState = CurrencyConverterState(),
                    wordClockState = WordClockState(),
                    dictionaryState = DictionaryState(),
                    calculatorState =
                        if (clearShortcutWhenBlank || lockedMode == null) {
                            CalculatorState()
                        } else {
                            createToolModeState(lockedMode)
                        },
                    webSuggestions = emptyList(),
                    webSuggestionsLoading = false,
                    isAppSearchInProgress = false,
                    detectedShortcutTarget =
                        if (clearShortcutWhenBlank) null else updatedAliasState.lockedShortcutTarget,
                    detectedAliasSearchSection =
                        if (clearShortcutWhenBlank) null else updatedAliasState.lockedAliasSearchSection,
                    isCurrencyConverterAliasMode =
                        !clearShortcutWhenBlank && updatedAliasState.lockedCurrencyConverterAlias,
                    isWordClockAliasMode =
                        !clearShortcutWhenBlank && updatedAliasState.lockedWordClockAlias,
                    isDictionaryAliasMode =
                        !clearShortcutWhenBlank && updatedAliasState.lockedDictionaryAlias,
                    detectedCustomToolId =
                        if (clearShortcutWhenBlank) null else updatedAliasState.lockedCustomToolId,
                    webSuggestionWasSelected = false,
                )
            }
            refreshRecentItems()
            refreshAliasRecentItems(updatedAliasState.lockedAliasSearchSection)
            return
        }

        when (val aliasResolution = resolveAliasQueryResolution(newQuery)) {
            is AliasQueryResolution.ReprocessQuery -> {
                onQueryChangeInternal(
                    aliasResolution.queryWithoutAlias,
                    clearShortcutWhenBlank = false,
                )
                return
            }

            is AliasQueryResolution.ExecuteSearchTarget -> {
                navigationHandler.openSearchTarget(
                    aliasResolution.queryWithoutAlias.trim(),
                    aliasResolution.target,
                )
                if (aliasResolution.queryWithoutAlias.isBlank()) {
                    clearQuery()
                } else {
                    onQueryChange(aliasResolution.queryWithoutAlias)
                }
                return
            }

            AliasQueryResolution.None -> Unit
        }

        val aliasState = aliasStateProvider()
        val detectedTarget: SearchTarget? = aliasState.lockedShortcutTarget
        val detectedAliasSearchSection: SearchSection? = aliasState.lockedAliasSearchSection

        val calculatorResult =
            resolveToolState(
                trimmedQuery = trimmedQuery,
                detectedTarget = detectedTarget,
                detectedAliasSearchSection = detectedAliasSearchSection,
                skipLocalTools =
                    aliasState.lockedCurrencyConverterAlias ||
                        aliasState.lockedWordClockAlias ||
                        aliasState.lockedDictionaryAlias ||
                        aliasState.lockedCustomToolId != null,
            )

        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(trimmedQuery)
        val queryContext = SearchQueryContext.fromNormalizedQuery(normalizedQuery)
        appSearchManager.resetNoMatchPrefixIfNeeded(normalizedQuery)

        val shouldSkipSearch = appSearchManager.shouldSkipDueToNoMatchPrefix(normalizedQuery)

        if (trimmedQuery.isNotBlank() && appShortcutSearchHandler.getAvailableShortcuts().isEmpty()) {
            loadAppShortcuts()
        }

        appSearchJob?.cancel()
        webSuggestionHandler.cancelSuggestions()

        val showingTool = calculatorResult.isToolMode || calculatorResult.result != null
        if (showingTool) {
            clearInformationCardsExcept(SearchViewModel.ActiveInformationCard.CALCULATOR)
        }
        val shouldOnlySearchApps = detectedAliasSearchSection == SearchSection.APPS
        val shouldSkipAppSearchDueToAlias =
            aliasState.lockedCurrencyConverterAlias ||
                aliasState.lockedWordClockAlias ||
                aliasState.lockedDictionaryAlias ||
                aliasState.lockedCustomToolId != null ||
                (detectedAliasSearchSection != null && !shouldOnlySearchApps)
        val shouldRunSecondarySearchBatch =
            !showingTool &&
                detectedTarget == null &&
                !aliasState.lockedCurrencyConverterAlias &&
                !aliasState.lockedWordClockAlias &&
                !aliasState.lockedDictionaryAlias &&
                aliasState.lockedCustomToolId == null &&
                detectedAliasSearchSection != SearchSection.APPS &&
                secondarySearchOrchestrator.willRunSecondarySearch(newQuery)
        val shouldRunAppSearch =
            !shouldSkipSearch &&
                detectedTarget == null &&
                !showingTool &&
                !shouldSkipAppSearchDueToAlias
        updateUiState { state ->
            state.copy(
                query = newQuery,
                searchResults =
                    if (
                        shouldSkipSearch ||
                            detectedTarget != null ||
                            showingTool ||
                            shouldSkipAppSearchDueToAlias
                    ) {
                        emptyList()
                    } else {
                        state.searchResults
                    },
                calculatorState = calculatorResult,
                webSuggestions = emptyList(),
                webSuggestionsLoading = false,
                isAppSearchInProgress = shouldRunAppSearch,
                isSecondarySearchInProgress =
                    if (shouldRunSecondarySearchBatch) {
                        state.isSecondarySearchInProgress
                    } else {
                        false
                    },
                detectedShortcutTarget = detectedTarget,
                detectedAliasSearchSection = detectedAliasSearchSection,
                isCurrencyConverterAliasMode = aliasState.lockedCurrencyConverterAlias,
                isWordClockAliasMode = aliasState.lockedWordClockAlias,
                isDictionaryAliasMode = aliasState.lockedDictionaryAlias,
                detectedCustomToolId = aliasState.lockedCustomToolId,
                // Keep stale secondary results during debounce so cards don't flicker.
                // When secondary search is not going to run (tool mode, alias mode, etc.),
                // clear them immediately since the orchestrator won't clean them up.
                contactResults = if (shouldRunSecondarySearchBatch) state.contactResults else emptyList(),
                fileResults = if (shouldRunSecondarySearchBatch) state.fileResults else emptyList(),
                settingResults = if (shouldRunSecondarySearchBatch) state.settingResults else emptyList(),
                appSettingResults = if (shouldRunSecondarySearchBatch) state.appSettingResults else emptyList(),
                appShortcutResults = if (shouldRunSecondarySearchBatch) state.appShortcutResults else emptyList(),
                calendarEvents = if (shouldRunSecondarySearchBatch) state.calendarEvents else emptyList(),
                noteResults = if (shouldRunSecondarySearchBatch) state.noteResults else emptyList(),
                aliasRecentItems = emptyList(),
            )
        }

        if (
            shouldRunAppSearch
        ) {
            val appsSnapshot = getSearchableAppsSnapshot()
            val gridLimit = getGridItemCount()
            val currentVersion = appSearchQueryVersion.incrementAndGet()

            appSearchJob =
                scope.launch(workerDispatcher) {
                    delay(appSearchDebounceMs)
                    if (currentVersion != appSearchQueryVersion.get()) return@launch

                    val results =
                        appSearchManager.deriveMatches(
                            queryContext,
                            appsSnapshot,
                            gridLimit,
                        )

                    if (currentVersion != appSearchQueryVersion.get()) return@launch

                    if (results.isEmpty() && normalizedQuery.length > 1) {
                        appSearchManager.setNoMatchPrefix(normalizedQuery)
                    } else if (normalizedQuery.length <= 1) {
                        appSearchManager.setNoMatchPrefix(null)
                    }

                    updateResultsState { state ->
                        state.copy(
                            searchResults = results,
                            pendingSearchResults = null,
                            isAppSearchInProgress = false,
                        )
                    }
                }
        } else if (shouldSkipSearch) {
            updateResultsState {
                it.copy(
                    searchResults = emptyList(),
                    isAppSearchInProgress = false,
                )
            }
        }

        if (!showingTool) {
            if (aliasState.lockedCurrencyConverterAlias) {
                secondarySearchOrchestrator.cancel()
                updateResultsState {
                    it.copy(
                        contactResults = emptyList(),
                        fileResults = emptyList(),
                        settingResults = emptyList(),
                        appSettingResults = emptyList(),
                        appShortcutResults = emptyList(),
                        calendarEvents = emptyList(),
                        noteResults = emptyList(),
                        webSuggestions = emptyList(),
                        webSuggestionsLoading = false,
                    )
                }
            } else if (aliasState.lockedWordClockAlias || aliasState.lockedDictionaryAlias || aliasState.lockedCustomToolId != null) {
                secondarySearchOrchestrator.cancel()
                updateResultsState {
                    it.copy(
                        contactResults = emptyList(),
                        fileResults = emptyList(),
                        settingResults = emptyList(),
                        appSettingResults = emptyList(),
                        appShortcutResults = emptyList(),
                        calendarEvents = emptyList(),
                        noteResults = emptyList(),
                        webSuggestions = emptyList(),
                        webSuggestionsLoading = false,
                    )
                }
            } else if (detectedTarget != null) {
                secondarySearchOrchestrator.performWebSuggestionsOnly(newQuery)
            } else if (detectedAliasSearchSection != null) {
                if (detectedAliasSearchSection == SearchSection.APPS) {
                    secondarySearchOrchestrator.cancel()
                    updateResultsState {
                        it.copy(
                            contactResults = emptyList(),
                            fileResults = emptyList(),
                            settingResults = emptyList(),
                            appSettingResults = emptyList(),
                            appShortcutResults = emptyList(),
                            calendarEvents = emptyList(),
                            noteResults = emptyList(),
                            webSuggestions = emptyList(),
                            webSuggestionsLoading = false,
                        )
                    }
                } else {
                    secondarySearchOrchestrator.performTargetedSecondarySearch(
                        query = newQuery,
                        section = detectedAliasSearchSection,
                        ignoreSectionToggle = true,
                    )
                }
            } else {
                secondarySearchOrchestrator.performSecondarySearches(newQuery)
            }
        }

        toolCoordinator.scheduleCurrencyConversion(
            trimmedQuery = trimmedQuery,
            showingTool = showingTool,
        )
        toolCoordinator.scheduleWordClock(
            trimmedQuery = trimmedQuery,
            showingTool = showingTool,
        )
        toolCoordinator.scheduleDictionaryLookup(
            trimmedQuery = trimmedQuery,
            showingTool = showingTool,
        )
    }
}
