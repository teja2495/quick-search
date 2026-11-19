package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.settings.calendarSettings.CalendarEventsBottomBar
import com.tk.quicksearch.settings.calendarSettings.CalendarEventsHeaderMenu
import com.tk.quicksearch.settings.calendarSettings.CalendarEventsSettingsSection
import com.tk.quicksearch.settings.notesSettings.NoteEditor
import com.tk.quicksearch.settings.notesSettings.NoteEditorToolbarState
import com.tk.quicksearch.settings.notesSettings.NotesNavigationMemory
import com.tk.quicksearch.settings.notesSettings.NotesSettingsBottomBar
import com.tk.quicksearch.settings.notesSettings.NotesSettingsSection
import com.tk.quicksearch.settings.notesSettings.noteEditorHeaderActions
import com.tk.quicksearch.settings.notesSettings.noteEditorTitleRes
import com.tk.quicksearch.settings.remindersSettings.RemindersSettingsSection
import com.tk.quicksearch.settings.toolsSettings.AiBackedToolConfigId
import com.tk.quicksearch.settings.toolsSettings.CustomToolEditorDetailContent
import com.tk.quicksearch.settings.toolsSettings.CustomToolNavigationMemory
import com.tk.quicksearch.settings.toolsSettings.ToolsDetailContent
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppFabTone
import com.tk.quicksearch.shared.ui.components.AppFloatingActionButton
import com.tk.quicksearch.reminders.ReminderEditorRequests
import androidx.compose.runtime.collectAsState
import com.tk.quicksearch.search.notificationHistory.NotificationHistoryAccess
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import com.tk.quicksearch.app.navigation.SettingsPageBackHandler
import com.tk.quicksearch.shared.ui.components.predictiveBackPreview
import com.tk.quicksearch.shared.ui.components.rememberPredictiveBackPreviewState
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.deviceSettings.NOTIFICATION_HISTORY_SETTING_ID
import com.tk.quicksearch.settings.tasker.TaskerIntegrationScreen
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.settings.appShortcutsSettings.AppShortcutSource
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenState
import com.tk.quicksearch.settings.shared.SettingsManagementSearchBar
import com.tk.quicksearch.settings.shared.settingsContentWidth
import com.tk.quicksearch.settings.shared.settingsScrollClip
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.settings.notesSettings.NoteDeleteConfirmationDialog
import com.tk.quicksearch.settings.notesSettings.NotesBulkDeleteConfirmationDialog
import com.tk.quicksearch.settings.bookmarksSettings.BookmarksHeaderMenu
import com.tk.quicksearch.settings.bookmarksSettings.BookmarksSettingsSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var atAGlanceSearchQuery by remember { mutableStateOf("") }
    var remindersSearchQuery by remember { mutableStateOf("") }
    var bookmarksSearchQuery by remember { mutableStateOf("") }
    var bookmarksAddRequest by remember { mutableIntStateOf(0) }
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
    var noteEditorToolbar by remember { mutableStateOf(NoteEditorToolbarState()) }
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
    val hasExcludedItems =
        state.suggestionExcludedApps.isNotEmpty() ||
            state.resultExcludedApps.isNotEmpty() ||
            state.excludedContacts.isNotEmpty() ||
            state.excludedFiles.isNotEmpty() ||
            state.excludedFileExtensions.isNotEmpty() ||
            state.excludedSettings.isNotEmpty() ||
            state.excludedAppShortcuts.isNotEmpty() ||
            state.excludedOtherItems.isNotEmpty()

    LaunchedEffect(detailType, hasExcludedItems) {
        if (detailType == SettingsDetailType.EXCLUDED_ITEMS && !hasExcludedItems) {
            callbacks.onBack()
        }
    }
    LaunchedEffect(detailType) {
        if (detailType != SettingsDetailType.NOTE_EDITOR) {
            noteEditorToolbar = NoteEditorToolbarState()
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

    val backPreview = rememberPredictiveBackPreviewState()
    val backExitsMultiSelect = detailType == SettingsDetailType.NOTES && notesMultiSelectActive
    // Leaving multi-select keeps the notes page, so only a real exit previews the page leaving.
    SettingsPageBackHandler(backPreview, preview = !backExitsMultiSelect) {
        if (backExitsMultiSelect) {
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
        modifier = modifier.predictiveBackPreview(backPreview),
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
                        } else if (detailType == SettingsDetailType.NOTE_EDITOR) {
                            noteEditorTitleRes(noteEditorToolbar)
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
                        if (detailType == SettingsDetailType.NOTE_EDITOR) {
                            noteEditorHeaderActions(noteEditorToolbar) { showNoteDeleteConfirm = true }
                        } else if (
                            detailType == SettingsDetailType.NOTIFICATION_HISTORY &&
                                notificationHistoryAccessGranted == true
                        ) {
                            {
                                AppIconButton(onClick = { showNotificationHistoryAppFilter = true }, tooltip = stringResource(R.string.notification_history_app_filter_title)) {
                                    Icon(
                                        imageVector = MaterialSymbols.MoreVert,
                                        contentDescription =
                                            stringResource(R.string.notification_history_app_filter_title),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        } else if (detailType == SettingsDetailType.BOOKMARKS) {
                            { BookmarksHeaderMenu() }
                        } else if (detailType == SettingsDetailType.CALENDAR_EVENTS) {
                            { CalendarEventsHeaderMenu() }
                        } else if (detailType == SettingsDetailType.CUSTOM_TOOL_EDITOR && pendingAiBackedToolForEditor == null) {
                            val pendingToolIdForHeader = remember { CustomToolNavigationMemory.peekPendingToolId() }
                            val existingToolForHeader = remember(pendingToolIdForHeader, state.customTools) {
                                pendingToolIdForHeader?.let { id -> state.customTools.firstOrNull { it.id == id } }
                            }
                            if (existingToolForHeader != null) {
                                {
                                    AppIconButton(onClick = {
                                        callbacks.onDeleteCustomTool(existingToolForHeader.id)
                                        callbacks.onBack()
                                    }, tooltip = stringResource(R.string.settings_custom_tool_delete_button)) {
                                        Icon(
                                            imageVector = MaterialSymbols.DeleteOutline,
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
                            )
                            .settingsScrollClip(),
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
                        )
                        .settingsScrollClip(),
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
                            )
                            .settingsScrollClip(),
                )
            } else if (detailType == SettingsDetailType.BOOKMARKS) {
                BookmarksSettingsSection(
                    searchQuery = bookmarksSearchQuery,
                    availableBrowsers = state.searchEngineOrder.filterIsInstance<SearchTarget.Browser>().map { it.app },
                    addRequest = bookmarksAddRequest,
                    modifier = Modifier.settingsContentWidth().fillMaxHeight().align(Alignment.CenterHorizontally)
                        .padding(start = DesignTokens.ContentHorizontalPadding, end = DesignTokens.ContentHorizontalPadding, bottom = 96.dp).settingsScrollClip(),
                )
            } else if (detailType == SettingsDetailType.DEVICE_SETTINGS) {
                DeviceSettingsSettingsSection(
                    modifier = Modifier.settingsContentWidth().fillMaxHeight().align(Alignment.CenterHorizontally)
                        .padding(start = DesignTokens.ContentHorizontalPadding, end = DesignTokens.ContentHorizontalPadding, bottom = DesignTokens.SectionTopPadding).settingsScrollClip(),
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
            } else if (detailType == SettingsDetailType.CALENDAR_EVENTS) {
                CalendarEventsSettingsSection(
                    onEventClick = callbacks.onLaunchCalendarEvent,
                    searchQuery = calendarEventsSearchQuery,
                    onHideEvent = callbacks.onHideCalendarEvent,
                    onUnhideEvent = callbacks.onUnhideCalendarEvent,
                    modifier = Modifier.settingsContentWidth().fillMaxHeight().align(Alignment.CenterHorizontally)
                        .padding(start = DesignTokens.ContentHorizontalPadding, end = DesignTokens.ContentHorizontalPadding, bottom = 96.dp).settingsScrollClip(),
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
                            )
                            .settingsScrollClip(),
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
                            )
                            .settingsScrollClip(),
                )
            } else if (detailType == SettingsDetailType.NOTE_EDITOR) {
                NoteEditor(
                    onNavigateToNotes = { onNavigateToDetail(SettingsDetailType.NOTES) },
                    onNavigateToSearch = onNavigateToSearch,
                    onToolbarState = { noteEditorToolbar = it },
                    hideTopBar = hideNoteEditorAppBar,
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                            )
                            .settingsScrollClip(),
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                            // These pages keep a fixed header and scroll (and clip) only their list.
                            .then(if (detailType in setOf(SettingsDetailType.AT_A_GLANCE, SettingsDetailType.TRIGGERS, SettingsDetailType.EXCLUDED_ITEMS)) Modifier else Modifier.settingsScrollClip(scrollState).verticalScroll(scrollState))
                            .padding(
                                bottom =
                                    when (detailType) {
                                        SettingsDetailType.APP_SHORTCUTS -> 96.dp
                                        SettingsDetailType.AT_A_GLANCE -> 92.dp
                                        else -> DesignTokens.SectionTopPadding
                                    },
                            ),
                ) {
                    SettingsNestedDetailBody(
                        state = state,
                        callbacks = callbacks,
                        detailType = detailType,
                        scrollState = scrollState,
                        atAGlanceSearchQuery = atAGlanceSearchQuery,
                        onNavigateToDetail = onNavigateToDetail,
                        onNicknameRemoved = onNicknameRemoved,
                    )
                }
            }
        }

        if (detailType == SettingsDetailType.EXCLUDED_ITEMS) {
            AppFloatingActionButton(
                onClick = { showClearAllConfirmation = true },
                icon = MaterialSymbols.DeleteOutline,
                contentDescription = stringResource(R.string.settings_action_clear_all),
                modifier =
                    Modifier
                        .align(androidx.compose.ui.Alignment.BottomEnd)
                        .padding(DesignTokens.SpacingLarge),
                tone = AppFabTone.Error,
            )
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
                }
            SettingsManagementSearchBar(
                query = query,
                onQueryChange = { updatedQuery ->
                    when (detailType) {
                        SettingsDetailType.APP_SHORTCUTS -> appShortcutsSearchQuery = updatedQuery
                        SettingsDetailType.APP_MANAGEMENT -> appManagementSearchQuery = updatedQuery
                    }
                },
                onClear = {
                    when (detailType) {
                        SettingsDetailType.APP_SHORTCUTS -> {
                            appShortcutsCollapseAllTrigger++
                            appShortcutsSearchQuery = ""
                        }
                        SettingsDetailType.APP_MANAGEMENT -> appManagementSearchQuery = ""
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
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            )
        } else if (detailType == SettingsDetailType.BOOKMARKS) {
            CalendarEventsBottomBar(
                query = bookmarksSearchQuery, onQueryChange = { bookmarksSearchQuery = it }, onClear = { bookmarksSearchQuery = "" },
                onNewEvent = { bookmarksAddRequest++ }, newItemLabelResId = R.string.bookmarks_add_title,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            )
        } else if (detailType == SettingsDetailType.CALENDAR_EVENTS) {
            SettingsManagementSearchBar(
                query = calendarEventsSearchQuery, onQueryChange = { calendarEventsSearchQuery = it },
                onClear = { calendarEventsSearchQuery = "" },
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        } else if (detailType == SettingsDetailType.AT_A_GLANCE) {
            AtAGlanceSettingsBottomBar(
                query = atAGlanceSearchQuery, onQueryChange = { atAGlanceSearchQuery = it }, onClear = { atAGlanceSearchQuery = "" },
                onNavigateToApiKeySetup = { onNavigateToDetail(SettingsDetailType.API_KEY_SETUP) }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
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
                    noteEditorToolbar.onConfirmedDelete?.invoke()
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
