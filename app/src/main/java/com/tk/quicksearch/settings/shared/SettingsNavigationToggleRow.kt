package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggleState
import com.tk.quicksearch.shared.ui.components.SegmentedTileContentPadding
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.LocalInSegmentedTile
import com.tk.quicksearch.shared.ui.components.AppSwitch
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.TonalIconBadgeSize

/**
 * A settings row with a leading icon, title/subtitle text, an optional navigation arrow,
 * and a toggle switch. Matches the visual style of search result type toggle rows.
 *
 * When [onRowClick] is provided, a chevron and vertical divider appear between the
 * navigation area and the toggle switch. Inside a [SegmentedTile] it takes the tile layout.
 * [footerContent] puts a secondary action below the row, aligned with the title.
 */
@Composable
fun SettingsNavigationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    isBeta: Boolean = false,
    tagLabel: String? = null,
    onRowClick: (() -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
) {
    val view = LocalView.current
    val rowInteractionSource = remember { MutableInteractionSource() }
    val rowIndication = LocalIndication.current
    val onToggle: (Boolean) -> Unit = { value ->
        hapticToggleState(view, value)
        onCheckedChange(value)
    }
    // In a segmented tile: tonal icon badge, tile padding, and bodyMedium supporting text.
    val segmented = LocalInSegmentedTile.current

    val horizontalPadding =
        if (segmented) SegmentedTileContentPadding else DesignTokens.CardHorizontalPadding
    val tileMinHeight =
        if (footerContent == null) SettingsNavigationToggleTileMinHeight else DesignTokens.ListItemMinHeight
    Column(modifier = modifier.fillMaxWidth().alpha(if (enabled) 1f else AppColors.DisabledContentAlpha)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .let { rowModifier ->
                        if (onRowClick == null) {
                            rowModifier.toggleable(
                                value = checked,
                                enabled = enabled,
                                role = Role.Switch,
                                onValueChange = onToggle,
                            )
                        } else {
                            rowModifier.clickable(
                                enabled = enabled,
                                role = Role.Button,
                                interactionSource = rowInteractionSource,
                                indication = rowIndication,
                                onClick = onRowClick,
                            )
                        }
                    }
                    .then(
                        if (segmented) {
                            Modifier
                                .heightIn(min = tileMinHeight)
                                .padding(
                                    start = horizontalPadding,
                                    top = if (footerContent == null) SegmentedTileContentPadding else DesignTokens.SpacingSmall,
                                    end = horizontalPadding,
                                    bottom = if (footerContent == null) SegmentedTileContentPadding else DesignTokens.SpacingXSmall,
                                )
                        } else {
                            Modifier.padding(
                                horizontal = horizontalPadding,
                                vertical = DesignTokens.CardVerticalPadding,
                            )
                        },
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
            ) {
                leadingIcon?.let {
                    if (segmented) {
                        TonalIconBadge(icon = it)
                    } else {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(DesignTokens.IconSize),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (isBeta || tagLabel != null) {
                            BetaTagChip(tagText = tagLabel)
                        }
                    }

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = if (segmented) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = if (segmented) DesignTokens.SpacingXSmall / 2 else DesignTokens.SpacingXSmall),
                        )
                    }
                }
            }

            if (onRowClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.ChevronRight,
                        contentDescription = stringResource(R.string.desc_navigate_forward),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VerticalDivider(
                        modifier = Modifier
                            .height(DesignTokens.IconSize)
                            .padding(start = DesignTokens.SpacingXSmall),
                        color = AppColors.SettingsDivider,
                    )
                }
            }

            AppSwitch(
                checked = checked,
                modifier = if (onRowClick != null) {
                    Modifier.semantics { contentDescription = title }
                } else {
                    Modifier
                },
                onCheckedChange = if (onRowClick != null) onToggle else null,
                enabled = enabled,
            )
        }

        if (footerContent != null) {
            val iconWidth = if (segmented) TonalIconBadgeSize else DesignTokens.IconSize
            val titleInset = if (leadingIcon != null) iconWidth + DesignTokens.ItemRowSpacing else 0.dp
            Column(
                modifier = Modifier.padding(
                    start = horizontalPadding + titleInset,
                    end = horizontalPadding,
                    bottom = DesignTokens.SpacingXSmall,
                ),
            ) {
                footerContent()
            }
        }
    }
}

private val SettingsNavigationToggleTileMinHeight = 72.dp
