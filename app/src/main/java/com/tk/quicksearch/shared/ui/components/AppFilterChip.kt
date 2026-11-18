package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes

/** Unselected look of an [AppFilterChip]. */
enum class AppFilterChipStyle {
    /** Transparent with a divider-colored outline (Material's default filter chip). */
    Outlined,

    /** Filled with the accent's tonal tint and no outline, for chips inside a tonal surface. */
    Tonal,
}

/**
 * Material 3 filter chip: a pill that fills with the accent when selected. [showSelectedCheck]
 * adds the leading check Material uses for multi-select chips; leave it off for single-select
 * rows such as type filters. Over a wallpaper, pass the wallpaper-aware [unselectedLabelColor]
 * and [unselectedBorderColor].
 */
@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    style: AppFilterChipStyle = AppFilterChipStyle.Outlined,
    showSelectedCheck: Boolean = false,
    unselectedLabelColor: Color = MaterialTheme.colorScheme.onSurface,
    unselectedBorderColor: Color = AppColors.SettingsDivider,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        modifier = modifier,
        leadingIcon =
            if (showSelectedCheck && selected) {
                {
                    Icon(
                        imageVector = MaterialSymbols.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else {
                null
            },
        shape = AppShapes.full,
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor =
                    when (style) {
                        AppFilterChipStyle.Outlined -> Color.Transparent
                        AppFilterChipStyle.Tonal -> AppColors.AccentTintTonal
                    },
                labelColor = unselectedLabelColor,
                selectedContainerColor = AppColors.Accent,
                selectedLabelColor = AppColors.OnAccent,
                selectedLeadingIconColor = AppColors.OnAccent,
            ),
        border =
            when (style) {
                AppFilterChipStyle.Outlined ->
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = unselectedBorderColor,
                        selectedBorderColor = Color.Transparent,
                    )
                AppFilterChipStyle.Tonal -> null
            },
    )
}
