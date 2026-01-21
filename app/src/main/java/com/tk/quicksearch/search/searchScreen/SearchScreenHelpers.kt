package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.searchScreen.dialogs.NicknameDialogState

/** Data class for Files section parameters */
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
        val permissionDisabledCard:
                (@Composable
                (
                        title: String,
                        message: String,
                        actionLabel: String,
                        onActionClick: () -> Unit) -> Unit),
        val showWallpaperBackground: Boolean
)

/** Data class for Settings section parameters */
data class SettingsSectionParams(
        val settings: List<DeviceSetting>,
        val isExpanded: Boolean,
        val pinnedSettingIds: Set<String>,
        val onSettingClick: (DeviceSetting) -> Unit,
        val onTogglePin: (DeviceSetting) -> Unit,
        val onExclude: (DeviceSetting) -> Unit,
        val onNicknameClick: (DeviceSetting) -> Unit,
        val getSettingNickname: (String) -> String?,
        val showAllResults: Boolean,
        val showExpandControls: Boolean,
        val onExpandClick: () -> Unit,
        val showWallpaperBackground: Boolean
)

/** Data class for App Shortcuts section parameters */
data class AppShortcutsSectionParams(
        val shortcuts: List<StaticShortcut>,
        val isExpanded: Boolean,
        val pinnedShortcutIds: Set<String>,
        val excludedShortcutIds: Set<String>,
        val onShortcutClick: (StaticShortcut) -> Unit,
        val onTogglePin: (StaticShortcut) -> Unit,
        val onExclude: (StaticShortcut) -> Unit,
        val onInclude: (StaticShortcut) -> Unit,
        val onAppInfoClick: (StaticShortcut) -> Unit,
        val showAllResults: Boolean,
        val showExpandControls: Boolean,
        val onExpandClick: () -> Unit,
        val iconPackPackage: String?,
        val showWallpaperBackground: Boolean
)

