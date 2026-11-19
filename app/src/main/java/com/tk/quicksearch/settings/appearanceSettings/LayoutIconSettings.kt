package com.tk.quicksearch.settings.appearanceSettings

import androidx.compose.material3.Icon
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsToggleSliderDetails
import com.tk.quicksearch.settings.shared.SettingsToggleRow
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

    val showColumns = !isTablet()
    val count = if (showColumns) 5 else 4
    SegmentedGroup(modifier = modifier) {
        var index = 0
        SegmentedTile(index = index++, count = count) {
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
        }

        if (showColumns) {
            SegmentedTile(index = index++, count = count) {
                AppColumnsSelector(
                        selectedColumns = phoneAppGridColumns,
                        onSelectColumns = onSetPhoneAppGridColumns,
                )
            }
        }

        SegmentedTile(index = index++, count = count) {
                SettingsToggleRow(
                        title = stringResource(R.string.settings_show_app_labels_title),
                        subtitle = stringResource(R.string.settings_show_app_labels_desc),
                        checked = showAppLabels,
                        onCheckedChange = onToggleAppLabels,
                        isFirstItem = false,
                        showDivider = false,
                )
        }

        SegmentedTile(index = index++, count = count) {
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
        }

        SegmentedTile(index = index, count = count, onClick = onIconPackClick) {
            SegmentedTileRow(
                title = iconPackTitle,
                description = iconPackDescription,
                trailing = {
                    Icon(
                        imageVector = MaterialSymbols.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@Composable
private fun AppColumnsSelector(
        selectedColumns: Int,
        onSelectColumns: (Int) -> Unit,
) {
    // Same layout as the App Result Rows setting: title above full-width, equal choices.
    SegmentedTileContent {
        Text(
                text = stringResource(R.string.settings_app_columns_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
        )
        ConnectedChoiceGroup(
                options =
                        listOf(
                                ChoiceOption(4, stringResource(R.string.settings_app_columns_4)),
                                ChoiceOption(5, stringResource(R.string.settings_app_columns_5)),
                        ),
                selected = if (selectedColumns == 4) 4 else 5,
                onSelect = onSelectColumns,
        )
    }
}
