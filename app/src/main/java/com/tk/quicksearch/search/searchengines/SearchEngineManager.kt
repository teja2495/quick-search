package com.tk.quicksearch.search.searchEngines

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles search engine configuration and management.
 */
class SearchEngineManager(
    private val context: Context,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) {
    
    companion object {
        private const val X_PACKAGE = "com.twitter.android"
        private const val YOUTUBE_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
        private const val REDDIT_PACKAGE = "com.reddit.frontpage"
        private const val SPOTIFY_PACKAGE = "com.spotify.music"
        private const val AMAZON_PACKAGE = "com.amazon.mShop.android.shopping"
        private const val YOU_COM_PACKAGE = "com.you.browser"
        private const val STARTPAGE_PACKAGE = "com.startpage.app"
    }

    var searchEngineOrder: List<SearchEngine> = loadSearchEngineOrder()
        private set

    var disabledSearchEngines: Set<SearchEngine> = loadDisabledSearchEngines()
        private set

    var isSearchEngineCompactMode: Boolean = userPreferences.isSearchEngineCompactMode()
        private set

    fun getEnabledSearchEngines(): List<SearchEngine> {
        return searchEngineOrder.filter { it !in disabledSearchEngines }
    }

    fun setSearchEngineEnabled(engine: SearchEngine, enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            // Check if trying to enable Direct Search without Gemini API key
            if (engine == SearchEngine.DIRECT_SEARCH && enabled) {
                val hasGeminiApiKey = !userPreferences.getGeminiApiKey().isNullOrBlank()
                if (!hasGeminiApiKey) {
                    // Don't enable the search engine without Gemini API key
                    return@launch
                }
            }

            val disabled = disabledSearchEngines.toMutableSet()
            if (enabled) {
                disabled.remove(engine)
            } else {
                disabled.add(engine)
            }
            disabledSearchEngines = disabled
            userPreferences.setDisabledSearchEngines(disabled.map { it.name }.toSet())
            onStateUpdate { state ->
                state.copy(disabledSearchEngines = disabledSearchEngines)
            }
        }
    }

    fun reorderSearchEngines(newOrder: List<SearchEngine>) {
        scope.launch(Dispatchers.IO) {
            searchEngineOrder = newOrder
            userPreferences.setSearchEngineOrder(newOrder.map { it.name })
            onStateUpdate { state ->
                state.copy(searchEngineOrder = searchEngineOrder)
            }
        }
    }

    fun setSearchEngineCompactMode(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            isSearchEngineCompactMode = enabled
            userPreferences.setSearchEngineCompactMode(enabled)
            onStateUpdate { state ->
                state.copy(
                    isSearchEngineCompactMode = isSearchEngineCompactMode,
                    showSearchEngineOnboarding = enabled && !userPreferences.hasSeenSearchEngineOnboarding()
                )
            }
        }
    }

    fun updateSearchEnginesForGemini(hasGemini: Boolean) {
        val updatedOrder = applyDirectSearchAvailability(searchEngineOrder, hasGemini)
        searchEngineOrder = updatedOrder
        if (!hasGemini) {
            disabledSearchEngines = disabledSearchEngines.filterNot { it == SearchEngine.DIRECT_SEARCH }.toSet()
        }
        userPreferences.setSearchEngineOrder(searchEngineOrder.map { it.name })
        userPreferences.setDisabledSearchEngines(disabledSearchEngines.map { it.name }.toSet())
        onStateUpdate { state ->
            state.copy(
                searchEngineOrder = searchEngineOrder,
                disabledSearchEngines = disabledSearchEngines
            )
        }
    }

    private fun loadSearchEngineOrder(): List<SearchEngine> {
        val savedOrder = userPreferences.getSearchEngineOrder()
        val hasGemini = !userPreferences.getGeminiApiKey().isNullOrBlank()
        val allEngines = if (hasGemini) {
            SearchEngine.values().toList()
        } else {
            SearchEngine.values().filterNot { it == SearchEngine.DIRECT_SEARCH }
        }

        if (savedOrder.isEmpty()) {
            // First time - use default order with all available engines
            val defaultOrder = allEngines
            userPreferences.setSearchEngineOrder(defaultOrder.map { it.name })
            return defaultOrder
        }

        // Merge saved order with any new engines that might have been added
        val savedEngines = savedOrder.mapNotNull { name ->
            SearchEngine.values().find { it.name == name }
        }
        val mergedSaved = applyDirectSearchAvailability(savedEngines, hasGemini)
        val newEngines = allEngines.filter { it !in mergedSaved }
        val finalOrder = mergedSaved + newEngines

        // Save the updated order if new engines were added
        if (newEngines.isNotEmpty()) {
            userPreferences.setSearchEngineOrder(finalOrder.map { it.name })
        }

        return finalOrder
    }

    private fun loadDisabledSearchEngines(): Set<SearchEngine> {
        val hasPreference = userPreferences.hasDisabledSearchEnginesPreference()
        val disabledNames = userPreferences.getDisabledSearchEngines()
        val hasGemini = !userPreferences.getGeminiApiKey().isNullOrBlank()
        val savedDisabled = disabledNames.mapNotNull { name ->
            SearchEngine.values().find { it.name == name }
        }.toMutableSet()
        
        val packageManager = context.packageManager
        
        // If no saved preferences (first-time user), set default disabled engines
        if (!hasPreference) {
            // Always disabled by default
            savedDisabled.addAll(listOf(
                SearchEngine.FACEBOOK_MARKETPLACE,
                SearchEngine.DUCKDUCKGO,
                SearchEngine.BRAVE,
                SearchEngine.BING,
                SearchEngine.AI_MODE,
                SearchEngine.GOOGLE_DRIVE,
                SearchEngine.GOOGLE_PHOTOS
            ))
            
            // Disable app-based engines if apps are not installed
            if (!isPackageInstalled(packageManager, REDDIT_PACKAGE)) {
                savedDisabled.add(SearchEngine.REDDIT)
            }
            if (!isPackageInstalled(packageManager, AMAZON_PACKAGE)) {
                savedDisabled.add(SearchEngine.AMAZON)
            }
            if (!isPackageInstalled(packageManager, X_PACKAGE)) {
                savedDisabled.add(SearchEngine.X)
            }
            if (!isPackageInstalled(packageManager, YOUTUBE_MUSIC_PACKAGE)) {
                savedDisabled.add(SearchEngine.YOUTUBE_MUSIC)
            }
            if (!isPackageInstalled(packageManager, SPOTIFY_PACKAGE)) {
                savedDisabled.add(SearchEngine.SPOTIFY)
            }
            if (!isPackageInstalled(packageManager, YOU_COM_PACKAGE)) {
                savedDisabled.add(SearchEngine.YOU_COM)
            }
            if (!isPackageInstalled(packageManager, STARTPAGE_PACKAGE)) {
                savedDisabled.add(SearchEngine.STARTPAGE)
            }
            
            // Save default disabled engines for new users
            val finalDisabled = if (hasGemini) {
                savedDisabled
            } else {
                savedDisabled.filterNot { it == SearchEngine.DIRECT_SEARCH }.toSet()
            }
            userPreferences.setDisabledSearchEngines(finalDisabled.map { it.name }.toSet())
            return finalDisabled
        }
        
        // For existing users (who have saved preferences), respect their choices
        // This includes users who enabled all engines (empty disabled set)
        return if (hasGemini) {
            savedDisabled
        } else {
            savedDisabled.filterNot { it == SearchEngine.DIRECT_SEARCH }.toSet()
        }
    }
    
    private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return packageManager.getLaunchIntentForPackage(packageName) != null
    }

    private fun applyDirectSearchAvailability(
        order: List<SearchEngine>,
        hasGemini: Boolean
    ): List<SearchEngine> {
        val hasDirect = order.contains(SearchEngine.DIRECT_SEARCH)
        val withoutDirect = order.filterNot { it == SearchEngine.DIRECT_SEARCH }
        return when {
            hasGemini && hasDirect -> order
            hasGemini -> listOf(SearchEngine.DIRECT_SEARCH) + withoutDirect
            else -> withoutDirect
        }
    }
}
