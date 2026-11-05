package com.tk.quicksearch.customInfo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private const val MAX_REPEAT_INTERVAL = 999

/** "Daily", "Every 2 weeks", … for [repeat], or null when it doesn't repeat. */
@Composable
fun customInfoRepeatLabel(repeat: CustomInfoRepeat?): String? {
    repeat ?: return null
    if (repeat.interval == 1) {
        return stringResource(
            when (repeat.unit) {
                CustomInfoRepeatUnit.DAY -> R.string.calendar_repeats_daily
                CustomInfoRepeatUnit.WEEK -> R.string.calendar_repeats_weekly
                CustomInfoRepeatUnit.MONTH -> R.string.calendar_repeats_monthly
                CustomInfoRepeatUnit.YEAR -> R.string.calendar_repeats_yearly
            },
        )
    }
    return everyIntervalLabel(repeat.unit, repeat.interval)
}

@Composable
private fun everyIntervalLabel(unit: CustomInfoRepeatUnit, interval: Int): String =
    pluralStringResource(
        when (unit) {
            CustomInfoRepeatUnit.DAY -> R.plurals.calendar_repeats_every_days
            CustomInfoRepeatUnit.WEEK -> R.plurals.calendar_repeats_every_weeks
            CustomInfoRepeatUnit.MONTH -> R.plurals.calendar_repeats_every_months
            CustomInfoRepeatUnit.YEAR -> R.plurals.calendar_repeats_every_years
        },
        interval,
        interval,
    )

/**
 * The editor's repeat row: shows the current rule and opens the presets, with "Custom" leading to
 * an every-N-units dialog. Runs repeat from the chosen date and time.
 */
@Composable
internal fun CustomInfoRepeatRow(
    repeat: CustomInfoRepeat?,
    onRepeatChange: (CustomInfoRepeat?) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    val presets: List<CustomInfoRepeat?> =
        listOf(null, CustomInfoRepeat.DAILY, CustomInfoRepeat.WEEKLY, CustomInfoRepeat.MONTHLY, CustomInfoRepeat.YEARLY)

    Box {
        SettingsToggleRow(
            title = customInfoRepeatLabel(repeat) ?: stringResource(R.string.custom_info_repeat_never),
            checked = false,
            onCheckedChange = {},
            onRowClick = { menuExpanded = true },
            showSwitch = false,
            leadingIcon = MaterialSymbols.Repeat,
            titleTextStyle = MaterialTheme.typography.bodyLarge,
            horizontalPadding = DesignTokens.CardHorizontalPadding,
            trailingAction = { CustomInfoRowChevron() },
        )
        AppDropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            focusable = true,
        ) {
            presets.forEachIndexed { index, preset ->
                if (index > 0) HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(customInfoRepeatLabel(preset) ?: stringResource(R.string.custom_info_repeat_never))
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
            initial = repeat,
            onDismiss = { showCustomDialog = false },
            onConfirm = {
                onRepeatChange(it)
                showCustomDialog = false
            },
        )
    }
}

@Composable
private fun CustomRepeatDialog(
    initial: CustomInfoRepeat?,
    onDismiss: () -> Unit,
    onConfirm: (CustomInfoRepeat) -> Unit,
) {
    var intervalText by remember { mutableStateOf((initial?.interval ?: 2).toString()) }
    var unit by remember { mutableStateOf(initial?.unit ?: CustomInfoRepeatUnit.DAY) }
    val interval = intervalText.toIntOrNull()?.takeIf { it in 1..MAX_REPEAT_INTERVAL }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.common_custom)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { value -> intervalText = value.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.custom_info_repeat_every)) },
                    singleLine = true,
                    isError = interval == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                CustomInfoRepeatUnit.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { unit = option }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = unit == option, onClick = { unit = option })
                        Text(
                            text = everyIntervalLabel(option, interval ?: 1),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { interval?.let { onConfirm(CustomInfoRepeat(unit, it)) } },
                enabled = interval != null,
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}
