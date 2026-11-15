package com.tk.quicksearch.shared.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** Strengthens a custom accent without dimming active controls or replacing its hue outright. */
internal fun contrastingItemMenuAccent(
    accent: Color,
    foreground: Color,
    backgrounds: List<Color>,
): Color {
    fun minimumContrast(color: Color): Float = backgrounds.minOf { background ->
        val a = color.luminance() + 0.05f
        val b = background.luminance() + 0.05f
        maxOf(a, b) / minOf(a, b)
    }

    val opaqueAccent = accent.copy(alpha = 1f)
    if (minimumContrast(opaqueAccent) >= 4.5f) return opaqueAccent
    // Small, bounded steps preserve as much of the accent as possible. The foreground is the
    // theme's contrasting text role, so this also handles pale accents in light mode and vice versa.
    for (step in 1..20) {
        val candidate = lerp(opaqueAccent, foreground.copy(alpha = 1f), step / 20f)
        if (minimumContrast(candidate) >= 4.5f) return candidate
    }
    return foreground.copy(alpha = 1f)
}
