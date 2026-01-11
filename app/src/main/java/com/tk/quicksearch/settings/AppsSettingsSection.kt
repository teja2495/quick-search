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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.settings.components.SettingsCard
import com.tk.quicksearch.settings.components.SettingsSectionTitle
import com.tk.quicksearch.settings.components.SettingsToggleRow
import com.tk.quicksearch.settings.main.SettingsNavigationCard
import com.tk.quicksearch.settings.main.SettingsSpacing
import com.tk.quicksearch.ui.theme.DesignTokens


@Composable
fun AppLabelsSection(
    keyboardAlignedLayout: Boolean,
    onToggleKeyboardAlignedLayout: (Boolean) -> Unit,
    showWallpaperBackground: Boolean,
    onToggleShowWallpaperBackground: (Boolean) -> Unit,
    hasFilePermission: Boolean = true,
    modifier: Modifier = Modifier
) {
    SettingsCard(modifier = modifier) {
        // Wallpaper background toggle (always shown, but disabled if files permission not granted)
        SettingsToggleRow(
            title = stringResource(R.string.settings_wallpaper_background_toggle),
            checked = showWallpaperBackground && hasFilePermission,
            onCheckedChange = { enabled ->
                // Always call the callback - it will handle permission request if needed
                onToggleShowWallpaperBackground(enabled)
            },
            isFirstItem = true
        )

        // Results alignment toggle (always shown)
        SettingsToggleRow(
            title = stringResource(R.string.settings_layout_option_bottom_title),
            subtitle = stringResource(R.string.settings_layout_option_bottom_desc),
            checked = keyboardAlignedLayout,
            onCheckedChange = onToggleKeyboardAlignedLayout,
            isLastItem = true
        )
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
    SettingsSectionTitle(
        title = stringResource(R.string.settings_hidden_apps_title),
        description = stringResource(R.string.settings_hidden_apps_desc)
    )

    SettingsCard {
        if (hiddenApps.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_hidden_apps_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(DesignTokens.CardHorizontalPadding)
            )
        } else {
            Column {
                hiddenApps.forEachIndexed { index, appInfo ->
                    HiddenAppRow(
                        appInfo = appInfo,
                        onUnhideApp = onUnhideApp,
                        isLastItem = index == hiddenApps.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenAppRow(
    appInfo: AppInfo,
    onUnhideApp: (AppInfo) -> Unit,
    isLastItem: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = DesignTokens.CardHorizontalPadding,
                    top = DesignTokens.cardItemTopPadding(false),
                    end = DesignTokens.CardHorizontalPadding,
                    bottom = DesignTokens.cardItemBottomPadding(isLastItem)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing)
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
                Spacer(modifier = Modifier.width(DesignTokens.TextButtonIconSpacing))
                Text(
                    text = stringResource(R.string.settings_action_unhide_app),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!isLastItem) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

