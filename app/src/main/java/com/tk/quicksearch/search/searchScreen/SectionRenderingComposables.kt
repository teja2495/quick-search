package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import com.tk.quicksearch.search.apps.AppGridView
import com.tk.quicksearch.search.contacts.ContactResultsSection
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SectionRenderContext
import com.tk.quicksearch.search.core.SectionRenderParams
import com.tk.quicksearch.search.files.FileResultsSection
import com.tk.quicksearch.settings.settingsScreen.SettingsResultsSection
import com.tk.quicksearch.search.searchScreen.AppsSectionParams
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams

// ============================================================================
// Section Rendering Functions
// ============================================================================

/**
 * Renders a single section based on its type and current state.
 */
@Composable
fun renderSection(
    section: SearchSection,
    params: SectionRenderParams,
    sectionContext: SectionRenderContext
) {
    when (section) {
        SearchSection.FILES -> renderFilesSection(params, sectionContext)
        SearchSection.CONTACTS -> renderContactsSection(params, sectionContext)
        SearchSection.APPS -> renderAppsSection(params, sectionContext)
        SearchSection.SETTINGS -> renderSettingsSection(params, sectionContext)
    }
}

/**
 * Renders the files section if it should be displayed.
 */
@Composable
private fun renderFilesSection(
    params: SectionRenderParams,
    context: SectionRenderContext
) {
    if (context.shouldRenderFiles) {
        val filesParams = params.filesParams.copy(
            files = context.filesList,
            isExpanded = context.isFilesExpanded,
            showAllResults = context.showAllFilesResults,
            showExpandControls = context.showFilesExpandControls,
            onExpandClick = context.filesExpandClick
        )
        FileResultsSection(
            hasPermission = filesParams.hasPermission,
            files = filesParams.files,
            isExpanded = filesParams.isExpanded,
            onFileClick = filesParams.onFileClick,
            onRequestPermission = filesParams.onRequestPermission,
            pinnedFileUris = filesParams.pinnedFileUris,
            onTogglePin = filesParams.onTogglePin,
            onExclude = filesParams.onExclude,
            onExcludeExtension = filesParams.onExcludeExtension,
            onNicknameClick = filesParams.onNicknameClick,
            getFileNickname = filesParams.getFileNickname,
            showAllResults = filesParams.showAllResults,
            showExpandControls = filesParams.showExpandControls,
            onExpandClick = filesParams.onExpandClick,
            permissionDisabledCard = filesParams.permissionDisabledCard,
            showWallpaperBackground = filesParams.showWallpaperBackground
        )
    }
}

/**
 * Renders the contacts section if it should be displayed.
 */
@Composable
private fun renderContactsSection(
    params: SectionRenderParams,
    context: SectionRenderContext
) {
    if (context.shouldRenderContacts) {
        val contactsParams = params.contactsParams.copy(
            contacts = context.contactsList,
            isExpanded = context.isContactsExpanded,
            showAllResults = context.showAllContactsResults,
            showExpandControls = context.showContactsExpandControls,
            onExpandClick = context.contactsExpandClick
        )
        ContactResultsSection(
            hasPermission = contactsParams.hasPermission,
            contacts = contactsParams.contacts,
            isExpanded = contactsParams.isExpanded,
            messagingApp = contactsParams.messagingApp ?: MessagingApp.MESSAGES,
            onContactClick = contactsParams.onContactClick,
            onShowContactMethods = contactsParams.onShowContactMethods,
            onCallContact = contactsParams.onCallContact,
            onSmsContact = contactsParams.onSmsContact,
            onContactMethodClick = contactsParams.onContactMethodClick,
            pinnedContactIds = contactsParams.pinnedContactIds,
            onTogglePin = contactsParams.onTogglePin,
            onExclude = contactsParams.onExclude,
            onNicknameClick = contactsParams.onNicknameClick,
            getContactNickname = contactsParams.getContactNickname,
            onOpenAppSettings = contactsParams.onOpenAppSettings,
            showAllResults = contactsParams.showAllResults,
            showExpandControls = contactsParams.showExpandControls,
            onExpandClick = contactsParams.onExpandClick,
            permissionDisabledCard = contactsParams.permissionDisabledCard,
            showWallpaperBackground = contactsParams.showWallpaperBackground
        )
    }
}

/**
 * Renders the apps section if it should be displayed.
 */
@Composable
private fun renderAppsSection(
    params: SectionRenderParams,
    context: SectionRenderContext
) {
    if (context.shouldRenderApps && params.appsParams != null) {
        AppGridView(
            apps = params.appsParams.apps,
            isSearching = params.appsParams.isSearching,
            hasAppResults = params.appsParams.hasAppResults,
            onAppClick = params.appsParams.onAppClick,
            onAppInfoClick = params.appsParams.onAppInfoClick,
            onUninstallClick = params.appsParams.onUninstallClick,
            onHideApp = params.appsParams.onHideApp,
            onPinApp = params.appsParams.onPinApp,
            onUnpinApp = params.appsParams.onUnpinApp,
            onNicknameClick = params.appsParams.onNicknameClick,
            getAppNickname = params.appsParams.getAppNickname,
            pinnedPackageNames = params.appsParams.pinnedPackageNames,
            showAppLabels = params.appsParams.showAppLabels,
            rowCount = params.appsParams.rowCount,
            iconPackPackage = params.appsParams.iconPackPackage,
            keyboardAlignedLayout = params.appsParams.keyboardAlignedLayout,
            isInitializing = params.appsParams.isInitializing
        )
    }
}

/**
 * Renders the settings section if it should be displayed.
 */
@Composable
private fun renderSettingsSection(
    params: SectionRenderParams,
    context: SectionRenderContext
) {
    if (context.shouldRenderSettings && params.settingsParams != null) {
        SettingsResultsSection(
            settings = context.settingsList,
            isExpanded = context.isSettingsExpanded,
            pinnedSettingIds = params.settingsParams.pinnedSettingIds,
            onSettingClick = params.settingsParams.onSettingClick,
            onTogglePin = params.settingsParams.onTogglePin,
            onExclude = params.settingsParams.onExclude,
            onNicknameClick = params.settingsParams.onNicknameClick,
            getSettingNickname = params.settingsParams.getSettingNickname,
            showAllResults = context.showAllSettingsResults,
            showExpandControls = context.showSettingsExpandControls,
            onExpandClick = context.settingsExpandClick,
            showWallpaperBackground = params.settingsParams.showWallpaperBackground
        )
    }
}