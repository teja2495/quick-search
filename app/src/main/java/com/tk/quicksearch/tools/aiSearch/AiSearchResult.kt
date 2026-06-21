package com.tk.quicksearch.tools.aiSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AiSearchState
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.shared.util.PhoneEmailLinkifiedText

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
                    !aiSearchState.answer.isNullOrBlank()
    val effectiveProviderId = aiSearchState.llmProviderId ?: aiSearchLlmProviderId

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
            Column(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                when (aiSearchState.status) {
                    AiSearchStatus.Loading -> {
                        GeminiLoadingAnimation()
                    }
                    AiSearchStatus.Success -> {
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
        }
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
