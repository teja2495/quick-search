package com.tk.quicksearch.tools.aiSearch

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.search.searchScreen.shared.InformationCard
import com.tk.quicksearch.search.searchScreen.shared.InformationCardCopyAction
import com.tk.quicksearch.search.searchScreen.shared.InformationCardFooter
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

internal val AiResultCardMinHeight = 175.dp

@Composable
internal fun GeminiResultCard(
        showWallpaperBackground: Boolean,
        showAttribution: Boolean,
        usedModelId: String?,
        llmProviderId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI,
        isAttributionClickable: Boolean = false,
        onGeminiModelInfoClick: () -> Unit = {},
        onOpenAiSearchConfigure: () -> Unit = {},
        copyText: String? = null,
        toolAttribution: ToolAttribution? = null,
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
            onOpenAiSearchConfigure = onOpenAiSearchConfigure,
            copyText = copyText,
            toolAttribution = toolAttribution,
    )
}

/** Tool branding shown at the bottom of a result card in place of the AI provider attribution. */
internal data class ToolAttribution(
        val icon: ImageVector,
        @param:StringRes val titleRes: Int,
)

@Composable
private fun ColumnWithContent(
        showWallpaperBackground: Boolean,
        content: @Composable () -> Unit,
        showAttribution: Boolean,
        usedModelId: String?,
        llmProviderId: AiSearchLlmProviderId,
        isAttributionClickable: Boolean,
        onGeminiModelInfoClick: () -> Unit,
        onOpenAiSearchConfigure: () -> Unit,
        copyText: String?,
        toolAttribution: ToolAttribution?,
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    // Flat (compact) cards have no container to fill, so they size to the answer.
    val cardMinHeight = if (LocalSearchCardLayoutEnabled.current) AiResultCardMinHeight else 0.dp
    val cardModifier =
            Modifier.fillMaxWidth()
                    .heightIn(min = cardMinHeight)
                    .let { baseModifier ->
                        if (copyText.isNullOrBlank()) {
                            baseModifier
                        } else {
                            baseModifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        clipboardManager.setText(AnnotatedString(copyText))
                                    },
                            )
                        }
                    }

    InformationCard(
            modifier = cardModifier,
            showWallpaperBackground = showWallpaperBackground,
    ) {
        content()
        val copyAnswer = copyText?.takeIf { it.isNotBlank() }
        if (toolAttribution != null || showAttribution || copyAnswer != null) {
            // Pins the footer to the card's bottom edge when the answer is shorter than the minimum height.
            Spacer(modifier = Modifier.weight(1f))
            InformationCardFooter(
                    source = {
                        if (toolAttribution != null) {
                            ToolAttributionRow(icon = toolAttribution.icon, titleRes = toolAttribution.titleRes)
                        } else if (showAttribution) {
                            AiProviderAttributionRow(
                                    usedModelId = usedModelId,
                                    llmProviderId = llmProviderId,
                                    isClickable = isAttributionClickable,
                                    onClick = onGeminiModelInfoClick,
                                    onLongClick = onOpenAiSearchConfigure,
                            )
                        }
                    },
                    actions = { copyAnswer?.let { InformationCardCopyAction(text = it) } },
            )
        }
    }
}

@Composable
private fun informationAttributionContentColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

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

