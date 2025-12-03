package com.tk.quicksearch.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.ui.theme.ThemeMode

// Constants for consistent spacing
private object ThemeSpacing {
    val cardHorizontalPadding = 20.dp
    val cardVerticalPadding = 12.dp
    val sectionTitleTopPadding = 24.dp
    val sectionTitleBottomPadding = 8.dp
    val sectionDescriptionBottomPadding = 16.dp
}

/**
 * Reusable radio button row component for theme selection.
 */
@Composable
private fun ThemeRadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = ThemeSpacing.cardHorizontalPadding,
                vertical = ThemeSpacing.cardVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

/**
 * Theme selection section for appearance settings.
 * Allows users to choose between system default, light, or dark theme.
 *
 * @param themeMode The currently selected theme mode
 * @param onThemeModeChange Callback when the theme mode changes
 * @param modifier Modifier to be applied to the section title
 */
@Composable
fun ThemeSection(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Section title
    Text(
        text = stringResource(R.string.settings_theme_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(
            top = ThemeSpacing.sectionTitleTopPadding,
            bottom = ThemeSpacing.sectionTitleBottomPadding
        )
    )
    
    // Section description
    Text(
        text = stringResource(R.string.settings_theme_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = ThemeSpacing.sectionDescriptionBottomPadding)
    )
    
    // Options card
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // System default option
            ThemeRadioRow(
                text = stringResource(R.string.settings_theme_option_system),
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
            )
            
            // Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Light option
            ThemeRadioRow(
                text = stringResource(R.string.settings_theme_option_light),
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeChange(ThemeMode.LIGHT) }
            )
            
            // Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Dark option
            ThemeRadioRow(
                text = stringResource(R.string.settings_theme_option_dark),
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeModeChange(ThemeMode.DARK) }
            )
        }
    }
}

