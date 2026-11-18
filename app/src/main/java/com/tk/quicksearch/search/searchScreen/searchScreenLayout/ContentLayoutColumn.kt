package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.ItemPriorityConfig
import com.tk.quicksearch.widgetsPanel.HomeWidgetStack
import com.tk.quicksearch.widgetsPanel.rememberHomePinnedWidgets
import com.tk.quicksearch.widgetsPanel.rememberHomeWidgetHost

private val ContentItemSpacing = DesignTokens.SpacingMedium

/**
 * Lays out the search content in [layoutOrder], with top matches first (or last when
 * [isReversed]) and the deferred search history after the items. When Home widgets are pinned the
 * items go through [HomeWidgetStack], which interleaves the widgets.
 */
@Composable
internal fun ContentLayoutColumn(
    layoutOrder: List<ItemPriorityConfig.ItemType>,
    isReversed: Boolean,
    showTopMatchesSection: Boolean,
    showHomeWidgets: Boolean,
    isOverlayPresentation: Boolean,
    isScrollInProgress: () -> Boolean,
    modifier: Modifier,
    topMatches: @Composable () -> Unit,
    deferredSearchHistory: @Composable () -> Unit,
    item: @Composable (ItemPriorityConfig.ItemType) -> Unit,
) {
    val homeWidgets = rememberHomePinnedWidgets(enabled = !isOverlayPresentation)
    // The container only depends on whether any widget is pinned, so typing a query keeps the same
    // layout tree instead of rebuilding every section.
    val homeWidgetHost = if (homeWidgets.isNotEmpty()) rememberHomeWidgetHost() else null

    if (homeWidgetHost != null) {
        HomeWidgetStack(
            layoutOrder = layoutOrder,
            isReversed = isReversed,
            widgets = homeWidgets,
            showWidgets = showHomeWidgets,
            host = homeWidgetHost,
            spacing = ContentItemSpacing,
            isScrollInProgress = isScrollInProgress,
            modifier = modifier,
            leadingContent = {
                if (showTopMatchesSection && !isReversed) topMatches()
            },
            trailingContent = {
                deferredSearchHistory()
                if (showTopMatchesSection && isReversed) topMatches()
            },
            itemContent = item,
        )
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ContentItemSpacing)) {
        if (showTopMatchesSection && !isReversed) {
            topMatches()
        }
        layoutOrder.forEach { itemType -> item(itemType) }

        deferredSearchHistory()

        if (showTopMatchesSection && isReversed) {
            topMatches()
        }
    }
}