@Composable
private fun GeminiWordmark(
        contentDescription: String,
        modifier: Modifier = Modifier,
        wordmarkTextColor: Color,
) {
    Box(modifier = modifier) {
        Image(
                painter = painterResource(R.drawable.gemini_wordmark_mark),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                colorFilter = null,
                modifier = Modifier.fillMaxSize(),
        )
        Image(
                painter = painterResource(R.drawable.gemini_wordmark_type),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(wordmarkTextColor),
                modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun GemmaWordmark(
        contentDescription: String,
        modifier: Modifier = Modifier,
        wordmarkTextColor: Color,
) {
    Box(modifier = modifier) {
        Image(
                painter = painterResource(R.drawable.gemma_wordmark_mark),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                colorFilter = null,
                modifier = Modifier.fillMaxSize(),
        )
        Image(
                painter = painterResource(R.drawable.gemma_wordmark_type),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(wordmarkTextColor),
                modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Attribution row showing the branding for the provider that generated the answer. */
@Composable
internal fun AiProviderAttributionRow(
        modifier: Modifier = Modifier,
        usedModelId: String? = null,
        llmProviderId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI,
        isClickable: Boolean = false,
        onClick: () -> Unit = {},
        onLongClick: () -> Unit = {},
) {
    val contentColor = informationAttributionContentColor()
    val poweredByText = stringResource(R.string.direct_search_powered_by)
    val isGemma = usedModelId?.lowercase()?.startsWith("gemma-") == true
    val rowModifier =
            if (isClickable) {
                // Full footer height as the touch target, with a rounded ripple around the wordmark.
                // The offset cancels the ripple's side padding so the wordmark stays on the body inset.
                modifier
                        .offset(x = -DesignTokens.SpacingXSmall)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(DesignTokens.SpacingSmall))
                        .combinedClickable(
                                onClick = onClick,
                                onLongClick = onLongClick,
                        )
                        .padding(horizontal = DesignTokens.SpacingXSmall)
            } else {
                modifier
            }
    Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        when (llmProviderId) {
            AiSearchLlmProviderId.OPENAI -> {
                Image(
                        painter = painterResource(R.drawable.openai_wordmark),
                        contentDescription = poweredByText,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(contentColor),
                        modifier =
                                Modifier.height(18.dp).aspectRatio(1564.3f / 428.4f),
                )
            }
            AiSearchLlmProviderId.ANTHROPIC -> {
                AnthropicClaudeWordmark(
                        contentDescription = poweredByText,
                        wordmarkTextColor = contentColor,
                        modifier =
                                Modifier.height(18.dp)
                                        .aspectRatio(689.97997f / 148.17999f),
                )
            }
            AiSearchLlmProviderId.GROQ -> {
                Image(
                        painter = painterResource(R.drawable.groq_wordmark),
                        contentDescription = poweredByText,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(contentColor),
                        modifier =
                                Modifier.height(18.dp).aspectRatio(152f / 55.5f),
                )
            }
            AiSearchLlmProviderId.META -> {
                Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                            painter = painterResource(R.drawable.meta_logo),
                            contentDescription = poweredByText,
                            modifier = Modifier.size(16.dp),
                    )
                    Text(
                            text = stringResource(R.string.settings_ai_provider_meta),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor,
                    )
                }
            }
            AiSearchLlmProviderId.OPENCODE -> {
                Image(
                        painter = painterResource(R.drawable.opencode_wordmark),
                        contentDescription = poweredByText,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(contentColor),
                        modifier =
                                Modifier.height(14.dp).aspectRatio(640.714f / 115f),
                )
            }
            else -> {
                if (llmProviderId.isCustom) {
                    Text(
                            text = usedModelId.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor,
                    )
                } else {
                    val logoAspectRatio = if (isGemma) 250f / 64f else 288f / 65f
                    val logoHeight = if (isGemma) 18.dp else 14.dp
                    if (isGemma) {
                        GemmaWordmark(
                                contentDescription = poweredByText,
                                wordmarkTextColor = contentColor,
                                modifier = Modifier.height(logoHeight).aspectRatio(logoAspectRatio),
                        )
                    } else {
                        GeminiWordmark(
                                contentDescription = poweredByText,
                                wordmarkTextColor = contentColor,
                                modifier = Modifier.height(logoHeight).aspectRatio(logoAspectRatio),
                        )
                    }
                }
            }
        }
    }
}

/** OpenCode ships two-tone marks for light and dark surfaces; pick the one for [contentColor]. */
internal fun openCodeLogoRes(contentColor: Color): Int =
        if (contentColor.luminance() > 0.5f) R.drawable.opencode_logo_dark else R.drawable.opencode_logo_light

/** Attribution row showing calculator branding. */
@Composable
internal fun CalculatorAttributionRow(
        modifier: Modifier = Modifier,
        toolType: SearchToolType = SearchToolType.CALCULATOR,
) {
    val titleRes =
            when (toolType) {
                SearchToolType.UNIT_CONVERTER -> R.string.unit_converter_info_title
                SearchToolType.DATE_CALCULATOR -> R.string.date_calculator_info_title
                SearchToolType.COLOR_VISUALIZER -> R.string.color_visualizer_toggle_title
                SearchToolType.CALCULATOR -> R.string.calculator_toggle_title
            }
    val icon =
            when (toolType) {
                SearchToolType.UNIT_CONVERTER -> MaterialSymbols.StraightenOutlined
                SearchToolType.DATE_CALCULATOR -> MaterialSymbols.CalendarMonthOutlined
                SearchToolType.COLOR_VISUALIZER -> MaterialSymbols.PaletteOutlined
                SearchToolType.CALCULATOR -> MaterialSymbols.CalculateOutlined
            }
    ToolAttributionRow(modifier = modifier, icon = icon, titleRes = titleRes)
}

/** Attribution row showing a tool's icon and name. */
@Composable
internal fun ToolAttributionRow(
        icon: ImageVector,
        @StringRes titleRes: Int,
        modifier: Modifier = Modifier,
) {
    val contentColor = informationAttributionContentColor()
    Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        // Decorative: the label beside it already names the tool for screen readers.
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
        )
        Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
        )
    }
}
