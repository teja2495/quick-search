package com.tk.quicksearch.search.apps.speedBump

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.shared.util.lockPortraitOrientationOnPhones

/**
 * Shows the SpeedBump pause on top of a bumped app that was opened outside Quick Search.
 *
 * Runs in its own task above the app, so finishing it reveals the app again; "Don't Open" sends
 * the user home instead.
 */
class SpeedBumpActivity : ComponentActivity() {
    private var packageNameToOpen: String? = null
    private var resolved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortraitOrientationOnPhones()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        val packageName = intent.getStringExtra(ExtraPackageName)
        val appInfo = packageName?.let(::loadAppInfo)
        if (appInfo == null) {
            finish()
            return
        }
        packageNameToOpen = packageName
        val preferences = UserAppPreferences(this)
        setContent {
            QuickSearchTheme(
                fontScaleMultiplier = preferences.getFontScaleMultiplier(),
                appTheme = preferences.getAppTheme(),
                appThemeMode = preferences.getAppThemeMode(),
            ) {
                SpeedBumpOverlay(
                    appInfo = appInfo,
                    iconPackPackage = preferences.getSelectedIconPackPackage(),
                    appIconShape = preferences.getAppIconShape(),
                    onOpen = ::openApp,
                    onCancel = ::cancel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A different bumped app came forward while this pause was still up; restart for it.
        if (intent.getStringExtra(ExtraPackageName) != packageNameToOpen) {
            setIntent(intent)
            recreate()
        }
    }

    private fun openApp() {
        if (resolved) return
        resolved = true
        packageNameToOpen?.let(SpeedBumpLaunchWatcher::allow)
        finishWithoutAnimation()
    }

    private fun cancel() {
        if (resolved) return
        resolved = true
        // The watcher keeps ignoring the app until home or another app takes over, so the app's
        // window briefly showing again as this screen closes doesn't start a new pause.
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        finishWithoutAnimation()
    }

    private fun finishWithoutAnimation() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun loadAppInfo(packageName: String): AppInfo? =
        runCatching {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            AppInfo(
                appName = packageManager.getApplicationLabel(applicationInfo).toString(),
                packageName = packageName,
                lastUsedTime = 0L,
                totalTimeInForeground = 0L,
                firstInstallTime = packageInfo.firstInstallTime,
                isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            )
        }.getOrElse { error ->
            if (error !is PackageManager.NameNotFoundException) throw error
            null
        }

    companion object {
        private const val ExtraPackageName = "speed_bump_package_name"

        fun start(context: Context, packageName: String) {
            runCatching {
                context.startActivity(
                    Intent(context, SpeedBumpActivity::class.java)
                        .putExtra(ExtraPackageName, packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                )
            }.onFailure { SpeedBumpLaunchWatcher.onPauseFailed(packageName) }
        }
    }
}
