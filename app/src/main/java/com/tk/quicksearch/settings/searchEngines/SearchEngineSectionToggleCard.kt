package com.tk.quicksearch.settings.searchEngines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
 * Card for toggling the entire search engine section on/off.
 */
@Composable
fun SearchEngineSectionToggleCard(
    searchEngineSectionEnabled: Boolean,
    onToggleSearchEngineSectionEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SearchEngineSettingsSpacing.cardHorizontalPadding,
                    vertical = SearchEngineSettingsSpacing.cardTopPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_search_engines_section_toggle_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_search_engines_section_toggle_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = searchEngineSectionEnabled,
                onCheckedChange = { enabled ->
                    hapticToggle(view)()
                    onToggleSearchEngineSectionEnabled(enabled)
                }
            )
        }
    }
}