package com.tk.quicksearch.searchEngines.inline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.compact.SearchEngineCard
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.searchEngines.shared.SearchTargetConstants
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.shared.util.isLandscape
import com.tk.quicksearch.shared.util.isTablet

/** Constants for search engine section layout. */
private object SearchEngineSectionConstants {
    val ICON_SIZE = SearchTargetConstants.DEFAULT_ICON_SIZE
    val SPACING = 20.dp
    val ROW_SPACING = 10.dp
    val PREDICTION_HIGHLIGHT_HEIGHT_EXTRA = 12.dp
    val PREDICTION_HIGHLIGHT_WIDTH_EXTRA = 8.dp
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
    hasBottomSearchBar: Boolean = false,
    showOverlayExpandChevron: Boolean = false,
    onOverlayExpandClick: (() -> Unit)? = null,
    isOverlayExpanded: Boolean = false,
    compactRowCount: Int = 1,
    predictedTarget: PredictedSubmitTarget? = null,
) {
    if (enabledEngines.isEmpty() && detectedShortcutTarget == null) return

    val scrollState = externalScrollState ?: rememberLazyListState()

    // Match compact section background with the persistent search bar for visual consistency.
    val backgroundColor = Color.Black.copy(alpha = 0.5f)

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
                if (isOverlayPresentation && !hasBottomSearchBar) {
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
                showOverlayExpandChevron = showOverlayExpandChevron,
                onOverlayExpandClick = onOverlayExpandClick,
                isOverlayExpanded = isOverlayExpanded,
                compactRowCount = compactRowCount,
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
    showOverlayExpandChevron: Boolean,
    onOverlayExpandClick: (() -> Unit)?,
    isOverlayExpanded: Boolean,
    compactRowCount: Int,
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
        SearchIcon(
            showOverlayExpandChevron = showOverlayExpandChevron,
            onOverlayExpandClick = onOverlayExpandClick,
            isOverlayExpanded = isOverlayExpanded,
        )

        Spacer(modifier = Modifier.width(SearchEngineSectionConstants.SEARCH_ICON_SPACING))

        ScrollableEngineIcons(
            query = query,
            enabledEngines = enabledEngines,
            scrollState = scrollState,
            onSearchEngineClick = onSearchEngineClick,
            onSearchEngineLongPress = onSearchEngineLongPress,
            compactRowCount = compactRowCount,
            predictedTarget = predictedTarget,
        )
    }
}

