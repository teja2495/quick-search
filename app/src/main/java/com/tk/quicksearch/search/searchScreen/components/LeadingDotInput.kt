package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

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

/** Most lines the search field wraps to before it scrolls; it grows with the query up to this. */
internal const val SearchFieldMaxLines = 3

/**
 * Cleans up what the user types into the search field. The field is multi-line only so long
 * queries wrap, so pasted line breaks become spaces (same length, so the selection stays valid).
 * A space the keyboard auto-inserts after a leading dot is removed; the buffer shifts the cursor.
 * Programmatic edits (`TextFieldState.edit`) skip this.
 */
internal object SearchQueryInputTransformation : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        for (index in 0 until length) {
            val char = charAt(index)
            if (char == '\n' || char == '\r') replace(index, index + 1, " ")
        }
        if (hasAutoSpaceAfterLeadingDot(originalText.toString(), asCharSequence().toString())) {
            replace(1, 2, "")
        }
    }
}
