package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.reminders.ReminderEditorRequests
import com.tk.quicksearch.reminders.ReminderPermissions
import com.tk.quicksearch.reminders.rememberMissingReminderPermissionRequester
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.reminders.ReminderRelativeDateText
import com.tk.quicksearch.search.reminders.reminderScheduleLabel
import com.tk.quicksearch.settings.AppShortcutsSettings.shortcutMatchPriority
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RemindersSettingsSection(
    searchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember(context) { ReminderRepository(context) }
    val changeCount by ReminderRepository.changes.collectAsState()
    var hasPermissions by remember { mutableStateOf(ReminderPermissions.hasAllPermissions(context)) }
    val refreshPermissions = { hasPermissions = ReminderPermissions.hasAllPermissions(context) }
    val requestMissingPermission = rememberMissingReminderPermissionRequester(onResult = refreshPermissions)

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val reminders by produceState(initialValue = emptyList<ReminderInfo>(), changeCount) {
        value = withContext(Dispatchers.IO) { repository.getAllReminders() }
    }
    val locale = Locale.getDefault()
    val normalizedQuery = remember(searchQuery, locale) { searchQuery.trim().lowercase(locale) }
    val today = LocalDate.now()
    val sortedReminders =
        remember(reminders, normalizedQuery, locale) {
            reminders
                .filter { reminder ->
                    normalizedQuery.isBlank() ||
                        shortcutMatchPriority(name = reminder.title, query = normalizedQuery, locale = locale) != null
                }
                // Upcoming first (soonest at top), then past ones (most recent first).
                .sortedWith(
                    compareBy<ReminderInfo> { it.date.isBefore(today) }
                        .thenBy { if (it.date.isBefore(today)) -it.dueMillis else it.dueMillis },
                )
        }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Column(modifier = modifier) {
        if (!hasPermissions) {
            SettingsCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SectionTopPadding),
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable(onClick = requestMissingPermission)
                            .padding(
                                horizontal = DesignTokens.CardHorizontalPadding,
                                vertical = DesignTokens.CardVerticalPadding,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(DesignTokens.IconSize),
                    )
                    Text(
                        text = stringResource(R.string.reminders_permission_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            if (sortedReminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.reminders_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = screenHeight)) {
                    itemsIndexed(items = sortedReminders, key = { _, reminder -> reminder.reminderId }) { index, reminder ->
                        ReminderManagementRow(
                            reminder = reminder,
                            isPast = reminder.isDone || reminder.date.isBefore(today),
                            onClick = { ReminderEditorRequests.openEdit(reminder) },
                        )
                        if (index < sortedReminders.lastIndex) {
                            HorizontalDivider(color = AppColors.SettingsDivider)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderManagementRow(
    reminder: ReminderInfo,
    isPast: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .alpha(if (isPast) 0.65f else 1f)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_reminder),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
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
    }
}
