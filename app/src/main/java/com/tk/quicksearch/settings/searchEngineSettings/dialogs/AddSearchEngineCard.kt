package com.tk.quicksearch.settings.searchEngineSettings.dialogs

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.rotate
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
import com.tk.quicksearch.search.core.BrowserApp
import com.tk.quicksearch.searchEngines.CUSTOM_QUERY_PLACEHOLDER
import com.tk.quicksearch.searchEngines.CustomSearchTemplateValidation
import com.tk.quicksearch.searchEngines.fetchFaviconAsBase64
import com.tk.quicksearch.searchEngines.inferCustomSearchEngineName
import com.tk.quicksearch.searchEngines.IN_APP_BROWSER_PACKAGE
import com.tk.quicksearch.searchEngines.loadCustomIconAsBase64
import com.tk.quicksearch.searchEngines.validateCustomSearchTemplate
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.SegmentedTileContentPadding
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.withoutWhitespaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.withLink
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppTextButton

@Composable
fun AddSearchEngineCard(
    availableBrowsers: List<BrowserApp> = emptyList(),
    onSave: (String, String, String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    AddSearchEngineForm(
        availableBrowsers = availableBrowsers,
        onSave = onSave,
        onCancel = null,
        modifier = modifier,
        requestInitialFocus = false,
    )
}

@Composable
private fun AddSearchEngineForm(
    availableBrowsers: List<BrowserApp>,
    onSave: (String, String, String, String?) -> Unit,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean,
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
    var selectedBrowserPackage by remember { mutableStateOf<String?>(IN_APP_BROWSER_PACKAGE) }
    var isEditingName by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }
    val validation = remember(urlInput.text) { validateCustomSearchTemplate(urlInput.text) }
    val validTemplate = (validation as? CustomSearchTemplateValidation.Valid)?.normalizedTemplate
    val showValidationError =
        urlInput.text.isNotBlank() && validation is CustomSearchTemplateValidation.Invalid

    fun resetForm() {
        urlInput = TextFieldValue(text = "", selection = TextRange(0))
        nameInput = TextFieldValue(text = "", selection = TextRange(0))
        iconBase64 = null
        selectedBrowserPackage = IN_APP_BROWSER_PACKAGE
        isEditingName = false
    }

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

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            delay(500)
            focusRequester.requestFocus()
        }
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
    var isExpanded by rememberSaveable { mutableStateOf(requestInitialFocus) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "addSearchEngineChevronRotation",
    )
    val headerAlignmentBias by animateFloatAsState(
        targetValue = if (isExpanded) -1f else 0f,
        label = "addSearchEngineHeaderAlignment",
    )
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    // Once the form has expanded, scroll so the whole card is on screen.
    LaunchedEffect(isExpanded) {
        if (isExpanded && !requestInitialFocus) {
            delay(ExpandScrollDelayMillis)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().bringIntoViewRequester(bringIntoViewRequester),
    ) {
        // Collapsed by default like Add Custom Provider: a header tile that expands the form.
        SegmentedTileRow(
            title = stringResource(R.string.settings_add_search_engine_button),
            // The URL instructions show under the title only while the form is open.
            description = stringResource(R.string.settings_add_search_engine_dialog_message),
            descriptionVisible = isExpanded,
            // Glides from centered to top-aligned as the description grows in.
            verticalAlignment = BiasAlignment.Vertical(headerAlignmentBias),
            leading = { TonalIconBadge(icon = MaterialSymbols.Add) },
            trailing = {
                Icon(
                    imageVector = MaterialSymbols.ExpandMore,
                    contentDescription =
                        stringResource(if (isExpanded) R.string.desc_collapse else R.string.desc_expand),
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            modifier = Modifier.clickable(role = Role.Button) { isExpanded = !isExpanded },
        )
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // The header row's padding already sits above, so only the sides and bottom pad here.
                val formContentModifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = SegmentedTileContentPadding,
                            end = SegmentedTileContentPadding,
                            bottom = SegmentedTileContentPadding,
                        )

                Column(
                    modifier =
                        if (requestInitialFocus) {
                            formContentModifier.verticalScroll(scrollState)
                        } else {
                            formContentModifier
                        },
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                ) {
                    if (validTemplate != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EditableIcon(
                                iconBitmap = iconBitmap,
                                contentDescription = trimmedName,
                                modifier = Modifier.offset(y = (-2).dp),
                                onClick = { pickIconLauncher.launch(arrayOf("image/*")) },
                            )
                            if (isEditingName) {
                                AppOutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .focusRequester(nameFocusRequester),
                                    label = stringResource(R.string.settings_app_sort_name),
                                    singleLine = true,
                                    maxLines = 1,
                                    isError = !isNameValid,
                                    shape = AppShapes.extraSmall,
                                )
                            } else {
                                Text(
                                    text = nameInput.text,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .offset(y = (-2).dp)
                                            .clickable(role = Role.Button) { isEditingName = true },
                                )
                            }
                        }
                    }

                    AppOutlinedTextField(
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
                                        onSave(trimmedName, validTemplate, iconBase64.orEmpty(), selectedBrowserPackage)
                                        resetForm()
                                        onCancel?.invoke()
                                        isExpanded = false
                                    }
                                },
                            ),
                        label = stringResource(R.string.settings_app_shortcuts_url_label),
                        supportingText =
                            if (showValidationError) {
                                {
                                    when (validation.reason) {
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
                                                    val placeholderLink =
                                                        LinkAnnotation.Clickable(tag = CUSTOM_QUERY_PLACEHOLDER) {
                                                            val appendedText = "${urlInput.text}$CUSTOM_QUERY_PLACEHOLDER"
                                                            urlInput =
                                                                TextFieldValue(
                                                                    text = appendedText,
                                                                    selection = TextRange(appendedText.length),
                                                                )
                                                        }
                                                    withLink(placeholderLink) {
                                                        withStyle(
                                                            style =
                                                                SpanStyle(
                                                                    color = AppColors.LinkColor,
                                                                ),
                                                        ) {
                                                            append(CUSTOM_QUERY_PLACEHOLDER)
                                                        }
                                                    }
                                                    append(fullMessage.substring(placeholderEnd))
                                                }
                                            Text(
                                                text = messageText,
                                                style =
                                                    MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.error,
                                                    ),
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
                            } else {
                                null
                            },
                        shape = AppShapes.extraSmall,
                    )

                    if (validTemplate != null) {
                        BrowserPickerField(
                            availableBrowsers = availableBrowsers,
                            selectedPackage = selectedBrowserPackage,
                            onSelect = { selectedBrowserPackage = it },
                        )
                    }
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = SegmentedTileContentPadding,
                                end = SegmentedTileContentPadding,
                                bottom = SegmentedTileContentPadding,
                            ),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppTextButton(
                        onClick = { resetForm() },
                        enabled = urlInput.text.isNotEmpty(),
                    ) {
                        Text(text = stringResource(R.string.common_action_clear))
                    }
                    AppButton(
                        onClick = {
                            if (canSave) {
                                onSave(trimmedName, validTemplate, iconBase64.orEmpty(), selectedBrowserPackage)
                                resetForm()
                                onCancel?.invoke()
                                isExpanded = false
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
}

/** Lets the expand animation finish before scrolling the opened card into view. */
internal const val ExpandScrollDelayMillis = 300L
