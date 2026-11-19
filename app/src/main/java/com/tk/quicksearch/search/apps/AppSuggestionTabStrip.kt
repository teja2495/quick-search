package com.tk.quicksearch.search.apps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.util.hapticToggle

private val TabStripOpticalShift = 2.dp
// Deliberately below the 48dp touch target: tabs are mostly switched by swiping the pager,
// so a tighter strip is worth the smaller tap area.
private val SuggestionTabHeight = 36.dp
private const val VisibleTabCount = 3

/**
 * Shows three tabs at a time, each at least a third of the strip wide, with the selected one always
 * scrolled to the middle. A title longer than a third widens its tab instead of being cut off. Both
 * ends are padded so the first and last tabs can reach the center.
 */
@Composable
internal fun AppSuggestionTabStrip(
    tabs: List<AppSuggestionTab>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    if (tabs.isEmpty()) return
    val selected = selectedIndex.coerceIn(tabs.indices)
    val view = LocalView.current
    val tabColor = homeTextColor()
    val scrollState = rememberScrollState()
    // Each tab's center within the strip, written by the layout pass.
    var tabCenters by remember { mutableStateOf(emptyList<Int>()) }
    var viewportWidth by remember { mutableIntStateOf(0) }
    var hasCentered by remember { mutableStateOf(false) }
    LaunchedEffect(selected, tabCenters, viewportWidth) {
        val center = tabCenters.getOrNull(selected) ?: return@LaunchedEffect
        if (viewportWidth == 0) return@LaunchedEffect
        val target = center - viewportWidth / 2
        if (hasCentered) {
            scrollState.animateScrollTo(
                target,
                tween(AppMotion.DurationStandard, easing = AppMotion.EmphasizedEasing),
            )
        } else {
            scrollState.scrollTo(target)
            hasCentered = true
        }
    }
    CompositionLocalProvider(LocalContentColor provides tabColor) {
        Layout(
            content = {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selected == index,
                        onClick = {
                            if (index != selected) {
                                hapticToggle(view)()
                                onSelectedIndexChange(index)
                            }
                        },
                        modifier = Modifier.heightIn(min = SuggestionTabHeight),
                        selectedContentColor = tabColor,
                        unselectedContentColor = tabColor.copy(alpha = AppColors.ContentHighAlpha),
                    ) {
                        SuggestionTabLabel(title = tab.title, selected = index == selected)
                    }
                }
            },
            // Spacing around the strip is even to the text baseline, but descenders and the solid icon
            // tops below make the lower gap read tighter, so the strip sits a little lower.
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = TabStripOpticalShift * 2)
                    .onSizeChanged { viewportWidth = it.width }
                    .horizontalScroll(scrollState)
                    .selectableGroup(),
        ) { measurables, constraints ->
            val slot = viewportWidth / VisibleTabCount
            val placeables =
                measurables.map {
                    val width = maxOf(slot, it.maxIntrinsicWidth(Constraints.Infinity))
                    it.measure(Constraints(minWidth = width, maxWidth = width, minHeight = constraints.minHeight))
                }
            val height = placeables.maxOf { it.height }
            val leading = ((viewportWidth - placeables.first().width) / 2).coerceAtLeast(0)
            val trailing = ((viewportWidth - placeables.last().width) / 2).coerceAtLeast(0)
            val lefts = IntArray(placeables.size)
            var x = leading
            placeables.forEachIndexed { index, placeable ->
                lefts[index] = x
                x += placeable.width
            }
            val centers = placeables.mapIndexed { index, placeable -> lefts[index] + placeable.width / 2 }
            if (centers != tabCenters) tabCenters = centers
            layout(x + trailing, height) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(lefts[index], (height - placeable.height) / 2)
                }
            }
        }
    }
}

@Composable
private fun SuggestionTabLabel(title: String, selected: Boolean) {
    val boldAlpha = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedEasing),
        label = "Suggestion tab weight",
    )
    val labelStyle = MaterialTheme.typography.labelLarge
    // Measure both weights so changing selection keeps the scrollable tabs in place.
    Box(
        modifier = Modifier
            .heightIn(min = SuggestionTabHeight)
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.SpacingSmall)
            .clearAndSetSemantics { text = AnnotatedString(title) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            modifier = Modifier.graphicsLayer { alpha = 1f - boldAlpha.value },
            style = labelStyle,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = title,
            modifier = Modifier.graphicsLayer { alpha = boldAlpha.value },
            style = labelStyle,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}
