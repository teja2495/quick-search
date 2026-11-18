package com.tk.quicksearch.settings.customTools

import com.tk.quicksearch.shared.ui.components.AppButtonSize
import com.tk.quicksearch.settings.shared.settingsScrollClip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.search.data.preferences.WeatherTemperatureUnit
import com.tk.quicksearch.search.data.preferences.WeatherWindSpeedUnit
import com.tk.quicksearch.settings.shared.ModelFeatureSettingsCard
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTileContentPadding
import com.tk.quicksearch.settings.shared.TavilyKeyState
import com.tk.quicksearch.settings.shared.rememberTavilyKeyState
import com.tk.quicksearch.settings.settingsDetailScreen.AdvancedPayloadSettingsSection
import com.tk.quicksearch.shared.ui.components.CardTextField
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.LlmTextModel
import com.tk.quicksearch.tools.aiSearch.modelSupportsGrounding
import com.tk.quicksearch.tools.aiSearch.providerSupportsNativeSearch
import com.tk.quicksearch.tools.aiSearch.supportsThinkingControl
import com.tk.quicksearch.shared.ui.components.AppButton

@Composable
fun CustomToolEditorScreen(
    existingTool: CustomTool?,
    existingAlias: String,
    existingLocation: String = "",
    existingTemperatureUnit: WeatherTemperatureUnit = WeatherTemperatureUnit.CELSIUS,
    existingWindSpeedUnit: WeatherWindSpeedUnit = WeatherWindSpeedUnit.KILOMETERS_PER_HOUR,
    selectedProviderId: AiSearchLlmProviderId,
    defaultModelId: String,
    defaultThinkingEnabled: Boolean,
    thinkingEnabledByProvider: Map<AiSearchLlmProviderId, Boolean>,
    availableModels: List<LlmTextModel>,
    availableModelsByProvider: Map<AiSearchLlmProviderId, List<LlmTextModel>>,
    configuredProviderIds: Set<AiSearchLlmProviderId>,
    onRefreshAvailableLlmModels: () -> Unit,
    onProviderModelSelected: (AiSearchLlmProviderId, String) -> Unit,
    onSave: (name: String, prompt: String, location: String, providerId: AiSearchLlmProviderId, modelId: String, groundingEnabled: Boolean, aliasCode: String, thinkingEnabled: Boolean, advancedPayload: String?, advancedPayloadEnabled: Boolean, temperatureUnit: WeatherTemperatureUnit, windSpeedUnit: WeatherWindSpeedUnit) -> Unit,
    showNameInput: Boolean = true,
    showPromptInput: Boolean = true,
    showAliasInput: Boolean = true,
    showLocationInput: Boolean = false,
    showWeatherUnitInputs: Boolean = false,
    webSearchAlwaysEnabled: Boolean = false,
    shouldAutoFocusTitle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var nameInput by remember(existingTool?.id) {
        mutableStateOf(existingTool?.name.orEmpty())
    }
    var promptInput by remember(existingTool?.id) {
        mutableStateOf(existingTool?.prompt.orEmpty())
    }
    var selectedModelId by remember(existingTool?.id) {
        mutableStateOf(existingTool?.modelId ?: defaultModelId)
    }
    var selectedProviderInput by remember(existingTool?.id, selectedProviderId) {
        mutableStateOf(existingTool?.providerId ?: selectedProviderId)
    }
    var aliasInput by remember(existingTool?.id) {
        mutableStateOf(existingAlias)
    }
    var locationInput by remember(existingTool?.id) {
        mutableStateOf(existingLocation)
    }
    var temperatureUnit by remember(existingTool?.id) {
        mutableStateOf(existingTemperatureUnit)
    }
    var windSpeedUnit by remember(existingTool?.id) {
        mutableStateOf(existingWindSpeedUnit)
    }
    var groundingEnabled by remember(existingTool?.id) {
        mutableStateOf(existingTool?.groundingEnabled ?: false)
    }
    var thinkingEnabled by remember(existingTool?.id) {
        mutableStateOf(existingTool?.thinkingEnabled ?: defaultThinkingEnabled)
    }
    var thinkingWasChanged by remember(existingTool?.id) { mutableStateOf(false) }
    var advancedPayloadInput by remember(existingTool?.id) {
        mutableStateOf(existingTool?.advancedPayload.orEmpty())
    }
    var advancedPayloadEnabled by remember(existingTool?.id) {
        mutableStateOf(existingTool?.advancedPayloadEnabled == true)
    }

    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { onRefreshAvailableLlmModels() }

    LaunchedEffect(existingTool?.id, shouldAutoFocusTitle) {
        if (shouldAutoFocusTitle && nameInput.trim().isEmpty()) {
            focusRequester.requestFocus()
        }
    }

    val selectedProviderModels = remember(selectedProviderInput, availableModelsByProvider, availableModels) {
        availableModelsByProvider[selectedProviderInput]
            ?: if (selectedProviderInput == selectedProviderId) availableModels else emptyList()
    }

    LaunchedEffect(selectedModelId, selectedProviderInput, availableModelsByProvider) {
        val selectedProviderHasModel =
            availableModelsByProvider[selectedProviderInput]?.any { it.id == selectedModelId } == true
        if (selectedProviderInput.isCustom || selectedProviderHasModel) return@LaunchedEffect

        val customProviderForSelectedModel =
            availableModelsByProvider
                .filterKeys { it.isCustom }
                .filterValues { models -> models.any { it.id == selectedModelId } }
                .keys
                .singleOrNull()
        if (customProviderForSelectedModel != null) {
            selectedProviderInput = customProviderForSelectedModel
        }
    }

    LaunchedEffect(selectedProviderInput, selectedProviderModels, availableModelsByProvider) {
        val providerCatalogLoaded = selectedProviderInput in availableModelsByProvider
        if (providerCatalogLoaded &&
            selectedModelId.isNotBlank() &&
            selectedProviderModels.none { it.id == selectedModelId }
        ) {
            selectedModelId = ""
        }
    }

    val showThinkingToggle = supportsThinkingControl(selectedProviderInput, selectedModelId)
    val supportsAdvancedPayload = selectedProviderInput.isCustom

    val tavilyKeyState = rememberTavilyKeyState()
    // Tools that force web search on (Weather) produce wrong answers without it, so warn when
    // neither the model nor Tavily can supply web results.
    val showWebSearchWarning =
        webSearchAlwaysEnabled &&
            tavilyKeyState == TavilyKeyState.Absent &&
            !(
                providerSupportsNativeSearch(selectedProviderInput) &&
                    modelSupportsGrounding(
                        selectedModelId,
                        selectedProviderModels,
                        selectedProviderInput,
                    )
            )

    val isNameValid = !showNameInput || nameInput.trim().isNotBlank()
    val isPromptValid = !showPromptInput || promptInput.trim().isNotBlank()
    val isAliasValid = !showAliasInput || aliasInput.trim().isNotBlank()
    val canSave =
        isNameValid && isPromptValid && isAliasValid && selectedModelId.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                .settingsScrollClip(scrollState)
                .verticalScroll(scrollState)
                .padding(bottom = DesignTokens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            if (showWebSearchWarning) {
                SegmentedTile(
                    index = 0,
                    count = 1,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(SegmentedTileContentPadding),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            text = stringResource(R.string.settings_weather_no_web_search_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            if (showNameInput || showPromptInput || showLocationInput || showAliasInput) {
                CustomToolInputsCard(
                    showNameInput = showNameInput,
                    name = nameInput,
                    onNameChange = { nameInput = it },
                    nameFocusRequester = focusRequester,
                    showPromptInput = showPromptInput,
                    prompt = promptInput,
                    onPromptChange = { promptInput = it },
                    showLocationInput = showLocationInput,
                    location = locationInput,
                    onLocationChange = { locationInput = it },
                    showAliasInput = showAliasInput,
                    alias = aliasInput,
                    onAliasChange = { aliasInput = it },
                )
            }

            if (showWeatherUnitInputs) {
                WeatherUnitSettingsSection(
                    temperatureUnit = temperatureUnit,
                    onTemperatureUnitSelected = { temperatureUnit = it },
                )
            }

            ModelFeatureSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                selectedModelId = selectedModelId,
                selectedProviderId = selectedProviderInput,
                availableModels = selectedProviderModels,
                availableModelsByProvider = availableModelsByProvider,
                configuredProviderIds = configuredProviderIds,
                thinkingLabel = stringResource(R.string.settings_direct_search_thinking_label),
                webSearchLabel = stringResource(R.string.settings_direct_search_grounding_label),
                thinkingEnabled = thinkingEnabled,
                groundingEnabled = if (webSearchAlwaysEnabled) true else groundingEnabled,
                onModelSelected = { selectedModelId = it },
                onProviderModelSelected = { providerId, modelId ->
                    selectedProviderInput = providerId
                    selectedModelId = modelId
                    if (!thinkingWasChanged) {
                        thinkingEnabled = thinkingEnabledByProvider[providerId] ?: false
                    }
                    onProviderModelSelected(providerId, modelId)
                },
                onThinkingChange = {
                    thinkingEnabled = it
                    thinkingWasChanged = true
                },
                onGroundingChange = { enabled ->
                    if (!webSearchAlwaysEnabled) groundingEnabled = enabled
                },
                showThinkingCheckbox = showThinkingToggle,
                groundingCheckboxEnabled = !webSearchAlwaysEnabled,
                tavilyKeyState = tavilyKeyState,
            )

            if (supportsAdvancedPayload) {
                AdvancedPayloadSettingsSection(
                    payload = advancedPayloadInput,
                    enabled = advancedPayloadEnabled,
                    onSave = { payload, enabled ->
                        advancedPayloadInput = payload.orEmpty()
                        advancedPayloadEnabled = enabled
                    },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.ContentHorizontalPadding),
        ) {
            AppButton(
                onClick = {
                    if (canSave) {
                        onSave(
                            nameInput.trim(),
                            promptInput.trim(),
                            locationInput.trim(),
                            selectedProviderInput,
                            selectedModelId,
                            if (webSearchAlwaysEnabled) true else groundingEnabled,
                            aliasInput.trim(),
                            if (showThinkingToggle) thinkingEnabled else false,
                            advancedPayloadInput.trim().takeIf {
                                supportsAdvancedPayload && advancedPayloadEnabled && it.isNotEmpty()
                            },
                            supportsAdvancedPayload && advancedPayloadEnabled,
                            temperatureUnit,
                            windSpeedUnit,
                        )
                    }
                },
                enabled = canSave,
                size = AppButtonSize.Medium,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        }
    }

}

/** Name, prompt, location, and alias as segmented tiles, one field per tile. */
@Composable
private fun CustomToolInputsCard(
    showNameInput: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    nameFocusRequester: FocusRequester,
    showPromptInput: Boolean,
    prompt: String,
    onPromptChange: (String) -> Unit,
    showLocationInput: Boolean,
    location: String,
    onLocationChange: (String) -> Unit,
    showAliasInput: Boolean,
    alias: String,
    onAliasChange: (String) -> Unit,
) {
    val rowModifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.SpacingSmall, vertical = DesignTokens.SpacingXSmall)
    val rows =
        buildList<@Composable () -> Unit> {
            if (showNameInput) {
                add {
                    CardTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = stringResource(R.string.settings_custom_tool_name_label),
                        singleLine = true,
                        modifier = rowModifier.focusRequester(nameFocusRequester),
                    )
                }
            }
            if (showPromptInput) {
                add {
                    CardTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        label = stringResource(R.string.settings_custom_tool_prompt_label),
                        placeholder = stringResource(R.string.settings_custom_tool_prompt_hint),
                        minLines = 3,
                        maxLines = 10,
                        modifier = rowModifier.heightIn(min = 120.dp),
                    )
                }
            }
            if (showLocationInput) {
                add {
                    CardTextField(
                        value = location,
                        onValueChange = onLocationChange,
                        label = stringResource(R.string.weather_location_label),
                        placeholder = stringResource(R.string.weather_location_hint),
                        singleLine = true,
                        modifier = rowModifier,
                    )
                }
            }
            if (showAliasInput) {
                add {
                    CardTextField(
                        value = alias,
                        onValueChange = onAliasChange,
                        label = stringResource(R.string.settings_custom_tool_alias_label),
                        placeholder = stringResource(R.string.settings_custom_tool_alias_hint),
                        singleLine = true,
                        modifier = rowModifier,
                    )
                }
            }
        }
    SegmentedGroup {
        rows.forEachIndexed { index, row ->
            SegmentedTile(index = index, count = rows.size) { row() }
        }
    }
}

@Composable
private fun WeatherUnitSettingsSection(
    temperatureUnit: WeatherTemperatureUnit,
    onTemperatureUnitSelected: (WeatherTemperatureUnit) -> Unit,
) {
    SegmentedTile(index = 0, count = 1) {
        SegmentedTileContent {
            Text(
                text = stringResource(R.string.weather_temperature_unit_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ConnectedChoiceGroup(
                options =
                    WeatherTemperatureUnit.entries.map { unit ->
                        ChoiceOption(value = unit, label = stringResource(unit.labelRes))
                    },
                selected = temperatureUnit,
                onSelect = onTemperatureUnitSelected,
            )
        }
    }
}

private val WeatherTemperatureUnit.labelRes: Int
    get() =
        when (this) {
            WeatherTemperatureUnit.CELSIUS -> R.string.weather_temperature_unit_celsius
            WeatherTemperatureUnit.FAHRENHEIT -> R.string.weather_temperature_unit_fahrenheit
        }
