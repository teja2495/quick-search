package com.tk.quicksearch.settings.searchEnginesScreen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
import com.tk.quicksearch.searchEngines.AliasValidator.normalizeShortcutCodeInput
import com.tk.quicksearch.shared.util.withoutWhitespaces

@Composable
fun EditCustomSearchEngineDialog(
    customEngine: CustomSearchEngine,
    existingShortcuts: Map<String, String>,
    currentShortcutCode: String,
    onSave: (String, String, String, String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var nameInput by remember(customEngine.id) {
        mutableStateOf(
            TextFieldValue(
                text = customEngine.name,
                selection = TextRange(customEngine.name.length),
            ),
        )
    }
    var iconBase64 by remember(customEngine.id) { mutableStateOf(customEngine.faviconBase64) }

    val pickIconLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val encoded = loadCustomIconAsBase64(context, uri) ?: return@rememberLauncherForActivityResult
            iconBase64 = encoded
        }
    var urlInput by remember(customEngine.id) {
        mutableStateOf(
            TextFieldValue(
                text = customEngine.urlTemplate,
                selection = TextRange(customEngine.urlTemplate.length),
            ),
        )
    }
    var shortcutInput by remember(customEngine.id, currentShortcutCode) {
        mutableStateOf(
            TextFieldValue(
                text = normalizeShortcutCodeInput(currentShortcutCode),
                selection = TextRange(normalizeShortcutCodeInput(currentShortcutCode).length),
            ),
        )
    }

    val trimmedName = nameInput.text.trim()
    val isNameValid = trimmedName.isNotBlank()
    val validation = remember(urlInput.text) { validateCustomSearchTemplate(urlInput.text) }
    val validTemplate = (validation as? CustomSearchTemplateValidation.Valid)?.normalizedTemplate
    val showUrlError =
        urlInput.text.isNotBlank() && validation is CustomSearchTemplateValidation.Invalid
    val normalizedShortcut = remember(shortcutInput.text) { normalizeShortcutCodeInput(shortcutInput.text) }
    val isShortcutValid = remember(normalizedShortcut) { isValidGeneralAliasCode(normalizedShortcut) }
    val existingShortcutsForValidation =
        remember(customEngine.id, existingShortcuts) {
            existingShortcuts.filterKeys { it != "$CUSTOM_ID_PREFIX${customEngine.id}" }
        }
    val isShortcutPrefixValid =
        remember(normalizedShortcut, existingShortcutsForValidation) {
            !hasExactAliasConflict(normalizedShortcut, existingShortcutsForValidation)
        }
    val showShortcutError =
        shortcutInput.text.isNotBlank() && (!isShortcutValid || !isShortcutPrefixValid)
    val isShortcutAcceptable = normalizedShortcut.isBlank() || isShortcutValid

    val canSave = isNameValid && validTemplate != null && isShortcutAcceptable && isShortcutPrefixValid
    val iconBitmap =
        remember(iconBase64) {
            val encoded = iconBase64 ?: return@remember null
            val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
                ?: return@remember null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.settings_edit_search_engine_dialog_title))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.dialog_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    EditableIcon(
                        iconBitmap = iconBitmap,
                        contentDescription = customEngine.name,
                        modifier = Modifier.offset(y = (-2).dp),
                        onClick = { pickIconLauncher.launch(arrayOf("image/*")) },
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(R.string.settings_edit_search_engine_name_label)) },
                        singleLine = true,
                        maxLines = 1,
                        isError = !isNameValid && nameInput.text.isNotBlank(),
                        supportingText = {
                            if (!isNameValid && nameInput.text.isNotBlank()) {
                                Text(text = stringResource(R.string.settings_add_search_engine_error_required))
                            }
                        },
                    )
                }
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it.withoutWhitespaces() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.settings_edit_search_engine_url_label)) },
                    singleLine = true,
                    maxLines = 1,
                    isError = showUrlError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                if (canSave) {
                                    onSave(trimmedName, validTemplate!!, normalizedShortcut, iconBase64)
                                    onDismiss()
                                }
                            },
                        ),
                    supportingText = {
                        if (showUrlError) {
                            Text(
                                text =
                                    when ((validation as CustomSearchTemplateValidation.Invalid).reason) {
                                        CustomSearchTemplateValidation.Reason.EMPTY ->
                                            stringResource(R.string.settings_add_search_engine_error_required)
                                        CustomSearchTemplateValidation.Reason.MISSING_QUERY_PLACEHOLDER ->
                                            stringResource(R.string.settings_add_search_engine_error_placeholder)
                                        CustomSearchTemplateValidation.Reason.MULTIPLE_QUERY_PLACEHOLDERS ->
                                            stringResource(R.string.settings_add_search_engine_error_multiple_placeholders)
                                    },
                            )
                        }
                    },
                )

                OutlinedTextField(
                    value = shortcutInput,
                    onValueChange = {
                        val normalized = normalizeShortcutCodeInput(it.text)
                        shortcutInput = it.copy(text = normalized)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.settings_alias_label)) },
                    singleLine = true,
                    maxLines = 1,
                    isError = showShortcutError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                if (canSave) {
                                    onSave(trimmedName, validTemplate!!, normalizedShortcut, iconBase64)
                                    onDismiss()
                                }
                            },
                        ),
                    supportingText = {
                        if (showShortcutError) {
                            val errorMessage =
                                when {
                                    !isShortcutPrefixValid -> stringResource(R.string.dialog_edit_alias_error_prefix)
                                    !isShortcutValid -> stringResource(R.string.dialog_edit_alias_error_prefix)
                                    else -> ""
                                }
                            Text(text = errorMessage)
                        }
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(trimmedName, validTemplate!!, normalizedShortcut, iconBase64)
                        onDismiss()
                    }
                },
                enabled = canSave,
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
