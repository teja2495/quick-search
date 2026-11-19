package com.tk.quicksearch.search.apps.appLock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.tk.quicksearch.searchEngines.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.IN_APP_BROWSER_PACKAGE
import com.tk.quicksearch.searchEngines.buildCustomSearchUrl
import com.tk.quicksearch.searchEngines.buildSearchUrl
import com.tk.quicksearch.searchEngines.getAppPackageCandidates

/**
 * Works out which installed apps a search target could open, so a locked app (YouTube, Spotify, a
 * locked browser, ...) can't be reached by searching in it instead of tapping its icon.
 */
object SearchTargetAppLock {
    fun packagesFor(
        context: Context,
        query: String,
        target: SearchTarget,
        amazonDomain: String? = null,
    ): List<String> =
        when (target) {
            is SearchTarget.Engine -> enginePackages(context, query, target.engine, amazonDomain)
            is SearchTarget.Browser -> browserPackages(target.app.packageName)
            is SearchTarget.Custom -> customPackages(context, query, target.custom.urlTemplate, target.custom.browserPackage)
        }

    fun enginePackages(
        context: Context,
        query: String,
        engine: SearchEngine,
        amazonDomain: String? = null,
    ): List<String> {
        if (engine == SearchEngine.DIRECT_SEARCH) return emptyList()
        // The engine's own app when installed, plus whatever app the web URL resolves to (an app
        // link such as YouTube, or the default browser).
        val nativeApps = engine.getAppPackageCandidates().filter { context.isInstalled(it) }
        val urlHandler = runCatching { buildSearchUrl(query, engine, amazonDomain) }.getOrNull()?.let { context.resolveUrlHandler(it) }
        return (nativeApps + listOfNotNull(urlHandler)).distinct()
    }

    fun browserPackages(browserPackage: String): List<String> =
        if (browserPackage.isBlank() || browserPackage == IN_APP_BROWSER_PACKAGE) emptyList() else listOf(browserPackage)

    fun customPackages(
        context: Context,
        query: String,
        urlTemplate: String,
        browserPackage: String?,
    ): List<String> {
        if (!browserPackage.isNullOrBlank()) return browserPackages(browserPackage)
        return listOfNotNull(context.resolveUrlHandler(buildCustomSearchUrl(query, urlTemplate)))
    }

    private fun Context.isInstalled(packageName: String): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) != null

    /** The app that would handle [url] without asking, or null when the system would show a chooser. */
    private fun Context.resolveUrlHandler(url: String): String? {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        val handler =
            runCatching {
                packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            }.getOrNull()
        // "android" is the system chooser; it means no single app would open.
        return handler?.takeUnless { it == "android" || it == packageName }
    }
}
