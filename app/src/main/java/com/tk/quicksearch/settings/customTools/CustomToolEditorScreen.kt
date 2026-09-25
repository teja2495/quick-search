package com.tk.quicksearch.settings.customTools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.tk.quicksearch.settings.shared.SettingsCheckboxPill
import com.tk.quicksearch.settings.shared.TavilyKeyState
import com.tk.quicksearch.settings.shared.rememberTavilyKeyState
import com.tk.quicksearch.settings.settingsDetailScreen.AdvancedPayloadSettingsSection
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.LlmTextModel
import com.tk.quicksearch.tools.aiSearch.modelSupportsGrounding
import com.tk.quicksearch.tools.aiSearch.providerSupportsNativeSearch
import com.tk.quicksearch.tools.aiSearch.supportsThinkingControl

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
                .verticalScroll(scrollState)
                .padding(
                    start = DesignTokens.ContentHorizontalPadding,
                    end = DesignTokens.ContentHorizontalPadding,
                    bottom = DesignTokens.SpacingLarge,
                ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
            if (showWebSearchWarning) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = DesignTokens.ShapeLarge,
                ) {
                    Text(
                        text = stringResource(R.string.settings_weather_no_web_search_warning),
                        modifier =
                            Modifier.padding(
                                horizontal = DesignTokens.SpacingLarge,
                                vertical = DesignTokens.SpacingMedium,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            if (showNameInput) {
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
            }

            if (showPromptInput) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    Text(
                        text = stringResource(R.string.settings_custom_tool_prompt_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.settings_custom_tool_prompt_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        colors = dialogTextFieldColors(),
                    )
                }
            }

            if (showLocationInput) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    Text(
                        text = stringResource(R.string.weather_location_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedTextField(
                        value = locationInput,
                        onValueChange = { locationInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.weather_location_hint)) },
                        singleLine = true,
                        maxLines = 1,
                        colors = dialogTextFieldColors(),
                    )
                }
            }

            if (showWeatherUnitInputs) {
                WeatherUnitSettingsSection(
                    temperatureUnit = temperatureUnit,
                    onTemperatureUnitSelected = { temperatureUnit = it },
                )
            }

            if (showAliasInput) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    Text(
                        text = stringResource(R.string.settings_custom_tool_alias_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedTextField(
                        value = aliasInput,
                        onValueChange = { aliasInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.settings_custom_tool_alias_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        singleLine = true,
                        maxLines = 1,
                        colors = dialogTextFieldColors(),
                    )
                }
            }

            ModelFeatureSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                selectedModelId = selectedModelId,
                selectedProviderId = selectedProviderInput,
                availableModels = selectedProviderModels,
                availableModelsByProvider = availableModelsByProvider,
                configuredProviderIds = configuredProviderIds,
                modelLabel = stringResource(R.string.settings_direct_search_model_label),
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
                .padding(
                    horizontal = DesignTokens.ContentHorizontalPadding,
                    vertical = DesignTokens.SpacingMedium,
                ),
        ) {
            Button(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        }
    }

}

@Composable
private fun WeatherUnitSettingsSection(
    temperatureUnit: WeatherTemperatureUnit,
    onTemperatureUnitSelected: (WeatherTemperatureUnit) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Text(
            text = stringResource(R.string.weather_temperature_unit_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            WeatherTemperatureUnit.entries.forEach { unit ->
                SettingsCheckboxPill(
                    label = stringResource(unit.labelRes),
                    checked = temperatureUnit == unit,
                    onCheckedChange = { onTemperatureUnitSelected(unit) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val WeatherTemperatureUnit.labelRes: Int
    get() =
        when (this) {
            WeatherTemperatureUnit.CELSIUS -> R.string.weather_temperature_unit_celsius
            WeatherTemperatureUnit.FAHRENHEIT -> R.string.weather_temperature_unit_fahrenheit
        }
