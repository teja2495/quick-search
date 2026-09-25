package com.tk.quicksearch.widgets.widgetConfigScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.utils.TextIconColorOverride
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetPreferences

@Composable
fun WidgetTextIconColorSection(
    state: WidgetPreferences,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING),
    ) {
        Text(
            text = stringResource(R.string.widget_text_icon_color),
            style = MaterialTheme.typography.titleSmall,
        )
        TextIconColorChoiceSegmentedButtonRow(
            selectedOverride = state.textIconColorOverride,
            onSelectionChange = {
                onStateChange(state.copy(textIconColorOverride = it))
            },
        )
    }
}