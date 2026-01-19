package com.tk.quicksearch.settings.settingsScreen

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.util.hapticToggle
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.SettingsCard
import com.tk.quicksearch.settings.SettingsSpacing
import com.tk.quicksearch.settings.SettingsToggleRow
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.settings.settingsScreen.NavigationSection
import com.tk.quicksearch.settings.excludedItemsScreen.*
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEnginesSection
import com.tk.quicksearch.settings.settingsScreen.SectionSettingsSection
import com.tk.quicksearch.settings.settingsScreen.CombinedAppearanceCard
import com.tk.quicksearch.settings.settingsScreen.CombinedLayoutIconCard
import com.tk.quicksearch.settings.settingsScreen.SearchResultsSettingsSection
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngineAppearanceCard
import com.tk.quicksearch.settings.settingsScreen.CallsTextsSettingsSection
import com.tk.quicksearch.settings.settingsScreen.FileTypesSection
import com.tk.quicksearch.settings.settingsScreen.permissions.PermissionsSection
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.tile.requestAddQuickSearchTile
import com.tk.quicksearch.util.isDefaultDigitalAssistant

@Composable
fun SettingsDetailRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel,
    detailType: SettingsDetailType,
    onRequestUsagePermission: () -> Unit = {},
    onRequestContactPermission: () -> Unit = {},
    onRequestFilePermission: () -> Unit = {},
    onRequestCallPermission: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val state = SettingsScreenState(
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
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userPreferences = remember { UserAppPreferences(context) }
    var shouldShowShortcutHint by remember(detailType) {
        mutableStateOf(detailType == SettingsDetailType.SEARCH_ENGINES && userPreferences.shouldShowShortcutHintBanner())
    }
    var isDefaultAssistant by remember {
        mutableStateOf(context.isDefaultDigitalAssistant())
    }
    var directSearchSetupExpanded by remember(detailType) {
        mutableStateOf(
            if (detailType == SettingsDetailType.SEARCH_ENGINES) {
                // Always start expanded in search engine settings screen
                true
            } else {
                true
            }
        )
    }
    
    DisposableEffect(lifecycleOwner, detailType) {
        val observer = LifecycleEventObserver { _, event ->
            if (detailType != SettingsDetailType.SEARCH_ENGINES) return@LifecycleEventObserver
                when (event) {
                Lifecycle.Event.ON_START -> {
                    userPreferences.resetShortcutHintBannerSessionDismissed()
                    shouldShowShortcutHint = userPreferences.shouldShowShortcutHintBanner()
                        isDefaultAssistant = context.isDefaultDigitalAssistant()
                }
                Lifecycle.Event.ON_RESUME -> {
                    shouldShowShortcutHint = userPreferences.shouldShowShortcutHintBanner()
                        isDefaultAssistant = context.isDefaultDigitalAssistant()
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
    }

    val onRequestAddQuickSettingsTile = { requestAddQuickSearchTile(context) }
    
    val callbacks = SettingsScreenCallbacks(
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
        onToggleDirectDial = viewModel::setDirectDialEnabled,
        onToggleSection = { _, _ -> },
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
        onRefreshApps = viewModel::refreshApps,
        onRefreshContacts = viewModel::refreshContacts,
        onRefreshFiles = viewModel::refreshFiles,
        onRequestUsagePermission = onRequestUsagePermission,
        onRequestContactPermission = onRequestContactPermission,
        onRequestFilePermission = onRequestFilePermission,
        onRequestCallPermission = onRequestCallPermission
    )

    val onToggleDirectSearchSetupExpanded = {
        val newExpanded = !directSearchSetupExpanded
        directSearchSetupExpanded = newExpanded
        if (detailType == SettingsDetailType.SEARCH_ENGINES) {
            userPreferences.setDirectSearchSetupExpanded(newExpanded)
        }
    }

    SettingsDetailScreen(
        modifier = modifier,
        state = state,
        callbacks = callbacks,
        detailType = detailType,
        showShortcutHintBanner = shouldShowShortcutHint,
        onDismissShortcutHintBanner = onDismissShortcutHint,
        isDefaultAssistant = isDefaultAssistant,
        directSearchSetupExpanded = directSearchSetupExpanded,
        onToggleDirectSearchSetupExpanded = onToggleDirectSearchSetupExpanded
    )
}

@Composable
private fun SettingsDetailScreen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    detailType: SettingsDetailType,
    isDefaultAssistant: Boolean,
    showShortcutHintBanner: Boolean = false,
    onDismissShortcutHintBanner: () -> Unit = {},
    directSearchSetupExpanded: Boolean = true,
    onToggleDirectSearchSetupExpanded: (() -> Unit)? = null
) {
    val view = LocalView.current
    BackHandler(onBack = callbacks.onBack)
    val scrollState = rememberScrollState()
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    
    // Navigate back to settings when all excluded items are cleared
    val hasExcludedItems = state.suggestionExcludedApps.isNotEmpty() ||
                           state.resultExcludedApps.isNotEmpty() ||
                           state.excludedContacts.isNotEmpty() ||
                           state.excludedFiles.isNotEmpty() ||
                           state.excludedSettings.isNotEmpty() ||
                           state.excludedAppShortcuts.isNotEmpty()
    LaunchedEffect(hasExcludedItems) {
        if (detailType == SettingsDetailType.EXCLUDED_ITEMS && !hasExcludedItems) {
            callbacks.onBack()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SettingsDetailHeader(
                title = when (detailType) {
                    SettingsDetailType.SEARCH_ENGINES -> stringResource(R.string.settings_search_engines_title)
                    SettingsDetailType.EXCLUDED_ITEMS -> stringResource(R.string.settings_excluded_items_title)
                    SettingsDetailType.SEARCH_RESULTS -> stringResource(R.string.settings_search_results_title)
                    SettingsDetailType.APPEARANCE -> stringResource(R.string.settings_appearance_title)
                    SettingsDetailType.CALLS_TEXTS -> stringResource(R.string.settings_calls_texts_title)
                    SettingsDetailType.FILES -> stringResource(R.string.settings_file_types_title)
                    SettingsDetailType.LAUNCH_OPTIONS -> stringResource(R.string.settings_launch_options_title)
                    SettingsDetailType.PERMISSIONS -> stringResource(R.string.settings_permissions_title)
                    SettingsDetailType.FEEDBACK_DEVELOPMENT -> stringResource(R.string.settings_feedback_development_title)
                },
                onBack = callbacks.onBack,
                trailingContent = null
            )

            val contentModifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = SettingsSpacing.contentHorizontalPadding,
                    end = SettingsSpacing.contentHorizontalPadding,
                    bottom = SettingsSpacing.sectionTopPadding
                )

            Column(
                modifier = contentModifier
            ) {
                when (detailType) {
                    SettingsDetailType.SEARCH_ENGINES -> {
                        SearchEnginesSection(
                            searchEngineOrder = state.searchEngineOrder,
                            disabledSearchEngines = state.disabledSearchEngines,
                            onToggleSearchEngine = callbacks.onToggleSearchEngine,
                            onReorderSearchEngines = callbacks.onReorderSearchEngines,
                            shortcutCodes = state.shortcutCodes,
                            setShortcutCode = callbacks.setShortcutCode,
                            shortcutEnabled = state.shortcutEnabled,
                            setShortcutEnabled = callbacks.setShortcutEnabled,
                            isSearchEngineCompactMode = state.isSearchEngineCompactMode,
                            amazonDomain = state.amazonDomain,
                            onSetAmazonDomain = callbacks.onSetAmazonDomain,
                            onSetGeminiApiKey = callbacks.onSetGeminiApiKey,
                            geminiApiKeyLast4 = state.geminiApiKeyLast4,
                            personalContext = state.personalContext,
                            onSetPersonalContext = callbacks.onSetPersonalContext,
                            directSearchAvailable = state.hasGeminiApiKey,
                            showTitle = false,
                            showShortcutHintBanner = showShortcutHintBanner,
                            onDismissShortcutHintBanner = onDismissShortcutHintBanner,
                            directSearchSetupExpanded = directSearchSetupExpanded,
                            onToggleDirectSearchSetupExpanded = onToggleDirectSearchSetupExpanded,
                            onToggleSearchEngineCompactMode = callbacks.onToggleSearchEngineCompactMode,
                            showDirectSearchAtTop = true
                        )
                    }
                    SettingsDetailType.EXCLUDED_ITEMS -> {
                        ExcludedItemScreen(
                            suggestionExcludedApps = state.suggestionExcludedApps,
                            resultExcludedApps = state.resultExcludedApps,
                            excludedContacts = state.excludedContacts,
                            excludedFiles = state.excludedFiles,
                            excludedFileExtensions = state.excludedFileExtensions,
                            excludedSettings = state.excludedSettings,
                            excludedAppShortcuts = state.excludedAppShortcuts,
                            onRemoveSuggestionExcludedApp = callbacks.onRemoveSuggestionExcludedApp,
                            onRemoveResultExcludedApp = callbacks.onRemoveResultExcludedApp,
                            onRemoveExcludedContact = callbacks.onRemoveExcludedContact,
                            onRemoveExcludedFile = callbacks.onRemoveExcludedFile,
                            onRemoveExcludedFileExtension = callbacks.onRemoveExcludedFileExtension,
                            onRemoveExcludedSetting = callbacks.onRemoveExcludedSetting,
                            onRemoveExcludedAppShortcut = callbacks.onRemoveExcludedAppShortcut,
                            onClearAll = callbacks.onClearAllExclusions,
                            showTitle = false,
                            iconPackPackage = state.selectedIconPackPackage
                        )
                    }
                    SettingsDetailType.SEARCH_RESULTS -> {
                        SearchResultsSettingsSection(
                            state = state,
                            callbacks = callbacks
                        )
                    }
                    SettingsDetailType.APPEARANCE -> {
                        val appearanceContext = LocalContext.current

                        Column {
                            val hasIconPacks = state.availableIconPacks.isNotEmpty()
                            val selectedIconPackLabel =
                                remember(state.selectedIconPackPackage, state.availableIconPacks) {
                                    state.availableIconPacks
                                        .firstOrNull { it.packageName == state.selectedIconPackPackage }
                                        ?.label
                                        ?: appearanceContext.getString(R.string.settings_icon_pack_option_system)
                                }

                            // Wallpaper Background Card
                            CombinedAppearanceCard(
                                showWallpaperBackground = state.showWallpaperBackground,
                                wallpaperBackgroundAlpha = state.wallpaperBackgroundAlpha,
                                wallpaperBlurRadius = state.wallpaperBlurRadius,
                                onToggleShowWallpaperBackground = callbacks.onToggleShowWallpaperBackground,
                                onWallpaperBackgroundAlphaChange = callbacks.onWallpaperBackgroundAlphaChange,
                                onWallpaperBlurRadiusChange = callbacks.onWallpaperBlurRadiusChange,
                                hasFilePermission = true // Assume permission is granted in detail screen
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Search Engine Style Card
                            SearchEngineAppearanceCard(
                                isSearchEngineCompactMode = state.isSearchEngineCompactMode,
                                onToggleSearchEngineCompactMode = callbacks.onToggleSearchEngineCompactMode
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // One-Handed Mode and Icon Pack Card
                            CombinedLayoutIconCard(
                                keyboardAlignedLayout = state.keyboardAlignedLayout,
                                onToggleKeyboardAlignedLayout = callbacks.onToggleKeyboardAlignedLayout,
                                iconPackTitle = stringResource(R.string.settings_icon_pack_title),
                                iconPackDescription =
                                    if (hasIconPacks) {
                                        stringResource(
                                            R.string.settings_icon_pack_selected_label,
                                            selectedIconPackLabel
                                        )
                                    } else {
                                        stringResource(R.string.settings_icon_pack_empty)
                                    },
                                onIconPackClick = {
                                    if (hasIconPacks) {
                                        // Could show icon pack dialog here, but for now just show toast
                                        Toast.makeText(appearanceContext, "Icon pack selection not available in detail screen", Toast.LENGTH_SHORT).show()
                                    } else {
                                        callbacks.onSearchIconPacks()
                                    }
                                },
                                onRefreshIconPacks = {
                                    callbacks.onRefreshIconPacks()
                                    Toast.makeText(
                                        appearanceContext,
                                        appearanceContext.getString(R.string.settings_refreshing_icon_packs),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                    SettingsDetailType.CALLS_TEXTS -> {
                        CallsTextsSettingsSection(
                            messagingApp = state.messagingApp,
                            onSetMessagingApp = callbacks.onSetMessagingApp,
                            directDialEnabled = state.directDialEnabled,
                            onToggleDirectDial = callbacks.onToggleDirectDial,
                            hasCallPermission = true, // Assume permission is granted in detail screen
                            contactsSectionEnabled = SearchSection.CONTACTS !in state.disabledSections,
                            isWhatsAppInstalled = state.isWhatsAppInstalled,
                            isTelegramInstalled = state.isTelegramInstalled,
                            modifier = Modifier
                        )
                    }
                    SettingsDetailType.FILES -> {
                        FileTypesSection(
                            enabledFileTypes = state.enabledFileTypes,
                            onToggleFileType = callbacks.onToggleFileType,
                            showFolders = state.showFolders,
                            onToggleFolders = callbacks.onToggleFolders,
                            showSystemFiles = state.showSystemFiles,
                            onToggleSystemFiles = callbacks.onToggleSystemFiles,
                            showHiddenFiles = state.showHiddenFiles,
                            onToggleHiddenFiles = callbacks.onToggleHiddenFiles,
                            excludedExtensions = state.excludedFileExtensions,
                            onRemoveExcludedExtension = callbacks.onRemoveExcludedFileExtension,
                            filesSectionEnabled = SearchSection.FILES !in state.disabledSections,
                            showTitle = false,
                            modifier = Modifier
                        )
                    }
                    SettingsDetailType.LAUNCH_OPTIONS -> {
                        LaunchOptionsSettings(
                            isDefaultAssistant = isDefaultAssistant,
                            onSetDefaultAssistant = callbacks.onSetDefaultAssistant,
                            onAddQuickSettingsTile = callbacks.onAddQuickSettingsTile,
                            modifier = Modifier
                        )
                    }
                    SettingsDetailType.PERMISSIONS -> {
                        PermissionsSection(
                            hasUsagePermission = true, // Assume granted in detail screen
                            hasContactPermission = true, // Assume granted in detail screen
                            hasFilePermission = true, // Assume granted in detail screen
                            hasCallPermission = true, // Assume granted in detail screen
                            onRequestUsagePermission = { /* No-op in detail screen */ },
                            onRequestContactPermission = { /* No-op in detail screen */ },
                            onRequestFilePermission = { /* No-op in detail screen */ },
                            onRequestCallPermission = { /* No-op in detail screen */ },
                            showTitle = false,
                            modifier = Modifier
                        )
                    }
                    SettingsDetailType.FEEDBACK_DEVELOPMENT -> {
                        // Feedback & development content would go here
                        Text(
                            text = "Feedback & development options will be implemented here",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
            }
        }

        if (detailType == SettingsDetailType.EXCLUDED_ITEMS) {
            FloatingActionButton(
                onClick = { showClearAllConfirmation = true },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.settings_action_clear_all),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Clear All confirmation dialog
        if (showClearAllConfirmation && detailType == SettingsDetailType.EXCLUDED_ITEMS) {
            ClearAllConfirmationDialog(
                onConfirm = {
                    callbacks.onClearAllExclusions()
                    showClearAllConfirmation = false
                },
                onDismiss = { showClearAllConfirmation = false }
            )
        }
    }
}

/**
 * Header component for the settings detail screen.
 */
@Composable
private fun SettingsDetailHeader(
    title: String,
    onBack: () -> Unit,
    trailingContent: (@Composable (() -> Unit))? = null
) {
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
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailingContent?.let {
            Spacer(modifier = Modifier.width(SettingsSpacing.headerIconSpacing))
            it()
        }
    }
}


/**
 * Enum to represent different types of settings detail screens.
 */
enum class SettingsDetailType {
    SEARCH_ENGINES,
    EXCLUDED_ITEMS,
    SEARCH_RESULTS,
    APPEARANCE,
    CALLS_TEXTS,
    FILES,
    LAUNCH_OPTIONS,
    PERMISSIONS,
    FEEDBACK_DEVELOPMENT
}
