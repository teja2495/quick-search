package com.tk.quicksearch.search.handlers

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.searchengines.DirectSearchHandler
import com.tk.quicksearch.search.handlers.ShortcutValidator.normalizeShortcutCodeInput
import com.tk.quicksearch.search.handlers.ShortcutValidator.isValidShortcutCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class ShortcutHandler(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
    private val directSearchHandler: DirectSearchHandler
) {
    private var shortcutCodes: Map<SearchEngine, String> = emptyMap()
    private var shortcutEnabled: Map<SearchEngine, Boolean> = emptyMap()

    init {
        // Ensure shortcuts are always enabled (legacy compatibility)
        if (!userPreferences.areShortcutsEnabled()) {
            userPreferences.setShortcutsEnabled(true)
        }
        shortcutCodes = userPreferences.getAllShortcutCodes()
        shortcutEnabled = SearchEngine.values().associateWith {
            userPreferences.isShortcutEnabled(it)
        }
    }
    
    fun getInitialState(): ShortcutsState {
        return ShortcutsState(
            shortcutsEnabled = true,
            shortcutCodes = shortcutCodes,
            shortcutEnabled = shortcutEnabled
        )
    }

    fun setShortcutsEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            // Shortcuts are always enabled
            userPreferences.setShortcutsEnabled(true)
            uiStateUpdater {
                it.copy(shortcutsEnabled = true)
            }
        }
    }

    fun setShortcutCode(engine: SearchEngine, code: String) {
        scope.launch(Dispatchers.IO) {
            val normalizedCode = normalizeShortcutCodeInput(code)
            if (!isValidShortcutCode(normalizedCode)) {
                return@launch
            }
            userPreferences.setShortcutCode(engine, normalizedCode)
            shortcutCodes = shortcutCodes.toMutableMap().apply { put(engine, normalizedCode) }
            uiStateUpdater { 
                it.copy(shortcutCodes = shortcutCodes)
            }
        }
    }

    fun setShortcutEnabled(engine: SearchEngine, enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setShortcutEnabled(engine, enabled)
            shortcutEnabled = shortcutEnabled.toMutableMap().apply { put(engine, enabled) }
            uiStateUpdater { 
                it.copy(shortcutEnabled = shortcutEnabled)
            }
        }
    }
    
    fun getShortcutCode(engine: SearchEngine): String {
        return shortcutCodes[engine] ?: userPreferences.getShortcutCode(engine)
    }

    fun isShortcutEnabled(engine: SearchEngine): Boolean {
        return shortcutEnabled[engine] ?: userPreferences.isShortcutEnabled(engine)
    }

    fun detectShortcut(query: String): Pair<String, SearchEngine>? {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return null

        // Look for shortcut at the end of the query (e.g., "search query ggl")
        // Shortcut should be separated by space
        val words = trimmedQuery.split("\\s+".toRegex())
        if (words.size < 2) return null

        val lastWord = words.last().lowercase(Locale.getDefault())
        
        // Check each enabled search engine for matching shortcut
        for (engine in SearchEngine.values()) {
            if (engine == SearchEngine.DIRECT_SEARCH && directSearchHandler.getGeminiApiKey().isNullOrBlank()) continue
            if (!isShortcutEnabled(engine)) continue
            
            val shortcutCode = getShortcutCode(engine).lowercase(Locale.getDefault())
            if (lastWord == shortcutCode) {
                // Extract query without the shortcut
                val queryWithoutShortcut = words.dropLast(1).joinToString(" ")
                return Pair(queryWithoutShortcut, engine)
            }
        }
        
        return null
    }

    fun detectShortcutAtStart(query: String): Pair<String, SearchEngine>? {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return null

        // Look for shortcut at the start of the query
        val words = trimmedQuery.split("\\s+".toRegex())
        if (words.isEmpty()) return null

        val firstWord = words.first().lowercase(Locale.getDefault())
        
        // Check each enabled search engine for matching shortcut
        for (engine in SearchEngine.values()) {
            if (engine == SearchEngine.DIRECT_SEARCH && directSearchHandler.getGeminiApiKey().isNullOrBlank()) continue
            if (!isShortcutEnabled(engine)) continue
            
            val shortcutCode = getShortcutCode(engine).lowercase(Locale.getDefault())
            if (firstWord == shortcutCode) {
                // Extract query without the shortcut
                val queryWithoutShortcut = words.drop(1).joinToString(" ")
                return Pair(queryWithoutShortcut, engine)
            }
        }
        
        return null
    }

    
    data class ShortcutsState(
        val shortcutsEnabled: Boolean,
        val shortcutCodes: Map<SearchEngine, String>,
        val shortcutEnabled: Map<SearchEngine, Boolean>
    )
}
