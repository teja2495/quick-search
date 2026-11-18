package com.tk.quicksearch.settings.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.search.appSettings.BooleanSetting
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile

@Composable
internal fun BooleanSettingTile(
    setting: BooleanSetting,
    values: Map<String, Boolean>,
    onCommand: (SettingsCommand) -> Unit,
    icon: ImageVector,
    index: Int,
    count: Int,
) {
    SegmentedToggleTile(
        title = stringResource(setting.titleRes),
        description = setting.descriptionRes?.let { stringResource(it) },
        checked = setting.valueIn(values),
        onCheckedChange = { onCommand(SettingsCommand.SetBoolean(setting, it)) },
        icon = icon,
        index = index,
        count = count,
    )
}
