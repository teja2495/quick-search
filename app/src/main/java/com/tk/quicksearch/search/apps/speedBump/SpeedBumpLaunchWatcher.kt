package com.tk.quicksearch.search.apps.speedBump

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.tk.quicksearch.app.HomeActivity

/**
 * Shows the SpeedBump pause when a bumped app comes to the foreground from outside Quick Search
 * (another launcher, recents, a notification).
 *
 * Fed window-state events by the accessibility service. An app passes once it has been let through
 * (the pause ran out, or Quick Search launched it after its own pause) and stays allowed until
 * another app or a home screen takes the foreground. System UI, keyboards and permission prompts
 * that sit over an app don't count as leaving it.
 *
 * Main thread only.
 */
object SpeedBumpLaunchWatcher {
    private val AlwaysIgnoredPackages =
        setOf(
            "android",
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
        )

    private var appContext: Context? = null
    private var bumpedPackages: Set<String> = emptySet()
    private var outsideQuickSearchEnabled = false
    private var ignoredPackages: Set<String> = AlwaysIgnoredPackages
    private var onActiveChanged: ((Boolean) -> Unit)? = null

    /** The app the user may stay in without another pause, until they leave it. */
    private var allowedPackage: String? = null

    /**
     * Until when Quick Search's own Home screen events are ignored after it let [allowedPackage]
     * through. Closing Quick Search's pause dialog refocuses Home just before the app comes
     * forward, which would otherwise read as leaving the app and show the pause a second time.
     */
    private var launchGraceUntil = 0L

    /** The app whose pause is on screen, so its own repeated window events don't re-trigger it. */
    private var pendingPackage: String? = null

    private val changeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refreshState() }

    /** True when window events are needed: the switch is on and at least one app is bumped. */
    val isActive: Boolean
        get() = outsideQuickSearchEnabled && bumpedPackages.isNotEmpty()

    fun attach(context: Context, onActiveChanged: (Boolean) -> Unit) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        this.onActiveChanged = onActiveChanged
        SpeedBump.registerChangeListener(applicationContext, changeListener)
        refreshState()
    }

    fun detach() {
        appContext?.let { SpeedBump.unregisterChangeListener(it, changeListener) }
        appContext = null
        onActiveChanged = null
        allowedPackage = null
        pendingPackage = null
        launchGraceUntil = 0L
    }

    /** Lets [packageName] open without a pause until the user leaves it. */
    fun allow(packageName: String) {
        allowedPackage = packageName
        if (pendingPackage == packageName) pendingPackage = null
    }

    /**
     * Quick Search is about to launch [packageName] after its own pause. Like [allow], but also
     * ignores Quick Search's Home screen briefly while the pause closes and the app starts.
     */
    fun allowLaunchFromQuickSearch(packageName: String) {
        allow(packageName)
        launchGraceUntil = SystemClock.elapsedRealtime() + LaunchGraceMillis
    }

    /**
     * The pause for [packageName] couldn't be shown. A dismissed pause doesn't call this: the app
     * stays ignored until home or another app comes forward, since its window can resurface
     * for a moment while the pause closes.
     */
    fun onPauseFailed(packageName: String) {
        if (pendingPackage == packageName) pendingPackage = null
    }

    fun onWindowStateChanged(event: AccessibilityEvent) {
        val context = appContext ?: return
        if (!isActive) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName in ignoredPackages) return
        // Only a new activity means an app came forward. Apps also report popups and leftover
        // windows (Samsung's launcher sends one from the closing app ~1s after going Home).
        if (!isActivity(context, packageName, event.className?.toString())) return
        if (packageName == context.packageName) {
            // Quick Search's own pause and overlay sit on top of the app; only its Home screen
            // means the user left.
            if (event.className?.toString() == HomeActivity::class.java.name &&
                SystemClock.elapsedRealtime() >= launchGraceUntil
            ) {
                allowedPackage = null
                pendingPackage = null
            }
            return
        }
        // Another app came forward, so Quick Search's launch has landed (or was replaced).
        launchGraceUntil = 0L
        if (packageName == allowedPackage || packageName == pendingPackage) return
        allowedPackage = null
        pendingPackage = null
        if (packageName !in bumpedPackages) return
        pendingPackage = packageName
        SpeedBumpActivity.start(context, packageName)
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
        bumpedPackages = SpeedBump.bumpedPackages(context)
        outsideQuickSearchEnabled = SpeedBump.isOutsideQuickSearchEnabled(context)
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
