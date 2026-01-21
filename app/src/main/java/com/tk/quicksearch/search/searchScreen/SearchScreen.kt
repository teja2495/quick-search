package com.tk.quicksearch.search.searchScreen

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.dialogs.ContactActionPickerDialog
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.DirectDialOption
import com.tk.quicksearch.search.core.DirectSearchState
import com.tk.quicksearch.search.core.DirectSearchStatus
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.shortcutDisplayName
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.searchScreen.dialogs.NicknameDialogState
import com.tk.quicksearch.search.searchScreen.dialogs.SearchScreenDialogs
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.WallpaperUtils
import kotlinx.coroutines.launch

@Composable
fun SearchRoute(
        modifier: Modifier = Modifier,
        onSettingsClick: () -> Unit = {},
        onSearchEngineLongPress: () -> Unit = {},
        onCustomizeSearchEnginesClick: () -> Unit = {},
        onShowToast: (Int) -> Unit = {},
        viewModel: SearchViewModel = viewModel(),
        onWelcomeAnimationCompleted: (() -> Unit)? = null
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

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.action_undo)

    val showUndoSnackbar: (String, () -> Unit) -> Unit = { message, onUndo ->
        snackbarScope.launch {
            val result =
                    snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short
                    )
            if (result == SnackbarResult.ActionPerformed) {
                onUndo()
            }
        }
    }

    val onHideAppWithUndo: (AppInfo) -> Unit = { app ->
        val isSearching = uiState.query.isNotBlank()
        viewModel.hideApp(app)
        val messageRes =
                if (isSearching) R.string.toast_excluded_from_results
                else R.string.toast_excluded_from_suggestions
        showUndoSnackbar(context.getString(messageRes, app.appName)) {
            if (isSearching) {
                viewModel.unhideAppFromResults(app)
            } else {
                viewModel.unhideAppFromSuggestions(app)
            }
        }
    }

    val onExcludeContactWithUndo: (ContactInfo) -> Unit = { contact ->
        viewModel.excludeContact(contact)
        showUndoSnackbar(
                context.getString(R.string.toast_excluded_from_results, contact.displayName)
        ) {
            viewModel.removeExcludedContact(contact)
        }
    }

    val onExcludeFileWithUndo: (DeviceFile) -> Unit = { file ->
        viewModel.excludeFile(file)
        showUndoSnackbar(
                context.getString(R.string.toast_excluded_from_results, file.displayName)
        ) {
            viewModel.removeExcludedFile(file)
        }
    }

    val onExcludeFileExtensionWithUndo: (DeviceFile) -> Unit = { file ->
        val extension = FileUtils.getFileExtension(file.displayName)
        if (extension != null) {
            viewModel.excludeFileExtension(file)
            val extensionLabel = ".${extension} files"
            showUndoSnackbar(
                    context.getString(R.string.toast_excluded_from_results, extensionLabel)
            ) {
                viewModel.removeExcludedFileExtension(extension)
            }
        }
    }

    val onExcludeSettingWithUndo: (DeviceSetting) -> Unit = { setting ->
        viewModel.excludeSetting(setting)
        showUndoSnackbar(
                context.getString(R.string.toast_excluded_from_results, setting.title)
        ) {
            viewModel.removeExcludedSetting(setting)
        }
    }

    val onExcludeAppShortcutWithUndo: (StaticShortcut) -> Unit = { shortcut ->
        viewModel.excludeAppShortcut(shortcut)
        showUndoSnackbar(
                context.getString(
                        R.string.toast_excluded_from_results,
                        shortcutDisplayName(shortcut)
                )
        ) {
            viewModel.removeExcludedAppShortcut(shortcut)
        }
    }

    // Set up toast callback for ViewModel
    val showToast: (Int) -> Unit = { stringResId ->
        android.widget.Toast.makeText(
                        context,
                        context.getString(stringResId),
                        android.widget.Toast.LENGTH_SHORT
                )
                .show()
    }

    // UI feedback is now handled by UiFeedbackService in the ViewModel

    // Wrapper function that calls directly - performCall will handle permission check and fallback
    // to dialer
    val callContactWithPermission: (ContactInfo) -> Unit = { contact ->
        viewModel.callContact(contact)
    }

    val showContactMethodsBottomSheet: (ContactInfo) -> Unit = { contact ->
        viewModel.showContactMethodsBottomSheet(contact)
    }

    val dismissContactMethodsBottomSheet: () -> Unit = {
        viewModel.dismissContactMethodsBottomSheet()
    }

    val callPermissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
            ) { isGranted -> viewModel.onCallPermissionResult(isGranted) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.handleOnResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.pendingDirectCallNumber, uiState.pendingWhatsAppCallDataId) {
        val pendingNumber = uiState.pendingDirectCallNumber
        val pendingWhatsAppCall = uiState.pendingWhatsAppCallDataId

        if (pendingNumber != null || pendingWhatsAppCall != null) {
            if (context is android.app.Activity) {
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            } else {
                viewModel.onCallPermissionResult(false)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SearchScreen(
                modifier = Modifier.fillMaxSize(),
                state = uiState,
                onQueryChanged = viewModel::onQueryChange,
                onClearQuery = viewModel::clearQuery,
                onRequestUsagePermission = viewModel::openUsageAccessSettings,
                onSettingsClick = onSettingsClick,
                onAppClick = viewModel::launchApp,
                onAppInfoClick = viewModel::openAppInfo,
                onUninstallClick = viewModel::requestUninstall,
                onHideApp = onHideAppWithUndo,
                onPinApp = viewModel::pinApp,
                onUnpinApp = viewModel::unpinApp,
                onContactClick = viewModel::openContact,
                onShowContactMethods = showContactMethodsBottomSheet,
                onDismissContactMethods = dismissContactMethodsBottomSheet,
                onCallContact = callContactWithPermission,
                onSmsContact = viewModel::smsContact,
                onContactMethodClick = viewModel::handleContactMethod,
                onFileClick = viewModel::openFile,
                onPinContact = viewModel::pinContact,
                onUnpinContact = viewModel::unpinContact,
                onExcludeContact = onExcludeContactWithUndo,
                onPinFile = viewModel::pinFile,
                onUnpinFile = viewModel::unpinFile,
                onExcludeFile = onExcludeFileWithUndo,
                onExcludeFileExtension = onExcludeFileExtensionWithUndo,
                onSettingClick = viewModel::openSetting,
                onPinSetting = viewModel::pinSetting,
                onUnpinSetting = viewModel::unpinSetting,
                onExcludeSetting = onExcludeSettingWithUndo,
                onAppShortcutClick = viewModel::launchAppShortcut,
                onPinAppShortcut = viewModel::pinAppShortcut,
                onUnpinAppShortcut = viewModel::unpinAppShortcut,
                onExcludeAppShortcut = onExcludeAppShortcutWithUndo,
                onIncludeAppShortcut = viewModel::removeExcludedAppShortcut,
                onAppShortcutAppInfoClick = { shortcut -> viewModel.openAppInfo(shortcut.packageName) },
                onPhoneNumberSelected = viewModel::onPhoneNumberSelected,
                onDismissPhoneNumberSelection = viewModel::dismissPhoneNumberSelection,
                onSearchTargetClick = { query, target -> viewModel.openSearchTarget(query, target) },
                onSearchEngineLongPress = onSearchEngineLongPress,
                onDirectSearchEmailClick = viewModel::openEmail,
                onOpenAppSettings = viewModel::openAppSettings,
                onOpenStorageAccessSettings = viewModel::openAllFilesAccessSettings,
                onAppNicknameClick = { app ->
                    // This will be handled by the dialog state in SearchScreen
                },
                onClearDetectedShortcut = viewModel::clearDetectedShortcut,
                onContactNicknameClick = { contact ->
                    // This will be handled by the dialog state in SearchScreen
                },
                onFileNicknameClick = { file ->
                    // This will be handled by the dialog state in SearchScreen
                },
                getAppNickname = getAppNickname,
                getContactNickname = getContactNickname,
                getFileNickname = getFileNickname,
                onSaveAppNickname = viewModel::setAppNickname,
                onSaveContactNickname = viewModel::setContactNickname,
                onSaveFileNickname = viewModel::setFileNickname,
                getSettingNickname = getSettingNickname,
                onSaveSettingNickname = viewModel::setSettingNickname,
                getLastShownPhoneNumber = viewModel::getLastShownPhoneNumber,
                setLastShownPhoneNumber = viewModel::setLastShownPhoneNumber,
                onDirectDialChoiceSelected = viewModel::onDirectDialChoiceSelected,
                onDismissDirectDialChoice = viewModel::dismissDirectDialChoice,
                onReleaseNotesAcknowledged = viewModel::acknowledgeReleaseNotes,
                onWebSuggestionClick = { suggestion: String ->
                    viewModel.onWebSuggestionTap(suggestion)
                },
                onSearchEngineOnboardingDismissed = viewModel::onSearchEngineOnboardingDismissed,
                onContactActionHintDismissed = viewModel::onContactActionHintDismissed,
                onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
                onDeleteRecentQuery = viewModel::deleteRecentQuery,
                onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
                onCustomAction = viewModel::onCustomAction,
                getPrimaryContactCardAction = viewModel::getPrimaryContactCardAction,
                getSecondaryContactCardAction = viewModel::getSecondaryContactCardAction,
                onSavePrimaryContactCardAction = viewModel::setPrimaryContactCardAction,
                onSaveSecondaryContactCardAction = viewModel::setSecondaryContactCardAction
        )

        SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    val message = data.visuals.message
                    val marker = " excluded from "
                    val markerIndex = message.indexOf(marker)
                    val annotatedMessage =
                            if (markerIndex > 0) {
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(message.substring(0, markerIndex))
                                    }
                                    append(message.substring(markerIndex))
                                }
                            } else {
                                AnnotatedString(message)
                            }
                    val actionLabel = data.visuals.actionLabel
                    Snackbar(
                            action =
                                    actionLabel?.let { label ->
                                        {
                                            TextButton(onClick = { data.performAction() }) {
                                                Text(text = label)
                                            }
                                        }
                                    },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            actionContentColor = MaterialTheme.colorScheme.primary,
                            shape = DesignTokens.ShapeLarge
                    ) {
                        Text(
                                text = annotatedMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier =
                        Modifier.align(Alignment.BottomCenter)
                                .imePadding()
                                .padding(
                                        start = DesignTokens.SpacingLarge,
                                        end = DesignTokens.SpacingLarge,
                                        bottom = DesignTokens.SpacingHuge
                                )
        )
    }
}

