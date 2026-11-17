package com.tk.quicksearch.search.notes

import android.text.SpannableString
import android.text.util.Linkify
import android.text.style.URLSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.core.text.util.LinkifyCompat
import com.tk.quicksearch.search.utils.SearchTextNormalizer

internal object NotesTextUtils {
    private const val NOTE_LINK_ANNOTATION_TAG = "note_link"
    private val multiWhitespaceRegex = Regex("""\s+""")

    fun toSearchablePlainText(text: String): String =
        text
            .replace(multiWhitespaceRegex, " ")
            .trim()

    /**
     * First [maxLines] lines of [text], preserving line breaks. Normalizes `\r\n` and lone `\r` to `\n`.
     */
    fun firstLinesPreview(
        text: String,
        maxLines: Int = 3,
    ): String {
        if (text.isEmpty() || maxLines <= 0) return ""
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.split('\n')
        if (lines.size <= maxLines) return normalized.trimEnd()
        return lines.take(maxLines).joinToString("\n")
    }

    /** Returns the matching line with up to [contextLines] lines on either side. */
    fun matchCenteredPreview(
        text: String,
        query: String,
        contextLines: Int = 2,
    ): String? {
        if (text.isEmpty() || query.isBlank() || contextLines < 0) return null
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return null
        val normalizedBody = normalize(toSearchablePlainText(text))
        if (!normalizedBody.contains(normalizedQuery)) return null

        val normalizedText = text.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalizedText.split('\n')
        var matchedLine =
            lines.indexOfFirst { line ->
                normalize(toSearchablePlainText(line)).contains(normalizedQuery)
            }
        if (matchedLine < 0) {
            val firstToken = normalizedQuery.substringBefore(' ')
            matchedLine =
                lines.indexOfFirst { line ->
                    normalize(toSearchablePlainText(line)).contains(firstToken)
                }
        }
        if (matchedLine < 0) matchedLine = 0

        val start = (matchedLine - contextLines).coerceAtLeast(0)
        val endExclusive = (matchedLine + contextLines + 1).coerceAtMost(lines.size)
        return lines.subList(start, endExclusive).joinToString("\n").trimEnd()
    }

    fun normalize(value: String): String = SearchTextNormalizer.normalizeForSearch(value.trim())

    /** A detected link: [start] inclusive, [end] exclusive, and the URL it opens. */
    internal data class LinkRange(val start: Int, val end: Int, val url: String)

    fun buildLinkHighlightedAnnotatedString(
        text: String,
        linkColor: Color,
    ): AnnotatedString = linkAnnotatedString(text, findLinks(text), linkColor)

    /**
     * [newText] highlighted like [buildLinkHighlightedAnnotatedString], reusing the links of
     * [previous] outside the edited lines so a keystroke scans only its own paragraph instead of the
     * whole note. Links never span a line break, so the result matches a full rescan.
     */
    fun updateLinkHighlightedAnnotatedString(
        previous: AnnotatedString,
        newText: String,
        linkColor: Color,
    ): AnnotatedString {
        if (previous.spanStyles.any { it.item.color != linkColor }) {
            return buildLinkHighlightedAnnotatedString(newText, linkColor)
        }
        if (previous.text == newText) return previous
        val previousLinks =
            previous
                .getStringAnnotations(NOTE_LINK_ANNOTATION_TAG, 0, previous.length)
                .map { LinkRange(it.start, it.end, it.item) }
        val links = updateLinkRanges(previous.text, previousLinks, newText, ::findLinks)
        return linkAnnotatedString(newText, links, linkColor)
    }

    /**
     * The links of [newText], given [previousLinks] for [previousText]: links outside the lines the
     * edit touched are kept (shifted past it), and only those lines are passed to [findLinks].
     */
    internal fun updateLinkRanges(
        previousText: String,
        previousLinks: List<LinkRange>,
        newText: String,
        findLinks: (String) -> List<LinkRange>,
    ): List<LinkRange> {
        if (previousText == newText) return previousLinks
        val minLength = minOf(previousText.length, newText.length)
        var prefix = 0
        while (prefix < minLength && previousText[prefix] == newText[prefix]) prefix++
        var suffix = 0
        while (
            suffix < minLength - prefix &&
            previousText[previousText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
        ) {
            suffix++
        }
        val delta = newText.length - previousText.length
        val regionStart = if (prefix == 0) 0 else newText.lastIndexOf('\n', prefix - 1) + 1
        val regionEnd =
            newText.indexOf('\n', newText.length - suffix).let { if (it < 0) newText.length else it }
        val previousRegionEnd = regionEnd - delta
        val before = previousLinks.filter { it.end <= regionStart }
        val after =
            previousLinks
                .filter { it.start >= previousRegionEnd }
                .map { it.copy(start = it.start + delta, end = it.end + delta) }
        val edited =
            findLinks(newText.substring(regionStart, regionEnd))
                .map { it.copy(start = it.start + regionStart, end = it.end + regionStart) }
        return (before + edited + after).sortedBy { it.start }
    }

    private fun findLinks(text: String): List<LinkRange> {
        if (text.isEmpty()) return emptyList()
        val s = SpannableString(text)
        LinkifyCompat.addLinks(
            s,
            Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS,
        )
        return s.getSpans(0, s.length, URLSpan::class.java).mapNotNull { span ->
            val start = s.getSpanStart(span)
            val end = s.getSpanEnd(span)
            if (start >= 0 && end > start && end <= text.length) LinkRange(start, end, span.url) else null
        }
    }

    private fun linkAnnotatedString(
        text: String,
        links: List<LinkRange>,
        linkColor: Color,
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        return buildAnnotatedString {
            append(text)
            for (link in links) {
                addStyle(SpanStyle(color = linkColor), link.start, link.end)
                addStringAnnotation(NOTE_LINK_ANNOTATION_TAG, link.url, link.start, link.end)
            }
        }
    }

    fun linkUrlAtCharOffset(
        annotated: AnnotatedString,
        charOffset: Int,
    ): String? {
        val len = annotated.text.length
        if (len == 0) return null
        val idx = charOffset.coerceIn(0, len - 1)
        return annotated
            .getStringAnnotations(NOTE_LINK_ANNOTATION_TAG, idx, idx + 1)
            .firstOrNull()
            ?.item
    }
}
