package com.tk.quicksearch.search.searchScreen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.searchEngines.inline.InsetSearchBarGeometry
import com.tk.quicksearch.shared.ui.theme.DesignTokens

internal data class SearchBarGeometry(
    val useInsetEngineStrip: Boolean,
    val insetEngineStripOverlap: androidx.compose.ui.unit.Dp,
    val insetEngineStripFullBleedFraction: Float,
    val searchFieldModifier: Modifier,
)

@Composable
internal fun rememberSearchBarGeometry(
    state: SearchUiState,
    showBottomSearchBar: Boolean,
    expandedSection: ExpandedSection,
    isSearchHistoryExpanded: Boolean,
    isOverlayPresentation: Boolean,
): SearchBarGeometry {
    val density = LocalDensity.current
    // With the bottom search bar, the compact engine strip becomes a rounded container attached to
    // the top of the search bar instead of a full-bleed band behind it. It tucks behind the bar so
    // the two read as one shape.
    val useInsetEngineStrip =
            showBottomSearchBar &&
                    state.isSearchEngineCompactMode &&
                    expandedSection == ExpandedSection.NONE &&
                    !isSearchHistoryExpanded &&
                    state.detectedShortcutTarget == null &&
                    state.detectedAliasSearchSection == null

    // The strip paints the card behind a transparent bar, so it has to reach past the bar's bottom
    // edge; falling short would draw its own outline inside the bar. The derived overlap assumes a
    // default-height field, so track the measured height and keep whichever is taller.
    var measuredSearchBarHeight by remember { mutableStateOf(0.dp) }
    val insetEngineStripOverlap = InsetSearchBarGeometry.overlapFor(measuredSearchBarHeight)

    // With the keyboard closed the card floats above the gesture handle, which the system keeps
    // clear anyway. With the keyboard open nothing reserves that space, so the card spreads to the
    // screen edges and down onto the keyboard. Keyed on the IME animation target so the card starts
    // moving together with the keyboard in both directions instead of after it settles.
    @OptIn(ExperimentalLayoutApi::class)
    val isImeOpeningOrOpen = WindowInsets.imeAnimationTarget.getBottom(density) > 0
    val insetEngineStripFullBleedFraction by
            animateFloatAsState(
                    targetValue =
                            if (useInsetEngineStrip && !isOverlayPresentation && isImeOpeningOrOpen) {
                                1f
                            } else {
                                0f
                            },
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "insetEngineStripFullBleed",
            )

    val searchFieldModifier =
            if (useInsetEngineStrip) {
                // Inset on every side by the same amount so the bar sits centred inside the card
                // the strip paints; the strip reaches down by exactly these spacings plus the bar.
                Modifier.padding(
                        start =
                                InsetSearchBarGeometry.barHorizontalInset(
                                        insetEngineStripFullBleedFraction,
                                ),
                        end =
                                InsetSearchBarGeometry.barHorizontalInset(
                                        insetEngineStripFullBleedFraction,
                                ),
                        top = InsetSearchBarGeometry.BarTopSpacing,
                        bottom = InsetSearchBarGeometry.BarBottomSpacing,
                ).onSizeChanged { size ->
                    val measured = with(density) { size.height.toDp() }
                    if (measured > 0.dp && measured != measuredSearchBarHeight) {
                        measuredSearchBarHeight = measured
                    }
                }
            } else if (showBottomSearchBar) {
                Modifier.padding(
                        top =
                                if (state.oneHandedMode) {
                                    DesignTokens.SpacingXSmall
                                } else {
                                    0.dp
                                },
                        bottom = DesignTokens.SpacingMedium,
                )
            } else {
                Modifier.padding(
                        bottom =
                                (if (state.oneHandedMode) {
                                    DesignTokens.SpacingMedium
                                } else {
                                    DesignTokens.SpacingXSmall
                                }) + DesignTokens.SpacingXXSmall,
                )
            }

    return SearchBarGeometry(
        useInsetEngineStrip,
        insetEngineStripOverlap,
        insetEngineStripFullBleedFraction,
        searchFieldModifier,
    )
}
