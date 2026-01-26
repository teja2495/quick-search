package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.ItemPriorityConfig
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.util.hapticToggle

/** Card for web suggestions and recent queries settings (without search engines navigation). */
@Composable
fun WebSuggestionsCard(
        webSuggestionsEnabled: Boolean,
        onWebSuggestionsToggle: (Boolean) -> Unit,
        webSuggestionsCount: Int,
        onWebSuggestionsCountChange: (Int) -> Unit,
        recentQueriesEnabled: Boolean,
        onRecentQueriesToggle: (Boolean) -> Unit,
        recentQueriesCount: Int,
        onRecentQueriesCountChange: (Int) -> Unit,
        modifier: Modifier = Modifier,
        contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
) {
    val view = LocalView.current
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column {
            // Web Search Suggestions Toggle Section
            SettingsToggleRow(
                    title = stringResource(R.string.web_search_suggestions_title),
                    checked = webSuggestionsEnabled,
                    onCheckedChange = onWebSuggestionsToggle,
                    isFirstItem = true,
                    isLastItem = false,
                    showDivider = false
            )

            // Web Suggestions Count Slider (only show if enabled)
            if (webSuggestionsEnabled) {
                var lastWebStep by remember { mutableStateOf(webSuggestionsCount) }
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(
                                                start = 24.dp,
                                                end = 24.dp,
                                                top = 0.dp,
                                                bottom = 16.dp
                                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Slider(
                            value = webSuggestionsCount.toFloat(),
                            onValueChange = { value ->
                                val step = value.toInt()
                                if (step != lastWebStep) {
                                    hapticToggle(view)()
                                    lastWebStep = step
                                }
                                onWebSuggestionsCountChange(value.toInt())
                            },
                            valueRange = 1f..5f,
                            steps = 3, // 1, 2, 3, 4, 5
                            modifier = Modifier.weight(1f)
                    )
                    Text(
                            text = webSuggestionsCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp)
                    )
                }

                // Divider
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            } else {
                // Divider (when slider is hidden)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Recent Queries Toggle Section
            SettingsToggleRow(
                    title = stringResource(R.string.recent_queries_toggle_title),
                    checked = recentQueriesEnabled,
                    onCheckedChange = onRecentQueriesToggle,
                    isFirstItem = false,
                    isLastItem = false,
                    showDivider = false
            )

            // Recent Queries Count Slider (only show if enabled)
            if (recentQueriesEnabled) {
                var lastRecentStep by remember { mutableStateOf(recentQueriesCount) }
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(
                                                start = 24.dp,
                                                end = 24.dp,
                                                top = 0.dp,
                                                bottom = 16.dp
                                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Slider(
                            value = recentQueriesCount.toFloat(),
                            onValueChange = { value ->
                                val step = value.toInt()
                                if (step != lastRecentStep) {
                                    hapticToggle(view)()
                                    lastRecentStep = step
                                }
                                onRecentQueriesCountChange(value.toInt())
                            },
                            valueRange = 1f..5f,
                            steps = 3, // 1, 2, 3, 4, 5
                            modifier = Modifier.weight(1f)
                    )
                    Text(
                            text = recentQueriesCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Combined card for calculator toggle and excluded items navigation.
 */
@Composable
fun CombinedExcludedItemsCard(
        calculatorEnabled: Boolean,
        onToggleCalculator: (Boolean) -> Unit,
        excludedItemsTitle: String,
        excludedItemsDescription: String,
        onNavigateToExcludedItems: () -> Unit,
        modifier: Modifier = Modifier
) {
    val view = LocalView.current
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column {
            // Calculator toggle
            SettingsToggleRow(
                    title = stringResource(R.string.calculator_toggle_title),
                    subtitle = stringResource(R.string.calculator_toggle_desc),
                    checked = calculatorEnabled,
                    onCheckedChange = onToggleCalculator,
                    leadingIcon = Icons.Rounded.Calculate,
                    isFirstItem = false,
                    isLastItem = false,
                    extraVerticalPadding = 4.dp
            )

            // Excluded Items Section
            SettingsNavigationRow(
                    item =
                            SettingsCardItem(
                                    title = excludedItemsTitle,
                                    description = excludedItemsDescription,
                                    icon = Icons.Rounded.VisibilityOff,
                                    actionOnPress = onNavigateToExcludedItems
                            ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
}

/** Search Results settings section that combines all search results related settings. */
@Composable
fun SearchResultsSettingsSection(
        state: SettingsScreenState,
        callbacks: SettingsScreenCallbacks,
        onNavigateToExcludedItems: () -> Unit,
        modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search Sections Section
        SectionSettingsSection(
                sectionOrder = ItemPriorityConfig.getSearchResultsPriority(),
                disabledSections = state.disabledSections,
                onToggleSection = callbacks.onToggleSection,
                showTitle = false
        )

        // Web Suggestions Card
        WebSuggestionsCard(
                webSuggestionsEnabled = state.webSuggestionsEnabled,
                onWebSuggestionsToggle = callbacks.onToggleWebSuggestions,
                webSuggestionsCount = state.webSuggestionsCount,
                onWebSuggestionsCountChange = callbacks.onWebSuggestionsCountChange,
                recentQueriesEnabled = state.recentQueriesEnabled,
                onRecentQueriesToggle = callbacks.onToggleRecentQueries,
                recentQueriesCount = state.recentQueriesCount,
                onRecentQueriesCountChange = callbacks.onRecentQueriesCountChange,
                modifier = Modifier.padding(top = 12.dp)
        )

        // Combined Calculator and Excluded Items Card
        CombinedExcludedItemsCard(
                calculatorEnabled = state.calculatorEnabled,
                onToggleCalculator = callbacks.onToggleCalculator,
                excludedItemsTitle = stringResource(R.string.settings_excluded_items_title),
                excludedItemsDescription = stringResource(R.string.settings_excluded_items_desc),
                onNavigateToExcludedItems = onNavigateToExcludedItems,
                modifier = Modifier.padding(top = 12.dp)
        )
    }
}
