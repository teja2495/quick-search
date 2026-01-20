package com.tk.quicksearch.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService

object InAppBrowserUtils {
    private val blockedPackages = setOf("com.github.android")
    private const val probeUrl = "https://www.example.com"

    fun openUrl(context: Context, url: String) {
        val uri = Uri.parse(url)
        val fallbackPackage = resolveBrowserPackage(context)
        val browserPackage = resolveCustomTabsPackage(context)
        if (browserPackage == null) {
            openFallback(context, uri, fallbackPackage)
            return
        }
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.intent.setPackage(browserPackage)

        if (context !is Activity) {
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            openFallback(context, uri, fallbackPackage ?: browserPackage)
        }
    }

    private fun openFallback(context: Context, uri: Uri, browserPackage: String?) {
        val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (browserPackage != null) {
                setPackage(browserPackage)
            }
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
        }
    }

    private fun resolveCustomTabsPackage(context: Context): String? {
        val packageManager = context.packageManager
        val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse(probeUrl))
        val resolvedActivities = queryIntentActivitiesCompat(packageManager, activityIntent)
        val customTabsPackages = resolvedActivities
            .mapNotNull { it.activityInfo?.packageName }
            .filterNot { blockedPackages.contains(it) }
            .filter { supportsCustomTabs(packageManager, it) }

        if (customTabsPackages.isEmpty()) return null

        val defaultPackage =
            resolveActivityCompat(packageManager, activityIntent)?.activityInfo?.packageName
        return if (defaultPackage != null && customTabsPackages.contains(defaultPackage)) {
            defaultPackage
        } else {
            customTabsPackages.first()
        }
    }

    private fun resolveBrowserPackage(context: Context): String? {
        val packageManager = context.packageManager
        val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse(probeUrl))
        val resolvedActivities = queryIntentActivitiesCompat(packageManager, activityIntent)
        val packages = resolvedActivities
            .mapNotNull { it.activityInfo?.packageName }
            .filterNot { blockedPackages.contains(it) }

        if (packages.isEmpty()) return null

        val defaultPackage =
            resolveActivityCompat(packageManager, activityIntent)?.activityInfo?.packageName
        return if (defaultPackage != null && packages.contains(defaultPackage)) {
            defaultPackage
        } else {
            packages.first()
        }
    }

    private fun supportsCustomTabs(packageManager: PackageManager, packageName: String): Boolean {
        val serviceIntent =
            Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION).setPackage(packageName)
        return resolveServiceCompat(packageManager, serviceIntent) != null
    }

    private fun queryIntentActivitiesCompat(
        packageManager: PackageManager,
        intent: Intent
    ): List<android.content.pm.ResolveInfo> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        }.getOrDefault(emptyList())
    }

    private fun resolveActivityCompat(
        packageManager: PackageManager,
        intent: Intent
    ): android.content.pm.ResolveInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        }.getOrNull()
    }

    private fun resolveServiceCompat(
        packageManager: PackageManager,
        intent: Intent
    ): android.content.pm.ResolveInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveService(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveService(intent, 0)
            }
        }.getOrNull()
    }
}
