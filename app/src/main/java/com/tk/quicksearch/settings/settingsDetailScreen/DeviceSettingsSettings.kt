package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.lazy.rememberLazyListState
import com.tk.quicksearch.settings.shared.settingsScrollClip
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsRepository
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.settings.shared.SettingsIconBadge
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.components.TonalIconBadge

@Composable
fun DeviceSettingsSettingsSection(
    settings: List<DeviceSetting>,
    onSettingClick: (DeviceSetting) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Search state populates this list during startup, which has not necessarily run when this
    // screen is opened directly (for example when a cold launch restores it). Load the catalog
    // here so the screen always lists the settings the user can search and open.
    val fallbackSettings by produceState(initialValue = emptyList(), settings.isEmpty(), context) {
        value =
            if (settings.isEmpty()) {
                withContext(Dispatchers.IO) {
                    DeviceSettingsRepository(context).loadShortcuts().sortedBy {
                        it.title.lowercase(Locale.getDefault())
                    }
                }
            } else {
                emptyList()
            }
    }

    val displayedSettings = remember(settings, fallbackSettings) {
        settings.ifEmpty { fallbackSettings }
    }

    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().settingsScrollClip(listState),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
    ) {
        if (displayedSettings.isEmpty()) {
            item(key = "empty") {
                SegmentedTile(index = 0, count = 1) {
                    Text(
                        text = stringResource(R.string.settings_device_settings_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(DesignTokens.SpacingLarge),
                    )
                }
            }
        } else {
            itemsIndexed(displayedSettings, key = { _, setting -> setting.id }) { index, setting ->
                SegmentedTile(index = index, count = displayedSettings.size, onClick = { onSettingClick(setting) }) {
                    SegmentedTileRow(
                        title = setting.title,
                        description = setting.description?.takeIf { it.isNotBlank() },
                        leading = { TonalIconBadge(icon = MaterialSymbols.Settings) },
                    )
                }
            }
        }
    }
}
