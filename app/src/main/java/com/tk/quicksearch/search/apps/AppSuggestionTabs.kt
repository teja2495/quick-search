package com.tk.quicksearch.search.apps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.util.hapticToggle

private val TabStripOpticalShift = 2.dp
private val SuggestionTabHeight = DesignTokens.MinTouchTarget

/** Every enabled suggestion category stays reachable; long translations scroll without truncation. */
@OptIn(ExperimentalMaterial3Api::class)
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
    SecondaryScrollableTabRow(
        selectedTabIndex = selected,
        // Spacing around the strip is even to the text baseline, but descenders and the solid icon
        // tops below make the lower gap read tighter, so the strip sits a little lower.
        modifier =
            Modifier.fillMaxWidth().padding(
                bottom = TabStripOpticalShift * 2,
            ),
        containerColor = Color.Transparent,
        contentColor = tabColor,
        edgePadding = DesignTokens.SpacingXSmall,
        indicator = {},
        divider = {},
    ) {
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
                unselectedContentColor = tabColor,
            ) {
                SuggestionTabLabel(title = tab.title, selected = index == selected)
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
            .padding(horizontal = DesignTokens.SpacingLarge)
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
