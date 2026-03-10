package com.tk.quicksearch.searchEngines

import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.tools.directSearch.DirectSearchHandler
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.isValidShortcutCode
import com.tk.quicksearch.searchEngines.AliasValidator.isValidShortcutPrefix
import com.tk.quicksearch.searchEngines.AliasValidator.normalizeShortcutCodeInput
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
        const val SEARCH_SECTION_APPS_ALIAS_ID = "search_section_apps"
        const val SEARCH_SECTION_APP_SHORTCUTS_ALIAS_ID = "search_section_app_shortcuts"
        const val SEARCH_SECTION_CONTACTS_ALIAS_ID = "search_section_contacts"
        const val SEARCH_SECTION_FILES_ALIAS_ID = "search_section_files"
        const val SEARCH_SECTION_SETTINGS_ALIAS_ID = "search_section_settings"
        val SEARCH_SECTION_ALIAS_IDS =
            setOf(
                SEARCH_SECTION_APPS_ALIAS_ID,
                SEARCH_SECTION_APP_SHORTCUTS_ALIAS_ID,
                SEARCH_SECTION_CONTACTS_ALIAS_ID,
                SEARCH_SECTION_FILES_ALIAS_ID,
                SEARCH_SECTION_SETTINGS_ALIAS_ID,
            )
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
        aliasCodes =
            aliasCodes.toMutableMap().apply {
                targets.forEach { target ->
                    if (target is SearchTarget.Engine && aliasEnabled[target.getId()] == false) {
                        put(target.getId(), "")
                    }
                }
            }

        val persistedCalculatorAlias =
            userPreferences.getAliasCode(CALCULATOR_ALIAS_FEATURE_ID).orEmpty()
        val normalizedCalculatorAlias = normalizeShortcutCodeInput(persistedCalculatorAlias)
        val isCalculatorAliasEnabled =
            userPreferences.isAliasEnabled(
                CALCULATOR_ALIAS_FEATURE_ID,
                defaultValue = true,
            )
        val calculatorAlias =
            when {
                !isCalculatorAliasEnabled -> ""
                isValidShortcutCode(normalizedCalculatorAlias) -> normalizedCalculatorAlias
                else -> DEFAULT_CALCULATOR_ALIAS
            }
        if (isCalculatorAliasEnabled && persistedCalculatorAlias != calculatorAlias) {
            userPreferences.setAliasCode(CALCULATOR_ALIAS_FEATURE_ID, calculatorAlias)
        } else if (!isCalculatorAliasEnabled && persistedCalculatorAlias.isNotBlank()) {
            userPreferences.clearAliasCode(CALCULATOR_ALIAS_FEATURE_ID)
        }
        aliasCodes =
            aliasCodes.toMutableMap().apply {
                put(CALCULATOR_ALIAS_FEATURE_ID, calculatorAlias)
            }
        aliasEnabled =
            aliasEnabled.toMutableMap().apply {
                put(CALCULATOR_ALIAS_FEATURE_ID, isCalculatorAliasEnabled && calculatorAlias.isNotEmpty())
            }

        SEARCH_SECTION_ALIAS_IDS.forEach { sectionAliasId ->
            val aliasCode = userPreferences.getAliasCodeAllowSingleChar(sectionAliasId).orEmpty()
            aliasCodes =
                aliasCodes.toMutableMap().apply {
                    put(sectionAliasId, aliasCode)
                }
            aliasEnabled =
                aliasEnabled.toMutableMap().apply {
                    put(sectionAliasId, aliasCode.isNotEmpty())
                }
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

    private fun setAliasInternal(
        targetId: String,
        code: String,
        target: SearchTarget?,
    ) {
        scope.launch(Dispatchers.IO) {
            val normalizedCode = normalizeShortcutCodeInput(code)
            val engineTarget = (target as? SearchTarget.Engine)?.engine
                ?: SearchEngine.values().firstOrNull { it.name == targetId }
            val isSearchSectionAlias = targetId in SEARCH_SECTION_ALIAS_IDS

            if (normalizedCode.isEmpty()) {
                userPreferences.clearAliasCode(targetId)
                if (engineTarget != null) {
                    userPreferences.setAliasEnabled(engineTarget, false)
                } else {
                    userPreferences.setAliasEnabled(targetId, false)
                }
                aliasCodes = aliasCodes.toMutableMap().apply { put(targetId, "") }
                aliasEnabled = aliasEnabled.toMutableMap().apply { put(targetId, false) }
                uiStateUpdater {
                    it.copy(
                        shortcutCodes = aliasCodes,
                        shortcutEnabled = aliasEnabled,
                    )
                }
                return@launch
            }

            val isValidCode =
                if (isSearchSectionAlias) {
                    normalizedCode.isNotEmpty()
                } else {
                    isValidShortcutCode(normalizedCode)
                }
            if (!isValidCode) {
                return@launch
            }

            val existingAliasesForValidation =
                aliasCodes
                    .filterKeys { it != targetId }
                    .filterValues { it.isNotBlank() }
            val isConflictFree =
                if (isSearchSectionAlias) {
                    !hasExactAliasConflict(normalizedCode, existingAliasesForValidation)
                } else {
                    isValidShortcutPrefix(normalizedCode, existingAliasesForValidation)
                }
            if (!isConflictFree) {
                return@launch
            }

            if (engineTarget != null) {
                userPreferences.setAliasCode(engineTarget, normalizedCode)
                userPreferences.setAliasEnabled(engineTarget, true)
            } else if (isSearchSectionAlias) {
                userPreferences.setAliasCodeAllowSingleChar(targetId, normalizedCode)
                userPreferences.setAliasEnabled(targetId, true)
            } else {
                userPreferences.setAliasCode(targetId, normalizedCode)
                if (targetId == CALCULATOR_ALIAS_FEATURE_ID) {
                    userPreferences.setAliasEnabled(targetId, true)
                }
            }
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

    fun getAlias(target: SearchTarget): String {
        val id = target.getId()
        return aliasCodes[id]
            ?: when (target) {
                is SearchTarget.Engine -> userPreferences.getAliasCode(target.engine)
                is SearchTarget.Browser -> userPreferences.getAliasCode(id).orEmpty()
                is SearchTarget.Custom -> userPreferences.getAliasCode(id).orEmpty()
            }
    }

    fun getAlias(
        targetId: String,
        defaultCode: String = "",
    ): String = aliasCodes[targetId] ?: userPreferences.getAliasCode(targetId) ?: defaultCode

    fun setAlias(
        target: SearchTarget,
        code: String,
    ) = setAliasInternal(target.getId(), code, target)

    fun setAlias(
        targetId: String,
        code: String,
    ) = setAliasInternal(targetId, code, null)

    fun isAliasEnabled(target: SearchTarget): Boolean {
        val id = target.getId()
        return aliasEnabled[id]
            ?: when (target) {
                is SearchTarget.Engine -> userPreferences.isAliasEnabled(target.engine)
                is SearchTarget.Browser -> getAlias(target).isNotEmpty()
                is SearchTarget.Custom -> getAlias(target).isNotEmpty()
            }
    }

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

            val aliasCode = getAlias(target).lowercase(Locale.getDefault())
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

            val aliasCode = getAlias(target).lowercase(Locale.getDefault())
            if (aliasCode.isEmpty()) continue
            aliases[aliasCode] = AliasTarget.Search(target)
        }
        val calculatorAliasCode = getAlias(CALCULATOR_ALIAS_FEATURE_ID)
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
