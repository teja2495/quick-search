package com.tk.quicksearch.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.R
import com.tk.quicksearch.util.hapticToggle
import com.tk.quicksearch.util.hapticConfirm

/**
 * Combined navigation card for excluded items and additional settings with divider.
 */
@Composable
fun CombinedSettingsNavigationCard(
    excludedItemsTitle: String,
    excludedItemsDescription: String,
    additionalSettingsTitle: String,
    additionalSettingsDescription: String,
    onExcludedItemsClick: () -> Unit,
    onAdditionalSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        // Excluded Items Section (always shown)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExcludedItemsClick)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = excludedItemsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = excludedItemsDescription,
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

        // Divider
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Additional Settings Section (always shown)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAdditionalSettingsClick)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = additionalSettingsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = additionalSettingsDescription,
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
}

/**
 * Refresh Data Card with options to refresh Apps, Contacts, and Files data.
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

@Composable
fun WebSuggestionsToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.web_search_suggestions_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = enabled,
                onCheckedChange = { newValue ->
                    hapticToggle(view)()
                    onToggle(newValue)
                }
            )
        }
    }
}

@Composable
fun CombinedSearchEnginesCard(
    searchEnginesTitle: String,
    searchEnginesDescription: String,
    onSearchEnginesClick: () -> Unit,
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
            // Search Engines Section (with navigation)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSearchEnginesClick)
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = searchEnginesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = searchEnginesDescription,
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

            // Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

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

@Composable
fun CombinedAppearanceCard(
    keyboardAlignedLayout: Boolean,
    onToggleKeyboardAlignedLayout: (Boolean) -> Unit,
    showWallpaperBackground: Boolean,
    onToggleShowWallpaperBackground: (Boolean) -> Unit,
    hasFilePermission: Boolean = true,
    iconPackTitle: String,
    iconPackDescription: String,
    onIconPackClick: () -> Unit,
    onRefreshIconPacks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Wallpaper background toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Always call the callback - it will handle permission request if needed
                        onToggleShowWallpaperBackground(!showWallpaperBackground)
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_wallpaper_background_toggle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = showWallpaperBackground && hasFilePermission,
                    onCheckedChange = { enabled ->
                        hapticToggle(view)()
                        onToggleShowWallpaperBackground(enabled)
                    }
                )
            }

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Results alignment toggle
            SettingsToggleRow(
                title = stringResource(R.string.settings_layout_option_bottom_title),
                subtitle = stringResource(R.string.settings_layout_option_bottom_desc),
                checked = keyboardAlignedLayout,
                onCheckedChange = onToggleKeyboardAlignedLayout,
                showDivider = false,
                extraVerticalPadding = 8.dp,
            )

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Icon Pack Section (with navigation)
            val hasIconPacks = iconPackDescription != stringResource(R.string.settings_icon_pack_empty)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onIconPackClick)
                    .padding(
                        start = 24.dp,
                        top = 16.dp,
                        end = 24.dp,
                        bottom = if (hasIconPacks) 16.dp else 20.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = iconPackTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = iconPackDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (hasIconPacks) Icons.Rounded.ChevronRight else Icons.Rounded.Refresh,
                    contentDescription = if (hasIconPacks) stringResource(R.string.desc_navigate_forward) else stringResource(R.string.settings_refresh_icon_packs),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .then(
                            if (!hasIconPacks) Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onRefreshIconPacks
                            )
                            else Modifier
                        )
                )
            }
        }
    }
}

@Composable
fun CalculatorToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!enabled) }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.calculator_toggle_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { newValue ->
                    hapticToggle(view)()
                    onToggle(newValue)
                }
            )
        }
    }
}

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSetDefaultAssistant)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_default_assistant_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            if (isDefaultAssistant) {
                                R.string.settings_default_assistant_desc_change
                            } else {
                                R.string.settings_default_assistant_desc
                            }
                        ),
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

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Quick Settings Tile Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddQuickSettingsTile)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_quick_settings_tile_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_quick_settings_tile_desc),
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
    }
}
