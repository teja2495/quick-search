package com.tk.quicksearch.search.contacts

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
    fun openVideoCall(context: Application, data: String, packageName: String, onShowToast: ((Int) -> Unit)? = null) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
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
    fun openCustomAppWithDataId(context: Application, dataId: Long, mimeType: String, packageName: String?): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    mimeType
                )
                packageName?.let { setPackage(it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if the intent can be resolved
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return true
            } else {
                Log.w("MessagingService", "Custom app intent cannot be resolved for mimeType: $mimeType")
                return false
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open custom app with dataId", e)
            return false
        }
    }

    /**
     * Opens a custom app with MIME type (fallback method).
     */
    fun openCustomApp(context: Application, data: String, mimeType: String, packageName: String?, onShowToast: ((Int) -> Unit)? = null) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(data), mimeType)
                packageName?.let { setPackage(it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open custom app", e)
            onShowToast?.invoke(R.string.error_launch_app)
        }
    }
}
