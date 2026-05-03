package com.tk.quicksearch.search.utils

private val FILE_SEARCH_SEPARATOR_REGEX = "[^\\p{L}\\p{Nd}]+".toRegex()

object FileSearchTextNormalizer {
    /**
     * Treat punctuation and repeated separators as word boundaries for file-name search.
     * This lets "teja karlapudi" match "teja-karlapudi", "teja--karlapudi",
     * "teja_karlapudi", and similar file-name variants.
     */
    fun normalizeForFileSearch(text: String): String =
        SearchTextNormalizer
            .normalizeForSearch(text)
            .replace(FILE_SEARCH_SEPARATOR_REGEX, " ")
            .trim()
            .replace(QUERY_WHITESPACE_REGEX, " ")

    fun queryTokens(text: String): List<String> =
        normalizeForFileSearch(text)
            .split(QUERY_WHITESPACE_REGEX)
            .filter { it.isNotBlank() }
}

private val QUERY_WHITESPACE_REGEX = "\\s+".toRegex()
