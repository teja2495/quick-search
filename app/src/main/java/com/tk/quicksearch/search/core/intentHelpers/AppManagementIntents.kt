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
            context.startActivity(uninstallIntent(appInfo))
        } catch (e: Exception) {
            onShowToast?.invoke(R.string.error_uninstall_app, appInfo.appName)
        }
    }

    /** Activity-result callers wait for the installer before advancing a batch. */
    fun uninstallIntent(appInfo: AppInfo, returnResult: Boolean = false): Intent =
        IntentUtils.createPackageIntent(Intent.ACTION_DELETE, appInfo.packageName).apply {
            appInfo.userHandleId?.let(UserHandleUtils::of)?.let { putExtra(Intent.EXTRA_USER, it) }
            if (returnResult) {
                flags = flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
        }
}
