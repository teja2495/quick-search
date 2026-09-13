package com.tk.quicksearch.search.core

import android.app.Application
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.net.URLEncoder
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.searchEngines.buildSearchUrl
import com.tk.quicksearch.searchEngines.getAppPackageCandidates
import com.tk.quicksearch.searchEngines.getNativeLaunchMode
import com.tk.quicksearch.searchEngines.SearchEngineNativeLaunchMode
import com.tk.quicksearch.shared.util.PackageConstants

private typealias NativeSearchHandler = (Application, String) -> Unit

/** Search engine specific intent functions. */
internal object SearchEngineIntents {
    private const val GMS_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION"
    private const val GOOGLE_SEARCH_ACTION = "com.google.android.googlequicksearchbox.GOOGLE_SEARCH"
    private const val GMS_SEARCH_EXTRA_QUERY = "query"

    internal data class ShareIntentSpec(
        val action: String,
        val packageName: String,
        val className: String,
        val mimeType: String,
        val text: String,
    )

    internal fun buildKagiShareIntentSpec(query: String) =
        ShareIntentSpec(
            action = Intent.ACTION_SEND,
            packageName = PackageConstants.KAGI_PACKAGE,
            className = KAGI_HOME_ACTIVITY,
            mimeType = "text/plain",
            text = query.trim(),
        )

    internal data class KagiAssistantLaunchSpec(
        val uriString: String,
        val text: String?,
    )

