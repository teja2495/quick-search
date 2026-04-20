package com.tk.quicksearch.settings.customTools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.aiSearch.GeminiTextModel

@Composable
fun CustomToolEditorScreen(
    existingTool: CustomTool?,
    existingAlias: String,
    availableModels: List<GeminiTextModel>,
    onSave: (name: String, prompt: String, modelId: String, groundingEnabled: Boolean, aliasCode: String, thinkingEnabled: Boolean) -> Unit,
    showThinkingToggle: Boolean = true,
    showGroundingCheckbox: Boolean = true,
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
    var aliasInput by remember(existingTool?.id) {
        mutableStateOf(existingAlias)
    }
    var groundingEnabled by remember(existingTool?.id) {
        mutableStateOf(existingTool?.groundingEnabled ?: false)
    }
    var thinkingEnabled by remember(existingTool?.id) {
        mutableStateOf(existingTool?.thinkingEnabled ?: false)
    }

    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(existingTool?.id, shouldAutoFocusTitle) {
        if (shouldAutoFocusTitle && nameInput.trim().isEmpty()) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(availableModels, selectedModelId) {
        val firstAvailableModelId = availableModels.firstOrNull()?.id ?: return@LaunchedEffect
        val hasSelectedModel = availableModels.any { it.id == selectedModelId }
        if (!hasSelectedModel) {
            selectedModelId = firstAvailableModelId
        }
    }

    val isNameValid = nameInput.trim().isNotBlank()
    val isPromptValid = promptInput.trim().isNotBlank()
    val isAliasValid = aliasInput.trim().isNotBlank()
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

            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                label = { Text(text = stringResource(R.string.settings_custom_tool_prompt_label)) },
                minLines = 4,
                maxLines = 8,
                colors = dialogTextFieldColors(),
                supportingText = {
                    Text(
                        text = stringResource(R.string.settings_custom_tool_prompt_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )

            OutlinedTextField(
                value = aliasInput,
                onValueChange = { aliasInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.settings_custom_tool_alias_label)) },
                singleLine = true,
                maxLines = 1,
                colors = dialogTextFieldColors(),
                supportingText = {
                    Text(
                        text = stringResource(R.string.settings_custom_tool_alias_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )

            ModelFeatureSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                selectedModelId = selectedModelId,
                availableModels = availableModels,
                modelLabel = stringResource(R.string.settings_direct_search_model_label),
                thinkingLabel = stringResource(R.string.settings_direct_search_thinking_label),
                webSearchLabel = stringResource(R.string.settings_direct_search_grounding_label),
                thinkingEnabled = thinkingEnabled,
                groundingEnabled = groundingEnabled,
                onModelSelected = { selectedModelId = it },
                onThinkingChange = { thinkingEnabled = it },
                onGroundingChange = { groundingEnabled = it },
                showThinkingCheckbox = showThinkingToggle,
                showGroundingCheckbox = showGroundingCheckbox,
            )
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
                            selectedModelId,
                            groundingEnabled,
                            aliasInput.trim(),
                            if (showThinkingToggle) thinkingEnabled else false,
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
