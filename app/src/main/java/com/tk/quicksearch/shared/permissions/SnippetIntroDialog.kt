package com.tk.quicksearch.shared.permissions

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog

/**
 * First-run explainer for snippets, shown the first time a user starts a new snippet.
 *
 * Doubles as the consent surface for the expansion half of the accessibility service, so it always
 * offers a way to continue without granting the permission.
 */
@Composable
fun SnippetIntroDialog(
    isAccessibilityEnabled: Boolean,
    onGrantPermission: () -> Unit,
    onContinueWithout: () -> Unit,
) {
    AppAlertDialog(
        // Both buttons record that the intro was seen, so a stray outside tap must not dismiss it.
        onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.notes_snippet_intro_title)) },
        text = {
            // The feature explanation always shows; only the permission half is conditional.
            val explanation = stringResource(R.string.notes_snippet_intro_message)
            val permissionNote =
                if (isAccessibilityEnabled) {
                    stringResource(R.string.notes_snippet_intro_already_enabled)
                } else {
                    stringResource(R.string.notes_snippet_intro_needs_permission)
                }
            Text("$explanation\n\n$permissionNote")
        },
        confirmButton = {
            if (isAccessibilityEnabled) {
                TextButton(onClick = onContinueWithout) {
                    Text(stringResource(R.string.dialog_okay))
                }
            } else {
                TextButton(onClick = onGrantPermission) {
                    Text(stringResource(R.string.notes_snippet_intro_grant))
                }
            }
        },
        dismissButton =
            if (isAccessibilityEnabled) {
                null
            } else {
                {
                    TextButton(onClick = onContinueWithout) {
                        Text(stringResource(R.string.notes_snippet_intro_skip))
                    }
                }
            },
    )
}
