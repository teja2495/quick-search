package com.tk.quicksearch.appNotifications

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import com.tk.quicksearch.R
import com.tk.quicksearch.appNotifications.AppNotificationFilters
import com.tk.quicksearch.search.glance.GlanceNotificationsStore
import com.tk.quicksearch.search.notificationHistory.NotificationHistoryStore
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.rememberAppLabel
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerAppIcon
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerMessage
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerSearch
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** How far back Recent matches looks. */
private const val RECENT_MATCH_WINDOW_MS = 7 * DateUtils.DAY_IN_MILLIS
private const val MAX_RECENT_MATCHES = 5

/** Keeps the card from shrinking to a sliver when nothing matched. */

/** A notification seen lately, from the shade or from notification history. */
private class RecentNotification(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postTime: Long,
) {
    val searchText: String get() = listOfNotNull(title, text).joinToString("\n")
}

/**
 * Notifications App notifications could have shown, newest first: those posted now plus
 * notification history, leaving out the kinds it never shows (ongoing, media, calls, progress and
 * other rows' notifications) and this app's own. A posting in both counts once, as on Home.
 */
@Composable
private fun rememberRecentNotifications(): List<RecentNotification> {
    val context = LocalContext.current
    val history by remember(context) { NotificationHistoryStore.entries(context) }.collectAsState(initial = emptyList())
    val live by GlanceNotificationsStore.appNotificationCandidates.collectAsState()
    return remember(history, live) {
        val fromShade = live.map { RecentNotification(it.key, it.packageName, it.title, it.text, it.postTime) }
        val fromHistory = history
            .filter { it.appNotificationCandidate }
            .map { entry ->
                RecentNotification(
                    entry.key,
                    entry.packageName,
                    entry.title.ifBlank { null },
                    entry.text.ifBlank { null },
                    entry.postTime,
                )
            }
        (fromShade + fromHistory)
            .filter { it.packageName != context.packageName }
            .distinctBy { it.key to it.postTime }
            .sortedByDescending { it.postTime }
    }
}

/** What [filters] would have shown lately, so the user can tell whether the keywords catch the right things. */
@Composable
internal fun AppNotificationRecentMatches(filters: AppNotificationFilters) {
    val recent = rememberRecentNotifications()
    val matches = remember(recent, filters) {
        val since = System.currentTimeMillis() - RECENT_MATCH_WINDOW_MS
        recent
            .filter { it.postTime >= since && filters.matches(it.packageName, it.searchText) }
            .take(MAX_RECENT_MATCHES)
    }
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
        Text(
            text = stringResource(R.string.app_notifications_recent_matches),
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.Accent,
            modifier = Modifier.padding(horizontal = DesignTokens.SpacingLarge, vertical = DesignTokens.SpacingXSmall),
        )
        SegmentedGroup {
            if (matches.isEmpty()) {
                SegmentedTile(index = 0, count = 1) {
                    Text(
                        text = stringResource(R.string.app_notifications_no_matches),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(DesignTokens.SpacingLarge),
                    )
                }
            }
            matches.forEachIndexed { index, notification ->
                SegmentedTile(index = index, count = matches.size) {
                    RecentMatchRow(notification)
                }
            }
        }
    }
}

/** One matched notification: app icon, title with its age, and the first line of its text. */
@Composable
private fun RecentMatchRow(notification: RecentNotification) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppPickerDrawerAppIcon(notification.packageName)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall / 2),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = notification.title ?: rememberAppLabel(notification.packageName),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = notificationAge(notification.postTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            notification.text?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Picks one app to add from [allApps] (null while they load), leaving out [added]. Tapping an app
 * passes it to [onPick] and closes the drawer.
 */
@Composable
internal fun AppNotificationAppPicker(
    allApps: List<AppNotificationPickerApp>?,
    added: Set<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val search = query.text.trim()
    val shown = allApps.orEmpty()
        .filter { it.packageName !in added && (search.isEmpty() || it.label.contains(search, ignoreCase = true)) }

    AppPickerDrawer(
        title = stringResource(R.string.app_notifications_choose_apps),
        onDismiss = onDismiss,
        search = AppPickerDrawerSearch(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.app_notifications_search_apps),
        ),
    ) { dismiss ->
        val pick: (String) -> Unit = { packageName ->
            onPick(packageName)
            dismiss()
        }
        if (allApps != null && shown.isEmpty()) {
            AppPickerDrawerMessage(stringResource(R.string.widget_custom_buttons_no_results), centered = true)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing)) {
            items(shown, key = { it.packageName }) { app ->
                PickerAppRow(app) { pick(app.packageName) }
            }
        }
    }
}

@Composable
private fun PickerAppRow(
    app: AppNotificationPickerApp,
    onClick: () -> Unit,
) {
    AppPickerDrawerRow(
        title = app.label,
        onClick = onClick,
        leading = { AppPickerDrawerAppIcon(app.packageName) },
        titleMaxLines = 1,
    )
}

/** How long ago [postTime] was, short and without periods: "Now", "5 min ago", "2 hrs ago", "3 days ago". */
@Composable
private fun notificationAge(postTime: Long): String {
    val elapsed = (System.currentTimeMillis() - postTime).coerceAtLeast(0L)
    val minutes = (elapsed / DateUtils.MINUTE_IN_MILLIS).toInt()
    val hours = (elapsed / DateUtils.HOUR_IN_MILLIS).toInt()
    val days = (elapsed / DateUtils.DAY_IN_MILLIS).toInt()
    val amount = when {
        minutes < 1 -> return stringResource(R.string.calendar_relative_now)
        hours < 1 -> pluralStringResource(R.plurals.app_notifications_age_minutes, minutes, minutes)
        days < 1 -> pluralStringResource(R.plurals.app_notifications_age_hours, hours, hours)
        else -> pluralStringResource(R.plurals.calendar_relative_days, days, days)
    }
    return stringResource(R.string.calendar_relative_ago_format, amount)
}
