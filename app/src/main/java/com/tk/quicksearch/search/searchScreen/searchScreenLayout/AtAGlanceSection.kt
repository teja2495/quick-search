package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import com.tk.quicksearch.search.searchScreen.shared.homeSectionTitleStyle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.reminders.ReminderEditorRequests
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.appSettings.LocalOpenAppSettingDestination
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * One row of the home At a Glance card. Today's calendar events are hosted by the calendar card
 * itself; every other glanceable source (ongoing calls, one-time codes, timers, alarm, reminders, missed calls,
 * App notifications, battery, Wi-Fi sign-in, progress notifications, workouts, weather, birthdays, tomorrow's events, Custom Info,
 * Do Not Disturb, airplane mode, hotspot, flashlight, storage, and future ones) contributes rows here.
 * Rows sit inside the card's inset and follow CalendarEventRow: 7dp before a 24dp icon, then 12dp
 * to the text.
 */
internal class AtAGlanceItem(
    val key: String,
    val content: @Composable () -> Unit,
)

/**
 * The At a Glance rows to lay out. When the last row goes away these stay the last rows shown
 * until the card has collapsed ([visibility] finishes hiding), so the card can animate away; [live]
 * is always the current rows, for hosts that stay on screen, such as the calendar card.
 */
internal class AtAGlanceItems(
    private val shown: List<AtAGlanceItem>,
    val live: List<AtAGlanceItem>,
    val visibility: MutableTransitionState<Boolean>,
) : List<AtAGlanceItem> by shown

/** Not snapshot state; only read while composing. */
private class RetainedAtAGlanceItems {
    var lastShown: List<AtAGlanceItem> = emptyList()
    var visibility: MutableTransitionState<Boolean>? = null
}

/**
 * Keeps [live]'s last rows around while the card collapses after its last row goes. While
 * [enabled] is off (a query is typed) nothing animates, so returning home shows the card at once.
 */
@Composable
private fun rememberRetainedAtAGlanceItems(
    enabled: Boolean,
    live: List<AtAGlanceItem>,
): AtAGlanceItems {
    val holder = remember { RetainedAtAGlanceItems() }
    val visibility =
        holder.visibility?.takeIf { enabled } ?: MutableTransitionState(live.isNotEmpty()).also { holder.visibility = it }
    if (!enabled) holder.visibility = null
    if (live.isNotEmpty()) holder.lastShown = live
    // Reading the transition state here recomposes once the card has finished collapsing.
    val collapsed = !visibility.targetState && visibility.isIdle
    val shown =
        when {
            live.isNotEmpty() -> live
            enabled && !collapsed -> holder.lastShown
            else -> emptyList()
        }
    SideEffect { visibility.targetState = live.isNotEmpty() }
    return AtAGlanceItems(shown = shown, live = live, visibility = visibility)
}

/**
 * Collects the non-calendar At a Glance rows, most urgent at the top. In the [reversed] (bottom
 * search bar) layout the source groups swap places, so the most urgent sits nearest the search bar,
 * but rows within a group keep their order, matching how the calendar card never reverses its own
 * events.
 */
