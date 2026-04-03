package com.tk.quicksearch.searchEngines

import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.tools.directSearch.DirectSearchHandler
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
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
    data class FeatureAliasDefinition(
        val featureId: String,
        val toolType: SearchToolType? = null,
        val standaloneMode: StandaloneFeatureAliasMode? = null,
        val requiresGeminiApiKey: Boolean = false,
    )

    enum class StandaloneFeatureAliasMode {
        CURRENCY_CONVERTER,
        WORD_CLOCK,
        DICTIONARY,
    }

    companion object {
        const val CALCULATOR_ALIAS_FEATURE_ID = "calculator_mode"
        const val UNIT_CONVERTER_ALIAS_FEATURE_ID = "unit_converter_mode"
        const val DATE_CALCULATOR_ALIAS_FEATURE_ID = "date_calculator_mode"
        const val CURRENCY_CONVERTER_ALIAS_FEATURE_ID = "currency_converter_mode"
        const val WORD_CLOCK_ALIAS_FEATURE_ID = "word_clock_mode"
        const val DICTIONARY_ALIAS_FEATURE_ID = "dictionary_mode"
        const val SEARCH_SECTION_APPS_ALIAS_ID = SearchSectionRegistry.SEARCH_SECTION_APPS_ALIAS_ID
        const val SEARCH_SECTION_APP_SHORTCUTS_ALIAS_ID =
            SearchSectionRegistry.SEARCH_SECTION_APP_SHORTCUTS_ALIAS_ID
        const val SEARCH_SECTION_CONTACTS_ALIAS_ID =
            SearchSectionRegistry.SEARCH_SECTION_CONTACTS_ALIAS_ID
        const val SEARCH_SECTION_FILES_ALIAS_ID = SearchSectionRegistry.SEARCH_SECTION_FILES_ALIAS_ID
        const val SEARCH_SECTION_SETTINGS_ALIAS_ID =
            SearchSectionRegistry.SEARCH_SECTION_SETTINGS_ALIAS_ID
        const val SEARCH_SECTION_CALENDAR_ALIAS_ID =
            SearchSectionRegistry.SEARCH_SECTION_CALENDAR_ALIAS_ID
        const val SEARCH_SECTION_APP_SETTINGS_ALIAS_ID =
            SearchSectionRegistry.SEARCH_SECTION_APP_SETTINGS_ALIAS_ID
        // Centralized feature-alias trigger map.
        // Add new feature aliases here so trigger behavior stays in one place.
        val FEATURE_ALIAS_DEFINITIONS: List<FeatureAliasDefinition> =
            listOf(
                FeatureAliasDefinition(
                    featureId = CALCULATOR_ALIAS_FEATURE_ID,
                    toolType = SearchToolType.CALCULATOR,
                ),
                FeatureAliasDefinition(
                    featureId = UNIT_CONVERTER_ALIAS_FEATURE_ID,
                    toolType = SearchToolType.UNIT_CONVERTER,
                ),
                FeatureAliasDefinition(
                    featureId = DATE_CALCULATOR_ALIAS_FEATURE_ID,
                    toolType = SearchToolType.DATE_CALCULATOR,
                ),
                FeatureAliasDefinition(
                    featureId = CURRENCY_CONVERTER_ALIAS_FEATURE_ID,
                    standaloneMode = StandaloneFeatureAliasMode.CURRENCY_CONVERTER,
                    requiresGeminiApiKey = true,
                ),
                FeatureAliasDefinition(
                    featureId = WORD_CLOCK_ALIAS_FEATURE_ID,
                    standaloneMode = StandaloneFeatureAliasMode.WORD_CLOCK,
                    requiresGeminiApiKey = true,
                ),
                FeatureAliasDefinition(
                    featureId = DICTIONARY_ALIAS_FEATURE_ID,
                    standaloneMode = StandaloneFeatureAliasMode.DICTIONARY,
                    requiresGeminiApiKey = true,
                ),
            )

        val TOOL_ALIAS_IDS = FEATURE_ALIAS_DEFINITIONS.mapTo(linkedSetOf()) { it.featureId }
        val SEARCH_SECTION_ALIAS_IDS = SearchSectionRegistry.searchSectionAliasIds
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
                val enabled = aliasCodes[id].orEmpty().isNotEmpty()
                id to enabled
            }
        TOOL_ALIAS_IDS.forEach(::loadToolAlias)

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

    private fun loadToolAlias(toolAliasId: String) {
        val persistedAlias = userPreferences.getAliasCode(toolAliasId).orEmpty()
        val normalizedAlias =
            if (isValidGeneralAliasCode(persistedAlias)) {
                normalizeShortcutCodeInput(persistedAlias)
            } else {
                ""
            }
        if (persistedAlias.isNotBlank() && normalizedAlias.isEmpty()) {
            userPreferences.clearAliasCode(toolAliasId)
        } else if (persistedAlias != normalizedAlias && normalizedAlias.isNotEmpty()) {
            userPreferences.setAliasCodeAllowSingleChar(toolAliasId, normalizedAlias)
        }
        aliasCodes =
            aliasCodes.toMutableMap().apply {
                put(toolAliasId, normalizedAlias)
            }
        aliasEnabled =
            aliasEnabled.toMutableMap().apply {
                put(toolAliasId, normalizedAlias.isNotEmpty())
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
                if (engineTarget != null) {
                    userPreferences.setAliasCode(engineTarget, "")
                } else {
                    userPreferences.clearAliasCode(targetId)
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

            val isValidCode = isValidGeneralAliasCode(normalizedCode)
            if (!isValidCode) {
                return@launch
            }

            val existingAliasesForValidation =
                aliasCodes
                    .filterKeys { it != targetId }
                    .filterValues { it.isNotBlank() }
            val isConflictFree = !hasExactAliasConflict(normalizedCode, existingAliasesForValidation)
            if (!isConflictFree) {
                return@launch
            }

            if (engineTarget != null) {
                userPreferences.setAliasCode(engineTarget, normalizedCode)
            } else if (isSearchSectionAlias) {
                userPreferences.setAliasCodeAllowSingleChar(targetId, normalizedCode)
            } else {
                userPreferences.setAliasCode(targetId, normalizedCode)
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
            val id = target.getId()
            aliasEnabled = aliasEnabled.toMutableMap().apply { put(id, getAlias(target).isNotEmpty()) }
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
        return aliasEnabled[id] ?: getAlias(target).isNotEmpty()
    }

    fun getFeatureAliasDefinition(featureId: String): FeatureAliasDefinition? =
        FEATURE_ALIAS_DEFINITIONS.firstOrNull { it.featureId == featureId }

    fun detectAliasAtStart(query: String): Pair<String, AliasTarget>? {
        ensureInitialized()
        if (query.isBlank()) return null

        val aliases = mutableMapOf<String, AliasTarget>()
        collectLeadingSearchTargetAliases(aliases)
        collectLeadingFeatureAliases(aliases)
        collectLeadingSectionAliases(aliases)
        val match = AliasParser.detectPrefixAlias(query, aliases) ?: return null
        return Pair(match.queryWithoutAlias, match.target)
    }

    fun detectSearchEngineAliasAtEnd(query: String): Pair<String, SearchTarget>? {
        ensureInitialized()
        if (!userPreferences.isSearchEngineAliasSuffixEnabled()) return null
        if (query.isBlank()) return null

        val aliases = mutableMapOf<String, SearchTarget>()
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
            if (target !is SearchTarget.Engine && target !is SearchTarget.Browser && target !is SearchTarget.Custom) continue
            if (!isAliasEnabled(target)) continue

            val aliasCode = getAlias(target).lowercase(Locale.getDefault())
            if (aliasCode.isEmpty()) continue
            aliases[aliasCode] = target
        }
        val match = AliasParser.detectSuffixAlias(
            query,
            aliases,
            requireTrailingSpace = userPreferences.isAliasTriggerAfterSpaceEnabled(),
        ) ?: return null
        return Pair(match.queryWithoutAlias, match.target)
    }

    private fun collectLeadingSearchTargetAliases(aliases: MutableMap<String, AliasTarget>) {
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
            if (!isAliasEnabled(target)) continue

            val aliasCode = getAlias(target).lowercase(Locale.getDefault())
            if (aliasCode.isEmpty()) continue
            aliases[aliasCode] = AliasTarget.Search(target)
        }
    }

    private fun collectLeadingFeatureAliases(aliases: MutableMap<String, AliasTarget>) {
        TOOL_ALIAS_IDS.forEach { featureAliasId ->
            val aliasCode = getAlias(featureAliasId).lowercase(Locale.getDefault())
            if (aliasCode.isNotEmpty()) {
                aliases[aliasCode] = AliasTarget.Feature(featureAliasId)
            }
        }
    }

    private fun collectLeadingSectionAliases(aliases: MutableMap<String, AliasTarget>) {
        SearchSectionRegistry.orderedDefinitions.forEach { definition ->
            putSectionAlias(
                aliases = aliases,
                aliasId = definition.aliasTargetId,
                section = definition.section,
            )
        }
    }

    private fun putSectionAlias(
        aliases: MutableMap<String, AliasTarget>,
        aliasId: String,
        section: SearchSection,
    ) {
        if (aliasEnabled[aliasId] != true) return
        val aliasCode = aliasCodes[aliasId].orEmpty().lowercase(Locale.getDefault())
        if (aliasCode.isEmpty()) return
        aliases[aliasCode] = AliasTarget.Section(section)
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
