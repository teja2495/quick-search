package com.tk.quicksearch.settings.searchEngines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
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
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.searchengines.*
import com.tk.quicksearch.search.handlers.ShortcutValidator.isValidShortcutCode
import com.tk.quicksearch.search.handlers.ShortcutValidator.normalizeShortcutCodeInput

private const val DEFAULT_AMAZON_DOMAIN = "amazon.com"

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
    val initialText = normalizeShortcutCodeInput(currentCode)
    var editingCode by remember(currentCode) {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }
    var enabledState by remember(isEnabled) { mutableStateOf(isEnabled) }
    val focusRequester = remember { FocusRequester() }
    val isValidShortcut = isValidShortcutCode(editingCode.text)
    val showShortcutError = editingCode.text.isNotEmpty() && !isValidShortcut
    
    LaunchedEffect(Unit) {
        if (enabledState) {
            focusRequester.requestFocus()
            // Ensure cursor is at the end of text
            editingCode = editingCode.copy(selection = TextRange(editingCode.text.length))
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
                        val normalized = normalizeShortcutCodeInput(it.text)
                        editingCode = it.copy(text = normalized)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    enabled = enabledState,
                    singleLine = true,
                    maxLines = 1,
                    isError = showShortcutError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValidShortcut) {
                                onSave(editingCode.text)
                                onDismiss()
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                if (showShortcutError) {
                    Text(
                        text = stringResource(R.string.dialog_edit_shortcut_error_length),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
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
                    if (isValidShortcut) {
                        onSave(editingCode.text)
                        onDismiss()
                    }
                },
                enabled = isValidShortcut
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
    val defaultDomain = DEFAULT_AMAZON_DOMAIN
    val initialDomain = currentDomain ?: defaultDomain
    var editingDomain by remember(currentDomain) { 
        mutableStateOf(
            TextFieldValue(
                text = initialDomain,
                selection = TextRange(initialDomain.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    
    // Normalize domain for validation (remove protocol, www, trailing slashes)
    val normalizedDomain = remember(editingDomain.text) {
        editingDomain.text.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removeSuffix("/")
    }
    
    // Validate domain (default domain is valid)
    val isValid = remember(normalizedDomain) {
        normalizedDomain.isBlank() || normalizedDomain == defaultDomain || isValidAmazonDomain(normalizedDomain)
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Small delay to ensure TextField is ready, then set cursor to end of text
        delay(50)
        editingDomain = editingDomain.copy(selection = TextRange(editingDomain.text.length))
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
                    onValueChange = { 
                        val newText = it.text.replace(" ", "")
                        editingDomain = it.copy(text = newText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    maxLines = 1,
                    isError = !isValid && normalizedDomain.isNotBlank() && normalizedDomain != defaultDomain,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValid) {
                                val domainToSave = if (editingDomain.text.isBlank() || editingDomain.text.trim() == defaultDomain) {
                                    null
                                } else {
                                    normalizedDomain
                                }
                                onSave(domainToSave)
                                onDismiss()
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                if (!isValid && normalizedDomain.isNotBlank() && normalizedDomain != defaultDomain) {
                    Text(
                        text = stringResource(R.string.dialog_edit_amazon_domain_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = stringResource(R.string.dialog_edit_amazon_domain_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        val domainToSave = if (editingDomain.text.isBlank() || editingDomain.text.trim() == defaultDomain) {
                            null
                        } else {
                            normalizedDomain
                        }
                        onSave(domainToSave)
                        onDismiss()
                    }
                },
                enabled = isValid
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
 * Dialog for entering Gemini API key for Direct Search feature.
 * 
 * @param onSave Callback when the API key is saved
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun GeminiApiKeyDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var isObscured by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val clipboardManager = LocalClipboardManager.current
    val isValid = apiKeyInput.isNotBlank()
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_gemini_api_key_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_gemini_api_key_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.settings_gemini_api_key_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    visualTransformation = if (isObscured) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = stringResource(R.string.settings_gemini_api_key_paste),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        clipboardManager.getText()
                                            ?.text
                                            ?.trim()
                                            ?.takeIf { it.isNotEmpty() }
                                            ?.let { pasted ->
                                                apiKeyInput = pasted
                                                focusRequester.requestFocus()
                                            }
                                    },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val icon = if (isObscured) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { isObscured = !isObscured },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValid) {
                                onSave(apiKeyInput.trim())
                                onDismiss()
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onSave(apiKeyInput.trim())
                        onDismiss()
                    }
                },
                enabled = isValid
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
