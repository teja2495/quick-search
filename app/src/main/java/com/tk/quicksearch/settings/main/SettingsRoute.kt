package com.tk.quicksearch.settings.main

import android.Manifest
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.permissions.PermissionRequestHandler
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.tiles.requestAddQuickSearchTile

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel,
    onNavigateToDetail: (SettingsDetailType) -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val onToggleSection = rememberSectionToggleHandler(viewModel, uiState.disabledSections)

    val state = SettingsScreenState(
        suggestionExcludedApps = uiState.suggestionExcludedApps,
        resultExcludedApps = uiState.resultExcludedApps,
        excludedContacts = uiState.excludedContacts,
        excludedFiles = uiState.excludedFiles,
        excludedSettings = uiState.excludedSettings,
        searchEngineOrder = uiState.searchEngineOrder,
        disabledSearchEngines = uiState.disabledSearchEngines,
        enabledFileTypes = uiState.enabledFileTypes,
        excludedFileExtensions = uiState.excludedFileExtensions,
        keyboardAlignedLayout = uiState.keyboardAlignedLayout,
        shortcutCodes = uiState.shortcutCodes,
        shortcutEnabled = uiState.shortcutEnabled,
        messagingApp = uiState.messagingApp,
        isWhatsAppInstalled = uiState.isWhatsAppInstalled,
        isTelegramInstalled = uiState.isTelegramInstalled,
        showWallpaperBackground = uiState.showWallpaperBackground,
        selectedIconPackPackage = uiState.selectedIconPackPackage,
        availableIconPacks = uiState.availableIconPacks,
        clearQueryAfterSearchEngine = uiState.clearQueryAfterSearchEngine,
        showAllResults = uiState.showAllResults,
        sortAppsByUsageEnabled = uiState.sortAppsByUsageEnabled,
        directDialEnabled = uiState.directDialEnabled,
        sectionOrder = uiState.sectionOrder,
        disabledSections = uiState.disabledSections,
        searchEngineSectionEnabled = uiState.searchEngineSectionEnabled,
        amazonDomain = uiState.amazonDomain,
        calculatorEnabled = uiState.calculatorEnabled,
        webSuggestionsEnabled = uiState.webSuggestionsEnabled,
        hasGeminiApiKey = uiState.hasGeminiApiKey,
        geminiApiKeyLast4 = uiState.geminiApiKeyLast4,
        personalContext = uiState.personalContext
    )

    val context = LocalContext.current
    val userPreferences = remember { UserAppPreferences(context) }
    var shouldShowBanner by remember { mutableStateOf(userPreferences.shouldShowUsagePermissionBanner()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingEnableDirectDial by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshIconPacks()
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
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

    val onRequestContactPermission = createPermissionRequestHandler(
        context = context,
        permissionLauncher = contactsPermissionLauncher,
        permission = Manifest.permission.READ_CONTACTS,
        fallbackAction = viewModel::openContactPermissionSettings
    )

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handlePermissionResult(
            isGranted = isGranted,
            context = context,
            permission = Manifest.permission.CALL_PHONE,
            onPermanentlyDenied = viewModel::openAppSettings,
            onPermissionChanged = viewModel::handleOptionalPermissionChange,
            onGranted = {
                if (pendingEnableDirectDial) {
                    viewModel.setDirectDialEnabled(true)
                }
            },
            onComplete = { pendingEnableDirectDial = false }
        )
    }

    val onRequestCallPermission = createPermissionRequestHandler(
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
                pendingEnableDirectDial = true
                onRequestCallPermission()
            }
        } else {
            pendingEnableDirectDial = false
            viewModel.setDirectDialEnabled(false)
        }
    }

    val onRequestAddQuickSettingsTile = { requestAddQuickSearchTile(context) }

    val callbacks = SettingsScreenCallbacks(
        onBack = onBack,
        onRemoveSuggestionExcludedApp = viewModel::unhideAppFromSuggestions,
        onRemoveResultExcludedApp = viewModel::unhideAppFromResults,
        onRemoveExcludedContact = viewModel::removeExcludedContact,
        onRemoveExcludedFile = viewModel::removeExcludedFile,
        onRemoveExcludedSetting = viewModel::removeExcludedSetting,
        onClearAllExclusions = viewModel::clearAllExclusions,
        onToggleSearchEngine = viewModel::setSearchEngineEnabled,
        onReorderSearchEngines = viewModel::reorderSearchEngines,
        onToggleFileType = viewModel::setFileTypeEnabled,
        onRemoveExcludedFileExtension = viewModel::removeExcludedFileExtension,
        onToggleKeyboardAlignedLayout = viewModel::setKeyboardAlignedLayout,
        setShortcutCode = viewModel::setShortcutCode,
        setShortcutEnabled = viewModel::setShortcutEnabled,
        onSetMessagingApp = viewModel::setMessagingApp,
        onToggleShowWallpaperBackground = viewModel::setShowWallpaperBackground,
        onSelectIconPack = viewModel::setIconPackPackage,
        onSearchIconPacks = viewModel::searchIconPacks,
        onToggleClearQueryAfterSearchEngine = viewModel::setClearQueryAfterSearchEngine,
        onToggleShowAllResults = viewModel::setShowAllResults,
        onToggleSortAppsByUsage = viewModel::setSortAppsByUsageEnabled,
        onToggleDirectDial = onToggleDirectDial,
        onToggleSection = onToggleSection,
        onReorderSections = viewModel::reorderSections,
        onToggleSearchEngineSectionEnabled = viewModel::setSearchEngineSectionEnabled,
        onSetAmazonDomain = viewModel::setAmazonDomain,
        onToggleCalculator = viewModel::setCalculatorEnabled,
        onToggleWebSuggestions = viewModel::setWebSuggestionsEnabled,
        onSetGeminiApiKey = viewModel::setGeminiApiKey,
        onSetPersonalContext = viewModel::setPersonalContext,
        onAddQuickSettingsTile = onRequestAddQuickSettingsTile,
        onSetDefaultAssistant = {
            try {
                val intent = Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general settings if voice input settings not available
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_unable_to_open_settings),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        onRefreshApps = { showToast ->
            if (SearchSection.APPS in uiState.disabledSections) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_refresh_apps_disabled),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.refreshApps(showToast)
            }
        },
        onRefreshContacts = { showToast ->
            if (SearchSection.CONTACTS in uiState.disabledSections) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_refresh_contacts_disabled),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.refreshContacts(showToast)
            }
        },
        onRefreshFiles = { showToast ->
            if (SearchSection.FILES in uiState.disabledSections) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_refresh_files_disabled),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.refreshFiles(showToast)
            }
        }
    )

    // Callback for messaging app selection with installation check
    val onMessagingAppSelected: (MessagingApp) -> Unit = { app ->
        val isInstalled = when (app) {
            MessagingApp.MESSAGES -> true // Messages is always available
            MessagingApp.WHATSAPP -> uiState.isWhatsAppInstalled
            MessagingApp.TELEGRAM -> uiState.isTelegramInstalled
        }

        if (isInstalled) {
            callbacks.onSetMessagingApp(app)
        } else {
            val appName = when (app) {
                MessagingApp.WHATSAPP -> context.getString(R.string.settings_messaging_option_whatsapp)
                MessagingApp.TELEGRAM -> context.getString(R.string.settings_messaging_option_telegram)
                MessagingApp.MESSAGES -> context.getString(R.string.settings_messaging_option_messages)
            }
            Toast.makeText(
                context,
                context.getString(R.string.settings_messaging_app_not_installed, appName),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Refresh permission state and reset banner session dismissed flag when activity starts/resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    userPreferences.resetUsagePermissionBannerSessionDismissed()
                    shouldShowBanner = userPreferences.shouldShowUsagePermissionBanner()
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.handleOnResume()
                    shouldShowBanner = userPreferences.shouldShowUsagePermissionBanner()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onDismissBanner = {
        userPreferences.incrementUsagePermissionBannerDismissCount()
        userPreferences.setUsagePermissionBannerSessionDismissed(true)
        shouldShowBanner = userPreferences.shouldShowUsagePermissionBanner()
    }

    SettingsScreen(
        modifier = modifier,
        state = state,
        callbacks = callbacks,
        hasUsagePermission = uiState.hasUsagePermission,
        onMessagingAppSelected = onMessagingAppSelected,
        hasContactPermission = uiState.hasContactPermission,
        hasFilePermission = uiState.hasFilePermission,
        hasCallPermission = uiState.hasCallPermission,
        shouldShowBanner = shouldShowBanner,
        onRequestUsagePermission = viewModel::openUsageAccessSettings,
        onRequestContactPermission = onRequestContactPermission,
        onRequestFilePermission = viewModel::openFilesPermissionSettings,
        onRequestCallPermission = onRequestCallPermission,
        onDismissBanner = onDismissBanner,
        onNavigateToDetail = onNavigateToDetail,
        scrollState = scrollState
    )
}
