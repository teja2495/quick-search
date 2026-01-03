package com.tk.quicksearch.search.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.core.*

/**
 * Data class for Files section parameters
 */
data class FilesSectionParams(
    val files: List<DeviceFile>,
    val hasPermission: Boolean,
    val isExpanded: Boolean,
    val pinnedFileUris: Set<String>,
    val onFileClick: (DeviceFile) -> Unit,
    val onRequestPermission: () -> Unit,
    val onTogglePin: (DeviceFile) -> Unit,
    val onExclude: (DeviceFile) -> Unit,
    val onExcludeExtension: (DeviceFile) -> Unit,
    val onNicknameClick: (DeviceFile) -> Unit,
    val getFileNickname: (String) -> String?,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val permissionDisabledCard: (@Composable (title: String, message: String, actionLabel: String, onActionClick: () -> Unit) -> Unit),
    val showWallpaperBackground: Boolean
)

/**
 * Data class for Settings section parameters
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
    val showWallpaperBackground: Boolean
)

/**
 * Data class for Contacts section parameters
 */
data class ContactsSectionParams(
    val contacts: List<ContactInfo>,
    val hasPermission: Boolean,
    val isExpanded: Boolean,
    val messagingApp: MessagingApp?,
    val pinnedContactIds: Set<Long>,
    val onContactClick: (ContactInfo) -> Unit,
    val onShowContactMethods: (ContactInfo) -> Unit,
    val onCallContact: (ContactInfo) -> Unit,
    val onSmsContact: (ContactInfo) -> Unit,
    val onContactMethodClick: (ContactInfo, com.tk.quicksearch.model.ContactMethod) -> Unit,
    val onTogglePin: (ContactInfo) -> Unit,
    val onExclude: (ContactInfo) -> Unit,
    val onNicknameClick: (ContactInfo) -> Unit,
    val getContactNickname: (Long) -> String?,
    val onOpenAppSettings: () -> Unit,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val permissionDisabledCard: (@Composable (title: String, message: String, actionLabel: String, onActionClick: () -> Unit) -> Unit),
    val showWallpaperBackground: Boolean
)

/**
 * Data class for Apps section parameters
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
    val rowCount: Int,
    val iconPackPackage: String?,
    val keyboardAlignedLayout: Boolean
)

/**
 * Helper function to build all the section parameters needed by SearchScreenContent
 */
