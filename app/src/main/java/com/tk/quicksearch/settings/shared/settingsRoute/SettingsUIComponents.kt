package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.settings.searchEnginesScreen.AliasDisplayType
import com.tk.quicksearch.settings.searchEnginesScreen.AliasCodeDisplay
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
    onAppsClickNoRipple: Boolean = false,
    appShortcutsSubtitle: String? = null,
    onAppShortcutsClick: (() -> Unit)? = null,
    onAppShortcutsClickNoRipple: Boolean = false,
    contactsSubtitle: String? = null,
    onContactsClick: (() -> Unit)? = null,
    onContactsClickNoRipple: Boolean = false,
    filesSubtitle: String? = null,
    onFilesClick: (() -> Unit)? = null,
    onFilesClickNoRipple: Boolean = false,
    deviceSettingsSubtitle: String? = null,
    onDeviceSettingsClick: (() -> Unit)? = null,
    onDeviceSettingsClickNoRipple: Boolean = false,
    calendarSubtitle: String? = null,
    calendarTagLabel: String? = null,
    onCalendarClick: (() -> Unit)? = null,
    onCalendarClickNoRipple: Boolean = false,
    appSettingsSubtitle: String? = null,
    onAppSettingsClick: (() -> Unit)? = null,
    onAppSettingsClickNoRipple: Boolean = false,
    sectionsWithHiddenAlias: Set<SearchSection> = emptySet(),
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    if (showTitle) {
        Text(
            text = stringResource(R.string.settings_sections_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding),
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.ExtraLargeCardShape) {
        Column(modifier = Modifier.fillMaxWidth()) {
            sectionOrder.forEachIndexed { index, section ->
                val isSectionEnabled = section !in disabledSections
                val isAppsRow = section == SearchSection.APPS
                val isAppShortcutsRow = section == SearchSection.APP_SHORTCUTS
                val isContactsRow = section == SearchSection.CONTACTS
                val isFilesRow = section == SearchSection.FILES
                val isDeviceSettingsRow = section == SearchSection.SETTINGS
                val isCalendarRow = section == SearchSection.CALENDAR
                val isAppSettingsRow = section == SearchSection.APP_SETTINGS
                val showAliasForSection = section !in sectionsWithHiddenAlias
                val aliasTargetId = section.getAliasTargetId()
                val metadata = getSectionMetadata(section)

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
                            isAppSettingsRow -> onAppSettingsClick
                            else -> null
                        },
                    noRippleOnRowClick =
                        when {
                            isAppsRow -> onAppsClickNoRipple
                            isAppShortcutsRow -> onAppShortcutsClickNoRipple
                            isContactsRow -> onContactsClickNoRipple
                            isFilesRow -> onFilesClickNoRipple
                            isDeviceSettingsRow -> onDeviceSettingsClickNoRipple
                            isCalendarRow -> onCalendarClickNoRipple
                            isAppSettingsRow -> onAppSettingsClickNoRipple
                            else -> false
                        },
                    subtitleContent =
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
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        } else {
                            null
                        },
                )

                if (index != sectionOrder.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private fun SearchSection.getAliasTargetId(): String =
    when (this) {
        SearchSection.APPS -> AliasHandler.SEARCH_SECTION_APPS_ALIAS_ID
        SearchSection.APP_SHORTCUTS -> AliasHandler.SEARCH_SECTION_APP_SHORTCUTS_ALIAS_ID
        SearchSection.CONTACTS -> AliasHandler.SEARCH_SECTION_CONTACTS_ALIAS_ID
        SearchSection.FILES -> AliasHandler.SEARCH_SECTION_FILES_ALIAS_ID
        SearchSection.SETTINGS -> AliasHandler.SEARCH_SECTION_SETTINGS_ALIAS_ID
        SearchSection.CALENDAR -> AliasHandler.SEARCH_SECTION_CALENDAR_ALIAS_ID
        SearchSection.APP_SETTINGS -> AliasHandler.SEARCH_SECTION_APP_SETTINGS_ALIAS_ID
    }
