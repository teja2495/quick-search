package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.preferences.ReminderPreferences
import com.tk.quicksearch.search.noted.NotedIntegration
import com.tk.quicksearch.settings.bookmarksSettings.BookmarksHeaderMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

/** Detail pages whose header has a trailing menu built by [DetailHeaderMenu]. */
internal fun SettingsDetailType.hasHeaderMenu(): Boolean =
    this == SettingsDetailType.BOOKMARKS || this == SettingsDetailType.REMINDERS

@Composable
internal fun DetailHeaderMenu(detailType: SettingsDetailType) {
    when (detailType) {
        SettingsDetailType.BOOKMARKS -> BookmarksHeaderMenu()
        SettingsDetailType.REMINDERS -> RemindersHeaderMenu()
        else -> Unit
    }
}

/** The Reminders page menu. Only exists while the Noted app is installed and connected. */
@Composable
private fun RemindersHeaderMenu() {
    val context = LocalContext.current
    if (!remember(context) { NotedIntegration.ensureChecked(context) }) return
    val preferences = remember(context) { ReminderPreferences(context) }
    var useNoted by remember { mutableStateOf(preferences.isUseNotedEnabled()) }
    var expanded by remember { mutableStateOf(false) }
    val toggleUseNoted = {
        useNoted = !useNoted
        preferences.setUseNotedEnabled(useNoted)
        // The Reminders list shows Noted tasks only while this is on; reload it.
        ReminderRepository.notifyChanged()
    }

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
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.reminders_use_noted)) },
                // Display-only: the row handles the tap, so the box sizes like an icon, without a 48dp target.
                leadingIcon = { Checkbox(checked = useNoted, onCheckedChange = null) },
                onClick = toggleUseNoted,
            )
        }
    }
}
