package com.tk.quicksearch.search.core

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

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
                searchBarIcon = MaterialSymbols.Apps,
                settingsIcon = MaterialSymbols.Apps,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_APPS_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.APP_SHORTCUTS,
                sectionLabelRes = R.string.section_app_shortcuts,
                searchHintRes = R.string.search_hint_app_shortcuts,
                searchBarIcon = MaterialSymbols.Shortcut,
                settingsIcon = MaterialSymbols.Shortcut,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_APP_SHORTCUTS_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.CONTACTS,
                sectionLabelRes = R.string.contacts_action_button_contacts,
                searchHintRes = R.string.search_hint_contacts,
                searchBarIcon = MaterialSymbols.Person,
                settingsIcon = MaterialSymbols.Contacts,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_CONTACTS_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.FILES,
                sectionLabelRes = R.string.section_files,
                searchHintRes = R.string.search_hint_files,
                searchBarIcon = MaterialSymbols.InsertDriveFile,
                settingsIcon = MaterialSymbols.InsertDriveFile,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_FILES_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.SETTINGS,
                sectionLabelRes = R.string.section_settings,
                searchHintRes = R.string.search_hint_settings,
                searchBarIcon = MaterialSymbols.Settings,
                settingsIcon = MaterialSymbols.Settings,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_SETTINGS_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.CALENDAR,
                sectionLabelRes = R.string.section_calendar,
                searchHintRes = R.string.search_hint_calendar,
                searchBarIcon = MaterialSymbols.CalendarMonth,
                settingsIcon = MaterialSymbols.CalendarMonth,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_CALENDAR_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.REMINDERS,
                sectionLabelRes = R.string.section_reminders,
                searchHintRes = R.string.search_hint_reminders,
                searchBarIcon = MaterialSymbols.NotificationsActive,
                settingsIcon = MaterialSymbols.NotificationsActive,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_REMINDERS_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.NOTES,
                sectionLabelRes = R.string.section_notes,
                searchHintRes = R.string.search_hint_notes,
                searchBarIcon = MaterialSymbols.Description,
                settingsIcon = MaterialSymbols.Description,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_NOTES_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.NOTED,
                sectionLabelRes = R.string.section_noted,
                searchHintRes = R.string.search_hint_noted,
                searchBarIcon = MaterialSymbols.EditNote,
                settingsIcon = MaterialSymbols.EditNote,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_NOTED_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.BOOKMARKS,
                sectionLabelRes = R.string.section_bookmarks,
                searchHintRes = R.string.search_hint_bookmarks,
                searchBarIcon = MaterialSymbols.Bookmark,
                settingsIcon = MaterialSymbols.Bookmark,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_BOOKMARKS_ALIAS_ID,
            ),
            SearchSectionUiMetadata(
                section = SearchSection.APP_SETTINGS,
                sectionLabelRes = R.string.section_app_settings,
                searchHintRes = R.string.search_hint_app_settings,
                searchBarIcon = MaterialSymbols.Settings,
                settingsIcon = MaterialSymbols.Settings,
                aliasTargetId = SearchSectionRegistry.SEARCH_SECTION_APP_SETTINGS_ALIAS_ID,
            ),
        ).associateBy { it.section }

    fun metadataFor(section: SearchSection): SearchSectionUiMetadata = metadataBySection.getValue(section)

}
