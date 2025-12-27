package com.tk.quicksearch.settings

import android.Manifest
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.permissions.PermissionRequestHandler
import com.tk.quicksearch.search.IconPackInfo
import com.tk.quicksearch.search.MessagingApp
import com.tk.quicksearch.search.SearchEngine
import com.tk.quicksearch.search.SearchSection
import com.tk.quicksearch.search.SearchViewModel

/**
 * Data class to hold all settings screen state and callbacks.
 * Reduces parameter count and improves maintainability.
 */
data class SettingsScreenState(
    val suggestionExcludedApps: List<AppInfo>,
    val resultExcludedApps: List<AppInfo>,
    val excludedContacts: List<ContactInfo>,
    val excludedFiles: List<DeviceFile>,
    val excludedSettings: List<SettingShortcut>,
    val searchEngineOrder: List<SearchEngine>,
    val disabledSearchEngines: Set<SearchEngine>,
    val enabledFileTypes: Set<FileType>,
    val keyboardAlignedLayout: Boolean,
    val shortcutCodes: Map<SearchEngine, String>,
    val shortcutEnabled: Map<SearchEngine, Boolean>,
    val messagingApp: MessagingApp,
    val isWhatsAppInstalled: Boolean,
    val isTelegramInstalled: Boolean,
    val showWallpaperBackground: Boolean,
    val selectedIconPackPackage: String? = null,
    val availableIconPacks: List<IconPackInfo> = emptyList(),
    val clearQueryAfterSearchEngine: Boolean,
    val showAllResults: Boolean,
    val sortAppsByUsageEnabled: Boolean,
    val directDialEnabled: Boolean,
    val sectionOrder: List<SearchSection>,
    val disabledSections: Set<SearchSection>,
    val searchEngineSectionEnabled: Boolean,
    val amazonDomain: String? = null,
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
    val onRemoveExcludedSetting: (SettingShortcut) -> Unit,
    val onClearAllExclusions: () -> Unit,
    val onToggleSearchEngine: (SearchEngine, Boolean) -> Unit,
    val onReorderSearchEngines: (List<SearchEngine>) -> Unit,
    val onToggleFileType: (FileType, Boolean) -> Unit,
    val onToggleKeyboardAlignedLayout: (Boolean) -> Unit,
    val setShortcutCode: (SearchEngine, String) -> Unit,
    val setShortcutEnabled: (SearchEngine, Boolean) -> Unit,
    val onSetMessagingApp: (MessagingApp) -> Unit,
    val onToggleShowWallpaperBackground: (Boolean) -> Unit,
    val onSelectIconPack: (String?) -> Unit,
    val onSearchIconPacks: () -> Unit,
    val onToggleClearQueryAfterSearchEngine: (Boolean) -> Unit,
    val onToggleShowAllResults: (Boolean) -> Unit,
    val onToggleSortAppsByUsage: (Boolean) -> Unit,
    val onToggleDirectDial: (Boolean) -> Unit,
    val onToggleSection: (SearchSection, Boolean) -> Unit,
    val onReorderSections: (List<SearchSection>) -> Unit,
    val onToggleSearchEngineSectionEnabled: (Boolean) -> Unit,
    val onSetAmazonDomain: (String?) -> Unit,
    val onSetGeminiApiKey: (String?) -> Unit,
    val onSetPersonalContext: (String?) -> Unit,
    val onSetDefaultAssistant: () -> Unit
)

/**
 * Constants for consistent spacing throughout the settings screen.
 */
object SettingsSpacing {
    val headerHorizontalPadding = 20.dp
    val headerVerticalPadding = 16.dp
    val headerIconSpacing = 8.dp
    val contentHorizontalPadding = 20.dp
    val sectionTopPadding = 24.dp
    val sectionTitleBottomPadding = 8.dp
    val sectionDescriptionBottomPadding = 16.dp
    val versionTopPadding = 45.dp
    val versionBottomPadding = 100.dp
    val singleCardPadding = PaddingValues(
        start = 24.dp,
        top = 20.dp,
        end = 24.dp,
        bottom = 24.dp
    )
}

/**
 * Retrieves the app version name from the package manager.
 */
@Composable
fun getAppVersionName(): String? {
    val context = LocalContext.current
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }
}

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
