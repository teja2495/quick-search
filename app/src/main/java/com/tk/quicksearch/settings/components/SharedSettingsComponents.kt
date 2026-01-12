package com.tk.quicksearch.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.R
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticToggle

/**
 * Reusable section title component.
 */
@Composable
fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding)
            )
        }

        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = DesignTokens.SectionDescriptionBottomPadding)
            )
        }
    }
}

/**
 * Reusable settings card component with consistent styling.
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.ExtraLargeCardShape
    ) {
        content()
    }
}

/**
 * Reusable toggle row component for settings cards.
 * Provides consistent styling and layout across all toggle rows.
 */
@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    extraVerticalPadding: Dp = 0.dp,
    showDivider: Boolean = true
) {
    val view = LocalView.current
    val topPadding = DesignTokens.cardItemTopPadding(isFirstItem) + extraVerticalPadding
    val bottomPadding = DesignTokens.cardItemBottomPadding(isLastItem) + extraVerticalPadding

    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = DesignTokens.CardHorizontalPadding,
                    top = topPadding,
                    end = DesignTokens.CardHorizontalPadding,
                    bottom = bottomPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing)
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSize)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = { newValue ->
                    hapticToggle(view)()
                    onCheckedChange(newValue)
                }
            )
        }

        if (showDivider && !isLastItem) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

/**
 * Reusable info row component for displaying read-only information.
 */
@Composable
fun SettingsInfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    showDivider: Boolean = true
) {
    val topPadding = DesignTokens.cardItemTopPadding(isFirstItem)
    val bottomPadding = DesignTokens.cardItemBottomPadding(isLastItem)

    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = DesignTokens.CardHorizontalPadding,
                    top = topPadding,
                    end = DesignTokens.CardHorizontalPadding,
                    bottom = bottomPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing)
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSize)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showDivider && !isLastItem) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
