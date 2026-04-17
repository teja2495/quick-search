package com.tk.quicksearch.tools.aiSearch

import android.util.Log
import com.tk.quicksearch.BuildConfig
import com.tk.quicksearch.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/**
 * Lightweight client for fetching direct answers from the Gemini API.
 * Uses the public Gemini endpoint so it can be proxied via Firebase AI Logic.
 */
class AiSearchClient(
    private val apiKey: String,
    private val context: android.content.Context,
) {
    companion object {
        private const val LOG_TAG = "AI_REQUEST"
        private const val BASE_API_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val SYSTEM_PROMPT =
            "Return only the direct answer as a single short sentence. " +
                "Provide additional context ONLY when its needed. " +
                "Use plain text with no markdown, bullets, emphasis, or special characters like *, _, `, or ~. " +
                "Whenever a phone number is included, format it in E.164 with country code so it can be dialed directly."
        private const val MAX_ATTEMPTS = 2
        private const val INITIAL_RETRY_DELAY_MS = 750L
        private const val MODELS_ENDPOINT = "$BASE_API_URL/models"

        /** Gemma-style thinking traces embedded in a single text part (e.g. Gemma 4 thinking mode). */
        private val REDACTED_THINKING_BLOCK =
            Regex(
                "<redacted_thinking>.*?</redacted_thinking>",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
            )

        /** Gemma 4+ channel-delimited reasoning blocks preceding the user-visible answer. */
        private val CHANNEL_THINKING_BLOCK =
            Regex("""<\|channel>thought[\s\S]*?<channel\|>""")

        suspend fun fetchAvailableTextModels(apiKey: String, context: android.content.Context): Result<List<GeminiTextModel>> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val models = mutableListOf<GeminiTextModel>()
                    var pageToken: String? = null

                    do {
                        val nextPageQuery =
                            pageToken?.let {
                                "&pageToken=${URLEncoder.encode(it, Charsets.UTF_8.name())}"
                            }.orEmpty()
                        val url = URL("$MODELS_ENDPOINT?key=$apiKey$nextPageQuery")

                        val connection =
                            (url.openConnection() as HttpURLConnection).apply {
                                requestMethod = "GET"
                                connectTimeout = 15000
                                readTimeout = 20000
                            }
                        try {
                            val responseCode = connection.responseCode
                            val rawResponse = readResponseBody(connection, responseCode)
                            if (responseCode !in 200..299) {
                                val message = parseError(rawResponse) ?: context.getString(R.string.error_gemini_load_models_failed)
                                throw IOException(message)
                            }

                            val root = JSONObject(rawResponse)
                            val rawModels = root.optJSONArray("models") ?: JSONArray()
                            for (index in 0 until rawModels.length()) {
                                val item = rawModels.optJSONObject(index) ?: continue
                                val name = item.optString("name")
                                if (!name.startsWith("models/")) continue

                                val modelId = name.removePrefix("models/")
                                if (!GeminiModelCatalog.isLikelyTextModel(modelId)) continue

                                val supportsGenerateContent =
                                    item
                                        .optJSONArray("supportedGenerationMethods")
                                        ?.let { methods ->
                                            (0 until methods.length()).any {
                                                methods.optString(it) == "generateContent"
                                            }
                                        } ?: false
                                if (!supportsGenerateContent) continue

                                val displayName =
                                    item.optString("displayName").takeIf { it.isNotBlank() } ?: modelId
                                val isGemma = modelId.lowercase().startsWith("gemma-")
                                models.add(
                                    GeminiTextModel(
                                        id = modelId,
                                        displayName = displayName,
                                        supportsSystemInstructions = !isGemma,
                                        supportsGrounding = !isGemma,
                                    ),
                                )
                            }

                            pageToken = root.optString("nextPageToken").takeIf { it.isNotBlank() }
                        } finally {
                            connection.disconnect()
                        }
                    } while (!pageToken.isNullOrBlank())

                    val deduped = models.distinctBy { it.id }
                    if (deduped.isEmpty()) {
                        GeminiModelCatalog.FALLBACK_TEXT_MODELS
                    } else {
                        val withDefault =
                            if (deduped.any { it.id == GeminiModelCatalog.DEFAULT_MODEL_ID }) {
                                deduped
                            } else {
                                deduped +
                                    GeminiTextModel(
                                        id = GeminiModelCatalog.DEFAULT_MODEL_ID,
                                        displayName = context.getString(R.string.gemini_model_flash_latest),
                                    )
                            }
                        withDefault.sortedBy { it.displayName.lowercase() }
                    }
                }
            }

        private fun readResponseBody(
            connection: HttpURLConnection,
            responseCode: Int,
        ): String {
            val stream =
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                } ?: return ""

            return BufferedReader(InputStreamReader(stream)).use { reader ->
                buildString {
                    var line = reader.readLine()
                    while (line != null) {
                        append(line)
                        line = reader.readLine()
                    }
                }
            }
        }

        private fun parseError(rawResponse: String): String? {
            if (rawResponse.isBlank()) return null
            return runCatching {
                val root = JSONObject(rawResponse)
                val error = root.optJSONObject("error") ?: return null
                error.optString("message").takeIf { it.isNotBlank() }
            }.getOrNull()
        }
    }

    suspend fun fetchAnswer(
        query: String,
        personalContext: String? = null,
        modelId: String = GeminiModelCatalog.DEFAULT_MODEL_ID,
        useGroundingWithGoogleSearch: Boolean = GeminiModelCatalog.DEFAULT_GROUNDING_ENABLED,
        thinkingEnabled: Boolean = false,
        useSystemInstruction: Boolean = true,
        systemInstruction: String? = null,
        responseMimeType: String = "text/plain",
    ): Result<String> =
        withContext(Dispatchers.IO) {
            var attempt = 1
            var delayMs = INITIAL_RETRY_DELAY_MS
            var lastError: Throwable? = null
            var groundingEnabledForAttempt = useGroundingWithGoogleSearch

            while (attempt <= MAX_ATTEMPTS) {
                val result =
                    executeRequest(
                        query = query,
                        personalContext = personalContext,
                        modelId = modelId,
                        useGroundingWithGoogleSearch = groundingEnabledForAttempt,
                        thinkingEnabled = thinkingEnabled,
                        useSystemInstruction = useSystemInstruction,
                        systemInstruction = systemInstruction,
                        responseMimeType = responseMimeType,
                    )
                if (result.isSuccess) return@withContext result

                lastError = result.exceptionOrNull()
                if (shouldFallbackToUngrounded(lastError, groundingEnabledForAttempt)) {
                    groundingEnabledForAttempt = false
                    if (BuildConfig.DEBUG) {
                        Log.d(LOG_TAG, "Retrying Gemini request without grounding after quota/rate-limit style error")
                    }
                    continue
                }
                if (attempt == MAX_ATTEMPTS || !shouldRetry(lastError)) {
                    return@withContext Result.failure(lastError ?: IllegalStateException("Unknown error"))
                }

                delay(delayMs)
                delayMs *= 2
                attempt++
            }

            Result.failure(lastError ?: IllegalStateException("Unknown error"))
        }

    private fun executeRequest(
        query: String,
        personalContext: String?,
        modelId: String,
        useGroundingWithGoogleSearch: Boolean,
        thinkingEnabled: Boolean,
        useSystemInstruction: Boolean,
        systemInstruction: String? = null,
        responseMimeType: String = "text/plain",
    ): Result<String> {
        var connection: HttpURLConnection? = null
        return try {
            val endpointModelId = modelId.trim().ifBlank { GeminiModelCatalog.DEFAULT_MODEL_ID }
            val url = URL("$BASE_API_URL/models/$endpointModelId:generateContent?key=$apiKey")
            connection =
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 20000
                }

            val payload =
                buildRequestBody(
                    query = query,
                    personalContext = personalContext,
                    useGroundingWithGoogleSearch = useGroundingWithGoogleSearch,
                    thinkingEnabled = thinkingEnabled,
                    useSystemInstruction = useSystemInstruction,
                    systemInstructionOverride = systemInstruction,
                    responseMimeType = responseMimeType,
                )
            logRequestDiagnostics(
                endpointModelId = endpointModelId,
                queryLength = query.length,
                hasPersonalContext = !personalContext.isNullOrBlank(),
                useGroundingWithGoogleSearch = useGroundingWithGoogleSearch,
                thinkingEnabled = thinkingEnabled,
                useSystemInstruction = useSystemInstruction,
                payload = payload,
            )
            connection.outputStream.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val responseCode = connection.responseCode
            val rawResponse = readResponseBody(connection, responseCode)
            logResponseDiagnostics(
                responseCode = responseCode,
                responseLength = rawResponse.length,
            )

            if (responseCode in 200..299) {
                val answer =
                    extractAnswer(rawResponse) ?: return Result.failure(
                        IllegalStateException(context.getString(R.string.error_gemini_empty_response)),
                    )
                Result.success(answer)
            } else {
                val message = parseError(rawResponse) ?: context.getString(R.string.error_gemini_request_failed, responseCode)
                Result.failure(ResponseException(responseCode, message))
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Gemini request failed", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun logRequestDiagnostics(
        endpointModelId: String,
        queryLength: Int,
        hasPersonalContext: Boolean,
        useGroundingWithGoogleSearch: Boolean,
        thinkingEnabled: Boolean,
        useSystemInstruction: Boolean,
        payload: String,
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            LOG_TAG,
            "Gemini request: model=$endpointModelId, queryLength=$queryLength, hasPersonalContext=$hasPersonalContext, " +
                "groundingEnabled=$useGroundingWithGoogleSearch, thinkingEnabled=$thinkingEnabled, systemInstructionEnabled=$useSystemInstruction",
        )
        Log.d(
            LOG_TAG,
            "Gemini request path: POST $BASE_API_URL/models/$endpointModelId:generateContent?key=[REDACTED_API_KEY]",
        )
        Log.d(LOG_TAG, "Gemini request payload: ${redactApiKeyForLogging(payload, apiKey)}")
    }

    private fun logResponseDiagnostics(
        responseCode: Int,
        responseLength: Int,
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            LOG_TAG,
            "Gemini response: code=$responseCode, bodyLength=$responseLength",
        )
    }

    private fun buildRequestBody(
        query: String,
        personalContext: String?,
        useGroundingWithGoogleSearch: Boolean,
        thinkingEnabled: Boolean,
        useSystemInstruction: Boolean,
        systemInstructionOverride: String? = null,
        responseMimeType: String = "text/plain",
    ): String {
        val effectiveSystemPrompt =
            systemInstructionOverride?.trim()?.takeIf { it.isNotBlank() } ?: SYSTEM_PROMPT
        val promptPrefix =
            when {
                useSystemInstruction -> null
                else -> buildString {
                    append(effectiveSystemPrompt)
                    if (!personalContext.isNullOrBlank()) {
                        append("\n\nUser personal context:\n${personalContext.trim()}")
                    }
                    append("\n\nUser query: ")
                }
            }
        val contentParts =
            JSONArray().apply {
                if (promptPrefix != null) {
                    put(JSONObject().put("text", promptPrefix + query))
                } else {
                    put(JSONObject().put("text", query))
                }
            }
        val content =
            JSONObject().apply {
                put("parts", contentParts)
            }
        val generationConfig =
            JSONObject().apply {
                put("responseMimeType", responseMimeType)
                put("temperature", 0.2)
                put(
                    "thinkingConfig",
                    JSONObject().apply {
                        put("includeThoughts", thinkingEnabled)
                    },
                )
            }

        val root = JSONObject()
        if (useSystemInstruction) {
            val systemInstructionJson =
                JSONObject().apply {
                    val systemParts = JSONArray()
                    systemParts.put(JSONObject().put("text", effectiveSystemPrompt))
                    if (!personalContext.isNullOrBlank()) {
                        systemParts.put(
                            JSONObject().put(
                                "text",
                                "\n\nUser personal context:\n${personalContext.trim()}",
                            ),
                        )
                    }
                    put("parts", systemParts)
                }
            root.put("systemInstruction", systemInstructionJson)
        }
        root.put("contents", JSONArray().put(content))
        if (useGroundingWithGoogleSearch) {
            root.put(
                "tools",
                JSONArray().put(
                    JSONObject().put("google_search", JSONObject()),
                ),
            )
        }
        root.put("generationConfig", generationConfig)
        return root.toString()
    }

    private fun stripInlineThinkingMarkers(text: String): String {
        var t = REDACTED_THINKING_BLOCK.replace(text, "")
        t = CHANNEL_THINKING_BLOCK.replace(t, "")
        return t.trim()
    }

    private fun extractAnswer(rawResponse: String): String? {
        if (rawResponse.isBlank()) return null
        return runCatching {
            val root = JSONObject(rawResponse)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            val textPieces = ArrayList<String>(parts.length())
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.optBoolean("thought", false)) continue
                val segment = part.optString("text")
                if (segment.isNotBlank()) textPieces.add(segment)
            }
            if (textPieces.isEmpty()) return null
            val rawAnswer = stripInlineThinkingMarkers(textPieces.joinToString("\n\n"))
            val sanitizedAnswer = rawAnswer.replace("*", "").trim()
            // Format temperature units properly
            val formattedAnswer =
                sanitizedAnswer
                    .replace(Regex("degrees?\\s+Fahrenheit", RegexOption.IGNORE_CASE), "°F")
                    .replace(Regex("degrees?\\s+Celsius", RegexOption.IGNORE_CASE), "°C")
                    .replace(Regex("degrees?\\s+F", RegexOption.IGNORE_CASE), "°F")
                    .replace(Regex("degrees?\\s+C", RegexOption.IGNORE_CASE), "°C")
            formattedAnswer.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun shouldRetry(error: Throwable?): Boolean =
        when (error) {
            is ResponseException -> error.code == 429 || error.code >= 500
            is IOException -> true
            else -> false
        }

    private fun shouldFallbackToUngrounded(
        error: Throwable?,
        groundingEnabledForAttempt: Boolean,
    ): Boolean {
        if (!groundingEnabledForAttempt) return false
        val responseError = error as? ResponseException ?: return false
        if (responseError.code !in setOf(429, 400, 403, 503)) return false

        val normalized = responseError.message.lowercase()
        return normalized.contains("quota") ||
            normalized.contains("rate limit")
    }

    private data class ResponseException(
        val code: Int,
        override val message: String,
    ) : Exception(message)
}
