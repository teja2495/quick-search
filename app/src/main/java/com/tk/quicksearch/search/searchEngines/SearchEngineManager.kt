package com.tk.quicksearch.search.searchEngines

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.BrowserApp
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.util.PackageConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles search engine configuration and management. */
class SearchEngineManager(
        private val context: Context,
        private val userPreferences: UserAppPreferences,
        private val scope: CoroutineScope,
        private val onStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) {

    companion object {
        private const val BRAVE_BROWSER_PACKAGE = "com.brave.browser"
    }

    var searchTargetsOrder: List<SearchTarget> = emptyList()
        private set

    var disabledSearchTargetIds: Set<String> = emptySet()
        private set

    var isSearchEngineCompactMode: Boolean = false
        private set

    private var isInitialized = false

    fun ensureInitialized() {
        if (!isInitialized) {
            val hasGemini = !userPreferences.getGeminiApiKey().isNullOrBlank()
            val availableEngines = getAvailableEngines(hasGemini)
            val availableBrowsers = loadInstalledBrowsers()
            val savedOrder = userPreferences.getSearchEngineOrder()
            searchTargetsOrder =
                    loadSearchTargetsOrder(savedOrder, availableEngines, availableBrowsers, hasGemini)
            disabledSearchTargetIds =
                    loadDisabledSearchTargetIds(
                            savedOrder,
                            availableEngines,
                            availableBrowsers,
                            hasGemini
                    )
            isSearchEngineCompactMode = userPreferences.isSearchEngineCompactMode()
            isInitialized = true
        }
    }

    fun getEnabledSearchTargets(): List<SearchTarget> {
        return searchTargetsOrder.filter { it.getId() !in disabledSearchTargetIds }
    }

    fun setSearchTargetEnabled(target: SearchTarget, enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (target is SearchTarget.Engine &&
                            target.engine == SearchEngine.DIRECT_SEARCH &&
                            enabled
            ) {
                val hasGeminiApiKey = !userPreferences.getGeminiApiKey().isNullOrBlank()
                if (!hasGeminiApiKey) {
                    return@launch
                }
            }

            // Prevent disabling the last enabled search engine
            if (!enabled) {
                val wouldBeDisabled = disabledSearchTargetIds.toMutableSet().apply { add(target.getId()) }
                val remainingEnabledCount = searchTargetsOrder.count { it.getId() !in wouldBeDisabled }
                if (remainingEnabledCount == 0) {
                    // Show toast on main thread
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            R.string.settings_search_engines_at_least_one_required,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }
            }

            val disabled = disabledSearchTargetIds.toMutableSet()
            val id = target.getId()
            if (enabled) {
                disabled.remove(id)
            } else {
                disabled.add(id)
            }
            disabledSearchTargetIds = disabled
            userPreferences.setDisabledSearchEngines(disabledSearchTargetIds)
            onStateUpdate { state -> state.copy(disabledSearchTargetIds = disabledSearchTargetIds) }
        }
    }

    fun reorderSearchTargets(newOrder: List<SearchTarget>) {
        scope.launch(Dispatchers.IO) {
            searchTargetsOrder = newOrder
            userPreferences.setSearchEngineOrder(newOrder.map { it.getId() })
            onStateUpdate { state -> state.copy(searchTargetsOrder = searchTargetsOrder) }
        }
    }

    fun setSearchEngineCompactMode(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            isSearchEngineCompactMode = enabled
            userPreferences.setSearchEngineCompactMode(enabled)
            onStateUpdate { state ->
                state.copy(
                        isSearchEngineCompactMode = isSearchEngineCompactMode,
                        showSearchEngineOnboarding =
                                enabled && !userPreferences.hasSeenSearchEngineOnboarding()
                )
            }
        }
    }

    fun updateSearchTargetsForGemini(hasGemini: Boolean) {
        val updatedOrder = applyDirectSearchAvailability(searchTargetsOrder, hasGemini)
        searchTargetsOrder = updatedOrder
        if (!hasGemini) {
            disabledSearchTargetIds =
                    disabledSearchTargetIds.filterNot { it == SearchEngine.DIRECT_SEARCH.name }
                            .toSet()
        }
        userPreferences.setSearchEngineOrder(searchTargetsOrder.map { it.getId() })
        userPreferences.setDisabledSearchEngines(disabledSearchTargetIds)
        onStateUpdate { state ->
            state.copy(
                    searchTargetsOrder = searchTargetsOrder,
                    disabledSearchTargetIds = disabledSearchTargetIds
            )
        }
    }

    fun refreshBrowserTargets() {
        scope.launch(Dispatchers.IO) {
            val availableBrowsers = loadInstalledBrowsers()
            val existingBrowserIds =
                    searchTargetsOrder.filterIsInstance<SearchTarget.Browser>()
                            .map { buildBrowserId(it.app.packageName) }
                            .toSet()
            val updatedOrder = mergeBrowsers(searchTargetsOrder, availableBrowsers)
            val browserIds =
                    availableBrowsers.map { buildBrowserId(it.packageName) }.toSet()
            val updatedDisabled =
                    disabledSearchTargetIds.toMutableSet().apply {
                        addAll(browserIds - existingBrowserIds)
                    }
            val cleanedDisabled =
                    updatedDisabled.filterNot { id ->
                        id.startsWith(BROWSER_ID_PREFIX) && id !in browserIds
                    }
                            .toSet()

            if (updatedOrder != searchTargetsOrder || cleanedDisabled != disabledSearchTargetIds) {
                searchTargetsOrder = updatedOrder
                disabledSearchTargetIds = cleanedDisabled
                userPreferences.setSearchEngineOrder(searchTargetsOrder.map { it.getId() })
                userPreferences.setDisabledSearchEngines(disabledSearchTargetIds)
                onStateUpdate { state ->
                    state.copy(
                            searchTargetsOrder = searchTargetsOrder,
                            disabledSearchTargetIds = disabledSearchTargetIds
                    )
                }
            }
        }
    }

    private fun getAvailableEngines(hasGemini: Boolean): List<SearchEngine> {
        val packageManager = context.packageManager
        return SearchEngine.values().filter { engine ->
            when (engine) {
                SearchEngine.DIRECT_SEARCH -> hasGemini
                SearchEngine.YOUTUBE_MUSIC -> isPackageInstalled(packageManager, PackageConstants.YOUTUBE_MUSIC_PACKAGE)
                SearchEngine.SPOTIFY -> isPackageInstalled(packageManager, PackageConstants.SPOTIFY_PACKAGE)
                else -> true
            }
        }
    }

    private fun loadSearchTargetsOrder(
            savedOrder: List<String>,
            availableEngines: List<SearchEngine>,
            availableBrowsers: List<BrowserApp>,
            hasGemini: Boolean
    ): List<SearchTarget> {
        val browserMap = availableBrowsers.associateBy { it.packageName }

        if (savedOrder.isEmpty()) {
            val defaultEngines = availableEngines.map { SearchTarget.Engine(it) }
            val defaultBrowsers = availableBrowsers.map { SearchTarget.Browser(it) }
            val defaultOrder = insertBrowsersAfterAnchor(defaultEngines, defaultBrowsers)
            userPreferences.setSearchEngineOrder(defaultOrder.map { it.getId() })
            return defaultOrder
        }

        val savedTargets =
                savedOrder.mapNotNull { id ->
                    parseSearchTargetId(id, availableEngines, browserMap)
                }
        val directAdjusted = applyDirectSearchAvailability(savedTargets, hasGemini)
        val withNewEngines = mergeMissingEngines(directAdjusted, availableEngines)
        val finalOrder = mergeBrowsers(withNewEngines, availableBrowsers)

        val savedIds = savedOrder
        val finalIds = finalOrder.map { it.getId() }
        if (savedIds != finalIds) {
            userPreferences.setSearchEngineOrder(finalIds)
        }

        return finalOrder
    }

    private fun loadDisabledSearchTargetIds(
            savedOrder: List<String>,
            availableEngines: List<SearchEngine>,
            availableBrowsers: List<BrowserApp>,
            hasGemini: Boolean
    ): Set<String> {
        val hasPreference = userPreferences.hasDisabledSearchEnginesPreference()
        val disabledNames = userPreferences.getDisabledSearchEngines()
        val engineNames = availableEngines.map { it.name }.toSet()
        val browserIds =
                availableBrowsers.map { buildBrowserId(it.packageName) }.toSet()
        val hasBrowserTargetsInOrder = savedOrder.any { it.startsWith(BROWSER_ID_PREFIX) }

        val savedDisabled =
                disabledNames.filter { id ->
                    when {
                        id.startsWith(BROWSER_ID_PREFIX) -> id in browserIds
                        else -> id in engineNames
                    }
                }
                        .toMutableSet()

        val packageManager = context.packageManager

        if (!hasPreference) {
            val defaultDisabled =
                    mutableSetOf(
                            SearchEngine.FACEBOOK_MARKETPLACE,
                            SearchEngine.DUCKDUCKGO,
                            SearchEngine.BRAVE,
                            SearchEngine.BING,
                            SearchEngine.AI_MODE,
                            SearchEngine.GOOGLE_DRIVE,
                            SearchEngine.GOOGLE_PHOTOS
                    )

            if (!isPackageInstalled(packageManager, PackageConstants.REDDIT_PACKAGE)) {
                defaultDisabled.add(SearchEngine.REDDIT)
            }
            if (!isPackageInstalled(packageManager, PackageConstants.AMAZON_PACKAGE)) {
                defaultDisabled.add(SearchEngine.AMAZON)
            }
            if (!isPackageInstalled(packageManager, PackageConstants.X_PACKAGE)) {
                defaultDisabled.add(SearchEngine.X)
            }
            if (!isPackageInstalled(packageManager, PackageConstants.YOU_COM_PACKAGE_NAME)) {
                defaultDisabled.add(SearchEngine.YOU_COM)
            }
            if (!isPackageInstalled(packageManager, PackageConstants.STARTPAGE_PACKAGE_NAME)) {
                defaultDisabled.add(SearchEngine.STARTPAGE)
            }

            val filteredDefault =
                    if (hasGemini) {
                        defaultDisabled
                    } else {
                        defaultDisabled.filterNot { it == SearchEngine.DIRECT_SEARCH }.toSet()
                    }

            val disabledIds =
                    filteredDefault.map { it.name }.toMutableSet().apply { addAll(browserIds) }
            userPreferences.setDisabledSearchEngines(disabledIds)
            return disabledIds
        }
        if (!hasBrowserTargetsInOrder) {
            val updated = savedDisabled + browserIds
            if (updated != savedDisabled) {
                userPreferences.setDisabledSearchEngines(updated)
            }
            return updated
        }

        return savedDisabled.toSet()
    }

    private fun parseSearchTargetId(
            id: String,
            availableEngines: List<SearchEngine>,
            browserMap: Map<String, BrowserApp>
    ): SearchTarget? {
        return if (id.startsWith(BROWSER_ID_PREFIX)) {
            val packageName = id.removePrefix(BROWSER_ID_PREFIX)
            val browser = browserMap[packageName] ?: return null
            SearchTarget.Browser(browser)
        } else {
            val engine = availableEngines.firstOrNull { it.name == id } ?: return null
            SearchTarget.Engine(engine)
        }
    }

    private fun mergeMissingEngines(
            order: List<SearchTarget>,
            availableEngines: List<SearchEngine>
    ): List<SearchTarget> {
        val existingEngines =
                order.filterIsInstance<SearchTarget.Engine>().map { it.engine }.toSet()
        val missingEngines =
                availableEngines.filter { it !in existingEngines }.map { SearchTarget.Engine(it) }
        return order + missingEngines
    }

    private fun mergeBrowsers(
            order: List<SearchTarget>,
            availableBrowsers: List<BrowserApp>
    ): List<SearchTarget> {
        val browserMap = availableBrowsers.associateBy { it.packageName }
        val cleanedOrder =
                order.mapNotNull { target ->
                    when (target) {
                        is SearchTarget.Browser -> {
                            val refreshed = browserMap[target.app.packageName]
                            refreshed?.let { SearchTarget.Browser(it) }
                        }
                        else -> target
                    }
                }

        val existingPackages =
                cleanedOrder.filterIsInstance<SearchTarget.Browser>()
                        .map { it.app.packageName }
                        .toSet()
        val missingBrowsers =
                availableBrowsers.filter { it.packageName !in existingPackages }
                        .map { SearchTarget.Browser(it) }

        return insertBrowsersAfterAnchor(cleanedOrder, missingBrowsers)
    }

    private fun insertBrowsersAfterAnchor(
            order: List<SearchTarget>,
            browsersToInsert: List<SearchTarget>
    ): List<SearchTarget> {
        if (browsersToInsert.isEmpty()) return order

        val lastBrowserIndex = order.indexOfLast { it is SearchTarget.Browser }
        val startpageIndex =
                order.indexOfFirst {
                    it is SearchTarget.Engine && it.engine == SearchEngine.STARTPAGE
                }
        val insertIndex =
                when {
                    lastBrowserIndex >= 0 -> lastBrowserIndex + 1
                    startpageIndex >= 0 -> startpageIndex + 1
                    else -> order.size
                }

        return order.toMutableList().apply { addAll(insertIndex, browsersToInsert) }
    }

    private fun buildBrowserId(packageName: String): String = "$BROWSER_ID_PREFIX$packageName"

    private fun loadInstalledBrowsers(): List<BrowserApp> {
        val packageManager = context.packageManager
        val browserCategoryIntent =
                Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_BROWSER) }

        val browserPackages =
                queryBrowserPackages(packageManager, browserCategoryIntent)

        return browserPackages
                .mapNotNull { packageName ->
                    val label =
                            runCatching {
                                val appInfo =
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            packageManager.getApplicationInfo(
                                                    packageName,
                                                    PackageManager.ApplicationInfoFlags.of(0)
                                            )
                                        } else {
                                            @Suppress("DEPRECATION")
                                            packageManager.getApplicationInfo(packageName, 0)
                                        }
                                packageManager.getApplicationLabel(appInfo).toString()
                            }
                                    .getOrNull()
                    val displayLabel =
                            when (packageName) {
                                BRAVE_BROWSER_PACKAGE -> "Brave Browser"
                                else -> label
                            }
                    displayLabel?.let { BrowserApp(packageName = packageName, label = it) }
                }
                .sortedBy { it.label.lowercase() }
    }

    private fun queryBrowserPackages(
            packageManager: PackageManager,
            intent: Intent
    ): Set<String> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
        }
                .getOrDefault(emptyList())
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
    }

    private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return packageManager.getLaunchIntentForPackage(packageName) != null
    }

    private fun applyDirectSearchAvailability(
            order: List<SearchTarget>,
            hasGemini: Boolean
    ): List<SearchTarget> {
        val hasDirect =
                order.any { it is SearchTarget.Engine && it.engine == SearchEngine.DIRECT_SEARCH }
        val withoutDirect =
                order.filterNot {
                    it is SearchTarget.Engine && it.engine == SearchEngine.DIRECT_SEARCH
                }
        return when {
            hasGemini && hasDirect -> order
            hasGemini -> listOf(SearchTarget.Engine(SearchEngine.DIRECT_SEARCH)) + withoutDirect
            else -> withoutDirect
        }
    }
}
