package com.tk.quicksearch.settings.additionalSettingsScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.R
import com.tk.quicksearch.util.hapticConfirm

/**
 * Refresh Data Card with options to refresh Apps, Contacts, and Files data.
 */
@Composable
fun RefreshDataCard(
    onRefreshApps: () -> Unit,
    onRefreshContacts: () -> Unit,
    onRefreshFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Title
            Text(
                text = stringResource(R.string.settings_refresh_data_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = 20.dp,
                    top = 16.dp,
                    end = 20.dp,
                    bottom = 12.dp
                )
            )

            // Horizontal row with three columns for Apps, Contacts, Files
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RefreshOption(
                    icon = Icons.Rounded.GridView,
                    title = stringResource(R.string.settings_refresh_apps_title),
                    onClick = onRefreshApps,
                    modifier = Modifier.weight(1f)
                )

                // Vertical divider
                VerticalDivider(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                RefreshOption(
                    icon = Icons.Rounded.Person,
                    title = stringResource(R.string.settings_refresh_contacts_title),
                    onClick = onRefreshContacts,
                    modifier = Modifier.weight(1f)
                )

                // Vertical divider
                VerticalDivider(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                RefreshOption(
                    icon = Icons.Rounded.InsertDriveFile,
                    title = stringResource(R.string.settings_refresh_files_title),
                    onClick = onRefreshFiles,
                    modifier = Modifier.weight(1f)
                )
            }

            // Add space below the options
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Combined assistant card for default assistant and quick settings tile settings.
 */
@Composable
fun CombinedAssistantCard(
    isDefaultAssistant: Boolean,
    onSetDefaultAssistant: () -> Unit,
    onAddQuickSettingsTile: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Default Assistant Section
            NavigationSection(
                title = stringResource(R.string.settings_default_assistant_title),
                description = stringResource(
                    if (isDefaultAssistant) {
                        R.string.settings_default_assistant_desc_change
                    } else {
                        R.string.settings_default_assistant_desc
                    }
                ),
                onClick = onSetDefaultAssistant
            )

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Quick Settings Tile Section
            NavigationSection(
                title = stringResource(R.string.settings_quick_settings_tile_title),
                description = stringResource(R.string.settings_quick_settings_tile_desc),
                onClick = onAddQuickSettingsTile
            )
        }
    }
}

/**
 * Refresh option component for individual data refresh buttons.
 */
@Composable
private fun RefreshOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Column(
        modifier = modifier
            .clickable {
                hapticConfirm(view)()
                onClick()
            }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Reusable navigation section for settings cards.
 * Shows a title, description, and chevron icon with click handling.
 */
@Composable
private fun NavigationSection(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 16.dp)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = stringResource(R.string.desc_navigate_forward),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}