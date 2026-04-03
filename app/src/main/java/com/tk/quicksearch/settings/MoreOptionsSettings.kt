package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.SettingsToggleRow

@Composable
fun MoreOptionsSettings(
    isToggleEnabled: (AppSettingsToggleKey) -> Boolean,
    onApplySettingsCommand: (SettingsCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleItems =
        listOf(
            ToggleItem(
                key = AppSettingsToggleKey.TOP_RESULT_INDICATOR,
                titleRes = R.string.top_result_indicator_toggle_title,
                subtitleRes = R.string.top_result_indicator_toggle_desc,
                leadingIcon = Icons.Rounded.CheckCircle,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.OPEN_KEYBOARD,
                titleRes = R.string.open_keyboard_toggle_title,
                subtitleRes = R.string.open_keyboard_toggle_desc,
                leadingIcon = Icons.Rounded.Keyboard,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.CLEAR_QUERY,
                titleRes = R.string.clear_query_toggle_title,
                subtitleRes = R.string.clear_query_toggle_desc,
                leadingIcon = Icons.Rounded.SearchOff,
            ),
            ToggleItem(
                key = AppSettingsToggleKey.AUTO_CLOSE_OVERLAY,
                titleRes = R.string.auto_close_overlay_toggle_title,
                subtitleRes = R.string.auto_close_overlay_toggle_desc,
                leadingIcon = Icons.Rounded.Close,
            ),
        )

    SettingsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            toggleItems.forEachIndexed { index, item ->
                SettingsToggleRow(
                    title = stringResource(item.titleRes),
                    subtitle = stringResource(item.subtitleRes),
                    checked = isToggleEnabled(item.key),
                    onCheckedChange = { enabled ->
                        onApplySettingsCommand(
                            SettingsCommand.Toggle(
                                key = item.key,
                                enabled = enabled,
                            ),
                        )
                    },
                    leadingIcon = item.leadingIcon,
                    isFirstItem = index == 0,
                    isLastItem = index == toggleItems.lastIndex,
                )
            }
        }
    }
}

private data class ToggleItem(
    val key: AppSettingsToggleKey,
    val titleRes: Int,
    val subtitleRes: Int,
    val leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
)
