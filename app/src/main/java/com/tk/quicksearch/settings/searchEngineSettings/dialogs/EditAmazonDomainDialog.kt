package com.tk.quicksearch.settings.searchEngineSettings.dialogs

import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
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
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.*
import kotlinx.coroutines.delay
import com.tk.quicksearch.shared.ui.components.AppTextButton

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
    onDismiss: () -> Unit,
) {
    val defaultDomain = DEFAULT_AMAZON_DOMAIN
    val initialDomain = currentDomain ?: defaultDomain
    var editingDomain by remember(currentDomain) {
        mutableStateOf(
            TextFieldValue(
                text = initialDomain,
                selection = TextRange(initialDomain.length),
            ),
        )
    }
    val focusRequester = remember { FocusRequester() }

    // Normalize domain for validation (remove protocol, www, trailing slashes)
    val normalizedDomain =
        remember(editingDomain.text) {
            editingDomain.text
                .trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .removeSuffix("/")
        }

    // Validate domain (default domain is valid)
    val isValid =
        remember(normalizedDomain) {
            normalizedDomain.isBlank() || normalizedDomain == defaultDomain || isValidAmazonDomain(normalizedDomain)
        }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Small delay to ensure TextField is ready, then set cursor to end of text
        delay(50)
        editingDomain = editingDomain.copy(selection = TextRange(editingDomain.text.length))
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_edit_amazon_domain_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                Text(
                    text = stringResource(R.string.dialog_edit_amazon_domain_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppOutlinedTextField(
                    value = editingDomain,
                    onValueChange = {
                        val newText = it.text.replace(" ", "")
                        editingDomain = it.copy(text = newText)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    singleLine = true,
                    maxLines = 1,
                    isError = !isValid && normalizedDomain.isNotBlank() && normalizedDomain != defaultDomain,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                if (isValid) {
                                    val domainToSave =
                                        if (editingDomain.text.isBlank() || editingDomain.text.trim() == defaultDomain) {
                                            null
                                        } else {
                                            normalizedDomain
                                        }
                                    onSave(domainToSave)
                                    onDismiss()
                                }
                            },
                        ),
                    label = null,
                    shape = AppShapes.extraSmall,
                )
                if (!isValid && normalizedDomain.isNotBlank() && normalizedDomain != defaultDomain) {
                    Text(
                        text = stringResource(R.string.dialog_edit_amazon_domain_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.dialog_edit_amazon_domain_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = {
                    if (isValid) {
                        val domainToSave =
                            if (editingDomain.text.isBlank() || editingDomain.text.trim() == defaultDomain) {
                                null
                            } else {
                                normalizedDomain
                            }
                        onSave(domainToSave)
                        onDismiss()
                    }
                },
                enabled = isValid,
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}
