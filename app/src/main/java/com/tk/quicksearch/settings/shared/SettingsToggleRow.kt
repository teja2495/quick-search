package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.shared.ui.components.SliderWithValue
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.tk.quicksearch.shared.ui.components.AppSlider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggleState
import com.tk.quicksearch.shared.ui.components.SegmentedTileContentPadding
import com.tk.quicksearch.shared.ui.components.SegmentedTileToggleHorizontalPadding
import com.tk.quicksearch.shared.ui.components.LocalInSegmentedTile
import com.tk.quicksearch.shared.ui.components.AppSwitch
import com.tk.quicksearch.shared.ui.components.TonalIconBadge

data class SettingsToggleSliderDetails(
    val value: Float,
    val onValueChange: (Float) -> Unit,
    val valueRange: ClosedFloatingPointRange<Float>,
    val valueLabel: String,
    val description: String? = null,
    val steps: Int = 0,
    val valueLabelWidth: Dp = DesignTokens.SpacingXXLarge,
)

/**
 * Reusable toggle row component for settings cards.
 * Provides consistent styling and layout across all toggle rows.
 *
 * @param showNavigationChevron With [onRowClick], marks the text as a link to a detail page with a
 *   chevron and divider before the switch, like [SettingsNavigationToggleRow].
 */
@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRowClick: (() -> Unit)? = null,
    onDisabledClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    titleContent: (@Composable () -> Unit)? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    sliderDetails: SettingsToggleSliderDetails? = null,
    showSwitch: Boolean = true,
    showNavigationChevron: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    titleTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    horizontalPadding: Dp = DesignTokens.SpacingXXLarge,
    leadingIconSize: Dp = DesignTokens.IconSizeSmall,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    extraVerticalPadding: Dp = 0.dp,
    showDivider: Boolean = true,
    showTipBanner: Boolean = false,
    tipBannerText: String? = null,
    tipBannerLinkText: String? = null,
    onTipBannerLinkClick: (() -> Unit)? = null,
    onTipBannerDismiss: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val segmented = LocalInSegmentedTile.current
    // A tile has its own inset; the card defaults (24dp sides, first/last padding) apply only on a card.
    val horizontalPadding =
        if (segmented && horizontalPadding == DesignTokens.SpacingXXLarge) SegmentedTileToggleHorizontalPadding else horizontalPadding
    // Tiles share one height rhythm, so per-card spacing tweaks (extraVerticalPadding) don't apply.
    val topPadding =
        if (segmented) SegmentedTileContentPadding else DesignTokens.cardItemTopPadding(isFirstItem) + extraVerticalPadding
    val bottomPadding =
        if (segmented) SegmentedTileContentPadding else DesignTokens.cardItemBottomPadding(isLastItem) + extraVerticalPadding
    val onToggle: (Boolean) -> Unit = { newValue ->
        if (enabled) {
            hapticToggleState(view, newValue)
            onCheckedChange(newValue)
        }
    }

    // A slider or tip banner that comes and goes with the switch grows and shrinks the row.
    Column(modifier = Modifier.animateContentSize(animationSpec = AppMotion.defaultSpatial())) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .then(
                        if (onRowClick != null) {
                            Modifier.clickable(
                                role = Role.Button,
                                enabled = enabled || onDisabledClick != null,
                                onClick = {
                                    if (enabled) {
                                        onRowClick()
                                    } else {
                                        onDisabledClick?.invoke()
                                    }
                                },
                            )
                        } else if (sliderDetails != null) {
                            Modifier
                        } else {
                            if (enabled) {
                                Modifier.toggleable(
                                    value = checked,
                                    role = Role.Switch,
                                    onValueChange = onToggle,
                                )
                            } else if (onDisabledClick != null) {
                                Modifier.clickable(role = Role.Button, onClick = onDisabledClick)
                            } else {
                                Modifier
                            }
                        },
                    )
                    .padding(
                        start = horizontalPadding,
                        top = topPadding,
                        end = horizontalPadding,
                        bottom = bottomPadding,
                    ),
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
                        modifier = Modifier.size(leadingIconSize),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing),
            ) {
                if (titleContent != null) {
                    titleContent()
                } else {
                    Text(
                        text = title,
                        style = titleTextStyle,
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.DisabledContentAlpha)
                            },
                    )
                }

                if (sliderDetails != null) {
                    sliderDetails.description?.let { description ->
                        Text(
                            text = description,
                            style = subtitleTextStyle,
                            color =
                                if (enabled) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.DisabledContentAlpha)
                                },
                        )
                    }
                    SliderWithValue(
                        modifier = Modifier.fillMaxWidth(),
                        valueSpacing = DesignTokens.SpacingLarge,
                        valueLabel = {
                            Text(
                                text = sliderDetails.valueLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(sliderDetails.valueLabelWidth),
                            )
                        },
                    ) { sliderModifier ->
                        AppSlider(
                            value = sliderDetails.value,
                            onValueChange = sliderDetails.onValueChange,
                            valueRange = sliderDetails.valueRange,
                            steps = sliderDetails.steps,
                            enabled = enabled,
                            modifier = sliderModifier,
                        )
                    }
                } else {
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = subtitleTextStyle,
                            color =
                                if (enabled) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.DisabledContentAlpha)
                                },
                        )
                    }

                    subtitleContent?.invoke()
                }
            }

            if (showNavigationChevron && onRowClick != null && enabled) {
                Row(
                    modifier = Modifier.offset(x = DesignTokens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.ChevronRight,
                        contentDescription = stringResource(R.string.desc_navigate_forward),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VerticalDivider(
                        modifier = Modifier.height(24.dp).padding(horizontal = DesignTokens.SpacingSmall),
                        color = AppColors.SettingsDivider,
                    )
                }
            }
            if (showSwitch) {
                AppSwitch(checked = checked, onCheckedChange = onToggle, enabled = enabled)
            }
            trailingAction?.invoke()
        }

        if (showTipBanner && !tipBannerText.isNullOrBlank()) {
            val annotatedTipText =
                buildAnnotatedString {
                    append(tipBannerText)
                    val linkText = tipBannerLinkText
                    if (!linkText.isNullOrBlank()) {
                        val start = tipBannerText.indexOf(linkText)
                        if (start >= 0) {
                            val end = start + linkText.length
                            addStyle(
                                style =
                                    SpanStyle(
                                        color = AppColors.LinkColor,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                start = start,
                                end = end,
                            )
                        }
                    }
                }
            TipBanner(
                annotatedText = annotatedTipText,
                showDismissButton = onTipBannerDismiss != null,
                onDismiss = onTipBannerDismiss,
                onContentClick = onTipBannerLinkClick,
                modifier =
                    Modifier.padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        bottom = DesignTokens.SpacingLarge,
                    ),
            )
        }

        if (showDivider && !isLastItem && !segmented) {
            HorizontalDivider(
                color = AppColors.SettingsDivider,
            )
        }
    }
}
