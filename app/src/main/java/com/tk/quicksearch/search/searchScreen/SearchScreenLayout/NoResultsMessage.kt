package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.hasAnySearchResults
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.delay

@Composable
internal fun NoResultsMessage(state: SearchUiState) {
    // Determine whether to show "No results" message when there's a query but no results and no
    // search engine shortcut is detected
    val shouldShowNoResults =
        remember(
            state.query,
            state.webSuggestionsEnabled,
            state.webSuggestions,
            state.detectedShortcutTarget,
        ) {
            val hasAnySearchResults = hasAnySearchResults(state)
            val trimmedQuery = state.query.trim()
            val queryLength = trimmedQuery.length
            trimmedQuery.isNotBlank() &&
                    !hasAnySearchResults &&
                    state.detectedShortcutTarget == null &&
                    (
                            !state.webSuggestionsEnabled ||
                                    (queryLength >= 2 && state.webSuggestions.isEmpty())
                    )
        }

    // Add delay before showing no results text to avoid flashing before
    // web suggestions load (only when web suggestions are enabled)
    var showNoResultsText by remember { mutableStateOf(false) }
    LaunchedEffect(shouldShowNoResults, state.query, state.webSuggestionsEnabled) {
        if (shouldShowNoResults) {
            // Only delay if web suggestions are enabled and might still be loading
            if (state.webSuggestionsEnabled) {
                delay(500L) // Wait for web suggestions load
            }
            showNoResultsText = true
        } else {
            showNoResultsText = false
        }
    }

    if (showNoResultsText) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Text(
                text = stringResource(R.string.no_results_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier =
                    Modifier.padding(
                        top = DesignTokens.SpacingSmall,
                        start = DesignTokens.SpacingLarge,
                        end = DesignTokens.SpacingLarge,
                    ),
            )
        }
    }
}