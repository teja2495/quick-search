package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.shared.ui.components.AppFilterChipStyle
import com.tk.quicksearch.shared.ui.components.AppLoadingIndicator
import com.tk.quicksearch.shared.ui.components.AppFilterChip
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.tools.aiSearch.aiProviderDisplayName
import com.tk.quicksearch.settings.shared.rememberTavilyKeyState
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.LlmTextModel
import com.tk.quicksearch.tools.aiSearch.isWebSearchAvailable
import com.tk.quicksearch.tools.aiSearch.modelSupportsGrounding
import com.tk.quicksearch.tools.aiSearch.supportsThinkingControl

/** Active AI model state for the searchable model row; tapping the row opens the model picker. */
data class AiModelInlineState(
    val providerId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI,
    val selectedModelId: String = "",
    val availableModels: List<LlmTextModel> = emptyList(),
    val modelsByProvider: Map<AiSearchLlmProviderId, List<LlmTextModel>> = emptyMap(),
    val configuredProviderIds: Set<AiSearchLlmProviderId> = emptySet(),
    val thinkingEnabled: Boolean = false,
    val groundingEnabled: Boolean = false,
    val onThinkingChange: (Boolean) -> Unit = {},
    val onGroundingChange: (Boolean) -> Unit = {},
    val onRefreshModels: () -> Unit = {},
)

/** Mirrors the model card in AI provider settings: selected model and provider, then Thinking / Web Search chips. */
@Composable
internal fun AiModelInlineContent(state: AiModelInlineState) {
    LaunchedEffect(Unit) { state.onRefreshModels() }
    val tavilyKeyState = rememberTavilyKeyState()
    val selectedModel = state.availableModels.firstOrNull { it.id == state.selectedModelId }
    val isLoadingModels = state.configuredProviderIds.any { it !in state.modelsByProvider }
    if (selectedModel == null && isLoadingModels) {
        AppLoadingIndicator(
            modifier =
                Modifier
                    .padding(top = DesignTokens.SpacingXSmall)
                    .size(DesignTokens.InlineProgressSize),
        )
        return
    }
    Text(
        text =
            if (selectedModel != null) {
                stringResource(
                    R.string.settings_ai_model_with_provider,
                    selectedModel.displayName,
                    aiProviderDisplayName(state.providerId),
                )
            } else {
                stringResource(R.string.settings_select_model)
            },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    if (selectedModel == null) return
    val showThinking = supportsThinkingControl(state.providerId, state.selectedModelId)
    val showWebSearch =
        isWebSearchAvailable(
            providerId = state.providerId,
            modelSupportsGrounding =
                modelSupportsGrounding(state.selectedModelId, state.availableModels, state.providerId),
            hasTavilyKey = tavilyKeyState.isPresent,
        )
    if (!showThinking && !showWebSearch) return
    Row(
        modifier = Modifier.padding(top = DesignTokens.SpacingXSmall),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        if (showThinking) {
            CompactCheckChip(
                label = stringResource(R.string.settings_direct_search_thinking_label),
                checked = state.thinkingEnabled,
                onCheckedChange = state.onThinkingChange,
            )
        }
        if (showWebSearch) {
            CompactCheckChip(
                label = stringResource(R.string.settings_direct_search_grounding_label),
                checked = state.groundingEnabled,
                onCheckedChange = state.onGroundingChange,
            )
        }
    }
}

/** Material 3 filter chip for an on/off model feature, small enough to sit under a result row's subtitle. */
@Composable
private fun CompactCheckChip(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val view = LocalView.current
    AppFilterChip(
        selected = checked,
        onClick = {
            hapticToggle(view)()
            onCheckedChange(!checked)
        },
        label = label,
        style = AppFilterChipStyle.Tonal,
        showSelectedCheck = true,
    )
}
