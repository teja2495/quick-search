package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.search.apps.appLock.AppLockGate
import com.tk.quicksearch.reminders.ReminderEditorRequests
import androidx.compose.runtime.collectAsState
import com.tk.quicksearch.search.notificationHistory.NotificationHistoryAccess
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.NOTIFICATION_HISTORY_SETTING_ID
import com.tk.quicksearch.settings.settingsDetailScreen.GesturesSettingsSection
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.settings.tasker.TaskerIntegrationScreen
import com.tk.quicksearch.tools.tasker.TaskerIntegration
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.settings.AppShortcutsSettings.AppShortcutSource
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.settings.shared.SettingsScreenState
import com.tk.quicksearch.settings.shared.SettingsManagementSearchBar
import com.tk.quicksearch.settings.shared.settingsContentWidth
import com.tk.quicksearch.settings.AppShortcutsSettings.AppShortcutsSettingsSection
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.settings.NoteDeleteConfirmationDialog
import com.tk.quicksearch.settings.NotesBulkDeleteConfirmationDialog
import com.tk.quicksearch.settings.settingsDetailScreen.CustomToolNavigationMemory
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
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

                Column(
                        modifier =
                                Modifier.settingsContentWidth()
                                        .fillMaxHeight()
                                        .align(Alignment.CenterHorizontally)
                                        .padding(
                                                start = DesignTokens.ContentHorizontalPadding,
                                                end = DesignTokens.ContentHorizontalPadding,
                                                bottom = DesignTokens.SectionTopPadding,
                                        ),
                ) {
                    ToolsSettingsSection(
                            toolStates =
                                    ToolSettingsRegistry.definitions.associate { definition ->
                                        val enabled =
                                                when (definition.id) {
                                                    ToolSettingId.CALCULATOR -> state.calculatorEnabled
                                                    ToolSettingId.UNIT_CONVERTER ->
                                                            state.unitConverterEnabled
                                                    ToolSettingId.DATE_CALCULATOR ->
                                                            state.dateCalculatorEnabled
                                                    ToolSettingId.COLOR_VISUALIZER ->
                                                            state.colorVisualizerEnabled
                                                    ToolSettingId.CURRENCY_CONVERTER ->
                                                            state.currencyConverterEnabled
                                                    ToolSettingId.WORD_CLOCK -> state.worldClockEnabled
                                                    ToolSettingId.DICTIONARY -> state.dictionaryEnabled
                                                    ToolSettingId.WEATHER -> state.weatherEnabled
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
                                val requiresGeminiApiKey =
                                        ToolSettingsRegistry.definitionFor(toolId)?.requiresGeminiApiKey == true
                                if (enabled && requiresGeminiApiKey && !state.hasApiKey) {
                                    android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(R.string.currency_converter_requires_gemini_key),
                                                    android.widget.Toast.LENGTH_SHORT,
                                            )
                                            .show()
                                    return@ToolsSettingsSection
                                }
                                when (toolId) {
                                    ToolSettingId.COLOR_VISUALIZER ->
                                            callbacks.onToggleColorVisualizer(enabled)
                                    ToolSettingId.CURRENCY_CONVERTER ->
                                            callbacks.onToggleCurrencyConverter(enabled)
                                    ToolSettingId.WORD_CLOCK -> callbacks.onToggleWorldClock(enabled)
                                    ToolSettingId.WEATHER -> callbacks.onToggleWeather(enabled)
                                    else -> {
                                        val definition =
                                                ToolSettingsRegistry.definitionFor(toolId)
                                                        ?: return@ToolsSettingsSection
                                        val toggleKey =
                                                definition.toggleKey ?: return@ToolsSettingsSection
                                        callbacks.onApplySettingsCommand(
                                                SettingsCommand.Toggle(
                                                        key = toggleKey,
                                                        enabled = enabled,
                                                ),
                                        )
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
                                    ToolSettingId.DICTIONARY -> {
                                        CustomToolNavigationMemory.setPendingAiBackedTool(AiBackedToolConfigId.DICTIONARY)
                                        onNavigateToDetail(SettingsDetailType.CUSTOM_TOOL_EDITOR)
                                    }
                                    ToolSettingId.WEATHER -> {
                                        CustomToolNavigationMemory.setPendingAiBackedTool(AiBackedToolConfigId.WEATHER)
                                        onNavigateToDetail(SettingsDetailType.CUSTOM_TOOL_EDITOR)
                                    }
                                    else -> Unit
                                }
                            },
                            onNavigateToGeminiApiSetup = {
                                onNavigateToDetail(
                                    if (state.hasApiKey) {
                                        SettingsDetailType.GEMINI_API_CONFIG
                                    } else {
                                        SettingsDetailType.API_KEY_SETUP
                                    },
                                )
                            },
                            showTaskerIntegration = remember(context) {
                                runCatching {
                                    context.packageManager.getPackageInfo(TaskerIntegration.PACKAGE_NAME, 0)
                                }.isSuccess
                            },
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
                val aiToolPreferences =
                    remember(context) { UserAppPreferences(context.applicationContext) }
                val existingTool = remember(pendingToolId, state.customTools) {
                    pendingToolId?.let { id -> state.customTools.firstOrNull { it.id == id } }
                }
                val builtInToolConfig = remember(pendingAiBackedTool, context) {
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
                                groundingEnabled = aiToolPreferences.isWeatherGroundingEnabled(),
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
                }
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
                com.tk.quicksearch.settings.customTools.CustomToolEditorScreen(
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
                    defaultModelId = state.geminiModel,
                    defaultThinkingEnabled = state.geminiThinkingEnabled,
                    thinkingEnabledByProvider =
                        state.llmApiKeyLast4ByProvider.keys.associateWith(
                            aiToolPreferences::isLlmThinkingEnabled,
                        ),
                    availableModels = state.availableGeminiModels,
                    availableModelsByProvider = state.availableLlmModelsByProvider,
                    configuredProviderIds = state.llmApiKeyLast4ByProvider.keys,
                    onRefreshAvailableGeminiModels = callbacks.onRefreshAvailableGeminiModels,
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
