package com.tk.quicksearch.settings.settingsDetailScreen

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.util.hapticToggle
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.settings.settingsDetailScreen.ExcludedItemScreen
import com.tk.quicksearch.settings.settingsDetailScreen.ClearAllConfirmationDialog
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngines
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngineAppearanceCard
import com.tk.quicksearch.settings.settingsDetailScreen.CallsTextsSettingsSection
import com.tk.quicksearch.settings.settingsDetailScreen.FileTypesSection
import com.tk.quicksearch.search.core.SearchSection


@Composable
internal fun SettingsDetailScreen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    detailType: SettingsDetailType,
    isDefaultAssistant: Boolean,
    showShortcutHintBanner: Boolean = false,
    onDismissShortcutHintBanner: () -> Unit = {},
    directSearchSetupExpanded: Boolean = true,
    onToggleDirectSearchSetupExpanded: (() -> Unit)? = null,
    onNavigateToDetail: (SettingsDetailType) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    BackHandler(onBack = callbacks.onBack)
    val scrollState = rememberScrollState()
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    
    // Navigate back to settings when all excluded items are cleared
    val hasExcludedItems = state.suggestionExcludedApps.isNotEmpty() ||
                           state.resultExcludedApps.isNotEmpty() ||
                           state.excludedContacts.isNotEmpty() ||
                           state.excludedFiles.isNotEmpty() ||
                           state.excludedFileExtensions.isNotEmpty() ||
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
                    start = DesignTokens.ContentHorizontalPadding,
                    end = DesignTokens.ContentHorizontalPadding,
                    bottom = DesignTokens.SectionTopPadding
                )

            Column(
                modifier = contentModifier
            ) {
                when (detailType) {
                    SettingsDetailType.SEARCH_ENGINES -> {
                        SearchEngines(
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
                            callbacks = callbacks,
                            onNavigateToExcludedItems = {
                                if (hasExcludedItems) {
                                    onNavigateToDetail(SettingsDetailType.EXCLUDED_ITEMS)
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_excluded_items_empty),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                    SettingsDetailType.APPEARANCE -> {
                        val appearanceContext = LocalContext.current

                        AppearanceSettingsSection(
                            oneHandedMode = state.oneHandedMode,
                            onToggleOneHandedMode = callbacks.onToggleOneHandedMode,
                            showWallpaperBackground = state.showWallpaperBackground,
                            wallpaperBackgroundAlpha = state.wallpaperBackgroundAlpha,
                            wallpaperBlurRadius = state.wallpaperBlurRadius,
                            onToggleShowWallpaperBackground = callbacks.onToggleShowWallpaperBackground,
                            onWallpaperBackgroundAlphaChange = callbacks.onWallpaperBackgroundAlphaChange,
                            onWallpaperBlurRadiusChange = callbacks.onWallpaperBlurRadiusChange,
                            onRequestWallpaperPermission = callbacks.onRequestWallpaperPermission,
                            isSearchEngineCompactMode = state.isSearchEngineCompactMode,
                            onToggleSearchEngineCompactMode = callbacks.onToggleSearchEngineCompactMode,
                            selectedIconPackPackage = state.selectedIconPackPackage,
                            availableIconPacks = state.availableIconPacks,
                            onSelectIconPack = callbacks.onSelectIconPack,
                            onRefreshIconPacks = {
                                callbacks.onRefreshIconPacks()
                                Toast.makeText(
                                    appearanceContext,
                                    appearanceContext.getString(R.string.settings_refreshing_icon_packs),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onSearchIconPacks = callbacks.onSearchIconPacks,
                            hasFilePermission = PermissionUtils.hasFileAccessPermission(LocalContext.current),
                            hasWallpaperPermission = state.hasWallpaperPermission,
                            wallpaperAvailable = state.wallpaperAvailable
                        )
                    }
                    SettingsDetailType.CALLS_TEXTS -> {
                        CallsTextsSettingsSection(
                            messagingApp = state.messagingApp,
                            onSetMessagingApp = callbacks.onSetMessagingApp,
                            directDialEnabled = state.directDialEnabled,
                            onToggleDirectDial = callbacks.onToggleDirectDial,
                            hasCallPermission = PermissionRequestHandler.checkCallPermission(LocalContext.current),
                            hasContactPermission = PermissionUtils.hasContactsPermission(LocalContext.current),
                            onNavigateToPermissions = { onNavigateToDetail(SettingsDetailType.PERMISSIONS) },
                            contactsSectionEnabled = true, // Always show calls/texts settings regardless of permissions
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
                            onAddHomeScreenWidget = callbacks.onAddHomeScreenWidget,
                            onAddQuickSettingsTile = callbacks.onAddQuickSettingsTile,
                            modifier = Modifier
                        )
                    }
                    SettingsDetailType.PERMISSIONS -> {
                        PermissionsSettings(
                            onRequestUsagePermission = callbacks.onRequestUsagePermission,
                            onRequestContactPermission = callbacks.onRequestContactPermission,
                            onRequestFilePermission = callbacks.onRequestFilePermission,
                            onRequestCallPermission = callbacks.onRequestCallPermission,
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
                horizontal = DesignTokens.ContentHorizontalPadding,
                vertical = DesignTokens.HeaderVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.desc_navigate_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(DesignTokens.HeaderIconSpacing))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailingContent?.let {
            Spacer(modifier = Modifier.width(DesignTokens.HeaderIconSpacing))
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
