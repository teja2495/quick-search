package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.shared.ui.theme.AppMotion
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.searchEngines.inline.InsetSearchBarGeometry
import com.tk.quicksearch.shared.ui.theme.DesignTokens

internal data class SearchBarGeometry(
    val useInsetEngineStrip: Boolean,
    val insetEngineStripOverlap: androidx.compose.ui.unit.Dp,
    /** Read only in layout or draw, or by the inset card itself, so it does not recompose the screen. */
    val insetEngineStripFullBleedFraction: () -> Float,
    /** True while the inset card is between resting and full bleed, which resizes the bottom chrome. */
    val isInsetEngineStripAnimating: () -> Boolean,
    val searchFieldModifier: Modifier,
    val insetCardBackdrop: InsetCardBackdrop,
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
    // With the bottom search bar, compact engines turn into a rounded card that the bar sits at the
    // top of, with the engine row below it, instead of a full-bleed band above the bar.
    val useInsetEngineStrip =
            showBottomSearchBar &&
                    state.isSearchEngineCompactMode &&
                    expandedSection == ExpandedSection.NONE &&
                    !isSearchHistoryExpanded &&
                    state.detectedShortcutTarget == null &&
                    state.detectedAliasSearchSection == null

    // The engine row paints the card behind a transparent bar, so it has to reach up past the bar's
    // top edge; falling short would draw the card's top edge inside the bar. The derived overlap assumes a
    // default-height field, so track the measured height and keep whichever is taller.
    var measuredSearchBarHeight by remember { mutableStateOf(0.dp) }
    val insetEngineStripOverlap = InsetSearchBarGeometry.overlapFor(measuredSearchBarHeight)

    // With the keyboard closed the card floats above the gesture handle, which the system keeps
    // clear anyway. With the keyboard open nothing reserves that space, so the card spreads to the
    // screen edges and down onto the keyboard. Keyed on the IME animation target so the card starts
    // moving together with the keyboard in both directions instead of after it settles. The overlay
    // has no gesture handle to clear, so its card always spans the overlay's full width.
    @OptIn(ExperimentalLayoutApi::class)
    val isImeOpeningOrOpen = WindowInsets.imeAnimationTarget.getBottom(density) > 0
    val insetEngineStripFullBleedFractionState =
            animateFloatAsState(
                    targetValue =
                            if (useInsetEngineStrip && (isOverlayPresentation || isImeOpeningOrOpen)) {
                                1f
                            } else {
                                0f
                            },
                    animationSpec = tween(durationMillis = AppMotion.DurationStandard, easing = AppMotion.EmphasizedEasing),
                    label = "insetEngineStripFullBleed",
            )

    val searchFieldModifier =
            if (useInsetEngineStrip) {
                // Inset from the card's edges; the card reaches up by exactly these spacings plus
                // the bar.
                // Horizontal inset applied in layout: it follows the full-bleed animation every frame.
                Modifier.layout { measurable, constraints ->
                    val inset =
                            InsetSearchBarGeometry.barHorizontalInset(
                                    insetEngineStripFullBleedFractionState.value,
                            ).roundToPx()
                    val placeable = measurable.measure(constraints.offset(horizontal = -inset * 2))
                    layout(constraints.constrainWidth(placeable.width + inset * 2), placeable.height) {
                        placeable.place(inset, 0)
                    }
                }.padding(
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
                // The results extend up beneath the top bar; keep the bar above them for touches.
                Modifier.zIndex(1f).padding(
                        bottom =
                                (if (state.oneHandedMode) {
                                    DesignTokens.SpacingMedium
                                } else {
                                    DesignTokens.SpacingXSmall
                                }) + DesignTokens.SpacingXSmall,
                )
            }

    return SearchBarGeometry(
        useInsetEngineStrip,
        insetEngineStripOverlap,
        remember(insetEngineStripFullBleedFractionState) { { insetEngineStripFullBleedFractionState.value } },
        remember(insetEngineStripFullBleedFractionState) {
            { insetEngineStripFullBleedFractionState.value.let { it > 0f && it < 1f } }
        },
        searchFieldModifier,
        rememberInsetCardBackdrop(),
    )
}
