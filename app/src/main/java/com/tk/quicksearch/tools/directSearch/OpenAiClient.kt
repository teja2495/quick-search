package com.tk.quicksearch.tools.directSearch

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
 * Lightweight client for fetching direct answers from the OpenAI Chat Completions API.
 * Supports optional web search via web_search_preview tool for supported models.
 */
class OpenAiClient(
    private val apiKey: String,
    private val context: Context,
) {
    companion object {
        private const val LOG_TAG = "OpenAiClient"
        private const val BASE_URL = "https://api.openai.com/v1"
        private const val MODELS_ENDPOINT = "$BASE_URL/models"
        private const val CHAT_ENDPOINT = "$BASE_URL/chat/completions"
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
                            setRequestProperty("Authorization", "Bearer $apiKey")
                            connectTimeout = 15000
                            readTimeout = 20000
                        }
                    try {
                        val responseCode = connection.responseCode
                        val raw = readResponseBody(connection, responseCode)
                        if (responseCode !in 200..299) {
                            val message = parseError(raw) ?: "Failed to load OpenAI models"
                            throw IOException(message)
                        }
                        val root = JSONObject(raw)
                        val data = root.optJSONArray("data") ?: JSONArray()
                        val models = mutableListOf<LlmTextModel>()
                        for (i in 0 until data.length()) {
                            val item = data.optJSONObject(i) ?: continue
                            val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                            if (!OpenAiModelCatalog.isLikelyTextModel(id)) continue
                            models.add(
                                LlmTextModel(
                                    id = id,
                                    displayName = id,
                                    supportsSystemInstructions = true,
                                    supportsGrounding = OpenAiModelCatalog.supportsWebSearch(id),
                                ),
                            )
                        }
                        val deduped = models.distinctBy { it.id }
                        if (deduped.isEmpty()) {
                            OpenAiModelCatalog.FALLBACK_TEXT_MODELS
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
        modelId: String = OpenAiModelCatalog.DEFAULT_MODEL_ID,
        useGroundingWithGoogleSearch: Boolean = OpenAiModelCatalog.DEFAULT_GROUNDING_ENABLED,
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
                        useGrounding = useGroundingWithGoogleSearch && OpenAiModelCatalog.supportsWebSearch(modelId),
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
        useSystemInstruction: Boolean,
        systemInstruction: String?,
    ): Result<String> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(CHAT_ENDPOINT)
            connection =
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $apiKey")
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
                useSystemInstruction = useSystemInstruction,
                systemInstruction = systemInstruction,
            )

            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "OpenAI request: model=$modelId, grounding=$useGrounding")
            }

            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val raw = readResponseBody(connection, responseCode)

            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "OpenAI response: code=$responseCode, length=${raw.length}")
            }

            if (responseCode in 200..299) {
                val answer = extractAnswer(raw)
                    ?: return Result.failure(IllegalStateException("Empty response from OpenAI"))
                Result.success(answer)
            } else {
                val message = parseError(raw) ?: "Request failed ($responseCode)"
                Result.failure(ResponseException(responseCode, message))
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "OpenAI request failed", e)
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
        useSystemInstruction: Boolean,
        systemInstruction: String?,
    ): String {
        val effectiveSystem =
            systemInstruction?.trim()?.takeIf { it.isNotBlank() } ?: SYSTEM_PROMPT
        val isReasoning = OpenAiModelCatalog.isReasoningModel(modelId)

        val messages = JSONArray()

        if (useSystemInstruction) {
            val systemContent = buildString {
                append(effectiveSystem)
                if (!personalContext.isNullOrBlank()) {
                    append("\n\nUser personal context:\n${personalContext.trim()}")
                }
            }
            // Reasoning models use "developer" role for system-level guidance
            val role = if (isReasoning) "developer" else "system"
            messages.put(JSONObject().put("role", role).put("content", systemContent))
        }

        messages.put(JSONObject().put("role", "user").put("content", query))

        val root = JSONObject()
        root.put("model", modelId.trim().ifBlank { OpenAiModelCatalog.DEFAULT_MODEL_ID })
        root.put("messages", messages)

        // Search-preview models and reasoning models do not accept temperature.
        if (!isReasoning && !useGrounding) {
            root.put("temperature", 0.2)
        }

        if (useGrounding) {
            // Web search in Chat Completions requires the dedicated search-preview model variant
            // and the web_search_options parameter (no tools array needed).
            val searchModel = OpenAiModelCatalog.searchPreviewModelFor(modelId)
            if (searchModel != null) {
                root.put("model", searchModel)
            }
            root.put("web_search_options", JSONObject())
        }

        return root.toString()
    }

    private fun extractAnswer(raw: String): String? {
        if (raw.isBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            val choices = root.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val message = choices.getJSONObject(0).optJSONObject("message") ?: return null

            // Content can be a string or an array of content blocks
            val contentValue = message.opt("content") ?: return null
            val text = when (contentValue) {
                is String -> contentValue
                is JSONArray -> {
                    val pieces = mutableListOf<String>()
                    for (i in 0 until contentValue.length()) {
                        val block = contentValue.optJSONObject(i) ?: continue
                        if (block.optString("type") == "text") {
                            val t = block.optString("text")
                            if (t.isNotBlank()) pieces.add(t)
                        }
                    }
                    pieces.joinToString("\n\n")
                }
                else -> return null
            }
            text
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
