package com.tk.quicksearch.search.core

enum class SearchSectionPermissionRequirement {
    CONTACTS,
    FILES,
    CALENDAR,
}

object SearchSectionPermissionRequirements {
    private val requirementBySection: Map<SearchSection, SearchSectionPermissionRequirement> =
        SearchSectionRegistry.orderedDefinitions
            .mapNotNull { definition ->
                definition.permissionRequirement?.let { requirement ->
                    definition.section to requirement
                }
            }
            .toMap()

    fun requirementFor(section: SearchSection): SearchSectionPermissionRequirement? =
        requirementBySection[section]
}
