package com.tk.quicksearch.reminders

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle

/** Colors the dates and times the reminder parser recognizes in the title, as typed. */
internal class ReminderNaturalLanguageOutputTransformation(
    private val highlightColor: Color,
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        ReminderNaturalLanguageParser.parse(asCharSequence().toString())?.highlightedRanges.orEmpty().forEach { range ->
            addStyle(SpanStyle(color = highlightColor), range.start, range.endExclusive)
        }
    }
}
