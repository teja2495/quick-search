package com.tk.quicksearch.tools.directSearch

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import android.content.ClipData
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

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
    val overlayCardColor = LocalOverlayResultCardColor.current
    val cardColors =
            if (overlayCardColor != null) {
                CardDefaults.cardColors(containerColor = overlayCardColor)
            } else {
                AppColors.getCardColors(showWallpaperBackground = showWallpaperBackground)
            }
    val cardElevation =
            AppColors.getCardElevation(showWallpaperBackground = showWallpaperBackground)

    val content: @Composable () -> Unit = {
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
                                                ?: stringResource(
                                                        R.string.direct_search_error_generic
                                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                        )
                    }
                    DirectSearchStatus.Idle -> {}
                }
            }
        }
    }

    Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        if (showWallpaperBackground) {
            Card(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 175.dp),
                    colors = cardColors,
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = cardElevation,
            ) { content() }
        } else {
            ElevatedCard(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 175.dp),
                    colors = cardColors,
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = cardElevation,
            ) { content() }
        }

        if (showAttribution) {
            GeminiAttributionRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    usedModelId = directSearchState.usedModelId,
                    onClick = onGeminiModelInfoClick,
                    onLongClick = onOpenDirectSearchConfigure,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorResult(
        calculatorState: CalculatorState,
        showWallpaperBackground: Boolean = false,
) {
    val result = calculatorState.result
    val isCalculatorMode = calculatorState.isCalculatorMode
    val showInvalidExpression = calculatorState.showInvalidExpression
    if (result == null && !isCalculatorMode) return

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val overlayCardColor = LocalOverlayResultCardColor.current
    val cardColors =
            if (overlayCardColor != null) {
                CardDefaults.cardColors(containerColor = overlayCardColor)
            } else {
                AppColors.getCardColors(showWallpaperBackground = showWallpaperBackground)
            }
    val cardElevation =
            AppColors.getCardElevation(showWallpaperBackground = showWallpaperBackground)

    val onLongClick: (() -> Unit)? =
            if (result != null) {
                { clipboardManager.setText(AnnotatedString(result)) }
            } else {
                null
            }

    val content: @Composable () -> Unit = {
        Column(
                modifier =
                        Modifier.fillMaxWidth().fillMaxHeight().padding(DesignTokens.SpacingLarge),
                verticalArrangement = Arrangement.Center,
        ) {
            when {
                result != null -> {
                    Text(
                            text = "= $result",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                showInvalidExpression -> {
                    Text(
                            text =
                                    stringResource(
                                            R.string.calculator_invalid_or_unsupported_expression
                                    ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    // Intentionally empty while in calculator mode with no expression.
                }
            }
        }
    }

    Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        if (showWallpaperBackground) {
            Card(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(min = 175.dp)
                                    .combinedClickable(
                                            onClick = {},
                                            onLongClick = onLongClick,
                                    ),
                    colors = cardColors,
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = cardElevation,
            ) { content() }
        } else {
            ElevatedCard(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(min = 175.dp)
                                    .combinedClickable(
                                            onClick = {},
                                            onLongClick = onLongClick,
                                    ),
                    colors = cardColors,
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = cardElevation,
            ) { content() }
        }

        CalculatorAttributionRow(
                modifier = Modifier.fillMaxWidth(),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Attribution row showing powered by Gemini or Gemma branding. */
@Composable
private fun GeminiAttributionRow(
        modifier: Modifier = Modifier,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        usedModelId: String? = null,
        onClick: () -> Unit = {},
        onLongClick: () -> Unit = {},
) {
    val poweredByText = stringResource(R.string.direct_search_powered_by)
    val isGemma = usedModelId?.lowercase()?.startsWith("gemma-") == true
    val logoRes = if (isGemma) R.drawable.gemma_logo else R.drawable.gemini_logo
    val logoAspectRatio = if (isGemma) 250f / 64f else 288f / 65f
    val logoHeight = if (isGemma) 20.dp else DesignTokens.SpacingLarge
    Row(
            modifier = modifier
                    .padding(horizontal = DesignTokens.SpacingLarge)
                    .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                            onLongClick = onLongClick,
                    ),
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
private fun CalculatorAttributionRow(
        modifier: Modifier = Modifier,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
            modifier = modifier.padding(horizontal = DesignTokens.SpacingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
                imageVector = Icons.Rounded.Calculate,
                contentDescription = stringResource(R.string.calculator_toggle_title),
                tint = contentColor,
                modifier = Modifier.size(14.dp),
        )
        Text(
                text = stringResource(R.string.calculator_toggle_title),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
        )
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

    fun rangesOverlap(
            a: IntRange,
            b: IntRange,
    ): Boolean = a.first <= b.last && b.first <= a.last

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
            matches.sortedBy { it.range.first }.fold(mutableListOf<ClickableMatch>()) { acc, match
                ->
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
            ) { append(text.substring(startIndex, endIndex)) }
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
