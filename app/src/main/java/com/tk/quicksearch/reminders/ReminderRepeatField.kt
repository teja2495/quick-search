package com.tk.quicksearch.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

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
        OutlinedButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                imageVector = MaterialSymbols.Repeat,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = repeat?.let { ReminderRepeat.label(resources, it) }
                    ?: stringResource(R.string.custom_info_repeat_never),
                modifier = Modifier.weight(1f),
            )
            if (repeat != null) {
                IconButton(onClick = { onRepeatChange(null) }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = MaterialSymbols.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
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
                if (index > 0) HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            preset?.let { ReminderRepeat.label(resources, it) }
                                ?: stringResource(R.string.custom_info_repeat_never),
                        )
                    },
                    onClick = {
                        onRepeatChange(preset)
                        menuExpanded = false
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.common_custom)) },
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
