package com.tk.quicksearch.search.apps

import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

private val DIACRITIC_MARKS_REGEX = "\\p{M}+".toRegex()
private const val TURKISH_DOTLESS_I = 'ı'
private const val ASCII_I = 'i'

/**
 * Fuzzy-only app name matching where the query's letters appear in order anywhere in the name,
 * e.g. "ffx" -> "Firefox" or "kdx" -> "KeePassDX". The first letter must start a word so short
 * queries don't match letters buried mid-word.
 */
object AppNameSubsequence {
    private const val MIN_QUERY_LENGTH = 3

    // Kept above the app fuzzy threshold; bonuses favor word-start hits and tight spans.
    private const val BASE_SCORE = 76
    private const val WORD_START_BONUS = 12
    private const val TIGHTNESS_BONUS = 6

    /** Letters and digits of a name, normalized like search text, with word starts marked. */
    class Target(
        val chars: String,
        val wordStarts: BooleanArray,
    )

    class Cache {
        private val entries = ConcurrentHashMap<String, Target>()

        fun prepare(text: String): Target = entries.computeIfAbsent(text, AppNameSubsequence::prepare)
    }

    fun prepare(text: String): Target {
        val source = Normalizer.normalize(text, Normalizer.Form.NFD).replace(DIACRITIC_MARKS_REGEX, "")
        val chars = StringBuilder(source.length)
        val wordStarts = ArrayList<Boolean>(source.length)
        var previous: Char? = null
        for (current in source) {
            if (!current.isLetterOrDigit()) {
                previous = null
                continue
            }
            val startsWord =
                previous == null ||
                    (previous.isLowerCase() && current.isUpperCase()) ||
                    (previous.isLetter() != current.isLetter())
            chars.append(current.lowercaseChar().let { if (it == TURKISH_DOTLESS_I) ASCII_I else it })
            wordStarts.add(startsWord)
            previous = current
        }
        return Target(chars.toString(), wordStarts.toBooleanArray())
    }

    /** Returns 0 when [compactQuery] isn't an in-order subsequence starting at a word. */
    fun score(
        compactQuery: String,
        target: Target,
    ): Int {
        val queryLength = compactQuery.length
        val chars = target.chars
        if (queryLength < MIN_QUERY_LENGTH || chars.length <= queryLength) return 0

        var start = -1
        for (index in 0..chars.length - queryLength) {
            if (target.wordStarts[index] && chars[index] == compactQuery[0]) {
                start = index
                break
            }
        }
        if (start < 0) return 0

        var wordStartHits = 1
        var position = start
        for (queryIndex in 1 until queryLength) {
            position = chars.indexOf(compactQuery[queryIndex], position + 1)
            if (position < 0) return 0
            if (target.wordStarts[position]) wordStartHits += 1
        }

        val span = position - start + 1
        return BASE_SCORE +
            WORD_START_BONUS * wordStartHits / queryLength +
            TIGHTNESS_BONUS * queryLength / span
    }
}
