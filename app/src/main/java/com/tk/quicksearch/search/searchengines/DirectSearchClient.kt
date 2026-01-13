package com.tk.quicksearch.search.searchEngines

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight client for fetching direct answers from the Gemini API.
 * Uses the public Gemini endpoint so it can be proxied via Firebase AI Logic.
 */
class DirectSearchClient(private val apiKey: String) {

    companion object {
        private const val LOG_TAG = "DirectSearchClient"
        private const val MODEL = "gemini-flash-latest"
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
        private const val SYSTEM_PROMPT =
            "Return only the direct answer as a single short sentence. " +
            "Provide additional context ONLY when its needed. " +
            "Use plain text with no markdown, bullets, emphasis, or special characters like *, _, `, or ~. " +
            "Whenever a phone number is included, format it in E.164 with country code so it can be dialed directly."
        private const val MAX_ATTEMPTS = 2
        private const val INITIAL_RETRY_DELAY_MS = 750L
    }

    suspend fun fetchAnswer(query: String, personalContext: String? = null): Result<String> = withContext(Dispatchers.IO) {
        var attempt = 1
        var delayMs = INITIAL_RETRY_DELAY_MS
        var lastError: Throwable? = null

        while (attempt <= MAX_ATTEMPTS) {
            val result = executeRequest(query, personalContext)
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

    private fun executeRequest(query: String, personalContext: String?): Result<String> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("$BASE_URL?key=$apiKey")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 20000
            }

            val payload = buildRequestBody(query, personalContext)
            val payloadForLogging = if (personalContext.isNullOrBlank()) {
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
                val answer = extractAnswer(rawResponse) ?: return Result.failure(
                    IllegalStateException("Empty response from Gemini")
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

    private fun buildRequestBody(query: String, personalContext: String?): String {
        val systemInstruction = JSONObject().apply {
            val systemParts = JSONArray()
            systemParts.put(JSONObject().put("text", SYSTEM_PROMPT))
            if (!personalContext.isNullOrBlank()) {
                systemParts.put(
                    JSONObject().put(
                        "text",
                        "\n\nUser personal context:\n${personalContext.trim()}"
                    )
                )
            }
            put("parts", systemParts)
        }
        val contentParts = JSONArray().apply {
            put(JSONObject().put("text", query))
        }
        val content = JSONObject().apply {
            put("parts", contentParts)
        }
        val tools = JSONArray().put(JSONObject().put("google_search", JSONObject()))
        val generationConfig = JSONObject().apply {
            put("responseMimeType", "text/plain")
            put("temperature", 0.2)
        }

        return JSONObject().apply {
            put("systemInstruction", systemInstruction)
            put("contents", JSONArray().put(content))
            put("tools", tools)
            put("generationConfig", generationConfig)
        }.toString()
    }

    private fun readResponseBody(connection: HttpURLConnection, responseCode: Int): String {
        val stream = if (responseCode in 200..299) {
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
            sanitizedAnswer.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun parseError(rawResponse: String): String? {
        if (rawResponse.isBlank()) return null
        return runCatching {
            val root = JSONObject(rawResponse)
            val error = root.optJSONObject("error") ?: return null
            error.optString("message").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun shouldRetry(error: Throwable?): Boolean {
        return when (error) {
            is ResponseException -> error.code == 429 || error.code >= 500
            is IOException -> true
            else -> false
        }
    }

    private data class ResponseException(val code: Int, override val message: String) :
        Exception(message)
}

