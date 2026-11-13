package com.tk.quicksearch.widgets.worldClockWidget

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

/** What fits in one clock's column: text sizes, and whether the city and day lines have room. */
internal data class WorldClockCellLayout(
    val timeSizeSp: Float,
    val labelSizeSp: Float,
    val subtitleSizeSp: Float,
    val showLabel: Boolean,
    val showSubtitle: Boolean,
)

/** How wide each line of a clock is, in dp per sp of text size, for its widest text. */
internal data class WorldClockTextWidths(
    val time: Float,
    val label: Float,
    val subtitle: Float,
)

/** Sizing shared by the placed widget and the configure-screen preview so both lay out alike. */
internal object WorldClockLayout {
    val HORIZONTAL_PADDING = 12.dp
    val VERTICAL_PADDING = 8.dp
    val CELL_SPACING = 8.dp
    const val PLACEHOLDER_TEXT_SIZE_SP = 14f

    /** Height of the configure-screen preview, roughly a 4x1 widget on a phone launcher. */
    val PREVIEW_HEIGHT = 110.dp

    private const val MIN_TIME_SIZE_SP = 10f

    /** Leaves a little room so a measured time never ends up ellipsized by rounding. */
    private const val FIT_MARGIN = 0.94f
    private const val LINE_HEIGHT_RATIO = 1.25f
    private const val LABEL_RATIO = 0.5f
    private const val LABEL_MIN_SP = 10f
    private const val SUBTITLE_RATIO = 0.9f

    /** The largest time that fits [clockCount] clocks in a widget of [widthDp] x [heightDp]. */
    fun maxTimeSizeSp(
        widthDp: Float,
        heightDp: Float,
        clockCount: Int,
        widths: WorldClockTextWidths,
        fontScale: Float,
    ): Float = cellLayout(widthDp, heightDp, clockCount, Float.MAX_VALUE, widths, fontScale).timeSizeSp

    /**
     * Sizes for [clockCount] clocks side by side in a widget of [widthDp] x [heightDp]. The time is
     * [preferredTimeSizeSp] unless that would not fit its column. The city and day lines scale with
     * the time but stop at the largest size at which the longest of them still fits, so they are not
     * cut off; they are dropped, day first, when there is no height left for them.
     */
    fun cellLayout(
        widthDp: Float,
        heightDp: Float,
        clockCount: Int,
        preferredTimeSizeSp: Float,
        widths: WorldClockTextWidths,
        fontScale: Float,
    ): WorldClockCellLayout {
        val count = clockCount.coerceAtLeast(1)
        val scale = fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
        val innerWidth = widthDp - HORIZONTAL_PADDING.value * 2 - CELL_SPACING.value * (count - 1)
        val cellWidth = (innerWidth / count).coerceAtLeast(1f) * FIT_MARGIN
        val innerHeight = (heightDp - VERTICAL_PADDING.value * 2).coerceAtLeast(1f)
        fun fitSp(widthDpPerSp: Float) = cellWidth / widthDpPerSp.coerceAtLeast(0.01f)

        val fitHeightSp = innerHeight / (LINE_HEIGHT_RATIO * scale)
        val timeSize = minOf(preferredTimeSizeSp, fitSp(widths.time), fitHeightSp).coerceAtLeast(MIN_TIME_SIZE_SP)
        val labelSize = minOf(timeSize * LABEL_RATIO, fitSp(widths.label)).coerceAtLeast(LABEL_MIN_SP)
        val subtitleSize = minOf(labelSize * SUBTITLE_RATIO, fitSp(widths.subtitle)).coerceAtLeast(LABEL_MIN_SP)

        val timeHeight = timeSize * LINE_HEIGHT_RATIO * scale
        val labelHeight = labelSize * LINE_HEIGHT_RATIO * scale
        val subtitleHeight = subtitleSize * LINE_HEIGHT_RATIO * scale
        val showLabel = innerHeight >= timeHeight + labelHeight
        val showSubtitle = showLabel && innerHeight >= timeHeight + labelHeight + subtitleHeight
        return WorldClockCellLayout(
            timeSizeSp = timeSize,
            labelSizeSp = labelSize,
            subtitleSizeSp = subtitleSize,
            showLabel = showLabel,
            showSubtitle = showSubtitle,
        )
    }
}

