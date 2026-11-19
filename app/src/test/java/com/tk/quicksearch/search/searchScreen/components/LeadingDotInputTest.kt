package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeadingDotInputTest {
    @Test
    fun detectsKeyboardSpaceAfterLeadingDot() {
        assertTrue(hasAutoSpaceAfterLeadingDot(previousText = ".", newText = ". m"))
        assertTrue(hasAutoSpaceAfterLeadingDot(previousText = "", newText = ". "))
        assertTrue(hasAutoSpaceAfterLeadingDot(previousText = ".", newText = ". "))
        assertTrue(hasAutoSpaceAfterLeadingDot(previousText = ".m", newText = ". mk"))
    }

    @Test
    fun removesInsertedSpaceAndShiftsCursor() {
        val state = TextFieldState(".m", TextRange(2))
        state.typed(". mk", cursor = 4)
        assertEquals(".mk", state.text.toString())
        assertEquals(TextRange(3), state.selection)
    }

    @Test
    fun turnsLineBreaksIntoSpaces() {
        val state = TextFieldState("", TextRange(0))
        state.typed("a\nb\r\nc", cursor = 6)
        assertEquals("a b  c", state.text.toString())
        assertEquals(TextRange(6), state.selection)
    }

    @Test
    fun keepsSpacesTheUserTyped() {
        assertFalse(hasAutoSpaceAfterLeadingDot(previousText = ".mkv", newText = ".mkv "))
        assertFalse(hasAutoSpaceAfterLeadingDot(previousText = "a.", newText = "a. b"))
        assertFalse(hasAutoSpaceAfterLeadingDot(previousText = ". ", newText = ". m"))
        assertFalse(hasAutoSpaceAfterLeadingDot(previousText = "", newText = "a. b"))
    }

    /** Simulates an input edit: replaces the text, then runs the search field's input transformation. */
    private fun TextFieldState.typed(text: String, cursor: Int) {
        edit {
            replace(0, length, text)
            selection = TextRange(cursor)
            with(SearchQueryInputTransformation) { transformInput() }
        }
    }
}
