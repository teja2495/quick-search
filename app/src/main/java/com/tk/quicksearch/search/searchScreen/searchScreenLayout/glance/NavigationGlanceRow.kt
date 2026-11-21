package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.glance.ProgressNotification
import com.tk.quicksearch.search.glance.asEtaText
import com.tk.quicksearch.search.glance.parseNavigationInstruction
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** The tonal tile holding the next turn's arrow, with the navigation app's icon as a corner badge. */
internal val NavigationManeuverTileSize = 48.dp
private val NavigationManeuverIconSize = 28.dp
private val NavigationAppBadgeSize = 18.dp

/**
 * Turn-by-turn navigation, maneuver first: the next turn's arrow in a tonal tile, the distance to it
 * as the headline, the instruction on one line, the arrival time, and the trip so far as a bar.
 * Without a parsed turn it falls back to the app icon, and without a distance to the instruction as
 * the title.
 */
@Composable
internal fun NavigationProgressRow(
    notification: ProgressNotification,
    appLabel: String,
) {
    val context = LocalContext.current
    val fraction =
        if (notification.progressMax > 0) notification.progress.toFloat() / notification.progressMax else 0f
    val instruction = remember(notification.title) { notification.title?.let(::parseNavigationInstruction) }
    val maneuver = instruction?.icon
    GlanceStatusRow(
        icon = {
            if (maneuver != null) {
                NavigationManeuverTile(maneuver, notification.packageName)
            } else {
                NotificationAppIcon(notification.packageName)
            }
        },
        iconSize = if (maneuver != null) NavigationManeuverTileSize else DesignTokens.IconSize,
        headline = notification.shortCriticalText,
        title = instruction?.title ?: appLabel,
        subtitle =
            (notification.subText ?: notification.text)?.asEtaText()
                ?: appLabel.takeIf { notification.title != null },
        onClick = {
            openNotificationTarget(context, notification.packageName, notification.contentIntent)
        },
        belowText = {
            if (fraction < MIN_VISIBLE_PROGRESS) return@GlanceStatusRow
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.SpacingXSmall),
                drawStopIndicator = {},
            )
        },
    )
}

@Composable
private fun NavigationManeuverTile(
    maneuver: ImageVector,
    packageName: String,
) {
    Box(modifier = Modifier.size(NavigationManeuverTileSize)) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(AppShapes.large)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = maneuver,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(NavigationManeuverIconSize),
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(NavigationAppBadgeSize),
        ) {
            NotificationAppIcon(packageName)
        }
    }
}
