package com.tk.quicksearch.search.searchScreen

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingResultAction
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.tools.aiTools.CurrencyConversionIntentParser
import com.tk.quicksearch.tools.aiTools.WordClockIntentParser
import com.tk.quicksearch.tools.aiTools.DictionaryIntentParser
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.shared.permissions.PermissionSettingsDialog
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.applySettingsCommand
import com.tk.quicksearch.settings.shared.isAppSettingToggleEnabled
import com.tk.quicksearch.search.searchScreen.SearchScreen as SearchScreenComposable
import com.tk.quicksearch.search.searchScreen.ExcludeUndoSnackbarHost
import kotlinx.coroutines.launch

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onOpenSearchHistorySettings: () -> Unit = {},
    onSearchEngineLongPress: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onOpenDirectSearchConfigure: () -> Unit = {},
    onOpenReleaseNotesFeatures: () -> Unit = {},
    onOpenAppSettingDestination: (AppSettingsDestination) -> Unit = {},
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
    var pendingPermissionSettingsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingPermissionSettingsType by remember { mutableStateOf<Int?>(null) }
    var pendingDirectDialToggleFromAppSetting by remember { mutableStateOf(false) }

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

    val onAppSettingToggle: (AppSettingResult, Boolean) -> Unit = { setting, enabled ->
        viewModel.trackRecentAppSettingTap(setting.id)
        val toggleKey = setting.toggleKey
        if (toggleKey != null && SearchSectionRegistry.sectionForToggle(toggleKey) != null) {
            viewModel.applySettingsCommand(SettingsCommand.Toggle(toggleKey, enabled))
        } else when (toggleKey) {
            AppSettingsToggleKey.OVERLAY_MODE -> {
                viewModel.setOverlayModeEnabled(enabled)
                if (enabled) {
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
            AppSettingsToggleKey.ONE_HANDED_MODE,
            AppSettingsToggleKey.BOTTOM_SEARCHBAR,
            AppSettingsToggleKey.APP_LABELS,
            AppSettingsToggleKey.SEARCH_ENGINE_COMPACT_MODE,
            AppSettingsToggleKey.SEARCH_ENGINE_ALIAS_SUFFIX,
            AppSettingsToggleKey.CALCULATOR,
            AppSettingsToggleKey.UNIT_CONVERTER,
            AppSettingsToggleKey.DATE_CALCULATOR,
            AppSettingsToggleKey.DICTIONARY,
            AppSettingsToggleKey.APP_SUGGESTIONS,
            AppSettingsToggleKey.WEB_SUGGESTIONS,
            AppSettingsToggleKey.RECENT_QUERIES,
            AppSettingsToggleKey.TOP_RESULT_INDICATOR,
            AppSettingsToggleKey.OPEN_KEYBOARD,
            AppSettingsToggleKey.CLEAR_QUERY,
            AppSettingsToggleKey.AUTO_CLOSE_OVERLAY,
            AppSettingsToggleKey.CIRCULAR_APP_ICONS,
            AppSettingsToggleKey.SHOW_FOLDERS,
            AppSettingsToggleKey.SHOW_SYSTEM_FILES,
            AppSettingsToggleKey.SEARCH_APPS,
            AppSettingsToggleKey.SEARCH_APP_SHORTCUTS,
            AppSettingsToggleKey.SEARCH_CONTACTS,
            AppSettingsToggleKey.SEARCH_FILES,
            AppSettingsToggleKey.SEARCH_DEVICE_SETTINGS,
            AppSettingsToggleKey.SEARCH_CALENDAR,
            AppSettingsToggleKey.SEARCH_APP_SETTINGS,
            AppSettingsToggleKey.ASSISTANT_LAUNCH_VOICE_MODE,
            AppSettingsToggleKey.WALLPAPER_ACCENT,
            AppSettingsToggleKey.THEMED_ICONS,
            AppSettingsToggleKey.APPS_PER_ROW,
            -> viewModel.applySettingsCommand(SettingsCommand.Toggle(toggleKey, enabled))
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
        }
    }

    val onAppSettingClick: (AppSettingResult) -> Unit = appSettingClick@{ setting ->
        viewModel.trackRecentAppSettingTap(setting.id)
        if (setting.action != AppSettingResultAction.NAVIGATE) return@appSettingClick
        setting.destination?.let { destination ->
            onOpenAppSettingDestination(destination)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        viewModel.handleOnResume()
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
    val shouldAutoCloseApp = uiState.autoCloseOverlay
    LaunchedEffect(Unit) {
        viewModel.externalNavigationEvent.collect {
            if (!shouldAutoCloseApp) return@collect
            if (isOverlayPresentation) {
                onOverlayDismissRequest?.invoke()
            } else {
                onCloseAppRequest?.invoke()
            }
        }
    }

    Box(modifier = containerModifier) {
        SearchScreenComposable(
            modifier =
                if (isOverlayPresentation) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.fillMaxSize()
                },
            state = uiState,
            onQueryChanged = viewModel::onQueryChange,
            onSelectRetainedQueryHandled = viewModel::consumeRetainedQuerySelectionRequest,
            onClearQuery = viewModel::clearQuery,
            onRequestUsagePermission = { viewModel.openUsageAccessSettings() },
            onSettingsClick = onSettingsClick,
            onAppClick = { app: com.tk.quicksearch.search.models.AppInfo ->
                viewModel.launchApp(app)
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
                viewModel.openFile(file)
            },
            onOpenFolder = { file: com.tk.quicksearch.search.models.DeviceFile ->
                viewModel.openContainingFolder(file)
            },
            onPinContact = viewModel::pinContact,
            onUnpinContact = viewModel::unpinContact,
            onExcludeContact = onExcludeContactWithUndo,
            onCalendarEventClick = { event: com.tk.quicksearch.search.models.CalendarEventInfo ->
                viewModel.openCalendarEvent(event)
            },
            onPinCalendarEvent = viewModel::pinCalendarEvent,
            onUnpinCalendarEvent = viewModel::unpinCalendarEvent,
            onExcludeCalendarEvent = onExcludeCalendarEventWithUndo,
            onIncludeCalendarEvent = viewModel::removeExcludedCalendarEvent,
            onPinFile = viewModel::pinFile,
            onUnpinFile = viewModel::unpinFile,
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
            onPinSetting = viewModel::pinSetting,
            onUnpinSetting = viewModel::unpinSetting,
            onExcludeSetting = onExcludeSettingWithUndo,
            onAppShortcutClick = { shortcut: com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut ->
                viewModel.launchAppShortcut(shortcut)
            },
            onPinAppShortcut = viewModel::pinAppShortcut,
            onUnpinAppShortcut = viewModel::unpinAppShortcut,
            onExcludeAppShortcut = onExcludeAppShortcutWithUndo,
            onIncludeAppShortcut = viewModel::removeExcludedAppShortcut,
            onAppShortcutAppInfoClick = { shortcut: com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut ->
                viewModel.openAppInfo(shortcut.packageName)
            },
            onPhoneNumberSelected = viewModel::onPhoneNumberSelected,
            onDismissPhoneNumberSelection = viewModel::dismissPhoneNumberSelection,
            onSearchTargetClick = { query: String, target: SearchTarget ->
                val trimmedQuery = query.trim()
                if (target is SearchTarget.Engine &&
                    target.engine == SearchEngine.DIRECT_SEARCH &&
                    uiState.hasGeminiApiKey) {
                    when {
                        uiState.currencyConverterEnabled &&
                                CurrencyConversionIntentParser.parseConfirmed(trimmedQuery) != null ->
                            viewModel.executeCurrencyConversion()
                        uiState.wordClockEnabled &&
                                WordClockIntentParser.parseConfirmed(trimmedQuery) != null ->
                            viewModel.executeWordClockLookup()
                        uiState.dictionaryEnabled &&
                                DictionaryIntentParser.parseConfirmed(trimmedQuery) != null ->
                            viewModel.executeDictionaryLookup()
                        else -> viewModel.openSearchTarget(query, target)
                    }
                } else {
                    viewModel.openSearchTarget(query, target)
                }
            },
            onSearchEngineLongPress = onSearchEngineLongPress,
            onDirectSearchEmailClick = { email: String ->
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
                pendingPermissionSettingsType = R.string.settings_files_permission_title
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
            onSaveAppNickname = viewModel::setAppNickname,
            onSaveAppShortcutNickname = viewModel::setAppShortcutNickname,
            onSaveContactNickname = viewModel::setContactNickname,
            onSaveFileNickname = viewModel::setFileNickname,
            onSaveCalendarEventNickname = viewModel::setCalendarEventNickname,
            getSettingNickname = getSettingNickname,
            onSaveSettingNickname = viewModel::setSettingNickname,
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
            onWebSuggestionClick = { suggestion: String ->
                viewModel.onWebSuggestionTap(suggestion)
            },
            onSearchEngineOnboardingDismissed = viewModel::onSearchEngineOnboardingDismissed,
            onContactActionHintDismissed = viewModel::onContactActionHintDismissed,
            onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
            onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
            onDeleteRecentItem = viewModel::deleteRecentItem,
            onOpenSearchHistorySettings = onOpenSearchHistorySettings,
            onDismissSearchHistoryTip = viewModel::dismissSearchHistoryTip,
            onCurrencyConversionClick = viewModel::executeCurrencyConversion,
            onDictionarySearchClick = viewModel::executeDictionaryLookup,
            onWordClockSearchClick = viewModel::executeWordClockLookup,
            onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
            onCustomAction = viewModel::onCustomAction,
            getPrimaryContactCardAction = viewModel::getPrimaryContactCardAction,
            getSecondaryContactCardAction = viewModel::getSecondaryContactCardAction,
            onSavePrimaryContactCardAction = viewModel::setPrimaryContactCardAction,
            onSaveSecondaryContactCardAction = viewModel::setSecondaryContactCardAction,
            onWallpaperLoaded = onWallpaperLoaded,
            isOverlayPresentation = isOverlayPresentation,
            onOverlayExpandRequest = onOverlayExpandRequest,
            isOverlayExpanded = isOverlayExpanded,
            onOverlayNumberKeyboardUiChanged = onOverlayNumberKeyboardUiChanged,
            onOverlayScrollableContentChanged = onOverlayScrollableContentChanged,
            onOpenPermissionsSettings = {
                onOpenAppSettingDestination(AppSettingsDestination.PERMISSIONS)
            },
        )

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
    }
}
