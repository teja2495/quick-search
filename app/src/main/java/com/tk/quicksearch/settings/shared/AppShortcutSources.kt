package com.tk.quicksearch.settings.shared

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.shortcutDisplayName
import java.util.Locale

data class AppShortcutSource(
    val packageName: String,
    val className: String,
    val label: String,
    val launchIntent: Intent,
    val icon: ImageBitmap?,
)

fun queryAppShortcutSources(packageManager: PackageManager): List<AppShortcutSource> =
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
        )
    }

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
        .distinctBy { "${it.packageName}:${it.className}" }
        .filterNot { source ->
            existingShortcutNamesByPackage[source.packageName]
                ?.contains(source.label.normalizedForCompare(locale))
                ?: false
        }.sortedWith(
            compareBy<AppShortcutSource> { it.label.lowercase(locale) }
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
