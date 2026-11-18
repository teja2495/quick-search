package com.tk.quicksearch.tools.aiSearch

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.tools.inlineSearch.SearchToolType
import com.tk.quicksearch.search.searchScreen.shared.InformationCard
import com.tk.quicksearch.search.searchScreen.shared.InformationCardCopyAction
import com.tk.quicksearch.search.searchScreen.shared.InformationCardFooter
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
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
                        .clip(AppShapes.small)
                        .combinedClickable(
                                role = Role.Button,
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
        val definition = AiSearchLlmProviderRegistry.definition(llmProviderId)
        if (definition == null) {
            Text(
                    text = usedModelId.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
            )
            return@Row
        }
        when (val attribution = definition.branding.attribution) {
            is AiProviderAttribution.Wordmark ->
                Image(
                        painter = painterResource(attribution.drawable),
                        contentDescription = poweredByText,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(contentColor),
                        modifier = Modifier.height(attribution.heightDp.dp).aspectRatio(attribution.aspectRatio),
                )
            AiProviderAttribution.Claude ->
                AnthropicClaudeWordmark(
                        contentDescription = poweredByText,
                        wordmarkTextColor = contentColor,
                        modifier = Modifier.height(18.dp).aspectRatio(689.97997f / 148.17999f),
                )
            AiProviderAttribution.Label ->
                Row(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                            painter = painterResource(definition.branding.icon),
                            contentDescription = poweredByText,
                            modifier = Modifier.size(16.dp),
                    )
                    Text(
                            text = stringResource(definition.nameRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor,
                    )
                }
            AiProviderAttribution.Gemini ->
                if (isGemma) {
                    GemmaWordmark(
                            contentDescription = poweredByText,
                            wordmarkTextColor = contentColor,
                            modifier = Modifier.height(18.dp).aspectRatio(250f / 64f),
                    )
                } else {
                    GeminiWordmark(
                            contentDescription = poweredByText,
                            wordmarkTextColor = contentColor,
                            modifier = Modifier.height(14.dp).aspectRatio(288f / 65f),
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
    ToolAttributionRow(
        modifier = modifier,
        icon = toolType.attributionIcon(),
        titleRes = toolType.setting.titleRes,
    )
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