@Composable
internal fun rememberAtAGlanceItems(
    enabled: Boolean,
    reversed: Boolean,
    onShowContactMethods: (ContactInfo) -> Unit,
): AtAGlanceItems {
    val battery = rememberBatteryGlances(enabled)
    val notifications = rememberNotificationGlances(enabled)
    val alarm = rememberUpcomingAlarmGlance(enabled)
    val reminders = rememberUpcomingRemindersGlance(enabled)
    val birthdays = rememberBirthdaysGlance(enabled, onShowContactMethods)
    val lowStorage = rememberLowStorageGlance(enabled)
    val doNotDisturb = rememberDoNotDisturbGlance(enabled)
    val wifiSignIn = rememberWifiSignInGlance(enabled)
    val flashlight = rememberFlashlightGlance(enabled)
    val airplaneMode = rememberAirplaneModeGlance(enabled)
    val hotspot = rememberHotspotGlance(enabled)
    val tomorrowEvents = rememberTomorrowEventsGlance(enabled)
    val customInfo = rememberCustomInfoGlance(enabled)
    val appNotifications = rememberAppNotificationsGlance(enabled, excludedKeys = setOfNotNull(notifications.otp?.key))
    // Most time-critical first: live and expiring items, then things due soon, then what needs a
    // look, then today's and tomorrow's context, and the passive device states last.
    val groups =
        listOf(
            notifications.ongoingCalls.map { call ->
                AtAGlanceItem(key = "ongoing-call-${call.key}") { OngoingCallRow(call, notifications.nowMillis) }
            },
            listOfNotNull(
                notifications.otp?.let { otp ->
                    AtAGlanceItem(key = "otp-${otp.key}") { OtpCodeRow(otp) { notifications.dismissOtp(otp) } }
                },
            ),
            notifications.timers.map { timer ->
                AtAGlanceItem(key = "timer-${timer.key}") { TimerRow(timer, notifications.nowMillis) }
            },
            listOfNotNull(alarm?.let { AtAGlanceItem(key = "alarm") { UpcomingAlarmRow(it) } }),
            reminders.reminders.map { reminder ->
                AtAGlanceItem(key = "reminder-${reminder.reminderId}") {
                    UpcomingReminderRow(
                        reminder = reminder,
                        nowMillis = reminders.nowMillis,
                        onClick = { ReminderEditorRequests.openEdit(reminder) },
                        onDone = { reminders.markDone(reminder) },
                        onSnooze = { reminders.snooze(reminder) },
                        onDismiss = { reminders.dismiss(reminder) },
                        onDelete = { reminders.delete(reminder) },
                    )
                }
            },
            listOfNotNull(
                notifications.missedCalls.takeIf { it.isNotEmpty() }?.let { calls ->
                    AtAGlanceItem(key = "missed-calls") { MissedCallsRow(calls, notifications.dismissMissedCalls) }
                },
            ),
            appNotifications?.let { glance ->
                glance.visible.map { notification ->
                    AtAGlanceItem(key = "app-notification-${notification.key}") {
                        AppNotificationRow(notification, glance.nowMillis)
                    }
                } +
                    listOfNotNull(
                        glance.takeIf { it.hiddenCount > 0 }?.let {
                            AtAGlanceItem(key = "app-notifications-more") { ShowMoreAppNotificationsRow(it) }
                        },
                    )
            }.orEmpty(),
            listOfNotNull(battery.lowBattery?.let { AtAGlanceItem(key = "low-battery") { LowBatteryRow(it) } }),
            listOfNotNull(wifiSignIn?.let { AtAGlanceItem(key = "wifi-sign-in") { WifiSignInRow(it) } }),
            notifications.progress.map { progress ->
                AtAGlanceItem(key = "progress-${progress.key}") { ProgressNotificationRow(progress) }
            } +
                notifications.finishedProgress.map { finished ->
                    AtAGlanceItem(key = "finished-progress-${finished.key}") {
                        FinishedProgressNotificationRow(finished) { notifications.dismissFinishedProgress(finished) }
                    }
                },
            notifications.workouts.map { workout ->
                AtAGlanceItem(key = "workout-${workout.key}") { WorkoutRow(workout) }
            },
            notifications.weather.map { weather ->
                AtAGlanceItem(key = "weather-${weather.key}") { WeatherRow(weather) { notifications.dismissWeather(weather) } }
            },
            birthdays.birthdays.map { birthday ->
                AtAGlanceItem(key = "birthday-${birthday.contactId}-${birthday.isAnniversary}") {
                    BirthdayRow(
                        birthday = birthday,
                        onClick = { birthdays.open(birthday) },
                        onDismiss = { birthdays.dismiss(birthday) },
                    )
                }
            },
            tomorrowEvents.events.map { event ->
                AtAGlanceItem(key = "tomorrow-event-${event.eventId}") {
                    TomorrowEventRow(
                        event = event,
                        onClick = { tomorrowEvents.open(event) },
                        onDismiss = { tomorrowEvents.dismiss(event) },
                    )
                }
            },
            customInfo.items.map { item ->
                AtAGlanceItem(key = "custom-info-${item.id}") {
                    CustomInfoRow(
                        item = item,
                        onDismiss = { customInfo.dismiss(item) },
                        onRetry = { customInfo.retry(item) },
                    )
                }
            },
            listOfNotNull(doNotDisturb?.let { AtAGlanceItem(key = "do-not-disturb") { DoNotDisturbRow(it) } }),
            listOfNotNull(airplaneMode?.let { AtAGlanceItem(key = "airplane-mode") { AirplaneModeRow(it) } }),
            listOfNotNull(hotspot?.let { AtAGlanceItem(key = "hotspot") { HotspotRow(it) } }),
            listOfNotNull(flashlight?.let { AtAGlanceItem(key = "flashlight") { FlashlightRow(it) } }),
            listOfNotNull(battery.charging?.let { AtAGlanceItem(key = "charging") { ChargingRow(it) } }),
            listOfNotNull(lowStorage?.let { AtAGlanceItem(key = "low-storage") { LowStorageRow(it) } }),
        )
    return rememberRetainedAtAGlanceItems(enabled, (if (reversed) groups.asReversed() else groups).flatten())
}

