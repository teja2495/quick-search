package com.tk.quicksearch.widgets.worldClockWidget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerMessage
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerSearch
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bottom drawer, like the Custom Buttons action picker, listing time zones grouped by country, each
 * with its capital or best-known city. Found by country, zone or city name; picking a place found by
 * a city it isn't shown as (San Francisco for Pacific Time) names the clock after that city. Zones
 * already on the widget are left out.
 */
@Composable
internal fun WorldClockLocationSearchDialog(
    selectedZoneIds: Set<String>,
    onDismiss: () -> Unit,
    /** The picked zone and, if it was found by another of its cities, that city. */
    onSelect: (WorldClockZone, String?) -> Unit,
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
            allZones?.let { zones -> WorldClockZoneSearch.search(query.text, zones.filter { it.zoneId !in selectedZoneIds }) }
        }
    val listState = rememberLazyListState()
    // New results start from the best match, not wherever the previous list was scrolled to.
    LaunchedEffect(results) { listState.scrollToItem(0) }
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
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing)) {
                    // groupBy keeps countries in the order of their best match.
                    results.groupBy { it.country }.forEach { (country, zones) ->
                        if (country.isNotBlank()) {
                            item(key = "country:$country") { CountryHeader(country) }
                        }
                        items(zones, key = { it.zoneId }) { zone ->
                            val matchedCity = remember(zone, query.text) { WorldClockZoneSearch.matchedCity(query.text, zone) }
                            ZoneRow(
                                zone = zone,
                                city = matchedCity ?: zone.city,
                                onClick = {
                                    closeKeyboard()
                                    onSelect(zone, matchedCity)
                                },
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun CountryHeader(country: String) {
    Text(
        text = country,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier.padding(
                start = DesignTokens.SpacingMedium,
                end = DesignTokens.SpacingMedium,
                top = DesignTokens.SpacingMedium,
            ),
    )
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

@Composable
private fun ZoneRow(
    zone: WorldClockZone,
    city: String,
    onClick: () -> Unit,
) {
    val time = remember(zone.zoneId) { Instant.now().atZone(ZoneId.of(zone.zoneId)).format(timeFormatter) }
    AppPickerDrawerRow(
        // Zones outside any country, such as UTC, have no header, so their name is enough.
        title = zone.zoneName.ifBlank { city },
        subtitle = city.takeIf { zone.zoneName.isNotBlank() && zone.country.isNotBlank() },
        onClick = onClick,
        trailing = {
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        titleMaxLines = 1,
    )
}
