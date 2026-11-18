package com.tk.quicksearch.search.searchScreen.components

import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.search.core.SearchSectionUiMetadataRegistry
import com.tk.quicksearch.shared.ui.components.ItemMenuTile
import com.tk.quicksearch.shared.ui.components.ItemMenuTileButton
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Grid of searchable sections opened from the search bar's leading icon. Anchored to that icon,
 * it opens above the search bar, or below it when there isn't room (top search bar).
 */
@Composable
internal fun SearchBarSectionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSectionSelected: (SearchSection) -> Unit,
) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = expanded
    if (!visibleState.currentState && !visibleState.targetState) return

    val density = LocalDensity.current
    val positionProvider =
        remember(density) {
            SectionMenuPositionProvider(
                verticalGapPx = with(density) { SectionMenuAnchorGap.roundToPx() },
                screenMarginPx = with(density) { SectionMenuScreenMargin.roundToPx() },
            )
        }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter =
                fadeIn(tween(SectionMenuAnimationMs, easing = AppMotion.EmphasizedDecelerateEasing)) +
                    scaleIn(
                        animationSpec = tween(SectionMenuAnimationMs, easing = AppMotion.EmphasizedDecelerateEasing),
                        initialScale = SectionMenuInitialScale,
                        transformOrigin = positionProvider.transformOrigin,
                    ),
            exit =
                fadeOut(tween(SectionMenuAnimationMs, easing = AppMotion.EmphasizedAccelerateEasing)) +
                    scaleOut(
                        animationSpec = tween(SectionMenuAnimationMs, easing = AppMotion.EmphasizedAccelerateEasing),
                        targetScale = SectionMenuInitialScale,
                        transformOrigin = positionProvider.transformOrigin,
                    ),
        ) {
            SectionMenuGrid(onDismiss = onDismiss, onSectionSelected = onSectionSelected)
        }
    }
}

@Composable
private fun SectionMenuGrid(
    onDismiss: () -> Unit,
    onSectionSelected: (SearchSection) -> Unit,
) {
    // Styled like the app long-press menu (ItemMenuPopup): header with close button, tinted tiles.
    Column(
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        modifier =
            Modifier
                .padding(horizontal = SectionMenuScreenMargin)
                .widthIn(max = SectionMenuMaxWidth)
                .fillMaxWidth()
                .clip(AppShapes.extraLarge)
                .background(AppColors.DialogBackground)
                .padding(
                    start = DesignTokens.SpacingMedium,
                    end = DesignTokens.SpacingMedium,
                    top = DesignTokens.SpacingSmall,
                    bottom = DesignTokens.SpacingMedium,
                ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.search_section_menu_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = DesignTokens.SpacingXSmall),
            )
            AppIconButton(onClick = onDismiss, tooltip = stringResource(R.string.dialog_cancel)) {
                Icon(
                    imageVector = MaterialSymbols.Close,
                    contentDescription = stringResource(R.string.dialog_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        sectionMenuRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
                row.forEach { section ->
                    val metadata = SearchSectionUiMetadataRegistry.metadataFor(section)
                    ItemMenuTileButton(
                        tile =
                            ItemMenuTile(
                                label = stringResource(metadata.sectionLabelRes),
                                icon = { Icon(imageVector = metadata.searchBarIcon, contentDescription = null) },
                                onClick = { onSectionSelected(section) },
                            ),
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep tiles the same width; any gap stays at the end of the last row.
                repeat(SectionMenuColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Centers the menu horizontally within the window margins and places it above the anchor, flipping
 * below when the space above can't fit it.
 */
private class SectionMenuPositionProvider(
    private val verticalGapPx: Int,
    private val screenMarginPx: Int,
) : PopupPositionProvider {
    var transformOrigin: TransformOrigin = TransformOrigin(0.5f, 1f)
        private set

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0)
        val aboveY = anchorBounds.top - verticalGapPx - popupContentSize.height
        val fitsAbove = aboveY >= screenMarginPx
        transformOrigin = TransformOrigin(0.5f, if (fitsAbove) 1f else 0f)
        val y = if (fitsAbove) aboveY else anchorBounds.bottom + verticalGapPx
        return IntOffset(x, y)
    }
}

private const val SectionMenuColumns = 3
private const val SectionMenuAnimationMs = AppMotion.DurationExit
private const val SectionMenuInitialScale = 0.9f

// The anchor is the leading icon; this clears the rest of the search bar plus a small gap.
private val SectionMenuAnchorGap = 28.dp
private val SectionMenuScreenMargin = DesignTokens.SpacingLarge
private val SectionMenuMaxWidth = 420.dp

private val sectionMenuRows = SearchSectionRegistry.orderedSections.chunked(SectionMenuColumns)
