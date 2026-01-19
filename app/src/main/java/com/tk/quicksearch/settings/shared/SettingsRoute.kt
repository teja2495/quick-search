package com.tk.quicksearch.settings.shared

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.tile.requestAddQuickSearchTile

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel,
    onNavigateToDetail: (com.tk.quicksearch.settings.settingsDetailScreens.SettingsDetailType) -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState =
        androidx.compose.foundation.rememberScrollState()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            excludedFileExtensions = uiState.excludedFileExtensions,
            keyboardAlignedLayout = uiState.keyboardAlignedLayout,
            shortcutCodes = uiState.shortcutCodes,
            shortcutEnabled = uiState.shortcutEnabled,
            messagingApp = uiState.messagingApp,
            isWhatsAppInstalled = uiState.isWhatsAppInstalled,
            isTelegramInstalled = uiState.isTelegramInstalled,
            showWallpaperBackground = uiState.showWallpaperBackground,
            wallpaperBackgroundAlpha = uiState.wallpaperBackgroundAlpha,
            wallpaperBlurRadius = uiState.wallpaperBlurRadius,
            selectedIconPackPackage = uiState.selectedIconPackPackage,
            availableIconPacks = uiState.availableIconPacks,
            showAllResults = uiState.showAllResults,
            directDialEnabled = uiState.directDialEnabled,
            sectionOrder = uiState.sectionOrder,
            disabledSections = uiState.disabledSections,
            isSearchEngineCompactMode = uiState.isSearchEngineCompactMode,
            amazonDomain = uiState.amazonDomain,
            calculatorEnabled = uiState.calculatorEnabled,
            webSuggestionsEnabled = uiState.webSuggestionsEnabled,
            webSuggestionsCount = uiState.webSuggestionsCount,
            recentQueriesEnabled = uiState.recentQueriesEnabled,
            recentQueriesCount = uiState.recentQueriesCount,
            hasGeminiApiKey = uiState.hasGeminiApiKey,
            geminiApiKeyLast4 = uiState.geminiApiKeyLast4,
            personalContext = uiState.personalContext
        )

    val shouldShowBanner = remember { mutableStateOf(uiState.shouldShowUsagePermissionBanner) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val pendingEnableDirectDial = remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshIconPacks()
        viewModel.handleOptionalPermissionChange()
    }

    val contactsPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            handlePermissionResult(
                isGranted = isGranted,
                context = context,
                permission = Manifest.permission.READ_CONTACTS,
                onPermanentlyDenied = viewModel::openContactPermissionSettings,
                onPermissionChanged = viewModel::handleOptionalPermissionChange
            )
        }

    val onRequestContactPermission =
        createPermissionRequestHandler(
            context = context,
            permissionLauncher = contactsPermissionLauncher,
            permission = Manifest.permission.READ_CONTACTS,
            fallbackAction = viewModel::openContactPermissionSettings
        )

    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
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
                        onComplete = { pendingEnableDirectDial.value = false }
            )
        }

    val onRequestCallPermission =
        createPermissionRequestHandler(
            context = context,
            permissionLauncher = callPermissionLauncher,
            permission = Manifest.permission.CALL_PHONE,
            fallbackAction = viewModel::openAppSettings
        )

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

    val onRequestAddQuickSettingsTile = { requestAddQuickSearchTile(context) }

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
            onToggleFileType = viewModel::setFileTypeEnabled,
            onToggleFolders = viewModel::setShowFolders,
            onToggleSystemFiles = viewModel::setShowSystemFiles,
            onToggleHiddenFiles = viewModel::setShowHiddenFiles,
            onRemoveExcludedFileExtension = viewModel::removeExcludedFileExtension,
            onToggleKeyboardAlignedLayout = viewModel::setKeyboardAlignedLayout,
            setShortcutCode = viewModel::setShortcutCode,
            setShortcutEnabled = viewModel::setShortcutEnabled,
            onSetMessagingApp = viewModel::setMessagingApp,
            onToggleShowWallpaperBackground = viewModel::setShowWallpaperBackground,
            onWallpaperBackgroundAlphaChange = viewModel::setWallpaperBackgroundAlpha,
            onWallpaperBlurRadiusChange = viewModel::setWallpaperBlurRadius,
            onSelectIconPack = viewModel::setIconPackPackage,
            onSearchIconPacks = viewModel::searchIconPacks,
            onRefreshIconPacks = viewModel::refreshIconPacks,
            onToggleShowAllResults = viewModel::setShowAllResults,
            onToggleDirectDial = onToggleDirectDial,
            onToggleSection = onToggleSection,
            onReorderSections = viewModel::reorderSections,
            onToggleSearchEngineCompactMode = viewModel::setSearchEngineCompactMode,
            onSetAmazonDomain = viewModel::setAmazonDomain,
            onToggleCalculator = viewModel::setCalculatorEnabled,
            onToggleWebSuggestions = viewModel::setWebSuggestionsEnabled,
            onWebSuggestionsCountChange = viewModel::setWebSuggestionsCount,
            onToggleRecentQueries = viewModel::setRecentQueriesEnabled,
            onRecentQueriesCountChange = viewModel::setRecentQueriesCount,
            onSetGeminiApiKey = viewModel::setGeminiApiKey,
            onSetPersonalContext = viewModel::setPersonalContext,
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
                                R.string.settings_unable_to_open_settings
                            ),
                            Toast.LENGTH_SHORT
                        )
                        .show()
                    }
                }
            },
            onRefreshApps = { showToast ->
                if (SearchSection.APPS in uiState.disabledSections) {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.settings_refresh_apps_disabled
                        ),
                        Toast.LENGTH_SHORT
                    )
                    .show()
                } else {
                    viewModel.refreshApps(showToast)
                }
            },
            onRefreshContacts = { showToast ->
                if (SearchSection.CONTACTS in uiState.disabledSections) {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.settings_refresh_contacts_disabled
                        ),
                        Toast.LENGTH_SHORT
                    )
                    .show()
                } else {
                    viewModel.refreshContacts(showToast)
                }
            },
            onRefreshFiles = { showToast ->
                if (SearchSection.FILES in uiState.disabledSections) {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.settings_refresh_files_disabled
                        ),
                        Toast.LENGTH_SHORT
                    )
                    .show()
                } else {
                    viewModel.refreshFiles(showToast)
                }
            },
            onRequestUsagePermission = onRequestUsagePermission,
            onRequestContactPermission = onRequestContactPermission,
            onRequestFilePermission = onRequestFilePermission,
            onRequestCallPermission = onRequestCallPermission
        )


    // Refresh permission state and reset banner session dismissed flag when activity starts/resumes
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.resetUsagePermissionBannerSessionDismissed()
                    shouldShowBanner.value = viewModel.uiState.value.shouldShowUsagePermissionBanner
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.handleOnResume()
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

    com.tk.quicksearch.settings.settingsScreen.SettingsScreen(
        modifier = modifier,
        state = state,
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
        scrollState = scrollState
    )
}