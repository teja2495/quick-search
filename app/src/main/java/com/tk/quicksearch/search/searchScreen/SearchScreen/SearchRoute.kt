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
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.shared.ui.theme.DesignTokens
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
    onOverlayDismissRequest: (() -> Unit)? = null,
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
        SearchScreenComposable(
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
            onAppClick = { app: com.tk.quicksearch.search.models.AppInfo -> viewModel.launchApp(app) },
            onAppInfoClick = { app: com.tk.quicksearch.search.models.AppInfo -> viewModel.openAppInfo(app) },
            onUninstallClick = { app: com.tk.quicksearch.search.models.AppInfo -> viewModel.requestUninstall(app) },
            onHideApp = onHideAppWithUndo,
            onPinApp = viewModel::pinApp,
            onUnpinApp = viewModel::unpinApp,
            onContactClick = viewModel::openContact,
            onShowContactMethods = showContactMethodsBottomSheet,
            onDismissContactMethods = dismissContactMethodsBottomSheet,
            onCallContact = callContactWithPermission,
            onSmsContact = viewModel::smsContact,
            onContactMethodClick = viewModel::handleContactMethod,
            onFileClick = { file: com.tk.quicksearch.search.models.DeviceFile -> viewModel.openFile(file) },
            onOpenFolder = { file: com.tk.quicksearch.search.models.DeviceFile -> viewModel.openContainingFolder(file) },
            onPinContact = viewModel::pinContact,
            onUnpinContact = viewModel::unpinContact,
            onExcludeContact = onExcludeContactWithUndo,
            onPinFile = viewModel::pinFile,
            onUnpinFile = viewModel::unpinFile,
            onExcludeFile = onExcludeFileWithUndo,
            onExcludeFileExtension = onExcludeFileExtensionWithUndo,
            onSettingClick = { setting: com.tk.quicksearch.search.deviceSettings.DeviceSetting -> viewModel.openSetting(setting) },
            onPinSetting = viewModel::pinSetting,
            onUnpinSetting = viewModel::unpinSetting,
            onExcludeSetting = onExcludeSettingWithUndo,
            onAppShortcutClick = { shortcut: com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut -> viewModel.launchAppShortcut(shortcut) },
            onPinAppShortcut = viewModel::pinAppShortcut,
            onUnpinAppShortcut = viewModel::unpinAppShortcut,
            onExcludeAppShortcut = onExcludeAppShortcutWithUndo,
            onIncludeAppShortcut = viewModel::removeExcludedAppShortcut,
            onAppShortcutAppInfoClick = { shortcut: com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut -> viewModel.openAppInfo(shortcut.packageName) },
            onPhoneNumberSelected = viewModel::onPhoneNumberSelected,
            onDismissPhoneNumberSelection = viewModel::dismissPhoneNumberSelection,
            onSearchTargetClick = { query: String, target: com.tk.quicksearch.search.core.SearchTarget -> viewModel.openSearchTarget(query, target) },
            onSearchEngineLongPress = onSearchEngineLongPress,
            onDirectSearchEmailClick = { email: String -> viewModel.openEmail(email) },
            onSetPersonalContext = viewModel::setPersonalContext,
            onSetGeminiModel = viewModel::setGeminiModel,
            onSetGeminiGroundingEnabled = viewModel::setGeminiGroundingEnabled,
            onRefreshAvailableGeminiModels = viewModel::refreshAvailableGeminiModels,
            onOpenAppSettings = { viewModel.openAppSettings() },
            onOpenStorageAccessSettings = { viewModel.openAllFilesAccessSettings() },
            onAppNicknameClick = { app: com.tk.quicksearch.search.models.AppInfo ->
                // This will be handled by the dialog state in SearchScreen
            },
            onClearDetectedShortcut = viewModel::clearDetectedShortcut,
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
            onOpenSearchHistorySettings = onOpenSearchHistorySettings,
            onDismissSearchHistoryTip = viewModel::dismissSearchHistoryTip,
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