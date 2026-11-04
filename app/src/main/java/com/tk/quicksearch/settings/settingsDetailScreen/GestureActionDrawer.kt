package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * One choice in a [GestureActionDrawer].
 *
 * @param opensPicker Shows a chevron: picking it opens another drawer instead of choosing an action.
 * @param onDelete Shows a remove button, for saved shortcuts.
 */
internal class GestureOption(
    val key: String,
    val label: String,
    val selected: Boolean = false,
    val subtitle: String? = null,
    val opensPicker: Boolean = false,
    val onDelete: (() -> Unit)? = null,
    val onClick: () -> Unit,
)

/** A group of [GestureOption]s. Groups are separated by a divider; empty ones are skipped. */
internal class GestureOptionSection(
    val options: List<GestureOption>,
)

/**
 * Bottom drawer for choosing what a gesture does, with options grouped into [sections]. Choosing
 * an option closes the drawer first and runs its action once the close animation ends.
 */
@Composable
internal fun GestureActionDrawer(
    title: String,
    sections: List<GestureOptionSection>,
    onDismiss: () -> Unit,
) {
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val visibleSections = sections.filter { it.options.isNotEmpty() }

    AppPickerDrawer(
        title = title,
        onDismiss = { pendingAction?.invoke() ?: onDismiss() },
    ) { dismiss ->
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing)) {
            visibleSections.forEachIndexed { index, section ->
                if (index > 0) {
                    item(key = "divider_$index") {
                        HorizontalDivider(
                            color = AppColors.SettingsDivider,
                            modifier = Modifier.padding(vertical = DesignTokens.SpacingSmall),
                        )
                    }
                }
                items(section.options, key = { it.key }) { option ->
                    GestureOptionRow(
                        option = option,
                        onClick = {
                            pendingAction = option.onClick
                            dismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureOptionRow(
    option: GestureOption,
    onClick: () -> Unit,
) {
    val trailing: @Composable () -> Unit =
        when {
            option.opensPicker -> {
                {
                    Icon(
                        imageVector = MaterialSymbols.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            option.onDelete != null -> {
                {
                    IconButton(onClick = option.onDelete) {
                        Icon(
                            imageVector = MaterialSymbols.Delete,
                            contentDescription = stringResource(R.string.action_remove),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            // An empty slot keeps the drawer row from adding its selected check; the radio marks it.
            else -> {
                {}
            }
        }
    AppPickerDrawerRow(
        title = option.label,
        subtitle = option.subtitle,
        selected = option.selected,
        onClick = onClick,
        leading =
            if (option.opensPicker) {
                null
            } else {
                { RadioButton(selected = option.selected, onClick = null) }
            },
        trailing = trailing,
    )
}
