package com.tk.quicksearch.search.notes

import java.util.Locale

internal object NotesMarkdownUtils {
    private val markdownSyntaxRegex = Regex("""[#>*_`~\[\]()\-]""")
    private val multiWhitespaceRegex = Regex("""\s+""")

    fun toSearchablePlainText(markdown: String): String =
        markdown
            .replace(markdownSyntaxRegex, " ")
            .replace(multiWhitespaceRegex, " ")
            .trim()

    fun buildPreviewText(
        markdown: String,
        maxChars: Int = 280,
    ): String {
        val plain = toSearchablePlainText(markdown)
        if (plain.length <= maxChars) return plain
        return plain.take(maxChars).trimEnd()
    }

    fun normalize(value: String): String = value.trim().lowercase(Locale.getDefault())
}
