package com.tk.quicksearch.search.searchScreen

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.app.UpdateHelper
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingResultAction
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.tools.aiTools.CurrencyConversionIntentParser
import com.tk.quicksearch.tools.aiTools.WorldClockIntentParser
import com.tk.quicksearch.tools.aiTools.DictionaryIntentParser
import com.tk.quicksearch.tools.aiTools.WeatherIntentParser
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.search.apps.speedBump.SpeedBump
import com.tk.quicksearch.search.apps.speedBump.SpeedBumpOverlay
import com.tk.quicksearch.shared.permissions.PermissionSettingsDialog
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.applySettingsCommand
import com.tk.quicksearch.settings.shared.isAppSettingToggleEnabled
import com.tk.quicksearch.settings.settingsDetailScreen.NotesNavigationMemory
import com.tk.quicksearch.search.data.CustomCalendarEventRepository
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.settings.settingsDetailScreen.CustomEventEditDialog
import com.tk.quicksearch.settings.settingsDetailScreen.DefaultCalendarDialog
import com.tk.quicksearch.settings.settingsDetailScreen.SecondaryRankingDialog
import com.tk.quicksearch.settings.AppearanceSettings.IconPackPickerDialog
import com.tk.quicksearch.search.searchScreen.SearchScreen as SearchScreenComposable
import com.tk.quicksearch.search.searchScreen.HomeHorizontalSwipe
import com.tk.quicksearch.search.searchScreen.LocalHomeHorizontalSwipeHandler
import com.tk.quicksearch.search.searchScreen.ExcludeUndoSnackbarHost
import kotlinx.coroutines.launch

private const val SWIPE_NAVIGATION_THRESHOLD_PX = 140f
private const val RATE_QUICK_SEARCH_SETTING_ID = "app_settings_rate_quick_search"

