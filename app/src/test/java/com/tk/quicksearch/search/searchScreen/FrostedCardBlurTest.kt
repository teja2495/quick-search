package com.tk.quicksearch.search.searchScreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrostedCardBlurTest {
    @Test
    fun uniformImageIsUnchanged() {
        val color = 0xFF336699.toInt()
        val pixels = IntArray(8 * 5) { color }
        boxBlurArgb(pixels, width = 8, height = 5, radius = 3)
        pixels.forEach { assertEquals(color, it) }
    }

    @Test
    fun singleBrightPixelSpreadsToNeighboursAndStaysOpaque() {
        val width = 9
        val height = 9
        val pixels = IntArray(width * height) { 0xFF000000.toInt() }
        pixels[4 * width + 4] = 0xFFFFFFFF.toInt()
        boxBlurArgb(pixels, width, height, radius = 1)

        val center = pixels[4 * width + 4]
        val neighbour = pixels[4 * width + 5]
        val far = pixels[0]
        assertTrue((center and 0xFF) > 0)
        assertEquals(center and 0xFF, neighbour and 0xFF)
        assertEquals(0, far and 0xFF)
        pixels.forEach { assertEquals(0xFF, it ushr 24) }
    }
}
