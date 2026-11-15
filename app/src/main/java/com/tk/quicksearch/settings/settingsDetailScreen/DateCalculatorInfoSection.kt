package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.R

private val dateCategories = listOf(
    ToolExampleCategory(
        nameRes = R.string.date_calculator_category_named_dates,
        examples = listOf("March 12 2025", "July 4", "Dec 25 2026"),
    ),
    ToolExampleCategory(
        nameRes = R.string.date_calculator_category_relative_dates,
        examples = listOf("in 3 months", "2 weeks", "2 weeks ago", "in 1 year 6 months", "10 days ago"),
    ),
    ToolExampleCategory(
        nameRes = R.string.date_calculator_category_date_differences,
        examples = listOf("March 5 to March 20", "Jan 1 to Dec 31 2025"),
    ),
    ToolExampleCategory(
        nameRes = R.string.date_calculator_category_offset_from_date,
        examples = listOf("5 days from March 12", "3 months before June 1", "2 weeks after July 4"),
    ),
    ToolExampleCategory(
        nameRes = R.string.date_calculator_category_time_arithmetic,
        examples = listOf("6 hours from now", "5min", "45 minutes ago", "2 hours 30 minutes later"),
    ),
    ToolExampleCategory(
        nameRes = R.string.date_calculator_category_time_ranges,
        examples = listOf("9am to 5pm", "14:00 to 17:30", "8:30am to 12:00pm"),
    ),
    ToolExampleCategory(
        nameRes = R.string.date_calculator_category_absolute_time,
        examples = listOf("5pm", "14:30", "9am"),
    ),
    ToolExampleCategory(
        nameRes = R.string.date_calculator_category_time_offset,
        examples = listOf("3 hours after 5pm", "30 minutes before 9am", "1 hour after 14:00"),
    ),
)

@Composable
internal fun DateCalculatorInfoSection(modifier: Modifier = Modifier) {
    ToolExamplesSection(categories = dateCategories, modifier = modifier)
}
