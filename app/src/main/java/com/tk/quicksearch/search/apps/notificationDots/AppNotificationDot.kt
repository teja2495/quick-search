package com.tk.quicksearch.search.apps.notificationDots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes

/** Active notification counts per app key, and whether badges show the count instead of a dot. */
@Immutable
data class NotificationBadges(
    val counts: Map<String, Int> = emptyMap(),
    val showCount: Boolean = false,
) {
    fun countFor(app: AppInfo): Int = counts[app.launchCountKey()] ?: 0

    companion object {
        val None = NotificationBadges()
    }
}

@Composable
fun rememberNotificationBadges(
    enabled: Boolean,
    showCount: Boolean,
): NotificationBadges {
    val counts by NotificationDotsStore.counts.collectAsState()
    return remember(enabled, showCount, counts) {
        if (enabled) NotificationBadges(counts, showCount) else NotificationBadges.None
    }
}

/**
 * Notification badge on an app icon: a small accent dot, or with [showCount] a Material 3 large
 * badge holding the count (capped at 99+). Nothing when [count] is 0.
 */
@Composable
fun BoxScope.AppNotificationBadge(
    count: Int,
    showCount: Boolean,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    if (!showCount) {
        val description = stringResource(R.string.desc_notification_dot)
        Box(
            modifier =
                modifier
                    .align(Alignment.TopEnd)
                    .size(NotificationDotSize)
                    .clip(AppShapes.full)
                    .background(AppColors.Accent)
                    .semantics { contentDescription = description },
        )
        return
    }
    val description = pluralStringResource(R.plurals.desc_notification_count, count, count)
    Box(
        modifier =
            modifier
                .align(Alignment.TopEnd)
                .defaultMinSize(minWidth = NotificationCountSize, minHeight = NotificationCountSize)
                .clip(AppShapes.full)
                .background(AppColors.Accent)
                .padding(horizontal = 4.dp)
                .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > MaxShownCount) "$MaxShownCount+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.OnAccent,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private val NotificationDotSize = 12.dp
private val NotificationCountSize = 16.dp
private const val MaxShownCount = 99
