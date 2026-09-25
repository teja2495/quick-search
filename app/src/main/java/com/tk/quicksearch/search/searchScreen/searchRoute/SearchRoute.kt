package com.tk.quicksearch.search.searchScreen.searchRoute

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import com.tk.quicksearch.search.core.intentHelpers.FileIntents
import com.tk.quicksearch.shared.ui.components.LocalPopupOverlayContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.app.UpdateHelper
import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.ItemCustomizationRemover
import com.tk.quicksearch.search.core.LocalItemCustomizationRemover
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingResultAction
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.appSettings.LocalOpenAppSettingDestination
import com.tk.quicksearch.search.appSettings.LocalOnSettingsImported
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.tools.aiTools.CurrencyConversionIntentParser
import com.tk.quicksearch.tools.aiTools.WorldClockIntentParser
import com.tk.quicksearch.tools.aiTools.DictionaryIntentParser
import com.tk.quicksearch.tools.aiTools.WeatherIntentParser
import com.tk.quicksearch.search.apps.appLock.LocalAppLockAuthenticator
import com.tk.quicksearch.search.apps.appLock.LocalAppLockCredentialAuthenticator
import com.tk.quicksearch.search.apps.speedBump.SpeedBump
import com.tk.quicksearch.search.apps.swipeGestures.AppSwipeGestures
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.settings.shared.isAppSettingToggleEnabled
import com.tk.quicksearch.settings.settingsDetailScreen.NotesNavigationMemory
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.searchScreen.SearchScreen as SearchScreenComposable
import com.tk.quicksearch.search.searchScreen.LocalHomeHorizontalSwipeHandler
import kotlinx.coroutines.launch

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onSearchEngineLongPress: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onOpenAiSearchConfigure: () -> Unit = {},
    onOpenToolsSettings: () -> Unit = {},
    onOpenCustomToolSettings: (String) -> Unit = {},
    onOpenReleaseNotesFeatures: () -> Unit = {},
    onOpenAppSettingDestination: (AppSettingsDestination) -> Unit = {},
    onOpenNotesDetail: (Long?) -> Unit = {},
    onOpenNotificationHistory: () -> Unit = {},
    onOpenWidgetsPanelFromSwipe: (() -> Unit)? = null,
    onOverlayDismissRequest: (() -> Unit)? = null,
    onCloseAppRequest: (() -> Unit)? = null,
    onShowToast: (Int) -> Unit = {},
    viewModel: SearchViewModel = viewModel(),
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    onWallpaperLoaded: (() -> Unit)? = null,
    isOverlayPresentation: Boolean = false,
    overlaySnackbarHostState: SnackbarHostState? = null,
    onOverlayExpandRequest: (() -> Unit)? = null,
    isOverlayExpanded: Boolean = false,
    onOverlayNumberKeyboardUiChanged: ((Boolean, Boolean) -> Unit)? = null,
    onOverlayScrollableContentChanged: ((Boolean) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // The app swipe action picker searches through the shared query. While it's open (and until
    // the original query is restored), keep the search screen on a snapshot so its search bar and
    // results don't mirror what's typed in the picker.
    val appSwipePickerRequest by AppSwipeGestures.pickerRequest.collectAsState()
    var searchScreenSnapshot by remember { mutableStateOf<SearchUiState?>(null) }
    if (appSwipePickerRequest != null && searchScreenSnapshot == null) {
        searchScreenSnapshot = uiState
    }
    val snapshot = searchScreenSnapshot
    if (appSwipePickerRequest == null && snapshot != null && uiState.query == snapshot.query) {
        searchScreenSnapshot = null
    }
    val searchScreenState = searchScreenSnapshot ?: uiState
    val context = LocalContext.current
    val voiceInputLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf(String::isNotBlank)
                ?.let(viewModel::onQueryChange)
        }
    val startVoiceInput: () -> Unit = {
        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.app_name))
            }
        try {
            voiceInputLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.voice_input_not_available, Toast.LENGTH_SHORT).show()
        }
    }

    val nicknameUpdateVersion = uiState.nicknameUpdateVersion
    val getAppNickname: (String) -> String? =
        remember(nicknameUpdateVersion) {
            { packageName -> viewModel.getAppNickname(packageName) }
        }
    val getContactNickname: (Long) -> String? =
        remember(nicknameUpdateVersion) {
            { contactId -> viewModel.getContactNickname(contactId) }
        }
    val getFileNickname: (String) -> String? =
        remember(nicknameUpdateVersion) { { uri -> viewModel.getFileNickname(uri) } }
    val getSettingNickname: (String) -> String? =
        remember(nicknameUpdateVersion) { { id -> viewModel.getSettingNickname(id) } }
    val getAppShortcutNickname: (String) -> String? =
        remember(nicknameUpdateVersion) { { id -> viewModel.getAppShortcutNickname(id) } }
    val getAppTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger? =
        remember(nicknameUpdateVersion) { { packageName -> viewModel.getAppTrigger(packageName) } }
    val getContactTrigger: (Long) -> com.tk.quicksearch.search.data.preferences.ResultTrigger? =
        remember(nicknameUpdateVersion) { { contactId -> viewModel.getContactTrigger(contactId) } }
    val getContactActionTrigger:
        (Long, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> com.tk.quicksearch.search.data.preferences.ResultTrigger? =
        remember(nicknameUpdateVersion) {
            { contactId, action -> viewModel.getContactActionTrigger(contactId, action) }
        }
    val getAllContactActionTriggers:
        () -> Map<com.tk.quicksearch.search.data.preferences.ContactActionTriggerKey, com.tk.quicksearch.search.data.preferences.ResultTrigger> =
        remember(nicknameUpdateVersion) { { viewModel.getAllContactActionTriggers() } }
    val getFileTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger? =
        remember(nicknameUpdateVersion) { { uri -> viewModel.getFileTrigger(uri) } }
    val getSettingTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger? =
        remember(nicknameUpdateVersion) { { id -> viewModel.getSettingTrigger(id) } }
    val getAppShortcutTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger? =
        remember(nicknameUpdateVersion) { { id -> viewModel.getAppShortcutTrigger(id) } }
    val getAllTriggerWordsById: () -> Map<String, String> =
        remember(nicknameUpdateVersion) { { viewModel.getAllTriggerWordsById() } }
    val getAllAliasWordsById: () -> Map<String, String> =
        remember(nicknameUpdateVersion) { { viewModel.getAllAliasWordsById() } }

    val undoActions = rememberRouteUndoActions(viewModel, uiState, overlaySnackbarHostState)
    val snackbarHostState = undoActions.snackbarHostState
    val popupUndoSnackbar = undoActions.popupUndoSnackbar
    val onHideAppWithUndo = undoActions.onHideAppWithUndo
    val onExcludeContactWithUndo = undoActions.onExcludeContactWithUndo
    val onExcludeFileWithUndo = undoActions.onExcludeFileWithUndo
    val onExcludeFileExtensionWithUndo = undoActions.onExcludeFileExtensionWithUndo
    val onExcludeSettingWithUndo = undoActions.onExcludeSettingWithUndo
    val onDisableAppShortcut = undoActions.onDisableAppShortcut
    val onDisableAllAppShortcutsForApp = undoActions.onDisableAllAppShortcutsForApp
    val onExcludeCalendarEventWithUndo = undoActions.onExcludeCalendarEventWithUndo
    val reminderActions = undoActions.reminderActions
    val onDeleteNoteWithUndo = undoActions.onDeleteNoteWithUndo

    val showToast: (Int) -> Unit = @Suppress("LocalContextGetResourceValueCall") { stringResId ->
        android.widget.Toast
            .makeText(
                context,
                context.getString(stringResId),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
    }

    // callContact handles the permission check and falls back to the dialer.
    val callContactWithPermission: (ContactInfo) -> Unit = { contact ->
        viewModel.callContact(contact)
    }

    val showContactMethodsBottomSheet: (ContactInfo) -> Unit = { contact ->
        viewModel.trackRecentContactTap(contact)
        viewModel.showContactMethodsBottomSheet(contact)
    }

    val dismissContactMethodsBottomSheet: () -> Unit = {
        viewModel.dismissContactMethodsBottomSheet()
    }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var showSecondaryRankingDialog by remember { mutableStateOf(false) }
    var showIconPackDialog by remember { mutableStateOf(false) }
    var showDefaultCalendarDialog by remember { mutableStateOf(false) }
    var pendingPermissionSettingsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingPermissionSettingsType by remember { mutableStateOf<Int?>(null) }
    var pendingDirectDialToggleFromAppSetting by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<DeviceFile?>(null) }
    // Non-null while a SpeedBump app is waiting out its interstitial before launching.
    var speedBumpApp by remember { mutableStateOf<com.tk.quicksearch.search.models.AppInfo?>(null) }
    val calendarPreferences = remember(context) { CalendarPreferences(context) }
    var defaultCalendarPackage by remember { mutableStateOf(calendarPreferences.getDefaultCalendarPackage()) }

    val callPermissionLauncher =
        if (context is android.app.Activity) {
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                if (pendingDirectDialToggleFromAppSetting) {
                    pendingDirectDialToggleFromAppSetting = false
                    if (isGranted) {
                        viewModel.setDirectDialEnabled(true)
                    } else {
                        var shouldShowSettingsDialog = false
                        PermissionHelper.handleDeniedRuntimePermission(
                            context = context,
                            permission = Manifest.permission.CALL_PHONE,
                            wasPreviouslyDenied = true,
                            onOpenSettings = {
                                shouldShowSettingsDialog = true
                                pendingPermissionSettingsType = R.string.settings_call_permission_title
                                pendingPermissionSettingsAction = viewModel::openAppSettings
                                showPermissionSettingsDialog = true
                            },
                        )
                        if (!shouldShowSettingsDialog) {
                            onShowToast(R.string.error_call_permission_required)
                        }
                    }
                    return@rememberLauncherForActivityResult
                }

                if (isGranted) {
                    viewModel.onCallPermissionResult(true)
                } else {
                    var shouldShowSettingsDialog = false
                    PermissionHelper.handleDeniedRuntimePermission(
                        context = context,
                        permission = Manifest.permission.CALL_PHONE,
                        wasPreviouslyDenied = true,
                        onOpenSettings = {
                            shouldShowSettingsDialog = true
                            pendingPermissionSettingsType = R.string.settings_call_permission_title
                            pendingPermissionSettingsAction = viewModel::openAppSettings
                            showPermissionSettingsDialog = true
                        },
                    )
                    viewModel.onCallPermissionResult(
                        isGranted = false,
                        shouldShowPermissionError = !shouldShowSettingsDialog,
                    )
                }
            }
        } else {
            null
        }

    val isAppSettingToggleChecked: (AppSettingResult) -> Boolean = { setting ->
        setting.toggleKey?.let { toggleKey -> uiState.isAppSettingToggleEnabled(toggleKey) } ?: false
    }

    val rateQuickSearchSetting = rememberRateQuickSearchSetting()

    val settingActions = rememberRouteSettingActions(
        viewModel = viewModel,
        uiState = uiState,
        isOverlayPresentation = isOverlayPresentation,
        onShowToast = onShowToast,
        onPendingDirectDialToggleChange = { pendingDirectDialToggleFromAppSetting = it },
        onCallPermissionRequest = { callPermissionLauncher?.launch(Manifest.permission.CALL_PHONE) },
        onShowSecondaryRankingDialog = { showSecondaryRankingDialog = true },
        onShowIconPackDialog = { showIconPackDialog = true },
        onShowDefaultCalendarDialog = { showDefaultCalendarDialog = true },
        onOpenAppSettingDestination = onOpenAppSettingDestination,
    )
    val onAppSettingToggle = settingActions.onAppSettingToggle
    val onAppSettingClick = settingActions.onAppSettingClick

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        viewModel.handleOnResume()
                        viewModel.refreshRateQuickSearchCardState()
                        if (uiState.overlayModeEnabled && context.isDefaultHomeApp()) {
                            viewModel.setOverlayModeEnabled(false)
                        }
                    }

                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.pendingDirectCallNumber, uiState.pendingThirdPartyCall) {
        val pendingNumber = uiState.pendingDirectCallNumber
        val pendingThirdPartyCall = uiState.pendingThirdPartyCall

        if (pendingNumber != null || pendingThirdPartyCall != null) {
            if (context is android.app.Activity) {
                callPermissionLauncher?.launch(Manifest.permission.CALL_PHONE)
            } else {
                viewModel.onCallPermissionResult(false)
            }
        }
    }

    val containerModifier =
        if (isOverlayPresentation) {
            modifier.fillMaxWidth()
        } else {
            modifier.fillMaxSize()
        }
    val gestures = rememberRouteGestures(
        viewModel = viewModel,
        uiState = uiState,
        isOverlayPresentation = isOverlayPresentation,
        onSettingsClick = onSettingsClick,
        onOpenWidgetsPanelFromSwipe = onOpenWidgetsPanelFromSwipe,
        onOverlayDismissRequest = onOverlayDismissRequest,
        onCloseAppRequest = onCloseAppRequest,
    )
    val requestBiometricAuthentication = gestures.requestBiometricAuthentication
    val requestDeviceCredentialAuthentication = gestures.requestDeviceCredentialAuthentication
    val runAfterAppUnlock = gestures.runAfterAppUnlock
    val swipeActions = gestures.swipeActions
    val customSwipeActions = gestures.customSwipeActions
    val swipeAliasTargets = gestures.swipeAliasTargets
    val homeSwipeUpAction = gestures.homeSwipeUpAction
    val homeSwipeDownAction = gestures.homeSwipeDownAction
    val homeDoubleTapAction = gestures.homeDoubleTapAction
    val homeCustomSwipeActions = gestures.homeCustomSwipeActions
    val homeAliasTargets = gestures.homeAliasTargets
    val closeQuickSearch = gestures.closeQuickSearch
    val handleHomeHorizontalSwipe = gestures.handleHomeHorizontalSwipe
    val swipeNavigationModifier = gestures.swipeNavigationModifier
    val itemCustomizationRemover = remember(viewModel) {
        ItemCustomizationRemover(
            removeAppNickname = { viewModel.setAppNickname(it, null) },
            removeAppTrigger = { viewModel.setAppTrigger(it, null) },
            removeAppShortcutNickname = { viewModel.setAppShortcutNickname(it, null) },
            removeAppShortcutTrigger = { viewModel.setAppShortcutTrigger(it, null) },
            removeContactNickname = { viewModel.setContactNickname(it, null) },
            removeContactTrigger = { viewModel.setContactTrigger(it, null) },
            removeFileNickname = { viewModel.setFileNickname(it, null) },
            removeFileTrigger = { viewModel.setFileTrigger(it, null) },
        )
    }

    Box(modifier = containerModifier) {
        CompositionLocalProvider(
            LocalHomeHorizontalSwipeHandler provides handleHomeHorizontalSwipe,
            LocalItemCustomizationRemover provides itemCustomizationRemover,
            LocalAppLockAuthenticator provides requestBiometricAuthentication,
            LocalAppLockCredentialAuthenticator provides requestDeviceCredentialAuthentication,
            LocalOpenAppSettingDestination provides onOpenAppSettingDestination,
            LocalOnSettingsImported provides viewModel::onSettingsImported,
            LocalPopupOverlayContent provides popupUndoSnackbar,
        ) {
            SearchScreenComposable(
                modifier =
                    if (isOverlayPresentation) {
                        Modifier.fillMaxWidth().then(swipeNavigationModifier)
                    } else {
                        Modifier.fillMaxSize().then(swipeNavigationModifier)
                    },
            state = searchScreenState,
            onQueryChanged = viewModel::onQueryChange,
            onSelectRetainedQueryHandled = viewModel::consumeRetainedQuerySelectionRequest,
            onRestoreSearchKeyboardHandled = viewModel::consumeSearchKeyboardRestoreRequest,
            onStartupKeyboardVisible = viewModel::notifyStartupKeyboardVisible,
            onClearQuery = viewModel::clearQuery,
            onVoiceClick = startVoiceInput,
            onRequestUsagePermission = { viewModel.openUsageAccessSettings() },
            onToggleOtherSearchItemPin = viewModel::toggleOtherSearchItemPin,
            onSettingsClick = onSettingsClick,
            onAppClick = { app: com.tk.quicksearch.search.models.AppInfo ->
                if (SpeedBump.isEnabled(context, app.packageName)) {
                    speedBumpApp = app
                } else {
                    runAfterAppUnlock(app.packageName, app.appName) {
                        viewModel.launchApp(app, context)
                    }
                }
            },
            onOpenInSplitScreen = { app: com.tk.quicksearch.search.models.AppInfo ->
                runAfterAppUnlock(app.packageName, app.appName) {
                    viewModel.launchAppInSplitScreen(app, context)
                }
            },
            onAppInfoClick = { app: com.tk.quicksearch.search.models.AppInfo ->
                runAfterAppUnlock(app.packageName, app.appName) {
                    viewModel.openAppInfo(app)
                }
            },
            onUninstallClick = { app: com.tk.quicksearch.search.models.AppInfo ->
                runAfterAppUnlock(app.packageName, app.appName) {
                    viewModel.requestUninstall(app)
                }
            },
            onHideApp = onHideAppWithUndo,
            onPinApp = viewModel::pinApp,
            onUnpinApp = viewModel::unpinApp,
            onReorderPinnedApps = viewModel::reorderPinnedApps,
            onReorderPinnedAppGrid = viewModel::reorderPinnedAppGrid,
            appFolderActions =
                remember(viewModel) {
                    com.tk.quicksearch.search.folders.AppGridFolderActions(
                        onCreateFolder = viewModel::createAppFolder,
                        onAddToFolder = viewModel::addToAppFolder,
                        onRemoveFromFolder = viewModel::removeFromAppFolder,
                        onUnpinFromFolder = viewModel::unpinFromAppFolder,
                        onReorderFolder = viewModel::reorderAppFolder,
                        onRenameFolder = viewModel::renameAppFolder,
                        onDeleteFolder = viewModel::deleteAppFolder,
                    )
                },
            onSuggestionTabSelected = viewModel::setSelectedAppSuggestionTab,
            onRateQuickSearchClick = { onAppSettingClick(rateQuickSearchSetting) },
            onRateQuickSearchNotNowClick = {
                viewModel.trackRecentAppSettingTap(RATE_QUICK_SEARCH_SETTING_ID)
                viewModel.dismissRateQuickSearchForNow()
            },
            onUpdateClick = {
                (context as? Activity)?.let(UpdateHelper::startUpdate)
            },
            onUpdateNotNowClick = viewModel::dismissUpdateForNow,
            onContactClick = { contact: com.tk.quicksearch.search.models.ContactInfo ->
                viewModel.openContact(contact)
            },
            onShowContactMethods = showContactMethodsBottomSheet,
            onDismissContactMethods = dismissContactMethodsBottomSheet,
            onCallContact = callContactWithPermission,
            onSmsContact = { contact: com.tk.quicksearch.search.models.ContactInfo ->
                viewModel.smsContact(contact)
            },
            onContactMethodClick = { contact, method ->
                viewModel.handleContactMethod(contact, method)
            },
            onFileClick = { file: com.tk.quicksearch.search.models.DeviceFile ->
                if (uiState.filePreviewsEnabled &&
                    (com.tk.quicksearch.search.models.FileTypeUtils.isPdf(file) ||
                        com.tk.quicksearch.search.models.FileTypeUtils.isImage(file))
                ) {
                    viewModel.recordFileOpen(file)
                    previewFile = file
                } else {
                    viewModel.openFile(file)
                }
            },
            onOpenFolder = { file: com.tk.quicksearch.search.models.DeviceFile ->
                viewModel.openContainingFolder(file)
            },
            onPinContact = viewModel::pinContact,
            onUnpinContact = viewModel::unpinContact,
            onMovePinnedContact = viewModel::movePinnedContact,
            onExcludeContact = onExcludeContactWithUndo,
            onCalendarEventClick = viewModel::openCalendarEvent,
            onPinCalendarEvent = viewModel::pinCalendarEvent,
            onUnpinCalendarEvent = viewModel::unpinCalendarEvent,
            onMovePinnedCalendarEvent = viewModel::movePinnedCalendarEvent,
            onExcludeCalendarEvent = onExcludeCalendarEventWithUndo,
            onIncludeCalendarEvent = viewModel::removeExcludedCalendarEvent,
            onArchiveTodayCalendarEvent = { event -> viewModel.archiveTodayCalendarEvent(event.eventId) },
            onNoteClick = { note ->
                viewModel.trackRecentNoteTap(note)
                NotesNavigationMemory.setPendingNoteId(note.noteId)
                onOpenNotesDetail(note.noteId)
            },
            onPinNote = viewModel::pinNote,
            onUnpinNote = viewModel::unpinNote,
            onMovePinnedNote = viewModel::movePinnedNote,
            onDeleteNote = onDeleteNoteWithUndo,
            reminderActions = reminderActions,
            onPinFile = viewModel::pinFile,
            onUnpinFile = viewModel::unpinFile,
            onMovePinnedFile = viewModel::movePinnedFile,
            onExcludeFile = onExcludeFileWithUndo,
            onExcludeFileExtension = onExcludeFileExtensionWithUndo,
            onSettingClick = { setting: com.tk.quicksearch.search.deviceSettings.DeviceSetting ->
                // Notification History is a Quick Search screen, so it navigates in-app instead of
                // starting an Activity the way every other device setting does.
                if (setting.id ==
                    com.tk.quicksearch.search.deviceSettings.NOTIFICATION_HISTORY_SETTING_ID
                ) {
                    onOpenNotificationHistory()
                } else {
                    viewModel.openSetting(setting)
                }
            },
            onAppSettingClick = onAppSettingClick,
            onAppSettingToggle = onAppSettingToggle,
            onAppSettingWebSuggestionsCountChange = viewModel::setWebSuggestionsCount,
            isAppSettingToggleChecked = isAppSettingToggleChecked,
            appSettingWebSuggestionsCount = uiState.webSuggestionsCount,
            appSettingPhoneAppGridColumns = uiState.phoneAppGridColumns,
            onAppSettingPhoneAppGridColumnsChange = viewModel::setPhoneAppGridColumns,
            appSettingAppResultRowCount = uiState.appResultRowCount,
            onAppSettingAppResultRowCountChange = viewModel::setAppResultRowCount,
            onPinSetting = viewModel::pinSetting,
            onUnpinSetting = viewModel::unpinSetting,
            onMovePinnedSetting = viewModel::movePinnedSetting,
            onExcludeSetting = onExcludeSettingWithUndo,
            onAppShortcutClick = { shortcut: com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut ->
                runAfterAppUnlock(shortcut.packageName, shortcut.appLabel) {
                    viewModel.launchAppShortcut(shortcut)
                }
            },
            onPinAppShortcut = viewModel::pinAppShortcut,
            onUnpinAppShortcut = viewModel::unpinAppShortcut,
            onMovePinnedAppShortcut = viewModel::movePinnedAppShortcut,
            onDisableAppShortcut = onDisableAppShortcut,
            onDisableAllAppShortcutsForApp = onDisableAllAppShortcutsForApp,
            onAppShortcutAppInfoClick = { shortcut: com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut ->
                runAfterAppUnlock(shortcut.packageName, shortcut.appLabel) {
                    viewModel.openAppInfo(shortcut.packageName)
                }
            },
            onPhoneNumberSelected = viewModel::onPhoneNumberSelected,
            onDismissPhoneNumberSelection = viewModel::dismissPhoneNumberSelection,
            onSearchTargetClick = { query: String, target: SearchTarget ->
                val trimmedQuery = query.trim()
                if (target is SearchTarget.Engine && target.engine == SearchEngine.DIRECT_SEARCH) {
                    when {
                        uiState.currencyConverterEnabled &&
                                uiState.calculatorState.result == null &&
                                CurrencyConversionIntentParser.parseConfirmed(trimmedQuery) != null ->
                            viewModel.executeCurrencyConversion()
                        uiState.worldClockEnabled &&
                                WorldClockIntentParser.parseConfirmed(trimmedQuery) != null ->
                            viewModel.executeWorldClockLookup()
                        uiState.dictionaryEnabled &&
                                DictionaryIntentParser.parseConfirmed(trimmedQuery) != null ->
                            viewModel.executeDictionaryLookup()
                        uiState.weatherEnabled &&
                                WeatherIntentParser.parseConfirmed(trimmedQuery)?.let { weatherQuery ->
                                    uiState.weatherLocationConfigured ||
                                        weatherQuery.requestedLocation?.isNotBlank() == true
                                } == true ->
                            viewModel.executeWeatherLookup()
                        else -> viewModel.openSearchTarget(query, target)
                    }
                } else {
                    viewModel.openSearchTarget(query, target)
                }
            },
            onSearchEngineLongPress = onSearchEngineLongPress,
            onAiSearchEmailClick = { email: String ->
                viewModel.openEmail(email)
            },
            onSetPersonalContext = viewModel::setPersonalContext,
            onSetActiveLlmModel = viewModel::setActiveLlmModel,
            onSetActiveLlmGroundingEnabled = viewModel::setActiveLlmGroundingEnabled,
            onRefreshAvailableLlmModels = viewModel::refreshAvailableLlmModels,
            onOpenAppSettings = {
                pendingPermissionSettingsType = R.string.settings_permissions_title
                pendingPermissionSettingsAction = { viewModel.openAppSettings() }
                showPermissionSettingsDialog = true
            },
            onOpenStorageAccessSettings = {
                pendingPermissionSettingsType = R.string.section_files
                pendingPermissionSettingsAction = { viewModel.openAllFilesAccessSettings() }
                showPermissionSettingsDialog = true
            },
            onOpenCalendarPermissionSettings = {
                pendingPermissionSettingsType = R.string.settings_calendar_permission_title
                pendingPermissionSettingsAction = { viewModel.openCalendarPermissionSettings() }
                showPermissionSettingsDialog = true
            },
            // Unused: SearchScreenStateManagement opens nickname dialogs from its own state.
            onAppNicknameClick = { app: com.tk.quicksearch.search.models.AppInfo -> },
            onClearDetectedShortcut = viewModel::clearDetectedShortcut,
            onSectionSelected = viewModel::activateSearchSectionFilter,
            onContactNicknameClick = { contact: com.tk.quicksearch.search.models.ContactInfo -> },
            onFileNicknameClick = { file: com.tk.quicksearch.search.models.DeviceFile -> },
            getAppNickname = getAppNickname,
            getContactNickname = getContactNickname,
            getFileNickname = getFileNickname,
            getAppShortcutNickname = getAppShortcutNickname,
            getCalendarEventNickname = viewModel::getCalendarEventNickname,
            getAppTrigger = getAppTrigger,
            getContactTrigger = getContactTrigger,
            getContactActionTrigger = getContactActionTrigger,
            getAllContactActionTriggers = getAllContactActionTriggers,
            getFileTrigger = getFileTrigger,
            getAppShortcutTrigger = getAppShortcutTrigger,
            getSettingTrigger = getSettingTrigger,
            getAllTriggerWordsById = getAllTriggerWordsById,
            getAllAliasWordsById = getAllAliasWordsById,
            onSaveAppNickname = viewModel::setAppNickname,
            onSaveAppShortcutNickname = viewModel::setAppShortcutNickname,
            onSaveContactNickname = viewModel::setContactNickname,
            onSaveFileNickname = viewModel::setFileNickname,
            onSaveCalendarEventNickname = viewModel::setCalendarEventNickname,
            onSaveAppTrigger = viewModel::setAppTrigger,
            onSaveAppShortcutTrigger = viewModel::setAppShortcutTrigger,
            onSaveContactTrigger = viewModel::setContactTrigger,
            onSaveContactActionTrigger = viewModel::setContactActionTrigger,
            onSaveFileTrigger = viewModel::setFileTrigger,
            onSaveSettingTrigger = viewModel::setSettingTrigger,
            getSettingNickname = getSettingNickname,
            onSaveSettingNickname = viewModel::setSettingNickname,
            getNoteTrigger = viewModel::getNoteTrigger,
            onSaveNoteTrigger = viewModel::setNoteTrigger,
            getAppShortcutIconOverride = viewModel::getAppShortcutIconOverride,
            onUpdateCustomAppShortcut = viewModel::updateCustomAppShortcut,
            onDeleteCustomAppShortcut = viewModel::deleteCustomAppShortcut,
            onSetAppShortcutIconOverride = viewModel::setAppShortcutIconOverride,
            getLastShownPhoneNumber = viewModel::getLastShownPhoneNumber,
            setLastShownPhoneNumber = viewModel::setLastShownPhoneNumber,
            onDirectDialChoiceSelected = viewModel::onDirectDialChoiceSelected,
            onDismissDirectDialChoice = viewModel::dismissDirectDialChoice,
            onReleaseNotesAcknowledged = viewModel::acknowledgeReleaseNotes,
            onReleaseNotesViewAllFeatures = {
                viewModel.acknowledgeReleaseNotes()
                onOpenReleaseNotesFeatures()
            },
            onAccessibilityPermissionDisclaimerDismissed =
                viewModel::dismissAccessibilityPermissionDisclaimer,
            onWebSuggestionClick = { suggestion: String ->
                viewModel.onWebSuggestionTap(suggestion)
            },
            onRecentQueryClick = viewModel::onRecentQueryTap,
            onSearchEngineOnboardingDismissed = viewModel::onSearchEngineOnboardingDismissed,
            onContactActionHintDismissed = viewModel::onContactActionHintDismissed,
            onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
            onOpenAiSearchConfigure = onOpenAiSearchConfigure,
            onAiFollowUpSubmit = viewModel::submitAiFollowUp,
            onDeleteRecentItem = viewModel::deleteRecentItem,
            onClearRecentItems = viewModel::clearRecentItems,
            onCurrencyConversionClick = viewModel::executeCurrencyConversion,
            onDictionarySearchClick = viewModel::executeDictionaryLookup,
            onWeatherSearchClick = viewModel::executeWeatherLookup,
            onWorldClockSearchClick = viewModel::executeWorldClockLookup,
            onCustomToolSearchClick = viewModel::executeCustomToolSearch,
            onTaskerIntentClick = viewModel::executeTaskerIntent,
            onOpenToolsSettings = onOpenToolsSettings,
            onOpenCustomToolSettings = onOpenCustomToolSettings,
            onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
            onCustomAction = viewModel::onCustomAction,
            onContactActionTrigger = viewModel::onCustomActionTrigger,
            getPrimaryContactCardAction = viewModel::getPrimaryContactCardAction,
            getSecondaryContactCardAction = viewModel::getSecondaryContactCardAction,
            onSavePrimaryContactCardAction = viewModel::setPrimaryContactCardAction,
            onSaveSecondaryContactCardAction = viewModel::setSecondaryContactCardAction,
            onWallpaperLoaded = onWallpaperLoaded,
            onWallpaperUnavailable = {
                viewModel.setWallpaperAvailable(false)
                if (
                    viewModel.uiState.value.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
                    viewModel.uiState.value.accentColorMode == AccentColorMode.FROM_WALLPAPER
                ) {
                    viewModel.setAccentColorMode(AccentColorMode.NONE)
                }
            },
            onSystemWallpaperChanged = viewModel::resetHomeTextColorForNewWallpaper,
            isOverlayPresentation = isOverlayPresentation,
            onOverlayExpandRequest = onOverlayExpandRequest,
            isOverlayExpanded = isOverlayExpanded,
            onOverlayNumberKeyboardUiChanged = onOverlayNumberKeyboardUiChanged,
            onOverlayScrollableContentChanged = onOverlayScrollableContentChanged,
            onOpenPermissionsSettings = {
                onOpenAppSettingDestination(AppSettingsDestination.PERMISSIONS)
            },
            onHomePinnedSectionOrderChange = viewModel::setHomePinnedSectionOrder,
            onChangeWallpaperClick = {
                launchSystemWallpaperPicker(context)
            },
            onOpenGesturesSettingsClick = {
                onOpenAppSettingDestination(AppSettingsDestination.GESTURES)
            },
            swipeUpAction = swipeActions[2],
            swipeDownAction = swipeActions[3],
            swipeUpCustomActionJson = customSwipeActions[2],
            swipeDownCustomActionJson = customSwipeActions[3],
            swipeUpAliasTarget = swipeAliasTargets[2],
            swipeDownAliasTarget = swipeAliasTargets[3],
            homeSwipeUpAction = homeSwipeUpAction,
            homeSwipeDownAction = homeSwipeDownAction,
            homeSwipeUpCustomActionJson = homeCustomSwipeActions[0],
            homeSwipeDownCustomActionJson = homeCustomSwipeActions[1],
            homeDoubleTapAction = homeDoubleTapAction,
            homeDoubleTapCustomActionJson = homeCustomSwipeActions[2],
            homeSwipeUpAliasTarget = homeAliasTargets[0],
            homeSwipeDownAliasTarget = homeAliasTargets[1],
            homeDoubleTapAliasTarget = homeAliasTargets[2],
            onGestureAliasTarget = { action, targetId ->
                when (action) {
                    SwipeGestureAction.SEARCH_ENGINE, HomeSwipeGestureAction.SEARCH_ENGINE -> viewModel.activateGestureSearchTarget(targetId)
                    SwipeGestureAction.TOOL, HomeSwipeGestureAction.TOOL -> viewModel.activateGestureTool(targetId)
                    else -> Unit
                }
            },
            onCloseQuickSearch = closeQuickSearch,
        )
        }

        if (overlaySnackbarHostState == null) {
            ExcludeUndoSnackbarHost(
                hostState = snackbarHostState,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .padding(
                            start = DesignTokens.SpacingLarge,
                            end = DesignTokens.SpacingLarge,
                            bottom = DesignTokens.SpacingHuge,
                        ),
            )
        }

        SearchRouteOverlays(
            viewModel = viewModel,
            uiState = uiState,
            showPermissionSettingsDialog = showPermissionSettingsDialog,
            pendingPermissionSettingsType = pendingPermissionSettingsType,
            onPermissionConfirm = {
                showPermissionSettingsDialog = false
                pendingPermissionSettingsAction?.invoke()
                pendingPermissionSettingsAction = null
                pendingPermissionSettingsType = null
            },
            onPermissionDismiss = {
                showPermissionSettingsDialog = false
                pendingPermissionSettingsAction = null
                pendingPermissionSettingsType = null
            },
            showSecondaryRankingDialog = showSecondaryRankingDialog,
            onSecondaryRankingDismiss = { showSecondaryRankingDialog = false },
            showIconPackDialog = showIconPackDialog,
            onIconPackDismiss = { showIconPackDialog = false },
            showDefaultCalendarDialog = showDefaultCalendarDialog,
            defaultCalendarPackage = defaultCalendarPackage,
            onCalendarSelected = { packageName ->
                defaultCalendarPackage = packageName
                calendarPreferences.setDefaultCalendarPackage(packageName)
                showDefaultCalendarDialog = false
            },
            onDefaultCalendarDismiss = { showDefaultCalendarDialog = false },
            speedBumpApp = speedBumpApp,
            onSpeedBumpOpen = { app ->
                speedBumpApp = null
                runAfterAppUnlock(app.packageName, app.appName) {
                    viewModel.launchApp(app, context)
                }
            },
            onSpeedBumpCancel = { speedBumpApp = null },
            previewFile = previewFile,
            onPreviewDismiss = { previewFile = null },
            onPreviewOpen = { file ->
                previewFile = null
                viewModel.openFile(file)
            },
            onPreviewShare = { file ->
                previewFile = null
                com.tk.quicksearch.search.core.intentHelpers.FileIntents.shareFile(context, file)
            },
        )
    }
}
