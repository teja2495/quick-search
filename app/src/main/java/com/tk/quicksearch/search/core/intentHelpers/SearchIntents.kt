package com.tk.quicksearch.search.core

import android.app.Application
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.searchEngines.IN_APP_BROWSER_PACKAGE
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
        SearchEngineIntents.getNativeHandler(searchEngine)?.let { handler ->
            handler(context, query)
            return
        }

        val searchUrl = buildSearchUrl(query, searchEngine, amazonDomain)

        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        if (!IntentUtils.canResolveIntent(context, intent)) {
            onShowToast?.invoke(
                R.string.common_error_unable_to_open,
                context.getString(searchEngine.getDisplayNameResId()),
            )
            return
        }

        try {
            context.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            onShowToast?.invoke(
                R.string.common_error_unable_to_open,
                context.getString(searchEngine.getDisplayNameResId()),
            )
        } catch (exception: SecurityException) {
            onShowToast?.invoke(
                R.string.common_error_unable_to_open,
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
        if (browserPackageName == IN_APP_BROWSER_PACKAGE) {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isBlank()) {
                openUrlInCustomTabsOrFallback(context, "https://www.google.com", onShowToast)
                return
            }
            val searchUrl = buildSearchUrl(trimmedQuery, SearchEngine.GOOGLE)
            openUrlInCustomTabsOrFallback(context, searchUrl, onShowToast)
            return
        }

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
            onShowToast?.invoke(R.string.common_error_unable_to_open, browserLabel)
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
        if (browserPackageName == IN_APP_BROWSER_PACKAGE) {
            val normalizedUrl = normalizeToBrowsableUrl(url)
            if (normalizedUrl == null) {
                onShowToast?.invoke(
                    R.string.common_error_unable_to_open,
                    context.getString(R.string.browser_in_app_name),
                )
                return
            }
            openUrlInCustomTabsOrFallback(context, normalizedUrl, onShowToast)
            return
        }

        val browserLabel = resolveAppLabel(context, browserPackageName)
        val normalizedUrl = normalizeToBrowsableUrl(url)
        if (normalizedUrl == null) {
            onShowToast?.invoke(R.string.common_error_unable_to_open, browserLabel)
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

        onShowToast?.invoke(R.string.common_error_unable_to_open, browserLabel ?: inferDisplayLabelFromUrl(normalizedUrl))
    }

    fun openCustomSearchUrl(
        context: Application,
        query: String,
        urlTemplate: String,
        browserPackage: String? = null,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        val rawUrl = buildCustomSearchUrl(query, urlTemplate)
        if (!browserPackage.isNullOrBlank()) {
            openBrowserUrl(context, rawUrl, browserPackage, onShowToast)
            return
        }
        val url = normalizeToBrowsableUrl(rawUrl) ?: rawUrl
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        if (tryStartIntent(context, intent)) return

        onShowToast?.invoke(R.string.common_error_unable_to_open, inferDisplayLabelFromUrl(url))
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

    private fun openUrlInCustomTabsOrFallback(
        context: Application,
        url: String,
        onShowToast: ((Int, String?) -> Unit)?,
    ) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build().apply {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            customTabsIntent.launchUrl(context, Uri.parse(url))
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }

        val fallback =
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        if (tryStartIntent(context, fallback)) return

        onShowToast?.invoke(
            R.string.common_error_unable_to_open,
            context.getString(R.string.browser_in_app_name),
        )
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
