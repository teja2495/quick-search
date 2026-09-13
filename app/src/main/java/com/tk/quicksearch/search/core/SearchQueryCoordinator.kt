package com.tk.quicksearch.search.core

import android.os.SystemClock
import com.tk.quicksearch.search.apps.AppSearchPerformanceLogger
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
    val lockedWorldClockAlias: Boolean,
    val lockedDictionaryAlias: Boolean,
    val lockedWeatherAlias: Boolean,
    val lockedCustomToolId: String? = null,
    val lockedTaskerIntentId: String? = null,
)

internal class LatestSearchJobRunner<T>(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val debounceMs: Long,
) {
    private var job: Job? = null
    private val version = AtomicLong(0L)

    fun submit(
        compute: suspend () -> T,
        publish: (T) -> Unit,
    ) {
        cancel()
        val currentVersion = version.incrementAndGet()
        job =
            scope.launch(dispatcher) {
                delay(debounceMs)
                if (currentVersion != version.get()) return@launch
                val result = compute()
                if (currentVersion != version.get()) return@launch
                publish(result)
            }
    }

    fun cancel() {
        job?.cancel()
        version.incrementAndGet()
    }
}

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
    private val appSearchRunner =
        LatestSearchJobRunner<List<com.tk.quicksearch.search.models.AppInfo>>(
            scope = scope,
            dispatcher = workerDispatcher,
            debounceMs = appSearchDebounceMs,
        )

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
                isWorldClockAliasMode = false,
                isDictionaryAliasMode = false,
                isWeatherAliasMode = false,
                calculatorState = CalculatorState(),
                currencyConverterState = CurrencyConverterState(),
                worldClockState = WorldClockState(),
                dictionaryState = DictionaryState(),
                weatherState = WeatherState(),
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

    /** Activates the same locked mode used after a leading alias, without requiring an alias word. */
    fun activateGestureSearchTarget(target: SearchTarget) {
        setDetectedAliasMode(shortcutTarget = target, section = null, toolMode = null)
        refreshGestureAliasMode()
    }

    /** Activates the same tool mode used after a leading alias, without requiring an alias word. */
    fun activateGestureTool(featureId: String) {
        applyFeatureAliasMode(featureId)
        refreshGestureAliasMode()
    }

    /**
     * Alias words always change the query before their mode is applied. Gestures do not, so force
     * the blank-query alias rendering path to publish the newly locked mode to the UI.
     */
    private fun refreshGestureAliasMode() {
        val query = currentResultsStateProvider().query
        onQueryChangeInternal(" ", clearShortcutWhenBlank = false)
        if (query.isNotBlank()) onQueryChangeInternal(query, clearShortcutWhenBlank = false)
    }

    fun clearDetectedShortcut() {
        clearDetectedAliasMode()
        updateUiState {
            it.copy(
                detectedShortcutTarget = null,
                detectedAliasSearchSection = null,
                isCurrencyConverterAliasMode = false,
                isWorldClockAliasMode = false,
                isDictionaryAliasMode = false,
                isWeatherAliasMode = false,
                detectedCustomToolId = null,
                detectedTaskerIntentId = null,
                calculatorState = CalculatorState(),
                currencyConverterState = CurrencyConverterState(),
                worldClockState = WorldClockState(),
                dictionaryState = DictionaryState(),
                weatherState = WeatherState(),
            )
        }
    }

    fun clearQuery() {
        clearDetectedAliasMode()
        onQueryChangeInternal("", clearShortcutWhenBlank = true)
    }

    fun cancel() {
        cancelAppSearch()
    }

    private fun cancelAppSearch() {
        appSearchRunner.cancel()
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
                lockedWorldClockAlias = if (isExclusive) false else current.lockedWorldClockAlias,
                lockedDictionaryAlias = if (isExclusive) false else current.lockedDictionaryAlias,
                lockedWeatherAlias = if (isExclusive) false else current.lockedWeatherAlias,
                lockedCustomToolId = if (isExclusive) null else current.lockedCustomToolId,
                lockedTaskerIntentId = if (isExclusive) null else current.lockedTaskerIntentId,
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
                lockedWorldClockAlias = false,
                lockedDictionaryAlias = false,
                lockedWeatherAlias = false,
                lockedCustomToolId = null,
                lockedTaskerIntentId = null,
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
            aliasState.lockedWorldClockAlias ||
            aliasState.lockedDictionaryAlias ||
            aliasState.lockedWeatherAlias ||
            aliasState.lockedCustomToolId != null ||
            aliasState.lockedTaskerIntentId != null
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
        if (featureId.startsWith("tasker_intent:")) {
            val current = aliasStateProvider()
            updateAliasState(
                current.copy(
                    lockedShortcutTarget = null,
                    lockedAliasSearchSection = null,
                    lockedToolMode = null,
                    lockedCurrencyConverterAlias = false,
                    lockedWorldClockAlias = false,
                    lockedDictionaryAlias = false,
                    lockedWeatherAlias = false,
                    lockedCustomToolId = null,
                    lockedTaskerIntentId = featureId,
                ),
            )
            return
        }
        // Handle custom tool aliases
        if (featureId.startsWith("custom_tool:")) {
            if (!handlers.aiSearchHandler.hasAnyLlmApiKeyCached()) {
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
                    lockedWorldClockAlias = false,
                    lockedDictionaryAlias = false,
                    lockedWeatherAlias = false,
                    lockedCustomToolId = featureId,
                    lockedTaskerIntentId = null,
                ),
            )
            return
        }

        val definition = aliasHandler.getFeatureAliasDefinition(featureId)
        if (definition == null) {
            clearDetectedAliasMode()
            return
        }

        if (definition.requiresGeminiApiKey && !handlers.aiSearchHandler.hasAnyLlmApiKeyCached()) {
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
                    lockedWorldClockAlias =
                        standaloneMode == AliasHandler.StandaloneFeatureAliasMode.WORD_CLOCK,
                    lockedDictionaryAlias =
                        standaloneMode == AliasHandler.StandaloneFeatureAliasMode.DICTIONARY,
                    lockedWeatherAlias =
                        standaloneMode == AliasHandler.StandaloneFeatureAliasMode.WEATHER,
                    lockedCustomToolId = null,
                    lockedTaskerIntentId = null,
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
                lockedWorldClockAlias = false,
                lockedDictionaryAlias = false,
                lockedWeatherAlias = false,
                lockedCustomToolId = null,
                lockedTaskerIntentId = null,
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
            SearchToolType.COLOR_VISUALIZER ->
                CalculatorState(
                    isColorVisualizerMode = true,
                    toolType = SearchToolType.COLOR_VISUALIZER,
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
            when (val aliasResolution = resolveAliasQueryResolution(newQuery)) {
                is AliasQueryResolution.ReprocessQuery -> {
                    onQueryChangeInternal(
                        aliasResolution.queryWithoutAlias,
                        clearShortcutWhenBlank = false,
                    )
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
                }

                AliasQueryResolution.None -> Unit
            }
            return
        }
        val aiSearchState = currentResultsStateProvider().AiSearchState
        if (aiSearchState.status != AiSearchStatus.Idle &&
            (aiSearchState.activeQuery == null ||
                aiSearchState.activeQuery != trimmedQuery)
        ) {
            handlers.aiSearchHandler.clearAiSearchState()
            updateResultsState { it.copy(AiSearchState = AiSearchState()) }
        }

        val currencyState = currentResultsStateProvider().currencyConverterState
        if (currencyState.status != CurrencyConverterStatus.Idle &&
            (currencyState.activeQuery == null || currencyState.activeQuery != trimmedQuery)
        ) {
            updateResultsState { it.copy(currencyConverterState = CurrencyConverterState()) }
        }
        val worldClockState = currentResultsStateProvider().worldClockState
        if (worldClockState.status != WorldClockStatus.Idle &&
            (worldClockState.activeQuery == null || worldClockState.activeQuery != trimmedQuery)
        ) {
            updateResultsState { it.copy(worldClockState = WorldClockState()) }
        }
        val dictionaryState = currentResultsStateProvider().dictionaryState
        if (dictionaryState.status != DictionaryStatus.Idle &&
            (dictionaryState.activeQuery == null || dictionaryState.activeQuery != trimmedQuery)
        ) {
            updateResultsState { it.copy(dictionaryState = DictionaryState()) }
        }
        val weatherState = currentResultsStateProvider().weatherState
        if (weatherState.status != WeatherStatus.Idle &&
            (weatherState.activeQuery == null || weatherState.activeQuery != trimmedQuery)
        ) {
            updateResultsState { it.copy(weatherState = WeatherState()) }
        }

        if (trimmedQuery.isBlank()) {
            val aliasState = aliasStateProvider()
            val hasLockedAliasMode =
                aliasState.lockedShortcutTarget != null ||
                    aliasState.lockedAliasSearchSection != null ||
                    aliasState.lockedToolMode != null ||
                    aliasState.lockedCurrencyConverterAlias ||
                    aliasState.lockedWorldClockAlias ||
                    aliasState.lockedDictionaryAlias ||
                    aliasState.lockedWeatherAlias ||
                    aliasState.lockedCustomToolId != null
                    || aliasState.lockedTaskerIntentId != null
            if (clearShortcutWhenBlank && hasLockedAliasMode && newQuery.isNotEmpty()) {
                cancelAppSearch()
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
                        worldClockState = WorldClockState(),
                        dictionaryState = DictionaryState(),
                        weatherState = WeatherState(),
                        calculatorState =
                            aliasState.lockedToolMode?.let(::createToolModeState)
                                ?: CalculatorState(),
                        webSuggestions = emptyList(),
                        webSuggestionsLoading = false,
                        isAppSearchInProgress = false,
                        detectedShortcutTarget = aliasState.lockedShortcutTarget,
                        detectedAliasSearchSection = aliasState.lockedAliasSearchSection,
                        isCurrencyConverterAliasMode = aliasState.lockedCurrencyConverterAlias,
                        isWorldClockAliasMode = aliasState.lockedWorldClockAlias,
                        isDictionaryAliasMode = aliasState.lockedDictionaryAlias,
                        isWeatherAliasMode = aliasState.lockedWeatherAlias,
                        detectedCustomToolId = aliasState.lockedCustomToolId,
                        detectedTaskerIntentId = aliasState.lockedTaskerIntentId,
                        webSuggestionWasSelected = false,
                    )
                }
                return
            }
            if (clearShortcutWhenBlank) {
                clearDetectedAliasMode()
            }
            val updatedAliasState = aliasStateProvider()
            cancelAppSearch()
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
                    worldClockState = WorldClockState(),
                    dictionaryState = DictionaryState(),
                    weatherState = WeatherState(),
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
                    isWorldClockAliasMode =
                        !clearShortcutWhenBlank && updatedAliasState.lockedWorldClockAlias,
                    isDictionaryAliasMode =
                        !clearShortcutWhenBlank && updatedAliasState.lockedDictionaryAlias,
                    isWeatherAliasMode =
                        !clearShortcutWhenBlank && updatedAliasState.lockedWeatherAlias,
                    detectedCustomToolId =
                        if (clearShortcutWhenBlank) null else updatedAliasState.lockedCustomToolId,
                    detectedTaskerIntentId =
                        if (clearShortcutWhenBlank) null else updatedAliasState.lockedTaskerIntentId,
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
                        aliasState.lockedWorldClockAlias ||
                        aliasState.lockedDictionaryAlias ||
                        aliasState.lockedWeatherAlias ||
                        aliasState.lockedCustomToolId != null ||
                        aliasState.lockedTaskerIntentId != null,
            )

        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(trimmedQuery)
        val queryContext = SearchQueryContext.fromNormalizedQuery(normalizedQuery)
        appSearchManager.resetNoMatchPrefixIfNeeded(normalizedQuery)

        val shouldSkipSearch = appSearchManager.shouldSkipDueToNoMatchPrefix(normalizedQuery)

        if (trimmedQuery.isNotBlank() && appShortcutSearchHandler.getAvailableShortcuts().isEmpty()) {
            loadAppShortcuts()
        }

        cancelAppSearch()
        webSuggestionHandler.cancelSuggestions()

        val showingTool = calculatorResult.isToolMode || calculatorResult.result != null
        if (showingTool) {
            clearInformationCardsExcept(SearchViewModel.ActiveInformationCard.CALCULATOR)
        }
        val shouldOnlySearchApps = detectedAliasSearchSection == SearchSection.APPS
        val shouldSkipAppSearchDueToAlias =
            aliasState.lockedCurrencyConverterAlias ||
                aliasState.lockedWorldClockAlias ||
                aliasState.lockedDictionaryAlias ||
                aliasState.lockedWeatherAlias ||
                aliasState.lockedCustomToolId != null ||
                aliasState.lockedTaskerIntentId != null ||
                (detectedAliasSearchSection != null && !shouldOnlySearchApps)
        val shouldRunSecondarySearchBatch =
            !showingTool &&
                detectedTarget == null &&
                !aliasState.lockedCurrencyConverterAlias &&
                !aliasState.lockedWorldClockAlias &&
                !aliasState.lockedDictionaryAlias &&
                !aliasState.lockedWeatherAlias &&
                aliasState.lockedCustomToolId == null &&
                aliasState.lockedTaskerIntentId == null &&
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
                secondarySearchSectionsInProgress =
                    if (shouldRunSecondarySearchBatch) {
                        state.secondarySearchSectionsInProgress
                    } else {
                        emptySet()
                    },
                detectedShortcutTarget = detectedTarget,
                detectedAliasSearchSection = detectedAliasSearchSection,
                isCurrencyConverterAliasMode = aliasState.lockedCurrencyConverterAlias,
                isWorldClockAliasMode = aliasState.lockedWorldClockAlias,
                isDictionaryAliasMode = aliasState.lockedDictionaryAlias,
                isWeatherAliasMode = aliasState.lockedWeatherAlias,
                detectedCustomToolId = aliasState.lockedCustomToolId,
                detectedTaskerIntentId = aliasState.lockedTaskerIntentId,
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
            val submittedAtElapsedMs = SystemClock.elapsedRealtime()
            AppSearchPerformanceLogger.log {
                "querySearchSubmitted queryLength=${normalizedQuery.length} debounceMs=$appSearchDebounceMs"
            }
            appSearchRunner.submit(
                compute = {
                    val computationStartedAtElapsedMs = SystemClock.elapsedRealtime()
                    val appsSnapshot = getSearchableAppsSnapshot()
                    val gridLimit = getGridItemCount()
                    val results = appSearchManager.deriveMatches(
                        queryContext,
                        appsSnapshot,
                        gridLimit,
                    )
                    val computationElapsedMs = SystemClock.elapsedRealtime() - computationStartedAtElapsedMs
                    AppSearchPerformanceLogger.logTiming(
                        event = "querySearchComputed",
                        elapsedMs = computationElapsedMs,
                        slowThresholdMs = 50L,
                    ) {
                        "queueMs=${computationStartedAtElapsedMs - submittedAtElapsedMs} " +
                            "queryLength=${normalizedQuery.length} candidates=${appsSnapshot.size} " +
                            "limit=$gridLimit results=${results.size}"
                    }
                    results
                },
                publish = { results ->
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
                    AppSearchPerformanceLogger.logTiming(
                        event = "querySearchPublished",
                        elapsedMs = SystemClock.elapsedRealtime() - submittedAtElapsedMs,
                        slowThresholdMs = appSearchDebounceMs + 100L,
                    ) {
                            "queryLength=${normalizedQuery.length} results=${results.size}"
                    }
                },
            )
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
            } else if (aliasState.lockedWorldClockAlias || aliasState.lockedDictionaryAlias || aliasState.lockedWeatherAlias || aliasState.lockedCustomToolId != null || aliasState.lockedTaskerIntentId != null) {
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
        toolCoordinator.scheduleWorldClock(
            trimmedQuery = trimmedQuery,
            showingTool = showingTool,
        )
        toolCoordinator.scheduleDictionaryLookup(
            trimmedQuery = trimmedQuery,
            showingTool = showingTool,
        )
        toolCoordinator.scheduleWeatherLookup(
            trimmedQuery = trimmedQuery,
            showingTool = showingTool,
        )
    }
}
