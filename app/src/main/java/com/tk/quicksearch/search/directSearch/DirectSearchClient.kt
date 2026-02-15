package com.tk.quicksearch.search.directSearch

import android.util.Log
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
class DirectSearchClient(
    private val apiKey: String,
) {
    companion object {
        private const val LOG_TAG = "DirectSearchClient"
        private const val BASE_API_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val SYSTEM_PROMPT =
            "Return only the direct answer as a single short sentence. " +
                "Provide additional context ONLY when its needed. " +
                "Use plain text with no markdown, bullets, emphasis, or special characters like *, _, `, or ~. " +
                "Whenever a phone number is included, format it in E.164 with country code so it can be dialed directly."
        private const val MAX_ATTEMPTS = 2
        private const val INITIAL_RETRY_DELAY_MS = 750L
        private const val MODELS_ENDPOINT = "$BASE_API_URL/models"

        suspend fun fetchAvailableTextModels(apiKey: String): Result<List<GeminiTextModel>> =
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
                                val message = parseError(rawResponse) ?: "Unable to load Gemini models"
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
                                        displayName = "Gemini Flash (Latest)",
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
        useSystemInstruction: Boolean = true,
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
                        useGroundingWithGoogleSearch = useGroundingWithGoogleSearch,
                        useSystemInstruction = useSystemInstruction,
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
        useGroundingWithGoogleSearch: Boolean,
        useSystemInstruction: Boolean,
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
                    useSystemInstruction = useSystemInstruction,
                )
            val payloadForLogging =
                if (personalContext.isNullOrBlank()) {
                    payload
                } else {
                    "[payload with personal context hidden]"
                }
            Log.i(LOG_TAG, "Gemini request JSON: $payloadForLogging")
            connection.outputStream.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val responseCode = connection.responseCode
            val rawResponse = readResponseBody(connection, responseCode)
            Log.i(LOG_TAG, "Gemini response JSON: $rawResponse")

            if (responseCode in 200..299) {
                val answer =
                    extractAnswer(rawResponse) ?: return Result.failure(
                        IllegalStateException("Empty response from Gemini"),
                    )
                Result.success(answer)
            } else {
                val message = parseError(rawResponse) ?: "Request failed ($responseCode)"
                Result.failure(ResponseException(responseCode, message))
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Gemini request failed", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildRequestBody(
        query: String,
        personalContext: String?,
        useGroundingWithGoogleSearch: Boolean,
        useSystemInstruction: Boolean,
    ): String {
        val promptPrefix =
            when {
                useSystemInstruction -> null
                else -> buildString {
                    append(SYSTEM_PROMPT)
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
                put("responseMimeType", "text/plain")
                put("temperature", 0.2)
            }

        val root = JSONObject()
        if (useSystemInstruction) {
            val systemInstruction =
                JSONObject().apply {
                    val systemParts = JSONArray()
                    systemParts.put(JSONObject().put("text", SYSTEM_PROMPT))
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
            root.put("systemInstruction", systemInstruction)
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
            val firstPart = parts.getJSONObject(0)
            val rawAnswer = firstPart.optString("text")
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

    private data class ResponseException(
        val code: Int,
        override val message: String,
    ) : Exception(message)
}
