package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppOverflowMenu
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
    if (!hasHiddenEvents) return

    AppOverflowMenu { dismiss ->
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
                dismiss()
                CalendarHiddenEventsState.setShowHiddenEvents(!showHiddenEvents)
            },
        )
    }
}
