package com.tk.quicksearch.settings.searchEngineSettings

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import com.tk.quicksearch.shared.ui.theme.AppColors

/**
 * Reusable divider component with consistent styling.
 */
@Composable
fun SearchEngineDivider() {
    HorizontalDivider(color = AppColors.SettingsDivider)
}
