package com.tk.quicksearch.settings.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.settings.main.SettingsNavigationCard
import com.tk.quicksearch.settings.main.SettingsSpacing

// Constants for consistent spacing
private object Spacing {
    val cardHorizontalPadding = 20.dp
    val cardVerticalPadding = 12.dp
    val cardTopPadding = 20.dp
    val cardBottomPadding = 20.dp
    val toggleSpacing = 12.dp
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
    isLastItem: Boolean = false,
    extraVerticalPadding: Dp = 0.dp,
    extraBottomPadding: Dp = 0.dp
) {
    val topPadding = if (isFirstItem) Spacing.cardTopPadding else Spacing.cardVerticalPadding
    val bottomPadding = if (isLastItem) Spacing.cardBottomPadding else Spacing.cardVerticalPadding
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.cardHorizontalPadding,
                top = topPadding + extraVerticalPadding,
                end = Spacing.cardHorizontalPadding,
                bottom = bottomPadding + extraVerticalPadding + extraBottomPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.toggleSpacing)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = true // Control enabled state via onCheckedChange callback
        )
    }
}

@Composable
fun AppLabelsSection(
    keyboardAlignedLayout: Boolean,
    onToggleKeyboardAlignedLayout: (Boolean) -> Unit,
    showWallpaperBackground: Boolean,
    onToggleShowWallpaperBackground: (Boolean) -> Unit,
    hasFilePermission: Boolean = true,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Wallpaper background toggle (always shown, but disabled if files permission not granted)
            SettingsToggleRow(
                text = stringResource(R.string.settings_wallpaper_background_toggle),
                checked = showWallpaperBackground && hasFilePermission,
                onCheckedChange = { enabled ->
                    // Always call the callback - it will handle permission request if needed
                    onToggleShowWallpaperBackground(enabled)
                },
                isFirstItem = false,
                extraVerticalPadding = 0.dp
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Results alignment toggle (always shown)
            SettingsToggleRow(
                text = stringResource(R.string.settings_layout_option_bottom),
                checked = keyboardAlignedLayout,
                onCheckedChange = onToggleKeyboardAlignedLayout,
                isLastItem = true
            )
        }
    }
}

@Composable
fun IconPackSection(
    selectedLabel: String,
    availableCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsNavigationCard(
        title = stringResource(R.string.settings_icon_pack_title),
        description = if (availableCount > 0) {
            stringResource(R.string.settings_icon_pack_selected_label, selectedLabel)
        } else {
            stringResource(R.string.settings_icon_pack_empty)
        },
        onClick = onClick,
        modifier = modifier,
        contentPadding = SettingsSpacing.singleCardPadding
    )
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
        modifier = Modifier.padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
    )
    Text(
        text = stringResource(R.string.settings_hidden_apps_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = SettingsSpacing.sectionDescriptionBottomPadding)
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

