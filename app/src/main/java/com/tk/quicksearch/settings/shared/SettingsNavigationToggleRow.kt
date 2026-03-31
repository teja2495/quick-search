package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle

/**
 * A settings row with a leading icon, title/subtitle text, an optional navigation arrow,
 * and a toggle switch. Matches the visual style of search result type toggle rows.
 *
 * When [onRowClick] is provided, a chevron and vertical divider appear between the
 * navigation area and the toggle switch.
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
                .padding(
                    start = DesignTokens.CardHorizontalPadding,
                    end = DesignTokens.CardHorizontalPadding,
                    top = DesignTokens.CardVerticalPadding,
                    bottom = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSize),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
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
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.desc_navigate_forward),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 8.dp),
                    color = AppColors.SettingsDivider,
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = { enabled ->
                hapticToggle(view)()
                onCheckedChange(enabled)
            },
            enabled = enabled,
            modifier = Modifier.scale(0.85f),
            colors = SwitchDefaults.colors(
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
        )
    }
}
