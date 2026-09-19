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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.runtime.produceState
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
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.delay
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
    onMovePinned: (CalendarEventInfo, Boolean) -> Unit = { _, _ -> },
    onExclude: (CalendarEventInfo) -> Unit,
    onInclude: (CalendarEventInfo) -> Unit,
    onNicknameClick: (CalendarEventInfo) -> Unit,
    onArchiveTodayEvent: (CalendarEventInfo) -> Unit,
    getEventNickname: (Long) -> String?,
    showAllResults: Boolean,
    showExpandControls: Boolean,
    onExpandClick: () -> Unit,
    isHomeScreenMode: Boolean = false,
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
    showPinnedItemMenu: Boolean = false,
    collapsedEvents: List<CalendarEventInfo>? = null,
    allowInternalScroll: Boolean = true,
) {
    if (!hasPermission) {
        permissionDisabledCard(
            stringResource(R.string.permission_required_title),
            stringResource(R.string.calendar_section_permission_subtitle),
            stringResource(R.string.permission_action_manage_android),
            onRequestPermission,
        )
        return
    }
    if (events.isEmpty()) return

    val effectiveCollapsedEvents = remember(collapsedEvents, events) {
        collapsedEvents
            ?.mapNotNull { collapsedEvent ->
                events.firstOrNull { it.eventId == collapsedEvent.eventId }
            }
            ?.sortedBy { it.startMillis }
            ?: events
    }
    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    val collapsedDisplayEvents =
        if (isHomeScreenMode) {
            if (events.size == 1) {
                events
            } else {
                events.filter { event ->
                    event.allDay || isCalendarEventCurrentlyRelevant(event, nowMillis)
                }
            }
        } else {
            effectiveCollapsedEvents.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
        }

    val predictedEventId = (predictedTarget as? PredictedSubmitTarget.Calendar)?.eventId
    val hasPredictedEvent = predictedEventId != null && events.any { it.eventId == predictedEventId }
    val displayAsExpanded = isExpanded || showAllResults
    val useCardLevelPrediction = hasPredictedEvent && (!displayAsExpanded || events.size == 1)
    val overlayDividerColor = LocalOverlayDividerColor.current
    val overlayCardColor = LocalOverlayResultCardColor.current
    val scrollState = rememberScrollState()
    val shouldUseInternalScroll = isExpanded && allowInternalScroll

    ExpandableResultsCard(
        resultCount = events.size,
        hasAdditionalResults = events.size > collapsedDisplayEvents.size,
        isExpanded = isExpanded,
        showAllResults = showAllResults,
        isTopPredicted = useCardLevelPrediction,
        showExpandControls = showExpandControls,
        expandedCardMaxHeight = expandedCardMaxHeight,
        constrainExpandedHeight = allowInternalScroll,
        hasScrollableContent = shouldUseInternalScroll && scrollState.maxValue > 0,
        fillExpandedHeight = fillExpandedHeight,
        showWallpaperBackground = showWallpaperBackground,
        overlayCardColor = overlayCardColor,
    ) { contentModifier, cardState ->
        val displayEvents =
            if (cardState.displayAsExpanded) {
                events
            } else {
                collapsedDisplayEvents
            }
        Column(
            modifier =
                contentModifier.then(
                    if (shouldUseInternalScroll) {
                        Modifier.verticalScroll(scrollState)
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = DesignTokens.SpacingMedium, vertical = 4.dp)
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
                        val isPinned = pinnedEventIds.contains(event.eventId)
                        CalendarEventRow(
                            event = event,
                            isPinned = isPinned,
                            isExcluded = excludedEventIds.contains(event.eventId),
                            hasNickname = !getEventNickname(event.eventId).isNullOrBlank(),
                            onClick = onEventClick,
                            onTogglePin = onTogglePin,
                            onMovePinned = onMovePinned,
                            onExclude = onExclude,
                            onInclude = onInclude,
                            onNicknameClick = onNicknameClick,
                            isPredicted = showPredictedOnRow,
                            isHomescreenTodayEvent =
                                isHomeScreenMode && !isPinned && !cardState.displayAsExpanded,
                            onArchive = onArchiveTodayEvent,
                            showPinnedItemMenu = showPinnedItemMenu,
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
internal fun CalendarEventRow(
    event: CalendarEventInfo,
    isPinned: Boolean,
    isExcluded: Boolean,
    hasNickname: Boolean,
    onClick: (CalendarEventInfo) -> Unit,
    onTogglePin: (CalendarEventInfo) -> Unit,
    onMovePinned: (CalendarEventInfo, Boolean) -> Unit = { _, _ -> },
    onExclude: (CalendarEventInfo) -> Unit,
    onInclude: (CalendarEventInfo) -> Unit,
    onNicknameClick: (CalendarEventInfo) -> Unit,
    isPredicted: Boolean,
    isHomescreenTodayEvent: Boolean = false,
    onArchive: (CalendarEventInfo) -> Unit = {},
    showPinnedItemMenu: Boolean = false,
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
                .topPredictedRowContentPadding()
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
                text = rememberQueryHighlightedText(event.title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isHomescreenTodayEvent) {
                    calendarHomeScheduleLabel(event)
                } else {
                    listOfNotNull(
                        formatCalendarEventDate(event),
                        recurrenceLabel,
                    ).joinToString(" \u00b7 ")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!isHomescreenTodayEvent) {
                Text(
                    text = calendarRelativeDateLabel(event),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false),
            containerColor = AppColors.DialogBackground,
        ) {
            val menuItems = if (isHomescreenTodayEvent) {
                listOf(
                    CalendarMenuItem(
                        textResId = R.string.dialog_done,
                        icon = { Icon(imageVector = Icons.Rounded.Check, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onArchive(event)
                        },
                    ),
                )
            } else {
                buildList {
                    if (showPinnedItemMenu && isPinned) {
                        add(
                            CalendarMenuItem(
                                textResId = R.string.action_unpin_app,
                                icon = { Icon(painter = painterResource(R.drawable.ic_unpin), contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onTogglePin(event)
                                },
                            ),
                        )
                        add(
                            CalendarMenuItem(
                                textResId = R.string.action_move_up,
                                icon = { Icon(imageVector = Icons.Rounded.ArrowUpward, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onMovePinned(event, true)
                                },
                            ),
                        )
                        add(
                            CalendarMenuItem(
                                textResId = R.string.action_move_down,
                                icon = { Icon(imageVector = Icons.Rounded.ArrowDownward, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onMovePinned(event, false)
                                },
                            ),
                        )
                        return@buildList
                    }
                    add(
                        CalendarMenuItem(
                            textResId =
                                if (isPinned) {
                                    R.string.action_unpin_app
                                } else {
                                    R.string.action_pin_app
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
                                    R.string.common_nickname
                                },
                            icon = { Icon(imageVector = Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNicknameClick(event)
                            },
                        ),
                    )
                }
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
