package com.tk.quicksearch.tools.directSearch

import android.content.ClipData
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.DirectSearchState
import com.tk.quicksearch.search.core.DirectSearchStatus
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import androidx.compose.ui.graphics.Color

/** Composable that displays direct search results with loading, success, and error states. */
@Composable
fun DirectSearchResult(
        directSearchState: DirectSearchState,
        showWallpaperBackground: Boolean = false,
        onGeminiModelInfoClick: () -> Unit = {},
        onOpenDirectSearchConfigure: () -> Unit = {},
        onPhoneNumberClick: (String) -> Unit = {},
        onEmailClick: (String) -> Unit = {},
) {
    if (directSearchState.status == DirectSearchStatus.Idle) return

    val showAttribution =
            directSearchState.status == DirectSearchStatus.Success &&
                    !directSearchState.answer.isNullOrBlank()

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    GeminiResultCard(
            showWallpaperBackground = showWallpaperBackground,
            showAttribution = showAttribution,
            usedModelId = directSearchState.usedModelId,
            isAttributionClickable = true,
            onGeminiModelInfoClick = onGeminiModelInfoClick,
            onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                when (directSearchState.status) {
                    DirectSearchStatus.Loading -> {
                        GeminiLoadingAnimation()
                    }
                    DirectSearchStatus.Success -> {
                        directSearchState.answer?.let { answer ->
                            Box(
                                    modifier =
                                            Modifier.fillMaxWidth().pointerInput(answer) {
                                                detectTapGestures(
                                                        onLongPress = {
                                                            clipboardManager.setText(
                                                                    AnnotatedString(answer)
                                                            )
                                                        },
                                                )
                                            },
                            ) {
                                ClickableDirectSearchText(
                                        text = answer,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        onPhoneNumberClick = onPhoneNumberClick,
                                        onEmailClick = onEmailClick,
                                )
                            }
                        }
                    }
                    DirectSearchStatus.Error -> {
                        Text(
                                text = directSearchState.errorMessage
                                        ?: stringResource(R.string.direct_search_error_generic),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                        )
                    }
                    DirectSearchStatus.Idle -> {}
                }
            }
        }
    }
}

/** Composable that displays text with clickable phone numbers and email IDs. */
@Composable
private fun ClickableDirectSearchText(
        text: String,
        style: androidx.compose.ui.text.TextStyle,
        color: Color,
        onPhoneNumberClick: (String) -> Unit,
        onEmailClick: (String) -> Unit,
) {
    // Regex patterns to match phone numbers (E.164 or grouped digits) and email addresses
    val phonePattern = Regex("""\+?\d[\d\s().-]{6,}""")
    val emailPattern = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    data class ClickableMatch(
            val range: IntRange,
            val tag: String,
            val value: String,
    )

    fun rangesOverlap(a: IntRange, b: IntRange): Boolean = a.first <= b.last && b.first <= a.last

    val matches = mutableListOf<ClickableMatch>()

    phonePattern.findAll(text).forEach { matchResult ->
        val phoneNumber = matchResult.value
        val cleanedNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanedNumber != null) {
            matches.add(ClickableMatch(matchResult.range, "PHONE", cleanedNumber))
        }
    }

    emailPattern.findAll(text).forEach { matchResult ->
        matches.add(ClickableMatch(matchResult.range, "EMAIL", matchResult.value))
    }

    // Remove overlapping matches by keeping the earliest occurrences
    val dedupedMatches =
            matches.sortedBy { it.range.first }.fold(mutableListOf<ClickableMatch>()) { acc, match ->
                if (acc.none { rangesOverlap(it.range, match.range) }) {
                    acc.add(match)
                }
                acc
            }

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0

        dedupedMatches.forEach { match ->
            val startIndex = match.range.first
            val endIndex = match.range.last + 1

            if (startIndex > lastIndex) {
                append(text.substring(lastIndex, startIndex))
            }

            pushStringAnnotation(
                    tag = match.tag,
                    annotation = match.value,
            )
            withStyle(
                    style =
                            SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                            ),
            ) {
                append(text.substring(startIndex, endIndex))
            }
            pop()

            lastIndex = endIndex
        }

        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    @Suppress("DEPRECATION")
    ClickableText(
            text = annotatedString,
            style = style.copy(color = color),
            onClick = { offset ->
                annotatedString
                        .getStringAnnotations(
                                start = offset,
                                end = offset,
                        )
                        .firstOrNull()
                        ?.let { annotation ->
                            when (annotation.tag) {
                                "PHONE" -> onPhoneNumberClick(annotation.item)
                                "EMAIL" -> onEmailClick(annotation.item)
                            }
                        }
            },
    )
}
