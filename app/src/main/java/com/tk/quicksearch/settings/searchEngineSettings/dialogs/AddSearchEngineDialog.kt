package com.tk.quicksearch.settings.searchEnginesScreen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import com.tk.quicksearch.shared.ui.components.AppBottomSheet
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.shared.util.withoutWhitespaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddSearchEngineDialog(
    availableBrowsers: List<com.tk.quicksearch.search.core.BrowserApp> = emptyList(),
    onSave: (String, String, String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var urlInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }
    var nameInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }
    var iconBase64 by remember { mutableStateOf<String?>(null) }
    var selectedBrowserPackage by remember { mutableStateOf<String?>(null) }
    var isEditingName by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }
    val validation = remember(urlInput.text) { validateCustomSearchTemplate(urlInput.text) }
    val validTemplate = (validation as? CustomSearchTemplateValidation.Valid)?.normalizedTemplate
    val showValidationError =
        urlInput.text.isNotBlank() && validation is CustomSearchTemplateValidation.Invalid

    val coroutineScope = rememberCoroutineScope()
    val pickIconLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    loadCustomIconAsBase64(context, uri, maxSizePx = 256)
                } ?: return@launch
                iconBase64 = encoded
            }
        }

    // Delay focus so the sheet finishes its expand animation before the keyboard appears
    LaunchedEffect(Unit) {
        delay(500)
        focusRequester.requestFocus()
    }

    LaunchedEffect(validTemplate) {
        if (validTemplate == null) {
            isEditingName = false
            iconBase64 = null
            nameInput = nameInput.copy(text = "", selection = TextRange(0))
            return@LaunchedEffect
        }

        val inferredName = withContext(Dispatchers.IO) { inferCustomSearchEngineName(validTemplate) }
        val fetchedIcon = withContext(Dispatchers.IO) { fetchFaviconAsBase64(validTemplate) }

        if (!fetchedIcon.isNullOrBlank()) {
            iconBase64 = fetchedIcon
        }
        if (!inferredName.isNullOrBlank()) {
            nameInput =
                TextFieldValue(
                    text = inferredName,
                    selection = TextRange(inferredName.length),
                )
            isEditingName = false
        } else {
            isEditingName = true
        }
    }

    LaunchedEffect(isEditingName) {
        if (isEditingName) {
            nameFocusRequester.requestFocus()
        }
    }

    val trimmedName = nameInput.text.trim()
    val isNameValid = trimmedName.isNotBlank()
    val canSave = validTemplate != null && isNameValid
    val iconBitmap =
        remember(iconBase64) {
            val encoded = iconBase64 ?: return@remember null
            val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
                ?: return@remember null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }

    val scrollState = rememberScrollState()

    AppBottomSheet(onDismissRequest = onDismiss, swipeToDismissEnabled = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            // Scrollable form content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = DesignTokens.ContentHorizontalPadding,
                        vertical = DesignTokens.SpacingLarge,
                    ),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                Text(
                    text = stringResource(R.string.settings_add_search_engine_button),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (validTemplate != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EditableIcon(
                            iconBitmap = iconBitmap,
                            contentDescription = trimmedName,
                            modifier = Modifier.offset(y = (-2).dp),
                            onClick = { pickIconLauncher.launch(arrayOf("image/*")) },
                        )
                        if (isEditingName) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .focusRequester(nameFocusRequester),
                                label = {
                                    Text(text = stringResource(R.string.settings_app_sort_name))
                                },
                                singleLine = true,
                                maxLines = 1,
                                isError = !isNameValid,
                                colors = dialogTextFieldColors(),
                            )
                        } else {
                            Text(
                                text = nameInput.text,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .offset(y = (-2).dp)
                                        .clickable { isEditingName = true },
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.settings_add_search_engine_dialog_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                TextField(
                    value = urlInput,
                    onValueChange = { newValue -> urlInput = newValue.withoutWhitespaces() },
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
                                    onSave(trimmedName, validTemplate!!, iconBase64.orEmpty(), selectedBrowserPackage)
                                    onDismiss()
                                }
                            },
                        ),
                    colors = dialogTextFieldColors(),
                )

                when {
                    showValidationError -> {
                        when ((validation as CustomSearchTemplateValidation.Invalid).reason) {
                            CustomSearchTemplateValidation.Reason.EMPTY -> {
                                Text(
                                    text = stringResource(R.string.settings_add_search_engine_error_required),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }

                            CustomSearchTemplateValidation.Reason.MISSING_QUERY_PLACEHOLDER -> {
                                val fullMessage = stringResource(R.string.settings_add_search_engine_error_placeholder)
                                val messageText =
                                    buildAnnotatedString {
                                        val placeholderStart = fullMessage.indexOf(CUSTOM_QUERY_PLACEHOLDER)
                                        if (placeholderStart < 0) {
                                            append(fullMessage)
                                            return@buildAnnotatedString
                                        }
                                        val placeholderEnd = placeholderStart + CUSTOM_QUERY_PLACEHOLDER.length
                                        append(fullMessage.substring(0, placeholderStart))
                                        pushStringAnnotation(tag = "placeholder", annotation = CUSTOM_QUERY_PLACEHOLDER)
                                        withStyle(
                                            style =
                                                SpanStyle(
                                                    color = AppColors.LinkColor,
                                                ),
                                        ) {
                                            append(CUSTOM_QUERY_PLACEHOLDER)
                                        }
                                        pop()
                                        append(fullMessage.substring(placeholderEnd))
                                    }
                                ClickableText(
                                    text = messageText,
                                    style =
                                        MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.error,
                                        ),
                                    onClick = { offset ->
                                        val hasPlaceholderLink =
                                            messageText
                                                .getStringAnnotations(
                                                    tag = "placeholder",
                                                    start = offset,
                                                    end = offset,
                                                ).isNotEmpty()
                                        if (hasPlaceholderLink) {
                                            val appendedText = "${urlInput.text}$CUSTOM_QUERY_PLACEHOLDER"
                                            urlInput =
                                                TextFieldValue(
                                                    text = appendedText,
                                                    selection = TextRange(appendedText.length),
                                                )
                                        }
                                    },
                                )
                            }

                            CustomSearchTemplateValidation.Reason.MULTIPLE_QUERY_PLACEHOLDERS -> {
                                Text(
                                    text =
                                        stringResource(
                                            R.string.settings_add_search_engine_error_multiple_placeholders,
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }

                if (validTemplate != null) {
                    BrowserPickerField(
                        availableBrowsers = availableBrowsers,
                        selectedPackage = selectedBrowserPackage,
                        onSelect = { selectedBrowserPackage = it },
                        onExpand = { coroutineScope.launch { scrollState.animateScrollTo(Int.MAX_VALUE) } },
                    )
                }
            }

            // Pinned action buttons — always visible above keyboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DesignTokens.ContentHorizontalPadding,
                        vertical = DesignTokens.SpacingMedium,
                    ),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
                Button(
                    onClick = {
                        if (canSave) {
                            onSave(trimmedName, validTemplate!!, iconBase64.orEmpty(), selectedBrowserPackage)
                            onDismiss()
                        }
                    },
                    enabled = canSave,
                ) {
                    Text(text = stringResource(R.string.dialog_save))
                }
            }
        }
    }
}
