package com.tk.quicksearch.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R

/**
 * Dialog for editing a search engine shortcut code.
 * 
 * @param engineName The display name of the search engine
 * @param currentCode The current shortcut code
 * @param isEnabled Whether the shortcut is currently enabled
 * @param onSave Callback when the code is saved
 * @param onToggle Optional callback when the enabled state changes
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun EditShortcutDialog(
    engineName: String,
    currentCode: String,
    isEnabled: Boolean,
    onSave: (String) -> Unit,
    onToggle: ((Boolean) -> Unit)?,
    onDismiss: () -> Unit
) {
    var editingCode by remember(currentCode) { mutableStateOf(currentCode) }
    var enabledState by remember(isEnabled) { mutableStateOf(isEnabled) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        if (enabledState) {
            focusRequester.requestFocus()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_edit_shortcut_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_edit_shortcut_message, engineName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextField(
                    value = editingCode,
                    onValueChange = { 
                        editingCode = it.lowercase().filter { char -> 
                            char.isLetterOrDigit() && char != ' ' 
                        } 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    enabled = enabledState,
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (editingCode.isNotBlank()) {
                                onSave(editingCode)
                            }
                            onDismiss()
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                if (onToggle != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = enabledState,
                            onCheckedChange = { 
                                enabledState = it
                                onToggle(it)
                            }
                        )
                        Text(
                            text = stringResource(R.string.dialog_enable_shortcut),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editingCode.isNotBlank()) {
                        onSave(editingCode)
                    }
                    onDismiss()
                },
                enabled = editingCode.isNotBlank()
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        }
    )
}

/**
 * Dialog for editing Amazon domain.
 * 
 * @param currentDomain The current Amazon domain (e.g., "amazon.co.uk" or null for default "amazon.com")
 * @param onSave Callback when the domain is saved
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun EditAmazonDomainDialog(
    currentDomain: String?,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultDomain = "amazon.com"
    var editingDomain by remember(currentDomain) { mutableStateOf(currentDomain ?: "") }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_edit_amazon_domain_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_edit_amazon_domain_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextField(
                    value = editingDomain,
                    onValueChange = { editingDomain = it.replace(" ", "") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    maxLines = 1,
                    placeholder = {
                        Text(text = defaultDomain)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val domainToSave = if (editingDomain.isBlank() || editingDomain.trim() == defaultDomain) {
                                null
                            } else {
                                editingDomain.trim()
                            }
                            onSave(domainToSave)
                            onDismiss()
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                Text(
                    text = stringResource(R.string.dialog_edit_amazon_domain_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val domainToSave = if (editingDomain.isBlank() || editingDomain.trim() == defaultDomain) {
                        null
                    } else {
                        editingDomain.trim()
                    }
                    onSave(domainToSave)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        }
    )
}
