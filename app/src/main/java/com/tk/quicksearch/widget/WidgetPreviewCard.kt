package com.tk.quicksearch.widget

import android.content.res.Configuration
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.widget.customButtons.CustomWidgetButtonIcon
import com.tk.quicksearch.widget.voiceSearch.MicAction

@Composable
fun WidgetPreviewCard(state: QuickSearchWidgetPreferences) {
    val context = LocalContext.current
    val colors = calculatePreviewColors(state)
    val borderShape = RoundedCornerShape(state.borderRadiusDp.dp)
    val shouldShowBorder = state.borderWidthDp >= WidgetConfigConstants.BORDER_VISIBILITY_THRESHOLD
    val iconPackPackage =
        remember(context) {
            UserAppPreferences(context).uiPreferences.getSelectedIconPackPackage()
        }
    val customButtons = state.customButtons.filterNotNull()

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
                    .height(WidgetConfigConstants.PREVIEW_HEIGHT)
                    .background(colors.background, shape = borderShape)
                    .then(
                        if (shouldShowBorder) {
                            Modifier.border(
                                width = state.borderWidthDp.dp,
                                color = colors.border,
                                shape = borderShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
        ) {
            if (state.iconAlignLeft) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = WidgetConfigConstants.PREVIEW_INNER_PADDING),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.showLabel) {
                        Text(
                            text = stringResource(R.string.widget_label_text),
                            color = colors.textIcon,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    if (state.showSearchIcon) {
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
                            .padding(horizontal = WidgetConfigConstants.PREVIEW_INNER_PADDING),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (state.showSearchIcon) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_widget_search),
                            contentDescription = stringResource(R.string.desc_search_icon),
                            tint = colors.textIcon,
                            modifier = Modifier.size(WidgetConfigConstants.PREVIEW_ICON_SIZE),
                        )
                    }
                    if (state.showLabel) {
                        Text(
                            text = stringResource(R.string.widget_label_text),
                            modifier =
                                Modifier.padding(
                                    start = if (state.showSearchIcon) WidgetConfigConstants.PREVIEW_ICON_TEXT_SPACING else 0.dp,
                                ),
                            color = colors.textIcon,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            if (customButtons.isNotEmpty() || state.micAction != MicAction.OFF) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(end = WidgetConfigConstants.PREVIEW_INNER_PADDING),
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
                        if (state.micAction != MicAction.OFF) {
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
    }
}

private data class PreviewColors(
    val background: Color,
    val border: Color,
    val textIcon: Color,
)

@Composable
private fun calculatePreviewColors(state: QuickSearchWidgetPreferences): PreviewColors {
    val isSystemInDarkTheme = isSystemInDarkTheme()

    // Determine effective theme based on user selection
    val effectiveTheme =
        when (state.theme) {
            WidgetTheme.SYSTEM -> if (isSystemInDarkTheme) WidgetTheme.DARK else WidgetTheme.LIGHT
            else -> state.theme
        }

    val background =
        WidgetColorUtils.getBackgroundColor(
            effectiveTheme,
            state.backgroundAlpha,
        )
    val border = WidgetColorUtils.getBorderColor(state.borderColor, state.backgroundAlpha)
    val textIcon =
        WidgetColorUtils.getTextIconColor(
            state.theme,
            state.backgroundAlpha,
            state.textIconColorOverride,
            isSystemInDarkTheme,
        )

    return PreviewColors(
        background = background,
        border = border,
        textIcon = textIcon,
    )
}
