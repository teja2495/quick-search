package com.tk.quicksearch.search.apps.appLock

import com.tk.quicksearch.shared.ui.components.AppMessages
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.tk.quicksearch.R
import java.lang.ref.WeakReference

/**
 * Single entry point for anything that opens, inspects, or uninstalls a locked app. Every such path
 * goes through [runAfterUnlock] so a locked app can't be reached from a surface the lock missed.
 */
object AppLockGate {
    @Volatile
    private var resumedActivity: WeakReference<FragmentActivity>? = null

    @Volatile
    private var trackingActivities = false

    /**
     * Remembers the resumed activity so code holding only the application context (the ViewModel's
     * navigation) can still show a prompt. Safe to call from every activity's onCreate.
     */
    fun trackActivities(application: Application) {
        if (trackingActivities) return
        trackingActivities = true
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    if (activity is FragmentActivity) resumedActivity = WeakReference(activity)
                }

                override fun onActivityPaused(activity: Activity) {
                    if (resumedActivity?.get() === activity) resumedActivity = null
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    fun isProtected(context: Context, packageName: String): Boolean =
        AppLock.isLocked(context, packageName)

    /** Runs [action] right away for unlocked apps, otherwise only after biometric authentication. */
    fun runAfterUnlock(
        context: Context,
        packageName: String,
        appName: String? = null,
        onCancelled: () -> Unit = {},
        action: () -> Unit,
    ) {
        if (!isProtected(context, packageName)) {
            action()
            return
        }
        authenticate(
            context,
            context.getString(R.string.app_lock_prompt_unlock, appName?.takeIf { it.isNotBlank() } ?: appLabel(context, packageName)),
            BiometricManager.Authenticators.BIOMETRIC_WEAK,
            onCancelled,
        ) {
            // Already unlocked here, so Extend App Lock shouldn't ask again as the app opens.
            AppLockLaunchWatcher.allowLaunchFromQuickSearch(packageName)
            action()
        }
    }

    /** Like [runAfterUnlock] for actions that may open any of [packageNames]; one prompt covers them. */
    fun runAfterUnlockAny(
        context: Context,
        packageNames: Collection<String>,
        onCancelled: () -> Unit = {},
        action: () -> Unit,
    ) {
        val lockedPackage = packageNames.firstOrNull { isProtected(context, it) }
        if (lockedPackage == null) {
            action()
        } else {
            runAfterUnlock(context, lockedPackage, onCancelled = onCancelled, action = action)
        }
    }

    /**
     * Shows a biometric prompt on the activity behind [context]. A fresh prompt is built for every
     * request because BiometricPrompt keeps one callback per activity, so a prompt remembered from
     * earlier would deliver its success to whichever caller registered last.
     */
    fun authenticate(
        context: Context,
        promptTitle: String,
        allowedAuthenticators: Int,
        onCancelled: () -> Unit = {},
        onAuthenticated: () -> Unit,
    ) {
        val activity =
            context.findFragmentActivity()
                ?: resumedActivity?.get()?.takeUnless { it.isFinishing || it.isDestroyed }
        if (activity == null) {
            // Fail closed: without an activity to host the prompt the locked app stays shut.
            AppMessages.show(context, R.string.app_lock_unavailable)
            onCancelled()
            return
        }
        // BiometricPrompt must be built and shown on the main thread.
        activity.runOnUiThread {
            val prompt =
                BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(activity),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            onAuthenticated()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            onCancelled()
                        }
                    },
                )
            val promptInfo =
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(promptTitle)
                    .setAllowedAuthenticators(allowedAuthenticators)
                    .apply {
                        if (allowedAuthenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL == 0) {
                            setNegativeButtonText(activity.getString(android.R.string.cancel))
                        }
                    }.build()
            prompt.authenticate(promptInfo)
        }
    }

    private fun appLabel(context: Context, packageName: String): String =
        runCatching {
            val packageManager = context.packageManager
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)

    private tailrec fun Context.findFragmentActivity(): FragmentActivity? =
        when (this) {
            is FragmentActivity -> this
            is ContextWrapper -> baseContext.findFragmentActivity()
            else -> null
        }
}
