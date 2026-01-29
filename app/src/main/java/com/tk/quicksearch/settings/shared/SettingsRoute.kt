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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shortcut
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.tile.requestAddQuickSearchTile
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticToggle
import com.tk.quicksearch.widget.requestAddQuickSearchWidget

/**
 * Data class to hold all settings screen state and callbacks. Reduces parameter count and improves
 * maintainability.
 */
data class SettingsScreenState(
        val suggestionExcludedApps: List<com.tk.quicksearch.search.models.AppInfo>,
        val resultExcludedApps: List<com.tk.quicksearch.search.models.AppInfo>,
        val excludedContacts: List<com.tk.quicksearch.search.models.ContactInfo>,
        val excludedFiles: List<com.tk.quicksearch.search.models.DeviceFile>,
        val excludedSettings: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting>,
        val excludedAppShortcuts: List<com.tk.quicksearch.search.data.StaticShortcut>,
        val searchEngineOrder: List<SearchTarget>,
        val disabledSearchEngines: Set<String>,
        val enabledFileTypes: Set<com.tk.quicksearch.search.models.FileType>,
        val showFolders: Boolean,
        val showSystemFiles: Boolean,
        val showHiddenFiles: Boolean,
        val excludedFileExtensions: Set<String>,
        val oneHandedMode: Boolean,
        val overlayModeEnabled: Boolean,
        val shortcutCodes: Map<String, String>,
        val shortcutEnabled: Map<String, Boolean>,
        val messagingApp: MessagingApp,
        val isWhatsAppInstalled: Boolean,
        val isTelegramInstalled: Boolean,
        val hasWallpaperPermission: Boolean,
        val wallpaperAvailable: Boolean,
        val showWallpaperBackground: Boolean,
        val wallpaperBackgroundAlpha: Float,
        val wallpaperBlurRadius: Float,
        val selectedIconPackPackage: String? = null,
        val availableIconPacks: List<IconPackInfo> = emptyList(),
        val directDialEnabled: Boolean,
        val disabledSections: Set<SearchSection>,
        val isSearchEngineCompactMode: Boolean,
        val amazonDomain: String? = null,
        val calculatorEnabled: Boolean,
        val webSuggestionsEnabled: Boolean,
        val webSuggestionsCount: Int,
        val recentQueriesEnabled: Boolean,
        val recentQueriesCount: Int,
        val hasGeminiApiKey: Boolean = false,
        val geminiApiKeyLast4: String? = null,
        val personalContext: String = ""
)

/** Data class to hold all settings screen callbacks. */
data class SettingsScreenCallbacks(
        val onBack: () -> Unit,
        val onRemoveSuggestionExcludedApp: (com.tk.quicksearch.search.models.AppInfo) -> Unit,
        val onRemoveResultExcludedApp: (com.tk.quicksearch.search.models.AppInfo) -> Unit,
        val onRemoveExcludedContact: (com.tk.quicksearch.search.models.ContactInfo) -> Unit,
        val onRemoveExcludedFile: (com.tk.quicksearch.search.models.DeviceFile) -> Unit,
        val onRemoveExcludedSetting:
                (com.tk.quicksearch.search.deviceSettings.DeviceSetting) -> Unit,
        val onRemoveExcludedAppShortcut: (com.tk.quicksearch.search.data.StaticShortcut) -> Unit,
        val onClearAllExclusions: () -> Unit,
        val onToggleSearchEngine: (SearchTarget, Boolean) -> Unit,
        val onReorderSearchEngines: (List<SearchTarget>) -> Unit,
        val onToggleFileType: (com.tk.quicksearch.search.models.FileType, Boolean) -> Unit,
        val onToggleFolders: (Boolean) -> Unit,
        val onToggleSystemFiles: (Boolean) -> Unit,
        val onToggleHiddenFiles: (Boolean) -> Unit,
        val onRemoveExcludedFileExtension: (String) -> Unit,
        val onToggleOneHandedMode: (Boolean) -> Unit,
        val onToggleOverlayMode: (Boolean) -> Unit,
        val setShortcutCode: (SearchTarget, String) -> Unit,
        val setShortcutEnabled: (SearchTarget, Boolean) -> Unit,
        val onSetMessagingApp: (MessagingApp) -> Unit,
        val onToggleShowWallpaperBackground: (Boolean) -> Unit,
        val onWallpaperBackgroundAlphaChange: (Float) -> Unit,
        val onWallpaperBlurRadiusChange: (Float) -> Unit,
        val onSelectIconPack: (String?) -> Unit,
        val onSearchIconPacks: () -> Unit,
        val onRefreshIconPacks: () -> Unit,
        val onToggleDirectDial: (Boolean) -> Unit,
        val onToggleSection: (SearchSection, Boolean) -> Unit,
        val onToggleSearchEngineCompactMode: (Boolean) -> Unit,
        val onSetAmazonDomain: (String?) -> Unit,
        val onToggleCalculator: (Boolean) -> Unit,
        val onToggleWebSuggestions: (Boolean) -> Unit,
        val onWebSuggestionsCountChange: (Int) -> Unit,
        val onToggleRecentQueries: (Boolean) -> Unit,
        val onRecentQueriesCountChange: (Int) -> Unit,
        val onSetGeminiApiKey: (String?) -> Unit,
        val onSetPersonalContext: (String?) -> Unit,
        val onAddHomeScreenWidget: () -> Unit,
        val onAddQuickSettingsTile: () -> Unit,
        val onSetDefaultAssistant: () -> Unit,
        val onRefreshApps: (Boolean) -> Unit,
        val onRefreshContacts: (Boolean) -> Unit,
        val onRefreshFiles: (Boolean) -> Unit,
        val onRequestUsagePermission: () -> Unit,
        val onRequestContactPermission: () -> Unit,
        val onRequestFilePermission: () -> Unit,
        val onRequestCallPermission: () -> Unit,
        val onRequestWallpaperPermission: () -> Unit
)

