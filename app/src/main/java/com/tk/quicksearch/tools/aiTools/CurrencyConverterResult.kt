package com.tk.quicksearch.tools.aiTools

import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.components.AppButtonTone
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CurrencyConverterState
import com.tk.quicksearch.search.core.CurrencyConverterStatus
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.GeminiLoadingAnimation
import com.tk.quicksearch.tools.aiSearch.GeminiResultCard
import java.util.Currency
import java.util.Locale
import java.text.NumberFormat
import java.math.RoundingMode
import com.tk.quicksearch.shared.ui.components.AppTonalButton

@Composable
fun CurrencyConverterResult(
        currencyConverterState: CurrencyConverterState,
        showWallpaperBackground: Boolean = false,
        onCheckRatesOnGoogle: (String) -> Unit = {},
) {
    if (currencyConverterState.status == CurrencyConverterStatus.Idle) return

    val copyText =
            if (currencyConverterState.status == CurrencyConverterStatus.Success) {
                currencyConverterState.convertedAmount
                        ?.let(::formatCurrencyAmountForDisplay)
                        ?.let { amount -> "$amount ${currencyConverterState.targetCurrencyCode.orEmpty()}" }
            } else {
                null
            }

    GeminiResultCard(
            showWallpaperBackground = showWallpaperBackground,
            showAttribution = false,
            usedModelId = null,
            copyText = copyText,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                when (currencyConverterState.status) {
                    CurrencyConverterStatus.Loading -> {
                        GeminiLoadingAnimation()
                    }
                    CurrencyConverterStatus.Success -> {
                        val amount =
                                formatCurrencyAmountForDisplay(
                                        currencyConverterState.convertedAmount.orEmpty(),
                                )
                        val code = currencyConverterState.targetCurrencyCode.orEmpty()
                        val name = currencyConverterState.targetCurrencyName.orEmpty()
                        val symbol = getCurrencySymbolForCode(code)
                        val line1 = listOfNotNull(symbol, amount).joinToString(separator = " ")
                        val currencyDescription =
                                if (name.isNotBlank() && !name.equals(code, ignoreCase = true)) {
                                    "$name ($code)"
                                } else {
                                    code.takeIf { it.isNotBlank() }
                                }
                        val updatedAt = currencyConverterState.ratesFetchedAtMillis
                        val freshness =
                                if (updatedAt != null) {
                                    stringResource(
                                            R.string.currency_converter_updated,
                                            formatCurrencyFreshness(updatedAt),
                                    )
                                } else {
                                    null
                                }
                        Box(
                                modifier = Modifier.fillMaxWidth().heightIn(min = CurrencyConverterMinHeight),
                        ) {
                            Column {
                                Text(
                                        text = line1,
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (currencyDescription != null) {
                                    Text(
                                            text = currencyDescription,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (freshness != null) {
                                Text(
                                        text = freshness,
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .align(Alignment.BottomCenter),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppColors.ContentMutedAlpha),
                                )
                            }
                        }
                    }
                    CurrencyConverterStatus.Error -> {
                        // Same min height as the success layout so the card doesn't jump between states.
                        Column(
                                modifier = Modifier.fillMaxWidth().heightIn(min = CurrencyConverterMinHeight),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement =
                                        Arrangement.spacedBy(
                                                DesignTokens.SpacingLarge,
                                                Alignment.CenterVertically,
                                        ),
                        ) {
                            Text(
                                    text =
                                            currencyConverterState.errorMessage
                                                    ?: stringResource(R.string.direct_search_error_generic),
                                    textAlign = TextAlign.Center,
                                    // Balanced wrapping avoids a lone word on the second line.
                                    style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Heading),
                                    color = MaterialTheme.colorScheme.error,
                            )
                            currencyConverterState.activeQuery?.takeIf { it.isNotBlank() }?.let { query ->
                                SearchOnGoogleButton(
                                        text = stringResource(R.string.currency_converter_check_rates_on_google),
                                        onClick = { onCheckRatesOnGoogle(query) },
                                )
                            }
                        }
                    }
                    CurrencyConverterStatus.Idle -> {}
                }
            }
        }
    }
}

/** Fallback shown on tool error cards to run the same query on Google instead. */
@Composable
internal fun SearchOnGoogleButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    AppTonalButton(
            onClick = onClick,
            modifier = modifier,
            tone = AppButtonTone.Secondary,
            contentPadding = PaddingValues(horizontal = DesignTokens.SpacingLarge),
    ) {
        Icon(
                painter = painterResource(R.drawable.google),
                contentDescription = null,
                modifier = Modifier.size(DesignTokens.ButtonIconSizeSmall),
                tint = Color.Unspecified,
        )
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
        Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun formatCurrencyFreshness(fetchedAtMillis: Long): String {
    val elapsedMillis = (System.currentTimeMillis() - fetchedAtMillis).coerceAtLeast(0L)
    val elapsedMinutes = elapsedMillis / 60_000L
    if (elapsedMinutes < 60L) return "$elapsedMinutes min ago"

    val elapsedHours = elapsedMillis / 3_600_000L
    return "$elapsedHours ${if (elapsedHours == 1L) "hour" else "hours"} ago"
}

private fun formatCurrencyAmountForDisplay(raw: String): String {
    val amount = raw.trim().toBigDecimalOrNull() ?: return raw.trim()
    return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }.format(amount)
}

private fun getCurrencySymbolForCode(code: String): String? {
    val normalizedCode = code.trim().uppercase(Locale.ROOT)
    if (normalizedCode.length != 3) return null
    return runCatching { Currency.getInstance(normalizedCode).getSymbol(Locale.getDefault()) }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals(normalizedCode, ignoreCase = true) }
}

/** Shared min height of the success and error layouts so the card doesn't jump between states. */
private val CurrencyConverterMinHeight = 144.dp
