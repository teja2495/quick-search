package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle

private const val TipBannerLinkTag = "tip_banner_link"

data class SettingsToggleSliderDetails(
    val value: Float,
    val onValueChange: (Float) -> Unit,
    val valueRange: ClosedFloatingPointRange<Float>,
    val valueLabel: String,
    val steps: Int = 0,
)

/**
 * Reusable toggle row component for settings cards.
 * Provides consistent styling and layout across all toggle rows.
 */
@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRowClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    sliderDetails: SettingsToggleSliderDetails? = null,
    leadingIcon: ImageVector? = null,
    titleTextStyle: TextStyle = MaterialTheme.typography.titleMedium,
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
    val topPadding = DesignTokens.cardItemTopPadding(isFirstItem) + extraVerticalPadding
    val bottomPadding = DesignTokens.cardItemBottomPadding(isLastItem) + extraVerticalPadding
    val onToggle: (Boolean) -> Unit = { newValue ->
        hapticToggle(view)()
        onCheckedChange(newValue)
    }

    Column {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .then(
                        if (onRowClick != null) {
                            Modifier.clickable(onClick = onRowClick)
                        } else if (sliderDetails != null) {
                            Modifier
                        } else {
                            Modifier.toggleable(
                                value = checked,
                                role = Role.Switch,
                                onValueChange = onToggle,
                            )
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
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(leadingIconSize),
                )
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
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (sliderDetails != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                    ) {
                        Slider(
                            value = sliderDetails.value,
                            onValueChange = sliderDetails.onValueChange,
                            valueRange = sliderDetails.valueRange,
                            steps = sliderDetails.steps,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = sliderDetails.valueLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(DesignTokens.SpacingXXLarge),
                        )
                    }
                } else {
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    subtitleContent?.invoke()
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.85f),
                colors =
                    SwitchDefaults.colors(
                        uncheckedTrackColor = Color.Transparent,
                    ),
            )
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
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                start = start,
                                end = end,
                            )
                            addStringAnnotation(
                                tag = TipBannerLinkTag,
                                annotation = linkText,
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
                onTextClick = { clickOffset ->
                    if (annotatedTipText.getStringAnnotations(TipBannerLinkTag, clickOffset, clickOffset).isNotEmpty()) {
                        onTipBannerLinkClick?.invoke()
                    }
                },
                modifier =
                    Modifier.padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        bottom = DesignTokens.SpacingLarge,
                    ),
            )
        }

        if (showDivider && !isLastItem) {
            HorizontalDivider(
                color = AppColors.SettingsDivider,
            )
        }
    }
}
