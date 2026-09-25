package com.tk.quicksearch.search.data.appShortcutRepository

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.util.Log
import java.util.Locale

/**
 * Loads launchable static (manifest) shortcuts using the official [LauncherApps] API.
 *
 * This is the "official" replacement for the XML-parsing approach in [loadShortcutsFromSystem].
 * It requires the app to be granted launcher/home role (i.e. registered with
 * `android.intent.category.HOME` and selected as the default home/launcher). When the role
 * is missing, [LauncherApps.getShortcuts] returns null/empty and we fall back to nothing here;
 * the caller is responsible for choosing between the legacy and official loaders via a flag.
 *
 * Behavior parity goal: produce the same set of shortcuts (manifest/static shortcuts) as the
 * legacy XML parser, formatted as [StaticShortcut] so the rest of the pipeline is unchanged.
 *
 * Dynamic/pinned shortcuts can be enabled by flipping [INCLUDE_DYNAMIC_SHORTCUTS] /
 * [INCLUDE_PINNED_SHORTCUTS] below if we want to opt-in later.
 */

private const val TAG = "LauncherAppsShortcuts"

// Manifest shortcuts == the static shortcuts the legacy XML parser used to find.
// Dynamic shortcuts == shortcuts apps publish at runtime (most modern apps use these).
// Pinned shortcuts == ones the user pinned previously through some launcher.
private const val INCLUDE_DYNAMIC_SHORTCUTS = true
private const val INCLUDE_PINNED_SHORTCUTS = true

/**
 * Sentinel intent action used for shortcuts fetched via [LauncherApps]. Android does not expose
 * a shortcut's real [Intent] to anyone other than the publishing app, so we cannot launch these
 * via [android.content.Context.startActivity]. Instead we stash the identifying info on a
 * sentinel intent and route it through [LauncherApps.startShortcut] at launch time.
 *
 * The launcher action handler lives in `ShortcutUtils.launchStaticShortcut`.
 */
const val ACTION_LAUNCHER_APPS_SHORTCUT = "com.tk.quicksearch.action.LAUNCHER_APPS_SHORTCUT"
const val EXTRA_LAUNCHER_APPS_SHORTCUT_PACKAGE = "com.tk.quicksearch.extra.LA_SHORTCUT_PACKAGE"
const val EXTRA_LAUNCHER_APPS_SHORTCUT_ID = "com.tk.quicksearch.extra.LA_SHORTCUT_ID"
const val EXTRA_LAUNCHER_APPS_SHORTCUT_USER_SERIAL = "com.tk.quicksearch.extra.LA_SHORTCUT_USER_SERIAL"

