package com.tk.quicksearch.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import com.tk.quicksearch.ui.theme.ThemeMode

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel,
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Handle section toggle with permission check
    val onToggleSection = rememberSectionToggleHandler(viewModel, uiState.disabledSections)
    
    val state = SettingsScreenState(
        hiddenApps = uiState.hiddenApps,
        excludedContacts = uiState.excludedContacts,
        excludedFiles = uiState.excludedFiles,
        showAppLabels = uiState.showAppLabels,
        searchEngineOrder = uiState.searchEngineOrder,
        disabledSearchEngines = uiState.disabledSearchEngines,
        enabledFileTypes = uiState.enabledFileTypes,
        keyboardAlignedLayout = uiState.keyboardAlignedLayout,
        shortcutCodes = uiState.shortcutCodes,
        shortcutEnabled = uiState.shortcutEnabled,
        messagingApp = uiState.messagingApp,
        showSectionTitles = uiState.showSectionTitles,
        sectionOrder = uiState.sectionOrder,
        disabledSections = uiState.disabledSections,
        searchEngineSectionEnabled = uiState.searchEngineSectionEnabled,
        shortcutsEnabled = uiState.shortcutsEnabled,
        amazonDomain = uiState.amazonDomain
    )
    
    val callbacks = SettingsScreenCallbacks(
        onBack = onBack,
        onRemoveExcludedApp = viewModel::unhideApp,
        onRemoveExcludedContact = viewModel::removeExcludedContact,
        onRemoveExcludedFile = viewModel::removeExcludedFile,
        onClearAllExclusions = viewModel::clearAllExclusions,
        onToggleAppLabels = viewModel::setShowAppLabels,
        onToggleSearchEngine = viewModel::setSearchEngineEnabled,
        onReorderSearchEngines = viewModel::reorderSearchEngines,
        onToggleFileType = viewModel::setFileTypeEnabled,
        onToggleKeyboardAlignedLayout = viewModel::setKeyboardAlignedLayout,
        setShortcutCode = viewModel::setShortcutCode,
        setShortcutEnabled = viewModel::setShortcutEnabled,
        onSetMessagingApp = viewModel::setMessagingApp,
        onToggleShowSectionTitles = viewModel::setShowSectionTitles,
        onToggleSection = onToggleSection,
        onReorderSections = viewModel::reorderSections,
        onToggleSearchEngineSectionEnabled = viewModel::setSearchEngineSectionEnabled,
        onToggleShortcutsEnabled = viewModel::setShortcutsEnabled,
        onSetAmazonDomain = viewModel::setAmazonDomain
    )
    
    val context = LocalContext.current
    val userPreferences = remember { UserAppPreferences(context) }
    var currentThemeMode by remember { mutableStateOf(ThemeMode.fromString(userPreferences.getThemeMode())) }
    var shouldShowBanner by remember { mutableStateOf(userPreferences.shouldShowUsagePermissionBanner()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
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
        currentThemeMode = currentThemeMode,
        hasUsagePermission = uiState.hasUsagePermission,
        hasContactPermission = uiState.hasContactPermission,
        hasFilePermission = uiState.hasFilePermission,
        shouldShowBanner = shouldShowBanner,
        onRequestUsagePermission = viewModel::openUsageAccessSettings,
        onRequestContactPermission = onRequestContactPermission,
        onRequestFilePermission = viewModel::openFilesPermissionSettings,
        onDismissBanner = onDismissBanner,
        onThemeModeChange = { themeMode ->
            userPreferences.setThemeMode(themeMode.value)
            currentThemeMode = themeMode
            onThemeModeChange(themeMode)
        }
    )
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    currentThemeMode: ThemeMode,
    hasUsagePermission: Boolean,
    hasContactPermission: Boolean,
    hasFilePermission: Boolean,
    shouldShowBanner: Boolean,
    onRequestUsagePermission: () -> Unit,
    onRequestContactPermission: () -> Unit,
    onRequestFilePermission: () -> Unit,
    onDismissBanner: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    BackHandler(onBack = callbacks.onBack)
    val scrollState = rememberScrollState()
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

            // Appearance Section
            AppLabelsSection(
                showAppLabels = state.showAppLabels,
                onToggleAppLabels = callbacks.onToggleAppLabels,
                keyboardAlignedLayout = state.keyboardAlignedLayout,
                onToggleKeyboardAlignedLayout = callbacks.onToggleKeyboardAlignedLayout,
                showSectionTitles = state.showSectionTitles,
                onToggleShowSectionTitles = callbacks.onToggleShowSectionTitles,
                appsSectionEnabled = SearchSection.APPS !in state.disabledSections,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // Theme Section
            ThemeSection(
                themeMode = currentThemeMode,
                onThemeModeChange = onThemeModeChange,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // Contacts Section
            MessagingSection(
                messagingApp = state.messagingApp,
                onSetMessagingApp = callbacks.onSetMessagingApp,
                contactsSectionEnabled = SearchSection.CONTACTS !in state.disabledSections,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // Files Section
            FileTypesSection(
                enabledFileTypes = state.enabledFileTypes,
                onToggleFileType = callbacks.onToggleFileType,
                filesSectionEnabled = SearchSection.FILES !in state.disabledSections,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // Search Engine Section (includes shortcuts)
            SearchEnginesSection(
                searchEngineOrder = state.searchEngineOrder,
                disabledSearchEngines = state.disabledSearchEngines,
                onToggleSearchEngine = callbacks.onToggleSearchEngine,
                onReorderSearchEngines = callbacks.onReorderSearchEngines,
                shortcutCodes = state.shortcutCodes,
                setShortcutCode = callbacks.setShortcutCode,
                shortcutEnabled = state.shortcutEnabled,
                setShortcutEnabled = callbacks.setShortcutEnabled,
                searchEngineSectionEnabled = state.searchEngineSectionEnabled,
                onToggleSearchEngineSectionEnabled = callbacks.onToggleSearchEngineSectionEnabled,
                shortcutsEnabled = state.shortcutsEnabled,
                onToggleShortcutsEnabled = callbacks.onToggleShortcutsEnabled,
                amazonDomain = state.amazonDomain,
                onSetAmazonDomain = callbacks.onSetAmazonDomain,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )
            
            // Excluded Items Section
            ExcludedItemsSection(
                hiddenApps = state.hiddenApps,
                excludedContacts = state.excludedContacts,
                excludedFiles = state.excludedFiles,
                onRemoveExcludedApp = callbacks.onRemoveExcludedApp,
                onRemoveExcludedContact = callbacks.onRemoveExcludedContact,
                onRemoveExcludedFile = callbacks.onRemoveExcludedFile,
                onClearAll = callbacks.onClearAllExclusions
            )

            // Permissions Section (at the bottom)
            PermissionsSection(
                hasUsagePermission = hasUsagePermission,
                hasContactPermission = hasContactPermission,
                hasFilePermission = hasFilePermission,
                onRequestUsagePermission = onRequestUsagePermission,
                onRequestContactPermission = onRequestContactPermission,
                onRequestFilePermission = onRequestFilePermission,
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


