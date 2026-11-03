package com.tk.quicksearch.search.core.intentHelpers

import android.app.Application
import android.content.Intent
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.UserHandleUtils

/** App management related intents. */
internal object AppManagementIntents {
    /** Requests uninstall for an app. */
    fun requestUninstall(
        context: Application,
        appInfo: AppInfo,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        val packageName = appInfo.packageName
        if (packageName == context.packageName) {
            onShowToast?.invoke(R.string.error_uninstall_self, null)
            return
        }

        try {
            val intent = IntentUtils.createPackageIntent(Intent.ACTION_DELETE, packageName)
            appInfo.userHandleId?.let(UserHandleUtils::of)?.let {
                intent.putExtra(Intent.EXTRA_USER, it)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            onShowToast?.invoke(R.string.error_uninstall_app, appInfo.appName)
        }
    }
}