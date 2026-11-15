package com.tk.quicksearch.widgets.widgetConfigScreen.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsSegmentToggleRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.utils.WidgetPreferences

/** Show Label switch tile at [index] of [count]; custom buttons leave no room for the label. */
@Composable
fun WidgetToggleSection(
    state: WidgetPreferences,
    hasCustomButtons: Boolean,
    index: Int = 0,
    count: Int = 1,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val context = LocalContext.current
    SettingsSegmentToggleRow(
        title = stringResource(R.string.widget_toggle_show_label),
        icon = MaterialSymbols.Label,
        checked = state.showLabel,
        onCheckedChange = { enabled ->
            if (enabled && hasCustomButtons) {
                Toast
                    .makeText(
                        context,
                        context.getString(R.string.widget_custom_buttons_restriction),
                        Toast.LENGTH_SHORT,
                    ).show()
            } else {
                onStateChange(state.copy(showLabel = enabled))
            }
        },
        index = index,
        count = count,
    )
}
