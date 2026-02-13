package com.tk.quicksearch.search.utils

import java.text.Normalizer
import java.util.Locale

private val DIACRITIC_MARKS_REGEX = "\\p{M}+".toRegex()

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
}
