package com.tk.quicksearch.settings.searchEngineSettings.dialogs

import com.tk.quicksearch.search.noted.NotedEndAlias
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
import com.tk.quicksearch.searchEngines.AliasValidator.normalizeShortcutCodeInput
import com.tk.quicksearch.searchEngines.AliasValidator.hasTriggerAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.sanitizeAliasInput
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.AppTextButton

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
 * @param infoText Replaces the [aliasInfoType] explanation, for aliases none of its types describe
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
    existingTriggerWords: Collection<String> = emptyList(),
    conflictErrorMessage: String? = null,
    allowEmptyAlias: Boolean = true,
    infoText: String? = null,
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
    // Engine, tool, and section alias lists don't carry the Noted aliases; add them while active.
    val context = LocalContext.current
    val notedAlias = remember(context) { NotedEndAlias.activeAliasesById(context) }
    val existingShortcutsForValidation =
        if (currentShortcutId.isNullOrEmpty()) {
            existingShortcuts + notedAlias
        } else {
            (existingShortcuts + notedAlias).filterKeys { it != currentShortcutId }
        }
    val isValidConflict = validateConflict(editingCode.text, existingShortcutsForValidation)
    val hasTriggerConflict =
        remember(editingCode.text, existingTriggerWords) {
            hasTriggerAliasConflict(editingCode.text, existingTriggerWords)
        }
    val hasAnyConflict = !isValidConflict || hasTriggerConflict
    val showShortcutError = editingCode.text.isNotEmpty() && hasAnyConflict
    val isEmptyInput = editingCode.text.isEmpty()
    val confirmEnabled = if (allowEmptyAlias) {
        isEmptyInput || (isValidShortcut && !hasAnyConflict)
    } else {
        !isEmptyInput && isValidShortcut && !hasAnyConflict
    }
    val resolvedInfoText =
        infoText ?: when (aliasInfoType) {
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
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                Text(
                    text = resolvedInfoText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppOutlinedTextField(
                    value = editingCode,
                    onValueChange = {
                        editingCode = it.copy(text = sanitizeAliasInput(it.text))
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
                                    onSave(if (isEmptyInput) "" else normalizeShortcutCodeInput(editingCode.text))
                                    onDismiss()
                                }
                            },
                        ),
                    label = stringResource(R.string.settings_custom_tool_alias_label),
                    supportingText =
                        if (showShortcutError) {
                            {
                                Text(
                                    text =
                                        if (!isValidConflict) {
                                            conflictErrorMessage ?: stringResource(R.string.dialog_edit_alias_error_prefix)
                                        } else {
                                            stringResource(R.string.dialog_edit_alias_error_trigger_conflict)
                                        },
                                )
                            }
                        } else {
                            { Text(text = stringResource(R.string.dialog_alias_multiple_hint)) }
                        },
                    shape = AppShapes.extraSmall,
                )
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = {
                    if (confirmEnabled) {
                        onSave(if (isEmptyInput) "" else normalizeShortcutCodeInput(editingCode.text))
                        onDismiss()
                    }
                },
                enabled = confirmEnabled,
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
            ) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}
