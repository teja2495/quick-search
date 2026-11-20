package com.tk.quicksearch.search.data

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/**
 * Apps whose OEM build disables their `CATEGORY_LAUNCHER` activity while the OEM launcher still
 * lists them through the package's `CATEGORY_INFO` front door. One UI does this for Link to
 * Windows: `StartUpCoreActivity` is disabled and only `StartUpActivity` (MAIN + INFO) remains.
 *
 * This is an allowlist on purpose: many packages expose an INFO activity without a launcher icon
 * (Traceur, device-policy stubs, TTS voices), and launchers don't show those.
 */
internal object InfoEntryApps {
    private val PACKAGES = setOf("com.microsoft.appmanager")

    fun query(
        packageManager: PackageManager,
        alreadyLaunchable: Set<String>,
    ): List<ResolveInfo> =
        PACKAGES.filter { it !in alreadyLaunchable }.mapNotNull { packageName ->
            val intent =
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_INFO)
                    .setPackage(packageName)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.queryIntentActivities(intent, 0)
                }
            }.getOrNull()
                ?.firstOrNull { it.activityInfo?.exported == true && it.activityInfo.applicationInfo.enabled }
        }
}
