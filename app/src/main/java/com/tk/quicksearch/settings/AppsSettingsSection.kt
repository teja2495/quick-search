package com.tk.quicksearch.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo

// Constants for consistent spacing
private object Spacing {
    val cardHorizontalPadding = 20.dp
    val cardVerticalPadding = 12.dp
    val cardTopPadding = 20.dp
    val cardBottomPadding = 20.dp
}

/**
 * Reusable toggle row component for settings cards.
 * Provides consistent styling and layout across all toggle rows.
 */
@Composable
private fun SettingsToggleRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false
) {
    val topPadding = if (isFirstItem) Spacing.cardTopPadding else Spacing.cardVerticalPadding
    val bottomPadding = if (isLastItem) Spacing.cardBottomPadding else Spacing.cardVerticalPadding
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.cardHorizontalPadding,
                top = topPadding,
                end = Spacing.cardHorizontalPadding,
                bottom = bottomPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun AppLabelsSection(
    showAppLabels: Boolean,
    onToggleAppLabels: (Boolean) -> Unit,
    keyboardAlignedLayout: Boolean,
    onToggleKeyboardAlignedLayout: (Boolean) -> Unit,
    showSectionTitles: Boolean,
    onToggleShowSectionTitles: (Boolean) -> Unit,
    appsSectionEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Results alignment toggle (always shown)
            SettingsToggleRow(
                text = stringResource(R.string.settings_layout_option_bottom),
                checked = keyboardAlignedLayout,
                onCheckedChange = onToggleKeyboardAlignedLayout,
                isFirstItem = true,
                isLastItem = !appsSectionEnabled
            )

            // App labels toggle (conditional)
            if (appsSectionEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    text = stringResource(R.string.settings_app_labels_toggle),
                    checked = showAppLabels,
                    onCheckedChange = onToggleAppLabels
                )
            }

            // Section titles toggle (always shown)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsToggleRow(
                text = stringResource(R.string.settings_section_titles_toggle),
                checked = showSectionTitles,
                onCheckedChange = onToggleShowSectionTitles,
                isLastItem = true
            )
        }
    }
}

@Composable
fun HiddenAppsSection(
    hiddenApps: List<AppInfo>,
    onUnhideApp: (AppInfo) -> Unit
) {
    Text(
        text = stringResource(R.string.settings_hidden_apps_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.settings_hidden_apps_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        if (hiddenApps.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_hidden_apps_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.cardHorizontalPadding)
            )
        } else {
            Column {
                hiddenApps.forEachIndexed { index, appInfo ->
                    HiddenAppRow(
                        appInfo = appInfo,
                        onUnhideApp = onUnhideApp
                    )
                    if (index != hiddenApps.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenAppRow(
    appInfo: AppInfo,
    onUnhideApp: (AppInfo) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.cardHorizontalPadding,
                vertical = Spacing.cardVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = appInfo.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = { onUnhideApp(appInfo) }) {
            Icon(
                imageVector = Icons.Rounded.Visibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.settings_action_unhide_app),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

