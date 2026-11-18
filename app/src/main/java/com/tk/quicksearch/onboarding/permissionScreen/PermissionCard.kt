package com.tk.quicksearch.onboarding.permissionScreen

import com.tk.quicksearch.shared.ui.components.AppSwitch
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.widgetsPanel.widgetPanelEdgeFade
import com.tk.quicksearch.shared.ui.components.TonalIconBadge

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
    showItemIcons: Boolean = true,
) {
    cardContainer(modifier) {
        val scrollState = rememberScrollState()
        Column(
            modifier =
                if (internalScrollEnabled) {
                    Modifier
                        .widgetPanelEdgeFade(scrollState)
                        .verticalScroll(scrollState)
                } else {
                    Modifier
                },
            verticalArrangement = Arrangement.spacedBy(if (segmented) DesignTokens.SegmentedListGap else 0.dp),
        ) {
            items.forEachIndexed { index, item ->
                if (segmented) {
                    SegmentedTile(index = index, count = items.size) {
                        SegmentedPermissionItem(item, showIcon = showItemIcons)
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
    val interactionModifier = when {
        isNavigationItem -> Modifier.clickable(role = Role.Button, onClick = onNavigationClick)
        !permissionState.isGranted -> Modifier.toggleable(value = false, role = Role.Switch) {
            hapticToggle(view)()
            onToggleChange(true)
        }
        onGrantedClick != null -> Modifier.clickable(role = Role.Button, onClick = onGrantedClick)
        else -> Modifier
    }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(interactionModifier)
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
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleMedium,
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
            AppSwitch(
                checked = false,
                onCheckedChange = null,
                modifier = Modifier.padding(start = DesignTokens.SpacingLarge),
            )
        }
    }
}

/** Settings-only presentation; onboarding retains its existing permission layout. */
@Composable
private fun SegmentedPermissionItem(
    item: PermissionCardItem,
    showIcon: Boolean,
) {
    val view = LocalView.current
    val granted = item.permissionState.isGranted
    val onClick = when {
        item.isNavigationItem -> item.onNavigationClick
        granted -> item.onGrantedClick
        else -> null
    }
    val interactionModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else if (!granted) {
        Modifier.toggleable(value = false, role = Role.Switch) {
            hapticToggle(view)()
            item.onToggleChange(true)
        }
    } else Modifier
    SegmentedTileRow(
        title = item.title,
        description = item.description,
        modifier = interactionModifier,
        leading = if (showIcon) ({ TonalIconBadge(item.icon) }) else null,
        trailing = {
            when {
                item.isNavigationItem -> Icon(MaterialSymbols.ChevronRight, contentDescription = null)
                granted -> Icon(
                    MaterialSymbols.CheckCircle,
                    contentDescription = stringResource(R.string.permissions_granted),
                    tint = AppColors.Accent,
                    modifier = Modifier.size(DesignTokens.IconSize),
                )
                else -> AppSwitch(checked = false, onCheckedChange = null)
            }
        },
    )
}
