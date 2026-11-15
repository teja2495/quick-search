package com.tk.quicksearch.widgets.widgetConfigScreen.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.utils.WidgetPreferences

/** Text and icon color; a tile at [index] of [count] in the caller's group. */
@Composable
fun WidgetTextIconColorSection(
    state: WidgetPreferences,
    index: Int = 0,
    count: Int = 1,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    WidgetSettingTile(title = stringResource(R.string.widget_text_icon_color), index = index, count = count) {
        TextIconColorChoiceSegmentedButtonRow(
            selectedOverride = state.textIconColorOverride,
            onSelectionChange = {
                onStateChange(state.copy(textIconColorOverride = it))
            },
        )
    }
}
