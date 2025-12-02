package com.tk.quicksearch.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    engine: SearchEngine,
    query: String,
    iconSize: Dp,
    itemWidth: Dp,
    onSearchEngineClick: (String, SearchEngine) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(itemWidth)
            .clickable {
                onSearchEngineClick(query, engine)
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = engine.getDrawableResId()),
            contentDescription = engine.getContentDescription(),
            modifier = Modifier.size(iconSize),
            contentScale = ContentScale.Fit
        )
    }
}
