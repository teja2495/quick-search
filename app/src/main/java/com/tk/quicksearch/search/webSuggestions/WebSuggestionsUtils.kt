package com.tk.quicksearch.search.webSuggestions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

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
    suspend fun getSuggestions(query: String): List<String> =
        withContext(Dispatchers.IO) {
            val trimmedQuery = query.trim()
            try {
                if (trimmedQuery.isBlank()) {
                    return@withContext emptyList()
                }

                val encodedQuery = URLEncoder.encode(trimmedQuery, "UTF-8")
                val url = "https://suggestqueries.google.com/complete/search?client=firefox&q=$encodedQuery"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank()) {
                    return@withContext emptyList()
                }

                val jsonArray = JSONArray(responseBody)

                if (jsonArray.length() < 2) {
                    return@withContext emptyList()
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
}
