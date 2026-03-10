package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.shared.ui.theme.AppColors

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

    val cardColors =
        if (overlayCardColor != null) {
            CardDefaults.cardColors(containerColor = overlayCardColor)
        } else {
            AppColors.getCardColors(showWallpaperBackground = showWallpaperBackground)
        }
    val cardElevation = AppColors.getCardElevation(showWallpaperBackground = showWallpaperBackground)

    if (showWallpaperBackground) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = cardColors,
            shape = MaterialTheme.shapes.extraLarge,
            elevation = cardElevation,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content(contentModifier, state)
            }
        }
    } else {
        ElevatedCard(
            modifier = modifier.fillMaxWidth(),
            colors = cardColors,
            shape = MaterialTheme.shapes.extraLarge,
            elevation = cardElevation,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content(contentModifier, state)
            }
        }
    }
}
