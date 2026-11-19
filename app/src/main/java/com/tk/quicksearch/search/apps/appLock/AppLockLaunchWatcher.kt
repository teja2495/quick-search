package com.tk.quicksearch.search.apps.appLock

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.tk.quicksearch.app.HomeActivity

/**
 * Asks for unlock when a locked app comes to the foreground from outside Quick Search (another
 * launcher, recents, a notification), mirroring SpeedBump's launch watcher.
 *
 * Fed window-state events by the accessibility service. An app passes once it has been unlocked
 * (here, or by Quick Search before launching it) and stays allowed until another app or a home
 * screen takes the foreground. System UI, keyboards and permission prompts that sit over an app
 * don't count as leaving it.
 *
 * Main thread only.
 */
object AppLockLaunchWatcher {
    private val AlwaysIgnoredPackages =
        setOf(
            "android",
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
        )

    private var appContext: Context? = null
    private var lockedPackages: Set<String> = emptySet()
    private var outsideQuickSearchEnabled = false
    private var ignoredPackages: Set<String> = AlwaysIgnoredPackages
    private var onActiveChanged: ((Boolean) -> Unit)? = null

    /** The app the user may stay in without unlocking again, until they leave it. */
    private var allowedPackage: String? = null

    /**
     * Until when Quick Search's own Home screen events are ignored after it let [allowedPackage]
     * through, since Home can refocus for a moment just before the app comes forward.
     */
    private var launchGraceUntil = 0L

    /** The app whose unlock screen is up, so its own repeated window events don't re-trigger it. */
    private var pendingPackage: String? = null

    private val changeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refreshState() }

    /** True when window events are needed: the switch is on and at least one app is locked. */
    val isActive: Boolean
        get() = outsideQuickSearchEnabled && lockedPackages.isNotEmpty()

    fun attach(context: Context, onActiveChanged: (Boolean) -> Unit) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        this.onActiveChanged = onActiveChanged
        AppLock.registerChangeListener(applicationContext, changeListener)
        refreshState()
    }

    fun detach() {
        appContext?.let { AppLock.unregisterChangeListener(it, changeListener) }
        appContext = null
        onActiveChanged = null
        allowedPackage = null
        pendingPackage = null
        launchGraceUntil = 0L
    }

    /** Lets [packageName] stay open without unlocking again until the user leaves it. */
    fun allow(packageName: String) {
        allowedPackage = packageName
        if (pendingPackage == packageName) pendingPackage = null
    }

    /**
     * Quick Search unlocked [packageName] and is about to open it. Like [allow], but also ignores
     * Quick Search's Home screen briefly while the prompt closes and the app starts.
     */
    fun allowLaunchFromQuickSearch(packageName: String) {
        allow(packageName)
        launchGraceUntil = SystemClock.elapsedRealtime() + LaunchGraceMillis
    }

    /** The unlock screen for [packageName] couldn't be shown or was closed without unlocking. */
    fun onUnlockFailed(packageName: String) {
        if (pendingPackage == packageName) pendingPackage = null
    }

    /**
     * Returns true when it started the unlock screen for the app that came forward, so other
     * watchers can wait until the app is unlocked.
     */
    fun onWindowStateChanged(event: AccessibilityEvent): Boolean {
        val context = appContext ?: return false
        if (!isActive) return false
        val packageName = event.packageName?.toString() ?: return false
        if (packageName in ignoredPackages) return false
        // Only a new activity means an app came forward; apps also report popups and leftovers.
        if (!isActivity(context, packageName, event.className?.toString())) return false
        if (packageName == context.packageName) {
            // Quick Search's own unlock screen and overlay sit on top of the app; only its Home
            // screen means the user left.
            if (event.className?.toString() == HomeActivity::class.java.name &&
                SystemClock.elapsedRealtime() >= launchGraceUntil
            ) {
                allowedPackage = null
                pendingPackage = null
            }
            return false
        }
        launchGraceUntil = 0L
        if (packageName == pendingPackage) return true
        if (packageName == allowedPackage) return false
        allowedPackage = null
        pendingPackage = null
        if (packageName !in lockedPackages) return false
        // App Lock only protects apps while Quick Search is the default launcher.
        if (!AppLockGate.isProtected(context, packageName)) return false
        pendingPackage = packageName
        AppLockActivity.start(context, packageName)
        return true
    }

    private const val LaunchGraceMillis = 5_000L

    private val activityClassCache = HashMap<String, Boolean>()

    private fun isActivity(context: Context, packageName: String, className: String?): Boolean {
        if (className.isNullOrEmpty()) return false
        val key = "$packageName/$className"
        return activityClassCache.getOrPut(key) {
            runCatching {
                context.packageManager.getActivityInfo(ComponentName(packageName, className), 0)
                true
            }.getOrDefault(false)
        }
    }

    private fun refreshState() {
        val context = appContext ?: return
        val wasActive = isActive
        lockedPackages = AppLock.lockedPackages(context)
        outsideQuickSearchEnabled = AppLock.isOutsideQuickSearchEnabled(context)
        ignoredPackages = AlwaysIgnoredPackages + inputMethodPackages(context)
        if (isActive != wasActive) onActiveChanged?.invoke(isActive)
    }

    private fun inputMethodPackages(context: Context): Set<String> =
        runCatching {
            context.getSystemService(InputMethodManager::class.java)
                ?.enabledInputMethodList
                ?.mapTo(mutableSetOf()) { it.packageName }
        }.getOrNull().orEmpty()
}
