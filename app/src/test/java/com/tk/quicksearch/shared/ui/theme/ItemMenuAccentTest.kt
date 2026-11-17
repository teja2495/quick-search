package com.tk.quicksearch.shared.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemMenuAccentTest {
    @Test
    fun preservesAnAlreadyLegibleAccent() {
        val accent = Color(0xFF81C784)
        assertEquals(
            accent,
            contrastingItemMenuAccent(accent, Color.White, listOf(Color(0xFF35343A))),
        )
    }

    @Test
    fun strengthensFaintAccentsAcrossLightAndDarkPopupSurfaces() {
        val palettes = listOf(
            Triple(Color(0xFF303050), Color(0xFFE6E1E5), listOf(Color(0xFF211F26), Color(0xFF36343B))),
            Triple(Color(0xFFE8E0FF), Color(0xFF1D1B20), listOf(Color(0xFFFFF7FF), Color(0xFFE6E0E9))),
        )
        palettes.forEach { (accent, foreground, backgrounds) ->
            val result = contrastingItemMenuAccent(accent, foreground, backgrounds)
            backgrounds.forEach { background ->
                val a = result.luminance() + 0.05f
                val b = background.luminance() + 0.05f
                assertTrue(maxOf(a, b) / minOf(a, b) >= 4.5f)
            }
            assertEquals(1f, result.alpha)
        }
    }
}
