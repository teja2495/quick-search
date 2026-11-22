package com.tk.quicksearch.settings.aiSettings

import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.components.AppMessages
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AnimatedSegmentedTiles
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile
import com.tk.quicksearch.tools.aiSearch.LlmTextModel
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.settings.shared.ModelFeatureSettingsCard
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import org.json.JSONObject

@Composable
fun AiProviderSettingsSection(
        personalContext: String,
        aiSearchLlmProviderId: AiSearchLlmProviderId,
        activeLlmModel: String,
        activeLlmGroundingEnabled: Boolean,
        activeLlmThinkingEnabled: Boolean,
        activeLlmAvailableModels: List<LlmTextModel>,
        availableLlmModelsByProvider: Map<AiSearchLlmProviderId, List<LlmTextModel>>,
        apiKeyLast4ByProvider: Map<AiSearchLlmProviderId, String>,
        customAdvancedPayloadByProvider: Map<AiSearchLlmProviderId, Pair<Boolean, String>>,
        onSetPersonalContext: (String?) -> Unit,
        onSetActiveLlmModel: (String?) -> Unit,
        onSetLlmModel: (AiSearchLlmProviderId, String?) -> Unit,
        onSetCustomAdvancedPayload: (AiSearchLlmProviderId, String?, Boolean) -> Unit,
        onSetActiveLlmGroundingEnabled: (Boolean) -> Unit,
        onSetActiveLlmThinkingEnabled: (Boolean) -> Unit,
        onRefreshAvailableLlmModels: () -> Unit,
        onRequestScrollToBottom: (() -> Unit)? = null,
        showThinkingCheckbox: Boolean = true,
        modifier: Modifier = Modifier,
) {
        val context = LocalContext.current
        var personalContextInput by remember(personalContext) { mutableStateOf(personalContext) }
        var selectedModelInput by remember(activeLlmModel) { mutableStateOf(activeLlmModel) }
        var groundingEnabledInput by
                remember(activeLlmGroundingEnabled) { mutableStateOf(activeLlmGroundingEnabled) }
        var thinkingEnabledInput by
                remember(activeLlmThinkingEnabled) { mutableStateOf(activeLlmThinkingEnabled) }

        LaunchedEffect(Unit) { onRefreshAvailableLlmModels() }
        LaunchedEffect(personalContextInput) { onRequestScrollToBottom?.invoke() }

        Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
                val selectedModel = activeLlmAvailableModels.firstOrNull { it.id == selectedModelInput }
                val supportsInstructions = selectedModel?.supportsSystemInstructions == true

                ModelFeatureSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        selectedModelId = selectedModelInput,
                        selectedProviderId = aiSearchLlmProviderId,
                        availableModels = activeLlmAvailableModels,
                        availableModelsByProvider = availableLlmModelsByProvider,
                        configuredProviderIds = apiKeyLast4ByProvider.keys,
                        thinkingLabel =
                                stringResource(R.string.settings_direct_search_thinking_label),
                        webSearchLabel =
                                stringResource(R.string.settings_direct_search_grounding_label),
                        thinkingEnabled = thinkingEnabledInput,
                        groundingEnabled = groundingEnabledInput,
                        onModelSelected = { modelId ->
                                selectedModelInput = modelId
                                onSetActiveLlmModel(modelId)
                        },
                        onProviderModelSelected = { providerId, modelId ->
                                selectedModelInput = modelId
                                onSetLlmModel(providerId, modelId)
                        },
                        onThinkingChange = { checked ->
                                thinkingEnabledInput = checked
                                onSetActiveLlmThinkingEnabled(checked)
                        },
                        onGroundingChange = { checked ->
                                groundingEnabledInput = checked
                                onSetActiveLlmGroundingEnabled(checked)
                        },
                        showThinkingCheckbox = showThinkingCheckbox,
                )

                Box(
                        modifier =
                                Modifier.fillMaxWidth().then(
                                        if (!supportsInstructions) {
                                                Modifier.clickable(role = Role.Button) {
                                                        AppMessages.show(context, context.getString(
                                                                                R.string
                                                                                        .settings_direct_search_personal_context_unsupported
                                                                        ))
                                                }
                                        } else {
                                                Modifier
                                        }
                                ),
                ) {
                        PersonalContextCard(
                                value = personalContextInput,
                                onValueChange = {
                                        personalContextInput = it
                                        val trimmed = it.trim()
                                        onSetPersonalContext(
                                                trimmed.takeIf { value -> value.isNotEmpty() }
                                        )
                                },
                                enabled = supportsInstructions,
                        )
                }

                if (aiSearchLlmProviderId.isCustom) {
                        val savedPayload = customAdvancedPayloadByProvider[aiSearchLlmProviderId]
                        AdvancedPayloadSettingsSection(
                                payload = savedPayload?.second.orEmpty(),
                                enabled = savedPayload?.first == true,
                                onSave = { payload, enabled ->
                                        onSetCustomAdvancedPayload(
                                                aiSearchLlmProviderId,
                                                payload,
                                                enabled,
                                        )
                                },
                        )
                }
        }
}

