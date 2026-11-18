package com.tk.quicksearch.settings.shared.settingsRoute

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionUiMetadataRegistry
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.settings.searchEngineSettings.AliasDisplayType
import com.tk.quicksearch.settings.searchEngineSettings.AliasCodeDisplay
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsNavigationToggleRow
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun SectionSettingsSection(
    sectionOrder: List<SearchSection>,
    disabledSections: Set<SearchSection>,
    onToggleSection: (SearchSection, Boolean) -> Unit,
    sectionAliasCodes: Map<String, String> = emptyMap(),
    onSetSectionAlias: ((String, String) -> Unit)? = null,
    appsSubtitle: String? = null,
    onAppsClick: (() -> Unit)? = null,
    appShortcutsSubtitle: String? = null,
    onAppShortcutsClick: (() -> Unit)? = null,
    contactsSubtitle: String? = null,
    onContactsClick: (() -> Unit)? = null,
    filesSubtitle: String? = null,
    onFilesClick: (() -> Unit)? = null,
    deviceSettingsSubtitle: String? = null,
    onDeviceSettingsClick: (() -> Unit)? = null,
    calendarSubtitle: String? = null,
    calendarTagLabel: String? = null,
    onCalendarClick: (() -> Unit)? = null,
    notesSubtitle: String? = null,
    onNotesClick: (() -> Unit)? = null,
    remindersSubtitle: String? = null,
    onRemindersClick: (() -> Unit)? = null,
    bookmarksSubtitle: String? = null,
    onBookmarksClick: (() -> Unit)? = null,
    appSettingsSubtitle: String? = null,
    onAppSettingsClick: (() -> Unit)? = null,
    sectionsWithHiddenAlias: Set<SearchSection> = emptySet(),
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    if (showTitle) {
        Text(
            text = stringResource(R.string.settings_search_results_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding).semantics { heading() },
        )
    }

    SegmentedGroup(modifier = Modifier.fillMaxWidth()) {
        sectionOrder.forEachIndexed { index, section ->
            val isSectionEnabled = section !in disabledSections
            val isAppsRow = section == SearchSection.APPS
            val isAppShortcutsRow = section == SearchSection.APP_SHORTCUTS
            val isContactsRow = section == SearchSection.CONTACTS
            val isFilesRow = section == SearchSection.FILES
            val isDeviceSettingsRow = section == SearchSection.SETTINGS
            val isCalendarRow = section == SearchSection.CALENDAR
            val isNotesRow = section == SearchSection.NOTES
            val isRemindersRow = section == SearchSection.REMINDERS
            val isBookmarksRow = section == SearchSection.BOOKMARKS
            val isAppSettingsRow = section == SearchSection.APP_SETTINGS
            val showAliasForSection = section !in sectionsWithHiddenAlias
            val aliasTargetId = SearchSectionUiMetadataRegistry.metadataFor(section).aliasTargetId
            val metadata = getSectionMetadata(section)

            SegmentedTile(index = index, count = sectionOrder.size) {
                SettingsNavigationToggleRow(
                    title = metadata.name,
                    checked = isSectionEnabled,
                    onCheckedChange = { enabled -> onToggleSection(section, enabled) },
                    subtitle =
                        when {
                            isAppsRow -> appsSubtitle
                            isAppShortcutsRow -> appShortcutsSubtitle
                            isContactsRow -> contactsSubtitle
                            isFilesRow -> filesSubtitle
                            isDeviceSettingsRow -> deviceSettingsSubtitle
                            isCalendarRow -> calendarSubtitle
                            isNotesRow -> notesSubtitle
                            isRemindersRow -> remindersSubtitle
                            isBookmarksRow -> bookmarksSubtitle
                            isAppSettingsRow -> appSettingsSubtitle
                            else -> null
                        },
                    leadingIcon = metadata.icon,
                    tagLabel = if (isCalendarRow) calendarTagLabel else null,
                    onRowClick =
                        when {
                            isAppsRow -> onAppsClick
                            isAppShortcutsRow -> onAppShortcutsClick
                            isContactsRow -> onContactsClick
                            isFilesRow -> onFilesClick
                            isDeviceSettingsRow -> onDeviceSettingsClick
                            isCalendarRow -> onCalendarClick
                            isNotesRow -> onNotesClick
                            isRemindersRow -> onRemindersClick
                            isBookmarksRow -> onBookmarksClick
                            isAppSettingsRow -> onAppSettingsClick
                            else -> null
                        },
                    footerContent =
                        if (showAliasForSection && onSetSectionAlias != null) {
                            {
                                AliasCodeDisplay(
                                    shortcutCode = sectionAliasCodes[aliasTargetId].orEmpty(),
                                    isEnabled = true,
                                    onCodeChange = { code -> onSetSectionAlias(aliasTargetId, code) },
                                    engineName = metadata.name,
                                    existingShortcuts = sectionAliasCodes,
                                    currentShortcutId = aliasTargetId,
                                    validateCode = { input -> input.isNotBlank() },
                                    validateConflict = { input, existing ->
                                        !hasExactAliasConflict(input, existing)
                                    },
                                    conflictErrorMessage = stringResource(R.string.dialog_edit_alias_error_prefix),
                                    aliasDisplayType = AliasDisplayType.SEARCH_TYPE,
                                )
                            }
                        } else {
                            null
                        },
                )
            }
        }
    }
}
