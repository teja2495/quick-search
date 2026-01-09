package com.tk.quicksearch.search.searchengines

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchEngine

/**
 * Utility functions for SearchEngine-related mappings and conversions.
 * Centralizes duplicate code across the codebase.
 */

/**
 * Data class holding metadata for a search engine.
 * This centralizes all engine-specific information in one place.
 */
private data class SearchEngineMetadata(
    @DrawableRes val drawableResId: Int,
    val contentDescription: String,
    val urlTemplate: String,
    val defaultShortcutCode: String
)

/**
 * Centralized mapping of SearchEngine enum to its metadata.
 * Adding a new engine only requires updating this map.
 */
private val SEARCH_ENGINE_METADATA: Map<SearchEngine, SearchEngineMetadata> = mapOf(
    SearchEngine.DIRECT_SEARCH to SearchEngineMetadata(
        drawableResId = R.drawable.direct_search,
        contentDescription = "Direct Search",
        urlTemplate = "",
        defaultShortcutCode = "dsh"
    ),
    SearchEngine.GOOGLE to SearchEngineMetadata(
        drawableResId = R.drawable.google,
        contentDescription = "Google",
        urlTemplate = "https://www.google.com/search?q=%s",
        defaultShortcutCode = "ggl"
    ),
    SearchEngine.CHATGPT to SearchEngineMetadata(
        drawableResId = R.drawable.chatgpt,
        contentDescription = "ChatGPT",
        urlTemplate = "https://chatgpt.com/?prompt=%s",
        defaultShortcutCode = "cgpt"
    ),
    SearchEngine.GEMINI to SearchEngineMetadata(
        drawableResId = R.drawable.ic_gemini_sparkle_search_engine,
        contentDescription = "Gemini",
        urlTemplate = "https://gemini.google.com/app?text=%s",
        defaultShortcutCode = "gmi"
    ),
    SearchEngine.PERPLEXITY to SearchEngineMetadata(
        drawableResId = R.drawable.perplexity,
        contentDescription = "Perplexity",
        urlTemplate = "https://www.perplexity.ai/search?q=%s",
        defaultShortcutCode = "ppx"
    ),
    SearchEngine.GROK to SearchEngineMetadata(
        drawableResId = R.drawable.grok,
        contentDescription = "Grok",
        urlTemplate = "https://grok.com/?q=%s",
        defaultShortcutCode = "grk"
    ),
    SearchEngine.GOOGLE_MAPS to SearchEngineMetadata(
        drawableResId = R.drawable.google_maps,
        contentDescription = "Google Maps",
        urlTemplate = "https://maps.google.com/?q=%s",
        defaultShortcutCode = "mps"
    ),
    SearchEngine.GOOGLE_DRIVE to SearchEngineMetadata(
        drawableResId = R.drawable.google_drive,
        contentDescription = "Google Drive",
        urlTemplate = "https://drive.google.com/drive/u/0/search?q=%s",
        defaultShortcutCode = "gdr"
    ),
    SearchEngine.GOOGLE_PHOTOS to SearchEngineMetadata(
        drawableResId = R.drawable.google_photos,
        contentDescription = "Google Photos",
        urlTemplate = "https://photos.google.com/search/%s",
        defaultShortcutCode = "gph"
    ),
    SearchEngine.GOOGLE_PLAY to SearchEngineMetadata(
        drawableResId = R.drawable.google_play,
        contentDescription = "Google Play",
        urlTemplate = "https://play.google.com/store/search?q=%s&c=apps",
        defaultShortcutCode = "gpl"
    ),
    SearchEngine.REDDIT to SearchEngineMetadata(
        drawableResId = R.drawable.reddit,
        contentDescription = "Reddit",
        urlTemplate = "https://www.reddit.com/search/?q=%s",
        defaultShortcutCode = "rdt"
    ),
    SearchEngine.YOUTUBE to SearchEngineMetadata(
        drawableResId = R.drawable.youtube,
        contentDescription = "YouTube",
        urlTemplate = "https://www.youtube.com/results?search_query=%s",
        defaultShortcutCode = "ytb"
    ),
    SearchEngine.YOUTUBE_MUSIC to SearchEngineMetadata(
        drawableResId = R.drawable.youtube_music,
        contentDescription = "YouTube Music",
        urlTemplate = "https://music.youtube.com/search?q=%s",
        defaultShortcutCode = "ytm"
    ),
    SearchEngine.SPOTIFY to SearchEngineMetadata(
        drawableResId = R.drawable.spotify,
        contentDescription = "Spotify",
        urlTemplate = "https://open.spotify.com/search/%s",
        defaultShortcutCode = "sfy"
    ),
    SearchEngine.FACEBOOK_MARKETPLACE to SearchEngineMetadata(
        drawableResId = R.drawable.facebook_marketplace,
        contentDescription = "Facebook Marketplace",
        urlTemplate = "https://www.facebook.com/marketplace/search/?query=%s",
        defaultShortcutCode = "fbm"
    ),
    SearchEngine.AMAZON to SearchEngineMetadata(
        drawableResId = R.drawable.amazon,
        contentDescription = "Amazon",
        urlTemplate = "https://www.amazon.com/s?k=%s",
        defaultShortcutCode = "amz"
    ),
    SearchEngine.YOU_COM to SearchEngineMetadata(
        drawableResId = R.drawable.you_com,
        contentDescription = "You.com",
        urlTemplate = "https://you.com/search?q=%s",
        defaultShortcutCode = "yu"
    ),
    SearchEngine.DUCKDUCKGO to SearchEngineMetadata(
        drawableResId = R.drawable.duckduckgo,
        contentDescription = "DuckDuckGo",
        urlTemplate = "https://duckduckgo.com/?q=%s",
        defaultShortcutCode = "ddg"
    ),
    SearchEngine.BRAVE to SearchEngineMetadata(
        drawableResId = R.drawable.brave,
        contentDescription = "Brave",
        urlTemplate = "https://search.brave.com/search?q=%s",
        defaultShortcutCode = "brv"
    ),
    SearchEngine.BING to SearchEngineMetadata(
        drawableResId = R.drawable.bing,
        contentDescription = "Bing",
        urlTemplate = "https://www.bing.com/search?q=%s",
        defaultShortcutCode = "bng"
    ),
    SearchEngine.X to SearchEngineMetadata(
        drawableResId = R.drawable.x,
        contentDescription = "X",
        urlTemplate = "https://x.com/search?q=%s",
        defaultShortcutCode = "twt"
    ),
    SearchEngine.AI_MODE to SearchEngineMetadata(
        drawableResId = R.drawable.ai_mode,
        contentDescription = "AI mode",
        urlTemplate = "https://www.google.com/search?q=%s&udm=50",
        defaultShortcutCode = "gai"
    )
)

