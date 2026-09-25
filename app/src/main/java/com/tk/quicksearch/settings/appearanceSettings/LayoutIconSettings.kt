package com.tk.quicksearch.settings.appearanceSettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleSliderDetails
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.util.isTablet

/** Card for app grid columns, app labels, icon pack, and icon appearance settings. */
@Composable
fun AppIconCard(
        showAppLabels: Boolean,
        onToggleAppLabels: (Boolean) -> Unit,
        phoneAppGridColumns: Int = UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
        onSetPhoneAppGridColumns: (Int) -> Unit = {},
        appIconSizeStep: Int = UiPreferences.DEFAULT_APP_ICON_SIZE_STEP,
        onSetAppIconSizeStep: (Int) -> Unit = {},
        iconPackTitle: String,
        iconPackDescription: String,
        onIconPackClick: () -> Unit,
        appIconShape: AppIconShape,
        onSetAppIconShape: (AppIconShape) -> Unit,
        modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var lastIconSizeStep by remember { mutableStateOf(appIconSizeStep) }
    LaunchedEffect(appIconSizeStep) {
        lastIconSizeStep = appIconSizeStep
    }

    SettingsCard(modifier = modifier.fillMaxWidth()) {
        Column {
            SettingsToggleRow(
                    title = stringResource(R.string.settings_app_icon_size_title),
                    checked = true,
                    onCheckedChange = {},
                    sliderDetails =
                            SettingsToggleSliderDetails(
                                    value = appIconSizeStep.toFloat(),
                                    onValueChange = { value ->
                                        val step = value.toInt()
                                        if (step != lastIconSizeStep) {
                                            hapticToggle(view)()
                                            lastIconSizeStep = step
                                        }
                                        onSetAppIconSizeStep(step)
                                    },
                                    valueRange =
                                            UiPreferences.MIN_APP_ICON_SIZE_STEP.toFloat()..
                                                    UiPreferences.MAX_APP_ICON_SIZE_STEP.toFloat(),
                                    steps =
                                            UiPreferences.MAX_APP_ICON_SIZE_STEP -
                                                    UiPreferences.MIN_APP_ICON_SIZE_STEP - 1,
                                    valueLabel =
                                            stringResource(
                                                    R.string.settings_app_icon_size_percent,
                                                    UiPreferences.appIconSizePercent(appIconSizeStep),
                                            ),
                                    valueLabelWidth = 44.dp,
                            ),
                    isFirstItem = true,
                    showSwitch = false,
            )

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
                    isFirstItem = false,
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

            // Icon Pack Section
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable(onClick = onIconPackClick)
                                    .padding(
                                            start = 24.dp,
                                            top = 16.dp,
                                            end = 24.dp,
                                            bottom = 16.dp,
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
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.desc_navigate_forward),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
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
