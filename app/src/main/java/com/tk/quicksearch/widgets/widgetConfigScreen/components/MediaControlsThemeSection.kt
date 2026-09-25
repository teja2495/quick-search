package com.tk.quicksearch.widgets.widgetConfigScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.utils.TextIconColorOverride
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetTheme

/**
 * Background choice for the Media Controls widget: white, black, or a custom color from the
 * picker. A custom color is stored in [WidgetPreferences.backgroundColor]; null means Light/Dark.
 * Light and Dark also switch the text color to the one that reads on them.
 */
@Composable
fun MediaControlsThemeSection(
    state: WidgetPreferences,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val customColor = state.backgroundColor?.let(::Color)
    ColorChoiceSection(
        title = stringResource(R.string.settings_app_theme_title),
        firstLabel = stringResource(R.string.common_theme_light),
        firstSelected = customColor == null && state.theme == WidgetTheme.LIGHT,
        onFirstClick = {
            onStateChange(
                state.copy(
                    theme = WidgetTheme.LIGHT,
                    backgroundColor = null,
                    textIconColorOverride = TextIconColorOverride.BLACK,
                    customTextIconColor = null,
                ),
            )
        },
        secondLabel = stringResource(R.string.common_theme_dark),
        secondSelected = customColor == null && state.theme != WidgetTheme.LIGHT,
        onSecondClick = {
            onStateChange(
                state.copy(
                    theme = WidgetTheme.DARK,
                    backgroundColor = null,
                    textIconColorOverride = TextIconColorOverride.WHITE,
                    customTextIconColor = null,
                ),
            )
        },
        customColor = customColor,
        pickerInitialColor = customColor ?: if (state.theme == WidgetTheme.LIGHT) Color.White else Color.Black,
        onCustomColorPicked = { onStateChange(state.copy(backgroundColor = it.toArgb())) },
    )
}

/** Text and icon color for the Media Controls widget: white, black, or a custom color. */
@Composable
fun MediaControlsTextColorSection(
    state: WidgetPreferences,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val customColor = state.customTextIconColor?.let(::Color)
    ColorChoiceSection(
        title = stringResource(R.string.widget_text_icon_color),
        firstLabel = stringResource(R.string.widget_text_icon_color_white),
        firstSelected = customColor == null && state.textIconColorOverride != TextIconColorOverride.BLACK,
        onFirstClick = {
            onStateChange(state.copy(textIconColorOverride = TextIconColorOverride.WHITE, customTextIconColor = null))
        },
        secondLabel = stringResource(R.string.widget_text_icon_color_black),
        secondSelected = customColor == null && state.textIconColorOverride == TextIconColorOverride.BLACK,
        onSecondClick = {
            onStateChange(state.copy(textIconColorOverride = TextIconColorOverride.BLACK, customTextIconColor = null))
        },
        customColor = customColor,
        pickerInitialColor =
            customColor
                ?: if (state.textIconColorOverride == TextIconColorOverride.BLACK) Color.Black else Color.White,
        onCustomColorPicked = { onStateChange(state.copy(customTextIconColor = it.toArgb())) },
    )
}

/** Two fixed choices plus Custom, which opens the color picker and shows the picked color as a dot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorChoiceSection(
    title: String,
    firstLabel: String,
    firstSelected: Boolean,
    onFirstClick: () -> Unit,
    secondLabel: String,
    secondSelected: Boolean,
    onSecondClick: () -> Unit,
    customColor: Color?,
    pickerInitialColor: Color,
    onCustomColorPicked: (Color) -> Unit,
) {
    var showColorPicker by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = firstSelected,
                onClick = onFirstClick,
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                icon = {},
            ) { Text(firstLabel) }
            SegmentedButton(
                selected = secondSelected,
                onClick = onSecondClick,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                icon = {},
            ) { Text(secondLabel) }
            SegmentedButton(
                selected = customColor != null,
                onClick = { showColorPicker = true },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                icon = {},
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.common_custom))
                    if (customColor != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier =
                                Modifier
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

    if (showColorPicker) {
        WidgetColorPickerDialog(
            initialColor = pickerInitialColor,
            onDismiss = { showColorPicker = false },
            onConfirm = { color ->
                onCustomColorPicked(color)
                showColorPicker = false
            },
        )
    }
}
