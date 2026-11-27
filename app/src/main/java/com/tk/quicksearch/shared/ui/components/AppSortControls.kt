package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Square size for the icon buttons beside the sort button, so the row reads as one band. */
val SortRowIconButtonSize = DesignTokens.TonalSettingsButtonHeight

/**
 * A full-width sort row: a tonal "Sort by" button that stretches across the free space and opens
 * the criteria menu (as wide as the button), then a tonal direction toggle, then [trailingActions]
 * (use [SortRowIconButtonSize] for their buttons).
 */
@Composable
fun <T> AppSortControls(
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    isAscending: Boolean,
    onAscendingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    trailingActions: @Composable RowScope.() -> Unit = {},
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var buttonWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val pillLabel = stringResource(R.string.settings_app_sort_by_format, optionLabel(selectedOption))

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        Box(modifier = Modifier.weight(1f).onSizeChanged { buttonWidthPx = it.width }) {
            AppTonalButton(
                onClick = { isMenuExpanded = true },
                icon = MaterialSymbols.ExpandMore,
                modifier = Modifier.fillMaxWidth().heightIn(min = DesignTokens.TonalSettingsButtonHeight),
            ) {
                Text(
                    text = pillLabel,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AppDropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier.width(with(density) { buttonWidthPx.toDp() }),
                offset = DpOffset(0.dp, DesignTokens.SpacingXSmall),
                focusable = true,
            ) {
                options.forEach { option ->
                    AppSelectableDropdownMenuItem(
                        text = optionLabel(option),
                        selected = option == selectedOption,
                        onClick = {
                            onOptionSelected(option)
                            isMenuExpanded = false
                        },
                    )
                }
            }
        }
        val ascendingLabel = stringResource(R.string.settings_app_sort_ascending)
        val descendingLabel = stringResource(R.string.settings_app_sort_descending)
        val directionAction = if (isAscending) descendingLabel else ascendingLabel
        val directionState = if (isAscending) ascendingLabel else descendingLabel
        AppIconButton(
            onClick = { onAscendingChange(!isAscending) },
            tooltip = directionAction,
            style = AppIconButtonStyle.Tonal,
            modifier = Modifier.size(SortRowIconButtonSize).semantics { stateDescription = directionState },
        ) {
            Icon(
                imageVector = if (isAscending) MaterialSymbols.ArrowUpward else MaterialSymbols.ArrowDownward,
                contentDescription = directionAction,
            )
        }
        trailingActions()
    }
}
