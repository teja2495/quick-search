package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppPill
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private data class DateCategory(@StringRes val nameRes: Int, val examples: List<String>)

private val dateCategories = listOf(
    DateCategory(
        nameRes = R.string.date_calculator_category_named_dates,
        examples = listOf("March 12 2025", "July 4", "Dec 25 2026"),
    ),
    DateCategory(
        nameRes = R.string.date_calculator_category_relative_dates,
        examples = listOf("in 3 months", "2 weeks ago", "in 1 year 6 months", "10 days ago"),
    ),
    DateCategory(
        nameRes = R.string.date_calculator_category_date_differences,
        examples = listOf("March 5 to March 20", "Jan 1 to Dec 31 2025"),
    ),
    DateCategory(
        nameRes = R.string.date_calculator_category_offset_from_date,
        examples = listOf("5 days from March 12", "3 months before June 1", "2 weeks after July 4"),
    ),
    DateCategory(
        nameRes = R.string.date_calculator_category_time_arithmetic,
        examples = listOf("6 hours from now", "45 minutes ago", "2 hours 30 minutes later"),
    ),
    DateCategory(
        nameRes = R.string.date_calculator_category_time_ranges,
        examples = listOf("9am to 5pm", "14:00 to 17:30", "8:30am to 12:00pm"),
    ),
    DateCategory(
        nameRes = R.string.date_calculator_category_absolute_time,
        examples = listOf("5pm", "14:30", "9am"),
    ),
    DateCategory(
        nameRes = R.string.date_calculator_category_time_offset,
        examples = listOf("3 hours after 5pm", "30 minutes before 9am", "1 hour after 14:00"),
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DateCalculatorInfoSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        Text(
            text = stringResource(R.string.settings_tools_info_examples_header),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = DesignTokens.SpacingXSmall,
                vertical = DesignTokens.SpacingXSmall,
            ),
        )

        dateCategories.forEach { category ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = AppColors.getCardColors(showWallpaperBackground = false),
                elevation = AppColors.getCardElevation(false),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = DesignTokens.CardHorizontalPadding,
                        vertical = DesignTokens.CardVerticalPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(category.nameRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        category.examples.forEach { example ->
                            AppPill(text = example)
                        }
                    }
                }
            }
        }
    }
}
