package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.util.hapticConfirm
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A compact icon-over-label tile shown in the Shortcuts and Actions grids. */
data class ItemMenuTile(
    val label: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    /** Shown in a small dropdown on long press when [onLongClick] isn't set. */
    val longPressOption: ItemMenuLongPressOption? = null,
    /** Content anchored to the tile, such as a small dropdown shown on long press. */
    val anchoredContent: (@Composable () -> Unit)? = null,
)

/**
 * A full-width row with a leading icon, label, optional trailing value and a chevron. Also used for
 * the side-by-side button rows, where only the icon, label, long click, [destructive] style and
 * [anchoredContent] apply.
 */
data class ItemMenuRow(
    val label: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    /** Runs only after this button remains continuously pressed for [longHoldDurationMillis]. */
    val onLongHold: (() -> Unit)? = null,
    val longHoldDurationMillis: Long = 0L,
    val trailingText: String? = null,
    val destructive: Boolean = false,
    /** List rows only: shown in a small dropdown on long press when [onLongClick] isn't set. */
    val longPressOption: ItemMenuLongPressOption? = null,
    /** Content anchored to a side-by-side button, such as a small dropdown shown on long press. */
    val anchoredContent: (@Composable () -> Unit)? = null,
    /**
     * Side-by-side buttons only: shows just the icon in a compact square box, with [label] used as
     * its accessibility description.
     */
    val iconOnly: Boolean = false,
)

/** A single option offered in a small dropdown when a tile, row or button is long pressed. */
data class ItemMenuLongPressOption(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/** A long-press "Remove" option, or null when there's nothing to remove. */
@Composable
fun itemMenuRemoveOption(onRemove: (() -> Unit)?): ItemMenuLongPressOption? {
    val label = stringResource(R.string.action_remove)
    return onRemove?.let { ItemMenuLongPressOption(label = label, icon = MaterialSymbols.Close, onClick = it) }
}

private const val ItemMenuGridColumns = 4

/**
 * Long-press menu shared by apps, app shortcuts and files.
 *
 * Layout, top to bottom: an optional Shortcuts grid, an Actions grid of quick one-tap actions,
 * then [buttonRows] of side-by-side buttons (e.g. Swipe up and Swipe down), a list of settings-like [rows],
 * then [footer] buttons sharing one row (e.g. App info and Uninstall), and finally a small centered [caption]. Empty groups are skipped, and grid titles are only shown when both grids are
 * present. A grid that fits in one row spreads its tiles across the full width, and labels that don't fit scroll as a marquee.
 */
@Composable
fun ItemMenuPopup(
    onDismiss: () -> Unit,
    title: @Composable () -> Unit,
    shortcutsTitle: String,
    actionsTitle: String,
    actions: List<ItemMenuTile>,
    rows: List<ItemMenuRow>,
    leadingContent: (@Composable () -> Unit)? = null,
    shortcuts: List<ItemMenuTile> = emptyList(),
    buttonRows: List<List<ItemMenuRow>> = emptyList(),
    footer: List<ItemMenuRow> = emptyList(),
    caption: (@Composable () -> Unit)? = null,
) {
    val dialogBackground = AppColors.DialogBackground
    AppBottomPopup(
        onDismiss = onDismiss,
        title = title,
        leadingContent = leadingContent,
        containerColor = dialogBackground,
        contentCardColor = dialogBackground,
        // Matches the popup's 24dp top padding so the header sits evenly between the edge and the menu.
        contentSpacing = DesignTokens.SpacingXXLarge,
        headerSpacing = DesignTokens.SpacingMedium,
        contentTopPadding = 0.dp,
        // The popup's own bottom padding already separates the last item from the edge.
        contentBottomPadding = 0.dp,
        // Lets the tiles and rows use more of the popup's width.
        contentHorizontalPadding = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val showGridTitles = shortcuts.isNotEmpty() && actions.isNotEmpty()
            if (shortcuts.isNotEmpty()) {
                if (showGridTitles) ItemMenuSectionTitle(shortcutsTitle)
                ItemMenuTileGrid(tiles = shortcuts)
                if (actions.isNotEmpty()) Spacer(Modifier.height(DesignTokens.SpacingLarge))
            }
            if (actions.isNotEmpty()) {
                if (showGridTitles) ItemMenuSectionTitle(actionsTitle)
                ItemMenuTileGrid(tiles = actions)
            }
            val hasGrids = shortcuts.isNotEmpty() || actions.isNotEmpty()
            val visibleButtonRows = buttonRows.filter { it.isNotEmpty() }
            if (visibleButtonRows.isNotEmpty()) {
                if (hasGrids) Spacer(Modifier.height(DesignTokens.SpacingMedium))
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
                    visibleButtonRows.forEach { buttons -> ItemMenuButtonRow(buttons) }
                }
            }
            if (rows.isNotEmpty()) {
                if (visibleButtonRows.isNotEmpty()) {
                    Spacer(Modifier.height(DesignTokens.SpacingSmall))
                } else if (hasGrids) {
                    Spacer(Modifier.height(DesignTokens.SpacingMedium))
                }
                rows.forEach { row -> ItemMenuListRow(row) }
            }
            if (footer.isNotEmpty()) {
                Spacer(Modifier.height(DesignTokens.SpacingMedium))
                ItemMenuButtonRow(footer)
            }
            if (caption != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Matches the popup's own 24dp bottom padding so the caption sits evenly between the footer and the edge.
                        .padding(top = DesignTokens.SpacingXXLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    caption()
                }
            }
        }
    }
}

