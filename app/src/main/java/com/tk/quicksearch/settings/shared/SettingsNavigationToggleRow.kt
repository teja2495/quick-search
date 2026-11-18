package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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

/**
 * A settings row with a leading icon, title/subtitle text, an optional navigation arrow,
 * and a toggle switch. Matches the visual style of search result type toggle rows.
 *
 * When [onRowClick] is provided, a chevron and vertical divider appear between the
 * navigation area and the toggle switch. Inside a [SegmentedTile] it takes the tile layout.
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
    noRippleOnRowClick: Boolean = false,
    subtitleContent: (@Composable () -> Unit)? = null,
) {
    val view = LocalView.current
    val rowInteractionSource = remember { MutableInteractionSource() }
    val rowIndication = LocalIndication.current
    // In a segmented tile: tonal icon badge, tile padding, and bodyMedium supporting text.
    val segmented = LocalInSegmentedTile.current

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.55f)
                .let { rowModifier ->
                    if (onRowClick != null && enabled) {
                        rowModifier.clickable(
                            interactionSource = rowInteractionSource,
                            indication = if (noRippleOnRowClick) null else rowIndication,
                            onClick = onRowClick,
                        )
                    } else {
                        rowModifier
                    }
                }
                .then(
                    if (segmented) {
                        Modifier.heightIn(min = SettingsNavigationToggleTileMinHeight).padding(SegmentedTileContentPadding)
                    } else {
                        Modifier.padding(
                            horizontal = DesignTokens.CardHorizontalPadding,
                            vertical = DesignTokens.CardVerticalPadding,
                        )
                    },
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (segmented) SegmentedTileContentPadding else DesignTokens.ItemRowSpacing),
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

                subtitleContent?.invoke()
            }
        }

        if (onRowClick != null) {
            Row(
                modifier = Modifier.offset(x = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MaterialSymbols.ChevronRight,
                    contentDescription = stringResource(R.string.desc_navigate_forward),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = DesignTokens.SpacingSmall),
                    color = AppColors.SettingsDivider,
                )
            }
        }

        AppSwitch(
            checked = checked,
            onCheckedChange = { enabled ->
                hapticToggleState(view, enabled)
                onCheckedChange(enabled)
            },
            enabled = enabled,
        )
    }
}

private val SettingsNavigationToggleTileMinHeight = 72.dp
