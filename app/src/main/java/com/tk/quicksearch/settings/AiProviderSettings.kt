package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.tools.aiSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.aiSearch.GeminiTextModel
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.settings.shared.ModelFeatureSettingsCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import org.json.JSONObject

@Composable
fun AiProviderSettingsSection(
        personalContext: String,
        aiSearchLlmProviderId: AiSearchLlmProviderId,
        geminiModel: String,
        geminiGroundingEnabled: Boolean,
        geminiThinkingEnabled: Boolean,
        availableGeminiModels: List<GeminiTextModel>,
        availableLlmModelsByProvider: Map<AiSearchLlmProviderId, List<GeminiTextModel>>,
        apiKeyLast4ByProvider: Map<AiSearchLlmProviderId, String>,
        customAdvancedPayloadByProvider: Map<AiSearchLlmProviderId, Pair<Boolean, String>>,
        onSetPersonalContext: (String?) -> Unit,
        onSetGeminiModel: (String?) -> Unit,
        onSetLlmModel: (AiSearchLlmProviderId, String?) -> Unit,
        onSetCustomAdvancedPayload: (AiSearchLlmProviderId, String?, Boolean) -> Unit,
        onSetGeminiGroundingEnabled: (Boolean) -> Unit,
        onSetGeminiThinkingEnabled: (Boolean) -> Unit,
        onRefreshAvailableGeminiModels: () -> Unit,
        onRequestScrollToBottom: (() -> Unit)? = null,
        showGroundingCheckbox: Boolean = true,
        showThinkingCheckbox: Boolean = true,
        modifier: Modifier = Modifier,
) {
        val context = LocalContext.current
        var personalContextInput by remember(personalContext) { mutableStateOf(personalContext) }
        var selectedModelInput by remember(geminiModel) { mutableStateOf(geminiModel) }
        var groundingEnabledInput by
                remember(geminiGroundingEnabled) { mutableStateOf(geminiGroundingEnabled) }
        var thinkingEnabledInput by
                remember(geminiThinkingEnabled) { mutableStateOf(geminiThinkingEnabled) }

        LaunchedEffect(Unit) { onRefreshAvailableGeminiModels() }
        LaunchedEffect(personalContextInput) { onRequestScrollToBottom?.invoke() }

        val modelOptions =
                remember(availableGeminiModels, selectedModelInput) {
                        val allKnownModels =
                                availableGeminiModels + GeminiModelCatalog.FALLBACK_TEXT_MODELS
                        val currentModel =
                                allKnownModels.find { it.id == selectedModelInput }
                                        ?: GeminiTextModel(selectedModelInput, selectedModelInput)

                        (availableGeminiModels + currentModel).distinctBy { it.id }
                }

        Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
                val selectedModel = modelOptions.firstOrNull { it.id == selectedModelInput }
                val supportsInstructions = selectedModel?.supportsSystemInstructions != false

                ModelFeatureSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        selectedModelId = selectedModelInput,
                        selectedProviderId = aiSearchLlmProviderId,
                        availableModels = availableGeminiModels,
                        availableModelsByProvider = availableLlmModelsByProvider,
                        configuredProviderIds = apiKeyLast4ByProvider.keys,
                        modelLabel =
                                stringResource(R.string.settings_direct_search_model_label),
                        thinkingLabel =
                                stringResource(R.string.settings_direct_search_thinking_label),
                        webSearchLabel =
                                stringResource(R.string.settings_direct_search_grounding_label),
                        thinkingEnabled = thinkingEnabledInput,
                        groundingEnabled = groundingEnabledInput,
                        onModelSelected = { modelId ->
                                selectedModelInput = modelId
                                onSetGeminiModel(modelId)
                        },
                        onProviderModelSelected = { providerId, modelId ->
                                selectedModelInput = modelId
                                onSetLlmModel(providerId, modelId)
                        },
                        onThinkingChange = { checked ->
                                thinkingEnabledInput = checked
                                onSetGeminiThinkingEnabled(checked)
                        },
                        onGroundingChange = { checked ->
                                groundingEnabledInput = checked
                                onSetGeminiGroundingEnabled(checked)
                        },
                        showThinkingCheckbox = showThinkingCheckbox,
                        showGroundingCheckbox = showGroundingCheckbox,
                )

                Box(
                        modifier =
                                Modifier.fillMaxWidth().then(
                                        if (!supportsInstructions) {
                                                Modifier.clickable {
                                                        Toast.makeText(
                                                                        context,
                                                                        context.getString(
                                                                                R.string
                                                                                        .settings_direct_search_personal_context_unsupported
                                                                        ),
                                                                        Toast.LENGTH_SHORT,
                                                                )
                                                                .show()
                                                }
                                        } else {
                                                Modifier
                                        }
                                ),
                ) {
                        Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        ) {
                                Text(
                                        text =
                                                stringResource(
                                                        R.string
                                                                .settings_direct_search_personal_context
                                                ),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                )

                                OutlinedTextField(
                                        value = personalContextInput,
                                        onValueChange = {
                                                personalContextInput = it
                                                val trimmed = it.trim()
                                                onSetPersonalContext(
                                                        trimmed.takeIf { value -> value.isNotEmpty() }
                                                )
                                        },
                                        enabled = supportsInstructions,
                                        modifier = Modifier.fillMaxWidth().height(160.dp),
                                        placeholder = {
                                                Text(
                                                        text =
                                                                stringResource(
                                                                        R.string
                                                                                .settings_direct_search_personal_context_hint
                                                                ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                )
                                        },
                                        shape = MaterialTheme.shapes.extraLarge,
                                        colors =
                                                TextFieldDefaults.colors(
                                                        focusedContainerColor = AppColors.getSettingsCardContainerColor(),
                                                        unfocusedContainerColor = AppColors.getSettingsCardContainerColor(),
                                                        disabledContainerColor = AppColors.getSettingsCardContainerColor(),
                                                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                                ),
                                        singleLine = false,
                                        minLines = 4,
                                        maxLines = 8,
                                )
                        }
                }

                if (aiSearchLlmProviderId.isCustom) {
                        val savedPayload = customAdvancedPayloadByProvider[aiSearchLlmProviderId]
                        AdvancedPayloadSettingsSection(
                                payload = savedPayload?.second.orEmpty(),
                                enabled = savedPayload?.first == true,
                                onSave = { payload, enabled ->
                                        onSetCustomAdvancedPayload(
                                                aiSearchLlmProviderId,
                                                payload,
                                                enabled,
                                        )
                                },
                        )
                }
        }
}

