package com.tk.quicksearch.shared.util

import android.content.Intent
import android.content.pm.PackageManager

/** Returns true when this app is currently set as the device's default Home app (launcher). */
fun android.content.Context.isDefaultHomeApp(): Boolean {
    val resolveInfo =
        packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        ) ?: return false

    val resolvedPackage = resolveInfo.activityInfo?.packageName ?: return false
    if (resolvedPackage == "android") return false
    return resolvedPackage == packageName
}
