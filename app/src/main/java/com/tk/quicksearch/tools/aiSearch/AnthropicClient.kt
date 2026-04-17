package com.tk.quicksearch.tools.aiSearch

import android.content.Context
import android.util.Log
import com.tk.quicksearch.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight client for fetching direct answers from the Anthropic Messages API.
 * Supports optional web search via the web_search_20250305 server-side tool.
 */
class AnthropicClient(
    private val apiKey: String,
    private val context: Context,
) {
    companion object {
        private const val LOG_TAG = "AI_REQUEST"
        private const val BASE_URL = "https://api.anthropic.com/v1"
        private const val MODELS_ENDPOINT = "$BASE_URL/models"
        private const val MESSAGES_ENDPOINT = "$BASE_URL/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val ANTHROPIC_BETA = "interleaved-thinking-2025-05-14"
        private const val MAX_TOKENS = 1024
        private const val SYSTEM_PROMPT =
            "Return only the direct answer as a single short sentence. " +
                "Provide additional context ONLY when its needed. " +
                "Use plain text with no markdown, bullets, emphasis, or special characters like *, _, `, or ~. " +
                "Whenever a phone number is included, format it in E.164 with country code so it can be dialed directly."
        private const val MAX_ATTEMPTS = 2
        private const val INITIAL_RETRY_DELAY_MS = 750L

        suspend fun fetchAvailableTextModels(
            apiKey: String,
            context: Context,
        ): Result<List<LlmTextModel>> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val url = URL(MODELS_ENDPOINT)
                    val connection =
                        (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            setRequestProperty("x-api-key", apiKey)
                            setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
                            connectTimeout = 15000
                            readTimeout = 20000
                        }
                    try {
                        val responseCode = connection.responseCode
                        val raw = readResponseBody(connection, responseCode)
                        if (responseCode !in 200..299) {
                            val message = parseError(raw) ?: "Failed to load Anthropic models"
                            throw IOException(message)
                        }
                        val root = JSONObject(raw)
                        val data = root.optJSONArray("data") ?: JSONArray()
                        val models = mutableListOf<LlmTextModel>()
                        for (i in 0 until data.length()) {
                            val item = data.optJSONObject(i) ?: continue
                            val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                            if (!AnthropicModelCatalog.isLikelyTextModel(id)) continue
                            val displayName = item.optString("display_name").takeIf { it.isNotBlank() } ?: id
                            models.add(
                                LlmTextModel(
                                    id = id,
                                    displayName = displayName,
                                    supportsSystemInstructions = true,
                                    supportsGrounding = true,
                                ),
                            )
                        }
                        val deduped = models.distinctBy { it.id }
                        if (deduped.isEmpty()) {
                            AnthropicModelCatalog.FALLBACK_TEXT_MODELS
                        } else {
                            deduped.sortedBy { it.displayName.lowercase() }
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
            }

        private fun readResponseBody(connection: HttpURLConnection, responseCode: Int): String {
            val stream =
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
                    ?: return ""
            return BufferedReader(InputStreamReader(stream)).use { it.readText() }
        }

        private fun parseError(raw: String): String? {
            if (raw.isBlank()) return null
            return runCatching {
                val root = JSONObject(raw)
                val error = root.optJSONObject("error") ?: return null
                error.optString("message").takeIf { it.isNotBlank() }
            }.getOrNull()
        }
    }

    suspend fun fetchAnswer(
        query: String,
        personalContext: String? = null,
        modelId: String = AnthropicModelCatalog.DEFAULT_MODEL_ID,
        useGroundingWithGoogleSearch: Boolean = AnthropicModelCatalog.DEFAULT_GROUNDING_ENABLED,
        thinkingEnabled: Boolean = false,
        useSystemInstruction: Boolean = true,
        systemInstruction: String? = null,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            var attempt = 1
            var delayMs = INITIAL_RETRY_DELAY_MS
            var lastError: Throwable? = null

            while (attempt <= MAX_ATTEMPTS) {
                val result =
                    executeRequest(
                        query = query,
                        personalContext = personalContext,
                        modelId = modelId,
                        useGrounding = useGroundingWithGoogleSearch,
                        thinkingEnabled = thinkingEnabled,
                        useSystemInstruction = useSystemInstruction,
                        systemInstruction = systemInstruction,
                    )
                if (result.isSuccess) return@withContext result

                lastError = result.exceptionOrNull()
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
        useGrounding: Boolean,
        thinkingEnabled: Boolean,
        useSystemInstruction: Boolean,
        systemInstruction: String?,
    ): Result<String> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(MESSAGES_ENDPOINT)
            connection =
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("x-api-key", apiKey)
                    setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
                    if (thinkingEnabled) {
                        setRequestProperty("anthropic-beta", ANTHROPIC_BETA)
                    }
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 30000
                }

            val payload = buildRequestBody(
                query = query,
                personalContext = personalContext,
                modelId = modelId,
                useGrounding = useGrounding,
                thinkingEnabled = thinkingEnabled,
                useSystemInstruction = useSystemInstruction,
                systemInstruction = systemInstruction,
            )

            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Anthropic request: model=$modelId, grounding=$useGrounding, thinking=$thinkingEnabled")
                Log.d(LOG_TAG, "Anthropic request payload: ${redactApiKeyForLogging(payload, apiKey)}")
            }

            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val raw = readResponseBody(connection, responseCode)

            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Anthropic response: code=$responseCode, length=${raw.length}")
            }

            if (responseCode in 200..299) {
                val answer = extractAnswer(raw)
                    ?: return Result.failure(IllegalStateException("Empty response from Claude"))
                Result.success(answer)
            } else {
                val message = parseError(raw) ?: "Request failed ($responseCode)"
                Result.failure(ResponseException(responseCode, message))
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Anthropic request failed", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildRequestBody(
        query: String,
        personalContext: String?,
        modelId: String,
        useGrounding: Boolean,
        thinkingEnabled: Boolean,
        useSystemInstruction: Boolean,
        systemInstruction: String?,
    ): String {
        val effectiveSystem =
            systemInstruction?.trim()?.takeIf { it.isNotBlank() } ?: SYSTEM_PROMPT

        val root = JSONObject()
        root.put("model", modelId.trim().ifBlank { AnthropicModelCatalog.DEFAULT_MODEL_ID })
        root.put("max_tokens", MAX_TOKENS)

        if (useSystemInstruction) {
            val systemContent = buildString {
                append(effectiveSystem)
                if (!personalContext.isNullOrBlank()) {
                    append("\n\nUser personal context:\n${personalContext.trim()}")
                }
            }
            root.put("system", systemContent)
        }

        val userContent = if (!useSystemInstruction) {
            buildString {
                append(effectiveSystem)
                if (!personalContext.isNullOrBlank()) {
                    append("\n\nUser personal context:\n${personalContext.trim()}")
                }
                append("\n\nUser query: $query")
            }
        } else {
            query
        }

        root.put(
            "messages",
            JSONArray().put(JSONObject().put("role", "user").put("content", userContent)),
        )

        if (useGrounding) {
            root.put(
                "tools",
                JSONArray().put(
                    JSONObject()
                        .put("type", "web_search_20250305")
                        .put("name", "web_search")
                        .put("max_uses", 1),
                ),
            )
        }

        if (thinkingEnabled) {
            root.put(
                "thinking",
                JSONObject()
                    .put("type", "enabled")
                    .put("budget_tokens", 1024),
            )
        }

        return root.toString()
    }

    private fun extractAnswer(raw: String): String? {
        if (raw.isBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            val content = root.optJSONArray("content") ?: return null
            val pieces = mutableListOf<String>()
            for (i in 0 until content.length()) {
                val block = content.optJSONObject(i) ?: continue
                // Only collect text blocks; skip tool_use, tool_result, thinking blocks
                if (block.optString("type") == "text") {
                    val text = block.optString("text")
                    if (text.isNotBlank()) pieces.add(text)
                }
            }
            if (pieces.isEmpty()) return null
            pieces
                .joinToString("\n\n")
                .replace("*", "")
                .replace(Regex("degrees?\\s+Fahrenheit", RegexOption.IGNORE_CASE), "°F")
                .replace(Regex("degrees?\\s+Celsius", RegexOption.IGNORE_CASE), "°C")
                .replace(Regex("degrees?\\s+F(?=\\b)", RegexOption.IGNORE_CASE), "°F")
                .replace(Regex("degrees?\\s+C(?=\\b)", RegexOption.IGNORE_CASE), "°C")
                .trim()
                .takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun readResponseBody(connection: HttpURLConnection, responseCode: Int): String {
        val stream =
            if (responseCode in 200..299) connection.inputStream else connection.errorStream
                ?: return ""
        return BufferedReader(InputStreamReader(stream)).use { it.readText() }
    }

    private fun parseError(raw: String): String? {
        if (raw.isBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            val error = root.optJSONObject("error") ?: return null
            error.optString("message").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun shouldRetry(error: Throwable?): Boolean =
        when (error) {
            is ResponseException -> error.code == 429 || error.code >= 500
            is IOException -> true
            else -> false
        }

    private data class ResponseException(val code: Int, override val message: String) : Exception(message)
}
