package com.tk.quicksearch.search.searchScreen.components

import com.tk.quicksearch.searchEngines.AliasValidator.splitAliasCodes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.SpanStyle
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.tools.aiSearch.QuickSearchHelp
import java.util.Locale

@Composable
internal fun rememberAliasHighlightOutputTransformation(
    enabledTargets: List<SearchTarget>,
    shortcutCodes: Map<String, String>,
    shortcutEnabled: Map<String, Boolean>,
    triggerWords: Collection<String>,
    isSearchEngineAliasSuffixEnabled: Boolean,
    highlightColor: Color,
): OutputTransformation {
    val searchTargetIds =
        remember(enabledTargets) {
            enabledTargets.mapTo(mutableSetOf()) { it.getId() }
        }
    val activeAliases =
        remember(shortcutCodes, shortcutEnabled, triggerWords) {
            buildSet {
                shortcutCodes.entries
                    .asSequence()
                    .filter { (id, code) -> code.isNotBlank() && (shortcutEnabled[id] != false) }
                    .flatMap { (_, rawAlias) -> splitAliasCodes(rawAlias) }
                    .forEach(::add)
                triggerWords
                    .asSequence()
                    .flatMap { splitAliasCodes(it) }
                    .forEach(::add)
            }.toList().sortedByDescending { it.length }
        }
    val activeSearchTargetAliases =
        remember(shortcutCodes, shortcutEnabled, searchTargetIds) {
            shortcutCodes.entries
                .asSequence()
                .filter { (id, _) -> id in searchTargetIds }
                .filter { (id, code) -> code.isNotBlank() && (shortcutEnabled[id] != false) }
                .flatMap { (_, code) -> splitAliasCodes(code) }
                .toSet()
                .toList()
                .sortedByDescending { it.length }
        }
    return remember(activeAliases, activeSearchTargetAliases, isSearchEngineAliasSuffixEnabled, highlightColor) {
        AliasHighlightOutputTransformation(
            prefixAliases = activeAliases,
            suffixAliases = activeSearchTargetAliases,
            highlightColor = highlightColor,
            highlightSuffixAlias = isSearchEngineAliasSuffixEnabled,
        )
    }
}

/** Colors the alias the query starts or ends with; the text and cursor are left as typed. */
@Stable
private class AliasHighlightOutputTransformation(
    private val prefixAliases: List<String>,
    private val suffixAliases: List<String>,
    private val highlightColor: Color,
    private val highlightSuffixAlias: Boolean,
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val highlightRange = resolveHighlightRange(asCharSequence().toString()) ?: return
        addStyle(SpanStyle(color = highlightColor), highlightRange.first, highlightRange.last + 1)
    }

    private fun resolveHighlightRange(query: String): IntRange? {
        if (query.isBlank()) return null
        // The built-in @help alias wins; no other alias is highlighted alongside it.
        QuickSearchHelp.aliasRange(query)?.let { return it }
        val trimmedStartQuery = query.trimStart()
        val leadingWhitespace = query.length - trimmedStartQuery.length
        val prefixMatch =
            findPrefixHighlightLength(trimmedStartQuery)?.let { highlightLength ->
                leadingWhitespace until (leadingWhitespace + highlightLength)
            }
        if (prefixMatch != null) {
            return prefixMatch
        }
        if (!highlightSuffixAlias) return null
        return resolveSuffixRange(query)
    }

    private fun findPrefixHighlightLength(query: String): Int? {
        if (query.isEmpty()) return null
        val firstWhitespaceIndex = query.indexOfFirst { it.isWhitespace() }
        val firstToken =
            if (firstWhitespaceIndex == -1) {
                query
            } else {
                query.substring(0, firstWhitespaceIndex)
            }
        if (firstToken.isEmpty()) return null
        val hasOnlyFirstToken = firstWhitespaceIndex == -1

        if (hasOnlyFirstToken && prefixAliases.any { it.startsWith(firstToken, ignoreCase = true) }) {
            return firstToken.length
        }

        for (alias in prefixAliases) {
            if (!firstToken.equals(alias, ignoreCase = true)) continue
            return firstToken.length
        }
        return null
    }

    private fun resolveSuffixRange(query: String): IntRange? {
        val trimmedEnd = query.trimEnd()
        if (trimmedEnd.isEmpty()) return null
        val lastWordStart = trimmedEnd.lastIndexOf(' ') + 1
        if (lastWordStart <= 0) return null
        if (trimmedEnd.substring(0, lastWordStart).isBlank()) return null
        val lastToken = trimmedEnd.substring(lastWordStart)
        if (lastToken.isEmpty()) return null
        if (suffixAliases.any { it.equals(lastToken, ignoreCase = true) }) {
            return lastWordStart until trimmedEnd.length
        }
        if (suffixAliases.any { it.startsWith(lastToken, ignoreCase = true) }) {
            return lastWordStart until trimmedEnd.length
        }
        return null
    }
}