@Composable
fun AdvancedPayloadSettingsSection(
        payload: String,
        enabled: Boolean,
        onSave: (String?, Boolean) -> Unit,
) {
        var payloadInput by remember(payload) { mutableStateOf(payload) }
        var enabledInput by remember(enabled) { mutableStateOf(enabled) }
        var hasJsonError by remember(payload) {
                mutableStateOf(payload.isNotBlank() && !isValidJsonObject(payload))
        }

        fun saveIfValid(
                nextPayload: String,
                nextEnabled: Boolean,
        ): Boolean {
                val trimmed = nextPayload.trim()
                if (trimmed.isBlank()) {
                        hasJsonError = false
                        onSave(null, false)
                        return true
                }
                if (!isValidJsonObject(trimmed)) {
                        hasJsonError = true
                        return false
                }
                hasJsonError = false
                enabledInput = nextEnabled
                onSave(trimmed, nextEnabled)
                return true
        }

        SettingsCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                        SettingsToggleRow(
                                title = stringResource(R.string.settings_custom_provider_advanced_payload_title),
                                subtitle =
                                        stringResource(
                                                R.string.settings_custom_provider_advanced_payload_desc
                                        ),
                                checked = enabledInput,
                                onCheckedChange = { checked ->
                                        enabledInput = checked
                                        if (checked) {
                                                val trimmed = payloadInput.trim()
                                                if (trimmed.isNotBlank()) {
                                                        saveIfValid(trimmed, true)
                                                }
                                        } else {
                                                saveIfValid(payloadInput, false)
                                        }
                                },
                                isFirstItem = true,
                                isLastItem = !enabledInput,
                                showDivider = false,
                        )

                        if (enabledInput) {
                                OutlinedTextField(
                                        value = payloadInput,
                                        onValueChange = { next ->
                                                payloadInput = next
                                                saveIfValid(next, true)
                                        },
                                        modifier =
                                                Modifier
                                                        .fillMaxWidth()
                                                        .height(160.dp)
                                                        .padding(
                                                                start = DesignTokens.CardHorizontalPadding,
                                                                end = DesignTokens.CardHorizontalPadding,
                                                                bottom = DesignTokens.CardVerticalPadding,
                                                        ),
                                        placeholder = {
                                                Text(
                                                        text =
                                                                stringResource(
                                                                        R.string
                                                                                .settings_custom_provider_advanced_payload_hint
                                                                ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                )
                                        },
                                        supportingText =
                                                if (hasJsonError) {
                                                        {
                                                                Text(
                                                                        text =
                                                                                stringResource(
                                                                                        R.string
                                                                                                .settings_custom_provider_advanced_payload_error
                                                                                )
                                                                )
                                                        }
                                                } else {
                                                        null
                                                },
                                        isError = hasJsonError,
                                        shape = MaterialTheme.shapes.extraLarge,
                                        colors =
                                                TextFieldDefaults.colors(
                                                        focusedContainerColor = AppColors.getSettingsCardContainerColor(),
                                                        unfocusedContainerColor = AppColors.getSettingsCardContainerColor(),
                                                        disabledContainerColor = AppColors.getSettingsCardContainerColor(),
                                                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                                        errorIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                                ),
                                        singleLine = false,
                                        minLines = 4,
                                        maxLines = 8,
                                )
                        }
                }
        }
}

private fun isValidJsonObject(value: String): Boolean =
        runCatching { JSONObject(value) }.isSuccess
