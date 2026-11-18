package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog

/** Shown when an AI-backed feature is opened without any AI provider API key configured. */
@Composable
internal fun AiApiKeyRequiredDialog(
    onDismiss: () -> Unit,
    onSetupKey: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_ai_api_key_setup_action),
        onConfirm = {
            onDismiss()
            onSetupKey()
        },
        dismissLabel = stringResource(R.string.dialog_cancel),
        title = { Text(stringResource(R.string.settings_ai_api_key_required_title)) },
        text = { Text(stringResource(R.string.settings_ai_api_key_required_message)) },
    )
}
