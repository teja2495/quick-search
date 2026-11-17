package com.tk.quicksearch.tools.aiSearch

import android.content.Context
import android.util.Log
import com.tk.quicksearch.BuildConfig
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/** Lightweight client for Meta Model API's OpenAI-compatible Responses API. */
class MetaClient(
    private val apiKey: String,
    @Suppress("unused") private val context: Context,
) {
    companion object {
        private const val LOG_TAG = "AI_REQUEST"
        private const val BASE_URL = "https://api.meta.ai/v1"
        private const val MODELS_ENDPOINT = "$BASE_URL/models"
        private const val RESPONSES_ENDPOINT = "$BASE_URL/responses"
        private const val SYSTEM_PROMPT =
            "Return only the direct answer as a single short sentence. " +
                "Provide additional context ONLY when its needed. " +
                "Use plain text with no markdown, bullets, emphasis, or special characters like *, _, `, or ~. " +
                "Whenever a phone number is included, format it in E.164 with country code so it can be dialed directly."
        private const val MAX_ATTEMPTS = 2
        private const val INITIAL_RETRY_DELAY_MS = 750L

        suspend fun fetchAvailableTextModels(
            apiKey: String,
            @Suppress("UNUSED_PARAMETER") context: Context,
        ): Result<List<LlmTextModel>> =
            withCancellableHttp {
                runCatching {
                    val connection =
                        (URL(MODELS_ENDPOINT).openConnection() as HttpURLConnection).trackCancellation().apply {
                            requestMethod = "GET"
                            setRequestProperty("Authorization", "Bearer $apiKey")
                            connectTimeout = 15000
                            readTimeout = 20000
                        }
                    try {
                        val responseCode = connection.responseCode
                        val raw = readResponseBody(connection, responseCode)
                        if (responseCode !in 200..299) {
                            throw IOException(parseError(raw) ?: "Failed to load Meta AI models")
                        }
                        val data = JSONObject(raw).optJSONArray("data") ?: JSONArray()
                        buildList {
                            for (index in 0 until data.length()) {
                                val item = data.optJSONObject(index) ?: continue
                                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                                if (!MetaModelCatalog.isTextModel(id)) continue
                                add(
                                    LlmTextModel(
                                        id = id,
                                        displayName =
                                            item.optString("display_name").takeIf { it.isNotBlank() }
                                                ?: item.optString("name").takeIf { it.isNotBlank() }
                                                ?: MetaModelCatalog.displayNameFor(id),
                                        supportsSystemInstructions = true,
                                        supportsGrounding = true,
                                    ),
                                )
                            }
                        }.distinctBy { it.id }
                            .sortedBy { it.displayName.lowercase() }
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
                JSONObject(raw).optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
            }.getOrNull()
        }
    }

    suspend fun fetchAnswer(
        query: String,
        personalContext: String? = null,
        modelId: String = MetaModelCatalog.DEFAULT_MODEL_ID,
        useGrounding: Boolean = MetaModelCatalog.DEFAULT_GROUNDING_ENABLED,
        thinkingEnabled: Boolean = false,
        useSystemInstruction: Boolean = true,
        systemInstruction: String? = null,
        history: List<AiConversationTurn> = emptyList(),
        responseMimeType: String = "text/plain",
    ): Result<String> =
        withCancellableHttp {
            var attempt = 1
            var retryDelayMs = INITIAL_RETRY_DELAY_MS
            var lastError: Throwable? = null
            while (attempt <= MAX_ATTEMPTS) {
                val result =
                    executeRequest(
                        query = query,
                        personalContext = personalContext,
                        modelId = modelId,
                        useGrounding = useGrounding,
                        thinkingEnabled = thinkingEnabled,
                        useSystemInstruction = useSystemInstruction,
                        systemInstruction = systemInstruction,
                        history = history,
                        responseMimeType = responseMimeType,
                    )
                if (result.isSuccess) return@withCancellableHttp result
                lastError = result.exceptionOrNull()
                if (attempt == MAX_ATTEMPTS || !shouldRetry(lastError)) {
                    return@withCancellableHttp Result.failure(lastError ?: IllegalStateException("Unknown error"))
                }
                delay(retryDelayMs)
                retryDelayMs *= 2
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
        history: List<AiConversationTurn>,
        responseMimeType: String,
    ): Result<String> {
        var connection: HttpURLConnection? = null
        return try {
            connection =
                (URL(RESPONSES_ENDPOINT).openConnection() as HttpURLConnection).trackCancellation().apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 60000
                }
            val payload =
                buildRequestBody(
                    query = query,
                    personalContext = personalContext,
                    modelId = modelId,
                    useGrounding = useGrounding,
                    thinkingEnabled = thinkingEnabled,
                    useSystemInstruction = useSystemInstruction,
                    systemInstruction = systemInstruction,
                    history = history,
                    responseMimeType = responseMimeType,
                )
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Meta AI request: model=$modelId, grounding=$useGrounding, thinking=$thinkingEnabled")
                Log.d(LOG_TAG, "Meta AI request payload: ${redactApiKeyForLogging(payload, apiKey)}")
            }
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val raw = readResponseBody(connection, responseCode)
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Meta AI response: code=$responseCode, length=${raw.length}")
            }
            if (responseCode in 200..299) {
                Result.success(
                    extractAnswer(raw)
                        ?: return Result.failure(IllegalStateException("Empty response from Meta AI")),
                )
            } else {
                Result.failure(ResponseException(responseCode, parseError(raw) ?: "Request failed ($responseCode)"))
            }
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Meta AI request failed", error)
            Result.failure(error)
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
        history: List<AiConversationTurn>,
        responseMimeType: String,
    ): String =
        JSONObject().apply {
            put("model", modelId.trim().ifBlank { MetaModelCatalog.DEFAULT_MODEL_ID })
            if (history.isEmpty()) {
                put("input", query)
            } else {
                put(
                    "input",
                    JSONArray()
                        .putChatHistory(history)
                        .put(JSONObject().put("role", "user").put("content", query)),
                )
            }
            put("store", false)
            if (useSystemInstruction) {
                val instructions =
                    buildString {
                        append(systemInstruction?.trim()?.takeIf { it.isNotBlank() } ?: SYSTEM_PROMPT)
                        if (!personalContext.isNullOrBlank()) {
                            append("\n\nUser personal context:\n${personalContext.trim()}")
                        }
                    }
                put("instructions", instructions)
            }
            if (useGrounding) {
                put("tools", JSONArray().put(JSONObject().put("type", "web_search")))
            }
            if (thinkingEnabled) {
                put("reasoning", JSONObject().put("effort", "high"))
            }
            if (responseMimeType.equals("application/json", ignoreCase = true)) {
                put("text", JSONObject().put("format", JSONObject().put("type", "json_object")))
            }
        }.toString()

    private fun extractAnswer(raw: String): String? {
        if (raw.isBlank()) return null
        return runCatching {
            val output = JSONObject(raw).optJSONArray("output") ?: return null
            val pieces = mutableListOf<String>()
            for (itemIndex in 0 until output.length()) {
                val item = output.optJSONObject(itemIndex) ?: continue
                if (item.optString("type") != "message") continue
                val content = item.optJSONArray("content") ?: continue
                for (contentIndex in 0 until content.length()) {
                    val block = content.optJSONObject(contentIndex) ?: continue
                    if (block.optString("type") == "output_text") {
                        block.optString("text").takeIf { it.isNotBlank() }?.let(pieces::add)
                    }
                }
            }
            pieces.joinToString("\n\n")
                .replace("*", "")
                .replace(Regex("degrees?\\s+Fahrenheit", RegexOption.IGNORE_CASE), "°F")
                .replace(Regex("degrees?\\s+Celsius", RegexOption.IGNORE_CASE), "°C")
                .replace(Regex("degrees?\\s+F(?=\\b)", RegexOption.IGNORE_CASE), "°F")
                .replace(Regex("degrees?\\s+C(?=\\b)", RegexOption.IGNORE_CASE), "°C")
                .trim()
                .takeIf { it.isNotBlank() }
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
