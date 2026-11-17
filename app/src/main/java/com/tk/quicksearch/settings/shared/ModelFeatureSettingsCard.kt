package com.tk.quicksearch.settings.shared

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.tools.aiSearch.AiProviderBadge
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.aiProviderDisplayName
import com.tk.quicksearch.tools.aiSearch.ModelPickerDialog
import com.tk.quicksearch.tools.aiSearch.LlmTextModel
import com.tk.quicksearch.tools.aiSearch.isWebSearchAvailable
import com.tk.quicksearch.tools.aiSearch.modelSupportsGrounding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.SegmentedTile

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
    availableModels: List<LlmTextModel>,
    availableModelsByProvider: Map<AiSearchLlmProviderId, List<LlmTextModel>> =
        mapOf(selectedProviderId to availableModels),
    configuredProviderIds: Set<AiSearchLlmProviderId> = setOf(selectedProviderId),
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
    isLoading: Boolean = false,
) {
    var showModelDialog by remember { mutableStateOf(false) }

    val modelOptions =
        remember(availableModels) {
            availableModels.distinctBy { it.id }.sortedBy { it.displayName.lowercase() }
        }

    val selectedModel = modelOptions.firstOrNull { it.id == selectedModelId }
    val isLoadingModels =
        isLoading ||
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

    val showFeatures = !isLoadingModels && selectedModel != null
    val showThinking = showFeatures && showThinkingCheckbox
    val showGrounding = showFeatures && showGroundingCheckbox && webSearchAvailable
    val tileCount = 1 + (if (showThinking) 1 else 0) + (if (showGrounding) 1 else 0)

    // The model, then each on/off feature as its own switch tile, as Material 3 lists settings.
    SegmentedGroup(modifier = modifier) {
        SegmentedTile(
            index = 0,
            count = tileCount,
            onClick = if (isLoadingModels) null else ({ showModelDialog = true }),
        ) {
            SegmentedTileRow(
                title = selectedModelLabel,
                description = aiProviderDisplayName(selectedProviderId),
                leading = { AiProviderBadge(providerId = selectedProviderId) },
                trailing = {
                    Icon(
                        imageVector = MaterialSymbols.UnfoldMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
        if (showThinking) {
            SegmentedToggleTile(
                title = thinkingLabel,
                icon = MaterialSymbols.Lightbulb,
                checked = thinkingEnabled,
                onCheckedChange = onThinkingChange,
                index = 1,
                count = tileCount,
            )
        }
        if (showGrounding) {
            SegmentedToggleTile(
                title = webSearchLabel,
                icon = MaterialSymbols.Public,
                checked = groundingEnabled,
                onCheckedChange = onGroundingChange,
                index = tileCount - 1,
                count = tileCount,
                enabled = groundingCheckboxEnabled,
            )
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
