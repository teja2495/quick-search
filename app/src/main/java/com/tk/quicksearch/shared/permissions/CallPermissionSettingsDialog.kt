package com.tk.quicksearch.shared.permissions

import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppTextButton

@Composable
fun PermissionSettingsDialog(
    permissionType: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.permission_settings_dialog_title, permissionType),
            )
        },
        text = {
            Text(
                text = stringResource(R.string.permission_settings_dialog_message, permissionType),
            )
        },
        confirmButton = {
            AppTextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.dialog_okay))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}
