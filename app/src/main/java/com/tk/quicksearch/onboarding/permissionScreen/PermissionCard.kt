package com.tk.quicksearch.onboarding.permissionScreen

import com.tk.quicksearch.settings.shared.SettingsSwitch
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.settings.shared.SettingsIconBadge
import com.tk.quicksearch.settings.shared.SettingsSegment
import com.tk.quicksearch.settings.shared.SettingsSegmentRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle

data class PermissionCardItem(
    val title: String,
    val description: String,
    val permissionState: PermissionState,
    val isMandatory: Boolean,
    val icon: ImageVector = MaterialSymbols.Settings,
    val onToggleChange: (Boolean) -> Unit,
    val isNavigationItem: Boolean = false,
    val onNavigationClick: () -> Unit = {},
    /** Opens the matching system settings page when the permission is already granted. */
    val onGrantedClick: (() -> Unit)? = null,
)

@Composable
fun PermissionCard(
    items: List<PermissionCardItem>,
    cardContainer: @Composable (modifier: Modifier, content: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    itemContentPadding: PaddingValues = PaddingValues(DesignTokens.SpacingXLarge),
    internalScrollEnabled: Boolean = false,
    segmented: Boolean = false,
) {
    cardContainer(modifier) {
        Column(
            modifier =
                if (internalScrollEnabled) {
                    Modifier.verticalScroll(rememberScrollState())
                } else {
                    Modifier
                },
            verticalArrangement = Arrangement.spacedBy(if (segmented) DesignTokens.SegmentedListGap else 0.dp),
        ) {
            items.forEachIndexed { index, item ->
                if (segmented) {
                    SettingsSegment(index = index, count = items.size) {
                        SegmentedPermissionItem(item)
                    }
                } else {
                    PermissionItem(
                        title = item.title,
                        description = item.description,
                        permissionState = item.permissionState,
                        isMandatory = item.isMandatory,
                        onToggleChange = item.onToggleChange,
                        isNavigationItem = item.isNavigationItem,
                        onNavigationClick = item.onNavigationClick,
                        onGrantedClick = item.onGrantedClick,
                        contentPadding = itemContentPadding,
                    )

                }

                if (!segmented && index < items.lastIndex) {
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
    isNavigationItem: Boolean,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    onGrantedClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(DesignTokens.SpacingXLarge),
) {
    val view = LocalView.current
    val grantedClick = onGrantedClick?.takeIf { !isNavigationItem && permissionState.isGranted }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    enabled = isNavigationItem || grantedClick != null,
                    onClick = grantedClick ?: onNavigationClick,
                )
                .padding(contentPadding),
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

        if (isNavigationItem) {
            Icon(
                imageVector = MaterialSymbols.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .padding(start = DesignTokens.SpacingLarge)
                        .size(DesignTokens.IconSize),
            )
        } else if (permissionState.isGranted) {
            Icon(
                imageVector = MaterialSymbols.CheckCircle,
                contentDescription = stringResource(R.string.permissions_granted),
                tint = AppColors.ActionPhone,
                modifier =
                    Modifier
                        .padding(start = DesignTokens.SpacingLarge)
                        .size(DesignTokens.IconSize),
            )
        } else {
            SettingsSwitch(
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
                modifier = Modifier.padding(start = DesignTokens.SpacingLarge),
            )
        }
    }
}

/** Settings-only presentation; onboarding retains its existing permission layout. */
@Composable
private fun SegmentedPermissionItem(item: PermissionCardItem) {
    val view = LocalView.current
    val granted = item.permissionState.isGranted
    val onClick = when {
        item.isNavigationItem -> item.onNavigationClick
        granted -> item.onGrantedClick
        else -> null
    }
    val interactionModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else if (!granted) {
        Modifier.toggleable(value = false, role = Role.Switch) {
            hapticToggle(view)()
            item.onToggleChange(true)
        }
    } else Modifier
    SettingsSegmentRow(
        title = item.title,
        description = item.description,
        modifier = interactionModifier,
        leading = { SettingsIconBadge(item.icon) },
        trailing = {
            when {
                item.isNavigationItem -> Icon(MaterialSymbols.ChevronRight, contentDescription = null)
                granted -> Icon(
                    MaterialSymbols.CheckCircle,
                    contentDescription = stringResource(R.string.permissions_granted),
                    tint = AppColors.Accent,
                    modifier = Modifier.size(DesignTokens.IconSize),
                )
                else -> SettingsSwitch(checked = false, onCheckedChange = null)
            }
        },
    )
}
