package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R

@Composable
internal fun UpdateQuickSearchCard(
    showWallpaperBackground: Boolean,
    onClick: () -> Unit,
    onNotNowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuickSearchPromptCard(
        title = stringResource(R.string.update_card_title),
        action = stringResource(R.string.action_update),
        showWallpaperBackground = showWallpaperBackground,
        onClick = onClick,
        onNotNowClick = onNotNowClick,
        modifier = modifier,
    )
}
