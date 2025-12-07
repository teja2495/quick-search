package com.tk.quicksearch.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut

// ============================================================================
// Common Parameter Interfaces
// ============================================================================

/**
 * Common parameters shared by sections that support permissions and expansion.
 */
interface ExpandableSectionParams {
    val hasPermission: Boolean
    val isExpanded: Boolean
    val showAllResults: Boolean
    val showExpandControls: Boolean
    val onExpandClick: () -> Unit
    val permissionDisabledCard: @Composable (String, String, String, () -> Unit) -> Unit
}

// ============================================================================
// Section Parameter Data Classes
// ============================================================================

/**
 * Parameters for rendering the contacts section.
 */
data class ContactsSectionParams(
    val contacts: List<ContactInfo>,
    override val hasPermission: Boolean,
    override val isExpanded: Boolean,
    val messagingApp: MessagingApp,
    val pinnedContactIds: Set<Long>,
    val onContactClick: (ContactInfo) -> Unit,
    val onCallContact: (ContactInfo) -> Unit,
    val onSmsContact: (ContactInfo) -> Unit,
    val onTogglePin: (ContactInfo) -> Unit,
    val onExclude: (ContactInfo) -> Unit,
    val onNicknameClick: (ContactInfo) -> Unit,
    val getContactNickname: (Long) -> String?,
    val onOpenAppSettings: () -> Unit,
    override val showAllResults: Boolean,
    override val showExpandControls: Boolean,
    override val onExpandClick: () -> Unit,
    override val permissionDisabledCard: @Composable (String, String, String, () -> Unit) -> Unit,
    val showWallpaperBackground: Boolean = false
) : ExpandableSectionParams

/**
 * Parameters for rendering the files section.
 */
data class FilesSectionParams(
    val files: List<DeviceFile>,
    override val hasPermission: Boolean,
    override val isExpanded: Boolean,
    val pinnedFileUris: Set<String>,
    val onFileClick: (DeviceFile) -> Unit,
    val onRequestPermission: () -> Unit,
    val onTogglePin: (DeviceFile) -> Unit,
    val onExclude: (DeviceFile) -> Unit,
    val onNicknameClick: (DeviceFile) -> Unit,
    val getFileNickname: (String) -> String?,
    override val showAllResults: Boolean,
    override val showExpandControls: Boolean,
    override val onExpandClick: () -> Unit,
    override val permissionDisabledCard: @Composable (String, String, String, () -> Unit) -> Unit,
    val showWallpaperBackground: Boolean = false
) : ExpandableSectionParams

/**
 * Parameters for rendering the settings section.
 */
data class SettingsSectionParams(
    val settings: List<SettingShortcut>,
    val isExpanded: Boolean,
    val pinnedSettingIds: Set<String>,
    val onSettingClick: (SettingShortcut) -> Unit,
    val onTogglePin: (SettingShortcut) -> Unit,
    val onExclude: (SettingShortcut) -> Unit,
    val onNicknameClick: (SettingShortcut) -> Unit,
    val getSettingNickname: (String) -> String?,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val showWallpaperBackground: Boolean = false
)

/**
 * Parameters for rendering the apps section.
 */
data class AppsSectionParams(
    val apps: List<AppInfo>,
    val isSearching: Boolean,
    val hasAppResults: Boolean,
    val pinnedPackageNames: Set<String>,
    val onAppClick: (AppInfo) -> Unit,
    val onAppInfoClick: (AppInfo) -> Unit,
    val onUninstallClick: (AppInfo) -> Unit,
    val onHideApp: (AppInfo) -> Unit,
    val onPinApp: (AppInfo) -> Unit,
    val onUnpinApp: (AppInfo) -> Unit,
    val onNicknameClick: (AppInfo) -> Unit,
    val getAppNickname: (String) -> String?,
    val showAppLabels: Boolean,
    val rowCount: Int
)

// ============================================================================
// Section Render Functions
// ============================================================================

/**
 * Renders the contacts section with the provided parameters.
 *
 * @param modifier Modifier to be applied to the section
 * @param params Parameters containing all data and callbacks for the contacts section
 */
@Composable
fun RenderContactsSection(
    modifier: Modifier = Modifier,
    params: ContactsSectionParams
) {
    ContactResultsSection(
        modifier = modifier,
        hasPermission = params.hasPermission,
        contacts = params.contacts,
        isExpanded = params.isExpanded,
        messagingApp = params.messagingApp,
        onContactClick = params.onContactClick,
        onCallContact = params.onCallContact,
        onSmsContact = params.onSmsContact,
        pinnedContactIds = params.pinnedContactIds,
        onTogglePin = params.onTogglePin,
        onExclude = params.onExclude,
        onNicknameClick = params.onNicknameClick,
        getContactNickname = params.getContactNickname,
        onOpenAppSettings = params.onOpenAppSettings,
        showAllResults = params.showAllResults,
        showExpandControls = params.showExpandControls,
        onExpandClick = params.onExpandClick,
        permissionDisabledCard = params.permissionDisabledCard,
        showWallpaperBackground = params.showWallpaperBackground
    )
}

/**
 * Renders the files section with the provided parameters.
 *
 * @param modifier Modifier to be applied to the section
 * @param params Parameters containing all data and callbacks for the files section
 */
@Composable
fun RenderFilesSection(
    modifier: Modifier = Modifier,
    params: FilesSectionParams
) {
    FileResultsSection(
        modifier = modifier,
        hasPermission = params.hasPermission,
        files = params.files,
        isExpanded = params.isExpanded,
        onFileClick = params.onFileClick,
        onRequestPermission = params.onRequestPermission,
        pinnedFileUris = params.pinnedFileUris,
        onTogglePin = params.onTogglePin,
        onExclude = params.onExclude,
        onNicknameClick = params.onNicknameClick,
        getFileNickname = params.getFileNickname,
        showAllResults = params.showAllResults,
        showExpandControls = params.showExpandControls,
        onExpandClick = params.onExpandClick,
        permissionDisabledCard = params.permissionDisabledCard,
        showWallpaperBackground = params.showWallpaperBackground
    )
}

/**
 * Renders the apps section with the provided parameters.
 *
 * @param modifier Modifier to be applied to the section
 * @param params Parameters containing all data and callbacks for the apps section
 */
@Composable
fun RenderAppsSection(
    modifier: Modifier = Modifier,
    params: AppsSectionParams
) {
    AppGridSection(
        apps = params.apps,
        isSearching = params.isSearching,
        hasAppResults = params.hasAppResults,
        onAppClick = params.onAppClick,
        onAppInfoClick = params.onAppInfoClick,
        onUninstallClick = params.onUninstallClick,
        onHideApp = params.onHideApp,
        onPinApp = params.onPinApp,
        onUnpinApp = params.onUnpinApp,
        onNicknameClick = params.onNicknameClick,
        getAppNickname = params.getAppNickname,
        pinnedPackageNames = params.pinnedPackageNames,
        showAppLabels = params.showAppLabels,
        modifier = modifier,
        rowCount = params.rowCount
    )
}
