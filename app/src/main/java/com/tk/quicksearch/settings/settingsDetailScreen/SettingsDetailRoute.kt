package com.tk.quicksearch.settings.settingsDetailScreen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.settings.shared.handlePermissionResult
import com.tk.quicksearch.tile.requestAddQuickSearchTile
import com.tk.quicksearch.util.WallpaperUtils
import com.tk.quicksearch.util.isDefaultDigitalAssistant
import com.tk.quicksearch.widget.requestAddQuickSearchWidget
import kotlinx.coroutines.launch

@Composable
fun SettingsDetailRoute(
        modifier: Modifier = Modifier,
        onBack: () -> Unit,
        viewModel: SearchViewModel,
        detailType: SettingsDetailType,
        onNavigateToDetail: (SettingsDetailType) -> Unit = {},
        onRequestUsagePermission: () -> Unit = {},
        onRequestContactPermission: () -> Unit = {},
        onRequestFilePermission: () -> Unit = {},
        onRequestCallPermission: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val state =
            SettingsScreenState(
                    suggestionExcludedApps = uiState.suggestionExcludedApps,
                    resultExcludedApps = uiState.resultExcludedApps,
                    excludedContacts = uiState.excludedContacts,
                    excludedFiles = uiState.excludedFiles,
                    excludedSettings = uiState.excludedSettings,
                    excludedAppShortcuts = uiState.excludedAppShortcuts,
                    searchEngineOrder = uiState.searchTargetsOrder,
                    disabledSearchEngines = uiState.disabledSearchTargetIds,
                    enabledFileTypes = uiState.enabledFileTypes,
                    showFolders = uiState.showFolders,
                    showSystemFiles = uiState.showSystemFiles,
                    showHiddenFiles = uiState.showHiddenFiles,
                    excludedFileExtensions = uiState.excludedFileExtensions,
                    oneHandedMode = uiState.oneHandedMode,
                    overlayModeEnabled = uiState.overlayModeEnabled,
                    hasSeenOverlayAssistantTip = uiState.hasSeenOverlayAssistantTip,
                    shortcutCodes = uiState.shortcutCodes,
                    shortcutEnabled = uiState.shortcutEnabled,
                    allAppShortcuts = uiState.allAppShortcuts,
                    allDeviceSettings = uiState.allDeviceSettings,
                    allApps = uiState.allApps,
                    disabledAppShortcutIds = uiState.disabledAppShortcutIds,
                    messagingApp = uiState.messagingApp,
                    isWhatsAppInstalled = uiState.isWhatsAppInstalled,
                    isTelegramInstalled = uiState.isTelegramInstalled,
                    isSignalInstalled = uiState.isSignalInstalled,
                    hasWallpaperPermission = uiState.hasWallpaperPermission,
                    wallpaperAvailable = uiState.wallpaperAvailable,
                    showWallpaperBackground = uiState.showWallpaperBackground,
                    wallpaperBackgroundAlpha = uiState.wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = uiState.wallpaperBlurRadius,
                    selectedIconPackPackage = uiState.selectedIconPackPackage,
                    availableIconPacks = uiState.availableIconPacks,
                    directDialEnabled = uiState.directDialEnabled,
                    disabledSections = uiState.disabledSections,
                    isSearchEngineCompactMode = uiState.isSearchEngineCompactMode,
                    amazonDomain = uiState.amazonDomain,
                    calculatorEnabled = uiState.calculatorEnabled,
                    appSuggestionsEnabled = uiState.appSuggestionsEnabled,
                    webSuggestionsEnabled = uiState.webSuggestionsEnabled,
                    webSuggestionsCount = uiState.webSuggestionsCount,
                    recentQueriesEnabled = uiState.recentQueriesEnabled,
                    hasGeminiApiKey = uiState.hasGeminiApiKey,
                    geminiApiKeyLast4 = uiState.geminiApiKeyLast4,
                    personalContext = uiState.personalContext,
            )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingWallpaperEnableAfterFilesPermission by remember { mutableStateOf(false) }
    var pendingWallpaperEnableShowDialog by remember { mutableStateOf(false) }
    var pendingWallpaperEnableAfterWallpaperPermission by remember { mutableStateOf(false) }
    var showWallpaperPermissionFallbackDialog by remember { mutableStateOf(false) }

    val wallpaperPermissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                handlePermissionResult(
                        isGranted = isGranted,
                        context = context,
                        permission = Manifest.permission.READ_MEDIA_IMAGES,
                        onPermanentlyDenied = viewModel::openAppSettings,
                        onPermissionChanged = viewModel::handleOptionalPermissionChange,
                        onGranted = { pendingWallpaperEnableAfterWallpaperPermission = true },
                )
            }

    val requestWallpaperPermission = {
        wallpaperPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
    }

    val attemptEnableWallpaperBackground: (Boolean) -> Unit = { showDialogOnPermissionRequired ->
        scope.launch {
            when (WallpaperUtils.getWallpaperBitmapResult(context)) {
                is WallpaperUtils.WallpaperLoadResult.Success -> {
                    viewModel.setWallpaperAvailable(true)
                    viewModel.setShowWallpaperBackground(true)
                }
                WallpaperUtils.WallpaperLoadResult.PermissionRequired -> {
                    if (showDialogOnPermissionRequired) {
                        showWallpaperPermissionFallbackDialog = true
                    } else {
                        requestWallpaperPermission()
                    }
                }
                else -> {}
            }
        }
    }

    val onToggleWallpaperBackground: (Boolean) -> Unit = { enabled ->
        if (!enabled) {
            viewModel.setShowWallpaperBackground(false)
        } else if (!PermissionUtils.hasFileAccessPermission(context)) {
            pendingWallpaperEnableAfterFilesPermission = true
            pendingWallpaperEnableShowDialog = true
            viewModel.openFilesPermissionSettings()
        } else {
            attemptEnableWallpaperBackground(false)
        }
    }

    LaunchedEffect(pendingWallpaperEnableAfterWallpaperPermission) {
        if (pendingWallpaperEnableAfterWallpaperPermission) {
            pendingWallpaperEnableAfterWallpaperPermission = false
            attemptEnableWallpaperBackground(false)
        }
    }

    LaunchedEffect(uiState.hasFilePermission, pendingWallpaperEnableAfterFilesPermission) {
        if (pendingWallpaperEnableAfterFilesPermission &&
                        PermissionUtils.hasFileAccessPermission(context)
        ) {
            pendingWallpaperEnableAfterFilesPermission = false
            val showDialog = pendingWallpaperEnableShowDialog
            pendingWallpaperEnableShowDialog = false
            attemptEnableWallpaperBackground(showDialog)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val userPreferences = remember { UserAppPreferences(context) }
    var shouldShowShortcutHint by
            remember(detailType) {
                mutableStateOf(
                        detailType == SettingsDetailType.SEARCH_ENGINES &&
                                userPreferences.shouldShowShortcutHintBanner(),
                )
            }
    var shouldShowDefaultEngineHint by
            remember(detailType) {
                mutableStateOf(
                        detailType == SettingsDetailType.SEARCH_ENGINES &&
                                userPreferences.shouldShowDefaultEngineHintBanner(),
                )
            }
    var isDefaultAssistant by remember { mutableStateOf(context.isDefaultDigitalAssistant()) }
    var directSearchSetupExpanded by
            remember(detailType) {
                mutableStateOf(
                        if (detailType == SettingsDetailType.SEARCH_ENGINES) {
                            // Always start expanded in search engine settings screen
                            true
                        } else {
                            true
                        },
                )
            }
    var disabledSearchEnginesExpanded by
            remember(detailType) {
                mutableStateOf(
                        if (detailType == SettingsDetailType.SEARCH_ENGINES) {
                            userPreferences.isDisabledSearchEnginesExpanded()
                        } else {
                            true
                        },
                )
            }

    DisposableEffect(lifecycleOwner, detailType) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.handleOptionalPermissionChange()
            }
            if (detailType != SettingsDetailType.SEARCH_ENGINES) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> {
                    userPreferences.resetShortcutHintBannerSessionDismissed()
                    shouldShowShortcutHint = userPreferences.shouldShowShortcutHintBanner()
                    shouldShowDefaultEngineHint = userPreferences.shouldShowDefaultEngineHintBanner()
                    isDefaultAssistant = context.isDefaultDigitalAssistant()
                }
                Lifecycle.Event.ON_RESUME -> {
                    shouldShowShortcutHint = userPreferences.shouldShowShortcutHintBanner()
                    shouldShowDefaultEngineHint = userPreferences.shouldShowDefaultEngineHintBanner()
                    isDefaultAssistant = context.isDefaultDigitalAssistant()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(detailType) {
        if (detailType == SettingsDetailType.APPEARANCE && uiState.showWallpaperBackground) {
            when (WallpaperUtils.getWallpaperBitmapResult(context)) {
                is WallpaperUtils.WallpaperLoadResult.Success -> {
                    viewModel.setWallpaperAvailable(true)
                }
                else -> {}
            }
        }
    }

    val onDismissShortcutHint = {
        userPreferences.incrementShortcutHintBannerDismissCount()
        userPreferences.setShortcutHintBannerSessionDismissed(true)
        shouldShowShortcutHint = userPreferences.shouldShowShortcutHintBanner()
        shouldShowDefaultEngineHint = userPreferences.shouldShowDefaultEngineHintBanner()
    }

    val onDismissDefaultEngineHint = {
        userPreferences.setDefaultEngineHintBannerDismissed(true)
        shouldShowDefaultEngineHint = userPreferences.shouldShowDefaultEngineHintBanner()
    }

    val onToggleSection = rememberSectionToggleHandler(viewModel, uiState.disabledSections)

    val onRequestAddHomeScreenWidget = { requestAddQuickSearchWidget(context) }
    val onRequestAddQuickSettingsTile = { requestAddQuickSearchTile(context) }
    var showShortcutSourcePicker by remember { mutableStateOf(false) }
    var appShortcutFocusPackageName by remember { mutableStateOf<String?>(null) }
    val addAppShortcutLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    viewModel.addCustomAppShortcutFromPickerResult(
                            resultData = result.data,
                            showDefaultToast = false,
                            onShortcutAdded = { addedShortcut ->
                                appShortcutFocusPackageName = addedShortcut.packageName
                                Toast.makeText(
                                                context,
                                                context.getString(
                                                        R.string.settings_app_shortcuts_add_success_with_app_name,
                                                        addedShortcut.appLabel,
                                                ),
                                                Toast.LENGTH_SHORT,
                                        )
                                        .show()
                            },
                            onAddFailed = {
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_app_shortcuts_add_failed),
                                                Toast.LENGTH_SHORT,
                                        )
                                        .show()
                            },
                    )
                }
            }
    val onOpenAddAppShortcutDialog: () -> Unit = {
        showShortcutSourcePicker = true
    }

    val onBackAction: () -> Unit =
            if (detailType.isLevel2()) {
                { onNavigateToDetail(SettingsDetailType.SEARCH_RESULTS) }
            } else {
                onBack
            }

    val callbacks =
            SettingsScreenCallbacks(
                    onBack = onBackAction,
                    onRemoveSuggestionExcludedApp = viewModel::unhideAppFromSuggestions,
                    onRemoveResultExcludedApp = viewModel::unhideAppFromResults,
                    onRemoveExcludedContact = viewModel::removeExcludedContact,
                    onRemoveExcludedFile = viewModel::removeExcludedFile,
                    onRemoveExcludedSetting = viewModel::removeExcludedSetting,
                    onRemoveExcludedAppShortcut = viewModel::removeExcludedAppShortcut,
                    onClearAllExclusions = viewModel::clearAllExclusions,
                    onToggleSearchEngine = viewModel::setSearchTargetEnabled,
                    onReorderSearchEngines = viewModel::reorderSearchTargets,
                    onAddCustomSearchEngine = viewModel::addCustomSearchEngine,
                    onUpdateCustomSearchEngine = viewModel::updateCustomSearchEngine,
                    onDeleteCustomSearchEngine = viewModel::deleteCustomSearchEngine,
                    onToggleFileType = viewModel::setFileTypeEnabled,
                    onToggleFolders = viewModel::setShowFolders,
                    onToggleSystemFiles = viewModel::setShowSystemFiles,
                    onToggleHiddenFiles = viewModel::setShowHiddenFiles,
                    onRemoveExcludedFileExtension = viewModel::removeExcludedFileExtension,
                    onToggleOneHandedMode = viewModel::setOneHandedMode,
                    onToggleOverlayMode = viewModel::setOverlayModeEnabled,
                    onDismissOverlayAssistantTip = viewModel::dismissOverlayAssistantTip,
                    setShortcutCode = viewModel::setShortcutCode,
                    setShortcutEnabled = viewModel::setShortcutEnabled,
                    onSetMessagingApp = viewModel::setMessagingApp,
                    onToggleShowWallpaperBackground = onToggleWallpaperBackground,
                    onWallpaperBackgroundAlphaChange = viewModel::setWallpaperBackgroundAlpha,
                    onWallpaperBlurRadiusChange = viewModel::setWallpaperBlurRadius,
                    onSelectIconPack = viewModel::setIconPackPackage,
                    onSearchIconPacks = viewModel::searchIconPacks,
                    onRefreshIconPacks = viewModel::refreshIconPacks,
                    onToggleDirectDial = viewModel::setDirectDialEnabled,
                    onToggleSection = onToggleSection,
                    onToggleSearchEngineCompactMode = viewModel::setSearchEngineCompactMode,
                    onSetAmazonDomain = viewModel::setAmazonDomain,
                    onToggleCalculator = viewModel::setCalculatorEnabled,
                    onToggleAppSuggestions = viewModel::setAppSuggestionsEnabled,
                    onToggleWebSuggestions = viewModel::setWebSuggestionsEnabled,
                    onWebSuggestionsCountChange = viewModel::setWebSuggestionsCount,
                    onToggleRecentQueries = viewModel::setRecentQueriesEnabled,
                    onSetGeminiApiKey = viewModel::setGeminiApiKey,
                    onSetPersonalContext = viewModel::setPersonalContext,
                    onToggleAppShortcutEnabled = viewModel::setAppShortcutEnabled,
                    onLaunchAppShortcut = viewModel::launchAppShortcut,
                    onOpenAddAppShortcutDialog = onOpenAddAppShortcutDialog,
                    onDeleteCustomAppShortcut = viewModel::deleteCustomAppShortcut,
                    onLaunchDeviceSetting = viewModel::openSetting,
                    onRequestAppUninstall = viewModel::requestUninstall,
                    onOpenAppInfo = viewModel::openAppInfo,
                    onAddHomeScreenWidget = onRequestAddHomeScreenWidget,
                    onAddQuickSettingsTile = onRequestAddQuickSettingsTile,
                    onSetDefaultAssistant = {
                        try {
                            val intent =
                                    Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to general settings if voice input settings not available
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                                context,
                                                context.getString(
                                                        R.string.settings_unable_to_open_settings,
                                                ),
                                                Toast.LENGTH_SHORT,
                                        )
                                        .show()
                            }
                        }
                    },
                    onRefreshApps = viewModel::refreshApps,
                    onRefreshContacts = viewModel::refreshContacts,
                    onRefreshFiles = viewModel::refreshFiles,
                    onRequestUsagePermission = onRequestUsagePermission,
                    onRequestContactPermission = onRequestContactPermission,
                    onRequestFilePermission = onRequestFilePermission,
                    onRequestCallPermission = onRequestCallPermission,
                    onRequestWallpaperPermission = requestWallpaperPermission,
            )

    val onToggleDirectSearchSetupExpanded = {
        val newExpanded = !directSearchSetupExpanded
        directSearchSetupExpanded = newExpanded
        if (detailType == SettingsDetailType.SEARCH_ENGINES) {
            userPreferences.setDirectSearchSetupExpanded(newExpanded)
        }
    }
    val onToggleDisabledSearchEnginesExpanded = {
        val newExpanded = !disabledSearchEnginesExpanded
        disabledSearchEnginesExpanded = newExpanded
        if (detailType == SettingsDetailType.SEARCH_ENGINES) {
            userPreferences.setDisabledSearchEnginesExpanded(newExpanded)
        }
    }

    if (detailType.isLevel2()) {
        SettingsDetailLevel2Screen(
                modifier = modifier,
                state = state,
                callbacks = callbacks,
                detailType = detailType,
                appShortcutFocusPackageName = appShortcutFocusPackageName,
                onAppShortcutFocusHandled = { appShortcutFocusPackageName = null },
        )
    } else {
        SettingsDetailLevel1Screen(
                modifier = modifier,
                state = state,
                callbacks = callbacks,
                detailType = detailType,
                showShortcutHintBanner = shouldShowShortcutHint,
                onDismissShortcutHintBanner = onDismissShortcutHint,
                showDefaultEngineHintBanner = shouldShowDefaultEngineHint,
                onDismissDefaultEngineHintBanner = onDismissDefaultEngineHint,
                isDefaultAssistant = isDefaultAssistant,
                directSearchSetupExpanded = directSearchSetupExpanded,
                onToggleDirectSearchSetupExpanded = onToggleDirectSearchSetupExpanded,
                disabledSearchEnginesExpanded = disabledSearchEnginesExpanded,
                onToggleDisabledSearchEnginesExpanded = onToggleDisabledSearchEnginesExpanded,
                onNavigateToDetail = onNavigateToDetail,
        )
    }

    if (showWallpaperPermissionFallbackDialog) {
        AlertDialog(
                onDismissRequest = { showWallpaperPermissionFallbackDialog = false },
                title = { Text(stringResource(R.string.wallpaper_permission_fallback_title)) },
                text = { Text(stringResource(R.string.wallpaper_permission_fallback_message)) },
                confirmButton = {
                    TextButton(
                            onClick = {
                                showWallpaperPermissionFallbackDialog = false
                                requestWallpaperPermission()
                            },
                    ) { Text(stringResource(R.string.dialog_yes)) }
                },
                dismissButton = {
                    TextButton(onClick = { showWallpaperPermissionFallbackDialog = false }) {
                        Text(stringResource(R.string.dialog_no))
                    }
                },
        )
    }

    if (showShortcutSourcePicker) {
        AppShortcutSourcePickerDialog(
                onDismiss = { showShortcutSourcePicker = false },
                onSourceSelected = { sourceIntent ->
                    showShortcutSourcePicker = false
                    runCatching { addAppShortcutLauncher.launch(sourceIntent) }
                            .onFailure {
                                Toast
                                        .makeText(
                                                context,
                                                context.getString(
                                                        R.string.settings_app_shortcuts_create_not_supported,
                                                ),
                                                Toast.LENGTH_SHORT,
                                        )
                                        .show()
                            }
                },
        )
    }
}
