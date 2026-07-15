package com.tk.quicksearch.settings.customTools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.settings.shared.ModelFeatureSettingsCard
import com.tk.quicksearch.settings.settingsDetailScreen.AdvancedPayloadSettingsSection
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.aiSearch.GeminiTextModel

@Composable
fun CustomToolEditorScreen(
    existingTool: CustomTool?,
    existingAlias: String,
    selectedProviderId: AiSearchLlmProviderId,
    availableModels: List<GeminiTextModel>,
    availableModelsByProvider: Map<AiSearchLlmProviderId, List<GeminiTextModel>>,
    configuredProviderIds: Set<AiSearchLlmProviderId>,
    onRefreshAvailableGeminiModels: () -> Unit,
    onProviderModelSelected: (AiSearchLlmProviderId, String) -> Unit,
    onSave: (name: String, prompt: String, providerId: AiSearchLlmProviderId, modelId: String, groundingEnabled: Boolean, aliasCode: String, thinkingEnabled: Boolean, advancedPayload: String?, advancedPayloadEnabled: Boolean) -> Unit,
    showNameInput: Boolean = true,
    showPromptInput: Boolean = true,
    showAliasInput: Boolean = true,
    shouldAutoFocusTitle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var nameInput by remember(existingTool?.id) {
        mutableStateOf(existingTool?.name.orEmpty())
    }
    var promptInput by remember(existingTool?.id) {
        mutableStateOf(existingTool?.prompt.orEmpty())
    }
    var selectedModelId by remember(existingTool?.id) {
        mutableStateOf(existingTool?.modelId ?: GeminiModelCatalog.DEFAULT_MODEL_ID)
    }
    var selectedProviderInput by remember(existingTool?.id, selectedProviderId) {
        mutableStateOf(existingTool?.providerId ?: selectedProviderId)
    }
    var aliasInput by remember(existingTool?.id) {
        mutableStateOf(existingAlias)
    }
    var groundingEnabled by remember(existingTool?.id) {
        mutableStateOf(existingTool?.groundingEnabled ?: false)
    }
    var thinkingEnabled by remember(existingTool?.id) {
        mutableStateOf(existingTool?.thinkingEnabled ?: false)
    }
    var advancedPayloadInput by remember(existingTool?.id) {
        mutableStateOf(existingTool?.advancedPayload.orEmpty())
    }
    var advancedPayloadEnabled by remember(existingTool?.id) {
        mutableStateOf(existingTool?.advancedPayloadEnabled == true)
    }

    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { onRefreshAvailableGeminiModels() }

    LaunchedEffect(existingTool?.id, shouldAutoFocusTitle) {
        if (shouldAutoFocusTitle && nameInput.trim().isEmpty()) {
            focusRequester.requestFocus()
        }
    }

    val selectedProviderModels = remember(selectedProviderInput, availableModelsByProvider, availableModels) {
        availableModelsByProvider[selectedProviderInput].orEmpty().ifEmpty { availableModels }
    }

    LaunchedEffect(selectedModelId, selectedProviderInput, availableModelsByProvider) {
        val selectedProviderHasModel =
            availableModelsByProvider[selectedProviderInput]?.any { it.id == selectedModelId } == true
        if (selectedProviderInput.isCustom || selectedProviderHasModel) return@LaunchedEffect

        val customProviderForSelectedModel =
            availableModelsByProvider
                .filterKeys { it.isCustom }
                .filterValues { models -> models.any { it.id == selectedModelId } }
                .keys
                .singleOrNull()
        if (customProviderForSelectedModel != null) {
            selectedProviderInput = customProviderForSelectedModel
        }
    }

    LaunchedEffect(existingTool?.id, selectedProviderModels, selectedModelId) {
        if (existingTool != null) return@LaunchedEffect
        val firstAvailableModelId = selectedProviderModels.firstOrNull()?.id ?: return@LaunchedEffect
        val hasSelectedModel = selectedProviderModels.any { it.id == selectedModelId }
        if (!hasSelectedModel) {
            selectedModelId = firstAvailableModelId
        }
    }

    val showThinkingToggle =
        selectedProviderInput != AiSearchLlmProviderId.OPENAI &&
            !selectedProviderInput.isCustom
    val showGroundingCheckbox =
        selectedProviderInput != AiSearchLlmProviderId.OPENAI &&
            !selectedProviderInput.isCustom &&
            selectedProviderInput != AiSearchLlmProviderId.GROQ
    val supportsAdvancedPayload = selectedProviderInput.isCustom

    val isNameValid = !showNameInput || nameInput.trim().isNotBlank()
    val isPromptValid = !showPromptInput || promptInput.trim().isNotBlank()
    val isAliasValid = !showAliasInput || aliasInput.trim().isNotBlank()
    val canSave = isNameValid && isPromptValid && isAliasValid

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(
                    start = DesignTokens.ContentHorizontalPadding,
                    end = DesignTokens.ContentHorizontalPadding,
                    bottom = DesignTokens.SpacingLarge,
                ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
            if (showNameInput) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text(text = stringResource(R.string.settings_custom_tool_name_label)) },
                    singleLine = true,
                    maxLines = 1,
                    colors = dialogTextFieldColors(),
                )
            }

            if (showPromptInput) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    Text(
                        text = stringResource(R.string.settings_custom_tool_prompt_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.settings_custom_tool_prompt_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        colors = dialogTextFieldColors(),
                    )
                }
            }

            if (showAliasInput) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    Text(
                        text = stringResource(R.string.settings_custom_tool_alias_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedTextField(
                        value = aliasInput,
                        onValueChange = { aliasInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.settings_custom_tool_alias_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        singleLine = true,
                        maxLines = 1,
                        colors = dialogTextFieldColors(),
                    )
                }
            }

            ModelFeatureSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                selectedModelId = selectedModelId,
                selectedProviderId = selectedProviderInput,
                availableModels = selectedProviderModels,
                availableModelsByProvider = availableModelsByProvider,
                configuredProviderIds = configuredProviderIds,
                modelLabel = stringResource(R.string.settings_direct_search_model_label),
                thinkingLabel = stringResource(R.string.settings_direct_search_thinking_label),
                webSearchLabel = stringResource(R.string.settings_direct_search_grounding_label),
                thinkingEnabled = thinkingEnabled,
                groundingEnabled = groundingEnabled,
                onModelSelected = { selectedModelId = it },
                onProviderModelSelected = { providerId, modelId ->
                    selectedProviderInput = providerId
                    selectedModelId = modelId
                    onProviderModelSelected(providerId, modelId)
                },
                onThinkingChange = { thinkingEnabled = it },
                onGroundingChange = { groundingEnabled = it },
                showThinkingCheckbox = showThinkingToggle,
                showGroundingCheckbox = showGroundingCheckbox,
            )

            if (supportsAdvancedPayload) {
                AdvancedPayloadSettingsSection(
                    payload = advancedPayloadInput,
                    enabled = advancedPayloadEnabled,
                    onSave = { payload, enabled ->
                        advancedPayloadInput = payload.orEmpty()
                        advancedPayloadEnabled = enabled
                    },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.ContentHorizontalPadding,
                    vertical = DesignTokens.SpacingMedium,
                ),
        ) {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(
                            nameInput.trim(),
                            promptInput.trim(),
                            selectedProviderInput,
                            selectedModelId,
                            groundingEnabled,
                            aliasInput.trim(),
                            if (showThinkingToggle) thinkingEnabled else false,
                            advancedPayloadInput.trim().takeIf {
                                supportsAdvancedPayload && advancedPayloadEnabled && it.isNotEmpty()
                            },
                            supportsAdvancedPayload && advancedPayloadEnabled,
                        )
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        }
    }

}
