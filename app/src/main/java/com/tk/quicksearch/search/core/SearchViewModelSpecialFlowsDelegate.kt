package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SearchViewModelSpecialFlowsDelegate(
    private val scope: CoroutineScope,
    private val userPreferences: com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences,
    private val aiSearchStateFlow: Flow<AiSearchState>,
    private val clearAiSearchState: () -> Unit,
    private val cancelInactiveTools: (SearchViewModel.ActiveInformationCard) -> Unit,
    private val shouldRecordPendingAiSearchQueryInHistory: () -> Boolean,
    private val setShouldRecordPendingAiSearchQueryInHistory: (Boolean) -> Unit,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val updateConfigState: ((SearchUiConfigState) -> SearchUiConfigState) -> Unit,
    private val updateFeatureState: ((SearchFeatureState) -> SearchFeatureState) -> Unit,
    private val resultsStateProvider: () -> SearchResultsState,
) {
    private var lastAiHistoryWrite: Job? = null

    fun clearInformationCardsExcept(activeCard: SearchViewModel.ActiveInformationCard) {
        cancelInactiveTools(activeCard)
        if (
            activeCard != SearchViewModel.ActiveInformationCard.AI_SEARCH &&
                resultsStateProvider().AiSearchState.status != AiSearchStatus.Idle
        ) {
            clearAiSearchState()
        }

        updateResultsState { state ->
            state.copy(
                AiSearchState =
                    if (activeCard == SearchViewModel.ActiveInformationCard.AI_SEARCH) {
                        state.AiSearchState
                    } else {
                        AiSearchState()
                    },
                calculatorState =
                    if (activeCard == SearchViewModel.ActiveInformationCard.CALCULATOR) {
                        state.calculatorState
                    } else {
                        CalculatorState()
                    },
                currencyConverterState =
                    if (activeCard == SearchViewModel.ActiveInformationCard.CURRENCY_CONVERTER) {
                        state.currencyConverterState
                    } else {
                        CurrencyConverterState()
                    },
                worldClockState =
                    if (activeCard == SearchViewModel.ActiveInformationCard.WORD_CLOCK) {
                        state.worldClockState
                    } else {
                        WorldClockState()
                    },
                dictionaryState =
                    if (activeCard == SearchViewModel.ActiveInformationCard.DICTIONARY) {
                        state.dictionaryState
                    } else {
                        DictionaryState()
                    },
                weatherState =
                    if (activeCard == SearchViewModel.ActiveInformationCard.WEATHER) {
                        state.weatherState
                    } else {
                        WeatherState()
                    },
            )
        }
    }

    fun setupAiSearchStateListener() {
        scope.launch {
            aiSearchStateFlow.collect { aiState ->
                if (aiState.status == AiSearchStatus.Loading && !aiState.isFollowUp) {
                    val activeQuery = aiState.activeQuery?.trim().orEmpty()
                    if (shouldRecordPendingAiSearchQueryInHistory() && activeQuery.isNotEmpty()) {
                        recordAiSearchHistory(RecentSearchEntry.Query(activeQuery))
                    }
                    setShouldRecordPendingAiSearchQueryInHistory(true)
                }
                if (aiState.status == AiSearchStatus.Success && !aiState.isFollowUp) {
                    val activeQuery = aiState.activeQuery?.trim().orEmpty()
                    val answer = aiState.answer?.trim().orEmpty()
                    if (activeQuery.isNotEmpty() && answer.isNotEmpty()) {
                        recordAiSearchHistory(
                            RecentSearchEntry.Query(
                                query = activeQuery,
                                aiAnswer = answer,
                                aiUsedModelId = aiState.usedModelId,
                                aiLlmProviderId = aiState.llmProviderId,
                            ),
                        )
                    }
                }
                if (aiState.status != AiSearchStatus.Idle) {
                    clearInformationCardsExcept(SearchViewModel.ActiveInformationCard.AI_SEARCH)
                }
                updateResultsState { it.copy(AiSearchState = aiState) }
            }
        }
    }

    /**
     * Writes AI search history off the main thread. Each write waits for the previous one so the
     * answer snapshot always lands after the pending query entry, and finishes even if the
     * surface closes. Only called from the main-thread collector, so [lastAiHistoryWrite] needs
     * no lock.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun recordAiSearchHistory(entry: RecentSearchEntry) {
        val previous = lastAiHistoryWrite
        lastAiHistoryWrite =
            scope.launch(Dispatchers.IO, start = CoroutineStart.ATOMIC) {
                withContext(NonCancellable) {
                    previous?.join()
                    userPreferences.addRecentItem(entry)
                }
            }
    }

    fun shouldShowSearchBarWelcome(): Boolean {
        if (userPreferences.consumeForceSearchBarWelcomeOnNextOpen()) {
            userPreferences.setHasSeenSearchBarWelcome(true)
            return true
        }

        val hasSeen = userPreferences.hasSeenSearchBarWelcome()
        if (!hasSeen) {
            userPreferences.setHasSeenSearchBarWelcome(true)
            return true
        }
        return false
    }

    fun requestSearchBarWelcomeAnimationFromOnboarding() {
        scope.launch(Dispatchers.IO) {
            userPreferences.setForceSearchBarWelcomeOnNextOpen(true)
            userPreferences.setHasSeenSearchBarWelcome(true)
        }
        updateConfigState { it.copy(showSearchBarWelcomeAnimation = true) }
    }

    fun onSearchBarWelcomeAnimationCompleted() {
        scope.launch(Dispatchers.IO) {
            userPreferences.setForceSearchBarWelcomeOnNextOpen(false)
        }
        updateConfigState { it.copy(showSearchBarWelcomeAnimation = false) }
    }

}