@Composable
fun AdvancedPayloadSettingsSection(
        payload: String,
        enabled: Boolean,
        onSave: (String?, Boolean) -> Unit,
) {
        var payloadInput by remember(payload) { mutableStateOf(payload) }
        var enabledInput by remember(enabled) { mutableStateOf(enabled) }
        var hasJsonError by remember(payload) {
                mutableStateOf(payload.isNotBlank() && !isValidJsonObject(payload))
        }

        fun saveIfValid(
                nextPayload: String,
                nextEnabled: Boolean,
        ): Boolean {
                val trimmed = nextPayload.trim()
                if (trimmed.isBlank()) {
                        hasJsonError = false
                        onSave(null, false)
                        return true
                }
                if (!isValidJsonObject(trimmed)) {
                        hasJsonError = true
                        return false
                }
                hasJsonError = false
                enabledInput = nextEnabled
                onSave(trimmed, nextEnabled)
                return true
        }

        SegmentedGroup {
            AnimatedSegmentedTiles {
              item { index, count ->
                SegmentedToggleTile(
                        title = stringResource(R.string.settings_custom_provider_advanced_payload_title),
                        description =
                                stringResource(R.string.settings_custom_provider_advanced_payload_desc),
                        icon = MaterialSymbols.Tune,
                        checked = enabledInput,
                        onCheckedChange = { checked ->
                                enabledInput = checked
                                if (checked) {
                                        val trimmed = payloadInput.trim()
                                        if (trimmed.isNotBlank()) {
                                                saveIfValid(trimmed, true)
                                        }
                                } else {
                                        saveIfValid(payloadInput, false)
                                }
                        },
                        index = index,
                        count = count,
                )
              }
              tile(visible = enabledInput) {
                                SegmentedTileContent {
                                        AppOutlinedTextField(
                                                value = payloadInput,
                                                onValueChange = { next ->
                                                        payloadInput = next
                                                        saveIfValid(next, true)
                                                },
                                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                                placeholder = {
                                                        Text(
                                                                text =
                                                                        stringResource(
                                                                                R.string
                                                                                        .settings_custom_provider_advanced_payload_hint
                                                                        ),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                        )
                                                },
                                                supportingText =
                                                        if (hasJsonError) {
                                                                {
                                                                        Text(
                                                                                text =
                                                                                        stringResource(
                                                                                                R.string
                                                                                                        .settings_custom_provider_advanced_payload_error
                                                                                        )
                                                                        )
                                                                }
                                                        } else {
                                                                null
                                                        },
                                                isError = hasJsonError,
                                                shape = AppShapes.large,
                                                singleLine = false,
                                                minLines = 4,
                                                maxLines = 8,
                                            label = null,
                                        )
                                }
              }
            }
        }
}

/** Laid out like the Home quick note card, in the settings card color: a title over a borderless field. */
@Composable
private fun PersonalContextCard(
        value: String,
        onValueChange: (String) -> Unit,
        enabled: Boolean,
) {
        val textAlpha = if (enabled) 1f else AppColors.DisabledContentAlpha
        val textState = rememberTextFieldState(value)
        val currentValue by rememberUpdatedState(value)
        val currentOnValueChange by rememberUpdatedState(onValueChange)
        LaunchedEffect(value) {
                if (textState.text.toString() != value) textState.setTextAndPlaceCursorAtEnd(value)
        }
        LaunchedEffect(textState) {
                snapshotFlow { textState.text.toString() }.collect { text ->
                        if (text != currentValue) currentOnValueChange(text)
                }
        }
        // Focusable only after a touch lands on it, so initial or restored focus never parks a
        // caret here when the screen opens; it locks again once focus leaves.
        var focusUnlockedByTouch by remember { mutableStateOf(false) }
        var isFieldFocused by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current
        val density = LocalDensity.current
        val imeInsets = WindowInsets.ime
        val isImeVisible by remember(imeInsets, density) {
                derivedStateOf { imeInsets.getBottom(density) > 0 }
        }
        // Closing the keyboard (back gesture, IME hide button) ends editing so no caret lingers.
        // Wait for the keyboard to show first, so a fresh tap isn't cleared before it opens.
        var imeShownWhileFocused by remember { mutableStateOf(false) }
        LaunchedEffect(isFieldFocused, isImeVisible) {
                if (!isFieldFocused) {
                        imeShownWhileFocused = false
                } else if (isImeVisible) {
                        imeShownWhileFocused = true
                } else if (imeShownWhileFocused) {
                        focusManager.clearFocus()
                }
        }
        Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = PersonalContextCardMinHeight),
                shape = AppShapes.widgetPanelCard,
                color = AppColors.getSettingsCardContainerColor(),
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                        Text(
                                text = stringResource(R.string.settings_direct_search_personal_context),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                        )
                        BasicTextField(
                                state = textState,
                                enabled = enabled,
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .pointerInput(Unit) {
                                                        awaitEachGesture {
                                                                awaitFirstDown(
                                                                        requireUnconsumed = false,
                                                                        pass = PointerEventPass.Initial,
                                                                )
                                                                focusUnlockedByTouch = true
                                                        }
                                                }
                                                .focusProperties { canFocus = focusUnlockedByTouch }
                                                .onFocusChanged { state ->
                                                        isFieldFocused = state.isFocused
                                                        if (!state.isFocused) focusUnlockedByTouch = false
                                                },
                                lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 8),
                                textStyle =
                                        MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                                        ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorator = { inner ->
                                        if (textState.text.isEmpty()) {
                                                Text(
                                                        text =
                                                                stringResource(
                                                                        R.string.settings_direct_search_personal_context_hint
                                                                ),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        // Dimmed to match the quick note's hint.
                                                        color =
                                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                        alpha = AppColors.ContentSecondaryAlpha * textAlpha,
                                                                ),
                                                )
                                        }
                                        inner()
                                },
                        )
                }
        }
}

private val PersonalContextCardMinHeight = 164.dp

private fun isValidJsonObject(value: String): Boolean =
        runCatching { JSONObject(value) }.isSuccess
