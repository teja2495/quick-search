package com.tk.quicksearch.search.core

import android.app.Application
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.searchEngines.buildCustomSearchUrl
import com.tk.quicksearch.searchEngines.buildSearchUrl
import com.tk.quicksearch.searchEngines.getDisplayNameResId
import java.util.Locale

/** Search and URL opening intents. */
internal object SearchIntents {
    /** Opens a search URL with the specified search engine. */
    fun openSearchUrl(
        context: Application,
        query: String,
        searchEngine: SearchEngine,
        amazonDomain: String? = null,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        // Handle apps with native integrations
        when (searchEngine) {
            SearchEngine.GEMINI -> {
                SearchEngineIntents.openGemini(context, query)
                return
            }

            SearchEngine.GOOGLE_PHOTOS -> {
                SearchEngineIntents.openGooglePhotos(context, query)
                return
            }

            SearchEngine.YOU_COM -> {
                SearchEngineIntents.openYouCom(context, query)
                return
            }

            SearchEngine.WIKIPEDIA -> {
                SearchEngineIntents.openWikipedia(context, query)
                return
            }

            SearchEngine.STARTPAGE -> {
                SearchEngineIntents.openStartpage(context, query)
                return
            }

            SearchEngine.SPOTIFY -> {
                SearchEngineIntents.openSpotify(context, query)
                return
            }

            SearchEngine.WAZE -> {
                SearchEngineIntents.openWaze(context, query)
                return
            }

            SearchEngine.CLAUDE -> {
                SearchEngineIntents.openClaude(context, query)
                return
            }

            SearchEngine.GOOGLE -> {
                SearchEngineIntents.openGoogle(context, query)
                return
            }

            SearchEngine.GROK -> {
                SearchEngineIntents.openGrok(context, query)
                return
            }

            else -> {} // Continue to web URL
        }

        val searchUrl = buildSearchUrl(query, searchEngine, amazonDomain)

        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        if (!IntentUtils.canResolveIntent(context, intent)) {
            onShowToast?.invoke(
                R.string.error_open_search_engine,
                context.getString(searchEngine.getDisplayNameResId()),
            )
            return
        }

        try {
            context.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            onShowToast?.invoke(
                R.string.error_open_search_engine,
                context.getString(searchEngine.getDisplayNameResId()),
            )
        } catch (exception: SecurityException) {
            onShowToast?.invoke(
                R.string.error_open_search_engine,
                context.getString(searchEngine.getDisplayNameResId()),
            )
        }
    }

    fun openBrowserSearch(
        context: Application,
        query: String,
        browserPackageName: String,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        val browserLabel = resolveAppLabel(context, browserPackageName)
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(browserPackageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (_: ActivityNotFoundException) {
                } catch (_: SecurityException) {
                }
            }
            onShowToast?.invoke(R.string.error_open_search_engine, browserLabel)
            return
        }

        val intent =
            Intent(Intent.ACTION_WEB_SEARCH).apply {
                setPackage(browserPackageName)
                putExtra(SearchManager.QUERY, trimmedQuery)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        try {
            if (IntentUtils.canResolveIntent(context, intent)) {
                context.startActivity(intent)
                return
            }
            if (tryOpenGoogleSearchInBrowser(context, trimmedQuery, browserPackageName)) return
        } catch (_: ActivityNotFoundException) {
            if (tryOpenGoogleSearchInBrowser(context, trimmedQuery, browserPackageName)) return
        } catch (_: SecurityException) {
            if (tryOpenGoogleSearchInBrowser(context, trimmedQuery, browserPackageName)) return
        }

        // Final fallback: open Google search in default browser
        openSearchUrl(context, trimmedQuery, SearchEngine.GOOGLE, onShowToast = onShowToast)
    }

    fun openBrowserUrl(
        context: Application,
        url: String,
        browserPackageName: String,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        val browserLabel = resolveAppLabel(context, browserPackageName)
        val normalizedUrl = normalizeToBrowsableUrl(url)
        if (normalizedUrl == null) {
            onShowToast?.invoke(R.string.error_open_search_engine, browserLabel)
            return
        }

        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
                setPackage(browserPackageName)
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        if (tryStartIntent(context, browserIntent)) return

        val fallbackIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        if (tryStartIntent(context, fallbackIntent)) return

        onShowToast?.invoke(R.string.error_open_search_engine, browserLabel ?: inferDisplayLabelFromUrl(normalizedUrl))
    }

    fun openCustomSearchUrl(
        context: Application,
        query: String,
        urlTemplate: String,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        val rawUrl = buildCustomSearchUrl(query, urlTemplate)
        val url = normalizeToBrowsableUrl(rawUrl) ?: rawUrl
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        if (tryStartIntent(context, intent)) return

        onShowToast?.invoke(R.string.error_open_search_engine, inferDisplayLabelFromUrl(url))
    }

    private fun normalizeToBrowsableUrl(url: String): String? {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return null

        return when {
            trimmedUrl.contains("://") -> trimmedUrl
            trimmedUrl.matches(Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$")) -> "https://$trimmedUrl"
            else -> "https://www.google.com/search?q=${Uri.encode(trimmedUrl)}"
        }
    }

    private fun tryStartIntent(
        context: Application,
        intent: Intent,
    ): Boolean =
        try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }

    private fun resolveAppLabel(
        context: Application,
        packageName: String,
    ): String? =
        runCatching {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString().takeIf { it.isNotBlank() }
        }.getOrNull()

    private fun inferDisplayLabelFromUrl(url: String): String {
        val parsed = Uri.parse(url)
        val host = parsed.host?.takeIf { it.isNotBlank() }
        if (host != null) {
            return host.removePrefix("www.")
        }
        val scheme = parsed.scheme?.takeIf { it.isNotBlank() } ?: return url
        return scheme.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase(Locale.getDefault()) else first.toString()
        }
    }

    private fun tryOpenGoogleSearchInBrowser(
        context: Application,
        query: String,
        browserPackageName: String,
    ): Boolean {
        val googleSearchUrl = buildSearchUrl(query, SearchEngine.GOOGLE)
        val googleIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(googleSearchUrl)).apply {
                setPackage(browserPackageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        if (!IntentUtils.canResolveIntent(context, googleIntent)) return false
        return try {
            context.startActivity(googleIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
