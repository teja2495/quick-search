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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Repeat
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
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

private const val ROW_MIN_HEIGHT_DP = 52

private data class CalendarMenuItem(
    val textResId: Int,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
)

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
    val hasPredictedEvent = predictedEventId != null && events.any { it.eventId == predictedEventId }
    val displayAsExpanded = isExpanded || showAllResults
    val useCardLevelPrediction = hasPredictedEvent && (!displayAsExpanded || events.size == 1)
    val overlayDividerColor = LocalOverlayDividerColor.current
    val overlayCardColor = LocalOverlayResultCardColor.current
    val scrollState = rememberScrollState()

    ExpandableResultsCard(
        resultCount = events.size,
        isExpanded = isExpanded,
        showAllResults = showAllResults,
        isTopPredicted = useCardLevelPrediction,
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
                        val showPredictedOnRow = isPredicted && !useCardLevelPrediction
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
                            isPredicted = showPredictedOnRow,
                        )
                        if (index < displayEvents.lastIndex && !showPredictedOnRow) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = overlayDividerColor ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }

                if (cardState.shouldShowExpandButton) {
                    ExpandButton(
                        onClick = onExpandClick,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        textResId = R.string.action_expand_more_events,
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 7.dp).size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val recurrenceLabel = calendarRecurrenceLabel(recurrenceRule = event.recurrenceRule)
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
            recurrenceLabel?.let { recurrence ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = recurrence,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = calendarRelativeDateLabel(event.startMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false),
            containerColor = AppColors.DialogBackground,
        ) {
            val menuItems = buildList {
                add(
                    CalendarMenuItem(
                        textResId =
                            if (isPinned) {
                                R.string.action_unpin_generic
                            } else {
                                R.string.action_pin_generic
                            },
                        icon = {
                            Icon(
                                painter =
                                    painterResource(
                                        if (isPinned) {
                                            R.drawable.ic_unpin
                                        } else {
                                            R.drawable.ic_pin
                                        },
                                    ),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showMenu = false
                            onTogglePin(event)
                        },
                    ),
                )
                add(
                    CalendarMenuItem(
                        textResId =
                            if (isExcluded) {
                                R.string.action_include_generic
                            } else {
                                R.string.action_exclude_generic
                            },
                        icon = {
                            Icon(
                                imageVector =
                                    if (isExcluded) {
                                        Icons.Rounded.Visibility
                                    } else {
                                        Icons.Rounded.VisibilityOff
                                    },
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showMenu = false
                            if (isExcluded) onInclude(event) else onExclude(event)
                        },
                    ),
                )
                add(
                    CalendarMenuItem(
                        textResId =
                            if (hasNickname) {
                                R.string.action_edit_nickname
                            } else {
                                R.string.action_add_nickname
                            },
                        icon = { Icon(imageVector = Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onNicknameClick(event)
                        },
                    ),
                )
            }

            menuItems.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider()
                }
                DropdownMenuItem(
                    text = { Text(text = stringResource(item.textResId)) },
                    leadingIcon = { item.icon() },
                    onClick = item.onClick,
                )
            }
        }
    }
}
