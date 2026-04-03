package com.tk.quicksearch.search.core

enum class SearchSectionPermissionRequirement {
    CONTACTS,
    FILES,
    CALENDAR,
}

object SearchSectionPermissionRequirements {
    private val requirementBySection: Map<SearchSection, SearchSectionPermissionRequirement> =
        mapOf(
            SearchSection.CONTACTS to SearchSectionPermissionRequirement.CONTACTS,
            SearchSection.FILES to SearchSectionPermissionRequirement.FILES,
            SearchSection.CALENDAR to SearchSectionPermissionRequirement.CALENDAR,
        )

    fun requirementFor(section: SearchSection): SearchSectionPermissionRequirement? =
        requirementBySection[section]
}

