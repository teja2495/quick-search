package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal class SearchViewModelSpecialFlowsDelegate(
    private val scope: CoroutineScope,
    private val userPreferences: com.tk.quicksearch.search.data.UserAppPreferences,
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
                wordClockState =
                    if (activeCard == SearchViewModel.ActiveInformationCard.WORD_CLOCK) {
                        state.wordClockState
                    } else {
                        WordClockState()
                    },
                dictionaryState =
                    if (activeCard == SearchViewModel.ActiveInformationCard.DICTIONARY) {
                        state.dictionaryState
                    } else {
                        DictionaryState()
                    },
            )
        }
    }

    fun setupAiSearchStateListener() {
        scope.launch {
            aiSearchStateFlow.collect { aiState ->
                if (aiState.status == AiSearchStatus.Loading) {
                    val activeQuery = aiState.activeQuery?.trim().orEmpty()
                    if (shouldRecordPendingAiSearchQueryInHistory() && activeQuery.isNotEmpty()) {
                        userPreferences.addRecentItem(RecentSearchEntry.Query(activeQuery))
                    }
                    setShouldRecordPendingAiSearchQueryInHistory(true)
                }
                if (aiState.status != AiSearchStatus.Idle) {
                    clearInformationCardsExcept(SearchViewModel.ActiveInformationCard.AI_SEARCH)
                }
                updateResultsState { it.copy(AiSearchState = aiState) }
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

    fun resetUsagePermissionBannerSessionDismissed() {
        userPreferences.resetUsagePermissionBannerSessionDismissed()
        refreshUsagePermissionBannerState()
    }

    fun incrementUsagePermissionBannerDismissCount() {
        userPreferences.incrementUsagePermissionBannerDismissCount()
        refreshUsagePermissionBannerState()
    }

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) {
        userPreferences.setUsagePermissionBannerSessionDismissed(dismissed)
        refreshUsagePermissionBannerState()
    }

    fun refreshUsagePermissionBannerState() {
        updateFeatureState {
            it.copy(
                shouldShowUsagePermissionBanner =
                    userPreferences.shouldShowUsagePermissionBanner(),
            )
        }
    }
}
