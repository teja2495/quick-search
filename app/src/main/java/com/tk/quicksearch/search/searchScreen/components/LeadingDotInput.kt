package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Keyboards auto-insert a space after punctuation, turning a typed ".mkv" into ". mkv", which
 * breaks file extension queries. Detects a space newly appearing right after a leading dot, whether
 * the keyboard adds it on its own edit, with the next character, or on a later composing edit.
 */
internal fun hasAutoSpaceAfterLeadingDot(
    previousText: String,
    newText: String,
): Boolean =
    newText.startsWith(". ") &&
        !previousText.startsWith(". ") &&
        (previousText.isEmpty() || previousText.startsWith("."))

internal fun TextFieldValue.withoutAutoSpaceAfterLeadingDot(previousText: String): TextFieldValue {
    if (!hasAutoSpaceAfterLeadingDot(previousText, text)) return this
    fun shift(index: Int) = if (index > 1) index - 1 else index
    return TextFieldValue(
        text = text.removeRange(1, 2),
        selection = TextRange(shift(selection.start), shift(selection.end)),
        composition = composition?.let { TextRange(shift(it.start), shift(it.end)) },
    )
}

/** Most lines the search field wraps to before it scrolls; it grows with the query up to this. */
internal const val SearchFieldMaxLines = 3

/**
 * The search field is multi-line only so long queries wrap; pasted line breaks become spaces. The
 * length is unchanged, so the selection stays valid.
 */
internal fun TextFieldValue.withLineBreaksAsSpaces(): TextFieldValue =
    if (text.any { it == '\n' || it == '\r' }) {
        copy(text = text.replace('\n', ' ').replace('\r', ' '))
    } else {
        this
    }
