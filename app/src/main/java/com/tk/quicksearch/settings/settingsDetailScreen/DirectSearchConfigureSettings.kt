package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.directSearch.GeminiModelCatalog
import com.tk.quicksearch.search.directSearch.GeminiModelPickerDialog
import com.tk.quicksearch.search.directSearch.GeminiTextModel
import com.tk.quicksearch.ui.theme.DesignTokens

@Composable
fun DirectSearchConfigureSettingsSection(
        personalContext: String,
        geminiModel: String,
        geminiGroundingEnabled: Boolean,
        availableGeminiModels: List<GeminiTextModel>,
        onSetPersonalContext: (String?) -> Unit,
        onSetGeminiModel: (String?) -> Unit,
        onSetGeminiGroundingEnabled: (Boolean) -> Unit,
        onRefreshAvailableGeminiModels: () -> Unit,
        modifier: Modifier = Modifier,
) {
        var personalContextInput by remember(personalContext) { mutableStateOf(personalContext) }
        var selectedModelInput by remember(geminiModel) { mutableStateOf(geminiModel) }
        var groundingEnabledInput by
                remember(geminiGroundingEnabled) { mutableStateOf(geminiGroundingEnabled) }
        var showModelDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { onRefreshAvailableGeminiModels() }

        val modelOptions =
                remember(availableGeminiModels, selectedModelInput) {
                        val allKnownModels =
                                availableGeminiModels + GeminiModelCatalog.FALLBACK_TEXT_MODELS
                        val currentModel =
                                allKnownModels.find { it.id == selectedModelInput }
                                        ?: GeminiTextModel(
                                                selectedModelInput,
                                                selectedModelInput
                                        ) // Fallback if truly unknown

                        (availableGeminiModels + currentModel).distinctBy { it.id }.sortedBy {
                                it.displayName.lowercase()
                        }
                }
        val selectedModelLabel =
                modelOptions.firstOrNull { it.id == selectedModelInput }?.displayName
                        ?: selectedModelInput

        Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
                val selectedModel = modelOptions.firstOrNull { it.id == selectedModelInput }
                val supportsInstructions = selectedModel?.supportsSystemInstructions != false
                val supportsGrounding = selectedModel?.supportsGrounding != false

                if (!supportsInstructions || !supportsGrounding) {
                        ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        CardDefaults.elevatedCardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme
                                                                .secondaryContainer,
                                                contentColor =
                                                        MaterialTheme.colorScheme
                                                                .onSecondaryContainer
                                        ),
                                shape = MaterialTheme.shapes.large
                        ) {
                                Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        Icon(
                                                imageVector =
                                                        androidx.compose.material.icons.Icons
                                                                .Rounded.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                        )
                                        val message =
                                                remember(
                                                        selectedModelLabel,
                                                        supportsInstructions,
                                                        supportsGrounding
                                                ) {
                                                        val unsupported = mutableListOf<String>()
                                                        if (!supportsInstructions)
                                                                unsupported.add("personal context")
                                                        if (!supportsGrounding)
                                                                unsupported.add("grounding")
                                                        "$selectedModelLabel does not support ${unsupported.joinToString(" or ")}."
                                                }
                                        Text(
                                                text = message,
                                                style = MaterialTheme.typography.bodyMedium,
                                        )
                                }
                        }
                }

                if (supportsInstructions) {
                        OutlinedTextField(
                                value = personalContextInput,
                                onValueChange = {
                                        personalContextInput = it
                                        val trimmed = it.trim()
                                        onSetPersonalContext(
                                                trimmed.takeIf { value -> value.isNotEmpty() }
                                        )
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                                placeholder = {
                                        Text(
                                                text =
                                                        stringResource(
                                                                R.string
                                                                        .settings_direct_search_personal_context_hint
                                                        )
                                        )
                                },
                                shape = MaterialTheme.shapes.large,
                                singleLine = false,
                                minLines = 5,
                        )
                }

                ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                ) {
                        Column(
                                modifier =
                                        Modifier.padding(
                                                horizontal = DesignTokens.CardHorizontalPadding,
                                                vertical = DesignTokens.CardVerticalPadding,
                                        ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .clickable {
                                                                        showModelDialog = true
                                                                }
                                                                .padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 12.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                                Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(2.dp),
                                                ) {
                                                        Text(
                                                                text =
                                                                        stringResource(
                                                                                R.string
                                                                                        .settings_direct_search_model_label
                                                                        ),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant,
                                                        )
                                                        Text(
                                                                text = selectedModelLabel,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface,
                                                        )
                                                }
                                                Icon(
                                                        imageVector = Icons.Rounded.ExpandMore,
                                                        contentDescription = null,
                                                        tint =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                )
                                        }
                                }

                                if (showModelDialog) {
                                        GeminiModelPickerDialog(
                                                selectedModelId = selectedModelInput,
                                                models = modelOptions,
                                                groundingEnabled = groundingEnabledInput,
                                                onGroundingChange = { checked ->
                                                        groundingEnabledInput = checked
                                                        onSetGeminiGroundingEnabled(checked)
                                                },
                                                onModelSelected = { modelId ->
                                                        selectedModelInput = modelId
                                                        onSetGeminiModel(modelId)
                                                        val newModel =
                                                                modelOptions.firstOrNull {
                                                                        it.id == modelId
                                                                }
                                                        if (newModel?.supportsGrounding == false &&
                                                                        groundingEnabledInput
                                                        ) {
                                                                groundingEnabledInput = false
                                                                onSetGeminiGroundingEnabled(false)
                                                        }
                                                },
                                                onDismiss = { showModelDialog = false }
                                        )
                                }
                        }
                }
        }
}
