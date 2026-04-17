package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.aiSearch.ModelPickerDialog
import com.tk.quicksearch.tools.aiSearch.GeminiTextModel

@Composable
fun ModelFeatureSettingsCard(
    selectedModelId: String,
    availableModels: List<GeminiTextModel>,
    modelLabel: String,
    thinkingLabel: String,
    webSearchLabel: String,
    thinkingEnabled: Boolean,
    groundingEnabled: Boolean,
    onModelSelected: (String) -> Unit,
    onThinkingChange: (Boolean) -> Unit,
    onGroundingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showThinkingCheckbox: Boolean = true,
    showGroundingCheckbox: Boolean = true,
    includeSelectedModelIfMissing: Boolean = true,
) {
    var showModelDialog by remember { mutableStateOf(false) }

    val modelOptions =
        remember(availableModels, selectedModelId, includeSelectedModelIfMissing) {
            val allKnownModels = availableModels + GeminiModelCatalog.FALLBACK_TEXT_MODELS
            val currentModel = allKnownModels.find { it.id == selectedModelId }

            val options =
                if (includeSelectedModelIfMissing && currentModel == null) {
                    availableModels + GeminiTextModel(selectedModelId, selectedModelId)
                } else {
                    availableModels
                }

            options.distinctBy { it.id }.sortedBy { it.displayName.lowercase() }
        }

    val selectedModel = modelOptions.firstOrNull { it.id == selectedModelId }
    val selectedModelLabel = selectedModel?.displayName ?: selectedModelId
    val supportsGrounding = selectedModel?.supportsGrounding != false

    LaunchedEffect(supportsGrounding, groundingEnabled) {
        if (!supportsGrounding && groundingEnabled) {
            onGroundingChange(false)
        }
    }

    SettingsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable { showModelDialog = true }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = modelLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = selectedModelLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val showGroundingPill = showGroundingCheckbox && supportsGrounding
            if (showThinkingCheckbox || showGroundingPill) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (showThinkingCheckbox) {
                        SettingsCheckboxPill(
                            label = thinkingLabel,
                            checked = thinkingEnabled,
                            onCheckedChange = onThinkingChange,
                            modifier =
                                if (showGroundingPill) {
                                    Modifier.weight(0.9f)
                                } else {
                                    Modifier.fillMaxWidth()
                                },
                        )
                    }

                    if (showGroundingPill) {
                        SettingsCheckboxPill(
                            label = webSearchLabel,
                            checked = groundingEnabled,
                            onCheckedChange = onGroundingChange,
                            modifier = Modifier.weight(1.1f),
                        )
                    }
                }
            }
        }
    }

    if (showModelDialog) {
        ModelPickerDialog(
            selectedModelId = selectedModelId,
            models = modelOptions,
            groundingEnabled = groundingEnabled,
            onGroundingChange = { checked ->
                onGroundingChange(checked)
            },
            onModelSelected = { modelId ->
                onModelSelected(modelId)
                val newModel = modelOptions.firstOrNull { it.id == modelId }
                if (newModel?.supportsGrounding == false && groundingEnabled) {
                    onGroundingChange(false)
                }
            },
            onDismiss = { showModelDialog = false },
            showGroundingToggle = false,
        )
    }
}
