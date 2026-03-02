package com.tk.quicksearch.settings.shared

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChevronRight
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shortcut
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.tools.directSearch.GeminiTextModel
import com.tk.quicksearch.tile.requestAddQuickSearchTile
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.widgets.utils.requestAddQuickSearchWidget
import com.tk.quicksearch.settings.appShortcuts.*
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenState
import com.tk.quicksearch.settings.shared.SectionSettingsSection
import com.tk.quicksearch.settings.shared.createPermissionRequestHandler
import com.tk.quicksearch.settings.shared.handlePermissionResult
import com.tk.quicksearch.settings.shared.rememberSectionToggleHandler
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel,
    onNavigateToDetail: (com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType) -> Unit =
        {},
    scrollState: androidx.compose.foundation.ScrollState =
        androidx.compose.foundation.rememberScrollState(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userPreferences = remember { UserAppPreferences(context) }

    val onToggleSection = rememberSectionToggleHandler(viewModel, uiState.disabledSections)

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

    val shouldShowBanner = remember { mutableStateOf(uiState.shouldShowUsagePermissionBanner) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val pendingEnableDirectDial = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showWallpaperFallbackDialog by remember { mutableStateOf(false) }
    var requiresImagePermissionAfterWallpaperSecurityError by remember { mutableStateOf(false) }
    var wallpaperButtonHasPermission by
        remember { mutableStateOf(PermissionRequestHandler.checkFilesPermission(context)) }

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

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshIconPacks()
        viewModel.handleOptionalPermissionChange()
    }

    val contactsPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            handlePermissionResult(
                isGranted = isGranted,
                context = context,
                permission = Manifest.permission.READ_CONTACTS,
                onPermanentlyDenied = viewModel::openContactPermissionSettings,
                onPermissionChanged = viewModel::handleOptionalPermissionChange,
            )
        }

    val onRequestContactPermission =
        createPermissionRequestHandler(
            context = context,
            permissionLauncher = contactsPermissionLauncher,
            permission = Manifest.permission.READ_CONTACTS,
            fallbackAction = viewModel::openContactPermissionSettings,
        )

    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            handlePermissionResult(
                isGranted = isGranted,
                context = context,
                permission = Manifest.permission.CALL_PHONE,
                onPermanentlyDenied = viewModel::openAppSettings,
                onPermissionChanged = viewModel::handleOptionalPermissionChange,
                onGranted = {
                    if (pendingEnableDirectDial.value) {
                        viewModel.setDirectDialEnabled(true)
                    }
                },
                onComplete = { pendingEnableDirectDial.value = false },
            )
        }

    val onRequestCallPermission =
        createPermissionRequestHandler(
            context = context,
            permissionLauncher = callPermissionLauncher,
            permission = Manifest.permission.CALL_PHONE,
            fallbackAction = viewModel::openAppSettings,
        )

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

    val legacyFilesPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                wallpaperButtonHasPermission = !requiresImagePermissionAfterWallpaperSecurityError
                scope.launch { tryFetchWallpaperWithFilesPermission() }
            } else {
                wallpaperButtonHasPermission = false
                viewModel.setWallpaperAvailable(false)
            }
            viewModel.handleOptionalPermissionChange()
        }

    val onRequestWallpaperPermission: () -> Unit = {
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

    val onToggleDirectDial: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            if (PermissionRequestHandler.checkCallPermission(context)) {
                viewModel.setDirectDialEnabled(true)
            } else {
                pendingEnableDirectDial.value = true
                onRequestCallPermission()
            }
        } else {
            pendingEnableDirectDial.value = false
            viewModel.setDirectDialEnabled(false)
        }
    }

    val onRequestAddHomeScreenWidget = { requestAddQuickSearchWidget(context) }
    val onRequestAddQuickSettingsTile = { requestAddQuickSearchTile(context) }

    val onToggleOverlayMode: (Boolean) -> Unit = { enabled ->
        viewModel.setOverlayModeEnabled(enabled)
    }
    var showShortcutSourcePicker by remember { mutableStateOf(false) }
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
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.settings_app_shortcuts_create_not_supported),
                            Toast.LENGTH_SHORT,
                        ).show()
                }
        }
    }

    // Define permission request handlers
    val onRequestUsagePermission = viewModel::openUsageAccessSettings
    val onRequestFilePermission = viewModel::openFilesPermissionSettings

    val callbacks =
        SettingsScreenCallbacks(
            onBack = onBack,
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
            onToggleOverlayMode = onToggleOverlayMode,
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
            onPickCustomImage = {},
            onSelectIconPack = viewModel::setIconPackPackage,
            onSearchIconPacks = viewModel::searchIconPacks,
            onRefreshIconPacks = viewModel::refreshIconPacks,
            onToggleAppLabels = viewModel::setShowAppLabels,
            onToggleDirectDial = onToggleDirectDial,
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
                onNavigateToDetail(com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType.DIRECT_SEARCH_CONFIGURE)
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
                        Toast
                            .makeText(
                                context,
                                context.getString(
                                    R.string.settings_unable_to_open_settings,
                                ),
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            },
            onToggleAssistantLaunchVoiceMode = { enabled ->
                userPreferences.setAssistantLaunchVoiceModeEnabled(enabled)
            },
            onRefreshApps = { showToast ->
                if (SearchSection.APPS in uiState.disabledSections) {
                    Toast
                        .makeText(
                            context,
                            context.getString(
                                R.string.settings_refresh_apps_disabled,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    viewModel.refreshApps(showToast)
                }
            },
            onRefreshContacts = { showToast ->
                if (SearchSection.CONTACTS in uiState.disabledSections) {
                    Toast
                        .makeText(
                            context,
                            context.getString(
                                R.string.settings_refresh_contacts_disabled,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    viewModel.refreshContacts(showToast)
                }
            },
            onRefreshFiles = { showToast ->
                if (SearchSection.FILES in uiState.disabledSections) {
                    Toast
                        .makeText(
                            context,
                            context.getString(
                                R.string.settings_refresh_files_disabled,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    viewModel.refreshFiles(showToast)
                }
            },
            onRequestUsagePermission = onRequestUsagePermission,
            onRequestContactPermission = onRequestContactPermission,
            onRequestFilePermission = onRequestFilePermission,
            onRequestCallPermission = onRequestCallPermission,
            onRequestWallpaperPermission = onRequestWallpaperPermission,
        )

    // Refresh permission state and reset banner session dismissed flag when activity starts/resumes
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        viewModel.resetUsagePermissionBannerSessionDismissed()
                        shouldShowBanner.value = viewModel.uiState.value.shouldShowUsagePermissionBanner
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        viewModel.handleOnResume()
                        wallpaperButtonHasPermission =
                            PermissionRequestHandler.checkFilesPermission(context) &&
                                !requiresImagePermissionAfterWallpaperSecurityError
                        shouldShowBanner.value = viewModel.uiState.value.shouldShowUsagePermissionBanner
                    }

                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onDismissBanner = {
        viewModel.incrementUsagePermissionBannerDismissCount()
        viewModel.setUsagePermissionBannerSessionDismissed(true)
        shouldShowBanner.value = viewModel.uiState.value.shouldShowUsagePermissionBanner
    }

    val resolvedState = state.copy(hasWallpaperPermission = wallpaperButtonHasPermission)

    com.tk.quicksearch.settings.settingsScreen.SettingsScreen(
        modifier = modifier,
        state = resolvedState,
        callbacks = callbacks,
        hasUsagePermission = uiState.hasUsagePermission,
        hasContactPermission = uiState.hasContactPermission,
        hasFilePermission = uiState.hasFilePermission,
        hasCallPermission = uiState.hasCallPermission,
        shouldShowBanner = shouldShowBanner.value,
        onRequestUsagePermission = viewModel::openUsageAccessSettings,
        onRequestContactPermission = onRequestContactPermission,
        onRequestFilePermission = viewModel::openFilesPermissionSettings,
        onRequestCallPermission = onRequestCallPermission,
        onDismissBanner = onDismissBanner,
        onNavigateToDetail = onNavigateToDetail,
        onSettingsImported = viewModel::handleOnResume,
        scrollState = scrollState,
    )

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
                            wallpaperPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
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
        val appShortcutSources =
            remember(state.allApps) {
                queryAppShortcutSources(
                    packageManager = context.packageManager,
                    repositoryApps = state.allApps,
                )
            }
        val filteredAppShortcutSources =
            remember(appShortcutSources, state.allAppShortcuts, context.packageName) {
                filterAppShortcutSources(
                    sources = appShortcutSources,
                    existingShortcuts = state.allAppShortcuts,
                    currentPackageName = context.packageName,
                )
            }
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
                )
            },
        )
    }
}
