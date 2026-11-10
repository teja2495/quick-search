package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme

/** Container colors for [AppDropdownMenu]. */
object AppDropdownMenuDefaults {
    /** Standard popup menu surface. */
    val containerColor: Color
        @Composable get() = AppColors.DialogBackground

    /** Long-press menus over result items, which sit on plain black/white for contrast with the cards. */
    val itemMenuContainerColor: Color
        @Composable get() = if (LocalAppIsDarkTheme.current) Color.Black else Color.White
}

/**
 * The app's popup menu: rounded 24dp surface that doesn't steal focus from the search field.
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
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = RoundedCornerShape(24.dp),
        properties = PopupProperties(focusable = focusable),
        containerColor = containerColor,
        content = content,
    )
}

/** Divider placed between [AppDropdownMenu] items. */
@Composable
fun AppDropdownMenuDivider() {
    HorizontalDivider()
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
                    modifier = Modifier.padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
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
        enabled = enabled,
        colors = colors,
    )
}
