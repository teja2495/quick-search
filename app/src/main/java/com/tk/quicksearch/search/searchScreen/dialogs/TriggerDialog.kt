package com.tk.quicksearch.search.searchScreen.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.ResultTrigger
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import kotlinx.coroutines.delay

@Composable
fun TriggerDialog(
    currentTrigger: ResultTrigger?,
    itemName: String,
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
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = triggerText,
                    onValueChange = { value ->
                        val word = value.text.trimStart().substringBefore(' ')
                        triggerText =
                            value.copy(
                                text = word,
                                selection = TextRange(word.length),
                            )
                    },
                    label = { Text(stringResource(R.string.dialog_trigger_hint)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                    trailingIcon = {
                        if (triggerText.text.isNotEmpty()) {
                            IconButton(onClick = { triggerText = TextFieldValue("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.desc_clear_search),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    colors = dialogTextFieldColors(),
                )

                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = triggerAfterSpace,
                        onCheckedChange = { triggerAfterSpace = it },
                    )
                    Text(text = stringResource(R.string.dialog_trigger_after_space))
                }
            }
        },
        confirmButton = {
            Button(
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
            ) { Text(text = stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}
