package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tk.quicksearch.search.core.SectionRenderParams
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.rememberSectionRenderContext
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.other.OtherSearchItemRegistry

/**
 * Top matches built once per frame and shared by the keyboard submit path and the displayed
 * section, so Enter always opens what is on screen.
 */
internal data class SearchTopMatchesState(
    val regularRenderingState: SectionRenderingState,
    val settled: SettledTopMatchesState,
    val isLocalSearchRefreshing: Boolean,
)

internal data class TopMatchSubmission(
    val params: SectionRenderParams,
    val topMatches: SearchTopMatchesState,
    val matches: List<TopMatchItem>,
    val shouldSubmit: Boolean,
    val selectedIndex: Int?,
    val moveSelection: (Int) -> Boolean,
)

@Composable
internal fun rememberTopMatchSubmission(
    state: SearchUiState,
    renderingState: SectionRenderingState,
    filesParams: FilesSectionParams,
    contactsParams: ContactsSectionParams,
    settingsParams: SettingsSectionParams,
    calendarParams: CalendarSectionParams,
    notesParams: NotesSectionParams,
    remindersParams: RemindersSectionParams?,
    appShortcutsParams: AppShortcutsSectionParams,
    appsParams: AppsSectionParams,
    hideResultsForTopMatchSubmit: Boolean,
    expandedSection: ExpandedSection,
    isSearchHistoryExpanded: Boolean,
): TopMatchSubmission {
    val isLocalSearchRefreshing =
            shouldDeferTopMatchesForLocalSearch(
                    query = state.query,
                    isAppSearchInProgress = state.isAppSearchInProgress,
                    isSecondarySearchInProgress = state.isSecondarySearchInProgress,
            )
    // The settled state is what the results area shows; ranking from it keeps the submit target
    // and the highlighted row in step while sections refresh.
    val regularRenderingState =
            rememberSettledRegularSearchRenderingState(
                    query = state.query,
                    currentState = renderingState,
                    isAppSearchRefreshing = state.isAppSearchInProgress,
                    secondarySectionsRefreshing = state.secondarySearchSectionsInProgress,
            )
    val topMatchSubmitContext =
            rememberSectionRenderContext(
                    state = state,
                    renderingState = regularRenderingState,
                    filesParams = filesParams,
                    contactsParams = contactsParams,
                    settingsParams = settingsParams,
                    calendarParams = calendarParams,
                    notesParams = notesParams,
                    remindersParams = remindersParams,
                    appShortcutsParams = appShortcutsParams,
                    appsParams = appsParams,
                    isSearching = state.query.isNotBlank(),
                    oneHandedMode = state.oneHandedMode,
            )
    val topMatchSubmitParams =
            SectionRenderParams(
                    renderingState = renderingState,
                    contactsParams = contactsParams,
                    filesParams = filesParams,
                    appShortcutsParams = appShortcutsParams,
                    settingsParams = settingsParams,
                    calendarParams = calendarParams,
                    notesParams = notesParams,
                    remindersParams = remindersParams,
                    appsParams = appsParams,
                    isReversed = state.oneHandedMode,
                    bookmarksParams = com.tk.quicksearch.search.bookmarks.LocalBookmarksSectionParams.current,
                    notedParams = com.tk.quicksearch.search.noted.LocalNotedSectionParams.current,
            )
    val currentTopMatchesForSubmit =
            rememberTopMatches(
                    query = state.query,
                    renderingState = regularRenderingState,
                    context = topMatchSubmitContext,
                    params = topMatchSubmitParams,
                    limit = state.topMatchesLimit,
                    topMatchesSectionOrder = state.topMatchesSectionOrder,
                    disabledTopMatchesSections = state.disabledTopMatchesSections,
                    secondaryRankingSignal = state.secondaryRankingSignal,
                    filterStaleCandidates = isLocalSearchRefreshing,
                    otherSearchItemIds =
                        remember(state.query, state.pinnedNonAppItemOrder, state.screenTimeState) {
                            OtherSearchItemRegistry.visibleSearchItemIds(
                                query = state.query,
                                pinnedItemOrder = state.pinnedNonAppItemOrder,
                                screenTimeState = state.screenTimeState,
                            )
                        },
                )
    // Contact and file lookups debounce longer than the normal Top Matches deadline.
    val isProviderSearchRefreshing =
        state.topMatchesEnabled && state.secondarySearchSectionsInProgress.any { section ->
            (section == SearchSection.CONTACTS || section == SearchSection.FILES) &&
                section in state.topMatchesSectionOrder &&
                section !in state.disabledTopMatchesSections
        }
    val settledTopMatchesForSubmit =
            rememberSettledTopMatches(
                    query = state.query,
                    currentMatches = currentTopMatchesForSubmit,
                    isSearchRefreshing = isLocalSearchRefreshing,
                    isProviderSearchRefreshing = isProviderSearchRefreshing,
                    limit = state.topMatchesLimit,
            )
    val topMatchesForSubmit = settledTopMatchesForSubmit.matches
    val shouldSubmitTopMatch =
            state.topMatchesEnabled &&
                    state.query.isNotBlank() &&
                    !hideResultsForTopMatchSubmit &&
                    expandedSection == ExpandedSection.NONE &&
                    !isSearchHistoryExpanded &&
                    settledTopMatchesForSubmit.isReady &&
                    topMatchesForSubmit.isNotEmpty()
    val keyboardNavigableTopMatches =
            if (state.oneHandedMode) {
                topMatchesForSubmit.asReversed()
            } else {
                topMatchesForSubmit
            }
    var selectedTopMatchIndex by remember { mutableStateOf<Int?>(null) }
    var previousTopMatchQuery by remember { mutableStateOf(state.query) }

    LaunchedEffect(state.query, shouldSubmitTopMatch, topMatchesForSubmit) {
        if (!shouldSubmitTopMatch) {
            selectedTopMatchIndex = null
            previousTopMatchQuery = state.query
            return@LaunchedEffect
        }

        val queryChanged = state.query != previousTopMatchQuery
        selectedTopMatchIndex =
                when {
                    queryChanged -> 0
                    selectedTopMatchIndex in topMatchesForSubmit.indices -> selectedTopMatchIndex
                    else -> 0
                }
        previousTopMatchQuery = state.query
    }

    fun moveSelectedTopMatch(delta: Int): Boolean {
        if (!shouldSubmitTopMatch || keyboardNavigableTopMatches.isEmpty()) return false
        val displayedIndexByActualIndex =
                keyboardNavigableTopMatches
                        .mapIndexed { displayedIndex, item ->
                            topMatchesForSubmit.indexOf(item) to displayedIndex
                        }
                        .toMap()
        val currentDisplayedIndex =
                selectedTopMatchIndex
                        ?.let(displayedIndexByActualIndex::get)
                        ?: 0
        val nextDisplayedIndex =
                (currentDisplayedIndex + delta).coerceIn(0, keyboardNavigableTopMatches.lastIndex)
        val selectedItem = keyboardNavigableTopMatches[nextDisplayedIndex]
        selectedTopMatchIndex = topMatchesForSubmit.indexOf(selectedItem).takeIf { it >= 0 }
        return true
    }
    return TopMatchSubmission(
        params = topMatchSubmitParams,
        topMatches =
            SearchTopMatchesState(
                regularRenderingState = regularRenderingState,
                settled = settledTopMatchesForSubmit,
                isLocalSearchRefreshing = isLocalSearchRefreshing,
            ),
        matches = topMatchesForSubmit,
        shouldSubmit = shouldSubmitTopMatch,
        selectedIndex = selectedTopMatchIndex,
        moveSelection = ::moveSelectedTopMatch,
    )
}
