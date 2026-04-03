package com.tk.quicksearch.searchEngines

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.shared.util.PackageConstants

enum class SearchEngineIconColorPolicy {
    NONE,
    INVERT_ON_LIGHT,
    DARKEN_ON_LIGHT,
}

enum class SearchEngineNativeLaunchMode {
    NONE,
    GEMINI,
    GOOGLE,
    GOOGLE_PHOTOS,
    YOU_COM,
    WIKIPEDIA,
    STARTPAGE,
    SPOTIFY,
    WAZE,
    CLAUDE,
    GROK,
}

data class SearchEngineDefinition(
    val engine: SearchEngine,
    @DrawableRes val drawableResId: Int,
    @StringRes val contentDescriptionResId: Int,
    val urlTemplate: String,
    val defaultShortcutCode: String,
    val homeUrl: String? = null,
    val appPackages: List<String> = emptyList(),
    val installOnly: Boolean = false,
    val defaultDisabledOnFirstRun: Boolean = false,
    val defaultDisableIfAppMissing: Boolean = false,
    val iconColorPolicy: SearchEngineIconColorPolicy = SearchEngineIconColorPolicy.NONE,
    val nativeLaunchMode: SearchEngineNativeLaunchMode = SearchEngineNativeLaunchMode.NONE,
)

/**
 * Central registry for built-in search engines.
 *
 * To add a new built-in search engine, add one entry in [definitions] and corresponding
 * string/drawable resources. Most search-engine behavior derives from this registry.
 */
