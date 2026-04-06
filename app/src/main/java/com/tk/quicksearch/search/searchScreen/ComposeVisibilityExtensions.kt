package com.tk.quicksearch.search.searchScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.tk.quicksearch.search.core.*

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
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier,
            ) {
                compactContent()
            }
        }

        is SearchEnginesVisibility.Full -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier,
            ) {
                fullContent()
            }
        }

        is SearchEnginesVisibility.ShortcutDetected -> {
            key(enginesState.target) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = modifier,
                ) {
                    shortcutContent(enginesState.target)
                }
            }
        }
    }
}

/**
 * Checks if there are any search results (dynamic results from searching),
 * not including pinned/static content.
 */
fun hasAnySearchResults(state: SearchUiState): Boolean {
    val hasAppResults = state.searchResults.isNotEmpty()
    val hasContactResults = state.contactResults.isNotEmpty()
    val hasFileResults = state.fileResults.isNotEmpty()
    val hasSettingResults = state.settingResults.isNotEmpty()
    val hasAppSettingResults = state.appSettingResults.isNotEmpty()
    val hasAppShortcutResults = state.appShortcutResults.isNotEmpty()
    val hasCalendarResults = state.calendarEvents.isNotEmpty()
    val hasNoteResults = state.noteResults.isNotEmpty()

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
