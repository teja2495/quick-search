package com.tk.quicksearch.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEnginesSection(
    modifier: Modifier = Modifier,
    query: String,
    hasAppResults: Boolean,
    enabledEngines: List<SearchEngine>,
    onSearchEngineClick: (String, SearchEngine) -> Unit
) {
    if (enabledEngines.isEmpty()) return

    val scrollState = rememberLazyListState()
    val density = LocalDensity.current

    Surface(
        modifier = modifier
            .layout { measurable, constraints ->
                // Parent has 20dp padding on each side
                // Extend width by 40dp (20dp on each side) to reach screen edges
                val parentPadding = with(density) { 20.dp.roundToPx() }
                val extendedWidth = constraints.maxWidth + (parentPadding * 2)
                val extendedConstraints = constraints.copy(
                    minWidth = extendedWidth,
                    maxWidth = extendedWidth
                )
                val placeable = measurable.measure(extendedConstraints)
                layout(
                    width = constraints.maxWidth, // Report parent width to avoid overflow
                    height = placeable.height
                ) {
                    // Place 20dp to the left so it extends to screen edges
                    placeable.placeRelative(x = -parentPadding, y = 0)
                }
            },
        color = Color.Black,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed search icon
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // Scrollable search engine icons with pagination (6 items at a time)
            BoxWithConstraints(
                modifier = Modifier.weight(1f)
            ) {
                val iconSize = 24.dp
                val spacing = 20.dp
                // Calculate item width: (available width - 5 spacings) / 6 items
                // maxWidth is already in Dp, so we can do arithmetic directly
                val totalSpacing = spacing * 5 // 5 spacings between 6 items
                val itemWidthDp = (maxWidth - totalSpacing) / 6 // 6 items with 5 spacings
                
                LazyRow(
                    state = scrollState,
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(enabledEngines) { index, engine ->
                        val drawableId = when (engine) {
                            SearchEngine.GOOGLE -> R.drawable.google
                            SearchEngine.CHATGPT -> R.drawable.chatgpt
                            SearchEngine.PERPLEXITY -> R.drawable.perplexity
                            SearchEngine.GROK -> R.drawable.grok
                            SearchEngine.GOOGLE_MAPS -> R.drawable.google_maps
                            SearchEngine.GOOGLE_PLAY -> R.drawable.google_play
                            SearchEngine.REDDIT -> R.drawable.reddit
                            SearchEngine.YOUTUBE -> R.drawable.youtube
                            SearchEngine.AMAZON -> R.drawable.amazon
                            SearchEngine.AI_MODE -> R.drawable.ai_mode
                        }
                        
                        val contentDescription = when (engine) {
                            SearchEngine.GOOGLE -> "Google"
                            SearchEngine.CHATGPT -> "ChatGPT"
                            SearchEngine.PERPLEXITY -> "Perplexity"
                            SearchEngine.GROK -> "Grok"
                            SearchEngine.GOOGLE_MAPS -> "Google Maps"
                            SearchEngine.GOOGLE_PLAY -> "Google Play"
                            SearchEngine.REDDIT -> "Reddit"
                            SearchEngine.YOUTUBE -> "YouTube"
                            SearchEngine.AMAZON -> "Amazon"
                            SearchEngine.AI_MODE -> "AI mode"
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(itemWidthDp)
                                .clickable {
                                    onSearchEngineClick(query, engine)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = drawableId),
                                contentDescription = contentDescription,
                                modifier = Modifier
                                    .size(iconSize),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }
}

