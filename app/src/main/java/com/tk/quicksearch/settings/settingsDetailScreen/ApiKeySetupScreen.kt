package com.tk.quicksearch.settings.settingsDetailScreen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.res.painterResource
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.tools.aiSearch.TavilyWebSearchMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CardContentVerticalPadding = DesignTokens.CardVerticalPadding + DesignTokens.SpacingSmall

@Composable
fun ApiKeySetupScreen(
    apiKeyLast4ByProvider: Map<AiSearchLlmProviderId, String>,
    customProviderBaseUrlByProvider: Map<AiSearchLlmProviderId, String>,
    isSavingApiKey: Boolean,
    onSetApiKey: (AiSearchLlmProviderId, String?) -> Unit,
    onAddCustomProvider: (baseUrl: String, apiKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val freeApiKeyGuideUrl = stringResource(R.string.settings_gemini_guide_url)
    val openFreeApiKeyGuide: () -> Unit = {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(freeApiKeyGuideUrl)),
            )
        }
        Unit
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        Text(
            text = stringResource(R.string.settings_api_key_setup_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (apiKeyLast4ByProvider.isEmpty()) {
            TipBanner(
                annotatedText =
                    buildAnnotatedString {
                        pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                        append(stringResource(R.string.settings_direct_search_how_to))
                        pop()
                        append("\n")
                        append(stringResource(R.string.settings_free_api_key_tip_description))
                    },
                onContentClick = openFreeApiKeyGuide,
                actionIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                actionContentDescription = stringResource(R.string.settings_open_free_api_key_guide),
                onActionClick = openFreeApiKeyGuide,
                showDismissButton = false,
            )
        }

        AiSearchLlmProviderId.entries.forEach { providerId ->
            ProviderApiKeyCard(
                providerId = providerId,
                apiKeyLast4 = apiKeyLast4ByProvider[providerId],
                configuredBaseUrl = customProviderBaseUrlByProvider[providerId],
                isSavingApiKey = isSavingApiKey,
                onSetApiKey = onSetApiKey,
            )
        }

        // Includes custom providers restored from a backup without their API key.
        customProviderBaseUrlByProvider.keys.forEach { providerId ->
            ProviderApiKeyCard(
                providerId = providerId,
                apiKeyLast4 = apiKeyLast4ByProvider[providerId],
                configuredBaseUrl = customProviderBaseUrlByProvider[providerId],
                isSavingApiKey = isSavingApiKey,
                onSetApiKey = onSetApiKey,
            )
        }

        AddCustomProviderCard(
            isSaving = isSavingApiKey,
            onSave = onAddCustomProvider,
        )

        TavilyWebSearchSection()
    }
}

