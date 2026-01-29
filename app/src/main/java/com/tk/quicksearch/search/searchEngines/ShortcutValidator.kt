package com.tk.quicksearch.search.searchEngines

import java.util.Locale

/**
 * Centralized validation and normalization for search engine shortcuts. Consolidates all shortcut
 * validation logic to ensure consistency across the app.
 */
object ShortcutValidator {

    /**
     * Normalizes shortcut input by trimming, lowercasing, and filtering to valid characters. Only
     * allows letters and digits, removes spaces and special characters.
     */
    fun normalizeShortcutCodeInput(input: String): String {
        return input.trim().lowercase(Locale.getDefault()).filter { char -> char.isLetterOrDigit() }
    }

    /**
     * Validates shortcut input. Must contain only letters and digits, be non-empty, and at least 2
     * characters.
     */
    fun isValidShortcutCode(input: String): Boolean {
        val normalized = normalizeShortcutCodeInput(input)
        return normalized.isNotEmpty() &&
                normalized.length >= 2 &&
                normalized.all { it.isLetterOrDigit() }
    }

    /**
     * Validates shortcut input for display purposes (allows shorter codes for UI feedback). Same as
     * isValidShortcutCode but allows single character codes.
     */
    fun isValidShortcutCodeForDisplay(input: String): Boolean {
        val normalized = normalizeShortcutCodeInput(input)
        return normalized.isNotEmpty() &&
                normalized.length >= 1 &&
                normalized.all { it.isLetterOrDigit() }
    }

    /**
     * Validates that a shortcut does not conflict with any existing shortcut. A conflict occurs if
     * one shortcut is a prefix of another (e.g., "br" conflicts with "brvt"). This prevents
     * ambiguity where multiple shortcuts could potentially match the same input.
     *
     * @param newShortcut The new shortcut to validate
     * @param existingShortcuts Map of existing shortcuts (target identifier -> shortcut code)
     * @return true if the shortcut has no prefix conflicts, false otherwise
     */
    fun isValidShortcutPrefix(
            newShortcut: String,
            existingShortcuts: Map<String, String>
    ): Boolean {
        val normalizedNew = normalizeShortcutCodeInput(newShortcut)
        if (normalizedNew.isEmpty()) return true

        // Check if any existing shortcut is a prefix of the new shortcut
        // OR if the new shortcut is a prefix of any existing shortcut
        return existingShortcuts.none { (_, existingCode) ->
            val normalizedExisting = normalizeShortcutCodeInput(existingCode)
            normalizedExisting.isNotEmpty() &&
                    (normalizedNew.startsWith(normalizedExisting) ||
                            normalizedExisting.startsWith(normalizedNew))
        }
    }
}
