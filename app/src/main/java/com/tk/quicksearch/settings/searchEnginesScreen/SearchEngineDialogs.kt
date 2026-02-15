package com.tk.quicksearch.settings.searchEnginesScreen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.searchEngines.*
import com.tk.quicksearch.search.searchEngines.ShortcutValidator.isValidShortcutCode
import com.tk.quicksearch.search.searchEngines.ShortcutValidator.isValidShortcutPrefix
import com.tk.quicksearch.search.searchEngines.ShortcutValidator.normalizeShortcutCodeInput
import kotlinx.coroutines.delay

private const val DEFAULT_AMAZON_DOMAIN = "amazon.com"

@Composable
fun AddSearchEngineDialog(
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var urlInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }
    val focusRequester = remember { FocusRequester() }
    val validation = remember(urlInput.text) { validateCustomSearchTemplate(urlInput.text) }
    val validTemplate = (validation as? CustomSearchTemplateValidation.Valid)?.normalizedTemplate
    val showValidationError =
        urlInput.text.isNotBlank() && validation is CustomSearchTemplateValidation.Invalid

    val isFetchingFavicon = false

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Favicon fetching is temporarily disabled.
    // LaunchedEffect(validTemplate) {
    //     if (validTemplate == null) return@LaunchedEffect
    //     val fetched = withContext(Dispatchers.IO) { fetchFaviconAsBase64(validTemplate) }
    // }

    val canSave = validTemplate != null && !isFetchingFavicon

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_add_search_engine_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_add_search_engine_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                TextField(
                    value = urlInput,
                    onValueChange = { newValue -> urlInput = newValue },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    singleLine = true,
                    maxLines = 1,
                    isError = showValidationError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                if (canSave) {
                                    onSave(validTemplate!!, "")
                                    onDismiss()
                                }
                            },
                        ),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                )

                when {
                    showValidationError -> {
                        Text(
                            text =
                                when ((validation as CustomSearchTemplateValidation.Invalid).reason) {
                                    CustomSearchTemplateValidation.Reason.EMPTY ->
                                        stringResource(R.string.settings_add_search_engine_error_required)
                                    CustomSearchTemplateValidation.Reason.INVALID_URL ->
                                        stringResource(R.string.settings_add_search_engine_error_invalid_url)
                                    CustomSearchTemplateValidation.Reason.UNSUPPORTED_SCHEME ->
                                        stringResource(R.string.settings_add_search_engine_error_scheme)
                                    CustomSearchTemplateValidation.Reason.MISSING_QUERY_PLACEHOLDER ->
                                        stringResource(R.string.settings_add_search_engine_error_placeholder)
                                    CustomSearchTemplateValidation.Reason.MULTIPLE_QUERY_PLACEHOLDERS ->
                                        stringResource(R.string.settings_add_search_engine_error_multiple_placeholders)
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(validTemplate!!, "")
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
    val isShortcutValid = remember(normalizedShortcut) { isValidShortcutCode(normalizedShortcut) }
    val existingShortcutsForValidation =
        remember(customEngine.id, existingShortcuts) {
            existingShortcuts.filterKeys { it != "$CUSTOM_ID_PREFIX${customEngine.id}" }
        }
    val isShortcutPrefixValid =
        remember(normalizedShortcut, existingShortcutsForValidation) {
            isValidShortcutPrefix(normalizedShortcut, existingShortcutsForValidation)
        }
    val showShortcutError =
        shortcutInput.text.isNotBlank() && (!isShortcutValid || !isShortcutPrefixValid)

    val canSave = isNameValid && validTemplate != null && isShortcutValid && isShortcutPrefixValid
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
                    Box(
                        modifier =
                            Modifier
                                .size(44.dp)
                                .offset(y = (-4).dp),
                    ) {
                        androidx.compose.material3.Surface(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .align(androidx.compose.ui.Alignment.Center)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        pickIconLauncher.launch(arrayOf("image/*"))
                                    },
                            tonalElevation = 1.dp,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Box(modifier = Modifier.size(40.dp)) {
                                if (iconBitmap != null) {
                                    Image(
                                        bitmap = iconBitmap,
                                        contentDescription = customEngine.name,
                                        modifier = Modifier.size(40.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Public,
                                            contentDescription = customEngine.name,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        Box(
                            modifier =
                                Modifier
                                    .align(androidx.compose.ui.Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

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
                    onValueChange = { urlInput = it },
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
                                        CustomSearchTemplateValidation.Reason.INVALID_URL ->
                                            stringResource(R.string.settings_add_search_engine_error_invalid_url)
                                        CustomSearchTemplateValidation.Reason.UNSUPPORTED_SCHEME ->
                                            stringResource(R.string.settings_add_search_engine_error_scheme)
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
                    label = { Text(text = stringResource(R.string.settings_shortcut_label)) },
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
                                    !isShortcutPrefixValid -> stringResource(R.string.dialog_edit_shortcut_error_prefix)
                                    !isShortcutValid -> stringResource(R.string.dialog_edit_shortcut_error_length)
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

/**
 * Dialog for editing a search engine shortcut code.
 *
 * @param engineName The display name of the search engine
 * @param currentCode The current shortcut code
 * @param isEnabled Whether the shortcut is currently enabled
 * @param existingShortcuts Existing shortcuts for prefix validation
 * @param currentShortcutId Identifier for the shortcut being edited (excluded from validation)
 * @param onSave Callback when the code is saved
 * @param onToggle Optional callback when the enabled state changes
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun EditShortcutDialog(
    engineName: String,
    currentCode: String,
    isEnabled: Boolean,
    existingShortcuts: Map<String, String>,
    currentShortcutId: String? = null,
    onSave: (String) -> Unit,
    onToggle: ((Boolean) -> Unit)?,
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
    val initialEnabledState = if (onToggle == null) true else isEnabled
    var enabledState by remember(isEnabled, onToggle) { mutableStateOf(initialEnabledState) }
    val focusRequester = remember { FocusRequester() }
    val isValidShortcut = isValidShortcutCode(editingCode.text)
    val existingShortcutsForValidation =
        if (currentShortcutId.isNullOrEmpty()) {
            existingShortcuts
        } else {
            existingShortcuts.filterKeys { it != currentShortcutId }
        }
    val isValidPrefix = isValidShortcutPrefix(editingCode.text, existingShortcutsForValidation)
    val showShortcutError = editingCode.text.isNotEmpty() && (!isValidShortcut || !isValidPrefix)

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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.dialog_edit_shortcut_message, engineName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
                    enabled = enabledState,
                    singleLine = true,
                    maxLines = 1,
                    isError = showShortcutError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                if (isValidShortcut && isValidPrefix) {
                                    onSave(editingCode.text)
                                    onDismiss()
                                }
                            },
                        ),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                )
                if (showShortcutError) {
                    val errorMessage =
                        when {
                            !isValidPrefix -> stringResource(R.string.dialog_edit_shortcut_error_prefix)
                            !isValidShortcut -> stringResource(R.string.dialog_edit_shortcut_error_length)
                            else -> ""
                        }
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (onToggle != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Checkbox(
                            checked = enabledState,
                            onCheckedChange = {
                                enabledState = it
                                onToggle(it)
                            },
                        )
                        Text(
                            text = stringResource(R.string.dialog_enable_shortcut),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValidShortcut && isValidPrefix) {
                        onSave(editingCode.text)
                        onDismiss()
                    }
                },
                enabled = isValidShortcut && isValidPrefix,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_edit_amazon_domain_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.dialog_edit_amazon_domain_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextField(
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
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
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
            Button(
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
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
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
    onDismiss: () -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.dialog_gemini_api_key_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.settings_gemini_api_key_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    visualTransformation = if (isObscured) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = stringResource(R.string.settings_gemini_api_key_paste),
                                modifier =
                                    Modifier
                                        .size(20.dp)
                                        .clickable {
                                            clipboardManager
                                                .getText()
                                                ?.text
                                                ?.trim()
                                                ?.takeIf { it.isNotEmpty() }
                                                ?.let { pasted ->
                                                    apiKeyInput = pasted
                                                    focusRequester.requestFocus()
                                                }
                                        },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val icon = if (isObscured) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .size(20.dp)
                                        .clickable { isObscured = !isObscured },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                if (isValid) {
                                    onSave(apiKeyInput.trim())
                                    onDismiss()
                                }
                            },
                        ),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
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
                enabled = isValid,
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
