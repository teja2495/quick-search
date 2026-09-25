package com.tk.quicksearch.tools.aiTools

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import com.tk.quicksearch.tools.aiSearch.LlmRequest
import com.tk.quicksearch.tools.aiSearch.LlmResponseText
import com.tk.quicksearch.tools.aiSearch.modelSupportsGrounding
import com.tk.quicksearch.tools.aiSearch.prepareWebSearch
import com.tk.quicksearch.tools.aiSearch.providerSupportsNativeSearch
import org.json.JSONObject

class DictionaryNotRecognizedException : Exception()

private const val DICTIONARY_SYSTEM_INSTRUCTION =
        "You provide concise dictionary definitions. " +
                "Respond with ONLY a single JSON object (no markdown, no code fences). " +
                "Schema: {\"word\":\"<term>\",\"part_of_speech\":\"<text>\",\"meaning\":\"<definition>\",\"example\":\"<optional sentence>\",\"synonyms\":[\"<optional>\"]}. " +
                "meaning must be plain English and concise. " +
                "If the user query is not a dictionary request, respond exactly: {\"error\":\"not_dictionary\"}."

class DictionaryHandler(
        private val context: Context,
        private val userPreferences: UserAppPreferences,
) {
    fun parseModelResponse(raw: String): Result<DictionaryModelResult> {
        val payload = LlmResponseText.extractJsonObjectPayload(raw)
        return runCatching {
            check(payload.startsWith("{")) { "invalid" }
            val obj = JSONObject(payload)
            if (obj.optString("error") == "not_dictionary") {
                throw DictionaryNotRecognizedException()
            }
            val word = obj.getString("word").trim()
            val partOfSpeech = obj.optString("part_of_speech").trim()
            val meaning = obj.getString("meaning").trim()
            val example = obj.optString("example").trim()
            val synonyms =
                    obj.optJSONArray("synonyms")
                            ?.let { array ->
                                List(array.length()) { i -> array.optString(i).trim() }
                                        .filter { it.isNotBlank() }
                            }
                            .orEmpty()
            if (word.isBlank() || meaning.isBlank()) error("invalid")
            DictionaryModelResult(
                    word = word,
                    partOfSpeech = partOfSpeech,
                    meaning = meaning,
                    example = example,
                    synonyms = synonyms,
            )
        }
    }

    suspend fun define(
            confirmed: ConfirmedDictionaryQuery,
    ): Result<Pair<DictionaryModelResult, String>> {
        val providerId = userPreferences.getDictionaryProviderId()
        val provider = AiSearchLlmProviderRegistry.get(providerId, context)
        val apiKey = userPreferences.getLlmApiKey(providerId)?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            return Result.failure(
                    IllegalStateException(context.getString(R.string.direct_search_error_no_key)),
            )
        }
        val modelId = userPreferences.getDictionaryModel().trim()
        if (modelId.isBlank()) {
            return Result.failure(
                    IllegalStateException(
                            context.getString(R.string.ai_error_selected_model_unavailable),
                    ),
            )
        }
        val groundingEnabled = userPreferences.isDictionaryGroundingEnabled()
        val thinkingEnabled = userPreferences.isDictionaryThinkingEnabled()
        val advancedPayload = userPreferences.getDictionaryAdvancedPayload()
        val userMessage =
                "Provide a dictionary entry for: ${confirmed.term}. " +
                        "Original user query: ${confirmed.originalQuery}"
        val webSearch =
                prepareWebSearch(
                        userPreferences = userPreferences,
                        searchQuery = confirmed.term,
                        prompt = userMessage,
                        nativeSearchSupported =
                                providerSupportsNativeSearch(providerId) &&
                                        modelSupportsGrounding(
                                            modelId,
                                            provider.fallbackTextModels,
                                            providerId,
                                        ),
                        nativeSearchRequested = groundingEnabled,
                )
        val result =
                provider.fetchAnswer(
                        apiKey = apiKey,
                        context = context,
                        request =
                                LlmRequest(
                                        query = webSearch.prompt,
                                        personalContext = null,
                                        modelId = modelId,
                                        useGroundingWithGoogleSearch = webSearch.useNativeSearch,
                                        thinkingEnabled = thinkingEnabled,
                                        useSystemInstruction = true,
                                        systemInstruction = DICTIONARY_SYSTEM_INSTRUCTION,
                                        responseMimeType = "application/json",
                                        advancedPayloadJson = advancedPayload.second.takeIf { advancedPayload.first },
                                ),
                )
        return result.mapCatching { response ->
            val parsed = parseModelResponse(response.text).getOrElse { throw it }
            parsed to modelId
        }
    }
}

data class DictionaryModelResult(
        val word: String,
        val partOfSpeech: String,
        val meaning: String,
        val example: String,
        val synonyms: List<String>,
)
