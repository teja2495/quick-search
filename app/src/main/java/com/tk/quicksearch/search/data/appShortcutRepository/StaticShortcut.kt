package com.tk.quicksearch.search.data.AppShortcutRepository

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.SearchTargetQueryShortcutActivity
import com.tk.quicksearch.searchEngines.getDisplayNameResId
import com.tk.quicksearch.searchEngines.getDrawableResId
import com.tk.quicksearch.searchEngines.getAppPackageCandidates
import com.tk.quicksearch.searchEngines.resolveSearchTargetShortcutPackageName
import androidx.appcompat.content.res.AppCompatResources
import android.content.Context
import java.io.ByteArrayOutputStream

private const val CUSTOM_SHORTCUT_ID_PREFIX = "custom_"

private data class HardcodedShortcutDefinition(
    val id: String,
    val packageName: String,
    val shortLabelResId: Int,
    val longLabelResId: Int = shortLabelResId,
    val targetClassName: String? = null,
    val shortcutSourceClassName: String? = targetClassName,
    val intentAction: String? = null,
    val intentDataUri: String? = null,
) {
    fun toStaticShortcut(
        context: Context,
        appLabel: String,
        iconBase64: String?,
    ): StaticShortcut {
        val intent =
            Intent().apply {
                intentAction?.let { action = it }
                intentDataUri?.let { data = Uri.parse(it) }
                if (!targetClassName.isNullOrBlank()) {
                    component = ComponentName(packageName, targetClassName)
                } else {
                    `package` = packageName
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        return StaticShortcut(
            packageName = packageName,
            appLabel = appLabel,
            id = id,
            shortLabel = context.getString(shortLabelResId),
            longLabel = context.getString(longLabelResId),
            iconResId = null,
            iconBase64 = iconBase64,
            enabled = true,
            intents = listOf(intent),
        )
    }
}

private val HARDCODED_SHORTCUT_DEFINITIONS =
    listOf(
        HardcodedShortcutDefinition(
            id = "song_search",
            packageName = "com.google.android.googlequicksearchbox",
            shortLabelResId = R.string.shortcut_song_search_label,
            targetClassName = "com.google.android.apps.search.soundsearch.shortcut.AliasAddShortcutActivity",
        ),
        HardcodedShortcutDefinition(
            id = "watch_later",
            packageName = "com.google.android.youtube",
            shortLabelResId = R.string.shortcut_watch_later_label,
            intentAction = Intent.ACTION_VIEW,
            intentDataUri = "https://www.youtube.com/playlist?list=WL",
        ),
    )
internal val HARDCODED_SHORTCUT_KEYS =
    HARDCODED_SHORTCUT_DEFINITIONS
        .asSequence()
        .map { "${it.packageName}:${it.id}" }
        .toSet()

data class StaticShortcut(
    val packageName: String,
    val appLabel: String,
    val id: String,
    val shortLabel: String?,
    val longLabel: String?,
    val iconResId: Int?,
    val iconBase64: String? = null,
    val enabled: Boolean,
    val intents: List<Intent>,
)

// Utility functions that depend on these models
internal fun shortcutDisplayName(shortcut: StaticShortcut): String =
    shortcut.shortLabel?.takeIf { it.isNotBlank() }
        ?: shortcut.longLabel?.takeIf { it.isNotBlank() } ?: shortcut.id

internal fun shortcutKey(shortcut: StaticShortcut): String = "${shortcut.packageName}:${shortcut.id}"

internal fun isUserCreatedShortcut(shortcut: StaticShortcut): Boolean =
    shortcut.id.startsWith(CUSTOM_SHORTCUT_ID_PREFIX)

internal fun isValidShortcutId(id: String): Boolean {
    val trimmed = id.trim()
    if (trimmed.isBlank()) return false
    if (trimmed.startsWith("@")) return false
    if (trimmed.all { it.isDigit() }) return false
    return true
}

internal fun loadHardcodedShortcuts(
    existingStaticShortcuts: List<StaticShortcut>,
    context: Context,
    packageManager: PackageManager,
): List<StaticShortcut> {
    val locale = java.util.Locale.getDefault()
    val existingByPackageAndLabel =
        existingStaticShortcuts
            .asSequence()
            .map { shortcut ->
                shortcut.packageName to shortcutDisplayName(shortcut).trim().lowercase(locale)
            }.toSet()
    val sourceIconByComponent = loadShortcutSourceIconBase64ByComponent(context, packageManager)

    return HARDCODED_SHORTCUT_DEFINITIONS
        .asSequence()
        .map { definition ->
            definition.copy(
                targetClassName =
                    definition.targetClassName?.let { targetClass ->
                        resolveClassName(
                            definition.packageName,
                            targetClass,
                        )
                    },
                shortcutSourceClassName =
                    definition.shortcutSourceClassName?.let { sourceClass ->
                        resolveClassName(
                            definition.packageName,
                            sourceClass,
                        )
                    },
            )
        }.filter { definition ->
            isAppInstalled(definition.packageName, packageManager)
        }.map { definition ->
            val sourceComponentKey =
                definition.shortcutSourceClassName?.let { sourceClassName ->
                    "${definition.packageName}/$sourceClassName"
                }
            val iconBase64 =
                sourceComponentKey?.let { sourceIconByComponent[it] }
                    ?: loadAppIconBase64(context, definition.packageName)
            definition.toStaticShortcut(
                context = context,
                appLabel = resolveAppLabel(context, definition.packageName, packageManager),
                iconBase64 = iconBase64,
            )
        }.filterNot { shortcut ->
            (shortcut.packageName to shortcutDisplayName(shortcut).trim().lowercase(locale)) in
                existingByPackageAndLabel
        }.toList()
}

private fun isAppInstalled(packageName: String, packageManager: PackageManager): Boolean =
    kotlin.runCatching { packageManager.getApplicationInfo(packageName, 0) }.isSuccess

private fun loadShortcutSourceIconBase64ByComponent(
    context: Context,
    packageManager: PackageManager,
): Map<String, String> {
    val resolveInfos =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                Intent(Intent.ACTION_CREATE_SHORTCUT),
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(
                Intent(Intent.ACTION_CREATE_SHORTCUT),
                PackageManager.MATCH_DEFAULT_ONLY,
            )
        }

    return resolveInfos
        .asSequence()
        .mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            if (!activityInfo.exported) return@mapNotNull null
            val packageName = activityInfo.packageName
            val className = resolveClassName(packageName, activityInfo.name)
            val iconBase64 =
                resolveShortcutSourceIconDrawable(resolveInfo, packageManager)
                    ?.toBitmap(width = 96, height = 96)
                    ?.let(::bitmapToBase64Png)
                    ?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
            "${packageName}/${className}" to iconBase64
        }.toMap()
}

