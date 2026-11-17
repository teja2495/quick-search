package com.tk.quicksearch.settings.searchEngineSettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.components.ExpandableSegmentedGroup
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Collapsible card for the search engine section style: an Inline/Compact button group with a
 * line describing the selected style, and the row count group while Compact is selected.
 */
@Composable
fun SearchEngineAppearanceCard(
    isSearchEngineCompactMode: Boolean,
    onToggleSearchEngineCompactMode: (Boolean) -> Unit,
    compactRowCount: Int,
    onSetCompactRowCount: (Int) -> Unit,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ExpandableSegmentedGroup(
        title = stringResource(R.string.settings_search_engine_display_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        tileCount = 1,
        modifier = modifier,
        firstTileContent = {
            SearchEngineStyleChoices(
                compact = isSearchEngineCompactMode,
                onSelect = onToggleSearchEngineCompactMode,
            )
            Text(
                text =
                    stringResource(
                        if (isSearchEngineCompactMode) {
                            R.string.settings_search_engine_display_compact_desc
                        } else {
                            R.string.settings_search_engine_display_inline_desc
                        },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = DesignTokens.SpacingXSmall),
            )
            AnimatedVisibility(
                visible = isSearchEngineCompactMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                SearchEngineCompactRowChoices(
                    rowCount = compactRowCount,
                    onSelect = onSetCompactRowCount,
                )
            }
        },
    )
}

@Composable
internal fun SearchEngineStyleChoices(
    compact: Boolean,
    onSelect: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ConnectedChoiceGroup(
        options =
            listOf(
                ChoiceOption(false, stringResource(R.string.settings_search_engine_display_inline_title)),
                ChoiceOption(true, stringResource(R.string.settings_search_engine_display_compact_title)),
            ),
        selected = compact,
        enabled = enabled,
        onSelect = { selected -> if (selected != compact) onSelect(selected) },
    )
}

@Composable
internal fun SearchEngineCompactRowChoices(
    rowCount: Int,
    onSelect: (Int) -> Unit,
) {
    ConnectedChoiceGroup(
        options =
            listOf(
                ChoiceOption(1, stringResource(R.string.settings_compact_rows_one_row)),
                ChoiceOption(2, stringResource(R.string.settings_compact_rows_two_rows)),
            ),
        selected = if (rowCount == 2) 2 else 1,
        onSelect = onSelect,
    )
}
