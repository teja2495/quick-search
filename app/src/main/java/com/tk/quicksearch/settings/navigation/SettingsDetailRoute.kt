package com.tk.quicksearch.settings.navigation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.settings.AppShortcutsSettings.*
import com.tk.quicksearch.settings.settingsDetailScreen.*
import com.tk.quicksearch.settings.shared.handlePermissionResult
import com.tk.quicksearch.tile.requestAddQuickSearchTile
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.isDefaultDigitalAssistant
import com.tk.quicksearch.widgets.utils.requestAddQuickSearchWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    folderWhitelistPatterns = uiState.folderWhitelistPatterns,
                    folderBlacklistPatterns = uiState.folderBlacklistPatterns,
                    excludedFileExtensions = uiState.excludedFileExtensions,
                    oneHandedMode = uiState.oneHandedMode,
                    bottomSearchBarEnabled = uiState.bottomSearchBarEnabled,
                    overlayModeEnabled = uiState.overlayModeEnabled,
                    hasSeenOverlayAssistantTip = uiState.hasSeenOverlayAssistantTip,
                    shortcutCodes = uiState.shortcutCodes,
                    shortcutEnabled = uiState.shortcutEnabled,
                    allAppShortcuts = uiState.allAppShortcuts,
                    allDeviceSettings = uiState.allDeviceSettings,
                    allApps = uiState.allApps,
                    disabledAppShortcutIds = uiState.disabledAppShortcutIds,
                    messagingApp = uiState.messagingApp,
                    callingApp = uiState.callingApp,
                    isWhatsAppInstalled = uiState.isWhatsAppInstalled,
                    isTelegramInstalled = uiState.isTelegramInstalled,
                    isSignalInstalled = uiState.isSignalInstalled,
                    isGoogleMeetInstalled = uiState.isGoogleMeetInstalled,
                    hasWallpaperPermission = uiState.hasWallpaperPermission,
                    wallpaperAvailable = uiState.wallpaperAvailable,
                    wallpaperBackgroundAlpha = uiState.wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = uiState.wallpaperBlurRadius,
                    overlayGradientTheme = uiState.overlayGradientTheme,
                    overlayThemeIntensity = uiState.overlayThemeIntensity,
                    fontScaleMultiplier = uiState.fontScaleMultiplier,
                    backgroundSource = uiState.backgroundSource,
                    customImageUri = uiState.customImageUri,
                    selectedIconPackPackage = uiState.selectedIconPackPackage,
                    availableIconPacks = uiState.availableIconPacks,
                    showAppLabels = uiState.showAppLabels,
                    directDialEnabled = uiState.directDialEnabled,
                    disabledSections = uiState.disabledSections,
                    isSearchEngineCompactMode = uiState.isSearchEngineCompactMode,
                    searchEngineCompactRowCount = uiState.searchEngineCompactRowCount,
                    amazonDomain = uiState.amazonDomain,
                    calculatorEnabled = uiState.calculatorEnabled,
                    appSuggestionsEnabled = uiState.appSuggestionsEnabled,
                    webSuggestionsEnabled = uiState.webSuggestionsEnabled,
                    webSuggestionsCount = uiState.webSuggestionsCount,
                    recentQueriesEnabled = uiState.recentQueriesEnabled,
                    hasGeminiApiKey = uiState.hasGeminiApiKey,
                    geminiApiKeyLast4 = uiState.geminiApiKeyLast4,
                    personalContext = uiState.personalContext,
                    geminiModel = uiState.geminiModel,
                    geminiGroundingEnabled = uiState.geminiGroundingEnabled,
                    availableGeminiModels = uiState.availableGeminiModels,
            )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showWallpaperFallbackDialog by remember { mutableStateOf(false) }
    var requiresImagePermissionAfterWallpaperSecurityError by remember { mutableStateOf(false) }
    var wallpaperButtonHasPermission by
            remember {
                mutableStateOf(PermissionRequestHandler.checkFilesPermission(context))
            }

    suspend fun tryFetchWallpaperWithFilesPermission() {
        when (WallpaperUtils.getWallpaperBitmapResult(context)) {
            is WallpaperUtils.WallpaperLoadResult.Success -> {
                requiresImagePermissionAfterWallpaperSecurityError = false
                wallpaperButtonHasPermission = true
                viewModel.setWallpaperAvailable(true)
                viewModel.setBackgroundSource(BackgroundSource.SYSTEM_WALLPAPER)
            }
            WallpaperUtils.WallpaperLoadResult.SecurityError -> {
                requiresImagePermissionAfterWallpaperSecurityError = true
                wallpaperButtonHasPermission = false
                viewModel.setWallpaperAvailable(false)
                showWallpaperFallbackDialog = true
            }
            WallpaperUtils.WallpaperLoadResult.PermissionRequired -> {
                wallpaperButtonHasPermission = false
                viewModel.setWallpaperAvailable(false)
            }
            WallpaperUtils.WallpaperLoadResult.Unavailable -> {
                viewModel.setWallpaperAvailable(false)
            }
        }
    }

    val wallpaperFilesAccessLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
            ) {
                val filesGranted = PermissionRequestHandler.checkFilesPermission(context)
                wallpaperButtonHasPermission =
                        filesGranted && !requiresImagePermissionAfterWallpaperSecurityError
                if (filesGranted) {
                    scope.launch { tryFetchWallpaperWithFilesPermission() }
                } else {
                    viewModel.setWallpaperAvailable(false)
                }
                viewModel.handleOptionalPermissionChange()
            }

    val wallpaperPermissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                if (isGranted) {
                    scope.launch { tryFetchWallpaperWithFilesPermission() }
                } else {
                    wallpaperButtonHasPermission = false
                    viewModel.setWallpaperAvailable(false)
                }
                viewModel.handleOptionalPermissionChange()
            }

    val legacyFilesPermissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                if (isGranted) {
                    wallpaperButtonHasPermission =
                            !requiresImagePermissionAfterWallpaperSecurityError
                    scope.launch { tryFetchWallpaperWithFilesPermission() }
                } else {
                    wallpaperButtonHasPermission = false
                    viewModel.setWallpaperAvailable(false)
                }
                viewModel.handleOptionalPermissionChange()
            }

    val overlayCustomImagePickerLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                runCatching {
                            context.contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        }
                        .onFailure {
                            // Some providers do not support persistable permissions.
                        }
                viewModel.setCustomImageUri(uri.toString())
                viewModel.setBackgroundSource(BackgroundSource.CUSTOM_IMAGE)
            }

    val onSelectWallpaperSource: () -> Unit = {
        if (requiresImagePermissionAfterWallpaperSecurityError &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            wallpaperPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else if (!PermissionRequestHandler.checkFilesPermission(context)) {
            wallpaperButtonHasPermission = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PermissionRequestHandler.launchAllFilesAccessRequest(
                        wallpaperFilesAccessLauncher,
                        context,
                )
            } else {
                legacyFilesPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            scope.launch { tryFetchWallpaperWithFilesPermission() }
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
    var assistantLaunchVoiceModeEnabled by
            remember {
                mutableStateOf(userPreferences.isAssistantLaunchVoiceModeEnabled())
            }
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
                wallpaperButtonHasPermission =
                        PermissionRequestHandler.checkFilesPermission(context) &&
                                !requiresImagePermissionAfterWallpaperSecurityError
            }
            if (
                    detailType != SettingsDetailType.SEARCH_ENGINES &&
                            detailType != SettingsDetailType.LAUNCH_OPTIONS
            ) {
                return@LifecycleEventObserver
            }
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (detailType == SettingsDetailType.SEARCH_ENGINES) {
                        userPreferences.resetShortcutHintBannerSessionDismissed()
                        shouldShowShortcutHint = userPreferences.shouldShowShortcutHintBanner()
                        shouldShowDefaultEngineHint =
                                userPreferences.shouldShowDefaultEngineHintBanner()
                    }
                    isDefaultAssistant = context.isDefaultDigitalAssistant()
                    assistantLaunchVoiceModeEnabled =
                            userPreferences.isAssistantLaunchVoiceModeEnabled()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (detailType == SettingsDetailType.SEARCH_ENGINES) {
                        shouldShowShortcutHint = userPreferences.shouldShowShortcutHintBanner()
                        shouldShowDefaultEngineHint =
                                userPreferences.shouldShowDefaultEngineHintBanner()
                    }
                    isDefaultAssistant = context.isDefaultDigitalAssistant()
                    assistantLaunchVoiceModeEnabled =
                            userPreferences.isAssistantLaunchVoiceModeEnabled()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
    var filteredAppShortcutSources by remember { mutableStateOf<List<AppShortcutSource>>(emptyList()) }
    var appShortcutFocusShortcut by remember { mutableStateOf<StaticShortcut?>(null) }
    var appShortcutFocusPackageName by remember { mutableStateOf<String?>(null) }
    var pendingShortcutSourcePackage by remember { mutableStateOf<String?>(null) }
    var appActivityDialogSource by remember { mutableStateOf<AppShortcutSource?>(null) }
    var appActivityDialogItems by remember { mutableStateOf<List<AppActivitySource>>(emptyList()) }
    val addAppShortcutLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    viewModel.addCustomAppShortcutFromPickerResult(
                            resultData = result.data,
                            sourcePackageName = pendingShortcutSourcePackage,
                            showDefaultToast = false,
                            onShortcutAdded = { addedShortcut ->
                                appShortcutFocusShortcut = addedShortcut
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
                pendingShortcutSourcePackage = null
            }
    val onOpenAddAppShortcutDialog: () -> Unit = {
        showShortcutSourcePicker = true
    }
    val onAddShortcutFromSource: (AppShortcutSource) -> Unit = { source ->
        if (isAppActivitySource(source)) {
            val activities = queryAppActivitiesForPackage(context.packageManager, source.packageName)
            appActivityDialogSource = source
            appActivityDialogItems = activities
        } else {
            pendingShortcutSourcePackage = source.packageName
            runCatching { addAppShortcutLauncher.launch(source.launchIntent) }
                    .onFailure {
                        pendingShortcutSourcePackage = null
                        Toast.makeText(
                                        context,
                                        context.getString(
                                                R.string.settings_app_shortcuts_create_not_supported,
                                        ),
                                        Toast.LENGTH_SHORT,
                                )
                                .show()
                    }
        }
    }

    LaunchedEffect(
            detailType,
            state.allApps,
            state.allAppShortcuts,
            context.packageName,
    ) {
        if (detailType != SettingsDetailType.APP_SHORTCUTS) {
            filteredAppShortcutSources = emptyList()
            return@LaunchedEffect
        }

        val appShortcutSources =
                withContext(Dispatchers.Default) {
                    queryAppShortcutSources(
                            packageManager = context.packageManager,
                            repositoryApps = state.allApps,
                    )
                }
        filteredAppShortcutSources =
                withContext(Dispatchers.Default) {
                    filterAppShortcutSources(
                            sources = appShortcutSources,
                            existingShortcuts = state.allAppShortcuts,
                            currentPackageName = context.packageName,
                    )
                }
    }

    val onBackAction: () -> Unit =
            if (detailType.isLevel2()) {
                {
                    when (detailType) {
                        SettingsDetailType.DIRECT_SEARCH_CONFIGURE -> {
                            onNavigateToDetail(SettingsDetailType.SEARCH_ENGINES)
                        }
                        else -> {
                            onNavigateToDetail(SettingsDetailType.SEARCH_RESULTS)
                        }
                    }
                }
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
                    onSetFolderWhitelistPatterns = viewModel::setFolderWhitelistPatterns,
                    onSetFolderBlacklistPatterns = viewModel::setFolderBlacklistPatterns,
                    onRemoveExcludedFileExtension = viewModel::removeExcludedFileExtension,
                    onToggleOneHandedMode = viewModel::setOneHandedMode,
                    onToggleBottomSearchBar = viewModel::setBottomSearchBarEnabled,
                    onToggleOverlayMode = viewModel::setOverlayModeEnabled,
                    onDismissOverlayAssistantTip = viewModel::dismissOverlayAssistantTip,
                    setShortcutCode = viewModel::setShortcutCode,
                    setShortcutEnabled = viewModel::setShortcutEnabled,
                    onSetMessagingApp = viewModel::setMessagingApp,
                    onSetCallingApp = viewModel::setCallingApp,
                    onWallpaperBackgroundAlphaChange = viewModel::setWallpaperBackgroundAlpha,
                    onWallpaperBlurRadiusChange = viewModel::setWallpaperBlurRadius,
                    onSetOverlayGradientTheme = viewModel::setOverlayGradientTheme,
                    onOverlayThemeIntensityChange = viewModel::setOverlayThemeIntensity,
                    onFontScaleMultiplierChange = viewModel::setFontScaleMultiplier,
                    onSetBackgroundSource = viewModel::setBackgroundSource,
                    onPickCustomImage = {
                        overlayCustomImagePickerLauncher.launch(arrayOf("image/*"))
                    },
                    onSelectIconPack = viewModel::setIconPackPackage,
                    onSearchIconPacks = viewModel::searchIconPacks,
                    onRefreshIconPacks = viewModel::refreshIconPacks,
                    onToggleAppLabels = viewModel::setShowAppLabels,
                    onToggleDirectDial = viewModel::setDirectDialEnabled,
                    onToggleSection = onToggleSection,
                    onToggleSearchEngineCompactMode = viewModel::setSearchEngineCompactMode,
                    onSetSearchEngineCompactRowCount = viewModel::setSearchEngineCompactRowCount,
                    onSetAmazonDomain = viewModel::setAmazonDomain,
                    onToggleCalculator = viewModel::setCalculatorEnabled,
                    onToggleAppSuggestions = viewModel::setAppSuggestionsEnabled,
                    onToggleWebSuggestions = viewModel::setWebSuggestionsEnabled,
                    onWebSuggestionsCountChange = viewModel::setWebSuggestionsCount,
                    onToggleRecentQueries = viewModel::setRecentQueriesEnabled,
                    onSetGeminiApiKey = viewModel::setGeminiApiKey,
                    onSetPersonalContext = viewModel::setPersonalContext,
                    onSetGeminiModel = viewModel::setGeminiModel,
                    onSetGeminiGroundingEnabled = viewModel::setGeminiGroundingEnabled,
                    onRefreshAvailableGeminiModels = viewModel::refreshAvailableGeminiModels,
                    onOpenDirectSearchConfigure = {
                        onNavigateToDetail(SettingsDetailType.DIRECT_SEARCH_CONFIGURE)
                    },
                    onToggleAppShortcutEnabled = viewModel::setAppShortcutEnabled,
                    onLaunchAppShortcut = viewModel::launchAppShortcut,
                    onOpenAddAppShortcutDialog = onOpenAddAppShortcutDialog,
                    onAddAppShortcutFromSource = onAddShortcutFromSource,
                    onAddSearchTargetQueryShortcut = { target, shortcutName, shortcutQuery ->
                        viewModel.addSearchTargetQueryShortcut(
                                target = target,
                                shortcutName = shortcutName,
                                shortcutQuery = shortcutQuery,
                                showDefaultToast = false,
                                onShortcutAdded = { addedShortcut ->
                                    appShortcutFocusShortcut = addedShortcut
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
                    },
                    onUpdateCustomAppShortcut = viewModel::updateCustomAppShortcut,
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
                    onToggleAssistantLaunchVoiceMode = { enabled ->
                        userPreferences.setAssistantLaunchVoiceModeEnabled(enabled)
                        assistantLaunchVoiceModeEnabled = enabled
                    },
                    onRefreshApps = viewModel::refreshApps,
                    onRefreshContacts = viewModel::refreshContacts,
                    onRefreshFiles = viewModel::refreshFiles,
                    onRequestUsagePermission = onRequestUsagePermission,
                    onRequestContactPermission = onRequestContactPermission,
                    onRequestFilePermission = onRequestFilePermission,
                    onRequestCallPermission = onRequestCallPermission,
                    onRequestWallpaperPermission = onSelectWallpaperSource,
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
    val resolvedState =
            if (detailType == SettingsDetailType.APPEARANCE) {
                state.copy(hasWallpaperPermission = wallpaperButtonHasPermission)
            } else {
                state
            }

    if (detailType.isLevel2()) {
        SettingsDetailLevel2Screen(
                modifier = modifier,
                state = resolvedState,
                callbacks = callbacks,
                detailType = detailType,
                hasUsagePermission = uiState.hasUsagePermission,
                appShortcutFocusShortcut = appShortcutFocusShortcut,
                appShortcutFocusPackageName = appShortcutFocusPackageName,
                appShortcutSources = filteredAppShortcutSources,
                searchTargets = state.searchEngineOrder,
                onAppShortcutFocusHandled = {
                    appShortcutFocusShortcut = null
                    appShortcutFocusPackageName = null
                },
        )
    } else {
        SettingsDetailLevel1Screen(
                modifier = modifier,
                state = resolvedState,
                callbacks = callbacks,
                detailType = detailType,
                showShortcutHintBanner = shouldShowShortcutHint,
                onDismissShortcutHintBanner = onDismissShortcutHint,
                showDefaultEngineHintBanner = shouldShowDefaultEngineHint,
                onDismissDefaultEngineHintBanner = onDismissDefaultEngineHint,
                isDefaultAssistant = isDefaultAssistant,
                assistantLaunchVoiceModeEnabled = assistantLaunchVoiceModeEnabled,
                directSearchSetupExpanded = directSearchSetupExpanded,
                onToggleDirectSearchSetupExpanded = onToggleDirectSearchSetupExpanded,
                disabledSearchEnginesExpanded = disabledSearchEnginesExpanded,
                onToggleDisabledSearchEnginesExpanded = onToggleDisabledSearchEnginesExpanded,
                onNavigateToDetail = onNavigateToDetail,
        )
    }

    if (showWallpaperFallbackDialog) {
        AlertDialog(
                onDismissRequest = {
                    showWallpaperFallbackDialog = false
                    wallpaperButtonHasPermission = false
                },
                title = {
                    Text(text = context.getString(R.string.wallpaper_permission_fallback_title))
                },
                text = {
                    Text(text = context.getString(R.string.wallpaper_permission_fallback_message))
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                showWallpaperFallbackDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    wallpaperPermissionLauncher.launch(
                                            Manifest.permission.READ_MEDIA_IMAGES
                                    )
                                } else {
                                    scope.launch { tryFetchWallpaperWithFilesPermission() }
                                }
                            },
                    ) {
                        Text(text = context.getString(R.string.dialog_ok))
                    }
                },
                dismissButton = {
                    TextButton(
                            onClick = {
                                showWallpaperFallbackDialog = false
                                wallpaperButtonHasPermission = false
                                requiresImagePermissionAfterWallpaperSecurityError = true
                                viewModel.setWallpaperAvailable(false)
                            },
                    ) {
                        Text(text = context.getString(R.string.dialog_cancel))
                    }
                },
        )
    }

    if (showShortcutSourcePicker) {
        AppShortcutSourcePickerDialog(
                sources = filteredAppShortcutSources,
                onDismiss = { showShortcutSourcePicker = false },
                onSourceSelected = { source ->
                    showShortcutSourcePicker = false
                    onAddShortcutFromSource(source)
                },
        )
    }

    appActivityDialogSource?.let { selectedSource ->
        AppActivityPickerDialog(
                activities = appActivityDialogItems,
                onDismiss = {
                    appActivityDialogSource = null
                    appActivityDialogItems = emptyList()
                },
                onActivitySelected = { activity ->
                    appActivityDialogSource = null
                    appActivityDialogItems = emptyList()
                    viewModel.addCustomAppActivityShortcut(
                            packageName = selectedSource.packageName,
                            activityClassName = activity.className,
                            activityLabel = activity.label,
                            showDefaultToast = false,
                            onShortcutAdded = { addedShortcut ->
                                appShortcutFocusShortcut = addedShortcut
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
                },
        )
    }
}
