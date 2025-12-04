package com.tk.quicksearch.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R

/**
 * Preview card showing how the widget will look with current settings.
 */
@Composable
fun WidgetPreviewCard(state: QuickSearchWidgetPreferences) {
    val colors = calculatePreviewColors(state)
    val borderShape = RoundedCornerShape(state.borderRadiusDp.dp)
    val shouldShowBorder = state.borderWidthDp >= WidgetConfigConstants.BORDER_VISIBILITY_THRESHOLD

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WidgetConfigConstants.PREVIEW_CARD_PADDING)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WidgetConfigConstants.PREVIEW_HEIGHT)
                    .background(colors.background, shape = borderShape)
                    .then(
                        if (shouldShowBorder) {
                            Modifier.border(
                                width = state.borderWidthDp.dp,
                                color = colors.border,
                                shape = borderShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = WidgetConfigConstants.PREVIEW_INNER_PADDING),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_widget_search),
                    contentDescription = stringResource(R.string.desc_search_icon),
                    tint = colors.textIcon,
                    modifier = Modifier.size(WidgetConfigConstants.PREVIEW_ICON_SIZE)
                )
                if (state.showLabel) {
                    Text(
                        text = stringResource(R.string.widget_label_text),
                        modifier = Modifier.padding(start = WidgetConfigConstants.PREVIEW_ICON_TEXT_SPACING),
                        color = colors.textIcon,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Data class holding calculated colors for widget preview.
 */
private data class PreviewColors(
    val background: Color,
    val border: Color,
    val textIcon: Color
)

/**
 * Calculates colors for widget preview based on current state.
 */
@Composable
private fun calculatePreviewColors(state: QuickSearchWidgetPreferences): PreviewColors {
    val background = WidgetColorUtils.getBackgroundColor(
        state.backgroundColorIsWhite,
        state.backgroundAlpha
    )
    val border = WidgetColorUtils.getBorderColor(state.borderColor, state.backgroundAlpha)
    val textIcon = WidgetColorUtils.getTextIconColor(
        state.borderColor,
        state.backgroundColorIsWhite,
        state.backgroundAlpha
    )
    
    return PreviewColors(
        background = background,
        border = border,
        textIcon = textIcon
    )
}