private fun launchSystemWallpaperPicker(context: Context) {
    val intent = Intent(Intent.ACTION_SET_WALLPAPER)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            context.getString(
                R.string.common_error_unable_to_open,
                context.getString(R.string.action_change_wallpaper),
            ),
            Toast.LENGTH_SHORT,
        ).show()
    }
}

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
    val context = LocalContext.current

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

    val snackbarHostState = remember { SnackbarHostState() }
    val effectiveSnackbarHostState = overlaySnackbarHostState ?: snackbarHostState
    val snackbarScope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.action_undo)

    val showUndoSnackbar: (String, () -> Unit) -> Unit = { message, onUndo ->
        snackbarScope.launch {
            val result =
                effectiveSnackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = undoLabel,
                    duration = androidx.compose.material3.SnackbarDuration.Short,
                )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                onUndo()
            }
        }
    }

    val onHideAppWithUndo: (AppInfo) -> Unit = @Suppress("LocalContextGetResourceValueCall") { app ->
        val isSearching = uiState.query.isNotBlank()
        viewModel.hideApp(app)
        val messageRes =
            if (isSearching) {
                R.string.toast_excluded_from_results
            } else {
                R.string.toast_excluded_from_suggestions
            }
        showUndoSnackbar(context.getString(messageRes, app.appName)) {
            if (isSearching) {
                viewModel.unhideAppFromResults(app)
            } else {
                viewModel.unhideAppFromSuggestions(app)
            }
        }
    }

    val onExcludeContactWithUndo: (ContactInfo) -> Unit = @Suppress("LocalContextGetResourceValueCall") { contact ->
        viewModel.excludeContact(contact)
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, contact.displayName),
        ) {
            viewModel.removeExcludedContact(contact)
        }
    }

    val onExcludeFileWithUndo: (DeviceFile) -> Unit = @Suppress("LocalContextGetResourceValueCall") { file ->
        viewModel.excludeFile(file)
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, file.displayName),
        ) {
            viewModel.removeExcludedFile(file)
        }
    }

    val onExcludeFileExtensionWithUndo: (DeviceFile) -> Unit = @Suppress("LocalContextGetResourceValueCall") { file ->
        val extension = FileUtils.getFileExtension(file.displayName)
        if (extension != null) {
            viewModel.excludeFileExtension(file)
            val extensionLabel = context.getString(R.string.file_extension_label, extension)
            showUndoSnackbar(
                context.getString(R.string.toast_excluded_from_results, extensionLabel),
            ) {
                viewModel.removeExcludedFileExtension(extension)
            }
        }
    }

    val onExcludeSettingWithUndo: (DeviceSetting) -> Unit = @Suppress("LocalContextGetResourceValueCall") { setting ->
        viewModel.excludeSetting(setting)
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, setting.title),
        ) {
            viewModel.removeExcludedSetting(setting)
        }
    }

    val onExcludeAppShortcutWithUndo: (com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut) -> Unit = @Suppress("LocalContextGetResourceValueCall") { shortcut ->
        viewModel.excludeAppShortcut(shortcut)
        showUndoSnackbar(
            context.getString(
                R.string.toast_excluded_from_results,
                shortcutDisplayName(shortcut),
            ),
        ) {
            viewModel.removeExcludedAppShortcut(shortcut)
        }
    }

    val onExcludeCalendarEventWithUndo: (CalendarEventInfo) -> Unit = @Suppress("LocalContextGetResourceValueCall") { event ->
        viewModel.excludeCalendarEvent(event)
        val label = event.title.ifBlank { context.getString(R.string.section_calendar) }
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, label),
        ) {
            viewModel.removeExcludedCalendarEvent(event)
        }
    }

    val onDeleteNoteWithUndo: (NoteInfo) -> Unit = noteDelete@{ note ->
        val staged = viewModel.stageDeleteNote(note) ?: return@noteDelete
        val label = staged.title.ifBlank { context.getString(R.string.notes_untitled) }
        var wasUndone = false
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, label),
        ) {
            wasUndone = true
            viewModel.undoDeleteNote(staged.noteId)
        }
        snackbarScope.launch {
            kotlinx.coroutines.delay(2_500L)
            if (!wasUndone) {
                viewModel.finalizeDeleteNote(staged.noteId)
            }
        }
    }

    // Set up toast callback for ViewModel
    val showToast: (Int) -> Unit = @Suppress("LocalContextGetResourceValueCall") { stringResId ->
        android.widget.Toast
            .makeText(
                context,
                context.getString(stringResId),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
    }

    // UI feedback is now handled by UiFeedbackService in the ViewModel

    // Wrapper function that calls directly - performCall will handle permission check and fallback
    // to dialer
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
    var editingCustomCalendarEvent by remember { mutableStateOf<CalendarEventInfo?>(null) }
    var previewFile by remember { mutableStateOf<DeviceFile?>(null) }
    // Non-null while a SpeedBump app is waiting out its interstitial before launching.
    var speedBumpApp by remember { mutableStateOf<com.tk.quicksearch.search.models.AppInfo?>(null) }
    val customCalendarEventRepository = remember(context) { CustomCalendarEventRepository(context) }
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

    val rateQuickSearchSetting =
        remember {
            AppSettingResult(
                id = RATE_QUICK_SEARCH_SETTING_ID,
                title = "",
                action = AppSettingResultAction.NAVIGATE,
                destination = AppSettingsDestination.RATE_QUICK_SEARCH,
            )
        }

    val onAppSettingToggle: (AppSettingResult, Boolean) -> Unit = { setting, enabled ->
        viewModel.trackRecentAppSettingTap(setting.id)
        when (val toggleKey = setting.toggleKey) {
            AppSettingsToggleKey.OVERLAY_MODE -> {
                val isDefaultHomeApp = context.isDefaultHomeApp()
                val shouldEnableOverlay = enabled && !isDefaultHomeApp
                viewModel.setOverlayModeEnabled(shouldEnableOverlay)
                if (shouldEnableOverlay) {
                    OverlayModeController.startOverlay(
                        context = context,
                        initialQuery = uiState.query.takeIf { it.isNotBlank() },
                    )
                    (context as? android.app.Activity)?.finish()
                } else if (isOverlayPresentation) {
                    OverlayModeController.openMainActivity(
                        context = context,
                        initialQuery = uiState.query.takeIf { it.isNotBlank() },
                    )
                    (context as? android.app.Activity)?.finish()
                }
            }
            AppSettingsToggleKey.DIRECT_DIAL -> {
                if (enabled) {
                    if (uiState.hasCallPermission) {
                        viewModel.setDirectDialEnabled(true)
                    } else if (context is android.app.Activity) {
                        pendingDirectDialToggleFromAppSetting = true
                        callPermissionLauncher?.launch(Manifest.permission.CALL_PHONE)
                    } else {
                        onShowToast(R.string.error_call_permission_required)
                    }
                } else {
                    pendingDirectDialToggleFromAppSetting = false
                    viewModel.setDirectDialEnabled(false)
                }
            }
            null -> Unit
            else -> viewModel.applySettingsCommand(SettingsCommand.Toggle(toggleKey, enabled))
        }
    }

    val onAppSettingClick: (AppSettingResult) -> Unit = appSettingClick@{ setting ->
        viewModel.trackRecentAppSettingTap(setting.id)
        if (setting.action != AppSettingResultAction.NAVIGATE) return@appSettingClick
        setting.destination?.let { destination ->
            if (destination == AppSettingsDestination.SEARCH_RESULT_RANKING) {
                showSecondaryRankingDialog = true
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.ICON_PACKS) {
                viewModel.refreshIconPacks()
                showIconPackDialog = true
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.OPEN_EVENTS_IN) {
                showDefaultCalendarDialog = true
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.RATE_QUICK_SEARCH) {
                viewModel.markRateQuickSearchCompleted()
            }
            onOpenAppSettingDestination(destination)
        }
    }

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
    val gesturePreferences = remember(context.applicationContext) {
        UserAppPreferences(context.applicationContext)
    }
    var isDefaultLauncher by remember { mutableStateOf(context.cachedDefaultHomeAppStatus()) }
    var swipeActions by remember {
        mutableStateOf(
            listOf(
                gesturePreferences.getSwipeRightAction(),
                gesturePreferences.getSwipeLeftAction(),
                gesturePreferences.getSwipeUpAction(),
                gesturePreferences.getSwipeDownAction(),
            ),
        )
    }
    var customSwipeActions by remember {
        mutableStateOf(
            listOf(
                gesturePreferences.getSwipeRightCustomAction(),
                gesturePreferences.getSwipeLeftCustomAction(),
                gesturePreferences.getSwipeUpCustomAction(),
                gesturePreferences.getSwipeDownCustomAction(),
            ),
        )
    }
    var swipeAliasTargets by remember { mutableStateOf(listOf(gesturePreferences.getSwipeRightAliasTarget(), gesturePreferences.getSwipeLeftAliasTarget(), gesturePreferences.getSwipeUpAliasTarget(), gesturePreferences.getSwipeDownAliasTarget())) }
    var homeSwipeUpAction by remember {
        mutableStateOf(gesturePreferences.getHomeSwipeUpAction())
    }
    var homeSwipeDownAction by remember {
        mutableStateOf(gesturePreferences.getHomeSwipeDownAction(isDefaultLauncher))
    }
    var homeDoubleTapAction by remember {
        mutableStateOf(gesturePreferences.getHomeDoubleTapAction())
    }
    var homeCustomSwipeActions by remember {
        mutableStateOf(
            listOf(
                gesturePreferences.getHomeSwipeUpCustomAction(),
                gesturePreferences.getHomeSwipeDownCustomAction(),
                gesturePreferences.getHomeDoubleTapCustomAction(),
            ),
        )
    }
    var homeAliasTargets by remember { mutableStateOf(listOf(gesturePreferences.getHomeSwipeUpAliasTarget(), gesturePreferences.getHomeSwipeDownAliasTarget(), gesturePreferences.getHomeDoubleTapAliasTarget())) }
    var isLauncherSwipeRightEnabled by remember { mutableStateOf(gesturePreferences.isLauncherSwipeRightEnabled()) }
    DisposableEffect(gesturePreferences) {
        val preferences =
            context.applicationContext.getSharedPreferences(
                com.tk.quicksearch.search.data.preferences.BasePreferences.PREFS_NAME,
                Context.MODE_PRIVATE,
            )
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            swipeActions =
                listOf(
                    gesturePreferences.getSwipeRightAction(),
                    gesturePreferences.getSwipeLeftAction(),
                    gesturePreferences.getSwipeUpAction(),
                    gesturePreferences.getSwipeDownAction(),
                )
            customSwipeActions =
                listOf(
                    gesturePreferences.getSwipeRightCustomAction(),
                    gesturePreferences.getSwipeLeftCustomAction(),
                    gesturePreferences.getSwipeUpCustomAction(),
                    gesturePreferences.getSwipeDownCustomAction(),
                )
            swipeAliasTargets = listOf(gesturePreferences.getSwipeRightAliasTarget(), gesturePreferences.getSwipeLeftAliasTarget(), gesturePreferences.getSwipeUpAliasTarget(), gesturePreferences.getSwipeDownAliasTarget())
            homeSwipeUpAction = gesturePreferences.getHomeSwipeUpAction()
            homeSwipeDownAction = gesturePreferences.getHomeSwipeDownAction(isDefaultLauncher)
            homeDoubleTapAction = gesturePreferences.getHomeDoubleTapAction()
            homeCustomSwipeActions =
                listOf(
                    gesturePreferences.getHomeSwipeUpCustomAction(),
                    gesturePreferences.getHomeSwipeDownCustomAction(),
                    gesturePreferences.getHomeDoubleTapCustomAction(),
                )
            homeAliasTargets = listOf(gesturePreferences.getHomeSwipeUpAliasTarget(), gesturePreferences.getHomeSwipeDownAliasTarget(), gesturePreferences.getHomeDoubleTapAliasTarget())
            isLauncherSwipeRightEnabled = gesturePreferences.isLauncherSwipeRightEnabled()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultLauncher = context.isDefaultHomeApp()
                homeSwipeDownAction = gesturePreferences.getHomeSwipeDownAction(isDefaultLauncher)
                if (
                    gesturePreferences.getHomeDoubleTapAction() == HomeSwipeGestureAction.LOCK_SCREEN &&
                    !LockScreenAccessibilityService.isEnabled(context)
                ) {
                    gesturePreferences.setHomeDoubleTapAction(HomeSwipeGestureAction.NONE)
                    homeDoubleTapAction = HomeSwipeGestureAction.NONE
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val handleHomeHorizontalSwipe: (HomeHorizontalSwipe) -> Unit = { swipe ->
        when (swipe) {
            HomeHorizontalSwipe.RIGHT -> {
                if (isDefaultLauncher) {
                    if (isLauncherSwipeRightEnabled) onOpenWidgetsPanelFromSwipe?.invoke()
                } else {
                    when (swipeActions[0]) {
                        SwipeGestureAction.WIDGETS_PANEL -> onOpenWidgetsPanelFromSwipe?.invoke()
                        SwipeGestureAction.CUSTOM -> {
                            com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
                                .fromJson(customSwipeActions[0])
                                ?.let { action -> context.startActivity(com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity.createIntent(context, action)) }
                        }
                        SwipeGestureAction.SEARCH_ENGINE -> swipeAliasTargets[0]?.let(viewModel::activateGestureSearchTarget)
                        SwipeGestureAction.TOOL -> swipeAliasTargets[0]?.let(viewModel::activateGestureTool)
                        else -> Unit
                    }
                }
            }
            HomeHorizontalSwipe.LEFT -> {
                when (swipeActions[1]) {
                    SwipeGestureAction.SETTINGS -> onSettingsClick()
                    SwipeGestureAction.CUSTOM -> {
                        com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
                            .fromJson(customSwipeActions[1])
                            ?.let { action -> context.startActivity(com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity.createIntent(context, action)) }
                    }
                    SwipeGestureAction.SEARCH_ENGINE -> swipeAliasTargets[1]?.let(viewModel::activateGestureSearchTarget)
                    SwipeGestureAction.TOOL -> swipeAliasTargets[1]?.let(viewModel::activateGestureTool)
                    else -> Unit
                }
            }
        }
    }
    val swipeNavigationModifier =
        Modifier.pointerInput(isDefaultLauncher, isLauncherSwipeRightEnabled, swipeActions, customSwipeActions, uiState.query) {
            var totalHorizontalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalHorizontalDrag = 0f },
                onHorizontalDrag = { _, dragAmount ->
                    totalHorizontalDrag += dragAmount
                },
                onDragEnd = {
                    if (totalHorizontalDrag >= SWIPE_NAVIGATION_THRESHOLD_PX) {
                        handleHomeHorizontalSwipe(HomeHorizontalSwipe.RIGHT)
                    } else if (totalHorizontalDrag <= -SWIPE_NAVIGATION_THRESHOLD_PX) {
                        handleHomeHorizontalSwipe(HomeHorizontalSwipe.LEFT)
                    }
                    totalHorizontalDrag = 0f
                },
                onDragCancel = { totalHorizontalDrag = 0f },
            )
        }
    val shouldAutoCloseSearchSurface =
        shouldCloseSearchSurfaceAfterExternalNavigation(
            autoCloseEnabled = uiState.autoCloseOverlay,
            isOverlayPresentation = isOverlayPresentation,
            isDefaultLauncher = isDefaultLauncher,
        )
    LaunchedEffect(shouldAutoCloseSearchSurface, isOverlayPresentation) {
        viewModel.externalNavigationEvent.collect {
            if (!shouldAutoCloseSearchSurface) return@collect
            if (isOverlayPresentation) {
                onOverlayDismissRequest?.invoke()
            } else {
                onCloseAppRequest?.invoke()
            }
        }
    }

    Box(modifier = containerModifier) {
        CompositionLocalProvider(LocalHomeHorizontalSwipeHandler provides handleHomeHorizontalSwipe) {
            SearchScreenComposable(
                modifier =
                    if (isOverlayPresentation) {
                        Modifier.fillMaxWidth().then(swipeNavigationModifier)
                    } else {
                        Modifier.fillMaxSize().then(swipeNavigationModifier)
                    },
            state = uiState,
            onQueryChanged = viewModel::onQueryChange,
            onSelectRetainedQueryHandled = viewModel::consumeRetainedQuerySelectionRequest,
            onRestoreSearchKeyboardHandled = viewModel::consumeSearchKeyboardRestoreRequest,
            onStartupKeyboardVisible = viewModel::notifyStartupKeyboardVisible,
            onClearQuery = viewModel::clearQuery,
            onRequestUsagePermission = { viewModel.openUsageAccessSettings() },
            onToggleOtherSearchItemPin = viewModel::toggleOtherSearchItemPin,
            onSettingsClick = onSettingsClick,
            onAppClick = { app: com.tk.quicksearch.search.models.AppInfo ->
                if (SpeedBump.isEnabled(context, app.packageName)) {
                    speedBumpApp = app
                } else {
                    viewModel.launchApp(app, context)
                }
            },
            onOpenInSplitScreen = { app: com.tk.quicksearch.search.models.AppInfo ->
                viewModel.launchAppInSplitScreen(app, context)
            },
            onAppInfoClick = { app: com.tk.quicksearch.search.models.AppInfo ->
                viewModel.openAppInfo(app)
            },
            onUninstallClick = { app: com.tk.quicksearch.search.models.AppInfo ->
                viewModel.requestUninstall(app)
            },
            onHideApp = onHideAppWithUndo,
            onPinApp = viewModel::pinApp,
            onUnpinApp = viewModel::unpinApp,
            onReorderPinnedApps = viewModel::reorderPinnedApps,
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
            onCalendarEventClick = { event: com.tk.quicksearch.search.models.CalendarEventInfo ->
                if (event.eventId < 0) {
                    editingCustomCalendarEvent = event
                } else {
                    viewModel.openCalendarEvent(event)
                }
            },
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
            onPinFile = viewModel::pinFile,
            onUnpinFile = viewModel::unpinFile,
            onMovePinnedFile = viewModel::movePinnedFile,
            onExcludeFile = onExcludeFileWithUndo,
            onExcludeFileExtension = onExcludeFileExtensionWithUndo,
            onSettingClick = { setting: com.tk.quicksearch.search.deviceSettings.DeviceSetting ->
                viewModel.openSetting(setting)
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
            onAppShortcutClick = { shortcut: com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut ->
                viewModel.launchAppShortcut(shortcut)
            },
            onPinAppShortcut = viewModel::pinAppShortcut,
            onUnpinAppShortcut = viewModel::unpinAppShortcut,
            onMovePinnedAppShortcut = viewModel::movePinnedAppShortcut,
            onExcludeAppShortcut = onExcludeAppShortcutWithUndo,
            onIncludeAppShortcut = viewModel::removeExcludedAppShortcut,
            onAppShortcutAppInfoClick = { shortcut: com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut ->
                viewModel.openAppInfo(shortcut.packageName)
            },
            onPhoneNumberSelected = viewModel::onPhoneNumberSelected,
            onDismissPhoneNumberSelection = viewModel::dismissPhoneNumberSelection,
            onSearchTargetClick = { query: String, target: SearchTarget ->
                val trimmedQuery = query.trim()
                if (target is SearchTarget.Engine && target.engine == SearchEngine.DIRECT_SEARCH) {
                    when {
                        uiState.currencyConverterEnabled &&
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
            onSetGeminiModel = viewModel::setGeminiModel,
            onSetGeminiGroundingEnabled = viewModel::setGeminiGroundingEnabled,
            onRefreshAvailableGeminiModels = viewModel::refreshAvailableGeminiModels,
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
            onAppNicknameClick = { app: com.tk.quicksearch.search.models.AppInfo ->
                // This will be handled by the dialog state in SearchScreen
            },
            onClearDetectedShortcut = viewModel::clearDetectedShortcut,
            onSectionSelected = viewModel::activateSearchSectionFilter,
            onContactNicknameClick = { contact: com.tk.quicksearch.search.models.ContactInfo ->
                // This will be handled by the dialog state in SearchScreen
            },
            onFileNicknameClick = { file: com.tk.quicksearch.search.models.DeviceFile ->
                // This will be handled by the dialog state in SearchScreen
            },
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
            onSystemWallpaperChanged = viewModel::resetHomeTextColorForNewWallpaper,
            isOverlayPresentation = isOverlayPresentation,
            onOverlayExpandRequest = onOverlayExpandRequest,
            isOverlayExpanded = isOverlayExpanded,
            onOverlayNumberKeyboardUiChanged = onOverlayNumberKeyboardUiChanged,
            onOverlayScrollableContentChanged = onOverlayScrollableContentChanged,
            onOpenPermissionsSettings = {
                onOpenAppSettingDestination(AppSettingsDestination.PERMISSIONS)
            },
            onChangeWallpaperClick = {
                launchSystemWallpaperPicker(context)
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

        editingCustomCalendarEvent?.let { event ->
            CustomEventEditDialog(
                event = event,
                onDismiss = { editingCustomCalendarEvent = null },
                onSave = { title, dateTimeMillis, allDay ->
                    editingCustomCalendarEvent = null
                    customCalendarEventRepository.updateCustomEvent(event.eventId, title, dateTimeMillis, allDay)
                    viewModel.onQueryChange(uiState.query)
                },
                onDelete = {
                    editingCustomCalendarEvent = null
                    customCalendarEventRepository.deleteCustomEvent(event.eventId)
                    viewModel.onQueryChange(uiState.query)
                },
            )
        }

        if (showPermissionSettingsDialog) {
            PermissionSettingsDialog(
                permissionType = stringResource(pendingPermissionSettingsType ?: R.string.settings_permissions_title),
                onConfirm = {
                    showPermissionSettingsDialog = false
                    pendingPermissionSettingsAction?.invoke()
                    pendingPermissionSettingsAction = null
                    pendingPermissionSettingsType = null
                },
                onDismiss = {
                    showPermissionSettingsDialog = false
                    pendingPermissionSettingsAction = null
                    pendingPermissionSettingsType = null
                },
            )
        }

        if (showSecondaryRankingDialog) {
            SecondaryRankingDialog(
                selectedSignal = uiState.secondaryRankingSignal,
                onSignalSelected = { signal ->
                    viewModel.setSecondaryRankingSignal(signal)
                    viewModel.onQueryChange(uiState.query)
                },
                onDismiss = { showSecondaryRankingDialog = false },
            )
        }

        if (showIconPackDialog) {
            IconPackPickerDialog(
                availableIconPacks = uiState.availableIconPacks,
                selectedPackage = uiState.selectedIconPackPackage,
                maskUnsupportedIcons = uiState.maskUnsupportedIconPackIcons,
                onSelect = { packageName ->
                    viewModel.setIconPackPackage(packageName)
                    showIconPackDialog = false
                },
                onMaskUnsupportedIconsChange = viewModel::setIconPackUnsupportedIconMaskEnabled,
                onDownloadIconPacks = viewModel::searchIconPacks,
                onResetAllIcons = viewModel::resetAllAppIconsToDefault,
                onDismiss = { showIconPackDialog = false },
            )
        }

        if (showDefaultCalendarDialog) {
            DefaultCalendarDialog(
                selectedPackageName = defaultCalendarPackage,
                onCalendarSelected = { packageName ->
                    defaultCalendarPackage = packageName
                    calendarPreferences.setDefaultCalendarPackage(packageName)
                    showDefaultCalendarDialog = false
                },
                onDismiss = { showDefaultCalendarDialog = false },
            )
        }

        speedBumpApp?.let { app ->
            SpeedBumpOverlay(
                appInfo = app,
                iconPackPackage = uiState.selectedIconPackPackage,
                appIconShape = uiState.appIconShape,
                onOpen = {
                    speedBumpApp = null
                    viewModel.launchApp(app, context)
                },
                onCancel = { speedBumpApp = null },
            )
        }

        previewFile?.let { file ->
            com.tk.quicksearch.search.files.FilePreviewBottomSheet(
                deviceFile = file,
                onDismiss = { previewFile = null },
                onOpen = {
                    previewFile = null
                    viewModel.openFile(file)
                },
                onShare = {
                    previewFile = null
                    com.tk.quicksearch.search.core.FileIntents.shareFile(context, file)
                },
            )
        }
    }
}
