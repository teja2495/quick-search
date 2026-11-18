package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** One group of example queries for a tool, such as "Length" with "5 miles in km". */
internal data class ToolExampleCategory(@StringRes val nameRes: Int, val examples: List<String>)

/**
 * A tool's example queries: a short intro, then one segmented tile per category with an accent
 * titleSmall header and the examples as faint filled chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ToolExamplesSection(
    categories: List<ToolExampleCategory>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        Text(
            text = stringResource(R.string.settings_tools_info_examples_header),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DesignTokens.SpacingLarge),
        )

        SegmentedGroup {
            categories.forEachIndexed { index, category ->
                SegmentedTile(index = index, count = categories.size) {
                    SegmentedTileContent(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
                        Text(
                            text = stringResource(category.nameRes),
                            style = MaterialTheme.typography.titleSmall,
                            color = AppColors.Accent,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        ) {
                            category.examples.forEach { example -> ToolExampleChip(example) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * An example query as quiet content, not a control: bodyMedium on a faint onSurface fill with 8dp
 * corners and no border, so a page of them reads as text rather than a wall of buttons. The fill
 * comes from onSurface so it shows on tiles over wallpaper, custom image, AMOLED, and Material You.
 */
@Composable
private fun ToolExampleChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
            Modifier
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintSubtleAlpha),
                    AppShapes.small,
                )
                .padding(horizontal = DesignTokens.SpacingMedium, vertical = DesignTokens.SpacingSmall),
    )
}
