package com.tk.quicksearch.search.notes

import java.util.Locale

internal object NotesTextUtils {
    private val multiWhitespaceRegex = Regex("""\s+""")

    fun toSearchablePlainText(text: String): String =
        text
            .replace(multiWhitespaceRegex, " ")
            .trim()

    fun buildPreviewText(
        text: String,
        maxChars: Int = 280,
    ): String {
        val plain = toSearchablePlainText(text)
        if (plain.length <= maxChars) return plain
        return plain.take(maxChars).trimEnd()
    }

    fun normalize(value: String): String = value.trim().lowercase(Locale.getDefault())
}
