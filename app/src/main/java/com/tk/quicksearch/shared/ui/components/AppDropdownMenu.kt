package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
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

/** Colors for [AppDropdownMenu]. */
object AppDropdownMenuDefaults {
    /**
     * The menu surface. Material's default (`surfaceContainerLow`) nearly matches the drawers and
     * sheets menus open over, so menus sit on the highest container with a level-3 shadow instead.
     */
    val containerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest
}

/**
 * The app's Material 3 Expressive vertical menu on [AppDropdownMenuDefaults.containerColor].
 * The focus policy lets menus opened over search preserve the keyboard.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    focusable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    ReportOpenPopupEffect(expanded)
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        popupPositionProvider =
            MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                dropdownMenuAnchorPosition = MenuAnchorPosition.Below,
                offset = offset,
            ),
        properties = PopupProperties(focusable = focusable),
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShapes(),
            modifier = modifier
                // Keep the inset visible while scrolling so end rows clear the rounded corners.
                .padding(vertical = DesignTokens.SpacingXSmall)
                .verticalScroll(rememberScrollState()),
            contentPadding = PaddingValues(0.dp),
            containerColor = AppDropdownMenuDefaults.containerColor,
            shadowElevation = DesignTokens.ElevationLevel3,
            content = content,
        )
    }
}

/** Divider placed between [AppDropdownMenu] items. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppDropdownMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding),
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        text = { Text(text = text) },
        onClick = onClick,
        shape = MenuDefaults.standaloneItemShape,
        modifier = modifier.heightIn(min = DesignTokens.MinTouchTarget),
        leadingIcon = leadingIcon,
        supportingText = supportingText?.let { label -> { Text(text = label) } },
        enabled = enabled,
        colors = colors,
    )
}

/** An expressive single-choice row: a secondary-container pill and trailing check when selected, with radio semantics. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppSelectableDropdownMenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val checkSize = MenuDefaults.TrailingIconSize
    SelectableDropdownMenuItem(
        selected = selected,
        text = { Text(text = text) },
        onClick = onClick,
        shapes = MenuDefaults.itemShapes(),
        modifier = modifier.heightIn(min = DesignTokens.MinTouchTarget),
        trailingContent = {
            if (selected) {
                Icon(
                    imageVector = MaterialSymbols.Check,
                    contentDescription = null,
                    modifier = Modifier.size(checkSize),
                )
            } else {
                Spacer(Modifier.size(checkSize))
            }
        },
        // Unselected rows stay transparent on the menu surface (Material paints surfaceContainerLow,
        // which shows as darker bands on our higher surface). The selected pill uses the same
        // secondary container as the tonal sort button, so it matches the app's palette.
        colors = MenuDefaults.selectableItemColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTrailingContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        enabled = enabled,
    )
}
