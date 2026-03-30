package com.tk.quicksearch.search.contacts.actions

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tk.quicksearch.R

/**
 * Custom app-specific contact intent helpers for opening various third-party apps.
 */
object CustomAppActions {
    /**
     * Opens a video call app with the specified package and data.
     */
    fun openVideoCall(
        context: Application,
        data: String,
        packageName: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        try {
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setData(Uri.parse("tel:$data"))
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open video call", e)
            onShowToast?.invoke(R.string.error_google_meet_video_call_failed)
        }
    }

    /**
     * Opens a custom app using contact data ID and MIME type.
     */
    fun openCustomAppWithDataId(
        context: Application,
        dataId: Long,
        mimeType: String,
        packageName: String?,
    ): Boolean {
        return try {
            launchContactDataIntent(
                context = context,
                dataId = dataId,
                packageName = packageName,
                mimeType = mimeType,
            )
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open custom app with dataId", e)
            false
        }
    }

    /**
     * Opens a custom app with MIME type (fallback method).
     */
    fun openCustomApp(
        context: Application,
        data: String,
        mimeType: String,
        packageName: String?,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        try {
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(data), mimeType)
                    packageName?.let { setPackage(it) }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open custom app", e)
            onShowToast?.invoke(R.string.common_error_unable_to_open)
        }
    }
}
