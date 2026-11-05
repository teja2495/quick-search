package com.tk.quicksearch.search.data.bookmarks

import java.net.URI
import java.util.Locale

/** URL helpers shared by bookmark editing, import, and search. Pure Kotlin so they can be unit tested. */
object BookmarkUrls {
    private val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
    private val hostLikeRegex = Regex("^[^\\s/?#]+\\.[^\\s/?#]{2,}([/?#].*)?$")

    /**
     * Returns a browsable http(s) URL for what the user typed, adding https:// when the scheme is
     * missing, or null when it is not a web address.
     */
    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
        val withScheme =
            when {
                trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) -> trimmed
                // Another scheme (mailto:, javascript:, ftp://, ...) is not a website. "host:8080" is.
                hasNonPortScheme(trimmed) -> return null
                hostLikeRegex.matches(trimmed) -> "https://$trimmed"
                else -> return null
            }
        val host = hostOf(withScheme) ?: return null
        return if (host.isBlank()) null else withScheme
    }

    private fun hasNonPortScheme(text: String): Boolean {
        val match = schemeRegex.find(text) ?: return false
        return text.getOrNull(match.range.last + 1)?.isDigit() != true
    }

    fun isWebUrl(url: String): Boolean =
        (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) &&
            hostOf(url) != null

    fun hostOf(url: String): String? =
        runCatching { URI(url.trim()).host }
            .getOrNull()
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }

    /** Host without a leading "www.", for display and matching. */
    fun displayHost(url: String): String =
        hostOf(url)?.removePrefix("www.") ?: url.trim()

    /** Key that treats the same page with and without a trailing slash, "www." or http(s) as one. */
    fun dedupeKey(url: String): String {
        val trimmed = url.trim()
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return trimmed.lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: return trimmed.lowercase(Locale.ROOT)
        val path = uri.rawPath.orEmpty().trimEnd('/')
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        val port = if (uri.port > 0) ":${uri.port}" else ""
        return "$host$port$path$query"
    }

    /** "https://host" for [url], or null when it has no host. */
    fun originOf(url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        val host = uri.host ?: return null
        val port = if (uri.port > 0) ":${uri.port}" else ""
        return "$scheme://$host$port"
    }
}
