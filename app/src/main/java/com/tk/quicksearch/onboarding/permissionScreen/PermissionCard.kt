package com.tk.quicksearch.onboarding.permissionScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle

data class PermissionCardItem(
    val title: String,
    val description: String,
    val permissionState: PermissionState,
    val isMandatory: Boolean,
    val onToggleChange: (Boolean) -> Unit,
)

@Composable
fun PermissionCard(
    items: List<PermissionCardItem>,
    cardContainer: @Composable (modifier: Modifier, content: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    cardContainer(modifier) {
        Column {
            items.forEachIndexed { index, item ->
                PermissionItem(
                    title = item.title,
                    description = item.description,
                    permissionState = item.permissionState,
                    isMandatory = item.isMandatory,
                    onToggleChange = item.onToggleChange,
                )

                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = DesignTokens.SpacingXLarge),
                        thickness = DesignTokens.DividerThickness,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

/**
 * An item component that displays a permission with its title, description, and toggle/status.
 * Intended to be used inside a Card or other container.
 */
@Composable
fun PermissionItem(
    title: String,
    description: String,
    permissionState: PermissionState,
    isMandatory: Boolean,
    onToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(DesignTokens.SpacingXLarge),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isMandatory) {
                    Text(
                        text = stringResource(R.string.permissions_mandatory_indicator),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (permissionState.isGranted) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.permissions_granted),
                tint = AppColors.ActionPhone,
                modifier =
                    Modifier
                        .padding(start = DesignTokens.SpacingLarge)
                        .size(DesignTokens.IconSize),
            )
        } else {
            Switch(
                checked = false,
                onCheckedChange = { newValue ->
                    hapticToggle(view)()
                    if (newValue) {
                        // User wants to enable - request permission
                        onToggleChange(true)
                    } else if (!isMandatory) {
                        // User wants to disable optional permission (just update UI state)
                        onToggleChange(false)
                    }
                },
                modifier = Modifier.scale(0.85f).padding(start = DesignTokens.SpacingLarge),
                colors = SwitchDefaults.colors(uncheckedTrackColor = Color.Transparent),
            )
        }
    }
}
