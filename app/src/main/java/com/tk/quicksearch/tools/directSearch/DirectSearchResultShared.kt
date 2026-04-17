package com.tk.quicksearch.tools.directSearch

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.search.searchScreen.shared.InformationCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
internal fun GeminiResultCard(
        showWallpaperBackground: Boolean,
        showAttribution: Boolean,
        usedModelId: String?,
        llmProviderId: DirectSearchLlmProviderId = DirectSearchLlmProviderId.GEMINI,
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
            llmProviderId = llmProviderId,
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
        llmProviderId: DirectSearchLlmProviderId,
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
                    llmProviderId = llmProviderId,
                    isClickable = isAttributionClickable,
                    onClick = onGeminiModelInfoClick,
                    onLongClick = onOpenDirectSearchConfigure,
            )
        }
    }
}

@Composable
private fun informationAttributionContentColor(): Color =
    AppColors.wallpaperAwareMutedSearchForeground(alpha = 1f)

@Composable
private fun AnthropicClaudeWordmark(
        contentDescription: String,
        modifier: Modifier = Modifier,
        wordmarkTextColor: Color,
) {
    Box(modifier = modifier) {
        Image(
                painter = painterResource(R.drawable.claude_wordmark_mark),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                colorFilter = null,
                modifier = Modifier.fillMaxSize(),
        )
        Image(
                painter = painterResource(R.drawable.claude_wordmark_type),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(wordmarkTextColor),
                modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Attribution row showing powered by Gemini, Gemma, OpenAI, Claude, or Groq branding. */
@Composable
internal fun GeminiAttributionRow(
        modifier: Modifier = Modifier,
        usedModelId: String? = null,
        llmProviderId: DirectSearchLlmProviderId = DirectSearchLlmProviderId.GEMINI,
        isClickable: Boolean = false,
        onClick: () -> Unit = {},
        onLongClick: () -> Unit = {},
) {
    val contentColor = informationAttributionContentColor()
    val poweredByText = stringResource(R.string.direct_search_powered_by)
    val isGemma = usedModelId?.lowercase()?.startsWith("gemma-") == true
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
        when (llmProviderId) {
            DirectSearchLlmProviderId.OPENAI -> {
                Image(
                        painter = painterResource(R.drawable.openai_wordmark),
                        contentDescription = poweredByText,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(contentColor),
                        modifier =
                                Modifier.height(18.dp).aspectRatio(1564.3f / 428.4f),
                )
            }
            DirectSearchLlmProviderId.ANTHROPIC -> {
                AnthropicClaudeWordmark(
                        contentDescription = poweredByText,
                        wordmarkTextColor = contentColor,
                        modifier =
                                Modifier.height(18.dp)
                                        .aspectRatio(689.97997f / 148.17999f),
                )
            }
            DirectSearchLlmProviderId.GROQ -> {
                Image(
                        painter = painterResource(R.drawable.groq_wordmark),
                        contentDescription = poweredByText,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(contentColor),
                        modifier =
                                Modifier.height(18.dp).aspectRatio(152f / 55.5f),
                )
            }
            else -> {
                val logoRes = if (isGemma) R.drawable.gemma_logo else R.drawable.gemini_logo
                val logoAspectRatio = if (isGemma) 250f / 64f else 288f / 65f
                val logoHeight = if (isGemma) 18.dp else 14.dp
                Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = poweredByText,
                        contentScale = ContentScale.Fit,
                        colorFilter = null,
                        modifier = Modifier.height(logoHeight).aspectRatio(logoAspectRatio),
                )
            }
        }
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
