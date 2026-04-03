package com.tk.quicksearch.tools.aiTools

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.tools.directSearch.DirectSearchLlmProviderRegistry
import com.tk.quicksearch.tools.directSearch.LlmRequest
import org.json.JSONObject

class WordClockNotRecognizedException : Exception()

private const val WORD_CLOCK_SYSTEM_INSTRUCTION =
        "You are a world-clock and word-clock formatter. " +
                "Respond with ONLY a single JSON object (no markdown, no code fences). " +
                "Schema: {\"word_clock_text\":\"<text>\",\"time_text\":\"<normalized input time>\",\"place_text\":\"<optional formatted place>\",\"time_zone_text\":\"<optional timezone>\"}. " +
                "Set word_clock_text to the resolved local CLOCK TIME in 12-hour format with AM/PM (example: \"2:58 PM\"). " +
                "Set time_text to the resolved local DATE (example: \"Tuesday, March 31, 2026\"). " +
                "If the request resolves to a city, set place_text to \"<City>, <Country>\" (example: \"Tokyo, Japan\"). " +
                "For country-only/place-only requests, set place_text to the best canonical place label. " +
                "Set time_zone_text to a human-friendly timezone with abbreviation in brackets and no UTC offset in brackets (example: \"India Standard Time (IST)\"). " +
                "Treat location-based requests as valid (e.g., city, country, timezone like \"India\", \"Tokyo\", \"UTC+5:30\"). " +
                "For location requests, resolve the CURRENT local time at that location before formatting. " +
                "If the user query is not a word clock request, respond exactly: {\"error\":\"not_word_clock\"}."

class WordClockHandler(
        private val context: Context,
        private val userPreferences: UserAppPreferences,
) {
    fun parseModelResponse(raw: String): Result<WordClockModelResult> {
        val trimmed =
                raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching {
            val obj = JSONObject(trimmed)
            if (obj.optString("error") == "not_word_clock") {
                throw WordClockNotRecognizedException()
            }
            val wordClockText = obj.getString("word_clock_text").trim()
            val timeText = obj.optString("time_text").trim()
            val placeText = obj.optString("place_text").trim()
            val timeZoneText = normalizeTimeZoneText(obj.optString("time_zone_text").trim())
            if (wordClockText.isBlank()) error("invalid")
            WordClockModelResult(
                    wordClockText = wordClockText,
                    sourceTimeText = timeText,
                    placeText = placeText,
                    timeZoneText = timeZoneText,
            )
        }
    }

    suspend fun convert(
            confirmed: ConfirmedWordClockQuery,
    ): Result<Pair<WordClockModelResult, String>> {
        val providerId = userPreferences.getDirectSearchProviderId()
        val provider = DirectSearchLlmProviderRegistry.get(providerId, context)
        val apiKey = userPreferences.getLlmApiKey(providerId)?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            return Result.failure(
                    IllegalStateException(context.getString(R.string.direct_search_error_no_key)),
            )
        }
        val modelId =
                userPreferences.getCurrencyConverterModel().trim().ifBlank {
                    provider.defaultModelId
                }
        val userMessage =
                "Resolve this request into local clock time and date: ${confirmed.timeExpression}. " +
                        "If it is a location, compute the current local time there first. " +
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
                                        systemInstruction = WORD_CLOCK_SYSTEM_INSTRUCTION,
                                        responseMimeType = "application/json",
                                ),
                )
        return result.mapCatching { text ->
            val parsed = parseModelResponse(text).getOrElse { throw it }
            parsed to modelId
        }
    }

    private fun normalizeTimeZoneText(raw: String): String {
        if (raw.isBlank()) return raw
        val normalized = raw.trim()
        val hasUtcInParentheses = Regex("""\([^)]*UTC[^)]*\)""", RegexOption.IGNORE_CASE).containsMatchIn(normalized)
        if (!hasUtcInParentheses) return normalized

        val baseLabel = normalized.replace(Regex("""\s*\([^)]*\)\s*"""), "").trim()
        val abbreviation = deriveTimeZoneAbbreviation(baseLabel)
        return if (abbreviation != null && baseLabel.isNotBlank()) {
            "$baseLabel ($abbreviation)"
        } else {
            baseLabel.ifBlank { normalized }
        }
    }

    private fun deriveTimeZoneAbbreviation(label: String): String? {
        if (label.isBlank()) return null
        val stopWords = setOf("time", "standard", "daylight", "summer")
        val parts =
                label.split(Regex("""[\s\-/]+"""))
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
        val significant = parts.filterNot { stopWords.contains(it.lowercase()) }
        val source = if (significant.isNotEmpty()) significant else parts
        val abbr = source.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        return abbr.takeIf { it.length in 2..6 }
    }
}

data class WordClockModelResult(
        val wordClockText: String,
        val sourceTimeText: String,
        val placeText: String,
        val timeZoneText: String,
)
