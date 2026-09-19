package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.reminders.ReminderEditorRequests
import com.tk.quicksearch.search.calendar.calendarRelativeTimeLabel
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.reminders.reminderOverdueColor
import com.tk.quicksearch.search.reminders.reminderScheduleLabel
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val UpcomingReminderDismissSize = 28.dp
private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

/**
 * Home card for reminders due within 30 minutes or already overdue. It stays until the reminder is
 * marked done or removed from Home; removing it does not cancel the notification.
 */
@Composable
internal fun UpcomingReminderSection(showWallpaperBackground: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { ReminderRepository(context) }
    val changeCount by ReminderRepository.changes.collectAsState()
    var refreshKey by remember { mutableIntStateOf(0) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var reminders by remember { mutableStateOf(emptyList<ReminderInfo>()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(repository, refreshKey, changeCount) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            reminders = withContext(Dispatchers.IO) { repository.getHomeCardReminders(nowMillis) }
            delay(10_000)
        }
    }

    if (reminders.isEmpty()) return

    SearchResultCard(
        modifier = Modifier.fillMaxWidth(),
        showWallpaperBackground = showWallpaperBackground,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            reminders.forEachIndexed { index, reminder ->
                UpcomingReminderRow(
                    reminder = reminder,
                    nowMillis = nowMillis,
                    onClick = { ReminderEditorRequests.openEdit(reminder) },
                    onDone = {
                        reminders = reminders.filterNot { it.reminderId == reminder.reminderId }
                        scope.launch(Dispatchers.IO) { repository.setDone(reminder.reminderId, true) }
                    },
                    onDismiss = {
                        reminders = reminders.filterNot { it.reminderId == reminder.reminderId }
                        scope.launch(Dispatchers.IO) { repository.dismissFromHome(reminder.reminderId) }
                    },
                )
                if (index < reminders.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UpcomingReminderRow(
    reminder: ReminderInfo,
    nowMillis: Long,
    onClick: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    // Overdue reminders show their time (or date once a day late) with a red "Overdue" in place of
    // the relative label; upcoming ones read best relative to now.
    val isOverdue = reminder.isOverdue(nowMillis)
    val time = remember(reminder.dueMillis, context) {
        DateFormat.getTimeFormat(context).format(Date(reminder.dueMillis))
    }
    val scheduleText =
        when {
            !isOverdue -> "$time • ${calendarRelativeTimeLabel(reminder.dueMillis, nowMillis)}"
            nowMillis - reminder.dueMillis < DAY_MILLIS -> time
            else -> reminderScheduleLabel(reminder)
        }
    val overdueText = stringResource(R.string.reminder_status_overdue)
    val overdueColor = reminderOverdueColor()
    val scheduleLabel =
        buildAnnotatedString {
            append(scheduleText)
            if (isOverdue) {
                append(" • ")
                withStyle(SpanStyle(color = overdueColor)) { append(overdueText) }
            }
        }

    Row(
        modifier = Modifier.fillMaxWidth().padding(
            start = DesignTokens.SpacingLarge,
            end = DesignTokens.SpacingMedium,
            top = DesignTokens.SpacingMedium,
            bottom = DesignTokens.SpacingMedium,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true },
            ),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_reminder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = scheduleLabel,
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
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_mark_as_done)) },
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Check, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onDone()
                    },
                )
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(UpcomingReminderDismissSize),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.reminders_home_card_dismiss),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
