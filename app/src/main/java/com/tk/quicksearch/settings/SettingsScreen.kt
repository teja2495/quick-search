package com.tk.quicksearch.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.permissions.PermissionRequestHandler
import com.tk.quicksearch.search.MessagingApp
import com.tk.quicksearch.search.SearchSection
import com.tk.quicksearch.search.SearchViewModel

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel,
    onNavigateToDetail: (SettingsDetailType) -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Handle section toggle with permission check
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
        keyboardAlignedLayout = uiState.keyboardAlignedLayout,
        shortcutCodes = uiState.shortcutCodes,
        shortcutEnabled = uiState.shortcutEnabled,
        messagingApp = uiState.messagingApp,
        isWhatsAppInstalled = uiState.isWhatsAppInstalled,
        isTelegramInstalled = uiState.isTelegramInstalled,
        showWallpaperBackground = uiState.showWallpaperBackground,
        clearQueryAfterSearchEngine = uiState.clearQueryAfterSearchEngine,
        showAllResults = uiState.showAllResults,
        sortAppsByUsageEnabled = uiState.sortAppsByUsageEnabled,
        directDialEnabled = uiState.directDialEnabled,
        sectionOrder = uiState.sectionOrder,
        disabledSections = uiState.disabledSections,
        searchEngineSectionEnabled = uiState.searchEngineSectionEnabled,
        amazonDomain = uiState.amazonDomain,
        hasGeminiApiKey = uiState.hasGeminiApiKey,
        geminiApiKeyLast4 = uiState.geminiApiKeyLast4,
        personalContext = uiState.personalContext
    )
    
    val context = LocalContext.current
    val userPreferences = remember { UserAppPreferences(context) }
    var shouldShowBanner by remember { mutableStateOf(userPreferences.shouldShowUsagePermissionBanner()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingEnableDirectDial by remember { mutableStateOf(false) }
    
    // Launcher for contacts permission request
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Refresh permission state after request
        viewModel.handleOptionalPermissionChange()
        
        // If permission was not granted, check if we should open settings as fallback
        // This handles the case where permission was permanently denied
        if (!isGranted && context is android.app.Activity) {
            val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                context,
                Manifest.permission.READ_CONTACTS
            )
            // If we can't show rationale and permission is still denied,
            // it means permission was permanently denied, so open settings
            if (!shouldShowRationale && !PermissionRequestHandler.checkContactsPermission(context)) {
                viewModel.openContactPermissionSettings()
            }
        }
    }
    
    // Handler for contacts permission request - tries popup first, then settings
    val onRequestContactPermission = {
        // If context is not an Activity, we can't request permission via popup, so open settings
        if (context !is android.app.Activity) {
            viewModel.openContactPermissionSettings()
        } else {
            // Try to show permission popup first
            // If permission was permanently denied, Android will handle it gracefully
            // and we'll open settings in the launcher callback if needed
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }
    
    // Launcher for call permission request
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Refresh permission state after request
        viewModel.handleOptionalPermissionChange()

        if (isGranted && pendingEnableDirectDial) {
            viewModel.setDirectDialEnabled(true)
        }
        
        // If permission was not granted, check if we should open settings as fallback
        if (!isGranted && context is android.app.Activity) {
            val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                context,
                Manifest.permission.CALL_PHONE
            )
            // If we can't show rationale and permission is still denied,
            // it means permission was permanently denied, so open settings
            if (!shouldShowRationale && !com.tk.quicksearch.permissions.PermissionRequestHandler.checkCallPermission(context)) {
                viewModel.openAppSettings()
            }
        }

        pendingEnableDirectDial = false
    }
    
    // Handler for call permission request - tries popup first, then settings
    val onRequestCallPermission = {
        // If context is not an Activity, we can't request permission via popup, so open settings
        if (context !is android.app.Activity) {
            viewModel.openAppSettings()
        } else {
            // Try to show permission popup first
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    val onToggleDirectDial: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            if (PermissionRequestHandler.checkCallPermission(context)) {
                viewModel.setDirectDialEnabled(true)
            } else if (context !is android.app.Activity) {
                viewModel.openAppSettings()
            } else {
                pendingEnableDirectDial = true
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        } else {
            pendingEnableDirectDial = false
            viewModel.setDirectDialEnabled(false)
        }
    }

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
        onToggleKeyboardAlignedLayout = viewModel::setKeyboardAlignedLayout,
        setShortcutCode = viewModel::setShortcutCode,
        setShortcutEnabled = viewModel::setShortcutEnabled,
        onSetMessagingApp = viewModel::setMessagingApp,
        onToggleShowWallpaperBackground = viewModel::setShowWallpaperBackground,
        onToggleClearQueryAfterSearchEngine = viewModel::setClearQueryAfterSearchEngine,
        onToggleShowAllResults = viewModel::setShowAllResults,
        onToggleSortAppsByUsage = viewModel::setSortAppsByUsageEnabled,
        onToggleDirectDial = onToggleDirectDial,
        onToggleSection = onToggleSection,
        onReorderSections = viewModel::reorderSections,
        onToggleSearchEngineSectionEnabled = viewModel::setSearchEngineSectionEnabled,
        onSetAmazonDomain = viewModel::setAmazonDomain,
        onSetGeminiApiKey = viewModel::setGeminiApiKey,
        onSetPersonalContext = viewModel::setPersonalContext,
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
                        "Unable to open settings",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
                MessagingApp.WHATSAPP -> "WhatsApp"
                MessagingApp.TELEGRAM -> "Telegram"
                MessagingApp.MESSAGES -> "Messages"
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
                    // Reset session dismissed flag on app launch (when activity starts)
                    userPreferences.resetUsagePermissionBannerSessionDismissed()
                    shouldShowBanner = userPreferences.shouldShowUsagePermissionBanner()
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.handleOnResume()
                    // Also refresh banner state on resume
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

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    hasUsagePermission: Boolean,
    hasContactPermission: Boolean,
    hasFilePermission: Boolean,
    hasCallPermission: Boolean,
    shouldShowBanner: Boolean,
    onRequestUsagePermission: () -> Unit,
    onMessagingAppSelected: (MessagingApp) -> Unit,
    onRequestContactPermission: () -> Unit,
    onRequestFilePermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    onDismissBanner: () -> Unit,
    onNavigateToDetail: (SettingsDetailType) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState()
) {
    BackHandler(onBack = callbacks.onBack)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        SettingsHeader(onBack = callbacks.onBack)

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = SettingsSpacing.contentHorizontalPadding)
        ) {
            // Usage Permission Banner (at the top)
            // Show banner only if usage access permission is missing and user hasn't dismissed it twice
            if (!hasUsagePermission && shouldShowBanner) {
                UsagePermissionBanner(
                    onRequestPermission = onRequestUsagePermission,
                    onDismiss = onDismissBanner,
                    onCardClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                    modifier = Modifier.padding(bottom = SettingsSpacing.sectionTopPadding)
                )
            }

            // Search Sections Section
            SectionSettingsSection(
                sectionOrder = state.sectionOrder,
                disabledSections = state.disabledSections,
                onToggleSection = callbacks.onToggleSection,
                onReorderSections = callbacks.onReorderSections
            )

            // Internet Search Section
            Text(
                text = stringResource(R.string.settings_internet_search_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = SettingsSpacing.sectionTopPadding)
                    .padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
            )

            // Search Engine Section - Navigation Card
            SettingsNavigationCard(
                title = stringResource(R.string.settings_search_engines_title),
                description = stringResource(R.string.settings_search_engines_desc),
                onClick = { onNavigateToDetail(SettingsDetailType.SEARCH_ENGINES) },
                contentPadding = SettingsSpacing.singleCardPadding
            )

            // Appearance Section
            Text(
                text = stringResource(R.string.settings_app_labels_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = SettingsSpacing.sectionTopPadding)
                    .padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
            )
            AppLabelsSection(
                keyboardAlignedLayout = state.keyboardAlignedLayout,
                onToggleKeyboardAlignedLayout = callbacks.onToggleKeyboardAlignedLayout,
                showWallpaperBackground = state.showWallpaperBackground,
                onToggleShowWallpaperBackground = { enabled ->
                    if (enabled && !hasFilePermission) {
                        // Request files permission when user tries to enable wallpaper
                        onRequestFilePermission()
                    } else {
                        callbacks.onToggleShowWallpaperBackground(enabled)
                    }
                },
                hasFilePermission = hasFilePermission
            )

            // Contacts Section
            MessagingSection(
                messagingApp = state.messagingApp,
                onSetMessagingApp = callbacks.onSetMessagingApp,
                directDialEnabled = state.directDialEnabled,
                onToggleDirectDial = callbacks.onToggleDirectDial,
                hasCallPermission = hasCallPermission,
                contactsSectionEnabled = SearchSection.CONTACTS !in state.disabledSections,
                isWhatsAppInstalled = state.isWhatsAppInstalled,
                isTelegramInstalled = state.isTelegramInstalled,
                onMessagingAppSelected = onMessagingAppSelected,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // Files Section
            FileTypesSection(
                enabledFileTypes = state.enabledFileTypes,
                onToggleFileType = callbacks.onToggleFileType,
                filesSectionEnabled = SearchSection.FILES !in state.disabledSections,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // More Options Section
            Text(
                text = stringResource(R.string.settings_more_options_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = SettingsSpacing.sectionTopPadding)
                    .padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
            )

            // Combined Excluded Items and Additional Settings Card
            CombinedSettingsNavigationCard(
                excludedItemsTitle = stringResource(R.string.settings_excluded_items_title),
                excludedItemsDescription = stringResource(R.string.settings_excluded_items_desc),
                additionalSettingsTitle = stringResource(R.string.settings_additional_settings_title),
                additionalSettingsDescription = stringResource(R.string.settings_additional_settings_desc),
                hasExcludedItems = state.suggestionExcludedApps.isNotEmpty() ||
                                   state.resultExcludedApps.isNotEmpty() ||
                                   state.excludedContacts.isNotEmpty() ||
                                   state.excludedFiles.isNotEmpty() ||
                                   state.excludedSettings.isNotEmpty(),
                onExcludedItemsClick = { onNavigateToDetail(SettingsDetailType.EXCLUDED_ITEMS) },
                onAdditionalSettingsClick = { onNavigateToDetail(SettingsDetailType.ADDITIONAL_SETTINGS) },
                contentPadding = SettingsSpacing.singleCardPadding
            )

            // Permissions Section (at the bottom)
            PermissionsSection(
                hasUsagePermission = hasUsagePermission,
                hasContactPermission = hasContactPermission,
                hasFilePermission = hasFilePermission,
                hasCallPermission = hasCallPermission,
                onRequestUsagePermission = onRequestUsagePermission,
                onRequestContactPermission = onRequestContactPermission,
                onRequestFilePermission = onRequestFilePermission,
                onRequestCallPermission = onRequestCallPermission,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // Feedback button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SettingsSpacing.sectionTopPadding * 2),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(
                    onClick = {
                        val versionName = try {
                            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            packageInfo.versionName ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }
                        
                        val androidVersion = android.os.Build.VERSION.RELEASE
                        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                        
                        val emailBody = """
                            

                            
                            ---
                            App Version: $versionName
                            Android Version: $androidVersion
                            Device: $deviceModel
                        """.trimIndent()

                        val subject = "Quick Search Feedback"

                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:tejakarlapudi.apps@gmail.com?subject=${Uri.encode(subject)}&body=${Uri.encode(emailBody)}")
                        }

                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle case where no email app is installed
                        }
                    }
                ) {
                    Text(
                        text = "Send Feedback & Bug Reports",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // App Version
            Spacer(modifier = Modifier.height(16.dp))
            SettingsVersionDisplay()
        }
    }
}

