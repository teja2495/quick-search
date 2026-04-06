package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey

/**
 * Canonical definition table for every search section.
 *
 * Centralizing these definitions keeps alias wiring, settings wiring,
 * ordering, and secondary-search capability in one place.
 */
data class SearchSectionDefinition(
    val section: SearchSection,
    val itemType: ItemPriorityConfig.ItemType,
    val aliasTargetId: String,
    val appSettingsToggleKey: AppSettingsToggleKey,
    val permissionRequirement: SearchSectionPermissionRequirement? = null,
    val participatesInSecondarySearch: Boolean,
    val minimumQueryLength: Int,
)

object SearchSectionRegistry {
    const val SEARCH_SECTION_APPS_ALIAS_ID = "search_section_apps"
    const val SEARCH_SECTION_APP_SHORTCUTS_ALIAS_ID = "search_section_app_shortcuts"
    const val SEARCH_SECTION_CONTACTS_ALIAS_ID = "search_section_contacts"
    const val SEARCH_SECTION_FILES_ALIAS_ID = "search_section_files"
    const val SEARCH_SECTION_SETTINGS_ALIAS_ID = "search_section_settings"
    const val SEARCH_SECTION_CALENDAR_ALIAS_ID = "search_section_calendar"
    const val SEARCH_SECTION_NOTES_ALIAS_ID = "search_section_notes"
    const val SEARCH_SECTION_APP_SETTINGS_ALIAS_ID = "search_section_app_settings"

    val orderedDefinitions: List<SearchSectionDefinition> =
        listOf(
            SearchSectionDefinition(
                section = SearchSection.APPS,
                itemType = ItemPriorityConfig.ItemType.APPS_SECTION,
                aliasTargetId = SEARCH_SECTION_APPS_ALIAS_ID,
                appSettingsToggleKey = AppSettingsToggleKey.SEARCH_APPS,
                participatesInSecondarySearch = false,
                minimumQueryLength = 0,
            ),
            SearchSectionDefinition(
                section = SearchSection.APP_SHORTCUTS,
                itemType = ItemPriorityConfig.ItemType.APP_SHORTCUTS_SECTION,
                aliasTargetId = SEARCH_SECTION_APP_SHORTCUTS_ALIAS_ID,
                appSettingsToggleKey = AppSettingsToggleKey.SEARCH_APP_SHORTCUTS,
                participatesInSecondarySearch = true,
                minimumQueryLength = 1,
            ),
            SearchSectionDefinition(
                section = SearchSection.CONTACTS,
                itemType = ItemPriorityConfig.ItemType.CONTACTS_SECTION,
                aliasTargetId = SEARCH_SECTION_CONTACTS_ALIAS_ID,
                appSettingsToggleKey = AppSettingsToggleKey.SEARCH_CONTACTS,
                permissionRequirement = SearchSectionPermissionRequirement.CONTACTS,
                participatesInSecondarySearch = true,
                minimumQueryLength = 2,
            ),
            SearchSectionDefinition(
                section = SearchSection.FILES,
                itemType = ItemPriorityConfig.ItemType.FILES_SECTION,
                aliasTargetId = SEARCH_SECTION_FILES_ALIAS_ID,
                appSettingsToggleKey = AppSettingsToggleKey.SEARCH_FILES,
                permissionRequirement = SearchSectionPermissionRequirement.FILES,
                participatesInSecondarySearch = true,
                minimumQueryLength = 2,
            ),
            SearchSectionDefinition(
                section = SearchSection.CALENDAR,
                itemType = ItemPriorityConfig.ItemType.CALENDAR_SECTION,
                aliasTargetId = SEARCH_SECTION_CALENDAR_ALIAS_ID,
                appSettingsToggleKey = AppSettingsToggleKey.SEARCH_CALENDAR,
                permissionRequirement = SearchSectionPermissionRequirement.CALENDAR,
                participatesInSecondarySearch = true,
                minimumQueryLength = 2,
            ),
            SearchSectionDefinition(
                section = SearchSection.NOTES,
                itemType = ItemPriorityConfig.ItemType.NOTES_SECTION,
                aliasTargetId = SEARCH_SECTION_NOTES_ALIAS_ID,
                appSettingsToggleKey = AppSettingsToggleKey.SEARCH_NOTES,
                participatesInSecondarySearch = true,
                minimumQueryLength = 1,
            ),
            SearchSectionDefinition(
                section = SearchSection.SETTINGS,
                itemType = ItemPriorityConfig.ItemType.SETTINGS_SECTION,
                aliasTargetId = SEARCH_SECTION_SETTINGS_ALIAS_ID,
                appSettingsToggleKey = AppSettingsToggleKey.SEARCH_DEVICE_SETTINGS,
                participatesInSecondarySearch = true,
                minimumQueryLength = 2,
            ),
            SearchSectionDefinition(
                section = SearchSection.APP_SETTINGS,
                itemType = ItemPriorityConfig.ItemType.APP_SETTINGS_SECTION,
                aliasTargetId = SEARCH_SECTION_APP_SETTINGS_ALIAS_ID,
                appSettingsToggleKey = AppSettingsToggleKey.SEARCH_APP_SETTINGS,
                participatesInSecondarySearch = true,
                minimumQueryLength = 2,
            ),
        )

    private val bySection = orderedDefinitions.associateBy { it.section }
    private val byAliasTargetId = orderedDefinitions.associateBy { it.aliasTargetId }
    private val byToggleKey = orderedDefinitions.associateBy { it.appSettingsToggleKey }
    private val byItemType = orderedDefinitions.associateBy { it.itemType }

    val orderedSections: List<SearchSection> = orderedDefinitions.map { it.section }
    val searchSectionAliasIds: Set<String> = orderedDefinitions.map { it.aliasTargetId }.toSet()
    val secondarySearchDefinitions: List<SearchSectionDefinition> =
        orderedDefinitions.filter { it.participatesInSecondarySearch }

    fun definitionFor(section: SearchSection): SearchSectionDefinition = bySection.getValue(section)

    fun definitionForAliasTargetId(aliasTargetId: String): SearchSectionDefinition? =
        byAliasTargetId[aliasTargetId]

    fun sectionForToggle(toggleKey: AppSettingsToggleKey): SearchSection? =
        byToggleKey[toggleKey]?.section

    fun sectionForItemType(itemType: ItemPriorityConfig.ItemType): SearchSection? =
        byItemType[itemType]?.section
}
