package com.tk.quicksearch.search

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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

/**
 * Enum representing which section is currently expanded.
 */
enum class ExpandedSection {
    NONE,
    CONTACTS,
    FILES,
    SETTINGS
}

/**
 * Constants for search screen layout.
 */
private object SearchScreenConstants {
    const val INITIAL_RESULT_COUNT = 1
    const val ROW_COUNT = 2
    const val SEARCH_ROW_COUNT = 1
    const val COLUMNS = 5
}

/**
 * Data class holding all derived state calculations.
 */
private data class DerivedState(
    val isSearching: Boolean,
    val hasPinnedContacts: Boolean,
    val hasPinnedFiles: Boolean,
    val hasPinnedSettings: Boolean,
    val visibleRowCount: Int,
    val visibleAppLimit: Int,
    val displayApps: List<AppInfo>,
    val pinnedPackageNames: Set<String>,
    val pinnedSettingIds: Set<String>,
    val hasAppResults: Boolean,
    val hasContactResults: Boolean,
    val hasFileResults: Boolean,
    val hasSettingResults: Boolean,
    val pinnedContactIds: Set<Long>,
    val pinnedFileUris: Set<String>,
    val autoExpandFiles: Boolean,
    val autoExpandContacts: Boolean,
    val autoExpandSettings: Boolean,
    val hasMultipleExpandableSections: Boolean,
    val orderedSections: List<SearchSection>,
    val shouldShowApps: Boolean,
    val shouldShowContacts: Boolean,
    val shouldShowFiles: Boolean,
    val shouldShowSettings: Boolean
)

/**
 * Calculates all derived state from SearchUiState.
 */
