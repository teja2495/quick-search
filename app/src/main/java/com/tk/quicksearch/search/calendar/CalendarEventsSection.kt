package com.tk.quicksearch.search.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ROW_MIN_HEIGHT_DP = 52

@Composable
fun CalendarEventsSection(
    events: List<CalendarEventInfo>,
    hasPermission: Boolean,
    isExpanded: Boolean,
    pinnedEventIds: Set<Long>,
    excludedEventIds: Set<Long>,
    onEventClick: (CalendarEventInfo) -> Unit,
    onRequestPermission: () -> Unit,
    onTogglePin: (CalendarEventInfo) -> Unit,
    onExclude: (CalendarEventInfo) -> Unit,
    onInclude: (CalendarEventInfo) -> Unit,
    onNicknameClick: (CalendarEventInfo) -> Unit,
    getEventNickname: (Long) -> String?,
    showAllResults: Boolean,
    showExpandControls: Boolean,
    onExpandClick: () -> Unit,
    expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    permissionDisabledCard:
        (
        @Composable (
            title: String,
            message: String,
            actionLabel: String,
            onActionClick: () -> Unit,
        ) -> Unit
        ),
    showWallpaperBackground: Boolean,
    predictedTarget: PredictedSubmitTarget? = null,
    fillExpandedHeight: Boolean = false,
) {
    if (!hasPermission) {
        permissionDisabledCard(
            stringResource(R.string.calendar_permission_title),
            stringResource(R.string.calendar_permission_subtitle),
            stringResource(R.string.permission_action_manage_android),
            onRequestPermission,
        )
        return
    }
    if (events.isEmpty()) return

    val predictedEventId = (predictedTarget as? PredictedSubmitTarget.Calendar)?.eventId
    val overlayDividerColor = LocalOverlayDividerColor.current
    val overlayCardColor = LocalOverlayResultCardColor.current
    val scrollState = rememberScrollState()

    ExpandableResultsCard(
        resultCount = events.size,
        isExpanded = isExpanded,
        showAllResults = showAllResults,
        showExpandControls = showExpandControls,
        expandedCardMaxHeight = expandedCardMaxHeight,
        hasScrollableContent = scrollState.maxValue > 0,
        fillExpandedHeight = fillExpandedHeight,
        showWallpaperBackground = showWallpaperBackground,
        overlayCardColor = overlayCardColor,
    ) { contentModifier, cardState ->
        val displayEvents =
            if (cardState.displayAsExpanded) {
                events
            } else {
                events.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
            }
        Column(
            modifier =
                contentModifier.then(
                    if (isExpanded) {
                        Modifier.verticalScroll(scrollState)
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(
                modifier =
                    Modifier.padding(horizontal = DesignTokens.SpacingMedium, vertical = 4.dp)
                        .padding(
                            bottom =
                                if (cardState.shouldFillExpandedHeight) {
                                    DesignTokens.SpacingSmall
                                } else {
                                    0.dp
                                },
                        ),
            ) {
                displayEvents.forEachIndexed { index, event ->
                    key(event.eventId) {
                        val isPredicted = predictedEventId != null && event.eventId == predictedEventId
                        CalendarEventRow(
                            event = event,
                            isPinned = pinnedEventIds.contains(event.eventId),
                            isExcluded = excludedEventIds.contains(event.eventId),
                            hasNickname = !getEventNickname(event.eventId).isNullOrBlank(),
                            onClick = onEventClick,
                            onTogglePin = onTogglePin,
                            onExclude = onExclude,
                            onInclude = onInclude,
                            onNicknameClick = onNicknameClick,
                            isPredicted = isPredicted,
                        )
                        if (index < displayEvents.lastIndex && !isPredicted) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = overlayDividerColor ?: MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }

                if (cardState.shouldShowExpandButton) {
                    ExpandButton(
                        onClick = onExpandClick,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarEventRow(
    event: CalendarEventInfo,
    isPinned: Boolean,
    isExcluded: Boolean,
    hasNickname: Boolean,
    onClick: (CalendarEventInfo) -> Unit,
    onTogglePin: (CalendarEventInfo) -> Unit,
    onExclude: (CalendarEventInfo) -> Unit,
    onInclude: (CalendarEventInfo) -> Unit,
    onNicknameClick: (CalendarEventInfo) -> Unit,
    isPredicted: Boolean,
) {
    var showMenu by remember { mutableStateOf(false) }
    val rowView = androidx.compose.ui.platform.LocalView.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT_DP.dp)
                .topPredictedRowContainer(isTopPredicted = isPredicted)
                .combinedClickable(
                    onClick = {
                        hapticConfirm(rowView)()
                        onClick(event)
                    },
                    onLongClick = { showMenu = true },
                )
                .topPredictedRowContentPadding(isTopPredicted = isPredicted)
                .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatCalendarEventDate(event),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = AppColors.DialogBackground,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isPinned) {
                                R.string.action_unpin_generic
                            } else {
                                R.string.action_pin_generic
                            },
                        ),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPinned) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                    )
                },
                onClick = {
                    showMenu = false
                    onTogglePin(event)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isExcluded) {
                                R.string.action_include_generic
                            } else {
                                R.string.action_exclude_generic
                            },
                        ),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isExcluded) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                    )
                },
                onClick = {
                    showMenu = false
                    if (isExcluded) onInclude(event) else onExclude(event)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (hasNickname) {
                                R.string.action_edit_nickname
                            } else {
                                R.string.action_add_nickname
                            },
                        ),
                    )
                },
                leadingIcon = { Icon(imageVector = Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onNicknameClick(event)
                },
            )
        }
    }
}

private fun formatCalendarEventDate(event: CalendarEventInfo): String {
    val locale = Locale.getDefault()
    val datePattern = "EEE, MMM d"
    val dateTimePattern = "EEE, MMM d • h:mm a"
    val allDayFormat = SimpleDateFormat(datePattern, locale)
    val dateTimeFormat = SimpleDateFormat(dateTimePattern, locale)
    val date = Date(event.startMillis)
    return if (event.allDay) {
        allDayFormat.format(date)
    } else {
        dateTimeFormat.format(date)
    }
}
