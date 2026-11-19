package com.tk.quicksearch.searchEngines

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.searchEngines.SearchEngine

enum class SearchEngineIconColorPolicy {
    NONE,
    INVERT_ON_LIGHT,
    DARKEN_ON_LIGHT,
}

@DrawableRes
fun SearchEngine.getDrawableResId(): Int? =
    drawableResId

fun SearchEngine.getAppPackageCandidates(): List<String> =
    appPackages

fun SearchEngine.isInstallOnlyEngine(): Boolean =
    installOnly

fun SearchEngine.isDefaultDisabledOnFirstRun(): Boolean =
    defaultDisabledOnFirstRun

fun SearchEngine.shouldDefaultDisableIfAppMissing(): Boolean =
    defaultDisableIfAppMissing

fun SearchEngine.getIconColorPolicy(): SearchEngineIconColorPolicy =
    iconColorPolicy

/**
 * Builds a search URL for the given query and search engine.
 *
 * @param query The search query to encode and insert into the URL
 * @param searchEngine The search engine to build the URL for
 * @param amazonDomain Optional custom Amazon domain (e.g., "amazon.co.uk" instead of "amazon.com")
 * @return The complete search URL with the encoded query, or base URL without query params if query is empty
 */
fun buildSearchUrl(
    query: String,
    searchEngine: SearchEngine,
    amazonDomain: String? = null,
): String {
    if (searchEngine == SearchEngine.DIRECT_SEARCH) {
        throw IllegalArgumentException("AI Search does not use a browser URL")
    }

    val amazonUrlTemplate =
        if (searchEngine == SearchEngine.AMAZON) {
            val domain = amazonDomain ?: "amazon.com"
            "https://www.$domain/s?k=%s"
        } else {
            searchEngine.urlTemplate
        }

    if (query.isBlank()) {
        if (searchEngine == SearchEngine.AMAZON) {
            val domain = amazonDomain ?: "amazon.com"
            return "https://www.$domain"
        }

        searchEngine.homeUrl?.let { homeUrl ->
            return homeUrl
        }

        val template = searchEngine.urlTemplate
        val parts = template.split("?", limit = 2)
        if (parts.size == 1) {
            return template.replace("%s", "")
        }

        val baseUrl = parts[0]
        val queryString = parts[1]

        val params =
            queryString
                .split("&")
                .filter { !it.contains("%s") }

        return if (params.isEmpty()) {
            baseUrl
        } else {
            "$baseUrl?${params.joinToString("&")}"
        }
    }

    val encodedQuery = Uri.encode(query)
    val templateToUse =
        if (searchEngine == SearchEngine.AMAZON) {
            amazonUrlTemplate
        } else {
            searchEngine.urlTemplate
        }
    return templateToUse.replace("%s", encodedQuery)
}

fun SearchEngine.getDefaultShortcutCode(): String =
    defaultShortcutCode

@StringRes
fun SearchEngine.getContentDescriptionResId(): Int =
    contentDescriptionResId

@StringRes
fun SearchEngine.getDisplayNameResId(): Int =
    getContentDescriptionResId()

@Composable
fun SearchEngine.getContentDescription(): String = stringResource(getContentDescriptionResId())

@Composable
fun SearchEngine.getDisplayName(): String = stringResource(getDisplayNameResId())


/**
 * Validates an Amazon domain format.
 *
 * @param domain The domain to validate (should already be normalized - no protocol, www, trailing slashes)
 * @return true if the domain is valid, false otherwise
 */
fun isValidAmazonDomain(domain: String): Boolean {
    if (domain.isBlank()) {
        return false
    }

    val trimmed = domain.trim()

    if (!trimmed.startsWith("amazon.", ignoreCase = true)) {
        return false
    }

    val afterAmazon = trimmed.substringAfter("amazon.", missingDelimiterValue = "")
    if (afterAmazon.isEmpty()) {
        return false
    }

    // Accepts suffixes such as de, com, co.uk and co.jp.
    val parts = afterAmazon.split(".")
    if (parts.isEmpty() || parts.any { it.isEmpty() }) {
        return false
    }

    val tld = parts.last()
    if (tld.length < 2) {
        return false
    }

    val domainPattern = Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$")
    if (!parts.all { it.matches(domainPattern) }) {
        return false
    }

    return true
}
