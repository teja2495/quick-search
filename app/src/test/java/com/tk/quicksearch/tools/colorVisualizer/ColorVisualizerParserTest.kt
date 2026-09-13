package com.tk.quicksearch.tools.colorVisualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorVisualizerParserTest {
    @Test
    fun parsesSixDigitHexWithOrWithoutHash() {
        assertEquals("#FF0000", ColorVisualizerParser.parse("#ff0000")?.hex)
        assertEquals("#00FF00", ColorVisualizerParser.parse("00ff00")?.hex)
    }

    @Test
    fun parsesRgbWithWhitespace() {
        val color = ColorVisualizerParser.parse("rgb(255, 0, 0)")

        assertEquals("#FF0000", color?.hex)
        assertEquals(0xFFFF0000.toInt(), color?.argb)
    }

    @Test
    fun rejectsNonColorAndOutOfRangeRgbInput() {
        assertNull(ColorVisualizerParser.parse("red wallpaper"))
        assertNull(ColorVisualizerParser.parse("rgb(256, 0, 0)"))
        assertNull(ColorVisualizerParser.parse("#FF00"))
    }
}
