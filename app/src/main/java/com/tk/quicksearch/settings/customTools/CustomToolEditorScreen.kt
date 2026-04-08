package com.tk.quicksearch.settings.customTools

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.directSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.directSearch.GeminiModelPickerDialog
import com.tk.quicksearch.tools.directSearch.GeminiTextModel

@Composable
fun CustomToolEditorScreen(
    existingTool: CustomTool?,
    existingAlias: String,
    availableModels: List<GeminiTextModel>,
    onSave: (name: String, prompt: String, modelId: String, groundingEnabled: Boolean, aliasCode: String, thinkingEnabled: Boolean) -> Unit,
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
    var showModelPicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (existingTool == null) {
            focusRequester.requestFocus()
        }
    }

    val isNameValid = nameInput.trim().isNotBlank()
    val isPromptValid = promptInput.trim().isNotBlank()
    val isAliasValid = aliasInput.trim().isNotBlank()
    val canSave = isNameValid && isPromptValid && isAliasValid

    val selectedModelName = remember(selectedModelId, availableModels) {
        availableModels.firstOrNull { it.id == selectedModelId }?.displayName
            ?: GeminiModelCatalog.FALLBACK_TEXT_MODELS.firstOrNull { it.id == selectedModelId }?.displayName
            ?: selectedModelId
    }

    val modelArrowRotation by animateFloatAsState(
        targetValue = if (showModelPicker) 180f else 0f,
        animationSpec = tween(200),
        label = "modelArrow",
    )

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

            SettingsCard(modifier = Modifier.fillMaxWidth()) {
                // Model picker row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showModelPicker = true }
                        .padding(
                            horizontal = DesignTokens.CardHorizontalPadding,
                            vertical = DesignTokens.CardVerticalPadding,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_custom_tool_model_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingXXSmall))
                        Text(
                            text = selectedModelName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(modelArrowRotation),
                    )
                }

                HorizontalDivider(color = AppColors.SettingsDivider)

                SettingsToggleRow(
                    title = stringResource(R.string.settings_custom_tool_grounding_label),
                    subtitle = stringResource(R.string.settings_custom_tool_grounding_hint),
                    checked = groundingEnabled,
                    onCheckedChange = { groundingEnabled = it },
                    isFirstItem = false,
                    isLastItem = false,
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_custom_tool_thinking_label),
                    subtitle = stringResource(R.string.settings_custom_tool_thinking_hint),
                    checked = thinkingEnabled,
                    onCheckedChange = { thinkingEnabled = it },
                    isFirstItem = false,
                    isLastItem = true,
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
                        onSave(nameInput.trim(), promptInput.trim(), selectedModelId, groundingEnabled, aliasInput.trim(), thinkingEnabled)
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

    if (showModelPicker) {
        val modelsToShow = availableModels.ifEmpty { GeminiModelCatalog.FALLBACK_TEXT_MODELS }
        GeminiModelPickerDialog(
            selectedModelId = selectedModelId,
            models = modelsToShow,
            groundingEnabled = false,
            onGroundingChange = {},
            onModelSelected = { modelId ->
                selectedModelId = modelId
                showModelPicker = false
            },
            onDismiss = { showModelPicker = false },
            showGroundingToggle = false,
        )
    }
}
