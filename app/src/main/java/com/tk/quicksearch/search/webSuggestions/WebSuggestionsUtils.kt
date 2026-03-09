package com.tk.quicksearch.search.webSuggestions

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Utility functions for fetching web search suggestions from Google's Suggest API. */
object WebSuggestionsUtils {
    // Use shorter timeouts for suggestions - fail fast on slow networks
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

    /**
     * Fetches web search suggestions for the given query. Returns a list of up to 5 suggestions, or
     * empty list on error.
     */
    suspend fun getSuggestions(query: String): List<String> {
        return try {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isBlank()) {
                return emptyList()
            }

            val encodedQuery = URLEncoder.encode(trimmedQuery, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&q=$encodedQuery"
            val request = Request.Builder().url(url).build()

            val responseBody = executeCancellable(request) ?: return emptyList()
            if (responseBody.isBlank()) {
                return emptyList()
            }

            val jsonArray = JSONArray(responseBody)
            if (jsonArray.length() < 2) {
                return emptyList()
            }

            val suggestionsArray = jsonArray.getJSONArray(1)
            val suggestions = mutableListOf<String>()

            // Get up to 5 suggestions
            val maxSuggestions = minOf(5, suggestionsArray.length())

            for (i in 0 until maxSuggestions) {
                val suggestion = suggestionsArray.getString(i)

                if (suggestion.isNotBlank()) {
                    val capitalizedSuggestion = suggestion.replaceFirstChar { it.uppercase() }

                    // Skip the first suggestion if it matches the query (case-insensitive)
                    val matchesQuery = i == 0 && capitalizedSuggestion.equals(trimmedQuery, ignoreCase = true)
                    if (matchesQuery) {
                        continue
                    }

                    suggestions.add(capitalizedSuggestion)
                }
            }

            suggestions
        } catch (e: Exception) {
            // Return empty list on any error
            emptyList()
        }
    }

    private suspend fun executeCancellable(request: Request): String? =
        suspendCancellableCoroutine { continuation ->
            val call: Call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }

            call.enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        response.use {
                            val body = if (it.isSuccessful) it.body?.string() else null
                            if (continuation.isActive) {
                                continuation.resume(body)
                            }
                        }
                    }
                },
            )
        }
}
