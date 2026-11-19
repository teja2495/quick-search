package com.tk.quicksearch.widgets.worldClockWidget

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLengthTrim
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.AppTextButton
import com.tk.quicksearch.shared.ui.theme.AppShapes

/** The widget's clocks in order, each of which can be renamed, moved left or removed, plus an Add row. */
@Composable
internal fun WorldClockLocationsSection(
    locations: List<WorldClockLocation>,
    canAdd: Boolean,
    onAddClick: () -> Unit,
    onLocationsChange: (List<WorldClockLocation>) -> Unit,
) {
    var renamingIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val count = locations.size + 1
    SegmentedGroup(title = stringResource(R.string.widget_world_clock_locations)) {
        locations.forEachIndexed { index, location ->
            SegmentedTile(index = index, count = count, onClick = { renamingIndex = index }) {
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
            }
        }
        SegmentedTile(index = locations.size, count = count, onClick = if (canAdd) onAddClick else null) {
            AddLocationRow(enabled = canAdd)
        }
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
                .heightIn(min = LocationRowMinHeight)
                .padding(start = DesignTokens.SpacingLarge, top = DesignTokens.SpacingSmall, bottom = DesignTokens.SpacingSmall, end = DesignTokens.SpacingXSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        TonalIconBadge(icon = MaterialSymbols.Public)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (hasLabel) location.label else city,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(if (hasLabel) city else "", country).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AppIconButton(onClick = onRename, tooltip = stringResource(R.string.settings_edit_label)) {
            Icon(
                imageVector = MaterialSymbols.Edit,
                contentDescription = stringResource(R.string.settings_edit_label),
                modifier = Modifier.size(DesignTokens.IconSizeSmall),
            )
        }
        if (canMoveUp) {
            AppIconButton(onClick = onMoveUp, tooltip = stringResource(R.string.action_move_up)) {
                Icon(
                    imageVector = MaterialSymbols.ArrowUpward,
                    contentDescription = stringResource(R.string.action_move_up),
                    modifier = Modifier.size(DesignTokens.IconSizeSmall),
                )
            }
        }
        AppIconButton(onClick = onRemove, tooltip = stringResource(R.string.action_remove)) {
            Icon(
                imageVector = MaterialSymbols.Close,
                contentDescription = stringResource(R.string.action_remove),
                modifier = Modifier.size(DesignTokens.IconSizeSmall),
            )
        }
    }
}

@Composable
private fun AddLocationRow(enabled: Boolean) {
    val color = if (enabled) AppColors.Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.DisabledContentAlpha)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = LocationRowMinHeight)
                .padding(horizontal = DesignTokens.SpacingLarge, vertical = DesignTokens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(AppShapes.full)
                    .background(color.copy(alpha = AppColors.TintTonalAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MaterialSymbols.Add,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(DesignTokens.IconSize),
            )
        }
        Text(
            text = stringResource(R.string.widget_world_clock_add_location),
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
    }
}

private val LocationRowMinHeight = 72.dp

/**
 * Sets the name shown on the widget; the current one is the hint. Leaving it empty keeps that name,
 * and typing the city's name goes back to following the city's name.
 */
@Composable
private fun RenameLocationDialog(
    location: WorldClockLocation,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val cityName = remember(location.zoneId) { WorldClockZones.cityName(location.zoneId) }
    val currentName = remember(location) { WorldClockZones.displayName(location) }
    val textState = rememberSaveable(location.zoneId, saver = TextFieldState.Saver) { TextFieldState() }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widget_world_clock_edit_name)) },
        text = {
            AppOutlinedTextField(
                state = textState,
                inputTransformation = InputTransformation.maxLengthTrim(WorldClockWidgetDefaults.LABEL_MAX_LENGTH),
                placeholder = { Text(currentName) },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                label = null,
                shape = AppShapes.extraSmall,
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = {
                    val name = textState.text.toString().trim()
                    onConfirm(
                        when {
                            name.isEmpty() -> location.label
                            name == cityName -> ""
                            else -> name
                        },
                    )
                },
            ) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = { AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}