private fun resolveShortcutSourceIconDrawable(resolveInfo: android.content.pm.ResolveInfo, packageManager: PackageManager): android.graphics.drawable.Drawable? =
    kotlin.runCatching { resolveInfo.loadIcon(packageManager) }.getOrNull()

internal fun loadAppIconBase64(context: Context, packageName: String): String? =
    kotlin.runCatching { context.packageManager.getApplicationIcon(packageName) }
        .getOrNull()
        ?.toBitmap(width = 96, height = 96)
        ?.let(::bitmapToBase64Png)

private fun bitmapToBase64Png(bitmap: Bitmap): String? =
    kotlin.runCatching {
        ByteArrayOutputStream().use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) return null
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
    }.getOrNull()

internal fun resolveAppLabel(context: Context, packageName: String, packageManager: PackageManager): String {
    val appInfo = kotlin.runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
    val localizedLabel =
        appInfo?.let {
            kotlin.runCatching { packageManager.getApplicationLabel(it)?.toString() }.getOrNull()
        }
    return localizedLabel?.takeIf { it.isNotBlank() }
        ?: appInfo?.nonLocalizedLabel?.toString()?.takeIf { it.isNotBlank() }
        ?: formatPackageNameAsLabel(packageName)
}

private fun formatPackageNameAsLabel(packageName: String): String =
    packageName
        .substringAfterLast(".")
        .replaceFirstChar { it.titlecase(java.util.Locale.getDefault()) }

internal fun resolveClassName(
    targetPackage: String,
    targetClass: String,
): String =
    when {
        targetClass.startsWith(".") -> targetPackage + targetClass
        targetClass.contains(".") -> targetClass
        else -> "$targetPackage.$targetClass"
    }

internal fun createSearchTargetShortcutIntent(
    context: Context,
    target: SearchTarget,
    query: String,
): Intent? =
    when (target) {
        is SearchTarget.Engine -> {
            SearchTargetQueryShortcutActivity.createIntent(
                context = context,
                targetType = SearchTargetQueryShortcutActivity.TARGET_TYPE_ENGINE,
                query = query,
                engineName = target.engine.name,
            )
        }

        is SearchTarget.Browser -> {
            SearchTargetQueryShortcutActivity.createIntent(
                context = context,
                targetType = SearchTargetQueryShortcutActivity.TARGET_TYPE_BROWSER,
                query = query,
                browserPackage = target.app.packageName,
            )
        }

        is SearchTarget.Custom -> {
            SearchTargetQueryShortcutActivity.createIntent(
                context = context,
                targetType = SearchTargetQueryShortcutActivity.TARGET_TYPE_CUSTOM,
                query = query,
                customUrlTemplate = target.custom.urlTemplate,
            )
        }
    }

internal fun resolveSearchTargetLabel(context: Context, target: SearchTarget): String =
    when (target) {
        is SearchTarget.Engine -> context.getString(target.engine.getDisplayNameResId())
        is SearchTarget.Browser -> target.app.label
        is SearchTarget.Custom -> target.custom.name
    }

internal fun resolveSearchTargetIconBase64(context: Context, target: SearchTarget): String? =
    when (target) {
        is SearchTarget.Engine -> {
            val drawable =
                AppCompatResources.getDrawable(context, target.engine.getDrawableResId())
                    ?: return null
            bitmapToBase64Png(drawable.toBitmap(width = 96, height = 96))
        }

        is SearchTarget.Browser -> {
            loadAppIconBase64(context, target.app.packageName)
        }

        is SearchTarget.Custom -> {
            target.custom.faviconBase64
        }
    }

internal fun resolveSearchTargetShortcutPackageName(
    context: Context,
    target: SearchTarget,
    packageManager: PackageManager,
): String {
    return when (target) {
        is SearchTarget.Engine -> {
            val candidates = target.engine.getAppPackageCandidates()
            candidates.firstOrNull { packageName ->
                packageManager.getLaunchIntentForPackage(packageName) != null
            }
                ?: candidates.firstOrNull()
                ?: com.tk.quicksearch.searchEngines.resolveSearchTargetShortcutPackageName(target)
        }
        is SearchTarget.Browser -> target.app.packageName
        is SearchTarget.Custom ->
            com.tk.quicksearch.searchEngines.resolveSearchTargetShortcutPackageName(target)
    }
}