fun loadShortcutsViaLauncherApps(
    context: Context,
    packageManager: PackageManager,
): List<StaticShortcut> {
    val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: run {
                Log.w(TAG, "LauncherApps service unavailable")
                return emptyList()
            }

    val hasPermission = launcherApps.hasShortcutHostPermission()
    Log.d(TAG, "hasShortcutHostPermission=$hasPermission")
    if (!hasPermission) {
        // Not the default launcher / home app - LauncherApps.getShortcuts would return null.
        return emptyList()
    }

    val profiles: List<UserHandle> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            kotlin.runCatching { launcherApps.profiles }.getOrNull()?.takeIf { it.isNotEmpty() }
                ?: listOf(Process.myUserHandle())
        } else {
            listOf(Process.myUserHandle())
        }

    var queryFlags = LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
    if (INCLUDE_DYNAMIC_SHORTCUTS) queryFlags = queryFlags or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
    if (INCLUDE_PINNED_SHORTCUTS) queryFlags = queryFlags or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED

    val query =
        LauncherApps.ShortcutQuery().apply {
            setQueryFlags(queryFlags)
        }

    val labelCache = mutableMapOf<String, String>()
    val results = mutableListOf<StaticShortcut>()
    val convertFailReasons = mutableMapOf<String, Int>()
    var rawTotal = 0
    var droppedDisabled = 0
    var droppedConversion = 0

    for (userHandle in profiles) {
        val fetchResult =
            kotlin.runCatching { launcherApps.getShortcuts(query, userHandle) }
        val error = fetchResult.exceptionOrNull()
        if (error != null) {
            Log.w(TAG, "getShortcuts() failed for user=$userHandle: ${error.message}")
        }
        val infos: List<ShortcutInfo> = fetchResult.getOrNull().orEmpty()
        rawTotal += infos.size
        Log.d(TAG, "user=$userHandle raw shortcut count=${infos.size}")
        for (info in infos) {
            if (!info.isEnabled) {
                droppedDisabled++
                continue
            }
            val failureReason = StringBuilder()
            val converted =
                convertShortcutInfo(
                    context = context,
                    packageManager = packageManager,
                    launcherApps = launcherApps,
                    info = info,
                    labelCache = labelCache,
                    failureReason = failureReason,
                )
            if (converted == null) {
                droppedConversion++
                val reason = failureReason.toString().ifBlank { "unknown" }
                convertFailReasons[reason] = (convertFailReasons[reason] ?: 0) + 1
                continue
            }
            results.add(converted)
        }
    }

    val beforeFilter = results.size
    val filtered = filterShortcuts(results, packageManager, context)
    Log.d(
        TAG,
        "summary: raw=$rawTotal disabled=$droppedDisabled convertFail=$droppedConversion " +
            "converted=$beforeFilter postFilter=${filtered.size} queryFlags=0x${queryFlags.toString(16)}",
    )
    if (convertFailReasons.isNotEmpty()) {
        Log.d(TAG, "convertFail breakdown: $convertFailReasons")
    }
    val locale = Locale.getDefault()
    return filtered.sortedWith(
        compareBy<StaticShortcut> { it.appLabel.lowercase(locale) }
            .thenBy { shortcutDisplayName(it).lowercase(locale) }
            .thenBy { it.id },
    )
}

private fun convertShortcutInfo(
    context: Context,
    packageManager: PackageManager,
    launcherApps: LauncherApps,
    info: ShortcutInfo,
    labelCache: MutableMap<String, String>,
    failureReason: StringBuilder = StringBuilder(),
): StaticShortcut? {
    val packageName = info.`package`
    val id = info.id?.trim().orEmpty()
    if (packageName.isBlank() || id.isBlank()) {
        failureReason.append("blank_pkg_or_id")
        return null
    }
    // Note: we deliberately skip the legacy `isValidShortcutId` check here because real
    // LauncherApps shortcut IDs can be purely numeric (e.g. chat row IDs) or contain `@`
    // (e.g. WhatsApp JIDs). The legacy check was tailored to XML-parsed IDs.
    if (id.startsWith("@")) {
        failureReason.append("id_starts_with_at")
        return null
    }

    val appLabel =
        labelCache.getOrPut(packageName) {
            resolveAppLabel(context, packageName, packageManager)
        }

    val shortLabel = info.shortLabel?.toString()?.takeIf { it.isNotBlank() }
    val longLabel = info.longLabel?.toString()?.takeIf { it.isNotBlank() }

    // Always route through LauncherApps.startShortcut, regardless of whether the
    // publisher-visible intent happens to be available. This is the only reliable launch
    // path for shortcuts whose target activity is non-exported (most dynamic shortcuts).
    val userSerial =
        kotlin.runCatching {
            val um = context.getSystemService(Context.USER_SERVICE) as android.os.UserManager
            um.getSerialNumberForUser(info.userHandle)
        }.getOrDefault(-1L)

    val sentinelIntent =
        Intent(ACTION_LAUNCHER_APPS_SHORTCUT).apply {
            `package` = context.packageName
            putExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_PACKAGE, packageName)
            putExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_ID, id)
            putExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_USER_SERIAL, userSerial)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    return StaticShortcut(
        packageName = packageName,
        appLabel = appLabel,
        id = id,
        shortLabel = shortLabel,
        longLabel = longLabel,
        iconResId = null,
        iconBase64 = null,
        enabled = info.isEnabled,
        intents = listOf(sentinelIntent),
    )
}
