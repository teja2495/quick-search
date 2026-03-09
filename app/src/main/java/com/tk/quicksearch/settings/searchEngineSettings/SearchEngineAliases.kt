package com.tk.quicksearch.settings.searchEnginesScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.searchEnginesScreen.EditAliasDialog

/**
 * Display component for alias code with edit dialog.
 */
@Composable
internal fun AliasCodeDisplay(
    shortcutCode: String,
    isEnabled: Boolean,
    onCodeChange: ((String) -> Unit)?,
    onToggle: ((Boolean) -> Unit)?,
    engineName: String = "",
    existingShortcuts: Map<String, String> = emptyMap(),
    currentShortcutId: String? = null,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val isCustomEngine = currentShortcutId?.startsWith("custom:") == true
    val allowAliasDialog = !isCustomEngine

    if (showDialog && onCodeChange != null && allowAliasDialog) {
        EditAliasDialog(
            engineName = engineName,
            currentCode = shortcutCode,
            isEnabled = isEnabled,
            existingShortcuts = existingShortcuts,
            currentShortcutId = currentShortcutId,
            onSave = { code -> onCodeChange(code) },
            onToggle = onToggle,
            onDismiss = { showDialog = false },
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isCustomEngine) {
            Text(
                text = stringResource(R.string.settings_edit_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (isEnabled) {
            Text(
                text = stringResource(R.string.settings_alias_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = shortcutCode,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    if (allowAliasDialog) {
                        Modifier.clickable { showDialog = true }
                    } else {
                        Modifier
                    },
            )
        } else {
            Text(
                text =
                    if (isCustomEngine) {
                        stringResource(R.string.settings_edit_label)
                    } else {
                        stringResource(R.string.settings_add_alias)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    if (allowAliasDialog) {
                        Modifier.clickable { showDialog = true }
                    } else {
                        Modifier
                    },
            )
        }
    }
}
