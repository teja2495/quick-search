package com.tk.quicksearch.settings.toolsSettings

import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.shared.ui.components.AppMessages
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.tools.tasker.TaskerIntegration
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenState
import com.tk.quicksearch.settings.shared.settingsContentWidth
import com.tk.quicksearch.settings.shared.settingsScrollClip
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.supportsThinkingControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.layout.ColumnScope
import android.content.Context

@Composable
internal fun ColumnScope.ToolsDetailContent(
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    context: Context,
    onNavigateToDetail: (SettingsDetailType) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
) {
    val taskerInstalled by produceState(false, context) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getPackageInfo(TaskerIntegration.PACKAGE_NAME, 0)
            }.isSuccess
        }
    }

                Column(
                        modifier =
                                Modifier.settingsContentWidth()
                                        .fillMaxHeight()
                                        .align(Alignment.CenterHorizontally)
                                        .padding(
                                                start = DesignTokens.ContentHorizontalPadding,
                                                end = DesignTokens.ContentHorizontalPadding,
                                                bottom = DesignTokens.SectionTopPadding,
                                        )
                                        .settingsScrollClip(),
                ) {
                    ToolsSettingsSection(
                            toolStates =
                                    ToolSettingsRegistry.definitions.associate { definition ->
                                        val enabled =
                                                definition.booleanSetting?.valueIn(state.booleanSettings)
                                                        ?: when (definition.id) {
                                                            ToolSettingId.CURRENCY_CONVERTER -> state.currencyConverterEnabled
                                                            ToolSettingId.WORD_CLOCK -> state.worldClockEnabled
                                                            ToolSettingId.DICTIONARY -> state.dictionaryEnabled
                                                            ToolSettingId.WEATHER -> state.weatherEnabled
                                                            else -> false
                                                        }
                                        definition.id to
                                                ToolSettingUiState(
                                                        enabled = enabled,
                                                        aliasCode =
                                                                definition.aliasFeatureId
                                                                        ?.let { state.shortcutCodes[it] }
                                                                        .orEmpty(),
                                                )
                                    },
                            hasApiKey = state.hasApiKey,
                            existingShortcuts = state.shortcutCodes,
                            onToolAliasChange = { toolId, code ->
                                val definition =
                                        ToolSettingsRegistry.definitionFor(toolId) ?: return@ToolsSettingsSection
                                definition.aliasFeatureId?.let {
                                    callbacks.onSetSearchSectionAlias(it, code)
                                }
                            },
                            onToolToggle = { toolId, enabled ->
                                val definition =
                                        ToolSettingsRegistry.definitionFor(toolId) ?: return@ToolsSettingsSection
                                if (enabled && definition.requiresLlmApiKey && !state.hasApiKey) {
                                    AppMessages.show(context, context.getString(R.string.currency_converter_requires_gemini_key))
                                    return@ToolsSettingsSection
                                }
                                definition.booleanSetting?.let { setting ->
                                    callbacks.onApplySettingsCommand(SettingsCommand.SetBoolean(setting, enabled))
                                    return@ToolsSettingsSection
                                }
                                when (toolId) {
                                    ToolSettingId.CURRENCY_CONVERTER ->
                                            callbacks.onToggleCurrencyConverter(enabled)
                                    ToolSettingId.WORD_CLOCK -> callbacks.onToggleWorldClock(enabled)
                                    ToolSettingId.WEATHER -> callbacks.onToggleWeather(enabled)
                                    else -> {
                                        val toggleKey =
                                                definition.toggleKey ?: return@ToolsSettingsSection
                                        callbacks.onApplySettingsCommand(SettingsCommand.Toggle(toggleKey, enabled))
                                    }
                                }
                            },
                            onToolInfoClick = { toolId ->
                                val destination =
                                        ToolSettingsRegistry.definitionFor(toolId)?.infoDestination
                                if (destination != null) {
                                    onNavigateToDetail(destination)
                                }
                            },
                            onToolConfigureClick = { toolId ->
                                when (toolId) {
                                    ToolSettingId.WORD_CLOCK -> {
                                        CustomToolNavigationMemory.setPendingAiBackedTool(AiBackedToolConfigId.WORD_CLOCK)
                                        onNavigateToDetail(SettingsDetailType.CUSTOM_TOOL_EDITOR)
                                    }
                                    ToolSettingId.WEATHER -> {
                                        CustomToolNavigationMemory.setPendingAiBackedTool(AiBackedToolConfigId.WEATHER)
                                        onNavigateToDetail(SettingsDetailType.CUSTOM_TOOL_EDITOR)
                                    }
                                    else -> Unit
                                }
                            },
                            onNavigateToLlmApiSetup = {
                                onNavigateToDetail(
                                    if (state.hasApiKey) {
                                        SettingsDetailType.GEMINI_API_CONFIG
                                    } else {
                                        SettingsDetailType.API_KEY_SETUP
                                    },
                                )
                            },
                            showTaskerIntegration = taskerInstalled,
                            onNavigateToTaskerIntegration = {
                                onNavigateToDetail(SettingsDetailType.TASKER_INTEGRATION)
                            },
                            customTools = state.customTools,
                            disabledCustomToolIds = state.disabledCustomToolIds,
                            customToolAliases = state.shortcutCodes,
                            onCustomToolToggle = { toolId, enabled ->
                                callbacks.onToggleCustomTool(toolId, enabled)
                            },
                            onCustomToolAliasChange = { toolId, code ->
                                callbacks.onSetSearchSectionAlias(toolId, code)
                            },
                            onCustomToolClick = { toolId ->
                                CustomToolNavigationMemory.setPendingToolId(toolId)
                                onNavigateToDetail(SettingsDetailType.CUSTOM_TOOL_EDITOR)
                            },
                            onCreateNewTool = {
                                CustomToolNavigationMemory.setPendingToolId(null)
                                onNavigateToDetail(SettingsDetailType.CUSTOM_TOOL_EDITOR)
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            scrollState = scrollState,
                    )
                }
}

