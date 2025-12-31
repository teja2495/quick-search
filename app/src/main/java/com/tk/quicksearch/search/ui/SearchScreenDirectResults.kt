package com.tk.quicksearch.search.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.util.PhoneNumberUtils
import com.tk.quicksearch.search.core.*

/**
 * Composable that displays direct search results with loading, success, and error states.
 */
@Composable
fun DirectSearchResult(
    DirectSearchState: DirectSearchState,
    onRetry: () -> Unit,
    showWallpaperBackground: Boolean = false,
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {}
) {
    if (DirectSearchState.status == DirectSearchStatus.Idle) return

    val showAttribution = DirectSearchState.status == DirectSearchStatus.Success &&
        !DirectSearchState.answer.isNullOrBlank()

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (DirectSearchState.status) {
                DirectSearchStatus.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = stringResource(R.string.direct_search_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DirectSearchStatus.Success -> {
                    DirectSearchState.answer?.let { answer ->
                        ClickableDirectSearchText(
                            text = answer,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            onPhoneNumberClick = onPhoneNumberClick,
                            onEmailClick = onEmailClick
                        )
                    }
                }
                DirectSearchStatus.Error -> {
                    Text(
                        text = DirectSearchState.errorMessage
                            ?: stringResource(R.string.direct_search_error_generic),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (DirectSearchState.activeQuery != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(onClick = onRetry) {
                                Text(text = stringResource(R.string.direct_search_action_retry))
                            }
                        }
                    }
                }
                DirectSearchStatus.Idle -> {}
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val minCardHeight = 140.dp

        if (showWallpaperBackground) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minCardHeight),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                content()
            }
        } else {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minCardHeight),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                content()
            }
        }

        if (showAttribution) {
            GeminiAttributionRow(
                modifier = Modifier.fillMaxWidth(),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Composable that displays calculator results.
 */
@Composable
fun CalculatorResult(
    calculatorState: CalculatorState,
    showWallpaperBackground: Boolean = false
) {
    val result = calculatorState.result
    if (result == null) return

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "= $result",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val minCardHeight = 140.dp

        if (showWallpaperBackground) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minCardHeight),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                content()
            }
        } else {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minCardHeight),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                content()
            }
        }
    }
}

/**
 * Attribution row showing powered by Gemini branding.
 */
@Composable
private fun GeminiAttributionRow(
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.direct_search_powered_by),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
        Image(
            painter = painterResource(id = R.drawable.gemini_logo),
            contentDescription = stringResource(R.string.direct_search_powered_by),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(12.dp)
                .aspectRatio(288f / 65f)
        )
    }
}

/**
 * Composable that displays text with clickable phone numbers and email IDs.
 */
@Composable
private fun ClickableDirectSearchText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    onPhoneNumberClick: (String) -> Unit,
    onEmailClick: (String) -> Unit
) {
    // Regex patterns to match phone numbers (E.164 or grouped digits) and email addresses
    val phonePattern = Regex("""\+?\d[\d\s().-]{6,}""")
    val emailPattern = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    data class ClickableMatch(
        val range: IntRange,
        val tag: String,
        val value: String
    )

    fun rangesOverlap(a: IntRange, b: IntRange): Boolean {
        return a.first <= b.last && b.first <= a.last
    }

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
    val dedupedMatches = matches
        .sortedBy { it.range.first }
        .fold(mutableListOf<ClickableMatch>()) { acc, match ->
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
                annotation = match.value
            )
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
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

    ClickableText(
        text = annotatedString,
        style = style.copy(color = color),
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                when (annotation.tag) {
                    "PHONE" -> onPhoneNumberClick(annotation.item)
                    "EMAIL" -> onEmailClick(annotation.item)
                }
            }
        }
    )
}
