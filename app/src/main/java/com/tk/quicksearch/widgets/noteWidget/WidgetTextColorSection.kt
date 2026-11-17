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
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.widgets.utils.TextIconColorOverride
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetColorChoiceGroup
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetSettingTile

/**
 * Text color: white, black or a custom color. Until one is picked the text is automatic, and the
 * option matching the color it currently resolves to is shown as selected. A tile at [index] of
 * [count] in the caller's group.
 */
@Composable
internal fun WidgetTextColorSection(
    style: WidgetPreferences,
    index: Int = 0,
    count: Int = 1,
    onStyleChange: (WidgetPreferences) -> Unit,
) {
    val customColor = style.customTextIconColor?.let(::Color)
    val isWhite =
        NoteWidgetColors.forWidget(style, isSystemDark = isSystemInDarkTheme()).title == AppColors.WidgetTextLight
    val title = stringResource(R.string.widget_text_color)

    WidgetSettingTile(title = title, index = index, count = count) {
        WidgetColorChoiceGroup(
            options =
                listOf(
                    ChoiceOption(
                        TextIconColorOverride.WHITE.name,
                        stringResource(R.string.widget_text_icon_color_white),
                        icon = MaterialSymbols.LightMode,
                    ),
                    ChoiceOption(
                        TextIconColorOverride.BLACK.name,
                        stringResource(R.string.widget_text_icon_color_black),
                        icon = MaterialSymbols.DarkMode,
                    ),
                ),
            selected = if (isWhite) TextIconColorOverride.WHITE.name else TextIconColorOverride.BLACK.name,
            onSelect = { name ->
                onStyleChange(
                    style.copy(textIconColorOverride = TextIconColorOverride.valueOf(name), customTextIconColor = null),
                )
            },
            customColor = customColor,
            pickerInitialColor = customColor ?: MaterialTheme.colorScheme.primary,
            onCustomColorPicked = { onStyleChange(style.copy(customTextIconColor = it.toArgb())) },
            pickerTitle = title,
        )
    }
}
