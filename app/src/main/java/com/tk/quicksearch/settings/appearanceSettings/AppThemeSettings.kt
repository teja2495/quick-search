package com.tk.quicksearch.settings.AppearanceSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun AppThemeCard(
    currentThemeMode: AppThemeMode,
    onSetAppThemeMode: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.settings_app_theme_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppThemeOption(
                    label = stringResource(R.string.settings_app_theme_light),
                    icon = Icons.Rounded.LightMode,
                    selected = currentThemeMode == AppThemeMode.LIGHT,
                    onClick = { onSetAppThemeMode(AppThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f),
                )
                AppThemeOption(
                    label = stringResource(R.string.settings_app_theme_dark),
                    icon = Icons.Rounded.DarkMode,
                    selected = currentThemeMode == AppThemeMode.DARK,
                    onClick = { onSetAppThemeMode(AppThemeMode.DARK) },
                    modifier = Modifier.weight(1f),
                )
                AppThemeOption(
                    label = stringResource(R.string.settings_app_theme_system),
                    icon = Icons.Rounded.Settings,
                    selected = currentThemeMode == AppThemeMode.SYSTEM,
                    onClick = { onSetAppThemeMode(AppThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AppThemeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    val backgroundColor =
        if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        }

    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.large)
                .background(color = backgroundColor, shape = MaterialTheme.shapes.large)
                .border(
                    width = DesignTokens.BorderWidth,
                    color = borderColor,
                    shape = MaterialTheme.shapes.large,
                )
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .padding(
                    horizontal = DesignTokens.ChipHorizontalPadding,
                    vertical = DesignTokens.ChipVerticalPadding,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