@Composable
private fun TavilyWebSearchSection() {
    val context = LocalContext.current
    val preferences = remember(context) { UserAppPreferences(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    var apiKeyLast4 by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(TavilyWebSearchMode.DEFAULT) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(preferences) {
        val (key, savedMode) =
            withContext(Dispatchers.IO) {
                preferences.getTavilyApiKey() to preferences.getTavilyWebSearchMode()
            }
        apiKeyLast4 = key?.trim()?.takeLast(4)
        mode = savedMode
    }

    fun saveApiKey(key: String?) {
        coroutineScope.launch {
            isSaving = true
            withContext(Dispatchers.IO) { preferences.setTavilyApiKey(key) }
            apiKeyLast4 = key?.trim()?.takeIf { it.isNotEmpty() }?.takeLast(4)
            isSaving = false
        }
    }

    TavilyApiKeyCard(
        apiKeyLast4 = apiKeyLast4,
        isSaving = isSaving,
        mode = mode,
        onSaveApiKey = ::saveApiKey,
        onModeChange = { selected ->
            mode = selected
            coroutineScope.launch(Dispatchers.IO) { preferences.setTavilyWebSearchMode(selected) }
        },
    )
}

@Composable
private fun TavilyApiKeyCard(
    apiKeyLast4: String?,
    isSaving: Boolean,
    mode: TavilyWebSearchMode,
    onSaveApiKey: (String?) -> Unit,
    onModeChange: (TavilyWebSearchMode) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var apiKeyInput by remember { mutableStateOf("") }
    val trimmedKey = apiKeyInput.trim()
    val hasSavedKey = apiKeyLast4 != null

    fun pasteFromClipboard() {
        apiKeyInput =
            clipboardManager
                .getText()
                ?.text
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                .orEmpty()
    }

    SettingsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        horizontal = DesignTokens.CardHorizontalPadding,
                        vertical = DesignTokens.CardVerticalPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                ) {
                    Text(
                        text = stringResource(R.string.settings_tavily_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text =
                            if (hasSavedKey) {
                                stringResource(R.string.settings_api_key_saved_last4, apiKeyLast4.orEmpty())
                            } else {
                                stringResource(R.string.settings_tavily_desc)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (hasSavedKey) {
                    IconButton(
                        enabled = !isSaving,
                        onClick = {
                            apiKeyInput = ""
                            onSaveApiKey(null)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.settings_gemini_api_key_reset),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (hasSavedKey) {
                Column(modifier = Modifier.selectableGroup()) {
                    TavilyWebSearchMode.entries.forEach { option ->
                        TavilyModeOption(
                            label = stringResource(option.labelResId()),
                            selected = option == mode,
                            onClick = { onModeChange(option) },
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {},
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .pointerInput(apiKeyInput) {
                                detectTapGestures {
                                    if (apiKeyInput.isEmpty()) {
                                        pasteFromClipboard()
                                    } else {
                                        apiKeyInput = ""
                                    }
                                }
                            },
                    leadingIcon =
                        if (apiKeyInput.isEmpty()) {
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    TextButton(
                                        enabled = !isSaving,
                                        onClick = { pasteFromClipboard() },
                                        modifier = Modifier.wrapContentWidth(),
                                    ) {
                                        Text(text = stringResource(R.string.settings_gemini_api_key_paste_hint))
                                    }
                                }
                            }
                        } else {
                            null
                        },
                    shape = DesignTokens.ShapeXXLarge,
                    colors =
                        dialogTextFieldColors(
                            unfocusedIndicatorColor =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        ),
                    singleLine = true,
                    readOnly = true,
                )

                if (trimmedKey.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            enabled = !isSaving,
                            onClick = { apiKeyInput = "" },
                        ) {
                            Text(text = stringResource(R.string.common_action_clear))
                        }
                        Button(
                            enabled = !isSaving,
                            onClick = {
                                onSaveApiKey(trimmedKey)
                                apiKeyInput = ""
                            },
                        ) {
                            Text(
                                text =
                                    if (isSaving) {
                                        stringResource(R.string.settings_gemini_api_key_saving)
                                    } else {
                                        stringResource(R.string.dialog_save)
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TavilyModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(vertical = DesignTokens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = DesignTokens.SpacingMedium),
        )
    }
}

private fun TavilyWebSearchMode.labelResId(): Int =
    when (this) {
        TavilyWebSearchMode.ALWAYS -> R.string.settings_tavily_mode_always
        TavilyWebSearchMode.WHEN_MODEL_UNSUPPORTED -> R.string.settings_tavily_mode_when_unsupported
    }

@Composable
private fun AddCustomProviderCard(
    isSaving: Boolean,
    onSave: (baseUrl: String, apiKey: String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var baseUrlInput by rememberSaveable { mutableStateOf("") }
    var apiKeyInput by rememberSaveable { mutableStateOf("") }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "customProviderChevronRotation",
    )
    val canSave =
        baseUrlInput.trim().isNotBlank() &&
            apiKeyInput.trim().isNotBlank()

    fun pasteKeyFromClipboard() {
        apiKeyInput =
            clipboardManager
                .getText()
                ?.text
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                .orEmpty()
    }

    SettingsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        horizontal = DesignTokens.CardHorizontalPadding,
                        vertical = CardContentVerticalPadding,
                    ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_add_custom_provider_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_add_custom_provider_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription =
                            stringResource(
                                if (isExpanded) R.string.desc_collapse else R.string.desc_expand,
                            ),
                        modifier = Modifier.rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                ) {
                    Text(
                        text = stringResource(R.string.settings_custom_provider_base_url_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(text = stringResource(R.string.settings_custom_provider_base_url_input_label))
                        },
                        placeholder = {
                            Text(text = stringResource(R.string.settings_custom_provider_base_url_input_label))
                        },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {},
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .pointerInput(apiKeyInput) {
                                    detectTapGestures {
                                        if (apiKeyInput.isEmpty()) {
                                            pasteKeyFromClipboard()
                                        } else {
                                            apiKeyInput = ""
                                        }
                                    }
                                },
                        leadingIcon =
                            if (apiKeyInput.isEmpty()) {
                                {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        TextButton(
                                            enabled = !isSaving,
                                            onClick = { pasteKeyFromClipboard() },
                                            modifier = Modifier.wrapContentWidth(),
                                        ) {
                                            Text(text = stringResource(R.string.settings_gemini_api_key_paste_hint))
                                        }
                                    }
                                }
                            } else {
                                null
                            },
                        singleLine = true,
                        readOnly = true,
                        colors =
                            dialogTextFieldColors(
                                unfocusedIndicatorColor =
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            enabled = !isSaving && (baseUrlInput.isNotBlank() || apiKeyInput.isNotBlank()),
                            onClick = {
                                baseUrlInput = ""
                                apiKeyInput = ""
                            },
                        ) {
                            Text(text = stringResource(R.string.common_action_clear))
                        }
                        Button(
                            enabled = !isSaving && canSave,
                            onClick = {
                                onSave(
                                    baseUrlInput.trim(),
                                    apiKeyInput.trim(),
                                )
                            },
                        ) {
                            Text(
                                text =
                                    if (isSaving) {
                                        stringResource(R.string.settings_gemini_api_key_saving)
                                    } else {
                                        stringResource(R.string.dialog_save)
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
