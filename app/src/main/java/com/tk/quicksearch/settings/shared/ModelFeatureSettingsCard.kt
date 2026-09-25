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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.ModelPickerDialog
import com.tk.quicksearch.tools.aiSearch.GeminiTextModel
import com.tk.quicksearch.tools.aiSearch.isWebSearchAvailable
import com.tk.quicksearch.tools.aiSearch.modelSupportsGrounding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Whether a Tavily key is stored, with `Unknown` while the encrypted read is still in flight. */
enum class TavilyKeyState {
    Unknown,
    Present,
    Absent,
    ;

    val isPresent: Boolean
        get() = this == Present
}

/**
 * Reads the stored Tavily key off the main thread. Stays [TavilyKeyState.Unknown] until the read
 * lands, so callers can avoid acting on a key that is merely not loaded yet.
 */
@Composable
fun rememberTavilyKeyState(): TavilyKeyState {
    val context = LocalContext.current
    val preferences = remember(context) { UserAppPreferences(context.applicationContext) }
    var state by remember(preferences) { mutableStateOf(TavilyKeyState.Unknown) }
    LaunchedEffect(preferences) {
        val hasKey = withContext(Dispatchers.IO) { !preferences.getTavilyApiKey().isNullOrBlank() }
        state = if (hasKey) TavilyKeyState.Present else TavilyKeyState.Absent
    }
    return state
}

@Composable
fun ModelFeatureSettingsCard(
    selectedModelId: String,
    selectedProviderId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI,
    availableModels: List<GeminiTextModel>,
    availableModelsByProvider: Map<AiSearchLlmProviderId, List<GeminiTextModel>> =
        mapOf(selectedProviderId to availableModels),
    configuredProviderIds: Set<AiSearchLlmProviderId> = setOf(selectedProviderId),
    modelLabel: String,
    thinkingLabel: String,
    webSearchLabel: String,
    thinkingEnabled: Boolean,
    groundingEnabled: Boolean,
    onModelSelected: (String) -> Unit,
    onThinkingChange: (Boolean) -> Unit,
    onGroundingChange: (Boolean) -> Unit,
    onProviderModelSelected: (AiSearchLlmProviderId, String) -> Unit = { _, modelId ->
        onModelSelected(modelId)
    },
    modifier: Modifier = Modifier,
    showThinkingCheckbox: Boolean = true,
    showGroundingCheckbox: Boolean = true,
    groundingCheckboxEnabled: Boolean = true,
    tavilyKeyState: TavilyKeyState = rememberTavilyKeyState(),
) {
    var showModelDialog by remember { mutableStateOf(false) }

    val modelOptions =
        remember(availableModels) {
            availableModels.distinctBy { it.id }.sortedBy { it.displayName.lowercase() }
        }

    val selectedModel = modelOptions.firstOrNull { it.id == selectedModelId }
    val isLoadingModels =
        configuredProviderIds.isNotEmpty() &&
            configuredProviderIds.any { it !in availableModelsByProvider }
    val selectedModelLabel =
        when {
            isLoadingModels -> stringResource(R.string.settings_loading_models)
            selectedModel != null -> selectedModel.displayName
            else -> stringResource(R.string.settings_select_model)
        }
    val webSearchAvailable =
        isWebSearchAvailable(
            providerId = selectedProviderId,
            modelSupportsGrounding =
                modelSupportsGrounding(selectedModelId, availableModels, selectedProviderId),
            hasTavilyKey = tavilyKeyState.isPresent,
        )

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
                        .clickable(enabled = !isLoadingModels) { showModelDialog = true }
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

            val showGroundingPill = showGroundingCheckbox && webSearchAvailable
            if (!isLoadingModels && selectedModel != null && (showThinkingCheckbox || showGroundingPill)) {
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
                            enabled = groundingCheckboxEnabled,
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
            },
            onProviderModelSelected = { providerId, modelId ->
                onProviderModelSelected(providerId, modelId)
            },
            onDismiss = { showModelDialog = false },
            showGroundingToggle = false,
            selectedProviderId = selectedProviderId,
            modelsByProvider = availableModelsByProvider,
            configuredProviderIds = configuredProviderIds,
        )
    }
}