/** A single-line label that scrolls when it doesn't fit; [basicMarquee] stays still when it does. */
@Composable
private fun ItemMenuMarqueeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    /** Changing this restarts the marquee from the beginning. */
    replayKey: Int = 0,
) {
    key(replayKey) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = textAlign,
            modifier = modifier.basicMarquee(),
        )
    }
}

/** Small dropdown anchored to its parent, shown on long press of an item with a [ItemMenuLongPressOption]. */
@Composable
fun ItemMenuLongPressDropdown(
    option: ItemMenuLongPressOption,
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = 0.dp, y = 8.dp),
        shape = RoundedCornerShape(24.dp),
        properties = PopupProperties(focusable = false),
        containerColor = if (LocalAppIsDarkTheme.current) Color.Black else Color.White,
    ) {
        DropdownMenuItem(
            text = { Text(text = option.label) },
            leadingIcon = { Icon(imageVector = option.icon, contentDescription = null) },
            onClick = {
                onDismiss()
                option.onClick()
            },
        )
    }
}

/** Long click for an item: its own [onLongClick], else opening its long-press option, else none. */
private fun resolveLongClick(
    onLongClick: (() -> Unit)?,
    option: ItemMenuLongPressOption?,
    showOption: () -> Unit,
): (() -> Unit)? = onLongClick ?: option?.let { showOption }

@Composable
private fun ItemMenuSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = DesignTokens.SpacingSmall),
    )
}

