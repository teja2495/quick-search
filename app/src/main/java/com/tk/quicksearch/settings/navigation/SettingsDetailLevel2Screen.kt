package com.tk.quicksearch.settings.settingsDetailScreen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
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
import com.tk.quicksearch.search.data.CustomCalendarEventRepository
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.UserAppPreferences
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ConfigureApiKeyCardVerticalPadding =
    DesignTokens.CardVerticalPadding + DesignTokens.SpacingSmall

@Composable
internal fun SettingsDetailLevel2Screen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    detailType: SettingsDetailType,
    hasUsagePermission: Boolean,
    appShortcutFocusShortcut: StaticShortcut? = null,
    appShortcutFocusPackageName: String? = null,
    appShortcutSources: List<AppShortcutSource> = emptyList(),
    searchTargets: List<SearchTarget> = emptyList(),
    onAppShortcutFocusHandled: () -> Unit = {},
    onNavigateToDetail: (SettingsDetailType) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNicknameRemoved: () -> Unit = {},
) {
    if (!detailType.isLevel2()) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val imeBottom = WindowInsets.ime.getBottom(density)
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    var appShortcutsSearchQuery by remember { mutableStateOf("") }
    var appManagementSearchQuery by remember { mutableStateOf("") }
    var calendarEventsSearchQuery by remember { mutableStateOf("") }
    var calendarEventsRefreshSignal by remember { mutableIntStateOf(0) }
    var showCreateCalendarEventDialog by remember { mutableStateOf(false) }
    var notesSearchQuery by remember { mutableStateOf("") }
    var notesMultiSelectActive by remember { mutableStateOf(false) }
    var notesSelectedIds by remember { mutableStateOf(setOf<Long>()) }
    var notesRefreshSignal by remember { mutableIntStateOf(0) }
    var showNotesBulkDeleteConfirm by remember { mutableStateOf(false) }
    var appShortcutsCollapseAllTrigger by remember { mutableIntStateOf(0) }
    var noteEditorCanDelete by remember { mutableStateOf(false) }
    var noteEditorOnConfirmedDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showNoteDeleteConfirm by remember { mutableStateOf(false) }
    val pendingAiBackedToolForEditor =
        remember(detailType) {
            if (detailType == SettingsDetailType.CUSTOM_TOOL_EDITOR) {
                CustomToolNavigationMemory.peekPendingAiBackedTool()
            } else {
                null
            }
        }
    val hideNoteEditorAppBar =
        remember(detailType) {
            if (detailType == SettingsDetailType.NOTE_EDITOR) {
                NotesNavigationMemory.consumeHideEditorAppBarRequest()
            } else {
                false
            }
        }
    val notesEnabled = FeatureFlags.isSearchSectionEnabled(SearchSection.NOTES)

    if (!notesEnabled &&
        (detailType == SettingsDetailType.NOTES || detailType == SettingsDetailType.NOTE_EDITOR)
    ) {
        LaunchedEffect(detailType) {
            callbacks.onBack()
        }
        return
    }

    val hasExcludedItems =
        state.suggestionExcludedApps.isNotEmpty() ||
            state.resultExcludedApps.isNotEmpty() ||
            state.excludedContacts.isNotEmpty() ||
            state.excludedFiles.isNotEmpty() ||
            state.excludedFileExtensions.isNotEmpty() ||
            state.excludedSettings.isNotEmpty() ||
            state.excludedAppShortcuts.isNotEmpty()

    LaunchedEffect(detailType, hasExcludedItems) {
        if (detailType == SettingsDetailType.EXCLUDED_ITEMS && !hasExcludedItems) {
            callbacks.onBack()
        }
    }
    LaunchedEffect(detailType) {
        if (detailType != SettingsDetailType.NOTE_EDITOR) {
            noteEditorCanDelete = false
            noteEditorOnConfirmedDelete = null
            showNoteDeleteConfirm = false
        }
        if (detailType != SettingsDetailType.NOTES) {
            notesMultiSelectActive = false
            notesSelectedIds = emptySet()
            showNotesBulkDeleteConfirm = false
        }
    }
    LaunchedEffect(detailType, imeBottom) {
        if ((detailType == SettingsDetailType.GEMINI_API_CONFIG || detailType == SettingsDetailType.API_KEY_SETUP) && imeBottom > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    BackHandler {
        if (detailType == SettingsDetailType.NOTES && notesMultiSelectActive) {
            notesMultiSelectActive = false
            notesSelectedIds = emptySet()
        } else {
            callbacks.onBack()
        }
    }
    SettingsScreenBackground(
        appTheme = state.appTheme,
        overlayThemeIntensity = state.overlayThemeIntensity,
        deviceThemeEnabled = state.deviceThemeEnabled,
        modifier = modifier,
    ) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!(detailType == SettingsDetailType.NOTE_EDITOR && hideNoteEditorAppBar)) {
                SettingsDetailHeader(
                    title = stringResource(
                        if (detailType == SettingsDetailType.CUSTOM_TOOL_EDITOR) {
                            when (pendingAiBackedToolForEditor) {
                                AiBackedToolConfigId.CURRENCY_CONVERTER -> R.string.currency_converter_toggle_title
                                AiBackedToolConfigId.WORD_CLOCK -> R.string.world_clock_toggle_title
                                AiBackedToolConfigId.DICTIONARY -> R.string.dictionary_toggle_title
                                null -> detailType.titleResId()
                            }
                        } else {
                            detailType.titleResId()
                        }
                    ),
                    onBack = {
                        if (detailType == SettingsDetailType.NOTES && notesMultiSelectActive) {
                            notesMultiSelectActive = false
                            notesSelectedIds = emptySet()
                        } else {
                            callbacks.onBack()
                        }
                    },
                    trailingContent =
                        if (detailType == SettingsDetailType.NOTE_EDITOR && noteEditorCanDelete) {
                            {
                                IconButton(onClick = { showNoteDeleteConfirm = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription =
                                            stringResource(R.string.notes_delete_note_desc),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        } else if (detailType == SettingsDetailType.CUSTOM_TOOL_EDITOR && pendingAiBackedToolForEditor == null) {
                            val pendingToolIdForHeader = remember { CustomToolNavigationMemory.peekPendingToolId() }
                            val existingToolForHeader = remember(pendingToolIdForHeader, state.customTools) {
                                pendingToolIdForHeader?.let { id -> state.customTools.firstOrNull { it.id == id } }
                            }
                            if (existingToolForHeader != null) {
                                {
                                    IconButton(onClick = {
                                        callbacks.onDeleteCustomTool(existingToolForHeader.id)
                                        callbacks.onBack()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = stringResource(R.string.settings_custom_tool_delete_button),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            } else {
                                null
                            }
                        } else {
                            null
                        },
                )
            }

            if (detailType == SettingsDetailType.APP_MANAGEMENT) {
                AppManagementSettingsSection(
                    apps = state.allApps,
                    hasUsagePermission = hasUsagePermission,
                    iconPackPackage = state.selectedIconPackPackage,
                    searchQuery = appManagementSearchQuery,
                    onRequestAppUninstall = callbacks.onRequestAppUninstall,
                    onOpenAppInfo = callbacks.onOpenAppInfo,
                    onRefreshApps = callbacks.onRefreshApps,
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom = 96.dp,
                            ),
                )
            } else if (detailType == SettingsDetailType.APP_SHORTCUTS) {
                AppShortcutsSettingsSection(
                    shortcuts = state.allAppShortcuts,
                    disabledShortcutIds = state.disabledAppShortcutIds,
                    iconPackPackage = state.selectedIconPackPackage,
                    searchQuery = appShortcutsSearchQuery,
                    collapseAllTrigger = appShortcutsCollapseAllTrigger,
                    onShortcutEnabledChange = callbacks.onToggleAppShortcutEnabled,
                    onShortcutNameClick = callbacks.onLaunchAppShortcut,
                    shortcutSources = appShortcutSources,
                    onAddShortcutFromSource = callbacks.onAddAppShortcutFromSource,
                    onAddAppDeepLinkShortcut = callbacks.onAddAppDeepLinkShortcut,
                    searchTargets = searchTargets,
                    onAddQueryShortcut = callbacks.onAddSearchTargetQueryShortcut,
                    onUpdateCustomShortcut = callbacks.onUpdateCustomAppShortcut,
                    onDeleteCustomShortcut = callbacks.onDeleteCustomAppShortcut,
                    focusShortcut = appShortcutFocusShortcut,
                    focusPackageName = appShortcutFocusPackageName,
                    onFocusHandled = onAppShortcutFocusHandled,
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom = 96.dp,
                            ),
                )
            } else if (detailType == SettingsDetailType.TOOLS) {
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
                                                    ToolSettingId.CURRENCY_CONVERTER ->
                                                            state.currencyConverterEnabled
                                                    ToolSettingId.WORD_CLOCK -> state.worldClockEnabled
                                                    ToolSettingId.DICTIONARY -> state.dictionaryEnabled
                                                }
                                        definition.id to
                                                ToolSettingUiState(
                                                        enabled = enabled,
                                                        aliasCode =
                                                                state.shortcutCodes[
                                                                                definition
                                                                                        .aliasFeatureId
                                                                        ]
                                                                        .orEmpty(),
                                                )
                                    },
                            hasApiKey = state.hasApiKey,
                            existingShortcuts = state.shortcutCodes,
                            onToolAliasChange = { toolId, code ->
                                val definition =
                                        ToolSettingsRegistry.definitionFor(toolId) ?: return@ToolsSettingsSection
                                callbacks.onSetSearchSectionAlias(definition.aliasFeatureId, code)
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
                                    ToolSettingId.CURRENCY_CONVERTER ->
                                            callbacks.onToggleCurrencyConverter(enabled)
                                    ToolSettingId.WORD_CLOCK -> callbacks.onToggleWorldClock(enabled)
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
                                    ToolSettingId.CURRENCY_CONVERTER -> {
                                        CustomToolNavigationMemory.setPendingAiBackedTool(AiBackedToolConfigId.CURRENCY_CONVERTER)
                                        onNavigateToDetail(SettingsDetailType.CUSTOM_TOOL_EDITOR)
                                    }
                                    ToolSettingId.WORD_CLOCK -> {
                                        CustomToolNavigationMemory.setPendingAiBackedTool(AiBackedToolConfigId.WORD_CLOCK)
                                        onNavigateToDetail(SettingsDetailType.CUSTOM_TOOL_EDITOR)
                                    }
                                    ToolSettingId.DICTIONARY -> {
                                        CustomToolNavigationMemory.setPendingAiBackedTool(AiBackedToolConfigId.DICTIONARY)
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
                    )
                }
            } else if (detailType == SettingsDetailType.TASKER_INTEGRATION) {
                TaskerIntegrationScreen(
                    tools = state.taskerIntentTools,
                    existingAliases = state.shortcutCodes,
                    onAdd = callbacks.onAddTaskerIntentTool,
                    onDelete = callbacks.onDeleteTaskerIntentTool,
                    modifier = Modifier
                        .settingsContentWidth()
                        .fillMaxHeight()
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            start = DesignTokens.ContentHorizontalPadding,
                            end = DesignTokens.ContentHorizontalPadding,
                            bottom = DesignTokens.SectionTopPadding,
                        ),
                )
            } else if (detailType == SettingsDetailType.CUSTOM_TOOL_EDITOR) {
                val pendingToolId = remember { CustomToolNavigationMemory.consumePendingToolId() }
                val pendingAiBackedTool = remember { CustomToolNavigationMemory.consumePendingAiBackedTool() }
                val existingTool = remember(pendingToolId, state.customTools) {
                    pendingToolId?.let { id -> state.customTools.firstOrNull { it.id == id } }
                }
                val builtInToolConfig = remember(pendingAiBackedTool, context) {
                    pendingAiBackedTool?.let { tool ->
                        val preferences = UserAppPreferences(context)
                        when (tool) {
                            AiBackedToolConfigId.CURRENCY_CONVERTER -> BuiltInToolConfig(
                                toolId = tool,
                                title = context.getString(R.string.currency_converter_toggle_title),
                                modelId = preferences.getCurrencyConverterModel(),
                                providerId = preferences.getCurrencyConverterProviderId(),
                                groundingEnabled = preferences.isCurrencyConverterGroundingEnabled(),
                                thinkingEnabled = preferences.isCurrencyConverterThinkingEnabled(),
                                advancedPayload = preferences.getCurrencyConverterAdvancedPayload(),
                                aliasFeatureId = AliasHandler.CURRENCY_CONVERTER_ALIAS_FEATURE_ID,
                            )
                            AiBackedToolConfigId.WORD_CLOCK -> BuiltInToolConfig(
                                toolId = tool,
                                title = context.getString(R.string.world_clock_toggle_title),
                                modelId = preferences.getWorldClockModel(),
                                providerId = preferences.getWorldClockProviderId(),
                                groundingEnabled = preferences.isWorldClockGroundingEnabled(),
                                thinkingEnabled = preferences.isWorldClockThinkingEnabled(),
                                advancedPayload = preferences.getWorldClockAdvancedPayload(),
                                aliasFeatureId = AliasHandler.WORD_CLOCK_ALIAS_FEATURE_ID,
                            )
                            AiBackedToolConfigId.DICTIONARY -> BuiltInToolConfig(
                                toolId = tool,
                                title = context.getString(R.string.dictionary_toggle_title),
                                modelId = preferences.getDictionaryModel(),
                                providerId = preferences.getDictionaryProviderId(),
                                groundingEnabled = preferences.isDictionaryGroundingEnabled(),
                                thinkingEnabled = preferences.isDictionaryThinkingEnabled(),
                                advancedPayload = preferences.getDictionaryAdvancedPayload(),
                                aliasFeatureId = AliasHandler.DICTIONARY_ALIAS_FEATURE_ID,
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
                    selectedProviderId = state.aiSearchLlmProviderId,
                    availableModels = state.availableGeminiModels,
                    availableModelsByProvider = state.availableLlmModelsByProvider,
                    configuredProviderIds = state.llmApiKeyLast4ByProvider.keys,
                    onRefreshAvailableGeminiModels = callbacks.onRefreshAvailableGeminiModels,
                    onProviderModelSelected = { _, _ -> },
                    showNameInput = builtInToolConfig == null,
                    showPromptInput = builtInToolConfig == null,
                    showAliasInput = builtInToolConfig == null,
                    shouldAutoFocusTitle = builtInToolConfig == null && shouldAutoFocusTitle,
                    onSave = { name, prompt, providerId, modelId, groundingEnabled, aliasCode, thinkingEnabled, advancedPayload, advancedPayloadEnabled ->
                        if (builtInToolConfig != null) {
                            callbacks.onSetAiToolSettings(
                                builtInToolConfig.toolId,
                                providerId,
                                modelId,
                                groundingEnabled,
                                thinkingEnabled,
                                advancedPayload,
                                advancedPayloadEnabled,
                            )
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
            } else if (detailType == SettingsDetailType.CALENDAR_EVENTS) {
                CalendarEventsSettingsSection(
                    onEventClick = callbacks.onLaunchCalendarEvent,
                    searchQuery = calendarEventsSearchQuery,
                    refreshSignal = calendarEventsRefreshSignal,
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom = 96.dp,
                            ),
                )
            } else if (detailType == SettingsDetailType.NOTES) {
                NotesSettingsSection(
                    searchQuery = notesSearchQuery,
                    onOpenNoteEditor = { noteId ->
                        NotesNavigationMemory.setPendingNoteId(noteId)
                        onNavigateToDetail(SettingsDetailType.NOTE_EDITOR)
                    },
                    multiSelectActive = notesMultiSelectActive,
                    selectedNoteIds = notesSelectedIds,
                    onEnterMultiSelect = { noteId ->
                        notesMultiSelectActive = true
                        notesSelectedIds = notesSelectedIds + noteId
                    },
                    onToggleNoteSelected = { noteId ->
                        notesSelectedIds =
                            if (noteId in notesSelectedIds) {
                                notesSelectedIds - noteId
                            } else {
                                notesSelectedIds + noteId
                            }
                    },
                    notesRefreshSignal = notesRefreshSignal,
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom = 96.dp,
                            ),
                )
            } else if (detailType == SettingsDetailType.NOTE_EDITOR) {
                NoteEditor(
                    onNavigateToNotes = { onNavigateToDetail(SettingsDetailType.NOTES) },
                    onNavigateToSearch = onNavigateToSearch,
                    onDeleteToolbarState = { canDelete, onConfirmedDelete ->
                        noteEditorCanDelete = canDelete
                        noteEditorOnConfirmedDelete =
                            if (canDelete) {
                                onConfirmedDelete
                            } else {
                                null
                            }
                    },
                    hideTopBar = hideNoteEditorAppBar,
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                            ),
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .verticalScroll(scrollState)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom =
                                    if (detailType == SettingsDetailType.APP_SHORTCUTS) {
                                        96.dp
                                    } else {
                                        DesignTokens.SectionTopPadding
                                    },
                            ),
                ) {
                    when (detailType) {
                        SettingsDetailType.EXCLUDED_ITEMS -> {
                            ExcludedItemScreen(
                                suggestionExcludedApps = state.suggestionExcludedApps,
                                resultExcludedApps = state.resultExcludedApps,
                                excludedContacts = state.excludedContacts,
                                excludedFiles = state.excludedFiles,
                                excludedFileExtensions = state.excludedFileExtensions,
                                excludedSettings = state.excludedSettings,
                                excludedAppShortcuts = state.excludedAppShortcuts,
                                onRemoveSuggestionExcludedApp = callbacks.onRemoveSuggestionExcludedApp,
                                onRemoveResultExcludedApp = callbacks.onRemoveResultExcludedApp,
                                onRemoveExcludedContact = callbacks.onRemoveExcludedContact,
                                onRemoveExcludedFile = callbacks.onRemoveExcludedFile,
                                onRemoveExcludedFileExtension = callbacks.onRemoveExcludedFileExtension,
                                onRemoveExcludedSetting = callbacks.onRemoveExcludedSetting,
                                onRemoveExcludedAppShortcut = callbacks.onRemoveExcludedAppShortcut,
                                showTitle = false,
                                iconPackPackage = state.selectedIconPackPackage,
                            )
                        }

                        SettingsDetailType.APP_SHORTCUTS -> Unit
                        SettingsDetailType.DEVICE_SETTINGS -> {
                            DeviceSettingsSettingsSection(
                                settings = state.allDeviceSettings,
                                onSettingClick = callbacks.onLaunchDeviceSetting,
                            )
                        }

                        SettingsDetailType.CALLS_TEXTS -> {
                            CallsTextsSettingsSection(
                                messagingApp = state.messagingApp,
                                callingApp = state.callingApp,
                                onSetMessagingApp = callbacks.onSetMessagingApp,
                                onSetCallingApp = callbacks.onSetCallingApp,
                                directDialEnabled = state.directDialEnabled,
                                onToggleDirectDial = callbacks.onToggleDirectDial,
                                numberSearchEnabled = state.numberSearchEnabled,
                                onToggleNumberSearch = callbacks.onToggleNumberSearch,
                                hasCallPermission = PermissionHelper.checkCallPermission(context),
                                contactsSectionEnabled = true,
                                isWhatsAppInstalled = state.isWhatsAppInstalled,
                                isTelegramInstalled = state.isTelegramInstalled,
                                isSignalInstalled = state.isSignalInstalled,
                                isGoogleMeetInstalled = state.isGoogleMeetInstalled,
                                modifier = Modifier,
                            )
                        }

                        SettingsDetailType.FILES -> {
                            FileTypesSection(
                                enabledFileTypes = state.enabledFileTypes,
                                onToggleFileType = callbacks.onToggleFileType,
                                showFolders = state.showFolders,
                                onToggleFolders = { enabled ->
                                    callbacks.onApplySettingsCommand(
                                        SettingsCommand.Toggle(
                                            key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.SHOW_FOLDERS,
                                            enabled = enabled,
                                        ),
                                    )
                                },
                                filePreviewsEnabled = state.filePreviewsEnabled,
                                onToggleFilePreviews = callbacks.onToggleFilePreviews,
                                showSystemFiles = state.showSystemFiles,
                                onToggleSystemFiles = { enabled ->
                                    callbacks.onApplySettingsCommand(
                                        SettingsCommand.Toggle(
                                            key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.SHOW_SYSTEM_FILES,
                                            enabled = enabled,
                                        ),
                                    )
                                },
                                folderWhitelistPatterns = state.folderWhitelistPatterns,
                                onSetFolderWhitelistPatterns = callbacks.onSetFolderWhitelistPatterns,
                                folderBlacklistPatterns = state.folderBlacklistPatterns,
                                onSetFolderBlacklistPatterns = callbacks.onSetFolderBlacklistPatterns,
                                excludedExtensions = state.excludedFileExtensions,
                                onRemoveExcludedExtension = callbacks.onRemoveExcludedFileExtension,
                                showTitle = false,
                                modifier = Modifier,
                            )
                        }

                        SettingsDetailType.GEMINI_API_CONFIG -> {
                            SettingsCard(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = DesignTokens.SpacingLarge),
                            ) {
                                SettingsNavigationRow(
                                    item =
                                        SettingsCardItem(
                                            title = stringResource(R.string.settings_configure_api_key_title),
                                            description = stringResource(R.string.settings_api_key_setup_nav_desc),
                                            iconResId = R.drawable.direct_search,
                                            actionOnPress = {
                                                onNavigateToDetail(SettingsDetailType.API_KEY_SETUP)
                                            },
                                        ),
                                    contentPadding = PaddingValues(
                                        horizontal = DesignTokens.CardHorizontalPadding,
                                        vertical = ConfigureApiKeyCardVerticalPadding,
                                    ),
                                )
                            }

                            if (state.hasApiKey) {
                                AiProviderSettingsSection(
                                    personalContext = state.personalContext,
                                    aiSearchLlmProviderId = state.aiSearchLlmProviderId,
                                    geminiModel = state.geminiModel,
                                    geminiGroundingEnabled = state.geminiGroundingEnabled,
                                    geminiThinkingEnabled = state.geminiThinkingEnabled,
                                    availableGeminiModels = state.availableGeminiModels,
                                    availableLlmModelsByProvider = state.availableLlmModelsByProvider,
                                    apiKeyLast4ByProvider = state.llmApiKeyLast4ByProvider,
                                    customAdvancedPayloadByProvider =
                                        state.customLlmAdvancedPayloadByProvider,
                                    onSetPersonalContext = callbacks.onSetPersonalContext,
                                    onSetGeminiModel = callbacks.onSetGeminiModel,
                                    onSetLlmModel = callbacks.onSetLlmModel,
                                    onSetCustomAdvancedPayload =
                                        callbacks.onSetCustomLlmAdvancedPayload,
                                    onSetGeminiGroundingEnabled = callbacks.onSetGeminiGroundingEnabled,
                                    onSetGeminiThinkingEnabled = callbacks.onSetGeminiThinkingEnabled,
                                    onRefreshAvailableGeminiModels = callbacks.onRefreshAvailableGeminiModels,
                                    showGroundingCheckbox =
                                        state.aiSearchLlmProviderId != AiSearchLlmProviderId.OPENAI &&
                                            !state.aiSearchLlmProviderId.isCustom &&
                                            state.aiSearchLlmProviderId != AiSearchLlmProviderId.GROQ,
                                    showThinkingCheckbox =
                                        state.aiSearchLlmProviderId != AiSearchLlmProviderId.OPENAI &&
                                            !state.aiSearchLlmProviderId.isCustom,
                                    onRequestScrollToBottom = {
                                        coroutineScope.launch {
                                            scrollState.scrollTo(scrollState.maxValue)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        SettingsDetailType.API_KEY_SETUP -> {
                            ApiKeySetupScreen(
                                apiKeyLast4ByProvider = state.llmApiKeyLast4ByProvider,
                                customProviderBaseUrlByProvider = state.customLlmBaseUrlByProvider,
                                isSavingApiKey = state.isSavingGeminiApiKey,
                                onSetApiKey = callbacks.onSetLlmApiKey,
                                onAddCustomProvider = callbacks.onAddCustomLlmProvider,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        SettingsDetailType.TOOLS -> Unit

                        SettingsDetailType.NICKNAMES -> {
                            NicknameItemsScreen(
                                modifier = Modifier.fillMaxWidth(),
                                onAfterRemove = onNicknameRemoved,
                            )
                        }

                        SettingsDetailType.TRIGGERS -> {
                            TriggerItemsScreen(modifier = Modifier.fillMaxWidth())
                        }

                        SettingsDetailType.GESTURES -> {
                            GesturesSettingsSection(modifier = Modifier.fillMaxWidth())
                        }

                        SettingsDetailType.UNIT_CONVERTER_INFO -> {
                            UnitConverterInfoSection(modifier = Modifier.fillMaxWidth())
                        }

                        SettingsDetailType.DATE_CALCULATOR_INFO -> {
                            DateCalculatorInfoSection(modifier = Modifier.fillMaxWidth())
                        }

                        else -> Unit
                    }
                }
            }
        }

        if (detailType == SettingsDetailType.EXCLUDED_ITEMS) {
            FloatingActionButton(
                onClick = { showClearAllConfirmation = true },
                modifier =
                    Modifier
                        .align(androidx.compose.ui.Alignment.BottomEnd)
                        .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.settings_action_clear_all),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        if (detailType == SettingsDetailType.NOTES) {
            NotesSettingsBottomBar(
                query = notesSearchQuery,
                onQueryChange = { notesSearchQuery = it },
                onClear = { notesSearchQuery = "" },
                onNewNote = {
                    NotesNavigationMemory.setPendingNoteId(null)
                    onNavigateToDetail(SettingsDetailType.NOTE_EDITOR)
                },
                multiSelectActive = notesMultiSelectActive,
                selectedNoteCount = notesSelectedIds.size,
                onDeleteSelected = { showNotesBulkDeleteConfirm = true },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
            )
        } else if (
            detailType == SettingsDetailType.APP_SHORTCUTS ||
                detailType == SettingsDetailType.APP_MANAGEMENT
        ) {
            val query =
                when (detailType) {
                    SettingsDetailType.APP_SHORTCUTS -> appShortcutsSearchQuery
                    SettingsDetailType.APP_MANAGEMENT -> appManagementSearchQuery
                    else -> ""
                }
            SettingsManagementSearchBar(
                query = query,
                onQueryChange = { updatedQuery ->
                    when (detailType) {
                        SettingsDetailType.APP_SHORTCUTS -> appShortcutsSearchQuery = updatedQuery
                        SettingsDetailType.APP_MANAGEMENT -> appManagementSearchQuery = updatedQuery
                        else -> Unit
                    }
                },
                onClear = {
                    when (detailType) {
                        SettingsDetailType.APP_SHORTCUTS -> {
                            appShortcutsCollapseAllTrigger++
                            appShortcutsSearchQuery = ""
                        }
                        SettingsDetailType.APP_MANAGEMENT -> appManagementSearchQuery = ""
                        else -> Unit
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        } else if (detailType == SettingsDetailType.CALENDAR_EVENTS) {
            CalendarEventsBottomBar(
                query = calendarEventsSearchQuery,
                onQueryChange = { calendarEventsSearchQuery = it },
                onClear = { calendarEventsSearchQuery = "" },
                onNewEvent = { showCreateCalendarEventDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        }

        if (showClearAllConfirmation && detailType == SettingsDetailType.EXCLUDED_ITEMS) {
            ClearAllConfirmationDialog(
                onConfirm = {
                    callbacks.onClearAllExclusions()
                    showClearAllConfirmation = false
                },
                onDismiss = { showClearAllConfirmation = false },
            )
        }

        if (showNoteDeleteConfirm && detailType == SettingsDetailType.NOTE_EDITOR) {
            NoteDeleteConfirmationDialog(
                onConfirm = {
                    noteEditorOnConfirmedDelete?.invoke()
                    showNoteDeleteConfirm = false
                },
                onDismiss = { showNoteDeleteConfirm = false }
            )
        }

        if (showNotesBulkDeleteConfirm && detailType == SettingsDetailType.NOTES) {
            NotesBulkDeleteConfirmationDialog(
                selectedCount = notesSelectedIds.size,
                onConfirm = {
                    val ids = notesSelectedIds.toList()
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            val context1 = context // captured
                            val repository = NotesRepository(context1)
                            ids.forEach { id ->
                                repository.stageDelete(id)
                                repository.finalizeDelete(id)
                            }
                        }
                        notesRefreshSignal++
                        notesMultiSelectActive = false
                        notesSelectedIds = emptySet()
                        showNotesBulkDeleteConfirm = false
                    }
                },
                onDismiss = { showNotesBulkDeleteConfirm = false }
            )
        }

        if (showCreateCalendarEventDialog && detailType == SettingsDetailType.CALENDAR_EVENTS) {
            CreateCalendarEventDialog(
                onDismiss = { showCreateCalendarEventDialog = false },
                onConfirm = { title, dateTimeMillis, allDay ->
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            CustomCalendarEventRepository(context).createCustomEvent(title, dateTimeMillis, allDay)
                        }
                        calendarEventsRefreshSignal++
                        showCreateCalendarEventDialog = false
                    }
                },
            )
        }
    }
    }
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
) {
    fun toCustomTool(): CustomTool =
        CustomTool(
            id = "builtin:${toolId.name.lowercase()}",
            name = title,
            prompt = "builtin",
            modelId = modelId,
            providerId = providerId,
            groundingEnabled = groundingEnabled,
            thinkingEnabled = thinkingEnabled,
            advancedPayload = advancedPayload.second,
            advancedPayloadEnabled = advancedPayload.first,
        )
}
