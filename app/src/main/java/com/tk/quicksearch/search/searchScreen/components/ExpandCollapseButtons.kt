package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import com.tk.quicksearch.search.searchScreen.shared.compactEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import com.tk.quicksearch.search.searchScreen.shared.ResultSegment
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.searchScreen.LocalOverlayActionColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.LocalAppTheme

private const val EXPAND_ICON_SIZE = 18
private const val COMPACT_EXPAND_ICON_SIZE = 14
private const val COMPACT_EXPAND_BUTTON_HEIGHT = 36

/**
 * Divider between a section's last row and its More button. Only flat rows (card layout off) get
 * one; card sections end at the card edge instead.
 */
@Composable
internal fun ExpandButtonDivider(showWallpaperBackground: Boolean) {
    if (LocalSearchCardLayoutEnabled.current) return
    HorizontalDivider(
        modifier = Modifier.compactEdgeToEdge().fillMaxWidth(),
        color = LocalOverlayDividerColor.current
            ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * A section's More button as the last tile of a segmented list ([index] of [count]). Only call it
 * while [segmentedRowsActive]; card and flat sections keep their own More layouts.
 */
@Composable
internal fun ExpandButtonSegment(
    index: Int,
    count: Int,
    onClick: () -> Unit,
    textResId: Int = R.string.action_expand_more,
) {
    ResultSegment(index = index, count = count) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ExpandButton(onClick = onClick, textResId = textResId)
        }
    }
}

@Composable
internal fun ExpandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textResId: Int = R.string.action_expand_more,
    usePillBackground: Boolean = false,
    showWallpaperBackground: Boolean = false,
    icon: ImageVector = MaterialSymbols.ExpandMore,
    compact: Boolean = false,
) {
    // Flat sections (card layout off) always use the short More button.
    val compact = compact || !LocalSearchCardLayoutEnabled.current
    val overlayActionColor = LocalOverlayActionColor.current
    val moreActionColor = expandCollapseActionContentColor(overlayActionColor)
    val cardContainerColor = resultCardContainerColor(showWallpaperBackground)
    val baseTextStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    val moreTextStyle =
        if (LocalAppTheme.current == AppTheme.MONOCHROME) {
            baseTextStyle.copy(fontWeight = FontWeight.SemiBold)
        } else {
            baseTextStyle
        }
    val iconSize = if (compact) COMPACT_EXPAND_ICON_SIZE else EXPAND_ICON_SIZE

    if (usePillBackground) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 56.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.Transparent),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = cardContainerColor,
                    contentColor = moreActionColor,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(textResId),
                style = moreTextStyle,
                color = moreActionColor,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.desc_expand),
                tint = moreActionColor,
                modifier = Modifier.size(iconSize.dp),
            )
        }
    } else {
        // Compact drops the 40dp minimum and 48dp touch-target padding so the button is a short row.
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides
                if (compact) Dp.Unspecified else LocalMinimumInteractiveComponentSize.current,
        ) {
            TextButton(
                onClick = onClick,
                modifier = if (compact) modifier.height(COMPACT_EXPAND_BUTTON_HEIGHT.dp) else modifier,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text(
                    text = stringResource(textResId),
                    style = moreTextStyle,
                    color = moreActionColor,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.desc_expand),
                    tint = moreActionColor,
                    modifier = Modifier.size(iconSize.dp),
                )
            }
        }
    }
}

@Composable
internal fun CollapseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean = false,
) {
    val overlayActionColor = LocalOverlayActionColor.current
    val collapseContentColor = expandCollapseActionContentColor(overlayActionColor)
    val isLightWithWallpaper = showWallpaperBackground && !LocalAppIsDarkTheme.current
    val collapseBorderColor = if (isLightWithWallpaper) Color.Transparent else collapseContentColor.copy(alpha = 0.5f)
    val cardContainerColor = resultCardContainerColor(showWallpaperBackground)
    val collapseTextStyle =
        if (LocalAppTheme.current == AppTheme.MONOCHROME) {
            MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
        } else {
            MaterialTheme.typography.bodySmall
        }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(1.dp, collapseBorderColor),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = cardContainerColor,
                contentColor = collapseContentColor,
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = MaterialSymbols.ExpandLess,
            contentDescription = stringResource(R.string.desc_collapse),
            tint = collapseContentColor,
            modifier = Modifier.size(EXPAND_ICON_SIZE.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.desc_collapse),
            style = collapseTextStyle,
            color = collapseContentColor,
        )
    }
}

@Composable
private fun resultCardContainerColor(showWallpaperBackground: Boolean): Color =
    if (LocalSearchCardLayoutEnabled.current) {
        AppColors.getSearchResultCardContainerColor(
            showWallpaperBackground,
            LocalOverlayResultCardColor.current,
        )
    } else {
        Color.Transparent
    }

@Composable
private fun expandCollapseActionContentColor(overlayActionColor: Color?): Color =
    MaterialTheme.colorScheme.primary
