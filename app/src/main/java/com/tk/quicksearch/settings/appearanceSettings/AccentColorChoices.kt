package com.tk.quicksearch.settings.appearanceSettings

import com.tk.quicksearch.shared.ui.components.AppMessages
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetColorPickerDialog

internal fun hasWallpaperColorSource(
    backgroundSource: BackgroundSource,
    hasWallpaperPermission: Boolean,
    wallpaperAvailable: Boolean,
): Boolean =
    backgroundSource == BackgroundSource.CUSTOM_IMAGE ||
        (backgroundSource == BackgroundSource.SYSTEM_WALLPAPER && hasWallpaperPermission && wallpaperAvailable)

@Composable
internal fun AccentColorChoices(
    accentColorMode: AccentColorMode,
    customAccentColorArgb: Int,
    wallpaperAccentAvailable: Boolean,
    appTheme: AppTheme,
    deviceThemeEnabled: Boolean,
    onAccentColorModeChange: (AccentColorMode) -> Unit,
    onCustomAccentColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showCustomAccentPicker by rememberSaveable { mutableStateOf(false) }

    ConnectedChoiceGroup(
        options = buildList {
            add(ChoiceOption(AccentColorMode.NONE, stringResource(R.string.settings_app_theme_title)))
            if (wallpaperAccentAvailable) {
                add(ChoiceOption(AccentColorMode.FROM_WALLPAPER, stringResource(R.string.settings_accent_color_from_wallpaper)))
            }
            add(
                ChoiceOption(
                    value = AccentColorMode.CUSTOM,
                    label = stringResource(R.string.common_custom),
                    swatch = Color(customAccentColorArgb).takeIf { accentColorMode == AccentColorMode.CUSTOM },
                ),
            )
        },
        selected = accentColorMode,
        onSelect = { mode ->
            val blockedMessage = when {
                mode == AccentColorMode.FROM_WALLPAPER && !wallpaperAccentAvailable ->
                    R.string.settings_accent_color_wallpaper_required_toast
                mode == AccentColorMode.FROM_WALLPAPER && appTheme != AppTheme.MONOCHROME ->
                    R.string.settings_accent_color_default_theme_required_toast
                mode != AccentColorMode.NONE && deviceThemeEnabled ->
                    R.string.settings_device_theme_blocked_toast
                else -> null
            }
            if (blockedMessage != null) {
                AppMessages.show(context, blockedMessage)
            } else {
                hapticToggle(view)()
                if (mode == AccentColorMode.CUSTOM) {
                    showCustomAccentPicker = true
                } else if (accentColorMode != mode) {
                    onAccentColorModeChange(mode)
                }
            }
        },
        modifier = modifier,
        haptic = false,
    )

    if (showCustomAccentPicker) {
        WidgetColorPickerDialog(
            initialColor = Color(customAccentColorArgb),
            onDismiss = { showCustomAccentPicker = false },
            onConfirm = { color ->
                onCustomAccentColorChange(color.toArgb())
                if (accentColorMode != AccentColorMode.CUSTOM) {
                    onAccentColorModeChange(AccentColorMode.CUSTOM)
                }
                showCustomAccentPicker = false
            },
            title = stringResource(R.string.settings_accent_color_picker_title),
        )
    }
}
