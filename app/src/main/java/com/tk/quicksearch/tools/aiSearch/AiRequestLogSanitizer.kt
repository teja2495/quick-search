package com.tk.quicksearch.tools.aiSearch

private const val REDACTED = "[REDACTED_API_KEY]"

/**
 * Removes the user's API key from strings before debug logging (e.g. if it appears in the body
 * or is embedded in an error message). Uses literal replacement only.
 */
internal fun redactApiKeyForLogging(text: String, apiKey: String): String {
    if (apiKey.isBlank()) return text
    var result = text.replace(apiKey, REDACTED)
    val trimmed = apiKey.trim()
    if (trimmed.isNotBlank() && trimmed != apiKey) {
        result = result.replace(trimmed, REDACTED)
    }
    return result
}
