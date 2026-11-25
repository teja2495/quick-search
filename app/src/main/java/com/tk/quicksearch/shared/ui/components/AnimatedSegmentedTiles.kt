package com.tk.quicksearch.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Enter transition for a settings row or group that appears because another setting changed. */
val SettingsRevealEnter: EnterTransition =
    expandVertically(animationSpec = AppMotion.defaultSpatial()) +
        fadeIn(animationSpec = tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing))

/** Exit transition for a settings row or group that hides because another setting changed. */
val SettingsRevealExit: ExitTransition =
    shrinkVertically(animationSpec = AppMotion.defaultSpatial()) +
        fadeOut(animationSpec = tween(AppMotion.DurationExit, easing = AppMotion.EmphasizedAccelerateEasing))

/**
 * Shows or hides settings content that depends on another setting, expanding and fading it in or
 * shrinking it out. Content that is visible on first composition shows without animating.
 */
@Composable
fun ColumnScope.SettingsRevealVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = SettingsRevealEnter,
        exit = SettingsRevealExit,
    ) {
        content()
    }
}

/** Declares the tiles of an [AnimatedSegmentedTiles] list, in order. */
class AnimatedSegmentedTilesScope internal constructor() {
    internal class Entry(
        val key: Any?,
        val visible: Boolean,
        val content: @Composable (index: Int, count: Int) -> Unit,
    )

    internal val entries = mutableListOf<Entry>()

    /**
     * A tile that draws its own segmented tile (for example [SegmentedTile], a choice tile, or a
     * navigation row) at the given position. Declare it every time and pass [visible] instead of
     * leaving it out, so it can animate. [key] defaults to the declaration position; pass one when
     * tiles are declared from a list that can change.
     */
    fun item(
        visible: Boolean = true,
        key: Any? = null,
        content: @Composable (index: Int, count: Int) -> Unit,
    ) {
        entries += Entry(key, visible, content)
    }

    /** A plain [SegmentedTile] holding [content]. See [item]. */
    fun tile(
        visible: Boolean = true,
        key: Any? = null,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        content: @Composable () -> Unit,
    ) {
        item(visible, key) { index, count ->
            SegmentedTile(index = index, count = count, modifier = modifier, onClick = onClick, content = content)
        }
    }
}

/**
 * Segmented tiles inside a [SegmentedGroup] (or any column with the segmented gap) where some tiles
 * show only while another setting allows them. A tile that becomes visible expands in and one that
 * hides shrinks out, keeping its shape while it leaves; neighbors morph their corners to the new
 * first and last positions. Positions start at [firstIndex] and [trailingCount] tiles that follow
 * outside this list count toward the group size.
 */
@Composable
fun ColumnScope.AnimatedSegmentedTiles(
    firstIndex: Int = 0,
    trailingCount: Int = 0,
    content: AnimatedSegmentedTilesScope.() -> Unit,
) {
    val entries = AnimatedSegmentedTilesScope().apply(content).entries
    val count = firstIndex + entries.count { it.visible } + trailingCount
    var visibleIndex = firstIndex
    // Each tile carries the segmented gap above it inside its own reveal, so a hiding tile takes
    // its gap with it as it shrinks. Left to the parent's spacedBy, the gap would stay until the
    // exit fully settles and then drop in one jump. The block hides its first gap above its top,
    // where the parent's own gap (or nothing, when it leads the group) already is.
    Column(
        modifier =
            Modifier.fillMaxWidth().layout { measurable, constraints ->
                val gap = DesignTokens.SegmentedListGap.roundToPx()
                val placeable = measurable.measure(constraints.copy(minHeight = 0))
                val height = (placeable.height - gap).coerceAtLeast(0)
                layout(placeable.width, height) { placeable.place(0, -gap) }
            },
    ) {
        entries.forEachIndexed { position, entry ->
            val index = if (entry.visible) visibleIndex++ else visibleIndex
            key(entry.key ?: position) {
                AnimatedSegmentedTile(entry.visible, index, count, entry.content)
            }
        }
    }
}

@Composable
private fun ColumnScope.AnimatedSegmentedTile(
    visible: Boolean,
    index: Int,
    count: Int,
    content: @Composable (index: Int, count: Int) -> Unit,
) {
    // While shrinking out, a hidden tile keeps the position it last showed at.
    val shown = remember { IntArray(2).also { it[0] = index; it[1] = count } }
    if (visible) {
        shown[0] = index
        shown[1] = count
    }
    SettingsRevealVisibility(visible = visible) {
        Column(modifier = Modifier.padding(top = DesignTokens.SegmentedListGap)) {
            if (visible) content(index, count) else content(shown[0], shown[1])
        }
    }
}