@Composable
internal fun ColumnScope.CustomToolEditorDetailContent(
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    context: Context,
) {

                val pendingToolId = remember { CustomToolNavigationMemory.consumePendingToolId() }
                val pendingAiBackedTool = remember { CustomToolNavigationMemory.consumePendingAiBackedTool() }
                val existingTool = remember(pendingToolId, state.customTools) {
                    pendingToolId?.let { id -> state.customTools.firstOrNull { it.id == id } }
                }
                val editorPreferences by produceState<ToolEditorPreferences?>(
                    null, pendingAiBackedTool, context, state.llmApiKeyLast4ByProvider.keys,
                ) {
                    value = withContext(Dispatchers.IO) {
                        val aiToolPreferences = UserAppPreferences(context.applicationContext)
                        val config =
                            pendingAiBackedTool?.let { tool ->
                                when (tool) {
                                    AiBackedToolConfigId.CURRENCY_CONVERTER -> BuiltInToolConfig(
                                        toolId = tool,
                                        title = context.getString(R.string.currency_converter_toggle_title),
                                        modelId = aiToolPreferences.getCurrencyConverterModel(),
                                        providerId = aiToolPreferences.getCurrencyConverterProviderId(),
                                        groundingEnabled = aiToolPreferences.isCurrencyConverterGroundingEnabled(),
                                        thinkingEnabled = aiToolPreferences.isCurrencyConverterThinkingEnabled(),
                                        advancedPayload = aiToolPreferences.getCurrencyConverterAdvancedPayload(),
                                        aliasFeatureId = AliasHandler.CURRENCY_CONVERTER_ALIAS_FEATURE_ID,
                                    )
                                    AiBackedToolConfigId.WORD_CLOCK -> BuiltInToolConfig(
                                        toolId = tool,
                                        title = context.getString(R.string.world_clock_toggle_title),
                                        modelId = aiToolPreferences.getWorldClockModel(),
                                        providerId = aiToolPreferences.getWorldClockProviderId(),
                                        groundingEnabled = aiToolPreferences.isWorldClockGroundingEnabled(),
                                        thinkingEnabled = aiToolPreferences.isWorldClockThinkingEnabled(),
                                        advancedPayload = aiToolPreferences.getWorldClockAdvancedPayload(),
                                        aliasFeatureId = AliasHandler.WORD_CLOCK_ALIAS_FEATURE_ID,
                                    )
                                    AiBackedToolConfigId.DICTIONARY -> BuiltInToolConfig(
                                        toolId = tool,
                                        title = context.getString(R.string.dictionary_toggle_title),
                                        modelId = aiToolPreferences.getDictionaryModel(),
                                        providerId = aiToolPreferences.getDictionaryProviderId(),
                                        groundingEnabled = aiToolPreferences.isDictionaryGroundingEnabled(),
                                        thinkingEnabled = aiToolPreferences.isDictionaryThinkingEnabled(),
                                        advancedPayload = aiToolPreferences.getDictionaryAdvancedPayload(),
                                        aliasFeatureId = AliasHandler.DICTIONARY_ALIAS_FEATURE_ID,
                                    )
                                    AiBackedToolConfigId.WEATHER -> BuiltInToolConfig(
                                        toolId = tool,
                                        title = context.getString(R.string.weather_toggle_title),
                                        modelId = aiToolPreferences.getWeatherModel(),
                                        providerId = aiToolPreferences.getWeatherProviderId(),
                                        groundingEnabled = true,
                                        thinkingEnabled = aiToolPreferences.isWeatherThinkingEnabled(),
                                        advancedPayload = aiToolPreferences.getWeatherAdvancedPayload(),
                                        aliasFeatureId = AliasHandler.WEATHER_ALIAS_FEATURE_ID,
                                        prompt = aiToolPreferences.getWeatherSystemPrompt(),
                                        location = aiToolPreferences.getWeatherLocation(),
                                        temperatureUnit = aiToolPreferences.getWeatherTemperatureUnit(),
                                        windSpeedUnit = aiToolPreferences.getWeatherWindSpeedUnit(),
                                    )
                                }
                            }
                        ToolEditorPreferences(
                            builtInToolConfig = config,
                            thinkingEnabledByProvider = state.llmApiKeyLast4ByProvider.keys.associateWith(
                                aiToolPreferences::isLlmThinkingEnabled,
                            ),
                        )
                    }
                }
                // Create the editor only after its saved values are loaded so its remembered
                // fields never initialize from temporary defaults.
                val loadedPreferences = editorPreferences ?: return
                val builtInToolConfig = loadedPreferences.builtInToolConfig
                val editorTool = remember(existingTool, builtInToolConfig) {
                    existingTool ?: builtInToolConfig?.toCustomTool()
                }
                val existingAlias = remember(existingTool?.id, builtInToolConfig?.aliasFeatureId, state.shortcutCodes) {
                    when {
                        existingTool != null -> existingTool.id.let { state.shortcutCodes[it] }.orEmpty()
                        builtInToolConfig != null -> state.shortcutCodes[builtInToolConfig.aliasFeatureId].orEmpty()
                        else -> ""
                    }
                }
                val shouldAutoFocusTitle = remember(pendingToolId, existingTool?.name) {
                    pendingToolId == null || existingTool?.name?.trim().isNullOrEmpty()
                }
                com.tk.quicksearch.settings.toolsSettings.CustomToolEditorScreen(
                    existingTool = editorTool,
                    existingAlias = existingAlias,
                    existingLocation = builtInToolConfig?.location.orEmpty(),
                    existingTemperatureUnit =
                        builtInToolConfig?.temperatureUnit
                            ?: com.tk.quicksearch.search.data.preferences.WeatherTemperatureUnit.CELSIUS,
                    existingWindSpeedUnit =
                        builtInToolConfig?.windSpeedUnit
                            ?: com.tk.quicksearch.search.data.preferences.WeatherWindSpeedUnit.KILOMETERS_PER_HOUR,
                    selectedProviderId = state.aiSearchLlmProviderId,
                    defaultModelId = state.activeLlmModel,
                    defaultThinkingEnabled = state.activeLlmThinkingEnabled,
                    thinkingEnabledByProvider = loadedPreferences.thinkingEnabledByProvider,
                    availableModels = state.activeLlmAvailableModels,
                    availableModelsByProvider = state.availableLlmModelsByProvider,
                    configuredProviderIds = state.llmApiKeyLast4ByProvider.keys,
                    onRefreshAvailableLlmModels = callbacks.onRefreshAvailableLlmModels,
                    onProviderModelSelected = { _, _ -> },
                    showNameInput = builtInToolConfig == null,
                    showPromptInput = builtInToolConfig == null || builtInToolConfig.toolId == AiBackedToolConfigId.WEATHER,
                    showAliasInput = builtInToolConfig == null || builtInToolConfig.toolId == AiBackedToolConfigId.WEATHER,
                    showLocationInput = builtInToolConfig?.toolId == AiBackedToolConfigId.WEATHER,
                    showWeatherUnitInputs = builtInToolConfig?.toolId == AiBackedToolConfigId.WEATHER,
                    webSearchAlwaysEnabled = builtInToolConfig?.toolId == AiBackedToolConfigId.WEATHER,
                    shouldAutoFocusTitle = builtInToolConfig == null && shouldAutoFocusTitle,
                    onSave = { name, prompt, location, providerId, modelId, groundingEnabled, aliasCode, thinkingEnabled, advancedPayload, advancedPayloadEnabled, temperatureUnit, windSpeedUnit ->
                        if (builtInToolConfig != null) {
                            callbacks.onSetAiToolSettings(
                                builtInToolConfig.toolId,
                                providerId,
                                modelId,
                                groundingEnabled,
                                thinkingEnabled,
                                advancedPayload,
                                advancedPayloadEnabled,
                                prompt,
                                location,
                                temperatureUnit,
                                windSpeedUnit,
                            )
                            if (builtInToolConfig.toolId == AiBackedToolConfigId.WEATHER) {
                                callbacks.onSetSearchSectionAlias(
                                    builtInToolConfig.aliasFeatureId,
                                    aliasCode,
                                )
                            }
                        } else if (existingTool != null) {
                            callbacks.onUpdateCustomTool(existingTool.id, name, prompt, providerId, modelId, groundingEnabled, thinkingEnabled, advancedPayload, advancedPayloadEnabled)
                            callbacks.onSetSearchSectionAlias(existingTool.id, aliasCode)
                        } else {
                            callbacks.onAddCustomTool(name, prompt, providerId, modelId, groundingEnabled, aliasCode, thinkingEnabled, advancedPayload, advancedPayloadEnabled)
                        }
                        callbacks.onBack()
                    },
                    modifier = Modifier
                        .settingsContentWidth()
                        .fillMaxHeight()
                        .align(Alignment.CenterHorizontally),
                )
}

