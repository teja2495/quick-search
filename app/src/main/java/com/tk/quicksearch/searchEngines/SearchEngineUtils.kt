package com.tk.quicksearch.searchEngines

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.search.core.SearchEngine

@DrawableRes
fun SearchEngine.getDrawableResId(): Int =
    SearchEngineRegistry.get(this).drawableResId

fun SearchEngine.getAppPackageCandidates(): List<String> =
    SearchEngineRegistry.get(this).appPackages

fun SearchEngine.isInstallOnlyEngine(): Boolean =
    SearchEngineRegistry.get(this).installOnly

fun SearchEngine.isDefaultDisabledOnFirstRun(): Boolean =
    SearchEngineRegistry.get(this).defaultDisabledOnFirstRun

fun SearchEngine.shouldDefaultDisableIfAppMissing(): Boolean =
    SearchEngineRegistry.get(this).defaultDisableIfAppMissing

fun SearchEngine.getIconColorPolicy(): SearchEngineIconColorPolicy =
    SearchEngineRegistry.get(this).iconColorPolicy

fun SearchEngine.getNativeLaunchMode(): SearchEngineNativeLaunchMode =
    SearchEngineRegistry.get(this).nativeLaunchMode

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
        throw IllegalArgumentException("Direct Answer does not use a browser URL")
    }
    val metadata = SearchEngineRegistry.get(searchEngine)

    // Build Amazon URL template with custom domain if provided
    val amazonUrlTemplate =
        if (searchEngine == SearchEngine.AMAZON) {
            val domain = amazonDomain ?: "amazon.com"
            "https://www.$domain/s?k=%s"
        } else {
            metadata.urlTemplate
        }

    // If query is blank, return home URL for specific engines that need it
    if (query.isBlank()) {
        if (searchEngine == SearchEngine.AMAZON) {
            val domain = amazonDomain ?: "amazon.com"
            return "https://www.$domain"
        }

        metadata.homeUrl?.let { homeUrl ->
            return homeUrl
        }

        // For other engines, return base URL without query parameters
        val template = metadata.urlTemplate
        // Split URL into base and query parts
        val parts = template.split("?", limit = 2)
        if (parts.size == 1) {
            // No query parameters, return as-is
            return template.replace("%s", "")
        }

        val baseUrl = parts[0]
        val queryString = parts[1]

        // Split query parameters and filter out the one containing %s
        val params =
            queryString
                .split("&")
                .filter { !it.contains("%s") }

        // Reconstruct URL
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
            metadata.urlTemplate
        }
    return templateToUse.replace("%s", encodedQuery)
}

fun SearchEngine.getDefaultShortcutCode(): String =
    SearchEngineRegistry.get(this).defaultShortcutCode

@StringRes
fun SearchEngine.getContentDescriptionResId(): Int =
    SearchEngineRegistry.get(this).contentDescriptionResId

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

    // Must start with "amazon."
    if (!trimmed.startsWith("amazon.", ignoreCase = true)) {
        return false
    }

    // Extract the part after "amazon."
    val afterAmazon = trimmed.substringAfter("amazon.", missingDelimiterValue = "")
    if (afterAmazon.isEmpty()) {
        return false
    }

    // Check for valid domain format: should have at least one dot followed by TLD (min 2 chars)
    // Examples: co.uk, de, fr, com, co.jp
    val parts = afterAmazon.split(".")
    if (parts.isEmpty() || parts.any { it.isEmpty() }) {
        return false
    }

    // Last part (TLD) should be at least 2 characters
    val tld = parts.last()
    if (tld.length < 2) {
        return false
    }

    // Check that all parts contain only valid domain characters (letters, digits, hyphens)
    val domainPattern = Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$")
    if (!parts.all { it.matches(domainPattern) }) {
        return false
    }

    return true
}