/** Constants for drag and drop behavior and animations. */
private object DragConstants {
    val rowHorizontalPadding: Dp = DesignTokens.CardHorizontalPadding
    val rowVerticalPadding: Dp = DesignTokens.CardVerticalPadding
    val iconSize: Dp = DesignTokens.IconSize
    val rowSpacing: Dp = DesignTokens.ItemRowSpacing
}

/** Data class holding section display metadata. */
private data class SectionMetadata(val name: String, val icon: ImageVector)

/** Gets the display metadata for a given search section. */
@Composable
private fun getSectionMetadata(section: SearchSection): SectionMetadata {
    return when (section) {
        SearchSection.APPS ->
                SectionMetadata(
                        name = stringResource(R.string.section_apps),
                        icon = Icons.Rounded.Apps
                )
        SearchSection.APP_SHORTCUTS ->
                SectionMetadata(
                        name = stringResource(R.string.section_app_shortcuts),
                        icon = Icons.Rounded.Shortcut
                )
        SearchSection.CONTACTS ->
                SectionMetadata(
                        name = stringResource(R.string.section_contacts),
                        icon = Icons.Rounded.Contacts
                )
        SearchSection.FILES ->
                SectionMetadata(
                        name = stringResource(R.string.section_files),
                        icon = Icons.Rounded.InsertDriveFile
                )
        SearchSection.SETTINGS ->
                SectionMetadata(
                        name = stringResource(R.string.section_settings),
                        icon = Icons.Rounded.Settings
                )
    }
}

