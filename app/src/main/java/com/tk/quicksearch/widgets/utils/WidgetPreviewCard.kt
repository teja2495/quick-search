package com.tk.quicksearch.widgets.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonIcon
import com.tk.quicksearch.widgets.searchWidget.MicAction
import kotlin.math.floor

@Composable
fun WidgetPreviewCard(
    state: WidgetPreferences,
    widgetVariant: WidgetVariant = WidgetVariant.STANDARD,
) {
    val previewState = state.enforceVariantConstraints(widgetVariant)
    val context = LocalContext.current
    val colors = calculatePreviewColors(previewState)
    val borderShape = RoundedCornerShape(previewState.borderRadiusDp.dp)
    val shouldShowBorder = previewState.borderWidthDp >= WidgetConfigConstants.BORDER_VISIBILITY_THRESHOLD
    val iconPackPackage =
        remember(context) {
            UserAppPreferences(context).uiPreferences.getSelectedIconPackPackage()
        }
    val previewWidth = WidgetLayoutUtils.DEFAULT_WIDTH_DP.dp
    val outerHorizontalPadding =
        computeSafePreviewOuterPadding(
            previewWidth = previewWidth,
            requestedPaddingDp = previewState.internalHorizontalPaddingDp,
        )
    val verticalInset = previewState.internalVerticalPaddingDp.finiteOr(0f).dp
    val innerHorizontalPadding = WidgetConfigConstants.PREVIEW_INNER_PADDING
    val previewBarHeight = (WidgetConfigConstants.PREVIEW_HEIGHT - (verticalInset * 2)).coerceAtLeast(1.dp)
    val customButtons = previewState.customButtons.filterNotNull()

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = WidgetConfigConstants.PREVIEW_TOP_PADDING,
                    bottom = WidgetConfigConstants.PREVIEW_BOTTOM_PADDING,
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = outerHorizontalPadding, end = outerHorizontalPadding)
                    .height(previewBarHeight)
                    .background(colors.background, shape = borderShape)
                    .then(
                        if (shouldShowBorder) {
                            Modifier.border(
                                width = previewState.borderWidthDp.dp,
                                color = colors.border,
                                shape = borderShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
        ) {
            if (widgetVariant == WidgetVariant.STANDARD && previewState.iconAlignLeft) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = innerHorizontalPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    if (previewState.showLabel) {
                        Text(
                            text = stringResource(R.string.widget_label_text),
                            color = colors.textIcon,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    if (previewState.showSearchIcon) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_widget_search),
                                contentDescription = stringResource(R.string.desc_search_icon),
                                tint = colors.textIcon,
                                modifier = Modifier.size(WidgetConfigConstants.PREVIEW_ICON_SIZE),
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = innerHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (widgetVariant == WidgetVariant.STANDARD && previewState.showSearchIcon) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_widget_search),
                            contentDescription = stringResource(R.string.desc_search_icon),
                            tint = colors.textIcon,
                            modifier = Modifier.size(WidgetConfigConstants.PREVIEW_ICON_SIZE),
                        )
                    }
                    if (widgetVariant == WidgetVariant.STANDARD && previewState.showLabel) {
                        Text(
                            text = stringResource(R.string.widget_label_text),
                            modifier =
                                Modifier.padding(
                                    start =
                                        if (previewState.showSearchIcon) {
                                            WidgetConfigConstants.PREVIEW_ICON_TEXT_SPACING
                                        } else {
                                            0.dp
                                        },
                                ),
                            color = colors.textIcon,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            when (widgetVariant) {
                WidgetVariant.STANDARD -> {
                    if (customButtons.isNotEmpty() || previewState.micAction != MicAction.OFF) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(end = innerHorizontalPadding),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(WidgetConfigConstants.CUSTOM_BUTTON_SPACING),
                            ) {
                                customButtons.forEach { action ->
                                    Box(
                                        modifier = Modifier.size(36.dp), // Match micTouchSpace from actual widget
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CustomWidgetButtonIcon(
                                            action = action,
                                            iconSize = WidgetConfigConstants.PREVIEW_ICON_SIZE,
                                            iconPackPackage = iconPackPackage,
                                            tintColor = colors.textIcon,
                                        )
                                    }
                                }
                                if (previewState.micAction != MicAction.OFF) {
                                    Box(
                                        modifier = Modifier.size(36.dp), // Match micTouchSpace from actual widget
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_widget_mic),
                                            contentDescription = stringResource(R.string.desc_voice_search_icon),
                                            tint = colors.textIcon,
                                            modifier = Modifier.size(WidgetConfigConstants.PREVIEW_ICON_SIZE),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                WidgetVariant.CUSTOM_BUTTONS_ONLY -> {
                    val placeholderIcons =
                        listOf(
                            R.drawable.ic_widget_contact,
                            R.drawable.ic_widget_apps,
                            R.drawable.ic_widget_folder,
                            R.drawable.ic_widget_file,
                            R.drawable.ic_widget_shortcut,
                            R.drawable.ic_widget_settings,
                        )
                    val touchSpace = if (customButtons.size >= 5 || customButtons.isEmpty()) 28.dp else 36.dp
                    val containerWidth =
                        (previewWidth.value - (outerHorizontalPadding.value * 2f)).coerceAtLeast(0f)
                    val availableWidth =
                        (containerWidth - (innerHorizontalPadding.value * 2f)).coerceAtLeast(0f)
                    val minimumGap = if (touchSpace == 28.dp) 4.dp else 8.dp
                    val maxVisibleButtons =
                        if (customButtons.isEmpty()) {
                            0
                        } else {
                            floor((availableWidth - minimumGap.value) / (touchSpace.value + minimumGap.value))
                                .toInt()
                                .coerceIn(0, customButtons.size)
                        }
                    val visibleButtons = customButtons.take(maxVisibleButtons)
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = innerHorizontalPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            if (visibleButtons.isNotEmpty()) {
                                visibleButtons.forEach { action ->
                                    Box(
                                        modifier = Modifier.size(touchSpace),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CustomWidgetButtonIcon(
                                            action = action,
                                            iconSize = WidgetConfigConstants.PREVIEW_ICON_SIZE,
                                            iconPackPackage = iconPackPackage,
                                            tintColor = colors.textIcon,
                                        )
                                    }
                                }
                            } else {
                                placeholderIcons.forEach { iconRes ->
                                    Box(
                                        modifier = Modifier.size(touchSpace),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = null,
                                            tint = colors.textIcon,
                                            modifier = Modifier.size(WidgetConfigConstants.PREVIEW_ICON_SIZE),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun computeSafePreviewOuterPadding(
    previewWidth: androidx.compose.ui.unit.Dp,
    requestedPaddingDp: Float,
): androidx.compose.ui.unit.Dp {
    val safeWidth = previewWidth.value.finiteOr(WidgetLayoutUtils.DEFAULT_WIDTH_DP).coerceAtLeast(1f)
    val requested = requestedPaddingDp.finiteOr(0f).coerceAtLeast(0f)
    val maxPadding = ((safeWidth - PREVIEW_MIN_RENDERABLE_WIDTH_DP) / 2f).coerceAtLeast(0f)
    return requested.coerceAtMost(maxPadding).dp
}

private fun Float.finiteOr(default: Float): Float = if (isFinite()) this else default

private const val PREVIEW_MIN_RENDERABLE_WIDTH_DP = 48f

private data class PreviewColors(
    val background: Color,
    val border: Color,
    val textIcon: Color,
)

@Composable
private fun calculatePreviewColors(state: WidgetPreferences): PreviewColors {
    val isSystemInDarkTheme = isSystemInDarkTheme()

    // Determine effective theme based on user selection
    val effectiveTheme =
        when (state.theme) {
            WidgetTheme.SYSTEM -> if (isSystemInDarkTheme) WidgetTheme.DARK else WidgetTheme.LIGHT
            else -> state.theme
        }

    val customBackgroundColor = state.backgroundColor?.let(::Color)
    val background =
        customBackgroundColor?.copy(alpha = state.backgroundAlpha)
            ?: WidgetColorUtils.getBackgroundColor(
                effectiveTheme,
                state.backgroundAlpha,
            )
    val border = WidgetColorUtils.getBorderColor(state.borderColor, state.borderAlpha)
    val textIcon =
        WidgetColorUtils.getTextIconColor(
            state.theme,
            state.backgroundAlpha,
            state.textIconColorOverride,
            customBackgroundColor = customBackgroundColor,
            isSystemInDarkTheme,
        )

    return PreviewColors(
        background = background,
        border = border,
        textIcon = textIcon,
    )
}
