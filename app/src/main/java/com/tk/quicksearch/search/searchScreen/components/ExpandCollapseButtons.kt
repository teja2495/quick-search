package com.tk.quicksearch.search.searchScreen.components

import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardDefaults
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.search.searchScreen.shared.ResultSegment
import com.tk.quicksearch.search.searchScreen.shared.compactEdgeToEdge
import com.tk.quicksearch.settings.shared.settingsFloatingActionShape
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.LocalAppTheme

private const val EXPAND_ICON_SIZE = 18
private const val COMPACT_EXPAND_ICON_SIZE = 14
private const val COMPACT_EXPAND_BUTTON_HEIGHT = 36
private val EXPAND_SEGMENT_MIN_HEIGHT = 40.dp
private val COLLAPSE_BUTTON_HEIGHT = 48.dp

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
 * is the button, like Clear History. Only call it while [segmentedRowsActive]; card and flat
 * sections keep their own More layouts.
 */
@Composable
internal fun ExpandButtonSegment(
    index: Int,
    count: Int,
    onClick: () -> Unit,
    textResId: Int = R.string.action_expand_more,
) {
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
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
    ) {
        ExpandCollapseButtonContent(
            text = stringResource(textResId),
            icon = icon,
            contentColor = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CompactExpandButton(
    onClick: () -> Unit,
    modifier: Modifier,
    textResId: Int,
    icon: ImageVector,
) {
    val contentColor = MaterialTheme.colorScheme.primary
    // Compact drops the 40dp minimum and 48dp touch-target padding so the button is a short row.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        TextButton(
            onClick = onClick,
            modifier = modifier.height(COMPACT_EXPAND_BUTTON_HEIGHT.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        ) {
            Text(
                text = stringResource(textResId),
                style = monochromeAwareStyle(MaterialTheme.typography.labelSmall),
                color = contentColor,
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(COMPACT_EXPAND_ICON_SIZE.dp),
            )
        }
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
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier.height(COLLAPSE_BUTTON_HEIGHT),
        shape = settingsFloatingActionShape(interactionSource),
        color =
            AppColors.getSearchResultCardContainerColor(
                showWallpaperBackground,
                LocalOverlayResultCardColor.current,
            ),
        contentColor = contentColor,
        interactionSource = interactionSource,
    ) {
        ExpandCollapseButtonContent(
            text = stringResource(R.string.desc_collapse),
            icon = MaterialSymbols.ExpandLess,
            contentColor = contentColor,
            modifier = Modifier.padding(start = 16.dp, end = 20.dp),
        )
    }
}

/** Leading icon and `labelLarge` label shared by the More and Collapse pills. */
@Composable
private fun ExpandCollapseButtonContent(
    text: String,
    icon: ImageVector,
    contentColor: Color,
    modifier: Modifier = Modifier,
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
            modifier = Modifier.size(EXPAND_ICON_SIZE.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = monochromeAwareStyle(MaterialTheme.typography.labelLarge),
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun monochromeAwareStyle(style: TextStyle): TextStyle =
    if (LocalAppTheme.current == AppTheme.MONOCHROME) style.copy(fontWeight = FontWeight.SemiBold) else style
