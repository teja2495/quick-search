package com.tk.quicksearch.widgets.worldClockWidget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants

/** The widget's clocks in order, each of which can be renamed, moved left or removed, plus an Add row. */
@Composable
internal fun WorldClockLocationsSection(
    locations: List<WorldClockLocation>,
    canAdd: Boolean,
    onAddClick: () -> Unit,
    onLocationsChange: (List<WorldClockLocation>) -> Unit,
) {
    var renamingIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)) {
        Text(
            text = stringResource(R.string.widget_world_clock_locations),
            style = MaterialTheme.typography.titleSmall,
        )
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            locations.forEachIndexed { index, location ->
                LocationRow(
                    location = location,
                    canMoveUp = index > 0,
                    onRename = { renamingIndex = index },
                    onMoveUp = {
                        onLocationsChange(
                            locations.toMutableList().apply { add(index - 1, removeAt(index)) },
                        )
                    },
                    onRemove = { onLocationsChange(locations.filterIndexed { i, _ -> i != index }) },
                )
                HorizontalDivider(color = AppColors.SettingsDivider)
            }
            AddLocationRow(enabled = canAdd, onClick = onAddClick)
        }
        Text(
            text = stringResource(R.string.widget_world_clock_locations_hint, WorldClockWidgetDefaults.MAX_LOCATIONS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val renaming = renamingIndex?.let { locations.getOrNull(it) }
    if (renaming != null) {
        RenameLocationDialog(
            location = renaming,
            onDismiss = { renamingIndex = null },
            onConfirm = { label ->
                val index = renamingIndex ?: return@RenameLocationDialog
                renamingIndex = null
                onLocationsChange(
                    locations.mapIndexed { i, it -> if (i == index) it.copy(label = label.trim().sanitizedLabel()) else it },
                )
            },
        )
    }
}

@Composable
private fun LocationRow(
    location: WorldClockLocation,
    canMoveUp: Boolean,
    onRename: () -> Unit,
    onMoveUp: () -> Unit,
    onRemove: () -> Unit,
) {
    val city = WorldClockZones.cityName(location.zoneId)
    val country = remember(location.zoneId) { WorldClockZones.countryName(location.zoneId) }
    val hasLabel = location.label.isNotBlank()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onRename)
                .padding(start = DesignTokens.CardHorizontalPadding, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (hasLabel) location.label else city,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(if (hasLabel) city else "", country).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRename) {
            Icon(
                imageVector = MaterialSymbols.Edit,
                contentDescription = stringResource(R.string.settings_edit_label),
                modifier = Modifier.size(DesignTokens.IconSizeSmall),
            )
        }
        if (canMoveUp) {
            IconButton(onClick = onMoveUp) {
                Icon(
                    imageVector = MaterialSymbols.ArrowUpward,
                    contentDescription = stringResource(R.string.action_move_up),
                    modifier = Modifier.size(DesignTokens.IconSizeSmall),
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = MaterialSymbols.Close,
                contentDescription = stringResource(R.string.action_remove),
                modifier = Modifier.size(DesignTokens.IconSizeSmall),
            )
        }
    }
}

@Composable
private fun AddLocationRow(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = DesignTokens.CardHorizontalPadding, vertical = DesignTokens.CardVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Icon(
            imageVector = MaterialSymbols.Add,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(DesignTokens.IconSizeSmall),
        )
        Text(
            text = stringResource(R.string.widget_world_clock_add_location),
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
    }
}

/** Sets the name shown on the widget; leaving it empty goes back to the city's name. */
@Composable
private fun RenameLocationDialog(
    location: WorldClockLocation,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable(location.zoneId) { mutableStateOf(location.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_edit_label)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(WorldClockWidgetDefaults.LABEL_MAX_LENGTH) },
                label = { Text(stringResource(R.string.widget_world_clock_label_hint)) },
                placeholder = { Text(WorldClockZones.cityName(location.zoneId)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.dialog_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}