@Composable
fun SectionSettingsSection(
        sectionOrder: List<SearchSection>,
        disabledSections: Set<SearchSection>,
        onToggleSection: (SearchSection, Boolean) -> Unit,
        modifier: Modifier = Modifier,
        showTitle: Boolean = true
) {
    if (showTitle) {
        Text(
                text = stringResource(R.string.settings_sections_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding)
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.ExtraLargeCardShape) {
        Column(modifier = Modifier.fillMaxWidth()) {
            sectionOrder.forEachIndexed { index, section ->
                SectionRowWithoutDrag(
                        section = section,
                        isEnabled = section !in disabledSections,
                        onToggle = { enabled -> onToggleSection(section, enabled) }
                )

                if (index != sectionOrder.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionRowWithoutDrag(
        section: SearchSection,
        isEnabled: Boolean,
        onToggle: (Boolean) -> Unit,
        bottomPadding: Dp = DragConstants.rowVerticalPadding
) {
    val view = LocalView.current
    val metadata = getSectionMetadata(section)

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(
                                    start = DragConstants.rowHorizontalPadding,
                                    end = DragConstants.rowHorizontalPadding,
                                    top = DragConstants.rowVerticalPadding,
                                    bottom = bottomPadding
                            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DragConstants.rowSpacing)
    ) {
        Icon(
                imageVector = metadata.icon,
                contentDescription = metadata.name,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DragConstants.iconSize)
        )

        Text(
                text = metadata.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
        )

        androidx.compose.material3.Switch(
                checked = isEnabled,
                onCheckedChange = { enabled ->
                    hapticToggle(view)()
                    onToggle(enabled)
                }
        )
    }
}

@Composable
private fun SectionRow(
        section: SearchSection,
        isEnabled: Boolean,
        onToggle: (Boolean) -> Unit,
        dragHandleModifier: Modifier,
        bottomPadding: Dp = DragConstants.rowVerticalPadding
) {
    val view = LocalView.current
    val metadata = getSectionMetadata(section)

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(
                                    start = DragConstants.rowHorizontalPadding,
                                    end = DragConstants.rowHorizontalPadding,
                                    top = DragConstants.rowVerticalPadding,
                                    bottom = bottomPadding
                            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DragConstants.rowSpacing)
    ) {
        Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = stringResource(R.string.settings_action_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DragConstants.iconSize).then(dragHandleModifier)
        )

        Icon(
                imageVector = metadata.icon,
                contentDescription = metadata.name,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DragConstants.iconSize)
        )

        Text(
                text = metadata.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
        )

        androidx.compose.material3.Switch(
                checked = isEnabled,
                onCheckedChange = { enabled ->
                    hapticToggle(view)()
                    onToggle(enabled)
                }
        )
    }
}

/**
 * Handles permission requests and section toggling for the settings screen. Returns a callback
 * function for toggling sections that handles permission requests.
 */
@Composable
fun rememberSectionToggleHandler(
        viewModel: SearchViewModel,
        disabledSections: Set<SearchSection>
): (SearchSection, Boolean) -> Unit {
    val context = LocalContext.current
    val pendingSectionEnable = remember { mutableStateOf<SearchSection?>(null) }

    // Launcher for runtime permissions (contacts, files on pre-R)
    val runtimePermissionsLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
                val filesGranted =
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                        } else {
                            false
                        }

                // Refresh permission state
                viewModel.handleOptionalPermissionChange()

                // If permission was granted and we have a pending section, enable it
                pendingSectionEnable.value?.let { section ->
                    when (section) {
                        SearchSection.CONTACTS -> {
                            if (contactsGranted) {
                                viewModel.setSectionEnabled(section, true)
                            }
                        }
                        SearchSection.FILES -> {
                            if (filesGranted ||
                                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                                                    Environment.isExternalStorageManager())
                            ) {
                                viewModel.setSectionEnabled(section, true)
                            }
                        }
                        else -> {}
                    }
                    pendingSectionEnable.value = null
                }
            }

    // Launcher for all files access (Android R+)
    val allFilesAccessLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
            ) {
                val filesGranted = PermissionRequestHandler.checkFilesPermission(context)

                // Refresh permission state
                viewModel.handleOptionalPermissionChange()

                // If permission was granted and we have a pending section, enable it
                if (pendingSectionEnable.value == SearchSection.FILES && filesGranted) {
                    viewModel.setSectionEnabled(SearchSection.FILES, true)
                    pendingSectionEnable.value = null
                }
            }

    return androidx.compose.runtime.remember(viewModel, disabledSections) {
        { section: SearchSection, enabled: Boolean ->
            if (enabled) {
                // Check if section can be enabled (has permissions)
                if (viewModel.canEnableSection(section)) {
                    viewModel.setSectionEnabled(section, true)
                } else {
                    // Request permissions based on section
                    when (section) {
                        SearchSection.CONTACTS -> {
                            pendingSectionEnable.value = section
                            runtimePermissionsLauncher.launch(
                                    arrayOf(Manifest.permission.READ_CONTACTS)
                            )
                        }
                        SearchSection.FILES -> {
                            pendingSectionEnable.value = section
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                PermissionRequestHandler.launchAllFilesAccessRequest(
                                        allFilesAccessLauncher,
                                        context
                                )
                            } else {
                                runtimePermissionsLauncher.launch(
                                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                )
                            }
                        }
                        SearchSection.APPS -> {
                            // Apps section doesn't require permissions
                            viewModel.setSectionEnabled(section, true)
                        }
                        SearchSection.APP_SHORTCUTS -> {
                            viewModel.setSectionEnabled(section, true)
                        }
                        SearchSection.SETTINGS -> {
                            viewModel.setSectionEnabled(section, true)
                        }
                    }
                }
            } else {
                // Check if disabling this section would leave no sections enabled
                val enabledSectionsCount = SearchSection.values().count { it !in disabledSections }
                if (enabledSectionsCount <= 1 && section !in disabledSections) {
                    // This is the last enabled section, prevent disabling
                    Toast.makeText(
                                    context,
                                    context.getString(
                                            R.string.settings_sections_at_least_one_required
                                    ),
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                } else {
                    viewModel.setSectionEnabled(section, false)
                }
            }
        }
    }
}

