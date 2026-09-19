package com.tk.quicksearch.search.notes

import java.util.Locale

/** Text to write back into an editable field, with the caret position that should follow it. */
data class SnippetReplacement(
    val text: String,
    val cursor: Int,
)

/**
 * Bookkeeping for the expansion that just happened, so the trailing space can act as an undo
 * handle. Only ever valid for the single text change that immediately follows the expansion.
 */
data class PendingSnippetUndo(
    val textAfterExpansion: String,
    val keyword: String,
    val startIndex: Int,
    val bodyLength: Int,
) {
    /** Index of the space appended after the expanded body. */
    val spaceIndex: Int get() = startIndex + bodyLength
}

/**
 * Pure text math behind snippet expansion. Kept free of [android.view.accessibility] types so the
 * substring arithmetic can be reasoned about (and unit tested) without a device.
 */
object SnippetExpander {
    fun normalizeKeyword(keyword: String): String = keyword.trim().lowercase(Locale.getDefault())

    /**
     * Expands the keyword ending at [cursor] into its body plus a trailing space.
     *
     * Returns `null` when the text before the caret does not end in a known keyword. [keywords]
     * must already be normalized by [normalizeKeyword].
     */
    fun computeExpansion(
        text: String,
        cursor: Int,
        keywords: Map<String, String>,
    ): Pair<SnippetReplacement, PendingSnippetUndo>? {
        if (keywords.isEmpty()) return null
        if (cursor !in 0..text.length) return null
        val before = text.take(cursor)
        if (before.isEmpty()) return null

        // Longest match wins so "sig" never shadows "siglong".
        val match =
            keywords.entries
                .filter { (keyword, body) ->
                    keyword.isNotEmpty() && body.isNotEmpty() && endsWithKeyword(before, keyword)
                }
                .maxByOrNull { it.key.length }
                ?: return null

        val keyword = match.key
        val body = match.value
        val start = cursor - keyword.length
        val rest = text.substring(cursor)
        val newText = text.take(start) + body + " " + rest
        val replacement = SnippetReplacement(text = newText, cursor = start + body.length + 1)
        val undo =
            PendingSnippetUndo(
                textAfterExpansion = newText,
                // Restore exactly what was typed, not the normalized form.
                keyword = before.substring(start),
                startIndex = start,
                bodyLength = body.length,
            )
        return replacement to undo
    }

    /**
     * Reverts [pending] back to the typed keyword when the user deleted the appended space.
     *
     * Returns `null` for any other edit, which the caller should treat as "the undo window closed".
     */
    fun computeUndo(
        text: String,
        cursor: Int,
        pending: PendingSnippetUndo,
    ): SnippetReplacement? {
        val spaceIndex = pending.spaceIndex
        if (spaceIndex !in 0 until pending.textAfterExpansion.length) return null
        if (pending.textAfterExpansion[spaceIndex] != ' ') return null
        // The only accepted edit is deleting that one space, caret left where it was.
        val expected = pending.textAfterExpansion.removeRange(spaceIndex, spaceIndex + 1)
        if (text != expected || cursor != spaceIndex) return null

        val restored = text.take(pending.startIndex) + pending.keyword + text.substring(spaceIndex)
        return SnippetReplacement(
            text = restored,
            cursor = pending.startIndex + pending.keyword.length,
        )
    }

    /**
     * True when [before] ends with [keyword] on a word boundary, so "addr" does not fire inside
     * "myaddr". Keywords that open with punctuation (";sig") carry their own boundary.
     */
    private fun endsWithKeyword(
        before: String,
        keyword: String,
    ): Boolean {
        if (!before.endsWith(keyword, ignoreCase = true)) return false
        val start = before.length - keyword.length
        if (start == 0) return true
        if (!keyword.first().isLetterOrDigit()) return true
        return !before[start - 1].isLetterOrDigit()
    }
}
