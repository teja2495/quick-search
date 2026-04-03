package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.searchScreen.LocalOverlayActionColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.LocalAppTheme

private const val EXPAND_ICON_SIZE = 18

@Composable
internal fun ExpandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textResId: Int = R.string.action_expand_more,
) {
    val overlayActionColor = LocalOverlayActionColor.current
    val moreActionColor = expandCollapseActionContentColor(overlayActionColor)
    val moreTextStyle =
        if (LocalAppTheme.current == AppTheme.MONOCHROME) {
            MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
        } else {
            MaterialTheme.typography.bodySmall
        }

    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    ) {
        Text(
            text = stringResource(textResId),
            style = moreTextStyle,
            color = moreActionColor,
        )
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = stringResource(R.string.desc_expand),
            tint = moreActionColor,
            modifier = Modifier.size(EXPAND_ICON_SIZE.dp),
        )
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
            imageVector = Icons.Rounded.ExpandLess,
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
    AppColors.getSearchResultCardContainerColor(
        showWallpaperBackground,
        LocalOverlayResultCardColor.current,
    )

@Composable
private fun expandCollapseActionContentColor(overlayActionColor: Color?): Color =
    MaterialTheme.colorScheme.primary