/** Data class for Contacts section parameters */
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
        val onContactMethodClick:
                (ContactInfo, com.tk.quicksearch.search.models.ContactMethod) -> Unit,
        val onTogglePin: (ContactInfo) -> Unit,
        val onExclude: (ContactInfo) -> Unit,
        val onNicknameClick: (ContactInfo) -> Unit,
        val getContactNickname: (Long) -> String?,
        val getPrimaryContactCardAction:
                (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
        val getSecondaryContactCardAction:
                (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
        val onPrimaryActionLongPress: (ContactInfo) -> Unit,
        val onSecondaryActionLongPress: (ContactInfo) -> Unit,
        val onCustomAction:
                (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
        val showContactActionHint: Boolean = false,
        val onContactActionHintDismissed: () -> Unit = {},
        val onOpenAppSettings: () -> Unit,
        val showAllResults: Boolean,
        val showExpandControls: Boolean,
        val onExpandClick: () -> Unit,
        val permissionDisabledCard:
                (@Composable
                (
                        title: String,
                        message: String,
                        actionLabel: String,
                        onActionClick: () -> Unit) -> Unit),
        val showWallpaperBackground: Boolean
)

/** Data class for Apps section parameters */
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
        val oneHandedMode: Boolean,
        val isInitializing: Boolean
)

/** Helper function to build all the section parameters needed by SearchScreenContent */
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
        onSettingClick: (DeviceSetting) -> Unit,
        onPinSetting: (DeviceSetting) -> Unit,
        onUnpinSetting: (DeviceSetting) -> Unit,
        onExcludeSetting: (DeviceSetting) -> Unit,
        onAppShortcutClick: (StaticShortcut) -> Unit,
        onPinAppShortcut: (StaticShortcut) -> Unit,
        onUnpinAppShortcut: (StaticShortcut) -> Unit,
        onExcludeAppShortcut: (StaticShortcut) -> Unit,
        onIncludeAppShortcut: (StaticShortcut) -> Unit,
        onAppShortcutAppInfoClick: (StaticShortcut) -> Unit,
        onContactClick: (ContactInfo) -> Unit,
        onShowContactMethods: (ContactInfo) -> Unit,
        onCallContact: (ContactInfo) -> Unit,
        onSmsContact: (ContactInfo) -> Unit,
        onContactMethodClick: (ContactInfo, com.tk.quicksearch.search.models.ContactMethod) -> Unit,
        onPinContact: (ContactInfo) -> Unit,
        onUnpinContact: (ContactInfo) -> Unit,
        onExcludeContact: (ContactInfo) -> Unit,
        getPrimaryContactCardAction:
                (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
        getSecondaryContactCardAction:
                (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
        onPrimaryActionLongPress: (ContactInfo) -> Unit,
        onSecondaryActionLongPress: (ContactInfo) -> Unit,
        onCustomAction:
                (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
        onContactActionHintDismissed: () -> Unit,
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
) =
        remember(
                state,
                derivedState,
                expandedSection,
                onFileClick,
                onPinFile,
                onUnpinFile,
                onExcludeFile,
                onExcludeFileExtension,
                onOpenStorageAccessSettings,
                onSettingClick,
                onPinSetting,
                onUnpinSetting,
                onExcludeSetting,
                onAppShortcutClick,
                onPinAppShortcut,
                onUnpinAppShortcut,
                onExcludeAppShortcut,
                onIncludeAppShortcut,
                onAppShortcutAppInfoClick,
                onContactClick,
                onShowContactMethods,
                onCallContact,
                onSmsContact,
                onContactMethodClick,
                onPinContact,
                onUnpinContact,
                onExcludeContact,
                onOpenAppSettings,
                getPrimaryContactCardAction,
                getSecondaryContactCardAction,
                onPrimaryActionLongPress,
                onSecondaryActionLongPress,
                onCustomAction,
                onContactActionHintDismissed,
                onAppClick,
                onAppInfoClick,
                onUninstallClick,
                onHideApp,
                onPinApp,
                onUnpinApp,
                getFileNickname,
                getContactNickname,
                getSettingNickname,
                getAppNickname,
                onUpdateNicknameDialogState,
                onUpdateExpandedSection
        ) {
                val filesParams =
                        FilesSectionParams(
                                files = state.fileResults,
                                hasPermission = state.hasFilePermission,
                                isExpanded = expandedSection == ExpandedSection.FILES,
                                pinnedFileUris = derivedState.pinnedFileUris,
                                onFileClick = onFileClick,
                                onRequestPermission = onOpenStorageAccessSettings,
                                onTogglePin = { file ->
                                        if (derivedState.pinnedFileUris.contains(
                                                        file.uri.toString()
                                                )
                                        ) {
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
                                                        currentNickname =
                                                                getFileNickname(
                                                                        file.uri.toString()
                                                                ),
                                                        itemName = file.displayName
                                                )
                                        )
                                },
                                getFileNickname = getFileNickname,
                                showAllResults = false,
                                showExpandControls = derivedState.isSearching,
                                onExpandClick = {
                                        onUpdateExpandedSection(
                                                if (expandedSection == ExpandedSection.FILES)
                                                        ExpandedSection.NONE
                                                else ExpandedSection.FILES
                                        )
                                },
                                permissionDisabledCard = {
                                        title,
                                        message,
                                        actionLabel,
                                        onActionClick ->
                                        PermissionDisabledCard(
                                                title = title,
                                                message = message,
                                                actionLabel = actionLabel,
                                                onActionClick = onActionClick
                                        )
                                },
                                showWallpaperBackground = state.showWallpaperBackground
                        )

                val appShortcutParams =
                        AppShortcutsSectionParams(
                                shortcuts = state.appShortcutResults,
                                isExpanded = expandedSection == ExpandedSection.APP_SHORTCUTS,
                                pinnedShortcutIds = derivedState.pinnedAppShortcutIds,
                                excludedShortcutIds =
                                        state.excludedAppShortcuts.map { shortcutKey(it) }.toSet(),
                                onShortcutClick = onAppShortcutClick,
                                onTogglePin = { shortcut ->
                                        if (derivedState.pinnedAppShortcutIds.contains(
                                                        shortcutKey(shortcut)
                                                )
                                        ) {
                                                onUnpinAppShortcut(shortcut)
                                        } else {
                                                onPinAppShortcut(shortcut)
                                        }
                                },
                                onExclude = onExcludeAppShortcut,
                                onInclude = onIncludeAppShortcut,
                                onAppInfoClick = onAppShortcutAppInfoClick,
                                showAllResults = false,
                                showExpandControls = derivedState.isSearching,
                                onExpandClick = {
                                        onUpdateExpandedSection(
                                                if (expandedSection == ExpandedSection.APP_SHORTCUTS
                                                )
                                                        ExpandedSection.NONE
                                                else ExpandedSection.APP_SHORTCUTS
                                        )
                                },
                                iconPackPackage = state.selectedIconPackPackage,
                                showWallpaperBackground = state.showWallpaperBackground
                        )

                val settingsParams =
                        SettingsSectionParams(
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
                                                        currentNickname =
                                                                getSettingNickname(setting.id),
                                                        itemName = setting.title
                                                )
                                        )
                                },
                                getSettingNickname = getSettingNickname,
                                showAllResults = false,
                                showExpandControls = derivedState.isSearching,
                                onExpandClick = {
                                        onUpdateExpandedSection(
                                                if (expandedSection == ExpandedSection.SETTINGS)
                                                        ExpandedSection.NONE
                                                else ExpandedSection.SETTINGS
                                        )
                                },
                                showWallpaperBackground = state.showWallpaperBackground
                        )

                val contactsParams =
                        ContactsSectionParams(
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
                                        if (derivedState.pinnedContactIds.contains(
                                                        contact.contactId
                                                )
                                        ) {
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
                                                        currentNickname =
                                                                getContactNickname(
                                                                        contact.contactId
                                                                ),
                                                        itemName = contact.displayName
                                                )
                                        )
                                },
                                getContactNickname = getContactNickname,
                                getPrimaryContactCardAction = getPrimaryContactCardAction,
                                getSecondaryContactCardAction = getSecondaryContactCardAction,
                                onPrimaryActionLongPress = onPrimaryActionLongPress,
                                onSecondaryActionLongPress = onSecondaryActionLongPress,
                                onCustomAction = onCustomAction,
                                showContactActionHint = state.showContactActionHint,
                                onContactActionHintDismissed = onContactActionHintDismissed,
                                onOpenAppSettings = onOpenAppSettings,
                                showAllResults = false,
                                showExpandControls = derivedState.isSearching,
                                onExpandClick = {
                                        onUpdateExpandedSection(
                                                if (expandedSection == ExpandedSection.CONTACTS)
                                                        ExpandedSection.NONE
                                                else ExpandedSection.CONTACTS
                                        )
                                },
                                permissionDisabledCard = {
                                        title,
                                        message,
                                        actionLabel,
                                        onActionClick ->
                                        PermissionDisabledCard(
                                                title = title,
                                                message = message,
                                                actionLabel = actionLabel,
                                                onActionClick = onActionClick
                                        )
                                },
                                showWallpaperBackground = state.showWallpaperBackground
                        )
                val appsParams =
                        AppsSectionParams(
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
                                                        currentNickname =
                                                                getAppNickname(app.packageName),
                                                        itemName = app.appName
                                                )
                                        )
                                },
                                getAppNickname = getAppNickname,
                                showAppLabels = true,
                                rowCount = derivedState.visibleRowCount,
                                iconPackPackage = state.selectedIconPackPackage,
                                oneHandedMode = state.oneHandedMode,
                                isInitializing = state.isInitializing
                        )

                SectionParams(
                        filesParams = filesParams,
                        appShortcutsParams = appShortcutParams,
                        settingsParams = settingsParams,
                        contactsParams = contactsParams,
                        appsParams = appsParams
                )
        }

/** Data class to hold all section parameters */
data class SectionParams(
        val filesParams: FilesSectionParams,
        val appShortcutsParams: AppShortcutsSectionParams,
        val settingsParams: SettingsSectionParams,
        val contactsParams: ContactsSectionParams,
        val appsParams: AppsSectionParams
)
