package com.tk.quicksearch.search.searchScreen.components

import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.frostedCardBackground
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.search.searchScreen.shared.ResultSegment
import com.tk.quicksearch.search.searchScreen.shared.compactEdgeToEdge
import com.tk.quicksearch.search.searchScreen.shared.segmentedRowsActive
import com.tk.quicksearch.shared.ui.theme.floatingActionShape
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.components.AppTextButton

private const val EXPAND_ICON_SIZE = 18
private val COMPACT_EXPAND_BUTTON_MIN_HEIGHT = 48.dp
private val EXPAND_SEGMENT_MIN_HEIGHT = 40.dp
private val COLLAPSE_BUTTON_HEIGHT = 48.dp

// Between the M3 extra-small (32dp) and small (40dp) buttons; the Surface still keeps a 48dp
// touch target around it.
private val COMPACT_SECTION_ACTION_PILL_HEIGHT = 36.dp
private val COMPACT_SECTION_ACTION_ICON_SIZE = 16.dp

/**
 * Divider between a section's last row and its More button. Only flat rows (card layout off) get
 * one; card sections end at the card edge instead.
 */
@Composable
internal fun ExpandButtonDivider(showWallpaperBackground: Boolean) {
    if (LocalSearchCardLayoutEnabled.current) return
    HorizontalDivider(
        modifier = Modifier.compactEdgeToEdge().fillMaxWidth(),
        color = SearchResultCardDefaults.dividerColor(showWallpaperBackground),
    )
}

/**
 * A section's More button as the last tile of a segmented list ([index] of [count]): the whole tile
 * is the button, like Clear History. Outside segmented lists it falls back to [ExpandButton].
 */
@Composable
internal fun ExpandButtonSegment(
    index: Int,
    count: Int,
    onClick: () -> Unit,
    textResId: Int = R.string.action_expand_more,
) {
    if (!segmentedRowsActive()) {
        ExpandButton(onClick = onClick, textResId = textResId)
        return
    }
    ResultSegment(index = index, count = count, onClick = onClick) {
        ExpandCollapseButtonContent(
            text = stringResource(textResId),
            icon = MaterialSymbols.ExpandMore,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().heightIn(min = EXPAND_SEGMENT_MIN_HEIGHT),
        )
    }
}

@Composable
internal fun ExpandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textResId: Int = R.string.action_expand_more,
    icon: ImageVector = MaterialSymbols.ExpandMore,
    compact: Boolean = false,
) {
    // Flat sections (card layout off) always use the short text-only More button.
    if (compact || !LocalSearchCardLayoutEnabled.current) {
        CompactExpandButton(onClick = onClick, modifier = modifier, textResId = textResId, icon = icon)
        return
    }
    AppTextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
    ) {
        ExpandCollapseButtonContent(
            text = stringResource(textResId),
            icon = icon,
            contentColor = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Flat (Compact Layout) More button: an M3 list row spanning the screen like the rows above it, with
 * the same leading icon and `labelLarge` label as the card layout's More tile.
 */
@Composable
private fun CompactExpandButton(
    onClick: () -> Unit,
    modifier: Modifier,
    textResId: Int,
    icon: ImageVector,
) {
    val contentColor = MaterialTheme.colorScheme.primary
    Box(
        modifier =
            modifier
                .compactEdgeToEdge()
                .fillMaxWidth()
                .heightIn(min = COMPACT_EXPAND_BUTTON_MIN_HEIGHT)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ExpandCollapseButtonContent(
            text = stringResource(textResId),
            icon = icon,
            contentColor = contentColor,
        )
    }
}

/**
 * Floating Collapse action over an expanded section: a pill on the result tiles' own surface (so it
 * reads as part of the list on wallpaper, AMOLED, and Material You) that squares its corners while
 * pressed.
 */
@Composable
internal fun CollapseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean = false,
) = SectionActionPill(
    text = stringResource(R.string.desc_collapse),
    icon = MaterialSymbols.ExpandLess,
    onClick = onClick,
    modifier = modifier,
    showWallpaperBackground = showWallpaperBackground,
)

/**
 * The Collapse button's pill, for other section-level actions that should match it (such as All
 * Apps): frosted result-tile surface, primary icon and `labelLarge` label, corners that square
 * while pressed.
 *
 * @param compact Uses a 36dp height, a 16dp icon, and tighter padding, for actions that sit under
 *   dense content (All Apps under the app grid).
 */
@Composable
internal fun SectionActionPill(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean = false,
    compact: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = MaterialTheme.colorScheme.primary
    val shape = floatingActionShape(interactionSource)
    val containerColor = SearchResultCardDefaults.containerColor(showWallpaperBackground)
    Surface(
        onClick = onClick,
        modifier = modifier.height(if (compact) COMPACT_SECTION_ACTION_PILL_HEIGHT else COLLAPSE_BUTTON_HEIGHT),
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
        interactionSource = interactionSource,
    ) {
        // Frosted like the result cards it floats over, with their lighter frosted scrim. Drawn
        // inside the Surface so it matches the visible pill: on the Surface's own modifier it
        // would take the 48dp touch-target size and show as a halo around the compact pill.
        Box(
            modifier = Modifier.frostedCardBackground(shape).background(containerColor, shape),
            contentAlignment = Alignment.Center,
        ) {
            ExpandCollapseButtonContent(
                text = text,
                icon = icon,
                contentColor = contentColor,
                iconSize = if (compact) COMPACT_SECTION_ACTION_ICON_SIZE else EXPAND_ICON_SIZE.dp,
                modifier =
                    if (compact) {
                        Modifier.padding(start = DesignTokens.SpacingMedium, end = DesignTokens.SpacingLarge)
                    } else {
                        Modifier.padding(start = DesignTokens.SpacingLarge, end = DesignTokens.SpacingXLarge)
                    },
            )
        }
    }
}

/** Leading icon and `labelLarge` label shared by the More and Collapse pills. */
@Composable
private fun ExpandCollapseButtonContent(
    text: String,
    icon: ImageVector,
    contentColor: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = EXPAND_ICON_SIZE.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
        )
    }
}
