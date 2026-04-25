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

    fun normalize(value: String): String = SearchTextNormalizer.normalizeForSearch(value.trim())

    fun buildLinkHighlightedAnnotatedString(
        text: String,
        linkColor: Color,
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        val s = SpannableString(text)
        LinkifyCompat.addLinks(
            s,
            Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS,
        )
        val spans = s.getSpans(0, s.length, URLSpan::class.java)
        return buildAnnotatedString {
            append(text)
            for (span in spans) {
                val start = s.getSpanStart(span)
                val end = s.getSpanEnd(span)
                if (start >= 0 && end > start && end <= text.length) {
                    addStyle(SpanStyle(color = linkColor), start, end)
                    addStringAnnotation(NOTE_LINK_ANNOTATION_TAG, span.url, start, end)
                }
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
