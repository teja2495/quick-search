package com.tk.quicksearch.search.notes

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString

/**
 * Colors the links in a note field as typed. Keeps the last highlighted text so a keystroke
 * rescans only its own lines ([NotesTextUtils.updateLinkHighlightedAnnotatedString]), and answers
 * which link a tap landed on.
 */
class NoteLinkOutputTransformation(
    private val linkColor: Color,
) : OutputTransformation {
    private var highlighted = AnnotatedString("")

    private fun highlight(text: String): AnnotatedString =
        NotesTextUtils.updateLinkHighlightedAnnotatedString(highlighted, text, linkColor).also {
            highlighted = it
        }

    override fun TextFieldBuffer.transformOutput() {
        highlight(asCharSequence().toString()).spanStyles.forEach { span ->
            addStyle(span.item, span.start, span.end)
        }
    }

    /** The URL of the link at [charOffset] in [text], or null when the tap missed every link. */
    fun linkUrlAt(text: String, charOffset: Int): String? =
        NotesTextUtils.linkUrlAtCharOffset(highlight(text), charOffset)
}
