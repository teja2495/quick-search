package com.tk.quicksearch.settings.settingsDetailScreens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.settings.excludedItemsScreen.ExcludedItemScreen
import com.tk.quicksearch.util.hapticToggle
import com.tk.quicksearch.settings.shared.*


/**
 * Card for web suggestions and recent queries settings (without search engines navigation).
 */
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
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Web Search Suggestions Toggle Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWebSuggestionsToggle(!webSuggestionsEnabled) }
                    .padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.web_search_suggestions_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = webSuggestionsEnabled,
                    onCheckedChange = { enabled ->
                        hapticToggle(view)()
                        onWebSuggestionsToggle(enabled)
                    }
                )
            }

            // Web Suggestions Count Slider (only show if enabled)
            if (webSuggestionsEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Slider(
                        value = webSuggestionsCount.toFloat(),
                        onValueChange = { value ->
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
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            } else {
                // Divider (when slider is hidden)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // Recent Queries Toggle Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRecentQueriesToggle(!recentQueriesEnabled) }
                    .padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.recent_queries_toggle_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = recentQueriesEnabled,
                    onCheckedChange = { enabled ->
                        hapticToggle(view)()
                        onRecentQueriesToggle(enabled)
                    }
                )
            }

            // Recent Queries Count Slider (only show if enabled)
            if (recentQueriesEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Slider(
                        value = recentQueriesCount.toFloat(),
                        onValueChange = { value ->
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
 * Combined card for calculator toggle, auto expand results toggle, and excluded items navigation.
 */
@Composable
fun CombinedExcludedItemsCard(
    calculatorEnabled: Boolean,
    onToggleCalculator: (Boolean) -> Unit,
    showAllResults: Boolean,
    onToggleShowAllResults: (Boolean) -> Unit,
    excludedItemsTitle: String,
    excludedItemsDescription: String,
    onExcludedItemsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Calculator toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleCalculator(!calculatorEnabled) }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.calculator_toggle_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.calculator_toggle_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = calculatorEnabled,
                    onCheckedChange = { enabled ->
                        hapticToggle(view)()
                        onToggleCalculator(enabled)
                    }
                )
            }

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Auto expand results toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleShowAllResults(!showAllResults) }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_show_all_results_toggle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_show_all_results_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showAllResults,
                    onCheckedChange = { enabled ->
                        hapticToggle(view)()
                        onToggleShowAllResults(enabled)
                    }
                )
            }

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Excluded Items Section
            SettingsCardItemRow(
                item = SettingsCardItem(
                    title = excludedItemsTitle,
                    description = excludedItemsDescription,
                    actionOnPress = onExcludedItemsClick
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
}

/**
 * Search Results settings section that combines all search results related settings.
 */
@Composable
fun SearchResultsSettingsSection(
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search Sections Section
        SectionSettingsSection(
            sectionOrder = state.sectionOrder,
            disabledSections = state.disabledSections,
            onToggleSection = callbacks.onToggleSection,
            onReorderSections = callbacks.onReorderSections,
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

        // Combined Calculator, Auto Expand Results and Excluded Items Card
        val context = LocalContext.current
        CombinedExcludedItemsCard(
            calculatorEnabled = state.calculatorEnabled,
            onToggleCalculator = callbacks.onToggleCalculator,
            showAllResults = state.showAllResults,
            onToggleShowAllResults = callbacks.onToggleShowAllResults,
            excludedItemsTitle = stringResource(R.string.settings_excluded_items_title),
            excludedItemsDescription = stringResource(R.string.settings_excluded_items_desc),
            onExcludedItemsClick = {
                // Navigate to excluded items detail screen
                // This would need to be handled by the parent component
                // For now, show the inline screen if there are items, otherwise show toast
                val hasItems =
                    state.suggestionExcludedApps.isNotEmpty() ||
                    state.resultExcludedApps.isNotEmpty() ||
                    state.excludedContacts.isNotEmpty() ||
                    state.excludedFiles.isNotEmpty() ||
                    state.excludedSettings.isNotEmpty() ||
                    state.excludedAppShortcuts.isNotEmpty()
                if (hasItems) {
                    // Could navigate to detail screen, but for now show inline
                    // In a real implementation, this would navigate to SettingsDetailType.EXCLUDED_ITEMS
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_excluded_items_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier.padding(top = 12.dp)
        )

        // Show excluded items inline if there are any
        val hasExcludedItems = state.suggestionExcludedApps.isNotEmpty() ||
                               state.resultExcludedApps.isNotEmpty() ||
                               state.excludedContacts.isNotEmpty() ||
                               state.excludedFiles.isNotEmpty() ||
                               state.excludedSettings.isNotEmpty() ||
                               state.excludedAppShortcuts.isNotEmpty()

        if (hasExcludedItems) {
            ExcludedItemScreen(
                suggestionExcludedApps = state.suggestionExcludedApps,
                resultExcludedApps = state.resultExcludedApps,
                excludedContacts = state.excludedContacts,
                excludedFiles = state.excludedFiles,
                excludedFileExtensions = state.excludedFileExtensions,
                excludedSettings = state.excludedSettings,
                excludedAppShortcuts = state.excludedAppShortcuts,
                onRemoveSuggestionExcludedApp = callbacks.onRemoveSuggestionExcludedApp,
                onRemoveResultExcludedApp = callbacks.onRemoveResultExcludedApp,
                onRemoveExcludedContact = callbacks.onRemoveExcludedContact,
                onRemoveExcludedFile = callbacks.onRemoveExcludedFile,
                onRemoveExcludedFileExtension = callbacks.onRemoveExcludedFileExtension,
                onRemoveExcludedSetting = callbacks.onRemoveExcludedSetting,
                onRemoveExcludedAppShortcut = callbacks.onRemoveExcludedAppShortcut,
                onClearAll = callbacks.onClearAllExclusions,
                showTitle = false,
                iconPackPackage = state.selectedIconPackPackage
            )
        }
    }
}