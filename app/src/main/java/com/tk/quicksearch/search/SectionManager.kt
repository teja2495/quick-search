package com.tk.quicksearch.search

import com.tk.quicksearch.data.UserAppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles section configuration and management.
 */
class SectionManager(
    private val userPreferences: UserAppPreferences,
    private val permissionManager: PermissionManager,
    private val scope: CoroutineScope,
    private val onStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) {

    var sectionOrder: List<SearchSection> = loadSectionOrder()
        private set

    var disabledSections: Set<SearchSection> = permissionManager.computeDisabledSections()
        private set

    fun reorderSections(newOrder: List<SearchSection>) {
        scope.launch(Dispatchers.IO) {
            sectionOrder = newOrder
            userPreferences.setSectionOrder(newOrder.map { it.name })
            onStateUpdate { state ->
                state.copy(sectionOrder = sectionOrder)
            }
        }
    }

    fun setSectionEnabled(section: SearchSection, enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (enabled && !permissionManager.canEnableSection(section)) {
                // Permission not granted - don't enable, permission should be requested from UI
                return@launch
            }

            disabledSections = if (enabled) {
                permissionManager.enableSection(section, disabledSections)
            } else {
                permissionManager.disableSection(section, disabledSections)
            }

            if (!enabled) {
                // Note: Permission refresh logic would be handled by the caller
            }

            onStateUpdate { state ->
                state.copy(disabledSections = disabledSections)
            }
        }
    }

    fun canEnableSection(section: SearchSection): Boolean {
        return permissionManager.canEnableSection(section)
    }

    private fun loadSectionOrder(): List<SearchSection> {
        val savedOrder = userPreferences.getSectionOrder()
        val defaultOrder = listOf(
            SearchSection.APPS,
            SearchSection.CONTACTS,
            SearchSection.FILES,
            SearchSection.SETTINGS
        )

        if (savedOrder.isEmpty()) {
            // First time - use default order: Apps, Contacts, Files
            return defaultOrder
        }

        // Merge saved order with any new sections that might have been added
        val savedSections = savedOrder.mapNotNull { name ->
            SearchSection.values().find { it.name == name }
        }
        val newSections = SearchSection.values().filter { it !in savedSections }
        return savedSections + newSections
    }
}
