package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardDefaults
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
    isExpanded: Boolean,
    showAllResults: Boolean,
    isTopPredicted: Boolean = false,
    showExpandControls: Boolean,
    expandedCardMaxHeight: Dp,
    hasScrollableContent: Boolean,
    fillExpandedHeight: Boolean,
    showWallpaperBackground: Boolean,
    overlayCardColor: Color?,
    content: @Composable (Modifier, ExpandableResultsCardState) -> Unit,
) {
    val displayAsExpanded = isExpanded || showAllResults
    val canShowExpand =
        showExpandControls && resultCount > SearchScreenConstants.INITIAL_RESULT_COUNT
    val state =
        ExpandableResultsCardState(
            displayAsExpanded = displayAsExpanded,
            shouldShowExpandButton = !displayAsExpanded && canShowExpand,
            shouldShowCollapseButton = isExpanded && showExpandControls,
            shouldFillExpandedHeight = fillExpandedHeight && isExpanded && hasScrollableContent,
        )

    val contentModifier =
        if (isExpanded) {
            Modifier.fillMaxWidth()
                .heightIn(
                    min = if (state.shouldFillExpandedHeight) expandedCardMaxHeight else 0.dp,
                    max = expandedCardMaxHeight,
                )
        } else {
            Modifier.fillMaxWidth()
        }

    val cardModifier =
        modifier
            .fillMaxWidth()
                .predictedSubmitHighlight(
                    isPredicted = isTopPredicted,
                    shape = SearchResultCardDefaults.shape,
                    opaqueCardTopResultBorder = true,
                )

    SearchResultCard(
        modifier = cardModifier,
        showWallpaperBackground = showWallpaperBackground,
        overlayContainerColor = overlayCardColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                ),
        ) {
            content(contentModifier, state)
        }
    }
}

internal fun topResultIndicator(isTopPredicted: Boolean): Shape =
    if (isTopPredicted) {
        DesignTokens.ShapeLarge
    } else {
        SearchResultCardDefaults.shape
    }

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

internal fun Modifier.topPredictedRowContentPadding(
    @Suppress("UNUSED_PARAMETER") isTopPredicted: Boolean,
): Modifier =
    this
