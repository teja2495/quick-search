package com.tk.quicksearch.customInfo

import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.animation.core.tween
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.GlanceStatusRow
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.TonalButton
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.components.CardTextField
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.MarkdownText
import java.util.Calendar
import kotlinx.coroutines.delay

/** Title and prompt in one card, with the Preview action tucked under the prompt. */
@Composable
internal fun CustomInfoContentCard(
    title: String,
    onTitleChange: (String) -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    previewEnabled: Boolean,
    onPreview: () -> Unit,
) {
    SegmentedGroup {
        SegmentedTile(index = 0, count = 2) {
            CardTextField(
                value = title,
                onValueChange = onTitleChange,
                label = stringResource(R.string.notes_title_hint),
                singleLine = true,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = DesignTokens.SpacingSmall, vertical = DesignTokens.SpacingXSmall),
            )
        }
        SegmentedTile(index = 1, count = 2) {
            Column {
                CardTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    label = stringResource(R.string.custom_info_prompt),
                    minLines = 3,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
                        .padding(horizontal = DesignTokens.SpacingSmall, vertical = DesignTokens.SpacingXSmall),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        end = DesignTokens.SpacingLarge,
                        bottom = DesignTokens.SpacingMedium,
                    ),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TonalButton(
                        text = stringResource(R.string.custom_info_preview),
                        onClick = onPreview,
                        icon = MaterialSymbols.PlayArrow,
                        enabled = previewEnabled,
                        small = true,
                    )
                }
            }
        }
    }
}

/** The answer laid out as the At a Glance row it will become on Home. */
@Composable
internal fun CustomInfoPreviewCard(
    title: String,
    loading: Boolean,
    response: String,
    onDismiss: () -> Unit,
) {
    SegmentedTile(index = 0, count = 1) {
        Box(Modifier.padding(horizontal = DesignTokens.SpacingSmall, vertical = DesignTokens.SpacingXSmall)) {
            GlanceStatusRow(
                icon = {
                    Icon(MaterialSymbols.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                title = title.ifBlank { stringResource(R.string.custom_info_title) },
                onClick = {},
                onDismiss = onDismiss,
                belowText = {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(top = DesignTokens.SpacingXSmall).size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        MarkdownText(
                            markdown = response,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
    }
}

/** Date, repeat, and notification settings as rows of one card. */
@Composable
internal fun CustomInfoScheduleCard(
    dueMillis: Long?,
    onDateClick: () -> Unit,
    repeat: CustomInfoRepeat?,
    onRepeatChange: (CustomInfoRepeat?) -> Unit,
    sendNotification: Boolean,
    onSendNotificationChange: (Boolean) -> Unit,
) {
    SegmentedGroup {
        SegmentedTile(index = 0, count = 3) {
            SettingsToggleRow(
                title = dueMillis?.let { customInfoDueLabel(it) } ?: stringResource(R.string.custom_info_set_date_time),
                checked = false,
                onCheckedChange = {},
                onRowClick = onDateClick,
                showSwitch = false,
                leadingIcon = MaterialSymbols.CalendarMonth,
                isFirstItem = true,
                isLastItem = true,
                trailingAction = { CustomInfoRowChevron() },
            )
        }
        SegmentedTile(index = 1, count = 3) {
            CustomInfoRepeatRow(repeat = repeat, onRepeatChange = onRepeatChange)
        }
        SegmentedTile(index = 2, count = 3) {
            SettingsToggleRow(
                title = stringResource(R.string.custom_info_send_notification),
                checked = sendNotification,
                onCheckedChange = onSendNotificationChange,
                leadingIcon = MaterialSymbols.NotificationsNone,
                isFirstItem = true,
                isLastItem = true,
            )
        }
    }
}

/**
 * A schedule row's trailing chevron, held at the switch's 48dp touch-target height so rows
 * without a switch match the notification row.
 */
@Composable
internal fun CustomInfoRowChevron() {
    Box(modifier = Modifier.height(48.dp), contentAlignment = Alignment.Center) {
        Icon(
            MaterialSymbols.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A brief "Saved" check for the header, shown each time [savedCount] goes up. */
@Composable
internal fun CustomInfoSavedIndicator(savedCount: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(savedCount) {
        if (savedCount == 0) return@LaunchedEffect
        visible = true
        delay(SAVED_INDICATOR_MS)
        visible = false
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing)), exit = fadeOut(tween(AppMotion.DurationExit, easing = AppMotion.EmphasizedAccelerateEasing))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                MaterialSymbols.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Box(Modifier.width(DesignTokens.SpacingXSmall))
            Text(
                text = stringResource(R.string.custom_info_saved),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Today, 6:00 PM", "Tomorrow, 9:00 AM", or the weekday and date further out. */
@Composable
private fun customInfoDueLabel(millis: Long): String {
    val context = LocalContext.current
    val time = DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME)
    val due = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    fun Calendar.sameDay(other: Calendar) =
        get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    return when {
        due.sameDay(today) -> "${stringResource(R.string.calendar_relative_today)}, $time"
        due.sameDay(tomorrow) -> "${stringResource(R.string.calendar_relative_tomorrow)}, $time"
        else -> DateUtils.formatDateTime(
            context,
            millis,
            DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_TIME,
        )
    }
}

private const val SAVED_INDICATOR_MS = 1_500L
