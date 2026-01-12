package com.tk.quicksearch.search.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.layout
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.DirectDialOption
import com.tk.quicksearch.search.core.DirectSearchState
import com.tk.quicksearch.search.core.DirectSearchStatus
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.ContactMethod
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.util.CalculatorUtils
import com.tk.quicksearch.util.WallpaperUtils
import com.tk.quicksearch.search.contacts.PhoneNumberSelectionDialog
import com.tk.quicksearch.search.contacts.DirectDialChoiceDialog
import com.tk.quicksearch.search.contacts.ContactMethodsDialog


@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onSearchEngineLongPress: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    viewModel: SearchViewModel = viewModel(),
    onWelcomeAnimationCompleted: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // Wrapper function that calls directly - performCall will handle permission check and fallback to dialer
    val callContactWithPermission: (ContactInfo) -> Unit = { contact ->
        viewModel.callContact(contact)
    }

    val showContactMethodsBottomSheet: (ContactInfo) -> Unit = { contact ->
        viewModel.showContactMethodsBottomSheet(contact)
    }

    val dismissContactMethodsBottomSheet: () -> Unit = {
        viewModel.dismissContactMethodsBottomSheet()
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onCallPermissionResult(isGranted)
    }

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

    SearchScreen(
        modifier = modifier,
        state = uiState,
        onQueryChanged = viewModel::onQueryChange,
        onClearQuery = viewModel::clearQuery,
        onRequestUsagePermission = viewModel::openUsageAccessSettings,
        onSettingsClick = onSettingsClick,
        onAppClick = viewModel::launchApp,
        onAppInfoClick = viewModel::openAppInfo,
        onUninstallClick = viewModel::requestUninstall,
        onHideApp = viewModel::hideApp,
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
        onExcludeContact = viewModel::excludeContact,
        onPinFile = viewModel::pinFile,
        onUnpinFile = viewModel::unpinFile,
        onExcludeFile = viewModel::excludeFile,
        onExcludeFileExtension = viewModel::excludeFileExtension,
        onSettingClick = viewModel::openSetting,
        onPinSetting = viewModel::pinSetting,
        onUnpinSetting = viewModel::unpinSetting,
        onExcludeSetting = viewModel::excludeSetting,
        onPhoneNumberSelected = viewModel::onPhoneNumberSelected,
        onDismissPhoneNumberSelection = viewModel::dismissPhoneNumberSelection,
        onSearchEngineClick = { query, engine -> viewModel.openSearchUrl(query, engine) },
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
        getAppNickname = viewModel::getAppNickname,
        getContactNickname = viewModel::getContactNickname,
        getFileNickname = viewModel::getFileNickname,
        onSaveAppNickname = viewModel::setAppNickname,
        onSaveContactNickname = viewModel::setContactNickname,
        onSaveFileNickname = viewModel::setFileNickname,
        getSettingNickname = viewModel::getSettingNickname,
        onSaveSettingNickname = viewModel::setSettingNickname,
        onDirectDialChoiceSelected = viewModel::onDirectDialChoiceSelected,
        onDismissDirectDialChoice = viewModel::dismissDirectDialChoice,
        onReleaseNotesAcknowledged = viewModel::acknowledgeReleaseNotes,
        onWebSuggestionClick = { suggestion: String ->
            viewModel.onWebSuggestionTap(suggestion)
        },
        onSearchEngineOnboardingDismissed = viewModel::onSearchEngineOnboardingDismissed,
        onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
        onWelcomeAnimationCompleted = onWelcomeAnimationCompleted
    )
}

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
    onSettingClick: (SettingShortcut) -> Unit,
    onPinSetting: (SettingShortcut) -> Unit,
    onUnpinSetting: (SettingShortcut) -> Unit,
    onExcludeSetting: (SettingShortcut) -> Unit,
    onSearchEngineClick: (String, SearchEngine) -> Unit,
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
    onSaveSettingNickname: (SettingShortcut, String?) -> Unit,
    onDirectDialChoiceSelected: (DirectDialOption, Boolean) -> Unit,
    onDismissDirectDialChoice: () -> Unit,
    onReleaseNotesAcknowledged: () -> Unit,
    onWebSuggestionClick: (String) -> Unit = {},
    onSearchEngineOnboardingDismissed: () -> Unit = {},
    onClearDetectedShortcut: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val directAnswerContactName = stringResource(R.string.direct_answer_contact_name)

    var wallpaperBitmap by remember {
        mutableStateOf<ImageBitmap?>(
            WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()
        )
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
    val alignResultsToBottom = state.keyboardAlignedLayout &&
        expandedSection == ExpandedSection.NONE &&
        !showDirectSearch

    // Nickname dialog state
    var nicknameDialogState by remember { mutableStateOf<NicknameDialogState?>(null) }

    // Keyboard switching state
    var manuallySwitchedToNumberKeyboard by remember { mutableStateOf(false) }

    // Reset expansion when query changes
    LaunchedEffect(state.query) {
        expandedSection = ExpandedSection.NONE
    }

    // Handle back button when section is expanded
    BackHandler(enabled = expandedSection != ExpandedSection.NONE) {
        keyboardController?.show()
        expandedSection = ExpandedSection.NONE
    }

    // Handle scroll behavior for keyboard-aligned layout
    KeyboardAlignedScrollBehavior(
        scrollState = scrollState,
        expandedSection = expandedSection,
        keyboardAlignedLayout = state.keyboardAlignedLayout,
        query = state.query,
        displayAppsSize = derivedState.displayApps.size,
        contactResultsSize = state.contactResults.size,
        fileResultsSize = state.fileResults.size,
        pinnedContactsSize = state.pinnedContacts.size,
        pinnedFilesSize = state.pinnedFiles.size,
        settingResultsSize = state.settingResults.size,
        pinnedSettingsSize = state.pinnedSettings.size,
        hasUsagePermission = state.hasUsagePermission,
        errorMessage = state.errorMessage,
        reverseScrolling = alignResultsToBottom
    )

    val sectionParams = buildSectionParams(
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
        onUpdateNicknameDialogState = { newState ->
            nicknameDialogState = newState
        },
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

    val renderingState = SectionRenderingState(
        isSearching = derivedState.isSearching,
        expandedSection = expandedSection,
        hasAppResults = derivedState.hasAppResults,
        hasContactResults = derivedState.hasContactResults,
        hasFileResults = derivedState.hasFileResults,
        hasSettingResults = derivedState.hasSettingResults,
        hasPinnedContacts = derivedState.hasPinnedContacts,
        hasPinnedFiles = derivedState.hasPinnedFiles,
        hasPinnedSettings = derivedState.hasPinnedSettings,
        shouldShowApps = derivedState.shouldShowApps,
        shouldShowContacts = derivedState.shouldShowContacts,
        shouldShowFiles = derivedState.shouldShowFiles,
        shouldShowSettings = derivedState.shouldShowSettings,
        autoExpandFiles = derivedState.autoExpandFiles,
        autoExpandContacts = derivedState.autoExpandContacts,
        autoExpandSettings = derivedState.autoExpandSettings,
        hasMultipleExpandableSections = derivedState.hasMultipleExpandableSections,
        displayApps = derivedState.displayApps,
        contactResults = state.contactResults,
        fileResults = state.fileResults,
        settingResults = state.settingResults,
        pinnedContacts = state.pinnedContacts,
        pinnedFiles = state.pinnedFiles,
        pinnedSettings = state.pinnedSettings,
        orderedSections = derivedState.orderedSections
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Background
        SearchScreenBackground(
            showWallpaperBackground = state.showWallpaperBackground,
            wallpaperBitmap = wallpaperBitmap
        )

        // Main content
        SearchScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = state,
            renderingState = renderingState,
            contactsParams = sectionParams.contactsParams,
            filesParams = sectionParams.filesParams,
            settingsParams = sectionParams.settingsParams,
            appsParams = sectionParams.appsParams,
            onQueryChanged = onQueryChanged,
            onClearQuery = onClearQuery,
            onSettingsClick = onSettingsClick,
            onAppClick = onAppClick,
            onRequestUsagePermission = onRequestUsagePermission,
            onSearchEngineClick = onSearchEngineClick,
            onSearchEngineLongPress = onSearchEngineLongPress,
            onDirectSearchEmailClick = onDirectSearchEmailClick,
            onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
            onPhoneNumberClick = { phoneNumber ->
                // Create a temporary ContactInfo to use the call functionality
                val tempContact = ContactInfo(
                    contactId = -1L,
                    lookupKey = "",
                    displayName = directAnswerContactName,
                    phoneNumbers = listOf(phoneNumber)
                )
                onCallContact(tempContact)
            },
            onWebSuggestionClick = onWebSuggestionClick,
            onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
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
        }
    )
}