@Composable
internal fun atAGlanceDividerColor(showWallpaperBackground: Boolean) =
    LocalOverlayDividerColor.current
        ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant

/** Renders [items] separated by dividers, optionally with a divider before or after the group. */
@Composable
internal fun AtAGlanceRows(
    items: List<AtAGlanceItem>,
    showWallpaperBackground: Boolean,
    dividerBefore: Boolean = false,
    dividerAfter: Boolean = false,
) {
    val dividerColor = atAGlanceDividerColor(showWallpaperBackground)
    val rows = rememberAnimatedGlanceRows(items)
    if (rows.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        if (dividerBefore) HorizontalDivider(color = dividerColor)
        rows.forEachIndexed { index, row ->
            key(row.item.key) {
                AnimatedVisibility(
                    visibleState = row.visibility,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Column {
                        if (index > 0) HorizontalDivider(color = dividerColor)
                        row.item.content()
                    }
                }
            }
        }
        if (dividerAfter) HorizontalDivider(color = dividerColor)
    }
}

private class AnimatedGlanceRow(
    val item: AtAGlanceItem,
    val visibility: MutableTransitionState<Boolean>,
)

/** The rows last shown by [rememberAnimatedGlanceRows]; not snapshot state, only read while composing. */
private class AnimatedGlanceRows {
    var rows: List<AnimatedGlanceRow> = emptyList()
    var initialized = false
}

/**
 * [items] with the rows that just left kept in their old places until they finish collapsing, so
 * a dismissed row shrinks away and new rows (such as the ones "Show more" unfolds) grow in. Rows
 * present the first time the group is shown appear without animating.
 */
@Composable
private fun rememberAnimatedGlanceRows(items: List<AtAGlanceItem>): List<AnimatedGlanceRow> {
    val holder = remember { AnimatedGlanceRows() }
    val previous = holder.rows.associateBy { it.item.key }
    fun rowFor(item: AtAGlanceItem): AnimatedGlanceRow {
        val visibility =
            previous[item.key]?.visibility
                ?: MutableTransitionState(!holder.initialized).apply { targetState = true }
        return AnimatedGlanceRow(item, visibility)
    }
    val currentIndex = items.withIndex().associate { (index, item) -> item.key to index }
    val merged = mutableListOf<AnimatedGlanceRow>()
    val emitted = mutableSetOf<String>()
    var next = 0
    for (old in holder.rows) {
        val index = currentIndex[old.item.key]
        if (index != null) {
            while (next <= index) {
                val item = items[next++]
                if (emitted.add(item.key)) merged += rowFor(item)
            }
        } else {
            // Reading the transition state here recomposes once the row has finished leaving.
            val gone = !old.visibility.targetState && old.visibility.isIdle
            if (!gone && emitted.add(old.item.key)) merged += old
        }
    }
    while (next < items.size) {
        val item = items[next++]
        if (emitted.add(item.key)) merged += rowFor(item)
    }
    holder.rows = merged
    holder.initialized = true
    SideEffect {
        merged.forEach { row -> row.visibility.targetState = row.item.key in currentIndex }
    }
    return merged
}

