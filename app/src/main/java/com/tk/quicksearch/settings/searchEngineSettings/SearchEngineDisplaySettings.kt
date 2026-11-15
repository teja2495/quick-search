package com.tk.quicksearch.settings.searchEngineSettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsChoice
import com.tk.quicksearch.settings.shared.SettingsChoiceGroup
import com.tk.quicksearch.settings.shared.SettingsSegment
import com.tk.quicksearch.settings.shared.SettingsSegmentGroup
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle

/**
 * Card for toggling the appearance of the search engine section (Compact vs Inline).
 */
@Composable
fun SearchEngineAppearanceCard(
    isSearchEngineCompactMode: Boolean,
    onToggleSearchEngineCompactMode: (Boolean) -> Unit,
    compactRowCount: Int,
    onSetCompactRowCount: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    // One tile: the two display options are a single choice.
    SettingsSegmentGroup(modifier = modifier) {
      SettingsSegment(index = 0, count = 1) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = DesignTokens.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Text(
                text = stringResource(R.string.settings_search_engine_display_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.padding(
                        start = DesignTokens.SpacingXLarge,
                        top = DesignTokens.SpacingSmall,
                        end = DesignTokens.SpacingLarge,
                    ),
            )

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                SearchEngineDisplayOption(
                    title = stringResource(R.string.settings_search_engine_display_inline_title),
                    description = stringResource(R.string.settings_search_engine_display_inline_desc),
                    selected = !isSearchEngineCompactMode,
                    onClick = {
                        if (isSearchEngineCompactMode) {
                            hapticToggle(view)()
                            onToggleSearchEngineCompactMode(false)
                        }
                    },
                )

                SearchEngineDisplayOption(
                    title = stringResource(R.string.settings_search_engine_display_compact_title),
                    description = stringResource(R.string.settings_search_engine_display_compact_desc),
                    selected = isSearchEngineCompactMode,
                    onClick = {
                        if (!isSearchEngineCompactMode) {
                            hapticToggle(view)()
                            onToggleSearchEngineCompactMode(true)
                        }
                    },
                    extraContent = {
                        CompactRowCountPills(
                            selectedRowCount = compactRowCount,
                            onSelectRowCount = onSetCompactRowCount,
                        )
                    },
                )
            }
        }
      }
    }
}

@Composable
private fun SearchEngineDisplayOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    start = DesignTokens.SpacingXLarge,
                    end = DesignTokens.SpacingLarge,
                    top = DesignTokens.SpacingMedium,
                    bottom = DesignTokens.SpacingMedium,
                ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
            androidx.compose.material3.RadioButton(
                selected = selected,
                onClick = null, // Handled by Row clickable
            )
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (selected && extraContent != null) {
            extraContent()
        }
    }
}

@Composable
private fun CompactRowCountPills(
    selectedRowCount: Int,
    onSelectRowCount: (Int) -> Unit,
) {
    SettingsChoiceGroup(
        options =
            listOf(
                SettingsChoice(1, stringResource(R.string.settings_compact_rows_one_row)),
                SettingsChoice(2, stringResource(R.string.settings_compact_rows_two_rows)),
            ),
        selected = if (selectedRowCount == 2) 2 else 1,
        onSelect = onSelectRowCount,
        fillWidth = false,
        modifier = Modifier.padding(start = DesignTokens.IconSizeXLarge),
    )
}
