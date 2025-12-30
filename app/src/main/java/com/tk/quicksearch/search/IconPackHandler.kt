package com.tk.quicksearch.search

import android.app.Application
import com.tk.quicksearch.data.UserAppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles icon pack selection and management operations.
 */
class IconPackHandler(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) {

    var selectedIconPackPackage: String? = userPreferences.getSelectedIconPackPackage()
        private set

    var availableIconPacks: List<IconPackInfo> = emptyList()
        private set

    fun getCurrentSelectedIconPackPackage(): String? = selectedIconPackPackage

    fun refreshIconPacks() {
        scope.launch(Dispatchers.IO) {
            val packs = IconPackManager.findInstalledIconPacks(application)
            val normalizedSelection = selectedIconPackPackage?.takeIf { pkg ->
                packs.any { it.packageName == pkg }
            }

            if (normalizedSelection == null && selectedIconPackPackage != null) {
                userPreferences.setSelectedIconPackPackage(null)
            }

            selectedIconPackPackage = normalizedSelection
            availableIconPacks = packs
            onStateUpdate {
                copy(
                    availableIconPacks = packs,
                    selectedIconPackPackage = normalizedSelection
                )
            }
        }
    }

    fun setIconPackPackage(packageName: String?) {
        scope.launch(Dispatchers.IO) {
            val normalizedSelection = packageName?.takeIf { pkg ->
                availableIconPacks.any { it.packageName == pkg }
            }

            selectedIconPackPackage = normalizedSelection
            userPreferences.setSelectedIconPackPackage(normalizedSelection)
            clearAppIconCaches()

            onStateUpdate {
                copy(selectedIconPackPackage = normalizedSelection)
            }

            prefetchVisibleAppIcons()
        }
    }

    private fun clearAppIconCaches() {
        // This would need to be implemented based on the existing icon caching logic
        // For now, we'll leave it as a placeholder
    }

    private fun prefetchVisibleAppIcons() {
        // This would need to be implemented based on the existing prefetch logic
        // For now, we'll leave it as a placeholder
    }
}