@Composable
private fun rememberDerivedState(
    state: SearchUiState
): DerivedState {
    val isSearching = state.query.isNotBlank()
    val hasPinnedContacts = state.pinnedContacts.isNotEmpty() && state.hasContactPermission
    val hasPinnedFiles = state.pinnedFiles.isNotEmpty() && state.hasFilePermission
    val hasPinnedSettings = state.pinnedSettings.isNotEmpty()
    val visibleRowCount = if (isSearching || hasPinnedContacts || hasPinnedFiles || hasPinnedSettings) {
        SearchScreenConstants.SEARCH_ROW_COUNT
    } else {
        SearchScreenConstants.ROW_COUNT
    }
    val visibleAppLimit = visibleRowCount * SearchScreenConstants.COLUMNS
    
    val displayApps = remember(
        state.query,
        state.recentApps,
        state.searchResults,
        state.pinnedApps,
        visibleAppLimit
    ) {
        if (!isSearching) {
            val pinnedPackages = state.pinnedApps.map { it.packageName }.toSet()
            (state.pinnedApps + state.recentApps.filterNot { pinnedPackages.contains(it.packageName) })
                .take(visibleAppLimit)
        } else {
            state.searchResults.take(visibleAppLimit)
        }
    }
    
    val pinnedPackageNames = remember(state.pinnedApps) {
        state.pinnedApps.map { it.packageName }.toSet()
    }
    val hasAppResults = displayApps.isNotEmpty()
    val hasContactResults = state.contactResults.isNotEmpty()
    val hasFileResults = state.fileResults.isNotEmpty()
    val hasSettingResults = state.settingResults.isNotEmpty()
    val pinnedContactIds = remember(state.pinnedContacts) {
        state.pinnedContacts.map { it.contactId }.toSet()
    }
    val pinnedFileUris = remember(state.pinnedFiles) {
        state.pinnedFiles.map { it.uri.toString() }.toSet()
    }
    val pinnedSettingIds = remember(state.pinnedSettings) {
        state.pinnedSettings.map { it.id }.toSet()
    }
    val autoExpandFiles = (hasFileResults && !hasContactResults) || state.showAllResults
    val autoExpandContacts = (hasContactResults && !hasFileResults) || state.showAllResults
    val autoExpandSettings = (!isSearching && hasSettingResults && !hasContactResults && !hasFileResults) || state.showAllResults
    val hasMultipleExpandableSections = listOf(hasContactResults, hasFileResults, hasSettingResults).count { it } > 1
    
    val orderedSections = remember(state.sectionOrder, state.disabledSections) {
        state.sectionOrder.filter { it !in state.disabledSections }
    }
    
    val shouldShowApps = SearchSection.APPS !in state.disabledSections && hasAppResults
    val shouldShowContacts = SearchSection.CONTACTS !in state.disabledSections && 
        (!state.hasContactPermission || hasContactResults || hasPinnedContacts)
    val shouldShowFiles = SearchSection.FILES !in state.disabledSections && 
        (!state.hasFilePermission || hasFileResults || hasPinnedFiles)
    val shouldShowSettings = SearchSection.SETTINGS !in state.disabledSections &&
        (hasSettingResults || hasPinnedSettings)
    
    return DerivedState(
        isSearching = isSearching,
        hasPinnedContacts = hasPinnedContacts,
        hasPinnedFiles = hasPinnedFiles,
        hasPinnedSettings = hasPinnedSettings,
        visibleRowCount = visibleRowCount,
        visibleAppLimit = visibleAppLimit,
        displayApps = displayApps,
        pinnedPackageNames = pinnedPackageNames,
        pinnedSettingIds = pinnedSettingIds,
        hasAppResults = hasAppResults,
        hasContactResults = hasContactResults,
        hasFileResults = hasFileResults,
        hasSettingResults = hasSettingResults,
        pinnedContactIds = pinnedContactIds,
        pinnedFileUris = pinnedFileUris,
        autoExpandFiles = autoExpandFiles,
        autoExpandContacts = autoExpandContacts,
        autoExpandSettings = autoExpandSettings,
        hasMultipleExpandableSections = hasMultipleExpandableSections,
        orderedSections = orderedSections,
        shouldShowApps = shouldShowApps,
        shouldShowContacts = shouldShowContacts,
        shouldShowFiles = shouldShowFiles,
        shouldShowSettings = shouldShowSettings
    )
}

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onSearchEngineLongPress: () -> Unit = {},
    viewModel: SearchViewModel = viewModel()
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
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.handleOnResume()
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
        onRetryDirectSearch = viewModel::retryDirectSearch,
        onDirectSearchEmailClick = viewModel::openEmail,
        onOpenAppSettings = viewModel::openAppSettings,
        onOpenStorageAccessSettings = viewModel::openAllFilesAccessSettings,
        onAppNicknameClick = { app ->
            // This will be handled by the dialog state in SearchScreen
        },
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
        }
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
    onRetryDirectSearch: () -> Unit,
    onDirectSearchEmailClick: (String) -> Unit,
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
    onWebSuggestionClick: (String) -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    
    // Load wallpaper bitmap - check cache first for instant display
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
    
    // Calculate derived state
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
        // Reset keyboard state only when query is cleared completely
        if (state.query.isEmpty()) {
            manuallySwitchedToNumberKeyboard = false
        }
    }

    // Check for math expressions to determine pill visibility
    val hasMathExpression = CalculatorUtils.isMathExpression(state.query)

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

    val filesParams = FilesSectionParams(
        files = state.fileResults,
        hasPermission = state.hasFilePermission,
        isExpanded = expandedSection == ExpandedSection.FILES,
        pinnedFileUris = derivedState.pinnedFileUris,
        onFileClick = onFileClick,
        onRequestPermission = onOpenStorageAccessSettings,
        onTogglePin = { file ->
            if (derivedState.pinnedFileUris.contains(file.uri.toString())) {
                onUnpinFile(file)
            } else {
                onPinFile(file)
            }
        },
        onExclude = onExcludeFile,
        onExcludeExtension = onExcludeFileExtension,
        onNicknameClick = { file ->
            nicknameDialogState = NicknameDialogState.File(
                file = file,
                currentNickname = getFileNickname(file.uri.toString()),
                itemName = file.displayName
            )
        },
        getFileNickname = getFileNickname,
        showAllResults = derivedState.autoExpandFiles,
        showExpandControls = derivedState.hasMultipleExpandableSections,
        onExpandClick = {
            if (expandedSection == ExpandedSection.FILES) {
                keyboardController?.show()
                expandedSection = ExpandedSection.NONE
            } else {
                keyboardController?.hide()
                expandedSection = ExpandedSection.FILES
            }
        },
        permissionDisabledCard = { title, message, actionLabel, onActionClick ->
            PermissionDisabledCard(
                title = title,
                message = message,
                actionLabel = actionLabel,
                onActionClick = onActionClick
            )
        },
        showWallpaperBackground = state.showWallpaperBackground
    )
    
    val settingsParams = SettingsSectionParams(
        settings = state.settingResults,
        isExpanded = expandedSection == ExpandedSection.SETTINGS,
        pinnedSettingIds = derivedState.pinnedSettingIds,
        onSettingClick = onSettingClick,
        onTogglePin = { setting ->
            if (derivedState.pinnedSettingIds.contains(setting.id)) {
                onUnpinSetting(setting)
            } else {
                onPinSetting(setting)
            }
        },
        onExclude = onExcludeSetting,
        onNicknameClick = { setting ->
            nicknameDialogState = NicknameDialogState.Setting(
                setting = setting,
                currentNickname = getSettingNickname(setting.id),
                itemName = setting.title
            )
        },
        getSettingNickname = getSettingNickname,
        showAllResults = derivedState.autoExpandSettings,
        showExpandControls = derivedState.hasMultipleExpandableSections,
        onExpandClick = {
            if (expandedSection == ExpandedSection.SETTINGS) {
                keyboardController?.show()
                expandedSection = ExpandedSection.NONE
            } else {
                keyboardController?.hide()
                expandedSection = ExpandedSection.SETTINGS
            }
        },
        showWallpaperBackground = state.showWallpaperBackground
    )

    val contactsParams = ContactsSectionParams(
        contacts = state.contactResults,
        hasPermission = state.hasContactPermission,
        isExpanded = expandedSection == ExpandedSection.CONTACTS,
        messagingApp = state.messagingApp,
        pinnedContactIds = derivedState.pinnedContactIds,
        onContactClick = onContactClick,
        onShowContactMethods = onShowContactMethods,
        onCallContact = onCallContact,
        onSmsContact = onSmsContact,
        onContactMethodClick = onContactMethodClick,
        onTogglePin = { contact ->
            if (derivedState.pinnedContactIds.contains(contact.contactId)) {
                onUnpinContact(contact)
            } else {
                onPinContact(contact)
            }
        },
        onExclude = onExcludeContact,
        onNicknameClick = { contact ->
            nicknameDialogState = NicknameDialogState.Contact(
                contact = contact,
                currentNickname = getContactNickname(contact.contactId),
                itemName = contact.displayName
            )
        },
        getContactNickname = getContactNickname,
        onOpenAppSettings = onOpenAppSettings,
        showAllResults = derivedState.autoExpandContacts,
        showExpandControls = derivedState.hasMultipleExpandableSections,
        onExpandClick = {
            if (expandedSection == ExpandedSection.CONTACTS) {
                keyboardController?.show()
                expandedSection = ExpandedSection.NONE
            } else {
                keyboardController?.hide()
                expandedSection = ExpandedSection.CONTACTS
            }
        },
        permissionDisabledCard = { title, message, actionLabel, onActionClick ->
            PermissionDisabledCard(
                title = title,
                message = message,
                actionLabel = actionLabel,
                onActionClick = onActionClick
            )
        },
        showWallpaperBackground = state.showWallpaperBackground
    )

    val appsParams = AppsSectionParams(
        apps = derivedState.displayApps,
        isSearching = derivedState.isSearching,
        hasAppResults = derivedState.hasAppResults,
        pinnedPackageNames = derivedState.pinnedPackageNames,
        onAppClick = onAppClick,
        onAppInfoClick = onAppInfoClick,
        onUninstallClick = onUninstallClick,
        onHideApp = onHideApp,
        onPinApp = onPinApp,
        onUnpinApp = onUnpinApp,
        onNicknameClick = { app ->
            nicknameDialogState = NicknameDialogState.App(
                app = app,
                currentNickname = getAppNickname(app.packageName),
                itemName = app.appName
            )
        },
        getAppNickname = getAppNickname,
        showAppLabels = true,
        rowCount = derivedState.visibleRowCount,
        iconPackPackage = state.selectedIconPackPackage
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

    // Check if we're in dark mode by checking the background color luminance
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDarkMode = remember(backgroundColor) {
        // Calculate relative luminance (0 = black, 1 = white)
        // Using the standard formula: 0.299*R + 0.587*G + 0.114*B
        val luminance = backgroundColor.red * 0.299f + backgroundColor.green * 0.587f + backgroundColor.blue * 0.114f
        luminance < 0.5f
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Blurred wallpaper background (only if enabled)
        if (state.showWallpaperBackground) {
            wallpaperBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 20.dp),
                    contentScale = ContentScale.FillBounds
                )
                
                // Dark overlay in dark mode
                if (isDarkMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    )
                } else {
                    // Light overlay in light mode
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }
        
        // Fallback background if wallpaper is disabled or not available
        if (!state.showWallpaperBackground || wallpaperBitmap == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
        
        // Content on top
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .navigationBarsPadding()
                .padding(
                    start = 20.dp,
                    top = 16.dp,
                    end = 20.dp
                ),
            verticalArrangement = Arrangement.Top
        ) {
        // Calculate enabled engines
        val enabledEngines: List<SearchEngine> = remember(
            state.searchEngineOrder,
            state.disabledSearchEngines
        ) {
            state.searchEngineOrder.filter { it !in state.disabledSearchEngines }
        }

        // Fixed search bar at the top
        PersistentSearchField(
            query = state.query,
            onQueryChange = onQueryChanged,
            onClearQuery = onClearQuery,
            onSettingsClick = onSettingsClick,
            enabledEngines = enabledEngines,
            shouldUseNumberKeyboard = manuallySwitchedToNumberKeyboard,
            onSearchAction = {
                val trimmedQuery = state.query.trim()
                if (trimmedQuery.isBlank()) return@PersistentSearchField

                // If query has trailing/leading spaces, trim it first
                if (state.query != trimmedQuery) {
                    onQueryChanged(trimmedQuery)
                }

                val firstApp = derivedState.displayApps.firstOrNull()
                if (firstApp != null) {
                    onAppClick(firstApp)
                } else {
                    val primaryEngine = enabledEngines.firstOrNull()
                    if (primaryEngine != null) {
                        // Keep keyboard visible when generating direct answer
                        if (primaryEngine == SearchEngine.DIRECT_SEARCH) {
                            keyboardController?.show()
                        }
                        onSearchEngineClick(trimmedQuery, primaryEngine)
                    }
                }
            }
        )

        // Add spacing between search bar and apps list when bottom aligned setting is off
        if (!state.keyboardAlignedLayout) {
            Spacer(modifier = Modifier.padding(top = 8.dp))
        }

        // Scrollable content between search bar and search engines
        SearchContentArea(
            modifier = Modifier.weight(1f),
            state = state,
            renderingState = renderingState,
            contactsParams = contactsParams,
            filesParams = filesParams,
            settingsParams = settingsParams,
            appsParams = appsParams,
            onRequestUsagePermission = onRequestUsagePermission,
            scrollState = scrollState,
            onRetryDirectSearch = onRetryDirectSearch,
            onPhoneNumberClick = { phoneNumber ->
                // Create a temporary ContactInfo to use the call functionality
                val tempContact = ContactInfo(
                    contactId = -1L,
                    lookupKey = "",
                    displayName = "Direct Answer",
                    phoneNumbers = listOf(phoneNumber)
                )
                onCallContact(tempContact)
            },
            onEmailClick = onDirectSearchEmailClick,
            onWebSuggestionClick = onWebSuggestionClick
        )

        // Keyboard switch pill - appears above search engines
        if (expandedSection == ExpandedSection.NONE) {
            val pillText = if (manuallySwitchedToNumberKeyboard) {
                stringResource(R.string.keyboard_switch_back)
            } else if (hasMathExpression && state.calculatorEnabled) {
                stringResource(R.string.keyboard_switch_to_number)
            } else {
                null
            }

            pillText?.let {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    KeyboardSwitchPill(
                        text = it,
                        onClick = {
                            if (manuallySwitchedToNumberKeyboard) {
                                manuallySwitchedToNumberKeyboard = false
                            } else {
                                manuallySwitchedToNumberKeyboard = true
                            }
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // Fixed search engines section at the bottom (above keyboard, not scrollable)
        // Hide when files or contacts are expanded, or when search engine section is disabled
        if (expandedSection == ExpandedSection.NONE && state.searchEngineSectionEnabled) {
            SearchEngineIconsSection(
                query = state.query,
                hasAppResults = derivedState.hasAppResults,
                enabledEngines = enabledEngines,
                onSearchEngineClick = { query, engine ->
                    // Keep keyboard visible when generating direct answer
                    if (engine == SearchEngine.DIRECT_SEARCH) {
                        keyboardController?.show()
                    }
                    onSearchEngineClick(query, engine)
                },
                onSearchEngineLongPress = onSearchEngineLongPress,
                modifier = Modifier.imePadding()
            )
        } else if (expandedSection == ExpandedSection.NONE && !state.searchEngineSectionEnabled) {
            // Add padding when search engine section is disabled to prevent keyboard from covering content
            Spacer(modifier = Modifier.imePadding())
        }
        }
    }
    
    // Phone number selection dialog
    state.phoneNumberSelection?.let { selection ->
        PhoneNumberSelectionDialog(
            contactInfo = selection.contactInfo,
            isCall = selection.isCall,
            onPhoneNumberSelected = onPhoneNumberSelected,
            onDismiss = onDismissPhoneNumberSelection
        )
    }
    
    state.directDialChoice?.let { choice ->
        DirectDialChoiceDialog(
            contactName = choice.contactName,
            phoneNumber = choice.phoneNumber,
            onSelectOption = onDirectDialChoiceSelected,
            onDismiss = onDismissDirectDialChoice
        )
    }

    // Contact methods dialog
    state.contactMethodsBottomSheet?.let { contactInfo ->
        val context = LocalContext.current
        val userPreferences = remember { com.tk.quicksearch.data.UserAppPreferences(context) }
        ContactMethodsDialog(
            contactInfo = contactInfo,
            onContactMethodClick = onContactMethodClick,
            onDismiss = onDismissContactMethods,
            getLastShownPhoneNumber = { contactId -> userPreferences.getLastShownPhoneNumber(contactId) },
            setLastShownPhoneNumber = { contactId, phoneNumber -> userPreferences.setLastShownPhoneNumber(contactId, phoneNumber) }
        )
    }

    if (state.showReleaseNotesDialog) {
        ReleaseNotesDialog(
            versionName = state.releaseNotesVersionName,
            onAcknowledge = onReleaseNotesAcknowledged
        )
    }
    
    // Nickname dialog
    nicknameDialogState?.let { dialogState ->
        when (dialogState) {
            is NicknameDialogState.App -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveAppNickname(dialogState.app, nickname)
                        nicknameDialogState = null
                    },
                    onDismiss = { nicknameDialogState = null }
                )
            }
            is NicknameDialogState.Contact -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveContactNickname(dialogState.contact, nickname)
                        nicknameDialogState = null
                    },
                    onDismiss = { nicknameDialogState = null }
                )
            }
            is NicknameDialogState.File -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveFileNickname(dialogState.file, nickname)
                        nicknameDialogState = null
                    },
                    onDismiss = { nicknameDialogState = null }
                )
            }
            is NicknameDialogState.Setting -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveSettingNickname(dialogState.setting, nickname)
                        nicknameDialogState = null
                    },
                    onDismiss = { nicknameDialogState = null }
                )
            }
        }
    }
}

