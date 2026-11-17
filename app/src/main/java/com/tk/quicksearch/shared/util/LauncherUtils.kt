package com.tk.quicksearch.shared.util

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import com.tk.quicksearch.search.data.preferences.BootstrapPreferences

@Volatile
private var cachedDefaultHomeAppStatus: Boolean? = null

@Volatile
private var defaultHomeAppStatusResolvedAtMs = 0L  // 0 = never resolved in this process

// Several resume observers ask at once; the role can't change without the user leaving the app.
private const val DEFAULT_HOME_STATUS_REUSE_MS = 1_000L

/** Ends the reuse window, so the next resume asks the system again (the role may change while paused). */
fun expireDefaultHomeAppStatusReuse() {
    defaultHomeAppStatusResolvedAtMs = 0L
}

/** Returns true when this app is currently set as the device's default Home app (launcher). */
fun android.content.Context.isDefaultHomeApp(): Boolean {
    val now = SystemClock.elapsedRealtime()
    cachedDefaultHomeAppStatus?.let { cached ->
        val resolvedAtMs = defaultHomeAppStatusResolvedAtMs
        if (resolvedAtMs != 0L && now - resolvedAtMs in 0 until DEFAULT_HOME_STATUS_REUSE_MS) return cached
    }
    val result = resolveDefaultHomeAppStatus()
    // Write the launch-time hint only when it changes; an apply() on every resume is wasted disk work.
    if (result != (cachedDefaultHomeAppStatus ?: BootstrapPreferences.getDefaultHomeHint(this))) {
        BootstrapPreferences.setDefaultHomeHint(this, result)
    }
    cachedDefaultHomeAppStatus = result
    defaultHomeAppStatusResolvedAtMs = now
    return result
}

/** Uses the launch-time role snapshot when available, avoiding system-service work in composition. */
fun android.content.Context.cachedDefaultHomeAppStatus(): Boolean =
    cachedDefaultHomeAppStatus ?: BootstrapPreferences.getDefaultHomeHint(this).also {
        cachedDefaultHomeAppStatus = it
    }

private fun android.content.Context.resolveDefaultHomeAppStatus(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
            return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        }
    }

    val resolveInfo =
        packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        ) ?: return false

    val resolvedPackage = resolveInfo.activityInfo?.packageName ?: return false
    if (resolvedPackage == "android") return false
    return resolvedPackage == packageName
}