@Composable
private fun ItemMenuTileGrid(tiles: List<ItemMenuTile>) {
    // A single short row spreads evenly across the full width; multi-row grids stay aligned.
    val spreadAcrossRow = tiles.size <= ItemMenuGridColumns
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
        tiles.chunked(ItemMenuGridColumns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                row.forEach { tile ->
                    ItemMenuTileButton(tile = tile, modifier = Modifier.weight(1f))
                }
                if (!spreadAcrossRow) {
                    repeat(ItemMenuGridColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ItemMenuTileButton(
    tile: ItemMenuTile,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var showLongPressOption by remember { mutableStateOf(false) }
    var marqueeReplayKey by remember { mutableIntStateOf(0) }
    val onLongClick = resolveLongClick(tile.onLongClick, tile.longPressOption) { showLongPressOption = true }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DesignTokens.ShapeSmall)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .combinedClickable(
                    onClick = tile.onClick,
                    onLongClick = onLongClick?.let { onLongClick ->
                        {
                            hapticConfirm(view)()
                            marqueeReplayKey++
                            onLongClick()
                        }
                    },
                )
                .padding(vertical = DesignTokens.SpacingMedium, horizontal = DesignTokens.SpacingXSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalContentColor provides AppColors.DialogText) {
                    tile.icon()
                }
            }
            ItemMenuMarqueeText(
                text = tile.label,
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.DialogText,
                textAlign = TextAlign.Center,
                replayKey = marqueeReplayKey,
            )
        }
        // Matches the tile's bounds so anchored popups position against it.
        Box(modifier = Modifier.matchParentSize()) {
            tile.anchoredContent?.invoke()
            tile.longPressOption?.let { option ->
                ItemMenuLongPressDropdown(option, showLongPressOption) { showLongPressOption = false }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemMenuListRow(row: ItemMenuRow) {
    val view = LocalView.current
    val contentColor =
        if (row.destructive) MaterialTheme.colorScheme.error else AppColors.DialogText
    var showLongPressOption by remember { mutableStateOf(false) }
    var marqueeReplayKey by remember { mutableIntStateOf(0) }
    val onLongClick = resolveLongClick(row.onLongClick, row.longPressOption) { showLongPressOption = true }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(DesignTokens.ShapeSmall)
                .combinedClickable(
                    onClick = row.onClick,
                    onLongClick = onLongClick?.let { onLongClick ->
                        {
                            hapticConfirm(view)()
                            marqueeReplayKey++
                            onLongClick()
                        }
                    },
                )
                .padding(vertical = DesignTokens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
            Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    row.icon()
                }
            }
            ItemMenuMarqueeText(
                text = row.label,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                modifier = Modifier.weight(1f),
                replayKey = marqueeReplayKey,
            )
            row.trailingText?.let { value ->
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!row.destructive) {
                Icon(
                    imageVector = MaterialSymbols.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        row.longPressOption?.let { option ->
            // Matches the row's bounds so the dropdown positions against it.
            Box(modifier = Modifier.matchParentSize()) {
                ItemMenuLongPressDropdown(option, showLongPressOption) { showLongPressOption = false }
            }
        }
    }
}

@Composable
private fun ItemMenuButtonRow(buttons: List<ItemMenuRow>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        buttons.forEach { button ->
            ItemMenuButton(
                button = button,
                modifier = if (button.iconOnly) Modifier.width(56.dp) else Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemMenuButton(
    button: ItemMenuRow,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val contentColor =
        if (button.destructive) MaterialTheme.colorScheme.error else AppColors.DialogText
    val containerColor =
        if (button.destructive) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        }
    var marqueeReplayKey by remember { mutableIntStateOf(0) }
    val longHoldInteractionSource = remember { MutableInteractionSource() }
    val longHoldIndication = LocalIndication.current
    val coroutineScope = rememberCoroutineScope()
    val clickModifier =
        if (button.onLongHold != null && button.longHoldDurationMillis > 0L) {
            Modifier
                .indication(longHoldInteractionSource, longHoldIndication)
                .semantics {
                    onClick {
                        button.onClick()
                        true
                    }
                }
                .pointerInput(button.onClick, button.onLongHold, button.longHoldDurationMillis) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val press = PressInteraction.Press(down.position)
                        longHoldInteractionSource.tryEmit(press)
                        var longHoldTriggered = false
                        val longHoldJob = coroutineScope.launch {
                            delay(button.longHoldDurationMillis)
                            longHoldTriggered = true
                            longHoldInteractionSource.tryEmit(PressInteraction.Release(press))
                            hapticConfirm(view)()
                            button.onLongHold.invoke()
                        }
                        val up = waitForUpOrCancellation()
                        longHoldJob.cancel()
                        if (!longHoldTriggered) {
                            longHoldInteractionSource.tryEmit(
                                if (up != null) PressInteraction.Release(press)
                                else PressInteraction.Cancel(press),
                            )
                            if (up != null) button.onClick()
                        }
                    }
                }
        } else {
            Modifier.combinedClickable(
                onClick = button.onClick,
                onLongClick = button.onLongClick?.let { onLongClick ->
                    {
                        hapticConfirm(view)()
                        marqueeReplayKey++
                        onLongClick()
                    }
                },
            )
        }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(DesignTokens.ShapeSmall)
                .background(containerColor)
                .then(clickModifier)
                .then(if (button.iconOnly) Modifier.semantics { contentDescription = button.label } else Modifier)
                .padding(horizontal = DesignTokens.SpacingMedium, vertical = DesignTokens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall, Alignment.CenterHorizontally),
        ) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    button.icon()
                }
            }
            if (!button.iconOnly) {
                ItemMenuMarqueeText(
                    text = button.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    modifier = Modifier.weight(1f, fill = false),
                    replayKey = marqueeReplayKey,
                )
            }
        }
        button.anchoredContent?.let { content ->
            // Matches the button's bounds so anchored popups position and size against it.
            Box(modifier = Modifier.matchParentSize()) { content() }
        }
    }
}
