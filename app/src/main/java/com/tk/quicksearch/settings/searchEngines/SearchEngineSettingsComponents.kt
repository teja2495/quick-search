package com.tk.quicksearch.settings.searchEngines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.settings.components.SettingsToggleRow

/**
 * Constants for consistent spacing and styling in search engine settings.
 * @deprecated Use DesignTokens instead for new components.
 */
object SearchEngineSettingsSpacing {
    val cardHorizontalPadding = com.tk.quicksearch.ui.theme.DesignTokens.CardHorizontalPadding
    val cardVerticalPadding = com.tk.quicksearch.ui.theme.DesignTokens.CardVerticalPadding
    val cardTopPadding = com.tk.quicksearch.ui.theme.DesignTokens.CardTopPadding
    val cardBottomPadding = com.tk.quicksearch.ui.theme.DesignTokens.CardBottomPadding
    val apiKeyButtonBottomPadding = 8.dp
    val rowHorizontalPadding = com.tk.quicksearch.ui.theme.DesignTokens.CardHorizontalPadding
    val rowVerticalPadding = com.tk.quicksearch.ui.theme.DesignTokens.CardVerticalPadding
    val itemHeight = com.tk.quicksearch.ui.theme.DesignTokens.DraggableItemHeight
}

/**
 * Reusable toggle row component for search engine settings cards.
 * Provides consistent styling and layout across all toggle rows.
 * @deprecated Use SettingsToggleRow from shared components instead.
 */
@Composable
fun SearchEngineToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false
) {
    SettingsToggleRow(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        subtitle = subtitle,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem
    )
}

/**
 * Reusable divider component with consistent styling.
 */
@Composable
fun SearchEngineDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
