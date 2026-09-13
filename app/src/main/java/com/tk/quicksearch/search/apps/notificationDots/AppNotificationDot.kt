package com.tk.quicksearch.search.apps.notificationDots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.theme.AppColors

@Composable
fun rememberNotificationDotKeys(enabled: Boolean): Set<String> {
    val keys by NotificationDotsStore.keys.collectAsState()
    return if (enabled) keys else emptySet()
}

fun AppInfo.hasNotificationDot(keys: Set<String>): Boolean = launchCountKey() in keys

@Composable
fun BoxScope.AppNotificationDot(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val description = stringResource(R.string.desc_notification_dot)
    Box(
        modifier =
            modifier
                .align(Alignment.TopEnd)
                .size(12.dp)
                .clip(CircleShape)
                .background(AppColors.Accent)
                .semantics { contentDescription = description },
    )
}
