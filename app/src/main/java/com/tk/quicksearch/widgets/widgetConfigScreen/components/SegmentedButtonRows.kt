package com.tk.quicksearch.widgets.WidgetConfigScreen.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.searchWidget.MicAction
import com.tk.quicksearch.widgets.utils.SearchIconDisplay
import com.tk.quicksearch.widgets.utils.TextIconColorOverride
import com.tk.quicksearch.widgets.utils.WidgetTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeChoiceSegmentedButtonRow(
    selectedTheme: WidgetTheme?,
    onSelectionChange: (WidgetTheme) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selectedTheme === WidgetTheme.LIGHT,
            onClick = { onSelectionChange(WidgetTheme.LIGHT) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.common_theme_light)) }
        SegmentedButton(
            selected = selectedTheme === WidgetTheme.DARK,
            onClick = { onSelectionChange(WidgetTheme.DARK) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.common_theme_dark)) }
        SegmentedButton(
            selected = selectedTheme === WidgetTheme.SYSTEM,
            onClick = { onSelectionChange(WidgetTheme.SYSTEM) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.common_theme_system)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchIconChoiceSegmentedButtonRow(
    selectedDisplay: SearchIconDisplay,
    onSelectionChange: (SearchIconDisplay) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selectedDisplay == SearchIconDisplay.LEFT,
            onClick = { onSelectionChange(SearchIconDisplay.LEFT) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.widget_icon_left)) }
        SegmentedButton(
            selected = selectedDisplay == SearchIconDisplay.CENTER,
            onClick = { onSelectionChange(SearchIconDisplay.CENTER) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.widget_icon_center)) }
        SegmentedButton(
            selected = selectedDisplay == SearchIconDisplay.OFF,
            onClick = { onSelectionChange(SearchIconDisplay.OFF) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.widget_icon_off)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicActionChoiceSegmentedButtonRow(
    selectedAction: MicAction,
    onSelectionChange: (MicAction) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selectedAction == MicAction.DEFAULT_VOICE_SEARCH,
            onClick = { onSelectionChange(MicAction.DEFAULT_VOICE_SEARCH) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.widget_mic_action_default)) }
        SegmentedButton(
            selected = selectedAction == MicAction.DIGITAL_ASSISTANT,
            onClick = { onSelectionChange(MicAction.DIGITAL_ASSISTANT) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.widget_mic_action_digital_assistant)) }
        SegmentedButton(
            selected = selectedAction == MicAction.OFF,
            onClick = { onSelectionChange(MicAction.OFF) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.widget_mic_action_off)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextIconColorChoiceSegmentedButtonRow(
    selectedOverride: TextIconColorOverride,
    onSelectionChange: (TextIconColorOverride) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selectedOverride == TextIconColorOverride.THEME,
            onClick = { onSelectionChange(TextIconColorOverride.THEME) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.widget_text_icon_color_theme)) }
        SegmentedButton(
            selected = selectedOverride == TextIconColorOverride.WHITE,
            onClick = { onSelectionChange(TextIconColorOverride.WHITE) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.widget_text_icon_color_white)) }
        SegmentedButton(
            selected = selectedOverride == TextIconColorOverride.BLACK,
            onClick = { onSelectionChange(TextIconColorOverride.BLACK) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.widget_text_icon_color_black)) }
    }
}