/**
 * Maps SearchEngine enum to its corresponding drawable resource ID.
 */
@DrawableRes
fun SearchEngine.getDrawableResId(): Int =
    SEARCH_ENGINE_METADATA[this]?.drawableResId
        ?: throw IllegalArgumentException("Unknown SearchEngine: $this")

/**
 * Maps SearchEngine enum to its content description string.
 */
fun SearchEngine.getContentDescription(): String =
    SEARCH_ENGINE_METADATA[this]?.contentDescription
        ?: throw IllegalArgumentException("Unknown SearchEngine: $this")

/**
 * Builds a search URL for the given query and search engine.
 * 
 * @param query The search query to encode and insert into the URL
 * @param searchEngine The search engine to build the URL for
 * @param amazonDomain Optional custom Amazon domain (e.g., "amazon.co.uk" instead of "amazon.com")
 * @return The complete search URL with the encoded query, or base URL without query params if query is empty
 */
fun buildSearchUrl(query: String, searchEngine: SearchEngine, amazonDomain: String? = null): String {
    if (searchEngine == SearchEngine.DIRECT_SEARCH) {
        throw IllegalArgumentException("Direct Answer does not use a browser URL")
    }
    val metadata = SEARCH_ENGINE_METADATA[searchEngine]
        ?: throw IllegalArgumentException("Unknown SearchEngine: $searchEngine")
    
    // Build Amazon URL template with custom domain if provided
    val amazonUrlTemplate = if (searchEngine == SearchEngine.AMAZON) {
        val domain = amazonDomain ?: "amazon.com"
        "https://www.$domain/s?k=%s"
    } else {
        metadata.urlTemplate
    }
    
    // If query is blank, return home URL for specific engines that need it
    if (query.isBlank()) {
        // Special handling for engines that should go to home page instead of search page
        val homeUrl = when (searchEngine) {
            SearchEngine.AMAZON -> {
                val domain = amazonDomain ?: "amazon.com"
                "https://www.$domain"
            }
            SearchEngine.YOUTUBE -> "https://www.youtube.com"
            SearchEngine.REDDIT -> "https://www.reddit.com"
            SearchEngine.GEMINI -> "https://gemini.google.com/app"
            SearchEngine.GOOGLE_PLAY -> "https://play.google.com/store/apps"
            SearchEngine.GOOGLE_PHOTOS -> "https://photos.google.com/"
            SearchEngine.FACEBOOK_MARKETPLACE -> "https://www.facebook.com/marketplace/"
            SearchEngine.YOU_COM -> "https://you.com"
            else -> null
        }
        if (homeUrl != null) {
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
        val params = queryString.split("&")
            .filter { !it.contains("%s") }
        
        // Reconstruct URL
        return if (params.isEmpty()) {
            baseUrl
        } else {
            "$baseUrl?${params.joinToString("&")}"
        }
    }
    
    val encodedQuery = Uri.encode(query)
    val templateToUse = if (searchEngine == SearchEngine.AMAZON) {
        amazonUrlTemplate
    } else {
        metadata.urlTemplate
    }
    return templateToUse.replace("%s", encodedQuery)
}

/**
 * Gets the default shortcut code for a search engine.
 * 
 * @return The default shortcut code string, or null if not found
 */
fun SearchEngine.getDefaultShortcutCode(): String =
    SEARCH_ENGINE_METADATA[this]?.defaultShortcutCode
        ?: throw IllegalArgumentException("Unknown SearchEngine: $this")

/**
 * Normalizes shortcut input by lowercasing and stripping invalid characters.
 */
fun normalizeShortcutCodeInput(input: String): String =
    input.lowercase().filter { char -> char.isLetterOrDigit() && char != ' ' }

/**
 * Validates shortcut input. Must be at least 2 characters after normalization.
 */
fun isValidShortcutCode(input: String): Boolean =
    normalizeShortcutCodeInput(input).length >= 2

/**
 * Maps SearchEngine enum to its string resource ID for display name.
 */
@StringRes
fun SearchEngine.getDisplayNameResId(): Int = when (this) {
    SearchEngine.DIRECT_SEARCH -> R.string.search_engine_direct_search
    SearchEngine.GOOGLE -> R.string.search_engine_google
    SearchEngine.CHATGPT -> R.string.search_engine_chatgpt
    SearchEngine.PERPLEXITY -> R.string.search_engine_perplexity
    SearchEngine.GROK -> R.string.search_engine_grok
    SearchEngine.GEMINI -> R.string.search_engine_gemini
    SearchEngine.GOOGLE_MAPS -> R.string.search_engine_google_maps
    SearchEngine.GOOGLE_DRIVE -> R.string.search_engine_google_drive
    SearchEngine.GOOGLE_PHOTOS -> R.string.search_engine_google_photos
    SearchEngine.GOOGLE_PLAY -> R.string.search_engine_google_play
    SearchEngine.REDDIT -> R.string.search_engine_reddit
    SearchEngine.YOUTUBE -> R.string.search_engine_youtube
    SearchEngine.YOUTUBE_MUSIC -> R.string.search_engine_youtube_music
    SearchEngine.SPOTIFY -> R.string.search_engine_spotify
    SearchEngine.FACEBOOK_MARKETPLACE -> R.string.search_engine_facebook_marketplace
    SearchEngine.AMAZON -> R.string.search_engine_amazon
    SearchEngine.YOU_COM -> R.string.search_engine_you_com
    SearchEngine.DUCKDUCKGO -> R.string.search_engine_duckduckgo
    SearchEngine.BRAVE -> R.string.search_engine_brave
    SearchEngine.BING -> R.string.search_engine_bing
    SearchEngine.X -> R.string.search_engine_x
    SearchEngine.AI_MODE -> R.string.search_engine_ai_mode
}

/**
 * Gets the display name for a search engine.
 * This is a Composable function that requires string resources.
 */
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
