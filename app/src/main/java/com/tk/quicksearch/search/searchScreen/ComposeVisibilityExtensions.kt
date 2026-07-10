package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.shared.featureFlags.FeatureFlags

/**
 * Composable extension functions for conditional rendering based on visibility states.
 * Provides clean, type-safe alternatives to manual if-else visibility logic.
 */

// Section-specific visibility helpers

/**
 * Shows search engines based on their visibility state.
 */
@Composable
fun SearchEnginesVisibility(
    enginesState: SearchEnginesVisibility,
    modifier: Modifier = Modifier,
    hiddenContent: @Composable () -> Unit = {},
    compactContent: @Composable () -> Unit = {},
    fullContent: @Composable () -> Unit = {},
    shortcutContent: @Composable (SearchTarget) -> Unit = {},
) {
    when (enginesState) {
        is SearchEnginesVisibility.Hidden -> {
            hiddenContent()
        }

        is SearchEnginesVisibility.Compact -> {
            compactContent()
        }

        is SearchEnginesVisibility.Full -> {
            fullContent()
        }

        is SearchEnginesVisibility.ShortcutDetected -> {
            key(enginesState.target) {
                shortcutContent(enginesState.target)
            }
        }
    }
}

/**
 * Checks if there are any search results (dynamic results from searching),
 * not including pinned/static content.
 */
fun hasAnySearchResults(state: SearchUiState): Boolean {
    val hasAppResults =
        hasVisibleResultsForSection(
            state = state,
            section = SearchSection.APPS,
            hasResults = state.searchResults.isNotEmpty(),
        )
    val hasContactResults =
        hasVisibleResultsForSection(
            state = state,
            section = SearchSection.CONTACTS,
            hasResults = state.contactResults.isNotEmpty(),
        )
    val hasFileResults =
        hasVisibleResultsForSection(
            state = state,
            section = SearchSection.FILES,
            hasResults = state.fileResults.isNotEmpty(),
        )
    val hasSettingResults =
        hasVisibleResultsForSection(
            state = state,
            section = SearchSection.SETTINGS,
            hasResults = state.settingResults.isNotEmpty(),
        )
    val hasAppSettingResults =
        hasVisibleResultsForSection(
            state = state,
            section = SearchSection.APP_SETTINGS,
            hasResults = state.appSettingResults.isNotEmpty(),
        )
    val hasAppShortcutResults =
        hasVisibleResultsForSection(
            state = state,
            section = SearchSection.APP_SHORTCUTS,
            hasResults = state.appShortcutResults.isNotEmpty(),
        )
    val hasCalendarResults =
        hasVisibleResultsForSection(
            state = state,
            section = SearchSection.CALENDAR,
            hasResults = state.calendarEvents.isNotEmpty(),
        )
    val hasNoteResults =
        FeatureFlags.isSearchSectionEnabled(SearchSection.NOTES) &&
            hasVisibleResultsForSection(
                state = state,
                section = SearchSection.NOTES,
                hasResults = state.noteResults.isNotEmpty(),
            )

    val hasResults =
        hasAppResults ||
            hasContactResults ||
            hasFileResults ||
            hasSettingResults ||
            hasAppSettingResults ||
            hasAppShortcutResults ||
            hasCalendarResults ||
            hasNoteResults

    return hasResults
}

private fun hasVisibleResultsForSection(
    state: SearchUiState,
    section: SearchSection,
    hasResults: Boolean,
): Boolean {
    if (!hasResults) return false
    if (state.detectedAliasSearchSection == section) return true
    if (state.query.isBlank()) return true
    if (section !in state.disabledSections) return true
    return state.topMatchesEnabled &&
        section !in state.disabledTopMatchesSections &&
        section in state.topMatchesSectionOrder
}
