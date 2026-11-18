package com.tk.quicksearch.reminders

import com.tk.quicksearch.shared.ui.components.AppIconButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.customInfo.CustomInfoRepeat
import com.tk.quicksearch.customInfo.CustomInfoRepeatUnit
import com.tk.quicksearch.customInfo.CustomRepeatDialog
import com.tk.quicksearch.shared.time.ScheduleRepeat
import com.tk.quicksearch.shared.time.ScheduleRepeatUnit
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuDivider
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.PickerFieldButton

private val repeatPresets: List<ScheduleRepeat?> =
    listOf(null) + ScheduleRepeatUnit.entries.map { ScheduleRepeat(1, it) }

/**
 * The reminder form's repeat button, below the time: shows the current rule, opens the presets and
 * "Custom" (every N days, weeks, months or years), and has a clear button when a rule is set.
 */
@Composable
internal fun ReminderRepeatField(
    repeat: ScheduleRepeat?,
    onRepeatChange: (ScheduleRepeat?) -> Unit,
) {
    val resources = LocalContext.current.resources
    var menuExpanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PickerFieldButton(
                icon = MaterialSymbols.Repeat,
                value = repeat?.let { ReminderRepeat.label(resources, it) }
                    ?: stringResource(R.string.custom_info_repeat_never),
                onClick = { menuExpanded = true },
                modifier = Modifier.weight(1f),
            )
            if (repeat != null) {
                AppIconButton(onClick = { onRepeatChange(null) }, tooltip = stringResource(R.string.common_action_clear)) {
                    Icon(
                        imageVector = MaterialSymbols.Close,
                        contentDescription = stringResource(R.string.common_action_clear),
                    )
                }
            }
        }
        AppDropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            focusable = true,
        ) {
            repeatPresets.forEachIndexed { index, preset ->
                if (index > 0) AppDropdownMenuDivider()
                AppDropdownMenuItem(
                    text = preset?.let { ReminderRepeat.label(resources, it) } ?: stringResource(R.string.custom_info_repeat_never),
                    onClick = {
                        onRepeatChange(preset)
                        menuExpanded = false
                    },
                )
            }
            AppDropdownMenuDivider()
            AppDropdownMenuItem(
                text = stringResource(R.string.common_custom),
                onClick = {
                    menuExpanded = false
                    showCustomDialog = true
                },
            )
        }
    }

    if (showCustomDialog) {
        CustomRepeatDialog(
            initial = repeat?.let { rule ->
                CustomInfoRepeatUnit.fromStorageValue(ReminderRepeat.unitName(rule.unit))
                    ?.let { CustomInfoRepeat(it, rule.interval.coerceAtLeast(1)) }
            },
            onDismiss = { showCustomDialog = false },
            onConfirm = { custom ->
                onRepeatChange(ReminderRepeat.repeat(custom.interval, custom.unit.storageValue))
                showCustomDialog = false
            },
        )
    }
}
