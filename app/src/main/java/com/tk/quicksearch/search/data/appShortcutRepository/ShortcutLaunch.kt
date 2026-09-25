package com.tk.quicksearch.search.data.AppShortcutRepository

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.UserManager
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.searchEngines.SearchTargetQueryShortcutActivity
import com.tk.quicksearch.tools.tasker.TaskerIntegration

private const val TASKER_INTENT_HANDLER_CLASS = "net.dinglisch.android.taskerm.IntentHandler"

fun launchStaticShortcut(
    context: Context,
    shortcut: StaticShortcut,
    skipSearchTargetQueryHistory: Boolean = false,
): String? {
    if (!shortcut.enabled) {
        return context.getString(R.string.error_shortcut_disabled)
    }

    // LauncherApps-fetched shortcuts are launched via the system shortcut service,
    // not through Intent dispatch (the publisher-visible intent isn't exposed to us).
    shortcut.intents.firstOrNull()?.let { firstIntent ->
        if (firstIntent.action == ACTION_LAUNCHER_APPS_SHORTCUT) {
            return launchLauncherAppsShortcut(context, shortcut, firstIntent)
        }
    }

    val pm = context.packageManager
    val intents = shortcut.intents.asReversed()
    var lastErrorMessage: String? = null
    var noActivityIntentDetails: String = ""
    intents.forEach { baseIntent ->
        val intent =
            Intent(baseIntent).apply {
                putExtra(Intent.EXTRA_SHORTCUT_ID, shortcut.id)
                if (
                    skipSearchTargetQueryHistory &&
                    action == SearchTargetQueryShortcutActivity.ACTION_LAUNCH_SEARCH_TARGET_QUERY_SHORTCUT
                ) {
                    putExtra(SearchTargetQueryShortcutActivity.EXTRA_SKIP_QUERY_HISTORY, true)
                }
            }
        val isTaskerTaskShortcut =
            shortcut.packageName == TaskerIntegration.PACKAGE_NAME &&
                intent.`package` == TaskerIntegration.PACKAGE_NAME &&
                intent.action?.startsWith("net.dinglisch.android.tasker.") == true
        val details = formatIntentDetails(intent)
        val resolved = pm.resolveActivity(intent, 0)
        if (resolved == null) {
            if (isTaskerTaskShortcut && launchTaskerShortcut(context, intent)) {
                return null
            }
            // Activities that can't be resolved via PackageManager (e.g., Knox-protected
            // activities) may still be launchable directly. Try if this is an explicit
            // component intent from a custom deep link (parsed from an intent URI).
            val isExplicitComponentDeepLink =
                shortcut.id.startsWith("custom_deeplink_") && intent.component != null
            if (isExplicitComponentDeepLink) {
                val error = runCatching { context.startActivity(intent) }.exceptionOrNull()
                if (error == null) return null
            }
            noActivityIntentDetails = details
            return@forEach
        }

        val activityInfo = resolved.activityInfo
        if (!activityInfo.exported) {
            lastErrorMessage =
                context.getString(R.string.error_shortcut_activity_not_exported) + details.toSuffixDetail()
            return@forEach
        }

        val requiredPermission = activityInfo.permission?.takeIf { it.isNotBlank() }
        if (requiredPermission != null &&
            context.checkSelfPermission(requiredPermission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            lastErrorMessage =
                context.getString(R.string.error_shortcut_requires_permission, requiredPermission) +
                    details.toSuffixDetail()
            return@forEach
        }

        val error = kotlin.runCatching { context.startActivity(intent) }.exceptionOrNull()
        if (error == null) {
            return null
        }
        lastErrorMessage =
            context.getString(
                R.string.error_shortcut_launch_failed,
                error.message ?: context.getString(R.string.error_unknown),
            ) + details.toSuffixDetail()
    }

    if (lastErrorMessage != null) {
        return lastErrorMessage
    }

    // For custom deep-link shortcuts, fall back to a generic browser VIEW intent
    // when none of the persisted intents resolve on this device.
    if (shortcut.id.startsWith("custom_deeplink_")) {
        val deepLink =
            shortcut.intents
                .asSequence()
                .mapNotNull { it.dataString?.trim() }
                .firstOrNull { it.isNotBlank() }
        if (!deepLink.isNullOrBlank()) {
            val browserFallbackIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            val fallbackError =
                kotlin.runCatching { context.startActivity(browserFallbackIntent) }.exceptionOrNull()
            if (fallbackError == null) {
                return null
            }
        }
    }

    return context.getString(R.string.error_shortcut_no_activity_resolves) + noActivityIntentDetails.toSuffixDetail()
}

private fun launchTaskerShortcut(
    context: Context,
    intent: Intent,
): Boolean =
    runCatching {
        context.startActivity(
            Intent(intent).apply {
                component =
                    android.content.ComponentName(
                        TaskerIntegration.PACKAGE_NAME,
                        TASKER_INTENT_HANDLER_CLASS,
                    )
            },
        )
    }.isSuccess

private fun launchLauncherAppsShortcut(
    context: Context,
    shortcut: StaticShortcut,
    sentinel: Intent,
): String? {
    val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return context.getString(R.string.error_unknown)
    if (!launcherApps.hasShortcutHostPermission()) {
        return context.getString(R.string.error_unknown)
    }
    val targetPackage =
        sentinel.getStringExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_PACKAGE)?.takeIf { it.isNotBlank() }
            ?: shortcut.packageName
    val shortcutId =
        sentinel.getStringExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_ID)?.takeIf { it.isNotBlank() }
            ?: shortcut.id
    val userSerial = sentinel.getLongExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_USER_SERIAL, -1L)
    val userHandle =
        kotlin.runCatching {
            val um = context.getSystemService(Context.USER_SERVICE) as UserManager
            if (userSerial >= 0L) um.getUserForSerialNumber(userSerial) else android.os.Process.myUserHandle()
        }.getOrNull() ?: android.os.Process.myUserHandle()

    val error =
        kotlin.runCatching {
            launcherApps.startShortcut(targetPackage, shortcutId, null, null, userHandle)
        }.exceptionOrNull()
    return if (error == null) {
        null
    } else {
        context.getString(
            R.string.error_shortcut_launch_failed,
            error.message ?: context.getString(R.string.error_unknown),
        )
    }
}

private fun formatIntentDetails(intent: Intent): String {
    val parts = mutableListOf<String>()
    intent.action?.takeIf { it.isNotBlank() }?.let { parts.add("action=$it") }
    intent.component
        ?.className
        ?.takeIf { it.isNotBlank() }
        ?.let { parts.add("component=$it") }
    intent.`package`?.takeIf { it.isNotBlank() }?.let { parts.add("package=$it") }
    intent.dataString?.takeIf { it.isNotBlank() }?.let { parts.add("data=$it") }
    if (parts.isEmpty()) return ""
    return "(${parts.joinToString(", ")})"
}

private fun String.toSuffixDetail(): String = if (isBlank()) "" else " $this"