object SearchEngineRegistry {
    val definitions: List<SearchEngineDefinition> =
        listOf(
            SearchEngineDefinition(
                engine = SearchEngine.DIRECT_SEARCH,
                drawableResId = R.drawable.direct_search,
                contentDescriptionResId = R.string.search_engine_direct_search,
                urlTemplate = "",
                defaultShortcutCode = "dsh",
            ),
            SearchEngineDefinition(
                engine = SearchEngine.GOOGLE,
                drawableResId = R.drawable.google,
                contentDescriptionResId = R.string.search_engine_google,
                urlTemplate = "https://www.google.com/search?q=%s",
                defaultShortcutCode = "ggl",
                appPackages = listOf(PackageConstants.GOOGLE_APP_PACKAGE),
                nativeLaunchMode = SearchEngineNativeLaunchMode.GOOGLE,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.CHATGPT,
                drawableResId = R.drawable.chatgpt,
                contentDescriptionResId = R.string.search_engine_chatgpt,
                urlTemplate = "https://chatgpt.com/?prompt=%s",
                defaultShortcutCode = "cgpt",
                appPackages = listOf(PackageConstants.CHATGPT_PACKAGE),
                iconColorPolicy = SearchEngineIconColorPolicy.INVERT_ON_LIGHT,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.GEMINI,
                drawableResId = R.drawable.ic_gemini_sparkle_search_engine,
                contentDescriptionResId = R.string.search_engine_gemini,
                urlTemplate = "https://gemini.google.com/app?text=%s",
                defaultShortcutCode = "gmi",
                homeUrl = "https://gemini.google.com/app",
                appPackages = listOf(PackageConstants.GEMINI_PACKAGE_NAME),
                nativeLaunchMode = SearchEngineNativeLaunchMode.GEMINI,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.PERPLEXITY,
                drawableResId = R.drawable.perplexity,
                contentDescriptionResId = R.string.search_engine_perplexity,
                urlTemplate = "https://www.perplexity.ai/search?q=%s",
                defaultShortcutCode = "ppx",
                appPackages = listOf(PackageConstants.PERPLEXITY_PACKAGE),
            ),
            SearchEngineDefinition(
                engine = SearchEngine.GROK,
                drawableResId = R.drawable.grok,
                contentDescriptionResId = R.string.search_engine_grok,
                urlTemplate = "https://grok.com/?q=%s",
                defaultShortcutCode = "grk",
                appPackages = listOf(PackageConstants.GROK_PACKAGE),
                iconColorPolicy = SearchEngineIconColorPolicy.INVERT_ON_LIGHT,
                nativeLaunchMode = SearchEngineNativeLaunchMode.GROK,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.GOOGLE_MAPS,
                drawableResId = R.drawable.google_maps,
                contentDescriptionResId = R.string.search_engine_google_maps,
                urlTemplate = "https://maps.google.com/?q=%s",
                defaultShortcutCode = "mps",
                appPackages = listOf(PackageConstants.GOOGLE_MAPS_PACKAGE),
            ),
            SearchEngineDefinition(
                engine = SearchEngine.WAZE,
                drawableResId = R.drawable.waze,
                contentDescriptionResId = R.string.search_engine_waze,
                urlTemplate = "https://www.waze.com/ul?q=%s",
                defaultShortcutCode = "wze",
                homeUrl = "https://www.waze.com",
                appPackages = listOf(PackageConstants.WAZE_PACKAGE),
                installOnly = true,
                nativeLaunchMode = SearchEngineNativeLaunchMode.WAZE,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.GOOGLE_DRIVE,
                drawableResId = R.drawable.google_drive,
                contentDescriptionResId = R.string.search_engine_google_drive,
                urlTemplate = "https://drive.google.com/drive/u/0/search?q=%s",
                defaultShortcutCode = "gdr",
                appPackages = listOf(PackageConstants.GOOGLE_DRIVE_PACKAGE),
                defaultDisabledOnFirstRun = true,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.GOOGLE_PHOTOS,
                drawableResId = R.drawable.google_photos,
                contentDescriptionResId = R.string.search_engine_google_photos,
                urlTemplate = "https://photos.google.com/search/%s",
                defaultShortcutCode = "gph",
                homeUrl = "https://photos.google.com/",
                appPackages = listOf(PackageConstants.GOOGLE_PHOTOS_PACKAGE_NAME),
                defaultDisabledOnFirstRun = true,
                nativeLaunchMode = SearchEngineNativeLaunchMode.GOOGLE_PHOTOS,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.GOOGLE_PLAY,
                drawableResId = R.drawable.google_play,
                contentDescriptionResId = R.string.search_engine_google_play,
                urlTemplate = "https://play.google.com/store/search?q=%s&c=apps",
                defaultShortcutCode = "gpl",
                homeUrl = "https://play.google.com/store/apps",
                appPackages = listOf(PackageConstants.GOOGLE_PLAY_PACKAGE),
            ),
            SearchEngineDefinition(
                engine = SearchEngine.REDDIT,
                drawableResId = R.drawable.reddit,
                contentDescriptionResId = R.string.search_engine_reddit,
                urlTemplate = "https://www.reddit.com/search/?q=%s",
                defaultShortcutCode = "rdt",
                homeUrl = "https://www.reddit.com",
                appPackages = listOf(PackageConstants.REDDIT_PACKAGE),
                defaultDisableIfAppMissing = true,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.YOUTUBE,
                drawableResId = R.drawable.youtube,
                contentDescriptionResId = R.string.search_engine_youtube,
                urlTemplate = "https://www.youtube.com/results?search_query=%s",
                defaultShortcutCode = "ytb",
                homeUrl = "https://www.youtube.com",
                appPackages = listOf(PackageConstants.YOUTUBE_PACKAGE),
            ),
            SearchEngineDefinition(
                engine = SearchEngine.YOUTUBE_MUSIC,
                drawableResId = R.drawable.youtube_music,
                contentDescriptionResId = R.string.search_engine_youtube_music,
                urlTemplate = "https://music.youtube.com/search?q=%s",
                defaultShortcutCode = "ytm",
                appPackages = listOf(PackageConstants.YOUTUBE_MUSIC_PACKAGE),
                installOnly = true,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.SPOTIFY,
                drawableResId = R.drawable.spotify,
                contentDescriptionResId = R.string.search_engine_spotify,
                urlTemplate = "https://open.spotify.com/search/%s",
                defaultShortcutCode = "sfy",
                appPackages = listOf(PackageConstants.SPOTIFY_PACKAGE),
                installOnly = true,
                nativeLaunchMode = SearchEngineNativeLaunchMode.SPOTIFY,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.CLAUDE,
                drawableResId = R.drawable.claude,
                contentDescriptionResId = R.string.search_engine_claude,
                urlTemplate = "https://claude.ai/search?q=%s",
                defaultShortcutCode = "cld",
                homeUrl = "https://claude.ai",
                appPackages = listOf(PackageConstants.CLAUDE_PACKAGE),
                installOnly = true,
                nativeLaunchMode = SearchEngineNativeLaunchMode.CLAUDE,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.FACEBOOK_MARKETPLACE,
                drawableResId = R.drawable.facebook_marketplace,
                contentDescriptionResId = R.string.search_engine_facebook_marketplace,
                urlTemplate = "https://www.facebook.com/marketplace/search/?query=%s",
                defaultShortcutCode = "fbm",
                homeUrl = "https://www.facebook.com/marketplace/",
                appPackages = listOf(PackageConstants.FACEBOOK_PACKAGE),
                defaultDisabledOnFirstRun = true,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.AMAZON,
                drawableResId = R.drawable.amazon,
                contentDescriptionResId = R.string.search_engine_amazon,
                urlTemplate = "https://www.amazon.com/s?k=%s",
                defaultShortcutCode = "amz",
                appPackages = listOf(PackageConstants.AMAZON_PACKAGE),
                defaultDisableIfAppMissing = true,
                iconColorPolicy = SearchEngineIconColorPolicy.DARKEN_ON_LIGHT,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.YOU_COM,
                drawableResId = R.drawable.you_com,
                contentDescriptionResId = R.string.search_engine_you_com,
                urlTemplate = "https://you.com/search?q=%s",
                defaultShortcutCode = "yu",
                homeUrl = "https://you.com",
                appPackages = listOf(PackageConstants.YOU_COM_PACKAGE_NAME),
                defaultDisableIfAppMissing = true,
                nativeLaunchMode = SearchEngineNativeLaunchMode.YOU_COM,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.WIKIPEDIA,
                drawableResId = R.drawable.wikipedia,
                contentDescriptionResId = R.string.search_engine_wikipedia,
                urlTemplate = "https://en.wikipedia.org/wiki/%s",
                defaultShortcutCode = "wki",
                homeUrl = "https://en.wikipedia.org/wiki/Main_Page",
                appPackages = listOf(PackageConstants.WIKIPEDIA_PACKAGE_NAME),
                defaultDisableIfAppMissing = true,
                iconColorPolicy = SearchEngineIconColorPolicy.INVERT_ON_LIGHT,
                nativeLaunchMode = SearchEngineNativeLaunchMode.WIKIPEDIA,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.DUCKDUCKGO,
                drawableResId = R.drawable.duckduckgo,
                contentDescriptionResId = R.string.search_engine_duckduckgo,
                urlTemplate = "https://duckduckgo.com/?q=%s",
                defaultShortcutCode = "ddg",
                defaultDisabledOnFirstRun = true,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.BRAVE,
                drawableResId = R.drawable.brave,
                contentDescriptionResId = R.string.search_engine_brave,
                urlTemplate = "https://search.brave.com/search?q=%s",
                defaultShortcutCode = "brv",
                defaultDisabledOnFirstRun = true,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.BING,
                drawableResId = R.drawable.bing,
                contentDescriptionResId = R.string.search_engine_bing,
                urlTemplate = "https://www.bing.com/search?q=%s",
                defaultShortcutCode = "bng",
                defaultDisabledOnFirstRun = true,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.X,
                drawableResId = R.drawable.x,
                contentDescriptionResId = R.string.search_engine_x,
                urlTemplate = "https://x.com/search?q=%s",
                defaultShortcutCode = "twt",
                appPackages = listOf(PackageConstants.X_PACKAGE),
                defaultDisableIfAppMissing = true,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.AI_MODE,
                drawableResId = R.drawable.ai_mode,
                contentDescriptionResId = R.string.search_engine_ai_mode,
                urlTemplate = "https://www.google.com/search?q=%s&udm=50",
                defaultShortcutCode = "gai",
                defaultDisabledOnFirstRun = true,
            ),
            SearchEngineDefinition(
                engine = SearchEngine.STARTPAGE,
                drawableResId = R.drawable.startpage,
                contentDescriptionResId = R.string.search_engine_startpage,
                urlTemplate = "https://www.startpage.com/sp/search?query=%s",
                defaultShortcutCode = "stp",
                homeUrl = "https://www.startpage.com",
                appPackages = listOf(PackageConstants.STARTPAGE_PACKAGE_NAME),
                defaultDisableIfAppMissing = true,
                nativeLaunchMode = SearchEngineNativeLaunchMode.STARTPAGE,
            ),
        )

    private val definitionMap: Map<SearchEngine, SearchEngineDefinition> =
        definitions.associateBy { it.engine }

    init {
        check(definitionMap.size == definitions.size) {
            "Duplicate SearchEngine definitions found in SearchEngineRegistry"
        }
        val missing = SearchEngine.values().filterNot { it in definitionMap.keys }
        check(missing.isEmpty()) {
            "Missing SearchEngine definitions for: ${missing.joinToString()}"
        }
    }

    fun get(engine: SearchEngine): SearchEngineDefinition =
        definitionMap[engine]
            ?: throw IllegalArgumentException("Unknown SearchEngine: $engine")
}
