package com.tk.quicksearch.tools.aiSearch

import com.tk.quicksearch.shared.ui.components.AppButtonSize
import com.tk.quicksearch.shared.ui.components.AppButtonTone
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AiSearchState
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.shared.util.FeedbackUtils
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.util.PhoneEmailLinkifiedText
import com.tk.quicksearch.shared.ui.components.AppTonalButton

/** Composable that displays AI search results with loading, success, and error states. */
@Composable
fun AiSearchResult(
        aiSearchState: AiSearchState,
        aiSearchLlmProviderId: AiSearchLlmProviderId,
        showWallpaperBackground: Boolean = false,
        onGeminiModelInfoClick: () -> Unit = {},
        onOpenAiSearchConfigure: () -> Unit = {},
        onPhoneNumberClick: (String) -> Unit = {},
        onEmailClick: (String) -> Unit = {},
) {
    if (aiSearchState.status == AiSearchStatus.Idle) return

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val showAttribution =
            aiSearchState.status == AiSearchStatus.Success &&
                    !aiSearchState.answer.isNullOrBlank() &&
                    !aiSearchState.isQuickSearchHelp
    val showHelpContact =
            aiSearchState.isQuickSearchHelp && aiSearchState.status != AiSearchStatus.Loading
    val effectiveProviderId = aiSearchState.llmProviderId ?: aiSearchLlmProviderId
    var fallbackTipDismissed by remember(aiSearchState.activeQuery) {
        mutableStateOf(false)
    }

    LaunchedEffect(aiSearchState.status) {
        if (aiSearchState.status == AiSearchStatus.Loading) {
            fallbackTipDismissed = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        if (aiSearchState.showWebSearchFallbackTip && !fallbackTipDismissed) {
            TipBanner(
                text = stringResource(R.string.gemini_web_search_quota_fallback_tip),
                onDismiss = { fallbackTipDismissed = true },
            )
        }

        GeminiResultCard(
            showWallpaperBackground = showWallpaperBackground,
            showAttribution = showAttribution,
            usedModelId = aiSearchState.usedModelId,
            llmProviderId = effectiveProviderId,
            isAttributionClickable = true,
            onGeminiModelInfoClick = onGeminiModelInfoClick,
            onOpenAiSearchConfigure = onOpenAiSearchConfigure,
            copyText = aiSearchState.answer,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Help cards fill the card's minimum height so Contact Developer sits at the bottom.
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .then(
                                if (showHelpContact) Modifier.heightIn(min = AiResultCardMinHeight) else Modifier,
                            ).padding(DesignTokens.SpacingLarge),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    ) {
                        when (aiSearchState.status) {
                            AiSearchStatus.Loading -> {
                                GeminiLoadingAnimation()
                            }
                            AiSearchStatus.Success -> {
                                if (aiSearchState.isQuickSearchHelp && aiSearchState.answer == null) {
                                    Text(
                                        text = stringResource(R.string.quick_search_help_intro),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                aiSearchState.answer?.let { answer ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        ClickableAiSearchText(
                                            text = answer,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            onPhoneNumberClick = onPhoneNumberClick,
                                            onEmailClick = onEmailClick,
                                            onLongClick = {
                                                clipboardManager.setText(AnnotatedString(answer))
                                            },
                                        )
                                    }
                                }
                            }
                            AiSearchStatus.Error -> {
                                Text(
                                    text = aiSearchState.errorMessage
                                        ?: stringResource(R.string.direct_search_error_generic),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            AiSearchStatus.Idle -> {}
                        }
                    }
                    if (showHelpContact) {
                        QuickSearchHelpContactButton(
                            question = aiSearchState.activeQuery.orEmpty(),
                            modifier = Modifier.padding(top = DesignTokens.SpacingMedium),
                        )
                    }
                }
            }
        }
    }
}

/** Opens the feedback email prefilled with the `@help` question, minus the alias. */
@Composable
private fun QuickSearchHelpContactButton(
    question: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    AppTonalButton(
        onClick = {
            FeedbackUtils.launchFeedbackEmail(context, QuickSearchHelp.stripAlias(question))
        },
        modifier = modifier,
        tone = AppButtonTone.Secondary,
        size = AppButtonSize.ExtraSmall,
        icon = MaterialSymbols.MailOutline,
    ) {
        Text(
            text = stringResource(R.string.quick_search_help_contact_developer),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Composable that displays text with clickable phone numbers and email IDs. */
@Composable
private fun ClickableAiSearchText(
        text: String,
        style: androidx.compose.ui.text.TextStyle,
        color: Color,
        onPhoneNumberClick: (String) -> Unit,
        onEmailClick: (String) -> Unit,
        onLongClick: () -> Unit,
) {
    PhoneEmailLinkifiedText(
            text = text,
            style = style,
            color = color,
            linkColor = MaterialTheme.colorScheme.primary,
            onPhoneNumberClick = onPhoneNumberClick,
            onEmailClick = onEmailClick,
            onLongClick = onLongClick,
    )
}
