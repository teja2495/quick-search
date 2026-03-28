package com.tk.quicksearch.searchEngines

import java.util.Locale

data class ParsedAliasMatch<T>(
    val queryWithoutAlias: String,
    val target: T,
)

internal object AliasParser {
    fun <T> detectSuffixAlias(
        query: String,
        aliases: Map<String, T>,
        requireTrailingSpace: Boolean = true,
    ): ParsedAliasMatch<T>? {
        if (query.isBlank()) return null

        // End aliases trigger only after the user types a trailing space (when enabled).
        if (requireTrailingSpace && !query.last().isWhitespace()) return null

        val queryWithoutTrailingWhitespace = query.trimEnd()
        if (queryWithoutTrailingWhitespace.isEmpty()) return null

        val words =
            queryWithoutTrailingWhitespace
                .trimStart()
                .split("\\s+".toRegex())
                .filter { it.isNotEmpty() }
        if (words.size < 2) return null
        val suffix = words.last().lowercase(Locale.getDefault())
        val match = aliases[suffix] ?: return null
        return ParsedAliasMatch(
            queryWithoutAlias = words.dropLast(1).joinToString(" "),
            target = match,
        )
    }

    fun <T> detectPrefixAlias(
        query: String,
        aliases: Map<String, T>,
    ): ParsedAliasMatch<T>? {
        val queryWithNoLeadingWhitespace = query.trimStart()
        if (queryWithNoLeadingWhitespace.isEmpty()) return null

        // Start aliases trigger only after the user types a separating space.
        val separatorIndex = queryWithNoLeadingWhitespace.indexOfFirst { it.isWhitespace() }
        if (separatorIndex <= 0) return null

        val prefix =
            queryWithNoLeadingWhitespace
                .substring(0, separatorIndex)
                .lowercase(Locale.getDefault())
        val match = aliases[prefix] ?: return null
        return ParsedAliasMatch(
            queryWithoutAlias =
                queryWithNoLeadingWhitespace.substring(separatorIndex).trimStart(),
            target = match,
        )
    }
}
