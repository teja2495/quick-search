package com.tk.quicksearch.widgets.WidgetConfigScreen.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetPreferences

@Composable
fun WidgetToggleSection(
    state: WidgetPreferences,
    hasCustomButtons: Boolean,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.SECTION_SPACING)) {
        ToggleRow(
            label = stringResource(R.string.widget_toggle_show_label),
            checked = state.showLabel,
            onCheckedChange = { enabled ->
                if (enabled && hasCustomButtons) {
                    Toast
                        .makeText(
                            context,
                            context.getString(
                                R.string
                                    .widget_custom_buttons_restriction,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                    return@ToggleRow
                }
                onStateChange(state.copy(showLabel = enabled))
            },
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(uncheckedTrackColor = Color.Transparent),
        )
    }
}