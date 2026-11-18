package com.tk.quicksearch.tools.colorVisualizer

import com.tk.quicksearch.search.core.CalculatorState
import com.tk.quicksearch.tools.inlineSearch.InlineToolData

internal fun evaluateColorVisualizer(query: String, explicit: Boolean): CalculatorState? {
    val color = ColorVisualizerParser.parse(query, allowBareHex = explicit) ?: return null
    return CalculatorState(result = color.hex, toolData = color)
}

data class VisualizedColor(
    val argb: Int,
    val hex: String,
) : InlineToolData

/**
 * Strict, anchored color parser. The prefix checks avoid regex work for ordinary search queries.
 */
object ColorVisualizerParser {
    private val hexPattern = Regex("^#?([0-9a-fA-F]{6})$")
    private val rgbPattern =
        Regex("^rgb\\s*\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*\\)$", RegexOption.IGNORE_CASE)

    /**
     * [allowBareHex] permits a hex code without the leading `#`. Ordinary searches require the `#`
     * so that plain six-digit numbers are not mistaken for colors.
     */
    fun parse(query: String, allowBareHex: Boolean = false): VisualizedColor? {
        if (query.length !in 6..32) return null
        val first = query.firstOrNull() ?: return null
        return when {
            first == '#' -> parseHex(query)
            allowBareHex && (first.isDigit() || first.lowercaseChar() in 'a'..'f') -> parseHex(query)
            query.length >= 4 && query.regionMatches(0, "rgb", 0, 3, ignoreCase = true) -> parseRgb(query)
            else -> null
        }
    }

    private fun parseHex(query: String): VisualizedColor? {
        val value = hexPattern.matchEntire(query)?.groupValues?.get(1) ?: return null
        val rgb = value.toIntOrNull(16) ?: return null
        return VisualizedColor(0xFF000000.toInt() or rgb, "#${value.uppercase()}")
    }

    private fun parseRgb(query: String): VisualizedColor? {
        val match = rgbPattern.matchEntire(query) ?: return null
        val channels = (1..3).map { match.groupValues[it].toIntOrNull() ?: return null }
        if (channels.any { it !in 0..255 }) return null
        val rgb = (channels[0] shl 16) or (channels[1] shl 8) or channels[2]
        return VisualizedColor(0xFF000000.toInt() or rgb, "#%02X%02X%02X".format(channels[0], channels[1], channels[2]))
    }
}
