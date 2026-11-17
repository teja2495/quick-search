package com.tk.quicksearch.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.animatePressedCorner
import com.tk.quicksearch.shared.util.hapticToggle

/**
 * A [SegmentedGroup] that collapses to its title, the Material 3 expandable list pattern: the
 * first tile holds a [title] header row with a trailing chevron that turns on the default spatial
 * spring; tapping the header toggles [expanded]. Expanded, [firstTileContent] shows under the
 * header in the same tile and [remainingTiles] (each a [SegmentedTile], indexed from 1 of
 * [tileCount]) follow. The first tile's bottom corners morph between the inner and outer radius.
 */
@Composable
fun ExpandableSegmentedGroup(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    tileCount: Int,
    modifier: Modifier = Modifier,
    firstTileContent: @Composable ColumnScope.() -> Unit,
    remainingTiles: @Composable ColumnScope.() -> Unit = {},
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val expandFraction by animateFloatAsState(
        targetValue = if (expanded && tileCount > 1) 1f else 0f,
        animationSpec = AppMotion.defaultSpatial(),
        label = "expandableGroupCorner",
    )
    val pressedInner =
        animatePressedCorner(
            interactionSource,
            rest = DesignTokens.SegmentedListInnerCorner,
            pressed = DesignTokens.SegmentPressedInnerCorner,
        )
    val firstTileShape =
        AppShapes.stacked(
            top = DesignTokens.SegmentedListOuterCorner,
            bottom = lerp(DesignTokens.SegmentedListOuterCorner, pressedInner, expandFraction.coerceIn(0f, 1f)),
        )

    SegmentedGroup(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SegmentedTile(
                index = 0,
                count = tileCount,
                interactionSource = interactionSource,
                shape = firstTileShape,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExpandableGroupHeader(
                        title = title,
                        expanded = expanded,
                        interactionSource = interactionSource,
                        onClick = {
                            hapticToggle(view)()
                            onExpandedChange(!expanded)
                        },
                    )
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = SegmentedTileContentPadding,
                                        end = SegmentedTileContentPadding,
                                        bottom = SegmentedTileContentPadding,
                                    ),
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                            content = firstTileContent,
                        )
                    }
                }
            }
            if (tileCount > 1) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.SegmentedListGap),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
                        content = remainingTiles,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableGroupHeader(
    title: String,
    expanded: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = AppMotion.defaultSpatial(),
        label = "expandableGroupChevron",
    )
    val actionLabel = stringResource(if (expanded) R.string.desc_collapse else R.string.desc_expand)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ExpandableHeaderMinHeight)
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(),
                    role = Role.Button,
                    onClickLabel = actionLabel,
                    onClick = onClick,
                ).padding(
                    horizontal = SegmentedTileToggleHorizontalPadding,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Icon(
            imageVector = MaterialSymbols.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )
    }
}

private val ExpandableHeaderMinHeight = 56.dp
