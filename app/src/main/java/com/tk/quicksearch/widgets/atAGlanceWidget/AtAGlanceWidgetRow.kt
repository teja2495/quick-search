package com.tk.quicksearch.widgets.atAGlanceWidget

import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Which color an icon is tinted: home's accent tint maps to the widget's title color. */
internal enum class AtAGlanceWidgetTint {
    TITLE,
    SUBTITLE,
    ERROR,
}

/** The icon at the start of an At a Glance widget row. */
internal sealed interface AtAGlanceWidgetIcon {
    /** A Material icon. */
    data class Vector(
        val vector: ImageVector,
        val tint: AtAGlanceWidgetTint = AtAGlanceWidgetTint.TITLE,
    ) : AtAGlanceWidgetIcon

    /** A drawable of the app's own. */
    data class Drawable(
        @DrawableRes val resId: Int,
        val tint: AtAGlanceWidgetTint = AtAGlanceWidgetTint.TITLE,
    ) : AtAGlanceWidgetIcon

    /** An app's own icon, shown untinted, falling back to a generic app icon. */
    data class App(
        val packageName: String,
    ) : AtAGlanceWidgetIcon

    /** Home's low battery icon: an outline, red when [isCritical], with a red fill tracking [percent]. */
    data class Battery(
        val percent: Int,
        val isCritical: Boolean,
    ) : AtAGlanceWidgetIcon
}

/** Home's title styles: bodyMedium, bodySmall (a code's source), bodyLarge (a timer's time), and red SemiBold. */
internal enum class AtAGlanceWidgetTitleStyle {
    BODY,
    SMALL,
    LARGE,
    CRITICAL,
}

/** Home's pill colors; accent colored pills use the neutral one. */
internal enum class AtAGlanceWidgetPillStyle {
    NEUTRAL,
    ERROR,
}

internal data class AtAGlanceWidgetPill(
    val text: String,
    val style: AtAGlanceWidgetPillStyle = AtAGlanceWidgetPillStyle.NEUTRAL,
)

/** Home's row buttons. */
internal enum class AtAGlanceWidgetButtonStyle {
    /** The red hang up circle. */
    HANG_UP,

    /** A filled circle around an icon, like a timer's Pause or Custom Info's retry. */
    TONAL,

    /** A bare icon, like a reminder's done check. */
    ICON,

    /** A filled pill with an icon and label, like a missed call's Call. */
    CHIP,

    /** A filled pill with an app icon or just a label, like a missed call's app; home outlines it. */
    OUTLINED_CHIP,
}

internal data class AtAGlanceWidgetButton(
    val tap: AtAGlanceWidgetTap,
    val style: AtAGlanceWidgetButtonStyle,
    val label: String,
    val icon: ImageVector? = null,
    /** The posting app's own button icon, drawn in place of [icon]. */
    val appIcon: Icon? = null,
    /** Shows this app's icon, for [AtAGlanceWidgetButtonStyle.OUTLINED_CHIP]. */
    val appPackage: String? = null,
    /** The circle's size, for [AtAGlanceWidgetButtonStyle.TONAL]. */
    val size: Dp = 36.dp,
)

/** What a tap on a row does; read back by [AtAGlanceWidgetActionActivity] when the row is tapped. */
internal enum class AtAGlanceWidgetTapType {
    /** Opens the notification [AtAGlanceWidgetTap.key], or launches [AtAGlanceWidgetTap.packageName]. */
    NOTIFICATION,
    OTP,
    MISSED_CALLS,
    ALARM,
    REMINDER,
    BIRTHDAY,
    CALENDAR_EVENT,
    CUSTOM_INFO,
    LOW_BATTERY,

    /** Not tappable; only names the row. */
    CHARGING,
    LOW_STORAGE,
    DO_NOT_DISTURB,
    AIRPLANE_MODE,
    HOTSPOT,
    WIFI_SIGN_IN,
    FLASHLIGHT,

    /** Ends the ongoing call [AtAGlanceWidgetTap.key], or opens its call screen when it can't. */
    HANG_UP_CALL,

    /** Marks reminder [AtAGlanceWidgetTap.id] done. */
    REMINDER_DONE,

    /** Opens Noted task [AtAGlanceWidgetTap.key] in Noted. */
    NOTED_TASK,

    /** Completes Noted task [AtAGlanceWidgetTap.key] in Noted. */
    NOTED_TASK_DONE,

    /** Runs Custom Info item [AtAGlanceWidgetTap.id] again. */
    CUSTOM_INFO_RETRY,

    /** Calls back the missed call [AtAGlanceWidgetTap.key]. */
    MISSED_CALL_BACK,

    /** Presses button [AtAGlanceWidgetTap.id] of timer [AtAGlanceWidgetTap.key]. */
    TIMER_ACTION,
}

/** What the row's close button hides, matching the rows home lets the user dismiss. */
internal enum class AtAGlanceWidgetDismissType {
    NOW_PLAYING,
    OTP,
    ALARM,
    REMINDER,
    NOTED_TASK,
    MISSED_CALLS,
    APP_NOTIFICATION,
    WEATHER,
    FINISHED_PROGRESS,
    BIRTHDAY,
    TOMORROW_EVENT,
    TODAY_EVENT,
    CUSTOM_INFO,
    WIFI_SIGN_IN,
    CHARGING,
    LOW_STORAGE,
}

internal data class AtAGlanceWidgetTap(
    val type: AtAGlanceWidgetTapType,
    val key: String? = null,
    val packageName: String? = null,
    val id: Long = 0L,
    /** Extra text the tap needs, such as a birthday contact's lookup key. */
    val extra: String? = null,
)

/** One At a Glance item as the widget draws it; compared by value to skip redraws that would change nothing. */
internal data class AtAGlanceWidgetRow(
    /** Stable across refreshes, so the launcher keeps the scroll position. */
    val key: String,
    val icon: AtAGlanceWidgetIcon,
    val title: String,
    val subtitle: String? = null,
    val tap: AtAGlanceWidgetTap,
    /** Shows a close button that hides the item as on home; it reads the tap's key and id. */
    val dismiss: AtAGlanceWidgetDismissType? = null,
    /** False draws the row without a tap target. */
    val tappable: Boolean = true,
    val titleStyle: AtAGlanceWidgetTitleStyle = AtAGlanceWidgetTitleStyle.BODY,
    val titleMaxLines: Int = 1,
    val subtitleMaxLines: Int = Int.MAX_VALUE,
    /** Draws the subtitle at 20sp, like a workout's stats. */
    val largeSubtitle: Boolean = false,
    val pill: AtAGlanceWidgetPill? = null,
    /** A one-time code below the text; tapping it copies, as on home. */
    val code: String? = null,
    /** Small text below the rest, like an app notification's app and age. */
    val footer: String? = null,
    /** Draws a progress bar below the text, from 0 to 1. */
    val progress: Float? = null,
    /** Buttons below the text, like a missed call's app chips. */
    val chips: List<AtAGlanceWidgetButton> = emptyList(),
    /** Buttons between the text and the close button. */
    val trailing: List<AtAGlanceWidgetButton> = emptyList(),
    /** Puts the close button in the top right corner rather than centered. */
    val dismissAtTop: Boolean = false,
)

/** The rows to draw and when they next change on their own (an alarm entering its window, a code expiring). */
internal class AtAGlanceWidgetSnapshot(
    val rows: List<AtAGlanceWidgetRow>,
    val nextChangeMillis: Long?,
)
