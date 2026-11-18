package com.tk.quicksearch.search.searchScreen.components

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardDefaults
import com.tk.quicksearch.search.searchScreen.shared.LocalSegmentedRowsStyle
import com.tk.quicksearch.search.searchScreen.shared.SegmentedRowsStyle
import com.tk.quicksearch.shared.ui.theme.DesignTokens

internal data class ExpandableResultsCardState(
    val displayAsExpanded: Boolean,
    val shouldShowExpandButton: Boolean,
    val shouldShowCollapseButton: Boolean,
    val shouldFillExpandedHeight: Boolean,
)

@Composable
internal fun ExpandableResultsCard(
    modifier: Modifier = Modifier,
    resultCount: Int,
    hasAdditionalResults: Boolean = resultCount > SearchScreenConstants.INITIAL_RESULT_COUNT,
    isExpanded: Boolean,
    showAllResults: Boolean,
    isTopPredicted: Boolean = false,
    showExpandControls: Boolean,
    expandedCardMaxHeight: Dp,
    constrainExpandedHeight: Boolean = true,
    hasScrollableContent: Boolean,
    fillExpandedHeight: Boolean,
    showWallpaperBackground: Boolean,
    overlayCardColor: Color?,
    segmented: Boolean = false,
    content: @Composable (Modifier, ExpandableResultsCardState) -> Unit,
) {
    val displayAsExpanded = isExpanded || showAllResults
    val canShowExpand = showExpandControls && hasAdditionalResults
    val state =
        ExpandableResultsCardState(
            displayAsExpanded = displayAsExpanded,
            shouldShowExpandButton = !displayAsExpanded && canShowExpand,
            shouldShowCollapseButton = isExpanded && showExpandControls,
            shouldFillExpandedHeight = fillExpandedHeight && isExpanded && hasScrollableContent,
        )

    val cardLayoutEnabled = LocalSearchCardLayoutEnabled.current
    val cardShape = SearchResultCardDefaults.shape
    val contentModifier =
        if (isExpanded && constrainExpandedHeight) {
            Modifier.fillMaxWidth()
                .heightIn(
                    min = if (state.shouldFillExpandedHeight) expandedCardMaxHeight else 0.dp,
                    max = expandedCardMaxHeight,
                )
                // The expanded rows scroll inside this viewport; round it like the card so rows
                // passing its top and bottom edges follow the card corners instead of a square cut.
                .then(if (cardLayoutEnabled) Modifier.clip(cardShape) else Modifier)
        } else {
            Modifier.fillMaxWidth()
        }

    val cardModifier =
        modifier
            .fillMaxWidth()
            .predictedSubmitHighlight(
                isPredicted = isTopPredicted,
                shape = cardShape,
                opaqueCardTopResultBorder = true,
            )

    if (segmented && cardLayoutEnabled) {
        // Rows draw their own tiles (ResultSegment); the card itself is only the group.
        val containerColor =
            SearchResultCardDefaults.containerColor(showWallpaperBackground, overlayCardColor)
        CompositionLocalProvider(LocalSegmentedRowsStyle provides SegmentedRowsStyle(containerColor)) {
            Column(modifier = cardModifier) {
                content(contentModifier, state)
            }
        }
        return
    }

    SearchResultCard(
        modifier = cardModifier,
        showWallpaperBackground = showWallpaperBackground,
        overlayContainerColor = overlayCardColor,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            content(contentModifier, state)
        }
    }
}

@Composable
internal fun topResultIndicator(isTopPredicted: Boolean): Shape =
    if (isTopPredicted) {
        AppShapes.large
    } else {
        SearchResultCardDefaults.shape
    }

@Composable
internal fun Modifier.topPredictedRowContainer(
    isTopPredicted: Boolean,
    shape: Shape = topResultIndicator(isTopPredicted),
): Modifier =
    this
        .then(
            if (isTopPredicted) {
                Modifier.padding(vertical = DesignTokens.SpacingXSmall)
            } else {
                Modifier
            },
        )
        .predictedSubmitHighlight(
            isPredicted = isTopPredicted,
            shape = shape,
        )
        .clip(shape)

/** Vertical padding for a result row; halved when the card layout is off so flat rows sit closer. */
@Composable
internal fun resultRowVerticalPadding(base: Dp): Dp =
    if (LocalSearchCardLayoutEnabled.current) base else base / 2
