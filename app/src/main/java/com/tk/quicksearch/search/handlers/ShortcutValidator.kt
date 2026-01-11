package com.tk.quicksearch.search.handlers

import java.util.Locale

/**
 * Centralized validation and normalization for search engine shortcuts.
 * Consolidates all shortcut validation logic to ensure consistency across the app.
 */
object ShortcutValidator {

    /**
     * Normalizes shortcut input by trimming, lowercasing, and filtering to valid characters.
     * Only allows letters and digits, removes spaces and special characters.
     */
    fun normalizeShortcutCodeInput(input: String): String {
        return input.trim()
            .lowercase(Locale.getDefault())
            .filter { char -> char.isLetterOrDigit() }
    }

    /**
     * Validates shortcut input.
     * Must contain only letters and digits, be non-empty, at least 2 characters, and at most 5 characters.
     */
    fun isValidShortcutCode(input: String): Boolean {
        val normalized = normalizeShortcutCodeInput(input)
        return normalized.isNotEmpty() &&
               normalized.length >= 2 &&
               normalized.length <= 5 &&
               normalized.all { it.isLetterOrDigit() }
    }

    /**
     * Validates shortcut input for display purposes (allows shorter codes for UI feedback).
     * Same as isValidShortcutCode but allows single character codes.
     */
    fun isValidShortcutCodeForDisplay(input: String): Boolean {
        val normalized = normalizeShortcutCodeInput(input)
        return normalized.isNotEmpty() &&
               normalized.length >= 1 &&
               normalized.length <= 5 &&
               normalized.all { it.isLetterOrDigit() }
    }
}