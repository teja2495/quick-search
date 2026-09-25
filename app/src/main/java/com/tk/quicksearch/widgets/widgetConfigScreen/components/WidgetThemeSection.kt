package com.tk.quicksearch.widgets.widgetConfigScreen.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.searchScreen.AppThemeColors
import com.tk.quicksearch.widgets.utils.BorderColorOption
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetTheme

@Composable
fun WidgetThemeSection(
    state: WidgetPreferences,
    showDeviceThemeOption: Boolean = false,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val forestPreviewColors =
        AppThemeColors(
            theme = AppTheme.FOREST,
            isDarkMode = isDarkMode,
        )
    val auroraPreviewColors =
        AppThemeColors(
            theme = AppTheme.AURORA,
            isDarkMode = isDarkMode,
        )
    val sunsetPreviewColors =
        AppThemeColors(
            theme = AppTheme.SUNSET,
            isDarkMode = isDarkMode,
        )
    val devicePrimary = MaterialTheme.colorScheme.primary
    val deviceSecondary = MaterialTheme.colorScheme.secondary
    val themeOptions =
        remember(
            showDeviceThemeOption,
            isDarkMode,
            devicePrimary,
            deviceSecondary,
        ) {
            buildList {
                if (showDeviceThemeOption) {
                    add(
                        WidgetBackgroundThemeOption(
                            backgroundColorArgb = devicePrimary.toArgb(),
                            labelRes = R.string.common_theme_device,
                            brush =
                                Brush.linearGradient(
                                    listOf(
                                        devicePrimary,
                                        deviceSecondary,
                                    ),
                                ),
                            isDeviceTheme = true,
                        ),
                    )
                }
                add(
                    WidgetBackgroundThemeOption(
                        backgroundColorArgb = forestPreviewColors.first().toArgb(),
                        labelRes = R.string.settings_app_theme_forest,
                        brush = Brush.linearGradient(forestPreviewColors),
                    ),
                )
                add(
                    WidgetBackgroundThemeOption(
                        backgroundColorArgb = auroraPreviewColors.first().toArgb(),
                        labelRes = R.string.settings_app_theme_aurora,
                        brush = Brush.linearGradient(auroraPreviewColors),
                    ),
                )
                add(
                    WidgetBackgroundThemeOption(
                        backgroundColorArgb = sunsetPreviewColors.first().toArgb(),
                        labelRes = R.string.settings_app_theme_sunset,
                        brush = Brush.linearGradient(sunsetPreviewColors),
                    ),
                )
            }
        }
    var showCustomBgColorDialog by rememberSaveable { mutableStateOf(false) }

    var showCustomBorderColorDialog by rememberSaveable { mutableStateOf(false) }

    // Resolve the custom border color, shown as a dot in the segmented button
    val customBorderColor: Color? =
        if (state.borderColorOption == BorderColorOption.CUSTOM) Color(state.borderColor) else null

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
                onStateChange(
                    state.copy(
                        theme = it,
                        backgroundColor = null,
                        useDeviceThemeBackground = false,
                        borderColorOption =
                            if (state.borderColorOption == BorderColorOption.DEVICE_THEME) {
                                BorderColorOption.BLACK
                            } else {
                                state.borderColorOption
                            },
                    ),
                )
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            themeOptions.forEach { option ->
                ThemeColorOptionChip(
                    modifier = Modifier.weight(1f),
                    brush = option.brush,
                    selected =
                        if (option.isDeviceTheme) {
                            state.useDeviceThemeBackground
                        } else {
                            !state.useDeviceThemeBackground &&
                                state.backgroundColor == option.backgroundColorArgb
                        },
                    label = stringResource(option.labelRes),
                    onClick = {
                        onStateChange(
                            state.copy(
                                backgroundColor = option.backgroundColorArgb,
                                useDeviceThemeBackground = option.isDeviceTheme,
                                borderColorOption =
                                    when {
                                        option.isDeviceTheme && state.borderColorOption != BorderColorOption.CUSTOM ->
                                            BorderColorOption.DEVICE_THEME
                                        !option.isDeviceTheme && state.borderColorOption == BorderColorOption.DEVICE_THEME ->
                                            BorderColorOption.BLACK
                                        else -> state.borderColorOption
                                    },
                            ),
                        )
                    },
                )
            }
            val isCustomSelected =
                !state.useDeviceThemeBackground &&
                state.backgroundColor != null &&
                    themeOptions.none { option -> option.backgroundColorArgb == state.backgroundColor }
            ThemeColorOptionChip(
                modifier = Modifier.weight(1f),
                color =
                    if (isCustomSelected) {
                        state.backgroundColor?.let(::Color) ?: Color.Transparent
                    } else {
                        Color.Transparent
                    },
                selected = isCustomSelected,
                onClick = {
                    showCustomBgColorDialog = true
                },
                label = stringResource(R.string.common_custom),
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

        // Border color section
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.widget_border_color),
            style = MaterialTheme.typography.titleSmall,
        )
        BorderColorChoiceSegmentedButtonRow(
            selectedOption = state.borderColorOption,
            customColor = customBorderColor,
            useDeviceTheme = state.useDeviceThemeBackground,
            onDeviceThemeClick = {
                onStateChange(state.copy(borderColorOption = BorderColorOption.DEVICE_THEME))
            },
            onWhiteClick = {
                onStateChange(state.copy(borderColorOption = BorderColorOption.WHITE))
            },
            onBlackClick = {
                onStateChange(state.copy(borderColorOption = BorderColorOption.BLACK))
            },
            onCustomClick = {
                showCustomBorderColorDialog = true
            },
        )
    }

    if (showCustomBgColorDialog) {
        WidgetColorPickerDialog(
            initialColor = state.backgroundColor?.let(::Color) ?: MaterialTheme.colorScheme.primary,
            onDismiss = { showCustomBgColorDialog = false },
            onConfirm = { color ->
                onStateChange(
                    state.copy(
                        backgroundColor = color.toArgb(),
                        useDeviceThemeBackground = false,
                    ),
                )
                showCustomBgColorDialog = false
            },
        )
    }

    if (showCustomBorderColorDialog) {
        WidgetColorPickerDialog(
            initialColor =
                when (state.borderColorOption) {
                    BorderColorOption.WHITE -> Color.White
                    BorderColorOption.BLACK -> Color.Black
                    BorderColorOption.CUSTOM -> Color(state.borderColor)
                    BorderColorOption.DEVICE_THEME -> MaterialTheme.colorScheme.primary
                },
            onDismiss = { showCustomBorderColorDialog = false },
            onConfirm = { color ->
                onStateChange(
                    state.copy(
                        borderColor = color.toArgb(),
                        borderColorOption = BorderColorOption.CUSTOM,
                    ),
                )
                showCustomBorderColorDialog = false
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

private data class WidgetBackgroundThemeOption(
    val backgroundColorArgb: Int,
    val labelRes: Int,
    val brush: Brush,
    val isDeviceTheme: Boolean = false,
)
