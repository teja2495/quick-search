package com.tk.quicksearch.settings.AppearanceSettings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.isTablet

/** Card for app grid columns, app labels, icon pack, and icon appearance settings. */
@Composable
fun AppIconCard(
        showAppLabels: Boolean,
        onToggleAppLabels: (Boolean) -> Unit,
        phoneAppGridColumns: Int = UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
        onSetPhoneAppGridColumns: (Int) -> Unit = {},
        iconPackTitle: String,
        iconPackDescription: String,
        onIconPackClick: () -> Unit,
        onRefreshIconPacks: () -> Unit = {},
        appIconShape: AppIconShape,
        onSetAppIconShape: (AppIconShape) -> Unit,
        modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier.fillMaxWidth()) {
        Column {
            if (!isTablet()) {
                AppColumnsSelector(
                        selectedColumns = phoneAppGridColumns,
                        onSelectColumns = onSetPhoneAppGridColumns,
                )
                HorizontalDivider(color = AppColors.SettingsDivider)
            }

            SettingsToggleRow(
                    title = stringResource(R.string.settings_show_app_labels_title),
                    subtitle = stringResource(R.string.settings_show_app_labels_desc),
                    checked = showAppLabels,
                    onCheckedChange = onToggleAppLabels,
                    extraVerticalPadding = 8.dp,
                    isFirstItem = isTablet(),
                    showDivider = false,
            )

            HorizontalDivider(color = AppColors.SettingsDivider)

            SettingsToggleRow(
                    title = stringResource(R.string.settings_circular_app_icons_title),
                    subtitle = stringResource(R.string.settings_circular_app_icons_desc),
                    checked = appIconShape == AppIconShape.CIRCLE,
                    onCheckedChange = { enabled ->
                        onSetAppIconShape(if (enabled) AppIconShape.CIRCLE else AppIconShape.DEFAULT)
                    },
                    isFirstItem = false,
                    isLastItem = false,
            )

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

@Composable
private fun AppColumnsSelector(
        selectedColumns: Int,
        onSelectColumns: (Int) -> Unit,
) {
    Row(
            modifier =
                    Modifier
                            .fillMaxWidth()
                            .padding(
                                    start = DesignTokens.SpacingXXLarge,
                                    top = DesignTokens.SpacingXLarge,
                                    end = DesignTokens.SpacingXXLarge,
                                    bottom = DesignTokens.SpacingMedium,
                            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = stringResource(R.string.settings_app_columns_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
            val fourSelected = selectedColumns == 4
            AssistChip(
                    onClick = { onSelectColumns(4) },
                    label = { Text(stringResource(R.string.settings_app_columns_4)) },
                    shape = DesignTokens.ShapeFull,
                    border = if (fourSelected) null else BorderStroke(1.dp, AppColors.SettingsDivider),
                    colors =
                            AssistChipDefaults.assistChipColors(
                                    containerColor =
                                            if (fourSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                    labelColor =
                                            if (fourSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.primary,
                            ),
            )
            val fiveSelected = selectedColumns != 4
            AssistChip(
                    onClick = { onSelectColumns(5) },
                    label = { Text(stringResource(R.string.settings_app_columns_5)) },
                    shape = DesignTokens.ShapeFull,
                    border = if (fiveSelected) null else BorderStroke(1.dp, AppColors.SettingsDivider),
                    colors =
                            AssistChipDefaults.assistChipColors(
                                    containerColor =
                                            if (fiveSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                    labelColor =
                                            if (fiveSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.primary,
                            ),
            )
        }
    }
}
