package com.tk.quicksearch.search.apps.appLock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppButtonSize
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.shared.util.lockPortraitOrientationOnPhones

/**
 * Covers a locked app that was opened outside Quick Search until the user unlocks it.
 *
 * Runs in its own task above the app, so finishing it reveals the app again; backing out or
 * leaving without unlocking sends the user home instead.
 */
class AppLockActivity : FragmentActivity() {
    private var packageNameToUnlock: String? = null
    private var appName: String = ""
    private var resolved = false
    private var promptShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortraitOrientationOnPhones()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = cancel()
            },
        )
        if (!bind(intent)) return
        val preferences = UserAppPreferences(this)
        setContent {
            QuickSearchTheme(
                fontScaleMultiplier = preferences.getFontScaleMultiplier(),
                appTheme = preferences.getAppTheme(),
                appThemeMode = preferences.getAppThemeMode(),
            ) {
                AppLockScreen(
                    packageName = packageNameToUnlock.orEmpty(),
                    appName = appName,
                    iconPackPackage = preferences.getSelectedIconPackPackage(),
                    appIconShape = preferences.getAppIconShape(),
                    onUnlock = ::promptUnlock,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A different locked app came forward while this screen was still up; restart for it.
        if (intent.getStringExtra(ExtraPackageName) != packageNameToUnlock) {
            setIntent(intent)
            recreate()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!promptShown && !resolved) {
            promptShown = true
            promptUnlock()
        }
    }

    override fun onStop() {
        super.onStop()
        // Left without unlocking (home, recents): don't leave a stale lock screen behind.
        if (!resolved && !isChangingConfigurations) {
            resolved = true
            packageNameToUnlock?.let(AppLockLaunchWatcher::onUnlockFailed)
            finish()
        }
    }

    private fun bind(intent: Intent): Boolean {
        val packageName = intent.getStringExtra(ExtraPackageName)
        val label = packageName?.let(::loadAppLabel)
        if (packageName == null || label == null) {
            packageName?.let(AppLockLaunchWatcher::onUnlockFailed)
            resolved = true
            finish()
            return false
        }
        packageNameToUnlock = packageName
        appName = label
        return true
    }

    private fun promptUnlock() {
        val packageName = packageNameToUnlock ?: return
        if (resolved) return
        // Stays on this screen when the prompt is dismissed, so the user can try again or back out.
        AppLockGate.runAfterUnlock(this, packageName, appName) {
            if (resolved) return@runAfterUnlock
            resolved = true
            AppLockLaunchWatcher.allow(packageName)
            finishWithoutAnimation()
        }
    }

    private fun cancel() {
        if (resolved) return
        resolved = true
        packageNameToUnlock?.let(AppLockLaunchWatcher::onUnlockFailed)
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

    private fun loadAppLabel(packageName: String): String? =
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    companion object {
        private const val ExtraPackageName = "app_lock_package_name"

        fun start(context: Context, packageName: String) {
            runCatching {
                context.startActivity(
                    Intent(context, AppLockActivity::class.java)
                        .putExtra(ExtraPackageName, packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                )
            }.onFailure { AppLockLaunchWatcher.onUnlockFailed(packageName) }
        }
    }
}

private val LockIconSize = 96.dp

/** Opaque so the locked app's content stays hidden behind it until it is unlocked. */
@Composable
private fun AppLockScreen(
    packageName: String,
    appName: String,
    iconPackPackage: String?,
    appIconShape: AppIconShape,
    onUnlock: () -> Unit,
) {
    val iconResult =
        rememberAppIcon(
            packageName = packageName,
            iconPackPackage = iconPackPackage,
            forceCircularMask = appIconShape == AppIconShape.CIRCLE,
        )
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = DesignTokens.SpacingHuge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        iconResult.bitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(LockIconSize).clip(AppShapes.largeIncreased),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(modifier = Modifier.height(DesignTokens.SpacingXXLarge))
        Text(
            text = stringResource(R.string.app_lock_prompt_unlock, appName),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(DesignTokens.Spacing40))
        AppButton(
            onClick = onUnlock,
            size = AppButtonSize.Medium,
            icon = MaterialSymbols.Lock,
        ) {
            Text(stringResource(R.string.action_unlock_app))
        }
    }
}
