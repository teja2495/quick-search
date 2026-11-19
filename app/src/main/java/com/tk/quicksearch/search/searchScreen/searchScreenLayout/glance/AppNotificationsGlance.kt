package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.appNotifications.AppNotification
import com.tk.quicksearch.appNotifications.AppNotificationsConfig
import com.tk.quicksearch.appNotifications.AppNotificationsSettings
import com.tk.quicksearch.search.glance.GlanceNotificationsStore
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Matched notifications shown on home before the rest fold behind "Show N more". */
private const val COLLAPSED_APP_NOTIFICATION_ROWS = 5

private const val JUST_NOW_MILLIS = 60_000L

/**
 * The matched notifications home shows, newest first: all of them once [expand] is called,
 * otherwise the first few, with [hiddenCount] more folded away.
 */
internal class AppNotificationsGlance(
    val visible: List<AppNotification>,
    val hiddenCount: Int,
    val nowMillis: Long,
    val expand: () -> Unit,
)

/**
 * App notifications' toggle, apps and keywords, kept current as they change. Null until they have
 * been read, which happens off the main thread the first time.
 */
@Composable
internal fun rememberAppNotificationsConfig(): AppNotificationsConfig? {
    val context = LocalContext.current
    val config by AppNotificationsSettings.config.collectAsState()
    LaunchedEffect(Unit) {
        if (AppNotificationsSettings.config.value == null) {
            withContext(Dispatchers.IO) { AppNotificationsSettings.load(context) }
        }
    }
    return config
}

/**
 * Every matching notification, one row each, newest first. The store has already matched them
 * against the apps and keywords; [excludedKeys] are notifications another row already shows, such
 * as the one-time code. Only the first few show until expanded, which lasts until the screen next
 * resumes.
 */
@Composable
internal fun rememberAppNotificationsGlance(
    enabled: Boolean,
    /** Notification listener access, read once per resume for every notification glance. */
    hasAccess: Boolean,
    excludedKeys: Set<String>,
): AppNotificationsGlance? {
    val context = LocalContext.current
    val refreshKey = rememberResumeRefreshKey()
    val config = rememberAppNotificationsConfig()
    val notifications by GlanceNotificationsStore.appNotifications.collectAsState()
    var expanded by remember(refreshKey) { mutableStateOf(false) }
    if (!enabled || !hasAccess || config?.isActive != true) return null
    val matches =
        remember(notifications, excludedKeys) {
            notifications.filter { it.key !in excludedKeys }.sortedByDescending { it.postTime }
        }
    if (matches.isEmpty()) return null
    // One more row would take the same space as "Show 1 more", so show it instead.
    val collapsed = !expanded && matches.size > COLLAPSED_APP_NOTIFICATION_ROWS + 1
    val visible = if (collapsed) matches.take(COLLAPSED_APP_NOTIFICATION_ROWS) else matches
    return AppNotificationsGlance(
        visible = visible,
        hiddenCount = matches.size - visible.size,
        nowMillis = remember(notifications, refreshKey) { System.currentTimeMillis() },
        expand = { expanded = true },
    )
}

/**
 * A matched notification's title and text with the posting app's icon, the app and how long ago
 * it was posted below, and a dismiss button that hides just this notification.
 */
@Composable
internal fun AppNotificationRow(
    notification: AppNotification,
    nowMillis: Long,
) {
    val context = LocalContext.current
    val appLabel = rememberAppLabel(notification.packageName)
    val age =
        if (nowMillis - notification.postTime < JUST_NOW_MILLIS) {
            stringResource(R.string.calendar_relative_now)
        } else {
            DateUtils.getRelativeTimeSpanString(
                notification.postTime,
                nowMillis,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString().replace(".", "")
        }
    GlanceStatusRow(
        icon = { NotificationAppIcon(notification.packageName) },
        title = notification.title ?: notification.text ?: appLabel,
        subtitle = notification.text.takeIf { notification.title != null },
        titleMaxLines = GlanceTextMaxLines,
        onClick = { openNotificationTarget(context, notification.packageName, notification.contentIntent) },
        onDismiss = { GlanceNotificationsStore.dismissAppNotification(notification) },
        dismissAtTop = true,
        belowText = {
            Text(
                text = "$appLabel • $age",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

/** Unfolds the matched notifications [AppNotificationsGlance] is holding back. */
@Composable
internal fun ShowMoreAppNotificationsRow(glance: AppNotificationsGlance) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = DesignTokens.MinTouchTarget)
                .clickable(role = Role.Button, onClick = glance.expand)
                .padding(horizontal = DesignTokens.GlanceRowHorizontalPadding, vertical = DesignTokens.SpacingSmall),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = MaterialSymbols.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.home_app_notifications_more, glance.hiddenCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
