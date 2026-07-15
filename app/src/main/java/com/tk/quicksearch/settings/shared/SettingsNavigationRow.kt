package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Data class for settings card items.
 */
data class SettingsCardItem(
    val title: String,
    val description: String,
    val isBeta: Boolean = false,
    val tagLabel: String? = null,
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
    val iconBitmap: ImageBitmap? = null,
    val iconTint: Color? = null,
    val actionIcon: ImageVector = Icons.Rounded.ChevronRight,
    val isEnabled: Boolean = true,
    val actionOnPress: () -> Unit,
)

/**
 * Individual row for a settings card item.
 */
@Composable
fun SettingsNavigationRow(
    item: SettingsCardItem,
    contentPadding: PaddingValues,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = item.isEnabled, onClick = item.actionOnPress)
                .padding(contentPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            // Icon (either ImageVector or painter resource)
            when {
                item.iconBitmap != null -> {
                    Image(
                        bitmap = item.iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(DesignTokens.IconSizeSmall),
                        contentScale = ContentScale.Fit,
                    )
                }

                item.icon != null -> {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconSizeSmall),
                    )
                }

                item.iconResId != null -> {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = null,
                        tint = item.iconTint ?: Color.Unspecified,
                        modifier = Modifier.size(DesignTokens.IconSizeSmall),
                    )
                }
            }

            // Title and description
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (item.isBeta || item.tagLabel != null) {
                        BetaTagChip(tagText = item.tagLabel)
                    }
                }
                androidx.compose.material3.Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Action icon
        if (item.isEnabled) {
            Icon(
                imageVector = item.actionIcon,
                contentDescription = stringResource(R.string.desc_navigate_forward),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = DesignTokens.SpacingSmall),
            )
        }
    }
}
