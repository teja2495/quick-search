package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.UserAppPreferences

class PermissionManager(
    private val contactRepository: ContactRepository,
    private val calendarRepository: CalendarRepository,
    private val fileRepository: FileSearchRepository,
    private val userPreferences: UserAppPreferences,
) {
    fun hasContactPermission(): Boolean = contactRepository.hasPermission()

    fun hasFilePermission(): Boolean = fileRepository.hasPermission()

    fun hasCalendarPermission(): Boolean = calendarRepository.hasPermission()

    fun computeDisabledSections(): Set<SearchSection> {
        val userDisabledSections =
            userPreferences
                .getDisabledSections()
                .mapNotNull { name -> SearchSection.values().find { it.name == name } }
                .toSet()

        val permissionBasedDisabledSections =
            SearchSection.values().filterNot(::hasPermissionForSection).toSet()

        return userDisabledSections + permissionBasedDisabledSections
    }

    fun canEnableSection(section: SearchSection): Boolean =
        hasPermissionForSection(section)

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

    private fun hasPermissionForSection(section: SearchSection): Boolean =
        when (SearchSectionPermissionRequirements.requirementFor(section)) {
            SearchSectionPermissionRequirement.CONTACTS -> hasContactPermission()
            SearchSectionPermissionRequirement.FILES -> hasFilePermission()
            SearchSectionPermissionRequirement.CALENDAR -> hasCalendarPermission()
            null -> true
        }
}
