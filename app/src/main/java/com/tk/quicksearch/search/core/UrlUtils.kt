package com.tk.quicksearch.search.core

import android.net.Uri

private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
private val IPV4_REGEX = Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d{1,5})?(?:[/?#].*)?$")
private val DOMAIN_REGEX =
    Regex(
        "^(?=.{1,253}$)(?!-)(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}(?::\\d{1,5})?(?:[/?#].*)?$",
    )

fun normalizeToBrowsableUrl(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank() || trimmed.any { it.isWhitespace() }) return null

    val candidate =
        when {
            SCHEME_REGEX.containsMatchIn(trimmed) -> trimmed
            trimmed.startsWith("www.", ignoreCase = true) -> "https://$trimmed"
            DOMAIN_REGEX.matches(trimmed) -> "https://$trimmed"
            IPV4_REGEX.matches(trimmed) -> "https://$trimmed"
            else -> return null
        }

    val uri = Uri.parse(candidate)
    return if (uri.host.isNullOrBlank()) null else candidate
}

fun isLikelyWebUrl(input: String): Boolean = normalizeToBrowsableUrl(input) != null
