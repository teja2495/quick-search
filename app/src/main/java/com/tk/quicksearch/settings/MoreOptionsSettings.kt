package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.theme.AppColors

@Composable
fun MoreOptionsSettings(
    appIconShape: AppIconShape,
    onSetAppIconShape: (AppIconShape) -> Unit,
    topResultIndicatorEnabled: Boolean,
    onTopResultIndicatorToggle: (Boolean) -> Unit,
    openKeyboardOnLaunch: Boolean,
    onOpenKeyboardOnLaunchToggle: (Boolean) -> Unit,
    clearQueryOnLaunch: Boolean,
    onClearQueryOnLaunchToggle: (Boolean) -> Unit,
    autoCloseOverlay: Boolean,
    onAutoCloseOverlayToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = AppColors.getCardElevation(false),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column {
            SettingsToggleRow(
                title = stringResource(R.string.auto_close_overlay_toggle_title),
                subtitle = stringResource(R.string.auto_close_overlay_toggle_desc),
                checked = autoCloseOverlay,
                onCheckedChange = onAutoCloseOverlayToggle,
                leadingIcon = Icons.Rounded.Close,
                isFirstItem = true,
                isLastItem = false,
            )

            SettingsToggleRow(
                title = stringResource(R.string.settings_circular_app_icons_title),
                subtitle = stringResource(R.string.settings_circular_app_icons_desc),
                checked = appIconShape == AppIconShape.CIRCLE,
                onCheckedChange = { enabled ->
                    onSetAppIconShape(
                        if (enabled) AppIconShape.CIRCLE else AppIconShape.DEFAULT,
                    )
                },
                leadingIcon = Icons.Rounded.Apps,
                isFirstItem = false,
                isLastItem = false,
            )

            SettingsToggleRow(
                title = stringResource(R.string.top_result_indicator_toggle_title),
                subtitle = stringResource(R.string.top_result_indicator_toggle_desc),
                checked = topResultIndicatorEnabled,
                onCheckedChange = onTopResultIndicatorToggle,
                leadingIcon = Icons.Rounded.CheckCircle,
                isFirstItem = false,
                isLastItem = false,
            )

            SettingsToggleRow(
                title = stringResource(R.string.open_keyboard_toggle_title),
                subtitle = stringResource(R.string.open_keyboard_toggle_desc),
                checked = openKeyboardOnLaunch,
                onCheckedChange = onOpenKeyboardOnLaunchToggle,
                leadingIcon = Icons.Rounded.Keyboard,
                isFirstItem = false,
                isLastItem = false,
            )

            SettingsToggleRow(
                title = stringResource(R.string.clear_query_toggle_title),
                subtitle = stringResource(R.string.clear_query_toggle_desc),
                checked = clearQueryOnLaunch,
                onCheckedChange = onClearQueryOnLaunchToggle,
                leadingIcon = Icons.Rounded.SearchOff,
                isFirstItem = false,
                isLastItem = true,
            )
        }
    }
}
