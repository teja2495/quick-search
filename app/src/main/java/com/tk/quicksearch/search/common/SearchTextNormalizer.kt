package com.tk.quicksearch.search.utils

import java.text.Normalizer
import java.util.Locale

private val DIACRITIC_MARKS_REGEX = "\\p{M}+".toRegex()
private val NORMALIZE_WHITESPACE_REGEX = "[\\s\\u00A0]+".toRegex()

object SearchTextNormalizer {
    /**
     * Normalizes text for search by removing diacritics and lowercasing with locale awareness.
     * Example: "háll" -> "hall"
     */
    fun normalizeForSearch(text: String): String =
        Normalizer
            .normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITIC_MARKS_REGEX, "")
            .lowercase(Locale.getDefault())

    /**
     * Normalizes query whitespace so trailing, repeated, and non-breaking spaces
     * are treated consistently across search providers and ranking.
     */
    fun normalizeQueryWhitespace(text: String): String =
        text
            .replace('\u00A0', ' ')
            .replace(NORMALIZE_WHITESPACE_REGEX, " ")
            .trim()
}
