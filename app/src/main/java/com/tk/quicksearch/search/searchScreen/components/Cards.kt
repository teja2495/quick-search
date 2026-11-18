package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.components.AppIconButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.AppButton

@Composable
internal fun PermissionDisabledCard(
    title: String,
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    showWallpaperBackground: Boolean,
) {
    SearchResultCard(
        modifier = Modifier.fillMaxWidth(),
        showWallpaperBackground = showWallpaperBackground,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.SpacingXLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppButton(onClick = onActionClick, modifier = Modifier.align(Alignment.End)) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
internal fun UsagePermissionCard(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    showWallpaperBackground: Boolean,
) {
    SearchResultCard(
        modifier = modifier,
        showWallpaperBackground = showWallpaperBackground,
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.SpacingXLarge),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.usage_permission_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                AppIconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(DesignTokens.ButtonHeightSmall),
                    tooltip = stringResource(R.string.common_close),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Close,
                        contentDescription =
                            stringResource(R.string.common_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconSizeSmall),
                    )
                }
            }
            Text(
                text = stringResource(R.string.usage_permission_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppButton(
                onClick = onRequestPermission,
                modifier = Modifier.align(Alignment.End),
            ) { Text(text = stringResource(R.string.action_open_settings)) }
        }
    }
}

/**
 * A search-result-styled card for sections that require a permission not yet granted.
 * Looks like a settings navigation row: icon + title/subtitle + chevron, fully tappable.
 */
@Composable
internal fun SectionPermissionResultCard(
    title: String,
    message: String,
    showWallpaperBackground: Boolean,
    onActionClick: () -> Unit,
) {
    SearchResultCard(
        modifier = Modifier.fillMaxWidth(),
        showWallpaperBackground = showWallpaperBackground,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onActionClick)
                    .heightIn(min = 52.dp)
                    .padding(
                        horizontal = DesignTokens.SpacingLarge,
                        vertical = DesignTokens.CardVerticalPadding,
                    ),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MaterialSymbols.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignTokens.IconSize),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = MaterialSymbols.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
