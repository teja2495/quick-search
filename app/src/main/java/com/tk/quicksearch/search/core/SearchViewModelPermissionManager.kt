package com.tk.quicksearch.search.core

import com.tk.quicksearch.data.ContactRepository
import com.tk.quicksearch.data.FileSearchRepository
import com.tk.quicksearch.data.UserAppPreferences

/**
 * Manages permission checks and section state based on permissions.
 * Centralizes permission-related logic to reduce duplication.
 */
class PermissionManager(
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val userPreferences: UserAppPreferences
) {
    
    /**
     * Checks if contacts permission is granted.
     */
    fun hasContactPermission(): Boolean = contactRepository.hasPermission()
    
    /**
     * Checks if files permission is granted.
     */
    fun hasFilePermission(): Boolean = fileRepository.hasPermission()
    
    /**
     * Computes disabled sections based on user preferences and permissions.
     * Sections are automatically disabled if required permissions are missing.
     */
    fun computeDisabledSections(): Set<SearchSection> {
        val userDisabledSections = userPreferences.getDisabledSections()
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
    
    /**
     * Checks if a section can be enabled (has required permissions).
     */
    fun canEnableSection(section: SearchSection): Boolean {
        return when (section) {
            SearchSection.CONTACTS -> hasContactPermission()
            SearchSection.FILES -> hasFilePermission()
            SearchSection.APPS -> true
            SearchSection.SETTINGS -> true
        }
    }
    
    /**
     * Updates disabled sections when enabling a section.
     * Returns the updated set of disabled sections.
     */
    fun enableSection(section: SearchSection, currentDisabled: Set<SearchSection>): Set<SearchSection> {
        if (!canEnableSection(section)) {
            return currentDisabled // Can't enable without permission
        }
        
        val userDisabledSections = userPreferences.getDisabledSections()
            .mapNotNull { name -> SearchSection.values().find { it.name == name } }
            .toMutableSet()
        userDisabledSections.remove(section)
        userPreferences.setDisabledSections(userDisabledSections.map { it.name }.toSet())
        
        return computeDisabledSections()
    }
    
    /**
     * Updates disabled sections when disabling a section.
     * Returns the updated set of disabled sections.
     */
    fun disableSection(section: SearchSection, currentDisabled: Set<SearchSection>): Set<SearchSection> {
        val userDisabledSections = userPreferences.getDisabledSections()
            .mapNotNull { name -> SearchSection.values().find { it.name == name } }
            .toMutableSet()
        userDisabledSections.add(section)
        userPreferences.setDisabledSections(userDisabledSections.map { it.name }.toSet())
        
        return computeDisabledSections()
    }
}