/** Tapping the title opens the At a Glance settings. */
@Composable
internal fun AtAGlanceTitle() {
    val openAppSettingDestination = LocalOpenAppSettingDestination.current
    Text(
        text = stringResource(R.string.settings_at_a_glance_title),
        style = homeSectionTitleStyle(),
        color = homeTextColor(),
        modifier =
            Modifier
                .padding(horizontal = DesignTokens.SpacingLarge)
                .clickable(enabled = openAppSettingDestination != null, role = Role.Button) {
                    openAppSettingDestination?.invoke(AppSettingsDestination.AT_A_GLANCE)
                },
    )
}

/**
 * Card chrome for At a Glance rows outside the calendar card, with the same inset the calendar card
 * gives its event rows so the rows look the same wherever they are hosted.
 */
@Composable
internal fun AtAGlanceCardShell(
    showWallpaperBackground: Boolean,
    content: @Composable () -> Unit,
) {
    SearchResultCard(
        modifier = Modifier.fillMaxWidth(),
        showWallpaperBackground = showWallpaperBackground,
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = DesignTokens.SpacingMedium, vertical = 4.dp),
        ) {
            content()
        }
    }
}

/**
 * The At a Glance card when there are no today's events to host the rows, with its title. The card
 * grows in when rows appear and collapses away once the last one is dismissed.
 */
@Composable
internal fun AtAGlanceCard(
    items: AtAGlanceItems,
    showWallpaperBackground: Boolean,
    showTitle: Boolean,
) {
    if (items.isEmpty()) return
    AnimatedVisibility(
        visibleState = items.visibility,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        // Matches the spacing of the home column the title and card sit in.
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (showTitle) AtAGlanceTitle()
            AtAGlanceCardShell(showWallpaperBackground = showWallpaperBackground) {
                AtAGlanceRows(items = items, showWallpaperBackground = showWallpaperBackground)
            }
        }
    }
}

/**
 * Media controls sit in their own card under the At a Glance title, apart from the other rows.
 * Closing it collapses the card first and records the dismissal once that animation ends. When
 * media is the only At a Glance item, pass [showTitle] so the title collapses with the card and
 * the whole section leaves in one motion.
 */
@Composable
internal fun NowPlayingCard(
    glance: NowPlayingGlance,
    showWallpaperBackground: Boolean,
    showTitle: Boolean = false,
) {
    val visibility = remember { MutableTransitionState(true) }
    val currentDismiss by rememberUpdatedState(glance.dismiss)
    var dismissed by remember { mutableStateOf(false) }
    val finishDismiss = {
        if (!dismissed) {
            dismissed = true
            currentDismiss()
        }
    }
    if (!visibility.targetState && visibility.isIdle) SideEffect { finishDismiss() }
    // Leaving home mid-collapse still dismisses, so the card doesn't come back on return.
    DisposableEffect(Unit) {
        onDispose { if (!visibility.targetState) finishDismiss() }
    }
    AnimatedVisibility(
        visibleState = visibility,
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        // Matches the spacing of the home column the title and card sit in.
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (showTitle) AtAGlanceTitle()
            AtAGlanceCardShell(showWallpaperBackground = showWallpaperBackground) {
                NowPlayingRow(glance, onDismiss = { visibility.targetState = false })
            }
        }
    }
}
