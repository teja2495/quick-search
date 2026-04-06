package com.tk.quicksearch.search.core

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.R

data class SearchSectionUiMetadata(
    val section: SearchSection,
    @StringRes val sectionLabelRes: Int,
    @StringRes val searchHintRes: Int,
    val searchBarIcon: ImageVector,
    val settingsIcon: ImageVector,
    val aliasTargetId: String,
)

object SearchSectionUiMetadataRegistry {
    private val metadataBySection: Map<SearchSection, SearchSectionUiMetadata> =
        listOf(
            SearchSectionUiMetadata(
                section = SearchSection.APPS,
                sectionLabelRes = R.string.section_apps,
                searchHintRes = R.string.search_hint_apps,
                searchBarIcon = Icons.Rounded.Apps,
                settingsIcon = Icons.Rounded.Apps,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_APPS_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.APP_SHORTCUTS,
                sectionLabelRes = R.string.section_app_shortcuts,
                searchHintRes = R.string.search_hint_app_shortcuts,
                searchBarIcon = Icons.AutoMirrored.Rounded.Shortcut,
                settingsIcon = Icons.AutoMirrored.Rounded.Shortcut,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_APP_SHORTCUTS_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.CONTACTS,
                sectionLabelRes = R.string.contacts_action_button_contacts,
                searchHintRes = R.string.search_hint_contacts,
                searchBarIcon = Icons.Rounded.Person,
                settingsIcon = Icons.Rounded.Contacts,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_CONTACTS_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.FILES,
                sectionLabelRes = R.string.section_files,
                searchHintRes = R.string.search_hint_files,
                searchBarIcon = Icons.AutoMirrored.Rounded.InsertDriveFile,
                settingsIcon = Icons.AutoMirrored.Rounded.InsertDriveFile,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_FILES_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.SETTINGS,
                sectionLabelRes = R.string.section_settings,
                searchHintRes = R.string.search_hint_settings,
                searchBarIcon = Icons.Rounded.Settings,
                settingsIcon = Icons.Rounded.Settings,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_SETTINGS_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.CALENDAR,
                sectionLabelRes = R.string.section_calendar,
                searchHintRes = R.string.search_hint_calendar,
                searchBarIcon = Icons.Rounded.CalendarMonth,
                settingsIcon = Icons.Rounded.CalendarMonth,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_CALENDAR_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.NOTES,
                sectionLabelRes = R.string.section_notes,
                searchHintRes = R.string.search_hint_notes,
                searchBarIcon = Icons.Rounded.Description,
                settingsIcon = Icons.Rounded.Description,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_NOTES_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.APP_SETTINGS,
                sectionLabelRes = R.string.section_app_settings,
                searchHintRes = R.string.search_hint_app_settings,
                searchBarIcon = Icons.Rounded.Settings,
                settingsIcon = Icons.Rounded.Settings,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_APP_SETTINGS_ALIAS_ID,
            ),
        ).associateBy { it.section }

    fun metadataFor(section: SearchSection): SearchSectionUiMetadata = metadataBySection.getValue(section)

    fun orderedMetadata(sections: List<SearchSection> = SearchSectionRegistry.orderedSections): List<SearchSectionUiMetadata> =
        sections.map(::metadataFor)
}
