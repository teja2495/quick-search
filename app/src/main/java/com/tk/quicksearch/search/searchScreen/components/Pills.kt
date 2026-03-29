package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme

@Composable
internal fun KeyboardSwitchPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = LocalAppIsDarkTheme.current
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val labelColor = if (isDarkTheme) Color.White else Color.Black
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor =
        if (isDarkTheme) {
            AppColors.Accent.copy(alpha = 0.22f)
        } else {
            Color.Black.copy(alpha = 0.1f)
        }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = DesignTokens.ShapeFull,
        color = backgroundColor,
        border = BorderStroke(DesignTokens.KeyboardPillBorderStrokeWidth, borderColor),
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(
                        horizontal = DesignTokens.SpacingMedium,
                        vertical = DesignTokens.SpacingXSmall,
                    ).height(DesignTokens.IconSize),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun OpenKeyboardAction(
    text: String,
    onClick: () -> Unit,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = LocalAppIsDarkTheme.current
    val backgroundColor = AppColors.getSearchEngineSectionBackground(showWallpaperBackground)
    val labelColor = if (isDarkTheme) Color.White else Color.Black
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        color = backgroundColor,
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 75.dp)
                    .padding(vertical = DesignTokens.SpacingXSmall),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Keyboard,
                contentDescription = null,
                tint = labelColor,
                modifier = Modifier.size(DesignTokens.IconSize),
            )
            Spacer(modifier = Modifier.size(DesignTokens.SpacingSmall))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun OverlayExpandPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = DesignTokens.ShapeFull,
        color = AppColors.OverlayMedium,
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(
                        horizontal = DesignTokens.SpacingMedium,
                        vertical = DesignTokens.SpacingXSmall,
                    ).height(DesignTokens.IconSize),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.size(DesignTokens.SpacingXSmall))
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = stringResource(R.string.desc_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
internal fun NumberKeyboardOperatorPills(
    onOperatorClick: (String) -> Unit,
    isOverlayPresentation: Boolean = false,
    extendToScreenEdges: Boolean = true,
    showWallpaperBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val operators = remember { listOf("+", "-", "*", "/", "(", ")") }
    val isDarkTheme = LocalAppIsDarkTheme.current
    val containerBackgroundColor =
        when {
            showWallpaperBackground ->
                if (isDarkTheme) Color.Black else Color.White
            isOverlayPresentation -> MaterialTheme.colorScheme.surface
            MaterialTheme.colorScheme.surface == MaterialTheme.colorScheme.background &&
                MaterialTheme.colorScheme.background.red < 0.1f &&
                MaterialTheme.colorScheme.background.green < 0.1f &&
                MaterialTheme.colorScheme.background.blue < 0.1f ->
                AppColors.SearchBarBackground
            else -> MaterialTheme.colorScheme.surface
        }
    val operatorChipColor = AppColors.KeyboardPillBackground
    val operatorChipTextColor = AppColors.KeyboardPillText
    val operatorChipBorderColor = AppColors.SearchChromeOutlineBorder

    Surface(
        modifier = if (extendToScreenEdges) modifier.extendToScreenEdges() else modifier,
        color = containerBackgroundColor,
        shape = RoundedCornerShape(ZeroCornerSize),
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        start = DesignTokens.SpacingLarge,
                        end = DesignTokens.SpacingLarge,
                        top = 9.dp,
                        bottom = 7.dp,
                    ),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            operators.forEach { operator ->
                Surface(
                    modifier =
                        Modifier.weight(1f).clickable { onOperatorClick(operator) },
                    shape = DesignTokens.ShapeFull,
                    color = operatorChipColor,
                    border = BorderStroke(DesignTokens.KeyboardPillBorderStrokeWidth, operatorChipBorderColor),
                    tonalElevation = DesignTokens.ElevationLevel0,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .padding(
                                    vertical = 2.dp,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = operator,
                            style = MaterialTheme.typography.titleMedium,
                            color = operatorChipTextColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
