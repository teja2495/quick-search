package com.tk.quicksearch.search.core.intentHelpers

import android.app.Application
import android.content.Intent

/** Basic intent utilities. */
internal object IntentUtils {
    /** Checks if the given intent can be resolved by any activity. */
    fun canResolveIntent(
        context: Application,
        intent: Intent,
    ): Boolean = intent.resolveActivity(context.packageManager) != null

    /** Creates an intent with package URI and NEW_TASK flag. */
    fun createPackageIntent(
        action: String,
        packageName: String,
    ): Intent =
        Intent(action).apply {
            data = android.net.Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}