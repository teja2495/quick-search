package com.tk.quicksearch.search.searchEngines.inline

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.search.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.util.hapticConfirm

/**
 * Composable for displaying a single search engine icon item.
 * 
 * @param engine The search engine to display
 * @param query The search query to pass when clicked
 * @param iconSize The size of the icon
 * @param itemWidth The width of the clickable area
 * @param onSearchEngineClick Callback when the engine is clicked
 */
@Composable
fun SearchEngineIconItem(
    engine: SearchTarget,
    query: String,
    iconSize: Dp,
    itemWidth: Dp,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .width(itemWidth)
            .combinedClickable(
                onClick = {
                    hapticConfirm(view)()
                    onSearchEngineClick(query, engine)
                },
                onLongClick = onSearchEngineLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        SearchTargetIcon(
            target = engine,
            iconSize = iconSize,
            style = IconRenderStyle.ADVANCED
        )
    }
}

