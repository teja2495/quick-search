package com.tk.quicksearch.shared.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R

/**
 * Material 3 modal date picker with the app's text buttons: Cancel dismisses, [confirmLabel]
 * (Save by default) calls [onConfirm]. The caller reads the picked date from [state].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    state: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = stringResource(R.string.dialog_save),
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppTextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}
