package com.tk.quicksearch.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.AppTextButton

/**
 * Standard confirmation dialog for deleting a single note.
 */
@Composable
fun NoteDeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.notes_delete_confirm_title))
        },
        text = {
            Text(
                text = stringResource(R.string.notes_delete_confirm_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        confirmButton = {
            AppTextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.dialog_delete))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

/**
 * Standard confirmation dialog for deleting multiple selected notes.
 */
@Composable
fun NotesBulkDeleteConfirmationDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.notes_delete_selected_confirm_title))
        },
        text = {
            Text(
                text = stringResource(R.string.notes_delete_selected_confirm_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        confirmButton = {
            AppTextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.dialog_delete))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}
