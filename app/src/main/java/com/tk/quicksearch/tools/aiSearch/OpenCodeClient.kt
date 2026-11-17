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
import java.util.UUID

/**
 * Lightweight client for OpenCode Zen and OpenCode Go. Each model is only served on its own wire
 * format (OpenAI chat/completions, OpenAI responses, or Anthropic messages), so requests are
 * routed per model. Web search grounding is not supported.
 *
 * Header sources:
 * - `Authorization: Bearer` / `x-api-key`: the `parseApiKey` of each route in
 *   anomalyco/opencode `packages/console/app/src/routes/zen/{,go/}v1/`.
 * - `User-Agent`, `x-opencode-session`: opencode.ai/docs/go ("Where can I use it?").
 * - `x-opencode-request`, `x-opencode-client`: read by
 *   `packages/console/app/src/routes/zen/util/handler.ts` and sent by OpenCode itself in
 *   `packages/opencode/src/session/llm/request.ts`.
 */
class OpenCodeClient(
    private val apiKey: String,
    private val context: Context,
) {
    companion object {
        private const val LOG_TAG = "AI_REQUEST"
        private const val CLIENT_NAME = "quick-search"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        // Higher than AnthropicClient's limit: several messages-format models here think before
        // answering, and a tight budget ends the reply before any text block.
        private const val MAX_TOKENS = 4096
        private const val FORMAT_MISMATCH_MARKER = "not supported for format"
        private const val SYSTEM_PROMPT =
            "Return only the direct answer as a single short sentence. " +
                "Provide additional context ONLY when its needed. " +
                "Use plain text with no markdown, bullets, emphasis, or special characters like *, _, `, or ~. " +
                "Whenever a phone number is included, format it in E.164 with country code so it can be dialed directly."
        private const val MAX_ATTEMPTS = 2
        private const val INITIAL_RETRY_DELAY_MS = 750L

        /** `<client>/<version>`, as opencode.ai/docs/go asks instead of a generic HTTP-library name. */
        internal val USER_AGENT = "$CLIENT_NAME/${BuildConfig.VERSION_NAME}"

        suspend fun fetchAvailableTextModels(
            apiKey: String,
            context: Context,
        ): Result<List<LlmTextModel>> =
            withCancellableHttp {
                val results = OpenCodeTier.entries.map { tier -> tier to fetchTierModelIds(apiKey, tier) }
                if (results.all { it.second.isFailure }) {
                    return@withCancellableHttp Result.failure(
                        results.first().second.exceptionOrNull() ?: IOException("Failed to load OpenCode models"),
                    )
                }
                runCatching {
                    results
                        .flatMap { (tier, ids) ->
                            ids.getOrDefault(emptyList())
                                .filter { OpenCodeModelCatalog.apiFormatFor(tier, it) != null }
                                .map { OpenCodeModelCatalog.textModel(tier, it) }
                        }.distinctBy { it.id }
                        .sortedBy { it.displayName.lowercase() }
                }
            }

        private fun fetchTierModelIds(
            apiKey: String,
            tier: OpenCodeTier,
        ): Result<List<String>> =
            runCatching {
                val url = URL("${tier.baseUrl}/models")
                val connection =
                    (url.openConnection() as HttpURLConnection).trackCancellation().apply {
                        requestMethod = "GET"
                        setRequestProperty("Authorization", "Bearer $apiKey")
                        setRequestProperty("User-Agent", USER_AGENT)
                        connectTimeout = 15000
                        readTimeout = 20000
                    }
                try {
                    val responseCode = connection.responseCode
                    val raw = readResponseBody(connection, responseCode)
                    if (responseCode !in 200..299) {
                        val message = parseError(raw) ?: "Failed to load OpenCode models"
                        throw IOException(message)
                    }
                    val data = JSONObject(raw).optJSONArray("data") ?: JSONArray()
                    (0 until data.length()).mapNotNull { i ->
                        data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }
                    }
                } finally {
                    connection.disconnect()
                }
            }

        /** Request headers for one call; the session stays fixed for a conversation, the request id per question. */
        internal fun buildHeaders(
            apiKey: String,
            format: OpenCodeApiFormat,
            sessionId: String,
            requestId: String,
        ): Map<String, String> =
            buildMap {
                if (format == OpenCodeApiFormat.MESSAGES) {
                    put("x-api-key", apiKey)
                    put("anthropic-version", ANTHROPIC_VERSION)
                } else {
                    put("Authorization", "Bearer $apiKey")
                }
                put("Content-Type", "application/json")
                put("User-Agent", USER_AGENT)
                put("x-opencode-session", sessionId)
                put("x-opencode-request", requestId)
                put("x-opencode-client", CLIENT_NAME)
            }

        internal fun buildRequestBody(
            format: OpenCodeApiFormat,
            modelId: String,
            query: String,
            personalContext: String?,
            useSystemInstruction: Boolean,
            systemInstruction: String?,
            history: List<AiConversationTurn>,
            advancedPayloadJson: String?,
        ): String {
            val effectiveSystem =
                systemInstruction?.trim()?.takeIf { it.isNotBlank() } ?: SYSTEM_PROMPT
            val systemContent =
                buildString {
                    append(effectiveSystem)
                    if (!personalContext.isNullOrBlank()) {
                        append("\n\nUser personal context:\n${personalContext.trim()}")
                    }
                }

            val messages = JSONArray()
            if (useSystemInstruction && format == OpenCodeApiFormat.CHAT_COMPLETIONS) {
                messages.put(JSONObject().put("role", "system").put("content", systemContent))
            }
            messages.putChatHistory(history)
            messages.put(JSONObject().put("role", "user").put("content", query))

            val root = JSONObject().put("model", modelId.trim())
            when (format) {
                OpenCodeApiFormat.CHAT_COMPLETIONS -> root.put("messages", messages)
                OpenCodeApiFormat.MESSAGES -> {
                    root.put("max_tokens", MAX_TOKENS)
                    if (useSystemInstruction) root.put("system", systemContent)
                    root.put("messages", messages)
                }
                OpenCodeApiFormat.RESPONSES -> {
                    if (useSystemInstruction) root.put("instructions", systemContent)
                    root.put("input", messages)
                }
            }
            mergeAdvancedPayload(root, advancedPayloadJson)
            return root.toString()
        }

        private fun mergeAdvancedPayload(
            root: JSONObject,
            advancedPayloadJson: String?,
        ) {
            val trimmed = advancedPayloadJson?.trim().takeUnless { it.isNullOrBlank() } ?: return
            val advancedPayload = JSONObject(trimmed)
            val protectedKeys = setOf("model", "messages", "input")
            advancedPayload.keys().forEach { key ->
                if (key in protectedKeys) return@forEach
                root.put(key, advancedPayload.get(key))
            }
        }

        /** Text of a successful response in [format]; skips reasoning, thinking, and tool blocks. */
        internal fun extractAnswer(
            format: OpenCodeApiFormat,
            raw: String,
        ): String? {
            if (raw.isBlank()) return null
            return runCatching {
                val root = JSONObject(raw)
                val text =
                    when (format) {
                        OpenCodeApiFormat.CHAT_COMPLETIONS -> {
                            val choices = root.optJSONArray("choices") ?: return null
                            if (choices.length() == 0) return null
                            choices.getJSONObject(0).optJSONObject("message")?.optString("content")
                        }
                        OpenCodeApiFormat.MESSAGES -> textBlocks(root.optJSONArray("content"), "text")
                        OpenCodeApiFormat.RESPONSES -> {
                            val output = root.optJSONArray("output") ?: JSONArray()
                            (0 until output.length())
                                .mapNotNull { output.optJSONObject(it) }
                                .filter { it.optString("type") == "message" }
                                .mapNotNull { textBlocks(it.optJSONArray("content"), "output_text") }
                                .joinToString("\n\n")
                        }
                    }?.takeIf { it.isNotBlank() } ?: return null
                LlmResponseText.stripThinkingMarkers(text)
                    .replace("*", "")
                    .replace(Regex("degrees?\\s+Fahrenheit", RegexOption.IGNORE_CASE), "°F")
                    .replace(Regex("degrees?\\s+Celsius", RegexOption.IGNORE_CASE), "°C")
                    .replace(Regex("degrees?\\s+F(?=\\b)", RegexOption.IGNORE_CASE), "°F")
                    .replace(Regex("degrees?\\s+C(?=\\b)", RegexOption.IGNORE_CASE), "°C")
                    .trim()
                    .takeIf { it.isNotBlank() }
            }.getOrNull()
        }

        private fun textBlocks(
            blocks: JSONArray?,
            type: String,
        ): String? {
            blocks ?: return null
            return (0 until blocks.length())
                .mapNotNull { blocks.optJSONObject(it) }
                .filter { it.optString("type") == type }
                .map { it.optString("text") }
                .filter { it.isNotBlank() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString("\n\n")
        }

        private fun readResponseBody(connection: HttpURLConnection, responseCode: Int): String {
            val stream =
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
                    ?: return ""
            return BufferedReader(InputStreamReader(stream)).use { it.readText() }
        }

        /** OpenAI and Anthropic formats both nest the message under `error`. */
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
        modelRef: String,
        sessionId: String?,
        useSystemInstruction: Boolean = true,
        systemInstruction: String? = null,
        history: List<AiConversationTurn> = emptyList(),
        advancedPayloadJson: String? = null,
    ): Result<String> =
        withCancellableHttp {
            val (tier, modelId) = OpenCodeModelCatalog.parseModelRef(modelRef)
            val primaryFormat =
                OpenCodeModelCatalog.apiFormatFor(tier, modelId)
                    ?: return@withCancellableHttp Result.failure(IllegalStateException("Model $modelId is not supported"))
            // One id per question, reused across retries and format fallbacks of that question.
            val requestId = UUID.randomUUID().toString()
            val session = sessionId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
            // The format table is static, so a model OpenCode moves to another format is retried
            // there instead of failing until the next app update.
            val formats = listOf(primaryFormat) + OpenCodeApiFormat.entries.filter { it != primaryFormat }
            var result: Result<String> = Result.failure(IllegalStateException("Unknown error"))
            for (format in formats) {
                result =
                    fetchWithRetry(
                        tier = tier,
                        format = format,
                        sessionId = session,
                        requestId = requestId,
                        payload =
                            buildRequestBody(
                                format = format,
                                modelId = modelId,
                                query = query,
                                personalContext = personalContext,
                                useSystemInstruction = useSystemInstruction,
                                systemInstruction = systemInstruction,
                                history = history,
                                advancedPayloadJson = advancedPayloadJson,
                            ),
                    )
                val error = result.exceptionOrNull()
                if (error !is ResponseException || !error.message.contains(FORMAT_MISMATCH_MARKER)) break
            }
            result
        }

    private suspend fun fetchWithRetry(
        tier: OpenCodeTier,
        format: OpenCodeApiFormat,
        sessionId: String,
        requestId: String,
        payload: String,
    ): Result<String> {
        var attempt = 1
        var delayMs = INITIAL_RETRY_DELAY_MS
        var lastError: Throwable? = null

        while (attempt <= MAX_ATTEMPTS) {
            val result = executeRequest(tier, format, sessionId, requestId, payload)
            if (result.isSuccess) return result

            lastError = result.exceptionOrNull()
            if (attempt == MAX_ATTEMPTS || !shouldRetry(lastError)) {
                return Result.failure(lastError ?: IllegalStateException("Unknown error"))
            }
            delay(delayMs)
            delayMs *= 2
            attempt++
        }
        return Result.failure(lastError ?: IllegalStateException("Unknown error"))
    }

    private fun executeRequest(
        tier: OpenCodeTier,
        format: OpenCodeApiFormat,
        sessionId: String,
        requestId: String,
        payload: String,
    ): Result<String> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(tier.baseUrl + format.path)
            connection =
                (url.openConnection() as HttpURLConnection).trackCancellation().apply {
                    requestMethod = "POST"
                    buildHeaders(apiKey, format, sessionId, requestId).forEach { (name, value) ->
                        setRequestProperty(name, value)
                    }
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 30000
                }

            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "OpenCode request: url=$url, session=$sessionId")
                Log.d(LOG_TAG, "OpenCode request payload: ${redactApiKeyForLogging(payload, apiKey)}")
            }

            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val raw = readResponseBody(connection, responseCode)

            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "OpenCode response: code=$responseCode, length=${raw.length}")
            }

            if (responseCode in 200..299) {
                val answer = extractAnswer(format, raw)
                    ?: return Result.failure(IllegalStateException("Empty response from OpenCode"))
                Result.success(answer)
            } else {
                val message = parseError(raw) ?: "Request failed ($responseCode)"
                Result.failure(ResponseException(responseCode, message))
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "OpenCode request failed", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun shouldRetry(error: Throwable?): Boolean =
        when (error) {
            is ResponseException -> error.code == 429 || error.code >= 500
            is IOException -> true
            else -> false
        }

    private data class ResponseException(val code: Int, override val message: String) : Exception(message)
}
