package com.tk.quicksearch.tools.directSearch

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.search.searchScreen.shared.InformationCard
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark

@Composable
internal fun GeminiResultCard(
        showWallpaperBackground: Boolean,
        showAttribution: Boolean,
        usedModelId: String?,
        isAttributionClickable: Boolean = false,
        onGeminiModelInfoClick: () -> Unit = {},
        onOpenDirectSearchConfigure: () -> Unit = {},
        content: @Composable () -> Unit,
) {
    ColumnWithContent(
            showWallpaperBackground = showWallpaperBackground,
            content = content,
            showAttribution = showAttribution,
            usedModelId = usedModelId,
            isAttributionClickable = isAttributionClickable,
            onGeminiModelInfoClick = onGeminiModelInfoClick,
            onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
    )
}

@Composable
private fun ColumnWithContent(
        showWallpaperBackground: Boolean,
        content: @Composable () -> Unit,
        showAttribution: Boolean,
        usedModelId: String?,
        isAttributionClickable: Boolean,
        onGeminiModelInfoClick: () -> Unit,
        onOpenDirectSearchConfigure: () -> Unit,
) {
    androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        InformationCard(
                modifier = Modifier.fillMaxWidth().heightIn(min = 175.dp),
                showWallpaperBackground = showWallpaperBackground,
        ) {
            content()
        }

        if (showAttribution) {
            GeminiAttributionRow(
                    modifier = Modifier.fillMaxWidth(),
                    usedModelId = usedModelId,
                    isClickable = isAttributionClickable,
                    onClick = onGeminiModelInfoClick,
                    onLongClick = onOpenDirectSearchConfigure,
            )
        }
    }
}

@Composable
private fun informationAttributionContentColor(): Color {
    val imageBackgroundIsDark = LocalImageBackgroundIsDark.current
    return when (imageBackgroundIsDark) {
        true -> Color.White
        false -> Color.Black
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/** Attribution row showing powered by Gemini or Gemma branding. */
@Composable
internal fun GeminiAttributionRow(
        modifier: Modifier = Modifier,
        usedModelId: String? = null,
        isClickable: Boolean = false,
        onClick: () -> Unit = {},
        onLongClick: () -> Unit = {},
) {
    val contentColor = informationAttributionContentColor()
    val poweredByText = stringResource(R.string.direct_search_powered_by)
    val isGemma = usedModelId?.lowercase()?.startsWith("gemma-") == true
    val logoRes = if (isGemma) R.drawable.gemma_logo else R.drawable.gemini_logo
    val logoAspectRatio = if (isGemma) 250f / 64f else 288f / 65f
    val logoHeight = if (isGemma) 20.dp else DesignTokens.SpacingLarge
    val rowModifier =
            if (isClickable) {
                modifier
                        .padding(horizontal = DesignTokens.SpacingLarge)
                        .combinedClickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null,
                                onClick = onClick,
                                onLongClick = onLongClick,
                        )
            } else {
                modifier.padding(horizontal = DesignTokens.SpacingLarge)
            }
    Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
                text = poweredByText,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
        )
        Image(
                painter = painterResource(id = logoRes),
                contentDescription = poweredByText,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(logoHeight).aspectRatio(logoAspectRatio),
        )
    }
}

/** Attribution row showing calculator branding. */
@Composable
internal fun CalculatorAttributionRow(
        modifier: Modifier = Modifier,
        toolType: SearchToolType = SearchToolType.CALCULATOR,
) {
    val contentColor = informationAttributionContentColor()
    val titleRes =
            when (toolType) {
                SearchToolType.UNIT_CONVERTER -> R.string.unit_converter_info_title
                SearchToolType.DATE_CALCULATOR -> R.string.date_calculator_info_title
                else -> R.string.calculator_toggle_title
            }
    val icon =
            when (toolType) {
                SearchToolType.UNIT_CONVERTER -> Icons.Rounded.Straighten
                SearchToolType.DATE_CALCULATOR -> Icons.Rounded.CalendarMonth
                else -> Icons.Rounded.Calculate
            }
    Row(
            modifier = modifier.padding(horizontal = DesignTokens.SpacingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
                imageVector = icon,
                contentDescription = stringResource(titleRes),
                tint = contentColor,
                modifier = Modifier.size(14.dp),
        )
        Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
        )
    }
}
