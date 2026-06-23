package com.tk.quicksearch.search.utils

import java.text.Normalizer

private val DIACRITIC_MARKS_REGEX = "\\p{M}+".toRegex()
private val NORMALIZE_WHITESPACE_REGEX = "[\\s\\u00A0]+".toRegex()
private val SEARCH_WHITESPACE_REGEX = "[\\s\\u00A0]+".toRegex()
private val SEARCH_SEPARATOR_REGEX = "[^\\p{L}\\p{Nd}]+".toRegex()
private const val TURKISH_DOTLESS_I = '\u0131'
private const val ASCII_I = 'i'

object SearchTextNormalizer {
    /**
     * Normalizes text for search by removing diacritics, folding case, and unifying
     * Turkish dotless i with ascii i so both forms remain searchable.
     * Example: "háll" -> "hall"
     */
    fun normalizeForSearch(text: String): String =
        Normalizer
            .normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITIC_MARKS_REGEX, "")
            .lowercase()
            .replace(TURKISH_DOTLESS_I, ASCII_I)

    /**
     * Normalizes query whitespace so trailing, repeated, and non-breaking spaces
     * are treated consistently across search providers and ranking.
     */
    fun normalizeQueryWhitespace(text: String): String =
        text
            .replace('\u00A0', ' ')
            .replace(NORMALIZE_WHITESPACE_REGEX, " ")
            .trim()

    fun removeSearchWhitespace(text: String): String = text.replace(SEARCH_WHITESPACE_REGEX, "")

    fun compactForSearch(text: String): String = text.replace(SEARCH_SEPARATOR_REGEX, "")
}
