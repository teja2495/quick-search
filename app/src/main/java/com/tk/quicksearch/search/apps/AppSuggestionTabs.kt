package com.tk.quicksearch.search.apps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import androidx.compose.ui.platform.LocalView

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
    SecondaryScrollableTabRow(
        selectedTabIndex = selected,
        // Spacing around the strip is even to the text baseline, but descenders and the solid icon
        // tops below make the lower gap read tighter, so the strip sits a little lower.
        modifier =
            Modifier.fillMaxWidth().padding(
                bottom = TabStripOpticalShift * 2,
            ),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = DesignTokens.SpacingXSmall,
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
                selectedContentColor = MaterialTheme.colorScheme.onSurface,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(
                    modifier = Modifier.heightIn(min = SuggestionTabHeight)
                        .padding(horizontal = DesignTokens.SpacingLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
