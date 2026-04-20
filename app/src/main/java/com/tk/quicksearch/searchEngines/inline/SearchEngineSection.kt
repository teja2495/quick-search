package com.tk.quicksearch.searchEngines.inline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.compact.SearchEngineCard
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.searchEngines.shared.SearchTargetConstants
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.shared.util.isLandscape
import com.tk.quicksearch.shared.util.isTablet
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

/** Constants for search engine section layout. */
private object SearchEngineSectionConstants {
    val ICON_SIZE = SearchTargetConstants.DEFAULT_ICON_SIZE
    val SPACING = 20.dp
    val ROW_SPACING = 10.dp
    val PREDICTION_HIGHLIGHT_HEIGHT_EXTRA = 12.dp
    val PREDICTION_HIGHLIGHT_WIDTH_EXTRA = 8.dp
    val PREDICTION_HIGHLIGHT_CONTENT_PADDING =
        (PREDICTION_HIGHLIGHT_WIDTH_EXTRA / 2) + DesignTokens.BorderWidth
    val COMPACT_TOP_DIVIDER_THICKNESS = DesignTokens.BorderWidth
    val SEARCH_ICON_SIZE = SearchTargetConstants.SEARCH_ICON_SIZE
    val HORIZONTAL_PADDING = SearchTargetConstants.HORIZONTAL_PADDING
    val VERTICAL_PADDING = SearchTargetConstants.VERTICAL_PADDING
    val SEARCH_ICON_SPACING = SearchTargetConstants.SEARCH_ICON_SPACING
    val TOOL_ICON_TEXT_SPACING = DesignTokens.SpacingSmall
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
    showOverlayExpandChevron: Boolean = false,
    onOverlayExpandClick: (() -> Unit)? = null,
    isOverlayExpanded: Boolean = false,
    compactRowCount: Int = 1,
    predictedTarget: PredictedSubmitTarget? = null,
    appIconShape: AppIconShape = AppIconShape.DEFAULT,
    toolActionLabel: String? = null,
    toolActionIcon: ImageVector? = null,
    onToolActionClick: (() -> Unit)? = null,
    showOnlyToolAction: Boolean = false,
) {
    val hasToolAction = toolActionLabel != null && onToolActionClick != null
    if (enabledEngines.isEmpty() && detectedShortcutTarget == null && !hasToolAction) return

    val scrollState = externalScrollState ?: rememberLazyListState()

    // Match compact section background with the persistent search bar for visual consistency.
    val backgroundColor = AppColors.getSearchEngineSectionBackground(showWallpaperBackground)

    if (detectedShortcutTarget != null) {
        // Check if query starts with the shortcut and remove it
        // The shortcut corresponds to the detected engine
        Box(
            modifier =
                modifier.extendToScreenEdges().padding(
                    horizontal = DesignTokens.SpacingXLarge,
                    vertical = 8.dp,
                ),
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
                appIconShape = appIconShape,
            )
        }
    } else {
        val compactTopDividerColor =
            if (showWallpaperBackground) {
                AppColors.WallpaperDivider
            } else {
                AppColors.Accent.copy(alpha = 0.22f)
            }
        Surface(
            modifier =
                modifier
                    .extendToScreenEdges()
                    .graphicsLayer {
                        shadowElevation = 0f
                    },
            color = backgroundColor,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    color = compactTopDividerColor,
                    thickness = SearchEngineSectionConstants.COMPACT_TOP_DIVIDER_THICKNESS,
                )
                if ((showOnlyToolAction || enabledEngines.isEmpty()) && hasToolAction) {
                    CompactToolActionContent(
                        label = toolActionLabel!!,
                        icon = toolActionIcon,
                        onClick = onToolActionClick!!,
                    )
                } else {
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
                        appIconShape = appIconShape,
                    )
                    if (hasToolAction) {
                        CompactToolActionContent(
                            label = toolActionLabel!!,
                            icon = toolActionIcon,
                            onClick = onToolActionClick!!,
                        )
                    }
                }
            }
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
    appIconShape: AppIconShape,
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
        if (showOverlayExpandChevron) {
            OverlayExpandChevron(
                onOverlayExpandClick = onOverlayExpandClick,
                isOverlayExpanded = isOverlayExpanded,
            )
            Spacer(modifier = Modifier.width(SearchEngineSectionConstants.SEARCH_ICON_SPACING))
        }

        ScrollableEngineIcons(
            query = query,
            enabledEngines = enabledEngines,
            scrollState = scrollState,
            onSearchEngineClick = onSearchEngineClick,
            onSearchEngineLongPress = onSearchEngineLongPress,
            compactRowCount = compactRowCount,
            predictedTarget = predictedTarget,
            appIconShape = appIconShape,
        )
    }
}

@Composable
private fun CompactToolActionContent(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        hapticConfirm(view)()
                        onClick()
                    },
                ).padding(
                    horizontal = SearchEngineSectionConstants.HORIZONTAL_PADDING,
                    vertical = SearchEngineSectionConstants.VERTICAL_PADDING,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(SearchEngineSectionConstants.ICON_SIZE),
            )
            Spacer(modifier = Modifier.width(SearchEngineSectionConstants.TOOL_ICON_TEXT_SPACING))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Overlay expand/collapse chevron shown when overlay controls are enabled. */
@Composable
private fun OverlayExpandChevron(
    onOverlayExpandClick: (() -> Unit)?,
    isOverlayExpanded: Boolean,
) {
    val imageVector =
        if (isOverlayExpanded) {
            Icons.Rounded.ExpandLess
        } else {
            Icons.Rounded.ExpandMore
        }
    val contentDescription =
        if (isOverlayExpanded) {
            stringResource(R.string.desc_collapse)
        } else {
            stringResource(R.string.desc_expand)
        }
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier =
            Modifier
                .size(SearchEngineSectionConstants.SEARCH_ICON_SIZE)
                .then(
                    if (onOverlayExpandClick != null) {
                        Modifier.clickable(onClick = onOverlayExpandClick)
                    } else {
                        Modifier
                    },
                ),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Scrollable row of search engine icons. Calculates item width to fit 7 items per visible row on phones. */
@Composable
private fun ScrollableEngineIcons(
    query: String,
    enabledEngines: List<SearchTarget>,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    compactRowCount: Int,
    predictedTarget: PredictedSubmitTarget?,
    appIconShape: AppIconShape,
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
                        horizontal = SearchEngineSectionConstants.PREDICTION_HIGHLIGHT_CONTENT_PADDING,
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
                                appIconShape = appIconShape,
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
                                appIconShape = appIconShape,
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
                        horizontal = SearchEngineSectionConstants.PREDICTION_HIGHLIGHT_CONTENT_PADDING,
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
                        appIconShape = appIconShape,
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
    val totalHorizontalContentPadding = SearchEngineSectionConstants.PREDICTION_HIGHLIGHT_CONTENT_PADDING * 2
    return (maxWidth - totalSpacing - totalHorizontalContentPadding) / itemsPerRow
}

@Composable
private fun calculateItemsPerRow(): Int =
    when {
        isTablet() && isLandscape() -> 10
        isTablet() -> 8
        else -> 7
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
