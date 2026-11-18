package com.tk.quicksearch.tools.aiSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerMessage
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerSearch
import com.tk.quicksearch.shared.ui.components.AppSwitch
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

data class LlmModelPickerOption(
    val providerId: AiSearchLlmProviderId,
    val model: LlmTextModel,
)

/** Bottom drawer for picking an AI model. */
@Composable
fun ModelPickerDialog(
    selectedModelId: String,
    models: List<LlmTextModel>,
    groundingEnabled: Boolean,
    onGroundingChange: (Boolean) -> Unit,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    showGroundingToggle: Boolean = true,
    selectedProviderId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI,
    modelsByProvider: Map<AiSearchLlmProviderId, List<LlmTextModel>> =
        mapOf(selectedProviderId to models),
    configuredProviderIds: Set<AiSearchLlmProviderId> = setOf(selectedProviderId),
    onProviderModelSelected: (AiSearchLlmProviderId, String) -> Unit = { _, modelId ->
        onModelSelected(modelId)
    },
) {
    val listState = rememberLazyListState()
    val selectedModel = models.firstOrNull { it.id == selectedModelId }
    val supportsGrounding = selectedModel?.supportsGrounding != false
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val pickerOptions =
        remember(models, modelsByProvider, configuredProviderIds, selectedProviderId) {
            val configured = configuredProviderIds.takeIf { it.isNotEmpty() } ?: setOf(selectedProviderId)
            configured
                .flatMap { providerId ->
                    val providerModels =
                        modelsByProvider[providerId]
                            ?: if (providerId == selectedProviderId) models else emptyList()
                    providerModels.map { model ->
                        LlmModelPickerOption(providerId = providerId, model = model)
                    }
                }
                .distinctBy { it.providerId.storageValue + ":" + it.model.id }
                .sortedWith(
                    compareBy<LlmModelPickerOption> { providerSortOrder(it.providerId) }
                        .thenBy { it.model.displayName.lowercase() },
                )
        }
    // Without a query, show only each provider's latest models (plus the selection); search covers all.
    val featuredOptions =
        remember(pickerOptions, selectedProviderId, selectedModelId) {
            val featuredIds =
                pickerOptions
                    .groupBy { it.providerId }
                    .flatMap { (providerId, options) ->
                        featuredModels(providerId, options.map { it.model }).map { providerId to it.id }
                    }.toSet()
            pickerOptions.filter { option ->
                (option.providerId to option.model.id) in featuredIds ||
                    (option.providerId == selectedProviderId && option.model.id == selectedModelId)
            }
        }
    val hasHiddenModels = featuredOptions.size < pickerOptions.size
    val filteredOptions =
        remember(pickerOptions, featuredOptions, searchQuery) {
            val query = searchQuery.text.trim().lowercase()
            if (query.isEmpty()) {
                featuredOptions
            } else {
                pickerOptions.filter { option ->
                    modelSearchText(option.model).contains(query) ||
                        providerSearchName(option.providerId).lowercase().contains(query)
                }
            }
        }
    val showProviderLabels = configuredProviderIds.size > 1

    LaunchedEffect(Unit) {
        val index =
            featuredOptions.indexOfFirst {
                it.providerId == selectedProviderId && it.model.id == selectedModelId
            }
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    AppPickerDrawer(
        title = stringResource(R.string.dialog_gemini_model_picker_title),
        onDismiss = onDismiss,
        search =
            AppPickerDrawerSearch(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(R.string.settings_model_picker_search_hint),
            ),
        footer =
            if (showGroundingToggle && supportsGrounding) {
                {
                    AppPickerDrawerRow(
                        title = stringResource(R.string.settings_direct_search_grounding_label),
                        onClick = { onGroundingChange(!groundingEnabled) },
                        leading = {
                            Icon(
                                imageVector = MaterialSymbols.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailing = { AppSwitch(checked = groundingEnabled, onCheckedChange = null) },
                    )
                }
            } else {
                null
            },
    ) { dismiss ->
        if (filteredOptions.isEmpty()) {
            AppPickerDrawerMessage(text = stringResource(R.string.widget_custom_buttons_no_results))
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing),
            ) {
                itemsIndexed(
                    filteredOptions,
                    key = { _, option -> "${option.providerId.storageValue}:${option.model.id}" },
                ) { _, option ->
                    AppPickerDrawerRow(
                        title = option.model.displayName,
                        selected = option.providerId == selectedProviderId && option.model.id == selectedModelId,
                        // M3 list item: with several providers, the logo leads and the provider is
                        // named under the model. With one, every row would repeat the same logo, so
                        // the rows are text only.
                        leading =
                            if (showProviderLabels) {
                                { AiProviderBadge(providerId = option.providerId) }
                            } else {
                                null
                            },
                        subtitle = if (showProviderLabels) aiProviderDisplayName(option.providerId) else null,
                        onClick = {
                            onProviderModelSelected(option.providerId, option.model.id)
                            dismiss()
                        },
                    )
                }
                if (hasHiddenModels && searchQuery.text.isBlank()) {
                    item {
                        Text(
                            text = stringResource(R.string.settings_model_picker_more_models_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = DesignTokens.SpacingMedium,
                                        vertical = DesignTokens.SpacingMedium,
                                    ),
                        )
                    }
                }
            }
        }
    }
}

/** The provider's latest models shown before the user searches. */
private fun featuredModels(
    providerId: AiSearchLlmProviderId,
    models: List<LlmTextModel>,
): List<LlmTextModel> =
    AiSearchLlmProviderRegistry.definition(providerId)?.featuredModels?.invoke(models) ?: models

private fun providerSortOrder(providerId: AiSearchLlmProviderId): Int =
    AiSearchLlmProviderRegistry.pickerOrder(providerId)

private fun providerSearchName(providerId: AiSearchLlmProviderId): String =
    AiSearchLlmProviderRegistry.definition(providerId)?.searchName ?: "Custom"

private fun modelSearchText(model: LlmTextModel): String =
    listOf(
        model.displayName,
        model.id,
        model.id.replace('-', ' '),
        model.id.replace('_', ' '),
    )
        .joinToString(" ")
        .lowercase()
