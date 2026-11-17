package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.connectedButtonShape

/** Declares the buttons of a [ConnectedButtonRow], in order. */
class ConnectedButtonRowScope internal constructor() {
    internal class Entry(
        val onClick: () -> Unit,
        val width: Dp?,
        val weight: Float,
        val content: @Composable RowScope.() -> Unit,
    )

    internal val entries = mutableListOf<Entry>()

    /**
     * Adds a button. It takes a fixed [width] when given, otherwise a [weight] share of the row.
     * Content is laid out centered in a row.
     */
    fun button(
        onClick: () -> Unit,
        width: Dp? = null,
        weight: Float = 1f,
        content: @Composable RowScope.() -> Unit,
    ) {
        entries += Entry(onClick, width, weight, content)
    }
}

/**
 * A row of Material 3 Expressive connected buttons, [DesignTokens.ConnectedGroupGap] apart. The
 * first and last buttons are fully round on the group's outer edges; edges between buttons use the
 * small inner corner. The row works out each button's position, so conditional buttons just work.
 *
 * Buttons are tonal in [tone]. With [pressMorph] the corners tighten while pressed ([pressedOuter]
 * for the outer edges). Content inherits [textStyle].
 */
@Composable
fun ConnectedButtonRow(
    modifier: Modifier = Modifier,
    tone: AppButtonTone = AppButtonTone.Accent,
    height: Dp = DesignTokens.FloatingActionHeight,
    pressMorph: Boolean = true,
    pressedOuter: Dp = DesignTokens.ButtonPressedCornerMedium,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    contentPadding: PaddingValues = PaddingValues(horizontal = DesignTokens.SpacingLarge),
    content: @Composable ConnectedButtonRowScope.() -> Unit,
) {
    val entries = ConnectedButtonRowScope().apply { content() }.entries
    val colors = tone.tonalColors()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ConnectedGroupGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entries.forEachIndexed { index, entry ->
            key(index) {
                val interactionSource = remember { MutableInteractionSource() }
                val isStart = index == 0
                val isEnd = index == entries.lastIndex
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
                val sizeModifier = entry.width?.let { Modifier.width(it) } ?: Modifier.weight(entry.weight)
                Surface(
                    onClick = entry.onClick,
                    modifier = sizeModifier.height(height),
                    interactionSource = interactionSource,
                    shape = shape,
                    color = colors.containerColor,
                    contentColor = colors.contentColor,
                    tonalElevation = DesignTokens.ElevationLevel0,
                ) {
                    CompositionLocalProvider(LocalTextStyle provides textStyle) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(contentPadding),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            content = entry.content,
                        )
                    }
                }
            }
        }
    }
}
