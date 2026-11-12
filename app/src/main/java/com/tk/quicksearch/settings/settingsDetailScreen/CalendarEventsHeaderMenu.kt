package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shares the Calendar Events page's hidden-event state between its header menu and its list. */
internal object CalendarHiddenEventsState {
    private val _hasHiddenEvents = MutableStateFlow(false)
    val hasHiddenEvents: StateFlow<Boolean> = _hasHiddenEvents.asStateFlow()

    private val _showHiddenEvents = MutableStateFlow(false)
    val showHiddenEvents: StateFlow<Boolean> = _showHiddenEvents.asStateFlow()

    fun setHasHiddenEvents(value: Boolean) {
        _hasHiddenEvents.value = value
    }

    fun setShowHiddenEvents(value: Boolean) {
        _showHiddenEvents.value = value
    }

    fun reset() {
        _hasHiddenEvents.value = false
        _showHiddenEvents.value = false
    }
}

/** The three-dot button in the Calendar Events page header, shown only while some events are hidden. */
@Composable
fun CalendarEventsHeaderMenu() {
    val hasHiddenEvents by CalendarHiddenEventsState.hasHiddenEvents.collectAsState()
    val showHiddenEvents by CalendarHiddenEventsState.showHiddenEvents.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    if (!hasHiddenEvents) return

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = MaterialSymbols.MoreVert,
                contentDescription = stringResource(R.string.settings_more_options_title),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppDropdownMenuItem(
                text =
                    stringResource(
                        if (showHiddenEvents) {
                            R.string.settings_calendar_hide_hidden_events
                        } else {
                            R.string.settings_calendar_show_hidden_events
                        },
                    ),
                icon = if (showHiddenEvents) MaterialSymbols.VisibilityOff else MaterialSymbols.Visibility,
                onClick = {
                    expanded = false
                    CalendarHiddenEventsState.setShowHiddenEvents(!showHiddenEvents)
                },
            )
        }
    }
}
