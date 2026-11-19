package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens

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
 * A row of Material 3 Expressive connected buttons ([ButtonGroupDefaults.ConnectedSpaceBetween]
 * apart), built on Material's [FilledTonalButton] with the connected button group shapes: the
 * first and last buttons are fully round on the group's outer edges; edges between buttons use the
 * small inner corner. The row works out each button's position, so conditional buttons just work.
 *
 * Buttons are tonal in [tone]. With [pressMorph] the inner corners tighten while pressed, as in
 * Material's connected button group. Content inherits [textStyle].
 */
@Composable
fun ConnectedButtonRow(
    modifier: Modifier = Modifier,
    tone: AppButtonTone = AppButtonTone.Accent,
    height: Dp = DesignTokens.FloatingActionHeight,
    pressMorph: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    contentPadding: PaddingValues = PaddingValues(horizontal = DesignTokens.SpacingLarge),
    content: @Composable ConnectedButtonRowScope.() -> Unit,
) {
    val entries = ConnectedButtonRowScope().apply { content() }.entries
    val colors = tone.tonalColors()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entries.forEachIndexed { index, entry ->
            key(index) {
                val shapes = connectedButtonShapes(isStart = index == 0, isEnd = index == entries.lastIndex)
                val sizeModifier = entry.width?.let { Modifier.width(it) } ?: Modifier.weight(entry.weight)
                FilledTonalButton(
                    onClick = entry.onClick,
                    shapes = if (pressMorph) shapes else ButtonShapes(shapes.shape, shapes.shape),
                    modifier = sizeModifier.height(height),
                    colors = colors,
                    contentPadding = contentPadding,
                ) {
                    CompositionLocalProvider(LocalTextStyle provides textStyle) {
                        Row(
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

/** Material's connected button group shapes for a button at the start, middle, or end of a group. */
@Composable
internal fun connectedButtonShapes(isStart: Boolean, isEnd: Boolean): ButtonShapes =
    when {
        isStart && isEnd -> ButtonShapes(AppShapes.full, AppShapes.full)
        isStart ->
            ButtonShapes(
                ButtonGroupDefaults.connectedLeadingButtonShape,
                ButtonGroupDefaults.connectedLeadingButtonPressShape,
            )
        isEnd ->
            ButtonShapes(
                ButtonGroupDefaults.connectedTrailingButtonShape,
                ButtonGroupDefaults.connectedTrailingButtonPressShape,
            )
        else ->
            ButtonShapes(
                AppShapes.connected(DesignTokens.ConnectedGroupInnerCorner, DesignTokens.ConnectedGroupInnerCorner),
                ButtonGroupDefaults.connectedMiddleButtonPressShape,
            )
    }
