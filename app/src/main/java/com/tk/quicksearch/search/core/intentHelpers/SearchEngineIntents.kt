package com.tk.quicksearch.search.core

import android.app.Application
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.searchEngines.buildSearchUrl
import com.tk.quicksearch.searchEngines.getAppPackageCandidates
import com.tk.quicksearch.shared.util.PackageConstants

private typealias NativeSearchHandler = (Application, String) -> Unit

/** Search engine specific intent functions. */
internal object SearchEngineIntents {
    private const val GMS_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION"
    private const val GOOGLE_SEARCH_ACTION = "com.google.android.googlequicksearchbox.GOOGLE_SEARCH"
    private const val GMS_SEARCH_EXTRA_QUERY = "query"

    private val nativeHandlers: Map<SearchEngine, NativeSearchHandler> =
        mapOf(
            SearchEngine.GEMINI to ::openGemini,
            SearchEngine.GOOGLE_PHOTOS to ::openGooglePhotos,
            SearchEngine.YOU_COM to ::openYouCom,
            SearchEngine.WIKIPEDIA to ::openWikipedia,
            SearchEngine.STARTPAGE to ::openStartpage,
            SearchEngine.SPOTIFY to ::openSpotify,
            SearchEngine.WAZE to ::openWaze,
            SearchEngine.CLAUDE to ::openClaude,
            SearchEngine.GOOGLE to ::openGoogle,
            SearchEngine.GROK to ::openGrok,
        )

    fun getNativeHandler(searchEngine: SearchEngine): NativeSearchHandler? = nativeHandlers[searchEngine]

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
            val launchIntent =
                context.packageManager.getLaunchIntentForPackage(
                    PackageConstants.GEMINI_PACKAGE_NAME,
                )
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (e: ActivityNotFoundException) {
                    Log.w("GeminiLaunch", "Failed to launch Gemini app: ${e.message}")
                } catch (e: SecurityException) {
                    Log.w("GeminiLaunch", "Security exception launching Gemini: ${e.message}")
                }
            }
            Log.e("GeminiLaunch", "Failed to open Gemini app")
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
            Log.w("GeminiLaunch", "Gemini app not resolved - may not be installed")
        }

        // If share intent fails, just log it (no user-facing error)
        Log.e("GeminiLaunch", "Failed to open Gemini app with query")
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
        openWebBackedEngine(
            context = context,
            query = query,
            searchEngine = SearchEngine.WIKIPEDIA,
            packageName = PackageConstants.WIKIPEDIA_PACKAGE_NAME,
            logTag = "WikipediaLaunch",
        )
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
}
