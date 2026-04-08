package com.tk.quicksearch.widgets.WidgetConfigScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.searchWidget.MicAction
import com.tk.quicksearch.widgets.utils.BorderColorOption
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
        ) { Text(stringResource(R.string.widget_icon_off)) }
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
        ) { Text(stringResource(R.string.settings_app_theme_title)) }
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

/**
 * Segmented button row for border color selection.
 * Matches the Light/Dark/System theme picker style exactly.
 * The Custom button shows the saved custom color as a filled dot when a color is set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorderColorChoiceSegmentedButtonRow(
    selectedOption: BorderColorOption,
    customColor: Color?,
    onWhiteClick: () -> Unit,
    onBlackClick: () -> Unit,
    onCustomClick: () -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selectedOption == BorderColorOption.WHITE,
            onClick = onWhiteClick,
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.widget_text_icon_color_white)) }
        SegmentedButton(
            selected = selectedOption == BorderColorOption.BLACK,
            onClick = onBlackClick,
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            icon = {},
        ) { Text(stringResource(R.string.widget_text_icon_color_black)) }
        SegmentedButton(
            selected = selectedOption == BorderColorOption.CUSTOM,
            onClick = onCustomClick,
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            icon = {},
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_overlay_source_custom))
                if (customColor != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(customColor)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}