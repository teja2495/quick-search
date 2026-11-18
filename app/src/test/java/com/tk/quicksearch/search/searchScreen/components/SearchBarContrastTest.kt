package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchBarContrastTest {
    @Test
    fun `replaces text that matches the custom fill in either theme`() {
        val darkFill = Color(0xFF1F1B24)
        val lightFill = Color(0xFFE0E0E0)

        assertEquals(Color.White, darkFill.readableOn(darkFill))
        assertEquals(Color.Black, lightFill.readableOn(lightFill))
    }

    @Test
    fun `keeps the preferred color when it already has sufficient contrast`() {
        val text = Color(0xFF1F1B24)
        assertEquals(text, text.readableOn(Color.White))
    }

    @Test
    fun `chooses a readable fallback even on medium fills`() {
        for (channel in 0..255) {
            val fill = Color(channel, channel, channel)
            val foreground = fill.readableOn(fill)
            val lighter = maxOf(fill.luminance(), foreground.luminance())
            val darker = minOf(fill.luminance(), foreground.luminance())
            assertTrue("Insufficient contrast at channel $channel", (lighter + 0.05f) / (darker + 0.05f) >= 4.5f)
        }
    }
}
