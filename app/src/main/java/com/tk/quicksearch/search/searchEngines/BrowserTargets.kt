package com.tk.quicksearch.search.searchEngines

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.tk.quicksearch.search.core.SearchTarget

fun resolveDefaultBrowserPackage(context: Context): String? {
    val packageManager = context.packageManager
    val webViewIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                webViewIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )?.activityInfo?.packageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(webViewIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        }
    }.getOrNull()
}

fun orderedBrowserTargets(
    targets: List<SearchTarget>,
    defaultBrowserPackage: String?,
): List<SearchTarget.Browser> {
    val browsers = targets.filterIsInstance<SearchTarget.Browser>()
    if (defaultBrowserPackage == null) return browsers

    val default = browsers.firstOrNull { it.app.packageName == defaultBrowserPackage }
    if (default == null) return browsers

    return listOf(default) + browsers.filterNot { it.app.packageName == defaultBrowserPackage }
}

fun defaultBrowserTarget(
    targets: List<SearchTarget>,
    defaultBrowserPackage: String?,
): SearchTarget.Browser? {
    val ordered = orderedBrowserTargets(targets, defaultBrowserPackage)
    return ordered.firstOrNull()
}
