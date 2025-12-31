package com.tk.quicksearch.search.searchengines

import android.content.Context
import android.content.pm.PackageManager
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
    }

    var searchEngineOrder: List<SearchEngine> = loadSearchEngineOrder()
        private set

    var disabledSearchEngines: Set<SearchEngine> = loadDisabledSearchEngines()
        private set

    var searchEngineSectionEnabled: Boolean = userPreferences.isSearchEngineSectionEnabled()
        private set

    fun getEnabledSearchEngines(): List<SearchEngine> {
        return searchEngineOrder.filter { it !in disabledSearchEngines }
    }

    fun setSearchEngineEnabled(engine: SearchEngine, enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
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

    fun setSearchEngineSectionEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            searchEngineSectionEnabled = enabled
            userPreferences.setSearchEngineSectionEnabled(enabled)
            onStateUpdate { state ->
                state.copy(searchEngineSectionEnabled = searchEngineSectionEnabled)
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
            // First time - use default order
            val defaultOrder = listOf(
                SearchEngine.GOOGLE,
                SearchEngine.CHATGPT,
                SearchEngine.PERPLEXITY,
                SearchEngine.AMAZON,
                SearchEngine.YOUTUBE,
                SearchEngine.GOOGLE_PLAY,
                SearchEngine.GOOGLE_DRIVE,
                SearchEngine.GOOGLE_PHOTOS,
                SearchEngine.GOOGLE_MAPS,
                SearchEngine.AI_MODE
            )
            return applyDirectSearchAvailability(defaultOrder, hasGemini)
        }

        // Merge saved order with any new engines that might have been added
        val savedEngines = savedOrder.mapNotNull { name ->
            SearchEngine.values().find { it.name == name }
        }
        val mergedSaved = applyDirectSearchAvailability(savedEngines, hasGemini)
        val newEngines = allEngines.filter { it !in mergedSaved }
        return mergedSaved + newEngines
    }

    private fun loadDisabledSearchEngines(): Set<SearchEngine> {
        val disabledNames = userPreferences.getDisabledSearchEngines()
        val hasGemini = !userPreferences.getGeminiApiKey().isNullOrBlank()
        val savedDisabled = disabledNames.mapNotNull { name ->
            SearchEngine.values().find { it.name == name }
        }.toMutableSet()
        
        val packageManager = context.packageManager
        
        // If no saved preferences, set default disabled engines
        if (disabledNames.isEmpty()) {
            // Always disabled by default
            savedDisabled.addAll(listOf(
                SearchEngine.FACEBOOK_MARKETPLACE,
                SearchEngine.YOU_COM,
                SearchEngine.BING,
                SearchEngine.BRAVE
            ))
            
            // Disable app-based engines if apps are not installed
            if (!isPackageInstalled(packageManager, X_PACKAGE)) {
                savedDisabled.add(SearchEngine.X)
            }
            if (!isPackageInstalled(packageManager, YOUTUBE_MUSIC_PACKAGE)) {
                savedDisabled.add(SearchEngine.YOUTUBE_MUSIC)
            }
            if (!isPackageInstalled(packageManager, REDDIT_PACKAGE)) {
                savedDisabled.add(SearchEngine.REDDIT)
            }
            if (!isPackageInstalled(packageManager, SPOTIFY_PACKAGE)) {
                savedDisabled.add(SearchEngine.SPOTIFY)
            }
            
            // Save default disabled engines for new users
            val finalDisabled = if (hasGemini) {
                savedDisabled
            } else {
                savedDisabled.filterNot { it == SearchEngine.DIRECT_SEARCH }.toSet()
            }
            userPreferences.setDisabledSearchEngines(finalDisabled.map { it.name }.toSet())
            return finalDisabled
        } else {
            // For existing users, check if app-based engines should be disabled
            // This handles the case where apps are uninstalled after initial setup
            // Note: We don't save this automatically to avoid overriding user preferences
            if (!isPackageInstalled(packageManager, X_PACKAGE)) {
                savedDisabled.add(SearchEngine.X)
            }
            if (!isPackageInstalled(packageManager, YOUTUBE_MUSIC_PACKAGE)) {
                savedDisabled.add(SearchEngine.YOUTUBE_MUSIC)
            }
            if (!isPackageInstalled(packageManager, REDDIT_PACKAGE)) {
                savedDisabled.add(SearchEngine.REDDIT)
            }
            if (!isPackageInstalled(packageManager, SPOTIFY_PACKAGE)) {
                savedDisabled.add(SearchEngine.SPOTIFY)
            }
        }
        
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
