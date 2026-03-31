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
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.delay

private const val NO_RESULTS_DELAY_MS = 500L

internal fun computeShouldShowNoResults(state: SearchUiState): Boolean {
    val hasAnySearchResults = hasAnySearchResults(state)
    val trimmedQuery = state.query.trim()
    val queryLength = trimmedQuery.length
    return trimmedQuery.isNotBlank() &&
        !hasAnySearchResults &&
        state.detectedShortcutTarget == null &&
        state.detectedAliasSearchSection == null &&
        !state.isCurrencyConverterAliasMode &&
        (
            !state.webSuggestionsEnabled ||
                (queryLength >= 2 && state.webSuggestions.isEmpty())
        )
}

@Composable
internal fun rememberNoResultsTextVisible(
    shouldShowNoResults: Boolean,
    query: String,
    webSuggestionsEnabled: Boolean,
): Boolean {
    var showNoResultsText by remember { mutableStateOf(false) }
    LaunchedEffect(shouldShowNoResults, query, webSuggestionsEnabled) {
        if (shouldShowNoResults) {
            if (webSuggestionsEnabled) {
                delay(NO_RESULTS_DELAY_MS)
            }
            showNoResultsText = true
        } else {
            showNoResultsText = false
        }
    }
    return showNoResultsText
}

@Composable
internal fun NoResultsMessage(state: SearchUiState) {
    val shouldShowNoResults =
        remember(
            state.query,
            state.webSuggestionsEnabled,
            state.webSuggestions,
            state.detectedShortcutTarget,
            state.detectedAliasSearchSection,
            state.isCurrencyConverterAliasMode,
        ) {
            computeShouldShowNoResults(state)
        }
    val showNoResultsText =
        rememberNoResultsTextVisible(
            shouldShowNoResults = shouldShowNoResults,
            query = state.query,
            webSuggestionsEnabled = state.webSuggestionsEnabled,
        )

    if (showNoResultsText) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Text(
                text = stringResource(R.string.no_results_found),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.wallpaperAwareMutedSearchForeground(alpha = 0.6f),
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
