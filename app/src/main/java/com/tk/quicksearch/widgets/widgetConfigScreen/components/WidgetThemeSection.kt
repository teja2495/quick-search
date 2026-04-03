package com.tk.quicksearch.widgets.WidgetConfigScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.searchScreen.AppThemeColors
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.ThemeChoiceSegmentedButtonRow
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetTheme

@Composable
fun WidgetThemeSection(
    state: WidgetPreferences,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val themeOptions =
        remember {
            listOf(
                WidgetBackgroundThemeOption(
                    appTheme = AppTheme.FOREST,
                    labelRes = R.string.settings_app_theme_forest,
                ),
                WidgetBackgroundThemeOption(
                    appTheme = AppTheme.AURORA,
                    labelRes = R.string.settings_app_theme_aurora,
                ),
                WidgetBackgroundThemeOption(
                    appTheme = AppTheme.SUNSET,
                    labelRes = R.string.settings_app_theme_sunset,
                ),
            )
        }
    var customHexValue by rememberSaveable { mutableStateOf("") }
    var showCustomColorDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING),
    ) {
        Text(
            text = stringResource(R.string.settings_app_theme_title),
            style = MaterialTheme.typography.titleSmall,
        )
        ThemeChoiceSegmentedButtonRow(
            selectedTheme = if (state.backgroundColor == null) state.theme else null,
            onSelectionChange = {
                customHexValue = ""
                onStateChange(state.copy(theme = it, backgroundColor = null))
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            themeOptions.forEach { option ->
                val previewColors =
                    AppThemeColors(
                        theme = option.appTheme,
                        isDarkMode = isDarkMode,
                    )
                val selectedArgb = previewColors.first().toArgb()
                ThemeColorOptionChip(
                    modifier = Modifier.weight(1f),
                    brush = Brush.linearGradient(previewColors),
                    selected = state.backgroundColor == selectedArgb,
                    label = stringResource(option.labelRes),
                    onClick = {
                        customHexValue = ""
                        onStateChange(state.copy(backgroundColor = selectedArgb))
                    },
                )
            }
            val isCustomSelected =
                state.backgroundColor != null &&
                    themeOptions.none { option ->
                        AppThemeColors(
                            theme = option.appTheme,
                            isDarkMode = isDarkMode,
                        ).first().toArgb() == state.backgroundColor
                    }
            ThemeColorOptionChip(
                modifier = Modifier.weight(1f),
                color =
                    if (isCustomSelected) {
                        state.backgroundColor?.let(::Color) ?: Color.Transparent
                    } else {
                        Color.Transparent
                    },
                selected = isCustomSelected,
                onClick = { showCustomColorDialog = true },
                label = stringResource(R.string.settings_overlay_source_custom),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
    }

    if (showCustomColorDialog) {
        CustomBackgroundColorDialog(
            initialHex = customHexValue,
            onDismiss = { showCustomColorDialog = false },
            onConfirm = { hex, color ->
                customHexValue = hex
                onStateChange(state.copy(backgroundColor = color.toArgb()))
                showCustomColorDialog = false
            },
        )
    }
}

@Composable
private fun ThemeColorOptionChip(
    modifier: Modifier = Modifier,
    color: Color? = null,
    brush: Brush? = null,
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .then(
                        if (brush != null) {
                            Modifier.background(brush = brush)
                        } else {
                            Modifier.background(color ?: Color.Transparent)
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium,
                    ).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier =
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else {
                icon?.invoke()
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CustomBackgroundColorDialog(
    initialHex: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Color) -> Unit,
) {
    var hexValue by rememberSaveable { mutableStateOf(initialHex) }
    var hasTyped by rememberSaveable { mutableStateOf(initialHex.isNotEmpty()) }
    val isValidHex = hexValue.matches(Regex("^[0-9A-Fa-f]{6}$"))

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.widget_background_color_custom_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = hexValue,
                    onValueChange = { updated ->
                        hexValue = updated.take(6).uppercase(java.util.Locale.US).filter { it.isDigit() || it in 'A'..'F' }
                        hasTyped = true
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.widget_background_color_custom_dialog_hint)) },
                    leadingIcon = { Text("#") },
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                        ),
                    colors = dialogTextFieldColors(),
                )
                if (hasTyped && !isValidHex) {
                    Text(
                        text = stringResource(R.string.widget_background_color_custom_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValidHex) {
                        onConfirm(hexValue, Color(android.graphics.Color.parseColor("#$hexValue")))
                    }
                },
                enabled = isValidHex,
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

private data class WidgetBackgroundThemeOption(
    val appTheme: AppTheme,
    val labelRes: Int,
)
