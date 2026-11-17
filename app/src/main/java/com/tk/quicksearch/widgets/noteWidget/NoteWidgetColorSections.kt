package com.tk.quicksearch.widgets.noteWidget

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.widgets.utils.TextIconColorOverride
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetTheme
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetColorChoiceGroup
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetSettingTile

/**
 * Background choice for the Note widget: follow the system theme, white, black, or a custom color
 * from the picker. A custom color is stored in [WidgetPreferences.backgroundColor]; null means one
 * of the themes. Picking a theme makes the text automatic again so it reads on the new background.
 * A tile at [index] of [count] in the caller's group.
 */
@Composable
fun NoteWidgetBackgroundSection(
    state: WidgetPreferences,
    index: Int = 0,
    count: Int = 1,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val customColor = state.backgroundColor?.let(::Color)
    WidgetSettingTile(title = stringResource(R.string.settings_app_theme_title), index = index, count = count) {
        WidgetColorChoiceGroup(
            options =
                listOf(
                    ChoiceOption(WidgetTheme.SYSTEM.name, stringResource(R.string.common_theme_system)),
                    ChoiceOption(WidgetTheme.LIGHT.name, stringResource(R.string.common_theme_light)),
                    ChoiceOption(WidgetTheme.DARK.name, stringResource(R.string.common_theme_dark)),
                ),
            selected = state.theme.name,
            onSelect = { name ->
                onStateChange(
                    state.copy(
                        theme = WidgetTheme.valueOf(name),
                        backgroundColor = null,
                        textIconColorOverride = TextIconColorOverride.THEME,
                    ),
                )
            },
            customColor = customColor,
            pickerInitialColor = customColor ?: MaterialTheme.colorScheme.primary,
            onCustomColorPicked = { onStateChange(state.copy(backgroundColor = it.toArgb())) },
        )
    }
}

/**
 * Text color for the Note widget: white or black. Until one is picked the text is automatic, and
 * the option matching the color it currently resolves to is shown as selected.
 */
@Composable
fun NoteWidgetTextColorSection(
    state: WidgetPreferences,
    index: Int = 0,
    count: Int = 1,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val isWhite =
        NoteWidgetColors.forWidget(state, isSystemDark = isSystemInDarkTheme()).title == AppColors.WidgetTextLight

    WidgetSettingTile(title = stringResource(R.string.widget_text_color), index = index, count = count) {
        ConnectedChoiceGroup(
            options =
                listOf(
                    ChoiceOption(
                        TextIconColorOverride.WHITE,
                        stringResource(R.string.widget_text_icon_color_white),
                        icon = MaterialSymbols.LightMode,
                    ),
                    ChoiceOption(
                        TextIconColorOverride.BLACK,
                        stringResource(R.string.widget_text_icon_color_black),
                        icon = MaterialSymbols.DarkMode,
                    ),
                ),
            selected = if (isWhite) TextIconColorOverride.WHITE else TextIconColorOverride.BLACK,
            onSelect = { onStateChange(state.copy(textIconColorOverride = it)) },
        )
    }
}
