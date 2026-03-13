package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsToggleRow

@Composable
fun MoreOptionsSettings(
    topResultIndicatorEnabled: Boolean,
    onTopResultIndicatorToggle: (Boolean) -> Unit,
    openKeyboardOnLaunch: Boolean,
    onOpenKeyboardOnLaunchToggle: (Boolean) -> Unit,
    clearQueryOnLaunch: Boolean,
    onClearQueryOnLaunchToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column {
            SettingsToggleRow(
                title = stringResource(R.string.top_result_indicator_toggle_title),
                subtitle = stringResource(R.string.top_result_indicator_toggle_desc),
                checked = topResultIndicatorEnabled,
                onCheckedChange = onTopResultIndicatorToggle,
                leadingIcon = Icons.Rounded.CheckCircle,
                isFirstItem = true,
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
