package com.tk.quicksearch.search.reminders

import android.text.format.DateFormat
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.calendar.calendarRelativeDateLabel
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ROW_MIN_HEIGHT_DP = 52

private data class ReminderMenuItem(
    val textResId: Int,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
)

@Composable
fun RemindersResultsSection(
    reminders: List<ReminderInfo>,
    isExpanded: Boolean,
    pinnedReminderIds: Set<Long>,
    onReminderClick: (ReminderInfo) -> Unit,
    onTogglePin: (ReminderInfo) -> Unit,
    onMovePinned: (ReminderInfo, Boolean) -> Unit = { _, _ -> },
    onMarkDone: (ReminderInfo) -> Unit,
    onDelete: (ReminderInfo) -> Unit,
    showAllResults: Boolean,
    showExpandControls: Boolean,
    onExpandClick: () -> Unit,
    showWallpaperBackground: Boolean,
    expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    fillExpandedHeight: Boolean = false,
    showPinnedItemMenu: Boolean = false,
    allowInternalScroll: Boolean = true,
) {
    if (reminders.isEmpty()) return

    val collapsedReminders = reminders.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
    val overlayDividerColor = LocalOverlayDividerColor.current
    val overlayCardColor = LocalOverlayResultCardColor.current
    val scrollState = rememberScrollState()
    val shouldUseInternalScroll = isExpanded && allowInternalScroll

    ExpandableResultsCard(
        resultCount = reminders.size,
        hasAdditionalResults = reminders.size > collapsedReminders.size,
        isExpanded = isExpanded,
        showAllResults = showAllResults,
        isTopPredicted = false,
        showExpandControls = showExpandControls,
        expandedCardMaxHeight = expandedCardMaxHeight,
        constrainExpandedHeight = allowInternalScroll,
        hasScrollableContent = shouldUseInternalScroll && scrollState.maxValue > 0,
        fillExpandedHeight = fillExpandedHeight,
        showWallpaperBackground = showWallpaperBackground,
        overlayCardColor = overlayCardColor,
    ) { contentModifier, cardState ->
        val displayReminders = if (cardState.displayAsExpanded) reminders else collapsedReminders
        Column(
            modifier =
                contentModifier.then(
                    if (shouldUseInternalScroll) Modifier.verticalScroll(scrollState) else Modifier,
                ),
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = DesignTokens.SpacingMedium, vertical = 4.dp)
                        .padding(
                            bottom =
                                if (cardState.shouldFillExpandedHeight) DesignTokens.SpacingSmall else 0.dp,
                        ),
            ) {
                displayReminders.forEachIndexed { index, reminder ->
                    key(reminder.reminderId) {
                        ReminderRow(
                            reminder = reminder,
                            isPinned = pinnedReminderIds.contains(reminder.reminderId),
                            onClick = onReminderClick,
                            onTogglePin = onTogglePin,
                            onMovePinned = onMovePinned,
                            onMarkDone = onMarkDone,
                            onDelete = onDelete,
                            isPredicted = false,
                            showPinnedItemMenu = showPinnedItemMenu,
                        )
                        if (index < displayReminders.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color =
                                    overlayDividerColor
                                        ?: if (showWallpaperBackground) {
                                            AppColors.WallpaperDivider
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                            )
                        }
                    }
                }

                if (cardState.shouldShowExpandButton) {
                    ExpandButton(
                        onClick = onExpandClick,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        textResId = R.string.action_expand_more_reminders,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ReminderRow(
    reminder: ReminderInfo,
    isPinned: Boolean,
    onClick: (ReminderInfo) -> Unit,
    onTogglePin: (ReminderInfo) -> Unit,
    onMovePinned: (ReminderInfo, Boolean) -> Unit = { _, _ -> },
    onMarkDone: (ReminderInfo) -> Unit,
    onDelete: (ReminderInfo) -> Unit,
    isPredicted: Boolean,
    showPinnedItemMenu: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }
    val rowView = LocalView.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT_DP.dp)
                .topPredictedRowContainer(isTopPredicted = isPredicted)
                .combinedClickable(
                    onClick = {
                        hapticConfirm(rowView)()
                        onClick(reminder)
                    },
                    onLongClick = { showMenu = true },
                )
                .topPredictedRowContentPadding()
                .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_reminder),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 7.dp).size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = rememberQueryHighlightedText(reminder.title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = reminderScheduleLabel(reminder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ReminderRelativeDateText(reminder)
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false),
            containerColor = AppColors.DialogBackground,
        ) {
            val menuItems =
                buildList {
                    fun addItem(textResId: Int, icon: @Composable () -> Unit, action: () -> Unit) {
                        add(
                            ReminderMenuItem(textResId, icon) {
                                showMenu = false
                                action()
                            },
                        )
                    }
                    val pinIcon: @Composable () -> Unit = {
                        Icon(
                            painter = painterResource(if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin),
                            contentDescription = null,
                        )
                    }
                    if (showPinnedItemMenu && isPinned) {
                        addItem(R.string.action_unpin_app, pinIcon) { onTogglePin(reminder) }
                        addItem(
                            R.string.action_move_up,
                            { Icon(imageVector = Icons.Rounded.ArrowUpward, contentDescription = null) },
                        ) { onMovePinned(reminder, true) }
                        addItem(
                            R.string.action_move_down,
                            { Icon(imageVector = Icons.Rounded.ArrowDownward, contentDescription = null) },
                        ) { onMovePinned(reminder, false) }
                    } else {
                        addItem(
                            if (isPinned) R.string.action_unpin_app else R.string.action_pin_app,
                            pinIcon,
                        ) { onTogglePin(reminder) }
                    }
                    if (!reminder.isDone) {
                        addItem(
                            R.string.action_mark_as_done,
                            { Icon(imageVector = Icons.Rounded.Check, contentDescription = null) },
                        ) { onMarkDone(reminder) }
                    }
                    addItem(
                        R.string.dialog_delete,
                        { Icon(imageVector = Icons.Rounded.Delete, contentDescription = null) },
                    ) { onDelete(reminder) }
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

/**
 * The relative line under a reminder's schedule: a subtle red "Overdue" once it is past due, and a
 * leading tick once it is done.
 */
@Composable
fun ReminderRelativeDateText(reminder: ReminderInfo) {
    val isOverdue = reminder.isOverdue()
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (reminder.isDone) ReminderDoneIcon()
        Text(
            text =
                if (isOverdue) stringResource(R.string.reminder_status_overdue) else reminderRelativeDateLabel(reminder),
            style = MaterialTheme.typography.bodySmall,
            color = if (isOverdue) reminderOverdueColor() else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun reminderOverdueColor(): Color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)

/** Matches calendar events: "In 2 hours" for timed reminders, "Tomorrow" for time-less ones. */
@Composable
fun reminderRelativeDateLabel(reminder: ReminderInfo): String =
    if (reminder.hasTime) {
        calendarRelativeDateLabel(reminder.dueMillis, isAllDay = false)
    } else {
        calendarRelativeDateLabel(reminder.dayStartMillis)
    }

/** "Fri, Sep 18 • 3:00 PM", or just the date for reminders without a time. */
@Composable
fun reminderScheduleLabel(reminder: ReminderInfo): String {
    val context = LocalContext.current
    val dateText = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(reminder.dayStartMillis))
    return if (reminder.hasTime) {
        "$dateText • ${DateFormat.getTimeFormat(context).format(Date(reminder.dueMillis))}"
    } else {
        dateText
    }
}

/** Tick shown on done reminders, tinted like the row's secondary text. */
@Composable
fun ReminderDoneIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = stringResource(R.string.reminder_status_done),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(14.dp),
    )
}
