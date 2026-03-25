package com.tk.quicksearch.settings.AppearanceSettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.theme.AppColors

/** Combined card for keyboard alignment and icon pack settings. */
@Composable
fun CombinedLayoutIconCard(
        oneHandedMode: Boolean,
        onToggleOneHandedMode: (Boolean) -> Unit,
        showAppLabels: Boolean,
        onToggleAppLabels: (Boolean) -> Unit,
        bottomSearchBarEnabled: Boolean,
        onToggleBottomSearchBar: (Boolean) -> Unit,
        iconPackTitle: String,
        iconPackDescription: String,
        onIconPackClick: () -> Unit,
        onRefreshIconPacks: () -> Unit = {},
        modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier.fillMaxWidth()) {
        Column {
            SettingsToggleRow(
                    title = stringResource(R.string.settings_layout_option_bottom_title),
                    subtitle = stringResource(R.string.settings_layout_option_bottom_desc),
                    checked = oneHandedMode,
                    onCheckedChange = onToggleOneHandedMode,
                    isFirstItem = true,
                    extraVerticalPadding = 8.dp,
            )

            SettingsToggleRow(
                    title = stringResource(R.string.settings_bottom_searchbar_title),
                    subtitle = stringResource(R.string.settings_bottom_searchbar_desc),
                    checked = bottomSearchBarEnabled,
                    onCheckedChange = onToggleBottomSearchBar,
                    extraVerticalPadding = 8.dp,
            )

            SettingsToggleRow(
                    title = stringResource(R.string.settings_show_app_labels_title),
                    subtitle = stringResource(R.string.settings_show_app_labels_desc),
                    checked = showAppLabels,
                    onCheckedChange = onToggleAppLabels,
                    extraVerticalPadding = 8.dp,
                    showDivider = false,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Icon Pack Section (with navigation)
            val hasIconPacks =
                    iconPackDescription != stringResource(R.string.settings_icon_pack_empty)
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable(onClick = onIconPackClick)
                                    .padding(
                                            start = 24.dp,
                                            top = 16.dp,
                                            end = 24.dp,
                                            bottom = if (hasIconPacks) 16.dp else 20.dp,
                                    ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                            text = iconPackTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                            text = iconPackDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                        imageVector =
                                if (hasIconPacks) {
                                    Icons.Rounded.ChevronRight
                                } else {
                                    Icons.Rounded.Refresh
                                },
                        contentDescription =
                                if (hasIconPacks) {
                                    stringResource(R.string.desc_navigate_forward)
                                } else {
                                    stringResource(R.string.settings_refresh_icon_packs)
                                },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                                Modifier.padding(start = 8.dp)
                                        .then(
                                                if (!hasIconPacks) {
                                                    Modifier.clickable(
                                                            interactionSource =
                                                                    remember {
                                                                        MutableInteractionSource()
                                                                    },
                                                            indication = null,
                                                            onClick = onRefreshIconPacks,
                                                    )
                                                } else {
                                                    Modifier
                                                },
                                        ),
                )
            }
        }
    }
}
