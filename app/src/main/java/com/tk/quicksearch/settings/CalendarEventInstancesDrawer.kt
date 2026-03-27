package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.calendar.calendarRecurrenceLabel
import com.tk.quicksearch.search.calendar.calendarRelativeDateLabel
import com.tk.quicksearch.search.calendar.formatCalendarEventDate
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.shared.ui.components.AppBottomSheet
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun CalendarEventInstancesDrawer(
    title: String,
    nearestInstance: CalendarEventInfo,
    instances: List<CalendarEventInfo>,
    onDismissRequest: () -> Unit,
    onInstanceClick: (CalendarEventInfo) -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = DesignTokens.SpacingLarge),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.padding(
                        start = DesignTokens.ContentHorizontalPadding,
                        end = DesignTokens.ContentHorizontalPadding,
                        bottom = DesignTokens.SpacingSmall,
                    ),
            )
            calendarRecurrenceLabel(
                recurrenceRule = nearestInstance.recurrenceRule,
                instanceCount = instances.size,
            )?.let { recurrenceText ->
                Text(
                    text = recurrenceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.padding(
                            start = DesignTokens.ContentHorizontalPadding,
                            end = DesignTokens.ContentHorizontalPadding,
                            bottom = DesignTokens.SpacingMedium,
                        ),
                )
            }
            HorizontalDivider(color = AppColors.SettingsDivider)
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
            ) {
                itemsIndexed(
                    items = instances,
                    key = { _, instance -> "${instance.eventId}_${instance.startMillis}" },
                ) { index, instance ->
                    CalendarEventInstanceDrawerRow(
                        dateLabel = formatCalendarEventDate(instance),
                        relativeLabel = calendarRelativeDateLabel(instance.startMillis),
                        onClick = { onInstanceClick(instance) },
                    )
                    if (index < instances.lastIndex) {
                        HorizontalDivider(color = AppColors.SettingsDivider)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarEventInstanceDrawerRow(
    dateLabel: String,
    relativeLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = relativeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