@Composable
private fun ReleaseNotesDialog(
    versionName: String?,
    onAcknowledge: () -> Unit
) {
    val title = if (versionName != null) {
        stringResource(R.string.release_notes_title, versionName)
    } else {
        stringResource(R.string.release_notes_title_no_version)
    }
    val bulletPoints = stringResource(R.string.release_notes_points)
        .split("\n")
        .filter { it.isNotBlank() }

    AlertDialog(
        onDismissRequest = onAcknowledge,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bulletPoints.forEach { point ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(text = stringResource(R.string.release_notes_action_got_it))
            }
        }
    )
}

sealed class NicknameDialogState {
    data class App(
        val app: AppInfo,
        val currentNickname: String?,
        val itemName: String
    ) : NicknameDialogState()
    
    data class Contact(
        val contact: ContactInfo,
        val currentNickname: String?,
        val itemName: String
    ) : NicknameDialogState()
    
    data class File(
        val file: DeviceFile,
        val currentNickname: String?,
        val itemName: String
    ) : NicknameDialogState()

    data class Setting(
        val setting: SettingShortcut,
        val currentNickname: String?,
        val itemName: String
    ) : NicknameDialogState()
}

@Composable
private fun PermissionDisabledCard(
    title: String,
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onActionClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PersistentSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    enabledEngines: List<SearchEngine>,
    onSearchAction: () -> Unit,
    shouldUseNumberKeyboard: Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Set search bar background to black with slight transparency
    val searchBarBackground = Color.Black.copy(alpha = 0.3f)
    val focusedContainerColor = Color.Black.copy(alpha = 0.3f)
    val unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
    // Light color for icons and text on dark grey background
    val iconAndTextColor = Color(0xFFE0E0E0)
    
    // Track if text is multi-line to adjust text size
    var isMultiLine by remember { mutableStateOf(false) }

    // Local text field value maintains cursor position even when state query changes from voice input.
    var textFieldValue by remember { mutableStateOf(TextFieldValue(query)) }

    LaunchedEffect(query) {
        if (query != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = query,
                selection = TextRange(query.length)
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Calculate available width for text (accounting for icons and padding)
        // Leading icon: ~48dp, trailing icons: ~48-96dp, horizontal padding: ~32dp
        val availableTextWidth = maxWidth - 176.dp
        
        // Hidden Text composable to measure text layout and detect line count
        // Positioned absolutely and made invisible so it doesn't affect layout
        Text(
            text = query.ifEmpty { " " },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 3,
            onTextLayout = { layoutResult ->
                isMultiLine = layoutResult.lineCount > 1
            },
            modifier = Modifier
                .width(availableTextWidth)
                .alpha(0f)
                .layout { measurable, constraints ->
                    // Measure but don't take up any space in layout
                    val placeable = measurable.measure(constraints)
                    layout(0, 0) {
                        // Don't place it - it's just for measurement
                    }
                }
        )

        // Determine text style based on whether text is multi-line
        val textStyle = if (isMultiLine) {
            MaterialTheme.typography.titleMedium
        } else {
            MaterialTheme.typography.titleLarge
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(28.dp)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(searchBarBackground)
        ) {
            TextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onQueryChange(newValue.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(28.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.titleMedium,
                        color = iconAndTextColor.copy(alpha = 0.6f)
                    )
                },
                textStyle = textStyle,
                singleLine = false,
                maxLines = 3,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.desc_search_icon),
                        tint = iconAndTextColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = onClearQuery) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.desc_clear_search),
                                    tint = iconAndTextColor
                                )
                            }
                        }
                        if (query.isEmpty()) {
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.desc_open_settings),
                                    tint = iconAndTextColor
                                )
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType = if (shouldUseNumberKeyboard) KeyboardType.Number else KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (query.isNotBlank()) {
                            onSearchAction()
                            // Only hide keyboard if the first engine is not DIRECT_ANSWER
                            val firstEngine = enabledEngines.firstOrNull()
                            if (firstEngine != SearchEngine.DIRECT_SEARCH) {
                                keyboardController?.hide()
                            }
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedTextColor = iconAndTextColor,
                    unfocusedTextColor = iconAndTextColor
                )
            )
        }
    }
}

@Composable
internal fun UsagePermissionCard(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.usage_permission_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.desc_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.usage_permission_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(R.string.action_open_settings))
            }
        }
    }
}

@Composable
internal fun InfoBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}


@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.empty_state_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KeyboardSwitchPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.4f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


