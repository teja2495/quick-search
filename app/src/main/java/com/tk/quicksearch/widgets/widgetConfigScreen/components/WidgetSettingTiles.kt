package com.tk.quicksearch.widgets.widgetConfigScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsChoice
import com.tk.quicksearch.settings.shared.SettingsFloatingActionHeight
import com.tk.quicksearch.settings.shared.SettingsChoiceGroup
import com.tk.quicksearch.settings.shared.SettingsSegment
import com.tk.quicksearch.settings.shared.SettingsSegmentContent
import com.tk.quicksearch.settings.shared.settingsFloatingActionShape
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * One widget setting as a Material 3 Expressive settings tile at [index] of [count] in a
 * [com.tk.quicksearch.settings.shared.SettingsSegmentGroup]: an optional `titleMedium` heading and
 * description over free-form [content] (choice groups, sliders, pickers).
 */
@Composable
fun WidgetSettingTile(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    index: Int = 0,
    count: Int = 1,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsSegment(index = index, count = count, modifier = modifier) {
        SettingsSegmentContent {
            if (title != null || description != null) {
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXSmall / 2)) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = WidgetTileTextInset),
                        )
                    }
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = WidgetTileTextInset),
                        )
                    }
                }
            }
            content()
        }
    }
}

/** Lines tile text up with toggle rows, as the settings pages do. */
internal val WidgetTileTextInset = 4.dp

/** Key of the Custom option in [WidgetColorChoiceGroup]. */
internal const val WIDGET_CUSTOM_COLOR_CHOICE = "custom"

/**
 * Fixed color choices plus Custom in a connected button group. Custom shows a palette icon until a
 * color is picked, then that color as a swatch; tapping it (again) opens the color picker.
 */
@Composable
internal fun WidgetColorChoiceGroup(
    options: List<SettingsChoice<String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    customColor: Color?,
    pickerInitialColor: Color,
    onCustomColorPicked: (Color) -> Unit,
    pickerTitle: String? = null,
) {
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    val custom =
        SettingsChoice(
            value = WIDGET_CUSTOM_COLOR_CHOICE,
            label = stringResource(R.string.common_custom),
            icon = if (customColor == null) MaterialSymbols.Palette else null,
            swatch = customColor,
        )
    SettingsChoiceGroup(
        options = options + custom,
        // An empty key matches no option, for a state none of the choices describes.
        selected = if (customColor != null) WIDGET_CUSTOM_COLOR_CHOICE else selected.orEmpty(),
        onSelect = { value ->
            if (value == WIDGET_CUSTOM_COLOR_CHOICE) showColorPicker = true else onSelect(value)
        },
    )
    if (showColorPicker) {
        WidgetColorPickerDialog(
            initialColor = pickerInitialColor,
            title = pickerTitle ?: stringResource(R.string.widget_background_color_custom_dialog_title),
            onDismiss = { showColorPicker = false },
            onConfirm = { color ->
                onCustomColorPicked(color)
                showColorPicker = false
            },
        )
    }
}

/**
 * Full-width Save button at the bottom of a widget settings screen: accent, fully round at rest and
 * morphing while pressed, like the floating actions on the settings pages.
 */
@Composable
fun WidgetSaveButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(SettingsFloatingActionHeight),
        shape = settingsFloatingActionShape(interactionSource),
        interactionSource = interactionSource,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = AppColors.Accent,
                contentColor = AppColors.OnAccent,
            ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
