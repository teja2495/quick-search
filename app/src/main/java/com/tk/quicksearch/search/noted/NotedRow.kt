package com.tk.quicksearch.search.noted

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.calendar.calendarRelativeDateLabel
import com.tk.quicksearch.search.reminders.reminderOverdueColor
import com.tk.quicksearch.search.models.NotedItem
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.search.searchScreen.components.resultRowVerticalPadding
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.util.hapticConfirm
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private const val ROW_MIN_HEIGHT_DP = 52
private const val ICON_SIZE_DP = 32

/** Callbacks for Noted rows in Top matches, provided once by the search screen. */
data class NotedSectionParams(
    val onItemClick: (NotedItem) -> Unit,
    val iconPackPackage: String? = null,
)

/** Travels by composition local like the Bookmarks params; null outside the search screen. */
val LocalNotedSectionParams = compositionLocalOf<NotedSectionParams?> { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NotedRow(
    item: NotedItem,
    onClick: (NotedItem) -> Unit,
    iconPackPackage: String?,
    isPredicted: Boolean = false,
    isPinned: Boolean = false,
    /** Null for items that can't be pinned: only dated tasks pin, as reminders. */
    onTogglePin: (() -> Unit)? = null,
) {
    val rowView = LocalView.current
    val context = LocalContext.current
    val subtitle = notedSubtitle(item)
    val isTask = item.kind == NotedItem.Kind.TASK
    val hasMenu = onTogglePin != null || isTask
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT_DP.dp)
                .topPredictedRowContainer(isTopPredicted = isPredicted)
                .combinedClickable(
                    onClick = {
                        hapticConfirm(rowView)()
                        onClick(item)
                    },
                    onLongClick = if (hasMenu) ({ showMenu = true }) else null,
                )
                .topPredictedRowContentPadding()
                .padding(vertical = resultRowVerticalPadding(12.dp)),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NotedAppIcon(iconPackPackage = iconPackPackage)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = rememberQueryHighlightedText(item.title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isTask) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isTask) NotedTaskRelativeDateText(item)
        }
        if (hasMenu) {
            AppDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                val actions =
                    buildList<Triple<Int, @Composable () -> Unit, () -> Unit>> {
                        val pinIcon: @Composable () -> Unit = {
                            Icon(
                                painter = painterResource(if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin),
                                contentDescription = null,
                            )
                        }
                        if (onTogglePin != null) {
                            add(Triple(if (isPinned) R.string.action_unpin_app else R.string.action_pin_app, pinIcon, onTogglePin))
                        }
                        if (isTask) {
                            add(
                                Triple(
                                    R.string.action_mark_as_done,
                                    { Icon(imageVector = MaterialSymbols.Check, contentDescription = null) },
                                    { NotedGlanceTasks.completeInBackground(context, item.id) },
                                ),
                            )
                        }
                    }
                actions.forEachIndexed { index, (textResId, icon, action) ->
                    if (index > 0) HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(textResId)) },
                        leadingIcon = icon,
                        onClick = {
                            showMenu = false
                            action()
                        },
                    )
                }
            }
        }
    }
}

/** Like reminders: a subtle red "Overdue" once past due, otherwise "In 2 hours" or "Tomorrow". */
@Composable
internal fun NotedTaskRelativeDateText(item: NotedItem) {
    val dueAt = item.dueAtMillis
    val dayStart =
        item.dueDate()?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: return
    val isOverdue = item.isOverdue()
    Text(
        text =
            when {
                isOverdue -> stringResource(R.string.reminder_status_overdue)
                item.isDueTimeSet && dueAt != null -> calendarRelativeDateLabel(dueAt, isAllDay = false)
                else -> calendarRelativeDateLabel(dayStart)
            },
        style = MaterialTheme.typography.bodySmall,
        color = if (isOverdue) reminderOverdueColor() else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The Noted app's launcher icon, with a generic notes icon until it loads. */
@Composable
internal fun NotedAppIcon(
    iconPackPackage: String?,
    modifier: Modifier = Modifier.padding(start = 3.dp).size(ICON_SIZE_DP.dp),
) {
    val bitmap =
        NotedIntegration.packageName?.let { rememberAppIcon(packageName = it, iconPackPackage = iconPackPackage).bitmap }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
    } else {
        Icon(
            imageVector = MaterialSymbols.EditNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

/** Note: "Note". List: "List". Task: reminder-style schedule and list name, e.g. "Wed, Nov 11 • 4:30 PM · To Do". */
@Composable
internal fun notedSubtitle(item: NotedItem): String? {
    val context = LocalContext.current
    val noteLabel = stringResource(R.string.notification_pinned_item_type_note)
    val listLabel = stringResource(R.string.noted_item_list)
    return remember(item, noteLabel, listLabel) {
        when (item.kind) {
            NotedItem.Kind.NOTE -> noteLabel
            NotedItem.Kind.LIST -> listLabel
            NotedItem.Kind.TASK -> {
                val dueAt = item.dueAtMillis
                val due =
                    item.dueDate()?.let { day ->
                        val dayStart = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val dateText = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(dayStart))
                        if (item.isDueTimeSet && dueAt != null) {
                            "$dateText • ${DateFormat.getTimeFormat(context).format(Date(dueAt))}"
                        } else {
                            dateText
                        }
                    }
                listOfNotNull(due, item.listTitle).joinToString(" · ").ifBlank { null }
            }
        }
    }
}
