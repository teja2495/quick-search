package com.tk.quicksearch.settings.settingsDetailScreen

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
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.res.painterResource
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId

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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        Text(
            text = stringResource(R.string.settings_api_key_setup_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AiSearchLlmProviderId.entries.forEach { providerId ->
            ProviderApiKeyCard(
                providerId = providerId,
                apiKeyLast4 = apiKeyLast4ByProvider[providerId],
                configuredBaseUrl = customProviderBaseUrlByProvider[providerId],
                isSavingApiKey = isSavingApiKey,
                onSetApiKey = onSetApiKey,
            )
        }

        apiKeyLast4ByProvider.keys.filter { it.isCustom }.forEach { providerId ->
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
    }
}

@Composable
private fun AddCustomProviderCard(
    isSaving: Boolean,
    onSave: (baseUrl: String, apiKey: String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var baseUrlInput by rememberSaveable { mutableStateOf("") }
    var apiKeyInput by rememberSaveable { mutableStateOf("") }
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
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
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
                colors = dialogTextFieldColors(),
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
                    Text(text = stringResource(R.string.settings_gemini_api_key_clear))
                }
                Button(
                    enabled = !isSaving && canSave,
                    onClick = {
                        onSave(
                            baseUrlInput.trim(),
                            apiKeyInput.trim(),
                        )
                        baseUrlInput = ""
                        apiKeyInput = ""
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_save))
                }
            }
        }
    }
}

@Composable
private fun ProviderApiKeyCard(
    providerId: AiSearchLlmProviderId,
    apiKeyLast4: String?,
    configuredBaseUrl: String?,
    isSavingApiKey: Boolean,
    onSetApiKey: (AiSearchLlmProviderId, String?) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val inputByProvider = remember { mutableStateMapOf<AiSearchLlmProviderId, String>() }
    var apiKeyInput by remember(providerId) {
        mutableStateOf(inputByProvider[providerId].orEmpty())
    }
    val trimmedKey = apiKeyInput.trim()
    val hasSavedKey = apiKeyLast4 != null
    val contentVerticalPadding =
        if (providerId.isCustom) {
            DesignTokens.CardVerticalPadding + DesignTokens.SpacingSmall
        } else {
            DesignTokens.CardVerticalPadding
        }
    fun pasteFromClipboard() {
        val pasted =
            clipboardManager
                .getText()
                ?.text
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                .orEmpty()
        apiKeyInput = pasted
        if (pasted.isNotEmpty()) {
            inputByProvider[providerId] = pasted
        } else {
            inputByProvider.remove(providerId)
        }
    }

    SettingsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        horizontal = DesignTokens.CardHorizontalPadding,
                        vertical = contentVerticalPadding,
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
                    horizontalAlignment =
                        if (hasSavedKey) Alignment.Start else Alignment.CenterHorizontally,
                ) {
                    ProviderLogo(
                        providerId = providerId,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    )
                    if (hasSavedKey) {
                        configuredBaseUrl?.takeIf { providerId.isCustom }?.let { baseUrl ->
                            Text(
                                text = baseUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = stringResource(R.string.settings_api_key_saved_last4, apiKeyLast4.orEmpty()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (hasSavedKey) {
                    IconButton(
                        enabled = !isSavingApiKey,
                        onClick = {
                            apiKeyInput = ""
                            inputByProvider.remove(providerId)
                            onSetApiKey(providerId, null)
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

            if (!hasSavedKey) {
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
                                        inputByProvider.remove(providerId)
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
                                        enabled = !isSavingApiKey,
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
                    colors = dialogTextFieldColors(),
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
                            enabled = !isSavingApiKey,
                            onClick = {
                                apiKeyInput = ""
                                inputByProvider.remove(providerId)
                            },
                        ) {
                            Text(text = stringResource(R.string.settings_gemini_api_key_clear))
                        }
                        Button(
                            enabled = !isSavingApiKey,
                            onClick = {
                                onSetApiKey(providerId, trimmedKey)
                                inputByProvider.remove(providerId)
                                apiKeyInput = ""
                            },
                        ) {
                            Text(
                                text =
                                    if (isSavingApiKey) {
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
private fun ProviderLogo(
    providerId: AiSearchLlmProviderId,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    when (providerId) {
        AiSearchLlmProviderId.OPENAI -> {
            Image(
                painter = painterResource(R.drawable.openai_wordmark),
                contentDescription = stringResource(R.string.settings_ai_provider_openai),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.height(20.dp).aspectRatio(1564.3f / 428.4f),
            )
        }
        AiSearchLlmProviderId.ANTHROPIC -> {
            Box(modifier = Modifier.height(20.dp).aspectRatio(689.97997f / 148.17999f)) {
                Image(
                    painter = painterResource(R.drawable.claude_wordmark_mark),
                    contentDescription = stringResource(R.string.search_engine_claude),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Image(
                    painter = painterResource(R.drawable.claude_wordmark_type),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        AiSearchLlmProviderId.GROQ -> {
            Image(
                painter = painterResource(R.drawable.groq_wordmark),
                contentDescription = stringResource(R.string.settings_ai_provider_groq),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.height(20.dp).aspectRatio(152f / 55.5f),
            )
        }
        AiSearchLlmProviderId.GEMINI -> {
            Box(modifier = Modifier.height(16.dp).aspectRatio(288f / 65f)) {
                Image(
                    painter = painterResource(R.drawable.gemini_wordmark_mark),
                    contentDescription = stringResource(R.string.search_engine_gemini),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Image(
                    painter = painterResource(R.drawable.gemini_wordmark_type),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        else -> {
            Text(
                text = stringResource(R.string.settings_ai_provider_custom_label),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
        }
    }
}
