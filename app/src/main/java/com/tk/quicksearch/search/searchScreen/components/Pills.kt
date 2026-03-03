package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
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
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
internal fun KeyboardSwitchPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.4f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(
                        horizontal = DesignTokens.SpacingMedium,
                        vertical = 4.dp,
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
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.4f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(
                        horizontal = DesignTokens.SpacingMedium,
                        vertical = 4.dp,
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
            Spacer(modifier = Modifier.size(4.dp))
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
    modifier: Modifier = Modifier,
) {
    val operators = remember { listOf("+", "-", "*", "/", "(", ")") }
    val containerBackgroundColor =
        if (isOverlayPresentation) {
            MaterialTheme.colorScheme.surface
        } else if (
            MaterialTheme.colorScheme.surface == MaterialTheme.colorScheme.background &&
                MaterialTheme.colorScheme.background.red < 0.1f &&
                MaterialTheme.colorScheme.background.green < 0.1f &&
                MaterialTheme.colorScheme.background.blue < 0.1f
        ) {
            Color.Black.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    val operatorChipColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val operatorChipTextColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = if (extendToScreenEdges) modifier.extendToScreenEdges() else modifier,
        color = containerBackgroundColor,
        shape = RoundedCornerShape(ZeroCornerSize),
        tonalElevation = 0.dp,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            operators.forEach { operator ->
                Surface(
                    modifier =
                        Modifier.weight(1f).clickable { onOperatorClick(operator) },
                    shape = RoundedCornerShape(999.dp),
                    color = operatorChipColor,
                    tonalElevation = 0.dp,
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