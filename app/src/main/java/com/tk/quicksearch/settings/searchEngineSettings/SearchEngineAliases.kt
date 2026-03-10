package com.tk.quicksearch.settings.searchEnginesScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.AliasValidator.isValidShortcutCode
import com.tk.quicksearch.searchEngines.AliasValidator.isValidShortcutPrefix
import com.tk.quicksearch.settings.shared.AliasPill
import com.tk.quicksearch.settings.searchEnginesScreen.AddEditAliasDialog

enum class AliasDisplayType {
    SEARCH_TYPE,
    SEARCH_ENGINE,
    TOOL,
}

/**
 * Display component for alias code with edit dialog.
 */
@Composable
internal fun AliasCodeDisplay(
    shortcutCode: String,
    isEnabled: Boolean,
    onCodeChange: ((String) -> Unit)?,
    engineName: String = "",
    existingShortcuts: Map<String, String> = emptyMap(),
    currentShortcutId: String? = null,
    validateCode: (String) -> Boolean = ::isValidShortcutCode,
    validateConflict: (String, Map<String, String>) -> Boolean = ::isValidShortcutPrefix,
    conflictErrorMessage: String? = null,
    addAliasLabel: String? = null,
    allowClearAction: Boolean = true,
    aliasDisplayType: AliasDisplayType = AliasDisplayType.SEARCH_ENGINE,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val isCustomEngine = currentShortcutId?.startsWith("custom:") == true
    val allowAliasDialog = !isCustomEngine

    if (showDialog && onCodeChange != null && allowAliasDialog) {
        AddEditAliasDialog(
            currentCode = shortcutCode,
            existingShortcuts = existingShortcuts,
            currentShortcutId = currentShortcutId,
            onSave = { code -> onCodeChange(code) },
            aliasInfoType =
                when (aliasDisplayType) {
                    AliasDisplayType.SEARCH_TYPE -> AliasInfoType.SEARCH_TYPE
                    AliasDisplayType.SEARCH_ENGINE -> AliasInfoType.SEARCH_ENGINE
                    AliasDisplayType.TOOL -> AliasInfoType.TOOL
                },
            aliasTargetName = engineName,
            dialogTitle =
                if (shortcutCode.isBlank()) {
                    stringResource(R.string.dialog_add_alias_for_search_type_title, engineName)
                } else {
                    stringResource(R.string.dialog_edit_alias_for_search_type_title, engineName)
                },
            validateCode = validateCode,
            validateConflict = validateConflict,
            conflictErrorMessage = conflictErrorMessage,
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
        } else if (isEnabled && shortcutCode.isNotBlank()) {
            AliasPill(
                text =
                    buildAnnotatedString {
                        append(shortcutCode)
                    },
                textStyle = MaterialTheme.typography.bodySmall,
                textColor = MaterialTheme.colorScheme.primary,
                leadingIcon = Icons.Rounded.Bolt,
                onClick = if (allowAliasDialog) ({ showDialog = true }) else null,
                onClearClick =
                    if (allowClearAction && shortcutCode.isNotBlank() && onCodeChange != null) {
                        { onCodeChange("") }
                    } else {
                        null
                    },
            )
        } else {
            AliasPill(
                text =
                    buildAnnotatedString {
                        append(
                            if (isCustomEngine) {
                                stringResource(R.string.settings_edit_label)
                            } else {
                                addAliasLabel ?: stringResource(R.string.settings_add_alias)
                            },
                        )
                    },
                textStyle =
                    if (isCustomEngine) {
                        MaterialTheme.typography.bodyMedium
                    } else {
                        MaterialTheme.typography.bodySmall
                    },
                textColor = MaterialTheme.colorScheme.primary,
                showBackground = false,
                leadingIcon = Icons.Rounded.Bolt,
                onClick = if (allowAliasDialog) ({ showDialog = true }) else null,
            )
        }
    }
}
