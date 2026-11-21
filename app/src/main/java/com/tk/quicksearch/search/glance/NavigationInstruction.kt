package com.tk.quicksearch.search.glance

import android.text.format.DateUtils
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import java.util.Calendar

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
            "u-turn" in text || "u turn" in text -> if (left) NavigationArrows.UTurnLeft else NavigationArrows.UTurnRight
            !left && !right ->
                MaterialSymbols.Straight.takeIf { "continue" in text || "straight" in text || "head" in text }
            "sharp" in text -> if (right) NavigationArrows.SharpRight else NavigationArrows.SharpLeft
            "slight" in text || "bear" in text || "keep" in text ->
                if (right) NavigationArrows.SlightRight else NavigationArrows.SlightLeft
            else -> if (right) NavigationArrows.TurnRight else NavigationArrows.TurnLeft
        }
    return NavigationInstruction(title = stripped, icon = icon)
}

private val ARRIVAL_PREFIX = Regex("""^\s*Arriv(?:e|es|ing|al)\b[:\s]*""", RegexOption.IGNORE_CASE)

/** Maps' "Arrive 5:03 PM" line as "ETA 5:03 PM"; other text and languages pass through unchanged. */
internal fun String.asEtaText(now: Long = System.currentTimeMillis()): String {
    val eta = replace(ARRIVAL_PREFIX, "ETA ")
    val relative = relativeArrival(eta, now) ?: return eta
    return "$eta · $relative"
}

private val CLOCK_TIME = Regex("""(\d{1,2}):(\d{2})(?:\s*([AaPp])\.?[Mm]\.?)?""")
private const val MAX_RELATIVE_MINUTES = 12 * 60

/** "in 3 min" for the clock time in [text], in the device language, or null when none parses or it is over 12 hours out. */
private fun relativeArrival(
    text: String,
    now: Long,
): String? {
    val match = CLOCK_TIME.find(text) ?: return null
    var hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    val meridiem = match.groupValues[3].lowercase()
    if (meridiem.isNotEmpty()) {
        if (hour !in 1..12) return null
        hour = hour % 12 + if (meridiem == "p") 12 else 0
    }
    if (hour > 23 || minute > 59) return null
    val target =
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // A time already behind us means tomorrow, as when arriving after midnight.
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    if (target - now > MAX_RELATIVE_MINUTES * DateUtils.MINUTE_IN_MILLIS) return null
    return DateUtils
        .getRelativeTimeSpanString(target, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
        .toString()
        .trimEnd('.')
}