    internal fun buildKagiAssistantLaunchSpec(query: String): KagiAssistantLaunchSpec {
        val trimmedQuery = query.trim().ifEmpty { null }
        val uriString =
            if (trimmedQuery == null) {
                KAGI_ASSISTANT_NEW_THREAD_URI
            } else {
                "$KAGI_ASSISTANT_NEW_THREAD_URI?text=${
                    URLEncoder.encode(trimmedQuery, "UTF-8").replace("+", "%20")
                }"
            }
        return KagiAssistantLaunchSpec(uriString = uriString, text = trimmedQuery)
    }

    fun getNativeHandler(searchEngine: SearchEngine): NativeSearchHandler? =
        when (searchEngine.getNativeLaunchMode()) {
            SearchEngineNativeLaunchMode.CHATGPT -> ::openChatGpt
            SearchEngineNativeLaunchMode.GEMINI -> ::openGemini
            SearchEngineNativeLaunchMode.GOOGLE -> ::openGoogle
            SearchEngineNativeLaunchMode.GOOGLE_PHOTOS -> ::openGooglePhotos
            SearchEngineNativeLaunchMode.YOU_COM -> ::openYouCom
            SearchEngineNativeLaunchMode.WIKIPEDIA -> ::openWikipedia
            SearchEngineNativeLaunchMode.STARTPAGE -> ::openStartpage
            SearchEngineNativeLaunchMode.SPOTIFY -> ::openSpotify
            SearchEngineNativeLaunchMode.WAZE -> ::openWaze
            SearchEngineNativeLaunchMode.CLAUDE -> ::openClaude
            SearchEngineNativeLaunchMode.GROK -> ::openGrok
            SearchEngineNativeLaunchMode.GOOGLE_TRANSLATE -> ::openGoogleTranslate
            SearchEngineNativeLaunchMode.KAGI -> ::openKagi
            SearchEngineNativeLaunchMode.KAGI_ASSISTANT -> ::openKagiAssistant
            SearchEngineNativeLaunchMode.FDROID -> ::openFdroid
            SearchEngineNativeLaunchMode.NONE -> null
        }

    /** Opens F-Droid's documented search URI when the app is installed. */
    fun openFdroid(
        context: Application,
        query: String,
    ) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            openWebBackedEngine(
                context = context,
                query = trimmedQuery,
                searchEngine = SearchEngine.FDROID,
                packageName = PackageConstants.FDROID_PACKAGE,
                logTag = "FDroidLaunch",
            )
            return
        }

        val appSearchIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("fdroid.search:${Uri.encode(trimmedQuery)}")).apply {
                setPackage(PackageConstants.FDROID_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        if (IntentUtils.canResolveIntent(context, appSearchIntent)) {
            try {
                context.startActivity(appSearchIntent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.w("FDroidLaunch", "Search URI failed: ${e.message}")
            } catch (e: SecurityException) {
                Log.w("FDroidLaunch", "Search URI security exception: ${e.message}")
            }
        }

        openWebUrl(context, buildSearchUrl(trimmedQuery, SearchEngine.FDROID))
    }

    /** Opens ChatGPT when the query is empty, otherwise opens its web search URL. */
    fun openChatGpt(
        context: Application,
        query: String,
    ) {
        openWebBackedEngine(
            context = context,
            query = query,
            searchEngine = SearchEngine.CHATGPT,
            packageName = PackageConstants.CHATGPT_PACKAGE,
            logTag = "ChatGptLaunch",
        )
    }

    /** Opens Google Translate with the query pre-filled as text to translate. */
    fun openGoogleTranslate(
        context: Application,
        query: String,
    ) {
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, query)
                setPackage(PackageConstants.GOOGLE_TRANSLATE_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        if (shareIntent.resolveActivity(context.packageManager) == null) {
            Log.w("GoogleTranslateLaunch", "Google Translate app not resolved")
            return
        }

        try {
            context.startActivity(shareIntent)
        } catch (e: ActivityNotFoundException) {
            Log.w("GoogleTranslateLaunch", "Share intent failed: ${e.message}")
        } catch (e: SecurityException) {
            Log.w("GoogleTranslateLaunch", "Share intent security exception: ${e.message}")
        }
    }

    /**
     * Opens the Gemini app with the query using a share intent. If query is empty, just launches
     * the app.
     *
     * Uses ACTION_SEND intent to com.google.android.apps.bard which reliably pre-fills the query.
     * Assumes Gemini app is installed.
     */
    fun openGemini(
        context: Application,
        query: String,
    ) {
        // If query is blank, just open the app
        if (query.isBlank()) {
            launchGeminiApp(context)
            return
        }

        // If query is not blank, use share intent to pre-fill it
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, query)
                setPackage(PackageConstants.GEMINI_PACKAGE_NAME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        if (shareIntent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(shareIntent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.w("GeminiLaunch", "Share intent failed: ${e.message}")
            } catch (e: SecurityException) {
                Log.w("GeminiLaunch", "Share intent security exception: ${e.message}")
            }
        } else {
            Log.w("GeminiLaunch", "Gemini share intent not resolved; launching the app instead")
        }

        if (!launchGeminiApp(context)) {
            Log.e("GeminiLaunch", "Failed to open Gemini app with query")
        }
    }

    private fun launchGeminiApp(context: Application): Boolean {
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(
                PackageConstants.GEMINI_PACKAGE_NAME,
            ) ?: return false

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w("GeminiLaunch", "Failed to launch Gemini app: ${e.message}")
            false
        } catch (e: SecurityException) {
            Log.w("GeminiLaunch", "Security exception launching Gemini: ${e.message}")
            false
        }
    }

    /** Opens Google app with search if installed, otherwise opens web URL. */
    fun openGoogle(
        context: Application,
        query: String,
    ) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            val launchIntent =
                context.packageManager.getLaunchIntentForPackage(
                    PackageConstants.GOOGLE_APP_PACKAGE,
                )
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (_: ActivityNotFoundException) {
                } catch (_: SecurityException) {
                }
            }
        } else {
            val searchUrl = buildSearchUrl(trimmedQuery, SearchEngine.GOOGLE)
            val queryIntentFlags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

            val intentCandidates =
                listOf(
                    Intent(Intent.ACTION_SEARCH).apply {
                        setPackage(PackageConstants.GOOGLE_APP_PACKAGE)
                        putExtra(SearchManager.QUERY, trimmedQuery)
                        putExtra(GMS_SEARCH_EXTRA_QUERY, trimmedQuery)
                        addFlags(queryIntentFlags)
                    },
                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                        setPackage(PackageConstants.GOOGLE_APP_PACKAGE)
                        putExtra(SearchManager.QUERY, trimmedQuery)
                        putExtra(GMS_SEARCH_EXTRA_QUERY, trimmedQuery)
                        addFlags(queryIntentFlags)
                    },
                    Intent(GOOGLE_SEARCH_ACTION).apply {
                        setPackage(PackageConstants.GOOGLE_APP_PACKAGE)
                        putExtra(SearchManager.QUERY, trimmedQuery)
                        putExtra(GMS_SEARCH_EXTRA_QUERY, trimmedQuery)
                        addFlags(queryIntentFlags)
                    },
                    Intent(GMS_SEARCH_ACTION).apply {
                        setPackage(PackageConstants.GOOGLE_APP_PACKAGE)
                        putExtra(SearchManager.QUERY, trimmedQuery)
                        putExtra(GMS_SEARCH_EXTRA_QUERY, trimmedQuery)
                        addFlags(queryIntentFlags)
                    },
                    Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                        setPackage(PackageConstants.GOOGLE_APP_PACKAGE)
                        putExtra(SearchManager.QUERY, trimmedQuery)
                        putExtra(GMS_SEARCH_EXTRA_QUERY, trimmedQuery)
                        addFlags(queryIntentFlags)
                    },
                )

            for (candidate in intentCandidates) {
                if (!IntentUtils.canResolveIntent(context, candidate)) continue
                try {
                    context.startActivity(candidate)
                    return
                } catch (_: ActivityNotFoundException) {
                } catch (_: SecurityException) {
                }
            }
        }
        openWebUrl(context, buildSearchUrl(trimmedQuery, SearchEngine.GOOGLE))
    }

    /** Opens Google Photos app if installed, otherwise opens web URL. */
    fun openGooglePhotos(
        context: Application,
        query: String,
    ) {
        openWebBackedEngine(
            context = context,
            query = query,
            searchEngine = SearchEngine.GOOGLE_PHOTOS,
            packageName = PackageConstants.GOOGLE_PHOTOS_PACKAGE_NAME,
            logTag = "GooglePhotosLaunch",
        )
    }

    /** Opens You.com app if installed, otherwise opens web URL. */
    fun openYouCom(
        context: Application,
        query: String,
    ) {
        openWebBackedEngine(
            context = context,
            query = query,
            searchEngine = SearchEngine.YOU_COM,
            packageName = PackageConstants.YOU_COM_PACKAGE_NAME,
            logTag = "YouComLaunch",
        )
    }

    /** Opens Wikipedia app if installed, otherwise opens web URL. */
    fun openWikipedia(
        context: Application,
        query: String,
    ) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            openWebBackedEngine(
                context = context,
                query = trimmedQuery,
                searchEngine = SearchEngine.WIKIPEDIA,
                packageName = PackageConstants.WIKIPEDIA_PACKAGE_NAME,
                logTag = "WikipediaLaunch",
            )
            return
        }

        val appSearchIntent =
            Intent(Intent.ACTION_SEND).apply {
                setPackage(PackageConstants.WIKIPEDIA_PACKAGE_NAME)
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, trimmedQuery)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        if (IntentUtils.canResolveIntent(context, appSearchIntent)) {
            try {
                context.startActivity(appSearchIntent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.w("WikipediaLaunch", "Search intent failed: ${e.message}")
            } catch (e: SecurityException) {
                Log.w("WikipediaLaunch", "Search security exception: ${e.message}")
            }
        }

        openWebUrl(context, buildSearchUrl(trimmedQuery, SearchEngine.WIKIPEDIA))
    }

    /** Opens Startpage app if installed, otherwise opens web URL. */
    fun openStartpage(
        context: Application,
        query: String,
    ) {
        openWebBackedEngine(
            context = context,
            query = query,
            searchEngine = SearchEngine.STARTPAGE,
            packageName = PackageConstants.STARTPAGE_PACKAGE_NAME,
            logTag = "StartpageLaunch",
        )
    }

    /** Opens Spotify app if installed, otherwise opens web URL. */
    fun openSpotify(
        context: Application,
        query: String,
    ) {
        openWebBackedEngine(
            context = context,
            query = query,
            searchEngine = SearchEngine.SPOTIFY,
            packageName = PackageConstants.SPOTIFY_PACKAGE,
            logTag = "SpotifyLaunch",
        )
    }

    /** Opens Waze app with query (when possible), otherwise opens web URL. */
    fun openWaze(
        context: Application,
        query: String,
    ) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(PackageConstants.WAZE_PACKAGE)

        if (query.isBlank()) {
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (_: ActivityNotFoundException) {
                } catch (_: SecurityException) {
                }
            }
            openWebUrl(context, buildSearchUrl(query, SearchEngine.WAZE))
            return
        }

        val encodedQuery = Uri.encode(query)
        val deepLinkIntents =
            listOf(
                Intent(Intent.ACTION_VIEW, Uri.parse("waze://?q=$encodedQuery")),
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.waze.com/ul?q=$encodedQuery")),
            ).map { intent ->
                intent.apply {
                    setPackage(PackageConstants.WAZE_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

        for (intent in deepLinkIntents) {
            if (!IntentUtils.canResolveIntent(context, intent)) continue
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }

        openWebUrl(context, buildSearchUrl(query, SearchEngine.WAZE))
    }

    /** Opens Claude app if installed with query via share intent; otherwise opens web URL. */
    fun openClaude(
        context: Application,
        query: String,
    ) {
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(
                PackageConstants.CLAUDE_PACKAGE,
            )

        if (launchIntent == null) {
            openWebUrl(context, buildSearchUrl(query, SearchEngine.CLAUDE))
            return
        }

        if (query.isBlank()) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                return
            } catch (e: Exception) {
                Log.w("ClaudeLaunch", "Failed to launch Claude: ${e.message}")
            }
            openWebUrl(context, buildSearchUrl(query, SearchEngine.CLAUDE))
            return
        }

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, query)
                setPackage(PackageConstants.CLAUDE_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        if (shareIntent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(shareIntent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.w("ClaudeLaunch", "Share intent failed: ${e.message}")
            } catch (e: SecurityException) {
                Log.w("ClaudeLaunch", "Share security exception: ${e.message}")
            }
        }

        openWebUrl(context, buildSearchUrl(query, SearchEngine.CLAUDE))
    }

    /** Opens Grok app if installed; for non-empty query tries app-targeted search first. */
    fun openGrok(
        context: Application,
        query: String,
    ) {
        val trimmedQuery = query.trim()
        val packageCandidates = SearchEngine.GROK.getAppPackageCandidates()
        val searchUrl = buildSearchUrl(trimmedQuery, SearchEngine.GROK)

        if (trimmedQuery.isBlank()) {
            for (packageName in packageCandidates) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent == null) continue
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (_: ActivityNotFoundException) {
                } catch (_: SecurityException) {
                }
            }
            openWebUrl(context, searchUrl)
            return
        }

        for (packageName in packageCandidates) {
            val appSearchIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            if (IntentUtils.canResolveIntent(context, appSearchIntent)) {
                try {
                    context.startActivity(appSearchIntent)
                    return
                } catch (_: ActivityNotFoundException) {
                } catch (_: SecurityException) {
                }
            }
        }

        openWebUrl(context, searchUrl)
    }

    /** Opens Kagi's installed Android app with the search URL, otherwise opens it in a browser. */
    fun openKagi(
        context: Application,
        query: String,
    ) {
        val spec = buildKagiShareIntentSpec(query)
        val appSearchIntent =
            Intent(spec.action).apply {
                setClassName(spec.packageName, spec.className)
                type = spec.mimeType
                putExtra(Intent.EXTRA_TEXT, spec.text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        if (IntentUtils.canResolveIntent(context, appSearchIntent)) {
            try {
                context.startActivity(appSearchIntent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }

        openWebUrl(context, buildSearchUrl(spec.text, SearchEngine.KAGI))
    }

    /** Opens Kagi Assistant's new-thread deep link with the query prefilled. */
    fun openKagiAssistant(
        context: Application,
        query: String,
    ) {
        val trimmedQuery = query.trim()
        val spec = buildKagiAssistantLaunchSpec(trimmedQuery)
        val appSearchIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(spec.uriString)).apply {
                setClassName(PackageConstants.KAGI_ASSISTANT_PACKAGE, KAGI_ASSISTANT_MAIN_ACTIVITY)
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

        if (IntentUtils.canResolveIntent(context, appSearchIntent)) {
            try {
                context.startActivity(appSearchIntent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }

        openWebUrl(context, buildSearchUrl(trimmedQuery, SearchEngine.KAGI_ASSISTANT))
    }

    /** Opens a web URL in a browser. */
    private fun openWebUrl(
        context: Application,
        url: String,
    ) {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        if (!IntentUtils.canResolveIntent(context, intent)) {
            Log.w("OpenWebUrl", "No activity found to handle URL: $url")
            return
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("OpenWebUrl", "Failed to open URL: ${e.message}")
        } catch (e: SecurityException) {
            Log.w("OpenWebUrl", "Security exception opening URL: ${e.message}")
        }
    }

    private fun openWebBackedEngine(
        context: Application,
        query: String,
        searchEngine: SearchEngine,
        packageName: String,
        logTag: String,
    ) {
        if (query.isBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (e: Exception) {
                    Log.w(logTag, "Failed to launch $packageName: ${e.message}")
                }
            }
        }

        openWebUrl(context, buildSearchUrl(query, searchEngine))
    }

    private const val KAGI_HOME_ACTIVITY = "com.kagi.search.HomeActivity"
    private const val KAGI_ASSISTANT_MAIN_ACTIVITY = "com.kagi.assistant.MainActivity"
    private const val KAGI_ASSISTANT_NEW_THREAD_URI = "com.kagi.assistant://thread/new"
}