/** Clock text patterns in the user's locale, as TextClock and DateTimeFormatter patterns. */
internal object WorldClockFormats {
    /** A wide time to measure, 10:58 AM UTC, so every other time fits where it does. */
    private const val SAMPLE_TIME_MILLIS = (10 * 60 + 58) * 60_000L

    /** How big the AM/PM marker is next to the digits, as on most clock widgets. */
    private const val MARKER_SIZE_RATIO = 0.5f
    private const val MEASURE_SIZE_SP = 100f
    private val SAMPLE_WEEK_START: LocalDate = LocalDate.of(2024, 1, 1)

    fun is24Hour(context: Context): Boolean = DateFormat.is24HourFormat(context)

    fun timePattern(is24Hour: Boolean): String =
        DateFormat.getBestDateTimePattern(Locale.getDefault(), if (is24Hour) "Hm" else "hm")

    /**
     * How wide, in dp per sp of text size, the widest time, city name ([names]) and weekday are in
     * the user's format and font scale. Measured with the platform sans-serif, which both the widget
     * and the preview use.
     */
    fun measure(
        context: Context,
        is24Hour: Boolean,
        names: List<String>,
    ): WorldClockTextWidths {
        val metrics = context.resources.displayMetrics
        val fontScale = context.resources.configuration.fontScale.takeIf { it > 0f } ?: 1f
        fun widthPerSp(
            texts: List<CharSequence>,
            typeface: Typeface,
        ): Float {
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    textSize = MEASURE_SIZE_SP * fontScale * metrics.density
                }
            return (texts.maxOfOrNull { measureText(paint, it) } ?: 0f) / metrics.density / MEASURE_SIZE_SP
        }
        val time = formatTime(timeFormat(is24Hour), SAMPLE_TIME_MILLIS, "UTC")
        val weekdayFormatter =
            runCatching { DateTimeFormatter.ofPattern(subtitlePattern()) }.getOrDefault(DateTimeFormatter.ofPattern("EEE"))
        val weekdays = DayOfWeek.entries.map { SAMPLE_WEEK_START.with(it).format(weekdayFormatter) }
        return WorldClockTextWidths(
            time = widthPerSp(listOf(time), Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)),
            label = widthPerSp(names, Typeface.create("sans-serif-medium", Typeface.NORMAL)),
            subtitle = widthPerSp(weekdays, Typeface.SANS_SERIF),
        )
    }

    /**
     * The TextClock format for the time. In 12-hour time an AM/PM marker that follows the digits is
     * drawn at [MARKER_SIZE_RATIO], so the digits get more of a narrow column. The span starts at
     * the space before the marker: TextClock keeps spans while it swaps the "a" for "PM", and one
     * reaching past the replaced text on the left side grows over it rather than being dropped.
     */
    fun timeFormat(is24Hour: Boolean): CharSequence {
        val pattern = timePattern(is24Hour)
        if (is24Hour) return pattern
        val marker = pattern.indexOf('a')
        if (marker <= 0 || !pattern[marker - 1].isWhitespace() || pattern.substring(0, marker).isBlank()) return pattern
        var end = marker
        while (end < pattern.length && pattern[end] == 'a') end++
        return SpannableString(pattern).apply {
            setSpan(RelativeSizeSpan(MARKER_SIZE_RATIO), marker - 1, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
    }

    /** [millis] in [zoneId] as TextClock would show it with [format], spans included. */
    fun formatTime(
        format: CharSequence,
        millis: Long,
        zoneId: String,
    ): CharSequence =
        runCatching {
            DateFormat.format(
                format,
                Calendar.getInstance(java.util.TimeZone.getTimeZone(zoneId)).apply { timeInMillis = millis },
            )
        }.getOrDefault("")

    /** [text]'s width with [paint], counting text under a [RelativeSizeSpan] at its size. */
    private fun measureText(
        paint: Paint,
        text: CharSequence,
    ): Float {
        if (text !is Spanned) return paint.measureText(text.toString())
        var width = 0f
        var start = 0
        while (start < text.length) {
            val next = text.nextSpanTransition(start, text.length, RelativeSizeSpan::class.java)
            val scale = text.getSpans(start, next, RelativeSizeSpan::class.java).fold(1f) { acc, span -> acc * span.sizeChange }
            width += paint.measureText(text, start, next) * scale
            start = next
        }
        return width
    }

    /** The weekday under a clock, e.g. "Thu". */
    fun subtitlePattern(): String = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE")
}
