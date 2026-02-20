package com.tk.quicksearch.search.searchEngines.inline

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.searchEngines.getId
import com.tk.quicksearch.search.searchEngines.compact.SearchEngineCard
import com.tk.quicksearch.search.searchEngines.extendToScreenEdges
import com.tk.quicksearch.search.searchEngines.shared.SearchTargetConstants
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.util.isLandscape
import com.tk.quicksearch.util.isTablet

/** Constants for search engine section layout. */
private object SearchEngineSectionConstants {
    val ICON_SIZE = SearchTargetConstants.DEFAULT_ICON_SIZE
    val SPACING = 20.dp
    val SEARCH_ICON_SIZE = SearchTargetConstants.SEARCH_ICON_SIZE
    val HORIZONTAL_PADDING = SearchTargetConstants.HORIZONTAL_PADDING
    val VERTICAL_PADDING = SearchTargetConstants.VERTICAL_PADDING
    val SEARCH_ICON_SPACING = SearchTargetConstants.SEARCH_ICON_SPACING
}

/**
 * Composable section displaying search engine icons in a scrollable row.
 *
 * The section extends to screen edges by compensating for parent padding. Displays a fixed search
 * icon followed by scrollable engine icons.
 *
 * @param modifier Modifier for the section
 * @param query The current search query
 * @param hasAppResults Whether app results are displayed (unused but kept for API compatibility)
 * @param enabledEngines List of enabled search engines to display
 * @param onSearchEngineClick Callback when a search engine is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineIconsSection(
    modifier: Modifier = Modifier,
    query: String,
    hasAppResults: Boolean,
    enabledEngines: List<SearchTarget>,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    externalScrollState: androidx.compose.foundation.lazy.LazyListState? = null,
    detectedShortcutTarget: SearchTarget? = null,
    onClearDetectedShortcut: () -> Unit = {},
    showWallpaperBackground: Boolean = false,
    isOverlayPresentation: Boolean = false,
    predictedTarget: PredictedSubmitTarget? = null,
) {
    if (enabledEngines.isEmpty() && detectedShortcutTarget == null) return

    val scrollState = externalScrollState ?: rememberLazyListState()

    // Use black background in dark mode, otherwise use theme surface color
    // Add transparency only in dark mode to allow wallpaper background to show through
    val backgroundColor =
        if (MaterialTheme.colorScheme.surface == MaterialTheme.colorScheme.background &&
            MaterialTheme.colorScheme.background.red < 0.1f &&
            MaterialTheme.colorScheme.background.green < 0.1f &&
            MaterialTheme.colorScheme.background.blue < 0.1f
        ) {
            // Dark mode detected - use transparent black
            Color.Black.copy(alpha = 0.5f)
        } else {
            // Light mode - use opaque theme surface color
            MaterialTheme.colorScheme.surface
        }

    if (detectedShortcutTarget != null) {
        // Check if query starts with the shortcut and remove it
        // The shortcut corresponds to the detected engine
        Box(
            modifier =
                modifier.extendToScreenEdges().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SearchEngineCard(
                target = detectedShortcutTarget,
                query = query,
                onClick = { onSearchEngineClick(query, detectedShortcutTarget) },
                onLongClick = onSearchEngineLongPress,
                onClear = onClearDetectedShortcut,
                showWallpaperBackground = showWallpaperBackground,
                isPredicted =
                    (predictedTarget as? PredictedSubmitTarget.SearchTarget)?.targetId ==
                        detectedShortcutTarget.getId(),
            )
        }
    } else {
        Surface(
            modifier = modifier.extendToScreenEdges(),
            color = backgroundColor,
            shape =
                if (isOverlayPresentation) {
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp,
                    )
                } else {
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                },
        ) {
            SearchEngineContent(
                query = query,
                enabledEngines = enabledEngines,
                scrollState = scrollState,
                onSearchEngineClick = onSearchEngineClick,
                onSearchEngineLongPress = onSearchEngineLongPress,
                predictedTarget = predictedTarget,
            )
        }
    }
}

/** Internal composable for the search engine section content. */
@Composable
private fun SearchEngineContent(
    query: String,
    enabledEngines: List<SearchTarget>,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    predictedTarget: PredictedSubmitTarget?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SearchEngineSectionConstants.HORIZONTAL_PADDING,
                    vertical = SearchEngineSectionConstants.VERTICAL_PADDING,
                ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchIcon()

        Spacer(modifier = Modifier.width(SearchEngineSectionConstants.SEARCH_ICON_SPACING))

        ScrollableEngineIcons(
            query = query,
            enabledEngines = enabledEngines,
            scrollState = scrollState,
            onSearchEngineClick = onSearchEngineClick,
            onSearchEngineLongPress = onSearchEngineLongPress,
            predictedTarget = predictedTarget,
        )
    }
}

/** Fixed search icon displayed at the start of the section. */
@Composable
private fun SearchIcon() {
    Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = "Search",
        modifier = Modifier.size(SearchEngineSectionConstants.SEARCH_ICON_SIZE),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Scrollable row of search engine icons. Calculates item width to fit 6 items per visible row. */
@Composable
private fun ScrollableEngineIcons(
    query: String,
    enabledEngines: List<SearchTarget>,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    predictedTarget: PredictedSubmitTarget?,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val itemWidthDp = calculateItemWidth(maxWidth)

        LazyRow(
            state = scrollState,
            horizontalArrangement = Arrangement.spacedBy(SearchEngineSectionConstants.SPACING),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(enabledEngines) { engine ->
                SearchEngineIconItem(
                    engine = engine,
                    query = query,
                    iconSize = SearchEngineSectionConstants.ICON_SIZE,
                    itemWidth = itemWidthDp,
                    onSearchEngineClick = onSearchEngineClick,
                    onSearchEngineLongPress = onSearchEngineLongPress,
                    isPredicted =
                        (predictedTarget as? PredictedSubmitTarget.SearchTarget)?.targetId ==
                            engine.getId(),
                )
            }
        }
    }
}

/**
 * Calculates the width for each search engine icon item. Formula: (available width - total spacing)
 * / number of items
 */
@Composable
private fun calculateItemWidth(maxWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
    val itemsPerRow =
        when {
            isTablet() && isLandscape() -> 10
            isTablet() -> 8
            else -> 6
        }
    val totalSpacing = SearchEngineSectionConstants.SPACING * (itemsPerRow - 1)
    return (maxWidth - totalSpacing) / itemsPerRow
}
