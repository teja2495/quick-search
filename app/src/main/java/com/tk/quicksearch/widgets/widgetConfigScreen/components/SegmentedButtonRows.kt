package com.tk.quicksearch.widgets.widgetConfigScreen.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsChoice
import com.tk.quicksearch.settings.shared.SettingsChoiceGroup
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.searchWidget.MicAction
import com.tk.quicksearch.widgets.utils.BorderColorOption
import com.tk.quicksearch.widgets.utils.SearchIconDisplay
import com.tk.quicksearch.widgets.utils.TextIconColorOverride
import com.tk.quicksearch.widgets.utils.WidgetTheme

/** Light, Dark or System; nothing is selected while a background color overrides the theme. */
@Composable
fun ThemeChoiceSegmentedButtonRow(
    selectedTheme: WidgetTheme?,
    onSelectionChange: (WidgetTheme) -> Unit,
) {
    SettingsChoiceGroup<WidgetTheme?>(
        options =
            listOf(
                SettingsChoice(WidgetTheme.LIGHT, stringResource(R.string.common_theme_light)),
                SettingsChoice(WidgetTheme.DARK, stringResource(R.string.common_theme_dark)),
                SettingsChoice(WidgetTheme.SYSTEM, stringResource(R.string.common_theme_system)),
            ),
        selected = selectedTheme,
        onSelect = { it?.let(onSelectionChange) },
    )
}

@Composable
fun SearchIconChoiceSegmentedButtonRow(
    selectedDisplay: SearchIconDisplay,
    onSelectionChange: (SearchIconDisplay) -> Unit,
) {
    SettingsChoiceGroup(
        options =
            listOf(
                SettingsChoice(SearchIconDisplay.LEFT, stringResource(R.string.widget_icon_left)),
                SettingsChoice(SearchIconDisplay.CENTER, stringResource(R.string.widget_icon_center)),
                SettingsChoice(SearchIconDisplay.OFF, stringResource(R.string.widget_icon_off)),
            ),
        selected = selectedDisplay,
        onSelect = onSelectionChange,
    )
}

@Composable
fun MicActionChoiceSegmentedButtonRow(
    selectedAction: MicAction,
    onSelectionChange: (MicAction) -> Unit,
) {
    SettingsChoiceGroup(
        options =
            listOf(
                SettingsChoice(MicAction.DEFAULT_VOICE_SEARCH, stringResource(R.string.widget_mic_action_default)),
                SettingsChoice(MicAction.DIGITAL_ASSISTANT, stringResource(R.string.widget_mic_action_digital_assistant)),
                SettingsChoice(MicAction.OFF, stringResource(R.string.widget_icon_off)),
            ),
        selected = selectedAction,
        onSelect = onSelectionChange,
    )
}

@Composable
fun TextIconColorChoiceSegmentedButtonRow(
    selectedOverride: TextIconColorOverride,
    onSelectionChange: (TextIconColorOverride) -> Unit,
) {
    SettingsChoiceGroup(
        options =
            listOf(
                SettingsChoice(TextIconColorOverride.THEME, stringResource(R.string.settings_app_theme_title)),
                SettingsChoice(
                    TextIconColorOverride.WHITE,
                    stringResource(R.string.widget_text_icon_color_white),
                    icon = MaterialSymbols.LightMode,
                ),
                SettingsChoice(
                    TextIconColorOverride.BLACK,
                    stringResource(R.string.widget_text_icon_color_black),
                    icon = MaterialSymbols.DarkMode,
                ),
            ),
        selected = selectedOverride,
        onSelect = onSelectionChange,
    )
}

/**
 * Border color choice: White, Black (or Device with the device theme background) and Custom. Custom
 * shows a palette icon until a color is saved, then that color as a swatch.
 */
@Composable
fun BorderColorChoiceSegmentedButtonRow(
    selectedOption: BorderColorOption,
    customColor: Color?,
    useDeviceTheme: Boolean,
    onDeviceThemeClick: () -> Unit,
    onWhiteClick: () -> Unit,
    onBlackClick: () -> Unit,
    onCustomClick: () -> Unit,
) {
    val fixed =
        if (useDeviceTheme) {
            listOf(SettingsChoice(BorderColorOption.DEVICE_THEME, stringResource(R.string.common_theme_device)))
        } else {
            listOf(
                SettingsChoice(
                    BorderColorOption.WHITE,
                    stringResource(R.string.widget_text_icon_color_white),
                    icon = MaterialSymbols.LightMode,
                ),
                SettingsChoice(
                    BorderColorOption.BLACK,
                    stringResource(R.string.widget_text_icon_color_black),
                    icon = MaterialSymbols.DarkMode,
                ),
            )
        }
    SettingsChoiceGroup(
        options =
            fixed +
                SettingsChoice(
                    BorderColorOption.CUSTOM,
                    stringResource(R.string.common_custom),
                    icon = if (customColor == null) MaterialSymbols.Palette else null,
                    swatch = customColor,
                ),
        selected = selectedOption,
        onSelect = { option ->
            when (option) {
                BorderColorOption.DEVICE_THEME -> onDeviceThemeClick()
                BorderColorOption.WHITE -> onWhiteClick()
                BorderColorOption.BLACK -> onBlackClick()
                BorderColorOption.CUSTOM -> onCustomClick()
            }
        },
    )
}
