package com.tk.quicksearch.search.apps

import android.app.Application
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles icon pack selection and management operations.
 */
class IconPackService(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit,
) {
    var selectedIconPackPackage: String? = userPreferences.getSelectedIconPackPackage()
        private set

    var availableIconPacks: List<IconPackInfo> = emptyList()
        private set

    /** Re-reads the saved selection (e.g. after a backup import) and revalidates it. */
    fun reloadFromPreferences() {
        selectedIconPackPackage = userPreferences.getSelectedIconPackPackage()
        refreshIconPacks()
    }

    fun refreshIconPacks() {
        scope.launch(Dispatchers.IO) {
            val packs = IconPackManager.findInstalledIconPacks(application)
            val normalizedSelection =
                selectedIconPackPackage?.takeIf { pkg ->
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
                    selectedIconPackPackage = normalizedSelection,
                )
            }
        }
    }

    fun setIconPackPackage(
        packageName: String?,
        visiblePackageNames: Collection<String> = emptyList(),
    ) {
        scope.launch(Dispatchers.IO) {
            val normalizedSelection =
                packageName?.takeIf { pkg ->
                    availableIconPacks.any { it.packageName == pkg }
                }

            if (selectedIconPackPackage == normalizedSelection) return@launch

            val iconOverridesToClear =
                findIconOverridesReplacedByPack(
                    iconPackPackage = normalizedSelection,
                    overriddenPackages = userPreferences.getAppIconOverridePackageNames(),
                ) { iconPackPackage, appPackage ->
                    IconPackManager.hasExplicitIcon(
                        context = application,
                        iconPackPackage = iconPackPackage,
                        targetPackage = appPackage,
                    )
                }

            if (iconOverridesToClear.isNotEmpty()) {
                userPreferences.clearAppIconOverrides(iconOverridesToClear)
                invalidateAppIconCache()
            }

            val packagesToPrefetch =
                visiblePackageNames
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .toList()

            if (packagesToPrefetch.isNotEmpty()) {
                prefetchAppIcons(
                    context = application,
                    packageNames = packagesToPrefetch,
                    iconPackPackage = normalizedSelection,
                    maxCount = packagesToPrefetch.size.coerceAtMost(30),
                )
            }

            selectedIconPackPackage = normalizedSelection
            userPreferences.setSelectedIconPackPackage(normalizedSelection)

            onStateUpdate { state ->
                state.copy(selectedIconPackPackage = normalizedSelection)
            }
        }
    }

    fun resetAllAppIconsToDefault() {
        scope.launch(Dispatchers.IO) {
            val overriddenPackages = userPreferences.getAppIconOverridePackageNames()
            if (overriddenPackages.isNotEmpty()) {
                userPreferences.clearAppIconOverrides(overriddenPackages)
            }

            val alreadyUsingSystemIcons = selectedIconPackPackage == null
            if (!alreadyUsingSystemIcons) {
                selectedIconPackPackage = null
                userPreferences.setSelectedIconPackPackage(null)
            }

            if (!alreadyUsingSystemIcons || overriddenPackages.isNotEmpty()) {
                invalidateAppIconCache()
            }

            onStateUpdate { state ->
                state.copy(selectedIconPackPackage = null)
            }
        }
    }

    fun prefetchVisibleAppIcons(
        pinnedApps: List<com.tk.quicksearch.search.models.AppInfo>,
        recents: List<com.tk.quicksearch.search.models.AppInfo>,
        searchResults: List<com.tk.quicksearch.search.models.AppInfo>,
        forceCircularMask: Boolean,
    ) {
        val iconPack = selectedIconPackPackage ?: return
        // Same cache-key inputs as the grid's rememberAppIcon (shape, work profile), or nothing hits.
        val requests =
            (pinnedApps + recents + searchResults.take(10)) // 10 is default GRID_ITEM_COUNT
                .map { AppIconRequest(it.packageName, it.userHandleId) }
        if (requests.isEmpty()) return

        scope.launch(Dispatchers.IO) {
            prefetchAppIconRequests(
                context = application,
                requests = requests,
                iconPackPackage = iconPack,
                maxCount = 30,
                forceCircularMask = forceCircularMask,
            )
        }
    }
}

internal fun findIconOverridesReplacedByPack(
    iconPackPackage: String?,
    overriddenPackages: Set<String>,
    hasExplicitIcon: (iconPackPackage: String, appPackage: String) -> Boolean,
): Set<String> {
    if (iconPackPackage == null || overriddenPackages.isEmpty()) return emptySet()

    return overriddenPackages.filterTo(mutableSetOf()) { appPackage ->
        hasExplicitIcon(iconPackPackage, appPackage)
    }
}
