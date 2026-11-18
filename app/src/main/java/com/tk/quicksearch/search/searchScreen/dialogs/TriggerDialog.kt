package com.tk.quicksearch.search.searchScreen.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.ResultTrigger
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import kotlinx.coroutines.delay
import com.tk.quicksearch.shared.ui.components.AppTextButton

@Composable
fun TriggerDialog(
    currentTrigger: ResultTrigger?,
    itemName: String,
    existingTriggerWords: Collection<String>,
    existingAliasWords: Collection<String> = emptyList(),
    onSave: (ResultTrigger?) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialText = currentTrigger?.word.orEmpty()
    var triggerText by
        remember(currentTrigger) {
            mutableStateOf(
                TextFieldValue(
                    text = initialText,
                    selection = TextRange(initialText.length),
                ),
            )
    }
    var triggerAfterSpace by remember(currentTrigger) {
        mutableStateOf(currentTrigger?.triggerAfterSpace ?: true)
    }
    val hasExistingTrigger = triggerText.text.isNotBlank()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val normalizedTrigger =
        remember(triggerText.text) {
            SearchTextNormalizer.normalizeForSearch(triggerText.text).trim().substringBefore(' ')
        }
    val hasTriggerConflict =
        remember(normalizedTrigger, existingTriggerWords) {
            normalizedTrigger.isNotBlank() &&
                existingTriggerWords.any {
                    SearchTextNormalizer.normalizeForSearch(it).trim().substringBefore(' ') ==
                        normalizedTrigger
                }
        }
    val hasAliasConflict =
        remember(normalizedTrigger, existingAliasWords) {
            normalizedTrigger.isNotBlank() &&
                existingAliasWords.any {
                    SearchTextNormalizer.normalizeForSearch(it).trim().substringBefore(' ') ==
                        normalizedTrigger
                }
        }
    val hasConflict = hasTriggerConflict || hasAliasConflict
    val canSave = !hasConflict

    LaunchedEffect(Unit) {
        delay(50)
        focusRequester.requestFocus()
        keyboardController?.show()
        triggerText = triggerText.copy(selection = TextRange(triggerText.text.length))
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    stringResource(
                        if (hasExistingTrigger) {
                            R.string.action_edit_trigger
                        } else {
                            R.string.action_add_trigger
                        },
                    ),
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dialog_trigger_message, itemName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(DesignTokens.SpacingLarge))

                AppOutlinedTextField(
                    value = triggerText,
                    onValueChange = { value ->
                        val word = value.text.trimStart().substringBefore(' ')
                        triggerText =
                            value.copy(
                                text = word,
                                selection = TextRange(word.length),
                            )
                    },
                    label = stringResource(R.string.dialog_trigger_hint),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    onClear = { triggerText = TextFieldValue("") },
                    isError = hasConflict,
                )
                if (hasConflict) {
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSmall))
                    Text(
                        text =
                            if (hasAliasConflict) {
                                stringResource(R.string.dialog_trigger_error_alias_conflict)
                            } else {
                                stringResource(R.string.dialog_trigger_error_taken)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.padding(top = DesignTokens.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = triggerAfterSpace,
                        onCheckedChange = { triggerAfterSpace = it },
                    )
                    Text(text = stringResource(R.string.alias_trigger_after_space))
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = {
                    val word = triggerText.text.trim().substringBefore(' ')
                    onSave(
                        if (word.isBlank()) {
                            null
                        } else {
                            ResultTrigger(word, triggerAfterSpace)
                        },
                    )
                },
                enabled = canSave,
            ) { Text(text = stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}
