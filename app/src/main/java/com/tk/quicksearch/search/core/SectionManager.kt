package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.UserAppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles section configuration and management. */
class SectionManager(
    private val userPreferences: UserAppPreferences,
    private val permissionManager: PermissionManager,
    private val scope: CoroutineScope,
    private val onStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit,
) {
    // Section order is centrally defined in ItemPriorityConfig
    // Uses searchingStatePriority as default section order (can be customized by query state)
    val sectionOrder: List<SearchSection> = ItemPriorityConfig.getSearchResultsPriority()

    var disabledSections: Set<SearchSection> = permissionManager.computeDisabledSections()
        private set

    fun setSectionEnabled(
        section: SearchSection,
        enabled: Boolean,
    ) {
        scope.launch(Dispatchers.IO) {
            if (enabled && !permissionManager.canEnableSection(section)) {
                // Permission not granted - don't enable, permission should be requested from UI
                return@launch
            }

            disabledSections =
                if (enabled) {
                    permissionManager.enableSection(section, disabledSections)
                } else {
                    permissionManager.disableSection(section, disabledSections)
                }

            if (!enabled) {
                // Note: Permission refresh logic would be handled by the caller
            }

            onStateUpdate { state -> state.copy(disabledSections = disabledSections) }
        }
    }

    fun canEnableSection(section: SearchSection): Boolean = permissionManager.canEnableSection(section)

    /**
     * Refreshes disabled sections based on current permissions. Should be called when permissions
     * change.
     */
    fun refreshDisabledSections() {
        disabledSections = permissionManager.computeDisabledSections()
        onStateUpdate { state -> state.copy(disabledSections = disabledSections) }
    }
}
