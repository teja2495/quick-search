package com.tk.quicksearch.shared.permissions

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog

@Composable
fun PermissionSettingsDialog(
    permissionType: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.dialog_okay),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.dialog_cancel),
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
    )
}
