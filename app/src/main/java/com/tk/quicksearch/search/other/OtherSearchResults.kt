package com.tk.quicksearch.search.other

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.ScreenTimeAppUsage
import com.tk.quicksearch.search.core.ScreenTimeState
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.AppColors

@Composable
fun OtherSearchResults(
    query: String,
    pinnedItemOrder: List<String>,
    state: ScreenTimeState,
    showWallpaperBackground: Boolean,
    iconPackPackage: String?,
    onTogglePin: (OtherSearchItemId) -> Unit,
) {
    OtherSearchItemRegistry.definitions.forEach { definition ->
        when (definition.id) {
            OtherSearchItemId.SCREEN_TIME ->
                if (
                    OtherSearchItemRegistry.shouldRenderScreenTime(
                        query = query,
                        pinnedItemOrder = pinnedItemOrder,
                        state = state,
                    )
                ) {
                    ScreenTimeResultCard(
                        state = state,
                        isPinned = OtherSearchItemRegistry.isPinned(definition.id, pinnedItemOrder),
                        showWallpaperBackground = showWallpaperBackground,
                        iconPackPackage = iconPackPackage,
                        onTogglePin = { onTogglePin(definition.id) },
                    )
                }
        }
    }
}

@Composable
internal fun ScreenTimeResultCard(
    state: ScreenTimeState,
    isPinned: Boolean,
    showWallpaperBackground: Boolean,
    iconPackPackage: String?,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPinMenu by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showPinMenu = true },
                ),
    ) {
        SearchResultCard(
            modifier = Modifier.fillMaxWidth(),
            showWallpaperBackground = showWallpaperBackground,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = DesignTokens.SpacingLarge,
                            vertical = DesignTokens.SpacingMedium,
                        ),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                ) {
                    Text(
                        text =
                            rememberQueryHighlightedText(
                                stringResource(R.string.other_screen_time_today_title),
                            ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                    )
                    when (state) {
                        is ScreenTimeState.Available ->
                            Text(
                                text = formatScreenTime(state.durationMillis),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        ScreenTimeState.Hidden,
                        ScreenTimeState.Loading,
                        -> Unit
                    }
                }

                if (state is ScreenTimeState.Available && state.topApps.isNotEmpty()) {
                    VerticalDivider(
                        modifier = Modifier.height(100.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    )
                    Column(
                        modifier = Modifier.weight(1.15f),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    ) {
                        state.topApps.forEach { app ->
                            ScreenTimeAppUsageRow(
                                app = app,
                                iconPackPackage = iconPackPackage,
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showPinMenu,
            onDismissRequest = { showPinMenu = false },
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false),
            containerColor = AppColors.DialogBackground,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text =
                            stringResource(
                                if (isPinned) R.string.action_unpin_app else R.string.action_pin_app,
                            ),
                    )
                },
                leadingIcon = {
                    Icon(
                        painter =
                            painterResource(
                                if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin,
                            ),
                        contentDescription = null,
                    )
                },
                onClick = {
                    showPinMenu = false
                    onTogglePin()
                },
            )
        }
    }
}

@Composable
private fun ScreenTimeAppUsageRow(
    app: ScreenTimeAppUsage,
    iconPackPackage: String?,
) {
    val icon =
        rememberAppIcon(
            packageName = app.packageName,
            iconPackPackage = iconPackPackage,
        ).bitmap

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatScreenTime(app.durationMillis, minimumOneMinute = true),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun formatScreenTime(
    durationMillis: Long,
    minimumOneMinute: Boolean = false,
): String {
    val rawMinutes = (durationMillis / 60_000L).toInt().coerceAtLeast(0)
    val totalMinutes =
        if (minimumOneMinute && durationMillis > 0L) {
            rawMinutes.coerceAtLeast(1)
        } else {
            rawMinutes
        }
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 ->
            stringResource(R.string.other_screen_time_hours_minutes, hours, minutes)
        hours > 0 -> pluralStringResource(R.plurals.app_menu_usage_hours, hours, hours)
        else -> pluralStringResource(R.plurals.app_menu_usage_minutes, minutes, minutes)
    }
}
