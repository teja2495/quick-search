package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsRepository
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    if (displayedSettings.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_device_settings_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 24.dp),
        )
        return
    }

    SettingsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            displayedSettings.forEachIndexed { index, setting ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSettingClick(setting) }
                            .padding(PaddingValues(horizontal = 24.dp, vertical = 16.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = setting.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        setting.description?.takeIf { it.isNotBlank() }?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.desc_navigate_forward),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (index != displayedSettings.lastIndex) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                }
            }
        }
    }
}
