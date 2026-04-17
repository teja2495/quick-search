package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import com.tk.quicksearch.settings.shared.SettingsCard
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.tools.directSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.directSearch.GeminiModelPickerDialog
import com.tk.quicksearch.tools.directSearch.GeminiTextModel
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun APIKeySettingsSection(
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
        var showModelDialog by remember { mutableStateOf(false) }

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

                SettingsCard(
                        modifier = Modifier.fillMaxWidth(),
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
                                                onDismiss = { showModelDialog = false },
                                                showGroundingToggle = false
                                        )
                                }
                                if (showThinkingCheckbox || showGroundingCheckbox) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                                if (showThinkingCheckbox) {
                                                        SettingsCheckboxPill(
                                                                label =
                                                                        stringResource(
                                                                                R.string
                                                                                        .settings_direct_search_thinking_label
                                                                        ),
                                                                checked = thinkingEnabledInput,
                                                                onCheckedChange = { checked ->
                                                                        thinkingEnabledInput = checked
                                                                        onSetGeminiThinkingEnabled(checked)
                                                                },
                                                                modifier =
                                                                        if (showGroundingCheckbox) {
                                                                                Modifier.weight(0.9f)
                                                                        } else {
                                                                                Modifier.fillMaxWidth()
                                                                        },
                                                        )
                                                }
                                                if (showGroundingCheckbox) {
                                                        SettingsCheckboxPill(
                                                                label =
                                                                        stringResource(
                                                                                R.string
                                                                                        .settings_direct_search_grounding_label
                                                                        ),
                                                                checked = groundingEnabledInput,
                                                                onCheckedChange = { checked ->
                                                                        groundingEnabledInput = checked
                                                                        onSetGeminiGroundingEnabled(checked)
                                                                },
                                                                enabled = supportsGrounding,
                                                                onDisabledClick = {
                                                                        Toast.makeText(
                                                                                        context,
                                                                                        context.getString(
                                                                                                R.string
                                                                                                        .settings_direct_search_web_search_unsupported
                                                                                        ),
                                                                                        Toast.LENGTH_SHORT,
                                                                                )
                                                                                .show()
                                                                },
                                                                modifier = Modifier.weight(1.1f),
                                                        )
                                                }
                                        }
                                }
                        }
                }

                SettingsCard(
                        modifier =
                                Modifier.fillMaxWidth().clickable(enabled = !supportsInstructions) {
                                        Toast.makeText(
                                                        context,
                                                        context.getString(R.string.settings_direct_search_personal_context_unsupported),
                                                        Toast.LENGTH_SHORT,
                                                )
                                                .show()
                                },
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
                                                .heightIn(min = 180.dp)
                                                .onFocusChanged { focusState ->
                                                        personalContextFocused = focusState.isFocused
                                                }
                                                .padding(
                                                        horizontal = DesignTokens.CardHorizontalPadding,
                                                        vertical = DesignTokens.CardVerticalPadding,
                                                ),
                                placeholder = {
                                        Text(
                                                text =
                                                        stringResource(
                                                                R.string
                                                                        .settings_direct_search_personal_context_hint
                                                        )
                                        )
                                },
                                shape = MaterialTheme.shapes.extraLarge,
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                disabledBorderColor = Color.Transparent,
                                                errorBorderColor = Color.Transparent,
                                        ),
                                singleLine = false,
                                minLines = 5,
                        )
                }
        }
}

@Composable
private fun SettingsCheckboxPill(
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        enabled: Boolean = true,
        onDisabledClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier,
) {
        val containerColor =
                if (checked) {
                        MaterialTheme.colorScheme.secondaryContainer
                } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
        val borderColor =
                if (checked) {
                        MaterialTheme.colorScheme.secondary
                } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                }

        Surface(
                modifier =
                        modifier
                                .clip(DesignTokens.ShapeFull)
                                .clickable(enabled = enabled || onDisabledClick != null) {
                                        if (enabled) {
                                                onCheckedChange(!checked)
                                        } else {
                                                onDisabledClick?.invoke()
                                        }
                                },
                shape = DesignTokens.ShapeFull,
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
        ) {
                Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                        Checkbox(
                                checked = checked,
                                onCheckedChange = { value ->
                                        if (enabled) {
                                                onCheckedChange(value)
                                        }
                                },
                                enabled = enabled,
                                modifier = Modifier.scale(0.72f),
                        )
                        Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color =
                                        if (enabled) {
                                                MaterialTheme.colorScheme.onSurface
                                        } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        },
                        )
                }
        }
}
