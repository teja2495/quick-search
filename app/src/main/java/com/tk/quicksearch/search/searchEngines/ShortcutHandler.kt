package com.tk.quicksearch.search.searchEngines

import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.directSearch.DirectSearchHandler
import com.tk.quicksearch.search.searchEngines.ShortcutValidator.isValidShortcutCode
import com.tk.quicksearch.search.searchEngines.ShortcutValidator.isValidShortcutPrefix
import com.tk.quicksearch.search.searchEngines.ShortcutValidator.normalizeShortcutCodeInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class ShortcutHandler(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
    private val directSearchHandler: DirectSearchHandler,
    private val searchTargetsProvider: () -> List<SearchTarget>,
) {
    private var shortcutCodes: Map<String, String> = emptyMap()
    private var shortcutEnabled: Map<String, Boolean> = emptyMap()

    private var isInitialized = false

    private fun ensureInitialized() {
        if (!isInitialized) {
            // Ensure shortcuts are always enabled (legacy compatibility)
            if (!userPreferences.areShortcutsEnabled()) {
                userPreferences.setShortcutsEnabled(true)
            }
            val targets =
                searchTargetsProvider().ifEmpty {
                    SearchEngine.values().map { SearchTarget.Engine(it) }
                }
            shortcutCodes =
                targets.associate { target ->
                    val id = target.getId()
                    val code =
                        when (target) {
                            is SearchTarget.Engine -> {
                                userPreferences.getShortcutCode(target.engine)
                            }

                            is SearchTarget.Browser -> {
                                userPreferences.getShortcutCode(id).orEmpty()
                            }
                        }
                    id to code
                }
            shortcutEnabled =
                targets.associate { target ->
                    val id = target.getId()
                    val enabled =
                        when (target) {
                            is SearchTarget.Engine -> {
                                userPreferences.isShortcutEnabled(target.engine)
                            }

                            is SearchTarget.Browser -> {
                                shortcutCodes[id].orEmpty().isNotEmpty()
                            }
                        }
                    id to enabled
                }
            isInitialized = true
        }
    }

    fun getInitialState(): ShortcutsState {
        ensureInitialized()
        return ShortcutsState(
            shortcutsEnabled = true,
            shortcutCodes = shortcutCodes,
            shortcutEnabled = shortcutEnabled,
        )
    }

    fun setShortcutsEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            // Shortcuts are always enabled
            userPreferences.setShortcutsEnabled(true)
            uiStateUpdater { it.copy(shortcutsEnabled = true) }
        }
    }

    fun setShortcutCode(
        target: SearchTarget,
        code: String,
    ) {
        scope.launch(Dispatchers.IO) {
            val normalizedCode = normalizeShortcutCodeInput(code)
            if (!isValidShortcutCode(normalizedCode)) {
                return@launch
            }
            val id = target.getId()
            // Filter out the current target's shortcut for validation
            val existingShortcutsForValidation = shortcutCodes.filterKeys { it != id }
            if (!isValidShortcutPrefix(normalizedCode, existingShortcutsForValidation)) {
                return@launch
            }
            when (target) {
                is SearchTarget.Engine -> userPreferences.setShortcutCode(target.engine, normalizedCode)
                is SearchTarget.Browser -> userPreferences.setShortcutCode(id, normalizedCode)
            }
            shortcutCodes = shortcutCodes.toMutableMap().apply { put(id, normalizedCode) }
            shortcutEnabled = shortcutEnabled.toMutableMap().apply { put(id, true) }
            uiStateUpdater {
                it.copy(
                    shortcutCodes = shortcutCodes,
                    shortcutEnabled = shortcutEnabled,
                )
            }
        }
    }

    fun setShortcutEnabled(
        target: SearchTarget,
        enabled: Boolean,
    ) {
        scope.launch(Dispatchers.IO) {
            if (target is SearchTarget.Engine) {
                userPreferences.setShortcutEnabled(target.engine, enabled)
            }
            val id = target.getId()
            shortcutEnabled = shortcutEnabled.toMutableMap().apply { put(id, enabled) }
            uiStateUpdater { it.copy(shortcutEnabled = shortcutEnabled) }
        }
    }

    fun getShortcutCode(target: SearchTarget): String {
        val id = target.getId()
        return shortcutCodes[id]
            ?: when (target) {
                is SearchTarget.Engine -> userPreferences.getShortcutCode(target.engine)
                is SearchTarget.Browser -> userPreferences.getShortcutCode(id).orEmpty()
            }
    }

    fun isShortcutEnabled(target: SearchTarget): Boolean {
        val id = target.getId()
        return shortcutEnabled[id]
            ?: when (target) {
                is SearchTarget.Engine -> userPreferences.isShortcutEnabled(target.engine)
                is SearchTarget.Browser -> getShortcutCode(target).isNotEmpty()
            }
    }

    fun detectShortcut(query: String): Pair<String, SearchTarget>? {
        ensureInitialized()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return null

        // Look for shortcut at the end of the query (e.g., "search query ggl")
        // Shortcut should be separated by space
        val words = trimmedQuery.split("\\s+".toRegex())
        if (words.size < 2) return null

        val lastWord = words.last().lowercase(Locale.getDefault())

        // Check each enabled search target for matching shortcut
        val targets =
            searchTargetsProvider().ifEmpty {
                SearchEngine.values().map { SearchTarget.Engine(it) }
            }
        for (target in targets) {
            if (target is SearchTarget.Engine &&
                target.engine == SearchEngine.DIRECT_SEARCH &&
                directSearchHandler.getGeminiApiKey().isNullOrBlank()
            ) {
                continue
            }
            if (!isShortcutEnabled(target)) continue

            val shortcutCode = getShortcutCode(target).lowercase(Locale.getDefault())
            if (shortcutCode.isEmpty()) continue
            if (lastWord == shortcutCode) {
                // Extract query without the shortcut
                val queryWithoutShortcut = words.dropLast(1).joinToString(" ")
                return Pair(queryWithoutShortcut, target)
            }
        }

        return null
    }

    fun detectShortcutAtStart(query: String): Pair<String, SearchTarget>? {
        ensureInitialized()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return null

        // Look for shortcut at the start of the query
        val words = trimmedQuery.split("\\s+".toRegex())
        if (words.isEmpty()) return null

        val firstWord = words.first().lowercase(Locale.getDefault())

        // Check each enabled search target for matching shortcut
        val targets =
            searchTargetsProvider().ifEmpty {
                SearchEngine.values().map { SearchTarget.Engine(it) }
            }
        for (target in targets) {
            if (target is SearchTarget.Engine &&
                target.engine == SearchEngine.DIRECT_SEARCH &&
                directSearchHandler.getGeminiApiKey().isNullOrBlank()
            ) {
                continue
            }
            if (!isShortcutEnabled(target)) continue

            val shortcutCode = getShortcutCode(target).lowercase(Locale.getDefault())
            if (shortcutCode.isEmpty()) continue
            if (firstWord == shortcutCode) {
                // Extract query without the shortcut
                val queryWithoutShortcut = words.drop(1).joinToString(" ")
                return Pair(queryWithoutShortcut, target)
            }
        }

        return null
    }

    data class ShortcutsState(
        val shortcutsEnabled: Boolean,
        val shortcutCodes: Map<String, String>,
        val shortcutEnabled: Map<String, Boolean>,
    )
}
