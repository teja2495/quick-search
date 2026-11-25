package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import com.tk.quicksearch.shared.ui.theme.AppColors

internal val LocalSearchResultQuery = staticCompositionLocalOf { "" }

private val QUERY_SEPARATOR_REGEX = Regex("[^\\p{L}\\p{Nd}]+")

@Composable
internal fun rememberQueryHighlightedText(
    text: String,
    query: String = LocalSearchResultQuery.current,
): AnnotatedString {
    val highlightColor = AppColors.Accent
    return remember(text, query, highlightColor) {
        val ranges = queryHighlightRanges(text, query)
        if (ranges.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                append(text)
                ranges.forEach { range ->
                    addStyle(
                        style = SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold),
                        start = range.first,
                        end = range.last + 1,
                    )
                }
            }
        }
    }
}

internal fun queryHighlightRanges(
    text: String,
    query: String,
): List<IntRange> {
    if (text.isEmpty() || query.isBlank()) return emptyList()

    val mappedText = NormalizedText.withOriginalOffsets(text)
    val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
    if (mappedText.value.isEmpty() || normalizedQuery.isEmpty()) return emptyList()

    val candidates =
        buildList {
            add(normalizedQuery)
            addAll(normalizedQuery.split(QUERY_SEPARATOR_REGEX))
        }.filter { it.isNotBlank() }
            .distinct()
            .sortedByDescending(String::length)

    val ranges = mutableListOf<IntRange>()
    candidates.forEach { candidate ->
        var start = mappedText.value.indexOf(candidate)
        while (start >= 0) {
            ranges += mappedText.originalRange(start, start + candidate.length)
            start = mappedText.value.indexOf(candidate, startIndex = start + candidate.length)
        }
    }

    if (ranges.isEmpty()) {
        ranges += mappedText.wordInitialRanges(normalizedQuery.replace(QUERY_SEPARATOR_REGEX, ""))
    }

    return mergeRanges(ranges)
}

private fun mergeRanges(ranges: List<IntRange>): List<IntRange> {
    if (ranges.isEmpty()) return emptyList()
    val sorted = ranges.sortedBy(IntRange::first)
    val merged = mutableListOf(sorted.first())
    sorted.drop(1).forEach { range ->
        val previous = merged.last()
        if (range.first <= previous.last + 1) {
            merged[merged.lastIndex] = previous.first..maxOf(previous.last, range.last)
        } else {
            merged += range
        }
    }
    return merged
}

private data class NormalizedText(
    val value: String,
    val originalStarts: IntArray? = null,
    val originalEnds: IntArray? = null,
) {
    fun originalRange(
        normalizedStart: Int,
        normalizedEndExclusive: Int,
    ): IntRange =
        originalStart(normalizedStart) until originalEnd(normalizedEndExclusive - 1)

    fun wordInitialRanges(compactQuery: String): List<IntRange> {
        if (compactQuery.length < 2) return emptyList()
        val initialIndexes =
            value.indices.filter { index ->
                value[index].isLetterOrDigit() &&
                    (index == 0 || !value[index - 1].isLetterOrDigit())
            }
        if (initialIndexes.size < compactQuery.length) return emptyList()

        for (start in 0..initialIndexes.size - compactQuery.length) {
            val candidate =
                buildString(compactQuery.length) {
                    repeat(compactQuery.length) { offset ->
                        append(value[initialIndexes[start + offset]])
                    }
                }
            if (candidate == compactQuery) {
                return List(compactQuery.length) { offset ->
                    val normalizedIndex = initialIndexes[start + offset]
                    originalStart(normalizedIndex) until originalEnd(normalizedIndex)
                }
            }
        }
        return emptyList()
    }

    private fun originalStart(normalizedIndex: Int): Int =
        originalStarts?.get(normalizedIndex) ?: normalizedIndex

    private fun originalEnd(normalizedIndex: Int): Int =
        originalEnds?.get(normalizedIndex) ?: (normalizedIndex + 1)

    companion object {
        fun withOriginalOffsets(text: String): NormalizedText {
            val normalizedText = SearchTextNormalizer.normalizeForSearch(text)
            if (normalizedText.length == text.length) {
                return NormalizedText(value = normalizedText)
            }

            val normalized = StringBuilder(text.length)
            val starts = mutableListOf<Int>()
            val ends = mutableListOf<Int>()
            var offset = 0
            while (offset < text.length) {
                val codePoint = text.codePointAt(offset)
                val charCount = Character.charCount(codePoint)
                val piece =
                    SearchTextNormalizer.normalizeForSearch(
                        String(Character.toChars(codePoint)),
                    )
                if (piece.isEmpty() && ends.isNotEmpty()) {
                    ends[ends.lastIndex] = offset + charCount
                }
                piece.forEach { char ->
                    normalized.append(char)
                    starts += offset
                    ends += offset + charCount
                }
                offset += charCount
            }
            return NormalizedText(
                value = normalized.toString(),
                originalStarts = starts.toIntArray(),
                originalEnds = ends.toIntArray(),
            )
        }
    }
}