data class ContactActionPickerDialogState(val contact: ContactInfo, val isPrimary: Boolean, val currentAction: com.tk.quicksearch.search.contacts.models.ContactCardAction?)

@Composable
fun SearchScreen(
        modifier: Modifier = Modifier,
        state: SearchUiState,
        onQueryChanged: (String) -> Unit,
        onClearQuery: () -> Unit,
        onSettingsClick: () -> Unit,
        onRequestUsagePermission: () -> Unit,
        onAppClick: (AppInfo) -> Unit,
        onAppInfoClick: (AppInfo) -> Unit,
        onUninstallClick: (AppInfo) -> Unit,
        onHideApp: (AppInfo) -> Unit,
        onPinApp: (AppInfo) -> Unit,
        onUnpinApp: (AppInfo) -> Unit,
        onContactClick: (ContactInfo) -> Unit,
        onShowContactMethods: (ContactInfo) -> Unit,
        onDismissContactMethods: () -> Unit,
        onCallContact: (ContactInfo) -> Unit,
        onSmsContact: (ContactInfo) -> Unit,
        onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
        onFileClick: (DeviceFile) -> Unit,
        onPinContact: (ContactInfo) -> Unit,
        onUnpinContact: (ContactInfo) -> Unit,
        onExcludeContact: (ContactInfo) -> Unit,
        onPinFile: (DeviceFile) -> Unit,
        onUnpinFile: (DeviceFile) -> Unit,
        onExcludeFile: (DeviceFile) -> Unit,
        onExcludeFileExtension: (DeviceFile) -> Unit,
        onSettingClick: (DeviceSetting) -> Unit,
        onPinSetting: (DeviceSetting) -> Unit,
        onUnpinSetting: (DeviceSetting) -> Unit,
        onExcludeSetting: (DeviceSetting) -> Unit,
        onAppShortcutClick: (StaticShortcut) -> Unit,
        onPinAppShortcut: (StaticShortcut) -> Unit,
        onUnpinAppShortcut: (StaticShortcut) -> Unit,
        onExcludeAppShortcut: (StaticShortcut) -> Unit,
        onIncludeAppShortcut: (StaticShortcut) -> Unit,
        onAppShortcutAppInfoClick: (StaticShortcut) -> Unit,
        onSearchTargetClick: (String, SearchTarget) -> Unit,
        onSearchEngineLongPress: () -> Unit,
        onDirectSearchEmailClick: (String) -> Unit,
        onWelcomeAnimationCompleted: (() -> Unit)? = null,
        onOpenAppSettings: () -> Unit,
        onOpenStorageAccessSettings: () -> Unit,
        onPhoneNumberSelected: (String, Boolean) -> Unit,
        onDismissPhoneNumberSelection: () -> Unit,
        onAppNicknameClick: (AppInfo) -> Unit,
        onContactNicknameClick: (ContactInfo) -> Unit,
        onFileNicknameClick: (DeviceFile) -> Unit,
        getAppNickname: (String) -> String?,
        getContactNickname: (Long) -> String?,
        getFileNickname: (String) -> String?,
        onSaveAppNickname: (AppInfo, String?) -> Unit,
        onSaveContactNickname: (ContactInfo, String?) -> Unit,
        onSaveFileNickname: (DeviceFile, String?) -> Unit,
        getSettingNickname: (String) -> String?,
        onSaveSettingNickname: (DeviceSetting, String?) -> Unit,
        getLastShownPhoneNumber: (Long) -> String?,
        setLastShownPhoneNumber: (Long, String) -> Unit,
        onDirectDialChoiceSelected: (DirectDialOption, Boolean) -> Unit,
        onDismissDirectDialChoice: () -> Unit,
        onReleaseNotesAcknowledged: () -> Unit,
        onWebSuggestionClick: (String) -> Unit = {},
        onSearchEngineOnboardingDismissed: () -> Unit = {},
        onContactActionHintDismissed: () -> Unit = {},
        onClearDetectedShortcut: () -> Unit = {},
        onCustomizeSearchEnginesClick: () -> Unit = {},
        onDeleteRecentQuery: (String) -> Unit = {},
        onCustomAction: (ContactInfo, ContactCardAction) -> Unit,
        getPrimaryContactCardAction: (Long) -> ContactCardAction?,
        getSecondaryContactCardAction: (Long) -> ContactCardAction?,
        onSavePrimaryContactCardAction: (Long, ContactCardAction) -> Unit,
        onSaveSecondaryContactCardAction: (Long, ContactCardAction) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val directAnswerContactName = stringResource(R.string.direct_answer_contact_name)

    var wallpaperBitmap by remember {
        mutableStateOf<ImageBitmap?>(WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap())
    }

    LaunchedEffect(Unit) {
        // Only load if not already cached
        if (wallpaperBitmap == null) {
            val bitmap = WallpaperUtils.getWallpaperBitmap(context)
            wallpaperBitmap = bitmap?.asImageBitmap()
        }
    }

    val derivedState = rememberDerivedState(state)

    // Section expansion state
    var expandedSection by remember { mutableStateOf<ExpandedSection>(ExpandedSection.NONE) }
    val scrollState = rememberScrollState()
    val showDirectSearch = state.DirectSearchState.status != DirectSearchStatus.Idle
    val alignResultsToBottom =
            state.oneHandedMode && expandedSection == ExpandedSection.NONE && !showDirectSearch

    // Nickname dialog state
    var nicknameDialogState by remember { mutableStateOf<NicknameDialogState?>(null) }

    // Contact Action Picker state
    var contactActionPickerDialogState by remember {
        mutableStateOf<ContactActionPickerDialogState?>(null)
    }

    // Keyboard switching state
    var manuallySwitchedToNumberKeyboard by remember { mutableStateOf(false) }

    // Reset expansion when query changes
    LaunchedEffect(state.query) { expandedSection = ExpandedSection.NONE }

    // Handle back button when section is expanded
    BackHandler(enabled = expandedSection != ExpandedSection.NONE) {
        keyboardController?.show()
        expandedSection = ExpandedSection.NONE
    }

    // Handle scroll behavior for one-handed mode
    OneHandedModeScrollBehavior(
            scrollState = scrollState,
            expandedSection = expandedSection,
            oneHandedMode = state.oneHandedMode,
            query = state.query,
            displayAppsSize = derivedState.displayApps.size,
            contactResultsSize = state.contactResults.size,
            appShortcutResultsSize = state.appShortcutResults.size,
            fileResultsSize = state.fileResults.size,
            pinnedContactsSize = state.pinnedContacts.size,
            pinnedAppShortcutsSize = state.pinnedAppShortcuts.size,
            pinnedFilesSize = state.pinnedFiles.size,
            settingResultsSize = state.settingResults.size,
            pinnedSettingsSize = state.pinnedSettings.size,
            hasUsagePermission = state.hasUsagePermission,
            errorMessage = state.errorMessage,
            reverseScrolling = alignResultsToBottom
    )

    val sectionParams =
            buildSectionParams(
                    state = state,
                    derivedState = derivedState,
                    onFileClick = onFileClick,
                    onPinFile = onPinFile,
                    onUnpinFile = onUnpinFile,
                    onExcludeFile = onExcludeFile,
                    onExcludeFileExtension = onExcludeFileExtension,
                    onOpenStorageAccessSettings = onOpenStorageAccessSettings,
                    onSettingClick = onSettingClick,
                    onPinSetting = onPinSetting,
                    onUnpinSetting = onUnpinSetting,
                    onExcludeSetting = onExcludeSetting,
                    onAppShortcutClick = onAppShortcutClick,
                    onPinAppShortcut = onPinAppShortcut,
                    onUnpinAppShortcut = onUnpinAppShortcut,
                    onExcludeAppShortcut = onExcludeAppShortcut,
                    onIncludeAppShortcut = onIncludeAppShortcut,
                    onAppShortcutAppInfoClick = onAppShortcutAppInfoClick,
                    onContactClick = onContactClick,
                    onShowContactMethods = onShowContactMethods,
                    onCallContact = onCallContact,
                    onSmsContact = onSmsContact,
                    onContactMethodClick = onContactMethodClick,
                    onPinContact = onPinContact,
                    onUnpinContact = onUnpinContact,
                    onExcludeContact = onExcludeContact,
                    onOpenAppSettings = onOpenAppSettings,
                    onAppClick = onAppClick,
                    onAppInfoClick = onAppInfoClick,
                    onUninstallClick = onUninstallClick,
                    onHideApp = onHideApp,
                    onPinApp = onPinApp,
                    onUnpinApp = onUnpinApp,
                    getFileNickname = getFileNickname,
                    getContactNickname = getContactNickname,
                    getSettingNickname = getSettingNickname,
                    getAppNickname = getAppNickname,
                    onPrimaryActionLongPress = { contact ->
                        val currentAction = getPrimaryContactCardAction(contact.contactId)
                        val defaultAction = if (currentAction == null) {
                            // Default primary action is phone call
                            contact.phoneNumbers.firstOrNull()?.let {
                                com.tk.quicksearch.search.contacts.models.ContactCardAction.Phone(it)
                            }
                        } else currentAction
                        contactActionPickerDialogState =
                                ContactActionPickerDialogState(contact, true, defaultAction)
                    },
                    onSecondaryActionLongPress = { contact ->
                        val currentAction = getSecondaryContactCardAction(contact.contactId)
                        val defaultAction = if (currentAction == null) {
                            // Default secondary action depends on messaging app
                            contact.phoneNumbers.firstOrNull()?.let { phoneNumber ->
                                when (state.messagingApp) {
                                    MessagingApp.MESSAGES -> com.tk.quicksearch.search.contacts.models.ContactCardAction.Sms(phoneNumber)
                                    MessagingApp.WHATSAPP -> com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppMessage(phoneNumber)
                                    MessagingApp.TELEGRAM -> com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramMessage(phoneNumber)
                                }
                            }
                        } else currentAction
                        contactActionPickerDialogState =
                                ContactActionPickerDialogState(contact, false, defaultAction as com.tk.quicksearch.search.contacts.models.ContactCardAction?)
                    },
                    onCustomAction = onCustomAction,
                    getPrimaryContactCardAction = getPrimaryContactCardAction,
                    getSecondaryContactCardAction = getSecondaryContactCardAction,
                    onContactActionHintDismissed = onContactActionHintDismissed,
                    onUpdateNicknameDialogState = { newState -> nicknameDialogState = newState },
                    onUpdateExpandedSection = { newSection ->
                        expandedSection = newSection
                        if (newSection == ExpandedSection.NONE) {
                            keyboardController?.show()
                        } else {
                            keyboardController?.hide()
                        }
                    },
                    expandedSection = expandedSection
            )

    val renderingState =
            SectionRenderingState(
                    isSearching = derivedState.isSearching,
                    expandedSection = expandedSection,
                    hasAppResults = derivedState.hasAppResults,
                    hasAppShortcutResults = derivedState.hasAppShortcutResults,
                    hasContactResults = derivedState.hasContactResults,
                    hasFileResults = derivedState.hasFileResults,
                    hasSettingResults = derivedState.hasSettingResults,
                    hasPinnedAppShortcuts = derivedState.hasPinnedAppShortcuts,
                    hasPinnedContacts = derivedState.hasPinnedContacts,
                    hasPinnedFiles = derivedState.hasPinnedFiles,
                    hasPinnedSettings = derivedState.hasPinnedSettings,
                    shouldShowApps = derivedState.shouldShowApps,
                    shouldShowAppShortcuts = derivedState.shouldShowAppShortcuts,
                    shouldShowContacts = derivedState.shouldShowContacts,
                    shouldShowFiles = derivedState.shouldShowFiles,
                    shouldShowSettings = derivedState.shouldShowSettings,
                    autoExpandFiles = derivedState.autoExpandFiles,
                    autoExpandContacts = derivedState.autoExpandContacts,
                    autoExpandSettings = derivedState.autoExpandSettings,
                    autoExpandAppShortcuts = derivedState.autoExpandAppShortcuts,
                    hasMultipleExpandableSections = derivedState.hasMultipleExpandableSections,
                    displayApps = derivedState.displayApps,
                    appShortcutResults = state.appShortcutResults,
                    contactResults = state.contactResults,
                    fileResults = state.fileResults,
                    settingResults = state.settingResults,
                    pinnedAppShortcuts = state.pinnedAppShortcuts,
                    pinnedContacts = state.pinnedContacts,
                    pinnedFiles = state.pinnedFiles,
                    pinnedSettings = state.pinnedSettings,
                    orderedSections = derivedState.orderedSections,
                    shortcutDetected = state.detectedShortcutTarget != null
            )

    Box(modifier = modifier.fillMaxSize()) {
        // Background
        SearchScreenBackground(
                showWallpaperBackground = state.showWallpaperBackground,
                wallpaperBitmap = wallpaperBitmap,
                wallpaperBackgroundAlpha = state.wallpaperBackgroundAlpha,
                wallpaperBlurRadius = state.wallpaperBlurRadius
        )

        // Main content
        SearchScreenContent(
                modifier = Modifier.fillMaxSize(),
                state = state,
                renderingState = renderingState,
                contactsParams = sectionParams.contactsParams,
                filesParams = sectionParams.filesParams,
                appShortcutsParams = sectionParams.appShortcutsParams,
                settingsParams = sectionParams.settingsParams,
                appsParams = sectionParams.appsParams,
                onQueryChanged = onQueryChanged,
                onClearQuery = onClearQuery,
                onSettingsClick = onSettingsClick,
                onAppClick = onAppClick,
                onRequestUsagePermission = onRequestUsagePermission,
                onSearchTargetClick = onSearchTargetClick,
                onSearchEngineLongPress = onSearchEngineLongPress,
                onDirectSearchEmailClick = onDirectSearchEmailClick,
                onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
                onPhoneNumberClick = { phoneNumber ->
                    // Create a temporary ContactInfo to use the call functionality
                    val tempContact =
                            ContactInfo(
                                    contactId = -1L,
                                    lookupKey = "",
                                    displayName = directAnswerContactName,
                                    phoneNumbers = listOf(phoneNumber)
                            )
                    onCallContact(tempContact)
                },
                onWebSuggestionClick = onWebSuggestionClick,
                onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
                onDeleteRecentQuery = onDeleteRecentQuery,
                onKeyboardSwitchToggle = {
                    manuallySwitchedToNumberKeyboard = !manuallySwitchedToNumberKeyboard
                },
                expandedSection = expandedSection,
                manuallySwitchedToNumberKeyboard = manuallySwitchedToNumberKeyboard,
                scrollState = scrollState,
                onClearDetectedShortcut = onClearDetectedShortcut
        )

        // Search engine onboarding overlay
        SearchEngineOnboardingOverlay(
                visible = state.showSearchEngineOnboarding,
                onDismiss = onSearchEngineOnboardingDismissed
        )
    }

    // All dialogs
    SearchScreenDialogs(
            state = state,
            nicknameDialogState = nicknameDialogState,
            onPhoneNumberSelected = onPhoneNumberSelected,
            onDismissPhoneNumberSelection = onDismissPhoneNumberSelection,
            onDirectDialChoiceSelected = onDirectDialChoiceSelected,
            onDismissDirectDialChoice = onDismissDirectDialChoice,
            onContactMethodClick = onContactMethodClick,
            onDismissContactMethods = onDismissContactMethods,
            onReleaseNotesAcknowledged = onReleaseNotesAcknowledged,
            onDismissNicknameDialog = { nicknameDialogState = null },
            onSaveAppNickname = { app, nickname ->
                onSaveAppNickname(app, nickname)
                nicknameDialogState = null
            },
            onSaveContactNickname = { contact, nickname ->
                onSaveContactNickname(contact, nickname)
                nicknameDialogState = null
            },
            onSaveFileNickname = { file, nickname ->
                onSaveFileNickname(file, nickname)
                nicknameDialogState = null
            },
            onSaveSettingNickname = { setting, nickname ->
                onSaveSettingNickname(setting, nickname)
                nicknameDialogState = null
            },
            getLastShownPhoneNumber = getLastShownPhoneNumber,
            setLastShownPhoneNumber = setLastShownPhoneNumber
    )

    // Render Contact Action Picker Dialog
    contactActionPickerDialogState?.let { pickerState ->
        ContactActionPickerDialog(
                contactInfo = pickerState.contact,
                currentAction = pickerState.currentAction,
                onActionSelected = { action ->
                    if (pickerState.isPrimary) {
                        onSavePrimaryContactCardAction(pickerState.contact.contactId, action)
                    } else {
                        onSaveSecondaryContactCardAction(pickerState.contact.contactId, action)
                    }
                    contactActionPickerDialogState = null
                },
                onDismiss = { contactActionPickerDialogState = null },
                getLastShownPhoneNumber = getLastShownPhoneNumber,
                setLastShownPhoneNumber = setLastShownPhoneNumber
        )
    }
}
