package com.tk.quicksearch.search.utils

import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

private val DIACRITIC_MARKS_REGEX = "\\p{M}+".toRegex()
private val NORMALIZE_WHITESPACE_REGEX = "[\\s\\u00A0]+".toRegex()
private val SEARCH_WHITESPACE_REGEX = "[\\s\\u00A0]+".toRegex()
private val SEARCH_SEPARATOR_REGEX = "[^\\p{L}\\p{Nd}]+".toRegex()
private val SEARCH_WORD_REGEX = "\\s+".toRegex()
private val SEARCH_TOKEN_SEPARATOR_REGEX = "[^\\p{L}\\p{N}]+".toRegex()
private const val TURKISH_DOTLESS_I = '\u0131'
private const val ASCII_I = 'i'

data class PreparedSearchText(
    val normalized: String,
    val compact: String,
    val words: List<String>,
    val fuzzyTokens: List<String>,
    val acronym: String,
)

/**
 * Content-keyed cache for catalog text that is searched repeatedly. Keeping this cache owned by
 * the catalog/handler avoids changing persisted models while making changed labels and nicknames
 * naturally use a different entry.
 */
class SearchTextCache {
    private val entries = ConcurrentHashMap<String, PreparedSearchText>()

    fun prepare(text: String): PreparedSearchText =
        entries.computeIfAbsent(text, SearchTextNormalizer::prepareForSearch)

    fun clear() {
        entries.clear()
    }
}

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

    fun prepareForSearch(text: String): PreparedSearchText =
        prepareNormalizedForSearch(normalizeForSearch(text))

    fun prepareNormalizedForSearch(normalizedText: String): PreparedSearchText {
        val words = normalizedText.split(SEARCH_WORD_REGEX)
        val fuzzyTokens = normalizedText.split(SEARCH_TOKEN_SEPARATOR_REGEX).filter { it.isNotBlank() }
        return PreparedSearchText(
            normalized = normalizedText,
            compact = compactForSearch(normalizedText),
            words = words,
            fuzzyTokens = fuzzyTokens,
            acronym = buildString(fuzzyTokens.size) {
                fuzzyTokens.forEach { token -> append(token[0].lowercaseChar()) }
            },
        )
    }

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