/**
 * Header component for the settings screen.
 */
@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsSpacing.headerHorizontalPadding,
                vertical = SettingsSpacing.headerVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.desc_navigate_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(SettingsSpacing.headerIconSpacing))
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Displays the app version at the bottom of the settings screen.
 */
@Composable
private fun SettingsVersionDisplay() {
    val versionName = getAppVersionName()
    Text(
        text = stringResource(R.string.settings_app_version, versionName ?: "Unknown"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = SettingsSpacing.versionBottomPadding,
                top = 0.dp
            ),
        textAlign = TextAlign.Center
    )
}

/**
 * Combined navigation card for excluded items and additional settings with divider.
 */
@Composable
private fun CombinedSettingsNavigationCard(
    excludedItemsTitle: String,
    excludedItemsDescription: String,
    additionalSettingsTitle: String,
    additionalSettingsDescription: String,
    hasExcludedItems: Boolean,
    onExcludedItemsClick: () -> Unit,
    onAdditionalSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        // Excluded Items Section (only shown if there are excluded items)
        if (hasExcludedItems) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExcludedItemsClick)
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = excludedItemsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = excludedItemsDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.desc_navigate_forward),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Divider
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // Additional Settings Section (always shown)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAdditionalSettingsClick)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = additionalSettingsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = additionalSettingsDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.desc_navigate_forward),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}


