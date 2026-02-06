package com.tk.quicksearch.search.contacts.actions

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ContactMethodMimeTypes
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.util.PackageConstants

/** Signal-specific contact actions. */
object SignalActions {
    private const val TAG = "SignalActions"
    private const val CONTACT_DATA_URI_PREFIX = "content://com.android.contacts/data/"
    private const val EXTRA_IS_VIDEO_CALL = "is_video_call"
    private const val EXTRA_IS_VIDEO_OFF = "is_video_off"

    fun openSignalChat(
            context: Application,
            dataId: Long?,
            onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) return false
        return launchContactDataIntent(
                context = context,
                dataId = dataId,
                mimeType = ContactMethodMimeTypes.SIGNAL_MESSAGE,
                errorResId = R.string.error_signal_chat_failed,
                onShowToast = onShowToast,
        )
    }

    fun openSignalChat(
            context: Application,
            phoneNumber: String,
            onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) return false

        return try {
            val cleaned = PhoneNumberUtils.cleanPhoneNumber(phoneNumber) ?: return false
            val uri = Uri.parse("smsto:${Uri.encode(cleaned)}")
            val intent =
                    Intent(Intent.ACTION_SENDTO, uri).apply {
                        setPackage(PackageConstants.SIGNAL_PACKAGE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                onShowToast?.invoke(R.string.error_signal_not_installed)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Signal chat", e)
            onShowToast?.invoke(R.string.error_signal_chat_failed)
            false
        }
    }

    fun openSignalCall(
            context: Application,
            dataId: Long?,
            onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) return false
        return launchContactDataIntent(
                context = context,
                dataId = dataId,
                mimeType = ContactMethodMimeTypes.SIGNAL_CALL,
                errorResId = R.string.error_signal_call_failed,
                onShowToast = onShowToast,
        )
    }

    fun openSignalVideoCall(
            context: Application,
            dataId: Long?,
            onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) return false

        val contactDataUri = Uri.parse("$CONTACT_DATA_URI_PREFIX$dataId")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contactDataUri, ContactMethodMimeTypes.SIGNAL_VIDEO_CALL)
            setPackage(PackageConstants.SIGNAL_PACKAGE)
            putExtra(EXTRA_IS_VIDEO_CALL, true)
            putExtra(EXTRA_IS_VIDEO_OFF, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchIntentIfResolvable(context, intent)) return true

        Log.w(TAG, "Signal video call could not be initiated")
        if (isSignalInstalled(context)) {
            onShowToast?.invoke(R.string.error_signal_video_call_failed)
        } else {
            onShowToast?.invoke(R.string.error_signal_not_installed)
        }
        return false
    }

    private fun launchContactDataIntent(
            context: Application,
            dataId: Long,
            mimeType: String,
            errorResId: Int,
            onShowToast: ((Int) -> Unit)? = null,
    ): Boolean =
            try {
                val intent =
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse("$CONTACT_DATA_URI_PREFIX$dataId"), mimeType)
                            setPackage(PackageConstants.SIGNAL_PACKAGE)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                if (launchIntentIfResolvable(context, intent)) {
                    true
                } else {
                    if (isSignalInstalled(context)) {
                        onShowToast?.invoke(errorResId)
                    } else {
                        onShowToast?.invoke(R.string.error_signal_not_installed)
                    }
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Signal intent failed for mimeType=$mimeType", e)
                onShowToast?.invoke(errorResId)
                false
            }

    private fun launchIntentIfResolvable(
            context: Application,
            intent: Intent,
    ): Boolean {
        val canResolve = intent.resolveActivity(context.packageManager) != null
        if (!canResolve) return false
        context.startActivity(intent)
        return true
    }

    private fun isSignalInstalled(context: Application): Boolean =
            context.packageManager.getLaunchIntentForPackage(PackageConstants.SIGNAL_PACKAGE) !=
                    null
}
