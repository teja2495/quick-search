package com.tk.quicksearch.shared.util

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.tk.quicksearch.search.utils.PhoneNumberUtils

private const val PHONE_ANNOTATION_TAG = "PHONE"
private const val EMAIL_ANNOTATION_TAG = "EMAIL"

private data class LinkifiedMatch(
    val range: IntRange,
    val tag: String,
    val value: String,
)

fun buildPhoneEmailLinkifiedText(
    text: String,
    linkColor: Color,
): AnnotatedString {
    val phonePattern = Regex("""\+?\d[\d\s().-]{6,}""")
    val emailPattern = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    fun rangesOverlap(a: IntRange, b: IntRange): Boolean = a.first <= b.last && b.first <= a.last

    val matches = mutableListOf<LinkifiedMatch>()

    phonePattern.findAll(text).forEach { matchResult ->
        val cleanedNumber = PhoneNumberUtils.cleanPhoneNumber(matchResult.value)
        if (cleanedNumber != null) {
            matches.add(
                LinkifiedMatch(
                    range = matchResult.range,
                    tag = PHONE_ANNOTATION_TAG,
                    value = cleanedNumber,
                ),
            )
        }
    }

    emailPattern.findAll(text).forEach { matchResult ->
        matches.add(
            LinkifiedMatch(
                range = matchResult.range,
                tag = EMAIL_ANNOTATION_TAG,
                value = matchResult.value,
            ),
        )
    }

    val dedupedMatches =
        matches
            .sortedBy { it.range.first }
            .fold(mutableListOf<LinkifiedMatch>()) { acc, match ->
                if (acc.none { rangesOverlap(it.range, match.range) }) {
                    acc.add(match)
                }
                acc
            }

    return buildAnnotatedString {
        var lastIndex = 0
        dedupedMatches.forEach { match ->
            val startIndex = match.range.first
            val endIndex = match.range.last + 1
            if (startIndex > lastIndex) {
                append(text.substring(lastIndex, startIndex))
            }
            pushStringAnnotation(tag = match.tag, annotation = match.value)
            withStyle(
                style =
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
            ) {
                append(text.substring(startIndex, endIndex))
            }
            pop()
            lastIndex = endIndex
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
fun PhoneEmailLinkifiedText(
    text: String,
    style: TextStyle,
    color: Color,
    linkColor: Color,
    onPhoneNumberClick: (String) -> Unit,
    onEmailClick: (String) -> Unit,
) {
    val annotatedText = buildPhoneEmailLinkifiedText(text = text, linkColor = linkColor)
    @Suppress("DEPRECATION")
    ClickableText(
        text = annotatedText,
        style = style.copy(color = color),
        onClick = { offset ->
            annotatedText
                .getStringAnnotations(start = offset, end = offset)
                .firstOrNull()
                ?.let { annotation ->
                    when (annotation.tag) {
                        PHONE_ANNOTATION_TAG -> onPhoneNumberClick(annotation.item)
                        EMAIL_ANNOTATION_TAG -> onEmailClick(annotation.item)
                    }
                }
        },
    )
}
