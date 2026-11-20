package com.tk.quicksearch.search.glance

import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

/**
 * A turn-by-turn navigation title split into its instruction and a turn icon for it. Maps titles
 * read "250 ft · Turn right to stay on Cypress Cove Cir", where the distance already shows in the
 * row's pill. [icon] is null when the text names no maneuver.
 */
internal data class NavigationInstruction(
    val title: String,
    val icon: ImageVector?,
)

private val LEADING_DISTANCE =
    Regex("""^\s*\d+(?:[.,]\d+)?\s*(?:ft|feet|mi|miles?|yd|yards?|km|m|meters?|metres?)\s*[·•\-–—:]\s*""", RegexOption.IGNORE_CASE)

internal fun parseNavigationInstruction(title: String): NavigationInstruction {
    val stripped = title.replace(LEADING_DISTANCE, "").trim().ifEmpty { title }
    val text = stripped.lowercase()
    val left = "left" in text
    val right = "right" in text
    val icon =
        when {
            "u-turn" in text || "u turn" in text -> if (left) MaterialSymbols.UTurnLeft else MaterialSymbols.UTurnRight
            !left && !right ->
                MaterialSymbols.Straight.takeIf { "continue" in text || "straight" in text || "head" in text }
            "sharp" in text -> if (right) MaterialSymbols.TurnSharpRight else MaterialSymbols.TurnSharpLeft
            "slight" in text || "bear" in text || "keep" in text ->
                if (right) MaterialSymbols.TurnSlightRight else MaterialSymbols.TurnSlightLeft
            else -> if (right) MaterialSymbols.TurnRight else MaterialSymbols.TurnLeft
        }
    return NavigationInstruction(title = stripped, icon = icon)
}

private val ARRIVAL_PREFIX = Regex("""^\s*Arriv(?:e|es|ing|al)\b[:\s]*""", RegexOption.IGNORE_CASE)

/** Maps' "Arrive 5:03 PM" line as "ETA 5:03 PM"; other text and languages pass through unchanged. */
internal fun String.asEtaText(): String = replace(ARRIVAL_PREFIX, "ETA ")