/** Fixed search icon displayed at the start of the section. */
@Composable
private fun SearchIcon(
    showOverlayExpandChevron: Boolean,
    onOverlayExpandClick: (() -> Unit)?,
    isOverlayExpanded: Boolean,
) {
    val imageVector =
        if (showOverlayExpandChevron && isOverlayExpanded) {
            Icons.Rounded.ExpandLess
        } else if (showOverlayExpandChevron) {
            Icons.Rounded.ExpandMore
        } else {
            Icons.Rounded.Search
        }
    val contentDescription =
        if (showOverlayExpandChevron && isOverlayExpanded) {
            stringResource(R.string.desc_collapse)
        } else if (showOverlayExpandChevron) {
            stringResource(R.string.desc_expand)
        } else {
            stringResource(R.string.desc_search_icon)
        }
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier =
            Modifier
                .size(SearchEngineSectionConstants.SEARCH_ICON_SIZE)
                .then(
                    if (
                        showOverlayExpandChevron &&
                            onOverlayExpandClick != null
                    ) {
                        Modifier.clickable(onClick = onOverlayExpandClick)
                    } else {
                        Modifier
                    },
                ),
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
    compactRowCount: Int,
    predictedTarget: PredictedSubmitTarget?,
) {
    val resolvedRowCount = compactRowCount.coerceIn(1, 2)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val itemsPerRow = calculateItemsPerRow()
        val itemWidthDp = calculateItemWidth(maxWidth, itemsPerRow)

        if (resolvedRowCount == 2) {
            val columns = buildTwoRowColumns(enabledEngines, itemsPerRow)
            val hasSecondRowItems = columns.any { it.bottom != null }
            val visibleRowCount = if (hasSecondRowItems) 2 else 1
            val predictedTargetId = (predictedTarget as? PredictedSubmitTarget.SearchTarget)?.targetId
            val hasPredictedItem = predictedTargetId != null && enabledEngines.any { it.getId() == predictedTargetId }
            val predictionHighlightExtraHeight =
                if (hasPredictedItem && visibleRowCount == 1) {
                    SearchEngineSectionConstants.PREDICTION_HIGHLIGHT_HEIGHT_EXTRA
                } else {
                    0.dp
                }
            LazyRow(
                state = scrollState,
                horizontalArrangement = Arrangement.spacedBy(SearchEngineSectionConstants.SPACING),
                contentPadding =
                    PaddingValues(
                        horizontal = SearchEngineSectionConstants.PREDICTION_HIGHLIGHT_WIDTH_EXTRA / 2,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            (SearchEngineSectionConstants.ICON_SIZE * visibleRowCount) +
                                (if (visibleRowCount == 2) SearchEngineSectionConstants.ROW_SPACING else 0.dp) +
                                predictionHighlightExtraHeight,
                        ),
            ) {
                rowItems(columns) { column ->
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(SearchEngineSectionConstants.ROW_SPACING),
                    ) {
                        column.top?.let { topEngine ->
                            SearchEngineIconItem(
                                engine = topEngine,
                                query = query,
                                iconSize = SearchEngineSectionConstants.ICON_SIZE,
                                itemWidth = itemWidthDp,
                                onSearchEngineClick = onSearchEngineClick,
                                onSearchEngineLongPress = onSearchEngineLongPress,
                                isPredicted =
                                    (predictedTarget as? PredictedSubmitTarget.SearchTarget)
                                        ?.targetId == topEngine.getId(),
                            )
                        }
                        column.bottom?.let { bottomEngine ->
                            SearchEngineIconItem(
                                engine = bottomEngine,
                                query = query,
                                iconSize = SearchEngineSectionConstants.ICON_SIZE,
                                itemWidth = itemWidthDp,
                                onSearchEngineClick = onSearchEngineClick,
                                onSearchEngineLongPress = onSearchEngineLongPress,
                                isPredicted =
                                    (predictedTarget as? PredictedSubmitTarget.SearchTarget)
                                        ?.targetId == bottomEngine.getId(),
                            )
                        }
                    }
                }
            }
        } else {
            LazyRow(
                state = scrollState,
                horizontalArrangement = Arrangement.spacedBy(SearchEngineSectionConstants.SPACING),
                contentPadding =
                    PaddingValues(
                        horizontal = SearchEngineSectionConstants.PREDICTION_HIGHLIGHT_WIDTH_EXTRA / 2,
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowItems(enabledEngines) { engine ->
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
}

/**
 * Calculates the width for each search engine icon item. Formula: (available width - total spacing)
 * / number of items
 */
@Composable
private fun calculateItemWidth(maxWidth: androidx.compose.ui.unit.Dp, itemsPerRow: Int): androidx.compose.ui.unit.Dp {
    val totalSpacing = SearchEngineSectionConstants.SPACING * (itemsPerRow - 1)
    return (maxWidth - totalSpacing) / itemsPerRow
}

@Composable
private fun calculateItemsPerRow(): Int =
    when {
        isTablet() && isLandscape() -> 10
        isTablet() -> 8
        else -> 6
    }

private data class TwoRowColumn(
    val top: SearchTarget?,
    val bottom: SearchTarget?,
)

private fun buildTwoRowColumns(
    engines: List<SearchTarget>,
    itemsPerRow: Int,
): List<TwoRowColumn> {
    if (engines.isEmpty()) return emptyList()

    val pageSize = itemsPerRow * 2
    return buildList {
        engines.chunked(pageSize).forEach { page ->
            val topRow = page.take(itemsPerRow)
            val bottomRow = page.drop(itemsPerRow)
            val columnCount = maxOf(topRow.size, bottomRow.size)
            repeat(columnCount) { index ->
                add(
                    TwoRowColumn(
                        top = topRow.getOrNull(index),
                        bottom = bottomRow.getOrNull(index),
                    ),
                )
            }
        }
    }
}
