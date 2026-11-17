package com.tk.quicksearch.widgets.widgetConfigScreen.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.utils.TextIconColorOverride
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetTheme

/**
 * Background choice for the Media Controls widget: Light, Dark (sun and moon icons), or a custom color from the
 * picker. A custom color is stored in [WidgetPreferences.backgroundColor]; null means Light/Dark.
 * Light and Dark also switch the text color to the one that reads on them.
 */
@Composable
fun MediaControlsThemeSection(
    state: WidgetPreferences,
    index: Int = 0,
    count: Int = 1,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val customColor = state.backgroundColor?.let(::Color)
    ColorChoiceSection(
        title = stringResource(R.string.settings_app_theme_title),
        index = index,
        count = count,
        firstLabel = stringResource(R.string.common_theme_light),
        firstIcon = MaterialSymbols.LightMode,
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
        secondIcon = MaterialSymbols.DarkMode,
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
    index: Int = 0,
    count: Int = 1,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val customColor = state.customTextIconColor?.let(::Color)
    ColorChoiceSection(
        title = stringResource(R.string.widget_text_icon_color),
        index = index,
        count = count,
        firstLabel = stringResource(R.string.widget_text_icon_color_white),
        firstIcon = MaterialSymbols.LightMode,
        firstSelected = customColor == null && state.textIconColorOverride != TextIconColorOverride.BLACK,
        onFirstClick = {
            onStateChange(state.copy(textIconColorOverride = TextIconColorOverride.WHITE, customTextIconColor = null))
        },
        secondLabel = stringResource(R.string.widget_text_icon_color_black),
        secondIcon = MaterialSymbols.DarkMode,
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

/** Two fixed choices plus Custom, which opens the color picker and shows the picked color. */
@Composable
private fun ColorChoiceSection(
    title: String,
    index: Int,
    count: Int,
    firstLabel: String,
    firstIcon: ImageVector? = null,
    firstSelected: Boolean,
    onFirstClick: () -> Unit,
    secondLabel: String,
    secondIcon: ImageVector? = null,
    secondSelected: Boolean,
    onSecondClick: () -> Unit,
    customColor: Color?,
    pickerInitialColor: Color,
    onCustomColorPicked: (Color) -> Unit,
) {
    WidgetSettingTile(title = title, index = index, count = count) {
        WidgetColorChoiceGroup(
            options =
                listOf(
                    ChoiceOption(FIRST_CHOICE, firstLabel, icon = firstIcon),
                    ChoiceOption(SECOND_CHOICE, secondLabel, icon = secondIcon),
                ),
            selected =
                when {
                    firstSelected -> FIRST_CHOICE
                    secondSelected -> SECOND_CHOICE
                    else -> null
                },
            onSelect = { if (it == FIRST_CHOICE) onFirstClick() else onSecondClick() },
            customColor = customColor,
            pickerInitialColor = pickerInitialColor,
            onCustomColorPicked = onCustomColorPicked,
        )
    }
}

private const val FIRST_CHOICE = "first"
private const val SECOND_CHOICE = "second"
