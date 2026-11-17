package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.connectedButtonShape

/**
 * A row of Material 3 Expressive connected buttons: children sit [DesignTokens.ConnectedGroupGap]
 * apart, and each child is a [ConnectedButton] that knows whether it starts or ends the group.
 */
@Composable
fun ConnectedButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ConnectedGroupGap),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * One button of a connected group. [isStart]/[isEnd] buttons are fully round on the group's
 * outer edges; edges between buttons use the small inner corner. With [pressMorph] the corners
 * tighten while pressed ([pressedOuter] for the outer edges). Content is laid out centered in a
 * row and inherits [textStyle].
 */
@Composable
fun ConnectedButton(
    onClick: () -> Unit,
    isStart: Boolean,
    isEnd: Boolean,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = DesignTokens.FloatingActionHeight,
    pressMorph: Boolean = true,
    pressedOuter: Dp = DesignTokens.ButtonPressedCornerMedium,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    contentPadding: PaddingValues = PaddingValues(horizontal = DesignTokens.SpacingLarge),
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val outer = height / 2
    val shape =
        if (pressMorph) {
            connectedButtonShape(interactionSource, isStart, isEnd, outer = outer, pressedOuter = pressedOuter)
        } else {
            AppShapes.connected(
                start = if (isStart) outer else DesignTokens.ConnectedGroupInnerCorner,
                end = if (isEnd) outer else DesignTokens.ConnectedGroupInnerCorner,
            )
        }
    Surface(
        onClick = onClick,
        modifier = modifier.height(height),
        interactionSource = interactionSource,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        CompositionLocalProvider(LocalTextStyle provides textStyle) {
            Row(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}
