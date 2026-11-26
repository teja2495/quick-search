package com.tk.quicksearch.searchEngines

import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.Locale

/**
 * Centralized validation and normalization for search engine shortcuts. Consolidates all shortcut
 * validation logic to ensure consistency across the app.
 *
 * An alias or trigger can hold several words separated by commas ("yt,tube"); each one works on
 * its own. They are stored as one comma-joined string.
 */
object AliasValidator {
    const val ALIAS_SEPARATOR = ','

    /**
     * Lowercases and removes whitespace while the user types, keeping commas (including a trailing
     * one) so another alias can be typed after it.
     */
    fun sanitizeAliasInput(input: String): String =
        input.lowercase(Locale.getDefault()).filterNot { char -> char.isWhitespace() }

    /** Splits a comma-separated alias string into its distinct, non-empty aliases. */
    fun splitAliasCodes(input: String): List<String> {
        if (input.isEmpty()) return emptyList()
        if (ALIAS_SEPARATOR !in input) {
            return sanitizeAliasInput(input).takeIf { it.isNotEmpty() }?.let(::listOf).orEmpty()
        }
        return input
            .split(ALIAS_SEPARATOR)
            .map(::sanitizeAliasInput)
            .filter { it.isNotEmpty() }
            .distinct()
    }

    /**
     * Normalizes shortcut input for storage: lowercased, whitespace removed, and every alias in a
     * comma-separated list kept once with empty entries dropped.
     */
    fun normalizeShortcutCodeInput(input: String): String =
        splitAliasCodes(input).joinToString(ALIAS_SEPARATOR.toString())

    /** Validates non-search-engine aliases. Any non-empty normalized code is allowed. */
    fun isValidGeneralAliasCode(input: String): Boolean =
        splitAliasCodes(input).isNotEmpty()

    /** Returns true when any of the new aliases exactly matches an existing alias (case/whitespace-insensitive). */
    fun hasExactAliasConflict(
        newAlias: String,
        existingAliases: Map<String, String>,
    ): Boolean {
        val newCodes = splitAliasCodes(newAlias)
        if (newCodes.isEmpty()) return false

        val existingCodes = existingAliases.values.flatMapTo(HashSet()) { splitAliasCodes(it) }
        return newCodes.any { it in existingCodes }
    }

    /** Returns true when any of the new aliases would use the same leading token as an existing trigger. */
    fun hasTriggerAliasConflict(
        newAlias: String,
        triggerWords: Collection<String>,
    ): Boolean {
        val aliasTokens = splitAliasCodes(newAlias).mapNotNull(::conflictToken)
        if (aliasTokens.isEmpty()) return false

        val triggerTokens =
            triggerWords.flatMapTo(HashSet()) { triggerWord ->
                splitAliasCodes(triggerWord).mapNotNull(::conflictToken)
            }
        return aliasTokens.any { it in triggerTokens }
    }

    private fun conflictToken(word: String): String? =
        SearchTextNormalizer.normalizeForSearch(word).trim().substringBefore(' ').takeIf { it.isNotBlank() }
}
