package com.tk.quicksearch.tools.directSearch

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CurrencyConverterState
import com.tk.quicksearch.search.core.CurrencyConverterStatus
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale

@Composable
fun CurrencyConverterResult(
        currencyConverterState: CurrencyConverterState,
        showWallpaperBackground: Boolean = false,
        onGeminiModelInfoClick: () -> Unit = {},
) {
    if (currencyConverterState.status == CurrencyConverterStatus.Idle) return

    val showAttribution =
            currencyConverterState.status == CurrencyConverterStatus.Success &&
                    !currencyConverterState.convertedAmount.isNullOrBlank()

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    GeminiResultCard(
            showWallpaperBackground = showWallpaperBackground,
            showAttribution = showAttribution,
            usedModelId = currencyConverterState.usedModelId,
            onGeminiModelInfoClick = onGeminiModelInfoClick,
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
                        val line1 = "$amount $code"
                        val subtitle =
                                if (name.isNotBlank() && !name.equals(code, ignoreCase = true)) {
                                    if (symbol != null) "$name ($symbol)" else name
                                } else {
                                    null
                                }
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth().pointerInput(line1) {
                                            detectTapGestures(
                                                    onLongPress = {
                                                        clipboardManager.setText(
                                                                AnnotatedString(line1)
                                                        )
                                                    },
                                            )
                                        },
                        ) {
                            Text(
                                    text = line1,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (subtitle != null) {
                                Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    CurrencyConverterStatus.Error -> {
                        Text(
                                text =
                                        currencyConverterState.errorMessage
                                                ?: stringResource(R.string.direct_search_error_generic),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                        )
                    }
                    CurrencyConverterStatus.Idle -> {}
                }
            }
        }
    }
}

private fun formatCurrencyAmountForDisplay(raw: String): String {
    val normalized = raw.trim().replace(",", ".")
    val bd = normalized.toBigDecimalOrNull() ?: return raw.trim()
    return bd.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

private fun getCurrencySymbolForCode(code: String): String? {
    val normalizedCode = code.trim().uppercase(Locale.ROOT)
    if (normalizedCode.length != 3) return null
    return runCatching { Currency.getInstance(normalizedCode).getSymbol(Locale.getDefault()) }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals(normalizedCode, ignoreCase = true) }
}
