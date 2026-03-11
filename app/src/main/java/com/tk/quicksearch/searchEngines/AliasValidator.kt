package com.tk.quicksearch.searchEngines

import java.util.Locale

/**
 * Centralized validation and normalization for search engine shortcuts. Consolidates all shortcut
 * validation logic to ensure consistency across the app.
 */
object AliasValidator {
    /**
     * Normalizes shortcut input by trimming, lowercasing, and removing whitespace.
     */
    fun normalizeShortcutCodeInput(input: String): String =
        input.trim().lowercase(Locale.getDefault()).filterNot { char -> char.isWhitespace() }

    /**
     * Validates search-engine shortcut input. Must be non-empty and at least 2 characters after
     * normalization.
     */
    fun isValidShortcutCode(input: String): Boolean {
        val normalized = normalizeShortcutCodeInput(input)
        return normalized.length >= 2
    }

    /** Validates non-search-engine aliases. Any non-empty normalized code is allowed. */
    fun isValidGeneralAliasCode(input: String): Boolean =
        normalizeShortcutCodeInput(input).isNotEmpty()

    /** Returns true when the new alias exactly matches an existing alias (case/whitespace-insensitive). */
    fun hasExactAliasConflict(
        newAlias: String,
        existingAliases: Map<String, String>,
    ): Boolean {
        val normalizedNew = normalizeShortcutCodeInput(newAlias)
        if (normalizedNew.isEmpty()) return false

        return existingAliases.values.any { existingAlias ->
            val normalizedExisting = normalizeShortcutCodeInput(existingAlias)
            normalizedExisting.isNotEmpty() && normalizedExisting == normalizedNew
        }
    }
}
