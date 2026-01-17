package com.tk.quicksearch.settings.settingsScreen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler
import com.tk.quicksearch.search.core.*

/**
 * Data class to hold all settings screen state and callbacks.
 * Reduces parameter count and improves maintainability.
 */
data class SettingsScreenState(
    val suggestionExcludedApps: List<AppInfo>,
    val resultExcludedApps: List<AppInfo>,
    val excludedContacts: List<ContactInfo>,
    val excludedFiles: List<DeviceFile>,
    val excludedSettings: List<DeviceSetting>,
    val excludedAppShortcuts: List<StaticShortcut>,
    val searchEngineOrder: List<SearchEngine>,
    val disabledSearchEngines: Set<SearchEngine>,
    val enabledFileTypes: Set<FileType>,
    val excludedFileExtensions: Set<String>,
    val keyboardAlignedLayout: Boolean,
    val shortcutCodes: Map<SearchEngine, String>,
    val shortcutEnabled: Map<SearchEngine, Boolean>,
    val messagingApp: MessagingApp,
    val isWhatsAppInstalled: Boolean,
    val isTelegramInstalled: Boolean,
    val showWallpaperBackground: Boolean,
    val wallpaperBackgroundAlpha: Float,
    val wallpaperBlurRadius: Float,
    val selectedIconPackPackage: String? = null,
    val availableIconPacks: List<com.tk.quicksearch.search.core.IconPackInfo> = emptyList(),
    val clearQueryAfterSearchEngine: Boolean,
    val showAllResults: Boolean,
    val sortAppsByUsageEnabled: Boolean,
    val fuzzyAppSearchEnabled: Boolean,
    val directDialEnabled: Boolean,
    val sectionOrder: List<SearchSection>,
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

/**
 * Data class to hold all settings screen callbacks.
 */
data class SettingsScreenCallbacks(
    val onBack: () -> Unit,
    val onRemoveSuggestionExcludedApp: (AppInfo) -> Unit,
    val onRemoveResultExcludedApp: (AppInfo) -> Unit,
    val onRemoveExcludedContact: (ContactInfo) -> Unit,
    val onRemoveExcludedFile: (DeviceFile) -> Unit,
    val onRemoveExcludedSetting: (DeviceSetting) -> Unit,
    val onRemoveExcludedAppShortcut: (StaticShortcut) -> Unit,
    val onClearAllExclusions: () -> Unit,
    val onToggleSearchEngine: (SearchEngine, Boolean) -> Unit,
    val onReorderSearchEngines: (List<SearchEngine>) -> Unit,
    val onToggleFileType: (FileType, Boolean) -> Unit,
    val onRemoveExcludedFileExtension: (String) -> Unit,
    val onToggleKeyboardAlignedLayout: (Boolean) -> Unit,
    val setShortcutCode: (SearchEngine, String) -> Unit,
    val setShortcutEnabled: (SearchEngine, Boolean) -> Unit,
    val onSetMessagingApp: (MessagingApp) -> Unit,
    val onToggleShowWallpaperBackground: (Boolean) -> Unit,
    val onWallpaperBackgroundAlphaChange: (Float) -> Unit,
    val onWallpaperBlurRadiusChange: (Float) -> Unit,
    val onSelectIconPack: (String?) -> Unit,
    val onSearchIconPacks: () -> Unit,
    val onRefreshIconPacks: () -> Unit,
    val onToggleClearQueryAfterSearchEngine: (Boolean) -> Unit,
    val onToggleShowAllResults: (Boolean) -> Unit,
    val onToggleSortAppsByUsage: (Boolean) -> Unit,
    val onToggleFuzzyAppSearch: (Boolean) -> Unit,
    val onToggleDirectDial: (Boolean) -> Unit,
    val onToggleSection: (SearchSection, Boolean) -> Unit,
    val onReorderSections: (List<SearchSection>) -> Unit,
    val onToggleSearchEngineCompactMode: (Boolean) -> Unit,
    val onSetAmazonDomain: (String?) -> Unit,
    val onToggleCalculator: (Boolean) -> Unit,
    val onToggleWebSuggestions: (Boolean) -> Unit,
    val onWebSuggestionsCountChange: (Int) -> Unit,
    val onToggleRecentQueries: (Boolean) -> Unit,
    val onRecentQueriesCountChange: (Int) -> Unit,
    val onSetGeminiApiKey: (String?) -> Unit,
    val onSetPersonalContext: (String?) -> Unit,
    val onAddQuickSettingsTile: () -> Unit,
    val onSetDefaultAssistant: () -> Unit,
    val onRefreshApps: (Boolean) -> Unit,
    val onRefreshContacts: (Boolean) -> Unit,
    val onRefreshFiles: (Boolean) -> Unit
)


/**
 * Handles permission requests and section toggling for the settings screen.
 * Returns a callback function for toggling sections that handles permission requests.
 */
@Composable
fun rememberSectionToggleHandler(
    viewModel: SearchViewModel,
    disabledSections: Set<SearchSection>
): (SearchSection, Boolean) -> Unit {
    val context = LocalContext.current
    var pendingSectionEnable by remember { mutableStateOf<SearchSection?>(null) }
    
    // Launcher for runtime permissions (contacts, files on pre-R)
    val runtimePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        val filesGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        } else {
            false
        }
        
        // Refresh permission state
        viewModel.handleOptionalPermissionChange()
        
        // If permission was granted and we have a pending section, enable it
        pendingSectionEnable?.let { section ->
            when (section) {
                SearchSection.CONTACTS -> {
                    if (contactsGranted) {
                        viewModel.setSectionEnabled(section, true)
                    }
                }
                SearchSection.FILES -> {
                    if (filesGranted || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && 
                        Environment.isExternalStorageManager())) {
                        viewModel.setSectionEnabled(section, true)
                    }
                }
                else -> {}
            }
            pendingSectionEnable = null
        }
    }
    
    // Launcher for all files access (Android R+)
    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val filesGranted = PermissionRequestHandler.checkFilesPermission(context)
        
        // Refresh permission state
        viewModel.handleOptionalPermissionChange()
        
        // If permission was granted and we have a pending section, enable it
        if (pendingSectionEnable == SearchSection.FILES && filesGranted) {
            viewModel.setSectionEnabled(SearchSection.FILES, true)
            pendingSectionEnable = null
        }
    }
    
    return remember(viewModel, disabledSections) {
        { section: SearchSection, enabled: Boolean ->
            if (enabled) {
                // Check if section can be enabled (has permissions)
                if (viewModel.canEnableSection(section)) {
                    viewModel.setSectionEnabled(section, true)
                } else {
                    // Request permissions based on section
                    when (section) {
                        SearchSection.CONTACTS -> {
                            pendingSectionEnable = section
                            runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                        }
                        SearchSection.FILES -> {
                            pendingSectionEnable = section
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
                val enabledSectionsCount = SearchSection.values().count { 
                    it !in disabledSections 
                }
                if (enabledSectionsCount <= 1 && section !in disabledSections) {
                    // This is the last enabled section, prevent disabling
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_sections_at_least_one_required),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.setSectionEnabled(section, false)
                }
            }
        }
    }
}

/**
 * Creates a standardized permission request handler that tries popup first, then settings.
 */
@Composable
fun createPermissionRequestHandler(
    context: Context,
    permissionLauncher: ActivityResultLauncher<String>,
    permission: String,
    fallbackAction: () -> Unit
): () -> Unit = remember(context, permissionLauncher, permission, fallbackAction) {
    {
        if (context !is Activity) {
            fallbackAction()
        } else {
            permissionLauncher.launch(permission)
        }
    }
}

/**
 * Handles the result of a permission request with standardized logic.
 */
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
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
        if (!shouldShowRationale) {
            // Permission permanently denied, open settings
            onPermanentlyDenied()
        }
    }

    onComplete?.invoke()
}
