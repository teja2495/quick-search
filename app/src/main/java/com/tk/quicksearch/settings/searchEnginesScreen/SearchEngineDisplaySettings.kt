package com.tk.quicksearch.settings.searchEnginesScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.util.hapticToggle

/**
 * Card for toggling the appearance of the search engine section (Compact vs Inline).
 */
@Composable
fun SearchEngineAppearanceCard(
    isSearchEngineCompactMode: Boolean,
    onToggleSearchEngineCompactMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_search_engine_display_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 16.dp),
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
                )
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
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = null, // Handled by Row clickable
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
