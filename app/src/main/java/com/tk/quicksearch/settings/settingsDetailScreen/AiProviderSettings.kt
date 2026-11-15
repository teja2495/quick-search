package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsSegment
import com.tk.quicksearch.settings.shared.SettingsSegmentContent
import com.tk.quicksearch.settings.shared.SettingsSegmentGroup
import com.tk.quicksearch.settings.shared.SettingsSegmentToggleRow
import com.tk.quicksearch.tools.aiSearch.LlmTextModel
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.settings.shared.ModelFeatureSettingsCard
import com.tk.quicksearch.shared.ui.components.CardTextField
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import org.json.JSONObject

@Composable
fun AiProviderSettingsSection(
        personalContext: String,
        aiSearchLlmProviderId: AiSearchLlmProviderId,
        activeLlmModel: String,
        activeLlmGroundingEnabled: Boolean,
        activeLlmThinkingEnabled: Boolean,
        activeLlmAvailableModels: List<LlmTextModel>,
        availableLlmModelsByProvider: Map<AiSearchLlmProviderId, List<LlmTextModel>>,
        apiKeyLast4ByProvider: Map<AiSearchLlmProviderId, String>,
        customAdvancedPayloadByProvider: Map<AiSearchLlmProviderId, Pair<Boolean, String>>,
        onSetPersonalContext: (String?) -> Unit,
        onSetActiveLlmModel: (String?) -> Unit,
        onSetLlmModel: (AiSearchLlmProviderId, String?) -> Unit,
        onSetCustomAdvancedPayload: (AiSearchLlmProviderId, String?, Boolean) -> Unit,
        onSetActiveLlmGroundingEnabled: (Boolean) -> Unit,
        onSetActiveLlmThinkingEnabled: (Boolean) -> Unit,
        onRefreshAvailableLlmModels: () -> Unit,
        onRequestScrollToBottom: (() -> Unit)? = null,
        showThinkingCheckbox: Boolean = true,
        modifier: Modifier = Modifier,
) {
        val context = LocalContext.current
        var personalContextInput by remember(personalContext) { mutableStateOf(personalContext) }
        var selectedModelInput by remember(activeLlmModel) { mutableStateOf(activeLlmModel) }
        var groundingEnabledInput by
                remember(activeLlmGroundingEnabled) { mutableStateOf(activeLlmGroundingEnabled) }
        var thinkingEnabledInput by
                remember(activeLlmThinkingEnabled) { mutableStateOf(activeLlmThinkingEnabled) }

        LaunchedEffect(Unit) { onRefreshAvailableLlmModels() }
        LaunchedEffect(personalContextInput) { onRequestScrollToBottom?.invoke() }

        Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
                val selectedModel = activeLlmAvailableModels.firstOrNull { it.id == selectedModelInput }
                val supportsInstructions = selectedModel?.supportsSystemInstructions == true

                ModelFeatureSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        selectedModelId = selectedModelInput,
                        selectedProviderId = aiSearchLlmProviderId,
                        availableModels = activeLlmAvailableModels,
                        availableModelsByProvider = availableLlmModelsByProvider,
                        configuredProviderIds = apiKeyLast4ByProvider.keys,
                        thinkingLabel =
                                stringResource(R.string.settings_direct_search_thinking_label),
                        webSearchLabel =
                                stringResource(R.string.settings_direct_search_grounding_label),
                        thinkingEnabled = thinkingEnabledInput,
                        groundingEnabled = groundingEnabledInput,
                        onModelSelected = { modelId ->
                                selectedModelInput = modelId
                                onSetActiveLlmModel(modelId)
                        },
                        onProviderModelSelected = { providerId, modelId ->
                                selectedModelInput = modelId
                                onSetLlmModel(providerId, modelId)
                        },
                        onThinkingChange = { checked ->
                                thinkingEnabledInput = checked
                                onSetActiveLlmThinkingEnabled(checked)
                        },
                        onGroundingChange = { checked ->
                                groundingEnabledInput = checked
                                onSetActiveLlmGroundingEnabled(checked)
                        },
                        showThinkingCheckbox = showThinkingCheckbox,
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
                        SettingsSegment(index = 0, count = 1) {
                                CardTextField(
                                        value = personalContextInput,
                                        onValueChange = {
                                                personalContextInput = it
                                                val trimmed = it.trim()
                                                onSetPersonalContext(
                                                        trimmed.takeIf { value -> value.isNotEmpty() }
                                                )
                                        },
                                        label = stringResource(R.string.settings_direct_search_personal_context),
                                        placeholder =
                                                stringResource(
                                                        R.string.settings_direct_search_personal_context_hint
                                                ),
                                        enabled = supportsInstructions,
                                        minLines = 3,
                                        maxLines = 8,
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .heightIn(min = 120.dp)
                                                        .padding(
                                                                horizontal = DesignTokens.SpacingSmall,
                                                                vertical = DesignTokens.SpacingSmall,
                                                        ),
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

        SettingsSegmentGroup {
                SettingsSegmentToggleRow(
                        title = stringResource(R.string.settings_custom_provider_advanced_payload_title),
                        description =
                                stringResource(R.string.settings_custom_provider_advanced_payload_desc),
                        icon = MaterialSymbols.Tune,
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
                        index = 0,
                        count = if (enabledInput) 2 else 1,
                )

                if (enabledInput) {
                        SettingsSegment(index = 1, count = 2) {
                                SettingsSegmentContent {
                                        OutlinedTextField(
                                                value = payloadInput,
                                                onValueChange = { next ->
                                                        payloadInput = next
                                                        saveIfValid(next, true)
                                                },
                                                modifier = Modifier.fillMaxWidth().height(160.dp),
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
                                                shape = MaterialTheme.shapes.large,
                                                colors = dialogTextFieldColors(),
                                                singleLine = false,
                                                minLines = 4,
                                                maxLines = 8,
                                        )
                                }
                        }
                }
        }
}

private fun isValidJsonObject(value: String): Boolean =
        runCatching { JSONObject(value) }.isSuccess
