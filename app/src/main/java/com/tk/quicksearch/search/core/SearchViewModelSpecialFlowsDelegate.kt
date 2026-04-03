package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal class SearchViewModelSpecialFlowsDelegate(
    private val scope: CoroutineScope,
    private val userPreferences: com.tk.quicksearch.search.data.UserAppPreferences,
    private val directSearchStateFlow: Flow<DirectSearchState>,
    private val clearDirectSearchState: () -> Unit,
    private val cancelInactiveTools: (SearchViewModel.ActiveInformationCard) -> Unit,
    private val shouldRecordPendingDirectSearchQueryInHistory: () -> Boolean,
    private val setShouldRecordPendingDirectSearchQueryInHistory: (Boolean) -> Unit,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val updateConfigState: ((SearchUiConfigState) -> SearchUiConfigState) -> Unit,
    private val updateFeatureState: ((SearchFeatureState) -> SearchFeatureState) -> Unit,
    private val resultsStateProvider: () -> SearchResultsState,
) {
    fun clearInformationCardsExcept(activeCard: SearchViewModel.ActiveInformationCard) {
        cancelInactiveTools(activeCard)
        if (
            activeCard != SearchViewModel.ActiveInformationCard.DIRECT_SEARCH &&
                resultsStateProvider().DirectSearchState.status != DirectSearchStatus.Idle
        ) {
            clearDirectSearchState()
        }

        updateResultsState { state ->
            state.copy(
                DirectSearchState =
                    if (activeCard == SearchViewModel.ActiveInformationCard.DIRECT_SEARCH) {
                        state.DirectSearchState
                    } else {
                        DirectSearchState()
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

    fun setupDirectSearchStateListener() {
        scope.launch {
            directSearchStateFlow.collect { dsState ->
                if (dsState.status == DirectSearchStatus.Loading) {
                    val activeQuery = dsState.activeQuery?.trim().orEmpty()
                    if (shouldRecordPendingDirectSearchQueryInHistory() && activeQuery.isNotEmpty()) {
                        userPreferences.addRecentItem(RecentSearchEntry.Query(activeQuery))
                    }
                    setShouldRecordPendingDirectSearchQueryInHistory(true)
                }
                if (dsState.status != DirectSearchStatus.Idle) {
                    clearInformationCardsExcept(SearchViewModel.ActiveInformationCard.DIRECT_SEARCH)
                }
                updateResultsState { it.copy(DirectSearchState = dsState) }
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
