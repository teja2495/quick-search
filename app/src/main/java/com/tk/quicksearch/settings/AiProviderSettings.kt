package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.tools.aiSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.aiSearch.GeminiTextModel
import com.tk.quicksearch.settings.shared.ModelFeatureSettingsCard
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun AiProviderSettingsSection(
        personalContext: String,
        geminiModel: String,
        geminiGroundingEnabled: Boolean,
        geminiThinkingEnabled: Boolean,
        availableGeminiModels: List<GeminiTextModel>,
        onSetPersonalContext: (String?) -> Unit,
        onSetGeminiModel: (String?) -> Unit,
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
        var personalContextFocused by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { onRefreshAvailableGeminiModels() }
        LaunchedEffect(personalContextInput, personalContextFocused) {
                if (personalContextFocused) {
                        onRequestScrollToBottom?.invoke()
                }
        }

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
                        availableModels = availableGeminiModels,
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
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(160.dp)
                                                .onFocusChanged { focusState ->
                                                        personalContextFocused = focusState.isFocused
                                                },
                                label = {
                                        Text(
                                                text =
                                                        stringResource(
                                                                R.string
                                                                        .settings_direct_search_personal_context
                                                        )
                                        )
                                },
                                supportingText =
                                        if (personalContextInput.isBlank()) {
                                                {
                                                        Text(
                                                                text =
                                                                        stringResource(
                                                                                R.string
                                                                                        .settings_direct_search_personal_context_hint
                                                                        ),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant,
                                                        )
                                                }
                                        } else {
                                                null
                                        },
                                colors = dialogTextFieldColors(),
                                singleLine = false,
                                minLines = 4,
                                maxLines = 8,
                        )
                }
        }
}
