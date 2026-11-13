package com.tk.quicksearch.widgets.noteWidget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Icon
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
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.widgets.utils.TextIconColorOverride
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetColorPickerDialog

/**
 * Text color: white, black or a custom color. Until one is picked the text is automatic, and the
 * option matching the color it currently resolves to is shown as selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetTextColorSection(
    style: WidgetPreferences,
    onStyleChange: (WidgetPreferences) -> Unit,
) {
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    val customColor = style.customTextIconColor?.let(::Color)
    val isWhite =
        customColor == null &&
            NoteWidgetColors.forWidget(style, isSystemDark = isSystemInDarkTheme()).title == AppColors.WidgetTextLight
    val selectOverride = { override: TextIconColorOverride ->
        onStyleChange(style.copy(textIconColorOverride = override, customTextIconColor = null))
    }

    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)) {
        Text(
            text = stringResource(R.string.widget_text_color),
            style = MaterialTheme.typography.titleSmall,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = isWhite,
                onClick = { selectOverride(TextIconColorOverride.WHITE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                icon = {},
            ) { Text(stringResource(R.string.widget_text_icon_color_white), maxLines = 1) }
            SegmentedButton(
                selected = customColor == null && !isWhite,
                onClick = { selectOverride(TextIconColorOverride.BLACK) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                icon = {},
            ) { Text(stringResource(R.string.widget_text_icon_color_black), maxLines = 1) }
            SegmentedButton(
                selected = customColor != null,
                onClick = { showColorPicker = true },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                icon = {},
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = MaterialSymbols.Palette,
                        contentDescription = stringResource(R.string.common_custom),
                        modifier = Modifier.size(18.dp),
                    )
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
            initialColor = customColor ?: MaterialTheme.colorScheme.primary,
            title = stringResource(R.string.widget_text_color),
            onDismiss = { showColorPicker = false },
            onConfirm = { color ->
                onStyleChange(style.copy(customTextIconColor = color.toArgb()))
                showColorPicker = false
            },
        )
    }
}
