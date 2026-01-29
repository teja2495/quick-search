package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.UserAppPreferences

class PermissionManager(
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val userPreferences: UserAppPreferences,
) {
    fun hasContactPermission(): Boolean = contactRepository.hasPermission()

    fun hasFilePermission(): Boolean = fileRepository.hasPermission()

    fun computeDisabledSections(): Set<SearchSection> {
        val userDisabledSections =
            userPreferences
                .getDisabledSections()
                .mapNotNull { name -> SearchSection.values().find { it.name == name } }
                .toSet()

        val permissionBasedDisabledSections = mutableSetOf<SearchSection>()
        if (!hasContactPermission()) {
            permissionBasedDisabledSections.add(SearchSection.CONTACTS)
        }
        if (!hasFilePermission()) {
            permissionBasedDisabledSections.add(SearchSection.FILES)
        }

        return userDisabledSections + permissionBasedDisabledSections
    }

    fun canEnableSection(section: SearchSection): Boolean =
        when (section) {
            SearchSection.CONTACTS -> hasContactPermission()
            SearchSection.FILES -> hasFilePermission()
            SearchSection.APPS -> true
            SearchSection.APP_SHORTCUTS -> true
            SearchSection.SETTINGS -> true
        }

    fun enableSection(
        section: SearchSection,
        currentDisabled: Set<SearchSection>,
    ): Set<SearchSection> {
        if (!canEnableSection(section)) {
            return currentDisabled // Can't enable without permission
        }

        val userDisabledSections =
            userPreferences
                .getDisabledSections()
                .mapNotNull { name -> SearchSection.values().find { it.name == name } }
                .toMutableSet()
        userDisabledSections.remove(section)
        userPreferences.setDisabledSections(userDisabledSections.map { it.name }.toSet())

        return computeDisabledSections()
    }

    fun disableSection(
        section: SearchSection,
        currentDisabled: Set<SearchSection>,
    ): Set<SearchSection> {
        val userDisabledSections =
            userPreferences
                .getDisabledSections()
                .mapNotNull { name -> SearchSection.values().find { it.name == name } }
                .toMutableSet()
        userDisabledSections.add(section)
        userPreferences.setDisabledSections(userDisabledSections.map { it.name }.toSet())

        return computeDisabledSections()
    }
}
