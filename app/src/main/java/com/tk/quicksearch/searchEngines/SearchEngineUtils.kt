package com.tk.quicksearch.searchEngines

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.shared.util.PackageConstants

private data class SearchEngineMetadata(
    @DrawableRes val drawableResId: Int,
    @StringRes val contentDescriptionResId: Int,
    val urlTemplate: String,
    val defaultShortcutCode: String,
    val homeUrl: String? = null,
    val appPackages: List<String> = emptyList(),
    val installOnly: Boolean = false,
    val defaultDisabledOnFirstRun: Boolean = false,
    val defaultDisableIfAppMissing: Boolean = false,
)

private val SEARCH_ENGINE_METADATA: Map<SearchEngine, SearchEngineMetadata> =
    mapOf(
        SearchEngine.DIRECT_SEARCH to
            SearchEngineMetadata(
                drawableResId = R.drawable.direct_search,
                contentDescriptionResId = R.string.search_engine_direct_search,
                urlTemplate = "",
                defaultShortcutCode = "dsh",
            ),
        SearchEngine.GOOGLE to
            SearchEngineMetadata(
                drawableResId = R.drawable.google,
                contentDescriptionResId = R.string.search_engine_google,
                urlTemplate = "https://www.google.com/search?q=%s",
                defaultShortcutCode = "ggl",
                appPackages = listOf(PackageConstants.GOOGLE_APP_PACKAGE),
            ),
        SearchEngine.CHATGPT to
            SearchEngineMetadata(
                drawableResId = R.drawable.chatgpt,
                contentDescriptionResId = R.string.search_engine_chatgpt,
                urlTemplate = "https://chatgpt.com/?prompt=%s",
                defaultShortcutCode = "cgpt",
                appPackages = listOf(PackageConstants.CHATGPT_PACKAGE),
            ),
        SearchEngine.GEMINI to
            SearchEngineMetadata(
                drawableResId = R.drawable.ic_gemini_sparkle_search_engine,
                contentDescriptionResId = R.string.search_engine_gemini,
                urlTemplate = "https://gemini.google.com/app?text=%s",
                defaultShortcutCode = "gmi",
                homeUrl = "https://gemini.google.com/app",
                appPackages = listOf(PackageConstants.GEMINI_PACKAGE_NAME),
            ),
        SearchEngine.PERPLEXITY to
            SearchEngineMetadata(
                drawableResId = R.drawable.perplexity,
                contentDescriptionResId = R.string.search_engine_perplexity,
                urlTemplate = "https://www.perplexity.ai/search?q=%s",
                defaultShortcutCode = "ppx",
                appPackages = listOf(PackageConstants.PERPLEXITY_PACKAGE),
            ),
        SearchEngine.GROK to
            SearchEngineMetadata(
                drawableResId = R.drawable.grok,
                contentDescriptionResId = R.string.search_engine_grok,
                urlTemplate = "https://grok.com/?q=%s",
                defaultShortcutCode = "grk",
                appPackages = listOf(PackageConstants.GROK_PACKAGE),
            ),
        SearchEngine.GOOGLE_MAPS to
            SearchEngineMetadata(
                drawableResId = R.drawable.google_maps,
                contentDescriptionResId = R.string.search_engine_google_maps,
                urlTemplate = "https://maps.google.com/?q=%s",
                defaultShortcutCode = "mps",
                appPackages = listOf(PackageConstants.GOOGLE_MAPS_PACKAGE),
            ),
        SearchEngine.WAZE to
            SearchEngineMetadata(
                drawableResId = R.drawable.waze,
                contentDescriptionResId = R.string.search_engine_waze,
                urlTemplate = "https://www.waze.com/ul?q=%s",
                defaultShortcutCode = "wze",
                homeUrl = "https://www.waze.com",
                appPackages = listOf(PackageConstants.WAZE_PACKAGE),
                installOnly = true,
            ),
        SearchEngine.GOOGLE_DRIVE to
            SearchEngineMetadata(
                drawableResId = R.drawable.google_drive,
                contentDescriptionResId = R.string.search_engine_google_drive,
                urlTemplate = "https://drive.google.com/drive/u/0/search?q=%s",
                defaultShortcutCode = "gdr",
                appPackages = listOf(PackageConstants.GOOGLE_DRIVE_PACKAGE),
                defaultDisabledOnFirstRun = true,
            ),
        SearchEngine.GOOGLE_PHOTOS to
            SearchEngineMetadata(
                drawableResId = R.drawable.google_photos,
                contentDescriptionResId = R.string.search_engine_google_photos,
                urlTemplate = "https://photos.google.com/search/%s",
                defaultShortcutCode = "gph",
                homeUrl = "https://photos.google.com/",
                appPackages = listOf(PackageConstants.GOOGLE_PHOTOS_PACKAGE_NAME),
                defaultDisabledOnFirstRun = true,
            ),
        SearchEngine.GOOGLE_PLAY to
            SearchEngineMetadata(
                drawableResId = R.drawable.google_play,
                contentDescriptionResId = R.string.search_engine_google_play,
                urlTemplate = "https://play.google.com/store/search?q=%s&c=apps",
                defaultShortcutCode = "gpl",
                homeUrl = "https://play.google.com/store/apps",
                appPackages = listOf(PackageConstants.GOOGLE_PLAY_PACKAGE),
            ),
        SearchEngine.REDDIT to
            SearchEngineMetadata(
                drawableResId = R.drawable.reddit,
                contentDescriptionResId = R.string.search_engine_reddit,
                urlTemplate = "https://www.reddit.com/search/?q=%s",
                defaultShortcutCode = "rdt",
                homeUrl = "https://www.reddit.com",
                appPackages = listOf(PackageConstants.REDDIT_PACKAGE),
                defaultDisableIfAppMissing = true,
            ),
        SearchEngine.YOUTUBE to
            SearchEngineMetadata(
                drawableResId = R.drawable.youtube,
                contentDescriptionResId = R.string.search_engine_youtube,
                urlTemplate = "https://www.youtube.com/results?search_query=%s",
                defaultShortcutCode = "ytb",
                homeUrl = "https://www.youtube.com",
                appPackages = listOf(PackageConstants.YOUTUBE_PACKAGE),
            ),
        SearchEngine.YOUTUBE_MUSIC to
            SearchEngineMetadata(
                drawableResId = R.drawable.youtube_music,
                contentDescriptionResId = R.string.search_engine_youtube_music,
                urlTemplate = "https://music.youtube.com/search?q=%s",
                defaultShortcutCode = "ytm",
                appPackages = listOf(PackageConstants.YOUTUBE_MUSIC_PACKAGE),
                installOnly = true,
            ),
        SearchEngine.SPOTIFY to
            SearchEngineMetadata(
                drawableResId = R.drawable.spotify,
                contentDescriptionResId = R.string.search_engine_spotify,
                urlTemplate = "https://open.spotify.com/search/%s",
                defaultShortcutCode = "sfy",
                appPackages = listOf(PackageConstants.SPOTIFY_PACKAGE),
                installOnly = true,
            ),
        SearchEngine.CLAUDE to
            SearchEngineMetadata(
                drawableResId = R.drawable.claude,
                contentDescriptionResId = R.string.search_engine_claude,
                urlTemplate = "https://claude.ai/search?q=%s",
                defaultShortcutCode = "cld",
                homeUrl = "https://claude.ai",
                appPackages = listOf(PackageConstants.CLAUDE_PACKAGE),
                installOnly = true,
            ),
        SearchEngine.FACEBOOK_MARKETPLACE to
            SearchEngineMetadata(
                drawableResId = R.drawable.facebook_marketplace,
                contentDescriptionResId = R.string.search_engine_facebook_marketplace,
                urlTemplate = "https://www.facebook.com/marketplace/search/?query=%s",
                defaultShortcutCode = "fbm",
                homeUrl = "https://www.facebook.com/marketplace/",
                appPackages = listOf(PackageConstants.FACEBOOK_PACKAGE),
                defaultDisabledOnFirstRun = true,
            ),
        SearchEngine.AMAZON to
            SearchEngineMetadata(
                drawableResId = R.drawable.amazon,
                contentDescriptionResId = R.string.search_engine_amazon,
                urlTemplate = "https://www.amazon.com/s?k=%s",
                defaultShortcutCode = "amz",
                appPackages = listOf(PackageConstants.AMAZON_PACKAGE),
                defaultDisableIfAppMissing = true,
            ),
        SearchEngine.YOU_COM to
            SearchEngineMetadata(
                drawableResId = R.drawable.you_com,
                contentDescriptionResId = R.string.search_engine_you_com,
                urlTemplate = "https://you.com/search?q=%s",
                defaultShortcutCode = "yu",
                homeUrl = "https://you.com",
                appPackages = listOf(PackageConstants.YOU_COM_PACKAGE_NAME),
                defaultDisableIfAppMissing = true,
            ),
        SearchEngine.WIKIPEDIA to
            SearchEngineMetadata(
                drawableResId = R.drawable.wikipedia,
                contentDescriptionResId = R.string.search_engine_wikipedia,
                urlTemplate = "https://en.wikipedia.org/wiki/%s",
                defaultShortcutCode = "wki",
                homeUrl = "https://en.wikipedia.org/wiki/Main_Page",
                appPackages = listOf(PackageConstants.WIKIPEDIA_PACKAGE_NAME),
                defaultDisableIfAppMissing = true,
            ),
        SearchEngine.DUCKDUCKGO to
            SearchEngineMetadata(
                drawableResId = R.drawable.duckduckgo,
                contentDescriptionResId = R.string.search_engine_duckduckgo,
                urlTemplate = "https://duckduckgo.com/?q=%s",
                defaultShortcutCode = "ddg",
                defaultDisabledOnFirstRun = true,
            ),
        SearchEngine.BRAVE to
            SearchEngineMetadata(
                drawableResId = R.drawable.brave,
                contentDescriptionResId = R.string.search_engine_brave,
                urlTemplate = "https://search.brave.com/search?q=%s",
                defaultShortcutCode = "brv",
                defaultDisabledOnFirstRun = true,
            ),
        SearchEngine.BING to
            SearchEngineMetadata(
                drawableResId = R.drawable.bing,
                contentDescriptionResId = R.string.search_engine_bing,
                urlTemplate = "https://www.bing.com/search?q=%s",
                defaultShortcutCode = "bng",
                defaultDisabledOnFirstRun = true,
            ),
        SearchEngine.X to
            SearchEngineMetadata(
                drawableResId = R.drawable.x,
                contentDescriptionResId = R.string.search_engine_x,
                urlTemplate = "https://x.com/search?q=%s",
                defaultShortcutCode = "twt",
                appPackages = listOf(PackageConstants.X_PACKAGE),
                defaultDisableIfAppMissing = true,
            ),
        SearchEngine.AI_MODE to
            SearchEngineMetadata(
                drawableResId = R.drawable.ai_mode,
                contentDescriptionResId = R.string.search_engine_ai_mode,
                urlTemplate = "https://www.google.com/search?q=%s&udm=50",
                defaultShortcutCode = "gai",
                defaultDisabledOnFirstRun = true,
            ),
        SearchEngine.STARTPAGE to
            SearchEngineMetadata(
                drawableResId = R.drawable.startpage,
                contentDescriptionResId = R.string.search_engine_startpage,
                urlTemplate = "https://www.startpage.com/sp/search?query=%s",
                defaultShortcutCode = "stp",
                homeUrl = "https://www.startpage.com",
                appPackages = listOf(PackageConstants.STARTPAGE_PACKAGE_NAME),
                defaultDisableIfAppMissing = true,
            ),
    )

