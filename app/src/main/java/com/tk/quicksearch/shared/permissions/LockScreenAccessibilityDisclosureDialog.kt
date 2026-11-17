package com.tk.quicksearch.shared.permissions

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.AppTextButton

@Composable
fun LockScreenAccessibilityDisclosureDialog(
    onAgree: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accessibility_permission_disclaimer_title)) },
        text = { Text(stringResource(R.string.accessibility_lock_screen_disclosure_message)) },
        confirmButton = {
            AppTextButton(onClick = onAgree) {
                Text(stringResource(R.string.action_agree))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_not_now))
            }
        },
    )
}
