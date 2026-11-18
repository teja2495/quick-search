package com.tk.quicksearch.widgets.widgetConfigScreen.components

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.utils.BorderColorOption
import com.tk.quicksearch.widgets.utils.WidgetPreferences

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

    SegmentedGroup {
        WidgetSettingTile(title = stringResource(R.string.settings_app_theme_title), index = 0, count = 2) {
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
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
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
                            Color(state.backgroundColor)
                        } else {
                            null
                        },
                    selected = isCustomSelected,
                    onClick = {
                        showCustomBgColorDialog = true
                    },
                    label = stringResource(R.string.common_custom),
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.PaletteOutlined,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(DesignTokens.IconSizeSmall),
                        )
                    },
                )
            }
        }
        WidgetSettingTile(title = stringResource(R.string.widget_border_color), index = 1, count = 2) {
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
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(AppShapes.large)
                    .then(
                        if (brush != null) {
                            Modifier.background(brush = brush)
                        } else {
                            Modifier.background(color ?: AppColors.AccentTintTonal)
                        },
                    )
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) AppColors.Accent else MaterialTheme.colorScheme.outlineVariant,
                        shape = AppShapes.large,
                    ).selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                    .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier =
                        Modifier
                            .size(DesignTokens.IconSize)
                            .clip(AppShapes.full)
                            .background(AppColors.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Check,
                        contentDescription = null,
                        tint = AppColors.OnAccent,
                        modifier = Modifier.size(16.dp),
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
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

private data class WidgetBackgroundThemeOption(
    val backgroundColorArgb: Int,
    val labelRes: Int,
    val brush: Brush,
    val isDeviceTheme: Boolean = false,
)