@Composable
internal fun buildSectionParams(
    state: SearchUiState,
    derivedState: DerivedState,
    onFileClick: (DeviceFile) -> Unit,
    onPinFile: (DeviceFile) -> Unit,
    onUnpinFile: (DeviceFile) -> Unit,
    onExcludeFile: (DeviceFile) -> Unit,
    onExcludeFileExtension: (DeviceFile) -> Unit,
    onOpenStorageAccessSettings: () -> Unit,
    onSettingClick: (SettingShortcut) -> Unit,
    onPinSetting: (SettingShortcut) -> Unit,
    onUnpinSetting: (SettingShortcut) -> Unit,
    onExcludeSetting: (SettingShortcut) -> Unit,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, com.tk.quicksearch.model.ContactMethod) -> Unit,
    onPinContact: (ContactInfo) -> Unit,
    onUnpinContact: (ContactInfo) -> Unit,
    onExcludeContact: (ContactInfo) -> Unit,
    onOpenAppSettings: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppInfoClick: (AppInfo) -> Unit,
    onUninstallClick: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (AppInfo) -> Unit,
    getFileNickname: (String) -> String?,
    getContactNickname: (Long) -> String?,
    getSettingNickname: (String) -> String?,
    getAppNickname: (String) -> String?,
    onUpdateNicknameDialogState: (NicknameDialogState?) -> Unit,
    onUpdateExpandedSection: (ExpandedSection) -> Unit,
    expandedSection: ExpandedSection
) = remember(
    state, derivedState, expandedSection,
    onFileClick, onPinFile, onUnpinFile, onExcludeFile, onExcludeFileExtension, onOpenStorageAccessSettings,
    onSettingClick, onPinSetting, onUnpinSetting, onExcludeSetting,
    onContactClick, onShowContactMethods, onCallContact, onSmsContact, onContactMethodClick,
    onPinContact, onUnpinContact, onExcludeContact, onOpenAppSettings,
    onAppClick, onAppInfoClick, onUninstallClick, onHideApp, onPinApp, onUnpinApp,
    getFileNickname, getContactNickname, getSettingNickname, getAppNickname,
    onUpdateNicknameDialogState, onUpdateExpandedSection
) {
    val filesParams = FilesSectionParams(
        files = state.fileResults,
        hasPermission = state.hasFilePermission,
        isExpanded = expandedSection == ExpandedSection.FILES,
        pinnedFileUris = derivedState.pinnedFileUris,
        onFileClick = onFileClick,
        onRequestPermission = onOpenStorageAccessSettings,
        onTogglePin = { file ->
            if (derivedState.pinnedFileUris.contains(file.uri.toString())) {
                onUnpinFile(file)
            } else {
                onPinFile(file)
            }
        },
        onExclude = onExcludeFile,
        onExcludeExtension = onExcludeFileExtension,
        onNicknameClick = { file ->
            onUpdateNicknameDialogState(
                NicknameDialogState.File(
                    file = file,
                    currentNickname = getFileNickname(file.uri.toString()),
                    itemName = file.displayName
                )
            )
        },
        getFileNickname = getFileNickname,
        showAllResults = derivedState.autoExpandFiles,
        showExpandControls = derivedState.hasMultipleExpandableSections,
        onExpandClick = {
            onUpdateExpandedSection(
                if (expandedSection == ExpandedSection.FILES) ExpandedSection.NONE else ExpandedSection.FILES
            )
        },
        permissionDisabledCard = { title, message, actionLabel, onActionClick ->
            PermissionDisabledCard(
                title = title,
                message = message,
                actionLabel = actionLabel,
                onActionClick = onActionClick
            )
        },
        showWallpaperBackground = state.showWallpaperBackground
    )

    val settingsParams = SettingsSectionParams(
        settings = state.settingResults,
        isExpanded = expandedSection == ExpandedSection.SETTINGS,
        pinnedSettingIds = derivedState.pinnedSettingIds,
        onSettingClick = onSettingClick,
        onTogglePin = { setting ->
            if (derivedState.pinnedSettingIds.contains(setting.id)) {
                onUnpinSetting(setting)
            } else {
                onPinSetting(setting)
            }
        },
        onExclude = onExcludeSetting,
        onNicknameClick = { setting ->
            onUpdateNicknameDialogState(
                NicknameDialogState.Setting(
                    setting = setting,
                    currentNickname = getSettingNickname(setting.id),
                    itemName = setting.title
                )
            )
        },
        getSettingNickname = getSettingNickname,
        showAllResults = derivedState.autoExpandSettings,
        showExpandControls = derivedState.hasMultipleExpandableSections,
        onExpandClick = {
            onUpdateExpandedSection(
                if (expandedSection == ExpandedSection.SETTINGS) ExpandedSection.NONE else ExpandedSection.SETTINGS
            )
        },
        showWallpaperBackground = state.showWallpaperBackground
    )

    val contactsParams = ContactsSectionParams(
        contacts = state.contactResults,
        hasPermission = state.hasContactPermission,
        isExpanded = expandedSection == ExpandedSection.CONTACTS,
        messagingApp = state.messagingApp,
        pinnedContactIds = derivedState.pinnedContactIds,
        onContactClick = onContactClick,
        onShowContactMethods = onShowContactMethods,
        onCallContact = onCallContact,
        onSmsContact = onSmsContact,
        onContactMethodClick = onContactMethodClick,
        onTogglePin = { contact ->
            if (derivedState.pinnedContactIds.contains(contact.contactId)) {
                onUnpinContact(contact)
            } else {
                onPinContact(contact)
            }
        },
        onExclude = onExcludeContact,
        onNicknameClick = { contact ->
            onUpdateNicknameDialogState(
                NicknameDialogState.Contact(
                    contact = contact,
                    currentNickname = getContactNickname(contact.contactId),
                    itemName = contact.displayName
                )
            )
        },
        getContactNickname = getContactNickname,
        onOpenAppSettings = onOpenAppSettings,
        showAllResults = derivedState.autoExpandContacts,
        showExpandControls = derivedState.hasMultipleExpandableSections,
        onExpandClick = {
            onUpdateExpandedSection(
                if (expandedSection == ExpandedSection.CONTACTS) ExpandedSection.NONE else ExpandedSection.CONTACTS
            )
        },
        permissionDisabledCard = { title, message, actionLabel, onActionClick ->
            PermissionDisabledCard(
                title = title,
                message = message,
                actionLabel = actionLabel,
                onActionClick = onActionClick
            )
        },
        showWallpaperBackground = state.showWallpaperBackground
    )

    val appsParams = AppsSectionParams(
        apps = derivedState.displayApps,
        isSearching = derivedState.isSearching,
        hasAppResults = derivedState.hasAppResults,
        pinnedPackageNames = derivedState.pinnedPackageNames,
        onAppClick = onAppClick,
        onAppInfoClick = onAppInfoClick,
        onUninstallClick = onUninstallClick,
        onHideApp = onHideApp,
        onPinApp = onPinApp,
        onUnpinApp = onUnpinApp,
        onNicknameClick = { app ->
            onUpdateNicknameDialogState(
                NicknameDialogState.App(
                    app = app,
                    currentNickname = getAppNickname(app.packageName),
                    itemName = app.appName
                )
            )
        },
        getAppNickname = getAppNickname,
        showAppLabels = true,
        rowCount = derivedState.visibleRowCount,
        iconPackPackage = state.selectedIconPackPackage,
        keyboardAlignedLayout = state.keyboardAlignedLayout
    )

    SectionParams(
        filesParams = filesParams,
        settingsParams = settingsParams,
        contactsParams = contactsParams,
        appsParams = appsParams
    )
}

/**
 * Data class to hold all section parameters
 */
data class SectionParams(
    val filesParams: FilesSectionParams,
    val settingsParams: SettingsSectionParams,
    val contactsParams: ContactsSectionParams,
    val appsParams: AppsSectionParams
)