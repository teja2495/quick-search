package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tk.quicksearch.search.core.SectionRenderParams
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.rememberSectionRenderContext
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.other.OtherSearchItemRegistry

internal data class TopMatchSubmission(
    val params: SectionRenderParams,
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
    val topMatchSubmitContext =
            rememberSectionRenderContext(
                    state = state,
                    renderingState = renderingState,
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
            )
    val currentTopMatchesForSubmit =
            rememberTopMatches(
                    query = state.query,
                    renderingState = renderingState,
                    context = topMatchSubmitContext,
                    params = topMatchSubmitParams,
                    limit = state.topMatchesLimit,
                    topMatchesSectionOrder = state.topMatchesSectionOrder,
                    disabledTopMatchesSections = state.disabledTopMatchesSections,
                    secondaryRankingSignal = state.secondaryRankingSignal,
                    filterStaleCandidates = isLocalSearchRefreshing,
                    otherSearchItemIds =
                        OtherSearchItemRegistry.visibleSearchItemIds(
                            query = state.query,
                            pinnedItemOrder = state.pinnedNonAppItemOrder,
                            screenTimeState = state.screenTimeState,
                        ),
                )
    val settledTopMatchesForSubmit =
            rememberSettledTopMatches(
                    query = state.query,
                    currentMatches = currentTopMatchesForSubmit,
                    isSearchRefreshing = isLocalSearchRefreshing,
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
        matches = topMatchesForSubmit,
        shouldSubmit = shouldSubmitTopMatch,
        selectedIndex = selectedTopMatchIndex,
        moveSelection = ::moveSelectedTopMatch,
    )
}
