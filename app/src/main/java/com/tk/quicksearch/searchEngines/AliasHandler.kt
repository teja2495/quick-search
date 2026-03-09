package com.tk.quicksearch.searchEngines

import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.tools.directSearch.DirectSearchHandler
import com.tk.quicksearch.searchEngines.ShortcutValidator.isValidShortcutCode
import com.tk.quicksearch.searchEngines.ShortcutValidator.isValidShortcutPrefix
import com.tk.quicksearch.searchEngines.ShortcutValidator.normalizeShortcutCodeInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class AliasHandler(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
    private val directSearchHandler: DirectSearchHandler,
    private val searchTargetsProvider: () -> List<SearchTarget>,
) {
    companion object {
        const val CALCULATOR_ALIAS_FEATURE_ID = "calculator_mode"
        const val DEFAULT_CALCULATOR_ALIAS = "cal"
    }

    private var aliasCodes: Map<String, String> = emptyMap()
    private var aliasEnabled: Map<String, Boolean> = emptyMap()

    private var isInitialized = false

    private fun loadFromPreferences() {
        // Ensure aliases are always enabled (legacy compatibility)
        if (!userPreferences.areAliasesEnabled()) {
            userPreferences.setAliasesEnabled(true)
        }
        val targets =
            searchTargetsProvider().ifEmpty {
                SearchEngine.values().map { SearchTarget.Engine(it) }
            }
        aliasCodes =
            targets.associate { target ->
                val id = target.getId()
                val code =
                    when (target) {
                        is SearchTarget.Engine -> {
                            userPreferences.getAliasCode(target.engine)
                        }

                        is SearchTarget.Browser -> {
                            userPreferences.getAliasCode(id).orEmpty()
                        }

                        is SearchTarget.Custom -> {
                            userPreferences.getAliasCode(id).orEmpty()
                        }
                    }
                id to code
            }
        aliasEnabled =
            targets.associate { target ->
                val id = target.getId()
                val enabled =
                    when (target) {
                        is SearchTarget.Engine -> {
                            userPreferences.isAliasEnabled(target.engine)
                        }

                        is SearchTarget.Browser -> {
                            aliasCodes[id].orEmpty().isNotEmpty()
                        }

                        is SearchTarget.Custom -> {
                            aliasCodes[id].orEmpty().isNotEmpty()
                        }
                    }
                id to enabled
            }

        val persistedCalculatorAlias =
            userPreferences.getAliasCode(CALCULATOR_ALIAS_FEATURE_ID).orEmpty()
        val normalizedCalculatorAlias = normalizeShortcutCodeInput(persistedCalculatorAlias)
        val calculatorAlias =
            when {
                isValidShortcutCode(normalizedCalculatorAlias) -> normalizedCalculatorAlias
                else -> DEFAULT_CALCULATOR_ALIAS
            }
        if (persistedCalculatorAlias != calculatorAlias) {
            userPreferences.setAliasCode(CALCULATOR_ALIAS_FEATURE_ID, calculatorAlias)
        }
        aliasCodes =
            aliasCodes.toMutableMap().apply {
                put(CALCULATOR_ALIAS_FEATURE_ID, calculatorAlias)
            }
        aliasEnabled =
            aliasEnabled.toMutableMap().apply {
                put(CALCULATOR_ALIAS_FEATURE_ID, calculatorAlias.isNotEmpty())
            }
    }

    private fun ensureInitialized() {
        if (!isInitialized) {
            loadFromPreferences()
            isInitialized = true
        }
    }

    fun reloadFromPreferences(): ShortcutsState {
        loadFromPreferences()
        isInitialized = true
        return ShortcutsState(
            shortcutsEnabled = true,
            shortcutCodes = aliasCodes,
            shortcutEnabled = aliasEnabled,
        )
    }

    fun getInitialAliasState(): AliasesState {
        ensureInitialized()
        return AliasesState(
            aliasesEnabled = true,
            aliasCodes = aliasCodes,
            aliasEnabled = aliasEnabled,
        )
    }

    fun getInitialState(): ShortcutsState {
        val aliasState = getInitialAliasState()
        return ShortcutsState(
            shortcutsEnabled = aliasState.aliasesEnabled,
            shortcutCodes = aliasState.aliasCodes,
            shortcutEnabled = aliasState.aliasEnabled,
        )
    }

    fun setAliasesEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            // Aliases are always enabled
            userPreferences.setAliasesEnabled(true)
            uiStateUpdater { it.copy(shortcutsEnabled = true) }
        }
    }

    fun setShortcutsEnabled(enabled: Boolean) = setAliasesEnabled(enabled)

    fun setAliasCode(
        target: SearchTarget,
        code: String,
    ) {
        scope.launch(Dispatchers.IO) {
            val normalizedCode = normalizeShortcutCodeInput(code)
            if (!isValidShortcutCode(normalizedCode)) {
                return@launch
            }
            val id = target.getId()
            // Filter out the current target's alias for validation
            val existingShortcutsForValidation = aliasCodes.filterKeys { it != id }
            if (!isValidShortcutPrefix(normalizedCode, existingShortcutsForValidation)) {
                return@launch
            }
            when (target) {
                is SearchTarget.Engine -> userPreferences.setAliasCode(target.engine, normalizedCode)
                is SearchTarget.Browser -> userPreferences.setAliasCode(id, normalizedCode)
                is SearchTarget.Custom -> userPreferences.setAliasCode(id, normalizedCode)
            }
            aliasCodes = aliasCodes.toMutableMap().apply { put(id, normalizedCode) }
            aliasEnabled = aliasEnabled.toMutableMap().apply { put(id, true) }
            uiStateUpdater {
                it.copy(
                    shortcutCodes = aliasCodes,
                    shortcutEnabled = aliasEnabled,
                )
            }
        }
    }

    fun setAliasCode(
        targetId: String,
        code: String,
    ) {
        scope.launch(Dispatchers.IO) {
            val normalizedCode = normalizeShortcutCodeInput(code)
            if (!isValidShortcutCode(normalizedCode)) {
                return@launch
            }
            val existingShortcutsForValidation = aliasCodes.filterKeys { it != targetId }
            if (!isValidShortcutPrefix(normalizedCode, existingShortcutsForValidation)) {
                return@launch
            }
            userPreferences.setAliasCode(targetId, normalizedCode)
            aliasCodes = aliasCodes.toMutableMap().apply { put(targetId, normalizedCode) }
            aliasEnabled = aliasEnabled.toMutableMap().apply { put(targetId, true) }
            uiStateUpdater {
                it.copy(
                    shortcutCodes = aliasCodes,
                    shortcutEnabled = aliasEnabled,
                )
            }
        }
    }

    fun setShortcutCode(
        target: SearchTarget,
        code: String,
    ) = setAliasCode(target, code)

    fun setAliasEnabled(
        target: SearchTarget,
        enabled: Boolean,
    ) {
        scope.launch(Dispatchers.IO) {
            if (target is SearchTarget.Engine) {
                userPreferences.setAliasEnabled(target.engine, enabled)
            }
            val id = target.getId()
            aliasEnabled = aliasEnabled.toMutableMap().apply { put(id, enabled) }
            uiStateUpdater { it.copy(shortcutEnabled = aliasEnabled) }
        }
    }

    fun setShortcutEnabled(
        target: SearchTarget,
        enabled: Boolean,
    ) = setAliasEnabled(target, enabled)

    fun getAliasCode(target: SearchTarget): String {
        val id = target.getId()
        return aliasCodes[id]
            ?: when (target) {
                is SearchTarget.Engine -> userPreferences.getAliasCode(target.engine)
                is SearchTarget.Browser -> userPreferences.getAliasCode(id).orEmpty()
                is SearchTarget.Custom -> userPreferences.getAliasCode(id).orEmpty()
            }
    }

    fun getAliasCode(
        targetId: String,
        defaultCode: String = "",
    ): String = aliasCodes[targetId] ?: userPreferences.getAliasCode(targetId) ?: defaultCode

    fun getShortcutCode(target: SearchTarget): String = getAliasCode(target)

    fun isAliasEnabled(target: SearchTarget): Boolean {
        val id = target.getId()
        return aliasEnabled[id]
            ?: when (target) {
                is SearchTarget.Engine -> userPreferences.isAliasEnabled(target.engine)
                is SearchTarget.Browser -> getAliasCode(target).isNotEmpty()
                is SearchTarget.Custom -> getAliasCode(target).isNotEmpty()
            }
    }

    fun isShortcutEnabled(target: SearchTarget): Boolean = isAliasEnabled(target)

    fun detectAlias(query: String): Pair<String, AliasTarget>? {
        ensureInitialized()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return null

        val targets =
            searchTargetsProvider().ifEmpty {
                SearchEngine.values().map { SearchTarget.Engine(it) }
            }
        val aliases = mutableMapOf<String, AliasTarget>()
        for (target in targets) {
            if (target is SearchTarget.Engine &&
                target.engine == SearchEngine.DIRECT_SEARCH &&
                directSearchHandler.getGeminiApiKey().isNullOrBlank()
            ) {
                continue
            }
            if (!isAliasEnabled(target)) continue

            val aliasCode = getAliasCode(target).lowercase(Locale.getDefault())
            if (aliasCode.isEmpty()) continue
            aliases[aliasCode] = AliasTarget.Search(target)
        }
        val match = AliasParser.detectSuffixAlias(trimmedQuery, aliases) ?: return null
        return Pair(match.queryWithoutAlias, match.target)
    }

    fun detectShortcut(query: String): Pair<String, SearchTarget>? {
        val aliasMatch = detectAlias(query) ?: return null
        val searchTarget = aliasMatch.second.asSearchTargetOrNull() ?: return null
        return Pair(aliasMatch.first, searchTarget)
    }

    fun detectAliasAtStart(query: String): Pair<String, AliasTarget>? {
        ensureInitialized()
        if (query.isBlank()) return null

        val targets =
            searchTargetsProvider().ifEmpty {
                SearchEngine.values().map { SearchTarget.Engine(it) }
            }
        val aliases = mutableMapOf<String, AliasTarget>()
        for (target in targets) {
            if (target is SearchTarget.Engine &&
                target.engine == SearchEngine.DIRECT_SEARCH &&
                directSearchHandler.getGeminiApiKey().isNullOrBlank()
            ) {
                continue
            }
            if (!isAliasEnabled(target)) continue

            val aliasCode = getAliasCode(target).lowercase(Locale.getDefault())
            if (aliasCode.isEmpty()) continue
            aliases[aliasCode] = AliasTarget.Search(target)
        }
        val calculatorAliasCode = getAliasCode(CALCULATOR_ALIAS_FEATURE_ID, DEFAULT_CALCULATOR_ALIAS)
            .lowercase(Locale.getDefault())
        if (calculatorAliasCode.isNotEmpty()) {
            aliases[calculatorAliasCode] = AliasTarget.Feature(CALCULATOR_ALIAS_FEATURE_ID)
        }
        val match = AliasParser.detectPrefixAlias(query, aliases) ?: return null
        return Pair(match.queryWithoutAlias, match.target)
    }

    fun detectShortcutAtStart(query: String): Pair<String, SearchTarget>? {
        val aliasMatch = detectAliasAtStart(query) ?: return null
        val searchTarget = aliasMatch.second.asSearchTargetOrNull() ?: return null
        return Pair(aliasMatch.first, searchTarget)
    }

    data class AliasesState(
        val aliasesEnabled: Boolean,
        val aliasCodes: Map<String, String>,
        val aliasEnabled: Map<String, Boolean>,
    )

    data class ShortcutsState(
        val shortcutsEnabled: Boolean,
        val shortcutCodes: Map<String, String>,
        val shortcutEnabled: Map<String, Boolean>,
    )
}
