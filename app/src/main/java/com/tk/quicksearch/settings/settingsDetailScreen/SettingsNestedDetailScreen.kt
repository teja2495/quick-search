package com.tk.quicksearch.settings.settingsDetailScreen

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
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.deviceSettings.NOTIFICATION_HISTORY_SETTING_ID
import com.tk.quicksearch.settings.tasker.TaskerIntegrationScreen
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.settings.appShortcutsSettings.AppShortcutSource
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenState
import com.tk.quicksearch.settings.shared.SettingsManagementSearchBar
import com.tk.quicksearch.settings.shared.settingsContentWidth
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.settings.NoteDeleteConfirmationDialog
import com.tk.quicksearch.settings.NotesBulkDeleteConfirmationDialog
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.tools.aiSearch.supportsThinkingControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
private val ConfigureApiKeyCardVerticalPadding =
    DesignTokens.CardVerticalPadding + DesignTokens.SpacingSmall

@Composable
internal fun SettingsNestedDetailScreen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    detailType: SettingsDetailType,
    hasUsagePermission: Boolean,
    appShortcutFocusShortcut: StaticShortcut? = null,
    appShortcutFocusPackageName: String? = null,
    isAppShortcutsLoading: Boolean = false,
    appShortcutSources: List<AppShortcutSource> = emptyList(),
    searchTargets: List<SearchTarget> = emptyList(),
    onAppShortcutFocusHandled: () -> Unit = {},
    onNavigateToDetail: (SettingsDetailType) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNicknameRemoved: () -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState =
        androidx.compose.foundation.rememberScrollState(),
) {
    if (!detailType.isNestedDetail()) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val imeBottom = WindowInsets.ime.getBottom(density)
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    var appShortcutsSearchQuery by remember { mutableStateOf("") }
    var appManagementSearchQuery by remember { mutableStateOf("") }
    var calendarEventsSearchQuery by remember { mutableStateOf("") }
    var remindersSearchQuery by remember { mutableStateOf("") }
    var notesSearchQuery by remember { mutableStateOf("") }
    var notificationHistorySearchQuery by remember { mutableStateOf("") }
    var showNotificationHistoryAppFilter by remember { mutableStateOf(false) }
    // The filter menu and search bar only make sense once there is history to act on.
    val notificationHistoryAccessGranted by NotificationHistoryAccess.granted.collectAsState()
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
        amoledThemeEnabled = state.amoledThemeEnabled,
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
                                AiBackedToolConfigId.WEATHER -> R.string.weather_toggle_title
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
                        } else if (
                            detailType == SettingsDetailType.NOTIFICATION_HISTORY &&
                                notificationHistoryAccessGranted == true
                        ) {
                            {
                                IconButton(onClick = { showNotificationHistoryAppFilter = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription =
                                            stringResource(R.string.notification_history_app_filter_title),
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
                AppShortcutsDetailContent(
                    state = state,
                    callbacks = callbacks,
                    context = context,
                    isLoading = isAppShortcutsLoading,
                    searchQuery = appShortcutsSearchQuery,
                    collapseAllTrigger = appShortcutsCollapseAllTrigger,
                    shortcutSources = appShortcutSources,
                    searchTargets = searchTargets,
                    focusShortcut = appShortcutFocusShortcut,
                    focusPackageName = appShortcutFocusPackageName,
                    onFocusHandled = onAppShortcutFocusHandled,
                )
            } else if (detailType == SettingsDetailType.TOOLS) {
                ToolsDetailContent(state, callbacks, context, onNavigateToDetail, scrollState)
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
                CustomToolEditorDetailContent(state, callbacks, context)
            } else if (detailType == SettingsDetailType.REMINDERS) {
                RemindersSettingsSection(
                    searchQuery = remindersSearchQuery,
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
            } else if (detailType == SettingsDetailType.CALENDAR_EVENTS) {
                CalendarEventsSettingsSection(
                    onEventClick = callbacks.onLaunchCalendarEvent,
                    searchQuery = calendarEventsSearchQuery,
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
            } else if (detailType == SettingsDetailType.NOTIFICATION_HISTORY) {
                NotificationHistorySettingsSection(
                    searchQuery = notificationHistorySearchQuery,
                    showAppFilterDialog = showNotificationHistoryAppFilter,
                    onDismissAppFilterDialog = { showNotificationHistoryAppFilter = false },
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
                                onSettingClick = { setting ->
                                    if (setting.id == NOTIFICATION_HISTORY_SETTING_ID) {
                                        onNavigateToDetail(
                                            SettingsDetailType.NOTIFICATION_HISTORY,
                                        )
                                    } else {
                                        callbacks.onLaunchDeviceSetting(setting)
                                    }
                                },
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
                                isWhatsAppBusinessInstalled = state.isWhatsAppBusinessInstalled,
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
                                    activeLlmModel = state.activeLlmModel,
                                    activeLlmGroundingEnabled = state.activeLlmGroundingEnabled,
                                    activeLlmThinkingEnabled = state.activeLlmThinkingEnabled,
                                    activeLlmAvailableModels = state.activeLlmAvailableModels,
                                    availableLlmModelsByProvider = state.availableLlmModelsByProvider,
                                    apiKeyLast4ByProvider = state.llmApiKeyLast4ByProvider,
                                    customAdvancedPayloadByProvider =
                                        state.customLlmAdvancedPayloadByProvider,
                                    onSetPersonalContext = callbacks.onSetPersonalContext,
                                    onSetActiveLlmModel = callbacks.onSetActiveLlmModel,
                                    onSetLlmModel = callbacks.onSetLlmModel,
                                    onSetCustomAdvancedPayload =
                                        callbacks.onSetCustomLlmAdvancedPayload,
                                    onSetActiveLlmGroundingEnabled = callbacks.onSetActiveLlmGroundingEnabled,
                                    onSetActiveLlmThinkingEnabled = callbacks.onSetActiveLlmThinkingEnabled,
                                    onRefreshAvailableLlmModels = callbacks.onRefreshAvailableLlmModels,
                                    showThinkingCheckbox =
                                        supportsThinkingControl(
                                            state.aiSearchLlmProviderId,
                                            state.activeLlmModel,
                                        ),
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
                                isSavingApiKey = state.isSavingLlmApiKey,
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

                        SettingsDetailType.AT_A_GLANCE -> {
                            AtAGlanceSettingsSection(modifier = Modifier.fillMaxWidth())
                        }

                        SettingsDetailType.EDGE_GESTURE -> {
                            EdgeGestureSettingsSection(modifier = Modifier.fillMaxWidth())
                        }

                        SettingsDetailType.FLOATING_BUTTON -> {
                            FloatingButtonSettingsSection(modifier = Modifier.fillMaxWidth())
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
            (detailType == SettingsDetailType.APP_SHORTCUTS && !isAppShortcutsLoading) ||
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
        } else if (
            detailType == SettingsDetailType.NOTIFICATION_HISTORY &&
                notificationHistoryAccessGranted == true
        ) {
            SettingsManagementSearchBar(
                query = notificationHistorySearchQuery,
                onQueryChange = { notificationHistorySearchQuery = it },
                onClear = { notificationHistorySearchQuery = "" },
                placeholder = stringResource(R.string.notification_history_search_hint),
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        } else if (detailType == SettingsDetailType.REMINDERS) {
            CalendarEventsBottomBar(
                query = remindersSearchQuery,
                onQueryChange = { remindersSearchQuery = it },
                onClear = { remindersSearchQuery = "" },
                onNewEvent = ReminderEditorRequests::openNew,
                newItemLabelResId = R.string.reminder_new_title,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        } else if (detailType == SettingsDetailType.CALENDAR_EVENTS) {
            SettingsManagementSearchBar(
                query = calendarEventsSearchQuery,
                onQueryChange = { calendarEventsSearchQuery = it },
                onClear = { calendarEventsSearchQuery = "" },
                modifier = Modifier.align(Alignment.BottomEnd),
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

    }
    }
}
