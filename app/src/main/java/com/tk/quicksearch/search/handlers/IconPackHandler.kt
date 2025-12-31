package com.tk.quicksearch.search.handlers

import android.app.Application
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.search.apps.prefetchAppIcons
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.managers.IconPackManager
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
            onStateUpdate { state ->
                state.copy(
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

            onStateUpdate { state ->
                state.copy(selectedIconPackPackage = normalizedSelection)
            }
        }
    }


    fun prefetchVisibleAppIcons(
        pinnedApps: List<com.tk.quicksearch.model.AppInfo>,
        recents: List<com.tk.quicksearch.model.AppInfo>,
        searchResults: List<com.tk.quicksearch.model.AppInfo>
    ) {
        val iconPack = selectedIconPackPackage ?: return
        val packageNames = buildList {
            addAll(pinnedApps.map { it.packageName })
            addAll(recents.map { it.packageName })
            addAll(searchResults.take(10).map { it.packageName }) // 10 is default GRID_ITEM_COUNT
        }
        if (packageNames.isEmpty()) return

        scope.launch(Dispatchers.IO) {
            prefetchAppIcons(
                context = application,
                packageNames = packageNames,
                iconPackPackage = iconPack,
                maxCount = 30
            )
        }
    }
}
