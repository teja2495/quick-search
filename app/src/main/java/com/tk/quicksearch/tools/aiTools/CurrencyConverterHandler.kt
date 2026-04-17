package com.tk.quicksearch.tools.aiTools

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import com.tk.quicksearch.tools.aiSearch.LlmRequest
import org.json.JSONObject

class CurrencyNotRecognizedException : Exception()

private const val CURRENCY_SYSTEM_INSTRUCTION =
        "You convert currencies using current market rates. " +
                "Respond with ONLY a single JSON object (no markdown, no code fences). " +
                "Schema: {\"from_currency\":\"USD\",\"to_currency\":\"INR\",\"from_amount\":\"100\",\"converted_amount\":\"<string>\",\"to_currency_name\":\"<English name>\"}. " +
                "Use ISO 4217 for from_currency and to_currency. " +
                "from_amount must echo the user's amount as a decimal string. " +
                "converted_amount is the result as a decimal string. " +
                "If the user query is not a currency conversion, respond exactly: {\"error\":\"not_currency\"}."

class CurrencyConverterHandler(
        private val context: Context,
        private val userPreferences: UserAppPreferences,
) {
    fun parseModelResponse(raw: String): Result<CurrencyConversionModelResult> {
        val trimmed =
                raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching {
            val obj = JSONObject(trimmed)
            if (obj.optString("error") == "not_currency") {
                throw CurrencyNotRecognizedException()
            }
            val converted = obj.getString("converted_amount").trim()
            val toCode = obj.getString("to_currency").trim().uppercase()
            val toName = obj.optString("to_currency_name").trim().ifBlank { toCode }
            val fromAmount = obj.optString("from_amount").trim()
            val fromCode = obj.optString("from_currency").trim().uppercase()
            if (converted.isBlank() || toCode.length != 3) error("invalid")
            CurrencyConversionModelResult(
                    convertedAmount = converted,
                    targetCurrencyCode = toCode,
                    targetCurrencyName = toName,
                    sourceAmount = fromAmount,
                    sourceCurrencyCode = fromCode,
            )
        }
    }

    suspend fun convert(confirmed: ConfirmedCurrencyQuery): Result<Pair<CurrencyConversionModelResult, String>> {
        val providerId = userPreferences.getAiSearchProviderId()
        val provider = AiSearchLlmProviderRegistry.get(providerId, context)
        val apiKey = userPreferences.getLlmApiKey(providerId)?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            return Result.failure(IllegalStateException(context.getString(R.string.direct_search_error_no_key)))
        }
        val modelId =
                userPreferences.getCurrencyConverterModel().trim().ifBlank {
                    provider.defaultModelId
                }
        val userMessage =
                "Convert ${confirmed.amount} ${confirmed.fromCurrency} to ${confirmed.toCurrency}. " +
                        "Original user query: ${confirmed.originalQuery}"
        val result =
                provider.fetchAnswer(
                        apiKey = apiKey,
                        context = context,
                        request =
                                LlmRequest(
                                        query = userMessage,
                                        personalContext = null,
                                        modelId = modelId,
                                        useGroundingWithGoogleSearch = true,
                                        useSystemInstruction = true,
                                        systemInstruction = CURRENCY_SYSTEM_INSTRUCTION,
                                        responseMimeType = "application/json",
                                ),
                )
        return result.mapCatching { text ->
            val parsed = parseModelResponse(text).getOrElse { throw it }
            parsed to modelId
        }
    }

    /** Used in alias mode — sends the raw query to the AI without pre-parsing. */
    suspend fun convertRaw(rawQuery: String): Result<Pair<CurrencyConversionModelResult, String>> {
        val providerId = userPreferences.getAiSearchProviderId()
        val provider = AiSearchLlmProviderRegistry.get(providerId, context)
        val apiKey = userPreferences.getLlmApiKey(providerId)?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            return Result.failure(IllegalStateException(context.getString(R.string.direct_search_error_no_key)))
        }
        val modelId =
                userPreferences.getCurrencyConverterModel().trim().ifBlank {
                    provider.defaultModelId
                }
        val result =
                provider.fetchAnswer(
                        apiKey = apiKey,
                        context = context,
                        request =
                                LlmRequest(
                                        query = "Currency conversion: $rawQuery",
                                        personalContext = null,
                                        modelId = modelId,
                                        useGroundingWithGoogleSearch = true,
                                        useSystemInstruction = true,
                                        systemInstruction = CURRENCY_SYSTEM_INSTRUCTION,
                                        responseMimeType = "application/json",
                                ),
                )
        return result.mapCatching { text ->
            val parsed = parseModelResponse(text).getOrElse { throw it }
            parsed to modelId
        }
    }
}

data class CurrencyConversionModelResult(
        val convertedAmount: String,
        val targetCurrencyCode: String,
        val targetCurrencyName: String,
        val sourceAmount: String,
        val sourceCurrencyCode: String,
)
