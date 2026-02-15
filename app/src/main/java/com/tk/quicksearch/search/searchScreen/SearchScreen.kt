package com.tk.quicksearch.search.searchScreen

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.dialogs.ContactActionPickerDialog
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.DirectDialOption
import com.tk.quicksearch.search.core.DirectSearchState
import com.tk.quicksearch.search.core.DirectSearchStatus
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.directSearch.GeminiModelCatalog
import com.tk.quicksearch.search.directSearch.GeminiModelPickerDialog
import com.tk.quicksearch.search.directSearch.GeminiTextModel
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.shortcutDisplayName
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.recentSearches.RecentSearchEntry
import com.tk.quicksearch.search.searchScreen.dialogs.NicknameDialogState
import com.tk.quicksearch.search.searchScreen.dialogs.SearchScreenDialogs
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.WallpaperUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ExcludeUndoSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
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
                shape = DesignTokens.ShapeLarge,
            ) {
                Text(
                    text = annotatedMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onSearchEngineLongPress: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onOpenDirectSearchConfigure: () -> Unit = {},
    onOverlayDismissRequest: (() -> Unit)? = null,
    onShowToast: (Int) -> Unit = {},
    viewModel: SearchViewModel = viewModel(),
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    onWallpaperLoaded: (() -> Unit)? = null,
    isOverlayPresentation: Boolean = false,
    overlaySnackbarHostState: SnackbarHostState? = null,
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
                    duration = SnackbarDuration.Short,
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

    val onExcludeContactWithUndo: (ContactInfo) -> Unit = { contact ->
        viewModel.excludeContact(contact)
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, contact.displayName),
        ) {
            viewModel.removeExcludedContact(contact)
        }
    }

    val onExcludeFileWithUndo: (DeviceFile) -> Unit = { file ->
        viewModel.excludeFile(file)
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, file.displayName),
        ) {
            viewModel.removeExcludedFile(file)
        }
    }

    val onExcludeFileExtensionWithUndo: (DeviceFile) -> Unit = { file ->
        val extension = FileUtils.getFileExtension(file.displayName)
        if (extension != null) {
            viewModel.excludeFileExtension(file)
            val extensionLabel = ".$extension files"
            showUndoSnackbar(
                context.getString(R.string.toast_excluded_from_results, extensionLabel),
            ) {
                viewModel.removeExcludedFileExtension(extension)
            }
        }
    }

    val onExcludeSettingWithUndo: (DeviceSetting) -> Unit = { setting ->
        viewModel.excludeSetting(setting)
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, setting.title),
        ) {
            viewModel.removeExcludedSetting(setting)
        }
    }

    val onExcludeAppShortcutWithUndo: (StaticShortcut) -> Unit = { shortcut ->
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

    // Set up toast callback for ViewModel
    val showToast: (Int) -> Unit = { stringResId ->
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

    val callPermissionLauncher =
        if (context is android.app.Activity) {
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { isGranted -> viewModel.onCallPermissionResult(isGranted) }
        } else {
            null
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

    LaunchedEffect(uiState.pendingDirectCallNumber, uiState.pendingWhatsAppCallDataId) {
        val pendingNumber = uiState.pendingDirectCallNumber
        val pendingWhatsAppCall = uiState.pendingWhatsAppCallDataId

        if (pendingNumber != null || pendingWhatsAppCall != null) {
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

    Box(modifier = containerModifier) {
        SearchScreen(
            modifier =
                if (isOverlayPresentation) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.fillMaxSize()
                },
            state = uiState,
            onQueryChanged = viewModel::onQueryChange,
            onClearQuery = viewModel::clearQuery,
            onRequestUsagePermission = { viewModel.openUsageAccessSettings() },
            onSettingsClick = onSettingsClick,
            onAppClick = { app -> viewModel.launchApp(app) },
            onAppInfoClick = { app -> viewModel.openAppInfo(app) },
            onUninstallClick = { app -> viewModel.requestUninstall(app) },
            onHideApp = onHideAppWithUndo,
            onPinApp = viewModel::pinApp,
            onUnpinApp = viewModel::unpinApp,
            onContactClick = viewModel::openContact,
            onShowContactMethods = showContactMethodsBottomSheet,
            onDismissContactMethods = dismissContactMethodsBottomSheet,
            onCallContact = callContactWithPermission,
            onSmsContact = viewModel::smsContact,
            onContactMethodClick = viewModel::handleContactMethod,
            onFileClick = { file -> viewModel.openFile(file) },
            onOpenFolder = { file -> viewModel.openContainingFolder(file) },
            onPinContact = viewModel::pinContact,
            onUnpinContact = viewModel::unpinContact,
            onExcludeContact = onExcludeContactWithUndo,
            onPinFile = viewModel::pinFile,
            onUnpinFile = viewModel::unpinFile,
            onExcludeFile = onExcludeFileWithUndo,
            onExcludeFileExtension = onExcludeFileExtensionWithUndo,
            onSettingClick = { setting -> viewModel.openSetting(setting) },
            onPinSetting = viewModel::pinSetting,
            onUnpinSetting = viewModel::unpinSetting,
            onExcludeSetting = onExcludeSettingWithUndo,
            onAppShortcutClick = { shortcut -> viewModel.launchAppShortcut(shortcut) },
            onPinAppShortcut = viewModel::pinAppShortcut,
            onUnpinAppShortcut = viewModel::unpinAppShortcut,
            onExcludeAppShortcut = onExcludeAppShortcutWithUndo,
            onIncludeAppShortcut = viewModel::removeExcludedAppShortcut,
            onAppShortcutAppInfoClick = { shortcut -> viewModel.openAppInfo(shortcut.packageName) },
            onPhoneNumberSelected = viewModel::onPhoneNumberSelected,
            onDismissPhoneNumberSelection = viewModel::dismissPhoneNumberSelection,
            onSearchTargetClick = { query, target -> viewModel.openSearchTarget(query, target) },
            onSearchEngineLongPress = onSearchEngineLongPress,
            onDirectSearchEmailClick = { email -> viewModel.openEmail(email) },
            onSetPersonalContext = viewModel::setPersonalContext,
            onSetGeminiModel = viewModel::setGeminiModel,
            onSetGeminiGroundingEnabled = viewModel::setGeminiGroundingEnabled,
            onRefreshAvailableGeminiModels = viewModel::refreshAvailableGeminiModels,
            onOpenAppSettings = { viewModel.openAppSettings() },
            onOpenStorageAccessSettings = { viewModel.openAllFilesAccessSettings() },
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
            getAppShortcutNickname = getAppShortcutNickname,
            onSaveAppNickname = viewModel::setAppNickname,
            onSaveAppShortcutNickname = viewModel::setAppShortcutNickname,
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
            onPersonalContextHintDismissed = viewModel::onPersonalContextHintDismissed,
            onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
            onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
            onDeleteRecentItem = viewModel::deleteRecentItem,
            onDisableSearchHistory = { viewModel.setRecentQueriesEnabled(false) },
            onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
            onCustomAction = viewModel::onCustomAction,
            getPrimaryContactCardAction = viewModel::getPrimaryContactCardAction,
            getSecondaryContactCardAction = viewModel::getSecondaryContactCardAction,
            onSavePrimaryContactCardAction = viewModel::setPrimaryContactCardAction,
            onSaveSecondaryContactCardAction = viewModel::setSecondaryContactCardAction,
            onWallpaperLoaded = onWallpaperLoaded,
            isOverlayPresentation = isOverlayPresentation,
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
    }
}

data class ContactActionPickerDialogState(
    val contact: ContactInfo,
    val isPrimary: Boolean,
    val currentAction: com.tk.quicksearch.search.contacts.models.ContactCardAction?,
)

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
    onOpenFolder: (DeviceFile) -> Unit,
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
    onSetPersonalContext: (String?) -> Unit = {},
    onSetGeminiModel: (String?) -> Unit = {},
    onSetGeminiGroundingEnabled: (Boolean) -> Unit = {},
    onRefreshAvailableGeminiModels: () -> Unit = {},
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    onWallpaperLoaded: (() -> Unit)? = null,
    isOverlayPresentation: Boolean = false,
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
    getAppShortcutNickname: (String) -> String?,
    onSaveAppNickname: (AppInfo, String?) -> Unit,
    onSaveAppShortcutNickname: (StaticShortcut, String?) -> Unit,
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
    onPersonalContextHintDismissed: () -> Unit = {},
    onClearDetectedShortcut: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onOpenDirectSearchConfigure: () -> Unit = {},
    onConsumeContactActionRequest: () -> Unit = {},
    onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
    onDisableSearchHistory: () -> Unit = {},
    onCustomAction: (ContactInfo, ContactCardAction) -> Unit,
    getPrimaryContactCardAction: (Long) -> ContactCardAction?,
    getSecondaryContactCardAction: (Long) -> ContactCardAction?,
    onSavePrimaryContactCardAction: (Long, ContactCardAction) -> Unit,
    onSaveSecondaryContactCardAction: (Long, ContactCardAction) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val directAnswerContactName = stringResource(R.string.direct_answer_contact_name)

    var wallpaperBitmap by remember(isOverlayPresentation) {
        mutableStateOf<ImageBitmap?>(
            if (isOverlayPresentation) {
                null
            } else {
                WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()
            },
        )
    }

    LaunchedEffect(isOverlayPresentation, wallpaperBitmap) {
        // Overlay background is provided by OverlayRoot; skip duplicate wallpaper loads here.
        if (isOverlayPresentation || wallpaperBitmap != null) {
            return@LaunchedEffect
        }
        val bitmap = WallpaperUtils.getWallpaperBitmap(context)
        wallpaperBitmap = bitmap?.asImageBitmap()
        // If wallpaper loaded successfully, notify that it should be enabled
        if (bitmap != null) {
            onWallpaperLoaded?.invoke()
        }
    }

    val derivedState = rememberDerivedState(state)

    fun getDefaultContactAction(
        contact: ContactInfo,
        isPrimary: Boolean,
    ): ContactCardAction? {
        val currentAction =
            if (isPrimary) {
                getPrimaryContactCardAction(contact.contactId)
            } else {
                getSecondaryContactCardAction(contact.contactId)
            }
        if (currentAction != null) return currentAction

        val phoneNumber = contact.phoneNumbers.firstOrNull() ?: return null
        return if (isPrimary) {
            ContactCardAction.Phone(phoneNumber)
        } else {
            when (state.messagingApp) {
                MessagingApp.MESSAGES -> ContactCardAction.Sms(phoneNumber)
                MessagingApp.WHATSAPP -> ContactCardAction.WhatsAppMessage(phoneNumber)
                MessagingApp.TELEGRAM -> ContactCardAction.TelegramMessage(phoneNumber)
                MessagingApp.SIGNAL -> ContactCardAction.SignalMessage(phoneNumber)
            }
        }
    }

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
    val contactActionRequest = state.contactActionPickerRequest
    LaunchedEffect(contactActionRequest) {
        contactActionRequest?.let { request ->
            contactActionPickerDialogState =
                ContactActionPickerDialogState(
                    contact = request.contactInfo,
                    isPrimary = request.isPrimary,
                    currentAction = request.currentAction,
                )
            onConsumeContactActionRequest()
        }
    }

    // Keyboard switching state
    var manuallySwitchedToNumberKeyboard by remember { mutableStateOf(false) }

    var showPersonalContextDialog by remember { mutableStateOf(false) }
    var showGeminiModelDialog by remember { mutableStateOf(false) }
    var personalContextInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = state.personalContext,
                selection = TextRange(state.personalContext.length),
            ),
        )
    }
    val personalContextFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showPersonalContextDialog) {
        if (showPersonalContextDialog) {
            delay(100)
            personalContextInput =
                personalContextInput.copy(
                    selection = TextRange(personalContextInput.text.length),
                )
            personalContextFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Reset expansion when query changes
    LaunchedEffect(state.query) { expandedSection = ExpandedSection.NONE }

    val openPersonalContextDialog = {
        personalContextInput =
            TextFieldValue(
                text = state.personalContext,
                selection = TextRange(state.personalContext.length),
            )
        showPersonalContextDialog = true
    }

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
        reverseScrolling = alignResultsToBottom,
    )

    // Handle keyboard visibility based on scroll position when overlay mode is off
    ScrollBasedKeyboardBehavior(
        scrollState = scrollState,
        overlayModeEnabled = state.overlayModeEnabled,
        oneHandedMode = state.oneHandedMode,
        reverseScrolling = alignResultsToBottom,
    )

    val sectionParams =
        buildSectionParams(
            state = state,
            derivedState = derivedState,
            onFileClick = onFileClick,
            onOpenFolder = onOpenFolder,
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
            getAppShortcutNickname = getAppShortcutNickname,
            onPrimaryActionLongPress = { contact ->
                contactActionPickerDialogState =
                    ContactActionPickerDialogState(
                        contact,
                        true,
                        getDefaultContactAction(contact, true),
                    )
            },
            onSecondaryActionLongPress = { contact ->
                contactActionPickerDialogState =
                    ContactActionPickerDialogState(
                        contact,
                        false,
                        getDefaultContactAction(contact, false),
                    )
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
            expandedSection = expandedSection,
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
            shortcutDetected = state.detectedShortcutTarget != null,
        )

    val screenModifier =
        if (isOverlayPresentation) {
            modifier.fillMaxWidth()
        } else {
            modifier.fillMaxSize()
        }

    Box(modifier = screenModifier) {
        if (!isOverlayPresentation) {
            SearchScreenBackground(
                showWallpaperBackground = state.showWallpaperBackground,
                wallpaperBitmap = wallpaperBitmap,
                wallpaperBackgroundAlpha = state.wallpaperBackgroundAlpha,
                wallpaperBlurRadius = state.wallpaperBlurRadius,
                overlayThemeIntensity = state.overlayThemeIntensity,
            )
        }

        // Main content
        SearchScreenContent(
            modifier =
                if (isOverlayPresentation) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.fillMaxSize()
                },
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
            onOpenPersonalContextDialog = openPersonalContextDialog,
            onPersonalContextHintDismissed = onPersonalContextHintDismissed,
            onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
            onPhoneNumberClick = { phoneNumber ->
                // Create a temporary ContactInfo to use the call functionality
                val tempContact =
                    ContactInfo(
                        contactId = -1L,
                        lookupKey = "",
                        displayName = directAnswerContactName,
                        phoneNumbers = listOf(phoneNumber),
                    )
                onCallContact(tempContact)
            },
            onWebSuggestionClick = onWebSuggestionClick,
            onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
            onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
            onDeleteRecentItem = onDeleteRecentItem,
            onDisableSearchHistory = onDisableSearchHistory,
            onGeminiModelInfoClick = { showGeminiModelDialog = true },
            onKeyboardSwitchToggle = {
                manuallySwitchedToNumberKeyboard = !manuallySwitchedToNumberKeyboard
            },
            expandedSection = expandedSection,
            manuallySwitchedToNumberKeyboard = manuallySwitchedToNumberKeyboard,
            scrollState = scrollState,
            onClearDetectedShortcut = onClearDetectedShortcut,
            isOverlayPresentation = isOverlayPresentation,
        )

        // Search engine onboarding overlay
        SearchEngineOnboardingOverlay(
            visible = state.showSearchEngineOnboarding,
            onDismiss = onSearchEngineOnboardingDismissed,
            isOverlayPresentation = isOverlayPresentation,
            showStartSearchingButton = state.showStartSearchingOnOnboarding,
        )
    }

    if (showPersonalContextDialog) {
        AlertDialog(
            onDismissRequest = { showPersonalContextDialog = false },
            title = {
                Text(text = stringResource(R.string.settings_direct_search_personal_context_title))
            },
            text = {
                OutlinedTextField(
                    value = personalContextInput,
                    onValueChange = { personalContextInput = it },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp)
                            .focusRequester(personalContextFocusRequester),
                    placeholder = {
                        Text(text = stringResource(R.string.settings_direct_search_personal_context_hint))
                    },
                    shape = MaterialTheme.shapes.large,
                    singleLine = false,
                    minLines = 5,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = personalContextInput.text.trim()
                        onSetPersonalContext(trimmed.takeIf { it.isNotEmpty() })
                        showPersonalContextDialog = false
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPersonalContextDialog = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    if (showGeminiModelDialog) {
        val modelOptions = remember(state.geminiModel, state.availableGeminiModels) {
            val allKnown = state.availableGeminiModels + GeminiModelCatalog.FALLBACK_TEXT_MODELS
            val currentModel = allKnown.find { it.id == state.geminiModel }
                ?: GeminiTextModel(state.geminiModel, state.geminiModel)
            (state.availableGeminiModels + currentModel).distinctBy { it.id }
                .sortedBy { it.displayName.lowercase() }
        }
        GeminiModelPickerDialog(
            selectedModelId = state.geminiModel,
            models = modelOptions,
            groundingEnabled = state.geminiGroundingEnabled,
            onGroundingChange = onSetGeminiGroundingEnabled,
            onModelSelected = { modelId ->
                onSetGeminiModel(modelId)
                val newModel = modelOptions.firstOrNull { it.id == modelId }
                if (newModel?.supportsGrounding == false && state.geminiGroundingEnabled) {
                    onSetGeminiGroundingEnabled(false)
                }
            },
            onDismiss = {
                showGeminiModelDialog = false
                onRefreshAvailableGeminiModels()
            },
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
        onSaveAppShortcutNickname = { shortcut, nickname ->
            onSaveAppShortcutNickname(shortcut, nickname)
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
        setLastShownPhoneNumber = setLastShownPhoneNumber,
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
            setLastShownPhoneNumber = setLastShownPhoneNumber,
        )
    }
}
