package com.tk.quicksearch.widgets.worldClockWidget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerIconBadge
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerMessage
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerSearch
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bottom drawer, like the Custom Buttons action picker, that searches the device's city time zones
 * by city, country or zone name. Places already on the widget are left out.
 */
@Composable
internal fun WorldClockLocationSearchDialog(
    selectedZoneIds: Set<String>,
    onDismiss: () -> Unit,
    onSelect: (WorldClockZone) -> Unit,
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val keyboardController = LocalSoftwareKeyboardController.current
    // Set once a place is picked or the drawer starts closing, so the keyboard committing its
    // in-progress text as the field goes away can't bring the old query back.
    var isClosing by remember { mutableStateOf(false) }
    val allZones by produceState<List<WorldClockZone>?>(initialValue = null) {
        value = withContext(Dispatchers.Default) { WorldClockZones.all() }
    }
    val results =
        remember(allZones, query.text, selectedZoneIds) {
            allZones?.let { zones -> WorldClockZones.search(query.text, zones.filter { it.zoneId !in selectedZoneIds }) }
        }
    val closeKeyboard: () -> Unit = {
        isClosing = true
        keyboardController?.hide()
    }

    AppPickerDrawer(
        title = stringResource(R.string.widget_world_clock_add_location),
        onDismiss = onDismiss,
        search =
            AppPickerDrawerSearch(
                query = query,
                onQueryChange = { if (!isClosing) query = it },
                placeholder = stringResource(R.string.widget_world_clock_search_hint),
                autoFocus = true,
            ),
        onDismissStarted = closeKeyboard,
    ) {
        when {
            results == null ->
                Box(modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.SpacingXLarge)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            results.isEmpty() -> AppPickerDrawerMessage(text = stringResource(R.string.widget_world_clock_no_results))
            else ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing)) {
                    items(results, key = { it.zoneId }) { zone ->
                        ZoneRow(
                            zone = zone,
                            onClick = {
                                closeKeyboard()
                                onSelect(zone)
                            },
                        )
                    }
                }
        }
    }
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

@Composable
private fun ZoneRow(
    zone: WorldClockZone,
    onClick: () -> Unit,
) {
    val time = remember(zone.zoneId) { Instant.now().atZone(ZoneId.of(zone.zoneId)).format(timeFormatter) }
    val offset = remember(zone.zoneId) { WorldClockZones.offsetFromDevice(zone.zoneId) }
    AppPickerDrawerRow(
        title = zone.city,
        subtitle = listOf(zone.country, zone.zoneName).filter { it.isNotBlank() }.joinToString(" · "),
        onClick = onClick,
        leading = { AppPickerDrawerIconBadge(icon = MaterialSymbols.Public) },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (offset != null) {
                    Text(
                        text = offset,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        titleMaxLines = 1,
    )
}
