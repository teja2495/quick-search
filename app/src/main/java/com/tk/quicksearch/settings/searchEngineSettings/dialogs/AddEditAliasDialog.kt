package com.tk.quicksearch.settings.searchEnginesScreen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
import com.tk.quicksearch.searchEngines.AliasValidator.normalizeShortcutCodeInput

enum class AliasInfoType {
    SEARCH_TYPE,
    SEARCH_ENGINE,
    TOOL,
}

/**
 * Reusable dialog for adding or editing a search target alias code.
 *
 * @param currentCode The current alias code
 * @param existingShortcuts Existing aliases used for conflict validation
 * @param currentShortcutId Identifier for the shortcut being edited (excluded from validation)
 * @param onSave Callback when the code is saved
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun AddEditAliasDialog(
    currentCode: String,
    existingShortcuts: Map<String, String>,
    currentShortcutId: String? = null,
    onSave: (String) -> Unit,
    aliasInfoType: AliasInfoType = AliasInfoType.SEARCH_ENGINE,
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    aliasTargetName: String = "",
    dialogTitle: String? = null,
    validateCode: (String) -> Boolean = ::isValidGeneralAliasCode,
    validateConflict: (String, Map<String, String>) -> Boolean = { input, existing ->
        !hasExactAliasConflict(input, existing)
    },
    conflictErrorMessage: String? = null,
    allowEmptyAlias: Boolean = true,
    onDismiss: () -> Unit,
) {
    val initialText = normalizeShortcutCodeInput(currentCode)
    var editingCode by remember(currentCode) {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length),
            ),
        )
    }
    val focusRequester = remember { FocusRequester() }
    val isValidShortcut = validateCode(editingCode.text)
    val existingShortcutsForValidation =
        if (currentShortcutId.isNullOrEmpty()) {
            existingShortcuts
        } else {
            existingShortcuts.filterKeys { it != currentShortcutId }
        }
    val isValidConflict = validateConflict(editingCode.text, existingShortcutsForValidation)
    val showShortcutError = editingCode.text.isNotEmpty() && !isValidConflict
    val isEmptyInput = editingCode.text.isEmpty()
    val confirmEnabled = if (allowEmptyAlias) {
        isEmptyInput || (isValidShortcut && isValidConflict)
    } else {
        !isEmptyInput && isValidShortcut && isValidConflict
    }
    val infoText =
        when (aliasInfoType) {
            AliasInfoType.SEARCH_TYPE ->
                stringResource(
                    R.string.dialog_alias_info_search_type,
                    aliasTargetName,
                )
            AliasInfoType.SEARCH_ENGINE ->
                stringResource(
                    if (isSearchEngineAliasSuffixEnabled) {
                        R.string.dialog_alias_info_search_engine
                    } else {
                        R.string.dialog_alias_info_search_engine_start_only
                    },
                    aliasTargetName,
                )
            AliasInfoType.TOOL ->
                stringResource(
                    R.string.dialog_alias_info_tool,
                    aliasTargetName,
                )
        }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Ensure cursor is at the end of text
        editingCode = editingCode.copy(selection = TextRange(editingCode.text.length))
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = dialogTitle ?: stringResource(R.string.dialog_edit_alias_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextField(
                    value = editingCode,
                    onValueChange = {
                        val normalized = normalizeShortcutCodeInput(it.text)
                        editingCode = it.copy(text = normalized)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    singleLine = true,
                    maxLines = 1,
                    isError = showShortcutError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                if (confirmEnabled) {
                                    onSave(if (isEmptyInput) "" else editingCode.text)
                                    onDismiss()
                                }
                            },
                        ),
                    colors = dialogTextFieldColors(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = infoText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showShortcutError) {
                    val errorMessage =
                        when {
                            !isValidConflict ->
                                conflictErrorMessage ?: stringResource(R.string.dialog_edit_alias_error_prefix)
                            else -> ""
                        }
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (confirmEnabled) {
                        onSave(if (isEmptyInput) "" else editingCode.text)
                        onDismiss()
                    }
                },
                enabled = confirmEnabled,
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
fun EditAliasDialog(
    currentCode: String,
    existingShortcuts: Map<String, String>,
    currentShortcutId: String? = null,
    onSave: (String) -> Unit,
    aliasInfoType: AliasInfoType = AliasInfoType.SEARCH_ENGINE,
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    aliasTargetName: String = "",
    dialogTitle: String? = null,
    validateCode: (String) -> Boolean = ::isValidGeneralAliasCode,
    validateConflict: (String, Map<String, String>) -> Boolean = { input, existing ->
        !hasExactAliasConflict(input, existing)
    },
    conflictErrorMessage: String? = null,
    onDismiss: () -> Unit,
) = AddEditAliasDialog(
    currentCode = currentCode,
    existingShortcuts = existingShortcuts,
    currentShortcutId = currentShortcutId,
    onSave = onSave,
    aliasInfoType = aliasInfoType,
    isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled,
    aliasTargetName = aliasTargetName,
    dialogTitle = dialogTitle,
    validateCode = validateCode,
    validateConflict = validateConflict,
    conflictErrorMessage = conflictErrorMessage,
    onDismiss = onDismiss,
)
