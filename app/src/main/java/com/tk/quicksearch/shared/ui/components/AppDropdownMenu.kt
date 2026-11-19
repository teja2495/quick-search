package com.tk.quicksearch.shared.ui.components

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** A three-dot button anchored to a dropdown. Menu items can dismiss it when selected. */
@Composable
fun AppOverflowMenu(content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val dismiss: () -> Unit = { expanded = false }
    Box {
        AppIconButton(onClick = { expanded = true }, tooltip = stringResource(R.string.settings_more_options_title)) {
            Icon(
                imageVector = MaterialSymbols.MoreVert,
                contentDescription = stringResource(R.string.settings_more_options_title),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        AppDropdownMenu(expanded = expanded, onDismissRequest = dismiss) {
            content(dismiss)
        }
    }
}

/** Container colors for [AppDropdownMenu]. */
object AppDropdownMenuDefaults {
    /** Standard popup menu surface. */
    val containerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer

    /** Tonal surface for long-press menus over result items. */
    val itemMenuContainerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer
}

/**
 * The app's popup menu: rounded tonal surface that doesn't steal focus from the search field.
 * Put [AppDropdownMenuDivider] between items.
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    containerColor: Color = AppDropdownMenuDefaults.containerColor,
    focusable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    ReportOpenPopupEffect(expanded)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = AppShapes.extraLarge,
        tonalElevation = DesignTokens.ElevationLevel0,
        shadowElevation = DesignTokens.ElevationLevel2,
        properties = PopupProperties(focusable = focusable),
        containerColor = containerColor,
        content = content,
    )
}

/** Divider placed between [AppDropdownMenu] items. */
@Composable
fun AppDropdownMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = DesignTokens.SpacingMedium),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * Text-and-icon row for [AppDropdownMenu], with an optional smaller [supportingText] line (such as
 * the current value); [destructive] tints it with the error color.
 */
@Composable
fun AppDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    supportingText: String? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    /** Overrides the leading icon's color, such as an accent for an active option. */
    iconTint: Color? = null,
) {
    AppDropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier,
        leadingIcon =
            icon?.let {
                {
                    if (iconTint != null) {
                        Icon(imageVector = it, contentDescription = null, tint = iconTint)
                    } else {
                        Icon(imageVector = it, contentDescription = null)
                    }
                }
            },
        supportingText = supportingText,
        destructive = destructive,
        enabled = enabled,
    )
}

/**
 * [AppDropdownMenuItem] with a custom [leadingIcon] slot, for icons that aren't a plain
 * [ImageVector] (drawable painters, icons chosen by the caller's menu model).
 */
@Composable
fun AppDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val colors =
        if (destructive) {
            MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.error,
                leadingIconColor = MaterialTheme.colorScheme.error,
            )
        } else {
            MenuDefaults.itemColors()
        }
    DropdownMenuItem(
        text = {
            if (supportingText == null) {
                Text(text = text)
            } else {
                // Extra vertical room so the two lines don't crowd the 48dp minimum row height.
                Column(
                    modifier = Modifier.padding(vertical = DesignTokens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing),
                ) {
                    Text(text = text)
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = DesignTokens.SpacingXSmall)
            .heightIn(min = DesignTokens.ItemMenuRowHeight)
            .clip(AppShapes.large),
        leadingIcon = leadingIcon,
        enabled = enabled,
        colors = colors,
    )
}

/**
 * A single-choice row for [AppDropdownMenu], such as a sort order: Material's
 * [SelectableDropdownMenuItem], with a check and an accent tint on the [selected] option and
 * radio button semantics.
 */
@Composable
fun AppSelectableDropdownMenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SelectableDropdownMenuItem(
        selected = selected,
        onClick = onClick,
        text = { Text(text = text) },
        shapes = MenuDefaults.itemShapes(shape = AppShapes.large, selectedShape = AppShapes.large),
        modifier = modifier
            .padding(horizontal = DesignTokens.SpacingXSmall)
            .heightIn(min = DesignTokens.ItemMenuRowHeight),
        selectedLeadingIcon = { Icon(imageVector = MaterialSymbols.Check, contentDescription = null) },
        enabled = enabled,
        colors =
            MenuDefaults.selectableItemColors(
                selectedContainerColor = AppColors.AccentTintTonal,
                selectedLeadingIconColor = AppColors.Accent,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}