/** Creates a standardized permission request handler that tries popup first, then settings. */
@Composable
fun createPermissionRequestHandler(
        context: Context,
        permissionLauncher: ActivityResultLauncher<String>,
        permission: String,
        fallbackAction: () -> Unit
): () -> Unit =
        androidx.compose.runtime.remember(context, permissionLauncher, permission, fallbackAction) {
            {
                if (context !is Activity) {
                    fallbackAction()
                } else {
                    permissionLauncher.launch(permission)
                }
            }
        }

/** Handles the result of a permission request with standardized logic. */
fun handlePermissionResult(
        isGranted: Boolean,
        context: Context,
        permission: String,
        onPermanentlyDenied: () -> Unit,
        onPermissionChanged: () -> Unit,
        onGranted: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null
) {
    onPermissionChanged()

    if (isGranted) {
        onGranted?.invoke()
    } else if (context is Activity) {
        val shouldShowRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
        if (!shouldShowRationale) {
            // Permission permanently denied, open settings
            onPermanentlyDenied()
        }
    }

    onComplete?.invoke()
}

@Composable
fun SettingsRoute(
        modifier: Modifier = Modifier,
        onBack: () -> Unit,
        viewModel: SearchViewModel,
        onNavigateToDetail:
                (com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType) -> Unit =
                {},
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
                    oneHandedMode = uiState.oneHandedMode,
                    overlayModeEnabled = uiState.overlayModeEnabled,
                    shortcutCodes = uiState.shortcutCodes,
                    shortcutEnabled = uiState.shortcutEnabled,
                    messagingApp = uiState.messagingApp,
                    isWhatsAppInstalled = uiState.isWhatsAppInstalled,
                    isTelegramInstalled = uiState.isTelegramInstalled,
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

    val wallpaperPermissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                handlePermissionResult(
                        isGranted = isGranted,
                        context = context,
                        permission = Manifest.permission.READ_MEDIA_IMAGES,
                        onPermanentlyDenied = viewModel::openAppSettings,
                        onPermissionChanged = viewModel::handleOptionalPermissionChange
                )
            }

    val onRequestWallpaperPermission =
            createPermissionRequestHandler(
                    context = context,
                    permissionLauncher = wallpaperPermissionLauncher,
                    permission = Manifest.permission.READ_MEDIA_IMAGES,
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

    val onRequestAddHomeScreenWidget = { requestAddQuickSearchWidget(context) }
    val onRequestAddQuickSettingsTile = { requestAddQuickSearchTile(context) }

    val onToggleOverlayMode: (Boolean) -> Unit = { enabled ->
        viewModel.setOverlayModeEnabled(enabled)
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
                    onToggleFileType = viewModel::setFileTypeEnabled,
                    onToggleFolders = viewModel::setShowFolders,
                    onToggleSystemFiles = viewModel::setShowSystemFiles,
                    onToggleHiddenFiles = viewModel::setShowHiddenFiles,
                    onRemoveExcludedFileExtension = viewModel::removeExcludedFileExtension,
                    onToggleOneHandedMode = viewModel::setOneHandedMode,
                    onToggleOverlayMode = onToggleOverlayMode,
                    setShortcutCode = viewModel::setShortcutCode,
                    setShortcutEnabled = viewModel::setShortcutEnabled,
                    onSetMessagingApp = viewModel::setMessagingApp,
                    onToggleShowWallpaperBackground = viewModel::setShowWallpaperBackground,
                    onWallpaperBackgroundAlphaChange = viewModel::setWallpaperBackgroundAlpha,
                    onWallpaperBlurRadiusChange = viewModel::setWallpaperBlurRadius,
                    onSelectIconPack = viewModel::setIconPackPackage,
                    onSearchIconPacks = viewModel::searchIconPacks,
                    onRefreshIconPacks = viewModel::refreshIconPacks,
                    onToggleDirectDial = onToggleDirectDial,
                    onToggleSection = onToggleSection,
                    onToggleSearchEngineCompactMode = viewModel::setSearchEngineCompactMode,
                    onSetAmazonDomain = viewModel::setAmazonDomain,
                    onToggleCalculator = viewModel::setCalculatorEnabled,
                    onToggleWebSuggestions = viewModel::setWebSuggestionsEnabled,
                    onWebSuggestionsCountChange = viewModel::setWebSuggestionsCount,
                    onToggleRecentQueries = viewModel::setRecentQueriesEnabled,
                    onRecentQueriesCountChange = viewModel::setRecentQueriesCount,
                    onSetGeminiApiKey = viewModel::setGeminiApiKey,
                    onSetPersonalContext = viewModel::setPersonalContext,
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
                    onRequestCallPermission = onRequestCallPermission,
                    onRequestWallpaperPermission = onRequestWallpaperPermission
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