private data class ToolEditorPreferences(
    val builtInToolConfig: BuiltInToolConfig?,
    val thinkingEnabledByProvider: Map<AiSearchLlmProviderId, Boolean>,
)

private data class BuiltInToolConfig(
    val toolId: AiBackedToolConfigId,
    val title: String,
    val modelId: String,
    val providerId: AiSearchLlmProviderId,
    val groundingEnabled: Boolean,
    val thinkingEnabled: Boolean,
    val advancedPayload: Pair<Boolean, String>,
    val aliasFeatureId: String,
    val prompt: String = "builtin",
    val location: String = "",
    val temperatureUnit: com.tk.quicksearch.search.data.preferences.WeatherTemperatureUnit =
        com.tk.quicksearch.search.data.preferences.WeatherTemperatureUnit.CELSIUS,
    val windSpeedUnit: com.tk.quicksearch.search.data.preferences.WeatherWindSpeedUnit =
        com.tk.quicksearch.search.data.preferences.WeatherWindSpeedUnit.KILOMETERS_PER_HOUR,
) {
    fun toCustomTool(): CustomTool =
        CustomTool(
            id = "builtin:${toolId.name.lowercase()}",
            name = title,
            prompt = prompt,
            modelId = modelId,
            providerId = providerId,
            groundingEnabled = groundingEnabled,
            thinkingEnabled = thinkingEnabled,
            advancedPayload = advancedPayload.second,
            advancedPayloadEnabled = advancedPayload.first,
        )
}
