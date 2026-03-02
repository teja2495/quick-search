package com.tk.quicksearch.settings.appShortcuts

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.data.shortcutDisplayName
import java.util.Locale

data class AppShortcutSource(
    val packageName: String,
    val className: String,
    val label: String,
    val launchIntent: Intent,
    val icon: ImageBitmap?,
    val sourceType: AppShortcutSourceType = AppShortcutSourceType.SHORTCUT_PROVIDER,
)

enum class AppShortcutSourceType {
    SHORTCUT_PROVIDER,
    APP_ACTIVITY,
}

data class AppActivitySource(
    val packageName: String,
    val className: String,
    val label: String,
    val details: String,
    val icon: ImageBitmap?,
)

fun queryAppShortcutSources(
    packageManager: PackageManager,
    repositoryApps: List<AppInfo> = emptyList(),
): List<AppShortcutSource> {
    val providerSources =
        queryShortcutSourceActivities(packageManager).map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val className = resolveInfo.activityInfo.name
            val intent = Intent(Intent.ACTION_CREATE_SHORTCUT).setClassName(packageName, className)
            val label = resolveInfo.resolveLabel(packageManager)
            val icon =
                runCatching { resolveInfo.loadIcon(packageManager) }
                    .getOrNull()
                    ?.toBitmap(width = 96, height = 96)
                    ?.asImageBitmap()
            AppShortcutSource(
                packageName = packageName,
                className = className,
                label = label,
                launchIntent = intent,
                icon = icon,
                sourceType = AppShortcutSourceType.SHORTCUT_PROVIDER,
            )
        }

    val appActivityPackages =
        if (repositoryApps.isNotEmpty()) {
            repositoryApps
                .map { it.packageName to it.appName }
                .distinctBy { it.first }
        } else {
            queryLaunchableApps(packageManager).map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                packageName to resolveInfo.resolveLabel(packageManager)
            }
        }
    val appActivitySources =
        appActivityPackages.map { (packageName, _) ->
            val appIcon =
                runCatching {
                    packageManager.getApplicationIcon(packageName)
                }.getOrNull()
                    ?.toBitmap(width = 96, height = 96)
                    ?.asImageBitmap()
            AppShortcutSource(
                packageName = packageName,
                className = APP_ACTIVITY_SOURCE_CLASS_NAME,
                label = APP_ACTIVITY_SOURCE_LABEL,
                launchIntent =
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        `package` = packageName
                    },
                icon = appIcon,
                sourceType = AppShortcutSourceType.APP_ACTIVITY,
            )
        }

    return (providerSources + appActivitySources).distinctBy {
        "${it.packageName}:${it.className}:${it.sourceType.name}"
    }
}

fun queryAppActivitiesForPackage(
    packageManager: PackageManager,
    packageName: String,
): List<AppActivitySource> {
    val packageInfo =
        runCatching { getPackageInfoCompat(packageManager, packageName) }
            .getOrNull()
            ?: return emptyList()
    val appIcon =
        runCatching { packageManager.getApplicationIcon(packageName) }
            .getOrNull()
            ?.toBitmap(width = 96, height = 96)
            ?.asImageBitmap()

    val locale = Locale.getDefault()
    return packageInfo.activities
        .orEmpty()
        .asSequence()
        .filter { activityInfo -> isLaunchableActivityCandidate(activityInfo) }
        .map { activityInfo ->
            val resolvedClassName = resolveClassName(packageName, activityInfo.name)
            val label =
                activityInfo
                    .loadLabel(packageManager)
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: resolvedClassName.substringAfterLast(".")
            AppActivitySource(
                packageName = packageName,
                className = resolvedClassName,
                label = label,
                details = resolvedClassName.substringAfterLast("."),
                icon = appIcon,
            )
        }
        .distinctBy { activity -> "${activity.packageName}:${activity.className}" }
        .sortedBy { it.label.lowercase(locale) }
        .toList()
}

private fun getPackageInfoCompat(
    packageManager: PackageManager,
    packageName: String,
): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
    }

private fun isLaunchableActivityCandidate(activityInfo: android.content.pm.ActivityInfo): Boolean {
    if (!activityInfo.exported || !activityInfo.enabled) return false
    // Permission-gated activities are often not directly launchable by other apps.
    if (!activityInfo.permission.isNullOrBlank()) return false
    return true
}

private fun resolveClassName(
    packageName: String,
    className: String,
): String =
    when {
        className.startsWith(".") -> "$packageName$className"
        '.' !in className -> "$packageName.$className"
        else -> className
    }

private fun queryLaunchableApps(packageManager: PackageManager): List<ResolveInfo> {
    val intent =
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
    val results =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
    return results
        .filter { it.activityInfo?.exported == true }
        .distinctBy { it.activityInfo?.packageName }
        .sortedBy { info -> info.resolveLabel(packageManager).lowercase() }
}


private const val APP_ACTIVITY_SOURCE_LABEL = "App Activity"
private const val APP_ACTIVITY_SOURCE_CLASS_NAME = "__app_activity__"

fun isAppActivitySource(source: AppShortcutSource): Boolean =
    source.sourceType == AppShortcutSourceType.APP_ACTIVITY

fun filterAppShortcutSources(
    sources: List<AppShortcutSource>,
    existingShortcuts: List<StaticShortcut>,
    currentPackageName: String,
): List<AppShortcutSource> {
    val locale = Locale.getDefault()
    val existingShortcutNamesByPackage =
        existingShortcuts
            .groupBy { it.packageName }
            .mapValues { (_, shortcuts) ->
                shortcuts
                    .map { shortcutDisplayName(it).normalizedForCompare(locale) }
                    .toSet()
            }

    return sources
        .asSequence()
        .filterNot { it.packageName == currentPackageName }
        .distinctBy { "${it.packageName}:${it.className}:${it.sourceType.name}" }
        .filterNot { source ->
            if (source.sourceType == AppShortcutSourceType.APP_ACTIVITY) return@filterNot false
            existingShortcutNamesByPackage[source.packageName]
                ?.contains(source.label.normalizedForCompare(locale))
                ?: false
        }.sortedWith(
            compareBy<AppShortcutSource> { it.packageName.lowercase(locale) }
                .thenBy { if (it.sourceType == AppShortcutSourceType.APP_ACTIVITY) 1 else 0 }
                .thenBy { it.label.lowercase(locale) }
                .thenBy { it.packageName }
                .thenBy { it.className },
        ).toList()
}

private fun queryShortcutSourceActivities(packageManager: PackageManager): List<ResolveInfo> {
    val intent = Intent(Intent.ACTION_CREATE_SHORTCUT)
    val results =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

    return results
        .filter { it.activityInfo?.exported == true }
        .sortedBy { info -> info.resolveLabel(packageManager).lowercase() }
}

private fun ResolveInfo.resolveLabel(packageManager: PackageManager): String {
    val packageName = activityInfo.packageName
    return (
        loadLabel(packageManager)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: formatPackageNameAsLabel(packageName)
    )
}

private fun formatPackageNameAsLabel(packageName: String): String =
    packageName
        .substringAfterLast(".")
        .replaceFirstChar { it.titlecase() }

private fun String.normalizedForCompare(locale: Locale): String = trim().lowercase(locale)