@DrawableRes
fun SearchEngine.getDrawableResId(): Int =
    SEARCH_ENGINE_METADATA[this]?.drawableResId
        ?: throw IllegalArgumentException("Unknown SearchEngine: $this")

fun SearchEngine.getAppPackageCandidates(): List<String> =
    SEARCH_ENGINE_METADATA[this]?.appPackages.orEmpty()

fun SearchEngine.isInstallOnlyEngine(): Boolean =
    SEARCH_ENGINE_METADATA[this]?.installOnly == true

fun SearchEngine.isDefaultDisabledOnFirstRun(): Boolean =
    SEARCH_ENGINE_METADATA[this]?.defaultDisabledOnFirstRun == true

fun SearchEngine.shouldDefaultDisableIfAppMissing(): Boolean =
    SEARCH_ENGINE_METADATA[this]?.defaultDisableIfAppMissing == true

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
    val metadata =
        SEARCH_ENGINE_METADATA[searchEngine]
            ?: throw IllegalArgumentException("Unknown SearchEngine: $searchEngine")

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
    SEARCH_ENGINE_METADATA[this]?.defaultShortcutCode
        ?: throw IllegalArgumentException("Unknown SearchEngine: $this")

@StringRes
fun SearchEngine.getContentDescriptionResId(): Int =
    SEARCH_ENGINE_METADATA[this]?.contentDescriptionResId
        ?: throw IllegalArgumentException("Unknown SearchEngine: $this")

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
